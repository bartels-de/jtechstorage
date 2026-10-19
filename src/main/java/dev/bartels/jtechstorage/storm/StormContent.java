package dev.bartels.jtechstorage.storm;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;

import com.mojang.brigadier.CommandDispatcher;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;

import dev.bartels.jtechstorage.JTechStorage;
import dev.bartels.jtechstorage.block.SignalLanternBlock;
import dev.bartels.jtechstorage.block.StorageTerminalBlockEntity;
import dev.bartels.jtechstorage.entity.CrystalShardling;
import dev.bartels.jtechstorage.entity.StormHerald;

/** Inhalte des Resonanz-Sturms (v0.25): Kristall-Splitterling, Sturm-Herold, Signal-Laterne und der Befehl. */
public final class StormContent {
	public static final ResourceKey<EntityType<?>> CRYSTAL_SHARDLING_KEY = ResourceKey.create(Registries.ENTITY_TYPE,
			JTechStorage.id("crystal_shardling"));
	public static final EntityType<CrystalShardling> CRYSTAL_SHARDLING = EntityType.Builder.of(CrystalShardling::new, MobCategory.MONSTER)
			.sized(0.8f, 0.7f)
			.clientTrackingRange(10)
			.build(CRYSTAL_SHARDLING_KEY);

	public static final ResourceKey<EntityType<?>> STORM_HERALD_KEY = ResourceKey.create(Registries.ENTITY_TYPE,
			JTechStorage.id("storm_herald"));
	public static final EntityType<StormHerald> STORM_HERALD = EntityType.Builder.of(StormHerald::new, MobCategory.MONSTER)
			.sized(1.0f, 3.0f)
			.fireImmune()
			.clientTrackingRange(12)
			.build(STORM_HERALD_KEY);

