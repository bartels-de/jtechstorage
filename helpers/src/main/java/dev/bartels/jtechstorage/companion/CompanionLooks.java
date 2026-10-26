package dev.bartels.jtechstorage.companion;

import org.jspecify.annotations.Nullable;

import net.minecraft.network.chat.Component;

/**
 * Companion appearance (v0.25.4): skin, two swappable body parts, three visible equipment slots and a freely
 * selectable glow colour. Everything is unlocked through the companion's progress (level, tier, monsters defeated).
 *
 * <p>Packed value ({@code look}): bits 0-2 skin, 3-4 part 1, 5-6 part 2, 7-9 collar, 10-12 amulet, 13-15 armour.
 * Equipment: 0 = invisible, 1-5 = material (iron ... amethyst obsidian), 6 = automatically the best unlocked one.
 * Equipment stats always come from the best unlocked material - the appearance is purely cosmetic.
 */
public final class CompanionLooks {
	public static final int SKIN = 0;
	public static final int PART1 = 1;
	public static final int PART2 = 2;
	public static final int COLLAR = 3;
	public static final int AMULET = 4;
	public static final int ARMOR = 5;
	/** Rows in the menu (skin ... armour); plus the AURA field (bit 16: 0 on, 1 off). */
	public static final int FIELDS = 6;
	public static final int AURA = 6;
	private static final int[] SHIFT = {0, 3, 5, 7, 10, 13, 16};
	private static final int[] MASK = {7, 3, 3, 7, 7, 7, 1};
	/** Number of values per field (equipment: invisible, 5 materials, automatic). */
	public static final int[] OPTIONS = {6, 3, 3, 7, 7, 7, 2};

	public static final String[] SKINS = {"standard", "frost", "glut", "jade", "gold", "seelenband"};
	public static final String[] MATERIALS = {"none", "iron", "gold", "diamond", "netherite", "obsidian", "auto"};
	public static final int AUTO = 6;
	/** New companions: default appearance, equipment automatic. */
	public static final int DEFAULT = (AUTO << 7) | (AUTO << 10) | (AUTO << 13);
	/** Glow colour: -1 = default (amethyst violet). */
	public static final int GLOW_DEFAULT = -1;
	public static final int GLOW_LEVEL = 10;

	/** Part 1 / part 2 per species (translation key jtechstorage.look.<species>.<part>.<variant>). */
	public static final String[][] PART1_NAMES = {{"swept", "crown", "sickle"}, {"tiers", "witch", "flat"}, {"fan", "long", "forked"}};
	public static final String[][] PART2_NAMES = {{"crystal", "leather", "none"}, {"pack", "cape", "none"}, {"crystal", "feather", "none"}};

	private static final int[] MATERIAL_LEVEL = {0, 10, 25, 45, 70, 100};
	private static final int[] MATERIAL_STAGE = {1, 1, 1, 2, 1, 3};

	private CompanionLooks() {
	}

	public static int get(int look, int field) {
		return (look >> SHIFT[field]) & MASK[field];
	}

	public static int with(int look, int field, int value) {
		return (look & ~(MASK[field] << SHIFT[field])) | ((value & MASK[field]) << SHIFT[field]);
	}

	public static int speciesIndex(String species) {
		return switch (species) {
			case "shroom" -> 1;
			case "raven" -> 2;
			default -> 0;
		};
	}

	public static String part1Group(String species) {
		return switch (species) {
			case "shroom" -> "cap";
			case "raven" -> "tail";
			default -> "horns";
		};
	}

	public static String part2Group(String species) {
		return switch (species) {
			case "shroom" -> "back";
			case "raven" -> "crest";
			default -> "wings";
		};
	}

