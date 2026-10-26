package dev.bartels.jtechstorage.entity;

import java.util.Locale;
import java.util.Set;

import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;

import net.fabricmc.fabric.api.tag.convention.v2.ConventionalItemTags;

/**
 * Categories the Amethyst Sprite sorts by. The order of checks in {@link #of} matters
 * (e.g. armor before tools, redstone dust is "Redstone", concrete powder is "Decoration & Colors" and not "Earth").
 * The enum order also determines the order when tidying within a chest.
 */
public enum SortCategory {
	FOOD,
	MAGIC,
	ARMOR,
	TOOLS,
	REDSTONE,
	ORES,
	METALS,
	DECOR,
	FARMING,
	PLANTS,
	WOOD,
	MOB_DROPS,
	EARTH,
	STONE,
	OTHER;

	private static final Set<Item> REDSTONE_ITEMS = Set.of(
			Items.REDSTONE, Items.REDSTONE_BLOCK, Items.REDSTONE_TORCH, Items.REPEATER, Items.COMPARATOR,
			Items.PISTON, Items.STICKY_PISTON, Items.OBSERVER, Items.HOPPER, Items.DROPPER, Items.DISPENSER,
			Items.LEVER, Items.DAYLIGHT_DETECTOR, Items.TARGET, Items.TRIPWIRE_HOOK, Items.NOTE_BLOCK,
			Items.TNT, Items.REDSTONE_LAMP, Items.SCULK_SENSOR, Items.CALIBRATED_SCULK_SENSOR,
			Items.CRAFTER, Items.SLIME_BLOCK, Items.HONEY_BLOCK);

	private static final Set<Item> MAGIC_ITEMS = Set.of(
			Items.ENCHANTED_BOOK, Items.EXPERIENCE_BOTTLE, Items.ENDER_EYE, Items.TOTEM_OF_UNDYING, Items.GLASS_BOTTLE,
			Items.BREWING_STAND, Items.ENCHANTING_TABLE, Items.NETHER_STAR, Items.END_CRYSTAL, Items.DRAGON_BREATH,
			Items.BOOK, Items.WRITABLE_BOOK, Items.WRITTEN_BOOK, Items.HEART_OF_THE_SEA, Items.CONDUIT);

	private static final Set<Item> FARM_ITEMS = Set.of(
			Items.WHEAT, Items.SUGAR_CANE, Items.BAMBOO, Items.CACTUS, Items.PUMPKIN, Items.CARVED_PUMPKIN, Items.MELON,
			Items.HAY_BLOCK, Items.COCOA_BEANS, Items.NETHER_WART, Items.SWEET_BERRIES, Items.GLOW_BERRIES, Items.BONE_MEAL,
			Items.MELON_SLICE, Items.EGG);

	private static final Set<Item> PLANT_ITEMS = Set.of(
			Items.KELP, Items.VINE, Items.LILY_PAD, Items.SHORT_GRASS, Items.TALL_GRASS, Items.FERN, Items.LARGE_FERN,
			Items.MOSS_BLOCK, Items.MOSS_CARPET, Items.SEAGRASS, Items.SEA_PICKLE, Items.DEAD_BUSH);

	private static final Set<Item> MOB_DROP_ITEMS = Set.of(
			Items.SPIDER_EYE, Items.ROTTEN_FLESH, Items.GHAST_TEAR, Items.PHANTOM_MEMBRANE, Items.INK_SAC,
			Items.GLOW_INK_SAC, Items.PRISMARINE_SHARD, Items.PRISMARINE_CRYSTALS, Items.SHULKER_SHELL,
			Items.RABBIT_HIDE, Items.RABBIT_FOOT, Items.BLAZE_POWDER, Items.MAGMA_CREAM, Items.HONEYCOMB,
			Items.ARMADILLO_SCUTE, Items.TURTLE_SCUTE, Items.NAUTILUS_SHELL, Items.ECHO_SHARD, Items.FERMENTED_SPIDER_EYE);

	/** Translation key, e.g. for status messages. */
	public String translationKey() {
		return "jtechstorage.category." + name().toLowerCase(Locale.ROOT);
	}

