package dev.bartels.jtechstorage.depths;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.Heightmap;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;

import dev.bartels.jtechstorage.JTechStorage;
import dev.bartels.jtechstorage.depths.entity.CrystalCrab;
import dev.bartels.jtechstorage.depths.entity.CrystalDeer;
import dev.bartels.jtechstorage.depths.entity.GeodeSheep;

/** Neue Tiere der Kristalltiefe (v0.36): Kristallhirsch, Geodenschaf, Kristallkrebs – und Kristallwolle in 16 Farben. */
public final class DepthsAnimals {
	private static final List<Item> CREATIVE = new ArrayList<>();
	private static final Map<DyeColor, Block> WOOL = new EnumMap<>(DyeColor.class);

	public static final EntityType<CrystalDeer> CRYSTAL_DEER = entity("crystal_deer",
			EntityType.Builder.of(CrystalDeer::new, MobCategory.CREATURE).sized(1.3f, 1.9f).eyeHeight(1.75f).passengerAttachments(1.32f)
					.clientTrackingRange(10));
	public static final EntityType<GeodeSheep> GEODE_SHEEP = entity("geode_sheep",
			EntityType.Builder.of(GeodeSheep::new, MobCategory.CREATURE).sized(0.9f, 1.3f).eyeHeight(1.1f).clientTrackingRange(10));
	public static final EntityType<CrystalCrab> CRYSTAL_CRAB = entity("crystal_crab",
			EntityType.Builder.of(CrystalCrab::new, MobCategory.CREATURE).sized(0.8f, 0.55f).clientTrackingRange(8));

	static {
		for (DyeColor color : DyeColor.values()) {
			String name = color.getName() + "_crystal_wool";
			ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, JTechStorage.id(name));
			Block block = Registry.register(BuiltInRegistries.BLOCK, key, new Block(BlockBehaviour.Properties.of().mapColor(color)
					.strength(0.8f).sound(SoundType.WOOL).ignitedByLava().lightLevel(state -> 2).setId(key)));
			Item item = Registry.register(BuiltInRegistries.ITEM, JTechStorage.id(name), new BlockItem(block, new Item.Properties()
					.useBlockDescriptionPrefix().setId(ResourceKey.create(Registries.ITEM, key.identifier()))));
			WOOL.put(color, block);
			CREATIVE.add(item);
		}

		egg("crystal_deer", CRYSTAL_DEER);
		egg("geode_sheep", GEODE_SHEEP);
		egg("crystal_crab", CRYSTAL_CRAB);
	}

	private DepthsAnimals() {
	}

	/** Kristallwolle in der Farbe {@code color}. */
	public static Block crystalWool(DyeColor color) {
		return WOOL.get(color);
	}

	private static void egg(String name, EntityType<?> type) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, JTechStorage.id(name + "_spawn_egg"));
		CREATIVE.add(Registry.register(BuiltInRegistries.ITEM, key, new net.minecraft.world.item.SpawnEggItem(new Item.Properties()
				.spawnEgg(type).setId(key))));
	}

	private static <T extends Entity> EntityType<T> entity(String name, EntityType.Builder<T> builder) {
		ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, JTechStorage.id(name));
		return Registry.register(BuiltInRegistries.ENTITY_TYPE, key, builder.build(key));
	}

	public static List<Item> creativeItems() {
		return CREATIVE;
	}

	public static void init() {
		DepthsAnimalSpawner.init();
		FabricDefaultAttributeRegistry.register(CRYSTAL_DEER, CrystalDeer.createAttributes());
		FabricDefaultAttributeRegistry.register(GEODE_SHEEP, GeodeSheep.createAttributes());
		FabricDefaultAttributeRegistry.register(CRYSTAL_CRAB, CrystalCrab.createAttributes());
		SpawnPlacements.register(CRYSTAL_DEER, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, CrystalDeer::checkSpawn);
		SpawnPlacements.register(GEODE_SHEEP, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, GeodeSheep::checkSpawn);
		SpawnPlacements.register(CRYSTAL_CRAB, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, CrystalCrab::checkSpawn);
	}
}