	public static final ResourceKey<Block> SIGNAL_LANTERN_KEY = ResourceKey.create(Registries.BLOCK, JTechStorage.id("signal_lantern"));
	public static final Block SIGNAL_LANTERN = new SignalLanternBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.LANTERN)
			.lightLevel(state -> state.getValue(SignalLanternBlock.LIT) ? 13 : 3).setId(SIGNAL_LANTERN_KEY));
	public static final Item SIGNAL_LANTERN_ITEM = new BlockItem(SIGNAL_LANTERN, new Item.Properties().useBlockDescriptionPrefix()
			.setId(ResourceKey.create(Registries.ITEM, SIGNAL_LANTERN_KEY.identifier())));

	/** Suchradius für das Terminal, auf das der Sturm zielt. */
	private static final int TERMINAL_SEARCH = 64;

	private StormContent() {
	}

	public static void init() {
		Registry.register(BuiltInRegistries.ENTITY_TYPE, CRYSTAL_SHARDLING_KEY, CRYSTAL_SHARDLING);
		FabricDefaultAttributeRegistry.register(CRYSTAL_SHARDLING, CrystalShardling.createAttributes());
		Registry.register(BuiltInRegistries.ENTITY_TYPE, STORM_HERALD_KEY, STORM_HERALD);
		FabricDefaultAttributeRegistry.register(STORM_HERALD, StormHerald.createAttributes());
		Registry.register(BuiltInRegistries.BLOCK, SIGNAL_LANTERN_KEY, SIGNAL_LANTERN);
		Registry.register(BuiltInRegistries.ITEM, SIGNAL_LANTERN_KEY.identifier(), SIGNAL_LANTERN_ITEM);

		ServerTickEvents.END_LEVEL_TICK.register(ResonanceStorm::tickLevel);
		ServerTickEvents.END_SERVER_TICK.register(StormContent::naturalStorms);
		ServerLifecycleEvents.SERVER_STOPPING.register(ResonanceStorm::clearAll);
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> registerCommand(dispatcher));
	}

	/** Reihenfolge im eigenen Kreativ-Tab. */
	public static List<Item> creativeItems() {
		List<Item> items = new ArrayList<>();
		items.add(SIGNAL_LANTERN_ITEM);
		return items;
	}

	// ------------------------------------------------------------------
	// Befehl: /jtechstorage storm start | stop
	// ------------------------------------------------------------------

	private static void registerCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
		// Die Berechtigung hängt am Unterbefehl – /jtechstorage companion … (v0.25) dürfen alle Spieler nutzen
		dispatcher.register(Commands.literal("jtechstorage")
				.then(Commands.literal("storm")
						.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
						.then(Commands.literal("start").executes(context -> start(context.getSource())))
						.then(Commands.literal("stop").executes(context -> stop(context.getSource())))));
	}

	private static int start(CommandSourceStack source) {
		ServerLevel level = source.getLevel();
		BlockPos terminal = nearestTerminal(level, BlockPos.containing(source.getPosition()));

		if (terminal == null) {
			source.sendFailure(Component.translatable("jtechstorage.storm.command.no_terminal", TERMINAL_SEARCH));
			return 0;
		}

		if (ResonanceStorm.start(level, terminal) == null) {
			source.sendFailure(Component.translatable("jtechstorage.storm.command.running"));
			return 0;
		}

		source.sendSuccess(() -> Component.translatable("jtechstorage.storm.command.started", terminal.getX(), terminal.getY(), terminal.getZ())
				.withStyle(ChatFormatting.LIGHT_PURPLE), true);
		return 1;
	}

	private static int stop(CommandSourceStack source) {
		int count = ResonanceStorm.stop(source.getLevel(), BlockPos.containing(source.getPosition()));

		if (count == 0) {
			source.sendFailure(Component.translatable("jtechstorage.storm.command.none"));
			return 0;
		}

		source.sendSuccess(() -> Component.translatable("jtechstorage.storm.command.stopped", count), true);
		return count;
	}

	// ------------------------------------------------------------------
	// Natürliche Stürme (v0.27): Wer viel Amethyst abbaut, lädt Resonanz auf – irgendwann entlädt sie sich als Sturm
	// über dem eigenen Terminal. Bedingungen: im Chunk des eigenen Terminals, mindestens ein eigener Helfer, mehr als
	// 500 Amethyst (Blöcke, Knospen, Cluster) seit dem letzten Sturm, mindestens ein Spieltag Abstand.
	// ------------------------------------------------------------------

	private static final net.minecraft.world.level.block.Block[] AMETHYST = {Blocks.AMETHYST_BLOCK, Blocks.AMETHYST_CLUSTER,
			Blocks.LARGE_AMETHYST_BUD, Blocks.MEDIUM_AMETHYST_BUD, Blocks.SMALL_AMETHYST_BUD, Blocks.BUDDING_AMETHYST};

	/** Abgebauter Amethyst laut Vanilla-Statistik. */
	public static int amethystMined(net.minecraft.server.level.ServerPlayer player) {
		int total = 0;

		for (net.minecraft.world.level.block.Block block : AMETHYST) {
			total += player.getStats().getValue(net.minecraft.stats.Stats.BLOCK_MINED.get(block));
		}

		return total;
	}

	private static void naturalStorms(net.minecraft.server.MinecraftServer server) {
		dev.bartels.jtechstorage.ModConfig.Storm config = dev.bartels.jtechstorage.ModConfig.get().storm;

		if (!config.naturalStorms) {
			return;
		}

		int tick = server.getTickCount();

		for (net.minecraft.server.level.ServerPlayer player : server.getPlayerList().getPlayers()) {
			if ((tick + Math.floorMod(player.getUUID().hashCode(), 100)) % 100 == 0) {
				tryNaturalStorm(player, config);
			}
		}
	}

	private static void tryNaturalStorm(net.minecraft.server.level.ServerPlayer player, dev.bartels.jtechstorage.ModConfig.Storm config) {
		ServerLevel level = player.level();

		if (player.isSpectator() || player.isCreative() || level.getDifficulty() == net.minecraft.world.Difficulty.PEACEFUL) {
			return;
		}

		StormData data = StormData.get(level.getServer());
		StormData.Entry last = data.of(player.getUUID());
		int mined = amethystMined(player);

		if (mined <= last.amethyst() + config.amethystPerStorm || level.getGameTime() - last.time() < (long) (config.cooldownDays * 24000)) {
			return;
		}

		// im Chunk des eigenen Terminals?
		BlockPos terminal = null;

		if (level.getChunkSource().getChunkNow(player.chunkPosition().x(), player.chunkPosition().z()) instanceof LevelChunk chunk) {
			for (BlockEntity entity : chunk.getBlockEntities().values()) {
				if (entity instanceof StorageTerminalBlockEntity t && t.isOwner(player)) {
					terminal = entity.getBlockPos();
					break;
				}
			}
		}

		if (terminal == null || ResonanceStorm.at(level, terminal) != null) {
			return;
		}

		// mindestens ein eigener Helfer in der Nähe des Terminals
		boolean hasHelper = !level.getEntitiesOfClass(dev.bartels.jtechstorage.entity.NetworkHelperMob.class,
				new net.minecraft.world.phys.AABB(terminal).inflate(96.0), h -> h.isAlive() && player.getUUID().equals(h.getOwnerId())).isEmpty();

		if (!hasHelper) {
			return;
		}

		if (ResonanceStorm.start(level, terminal) != null) {
			data.record(player.getUUID(), mined, level.getGameTime());
			player.sendSystemMessage(Component.translatable("jtechstorage.storm.natural", mined).withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.ITALIC));
		}
	}

	/** Nächstes Storage Terminal (geladene Chunks im Umkreis). */
	public static @org.jspecify.annotations.Nullable BlockPos nearestTerminal(ServerLevel level, BlockPos from) {
		BlockPos best = null;
		double bestDist = (double) TERMINAL_SEARCH * TERMINAL_SEARCH;
		ChunkPos center = ChunkPos.containing(from);
		int r = TERMINAL_SEARCH / 16 + 1;

		for (int cx = center.x() - r; cx <= center.x() + r; cx++) {
			for (int cz = center.z() - r; cz <= center.z() + r; cz++) {
				if (!(level.getChunkSource().getChunkNow(cx, cz) instanceof LevelChunk chunk)) {
					continue;
				}

				for (BlockEntity entity : chunk.getBlockEntities().values()) {
					if (entity instanceof StorageTerminalBlockEntity) {
						double dist = entity.getBlockPos().distToCenterSqr(Vec3.atCenterOf(from));

						if (dist < bestDist) {
							bestDist = dist;
							best = entity.getBlockPos();
						}
					}
				}
			}
		}

		return best;
	}
}
