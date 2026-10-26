package dev.bartels.jtechstorage.companion;

import java.util.List;
import java.util.UUID;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Bond (v0.26.0): grows through feeding, petting and playing and never decreases. Five levels unlock gestures,
 * the highest a skin "Soul Bond", a pink glow colour and an aura.
 */
public final class CompanionBond {
	/** Points required from level 0-4: Stranger, Familiar, Friend, Companion, Soulmate. */
	public static final int[] STAGES = {0, 100, 300, 600, 1000};
	public static final String[] STAGE_KEYS = {"stranger", "familiar", "friend", "partner", "soulmate"};
	public static final int MAX = 1000;

	public static final int FAVORITE = 25;
	public static final int FOOD = 3;
	public static final int FOOD_PER_DAY = 5;
	public static final int PET = 5;
	public static final int PETS_PER_DAY = 10;
	public static final int PET_COOLDOWN = 20 * 60;
	public static final int PLAY = 10;
	public static final int PLAYS_PER_DAY = 10;

	/** Gestures: 1 happy jump (Familiar), 2 cuddle (Friend), 3 species-specific gesture (Companion). */
	public static final int JOY = 1;
	public static final int NUZZLE = 2;
	public static final int SIGNATURE = 3;
	public static final int GESTURE_TICKS = 40;
	/** Pink glow colour "Soul Pink" (Soulmate). */
	public static final int SOUL_PINK = 0xFF7AB4;

	private static final List<Item> MANTIS_FOOD = List.of(Items.BEEF, Items.PORKCHOP, Items.MUTTON, Items.CHICKEN, Items.RABBIT, Items.COD, Items.SALMON);
	private static final List<Item> SHROOM_FOOD = List.of(Items.SWEET_BERRIES, Items.GLOW_BERRIES, Items.BONE_MEAL, Items.BROWN_MUSHROOM, Items.RED_MUSHROOM);
	private static final List<Item> RAVEN_FOOD = List.of(Items.WHEAT_SEEDS, Items.PUMPKIN_SEEDS, Items.MELON_SEEDS, Items.BEETROOT_SEEDS, Items.GOLD_NUGGET);

	private CompanionBond() {
	}

	public static int stage(int bond) {
		for (int s = STAGES.length - 1; s > 0; s--) {
			if (bond >= STAGES[s]) {
				return s;
			}
		}

		return 0;
	}

	/** Progress to the next level 0..1 (highest level: 1). */
	public static float progress(int bond) {
		int s = stage(bond);

		if (s >= STAGES.length - 1) {
			return 1.0f;
		}

		return (bond - STAGES[s]) / (float) (STAGES[s + 1] - STAGES[s]);
	}

	public static List<Item> foods(String species) {
		return switch (species) {
			case "shroom" -> SHROOM_FOOD;
			case "raven" -> RAVEN_FOOD;
			default -> MANTIS_FOOD;
		};
	}

	public static boolean isFood(String species, ItemStack stack) {
		return foods(species).contains(stack.getItem());
	}

	/** Favourite food of the day - changes daily, differs per companion. */
	public static Item favorite(String species, long day, UUID companion) {
		List<Item> list = foods(species);
		long h = day * 31L + companion.getLeastSignificantBits();
		return list.get((int) Math.floorMod(h ^ (h >>> 17), (long) list.size()));
	}

	/** Toy for throwing. */
	public static boolean isToy(ItemStack stack) {
		return stack.is(Items.STICK) || stack.is(Items.AMETHYST_SHARD);
	}

	public static String gestureKey(String species, int gesture) {
		return switch (gesture) {
			case JOY -> "joy";
			case NUZZLE -> "nuzzle";
			default -> switch (species) {
				case "shroom" -> "dance";
				case "raven" -> "bow";
				default -> "salute";
			};
		};
	}
}
