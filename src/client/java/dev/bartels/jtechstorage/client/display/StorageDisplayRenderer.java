package dev.bartels.jtechstorage.client.display;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

import dev.bartels.jtechstorage.display.DisplayShape;
import dev.bartels.jtechstorage.display.StorageDisplayBlockEntity;
import dev.bartels.jtechstorage.display.StorageDisplayBlockEntity.Mode;

/**
 * Lager-Bildschirm (v0.34): leuchtende Anzeige in einem dunklen Rahmen mit Amethyst-Ecken. Zusammengesetzte Bildschirme
 * (bis 4×3) zeichnet der Block oben links als ein Bild. Zahlen zählen weich zum neuen Wert, Gegenstände drehen sich.
 */
public class StorageDisplayRenderer implements BlockEntityRenderer<StorageDisplayBlockEntity, StorageDisplayRenderer.State> {
	private static final int BEZEL_TOP = 0xFF2E2446;
	private static final int BEZEL_BOTTOM = 0xFF1A1428;
	private static final int RIM = 0xFF07050B;
	private static final int SCREEN_TOP = 0xFF1B1430;
	private static final int SCREEN_BOTTOM = 0xFF0A0711;
	private static final int GEM = 0xFFC08CFF;
	private static final int TEXT = 0xFFEDE6FF;
	private static final int SUBTLE = 0xFF9A8FB8;
	/** Pro Bildschirm gesetzt (v0.36): Licht des Inhalts, Akzentfarben, Rahmenbreite. */
	private static int LIGHT = PanelCanvas.FULL_BRIGHT;
	private static int ACC = 0xFF7FE3F0;
	private static int ACC_TITLE = 0xFFB98CFF;
	private static final int BAD = 0xFFFF7A7A;
	private static float BEZEL = 3.0f;

	private final ItemModelResolver items;
	private final Font font;
	/** Angezeigter (weich nachlaufender) Zählerstand je Bildschirm. */
	private final Map<BlockPos, double[]> shown = new HashMap<>();

	public StorageDisplayRenderer(BlockEntityRendererProvider.Context context) {
		items = context.itemModelResolver();
		font = context.font();
	}

	public static class State extends BlockEntityRenderState {
		Direction facing = Direction.NORTH;
		boolean draw;
		int width = 1;
		int height = 1;
		boolean online;
		boolean linked;
		Mode mode = Mode.ITEM;
		String network = "";
		final ItemStackRenderState tracked = new ItemStackRenderState();
		boolean hasTracked;
		String trackedName = "";
		long count;
		double shownCount;
		int trend;
		int used;
		int total;
		long[] history = new long[0];
		final ItemStackRenderState[] top = {new ItemStackRenderState(), new ItemStackRenderState(), new ItemStackRenderState(),
				new ItemStackRenderState(), new ItemStackRenderState()};
		String[] topNames = new String[0];
		long[] topCounts = new long[0];
		float time;
		// Aussehen (v0.36)
		int[] frame = PanelLooks.FRAME[0];
		boolean frameless;
		int[] background = PanelLooks.BACKGROUND[0];
		int accent = PanelLooks.ACCENT[0];
		boolean glow = true;
		boolean scanlines = true;
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(StorageDisplayBlockEntity display, State state, float partialTick, Vec3 camera,
			ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(display, state, crumbling);
		var level = display.getLevel();
		state.facing = display.getBlockState().hasProperty(BlockStateProperties.HORIZONTAL_FACING)
				? display.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING) : Direction.NORTH;
		DisplayShape shape = level == null ? new DisplayShape(display.getBlockPos(), 1, 1) : DisplayShape.of(level, display.getBlockPos(), state.facing);
		state.draw = shape.origin().equals(display.getBlockPos());

		if (!state.draw) {
			return;
		}

