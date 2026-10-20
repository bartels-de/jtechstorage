package dev.bartels.jtechstorage;

import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.Strictness;

import net.fabricmc.loader.api.FabricLoader;

/**
 * Server-Konfiguration in {@code config/jtechstorage.json} (JSON mit //-Kommentaren). Wird beim Start gelesen, fehlende
 * Werte werden ergänzt und die Datei mit Kommentaren neu geschrieben. Änderungen gelten nach einem Neustart.
 * Der Server schickt seine Werte beim Betreten an den Client (für Tooltips und Handbuch).
 */
public final class ModConfig {
	public static final class Network {
		public int baseRange = 20;
		public int ironRange = 32;
		public int goldRange = 48;
		public int diamondRange = 64;
		public int netheriteRange = 96;
		public int enderRange = 128;
		public int baseCapacity = 8;
		public int ironCapacity = 16;
		public int goldCapacity = 32;
		public int diamondCapacity = 64;
		public int netheriteCapacity = 128;
		public int wirelessRangeFactor = 4;
		public int resonanceRangeBonus = 16;
		public double resonanceHelperSpeed = 1.8;
		public int beetleChargePerSecond = 2;
	}

	public static final class Boss {
		/** NaN = nicht gesetzt → Standardwert des jeweiligen Bosses (siehe {@link #fix()}). */
		public double health = Double.NaN;
		public double armor = Double.NaN;
		public double damageMultiplier = 1.0;

		Boss() {
		}

		Boss(double health, double armor) {
			this.health = health;
			this.armor = armor;
		}

		void defaults(double health, double armor) {
			if (Double.isNaN(this.health)) {
				this.health = health;
			}

			if (Double.isNaN(this.armor)) {
				this.armor = armor;
			}
		}
	}

	public static final class Bosses {
		public Boss robot = new Boss(400, 10);
		public double robotCoreChance = 0.3;
		public Boss spider = new Boss(600, 14);
	}

	public static final class Helpers {
		public double foxDigIntervalDays = 3.0;
		public int foxMaxFeedingsPerDay = 4;
		public double chelCraftIntervalSeconds = 5.0;
		public int chelMaxCraftsPerRound = 4;
		public double spritePauseSeconds = 2.5;
		public int spriteCollectRadius = 12;
		public int spriteBackpackRadius = 10;
		public int mothWatchRadius = 24;
		public int peterGuardRadius = 16;
		public double cheldroganHealth = 250.0;
		public double cheldroganDamage = 24.0;
		public double cheldroganBreathCooldownSeconds = 4.0;
		public double cheldroganRainCooldownSeconds = 10.0;
		public double phoenixMailBlocksPerSecond = 32.0;
		public double amethystPhoenixBlocksPerSecond = 12.0;
	}

	/** Begleiter (v0.25): Mantis, Pilz und Rabe vom Geodenhüter. */
	public static final class Companions {
		public int stage2Level = 30;
		public int stage3Level = 100;
		public double xpShare = 0.5;
		public double keeperChance = 0.6;
		public int keeperSearchRadius = 12;
		/** Ausrüstung (v0.25.4): Halsband, Amulett und Panzerung geben Werte (sonst reine Kosmetik). */
		public boolean equipment = true;
	}

	public Network network = new Network();
	public Bosses bosses = new Bosses();
	public Helpers helpers = new Helpers();
	public Companions companions = new Companions();
	public Storm storm = new Storm();

	/** Resonanz-Sturm (v0.27): startet von selbst am eigenen Terminal. */
	public static final class Storm {
		public boolean naturalStorms = true;
		public int amethystPerStorm = 500;
		public double cooldownDays = 1.0;
	}

	/** Farm-Werkzeuge (v0.27.1): Chunk Loader. */
	public static final class Farm {
		public int chunkLoadersPerPlayer = 25;
		public int chunkLoaderMinutesPerShard = 60;
	}

	public Farm farm = new Farm();

