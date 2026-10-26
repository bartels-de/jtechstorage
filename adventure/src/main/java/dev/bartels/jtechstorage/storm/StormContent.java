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
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;

import com.mojang.brigadier.CommandDispatcher;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;

import dev.bartels.jtechstorage.JTechStorage;
import dev.bartels.jtechstorage.block.StorageTerminalBlockEntity;
import dev.bartels.jtechstorage.entity.CrystalShardling;
import dev.bartels.jtechstorage.entity.StormHerald;
import dev.bartels.jtechstorage.api.JtsHooks;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.server.MinecraftServer;
import dev.bartels.jtechstorage.ModConfig;
import net.minecraft.world.Difficulty;
import dev.bartels.jtechstorage.entity.NetworkHelperMob;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

/** Contents of the Resonance Storm (v0.25): Crystal Shardling, Storm Herald, Signal Lantern and the command. */
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


	/** Search radius for the terminal the storm targets. */
	private static final int TERMINAL_SEARCH = 64;

	private StormContent() {
	}

	public static void init() {
		JtsHooks.stormDisturbed = ResonanceStorm::isDisturbed;
		JtsHooks.stormActive = (level, pos) -> ResonanceStorm.at(level, pos) != null;
		Registry.register(BuiltInRegistries.ENTITY_TYPE, CRYSTAL_SHARDLING_KEY, CRYSTAL_SHARDLING);
		FabricDefaultAttributeRegistry.register(CRYSTAL_SHARDLING, CrystalShardling.createAttributes());
		Registry.register(BuiltInRegistries.ENTITY_TYPE, STORM_HERALD_KEY, STORM_HERALD);
		FabricDefaultAttributeRegistry.register(STORM_HERALD, StormHerald.createAttributes());

		ServerTickEvents.END_LEVEL_TICK.register(ResonanceStorm::tickLevel);
		ServerTickEvents.END_SERVER_TICK.register(StormContent::naturalStorms);
		ServerLifecycleEvents.SERVER_STOPPING.register(ResonanceStorm::clearAll);
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> registerCommand(dispatcher));
	}

	/** Order in the own creative tab. */
	public static List<Item> creativeItems() {
		List<Item> items = new ArrayList<>();
		return items;
	}

	// ------------------------------------------------------------------
	// Command: /jtechstorage storm start | stop
	// ------------------------------------------------------------------

	private static void registerCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
		// The permission is attached to the subcommand – /jtechstorage companion … (v0.25) may be used by all players
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
	// Natural storms (v0.27): whoever mines a lot of amethyst charges up resonance – eventually it discharges as a storm
	// over their own terminal. Conditions: in the chunk of their own terminal, at least one own helper, more than
	// 500 amethyst (blocks, buds, clusters) since the last storm, at least one game day apart.
	// ------------------------------------------------------------------

	private static final Block[] AMETHYST = {Blocks.AMETHYST_BLOCK, Blocks.AMETHYST_CLUSTER,
			Blocks.LARGE_AMETHYST_BUD, Blocks.MEDIUM_AMETHYST_BUD, Blocks.SMALL_AMETHYST_BUD, Blocks.BUDDING_AMETHYST};

	/** Amethyst mined according to the vanilla statistics. */
	public static int amethystMined(ServerPlayer player) {
		int total = 0;

		for (Block block : AMETHYST) {
			total += player.getStats().getValue(Stats.BLOCK_MINED.get(block));
		}

		return total;
	}

	private static void naturalStorms(MinecraftServer server) {
		ModConfig.Storm config = ModConfig.get().storm;

		if (!config.naturalStorms) {
			return;
		}

		int tick = server.getTickCount();

		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			if ((tick + Math.floorMod(player.getUUID().hashCode(), 100)) % 100 == 0) {
				tryNaturalStorm(player, config);
			}
		}
	}

	private static void tryNaturalStorm(ServerPlayer player, ModConfig.Storm config) {
		ServerLevel level = player.level();

		if (player.isSpectator() || player.isCreative() || level.getDifficulty() == Difficulty.PEACEFUL) {
			return;
		}

		StormData data = StormData.get(level.getServer());
		StormData.Entry last = data.of(player.getUUID());
		int mined = amethystMined(player);

		if (mined <= last.amethyst() + config.amethystPerStorm || level.getGameTime() - last.time() < (long) (config.cooldownDays * 24000)) {
			return;
		}

		// in the chunk of their own terminal?
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

		// at least one own helper near the terminal
		boolean hasHelper = !level.getEntitiesOfClass(NetworkHelperMob.class,
				new AABB(terminal).inflate(96.0), h -> h.isAlive() && player.getUUID().equals(h.getOwnerId())).isEmpty();

		if (!hasHelper) {
			return;
		}

		if (ResonanceStorm.start(level, terminal) != null) {
			data.record(player.getUUID(), mined, level.getGameTime());
			player.sendSystemMessage(Component.translatable("jtechstorage.storm.natural", mined).withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.ITALIC));
		}
	}

	/** Nearest Storage Terminal (loaded chunks in the radius). */
	public static @Nullable BlockPos nearestTerminal(ServerLevel level, BlockPos from) {
		return StorageTerminalBlockEntity.nearestTerminal(level, from, TERMINAL_SEARCH);
	}
}