		state.frame = PanelLooks.at(PanelLooks.FRAME, display.getFrame());
		state.frameless = display.getFrame() == 5;
		state.background = PanelLooks.at(PanelLooks.BACKGROUND, display.getBackground());
		state.accent = PanelLooks.at(PanelLooks.ACCENT, display.getAccent());
		state.glow = display.isGlow();
		state.scanlines = display.hasScanlines();
		state.width = shape.width();
		state.height = shape.height();
		state.time = level == null ? 0.0f : (level.getGameTime() % 240000L) + partialTick;
		state.online = display.isOnline();
		state.linked = display.getLink() != null;
		state.mode = display.getMode();
		state.network = display.getNetworkName();
		ItemStack tracked = display.getTracked();
		state.hasTracked = !tracked.isEmpty();
		state.trackedName = state.hasTracked ? tracked.getHoverName().getString() : "";
		state.tracked.clear();

		if (state.hasTracked && level != null) {
			items.updateForTopItem(state.tracked, tracked, ItemDisplayContext.FIXED, level, null, (int) display.getBlockPos().asLong());
		}

		state.count = display.getCount();
		state.trend = display.getTrend();
		state.used = display.getUsed();
		state.total = display.getTotal();
		List<Long> history = display.getHistory();
		state.history = history.stream().mapToLong(Long::longValue).toArray();

		// Zähler läuft weich zum neuen Wert (etwa eine halbe Sekunde)
		double[] value = shown.computeIfAbsent(display.getBlockPos().immutable(), p -> new double[] {state.count, state.time});
		float dt = Math.max(0.0f, Math.min(20.0f, state.time - (float) value[1]));
		value[0] += (state.count - value[0]) * (1.0 - Math.exp(-dt / 5.0));

		if (Math.abs(state.count - value[0]) < 0.5) {
			value[0] = state.count;
		}

		value[1] = state.time;
		state.shownCount = value[0];

		if (shown.size() > 512) {
			shown.clear();
		}

		List<ItemStack> top = display.getTopItems();
		List<Long> counts = display.getTopCounts();
		int n = Math.min(5, Math.min(top.size(), counts.size()));
		state.topNames = new String[n];
		state.topCounts = new long[n];

		for (int i = 0; i < 5; i++) {
			state.top[i].clear();

			if (i < n && level != null) {
				items.updateForTopItem(state.top[i], top.get(i), ItemDisplayContext.GUI, level, null, i);
				state.topNames[i] = top.get(i).getHoverName().getString();
				state.topCounts[i] = counts.get(i);
			}
		}
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		if (!state.draw) {
			return;
		}

		pose.pushPose();
		pose.translate(0.5f, 0.5f, 0.5f);
		pose.rotateDegrees(Axis.YP, -state.facing.toYRot());
		// oben links auf der Vorderseite der 2 px starken Platte
		pose.translate(-0.5f, 0.5f, -0.5f + 2.0f / 16.0f + 0.002f);
		LIGHT = state.glow ? PanelCanvas.FULL_BRIGHT : state.lightCoords;
		ACC = state.accent;
		ACC_TITLE = PanelCanvas.mix(state.accent, 0xFFFFFFFF, 0.2f);
		BEZEL = state.frameless ? 0.6f : 3.0f;
		PanelCanvas c = new PanelCanvas(pose, collector, font);
		float w = state.width * PanelCanvas.PPB;
		float h = state.height * PanelCanvas.PPB;
		frame(c, state, w, h);

		float x0 = BEZEL + 4;
		float y0 = BEZEL + 4;
		float x1 = w - BEZEL - 4;
		float y1 = h - BEZEL - 4;

		if (!state.linked || !state.online) {
			offline(c, state, x0, y0, x1, y1);
		} else {
			switch (state.mode) {
				case ITEM -> item(c, state, x0, y0, x1, y1);
				case FILL -> fill(c, state, x0, y0, x1, y1, true);
				case GRAPH -> graph(c, state, x0, y0, x1, y1);
				case TOP -> top(c, state, x0, y0, x1, y1);
			}
		}

		c.flush();
		pose.popPose();
	}

	@Override
	public boolean shouldRenderOffScreen() {
		return true;
	}

