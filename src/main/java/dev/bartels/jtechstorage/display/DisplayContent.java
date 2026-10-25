package dev.bartels.jtechstorage.display;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;

import dev.bartels.jtechstorage.JTechStorage;

/** Lager-Bildschirm und Lager-Banner (v0.34). */
public final class DisplayContent {
	private static final List<Item> CREATIVE = new ArrayList<>();

	public static final Block STORAGE_DISPLAY = block("storage_display", StorageDisplayBlock::new,
			BlockBehaviour.Properties.ofFullCopy(Blocks.POLISHED_DEEPSLATE).strength(1.5f).noOcclusion().lightLevel(state -> 4).sound(SoundType.AMETHYST));
	public static final Block STORAGE_BANNER = block("storage_banner", StorageBannerBlock::new,
			BlockBehaviour.Properties.of().ignitedByLava().strength(1.0f).noOcclusion().noCollision().sound(SoundType.WOOL));

	public static final BlockEntityType<StorageDisplayBlockEntity> STORAGE_DISPLAY_BLOCK_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
			JTechStorage.id("storage_display"), FabricBlockEntityTypeBuilder.create(StorageDisplayBlockEntity::new, STORAGE_DISPLAY).build());
	public static final BlockEntityType<StorageBannerBlockEntity> STORAGE_BANNER_BLOCK_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
			JTechStorage.id("storage_banner"), FabricBlockEntityTypeBuilder.create(StorageBannerBlockEntity::new, STORAGE_BANNER).build());

	private DisplayContent() {
	}

	private static Block block(String name, java.util.function.Function<BlockBehaviour.Properties, Block> factory, BlockBehaviour.Properties properties) {
		ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, JTechStorage.id(name));
		Block block = Registry.register(BuiltInRegistries.BLOCK, key, factory.apply(properties.setId(key)));
		Item item = Registry.register(BuiltInRegistries.ITEM, JTechStorage.id(name), new BlockItem(block, new Item.Properties()
				.useBlockDescriptionPrefix().setId(ResourceKey.create(Registries.ITEM, key.identifier()))));
		CREATIVE.add(item);
		return block;
	}

	public static List<Item> creativeItems() {
		return CREATIVE;
	}

	public static void init() {
		PanelActions.init();
	}
}
