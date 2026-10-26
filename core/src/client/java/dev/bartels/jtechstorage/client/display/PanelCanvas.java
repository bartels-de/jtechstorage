package dev.bartels.jtechstorage.client.display;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.phys.AABB;

import dev.bartels.jtechstorage.JTechStorage;

/**
 * Drawing surface for network displays (v0.34). When created, the PoseStack sits at the top left of the surface (block units,
 * x right, y up, z toward the viewer). All coordinates here are "pixels" ({@link #PPB} per block, y down),
 * layers ({@code layer}) each sit a hair in front of the previous one.
 */
final class PanelCanvas {
	static final Identifier WHITE = JTechStorage.id("textures/entity/display/white.png");
	static final int FULL_BRIGHT = 0xF000F0;
	/** Pixels per block. */
	static final float PPB = 96.0f;
	private static final float LAYER = 0.0016f;

	private record Quad(float[] xyz, int[] colors, int light) {
	}

	final PoseStack pose;
	final SubmitNodeCollector collector;
	final Font font;
	private final List<Quad> solid = new ArrayList<>();
	private final List<Quad> translucent = new ArrayList<>();

	PanelCanvas(PoseStack pose, SubmitNodeCollector collector, Font font) {
		this.pose = pose;
		this.collector = collector;
		this.font = font;
	}

	private static float bx(float px) {
		return px / PPB;
	}

	private static float by(float py) {
		return -py / PPB;
	}

	private static float bz(float layer) {
		return layer * LAYER;
	}

	// ------------------------------------------------------------------ Surfaces

	/** Rectangle with a top-to-bottom gradient. */
	void rect(float x0, float y0, float x1, float y1, float layer, int top, int bottom, int light) {
		quad(x0, y0, x1, y0, x1, y1, x0, y1, layer, top, top, bottom, bottom, light);
	}

	void rect(float x0, float y0, float x1, float y1, float layer, int color, int light) {
		rect(x0, y0, x1, y1, layer, color, color, light);
	}

	/** Rectangle with a left-to-right gradient. */
	void hrect(float x0, float y0, float x1, float y1, float layer, int left, int right, int light) {
		quad(x0, y0, x1, y0, x1, y1, x0, y1, layer, left, right, right, left, light);
	}

	/** Border (lines inside the edge of the rectangle). */
	void frame(float x0, float y0, float x1, float y1, float t, float layer, int color, int light) {
		rect(x0, y0, x1, y0 + t, layer, color, light);
		rect(x0, y1 - t, x1, y1, layer, color, light);
		rect(x0, y0 + t, x0 + t, y1 - t, layer, color, light);
		rect(x1 - t, y0 + t, x1, y1 - t, layer, color, light);
	}

	/** Arbitrary quad (clockwise, starting at top left); semi-transparent if a color has alpha < 255. */
	void quad(float ax, float ay, float bx, float by, float cx, float cy, float dx, float dy, float layer, int ca, int cb, int cc, int cd, int light) {
		float z = bz(layer);
		Quad q = new Quad(new float[] {bx(ax), by(ay), z, bx(bx), by(by), z, bx(cx), by(cy), z, bx(dx), by(dy), z}, new int[] {ca, cb, cc, cd}, light);
		boolean opaque = (ca >>> 24) == 255 && (cb >>> 24) == 255 && (cc >>> 24) == 255 && (cd >>> 24) == 255;
		(opaque ? solid : translucent).add(q);
	}

	/** Line of thickness {@code t} from (x0,y0) to (x1,y1). */
	void line(float x0, float y0, float x1, float y1, float t, float layer, int color, int light) {
		float dx = x1 - x0;
		float dy = y1 - y0;
		float len = (float) Math.sqrt(dx * dx + dy * dy);

		if (len < 1.0e-4f) {
			return;
		}

		float nx = -dy / len * t / 2.0f;
		float ny = dx / len * t / 2.0f;
		quad(x0 + nx, y0 + ny, x1 + nx, y1 + ny, x1 - nx, y1 - ny, x0 - nx, y0 - ny, layer, color, color, color, color, light);
	}

	/** Submit the collected surfaces (once at the end). */
	void flush() {
		submit(solid, RenderTypes.entityCutout(WHITE));
		submit(translucent, RenderTypes.entityTranslucent(WHITE));
		solid.clear();
		translucent.clear();
	}

	private void submit(List<Quad> quads, RenderType type) {
		if (quads.isEmpty()) {
			return;
		}

		List<Quad> copy = new ArrayList<>(quads);
		collector.submitCustomGeometry(pose, type, (p, vc) -> {
			for (Quad q : copy) {
				for (int i = 0; i < 4; i++) {
					vc.addVertex(p, q.xyz[i * 3], q.xyz[i * 3 + 1], q.xyz[i * 3 + 2]).setColor(q.colors[i]).setUv(i == 1 || i == 2 ? 1 : 0, i >= 2 ? 1 : 0)
							.setOverlay(OverlayTexture.NO_OVERLAY).setLight(q.light).setNormal(p, 0.0f, 0.0f, 1.0f);
				}
			}
		});
	}

