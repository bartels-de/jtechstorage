package dev.bartels.jtechstorage.client.display;

import java.util.List;

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
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

import dev.bartels.jtechstorage.JTechStorage;
import dev.bartels.jtechstorage.display.StorageBannerBlockEntity;

/**
 * Lager-Banner (v0.34): Samtbahn mit Goldsaum, zwei Blöcke lang, weht sanft. Darauf eine Laufschrift mit bis zu acht
 * Gegenständen, ihrem Bestand und Trend – bei mehr als fünf Einträgen rollt sie langsam nach oben.
 */
public class StorageBannerRenderer implements BlockEntityRenderer<StorageBannerBlockEntity, StorageBannerRenderer.State> {
	private static final Identifier CLOTH = JTechStorage.id("textures/entity/display/storage_banner_cloth.png");
	private static final Identifier TRIM = JTechStorage.id("textures/entity/display/storage_banner_trim.png");
	private static final Identifier[] PATTERNS = {null, JTechStorage.id("textures/entity/display/storage_banner_pattern_stripes.png"),
			JTechStorage.id("textures/entity/display/storage_banner_pattern_rhombus.png"),
			JTechStorage.id("textures/entity/display/storage_banner_pattern_stars.png"),
			JTechStorage.id("textures/entity/display/storage_banner_pattern_crystals.png")};
	private static final int COLS = 6;
	private static final int ROWS = 28;
	/** Stoffbahn in Pixeln (96 pro Block): links/rechts, oben/unten. */
	private static final float CX0 = 3.0f;
	private static final float CX1 = 93.0f;
	private static final float CY0 = 9.0f;
	private static final float CY1 = 192.0f;
	/** Abstand des Stoffs zur Wand (Blöcke) – vor der Stange. */
	private static final float WALL = 3.3f / 16.0f;
	/** Bereich der Laufschrift (Pixel, entspricht dem dunklen Feld der Textur). */
	private static final float LIST_TOP = 55.0f;
	private static final float LIST_BOTTOM = 166.0f;
	private static final float ROW_H = 19.0f;
	private static final int GOLD = 0xFFF2CF6B;
	private static final int CREAM = 0xFFFFF3DA;
	private static final int SUBTLE = 0xFFB9A8DA;

	private final ItemModelResolver items;
	private final Font font;

	public StorageBannerRenderer(BlockEntityRendererProvider.Context context) {
		items = context.itemModelResolver();
		font = context.font();
	}

	public static class State extends BlockEntityRenderState {
		Direction facing = Direction.NORTH;
		boolean online;
		boolean linked;
		String network = "";
		final ItemStackRenderState[] icons = new ItemStackRenderState[StorageBannerBlockEntity.MAX];
		long[] counts = new long[0];
		int[] trends = new int[0];
		float time;
		float phase;
		// Aussehen (v0.36)
		int clothColor = 0xFF8932B8;
		int trimColor = 0xFFE8BB4A;
		int pattern;
		int textColor = 0xFFFFF3DA;
		float wind = 1.0f;

		State() {
			for (int i = 0; i < icons.length; i++) {
				icons[i] = new ItemStackRenderState();
			}
		}
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(StorageBannerBlockEntity banner, State state, float partialTick, Vec3 camera,
			ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(banner, state, crumbling);
		var level = banner.getLevel();
		state.facing = banner.getBlockState().hasProperty(BlockStateProperties.HORIZONTAL_FACING)
				? banner.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING) : Direction.NORTH;
		state.time = level == null ? 0.0f : (level.getGameTime() % 240000L) + partialTick;
		state.phase = (banner.getBlockPos().getX() * 7 + banner.getBlockPos().getZ() * 13 + banner.getBlockPos().getY() * 3) * 0.37f;
		state.online = banner.isOnline();
		state.linked = banner.getLink() != null;
		state.network = banner.getNetworkName();
		state.clothColor = 0xFF000000 | net.minecraft.world.item.DyeColor.byId(Math.floorMod(banner.getCloth(), 16)).getTextureDiffuseColor();
		state.trimColor = PanelLooks.at(PanelLooks.TRIM, banner.getTrim());
		state.pattern = Math.floorMod(banner.getPattern(), PATTERNS.length);
		state.textColor = PanelLooks.at(PanelLooks.TEXT, banner.getTextColor());
		state.wind = PanelLooks.WIND[Math.floorMod(banner.getWind(), PanelLooks.WIND.length)];
		List<ItemStack> list = banner.getItems();
		int n = Math.min(StorageBannerBlockEntity.MAX, Math.min(list.size(), Math.min(banner.getCounts().size(), banner.getTrends().size())));
		state.counts = new long[n];
		state.trends = new int[n];

		for (int i = 0; i < state.icons.length; i++) {
			state.icons[i].clear();

			if (i < n && level != null) {
				items.updateForTopItem(state.icons[i], list.get(i), ItemDisplayContext.GUI, level, null, i);
				state.counts[i] = banner.getCounts().get(i);
				state.trends[i] = banner.getTrends().get(i);
			}
		}
	}

