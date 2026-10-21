package dev.bartels.jtechstorage.client.gui;

import org.jspecify.annotations.Nullable;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import dev.bartels.jtechstorage.JTechStorage;

/**
 * Farben und Zeichen-Bausteine aller Mod-Oberflächen. Seit v0.29 drei Stile: klassisch (Vanilla-Look mit Kanten),
 * Glas (durchscheinend mit Leuchtrand) und modern (flach, Pixel-Rundungen) – jeweils hell und dunkel.
 */
public final class GuiTheme {
	/** Zuletzt bekannte Team-Farbe (für den Akzent „Team“). */
	private static int teamColor = 0xFF8D6ACC;

	private static final Identifier TOOLTIP_GLASS = JTechStorage.id("glass");
	private static final Identifier TOOLTIP_MODERN = JTechStorage.id("modern");

	private GuiTheme() {
	}

	public static void setTeamColor(int rgb) {
		teamColor = 0xFF000000 | rgb;
	}

	public static GuiSettings.Theme theme() {
		return GuiSettings.get().theme;
	}

	public static GuiSettings.Style style() {
		return theme().style;
	}

	public static boolean dark() {
		return theme().dark;
	}

	public static boolean classic() {
		return style() == GuiSettings.Style.CLASSIC;
	}

	// ------------------------------------------------------------------
	// Farben
	// ------------------------------------------------------------------

	/** Fensterfläche (bei Glas: deckende Näherung, z. B. für kleine Flächen auf dem Fenster). */
	public static int face() {
		return switch (theme()) {
			case LIGHT -> 0xFFC6C6C6;
			case DARK -> 0xFF2B2B31;
			case GLASS -> mix(accent(), 0xFF15111F, 0.82f);
			case FROST -> mix(accent(), 0xFFF3F0F8, 0.86f);
			case MODERN_LIGHT -> 0xFFF4F4F7;
			case MODERN_DARK -> 0xFF1D1E23;
		};
	}

	public static int highlight() {
		return switch (theme()) {
			case LIGHT, FROST, MODERN_LIGHT -> 0xFFFFFFFF;
			case DARK -> 0xFF4A4A54;
			case GLASS -> mix(face(), 0xFFFFFFFF, 0.18f);
			case MODERN_DARK -> 0xFF2A2C33;
		};
	}

	/** Schattenkante bzw. Rahmenlinie (modern). */
	public static int shadow() {
		return switch (theme()) {
			case LIGHT -> 0xFF555555;
			case DARK -> 0xFF131317;
			case GLASS -> 0xFF08070C;
			case FROST -> mix(face(), 0xFF000000, 0.3f);
			case MODERN_LIGHT -> 0xFFD8D8E0;
			case MODERN_DARK -> 0xFF30323B;
		};
	}

	/** Leicht abgesetzte Fläche (Zeilen-Streifen, Listen). */
	public static int inset() {
		return switch (theme()) {
			case LIGHT -> 0xFFBDBDBD;
			case DARK -> 0xFF232328;
			case GLASS -> mix(face(), 0xFF000000, 0.2f);
			case FROST -> mix(face(), 0xFF000000, 0.05f);
			case MODERN_LIGHT -> 0xFFECECF1;
			case MODERN_DARK -> 0xFF23242A;
		};
	}

	/** Innenfarbe vertiefter Flächen (Slots, Listen) – auch Grundlage für {@link #onColor} darauf. */
	public static int slot() {
		return switch (theme()) {
			case LIGHT -> 0xFF8B8B8B;
			case DARK -> 0xFF1B1B20;
			case GLASS -> mix(face(), 0xFF000000, 0.38f);
			case FROST -> mix(face(), 0xFFFFFFFF, 0.5f);
			case MODERN_LIGHT -> 0xFFE5E5EB;
			case MODERN_DARK -> 0xFF15161A;
		};
	}

	public static int slotDark() {
		return switch (theme()) {
			case LIGHT -> 0xFF373737;
			case DARK -> 0xFF0E0E11;
			case GLASS -> mix(face(), 0xFF000000, 0.55f);
			case FROST -> mix(face(), 0xFF000000, 0.18f);
			case MODERN_LIGHT -> 0xFFD2D2DA;
			case MODERN_DARK -> 0xFF2A2C34;
		};
	}

