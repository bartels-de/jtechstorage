package dev.bartels.jtechstorage.companion;

import java.util.List;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityLevelChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;

import dev.bartels.jtechstorage.JTechStorage;
import dev.bartels.jtechstorage.ModConfig;
import dev.bartels.jtechstorage.ModSounds;

/**
 * Begleiter (v0.25): Entity-Typen, Geodenhüter in Geoden, XP-Teilen, Mitreisen durch Dimensionen und die Befehle
 * {@code /jtechstorage companion accept|decline|call|release} (Spieler) sowie {@code keeper|xp} (Spielleiter).
 */
public final class CompanionContent {
	public static final ResourceKey<EntityType<?>> MANTIS_KEY = key("mantis");
	public static final EntityType<MantisCompanion> MANTIS = EntityType.Builder.of(MantisCompanion::new, MobCategory.CREATURE)
			.sized(0.8f, 1.2f).clientTrackingRange(10).build(MANTIS_KEY);
	public static final ResourceKey<EntityType<?>> SHROOM_KEY = key("shroom");
	public static final EntityType<ShroomCompanion> SHROOM = EntityType.Builder.of(ShroomCompanion::new, MobCategory.CREATURE)
			.sized(0.7f, 0.95f).clientTrackingRange(10).build(SHROOM_KEY);
	public static final ResourceKey<EntityType<?>> RAVEN_KEY = key("raven");
	public static final EntityType<RavenCompanion> RAVEN = EntityType.Builder.of(RavenCompanion::new, MobCategory.CREATURE)
			.sized(0.55f, 0.75f).clientTrackingRange(10).build(RAVEN_KEY);
	public static final ResourceKey<EntityType<?>> GEODE_KEEPER_KEY = key("geode_keeper");
	public static final EntityType<GeodeKeeper> GEODE_KEEPER = EntityType.Builder.of(GeodeKeeper::new, MobCategory.MISC)
			.sized(0.7f, 2.0f).clientTrackingRange(10).build(GEODE_KEEPER_KEY);

	/** Resonanz-Leckerli (v0.25.3): Amethyst + Honig – doppelte XP und Tempo für den Begleiter. */
	public static final ResourceKey<net.minecraft.world.item.Item> RESONANCE_TREAT_KEY = ResourceKey.create(Registries.ITEM, JTechStorage.id("resonance_treat"));
	public static final net.minecraft.world.item.Item RESONANCE_TREAT = new net.minecraft.world.item.Item(new net.minecraft.world.item.Item.Properties()
			.stacksTo(16).rarity(net.minecraft.world.item.Rarity.UNCOMMON).setId(RESONANCE_TREAT_KEY));

	/** Reihenfolge im eigenen Kreativ-Tab. */
	public static List<net.minecraft.world.item.Item> creativeItems() {
		return List.of(RESONANCE_TREAT);
	}

	/** Begleiter, die weiter als das vom Besitzer entfernt (oder in einer anderen Dimension) sind, werden geholt. */
	private static final double FAR = 40.0;

	private CompanionContent() {
	}

	private static ResourceKey<EntityType<?>> key(String name) {
		return ResourceKey.create(Registries.ENTITY_TYPE, JTechStorage.id(name));
	}