	/** Name of the variant or field value (for menu and tooltips). */
	public static Component valueName(String species, int field, int value) {
		return switch (field) {
			case SKIN -> Component.translatable("jtechstorage.look.skin." + SKINS[Math.clamp(value, 0, SKINS.length - 1)]);
			case PART1 -> Component.translatable("jtechstorage.look." + part1Group(species) + "." + PART1_NAMES[speciesIndex(species)][Math.clamp(value, 0, 2)]);
			case PART2 -> Component.translatable("jtechstorage.look." + part2Group(species) + "." + PART2_NAMES[speciesIndex(species)][Math.clamp(value, 0, 2)]);
			default -> Component.translatable("jtechstorage.look.material." + MATERIALS[Math.clamp(value, 0, MATERIALS.length - 1)]);
		};
	}

	public static Component fieldName(String species, int field) {
		return switch (field) {
			case SKIN -> Component.translatable("jtechstorage.look.field.skin");
			case PART1 -> Component.translatable("jtechstorage.look.field." + part1Group(species));
			case PART2 -> Component.translatable("jtechstorage.look.field." + part2Group(species));
			case COLLAR -> Component.translatable("jtechstorage.look.field.collar");
			case AMULET -> Component.translatable("jtechstorage.look.field.amulet");
			default -> Component.translatable("jtechstorage.look.field.armor");
		};
	}

	// ------------------------------------------------------------------
	// Unlocking
	// ------------------------------------------------------------------

	public static boolean materialUnlocked(int material, int level, int stage) {
		return material <= 0 || material == AUTO || material < MATERIAL_LEVEL.length && level >= MATERIAL_LEVEL[material] && stage >= MATERIAL_STAGE[material];
	}

	/** Best unlocked material 0-5 (determines the equipment stats). */
	public static int bestMaterial(int level, int stage) {
		for (int m = 5; m >= 1; m--) {
			if (materialUnlocked(m, level, stage)) {
				return m;
			}
		}

		return 0;
	}

	/** Material that is actually rendered (automatic -> best). */
	public static int shownMaterial(int value, int level, int stage) {
		if (value == AUTO) {
			return bestMaterial(level, stage);
		}

		return materialUnlocked(value, level, stage) ? value : 0;
	}

	/** null = free, otherwise the missing requirement as text. */
	public static @Nullable Component requirement(int field, int value, int level, int stage, int kills, int bond) {
		switch (field) {
			case SKIN -> {
				return switch (value) {
					case 1 -> level >= 20 ? null : Component.translatable("jtechstorage.look.req.level", 20);
					case 2 -> kills >= 200 ? null : Component.translatable("jtechstorage.look.req.kills", 200, kills);
					case 3 -> level >= 50 ? null : Component.translatable("jtechstorage.look.req.level", 50);
					case 4 -> level >= 100 && stage >= 3 ? null : Component.translatable("jtechstorage.look.req.level_stage", 100, 3);
					case 5 -> CompanionBond.stage(bond) >= 4 ? null : Component.translatable("jtechstorage.look.req.bond",
							Component.translatable("jtechstorage.bond.stage.soulmate"));
					default -> null;
				};
			}
			case PART1 -> {
				int need = value == 1 ? 30 : value == 2 ? 60 : 0;
				return level >= need ? null : Component.translatable("jtechstorage.look.req.level", need);
			}
			case PART2 -> {
				return value == 0 || stage >= 3 ? null : Component.translatable("jtechstorage.look.req.stage", 3);
			}
			case AURA -> {
				return null;
			}
			default -> {
				if (materialUnlocked(value, level, stage)) {
					return null;
				}

				return MATERIAL_STAGE[value] > 1 ? Component.translatable("jtechstorage.look.req.level_stage", MATERIAL_LEVEL[value], MATERIAL_STAGE[value])
						: Component.translatable("jtechstorage.look.req.level", MATERIAL_LEVEL[value]);
			}
		}
	}

	public static @Nullable Component glowRequirement(int level) {
		return level >= GLOW_LEVEL ? null : Component.translatable("jtechstorage.look.req.level", GLOW_LEVEL);
	}

	/** Valid value for a field? (The server checks before setting.) */
	public static boolean valid(int field, int value) {
		return field >= 0 && field <= AURA && value >= 0 && value < OPTIONS[field];
	}
}