	private static final Gson GSON = new GsonBuilder().setStrictness(Strictness.LENIENT).create();
	private static ModConfig local;
	private static ModConfig active;

	/** Aktive Werte: auf dem Server die Datei, im Client während einer Verbindung die Werte des Servers. */
	public static ModConfig get() {
		if (active == null) {
			load();
		}

		return active;
	}

	private static Path file() {
		return FabricLoader.getInstance().getConfigDir().resolve(JTechStorage.MOD_ID + ".json");
	}

	/** Liest die Datei (oder Standardwerte) und schreibt sie kommentiert zurück. */
	public static void load() {
		ModConfig config = null;

		try (Reader reader = Files.newBufferedReader(file(), StandardCharsets.UTF_8)) {
			config = GSON.fromJson(reader, ModConfig.class);
		} catch (java.nio.file.NoSuchFileException ignored) {
			// erster Start
		} catch (Exception e) {
			JTechStorage.LOGGER.warn("Could not read {} – using defaults for broken values: {}", file(), e.getMessage());
		}

		if (config == null) {
			config = new ModConfig();
		}

		config.fix();
		local = config;
		active = config;

		try {
			Files.createDirectories(file().getParent());
			Files.writeString(file(), config.toCommentedJson(), StandardCharsets.UTF_8);
		} catch (Exception e) {
			JTechStorage.LOGGER.warn("Could not write {}: {}", file(), e.getMessage());
		}
	}

	/** Kompakte Form für die Übertragung an Clients. */
	public static String toNetworkJson() {
		return GSON.toJson(get());
	}

	/** Client: Werte des Servers übernehmen. */
	public static void applyRemote(String json) {
		try {
			ModConfig remote = GSON.fromJson(json, ModConfig.class);

			if (remote != null) {
				remote.fix();
				active = remote;
			}
		} catch (Exception e) {
			JTechStorage.LOGGER.warn("Ignoring invalid server config: {}", e.getMessage());
		}
	}

	/** Client: nach dem Trennen wieder die eigene Datei verwenden. */
	public static void restoreLocal() {
		active = local;
	}

	// ------------------------------------------------------------------
	// Plausibilität
	// ------------------------------------------------------------------

