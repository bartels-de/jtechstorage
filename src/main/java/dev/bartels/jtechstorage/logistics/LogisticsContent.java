package dev.bartels.jtechstorage.logistics;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;

import dev.bartels.jtechstorage.JTechStorage;

/** Logistik (v0.40): Kristall-Förderbänder und Sortier-Weiche. */
public final class LogisticsContent {
	private static final List<Item> CREATIVE = new ArrayList<>();

	public static final Block CONVEYOR = block("crystal_conveyor", new ConveyorBlock(0.11,
			props("crystal_conveyor").strength(1.5f).noOcclusion()));
	public static final Block FAST_CONVEYOR = block("fast_crystal_conveyor", new ConveyorBlock(0.22,
			props("fast_crystal_conveyor").strength(1.5f).noOcclusion().lightLevel(state -> 2)));
	public static final Block SORTER = block("crystal_sorter", new SorterBlock(props("crystal_sorter").strength(2.5f).lightLevel(state -> 3)));
	public static final BlockEntityType<SorterBlockEntity> SORTER_BLOCK_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
			JTechStorage.id("crystal_sorter"), FabricBlockEntityTypeBuilder.create(SorterBlockEntity::new, SORTER).build());
	public static final MenuType<SorterMenu> SORTER_MENU = Registry.register(BuiltInRegistries.MENU, JTechStorage.id("crystal_sorter"),
			new MenuType<>(SorterMenu::new, FeatureFlags.VANILLA_SET));

	private LogisticsContent() {
	}

	private static BlockBehaviour.Properties props(String name) {
		return BlockBehaviour.Properties.ofFullCopy(Blocks.POLISHED_DEEPSLATE).setId(ResourceKey.create(Registries.BLOCK, JTechStorage.id(name)));
	}

	private static Block block(String name, Block block) {
		ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, JTechStorage.id(name));
		Registry.register(BuiltInRegistries.BLOCK, key, block);
		CREATIVE.add(Registry.register(BuiltInRegistries.ITEM, JTechStorage.id(name), new BlockItem(block, new Item.Properties()
				.useBlockDescriptionPrefix().setId(ResourceKey.create(Registries.ITEM, key.identifier())))));
		return block;
	}

	public static List<Item> creativeItems() {
		return CREATIVE;
	}

	public static void init() {
		// lädt die statischen Registrierungen
	}
}
