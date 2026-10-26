package dev.bartels.jtechstorage.chest;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;

import dev.bartels.jtechstorage.JTechStorage;

/** Amethyst chests (v0.28.0): Amethyst, Crystal and Resonance chest. */
public final class ChestContent {
	private static final List<Item> CREATIVE = new ArrayList<>();
	private static final Map<AmethystChestTier, AmethystChestBlock> BLOCKS = new EnumMap<>(AmethystChestTier.class);
	private static final Map<AmethystChestTier, Item> ITEMS = new EnumMap<>(AmethystChestTier.class);
	private static final Map<AmethystChestTier, MenuType<AmethystChestMenu>> MENUS = new EnumMap<>(AmethystChestTier.class);

	static {
		for (AmethystChestTier tier : AmethystChestTier.values()) {
			ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, JTechStorage.id(tier.id));
			int light = tier == AmethystChestTier.RESONANCE ? 4 : 0;
			AmethystChestBlock block = new AmethystChestBlock(tier, BlockBehaviour.Properties.ofFullCopy(Blocks.POLISHED_DEEPSLATE)
					.strength(3.0f, 6.0f).lightLevel(state -> light).setId(key));
			Item item = new BlockItem(block, new Item.Properties().useBlockDescriptionPrefix()
					.setId(ResourceKey.create(Registries.ITEM, key.identifier())));
			BLOCKS.put(tier, block);
			ITEMS.put(tier, item);
			MENUS.put(tier, AmethystChestMenu.type(tier));
			CREATIVE.add(item);
		}
	}

	public static final BlockEntityType<AmethystChestBlockEntity> AMETHYST_CHEST_BLOCK_ENTITY =
			FabricBlockEntityTypeBuilder.create(AmethystChestBlockEntity::new, BLOCKS.values().toArray(new Block[0])).build();

	private ChestContent() {
	}

	public static AmethystChestBlock block(AmethystChestTier tier) {
		return BLOCKS.get(tier);
	}

	public static MenuType<AmethystChestMenu> menuType(AmethystChestTier tier) {
		return MENUS.get(tier);
	}

	public static List<Item> creativeItems() {
		return CREATIVE;
	}

	public static void init() {
		for (AmethystChestTier tier : AmethystChestTier.values()) {
			Registry.register(BuiltInRegistries.BLOCK, JTechStorage.id(tier.id), BLOCKS.get(tier));
			Registry.register(BuiltInRegistries.ITEM, JTechStorage.id(tier.id), ITEMS.get(tier));
			Registry.register(BuiltInRegistries.MENU, JTechStorage.id(tier.id), MENUS.get(tier));
		}

		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, JTechStorage.id("amethyst_chest"), AMETHYST_CHEST_BLOCK_ENTITY);
	}
}