	@Override
	public int getViewDistance() {
		return 48;
	}

	// ------------------------------------------------------------------ Rahmen

	private static void frame(PanelCanvas c, State s, float w, float h) {
		int light = s.lightCoords;
		int glow = LIGHT;

		if (!s.frameless) {
			c.rect(0, 0, w, h, 0, s.frame[0], s.frame[1], light);
			c.frame(0, 0, w, h, 0.6f, 1, s.frame[2], light);                 // helle Außenkante
			c.rect(BEZEL - 0.6f, BEZEL - 0.6f, w - BEZEL + 0.6f, h - BEZEL + 0.6f, 1, RIM, light);
		} else {
			c.frame(0, 0, w, h, 0.6f, 1, RIM, light);                        // nur eine feine dunkle Kante
		}

		c.rect(BEZEL, BEZEL, w - BEZEL, h - BEZEL, 2, s.background[0], s.background[1], glow);

		if (!s.frameless) {
			// Amethyst-Ecken
			float g = 1.6f;
			float[][] corners = {{1.4f, 1.4f}, {w - 1.4f, 1.4f}, {1.4f, h - 1.4f}, {w - 1.4f, h - 1.4f}};

			for (float[] p : corners) {
				c.quad(p[0], p[1] - g, p[0] + g, p[1], p[0], p[1] + g, p[0] - g, p[1], 2, GEM, 0xFF9A66E8, 0xFF6E42B8, 0xFF9A66E8, glow);
			}
		}

		// Glas: Spiegelung oben, feine Rasterlinien
		c.rect(BEZEL, BEZEL, w - BEZEL, BEZEL + (h - 2 * BEZEL) * 0.35f, 9, 0x14FFFFFF, 0x00FFFFFF, glow);

		if (s.scanlines) {
			for (float y = BEZEL + 2; y < h - BEZEL; y += 3) {
				c.rect(BEZEL, y, w - BEZEL, y + 0.5f, 9, 0x22000000, glow);
			}
		}

		// Status-Licht
		float pulse = 0.55f + 0.45f * (float) Math.sin(s.time * 0.12f);
		int led = s.linked && s.online ? PanelCanvas.mix(0xFF2E7D4E, 0xFF7CFFA0, pulse)
				: (((int) (s.time / 10)) % 2 == 0 ? BAD : 0xFF5A2A2A);
		c.rect(w - BEZEL - 5, BEZEL + 2, w - BEZEL - 2.5f, BEZEL + 4.5f, 6, led, glow);
	}

	// ------------------------------------------------------------------ Anzeigen

	private static void offline(PanelCanvas c, State s, float x0, float y0, float x1, float y1) {
		int glow = LIGHT;
		float cx = (x0 + x1) / 2;
		float cy = (y0 + y1) / 2;
		String title = Component.translatable(s.linked ? "jtechstorage.display.screen.offline" : "jtechstorage.display.screen.unlinked").getString();
		float scale = c.fit(title, Math.min(2.0f, (x1 - x0) / 60.0f), x1 - x0);
		c.text(title, cx, cy - 9 * scale, scale, 0.5f, BAD, 5, glow);
		hint(c, Component.translatable("jtechstorage.display.screen.link_hint"), x0, cy + 3, x1, y1, Math.max(0.6f, scale * 0.6f));
	}

	/** Mehrzeiliger, zentrierter Hinweis. */
	private static void hint(PanelCanvas c, Component text, float x0, float y, float x1, float y1, float scale) {
		List<FormattedCharSequence> lines = c.font.split(text, (int) ((x1 - x0) / scale));

		for (FormattedCharSequence line : lines) {
			if (y + 9 * scale > y1) {
				break;
			}

			c.text(line, c.font.width(line), (x0 + x1) / 2, y, scale, 0.5f, SUBTLE, 5, LIGHT);
			y += 10 * scale;
		}
	}