	public static int slotLight() {
		return switch (theme()) {
			case LIGHT, FROST -> 0xFFFFFFFF;
			case DARK -> 0xFF45454F;
			case GLASS -> mix(face(), 0xFFFFFFFF, 0.12f);
			case MODERN_LIGHT -> 0xFFF9F9FB;
			case MODERN_DARK -> 0xFF202127;
		};
	}

	public static int text() {
		return switch (theme()) {
			case LIGHT -> 0xFF2E2E2E;
			case DARK -> 0xFFEDEDF4;
			case GLASS -> 0xFFF3ECFF;
			case FROST -> 0xFF2A2238;
			case MODERN_LIGHT -> 0xFF1E1E24;
			case MODERN_DARK -> 0xFFECECF1;
		};
	}

	/** Zweitrangiger Text – trotzdem mit genug Kontrast (≥ 4,5 : 1 auf Fenster und Streifen). */
	public static int subtle() {
		return switch (theme()) {
			case LIGHT -> 0xFF4A4A4A;
			case DARK -> 0xFFB8B8C6;
			case GLASS -> 0xFFC4B8DC;
			case FROST -> 0xFF54496A;
			case MODERN_LIGHT -> 0xFF55556A;
			case MODERN_DARK -> 0xFFA2A3B0;
		};
	}

	public static int good() {
		return dark() ? 0xFF7CEB96 : 0xFF155219;
	}

	public static int bad() {
		return dark() ? 0xFFFF8F72 : 0xFF8A240A;
	}

	public static int warn() {
		return dark() ? 0xFFFFC56B : 0xFF6A4000;
	}

	/** Platzhalter-Zeichen („+“) in leeren Slots. */
	public static int slotHint() {
		return onColor(slot()) == 0xFFFFFFFF ? 0xFF8C8C9C : 0xFF1E1E1E;
	}

	/** Akzentfarbe als Textfarbe (abgedunkelt bzw. aufgehellt, damit sie auf dem Fenster lesbar bleibt). */
	public static int accentText() {
		return dark() ? mix(accent(), 0xFFFFFFFF, 0.45f) : mix(accent(), 0xFF000000, 0.5f);
	}

	/** Schrift im Suchfeld. */
	public static int fieldText() {
		return classic() ? 0xFFFFFFFF : text();
	}

	/** Eigener Tooltip-Rahmen passend zum Stil (null = Vanilla). */
	public static @Nullable Identifier tooltipStyle() {
		return switch (style()) {
			case CLASSIC -> null;
			case GLASS -> TOOLTIP_GLASS;
			case MODERN -> TOOLTIP_MODERN;
		};
	}

	/** Helle neue Designs: Mengen in Slots bekommen eine dunkle Kontur, damit die weiße Zahl lesbar bleibt. */
	public static boolean outlineCounts() {
		return !classic() && !dark();
	}

	/** Relative Helligkeit nach WCAG (0..1). */
	public static double luminance(int argb) {
		return 0.2126 * channel((argb >> 16) & 255) + 0.7152 * channel((argb >> 8) & 255) + 0.0722 * channel(argb & 255);
	}

