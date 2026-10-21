package dev.bartels.jtechstorage.client.gui;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.util.Util;

import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;

/**
 * Oberflächen-Effekte (v0.29), jeder einzeln schaltbar: Hereingleiten beim Öffnen, Rand-Schimmer, Glanzlicht,
 * Hintergrund-Partikel und Zeitbasis für die Zahlen-Animation. Gilt für alle Fenster der Mod.
 */
public final class GuiFx {
	private static final int SLIDE_MS = 280;
	private static final int SLIDE_PX = 10;
	private static final int PARTICLES = 40;

	private static final Map<Screen, Long> OPENED = Collections.synchronizedMap(new WeakHashMap<>());
	private static final Set<Screen> HOOKED = Collections.newSetFromMap(Collections.synchronizedMap(new WeakHashMap<>()));
	private static final Set<Screen> PUSHED = Collections.newSetFromMap(Collections.synchronizedMap(new WeakHashMap<>()));

	private static GuiGraphicsExtractor lastParticleGraphics;
	private static long lastParticleFrame;

	private GuiFx() {
	}

	/** Einmal beim Client-Start: merkt sich, wann ein Fenster der Mod geöffnet wurde, und lässt es hereingleiten. */
	public static void register() {
		ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
			if (!ours(screen)) {
				return;
			}

			OPENED.putIfAbsent(screen, Util.getMillis());

			if (!HOOKED.add(screen)) {
				return; // nach Größenänderung nicht doppelt anmelden
			}

			ScreenEvents.beforeExtract(screen).register((s, graphics, mouseX, mouseY, delta) -> {
				float offset = slideOffset(s);
				currentSlide = offset;

				if (offset > 0) {
					graphics.pose().pushMatrix();
					graphics.pose().translate(0, offset);
					PUSHED.add(s);
				}
			});
			ScreenEvents.afterExtract(screen).register((s, graphics, mouseX, mouseY, delta) -> {
				if (PUSHED.remove(s)) {
					graphics.pose().popMatrix();
				}
			});
		});
	}

	private static boolean ours(Screen screen) {
		return screen.getClass().getName().startsWith("dev.bartels.jtechstorage.");
	}

	/** Millisekunden seit dem Öffnen (groß, wenn unbekannt). */
	public static long age(Screen screen) {
		Long opened = OPENED.get(screen);
		return opened == null ? Long.MAX_VALUE / 2 : Util.getMillis() - opened;
	}

	/** Wie weit das Fenster gerade noch nach unten versetzt ist (0 = an seinem Platz). */
	public static float slideOffset(Screen screen) {
		if (!GuiSettings.get().fadeIn) {
			return 0;
		}

		long age = age(screen);

		if (age >= SLIDE_MS) {
			return 0;
		}

		float t = age / (float) SLIDE_MS;
		float eased = 1 - (1 - t) * (1 - t) * (1 - t);
		return (1 - eased) * SLIDE_PX;
	}

	/** Fortschritt 0..1 der Zahlen-Animation für ein Element mit {@code delayMs} Verzögerung (1 = fertig / aus). */
	public static float countProgress(Screen screen, int delayMs) {
		if (!GuiSettings.get().countAnimation) {
			return 1;
		}

		long age = age(screen) - delayMs;

		if (age < 0) {
			return 0;
		}

		float t = Math.min(1, age / 600f);
		return 1 - (1 - t) * (1 - t) * (1 - t);
	}

	/** Hintergrund (Abdunklung) gegen das Hereingleiten verschieben, damit er stehen bleibt. */
	public static void beginBackground(Screen screen, GuiGraphicsExtractor graphics) {
		graphics.pose().pushMatrix();
		graphics.pose().translate(0, -slideOffset(screen));
	}

	public static void endBackground(GuiGraphicsExtractor graphics) {
		graphics.pose().popMatrix();
	}

	// ------------------------------------------------------------------
	// Zeichen-Effekte
	// ------------------------------------------------------------------

	/** Kristallfunken, die langsam über den ganzen Bildschirm aufsteigen (einmal pro Bild). */
	public static void particles(GuiGraphicsExtractor graphics) {
		if (!GuiSettings.get().particles) {
			return;
		}

		long now = System.nanoTime();

		if (graphics == lastParticleGraphics && now - lastParticleFrame < 4_000_000L) {
			return;
		}

		lastParticleGraphics = graphics;
		lastParticleFrame = now;

		int width = graphics.guiWidth();
		int height = graphics.guiHeight();
		long ms = Util.getMillis();
		int color = GuiTheme.mix(GuiTheme.accent(), 0xFFFFFFFF, 0.45f);
		// gegen das Hereingleiten ausgleichen – die Funken gehören zum Hintergrund
		graphics.pose().pushMatrix();
		graphics.pose().translate(0, -currentSlide);

		for (int i = 0; i < PARTICLES; i++) {
			float rx = hash(i * 3 + 1);
			float ry = hash(i * 3 + 2);
			float rs = hash(i * 3 + 3);
			float speed = 4 + rs * 9;
			int size = rs < 0.3f ? 2 : 1;
			float travel = height + 10;
			float y = height - ((ms / 1000f * speed + ry * travel) % travel);
			float x = rx * width + (float) Math.sin(ms / 900.0 + i) * 3;
			int alpha = (int) (90 + 90 * Math.sin(ms / 400.0 + i * 1.7));
			graphics.fill((int) x, (int) y, (int) x + size, (int) y + size, GuiTheme.alpha(color, alpha));
		}

		graphics.pose().popMatrix();
	}

	/** Aktueller Versatz des Fensters (für {@link #particles}). */
	private static float currentSlide;

	/** Heller Lichtpunkt mit Schweif, der langsam um den Fensterrand wandert. */
	public static void shimmer(GuiGraphicsExtractor graphics, int x, int y, int w, int h) {
		if (!GuiSettings.get().borderShimmer) {
			return;
		}

		int perimeter = 2 * (w + h);
		long pos = (Util.getMillis() / 9) % perimeter;
		int color = GuiTheme.mix(GuiTheme.accent(), 0xFFFFFFFF, 0.6f);

		for (int k = 0; k < 26; k++) {
			int p = (int) Math.floorMod(pos - k, (long) perimeter);
			int px;
			int py;

			if (p < w) {
				px = p;
				py = 0;
			} else if (p < w + h) {
				px = w - 1;
				py = p - w;
			} else if (p < 2 * w + h) {
				px = w - 1 - (p - w - h);
				py = h - 1;
			} else {
				px = 0;
				py = h - 1 - (p - 2 * w - h);
			}

			int alpha = (int) ((1 - k / 26f) * 240);
			graphics.fill(x + px, y + py, x + px + 1, y + py + 1, GuiTheme.alpha(color, alpha));
		}
	}

	/** Schräger Lichtstreifen, der alle paar Sekunden über das Fenster zieht. */
	public static void sheen(GuiGraphicsExtractor graphics, int x, int y, int w, int h) {
		if (!GuiSettings.get().sheen) {
			return;
		}

		float phase = (Util.getMillis() % 5200) / 5200f;

		if (phase >= 0.45f) {
			return;
		}

		float start = -40 + (w + 80) * (phase / 0.45f);
		int left = x + 2;
		int right = x + w - 2;

		for (int row = 2; row < h - 2; row++) {
			int x0 = x + Math.round(start + row * 0.35f);
			band(graphics, x0, x0 + 14, y + row, left, right, 0x1AFFFFFF);
			band(graphics, x0 + 14, x0 + 20, y + row, left, right, 0x0FFFFFFF);
		}
	}

	private static void band(GuiGraphicsExtractor graphics, int from, int to, int y, int left, int right, int color) {
		int a = Math.max(from, left);
		int b = Math.min(to, right);

		if (a < b) {
			graphics.fill(a, y, b, y + 1, color);
		}
	}

	private static float hash(int n) {
		int h = n * 0x27D4EB2D;
		h ^= h >>> 15;
		h *= 0x165667B1;
		h ^= h >>> 13;
		return (h & 0xFFFF) / 65536f;
	}
}