	private static boolean noItem(PanelCanvas c, State s, float x0, float y0, float x1, float y1) {
		if (s.hasTracked) {
			return false;
		}

		float scale = Math.max(0.6f, Math.min(1.3f, (x1 - x0) / 90.0f));
		String title = Component.translatable("jtechstorage.display.screen.no_item").getString();
		float ts = c.fit(title, scale * 1.2f, x1 - x0);
		float cy = (y0 + y1) / 2;
		c.text(title, (x0 + x1) / 2, cy - 12 * ts, ts, 0.5f, ACC_TITLE, 5, LIGHT);
		hint(c, Component.translatable("jtechstorage.display.screen.no_item_hint"), x0, cy, x1, y1, scale * 0.7f);
		return true;
	}

	/** Gegenstand + Bestand + Trend. Hochkant und 1×1: untereinander; breit: Gegenstand links, Zahlen rechts. */
	private static void item(PanelCanvas c, State s, float x0, float y0, float x1, float y1) {
		if (noItem(c, s, x0, y0, x1, y1)) {
			return;
		}

		int glow = LIGHT;
		float w = x1 - x0;
		float h = y1 - y0;
		long shownCount = Math.round(s.shownCount);
		String trend = PanelCanvas.trend(s.trend);
		float spin = s.time * 2.4f;

		if (s.width > s.height) {
			float size = Math.min(h * 0.78f, w * 0.34f);
			float cx = x0 + size * 0.6f;
			float cy = (y0 + y1) / 2;
			c.rect(cx - size * 0.4f, cy + size * 0.42f, cx + size * 0.4f, cy + size * 0.46f, 4, 0x40B98CFF, glow);
			c.spinning(s.tracked, cx, cy - size * 0.04f, size * 0.82f, spin, 6, glow);
			float tx = x0 + size * 1.3f;
			float tw = x1 - tx;
			float nameScale = c.fit(s.trackedName, Math.max(1.0f, h / 70.0f), tw);
			String number = c.number(shownCount, 1, tw / Math.max(1.0f, h / 30.0f));
			float countScale = c.fit(number, h / 30.0f, tw);
			float trendScale = c.fit(trend, Math.max(1.0f, h / 55.0f), tw);
			float total = 9 * nameScale + 4 + 8 * countScale + 5 + 9 * trendScale;
			float ty = cy - total / 2;
			c.text(s.trackedName, tx, ty, nameScale, 0, SUBTLE, 5, glow);
			ty += 9 * nameScale + 4;
			c.text(number, tx, ty, countScale, 0, TEXT, 5, glow);
			ty += 8 * countScale + 5;
			c.text(arrow(s.trend) + " " + trend, tx, ty, trendScale, 0, PanelCanvas.trendColor(s.trend), 5, glow);
			return;
		}

		float cx = (x0 + x1) / 2;
		float size = Math.min(w * 0.46f, h * 0.42f);
		float iconY = s.height > s.width ? (y0 + y1) / 2 - size * 0.45f : y0 + h * 0.27f;
		c.rect(cx - size * 0.6f, iconY + size * 0.5f, cx + size * 0.6f, iconY + size * 0.56f, 4, 0x40B98CFF, glow);
		c.spinning(s.tracked, cx, iconY, size, spin, 6, glow);
		String number = c.number(shownCount, w / 64.0f, w);
		float countScale = c.fit(number, w / 64.0f * (s.height > 1 ? 1.2f : 1.0f), w);
		float trendScale = c.fit(trend, w / 84.0f, w);
		float ty = iconY + size * 0.72f;
		c.text(number, cx, ty, countScale, 0.5f, TEXT, 5, glow);
		ty += 9 * countScale + 2;
		c.text(trend, cx, ty, trendScale, 0.5f, PanelCanvas.trendColor(s.trend), 5, glow);

		if (s.height > 1) {
			float ns = c.fit(s.trackedName, w / 90.0f, w);
			c.text(s.trackedName, cx, y1 - 10 * ns, ns, 0.5f, SUBTLE, 5, glow);
		}
	}

	private static String arrow(int trend) {
		return trend > 0 ? "▲" : trend < 0 ? "▼" : "■";
	}

