package dev.bartels.jtechstorage.client.gui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/**
 * Farben aller Mod-Oberflächen – hell (Vanilla-Look) oder dunkel – plus Akzentfarbe für Knöpfe und Rahmen.
 */
public final class GuiTheme {
	/** Zuletzt bekannte Team-Farbe (für den Akzent „Team“). */
	private static int teamColor = 0xFF8D6ACC;

	private GuiTheme() {
	}

	public static void setTeamColor(int rgb) {
		teamColor = 0xFF000000 | rgb;
	}

	private static boolean dark() {
		return GuiSettings.get().theme == GuiSettings.Theme.DARK;
	}

	// Fenster
	public static int face() {
		return dark() ? 0xFF2B2B31 : 0xFFC6C6C6;
	}

	public static int highlight() {
		return dark() ? 0xFF4A4A54 : 0xFFFFFFFF;
	}

	public static int shadow() {
		return dark() ? 0xFF131317 : 0xFF555555;
	}

	/** Leicht abgesetzte Fläche (Zeilen-Streifen, Listen). */
	public static int inset() {
		return dark() ? 0xFF232328 : 0xFFBDBDBD;
	}

	// Slots
	public static int slot() {
		return dark() ? 0xFF1B1B20 : 0xFF8B8B8B;
	}

	public static int slotDark() {
		return dark() ? 0xFF0E0E11 : 0xFF373737;
	}

	public static int slotLight() {
		return dark() ? 0xFF45454F : 0xFFFFFFFF;
	}

	// Text
	public static int text() {
		return dark() ? 0xFFEDEDF4 : 0xFF2E2E2E;
	}

	/** Zweitrangiger Text – trotzdem mit genug Kontrast (≥ 4,5 : 1 auf Fenster und Streifen). */
	public static int subtle() {
		return dark() ? 0xFFB8B8C6 : 0xFF4A4A4A;
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
		return dark() ? 0xFF8C8C9C : 0xFF1E1E1E;
	}

	/** Akzentfarbe als Textfarbe (abgedunkelt bzw. aufgehellt, damit sie auf dem Fenster lesbar bleibt). */
	public static int accentText() {
		return dark() ? mix(accent(), 0xFFFFFFFF, 0.45f) : mix(accent(), 0xFF000000, 0.5f);
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

	// ------------------------------------------------------------------
	// Zeichnen
	// ------------------------------------------------------------------

	/** Fenster im Vanilla-Stil. */
	public static void panel(GuiGraphicsExtractor graphics, int x, int y, int w, int h) {
		graphics.fill(x, y, x + w, y + h, 0xFF000000);
		graphics.fill(x + 1, y + 1, x + w - 1, y + h - 1, highlight());
		graphics.fill(x + 2, y + 2, x + w - 1, y + h - 1, shadow());
		graphics.fill(x + 2, y + 2, x + w - 2, y + h - 2, face());
	}

	/** Slot-Rahmen (18×18, x/y = linke obere Ecke des Rahmens). */
	public static void slot(GuiGraphicsExtractor graphics, int x, int y) {
		sunken(graphics, x, y, 18, 18);
	}

	/** Vertiefte Fläche beliebiger Größe. */
	public static void sunken(GuiGraphicsExtractor graphics, int x, int y, int w, int h) {
		graphics.fill(x, y, x + w, y + h, slotDark());
		graphics.fill(x + 1, y + 1, x + w, y + h, slotLight());
		graphics.fill(x + 1, y + 1, x + w - 1, y + h - 1, slot());
	}

	public static final int DISABLED_FACE = 0xFF4E4E52;
	public static final int DISABLED_TEXT = 0xFFCACACE;

	/** Knopf in Akzentfarbe (ohne Beschriftung). */
	public static void button(GuiGraphicsExtractor graphics, int x, int y, int w, int h, boolean enabled, boolean hover) {
		face(graphics, x, y, w, h, !enabled ? DISABLED_FACE : hover ? accentHover() : accent());
	}

	/** Erhabene Fläche mit Rahmen und Lichtkante in beliebiger Farbe. */
	public static void face(GuiGraphicsExtractor graphics, int x, int y, int w, int h, int color) {
		graphics.fill(x, y, x + w, y + h, 0xFF000000);
		graphics.fill(x + 1, y + 1, x + w - 1, y + h - 1, mix(color, 0xFF000000, 0.35f));
		graphics.fill(x + 1, y + 1, x + w - 2, y + h - 2, mix(color, 0xFFFFFFFF, 0.25f));
		graphics.fill(x + 2, y + 2, x + w - 2, y + h - 2, color);
	}

	/** Knopf mit Beschriftung: Farbe der Schrift passt sich dem Untergrund an, zu lange Texte werden mit „…“ gekürzt. */
	public static void button(GuiGraphicsExtractor graphics, Font font, int x, int y, int w, int h, Component label,
			boolean enabled, boolean hover) {
		labeled(graphics, font, x, y, w, h, label, !enabled ? DISABLED_FACE : hover ? accentHover() : accent(), enabled);
	}

	/** Beschriftete Fläche in beliebiger Farbe. */
	public static void labeled(GuiGraphicsExtractor graphics, Font font, int x, int y, int w, int h, Component label, int color,
			boolean enabled) {
		face(graphics, x, y, w, h, color);
		int textColor = enabled ? onColor(color) : DISABLED_TEXT;
		String text = fit(font, label.getString(), w - 6);
		graphics.text(font, text, x + (w - font.width(text) + 1) / 2, y + (h - 7) / 2, textColor, textColor == 0xFFFFFFFF);
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
