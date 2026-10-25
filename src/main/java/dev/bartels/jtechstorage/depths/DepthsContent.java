package dev.bartels.jtechstorage.depths;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TransparentBlock;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.material.PushReaction;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;

import dev.bartels.jtechstorage.JTechStorage;
import dev.bartels.jtechstorage.depths.entity.CrystalLurker;
import dev.bartels.jtechstorage.depths.entity.CrystalSnail;
import dev.bartels.jtechstorage.depths.entity.GlowFish;
import dev.bartels.jtechstorage.depths.entity.LakeWraith;
import dev.bartels.jtechstorage.depths.entity.SwarmShard;
import dev.bartels.jtechstorage.depths.entity.VeinCrawler;
import dev.bartels.jtechstorage.depths.feature.ChasmLavaFeature;
import dev.bartels.jtechstorage.depths.feature.CrystalScatterFeature;
import dev.bartels.jtechstorage.depths.feature.CrystalSpireFeature;
import dev.bartels.jtechstorage.depths.feature.FloatingShardFeature;
import dev.bartels.jtechstorage.depths.feature.HangingCrystalFeature;
import dev.bartels.jtechstorage.depths.feature.LavaFallFeature;
import dev.bartels.jtechstorage.depths.feature.SproutPatchFeature;
import dev.bartels.jtechstorage.depths.feature.VeinFeature;

/**
 * Kristalltiefe (v0.30): eigene Dimension mit drei Biomen (Geodenhallen, Kristallseen, Aderschlucht), neuen Blöcken
 * und dem Amethyst-Portal. Gelände, Biome und Platzierung stehen als Daten unter {@code data/jtechstorage/worldgen},
 * die besonderen Gebilde (Kristallsäulen, schwebende Splitter …) sind eigene Feature-Typen.
 */
public final class DepthsContent {
	public static final ResourceKey<Level> DEPTHS = ResourceKey.create(Registries.DIMENSION, JTechStorage.id("crystal_depths"));
	public static final ResourceKey<PoiType> PORTAL_POI = ResourceKey.create(Registries.POINT_OF_INTEREST_TYPE, JTechStorage.id("depths_portal"));

	private static final List<Item> CREATIVE = new ArrayList<>();

