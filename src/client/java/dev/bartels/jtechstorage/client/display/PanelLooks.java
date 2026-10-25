package dev.bartels.jtechstorage.client.display;

/** Farbtafeln für das Aussehen von Lager-Bildschirm und Lager-Banner (v0.36) – gemeinsam für Renderer und Menü. */
public final class PanelLooks {
	/** Rahmen: oben, unten, Außenkante. Index 5 = ohne Rahmen. */
	public static final int[][] FRAME = {
			{0xFF2E2446, 0xFF1A1428, 0xFF4A3A6E},
			{0xFF7444C8, 0xFF3A2066, 0xFFB98CFF},
			{0xFFC8983A, 0xFF7A5418, 0xFFF2D27A},
			{0xFFB86A48, 0xFF6E3A22, 0xFFE8A07A},
			{0xFF6A4628, 0xFF3A2414, 0xFF8E6440},
			{0xFF101010, 0xFF101010, 0xFF101010}};
	public static final String[] FRAME_NAMES = {"dark", "amethyst", "gold", "copper", "wood", "none"};
	/** Hintergrund: oben, unten. */
	public static final int[][] BACKGROUND = {
			{0xFF1B1430, 0xFF0A0711},
			{0xFF2A1450, 0xFF12081F},
			{0xFF0D2236, 0xFF06101A},
			{0xFF0F2A1C, 0xFF06120C},
			{0xFF2A140C, 0xFF120604},
			{0xFF050505, 0xFF000000}};
	public static final String[] BACKGROUND_NAMES = {"night", "violet", "ocean", "forest", "ember", "black"};
	public static final int[] ACCENT = {0xFF7FE3F0, 0xFFB98CFF, 0xFFF2C54A, 0xFF7CFFA0, 0xFFF08CBF, 0xFFFF8A5A, 0xFFEDEDF4, 0xFF6CA0FF};
	public static final String[] ACCENT_NAMES = {"cyan", "violet", "gold", "green", "pink", "orange", "white", "blue"};

	/** Saum: Gold, Silber, Kupfer, Amethyst, ohne. */
	public static final int[] TRIM = {0xFFE8BB4A, 0xFFD8DCE8, 0xFFC8784A, 0xFFB98CFF, 0};
	public static final String[] TRIM_NAMES = {"gold", "silver", "copper", "amethyst", "none"};
	public static final String[] PATTERN_NAMES = {"plain", "stripes", "rhombus", "stars", "crystals"};
	public static final int[] TEXT = {0xFFFFF3DA, 0xFFF2CF6B, 0xFFFFFFFF, 0xFF241A30};
	public static final String[] TEXT_NAMES = {"cream", "gold", "white", "dark"};
	public static final float[] WIND = {0.0f, 1.0f, 1.8f};
	public static final String[] WIND_NAMES = {"still", "light", "strong"};

	private PanelLooks() {
	}

	public static int at(int[] table, int index) {
		return table[Math.floorMod(index, table.length)];
	}

	public static int[] at(int[][] table, int index) {
		return table[Math.floorMod(index, table.length)];
	}

	/** Mischt zwei ARGB-Farben. */
	public static int mix(int a, int b, float t) {
		return PanelCanvas.mix(a, b, t);
	}
}