	public static void init() {
		Registry.register(BuiltInRegistries.ITEM, RESONANCE_TREAT_KEY, RESONANCE_TREAT);
		Registry.register(BuiltInRegistries.ENTITY_TYPE, MANTIS_KEY, MANTIS);
		Registry.register(BuiltInRegistries.ENTITY_TYPE, SHROOM_KEY, SHROOM);
		Registry.register(BuiltInRegistries.ENTITY_TYPE, RAVEN_KEY, RAVEN);
		Registry.register(BuiltInRegistries.ENTITY_TYPE, GEODE_KEEPER_KEY, GEODE_KEEPER);
		FabricDefaultAttributeRegistry.register(MANTIS, MantisCompanion.createAttributes());
		FabricDefaultAttributeRegistry.register(SHROOM, ShroomCompanion.createAttributes());
		FabricDefaultAttributeRegistry.register(RAVEN, RavenCompanion.createAttributes());
		FabricDefaultAttributeRegistry.register(GEODE_KEEPER, GeodeKeeper.createAttributes());

		// Durch Portale, beim Wiederbeleben und wenn er zu weit weg ist: der Begleiter kommt mit
		ServerEntityLevelChangeEvents.AFTER_PLAYER_CHANGE_LEVEL.register((player, origin, destination) -> bring(player, false));
		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> bring(newPlayer, false));
		ServerTickEvents.END_SERVER_TICK.register(CompanionContent::tick);
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> registerCommand(dispatcher));
	}

	public static @Nullable Companion create(String species, ServerLevel level) {
		EntityType<? extends Companion> type = switch (species) {
			case "mantis" -> MANTIS;
			case "shroom" -> SHROOM;
			case "raven" -> RAVEN;
			default -> null;
		};
		return type == null ? null : type.create(level, EntitySpawnReason.EVENT);
	}

	// ------------------------------------------------------------------
	// Begleiter finden und holen
	// ------------------------------------------------------------------

	public static @Nullable Companion find(MinecraftServer server, UUID owner) {
		CompanionData.Entry entry = CompanionData.get(server).of(owner);

		if (entry == null) {
			return null;
		}

		for (ServerLevel level : server.getAllLevels()) {
			if (level.getEntity(entry.companion()) instanceof Companion companion && companion.isAlive()) {
				return companion;
			}
		}

		return null;
	}

	/** Holt den Begleiter zum Spieler, wenn er folgt und weit weg oder in einer anderen Dimension ist. */
	private static void bring(ServerPlayer player, boolean evenIfStaying) {
		if (player.isSpectator() || !player.isAlive()) {
			return;
		}

		Companion companion = find(player.level().getServer(), player.getUUID());

		if (companion == null || companion.isOrderedToSit() && !evenIfStaying) {
			return;
		}

		if (evenIfStaying) {
			companion.setOrderedToSit(false);
			companion.setInSittingPose(false);
		}

		ServerLevel target = player.level();

		if (companion.level() == target && companion.distanceToSqr(player) < FAR * FAR && !evenIfStaying) {
			return;
		}

		Vec3 pos = player.position();

		if (companion.level() == target) {
			companion.snapTo(pos.x, pos.y, pos.z, player.getYRot(), 0.0f);
			companion.getNavigation().stop();
			companion.setPortalCooldown();
			return;
		}

		Entity moved = companion.teleport(new TeleportTransition(target, pos, Vec3.ZERO, player.getYRot(), 0.0f, TeleportTransition.DO_NOTHING));

		if (moved != null) {
			moved.setPortalCooldown();
			CompanionData.get(target.getServer()).updateCompanion(player.getUUID(), moved.getUUID());
		}
	}

	// ------------------------------------------------------------------
	// Hotkey (v0.25.2): Pilz heilt jetzt / automatisch ↔ Hotkey
	// ------------------------------------------------------------------

	public static void hotkey(ServerPlayer player, int action, int arg) {
		Companion companion = find(player.level().getServer(), player.getUUID());

		// Menü-Aktionen (v0.26) – funktionieren auch aus der Ferne
		if (action >= dev.bartels.jtechstorage.net.CompanionActionPayload.STANCE) {
			if (action == dev.bartels.jtechstorage.net.CompanionActionPayload.CALL) {
				call(player.createCommandSourceStack());
				return;
			}

			if (companion == null) {
				player.sendOverlayMessage(Component.translatable(CompanionData.get(player.level().getServer()).hasCompanion(player.getUUID())
						? "jtechstorage.companion.call_unloaded" : "jtechstorage.companion.none").withStyle(ChatFormatting.GRAY));
				return;
			}

			switch (action) {
				case dev.bartels.jtechstorage.net.CompanionActionPayload.STANCE -> companion.setStance(player, arg);
				case dev.bartels.jtechstorage.net.CompanionActionPayload.LEARN -> companion.learnTalent(player, arg);
				case dev.bartels.jtechstorage.net.CompanionActionPayload.RESET -> companion.resetTalents(player);
				case dev.bartels.jtechstorage.net.CompanionActionPayload.BAG -> {
					if (companion instanceof RavenCompanion raven) {
						raven.openBag(player);
					}
				}
				default -> {
				}
			}

			sendStatus(player);
			return;
		}

		if (action == 2) {
			if (companion == null) {
				player.sendOverlayMessage(Component.translatable("jtechstorage.companion.none").withStyle(ChatFormatting.GRAY));
			} else if (companion.level() != player.level() || companion.distanceToSqr(player) > 48 * 48) {
				player.sendOverlayMessage(Component.translatable("jtechstorage.companion.too_far", companion.getDisplayName()).withStyle(ChatFormatting.GRAY));
			} else {
				companion.toggleStay(player);
			}

			return;
		}

		if (!(companion instanceof ShroomCompanion shroom)) {
			player.sendOverlayMessage(Component.translatable(companion == null ? "jtechstorage.companion.none" : "jtechstorage.shroom.only")
					.withStyle(ChatFormatting.GRAY));
			return;
		}

		if (action == 1) {
			shroom.toggleAutoHeal(player);
		} else {
			shroom.manualHeal(player);
		}

		sendStatus(player);
	}

	// ------------------------------------------------------------------
	// Zustand für HUD und Menü (v0.26)
	// ------------------------------------------------------------------

	/** Schickt dem Spieler den Zustand seines Begleiters. */
	public static void sendStatus(ServerPlayer player) {
		MinecraftServer server = player.level().getServer();
		dev.bartels.jtechstorage.net.CompanionStatusPayload status;

		if (!CompanionData.get(server).hasCompanion(player.getUUID())) {
			status = dev.bartels.jtechstorage.net.CompanionStatusPayload.NONE;
		} else {
			Companion c = find(server, player.getUUID());

			if (c == null) {
				status = new dev.bartels.jtechstorage.net.CompanionStatusPayload(true, false, "", "", 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
						new int[CompanionTalents.COUNT]);
			} else {
				boolean sameLevel = c.level() == player.level();
				int flags = (c.isOrderedToSit() ? 1 : 0) | (c.isExhausted() ? 2 : 0) | (!sameLevel ? 32 : 0);
				int special = 0;
				int specialMax = 0;

				if (c instanceof ShroomCompanion shroom) {
					flags |= shroom.isAutoHeal() ? 4 : 0;
					special = shroom.healCooldown();
					specialMax = shroom.healCooldownMax();
				} else if (c instanceof MantisCompanion mantis) {
					special = mantis.getFlightEnergy();
					specialMax = mantis.maxFlight();
				} else if (c instanceof RavenCompanion raven) {
					flags |= (raven.isAway() ? 8 : 0) | (raven.hasTerminal() ? 16 : 0);
					special = raven.bagUsed();
					specialMax = raven.capacity();
				}

				String name = c.hasCustomName() && c.getCustomName() != null ? c.getCustomName().getString() : "";
				status = new dev.bartels.jtechstorage.net.CompanionStatusPayload(true, true, c.species(), name, c.getStage(), c.getLevel(), c.getXp(),
						c.getHealth(), c.getMaxHealth(), c.getStance(), flags, c.getXpBoostTicks(), special, specialMax,
						(float) c.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE),
						(float) c.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR), c.effectiveAttackInterval(),
						sameLevel ? (int) Math.sqrt(c.distanceToSqr(player)) : -1, c.talentRanks());
			}
		}

		net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(player, status);
	}

	// ------------------------------------------------------------------
	// XP teilen (aus dem Spieler-Mixin)
	// ------------------------------------------------------------------

	/** Teilt eingesammelte Erfahrung mit dem Begleiter (wenn er folgt und in der Nähe ist); Rückgabe: Anteil des Spielers. */
	public static int shareXp(ServerPlayer player, int amount) {
		CompanionData.Entry entry = CompanionData.get(player.level().getServer()).of(player.getUUID());

		if (entry == null || amount <= 0) {
			return amount;
		}

		if (!(player.level().getEntity(entry.companion()) instanceof Companion companion) || !companion.isActive()
				|| companion.distanceToSqr(player) > 32 * 32) {
			return amount;
		}

		double exact = amount * ModConfig.get().companions.xpShare;
		int part = (int) Math.floor(exact);

		if (player.getRandom().nextDouble() < exact - part) {
			part++;     // Bruchteile zufällig aufrunden, damit auch einzelne XP-Punkte geteilt werden
		}

		part = Math.min(part, amount);
		companion.addSharedXp(part);
		return amount - part;
	}

	// ------------------------------------------------------------------
	// Tick: Begleiter nachholen, Geoden suchen
	// ------------------------------------------------------------------

	private static void tick(MinecraftServer server) {
		int tick = server.getTickCount();

		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			int slot = Math.floorMod(player.getUUID().hashCode(), 200);

			if ((tick + slot) % 40 == 0) {
				bring(player, false);
			}

			if ((tick + slot) % 200 == 0) {
				scanForGeode(player);
			}

			if ((tick + slot) % 10 == 0 && net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.canSend(player,
					dev.bartels.jtechstorage.net.CompanionStatusPayload.TYPE)) {
				sendStatus(player);
			}
		}
	}

	private static long geodeKey(BlockPos pos) {
		return BlockPos.asLong(pos.getX() >> 4, pos.getY() >> 4, pos.getZ() >> 4);
	}

	/** Neue Geode in der Nähe? Dann einmal würfeln, ob hier ein Geodenhüter wohnt (seit v0.27 auch für Spieler mit Begleiter – zum Handeln). */
	private static void scanForGeode(ServerPlayer player) {
		ServerLevel level = player.level();
		CompanionData data = CompanionData.get(level.getServer());

		if (player.isSpectator() || player.getY() > 80 || level.dimension() != net.minecraft.world.level.Level.OVERWORLD) {
			return;
		}

		if (!level.getEntitiesOfClass(GeodeKeeper.class, player.getBoundingBox().inflate(64.0)).isEmpty()) {
			return;
		}

		int r = ModConfig.get().companions.keeperSearchRadius;
		BlockPos budding = findUnvisitedBudding(level, player.blockPosition(), r, data);

		if (budding == null) {
			return;
		}

		// die ganze Geode (Umkreis eines Chunks) gilt als besucht – jede Geode hat genau eine Chance
		for (int dx = -1; dx <= 1; dx++) {
			for (int dy = -1; dy <= 1; dy++) {
				for (int dz = -1; dz <= 1; dz++) {
					data.visitGeode(geodeKey(budding.offset(dx * 16, dy * 16, dz * 16)));
				}
			}
		}

		if (level.getRandom().nextDouble() >= ModConfig.get().companions.keeperChance) {
			return;
		}

		BlockPos spot = findSpot(level, budding);

		if (spot != null && GeodeKeeper.spawn(level, spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5) != null) {
			player.sendOverlayMessage(Component.translatable("jtechstorage.keeper.nearby").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.ITALIC));
			level.playSound(null, spot, ModSounds.KEEPER_GREET, net.minecraft.sounds.SoundSource.NEUTRAL, 1.0f, 0.8f);
		}
	}

	/** Knospender Amethyst einer noch nicht besuchten Geode – überspringt Abschnitte ohne Amethyst. */
	private static @Nullable BlockPos findUnvisitedBudding(ServerLevel level, BlockPos center, int r, CompanionData data) {
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

		for (int cx = (center.getX() - r) >> 4; cx <= (center.getX() + r) >> 4; cx++) {
			for (int cz = (center.getZ() - r) >> 4; cz <= (center.getZ() + r) >> 4; cz++) {
				net.minecraft.world.level.chunk.LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);

				if (chunk == null) {
					continue;
				}

				for (int sy = (center.getY() - r) >> 4; sy <= (center.getY() + r) >> 4; sy++) {
					int index = chunk.getSectionIndexFromSectionY(sy);

					if (index < 0 || index >= chunk.getSectionsCount()) {
						continue;
					}

					net.minecraft.world.level.chunk.LevelChunkSection section = chunk.getSection(index);

					if (section.hasOnlyAir() || !section.maybeHas(state -> state.is(Blocks.BUDDING_AMETHYST))) {
						continue;
					}

					for (int x = 0; x < 16; x++) {
						for (int y = 0; y < 16; y++) {
							for (int z = 0; z < 16; z++) {
								if (!section.getBlockState(x, y, z).is(Blocks.BUDDING_AMETHYST)) {
									continue;
								}

								pos.set((cx << 4) + x, (sy << 4) + y, (cz << 4) + z);

								if (pos.distManhattan(center) <= r * 2 && !data.isGeodeVisited(geodeKey(pos))) {
									return pos.immutable();
								}
							}
						}
					}
				}
			}
		}

		return null;
	}

	/** Freier Platz im Inneren der Geode: zwei Blöcke Luft über Amethyst, möglichst nah an der knospenden Stelle. */
	private static @Nullable BlockPos findSpot(ServerLevel level, BlockPos budding) {
		BlockPos best = null;
		double bestScore = Double.MAX_VALUE;

		for (BlockPos pos : BlockPos.betweenClosed(budding.offset(-6, -6, -6), budding.offset(6, 6, 6))) {
			if (!level.getBlockState(pos).isAir() || !level.getBlockState(pos.above()).isAir()) {
				continue;
			}

			BlockPos below = pos.below();
			BlockState ground = level.getBlockState(below);

			if (!ground.isFaceSturdy(level, below, Direction.UP)) {
				continue;
			}

			boolean inside = ground.is(Blocks.AMETHYST_BLOCK) || ground.is(Blocks.BUDDING_AMETHYST);
			double score = pos.distSqr(budding) + (inside ? 0 : 200);

			if (score < bestScore) {
				bestScore = score;
				best = pos.immutable();
			}
		}

		return best;
	}

	// ------------------------------------------------------------------
	// Befehle
	// ------------------------------------------------------------------

	private static void registerCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("jtechstorage")
				.then(Commands.literal("companion")
						.then(Commands.literal("accept").executes(c -> answer(c.getSource(), true)))
						.then(Commands.literal("decline").executes(c -> answer(c.getSource(), false)))
						.then(Commands.literal("call").executes(c -> call(c.getSource())))
						.then(Commands.literal("release")
								.executes(c -> {
									c.getSource().sendSystemMessage(Component.translatable("jtechstorage.companion.release_confirm").withStyle(ChatFormatting.GOLD));
									return 0;
								})
								.then(Commands.literal("confirm").executes(c -> release(c.getSource()))))
						.then(Commands.literal("keeper")
								.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
								.executes(c -> spawnKeeper(c.getSource())))
						.then(Commands.literal("xp")
								.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
								.then(Commands.argument("amount", IntegerArgumentType.integer(1))
										.executes(c -> giveXp(c.getSource(), IntegerArgumentType.getInteger(c, "amount")))))));
	}

	private static int answer(CommandSourceStack source, boolean accept) {
		ServerPlayer player = source.getPlayer();

		if (player == null) {
			return 0;
		}

		List<GeodeKeeper> keepers = player.level().getEntitiesOfClass(GeodeKeeper.class, player.getBoundingBox().inflate(12.0), k -> !k.isLeaving());

		if (keepers.isEmpty()) {
			source.sendFailure(Component.translatable("jtechstorage.keeper.none"));
			return 0;
		}

		GeodeKeeper keeper = keepers.get(0);

		if (accept) {
			keeper.accept(player);
		} else {
			keeper.decline(player);
		}

		return 1;
	}

	public static int call(CommandSourceStack source) {
		ServerPlayer player = source.getPlayer();

		if (player == null) {
			return 0;
		}

		Companion companion = find(player.level().getServer(), player.getUUID());

		if (companion == null) {
			source.sendFailure(Component.translatable(CompanionData.get(player.level().getServer()).hasCompanion(player.getUUID())
					? "jtechstorage.companion.call_unloaded" : "jtechstorage.companion.none"));
			return 0;
		}

		bring(player, true);
		source.sendSuccess(() -> Component.translatable("jtechstorage.companion.called").withStyle(ChatFormatting.LIGHT_PURPLE), false);
		return 1;
	}

	private static int release(CommandSourceStack source) {
		ServerPlayer player = source.getPlayer();

		if (player == null) {
			return 0;
		}

		MinecraftServer server = player.level().getServer();
		Companion companion = find(server, player.getUUID());
		CompanionData data = CompanionData.get(server);

		if (!data.hasCompanion(player.getUUID())) {
			source.sendFailure(Component.translatable("jtechstorage.companion.none"));
			return 0;
		}

		if (companion != null && companion.level() instanceof ServerLevel level) {
			level.sendParticles(ParticleTypes.WITCH, companion.getX(), companion.getY() + 0.5, companion.getZ(), 30, 0.4, 0.5, 0.4, 0.05);
			companion.discard();
		}

		data.forgetOwner(player.getUUID());
		source.sendSuccess(() -> Component.translatable("jtechstorage.companion.released").withStyle(ChatFormatting.GRAY), false);
		return 1;
	}

	private static int spawnKeeper(CommandSourceStack source) {
		ServerLevel level = source.getLevel();
		Vec3 pos = source.getPosition();
		Vec3 look = source.getEntity() != null ? source.getEntity().getLookAngle().multiply(1, 0, 1).normalize() : Vec3.ZERO;
		GeodeKeeper keeper = GeodeKeeper.spawn(level, pos.x + look.x * 2, pos.y, pos.z + look.z * 2);
		return keeper != null ? 1 : 0;
	}

	private static int giveXp(CommandSourceStack source, int amount) {
		ServerPlayer player = source.getPlayer();
		Companion companion = player != null ? find(player.level().getServer(), player.getUUID()) : null;

		if (companion == null) {
			source.sendFailure(Component.translatable("jtechstorage.companion.none"));
			return 0;
		}

		companion.addXp(amount);
		source.sendSuccess(() -> Component.translatable("jtechstorage.companion.xp_given", amount, companion.getLevel()), false);
		return 1;
	}
}