	/** Füllstand des Netzwerks. @return Unterkante des belegten Bereichs */
	private static float fill(PanelCanvas c, State s, float x0, float y0, float x1, float y1, boolean big) {
		int glow = LIGHT;
		float w = x1 - x0;
		float ratio = s.total <= 0 ? 0.0f : Math.min(1.0f, s.used / (float) s.total);
		int percent = Math.round(ratio * 100);
		String title = s.network.isEmpty() ? Component.translatable("jtechstorage.display.screen.storage").getString()
				: Component.translatable("jtechstorage.display.screen.storage_named", s.network).getString();
		String slots = Component.translatable("jtechstorage.display.screen.slots", PanelCanvas.full(s.used), PanelCanvas.full(s.total)).getString();
		int barLeft = ratio > 0.9f ? 0xFFFF5A6A : ratio > 0.75f ? 0xFFFFA24A : 0xFF7A4CFF;
		int barRight = ratio > 0.9f ? 0xFFFF9A7A : ratio > 0.75f ? 0xFFFFD27A : 0xFFE08CFF;
		float unit = Math.min(1.8f, Math.min(w / 90.0f, (y1 - y0) / 80.0f));
		float titleScale = c.fit(title, Math.max(0.8f, unit), w);
		float y = y0;
		c.text(title, x0, y, titleScale, 0, ACC_TITLE, 5, glow);
		y += 10 * titleScale + 3;

		if (big && s.height >= s.width) {
			// hochkant / 1×1: große Prozentzahl
			String pct = percent + " %";
			float ps = c.fit(pct, unit * 2.6f, w);
			c.text(pct, (x0 + x1) / 2, y + 2, ps, 0.5f, TEXT, 5, glow);
			y += 9 * ps + 6;
		}

		float barH = Math.max(6.0f, 9.0f * unit);
		bar(c, x0, y, x1, y + barH, ratio, barLeft, barRight, s.time);
		y += barH + 3;
		String line = big && s.height >= s.width ? slots : percent + " %  ·  " + slots;
		float ls = c.fit(line, Math.max(0.7f, unit * 0.9f), w);
		c.text(line, x0, y, ls, 0, SUBTLE, 5, glow);
		return y + 10 * ls;
	}

	private static void bar(PanelCanvas c, float x0, float y0, float x1, float y1, float ratio, int left, int right, float time) {
		int glow = LIGHT;
		c.rect(x0, y0, x1, y1, 3, 0xFF07050C, glow);
		c.frame(x0, y0, x1, y1, 0.7f, 4, 0xFF5A4A80, glow);
		float fx = x0 + 1 + (x1 - x0 - 2) * ratio;

		if (ratio > 0) {
			c.hrect(x0 + 1, y0 + 1, fx, y1 - 1, 5, left, PanelCanvas.mix(left, right, ratio), glow);
			c.rect(x0 + 1, y0 + 1, fx, y0 + (y1 - y0) * 0.45f, 6, 0x38FFFFFF, 0x08FFFFFF, glow);
			// wandernder Glanz
			float t = (time * 0.02f) % 1.6f - 0.3f;
			float sx = x0 + (x1 - x0) * t;

			if (sx > x0 + 1 && sx + 6 < fx) {
				c.quad(sx, y0 + 1, sx + 4, y0 + 1, sx + 6, y1 - 1, sx + 2, y1 - 1, 7, 0x30FFFFFF, 0x30FFFFFF, 0x00FFFFFF, 0x00FFFFFF, glow);
			}
		}

		// Teilstriche
		for (int i = 1; i < 10; i++) {
			float tx = x0 + (x1 - x0) * i / 10.0f;
			c.rect(tx - 0.25f, y1 - 2.0f, tx + 0.25f, y1 - 1, 7, 0x50FFFFFF, glow);
		}
	}