	// ------------------------------------------------------------------ Text

	int width(String text) {
		return font.width(text);
	}

	/** Largest scale ≤ {@code wanted} at which the text fits into {@code maxWidth} pixels. */
	float fit(String text, float wanted, float maxWidth) {
		int w = font.width(text);
		return w <= 0 ? wanted : Math.min(wanted, maxWidth / w);
	}

	/** Text with alignment {@code align}: 0 = left, 0.5 = center, 1 = right (relative to x). */
	void text(String text, float x, float y, float scale, float align, int color, float layer, int light) {
		text(FormattedCharSequence.forward(text, Style.EMPTY), font.width(text), x, y, scale, align, color, layer, light);
	}

	void text(FormattedCharSequence text, int width, float x, float y, float scale, float align, int color, float layer, int light) {
		if ((color >>> 24) < 8 || scale <= 0.0f) {
			return;
		}

		pose.pushPose();
		pose.translate(bx(x - width * scale * align), by(y), bz(layer));
		pose.scale(scale / PPB, -scale / PPB, scale / PPB);
		collector.submitText(pose, 0.0f, 0.0f, text, false, Font.DisplayMode.POLYGON_OFFSET, light, color, 0, 0);
		pose.popPose();
	}

	// ------------------------------------------------------------------ Items

	/** Flat icon (as in the inventory), centered at (cx,cy), edge length {@code size} pixels. */
	void icon(ItemStackRenderState item, float cx, float cy, float size, float layer, int light) {
		if (item.isEmpty() || size <= 0.0f) {
			return;
		}

		pose.pushPose();
		pose.translate(bx(cx), by(cy), bz(layer) + 0.004f);
		float s = size / PPB;
		pose.scale(s, s, s * 0.04f);
		item.submit(pose, collector, light, OverlayTexture.NO_OVERLAY, 0);
		pose.popPose();
	}

	/** Rotating 3D item (rendered as in an item frame), scaled to {@code size} pixels. */
	void spinning(ItemStackRenderState item, float cx, float cy, float size, float degrees, float layer, int light) {
		if (item.isEmpty() || size <= 0.0f) {
			return;
		}

		float extent = 0.5f;

		try {
			AABB box = item.getModelBoundingBox();
			extent = (float) Math.max(box.getXsize(), Math.max(box.getYsize(), box.getZsize()));
		} catch (RuntimeException ignored) {
			// default size
		}

		if (!(extent > 0.05f && extent < 4.0f)) {
			extent = 0.5f;
		}

		pose.pushPose();
		pose.translate(bx(cx), by(cy), bz(layer) + size / PPB * 0.5f);
		float s = size / PPB / extent;
		pose.scale(s, s, s);
		pose.rotateDegrees(Axis.YP, degrees);
		item.submit(pose, collector, light, OverlayTexture.NO_OVERLAY, 0);
		pose.popPose();
	}

	// ------------------------------------------------------------------ Numbers

	static Locale locale() {
		String code = Minecraft.getInstance().options.languageCode;
		return code != null && code.startsWith("de") ? Locale.GERMANY : Locale.US;
	}

	/** 12.403 or 12,403 – with thousands separators. */
	static String full(long value) {
		return NumberFormat.getIntegerInstance(locale()).format(value);
	}

	/** 12,4k · 3,1M – short form for narrow surfaces. */
	static String compact(long value) {
		long abs = Math.abs(value);

		if (abs < 10_000) {
			return full(value);
		}

		String[] units = {"k", "M", "G", "T"};
		double v = value;
		int unit = -1;

		while (Math.abs(v) >= 1000 && unit < units.length - 1) {
			v /= 1000.0;
			unit++;
		}

		String number = Math.abs(v) >= 100 ? String.format(locale(), "%.0f", v) : String.format(locale(), "%.1f", v);
		return number + units[unit];
	}

	/** Longest representation that fits into {@code maxWidth} pixels at scale {@code scale}. */
	String number(long value, float scale, float maxWidth) {
		String f = full(value);
		return font.width(f) * scale <= maxWidth ? f : compact(value);
	}

	static String trend(int perMinute) {
		if (perMinute == 0) {
			return "±0/min";
		}

		return (perMinute > 0 ? "+" : "-") + compact(Math.abs((long) perMinute)) + "/min";
	}

	static int trendColor(int perMinute) {
		return perMinute > 0 ? 0xFF7CFFA0 : perMinute < 0 ? 0xFFFF7A7A : 0xFF9A8FB8;
	}

	static int alpha(int color, float a) {
		int alpha = Math.round(Math.max(0.0f, Math.min(1.0f, a)) * (color >>> 24));
		return (alpha << 24) | (color & 0xFFFFFF);
	}

	static int mix(int a, int b, float t) {
		t = Math.max(0.0f, Math.min(1.0f, t));
		int r = 0;

		for (int shift = 0; shift <= 24; shift += 8) {
			int ca = (a >>> shift) & 0xFF;
			int cb = (b >>> shift) & 0xFF;
			r |= Math.round(ca + (cb - ca) * t) << shift;
		}

		return r;
	}
}
