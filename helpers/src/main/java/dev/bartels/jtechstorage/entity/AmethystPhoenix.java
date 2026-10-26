package dev.bartels.jtechstorage.entity;

import dev.bartels.jtechstorage.ModAdvancements;
import dev.bartels.jtechstorage.block.PhoenixStopBlockEntity;
import dev.bartels.jtechstorage.helpers.HelpersContent;
import dev.bartels.jtechstorage.item.FlightPlanItem;
import java.util.ArrayList;
import java.util.List;

import java.util.Optional;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HasCustomInventoryScreen;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.portal.TeleportTransition;
import org.jspecify.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.Prediction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemStackWithSlot;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;

import dev.bartels.jtechstorage.ModConfig;
import dev.bartels.jtechstorage.ModSounds;
import dev.bartels.jtechstorage.menu.AmethystPhoenixMenu;

/**
 * Amethyst Phoenix – the evolution stage of the phoenix (right-click with an amethyst obsidian upgrade).
 * Large, two seats for players, up to four equippable chests. With a flight plan it gets 2–6 stops
 * and then flies to them on its own in order; each stop sets its own rest duration.
 * Since v0.41 the stops may lie in different dimensions: it switches by flame jump (with riders
 * and cargo) and reappears above the next stop. If it gets stuck on the way (e.g. in the caves
 * of the Crystal Depths), it likewise jumps directly above the stop.
 * Whoever sits in front can start earlier with the jump key. The more chests and cargo, the slower it flies.
 * In sit mode it does not move.
 */
public class AmethystPhoenix extends NetworkHelperMob implements HasCustomInventoryScreen {
	public static final int CHEST_SLOTS = 27;
	public static final int MAX_CHESTS = 4;
	public static final int STATE_IDLE = 0;
	public static final int STATE_FLYING = 1;
	public static final int STATE_RESTING = 2;
	/** Cruise speed without load (blocks per tick). */
	private static final double BASE_SPEED = ModConfig.get().helpers.amethystPhoenixBlocksPerSecond / 20.0;
	private static final double CRUISE_HEIGHT = 18.0;