	/** Auslenkung des Stoffs (Blöcke, zum Betrachter) an der Stelle u (0..1 quer) und v (0..1 von oben). */
	private static float wave(State s, float u, float v) {
		float hang = (float) Math.pow(v, 1.3);
		float a = (float) Math.sin(s.time * 0.065f + s.phase + v * 4.2f - u * 1.3f);
		float b = (float) Math.sin(s.time * 0.031f + s.phase * 1.7f + v * 2.1f + u * 0.8f);
		return hang * (0.045f + (0.03f * a + 0.015f * b) * s.wind);
	}

	private static float sway(State s, float v) {
		return (float) Math.sin(s.time * 0.043f + s.phase + v * 2.4f) * v * 1.6f * s.wind;
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		pose.pushPose();
		pose.translate(0.5f, 0.5f, 0.5f);
		pose.rotateDegrees(Axis.YP, -state.facing.toYRot());
		pose.translate(-0.5f, 0.5f, -0.5f + WALL);   // oben links, Stoffebene
		cloth(state, pose, collector);
		content(state, pose, collector);
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

	// ------------------------------------------------------------------ Stoff

	private void cloth(State s, PoseStack pose, SubmitNodeCollector collector) {
		float[][] x = new float[ROWS + 1][COLS + 1];
		float[][] y = new float[ROWS + 1][COLS + 1];
		float[][] z = new float[ROWS + 1][COLS + 1];

		for (int r = 0; r <= ROWS; r++) {
			float v = r / (float) ROWS;

			for (int k = 0; k <= COLS; k++) {
				float u = k / (float) COLS;
				x[r][k] = (CX0 + (CX1 - CX0) * u + sway(s, v)) / PanelCanvas.PPB;
				y[r][k] = -(CY0 + (CY1 - CY0) * v) / PanelCanvas.PPB;
				z[r][k] = wave(s, u, v);
			}
		}

		int light = s.lightCoords;
		layer(pose, collector, CLOTH, s.clothColor, 0.0f, x, y, z, light);

		if (PATTERNS[s.pattern] != null) {
			layer(pose, collector, PATTERNS[s.pattern], PanelCanvas.mix(s.clothColor, 0xFFFFFFFF, 0.3f), 0.0007f, x, y, z, light);
		}

		layer(pose, collector, TRIM, s.trimColor != 0 ? s.trimColor : PanelCanvas.mix(s.clothColor, 0xFF000000, 0.45f), 0.0014f, x, y, z, light);
	}

	/** Eine eingefärbte Stoff-Ebene über das gewellte Netz (leicht vor der vorherigen). */
	private static void layer(PoseStack pose, SubmitNodeCollector collector, Identifier texture, int color, float lift, float[][] x, float[][] y,
			float[][] z, int light) {
		collector.submitCustomGeometry(pose, RenderTypes.entityCutout(texture), (p, vc) -> {
			for (int r = 0; r < ROWS; r++) {
				for (int k = 0; k < COLS; k++) {
					int[][] corners = {{r, k}, {r, k + 1}, {r + 1, k + 1}, {r + 1, k}};
					// Normale grob aus der Neigung – sorgt für Licht und Schatten in den Falten
					float dz = (z[r + 1][k] + z[r + 1][k + 1] - z[r][k] - z[r][k + 1]) * 0.5f;
					float dy = y[r][k] - y[r + 1][k];
					float ny = dz / Math.max(1.0e-4f, (float) Math.sqrt(dz * dz + dy * dy));
					float nz = (float) Math.sqrt(Math.max(0.0f, 1.0f - ny * ny));

					for (int[] c : corners) {
						vc.addVertex(p, x[c[0]][c[1]], y[c[0]][c[1]], z[c[0]][c[1]] + lift).setColor(color)
								.setUv(c[1] / (float) COLS, c[0] / (float) ROWS).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light)
								.setNormal(p, 0.0f, ny, nz);
					}
				}
			}
		});
	}

	// ------------------------------------------------------------------ Laufschrift

	/** Zeichnet mit einer Leinwand, die an der Stoffstelle {@code py} anliegt (folgt Welle und Neigung). */
	private interface Painter {
		void paint(PanelCanvas c);
	}

	private void at(State s, PoseStack pose, SubmitNodeCollector collector, float py, Painter painter) {
		float v = Math.max(0.0f, Math.min(1.0f, (py - CY0) / (CY1 - CY0)));
		float zc = wave(s, 0.5f, v);
		float dv = 0.02f;
		float slope = (wave(s, 0.5f, Math.min(1.0f, v + dv)) - wave(s, 0.5f, Math.max(0.0f, v - dv)))
				/ (2 * dv * (CY1 - CY0) / PanelCanvas.PPB);
		pose.pushPose();
		pose.translate(sway(s, v) / PanelCanvas.PPB, -py / PanelCanvas.PPB, zc + 0.003f);
		pose.rotate(Axis.XP, (float) -Math.atan(slope));
		pose.translate(0.0f, py / PanelCanvas.PPB, 0.0f);
		PanelCanvas c = new PanelCanvas(pose, collector, font);
		painter.paint(c);
		c.flush();
		pose.popPose();
	}

