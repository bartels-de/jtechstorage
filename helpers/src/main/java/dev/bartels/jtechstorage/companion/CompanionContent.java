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
import net.minecraft.server.level.TicketType;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityLevelChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;

import dev.bartels.jtechstorage.JTechStorage;
import dev.bartels.jtechstorage.ModConfig;
import dev.bartels.jtechstorage.ModSounds;
import dev.bartels.jtechstorage.net.CompanionActionPayload;
import dev.bartels.jtechstorage.net.CompanionStatusPayload;

/**
 * Companions (v0.25): entity types, geode keepers in geodes, XP sharing, travelling along through dimensions and the commands
 * {@code /jtechstorage companion accept|decline|call|release} (players) and {@code keeper|xp} (operators).
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
	public static final ResourceKey<EntityType<?>> SWARM_RAVEN_KEY = key("swarm_raven");
	/** Shadow raven from the raven swarm (v0.25.3). */
	public static final EntityType<SwarmRaven> SWARM_RAVEN = EntityType.Builder.of(SwarmRaven::new, MobCategory.MISC)
			.sized(0.5f, 0.5f).noSave().clientTrackingRange(8).updateInterval(1).build(SWARM_RAVEN_KEY);
	public static final ResourceKey<EntityType<?>> CROW_DECOY_KEY = key("crow_decoy");
	/** Crow decoy of the player (v0.26.4, Night Raven's crow doppelganger). */
	public static final EntityType<CrowDecoy> CROW_DECOY = EntityType.Builder.of(CrowDecoy::new, MobCategory.MISC)
			.sized(0.6f, 1.8f).noSave().clientTrackingRange(10).build(CROW_DECOY_KEY);
	public static final ResourceKey<EntityType<?>> GEODE_KEEPER_KEY = key("geode_keeper");
	public static final EntityType<GeodeKeeper> GEODE_KEEPER = EntityType.Builder.of(GeodeKeeper::new, MobCategory.MISC)
			.sized(0.7f, 2.0f).clientTrackingRange(10).build(GEODE_KEEPER_KEY);

	/** Resonance treat (v0.25.3): amethyst + honey - double XP and speed for the companion. */
	public static final ResourceKey<Item> RESONANCE_TREAT_KEY = ResourceKey.create(Registries.ITEM, JTechStorage.id("resonance_treat"));
	public static final Item RESONANCE_TREAT = new Item(new Item.Properties()
			.stacksTo(16).rarity(Rarity.UNCOMMON).setId(RESONANCE_TREAT_KEY));

	/** Order in the dedicated creative tab. */
	public static List<Item> creativeItems() {
		return List.of(RESONANCE_TREAT);
	}

	/** Companions further away than this from the owner (or in another dimension) are fetched. */
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
		Registry.register(BuiltInRegistries.ENTITY_TYPE, SWARM_RAVEN_KEY, SWARM_RAVEN);
		Registry.register(BuiltInRegistries.ENTITY_TYPE, CROW_DECOY_KEY, CROW_DECOY);
		Registry.register(BuiltInRegistries.ENTITY_TYPE, GEODE_KEEPER_KEY, GEODE_KEEPER);
		FabricDefaultAttributeRegistry.register(MANTIS, MantisCompanion.createAttributes());
		FabricDefaultAttributeRegistry.register(SHROOM, ShroomCompanion.createAttributes());
		FabricDefaultAttributeRegistry.register(RAVEN, RavenCompanion.createAttributes());
		FabricDefaultAttributeRegistry.register(SWARM_RAVEN, SwarmRaven.createAttributes());
		FabricDefaultAttributeRegistry.register(CROW_DECOY, CrowDecoy.createAttributes());
		// Mushroom talent "Second Life": saves the owner from death once every 10 minutes
		ServerLivingEntityEvents.ALLOW_DEATH.register((entity, source, amount) ->
				!(entity instanceof ServerPlayer player && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)
						&& (find(player.level().getServer(), player.getUUID()) instanceof ShroomCompanion shroom && shroom.trySecondLife(player)
						|| find(player.level().getServer(), player.getUUID()) instanceof RavenCompanion raven && raven.tryDecoy(player))));
		FabricDefaultAttributeRegistry.register(GEODE_KEEPER, GeodeKeeper.createAttributes());
		// Guardian crow (v0.26.3): the raven sometimes intercepts projectiles that would hit its owner
		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) ->
				!(entity instanceof ServerPlayer player && source.is(DamageTypeTags.IS_PROJECTILE)
						&& find(player.level().getServer(), player.getUUID()) instanceof RavenCompanion raven && raven.interceptFor(player, source)));

		// Through portals, on revival and when too far away: the companion follows
		ServerEntityLevelChangeEvents.AFTER_PLAYER_CHANGE_LEVEL.register((player, origin, destination) -> bringOrFetch(player, false));
		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> bringOrFetch(newPlayer, false));
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
	// Finding and fetching companions
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

	/**
	 * Safe spot for the companion near the player (v0.25.5): solid ground below or beside them, enough space. If the
	 * player is flying over the void (or high in the air with no ground within 48 blocks), there is none - the companion waits.
	 */
	public static @Nullable Vec3 safeSpot(ServerLevel level, Player player, Companion companion) {
		BlockPos base = player.blockPosition();
		int[][] offsets = {{0, 0}, {1, 0}, {-1, 0}, {0, 1}, {0, -1}, {2, 1}, {-2, -1}, {1, -2}, {-1, 2}};

		for (int[] o : offsets) {
			BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(base.getX() + o[0], base.getY() + 1, base.getZ() + o[1]);

			for (int dy = 0; dy < 50 && pos.getY() > level.getMinY(); dy++, pos.move(0, -1, 0)) {
				BlockPos below = pos.below();
				BlockState ground = level.getBlockState(below);

				if (!ground.isFaceSturdy(level, below, Direction.UP) && !ground.is(Blocks.FARMLAND)
						&& !ground.is(Blocks.DIRT_PATH)) {
					if (!ground.getCollisionShape(level, below).isEmpty() || !level.getFluidState(below).isEmpty()) {
						break;     // fence, leaves, water ...: not here, next column
					}

					continue;
				}

				Vec3 at = new Vec3(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);

				if (level.noCollision(companion, companion.getDimensions(companion.getPose()).makeBoundingBox(at)) && level.getFluidState(pos).isEmpty()) {
					return at;
				}

				break;
			}
		}

		return null;
	}

	/** Fetches it - or loads its chunk first if it is currently not loaded. */
	private static void bringOrFetch(ServerPlayer player, boolean evenIfStaying) {
		if (find(player.level().getServer(), player.getUUID()) == null) {
			fetch(player, evenIfStaying);
		} else if (!bring(player, evenIfStaying)) {
			FETCH.put(player.getUUID(), new int[] {FETCH_TICKS, evenIfStaying ? 1 : 0});     // try again later (no ground)
		}
	}

	private static final int FETCH_TICKS = 20 * 60;
	/** Player -> {remaining ticks, call (1) or only fetch if it is following (0)}. */
	private static final java.util.Map<UUID, int[]> FETCH = new java.util.HashMap<>();

	/** Unloaded companion: load the chunk at its last position and fetch it as soon as it is there (v0.25.5). */
	private static boolean fetch(ServerPlayer player, boolean evenIfStaying) {
		CompanionData.Last last = CompanionData.get(player.level().getServer()).last(player.getUUID());

		if (last == null) {
			return false;
		}

		FETCH.put(player.getUUID(), new int[] {FETCH_TICKS, evenIfStaying ? 1 : 0});
		return true;
	}

	private static void tickFetch(MinecraftServer server) {
		var it = FETCH.entrySet().iterator();

		while (it.hasNext()) {
			var entry = it.next();
			int[] state = entry.getValue();
			ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());

			if (player == null || --state[0] <= 0) {
				if (player != null && state[1] == 1) {
					player.sendOverlayMessage(Component.translatable("jtechstorage.companion.fetch_failed").withStyle(ChatFormatting.GRAY));
				}

				it.remove();
				continue;
			}

			if (!player.isAlive() || state[0] % 10 != 0) {
				continue;
			}

			Companion companion = find(server, player.getUUID());

			if (companion != null) {
				if (companion.isOrderedToSit() && state[1] == 0 || bring(player, state[1] == 1)) {
					it.remove();
				}

				continue;
			}

			CompanionData.Last last = CompanionData.get(server).last(player.getUUID());
			ServerLevel level = last == null ? null : server.getLevel(last.pos().dimension());

			if (level == null) {
				it.remove();
				continue;
			}

			level.getChunkSource().addTicketWithRadius(TicketType.ENDER_PEARL, ChunkPos.containing(last.pos().pos()), 2);
		}
	}

	/** Fetches the companion to the player if it is following and far away or in another dimension. false = no safe spot. */
	private static boolean bring(ServerPlayer player, boolean evenIfStaying) {
		if (player.isSpectator() || !player.isAlive()) {
			return false;
		}

		Companion companion = find(player.level().getServer(), player.getUUID());

		if (companion == null || companion.isOrderedToSit() && !evenIfStaying) {
			return companion != null;
		}

		if (evenIfStaying) {
			companion.setOrderedToSit(false);
			companion.setInSittingPose(false);
		}

		ServerLevel target = player.level();

		if (companion.level() == target && companion.distanceToSqr(player) < FAR * FAR && !evenIfStaying) {
			return true;
		}

		Vec3 pos = companion.isOnShoulder() ? player.position() : safeSpot(target, player, companion);

		if (pos == null) {
			return false;     // flying over the void / no ground: later, once it has landed
		}

		if (companion.level() == target) {
			companion.snapTo(pos.x, pos.y, pos.z, player.getYRot(), 0.0f);
			companion.getNavigation().stop();
			companion.setDeltaMovement(Vec3.ZERO);
			companion.fallDistance = 0;
			companion.setPortalCooldown();
			return true;
		}

		Entity moved = companion.teleport(new TeleportTransition(target, pos, Vec3.ZERO, player.getYRot(), 0.0f, TeleportTransition.DO_NOTHING));

		if (moved != null) {
			moved.setPortalCooldown();
			CompanionData.get(target.getServer()).updateCompanion(player.getUUID(), moved.getUUID());
		}

		return moved != null;
	}

	// ------------------------------------------------------------------
	// Hotkey (v0.25.2): mushroom heals now / automatic <-> hotkey
	// ------------------------------------------------------------------

	public static void hotkey(ServerPlayer player, int action, int arg) {
		Companion companion = find(player.level().getServer(), player.getUUID());

		// Menu actions (v0.26) - also work remotely
		if (action >= CompanionActionPayload.STANCE) {
			if (action == CompanionActionPayload.CALL) {
				call(player.createCommandSourceStack());
				return;
			}

			if (companion == null) {
				player.sendOverlayMessage(Component.translatable(CompanionData.get(player.level().getServer()).hasCompanion(player.getUUID())
						? "jtechstorage.companion.call_unloaded" : "jtechstorage.companion.none").withStyle(ChatFormatting.GRAY));
				return;
			}

			switch (action) {
				case CompanionActionPayload.STANCE -> companion.setStance(player, arg);
				case CompanionActionPayload.LEARN -> companion.learnTalent(player, arg);
				case CompanionActionPayload.RESET -> companion.resetTalents(player);
				case CompanionActionPayload.BAG -> {
					if (companion instanceof RavenCompanion raven) {
						raven.openBag(player);
					}
				}
				case CompanionActionPayload.EXPLORE -> {
					if (companion instanceof RavenCompanion raven) {
						raven.explore(player);
					}
				}
				case CompanionActionPayload.SPECIAL -> companion.useSpecial(player, false);
				case CompanionActionPayload.LOOK -> companion.setLook(player, arg >> 8, arg & 0xFF);
				case CompanionActionPayload.GLOW -> companion.setGlowColor(player, arg);
				case CompanionActionPayload.PET -> companion.pet(player);
				case CompanionActionPayload.GESTURE -> companion.requestGesture(player, arg);
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
	// Mushroom tier 3 (v0.25.1): creative flight in the own base (3x3 chunks around an own terminal)
	// ------------------------------------------------------------------

	public static final String FLIGHT_TAG = "jtechstorage.shroom_flight";

	private static void shroomFlight(ServerPlayer player) {
		boolean has = player.entityTags().contains(FLIGHT_TAG);
		boolean other = player.isCreative() || player.isSpectator();
		boolean allowed = false;

		if (!other && player.isAlive()) {
			Companion companion = has || CompanionData.get(player.level().getServer()).hasCompanion(player.getUUID())
					? find(player.level().getServer(), player.getUUID()) : null;
			allowed = companion instanceof ShroomCompanion shroom && shroom.getStage() == 3 && !shroom.isExhausted()
					&& shroom.level() == player.level() && shroom.distanceToSqr(player) < 48 * 48
					&& Companion.ownTerminalAround(player.level(), player.blockPosition(), player.getUUID()) != null;
		}

		if (allowed && has && !player.getAbilities().mayfly) {
			player.getAbilities().mayfly = true;      // e.g. allow again after a game mode change
			player.onUpdateAbilities();
		} else if (allowed && !has) {
			player.addTag(FLIGHT_TAG);
			player.getAbilities().mayfly = true;
			player.onUpdateAbilities();
			player.sendOverlayMessage(Component.translatable("jtechstorage.shroom.flight_on").withStyle(ChatFormatting.LIGHT_PURPLE));
		} else if (!allowed && has) {
			player.removeTag(FLIGHT_TAG);

			if (!other) {
				if (player.getAbilities().flying) {
					player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 20 * 10, 0));
				}

				player.getAbilities().mayfly = false;
				player.getAbilities().flying = false;
				player.onUpdateAbilities();
				player.sendOverlayMessage(Component.translatable("jtechstorage.shroom.flight_off").withStyle(ChatFormatting.GRAY));
			}
		}
	}

	// ------------------------------------------------------------------
	// State for HUD and menu (v0.26)
	// ------------------------------------------------------------------

	/** Sends the player the state of their companion. */
	public static void sendStatus(ServerPlayer player) {
		MinecraftServer server = player.level().getServer();
		CompanionStatusPayload status;

		if (!CompanionData.get(server).hasCompanion(player.getUUID())) {
			status = CompanionStatusPayload.NONE;
		} else {
			Companion c = find(server, player.getUUID());

			if (c == null) {
				status = new CompanionStatusPayload(true, false, "", "", 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
						new int[CompanionTalents.COUNT], CompanionLooks.DEFAULT, -1, 0, 0, -1, "");
			} else {
				boolean sameLevel = c.level() == player.level();
				int flags = (c.isOrderedToSit() ? 1 : 0) | (c.isExhausted() ? 2 : 0) | (!sameLevel ? 32 : 0) | (c.isInBase() ? 64 : 0)
						| (c.isGuarding() ? 256 : 0) | (c.isOnShoulder() ? 512 : 0) | (player.entityTags().contains(FLIGHT_TAG) ? 1024 : 0);
				int special = 0;
				int specialMax = 0;
				int timer = 0;

				if (c instanceof ShroomCompanion shroom) {
					flags |= shroom.isAutoHeal() ? 4 : 0;
					special = shroom.healCooldown();
					specialMax = shroom.healCooldownMax();
				} else if (c instanceof MantisCompanion mantis) {
					special = mantis.getFlightEnergy();
					specialMax = mantis.maxFlight();
				} else if (c instanceof RavenCompanion raven) {
					flags |= (raven.isAway() ? 8 : 0) | (raven.hasTerminal() ? 16 : 0) | (raven.isExploring() ? 128 : 0);
					special = raven.bagUsed();
					specialMax = raven.capacity();
					timer = raven.exploreTimer();
				}

				String name = c.hasCustomName() && c.getCustomName() != null ? c.getCustomName().getString() : "";
				status = new CompanionStatusPayload(true, true, c.species(), name, c.getStage(), c.getLevel(), c.getXp(),
						c.getHealth(), c.getMaxHealth(), c.getStance(), flags, c.getXpBoostTicks(), special, specialMax,
						(float) c.getAttributeValue(Attributes.ATTACK_DAMAGE),
						(float) c.getAttributeValue(Attributes.ARMOR), c.effectiveAttackInterval(),
						sameLevel ? (int) Math.sqrt(c.distanceToSqr(player)) : -1, timer, c.specialCooldown(), c.specialCooldownMax(), c.talentRanks(),
						c.getLook(), c.getGlowColor(), c.getKills(), c.getBond(), c instanceof RavenCompanion r ? r.decoyCooldown() : -1,
						BuiltInRegistries.ITEM.getKey(c.favoriteFood()).toString());
			}
		}

		ServerPlayNetworking.send(player, status);
	}

	// ------------------------------------------------------------------
	// Sharing XP (from the player mixin)
	// ------------------------------------------------------------------

	/** Shares collected experience with the companion (if it is following and nearby); returns: the player's share. */
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
			part++;     // randomly round fractions up so that single XP points are shared too
		}

		part = Math.min(part, amount);
		companion.addSharedXp(part);
		return amount - part;
	}

	// ------------------------------------------------------------------
	// Tick: catching up companions, searching for geodes
	// ------------------------------------------------------------------

	private static void tick(MinecraftServer server) {
		int tick = server.getTickCount();
		Companion.tickStunned();
		tickFetch(server);

		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			int slot = Math.floorMod(player.getUUID().hashCode(), 200);

			if ((tick + slot) % 40 == 0 && !FETCH.containsKey(player.getUUID())) {
				if (find(server, player.getUUID()) != null) {
					bring(player, false);
				} else if ((tick + slot) % 200 == 0) {
					// not loaded but was following last (death, teleport, login far away): load chunk and fetch
					CompanionData.Last last = CompanionData.get(server).last(player.getUUID());

					if (last != null && last.follows()) {
						fetch(player, false);
					}
				}
			}

			if ((tick + slot) % 200 == 0) {
				scanForGeode(player);
			}

			if ((tick + slot) % 10 == 0) {
				shroomFlight(player);
			}

			if ((tick + slot) % 10 == 0 && ServerPlayNetworking.canSend(player,
					CompanionStatusPayload.TYPE)) {
				sendStatus(player);
			}
		}
	}

	private static long geodeKey(BlockPos pos) {
		return BlockPos.asLong(pos.getX() >> 4, pos.getY() >> 4, pos.getZ() >> 4);
	}

	/** New geode nearby? Then roll the dice once whether a geode keeper lives here (since v0.27 also for players with a companion - for trading). */
	private static void scanForGeode(ServerPlayer player) {
		ServerLevel level = player.level();
		CompanionData data = CompanionData.get(level.getServer());

		if (player.isSpectator() || player.getY() > 80 || level.dimension() != Level.OVERWORLD) {
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

		// the whole geode (radius of one chunk) counts as visited - each geode gets exactly one chance
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
			level.playSound(null, spot, ModSounds.KEEPER_GREET, SoundSource.NEUTRAL, 1.0f, 0.8f);
		}
	}

	/** Budding amethyst of a not yet visited geode - skips sections without amethyst. */
	private static @Nullable BlockPos findUnvisitedBudding(ServerLevel level, BlockPos center, int r, CompanionData data) {
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

		for (int cx = (center.getX() - r) >> 4; cx <= (center.getX() + r) >> 4; cx++) {
			for (int cz = (center.getZ() - r) >> 4; cz <= (center.getZ() + r) >> 4; cz++) {
				LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);

				if (chunk == null) {
					continue;
				}

				for (int sy = (center.getY() - r) >> 4; sy <= (center.getY() + r) >> 4; sy++) {
					int index = chunk.getSectionIndexFromSectionY(sy);

					if (index < 0 || index >= chunk.getSectionsCount()) {
						continue;
					}

					LevelChunkSection section = chunk.getSection(index);

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

	/** Free space inside the geode: two blocks of air above amethyst, as close as possible to the budding spot. */
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
	// Commands
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
			if (CompanionData.get(player.level().getServer()).hasCompanion(player.getUUID()) && fetch(player, true)) {
				source.sendSuccess(() -> Component.translatable("jtechstorage.companion.fetching").withStyle(ChatFormatting.LIGHT_PURPLE), false);
				return 1;
			}

			source.sendFailure(Component.translatable(CompanionData.get(player.level().getServer()).hasCompanion(player.getUUID())
					? "jtechstorage.companion.call_unloaded" : "jtechstorage.companion.none"));
			return 0;
		}

		if (!bring(player, true)) {
			FETCH.put(player.getUUID(), new int[] {FETCH_TICKS, 1});
			source.sendSuccess(() -> Component.translatable("jtechstorage.companion.no_ground").withStyle(ChatFormatting.GRAY), false);
			return 1;
		}

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
