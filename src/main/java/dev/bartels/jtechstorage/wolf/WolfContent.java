package dev.bartels.jtechstorage.wolf;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.Heightmap;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;

import dev.bartels.jtechstorage.JTechStorage;
import dev.bartels.jtechstorage.ModAdvancements;
import dev.bartels.jtechstorage.ModContent;
import dev.bartels.jtechstorage.ModSounds;
import dev.bartels.jtechstorage.block.HelperBedBlock;
import dev.bartels.jtechstorage.entity.CrystalWolf;
import dev.bartels.jtechstorage.entity.MoonWolf;
import dev.bartels.jtechstorage.item.HelperSpawnItem;
import dev.bartels.jtechstorage.storm.StormContent;

/** Wölfe und Helfer-Zuhause (v0.26): Kristallwolf, Mondwolf, Amethyst-Knochen, Wolfskorb und Helferbett. */
public final class WolfContent {
	private static final List<Item> ITEMS = new ArrayList<>();

	public static final ResourceKey<EntityType<?>> CRYSTAL_WOLF_KEY = ResourceKey.create(Registries.ENTITY_TYPE, JTechStorage.id("crystal_wolf"));
	public static final EntityType<CrystalWolf> CRYSTAL_WOLF = EntityType.Builder.of(CrystalWolf::new, MobCategory.MISC)
			.sized(0.8f, 0.95f)
			.clientTrackingRange(10)
			.build(CRYSTAL_WOLF_KEY);

	public static final ResourceKey<EntityType<?>> MOON_WOLF_KEY = ResourceKey.create(Registries.ENTITY_TYPE, JTechStorage.id("moon_wolf"));
	public static final EntityType<MoonWolf> MOON_WOLF = EntityType.Builder.of(MoonWolf::new, MobCategory.MISC)
			.sized(1.2f, 1.3f)
			.clientTrackingRange(12)
			.build(MOON_WOLF_KEY);

	public static final Item CRYSTAL_WOLF_ITEM = item("crystal_wolf", p -> new HelperSpawnItem(() -> CRYSTAL_WOLF, p),
			new Item.Properties().stacksTo(1).rarity(Rarity.RARE));
	public static final Item MOON_WOLF_ITEM = item("moon_wolf", p -> new HelperSpawnItem(() -> MOON_WOLF, p),
			new Item.Properties().stacksTo(1).rarity(Rarity.EPIC));
	public static final Item AMETHYST_BONE = item("amethyst_bone", Item::new, new Item.Properties().stacksTo(16).rarity(Rarity.RARE));

	public static final ResourceKey<Block> WOLF_BASKET_KEY = ResourceKey.create(Registries.BLOCK, JTechStorage.id("wolf_basket"));
	public static final Block WOLF_BASKET = new HelperBedBlock(HelperBedBlock.KIND_BASKET, BlockBehaviour.Properties.ofFullCopy(Blocks.HAY_BLOCK)
			.noOcclusion().strength(0.6f).setId(WOLF_BASKET_KEY));
	public static final Item WOLF_BASKET_ITEM = blockItem(WOLF_BASKET, WOLF_BASKET_KEY);