	private void fix() {
		if (network == null) {
			network = new Network();
		}

		if (bosses == null) {
			bosses = new Bosses();
		}

		if (bosses.robot == null) {
			bosses.robot = new Bosses().robot;
		}

		if (bosses.spider == null) {
			bosses.spider = new Bosses().spider;
		}

		bosses.robot.defaults(400, 10);
		bosses.spider.defaults(600, 14);

		if (helpers == null) {
			helpers = new Helpers();
		}

		Network n = network;
		n.baseRange = clamp(n.baseRange, 1, 512);
		n.ironRange = clamp(n.ironRange, 1, 512);
		n.goldRange = clamp(n.goldRange, 1, 512);
		n.diamondRange = clamp(n.diamondRange, 1, 512);
		n.netheriteRange = clamp(n.netheriteRange, 1, 512);
		n.enderRange = clamp(n.enderRange, 1, 512);
		n.baseCapacity = clamp(n.baseCapacity, 1, 1024);
		n.ironCapacity = clamp(n.ironCapacity, 1, 1024);
		n.goldCapacity = clamp(n.goldCapacity, 1, 1024);
		n.diamondCapacity = clamp(n.diamondCapacity, 1, 1024);
		n.netheriteCapacity = clamp(n.netheriteCapacity, 1, 1024);
		n.wirelessRangeFactor = clamp(n.wirelessRangeFactor, 1, 64);
		n.resonanceRangeBonus = clamp(n.resonanceRangeBonus, 0, 256);
		n.resonanceHelperSpeed = clamp(n.resonanceHelperSpeed, 1.0, 10.0);
		n.beetleChargePerSecond = clamp(n.beetleChargePerSecond, 0, 1000);

		for (Boss boss : new Boss[] {bosses.robot, bosses.spider}) {
			boss.health = clamp(boss.health, 1.0, 100000.0);
			boss.armor = clamp(boss.armor, 0.0, 30.0);
			boss.damageMultiplier = clamp(boss.damageMultiplier, 0.0, 100.0);
		}

		bosses.robotCoreChance = clamp(bosses.robotCoreChance, 0.0, 1.0);

		Helpers h = helpers;
		h.foxDigIntervalDays = clamp(h.foxDigIntervalDays, 0.05, 1000.0);
		h.foxMaxFeedingsPerDay = clamp(h.foxMaxFeedingsPerDay, 0, 1000);
		h.chelCraftIntervalSeconds = clamp(h.chelCraftIntervalSeconds, 0.25, 3600.0);
		h.chelMaxCraftsPerRound = clamp(h.chelMaxCraftsPerRound, 1, 64);
		h.spritePauseSeconds = clamp(h.spritePauseSeconds, 0.05, 600.0);
		h.spriteCollectRadius = clamp(h.spriteCollectRadius, 0, 64);
		h.spriteBackpackRadius = clamp(h.spriteBackpackRadius, 0, 64);
		h.mothWatchRadius = clamp(h.mothWatchRadius, 1, 128);
		h.peterGuardRadius = clamp(h.peterGuardRadius, 1, 64);
		h.cheldroganHealth = clamp(h.cheldroganHealth, 1.0, 100000.0);
		h.cheldroganDamage = clamp(h.cheldroganDamage, 0.0, 10000.0);
		h.cheldroganBreathCooldownSeconds = clamp(h.cheldroganBreathCooldownSeconds, 0.5, 600.0);
		h.cheldroganRainCooldownSeconds = clamp(h.cheldroganRainCooldownSeconds, 1.0, 600.0);
		h.phoenixMailBlocksPerSecond = clamp(h.phoenixMailBlocksPerSecond, 1.0, 10000.0);
		h.amethystPhoenixBlocksPerSecond = clamp(h.amethystPhoenixBlocksPerSecond, 1.0, 100.0);

		if (companions == null) {
			companions = new Companions();
		}

		Companions c = companions;
		c.stage2Level = clamp(c.stage2Level, 1, 10000);
		c.stage3Level = clamp(c.stage3Level, c.stage2Level + 1, 10001);
		c.xpShare = clamp(c.xpShare, 0.0, 1.0);
		c.keeperChance = clamp(c.keeperChance, 0.0, 1.0);
		c.keeperSearchRadius = clamp(c.keeperSearchRadius, 4, 32);

		if (farm == null) {
			farm = new Farm();
		}

		farm.chunkLoadersPerPlayer = clamp(farm.chunkLoadersPerPlayer, 0, 1000);
		farm.chunkLoaderMinutesPerShard = clamp(farm.chunkLoaderMinutesPerShard, 1, 10080);

		if (storm == null) {
			storm = new Storm();
		}

		storm.amethystPerStorm = clamp(storm.amethystPerStorm, 1, 1000000);
		storm.cooldownDays = clamp(storm.cooldownDays, 0.0, 365.0);
	}

	private static int clamp(int value, int min, int max) {
		return Math.clamp(value, min, max);
	}

	private static double clamp(double value, double min, double max) {
		return Double.isNaN(value) ? min : Math.clamp(value, min, max);
	}

	// ------------------------------------------------------------------
	// Kommentierte Datei
	// ------------------------------------------------------------------

	private record Entry(String key, Object value, String comment) {
	}