	/** Verlauf des Bestands (letzte 10 Minuten); auf hohen Bildschirmen mit Füllstand darüber. */
	private static void graph(PanelCanvas c, State s, float x0, float y0, float x1, float y1) {
		int glow = LIGHT;

		if (s.height >= 2) {
			y0 = fill(c, s, x0, y0, x1, y1, false) + 4;
		}

		if (noItem(c, s, x0, y0, x1, y1)) {
			return;
		}

		float w = x1 - x0;
		float unit = Math.max(0.7f, Math.min(1.8f, Math.min(w / 120.0f, (y1 - y0) / 70.0f)));

		// Graph-Kasten
		float gx0 = x0;
		float gx1 = x1;
		float gy0 = y0;
		float labelH = 9 * unit * 0.8f + 2;
		float gy1 = y1 - labelH;
		c.rect(gx0, gy0, gx1, gy1, 3, 0xFF0D0A17, 0xFF080610, glow);
		c.frame(gx0, gy0, gx1, gy1, 0.7f, 4, 0xFF3E3460, glow);

		for (int i = 1; i < 4; i++) {
			float gy = gy0 + (gy1 - gy0) * i / 4.0f;

			for (float gx = gx0 + 2; gx < gx1 - 2; gx += 4) {
				c.rect(gx, gy - 0.2f, Math.min(gx + 2, gx1 - 2), gy + 0.2f, 4, 0x30FFFFFF, glow);
			}
		}

		// Kopfzeile im Kasten
		float hs = unit;
		float iconSize = 10 * hs;
		String head = s.trackedName + ": " + c.number(Math.round(s.shownCount), hs, w * 0.5f);
		String trend = "  " + PanelCanvas.trend(s.trend);
		float headScale = c.fit(head + trend, hs, gx1 - gx0 - iconSize - 8);

		if (headScale < 0.8f) {
			// schmal: nur Zahl und Trend
			head = c.number(Math.round(s.shownCount), hs, w * 0.5f);
			trend = " " + PanelCanvas.trend(s.trend);
			headScale = c.fit(head + trend, hs, gx1 - gx0 - iconSize - 8);
		}
		c.icon(s.tracked, gx0 + 3 + iconSize / 2, gy0 + 3 + iconSize / 2, iconSize, 8, glow);
		float tx = gx0 + iconSize + 6;
		float ty = gy0 + 3 + (iconSize - 8 * headScale) / 2;
		c.text(head, tx, ty, headScale, 0, TEXT, 8, glow);
		c.text(trend, tx + c.width(head) * headScale, ty, headScale, 0, PanelCanvas.trendColor(s.trend), 8, glow);

		// Werte: Verlauf + aktueller Stand
		int n = s.history.length;
		long[] values = new long[n + 1];
		System.arraycopy(s.history, 0, values, 0, n);
		values[n] = Math.round(s.shownCount);
		long min = Long.MAX_VALUE;
		long max = Long.MIN_VALUE;

		for (long v : values) {
			min = Math.min(min, v);
			max = Math.max(max, v);
		}

		float py0 = gy0 + iconSize + 7;
		float py1 = gy1 - 3;
		float px0 = gx0 + 3;
		float px1 = gx1 - 3;

		if (values.length < 2) {
			c.text("…", (px0 + px1) / 2, (py0 + py1) / 2, unit, 0.5f, SUBTLE, 8, glow);
		} else {
			double range = Math.max(1, max - min);
			double pad = range * 0.12;
			double lo = min - pad;
			double hi = max + pad;
			float lineT = Math.max(0.9f, 1.2f * unit);
			float prevX = 0;
			float prevY = 0;

			for (int i = 0; i < values.length; i++) {
				float px = px0 + (px1 - px0) * i / (values.length - 1);
				float py = (float) (py1 - (py1 - py0) * (values[i] - lo) / (hi - lo));

				if (i > 0) {
					// Fläche unter der Linie, dann die Linie
					c.quad(prevX, prevY, px, py, px, py1, prevX, py1, 6, 0x50 << 24 | (ACC & 0xFFFFFF), 0x50 << 24 | (ACC & 0xFFFFFF),
							0x06 << 24 | (ACC & 0xFFFFFF), 0x06 << 24 | (ACC & 0xFFFFFF), glow);
					c.line(prevX, prevY, px, py, lineT, 7, ACC, glow);
					c.rect(px - lineT / 2, py - lineT / 2, px + lineT / 2, py + lineT / 2, 7, ACC, glow);
				}

				prevX = px;
				prevY = py;
			}

			// pulsierender Punkt am aktuellen Wert
			float r = lineT * (1.4f + 0.5f * (float) Math.sin(s.time * 0.2f));
			c.quad(prevX, prevY - r, prevX + r, prevY, prevX, prevY + r, prevX - r, prevY, 8, 0xFFFFFFFF, ACC, ACC, ACC, glow);
		}

		// Zeitachse
		float ls = unit * 0.8f;
		int minutes = Math.max(1, Math.round(n * 10 / 60.0f));
		c.text(Component.translatable("jtechstorage.display.screen.ago", minutes).getString(), gx0, gy1 + 2, ls, 0, SUBTLE, 5, glow);
		c.text(Component.translatable("jtechstorage.display.screen.now").getString(), gx1, gy1 + 2, ls, 1, SUBTLE, 5, glow);
	}