	public static final Block CRYSTAL_STONE = block("crystal_stone", Block::new,
			BlockBehaviour.Properties.ofFullCopy(Blocks.DEEPSLATE).strength(2.5f, 6.0f).sound(SoundType.DEEPSLATE));
	public static final Block POLISHED_CRYSTAL_STONE = block("polished_crystal_stone", Block::new,
			BlockBehaviour.Properties.ofFullCopy(Blocks.POLISHED_DEEPSLATE).strength(2.5f, 6.0f));
	public static final Block CRYSTAL_STONE_BRICKS = block("crystal_stone_bricks", Block::new,
			BlockBehaviour.Properties.ofFullCopy(Blocks.DEEPSLATE_BRICKS).strength(2.5f, 6.0f));
	public static final Block VEIN_STONE = block("vein_stone", Block::new,
			BlockBehaviour.Properties.ofFullCopy(Blocks.DEEPSLATE).strength(3.0f, 6.0f));
	public static final Block GLOW_CRYSTAL = block("glow_crystal", Block::new,
			BlockBehaviour.Properties.ofFullCopy(Blocks.AMETHYST_BLOCK).strength(1.5f).lightLevel(state -> 12).sound(SoundType.AMETHYST));
	public static final Block RESONANCE_MOSS = block("resonance_moss", Block::new,
			BlockBehaviour.Properties.ofFullCopy(Blocks.MOSS_BLOCK).strength(1.0f).sound(SoundType.MOSS));
	public static final Block CRYSTAL_SPROUT = block("crystal_sprout", CrystalSproutBlock::new,
			BlockBehaviour.Properties.of().noCollision().instabreak().noOcclusion().lightLevel(state -> 4).sound(SoundType.SMALL_AMETHYST_BUD)
					.offsetType(BlockBehaviour.OffsetType.XZ).pushReaction(PushReaction.POPPED).replaceable());
	// ------------------------------------------------------------------ Teil 3 (v0.32): Erz, Fundstücke, Baublöcke
	public static final Block PURE_AMETHYST_ORE = block("pure_amethyst_ore", p -> new DropExperienceBlock(UniformInt.of(3, 7), p),
			BlockBehaviour.Properties.ofFullCopy(Blocks.DEEPSLATE_DIAMOND_ORE).strength(4.5f, 6.0f).lightLevel(state -> 1).requiresCorrectToolForDrops());
	public static final Block VEIN_PURE_AMETHYST_ORE = block("vein_pure_amethyst_ore", p -> new DropExperienceBlock(UniformInt.of(4, 8), p),
			BlockBehaviour.Properties.ofFullCopy(Blocks.DEEPSLATE_DIAMOND_ORE).strength(5.0f, 6.0f).lightLevel(state -> 2).requiresCorrectToolForDrops());
	public static final Block PURE_AMETHYST_BLOCK = block("pure_amethyst_block", Block::new,
			BlockBehaviour.Properties.ofFullCopy(Blocks.AMETHYST_BLOCK).strength(5.0f, 6.0f).lightLevel(state -> 6).sound(SoundType.AMETHYST).requiresCorrectToolForDrops());
	public static final Block CRACKED_CRYSTAL_STONE_BRICKS = block("cracked_crystal_stone_bricks", Block::new,
			BlockBehaviour.Properties.ofFullCopy(Blocks.CRACKED_DEEPSLATE_BRICKS).strength(2.5f, 6.0f));
	public static final Block CHISELED_CRYSTAL_STONE = block("chiseled_crystal_stone", Block::new,
			BlockBehaviour.Properties.ofFullCopy(Blocks.CHISELED_DEEPSLATE).strength(2.5f, 6.0f));
	public static final Block CRYSTAL_STONE_BRICK_STAIRS = block("crystal_stone_brick_stairs", p -> new StairBlock(CRYSTAL_STONE_BRICKS.defaultBlockState(), p),
			BlockBehaviour.Properties.ofFullCopy(Blocks.DEEPSLATE_BRICK_STAIRS).strength(2.5f, 6.0f));
	public static final Block CRYSTAL_STONE_BRICK_SLAB = block("crystal_stone_brick_slab", SlabBlock::new,
			BlockBehaviour.Properties.ofFullCopy(Blocks.DEEPSLATE_BRICK_SLAB).strength(2.5f, 6.0f));
	public static final Block CRYSTAL_GLASS = block("crystal_glass", TransparentBlock::new,
			BlockBehaviour.Properties.ofFullCopy(Blocks.TINTED_GLASS).strength(0.6f).sound(SoundType.AMETHYST));
	public static final Block CRYSTAL_LANTERN = block("crystal_lantern", LanternBlock::new,
			BlockBehaviour.Properties.ofFullCopy(Blocks.LANTERN).lightLevel(state -> 15).sound(SoundType.AMETHYST_CLUSTER));
	public static final Block BROKEN_TERMINAL = block("broken_terminal", BrokenTerminalBlock::new,
			BlockBehaviour.Properties.ofFullCopy(Blocks.POLISHED_DEEPSLATE).strength(3.0f, 6.0f).lightLevel(state -> 1));
	public static final Block RESONANCE_SHROOM = block("resonance_shroom", CrystalSproutBlock::new,
			BlockBehaviour.Properties.of().noCollision().instabreak().noOcclusion().lightLevel(state -> 5).sound(SoundType.FUNGUS)
					.offsetType(BlockBehaviour.OffsetType.XZ).pushReaction(PushReaction.POPPED).replaceable());
	public static final Block CRYSTAL_VINES = block("crystal_vines", CrystalVinesBlock::new,
			BlockBehaviour.Properties.of().noCollision().instabreak().noOcclusion().lightLevel(state -> 3).sound(SoundType.CAVE_VINES)
					.pushReaction(PushReaction.POPPED));
	// ------------------------------------------------------------------ Überarbeitung (v0.33): Farbe und neue Biome
	public static final Block AZURE_CRYSTAL = block("azure_crystal", p -> new SingingCrystalBlock(1.6f, p), crystalProps());
	public static final Block ROSE_CRYSTAL = block("rose_crystal", p -> new SingingCrystalBlock(1.0f, p), crystalProps());
	public static final Block CITRINE_CRYSTAL = block("citrine_crystal", p -> new SingingCrystalBlock(0.7f, p), crystalProps());
	public static final Block AZURE_CLUSTER = block("azure_cluster", p -> new net.minecraft.world.level.block.AmethystClusterBlock(7.0f, 3.0f, p), clusterProps());
	public static final Block ROSE_CLUSTER = block("rose_cluster", p -> new net.minecraft.world.level.block.AmethystClusterBlock(7.0f, 3.0f, p), clusterProps());
	public static final Block CITRINE_CLUSTER = block("citrine_cluster", p -> new net.minecraft.world.level.block.AmethystClusterBlock(7.0f, 3.0f, p), clusterProps());
	public static final Block CRYSTAL_TURF = block("crystal_turf", Block::new,
			BlockBehaviour.Properties.ofFullCopy(Blocks.DEEPSLATE).strength(2.0f, 6.0f).sound(SoundType.AMETHYST));
	public static final Block CRYSTAL_GRASS = block("crystal_grass", CrystalSproutBlock::new,
			BlockBehaviour.Properties.of().noCollision().instabreak().noOcclusion().sound(SoundType.SMALL_AMETHYST_BUD)
					.offsetType(BlockBehaviour.OffsetType.XZ).pushReaction(PushReaction.POPPED).replaceable());
	public static final Block RESONANCE_CAP = block("resonance_cap", ResonanceCapBlock::new,
			BlockBehaviour.Properties.ofFullCopy(Blocks.RED_MUSHROOM_BLOCK).strength(0.8f).lightLevel(state -> 6).sound(SoundType.FUNGUS)
					.bounceRestitution(0.85f).fallDistanceReduction(1.0f));
	public static final Block RESONANCE_STEM = block("resonance_stem", net.minecraft.world.level.block.RotatedPillarBlock::new,
			BlockBehaviour.Properties.ofFullCopy(Blocks.MUSHROOM_STEM).strength(1.0f).sound(SoundType.STEM));
	public static final Block ROSE_MOSS = block("rose_moss", Block::new,
			BlockBehaviour.Properties.ofFullCopy(Blocks.MOSS_BLOCK).strength(1.0f).sound(SoundType.MOSS));
	public static final Block ROSE_BLOOM = block("rose_bloom", CrystalSproutBlock::new,
			BlockBehaviour.Properties.of().noCollision().instabreak().noOcclusion().lightLevel(state -> 3).sound(SoundType.SMALL_AMETHYST_BUD)
					.offsetType(BlockBehaviour.OffsetType.XZ).pushReaction(PushReaction.POPPED).replaceable());
	public static final Block FROST_CRYSTAL = block("frost_crystal", Block::new,
			BlockBehaviour.Properties.ofFullCopy(Blocks.PACKED_ICE).strength(1.0f).sound(SoundType.AMETHYST));
	public static final Block CITRINE_SAND = block("citrine_sand", p -> new net.minecraft.world.level.block.ColoredFallingBlock(new net.minecraft.util.ColorRGBA(0xFFD8A030), p),
			BlockBehaviour.Properties.ofFullCopy(Blocks.SAND));
	public static final Block CITRINE_SANDSTONE = block("citrine_sandstone", Block::new,
			BlockBehaviour.Properties.ofFullCopy(Blocks.SANDSTONE));