	public static final ResourceKey<Block> HELPER_BED_KEY = ResourceKey.create(Registries.BLOCK, JTechStorage.id("helper_bed"));
	public static final Block HELPER_BED = new HelperBedBlock(HelperBedBlock.KIND_BED, BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS)
			.noOcclusion().strength(1.0f).setId(HELPER_BED_KEY));
	public static final Item HELPER_BED_ITEM = blockItem(HELPER_BED, HELPER_BED_KEY);

	private WolfContent() {
	}

	private static Item item(String name, java.util.function.Function<Item.Properties, Item> factory, Item.Properties properties) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, JTechStorage.id(name));
		Item item = Registry.register(BuiltInRegistries.ITEM, key, factory.apply(properties.setId(key)));
		ITEMS.add(item);
		return item;
	}

	private static Item blockItem(Block block, ResourceKey<Block> key) {
		Item item = new BlockItem(block, new Item.Properties().useBlockDescriptionPrefix().setId(ResourceKey.create(Registries.ITEM, key.identifier())));
		ITEMS.add(item);
		return item;
	}

	public static void init() {
		Registry.register(BuiltInRegistries.ENTITY_TYPE, CRYSTAL_WOLF_KEY, CRYSTAL_WOLF);
		FabricDefaultAttributeRegistry.register(CRYSTAL_WOLF, CrystalWolf.createAttributes());
		Registry.register(BuiltInRegistries.ENTITY_TYPE, MOON_WOLF_KEY, MOON_WOLF);
		FabricDefaultAttributeRegistry.register(MOON_WOLF, MoonWolf.createAttributes());
		Registry.register(BuiltInRegistries.BLOCK, WOLF_BASKET_KEY, WOLF_BASKET);
		Registry.register(BuiltInRegistries.ITEM, WOLF_BASKET_KEY.identifier(), WOLF_BASKET_ITEM);
		Registry.register(BuiltInRegistries.BLOCK, HELPER_BED_KEY, HELPER_BED);
		Registry.register(BuiltInRegistries.ITEM, HELPER_BED_KEY.identifier(), HELPER_BED_ITEM);

		UseEntityCallback.EVENT.register((player, level, hand, entity, hit) -> {
			if (entity instanceof Wolf wolf && player.getItemInHand(hand).is(ModContent.AMETHYST_CORE) && wolf.isTame() && wolf.isOwnedBy(player)) {
				if (level instanceof ServerLevel serverLevel) {
					evolve(serverLevel, wolf, player.getItemInHand(hand), player);
				}

				return InteractionResult.SUCCESS;
			}

			return InteractionResult.PASS;
		});
		ServerTickEvents.END_LEVEL_TICK.register(WolfContent::moonTick);
	}

	public static List<Item> creativeItems() {
		return ITEMS;
	}

	/** Gezähmter Wolf + Amethyst-Kern = Kristallwolf. */
	private static void evolve(ServerLevel level, Wolf wolf, ItemStack core, net.minecraft.world.entity.player.Player player) {
		CrystalWolf crystal = CRYSTAL_WOLF.create(level, EntitySpawnReason.CONVERSION);

		if (crystal == null) {
			return;
		}

		crystal.snapTo(wolf.getX(), wolf.getY(), wolf.getZ(), wolf.getYRot(), wolf.getXRot());
		crystal.setOwner(player);

		if (wolf.hasCustomName()) {
			crystal.setCustomName(wolf.getCustomName());
		}

		level.addFreshEntity(crystal);
		wolf.discard();
		core.consume(1, player);
		level.sendParticles(ParticleTypes.WITCH, crystal.getX(), crystal.getY() + 0.6, crystal.getZ(), 40, 0.4, 0.5, 0.4, 0.05);
		level.sendParticles(ParticleTypes.END_ROD, crystal.getX(), crystal.getY() + 0.6, crystal.getZ(), 20, 0.4, 0.5, 0.4, 0.08);
		crystal.playSound(ModSounds.CRYSTAL_WOLF_HOWL, 2.0f, 1.0f);
		player.sendSystemMessage(Component.translatable("jtechstorage.crystal_wolf.evolved").withStyle(ChatFormatting.LIGHT_PURPLE));

		if (player instanceof ServerPlayer serverPlayer) {
			ModAdvancements.grant(serverPlayer, "crystal_wolf");
		}
	}

	// ------------------------------------------------------------------
	// Mondwolf: erscheint in Vollmondnächten nahe einer Basis
	// ------------------------------------------------------------------

	private static void moonTick(ServerLevel level) {
		if (level.dimension() != Level.OVERWORLD || level.getGameTime() % 1200 != 600) {
			return;
		}

		long time = Math.floorMod(level.getDefaultClockTime(), 24000L);

		if (time < 13500L || time > 21000L) {
			return;
		}

		for (ServerPlayer player : level.players()) {
			if (player.isSpectator() || level.getMoonBrightness(player.blockPosition()) < 0.99f || level.getRandom().nextInt(3) != 0) {
				continue;
			}

			// nur bei einer Basis (Storage Terminal in der Nähe), mit einem Rudel aus mindestens 5 eigenen Kristallwölfen
			// und wenn kein Mondwolf in der Gegend ist
			BlockPos terminal = StormContent.nearestTerminal(level, player.blockPosition());

			if (terminal == null || ownPack(level, player, terminal) < PACK_FOR_ALPHA
					|| !level.getEntitiesOfClass(MoonWolf.class, player.getBoundingBox().inflate(128)).isEmpty()) {
				continue;
			}

			spawnWild(level, player);
		}
	}

	/** Ab so vielen eigenen Kristallwölfen erscheint der Mondwolf. */
	public static final int PACK_FOR_ALPHA = 5;

	/** Eigene Kristallwölfe des Spielers rund um seine Basis (64 Blöcke um das Terminal). */
	private static int ownPack(ServerLevel level, ServerPlayer player, BlockPos terminal) {
		return level.getEntitiesOfClass(CrystalWolf.class, new net.minecraft.world.phys.AABB(terminal).inflate(64),
				wolf -> !(wolf instanceof MoonWolf) && !wolf.isHologram() && player.getUUID().equals(wolf.getOwnerId())).size();
	}

	private static void spawnWild(ServerLevel level, ServerPlayer player) {
		BlockPos best = null;

		// Anhöhe suchen: der höchste von mehreren Punkten im Ring 24–40 Blöcke
		for (int i = 0; i < 10; i++) {
			double angle = level.getRandom().nextDouble() * Math.PI * 2;
			double radius = 24 + level.getRandom().nextDouble() * 16;
			int x = player.getBlockX() + (int) Math.round(Math.cos(angle) * radius);
			int z = player.getBlockZ() + (int) Math.round(Math.sin(angle) * radius);

			if (!level.isLoaded(new BlockPos(x, player.getBlockY(), z))) {
				continue;
			}

			BlockPos pos = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);

			if (!level.getBlockState(pos.below()).getFluidState().isEmpty()) {
				continue;
			}

			if (best == null || pos.getY() > best.getY()) {
				best = pos;
			}
		}

		if (best == null) {
			return;
		}

		MoonWolf wolf = MOON_WOLF.create(level, EntitySpawnReason.EVENT);

		if (wolf == null) {
			return;
		}

		wolf.makeWild();
		wolf.snapTo(best.getX() + 0.5, best.getY(), best.getZ() + 0.5, level.getRandom().nextFloat() * 360.0f, 0.0f);
		level.addFreshEntity(wolf);
		level.sendParticles(ParticleTypes.END_ROD, wolf.getX(), wolf.getY() + 1.0, wolf.getZ(), 40, 0.6, 1.0, 0.6, 0.05);
		player.sendSystemMessage(Component.translatable("jtechstorage.moon_wolf.appeared").withStyle(ChatFormatting.AQUA, ChatFormatting.ITALIC));
		level.playSound(null, wolf.blockPosition(), ModSounds.MOON_WOLF_HOWL, net.minecraft.sounds.SoundSource.NEUTRAL, 6.0f, 1.0f);
	}
}
