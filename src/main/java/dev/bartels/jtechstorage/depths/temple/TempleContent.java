package dev.bartels.jtechstorage.depths.temple;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;

import dev.bartels.jtechstorage.JTechStorage;
import dev.bartels.jtechstorage.end.EndContent;

/** Tempel der Lagermeister (v0.39): Boss, Siegel und die Beute (Hammer). */
public final class TempleContent {
	private static final List<Item> CREATIVE = new ArrayList<>();

	public static final EntityType<LastMaster> LAST_MASTER;
	public static final Block MASTER_SEAL;
	public static final Item MASTER_HAMMER;

	static {
		ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, JTechStorage.id("last_master"));
		LAST_MASTER = Registry.register(BuiltInRegistries.ENTITY_TYPE, key, EntityType.Builder.of(LastMaster::new, MobCategory.MONSTER)
				.sized(1.1f, 3.0f).eyeHeight(2.6f).fireImmune().clientTrackingRange(12).build(key));

		ResourceKey<Block> sealKey = ResourceKey.create(Registries.BLOCK, JTechStorage.id("master_seal"));
		MASTER_SEAL = Registry.register(BuiltInRegistries.BLOCK, sealKey, new MasterSealBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OBSIDIAN)
				.strength(-1.0f, 3600000.0f).noLootTable().lightLevel(state -> 7).setId(sealKey)));
		CREATIVE.add(Registry.register(BuiltInRegistries.ITEM, JTechStorage.id("master_seal"), new BlockItem(MASTER_SEAL, new Item.Properties()
				.useBlockDescriptionPrefix().rarity(Rarity.EPIC).setId(ResourceKey.create(Registries.ITEM, sealKey.identifier())))));

		ResourceKey<Item> hammerKey = ResourceKey.create(Registries.ITEM, JTechStorage.id("master_hammer"));
		MASTER_HAMMER = Registry.register(BuiltInRegistries.ITEM, hammerKey, new MasterHammerItem(new Item.Properties()
				.pickaxe(EndContent.AMETHYST_OBSIDIAN_TOOL, 5.0f, -3.0f).fireResistant().rarity(Rarity.EPIC).setId(hammerKey)));
		CREATIVE.add(MASTER_HAMMER);
	}

	private TempleContent() {
	}

	public static List<Item> creativeItems() {
		return CREATIVE;
	}

	public static void init() {
		FabricDefaultAttributeRegistry.register(LAST_MASTER, LastMaster.createAttributes());
		MasterHammerItem.init();
	}
}