	public static SortCategory of(ItemStack stack) {
		Item item = stack.getItem();
		BlockState block = item instanceof BlockItem blockItem ? blockItem.getBlock().defaultBlockState() : null;

		if (stack.has(DataComponents.FOOD) || stack.is(ConventionalItemTags.FOODS)) {
			return FOOD;
		}

		if (MAGIC_ITEMS.contains(item) || stack.has(DataComponents.POTION_CONTENTS) || stack.is(ConventionalItemTags.POTIONS)) {
			return MAGIC;
		}

		if (stack.is(ConventionalItemTags.ARMORS) || stack.has(DataComponents.EQUIPPABLE) && stack.isDamageableItem()) {
			return ARMOR;
		}

		if (stack.isDamageableItem() || stack.is(ConventionalItemTags.TOOLS) || stack.is(ItemTags.ARROWS)) {
			return TOOLS;
		}

		if (REDSTONE_ITEMS.contains(item) || stack.is(ItemTags.RAILS)) {
			return REDSTONE;
		}

		if (stack.is(ConventionalItemTags.ORES) || stack.is(ConventionalItemTags.RAW_MATERIALS)
				|| stack.is(ConventionalItemTags.DUSTS) || item == Items.COAL || item == Items.CHARCOAL
				|| item == Items.RAW_IRON_BLOCK || item == Items.RAW_GOLD_BLOCK || item == Items.RAW_COPPER_BLOCK) {
			return ORES;
		}

		if (stack.is(ConventionalItemTags.INGOTS) || stack.is(ConventionalItemTags.NUGGETS)
				|| stack.is(ConventionalItemTags.GEMS) || stack.is(ConventionalItemTags.STORAGE_BLOCKS)) {
			return METALS;
		}

		if (stack.is(ItemTags.WOOL) || stack.is(ItemTags.WOOL_CARPETS) || stack.is(ConventionalItemTags.DYES)
				|| stack.is(ConventionalItemTags.GLASS_BLOCKS) || stack.is(ConventionalItemTags.GLASS_PANES)
				|| stack.is(ConventionalItemTags.CONCRETES) || stack.is(ConventionalItemTags.CONCRETE_POWDERS)
				|| stack.is(ItemTags.TERRACOTTA) || stack.is(ConventionalItemTags.GLAZED_TERRACOTTAS)
				|| stack.is(ItemTags.BANNERS) || stack.is(ItemTags.CANDLES) || stack.is(ItemTags.BEDS)) {
			return DECOR;
		}

		if (stack.is(ConventionalItemTags.SEEDS) || stack.is(ConventionalItemTags.CROPS) || FARM_ITEMS.contains(item)) {
			return FARMING;
		}

		if (stack.is(ItemTags.SAPLINGS) || stack.is(ItemTags.LEAVES) || stack.is(ConventionalItemTags.FLOWERS)
				|| stack.is(ConventionalItemTags.MUSHROOMS) || PLANT_ITEMS.contains(item)
				|| (block != null && block.is(BlockTags.MINEABLE_WITH_HOE))) {
			return PLANTS;
		}

		if (stack.is(ItemTags.LOGS) || stack.is(ItemTags.PLANKS) || stack.is(ItemTags.WOODEN_SLABS)
				|| stack.is(ItemTags.WOODEN_STAIRS) || stack.is(ItemTags.WOODEN_FENCES) || stack.is(ItemTags.FENCE_GATES)
				|| stack.is(ItemTags.WOODEN_DOORS) || stack.is(ItemTags.WOODEN_TRAPDOORS) || stack.is(ItemTags.WOODEN_BUTTONS)
				|| stack.is(ItemTags.WOODEN_PRESSURE_PLATES) || stack.is(ItemTags.SIGNS) || stack.is(ItemTags.HANGING_SIGNS)
				|| stack.is(ItemTags.BOATS) || item == Items.STICK
				|| (block != null && block.is(BlockTags.MINEABLE_WITH_AXE))) {
			return WOOD;
		}

		if (stack.is(ConventionalItemTags.STRINGS) || stack.is(ConventionalItemTags.LEATHERS)
				|| stack.is(ConventionalItemTags.BONES) || stack.is(ConventionalItemTags.EGGS)
				|| stack.is(ConventionalItemTags.FEATHERS) || stack.is(ConventionalItemTags.GUNPOWDERS)
				|| stack.is(ConventionalItemTags.RODS) || stack.is(ConventionalItemTags.ENDER_PEARLS)
				|| stack.is(ConventionalItemTags.SLIME_BALLS) || MOB_DROP_ITEMS.contains(item)) {
			return MOB_DROPS;
		}

		if (stack.is(ConventionalItemTags.GRAVELS) || stack.is(ConventionalItemTags.SANDS) || stack.is(ItemTags.DIRT)
				|| item == Items.CLAY || item == Items.CLAY_BALL || item == Items.SNOWBALL || item == Items.SNOW_BLOCK
				|| (block != null && block.is(BlockTags.MINEABLE_WITH_SHOVEL))) {
			return EARTH;
		}

		if (stack.is(ConventionalItemTags.STONES) || stack.is(ConventionalItemTags.COBBLESTONES)
				|| stack.is(ItemTags.STONE_CRAFTING_MATERIALS) || (block != null && block.is(BlockTags.MINEABLE_WITH_PICKAXE))) {
			return STONE;
		}

		return OTHER;
	}
}
