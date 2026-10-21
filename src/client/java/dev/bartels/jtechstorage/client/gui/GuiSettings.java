package dev.bartels.jtechstorage.client.gui;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.fabricmc.loader.api.FabricLoader;

import dev.bartels.jtechstorage.JTechStorage;

/**
 * Persönliche Oberflächen-Einstellungen (nur Client), gespeichert in {@code config/jtechstorage-client.json}.
 */
public final class GuiSettings {
	/** Zeichenstil der Fenster (v0.29). */
	public enum Style {
		CLASSIC, GLASS, MODERN
	}

	public enum Theme {
		LIGHT(Style.CLASSIC, false), DARK(Style.CLASSIC, true), GLASS(Style.GLASS, true), FROST(Style.GLASS, false),
		MODERN_LIGHT(Style.MODERN, false), MODERN_DARK(Style.MODERN, true);

		public final Style style;
		public final boolean dark;

		Theme(Style style, boolean dark) {
			this.style = style;
			this.dark = dark;
		}
	}

	/** Voreinstellungen für die Effekte (setzen nur die einzelnen Schalter). */
	public enum Effects {
		OFF, SUBTLE, RICH, CUSTOM
	}

	public enum Accent {
		AMETHYST(0xFF8D6ACC), TEAM(0), BLUE(0xFF3F7FD0), GREEN(0xFF3E9E58), RED(0xFFC84040), GOLD(0xFFC89A2A),
		ROSE(0xFFD0609A), TEAL(0xFF2FA8A0), GRAY(0xFF707080);

		public final int color;

		Accent(int color) {
			this.color = color;
		}
	}

	/** Zeilen im Speicher-Terminal (Crafting-Terminal: 2 weniger). AUTO = so viele, wie auf den Bildschirm passen. */
	public enum Rows {
		AUTO(0), FOUR(4), SIX(6), EIGHT(8), TEN(10);

		public final int rows;

		Rows(int rows) {
			this.rows = rows;
		}
	}

	public enum CountSize {
		SMALL, NORMAL, LARGE
	}

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static GuiSettings instance;

	public Theme theme = Theme.LIGHT;
	public Accent accent = Accent.AMETHYST;
	public Rows rows = Rows.SIX;
	public CountSize countSize = CountSize.NORMAL;
	/** Begleiter-HUD oben links (v0.26). */
	public boolean companionHud = true;

	// Effekte (v0.29) – Standard: dezent
	public boolean fadeIn = true;
	public boolean hoverGlow = true;
	public boolean borderShimmer = true;
	public boolean sheen = false;
	public boolean particles = false;
	public boolean countAnimation = false;

	/** Welche Voreinstellung passt zu den aktuellen Schaltern? */
	public Effects effects() {
		if (matches(false, false, false, false, false, false)) {
			return Effects.OFF;
		}

		if (matches(true, true, true, false, false, false)) {
			return Effects.SUBTLE;
		}

		return matches(true, true, true, true, true, true) ? Effects.RICH : Effects.CUSTOM;
	}

	private boolean matches(boolean fade, boolean hover, boolean shimmer, boolean sheen, boolean particles, boolean count) {
		return fadeIn == fade && hoverGlow == hover && borderShimmer == shimmer && this.sheen == sheen && this.particles == particles
				&& countAnimation == count;
	}

	/** Setzt alle Schalter passend zur Voreinstellung (CUSTOM ändert nichts). */
	public void applyEffects(Effects preset) {
		if (preset == Effects.CUSTOM) {
			return;
		}

		boolean on = preset != Effects.OFF;
		boolean rich = preset == Effects.RICH;
		fadeIn = on;
		hoverGlow = on;
		borderShimmer = on;
		sheen = rich;
		particles = rich;
		countAnimation = rich;
	}

	public static GuiSettings get() {
		if (instance == null) {
			instance = load();
		}

		return instance;
	}

	private static Path file() {
		return FabricLoader.getInstance().getConfigDir().resolve("jtechstorage-client.json");
	}

	private static GuiSettings load() {
		try (Reader reader = Files.newBufferedReader(file())) {
			GuiSettings settings = GSON.fromJson(reader, GuiSettings.class);

			if (settings != null) {
				settings.fix();
				return settings;
			}
		} catch (Exception ignored) {
			// keine oder kaputte Datei – Standardwerte
		}

		return new GuiSettings();
	}

	private void fix() {
		if (theme == null) {
			theme = Theme.LIGHT;
		}

		if (accent == null) {
			accent = Accent.AMETHYST;
		}

		if (rows == null) {
			rows = Rows.SIX;
		}

		if (countSize == null) {
			countSize = CountSize.NORMAL;
		}
	}

	public void save() {
		try {
			Files.createDirectories(file().getParent());

			try (Writer writer = Files.newBufferedWriter(file())) {
				GSON.toJson(this, writer);
			}
		} catch (Exception e) {
			JTechStorage.LOGGER.warn("Konnte Oberflächen-Einstellungen nicht speichern", e);
		}
	}

	public static <E extends Enum<E>> E next(E value) {
		E[] values = value.getDeclaringClass().getEnumConstants();
		return values[(value.ordinal() + 1) % values.length];
	}
}