	private String toCommentedJson() {
		StringBuilder out = new StringBuilder();
		out.append("// JTechStorage server config. Changes apply after a restart.\n");
		out.append("// JTechStorage-Serverkonfiguration. Aenderungen gelten nach einem Neustart.\n");
		out.append("// Delete a line (or the file) to restore its default value.\n");
		out.append("{\n");

		Network n = network;
		section(out, "network", "Storage network / Lager-Netzwerk", List.of(
				new Entry("baseRange", n.baseRange, "Range without upgrade (blocks) / Reichweite ohne Upgrade"),
				new Entry("ironRange", n.ironRange, "Range: iron upgrade"),
				new Entry("goldRange", n.goldRange, "Range: gold upgrade"),
				new Entry("diamondRange", n.diamondRange, "Range: diamond upgrade"),
				new Entry("netheriteRange", n.netheriteRange, "Range: netherite upgrade"),
				new Entry("enderRange", n.enderRange, "Range: ender upgrade (wireless terminal is unlimited anyway)"),
				new Entry("baseCapacity", n.baseCapacity, "Linked containers without upgrade / Kisten ohne Upgrade"),
				new Entry("ironCapacity", n.ironCapacity, "Capacity: iron upgrade"),
				new Entry("goldCapacity", n.goldCapacity, "Capacity: gold upgrade"),
				new Entry("diamondCapacity", n.diamondCapacity, "Capacity: diamond upgrade"),
				new Entry("netheriteCapacity", n.netheriteCapacity, "Capacity: netherite upgrade"),
				new Entry("wirelessRangeFactor", n.wirelessRangeFactor, "Wireless terminal range = terminal range x this / Funk-Faktor"),
				new Entry("resonanceRangeBonus", n.resonanceRangeBonus, "Extra range while charged with resonance / Resonanz-Bonus"),
				new Entry("resonanceHelperSpeed", n.resonanceHelperSpeed, "Helper speed multiplier while charged / Helfer-Tempo mit Resonanz"),
				new Entry("beetleChargePerSecond", n.beetleChargePerSecond, "Resonance per second from a Spark Beetle (rain x2, thunder x5)")), false);

		Bosses b = bosses;
		out.append("  // Bosses / Bosse. damageMultiplier scales all attacks (1.0 = normal)\n");
		out.append("  \"bosses\": {\n");
		boss(out, "robot", "Amethyst Robot / Amethyst-Roboter", b.robot, false);
		out.append("    // Chance for an Amethyst Core or Wireless Core per kill (0.0-1.0)\n");
		out.append("    \"robotCoreChance\": ").append(format(b.robotCoreChance)).append(",\n");
		boss(out, "spider", "Great Amethyst Spider / Grosse Amethyst-Spinne (always drops exactly one upgrade)", b.spider, true);
		out.append("  },\n");

		Helpers h = helpers;
		section(out, "helpers", "Helpers / Helfer", List.of(
				new Entry("foxDigIntervalDays", h.foxDigIntervalDays, "Amethyst Fox: in-game days between finds / Tage zwischen Funden"),
				new Entry("foxMaxFeedingsPerDay", h.foxMaxFeedingsPerDay, "Amethyst Fox: berries per day / Fuetterungen pro Tag"),
				new Entry("chelCraftIntervalSeconds", h.chelCraftIntervalSeconds, "Chel: seconds between crafting rounds / Sekunden zwischen Runden"),
				new Entry("chelMaxCraftsPerRound", h.chelMaxCraftsPerRound, "Chel: crafts per round"),
				new Entry("spritePauseSeconds", h.spritePauseSeconds, "Amethyst Sprite: pause between jobs / Pause zwischen Auftraegen"),
				new Entry("spriteCollectRadius", h.spriteCollectRadius, "Amethyst Sprite: item pickup radius around the terminal"),
				new Entry("spriteBackpackRadius", h.spriteBackpackRadius, "Amethyst Sprite: radius for emptying placed backpacks"),
				new Entry("mothWatchRadius", h.mothWatchRadius, "Watch Moth: radius for glowing monsters / Wach-Radius"),
				new Entry("peterGuardRadius", h.peterGuardRadius, "Peter Chel Parker (and Cheldrogan): guard radius / Schutz-Radius"),
				new Entry("cheldroganHealth", h.cheldroganHealth, "Cheldrogan: health (0 = turns back into Peter) / Leben"),
				new Entry("cheldroganDamage", h.cheldroganDamage, "Cheldrogan: tail sweep damage / Schwanzhieb-Schaden"),
				new Entry("cheldroganBreathCooldownSeconds", h.cheldroganBreathCooldownSeconds, "Cheldrogan: crystal breath cooldown when ridden / Odem-Abklingzeit"),
				new Entry("cheldroganRainCooldownSeconds", h.cheldroganRainCooldownSeconds, "Cheldrogan: shard rain cooldown when ridden / Splitter-Regen-Abklingzeit"),
				new Entry("phoenixMailBlocksPerSecond", h.phoenixMailBlocksPerSecond, "Phoenix mail: flight speed for parcel delivery time"),
				new Entry("amethystPhoenixBlocksPerSecond", h.amethystPhoenixBlocksPerSecond, "Amethyst Phoenix: travel speed without cargo")), false);

		Companions c = companions;
		section(out, "companions", "Companions / Begleiter", List.of(
				new Entry("stage2Level", c.stage2Level, "Companion level for stage 2 / Level fuer Stufe 2"),
				new Entry("stage3Level", c.stage3Level, "Companion level for stage 3 / Level fuer Stufe 3"),
				new Entry("xpShare", c.xpShare, "Share of collected XP that goes to the companion (0.0-1.0) / XP-Anteil des Begleiters"),
				new Entry("keeperChance", c.keeperChance, "Chance that a Geode Keeper lives in a newly found geode (0.0-1.0)"),
				new Entry("keeperSearchRadius", c.keeperSearchRadius, "Radius around players in which geodes are noticed / Suchradius fuer Geoden"),
				new Entry("equipment", c.equipment, "Companion equipment (collar, amulet, armor) gives stats / Ausruestung gibt Werte")), false);

		Storm st = storm;
		section(out, "storm", "Resonance Storm / Resonanz-Sturm", List.of(
				new Entry("naturalStorms", st.naturalStorms, "Storms start on their own at your terminal (needs a helper) / Stuerme starten von selbst"),
				new Entry("amethystPerStorm", st.amethystPerStorm, "Amethyst mined before the first storm and between storms / Amethyst je Sturm"),
				new Entry("cooldownDays", st.cooldownDays, "Minimum in-game days between two storms of a player / Mindestabstand in Tagen")), false);

		Farm fa = farm;
		section(out, "farm", "Farm tools / Farm-Werkzeuge", List.of(
				new Entry("chunkLoadersPerPlayer", fa.chunkLoadersPerPlayer, "Active chunk loaders per player (0 = none allowed) / Aktive Chunk Loader pro Spieler"),
				new Entry("chunkLoaderMinutesPerShard", fa.chunkLoaderMinutesPerShard, "Minutes one amethyst shard keeps a chunk loaded / Minuten je Amethyst-Scherbe")), true);

		out.append("}\n");
		return out.toString();
	}

	private static void section(StringBuilder out, String name, String title, List<Entry> entries, boolean last) {
		out.append("  // ").append(title).append('\n');
		out.append("  \"").append(name).append("\": {\n");
		List<String> lines = new ArrayList<>();

		for (Entry entry : entries) {
			lines.add("    // " + entry.comment() + "\n    \"" + entry.key() + "\": " + format(entry.value()));
		}

		out.append(String.join(",\n", lines)).append('\n');
		out.append(last ? "  }\n" : "  },\n");
	}

	private static void boss(StringBuilder out, String name, String title, Boss boss, boolean last) {
		out.append("    // ").append(title).append('\n');
		out.append("    \"").append(name).append("\": { \"health\": ").append(format(boss.health))
				.append(", \"armor\": ").append(format(boss.armor))
				.append(", \"damageMultiplier\": ").append(format(boss.damageMultiplier)).append(" }")
				.append(last ? "\n" : ",\n");
	}

	private static String format(Object value) {
		if (value instanceof Double d) {
			return d == Math.rint(d) && Math.abs(d) < 1e9 ? String.format(java.util.Locale.ROOT, "%.1f", d) : Double.toString(d);
		}

		return String.valueOf(value);
	}
}