	private void content(State s, PoseStack pose, SubmitNodeCollector collector) {
		int glow = PanelCanvas.FULL_BRIGHT;
		float mid = (CX0 + CX1) / 2;
		float width = CX1 - CX0 - 14;
		String title = (s.network.isEmpty() ? Component.translatable("jtechstorage.banner.title").getString() : s.network)
				.toUpperCase(PanelCanvas.locale());
		at(s, pose, collector, 22, c -> {
			float ts = c.fit(title, 1.0f, width);
			c.text(title, mid, 26 - 4 * ts, ts, 0.5f, s.trimColor != 0 ? s.trimColor : s.textColor, 2, glow);
		});

		if (!s.linked || !s.online) {
			String key = s.linked ? "jtechstorage.display.screen.offline" : "jtechstorage.display.screen.unlinked";
			message(s, pose, collector, Component.translatable(key), Component.translatable("jtechstorage.display.screen.link_hint"));
			return;
		}

		int n = s.counts.length;

		if (n == 0) {
			message(s, pose, collector, Component.translatable("jtechstorage.banner.empty"), Component.translatable("jtechstorage.banner.empty_hint"));
			return;
		}

		float area = LIST_BOTTOM - LIST_TOP;
		int visible = (int) (area / ROW_H);

		if (n <= visible) {
			float start = LIST_TOP + (area - n * ROW_H) / 2;

			for (int i = 0; i < n; i++) {
				row(s, pose, collector, i, start + i * ROW_H, 1.0f);
			}

			return;
		}

		// rollende Liste: zweimal hintereinander, an den Rändern ausgeblendet
		float loop = n * ROW_H;
		float offset = (s.time * 0.32f) % loop;

		for (int i = 0; i < n * 2; i++) {
			float ry = LIST_TOP + i * ROW_H - offset;

			if (ry + ROW_H < LIST_TOP || ry > LIST_BOTTOM) {
				continue;
			}

			float fade = 7.0f;
			float a = Math.min(1.0f, Math.min((ry + ROW_H * 0.5f - LIST_TOP) / fade, (LIST_BOTTOM - ry - ROW_H * 0.5f) / fade));

			if (a > 0.04f) {
				row(s, pose, collector, i % n, ry, a);
			}
		}
	}

	private void row(State s, PoseStack pose, SubmitNodeCollector collector, int i, float ry, float alpha) {
		int glow = PanelCanvas.FULL_BRIGHT;
		long count = s.counts[i];
		int trend = s.trends[i];
		at(s, pose, collector, ry + ROW_H / 2, c -> {
			float cy = ry + ROW_H / 2;
			c.icon(s.icons[i], CX0 + 15, cy, 13.0f * alpha, 3, glow);
			String number = c.number(count, 1.0f, 46);
			float ns = c.fit(number, 1.0f, 46);
			c.text(number, CX0 + 26, cy - 4 * ns, ns, 0, PanelCanvas.alpha(s.textColor, alpha), 3, glow);
			String mark = trend > 0 ? "+" : trend < 0 ? "-" : "=";
			c.text(mark, CX1 - 10, cy - 4, 1.0f, 0.5f, PanelCanvas.alpha(PanelCanvas.trendColor(trend), alpha), 3, glow);
			// feine Trennlinie
			c.rect(CX0 + 9, ry + ROW_H - 0.4f, CX1 - 9, ry + ROW_H, 2, PanelCanvas.alpha((0x30 << 24) | ((s.trimColor != 0 ? s.trimColor : s.textColor) & 0xFFFFFF), alpha), glow);
		});
	}

	private void message(State s, PoseStack pose, SubmitNodeCollector collector, Component title, Component hint) {
		int glow = PanelCanvas.FULL_BRIGHT;
		float mid = (CX0 + CX1) / 2;
		float width = CX1 - CX0 - 16;
		at(s, pose, collector, (LIST_TOP + LIST_BOTTOM) / 2, c -> {
			float y = LIST_TOP + 22;
			float ts = c.fit(title.getString(), 1.0f, width);
			c.text(title.getString(), mid, y, ts, 0.5f, s.textColor, 3, glow);
			y += 14;

			for (FormattedCharSequence line : c.font.split(hint, (int) (width / 0.75f))) {
				if (y > LIST_BOTTOM - 8) {
					break;
				}

				c.text(line, c.font.width(line), mid, y, 0.75f, 0.5f, SUBTLE, 3, glow);
				y += 8;
			}
		});
	}
}