	/** Die fünf häufigsten Gegenstände im Netzwerk. */
	private static void top(PanelCanvas c, State s, float x0, float y0, float x1, float y1) {
		int glow = LIGHT;
		float w = x1 - x0;
		float unit = Math.max(0.6f, Math.min(1.8f, Math.min(w / 90.0f, (y1 - y0) / 80.0f)));
		String title = Component.translatable("jtechstorage.display.screen.top").getString();

		if (!s.network.isEmpty()) {
			title += " · " + s.network;
		}

		float ts = c.fit(title, unit, w);
		c.text(title, x0, y0, ts, 0, ACC_TITLE, 5, glow);
		float top = y0 + 10 * ts + 3;
		int n = s.topCounts.length;

		if (n == 0) {
			c.text(Component.translatable("jtechstorage.display.screen.empty").getString(), (x0 + x1) / 2, (top + y1) / 2 - 4 * unit, unit, 0.5f, SUBTLE, 5, glow);
			return;
		}

		float rowH = (y1 - top) / 5.0f;
		long max = Math.max(1, s.topCounts[0]);
		boolean names = w / rowH > 4.5f;
		int[] medal = {0xFFFFD24A, 0xFFD8DCE8, 0xFFE09A5A, SUBTLE, SUBTLE};

		for (int i = 0; i < n; i++) {
			float ry = top + i * rowH;
			float ratio = s.topCounts[i] / (float) max;
			c.hrect(x0, ry + 1, x0 + w * ratio, ry + rowH - 1, 4, 0x60B98CFF, 0x10B98CFF, glow);
			float rs = Math.min(unit, rowH / 11.0f);
			float icon = Math.min(rowH * 0.8f, 12.0f * rs + 3.0f);
			c.text(String.valueOf(i + 1), x0 + 1, ry + (rowH - 8 * rs) / 2, rs, 0, medal[i], 6, glow);
			float ix = x0 + 7 * rs + 2;
			c.icon(s.top[i], ix + icon / 2, ry + rowH / 2, icon, 6, glow);
			float textX = ix + icon + 3;
			String number = c.number(s.topCounts[i], rs, (x1 - textX) * (names ? 0.4f : 1.0f));
			float ns = c.fit(number, rs, x1 - textX);
			c.text(number, x1, ry + (rowH - 8 * ns) / 2, ns, 1, TEXT, 6, glow);

			if (names) {
				float room = x1 - textX - c.width(number) * ns - 6;
				String name = s.topNames[i];

				while (name.length() > 1 && c.width(name) * rs > room) {
					name = name.substring(0, name.length() - 2) + "…";
				}

				c.text(name, textX, ry + (rowH - 8 * rs) / 2, rs, 0, SUBTLE, 6, glow);
			}
		}
	}
}