	public static final DataComponentType<Integer> NOTE = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, JTechStorage.id("note"),
			DataComponentType.<Integer>builder().persistent(com.mojang.serialization.Codec.INT)
					.networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.VAR_INT).build());
	/** v0.38: am Lager-Schlüssel gemerktes Amethyst-Portal (zum Verknüpfen mit einem Portal der anderen Dimension). */
	public static final DataComponentType<net.minecraft.core.GlobalPos> PORTAL_MARK = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,
			JTechStorage.id("portal_mark"), DataComponentType.<net.minecraft.core.GlobalPos>builder().persistent(net.minecraft.core.GlobalPos.CODEC)
					.networkSynchronized(net.minecraft.core.GlobalPos.STREAM_CODEC).build());
	public static final Item RAW_PURE_AMETHYST = item("raw_pure_amethyst", Item::new, new Item.Properties().rarity(Rarity.UNCOMMON));
	public static final Item PURE_AMETHYST = item("pure_amethyst", Item::new, new Item.Properties().rarity(Rarity.RARE));
	public static final Item ANCIENT_NOTE = item("ancient_note", AncientNoteItem::new, new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));

	public static final DepthsPortalBlock DEPTHS_PORTAL = Registry.register(BuiltInRegistries.BLOCK, JTechStorage.id("depths_portal"),
			new DepthsPortalBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.NETHER_PORTAL).lightLevel(state -> 11).noLootTable()
					.setId(ResourceKey.create(Registries.BLOCK, JTechStorage.id("depths_portal")))));

	// ------------------------------------------------------------------ Bewohner (v0.31)
	public static final EntityType<CrystalLurker> CRYSTAL_LURKER = entity("crystal_lurker",
			EntityType.Builder.of(CrystalLurker::new, MobCategory.MONSTER).sized(1.3f, 2.0f).clientTrackingRange(10));
	public static final EntityType<SwarmShard> SWARM_SHARD = entity("swarm_shard",
			EntityType.Builder.of(SwarmShard::new, MobCategory.MONSTER).sized(0.5f, 0.7f).clientTrackingRange(8));
	public static final EntityType<LakeWraith> LAKE_WRAITH = entity("lake_wraith",
			EntityType.Builder.of(LakeWraith::new, MobCategory.MONSTER).sized(1.1f, 1.5f).clientTrackingRange(10));
	public static final EntityType<VeinCrawler> VEIN_CRAWLER = entity("vein_crawler",
			EntityType.Builder.of(VeinCrawler::new, MobCategory.MONSTER).sized(1.3f, 0.8f).fireImmune().clientTrackingRange(10));
	public static final EntityType<CrystalSnail> CRYSTAL_SNAIL = entity("crystal_snail",
			EntityType.Builder.of(CrystalSnail::new, MobCategory.CREATURE).sized(0.6f, 0.6f).clientTrackingRange(8));
	public static final EntityType<GlowFish> GLOW_FISH = entity("glow_fish",
			EntityType.Builder.of(GlowFish::new, MobCategory.WATER_AMBIENT).sized(0.5f, 0.4f).clientTrackingRange(4));
	public static final EntityType<dev.bartels.jtechstorage.depths.entity.Quokka> QUOKKA = entity("quokka",
			EntityType.Builder.of(dev.bartels.jtechstorage.depths.entity.Quokka::new, MobCategory.CREATURE).sized(0.6f, 0.75f).clientTrackingRange(8));

	static {
		egg("crystal_lurker", CRYSTAL_LURKER);
		egg("swarm_shard", SWARM_SHARD);
		egg("lake_wraith", LAKE_WRAITH);
		egg("vein_crawler", VEIN_CRAWLER);
		egg("crystal_snail", CRYSTAL_SNAIL);
		egg("glow_fish", GLOW_FISH);
		egg("quokka", QUOKKA);
	}

	private DepthsContent() {
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

	private static BlockBehaviour.Properties crystalProps() {
		return BlockBehaviour.Properties.ofFullCopy(Blocks.AMETHYST_BLOCK).strength(1.5f).lightLevel(state -> 6).sound(SoundType.AMETHYST);
	}

	private static BlockBehaviour.Properties clusterProps() {
		return BlockBehaviour.Properties.ofFullCopy(Blocks.AMETHYST_CLUSTER).lightLevel(state -> 3);
	}

	private static Block block(String name, Function<BlockBehaviour.Properties, Block> factory, BlockBehaviour.Properties properties) {
		ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, JTechStorage.id(name));
		Block block = Registry.register(BuiltInRegistries.BLOCK, key, factory.apply(properties.setId(key)));
		Item item = Registry.register(BuiltInRegistries.ITEM, JTechStorage.id(name), new BlockItem(block, new Item.Properties()
				.useBlockDescriptionPrefix().setId(ResourceKey.create(Registries.ITEM, key.identifier()))));
		CREATIVE.add(item);
		return block;
	}

	private static Item item(String name, Function<Item.Properties, Item> factory, Item.Properties properties) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, JTechStorage.id(name));
		Item item = Registry.register(BuiltInRegistries.ITEM, key, factory.apply(properties.setId(key)));
		CREATIVE.add(item);
		return item;
	}

	private static void feature(String name, MapCodec<? extends Feature> codec) {
		Registry.register(BuiltInRegistries.FEATURE_TYPE, JTechStorage.id(name), codec);
	}

	public static List<Item> creativeItems() {
		return CREATIVE;
	}

	public static void init() {
		feature("crystal_spire", CrystalSpireFeature.CODEC);
		feature("hanging_crystal", HangingCrystalFeature.CODEC);
		feature("floating_shard", FloatingShardFeature.CODEC);
		feature("crystal_scatter", CrystalScatterFeature.CODEC);
		feature("sprout_patch", SproutPatchFeature.CODEC);
		feature("vein", VeinFeature.CODEC);
		feature("lava_fall", LavaFallFeature.CODEC);
		feature("chasm_lava", ChasmLavaFeature.CODEC);
		feature("storage_ruin", dev.bartels.jtechstorage.depths.feature.StorageRuinFeature.CODEC);
		feature("crystal_shrine", dev.bartels.jtechstorage.depths.feature.CrystalShrineFeature.CODEC);
		feature("hanging_vines", dev.bartels.jtechstorage.depths.feature.HangingVinesFeature.CODEC);
		feature("shroom_patch", dev.bartels.jtechstorage.depths.feature.ShroomPatchFeature.CODEC);
		feature("prism_spire", dev.bartels.jtechstorage.depths.feature.PrismSpireFeature.CODEC);
		feature("stalactite", dev.bartels.jtechstorage.depths.feature.StalactiteFeature.CODEC);
		feature("giant_shroom", dev.bartels.jtechstorage.depths.feature.GiantShroomFeature.CODEC);
		feature("color_cluster", dev.bartels.jtechstorage.depths.feature.ColorClusterFeature.CODEC);
		feature("plant_patch", dev.bartels.jtechstorage.depths.feature.PlantPatchFeature.CODEC);
		feature("frozen_lake", dev.bartels.jtechstorage.depths.feature.FrozenLakeFeature.CODEC);
		feature("memory_crystal", dev.bartels.jtechstorage.depths.feature.MemoryCrystalFeature.CODEC);
		feature("biome_blend", dev.bartels.jtechstorage.depths.feature.BiomeBlendFeature.CODEC);
		// Vanilla-Registrierung (PoiHelper der Fabric-API ist zum Entfernen markiert)
		net.minecraft.world.entity.ai.village.poi.PoiTypes.register(net.minecraft.core.registries.BuiltInRegistries.POINT_OF_INTEREST_TYPE,
				PORTAL_POI, java.util.Set.copyOf(DEPTHS_PORTAL.getStateDefinition().getPossibleStates()), 0, 1);

		FabricDefaultAttributeRegistry.register(CRYSTAL_LURKER, CrystalLurker.createAttributes());
		FabricDefaultAttributeRegistry.register(SWARM_SHARD, SwarmShard.createAttributes());
		FabricDefaultAttributeRegistry.register(LAKE_WRAITH, LakeWraith.createAttributes());
		FabricDefaultAttributeRegistry.register(VEIN_CRAWLER, VeinCrawler.createAttributes());
		FabricDefaultAttributeRegistry.register(CRYSTAL_SNAIL, CrystalSnail.createAttributes());
		FabricDefaultAttributeRegistry.register(GLOW_FISH, GlowFish.createAttributes());
		FabricDefaultAttributeRegistry.register(QUOKKA, dev.bartels.jtechstorage.depths.entity.Quokka.createAttributes());
		SpawnPlacements.register(CRYSTAL_LURKER, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Monster::checkMonsterSpawnRules);
		SpawnPlacements.register(SWARM_SHARD, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Monster::checkMonsterSpawnRules);
		SpawnPlacements.register(VEIN_CRAWLER, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Monster::checkAnyLightMonsterSpawnRules);
		SpawnPlacements.register(LAKE_WRAITH, SpawnPlacementTypes.IN_WATER, Heightmap.Types.OCEAN_FLOOR, LakeWraith::checkSpawn);
		SpawnPlacements.register(CRYSTAL_SNAIL, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, CrystalSnail::checkSpawn);
		SpawnPlacements.register(GLOW_FISH, SpawnPlacementTypes.IN_WATER, Heightmap.Types.OCEAN_FLOOR, GlowFish::checkSpawn);
		SpawnPlacements.register(QUOKKA, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, dev.bartels.jtechstorage.depths.entity.Quokka::checkSpawn);
		DepthsPortals.init();
	}
}