	private static final EntityDataAccessor<Integer> DATA_STATE = SynchedEntityData.defineId(AmethystPhoenix.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> DATA_CHESTS = SynchedEntityData.defineId(AmethystPhoenix.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Boolean> DATA_SITTING = SynchedEntityData.defineId(AmethystPhoenix.class, EntityDataSerializers.BOOLEAN);

	private final SimpleContainer cargo = new SimpleContainer(CHEST_SLOTS * MAX_CHESTS) {
		@Override
		public void setChanged() {
			super.setChanged();
			updateLoad();
		}
	};
	/** Stop on the route (name as a copy in case the block is not currently loaded). */
	public record RouteStop(BlockPos pos, String name, Optional<ResourceKey<Level>> dimension) {
		public static final com.mojang.serialization.Codec<RouteStop> CODEC = com.mojang.serialization.codecs.RecordCodecBuilder.create(
				instance -> instance.group(
						BlockPos.CODEC.fieldOf("pos").forGetter(RouteStop::pos),
						com.mojang.serialization.Codec.STRING.fieldOf("name").forGetter(RouteStop::name),
						ResourceKey.codec(Registries.DIMENSION).optionalFieldOf("dimension")
								.forGetter(RouteStop::dimension)
				).apply(instance, RouteStop::new));

		/** Is the stop in this dimension? (if unspecified: yes – routes from before v0.41) */
		public boolean in(Level level) {
			return dimension.isEmpty() || dimension.get().equals(level.dimension());
		}

		RouteStop withName(String newName) {
			return new RouteStop(pos, newName, dimension);
		}
	}

	/** Duration of the ascent before a flame jump. */
	private static final int JUMP_TICKS = 30;
	/** How long (ticks) it may be stuck before it jumps directly above the stop. */
	private static final int STUCK_TICKS = 160;
	private int jumpTicks;
	private int stuckTicks;

	private final List<RouteStop> stops = new ArrayList<>();
	/** Index of the stop it is flying to or resting at. */
	private int target;
	private long departAt;
	private double cruiseY;
	private double speedFactor = 1.0;
	private int fillPercent;

	public AmethystPhoenix(EntityType<? extends AmethystPhoenix> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return createMobAttributes()
				.add(Attributes.MAX_HEALTH, 80.0)
				.add(Attributes.MOVEMENT_SPEED, 0.18)
				.add(Attributes.FLYING_SPEED, 0.3)
				.add(Attributes.FOLLOW_RANGE, 16.0)
				.add(Attributes.STEP_HEIGHT, 1.0);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DATA_STATE, STATE_IDLE);
		builder.define(DATA_CHESTS, 0);
		builder.define(DATA_SITTING, false);
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 8.0f) {
			@Override
			public boolean canUse() {
				return getState() != STATE_FLYING && super.canUse();
			}
		});
	}

	// ------------------------------------------------------------------
	// State
	// ------------------------------------------------------------------

	public int getState() {
		return entityData.get(DATA_STATE);
	}

	public boolean isFlying() {
		return getState() == STATE_FLYING && !isSitting();
	}

	public int getChests() {
		return entityData.get(DATA_CHESTS);
	}

	public boolean isSitting() {
		return entityData.get(DATA_SITTING);
	}

	public SimpleContainer getCargo() {
		return cargo;
	}

	public List<RouteStop> getStops() {
		return List.copyOf(stops);
	}

	private boolean hasRoute() {
		return stops.size() >= 2;
	}

	private @Nullable RouteStop targetStop() {
		return target >= 0 && target < stops.size() ? stops.get(target) : null;
	}

	public int getTargetStop() {
		return target;
	}

	/** Speed in percent (100 = no load). */
	public int getSpeedPercent() {
		return (int) Math.round(speedFactor * 100);
	}

	public int getFillPercent() {
		return fillPercent;
	}

	/** Minutes until departure (only while resting), otherwise -1. */
	public int getMinutesUntilDeparture() {
		if (getState() != STATE_RESTING || isSitting()) {
			return -1;
		}

		return (int) Math.max(0, (departAt - routeClock() + 1199) / 1200);
	}

	private void setState(int state) {
		entityData.set(DATA_STATE, state);
		setNoGravity(state == STATE_FLYING);
	}

	public void setSitting(boolean sitting) {
		entityData.set(DATA_SITTING, sitting);

		if (sitting) {
			setNoGravity(false);
			getNavigation().stop();
		} else {
			setNoGravity(getState() == STATE_FLYING);
			cruiseY = getY();
		}
	}

	@Override
	protected boolean canRest() {
		return false;
	}

	@Override
	protected String unlinkedStatus() {
		return "jtechstorage.amethyst_phoenix.status.no_route";
	}

	@Override
	protected String linkAdvancement() {
		return "phoenix";
	}

	/** Speed factor from chest count and fill level. */
	private void updateLoad() {
		int chests = getChests();
		double units = 0;

		for (int slot = 0; slot < chests * CHEST_SLOTS; slot++) {
			ItemStack stack = cargo.getItem(slot);

			if (!stack.isEmpty()) {
				units += (double) stack.getCount() / stack.getMaxStackSize();
			}
		}

		double fill = chests == 0 ? 0.0 : units / (chests * CHEST_SLOTS);
		fillPercent = (int) Math.round(fill * 100);
		speedFactor = Math.max(0.35, 1.0 - 0.08 * chests - 0.3 * fill);
	}

	// ------------------------------------------------------------------
	// Route
	// ------------------------------------------------------------------

	/** New route from the flight plan. It first flies to the first stop (if already there, it rests right away). */
	public void setRoute(List<RouteStop> route) {
		stops.clear();

		for (RouteStop stop : route) {
			if (stops.size() < FlightPlanItem.MAX_STOPS) {
				stops.add(new RouteStop(stop.pos().immutable(), stop.name(), stop.dimension()));
			}
		}

		target = 0;

		if (!hasRoute()) {
			setState(STATE_IDLE);
		} else if (stops.getFirst().in(level()) && position().distanceTo(landing(stops.getFirst().pos())) < 8.0) {
			arrive();
		} else {
			takeOff();
		}
	}

	/** Route from a flight plan (since v0.41 stops may lie in different dimensions). */
	private List<RouteStop> routeFrom(List<FlightPlanItem.Stop> plan) {
		List<RouteStop> route = new ArrayList<>();

		for (FlightPlanItem.Stop stop : plan) {
			route.add(new RouteStop(stop.pos().pos(), stop.name(), Optional.of(stop.pos().dimension())));
		}

		return route;
	}

	private List<FlightPlanItem.Stop> toPlan() {
		List<FlightPlanItem.Stop> plan = new ArrayList<>();

		for (RouteStop stop : stops) {
			plan.add(new FlightPlanItem.Stop(GlobalPos.of(stop.dimension().orElse(level().dimension()), stop.pos()), stop.name()));
		}

		return plan;
	}

	/** Route as text: “Harbor → Mine → Farm → …”. */
	public Component routeText() {
		MutableComponent text = Component.empty();

		for (int i = 0; i < stops.size(); i++) {
			if (i > 0) {
				text.append(" → ");
			}

			text.append(stopName(i));
		}

		return text.append(" ↺");
	}

	private static Vec3 landing(BlockPos stop) {
		return new Vec3(stop.getX() + 0.5, stop.getY() + 1.0, stop.getZ() + 0.5);
	}

	private void takeOff() {
		setState(STATE_FLYING);
		cruiseY = getY();
		jumpTicks = 0;
		stuckTicks = 0;
		playSound(ModSounds.AMETHYST_PHOENIX_DEPART, 1.5f, 1.0f);
	}

	/** Departure from the stop where it is currently resting. */
	public boolean departNow() {
		if (getState() != STATE_RESTING || isSitting() || !hasRoute()) {
			return false;
		}

		target = (target + 1) % stops.size();
		takeOff();
		tellPassengers(Component.translatable("jtechstorage.amethyst_phoenix.depart", stopName(target)));

		if (level() instanceof ServerLevel level) {
			level.sendParticles(ParticleTypes.FLAME, getX(), getY() + 0.5, getZ(), 30, 0.8, 0.3, 0.8, 0.05);
			level.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 0.5, getZ(), 15, 0.8, 0.3, 0.8, 0.05);
		}

		return true;
	}

	private void arrive() {
		setState(STATE_RESTING);
		setDeltaMovement(Vec3.ZERO);
		PhoenixStopBlockEntity.Rest rest = PhoenixStopBlockEntity.Rest.NIGHT;
		RouteStop stop = targetStop();

		// Read rest duration (and current name) from the stop – if the block is missing, it rests one night
		if (stop != null && stop.in(level()) && level().getBlockEntity(stop.pos()) instanceof PhoenixStopBlockEntity block) {
			rest = block.getRest();

			if (block.hasName()) {
				stops.set(target, stop.withName(block.getStopName()));
			}

			// Delivery order: swap with the chests next to the stop
			if (level() instanceof ServerLevel serverLevel && getChests() > 0) {
				PhoenixStopBlockEntity.Exchange exchange = block.exchange(serverLevel, cargo, getChests() * CHEST_SLOTS);

				if (exchange.unloaded() > 0 || exchange.loaded() > 0) {
					tellPassengers(Component.translatable("jtechstorage.amethyst_phoenix.exchange", exchange.unloaded(), exchange.loaded()));
					serverLevel.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 0.6, getZ(), 12, 0.8, 0.3, 0.8, 0.02);
				}
			}
		}

		departAt = nextDeparture(routeClock(), rest);
		playSound(SoundEvents.AMETHYST_BLOCK_CHIME, 1.2f, 0.8f);
		tellPassengers(Component.translatable("jtechstorage.amethyst_phoenix.arrived", stopName(target), Component.translatable(rest.key())));
	}

	/**
	 * Clock for rest and departure: always the Overworld's (v0.41). In the Crystal Depths the dimension's own clock stands still – it
	 * would otherwise never fly off again there; this way the time also stays consistent across a dimension change.
	 */
	private long routeClock() {
		return level() instanceof ServerLevel level ? level.getServer().overworld().getDefaultClockTime() : level().getDefaultClockTime();
	}

	static long nextDeparture(long time, PhoenixStopBlockEntity.Rest rest) {
		return rest.ticks > 0 ? time + rest.ticks : nextDeparture(time);
	}

	/** Next sunrise after a whole night (13000–24000) has passed. */
	static long nextDeparture(long time) {
		long day = Math.floorDiv(time, 24000L);
		long nightStart = day * 24000L + 13000L;

		if (Math.floorMod(time, 24000L) >= 13000L) {
			nightStart += 24000L;
		}

		return nightStart + 11000L;
	}

	private Component stopName(int index) {
		if (index < 0 || index >= stops.size()) {
			return Component.literal("?");
		}

		String name = stops.get(index).name();
		return name.isBlank() ? Component.translatable("jtechstorage.amethyst_phoenix.stop", index + 1) : Component.literal(name);
	}

	private void tellPassengers(Component message) {
		for (Entity passenger : getPassengers()) {
			if (passenger instanceof Player player) {
				player.sendSystemMessage(message.copy().withStyle(ChatFormatting.LIGHT_PURPLE));
			}
		}
	}

	// ------------------------------------------------------------------
	// Behavior
	// ------------------------------------------------------------------

	@Override
	public void aiStep() {
		if (!isHologram() && isFlying() && tickCount % 30 == 0) {
			playSound(ModSounds.AMETHYST_PHOENIX_FLAP, 0.9f, 0.9f + random.nextFloat() * 0.15f);
		}

		if (level() instanceof ServerLevel level && !isHologram()) {
			serverMovement(level);
		}

		super.aiStep();

		if (level() instanceof ServerLevel level && !isHologram() && tickCount % 20 == 3) {
			updateStatus();

			// Keep chunks along the route loaded (like an ender pearl) so it keeps shuttling even without a player
			if (hasRoute() && !isSitting()) {
				level.getChunkSource().addTicketWithRadius(TicketType.ENDER_PEARL, ChunkPos.containing(blockPosition()), 2);
			}
		}

		if (level().isClientSide() && isFlying() && tickCount % 3 == 0) {
			level().addParticle(ParticleTypes.FLAME, getX() - Mth.sin(yBodyRot * Mth.DEG_TO_RAD) * -1.4, getY() + 0.8,
					getZ() + Mth.cos(yBodyRot * Mth.DEG_TO_RAD) * -1.4, 0.0, -0.02, 0.0);
		}
	}

	private void serverMovement(ServerLevel level) {
		if (isSitting()) {
			// sinks gently to the ground and stays there
			Vec3 motion = getDeltaMovement();
			setDeltaMovement(motion.x * 0.6, Math.max(motion.y, -0.25), motion.z * 0.6);
			return;
		}

		int state = getState();

		if (state == STATE_RESTING) {
			if (hasRoute() && routeClock() >= departAt) {
				departNow();
			}

			return;
		}

		if (state != STATE_FLYING) {
			return;
		}

		RouteStop stop = targetStop();

		if (stop == null) {
			setState(STATE_IDLE);
			return;
		}

		if (!stop.in(level)) {
			jumpTick(level, stop);
			return;
		}

		Vec3 goal = landing(stop.pos());
		Vec3 pos = position();
		Vec3 flat = new Vec3(goal.x - pos.x, 0, goal.z - pos.z);
		double distance = flat.length();
		double speed = BASE_SPEED * speedFactor;

		if (distance < 0.6 && Math.abs(goal.y - pos.y) < 0.8) {
			setPos(goal.x, goal.y, goal.z);
			arrive();
			return;
		}

		Vec3 dir = distance > 1.0E-4 ? flat.scale(1.0 / distance) : Vec3.ZERO;
		double desiredY;

		if (distance > 14.0 && level.dimensionType().hasCeiling()) {
			// Cave dimension (Crystal Depths): stay under the ceiling ahead rather than above the terrain
			int roof = roofAbove(level, Mth.floor(pos.x + dir.x * 4), Mth.floor(pos.y), Mth.floor(pos.z + dir.z * 4));
			desiredY = Math.min(Math.max(goal.y + 4.0, pos.y), roof - 3.0);
		} else if (distance > 14.0) {
			// Cruising altitude: above the terrain ahead, at least CRUISE_HEIGHT above start/destination
			int ahead = level.getHeight(Heightmap.Types.MOTION_BLOCKING, Mth.floor(pos.x + dir.x * 8), Mth.floor(pos.z + dir.z * 8));
			int below = level.getHeight(Heightmap.Types.MOTION_BLOCKING, Mth.floor(pos.x), Mth.floor(pos.z));
			cruiseY = Math.max(cruiseY, Math.max(goal.y + CRUISE_HEIGHT, Math.max(ahead, below) + 8.0));
			cruiseY = Math.min(cruiseY, level.getMaxY() - 4);
			desiredY = cruiseY;
		} else {
			desiredY = goal.y;
		}

		double horizontal = speed * Math.min(1.0, distance / 6.0);

		// first climb, then fly: if far below cruising altitude, hardly any forward motion
		if (desiredY - pos.y > 6.0 && distance > 14.0) {
			horizontal *= 0.35;
		}

		double vertical = Mth.clamp((desiredY - pos.y) * 0.12, -speed, speed);

		if (horizontalCollision) {
			vertical = speed;
			cruiseY = pos.y + 6.0;
		}

		// if stuck (wall, cave), after a while it jumps directly above the stop
		if (horizontalCollision || (verticalCollision && distance > 3.0)) {
			if (++stuckTicks > STUCK_TICKS && distance > 3.0) {
				stuckTicks = 0;
				Vec3 above = arrivalAbove(level, stop.pos());
				flameBurst(level);
				teleportTo(above.x, above.y, above.z);
				flameBurst(level);
				cruiseY = above.y;
				return;
			}
		} else if (stuckTicks > 0) {
			stuckTicks--;
		}

		setDeltaMovement(dir.x * horizontal, vertical, dir.z * horizontal);

		if (distance > 0.5) {
			float yaw = (float) (Mth.atan2(dir.z, dir.x) * Mth.RAD_TO_DEG) - 90.0f;
			setYRot(Mth.approachDegrees(getYRot(), yaw, 6.0f));
			yBodyRot = getYRot();
			yHeadRot = getYRot();
		}
	}

	/** Flame jump into the dimension of the next stop: first steeply upward, then vanish and reappear there. */
	private void jumpTick(ServerLevel level, RouteStop stop) {
		if (jumpTicks == 0) {
			tellPassengers(Component.translatable("jtechstorage.amethyst_phoenix.jump", stopName(target)));
			playSound(ModSounds.AMETHYST_PHOENIX_DEPART, 1.5f, 0.8f);
		}

		jumpTicks++;
		setDeltaMovement(0.0, verticalCollision ? 0.0 : 0.3 + jumpTicks * 0.01, 0.0);
		level.sendParticles(ParticleTypes.FLAME, getX(), getY() + 0.5, getZ(), 8, 0.6, 0.4, 0.6, 0.03);
		level.sendParticles(ParticleTypes.WITCH, getX(), getY() + 0.5, getZ(), 3, 0.6, 0.4, 0.6, 0.02);

		if (jumpTicks < JUMP_TICKS) {
			return;
		}

		jumpTicks = 0;
		ServerLevel to = stop.dimension().map(key -> level.getServer().getLevel(key)).orElse(null);

		if (to == null) {
			// Dimension no longer exists: skip stop
			target = (target + 1) % stops.size();
			return;
		}

		flameBurst(level);
		to.getChunk(stop.pos());
		Vec3 above = arrivalAbove(to, stop.pos());
		Entity moved = teleport(new TeleportTransition(to, above, Vec3.ZERO, getYRot(), 0.0f,
				TeleportTransition.PLACE_PORTAL_TICKET));

		if (moved instanceof AmethystPhoenix arrived) {
			arrived.cruiseY = above.y;
			arrived.flameBurst(to);
		}
	}

	private void flameBurst(ServerLevel level) {
		level.sendParticles(ParticleTypes.FLAME, getX(), getY() + 0.8, getZ(), 50, 0.8, 0.8, 0.8, 0.12);
		level.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 0.8, getZ(), 20, 0.6, 0.6, 0.6, 0.08);
		level.playSound(null, getX(), getY(), getZ(), SoundEvents.BLAZE_SHOOT, getSoundSource(), 1.0f, 1.2f);
	}

	/** Free spot above the stop (up to 10 blocks high, as far as space allows). */
	private static Vec3 arrivalAbove(ServerLevel level, BlockPos stop) {
		BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
		int best = 1;

		for (int up = 1; up <= 10; up++) {
			boolean free = true;

			for (int h = 0; h < 3 && free; h++) {
				free = level.getBlockState(p.set(stop.getX(), stop.getY() + up + h, stop.getZ())).getCollisionShape(level, p).isEmpty();
			}

			if (!free) {
				break;
			}

			best = up;
		}

		return new Vec3(stop.getX() + 0.5, stop.getY() + best, stop.getZ() + 0.5);
	}

	/** First solid ceiling above (x, y, z), at most 32 blocks up. */
	private static int roofAbove(ServerLevel level, int x, int y, int z) {
		BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();

		for (int up = 1; up <= 32; up++) {
			if (!level.getBlockState(p.set(x, y + up, z)).getCollisionShape(level, p).isEmpty()) {
				return y + up;
			}
		}

		return y + 32;
	}

	@Override
	public void travel(Vec3 input) {
		if (isFlying()) {
			move(MoverType.SELF, getDeltaMovement());
			return;
		}

		super.travel(input);
	}

	private void updateStatus() {
		if (isSitting()) {
			setStatus("jtechstorage.amethyst_phoenix.status.sitting");
		} else if (!hasRoute()) {
			setStatus(unlinkedStatus());
		} else if (getState() == STATE_FLYING) {
			RouteStop stop = targetStop();

			if (stop != null && !stop.in(level())) {
				setStatus("jtechstorage.amethyst_phoenix.status.jumping", stopName(target));
				return;
			}

			int distance = stop == null ? 0 : (int) Math.sqrt(stop.pos().distToCenterSqr(position()));
			setStatus("jtechstorage.amethyst_phoenix.status.flying", stopName(target), distance, getSpeedPercent());
		} else if (getState() == STATE_RESTING) {
			setStatus("jtechstorage.amethyst_phoenix.status.resting", stopName(target), getMinutesUntilDeparture());
		} else {
			setStatus(unlinkedStatus());
		}
	}

	// ------------------------------------------------------------------
	// Seats
	// ------------------------------------------------------------------

	@Override
	protected boolean canAddPassenger(Entity passenger) {
		return getPassengers().size() < 2;
	}

	@Override
	protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float scale) {
		int index = Math.max(0, getPassengers().indexOf(passenger));
		double z = index == 0 ? 0.25 : -0.5;
		double drop = isSitting() || isNightRest() ? 0.62 : 0.0;   // when sitting/sleeping, the belly rests on the ground
		return new Vec3(0.0, dimensions.height() * 0.85 - drop, z).yRot(-getYRot() * Mth.DEG_TO_RAD);
	}

	@Override
	public net.minecraft.world.entity.@Nullable LivingEntity getControllingPassenger() {
		return null;   // always flies by itself – players only travel along
	}

	// ------------------------------------------------------------------
	// Interaction
	// ------------------------------------------------------------------

	/** Items that trigger their own action – the off hand must not additionally trigger “mount”. */
	private static boolean isActionItem(ItemStack stack) {
		return stack.is(Items.CHEST) || stack.is(HelpersContent.FLIGHT_PLAN);
	}

	@Override
	protected InteractionResult mobInteract(Player player, InteractionHand hand) {
		ItemStack held = player.getItemInHand(hand);

		// Client: report chest/feather immediately as “consumed”, otherwise the game then tries the (empty) off hand
		// and the player would suddenly sit on the phoenix.
		if (level().isClientSide() && isActionItem(held)) {
			return InteractionResult.SUCCESS;
		}

		// Mounting / menu only with the main hand – never via an empty off hand
		if (hand == InteractionHand.OFF_HAND && held.isEmpty()) {
			return InteractionResult.PASS;
		}

		return super.mobInteract(player, hand);
	}

	@Override
	protected InteractionResult interactWith(Player player, InteractionHand hand) {
		ItemStack held = player.getItemInHand(hand);

		if (held.is(Items.CHEST)) {
			if (getChests() >= MAX_CHESTS) {
				player.sendOverlayMessage(Component.translatable("jtechstorage.amethyst_phoenix.chests_full"));
				return InteractionResult.FAIL;
			}

			entityData.set(DATA_CHESTS, getChests() + 1);
			updateLoad();
			held.consume(1, player);
			playSound(SoundEvents.ARMOR_EQUIP_LEATHER.value(), 1.0f, 0.9f);
			player.sendOverlayMessage(Component.translatable("jtechstorage.amethyst_phoenix.chest_added", getChests(), MAX_CHESTS));
			return InteractionResult.SUCCESS;
		}

		if (held.is(HelpersContent.FLIGHT_PLAN)) {
			List<FlightPlanItem.Stop> plan = FlightPlanItem.stops(held);

			if (plan.size() < 2) {
				player.sendOverlayMessage(Component.translatable("jtechstorage.flight_plan.too_short"));
				return InteractionResult.FAIL;
			}

			setRoute(routeFrom(plan));
			playSound(SoundEvents.AMETHYST_BLOCK_CHIME, 1.0f, 1.4f);
			player.sendOverlayMessage(Component.translatable("jtechstorage.amethyst_phoenix.route_set", routeText()));
			return InteractionResult.SUCCESS;
		}

		if (hand != InteractionHand.MAIN_HAND) {
			return InteractionResult.PASS;
		}

		if (held.isEmpty() && player.isSecondaryUseActive()) {
			pack(player);
			return InteractionResult.SUCCESS;
		}

		if (held.isEmpty()) {
			if (player.isPassenger()) {
				return InteractionResult.PASS;
			}

			if (getPassengers().size() < 2) {
				player.startRiding(this);
				player.sendOverlayMessage(statusText());
				return InteractionResult.SUCCESS;
			}

			player.sendOverlayMessage(Component.translatable("jtechstorage.amethyst_phoenix.seats_full"));
			return InteractionResult.SUCCESS;
		}

		return InteractionResult.PASS;
	}

	/** Remove the last chest (only if empty). */
	public boolean removeChest(Player player) {
		int chests = getChests();

		if (chests == 0) {
			return false;
		}

		for (int slot = (chests - 1) * CHEST_SLOTS; slot < chests * CHEST_SLOTS; slot++) {
			if (!cargo.getItem(slot).isEmpty()) {
				player.sendOverlayMessage(Component.translatable("jtechstorage.amethyst_phoenix.chest_not_empty"));
				return false;
			}
		}

		entityData.set(DATA_CHESTS, chests - 1);
		updateLoad();
		player.getInventory().placeItemBackInInventory(new ItemStack(Items.CHEST), Prediction.SERVER_ONLY);
		playSound(SoundEvents.ARMOR_EQUIP_LEATHER.value(), 1.0f, 1.1f);
		return true;
	}

	/** Inventory key while riding: menu with chests, status and buttons. */
	@Override
	public void openCustomInventoryScreen(Player player) {
		if (player instanceof ServerPlayer serverPlayer && mayInteract(player)) {
			serverPlayer.openMenu(new MenuProvider());
		}
	}

	/** Packing up: chests with cargo and the route go into the item; fellow passengers dismount. */
	public boolean pack(Player player) {
		ejectPassengers();
		ItemStack item = createPackedItem(HelpersContent.AMETHYST_PHOENIX_ITEM, player);

		if (getChests() > 0) {
			List<ItemStack> items = new ArrayList<>();

			for (int slot = 0; slot < cargo.getContainerSize(); slot++) {
				items.add(cargo.getItem(slot).copy());
			}

			item.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(items));
			CompoundTag tag = new CompoundTag();
			tag.putInt("Chests", getChests());
			item.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
			cargo.clearContent();
		}

		if (hasRoute()) {
			item.set(HelpersContent.FLIGHT_PLAN_STOPS, List.copyOf(toPlan()));
		}

		player.getInventory().placeItemBackInInventory(item, Prediction.SERVER_ONLY);
		playSound(SoundEvents.FIRECHARGE_USE, 0.6f, 1.4f);
		discard();
		return true;
	}

	/** When placed from the item: take over chests, cargo and route. */
	public void loadFromItem(ItemStack stack) {
		CustomData data = stack.get(DataComponents.CUSTOM_DATA);
		int chests = data != null ? Math.clamp(data.copyTag().getIntOr("Chests", 0), 0, MAX_CHESTS) : 0;

		if (chests > 0) {
			entityData.set(DATA_CHESTS, chests);
			ItemContainerContents contents = stack.get(DataComponents.CONTAINER);

			if (contents != null) {
				NonNullList<ItemStack> items = NonNullList.withSize(cargo.getContainerSize(), ItemStack.EMPTY);
				contents.copyInto(items);

				for (int slot = 0; slot < items.size(); slot++) {
					cargo.setItem(slot, items.get(slot));
				}
			}

			updateLoad();
		}

		List<FlightPlanItem.Stop> plan = stack.get(HelpersContent.FLIGHT_PLAN_STOPS);
		GlobalPos a = stack.get(HelpersContent.ROUTE_A);
		GlobalPos b = stack.get(HelpersContent.ROUTE_B);

		if (plan == null && a != null && b != null) {
			// Phoenix item from v0.20–v0.24 (route feather)
			plan = List.of(new FlightPlanItem.Stop(a, "A"), new FlightPlanItem.Stop(b, "B"));
		}

		List<RouteStop> route = plan == null ? null : routeFrom(plan);

		if (route != null && route.size() >= 2) {
			setRoute(route);
		}
	}

	/** Evolution: a phoenix becomes an Amethyst Phoenix (owner, network, color, name and hat are kept). */
	public static void evolve(Phoenix phoenix, ServerLevel level, Player player) {
		AmethystPhoenix grown = HelpersContent.AMETHYST_PHOENIX.create(level, EntitySpawnReason.CONVERSION);

		if (grown == null) {
			return;
		}

		grown.snapTo(phoenix.getX(), phoenix.getY(), phoenix.getZ(), phoenix.getYRot(), 0.0f);
		grown.owner = phoenix.owner != null ? phoenix.owner : player.getUUID();
		grown.terminalPos = phoenix.terminalPos;
		grown.setAccentColor(phoenix.getAccentColor());

		if (phoenix.hasCustomName()) {
			grown.setCustomName(phoenix.getCustomName());
		}

		ItemStack hat = phoenix.getItemBySlot(EquipmentSlot.HEAD);

		if (!hat.isEmpty()) {
			grown.setItemSlot(EquipmentSlot.HEAD, hat.copy());
		}

		level.addFreshEntity(grown);
		phoenix.discard();
		level.sendParticles(ParticleTypes.FLAME, grown.getX(), grown.getY() + 1, grown.getZ(), 80, 1.0, 1.0, 1.0, 0.1);
		level.sendParticles(ParticleTypes.END_ROD, grown.getX(), grown.getY() + 1, grown.getZ(), 40, 1.0, 1.0, 1.0, 0.1);
		level.sendParticles(ParticleTypes.WITCH, grown.getX(), grown.getY() + 1, grown.getZ(), 40, 1.0, 1.0, 1.0, 0.1);
		grown.playSound(SoundEvents.TOTEM_USE, 1.0f, 1.2f);
		grown.playSound(ModSounds.AMETHYST_PHOENIX_EVOLVE, 2.0f, 1.0f);
		player.sendSystemMessage(Component.translatable("jtechstorage.amethyst_phoenix.evolved").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));

		if (player instanceof ServerPlayer serverPlayer) {
			ModAdvancements.grant(serverPlayer, "amethyst_phoenix");
		}
	}

	// ------------------------------------------------------------------
	// Odds and ends & saving
	// ------------------------------------------------------------------

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		return false;
	}

	@Override
	public boolean isPushable() {
		return !isFlying() && super.isPushable();
	}

	@Override
	protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
		super.dropCustomDeathLoot(level, source, recentlyHit);

		for (ItemStack stack : cargo.removeAllItems()) {
			spawnAtLocation(level, stack);
		}

		if (getChests() > 0) {
			spawnAtLocation(level, new ItemStack(Items.CHEST, getChests()));
		}
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putInt("Chests", getChests());
		output.putInt("State", getState());
		output.putBoolean("Sitting", isSitting());
		output.putInt("Target", target);
		output.putLong("DepartAt", departAt);
		output.putDouble("CruiseY", cruiseY);

		output.store("Stops", RouteStop.CODEC.listOf(), List.copyOf(stops));

		ValueOutput.TypedOutputList<ItemStackWithSlot> items = output.list("Cargo", ItemStackWithSlot.CODEC);

		for (int slot = 0; slot < cargo.getContainerSize(); slot++) {
			ItemStack stack = cargo.getItem(slot);

			if (!stack.isEmpty()) {
				items.add(new ItemStackWithSlot(slot, stack));
			}
		}
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		entityData.set(DATA_CHESTS, Math.clamp(input.getIntOr("Chests", 0), 0, MAX_CHESTS));
		target = input.getIntOr("Target", 0);
		departAt = input.getLongOr("DepartAt", 0L);
		cruiseY = input.getDoubleOr("CruiseY", getY());
		stops.clear();
		for (RouteStop stop : input.read("Stops", RouteStop.CODEC.listOf()).orElse(List.of())) {
			// Routes from before v0.41 have no dimension: use the one it is currently in
			stops.add(stop.dimension().isPresent() ? stop : new RouteStop(stop.pos(), stop.name(), Optional.of(level().dimension())));
		}

		BlockPos legacyA = input.read("StopA", BlockPos.CODEC).orElse(null);
		BlockPos legacyB = input.read("StopB", BlockPos.CODEC).orElse(null);

		if (stops.isEmpty() && legacyA != null && legacyB != null) {
			stops.add(new RouteStop(legacyA, "A", Optional.of(level().dimension())));
			stops.add(new RouteStop(legacyB, "B", Optional.of(level().dimension())));
		}

		target = stops.isEmpty() ? 0 : Math.floorMod(target, stops.size());
		setState(input.getIntOr("State", STATE_IDLE));
		entityData.set(DATA_SITTING, input.getBooleanOr("Sitting", false));

		if (isSitting()) {
			setNoGravity(false);
		}

		for (ItemStackWithSlot item : input.listOrEmpty("Cargo", ItemStackWithSlot.CODEC)) {
			if (item.isValidInContainer(cargo.getContainerSize())) {
				cargo.setItem(item.slot(), item.stack());
			}
		}

		updateLoad();
	}

	private class MenuProvider implements ExtendedMenuProvider<AmethystPhoenixMenu.OpenData> {
		@Override
		public AmethystPhoenixMenu.OpenData getScreenOpeningData(ServerPlayer player) {
			List<String> names = new ArrayList<>();

			for (int i = 0; i < stops.size(); i++) {
				names.add(stopName(i).getString());
			}

			return new AmethystPhoenixMenu.OpenData(getId(), names, target);
		}

		@Override
		public Component getDisplayName() {
			return AmethystPhoenix.this.getDisplayName();
		}

		@Override
		public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
			return new AmethystPhoenixMenu(containerId, inventory, AmethystPhoenix.this);
		}
	}


	@Override
	protected SoundEvent helperAmbientSound() {
		return isNightRest() ? ModSounds.AMETHYST_PHOENIX_SLEEP : ModSounds.AMETHYST_PHOENIX_AMBIENT;
	}

	/** At night on the ground (stop, nest or sitting): sleeps with its head in its back feathers – purely visual. */
	public boolean isNightRest() {
		long time = Math.floorMod(level().getDefaultClockTime(), 24000L);
		return !isHologram() && getState() != STATE_FLYING && onGround() && time >= 13000L && time <= 23000L;
	}

	/** Too big or has its own sleeping spot – not a helper bed. */
	@Override
	protected int bedKind() {
		return 0;
	}
}