	private static double channel(int value) {
		double c = value / 255.0;
		return c <= 0.03928 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4);
	}

	/** Kontrastverhältnis zweier Farben (1..21). */
	public static double contrast(int a, int b) {
		double la = luminance(a);
		double lb = luminance(b);
		return (Math.max(la, lb) + 0.05) / (Math.min(la, lb) + 0.05);
	}

	/** Weiß oder Fast-Schwarz – je nachdem, was auf {@code background} besser lesbar ist. */
	public static int onColor(int background) {
		return contrast(0xFFFFFFFF, background) >= contrast(0xFF1A1A1A, background) ? 0xFFFFFFFF : 0xFF1A1A1A;
	}

	// Akzent
	public static int accent() {
		GuiSettings.Accent accent = GuiSettings.get().accent;
		return accent == GuiSettings.Accent.TEAM ? teamColor : accent.color;
	}

	/** Farbe einer bestimmten Akzent-Auswahl (Team = zuletzt bekannte Team-Farbe). */
	public static int accentOf(GuiSettings.Accent accent) {
		return accent == GuiSettings.Accent.TEAM ? teamColor : accent.color;
	}

	public static int accentHover() {
		return mix(accent(), 0xFFFFFFFF, 0.3f);
	}

	public static int accentDark() {
		return mix(accent(), 0xFF000000, 0.65f);
	}

	public static int accentDarkHover() {
		return mix(accent(), 0xFF000000, 0.45f);
	}

	public static int mix(int a, int b, float t) {
		int r = (int) (((a >> 16) & 255) + (((b >> 16) & 255) - ((a >> 16) & 255)) * t);
		int g = (int) (((a >> 8) & 255) + (((b >> 8) & 255) - ((a >> 8) & 255)) * t);
		int bl = (int) ((a & 255) + ((b & 255) - (a & 255)) * t);
		return 0xFF000000 | r << 16 | g << 8 | bl;
	}

	/** Farbe mit neuer Deckkraft (0..255). */
	public static int alpha(int rgb, int alpha) {
		return (alpha & 255) << 24 | (rgb & 0xFFFFFF);
	}

	private static int lerpArgb(int a, int b, float t) {
		int alpha = (int) (((a >>> 24) & 255) + (((b >>> 24) & 255) - ((a >>> 24) & 255)) * t);
		return alpha(mix(a, b, t), alpha);
	}

	// ------------------------------------------------------------------
	// Grundformen mit Pixel-Rundung (r = 0..3)
	// ------------------------------------------------------------------

	private static int inset(int r, int row) {
		return switch (r) {
			case 1 -> row == 0 ? 1 : 0;
			case 2 -> row == 0 ? 2 : row == 1 ? 1 : 0;
			case 3 -> row == 0 ? 3 : row <= 2 ? 1 : 0;
			default -> 0;
		};
	}

	private static int cornerRows(int r) {
		return r <= 0 ? 0 : r == 1 ? 1 : r == 2 ? 2 : 3;
	}

	/** Gefülltes Rechteck mit abgeschnittenen Ecken. */
	public static void round(GuiGraphicsExtractor graphics, int x, int y, int w, int h, int r, int color) {
		int rows = Math.min(cornerRows(r), h / 2);

		for (int i = 0; i < rows; i++) {
			int s = inset(r, i);
			graphics.fill(x + s, y + i, x + w - s, y + i + 1, color);
			graphics.fill(x + s, y + h - 1 - i, x + w - s, y + h - i, color);
		}

		graphics.fill(x, y + rows, x + w, y + h - rows, color);
	}

	/** Wie {@link #round}, aber mit senkrechtem Farbverlauf. */
	public static void roundGradient(GuiGraphicsExtractor graphics, int x, int y, int w, int h, int r, int top, int bottom) {
		int rows = Math.min(cornerRows(r), h / 2);

		for (int i = 0; i < rows; i++) {
			int s = inset(r, i);
			graphics.fill(x + s, y + i, x + w - s, y + i + 1, lerpArgb(top, bottom, i / (float) Math.max(1, h - 1)));
			graphics.fill(x + s, y + h - 1 - i, x + w - s, y + h - i, lerpArgb(top, bottom, (h - 1 - i) / (float) Math.max(1, h - 1)));
		}

		graphics.fillGradient(x, y + rows, x + w, y + h - rows, lerpArgb(top, bottom, rows / (float) Math.max(1, h - 1)),
				lerpArgb(top, bottom, (h - 1 - rows) / (float) Math.max(1, h - 1)));
	}

	/** 1-Pixel-Rahmen mit abgeschnittenen Ecken. */
	public static void roundOutline(GuiGraphicsExtractor graphics, int x, int y, int w, int h, int r, int color) {
		if (r <= 0) {
			graphics.fill(x, y, x + w, y + 1, color);
			graphics.fill(x, y + h - 1, x + w, y + h, color);
			graphics.fill(x, y + 1, x + 1, y + h - 1, color);
			graphics.fill(x + w - 1, y + 1, x + w, y + h - 1, color);
			return;
		}

		int c = r == 1 ? 1 : 2;
		graphics.fill(x + c, y, x + w - c, y + 1, color);
		graphics.fill(x + c, y + h - 1, x + w - c, y + h, color);
		graphics.fill(x, y + c, x + 1, y + h - c, color);
		graphics.fill(x + w - 1, y + c, x + w, y + h - c, color);

		if (c == 2) {
			graphics.fill(x + 1, y + 1, x + 2, y + 2, color);
			graphics.fill(x + w - 2, y + 1, x + w - 1, y + 2, color);
			graphics.fill(x + 1, y + h - 2, x + 2, y + h - 1, color);
			graphics.fill(x + w - 2, y + h - 2, x + w - 1, y + h - 1, color);
		}
	}

	/** Weicher Schatten um ein Fenster (nur Glas und modern). */
	private static void dropShadow(GuiGraphicsExtractor graphics, int x, int y, int w, int h) {
		int[][] rings = {{4, 0x0F}, {3, 0x14}, {2, 0x1F}, {1, 0x2E}};

		for (int[] ring : rings) {
			int d = ring[0];
			round(graphics, x - d, y - d + 2, w + 2 * d, h + 2 * d, 3, ring[1] << 24);
		}
	}

	// ------------------------------------------------------------------
	// Bausteine
	// ------------------------------------------------------------------

	/** Hauptfenster: Fläche plus die eingeschalteten Effekte (Partikel dahinter, Rand-Schimmer, Glanzlicht). */
	public static void window(GuiGraphicsExtractor graphics, int x, int y, int w, int h) {
		GuiFx.particles(graphics);
		panel(graphics, x, y, w, h);
		GuiFx.shimmer(graphics, x, y, w, h);
		GuiFx.sheen(graphics, x, y, w, h);
	}

	/** Fensterfläche ohne Effekte (auch für Nebenfenster und Vorschau). */
	public static void panel(GuiGraphicsExtractor graphics, int x, int y, int w, int h) {
		switch (style()) {
			case CLASSIC -> {
				graphics.fill(x, y, x + w, y + h, 0xFF000000);
				graphics.fill(x + 1, y + 1, x + w - 1, y + h - 1, highlight());
				graphics.fill(x + 2, y + 2, x + w - 1, y + h - 1, shadow());
				graphics.fill(x + 2, y + 2, x + w - 2, y + h - 2, face());
			}
			case GLASS -> {
				dropShadow(graphics, x, y, w, h);
				int a = accent();

				if (dark()) {
					roundGradient(graphics, x, y, w, h, 3, alpha(mix(a, 0xFF1A1028, 0.72f), 0xEC), 0xF20E0C1A);
				} else {
					roundGradient(graphics, x, y, w, h, 3, 0xEEFAF7FF, alpha(mix(a, 0xFFE8E2F4, 0.82f), 0xF0));
				}

				// Akzent-Schein oben
				int glow = Math.min(21, h - 2);
				graphics.fillGradient(x + 2, y + 1, x + w - 2, y + 1 + glow, alpha(a, dark() ? 0x5C : 0x38), alpha(a, 0));
				roundOutline(graphics, x, y, w, h, 3, dark() ? alpha(mix(a, 0xFFFFFFFF, 0.25f), 0xC0) : alpha(a, 0x8C));
				graphics.fill(x + 3, y + 1, x + w - 3, y + 2, dark() ? 0x38FFFFFF : 0xE6FFFFFF);
			}
			case MODERN -> {
				dropShadow(graphics, x, y, w, h);
				round(graphics, x, y, w, h, 3, shadow());
				round(graphics, x + 1, y + 1, w - 2, h - 2, 2, face());
			}
		}
	}

	/** Slot-Rahmen (18×18, x/y = linke obere Ecke des Rahmens). */
	public static void slot(GuiGraphicsExtractor graphics, int x, int y) {
		sunken(graphics, x, y, 18, 18);
	}

	/** Vertiefte Fläche beliebiger Größe. */
	public static void sunken(GuiGraphicsExtractor graphics, int x, int y, int w, int h) {
		switch (style()) {
			case CLASSIC -> {
				graphics.fill(x, y, x + w, y + h, slotDark());
				graphics.fill(x + 1, y + 1, x + w, y + h, slotLight());
				graphics.fill(x + 1, y + 1, x + w - 1, y + h - 1, slot());
			}
			case GLASS -> {
				round(graphics, x, y, w, h, 1, dark() ? 0x61000000 : 0x8CFFFFFF);
				roundOutline(graphics, x, y, w, h, 1, dark() ? 0x14FFFFFF : alpha(accent(), 0x38));
				graphics.fill(x + 1, y + h - 1, x + w - 1, y + h, dark() ? alpha(accent(), 0x40) : 0xE6FFFFFF);
			}
			case MODERN -> {
				round(graphics, x, y, w, h, 1, slotDark());
				graphics.fill(x + 1, y + 1, x + w - 1, y + h - 1, slot());
			}
		}
	}

	/** Leuchten um einen Slot unter der Maus (x/y = Item-Position, 16×16). */
	public static void slotGlow(GuiGraphicsExtractor graphics, int x, int y) {
		round(graphics, x - 2, y - 2, 20, 20, 2, alpha(accent(), 0x4C));
		roundOutline(graphics, x - 1, y - 1, 18, 18, 1, mix(accent(), 0xFFFFFFFF, 0.3f));
	}

	public static final int DISABLED_FACE = 0xFF4E4E52;
	public static final int DISABLED_TEXT = 0xFFCACACE;

	/** Knopf in Akzentfarbe (ohne Beschriftung). */
	public static void button(GuiGraphicsExtractor graphics, int x, int y, int w, int h, boolean enabled, boolean hover) {
		face(graphics, x, y, w, h, !enabled ? DISABLED_FACE : hover ? accentHover() : accent());
		hoverRing(graphics, x, y, w, h, enabled && hover);
	}

	private static void hoverRing(GuiGraphicsExtractor graphics, int x, int y, int w, int h, boolean hover) {
		if (hover && !classic() && GuiSettings.get().hoverGlow) {
			roundOutline(graphics, x - 1, y - 1, w + 2, h + 2, 2, alpha(accent(), 0x80));
		}
	}

	/** Erhabene Fläche in beliebiger Farbe (Knöpfe, Reiter). */
	public static void face(GuiGraphicsExtractor graphics, int x, int y, int w, int h, int color) {
		switch (style()) {
			case CLASSIC -> {
				graphics.fill(x, y, x + w, y + h, 0xFF000000);
				graphics.fill(x + 1, y + 1, x + w - 1, y + h - 1, mix(color, 0xFF000000, 0.35f));
				graphics.fill(x + 1, y + 1, x + w - 2, y + h - 2, mix(color, 0xFFFFFFFF, 0.25f));
				graphics.fill(x + 2, y + 2, x + w - 2, y + h - 2, color);
			}
			case GLASS -> {
				roundGradient(graphics, x, y, w, h, 2, mix(color, 0xFFFFFFFF, 0.18f), mix(color, 0xFF000000, 0.2f));
				graphics.fill(x + 2, y + 1, x + w - 2, y + 2, 0x70FFFFFF);
			}
			case MODERN -> round(graphics, x, y, w, h, 2, color);
		}
	}

	/**
	 * Symbol-Knopf (z. B. Sortieren im Terminal): neutral oder – wenn {@code active} – in Akzentfarbe.
	 */
	public static void toggle(GuiGraphicsExtractor graphics, int x, int y, int w, int h, boolean active, boolean hover) {
		if (active) {
			face(graphics, x, y, w, h, hover ? accentHover() : accent());
		} else {
			switch (style()) {
				case CLASSIC -> {
					graphics.fill(x, y, x + w, y + h, 0xFF000000);
					graphics.fill(x + 1, y + 1, x + w - 1, y + h - 1, shadow());
					graphics.fill(x + 1, y + 1, x + w - 2, y + h - 2, highlight());
					graphics.fill(x + 2, y + 2, x + w - 2, y + h - 2, hover ? mix(face(), 0xFFFFFFFF, 0.15f) : face());
				}
				case GLASS -> {
					round(graphics, x, y, w, h, 2, dark() ? (hover ? 0x29FFFFFF : 0x14FFFFFF) : (hover ? 0xE6FFFFFF : 0x99FFFFFF));
					roundOutline(graphics, x, y, w, h, 2, alpha(accent(), 0x80));
				}
				case MODERN -> {
					round(graphics, x, y, w, h, 2, shadow());
					round(graphics, x + 1, y + 1, w - 2, h - 2, 1, hover ? mix(face(), accent(), 0.15f) : face());
				}
			}
		}

		hoverRing(graphics, x, y, w, h, hover);
	}

	/** Eingabefeld (Suche). */
	public static void field(GuiGraphicsExtractor graphics, int x, int y, int w, int h, boolean focused) {
		switch (style()) {
			case CLASSIC -> {
				graphics.fill(x, y, x + w, y + h, focused ? 0xFFFFFFFF : 0xFFA0A0A0);
				graphics.fill(x + 1, y + 1, x + w - 1, y + h - 1, 0xFF000000);
			}
			case GLASS -> {
				round(graphics, x, y, w, h, 2, dark() ? 0x59000000 : 0xB3FFFFFF);
				roundOutline(graphics, x, y, w, h, 2, focused ? mix(accent(), 0xFFFFFFFF, 0.3f) : alpha(accent(), 0x8C));
			}
			case MODERN -> {
				round(graphics, x, y, w, h, 2, focused ? accent() : shadow());
				round(graphics, x + 1, y + 1, w - 2, h - 2, 1, dark() ? slot() : 0xFFFFFFFF);
			}
		}
	}

	/** Lupe für Suchfelder (7×7), nur in den neuen Stilen. */
	public static void searchIcon(GuiGraphicsExtractor graphics, int x, int y) {
		int c = subtle();
		graphics.fill(x + 1, y, x + 4, y + 1, c);
		graphics.fill(x, y + 1, x + 1, y + 4, c);
		graphics.fill(x + 4, y + 1, x + 5, y + 4, c);
		graphics.fill(x + 1, y + 4, x + 4, y + 5, c);
		graphics.fill(x + 4, y + 4, x + 5, y + 5, c);
		graphics.fill(x + 5, y + 5, x + 6, y + 6, c);
		graphics.fill(x + 6, y + 6, x + 7, y + 7, c);
	}

	/** Fenstertitel: im Glas-Design mit leichtem Leuchten, modern mit Akzentstrich darunter. */
	public static void title(GuiGraphicsExtractor graphics, Font font, Component title, int x, int y) {
		if (style() == GuiSettings.Style.GLASS && dark()) {
			int glow = alpha(accent(), 0x40);
			graphics.text(font, title, x - 1, y, glow, false);
			graphics.text(font, title, x + 1, y, glow, false);
			graphics.text(font, title, x, y - 1, glow, false);
			graphics.text(font, title, x, y + 1, glow, false);
		}

		graphics.text(font, title, x, y, text(), false);

		if (style() == GuiSettings.Style.MODERN) {
			graphics.fill(x, y + 9, x + 12, y + 10, accent());
		}
	}

	/** Weiße Zahl mit dunkler Kontur (für helle Slots). */
	public static void outlinedNumber(GuiGraphicsExtractor graphics, Font font, String text, int x, int y) {
		int outline = 0xFF1A1424;
		graphics.text(font, text, x - 1, y, outline, false);
		graphics.text(font, text, x + 1, y, outline, false);
		graphics.text(font, text, x, y - 1, outline, false);
		graphics.text(font, text, x, y + 1, outline, false);
		graphics.text(font, text, x, y, 0xFFFFFFFF, false);
	}

	/** Knopf mit Beschriftung: Farbe der Schrift passt sich dem Untergrund an, zu lange Texte werden mit „…“ gekürzt. */
	public static void button(GuiGraphicsExtractor graphics, Font font, int x, int y, int w, int h, Component label,
			boolean enabled, boolean hover) {
		labeled(graphics, font, x, y, w, h, label, !enabled ? DISABLED_FACE : hover ? accentHover() : accent(), enabled);
		hoverRing(graphics, x, y, w, h, enabled && hover);
	}

	/** Beschriftete Fläche in beliebiger Farbe. */
	public static void labeled(GuiGraphicsExtractor graphics, Font font, int x, int y, int w, int h, Component label, int color,
			boolean enabled) {
		face(graphics, x, y, w, h, color);
		int textColor = enabled ? onColor(color) : DISABLED_TEXT;
		String text = fit(font, label.getString(), w - 6);
		graphics.text(font, text, x + (w - font.width(text) + 1) / 2, y + (h - 7) / 2, textColor, textColor == 0xFFFFFFFF && classic());
	}

	/** Kürzt einen Text auf {@code maxWidth} Pixel und hängt „…“ an. */
	public static String fit(Font font, String text, int maxWidth) {
		if (font.width(text) <= maxWidth) {
			return text;
		}

		String cut = font.plainSubstrByWidth(text, Math.max(0, maxWidth - font.width("…")));
		return cut.stripTrailing() + "…";
	}

	/** Passt der Text nicht in {@code maxWidth}? (Dann lohnt ein Tooltip.) */
	public static boolean overflows(Font font, Component text, int maxWidth) {
		return font.width(text) > maxWidth;
	}

	/** Text in Fensterfarbe, gekürzt auf {@code maxWidth}. */
	public static void label(GuiGraphicsExtractor graphics, Font font, Component text, int x, int y, int maxWidth, int color) {
		graphics.text(font, fit(font, text.getString(), maxWidth), x, y, color, false);
	}
}
