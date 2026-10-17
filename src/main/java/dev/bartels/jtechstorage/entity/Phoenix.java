package dev.bartels.jtechstorage.entity;

import java.util.EnumSet;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Prediction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

import dev.bartels.jtechstorage.ModSounds;
import dev.bartels.jtechstorage.ModContent;
import dev.bartels.jtechstorage.block.PhoenixNestBlockEntity;

/**
 * Amethyst-Phönix – der Post-Bote. Er wohnt in einem Phönix-Horst (der nächste Horst seines Netzwerks im Umkreis
 * von {@value #NEST_SEARCH} Blöcken), sitzt dort und dreht ab und zu eine Runde. Wird ein Paket verschickt, steigt er
 * in einer Flammensäule auf und ist unterwegs, bis er zurückkehrt. Am Ziel landet ein Boten-Phönix (nur Optik,
 * wird nicht gespeichert).
 */
public class Phoenix extends NetworkHelperMob {
	public static final int NEST_SEARCH = 16;
	private static final int DEPART_TICKS = 30;
	private static final int MESSENGER_TICKS = 110;
	private static final EntityDataAccessor<Boolean> DATA_AWAY = SynchedEntityData.defineId(Phoenix.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Boolean> DATA_MESSENGER = SynchedEntityData.defineId(Phoenix.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Integer> DATA_DEPART = SynchedEntityData.defineId(Phoenix.class, EntityDataSerializers.INT);

	private @Nullable BlockPos nest;
	private long awayUntil;
	private int messengerAge;
	private @Nullable BlockPos messengerTarget;
	private boolean circling;

	public Phoenix(EntityType<? extends Phoenix> type, Level level) {
		super(type, level);
		moveControl = new FlyingMoveControl<>(this, 10, true);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return createMobAttributes()
				.add(Attributes.MAX_HEALTH, 40.0)
				.add(Attributes.MOVEMENT_SPEED, 0.2)
				.add(Attributes.FLYING_SPEED, 0.22)
				.add(Attributes.FOLLOW_RANGE, 24.0);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DATA_AWAY, false);
		builder.define(DATA_MESSENGER, false);
		builder.define(DATA_DEPART, 0);
	}

	@Override
	protected PathNavigation createNavigation(Level level) {
		FlyingPathNavigation navigation = new FlyingPathNavigation(this, level);
		navigation.setCanFloat(true);
		return navigation;
	}

	@Override
	public void travel(Vec3 input) {
		travelFlying(input, getSpeed());
	}

	/** Ein Bote fliegt nachts genauso. */
	@Override
	protected boolean canRest() {
		return false;
	}

	@Override
	protected String unlinkedStatus() {
		return "jtechstorage.phoenix.status.unlinked";
	}

	@Override
	protected String linkAdvancement() {
		return "phoenix";
	}

	public boolean isAway() {
		return entityData.get(DATA_AWAY);
	}

	public boolean isMessenger() {
		return entityData.get(DATA_MESSENGER);
	}

	public boolean isDeparting() {
		return entityData.get(DATA_DEPART) > 0;
	}

	public @Nullable BlockPos getNest() {
		return nest;
	}

	private boolean busy() {
		return isAway() || isMessenger() || isDeparting() || isHologram();
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(1, new PerchGoal());
		goalSelector.addGoal(2, new CircleGoal());
		goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 8.0f) {
			@Override
			public boolean canUse() {
				return !busy() && super.canUse();
			}
		});
	}

	// ------------------------------------------------------------------
	// Statische Helfer für den Horst
	// ------------------------------------------------------------------

	/** Der Phönix, der in diesem Horst wohnt (oder null). */
	public static @Nullable Phoenix findHome(ServerLevel level, BlockPos nestPos) {
		return level.getEntitiesOfClass(Phoenix.class, new net.minecraft.world.phys.AABB(nestPos).inflate(NEST_SEARCH + 8),
				phoenix -> !phoenix.isMessenger() && !phoenix.isHologram() && nestPos.equals(phoenix.nest)).stream()
				.findFirst().orElse(null);
	}

	/** Boten-Phönix, der am Ziel-Horst landet, das Paket ablegt und wieder abfliegt (nur Optik). */
	public static void spawnMessenger(ServerLevel level, BlockPos nestPos) {
		Phoenix messenger = ModContent.PHOENIX.create(level, EntitySpawnReason.TRIGGERED);

		if (messenger == null) {
			return;
		}

		double angle = level.getRandom().nextDouble() * Math.PI * 2;
		messenger.snapTo(nestPos.getX() + 0.5 + Math.cos(angle) * 10, nestPos.getY() + 14, nestPos.getZ() + 0.5 + Math.sin(angle) * 10,
				(float) Math.toDegrees(angle) + 90.0f, 0.0f);
		messenger.entityData.set(DATA_MESSENGER, true);
		messenger.messengerTarget = nestPos.immutable();
		messenger.noPhysics = true;
		messenger.setNoGravity(true);
		messenger.setSilent(false);
		level.addFreshEntity(messenger);
		messenger.playSound(SoundEvents.BLAZE_SHOOT, 0.8f, 1.5f);
	}

	/** Startet zu einer Lieferung: Flammensäule, dann unterwegs für {@code ticks} Ticks. */
	public void depart(int ticks) {
		awayUntil = level().getGameTime() + DEPART_TICKS + ticks;
		entityData.set(DATA_DEPART, DEPART_TICKS);
		getNavigation().stop();
		playSound(ModSounds.PHOENIX_DEPART, 1.0f, 1.0f);
	}

	// ------------------------------------------------------------------
	// Ablauf
	// ------------------------------------------------------------------

	@Override
	public void aiStep() {
		super.aiStep();

		if (!(level() instanceof ServerLevel level) || isHologram()) {
			return;
		}

		if (isMessenger()) {
			messengerTick(level);
			return;
		}

		int depart = entityData.get(DATA_DEPART);

		if (depart > 0) {
			// steil nach oben, Flammenspur
			setDeltaMovement(0.0, 0.35 + (DEPART_TICKS - depart) * 0.03, 0.0);
			level.sendParticles(ParticleTypes.FLAME, getX(), getY(), getZ(), 6, 0.2, 0.2, 0.2, 0.02);
			level.sendParticles(ParticleTypes.WITCH, getX(), getY(), getZ(), 2, 0.2, 0.2, 0.2, 0.02);
			entityData.set(DATA_DEPART, depart - 1);

			if (depart == 1) {
				level.sendParticles(ParticleTypes.FLAME, getX(), getY(), getZ(), 40, 0.5, 0.5, 0.5, 0.15);
				level.sendParticles(ParticleTypes.END_ROD, getX(), getY(), getZ(), 15, 0.4, 0.4, 0.4, 0.1);
				playSound(SoundEvents.BLAZE_SHOOT, 1.0f, 1.3f);
				setAway(true);
			}

			return;
		}

		if (isAway()) {
			setDeltaMovement(Vec3.ZERO);

			if (nest != null) {
				setPos(nest.getX() + 0.5, nest.getY() + 0.5, nest.getZ() + 0.5);
			}

			long left = awayUntil - level.getGameTime();

			if (left <= 0) {
				returnHome(level);
			} else if (tickCount % 20 == 0) {
				setStatus("jtechstorage.phoenix.status.away", (left + 19) / 20);
			}

			return;
		}

		if (tickCount % 40 == 11) {
			findNest(level);

			if (nest == null) {
				setStatus(terminalPos == null ? unlinkedStatus() : "jtechstorage.phoenix.status.no_nest", NEST_SEARCH);
			} else {
				setStatus("jtechstorage.phoenix.status.home");
			}
		}

		if (tickCount % 8 == 0) {
			level.sendParticles(ParticleTypes.SMALL_FLAME, getX(), getY() + 0.5, getZ(), 1, 0.25, 0.2, 0.25, 0.0);
		}

		settleOnNest();
	}

	/** Sitzt er (fast) auf dem Horst, gleitet er sanft auf seinen Platz und bleibt ruhig sitzen – kein Hin- und Herzittern. */
	private void settleOnNest() {
		if (nest == null || circling) {
			return;
		}

		Vec3 perch = new Vec3(nest.getX() + 0.5, nest.getY() + 0.6, nest.getZ() + 0.5);
		double distance = position().distanceTo(perch);

		if (distance < 1.6) {
			getNavigation().stop();
			setDeltaMovement(getDeltaMovement().scale(0.3));
			setPos(position().lerp(perch, 0.12));
		}
	}

	private void returnHome(ServerLevel level) {
		setAway(false);

		if (nest != null) {
			setPos(nest.getX() + 0.5, nest.getY() + 12.0, nest.getZ() + 0.5);
			getNavigation().moveTo(nest.getX() + 0.5, nest.getY() + 0.7, nest.getZ() + 0.5, 1.2);
		}

		level.sendParticles(ParticleTypes.FLAME, getX(), getY(), getZ(), 40, 0.6, 0.6, 0.6, 0.1);
		playSound(SoundEvents.BLAZE_SHOOT, 0.8f, 1.6f);
		setStatus("jtechstorage.phoenix.status.home");
	}

	private void setAway(boolean away) {
		entityData.set(DATA_AWAY, away);
		setInvisible(away);
		noPhysics = away;
		setNoGravity(away);
	}

	private void findNest(ServerLevel level) {
		if (nest != null && level.isLoaded(nest) && level.getBlockEntity(nest) instanceof PhoenixNestBlockEntity existing
				&& (terminalPos == null || terminalPos.equals(existing.getLink()))) {
			return;
		}

		nest = null;
		BlockPos center = blockPosition();
		double best = Double.MAX_VALUE;

		for (BlockPos pos : BlockPos.betweenClosed(center.offset(-NEST_SEARCH, -6, -NEST_SEARCH), center.offset(NEST_SEARCH, 6, NEST_SEARCH))) {
			if (level.getBlockEntity(pos) instanceof PhoenixNestBlockEntity candidate
					&& terminalPos != null && terminalPos.equals(candidate.getLink())) {
				double distance = pos.distSqr(center);

				if (distance < best) {
					best = distance;
					nest = pos.immutable();
				}
			}
		}
	}

	private void messengerTick(ServerLevel level) {
		messengerAge++;
		BlockPos target = messengerTarget;

		if (target == null || messengerAge > MESSENGER_TICKS) {
			level.sendParticles(ParticleTypes.FLAME, getX(), getY(), getZ(), 20, 0.4, 0.4, 0.4, 0.08);
			discard();
			return;
		}

		Vec3 goal = new Vec3(target.getX() + 0.5, target.getY() + 1.2, target.getZ() + 0.5);
		Vec3 pos = position();

		if (messengerAge < 45) {
			// Anflug
			Vec3 step = goal.subtract(pos).scale(0.12);
			setPos(pos.add(step));
			lookAt(goal);
		} else if (messengerAge < 70) {
			// kurz über dem Horst flattern, Paket ablegen
			setPos(goal.add(0.0, Math.sin(messengerAge * 0.4) * 0.1, 0.0));

			if (messengerAge == 50) {
				level.sendParticles(ParticleTypes.FLAME, goal.x, goal.y - 0.4, goal.z, 25, 0.4, 0.2, 0.4, 0.05);
				level.sendParticles(ParticleTypes.WITCH, goal.x, goal.y - 0.4, goal.z, 15, 0.4, 0.2, 0.4, 0.05);
				playSound(SoundEvents.AMETHYST_BLOCK_CHIME, 1.0f, 1.2f);
			}
		} else {
			// wieder abfliegen
			setPos(pos.add(0.25, 0.35, 0.15));
		}

		if (messengerAge % 2 == 0) {
			level.sendParticles(ParticleTypes.FLAME, getX(), getY() + 0.3, getZ(), 2, 0.2, 0.1, 0.2, 0.01);
		}
	}

	private void lookAt(Vec3 target) {
		Vec3 diff = target.subtract(position());
		float yaw = (float) (Math.toDegrees(Math.atan2(diff.z, diff.x))) - 90.0f;
		setYRot(yaw);
		yBodyRot = yaw;
		yHeadRot = yaw;
	}

	// ------------------------------------------------------------------
	// Interaktion & Kleinkram
	// ------------------------------------------------------------------

	@Override
	protected InteractionResult interactWith(Player player, InteractionHand hand) {
		ItemStack held = player.getItemInHand(hand);

		if (busy()) {
			return InteractionResult.PASS;
		}

		if (held.is(dev.bartels.jtechstorage.end.EndContent.AMETHYST_OBSIDIAN_UPGRADE) && !isMessenger() && level() instanceof ServerLevel serverLevel) {
			AmethystPhoenix.evolve(this, serverLevel, player);
			held.consume(1, player);
			return InteractionResult.SUCCESS;
		}

		if (held.isEmpty() && player.isSecondaryUseActive()) {
			player.getInventory().placeItemBackInInventory(createPackedItem(ModContent.PHOENIX_ITEM, player), Prediction.SERVER_ONLY);
			playSound(SoundEvents.FIRECHARGE_USE, 0.6f, 1.6f);
			discard();
			return InteractionResult.SUCCESS;
		}

		// Rechtsklick mit leerer Hand: das Post-Menü seines Horsts öffnen
		if (held.isEmpty() && hand == InteractionHand.MAIN_HAND && player instanceof net.minecraft.server.level.ServerPlayer serverPlayer
				&& nest != null && level().isLoaded(nest)
				&& level().getBlockEntity(nest) instanceof dev.bartels.jtechstorage.block.PhoenixNestBlockEntity home
				&& dev.bartels.jtechstorage.block.PhoenixNestBlock.openFor(serverPlayer, home)) {
			return InteractionResult.SUCCESS;
		}

		return super.interactWith(player, hand);
	}

	@Override
	protected InteractionResult mobInteract(Player player, InteractionHand hand) {
		if (busy()) {
			return InteractionResult.PASS;
		}

		// Upgrade: Interaktion sofort beenden, damit nicht zusätzlich die leere Zweithand reagiert
		if (level().isClientSide() && player.getItemInHand(hand).is(dev.bartels.jtechstorage.end.EndContent.AMETHYST_OBSIDIAN_UPGRADE)) {
			return InteractionResult.SUCCESS;
		}

		return super.mobInteract(player, hand);
	}

	@Override
	public boolean shouldShowName() {
		return !isAway() && !isMessenger() && super.shouldShowName();
	}

	@Override
	public boolean shouldBeSaved() {
		return !isMessenger() && super.shouldBeSaved();
	}

	@Override
	public boolean isPickable() {
		return !isAway() && !isMessenger() && super.isPickable();
	}

	@Override
	public boolean isPushable() {
		return !busy() && super.isPushable();
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		return false;
	}

	@Override
	protected void checkFallDamage(double distance, boolean onGround, net.minecraft.world.level.block.state.BlockState state, BlockPos pos) {
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);

		if (nest != null) {
			output.store("Nest", BlockPos.CODEC, nest);
		}

		output.putBoolean("Away", isAway() || isDeparting());
		output.putLong("AwayUntil", awayUntil);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		nest = input.read("Nest", BlockPos.CODEC).orElse(null);
		awayUntil = input.getLongOr("AwayUntil", 0L);

		if (input.getBooleanOr("Away", false)) {
			setAway(true);
		}
	}

	/** Sitzt im Horst (bzw. fliegt dorthin zurück). */
	private class PerchGoal extends Goal {
		PerchGoal() {
			setFlags(EnumSet.of(Goal.Flag.MOVE));
		}

		@Override
		public boolean canUse() {
			return !busy() && !circling && nest != null && distanceToSqr(Vec3.atCenterOf(nest)) > 2.5 * 2.5;
		}

		@Override
		public boolean canContinueToUse() {
			return !busy() && nest != null && !getNavigation().isDone();
		}

		@Override
		public void start() {
			if (nest != null) {
				getNavigation().moveTo(nest.getX() + 0.5, nest.getY() + 0.6, nest.getZ() + 0.5, 0.8);
			}
		}
	}

	/** Dreht ab und zu eine Runde über dem Horst. */
	private class CircleGoal extends Goal {
		private int ticks;

		CircleGoal() {
			setFlags(EnumSet.of(Goal.Flag.MOVE));
		}

		@Override
		public boolean canUse() {
			return !busy() && nest != null && random.nextInt(1800) == 0;
		}

		@Override
		public boolean canContinueToUse() {
			return !busy() && ticks > 0;
		}

		@Override
		public void start() {
			ticks = 280;
			circling = true;
		}

		@Override
		public void stop() {
			circling = false;
		}

		@Override
		public void tick() {
			if (nest == null) {
				ticks = 0;
				return;
			}

			ticks--;

			if (ticks % 10 == 0) {
				// gemächliche Runde: Wegpunkt etwas voraus auf dem Kreis, nur alle 10 Ticks neu
				double angle = ticks * 0.022 + 0.5;
				getNavigation().moveTo(nest.getX() + 0.5 + Math.cos(angle) * 6, nest.getY() + 5, nest.getZ() + 0.5 + Math.sin(angle) * 6, 0.7);
			}
		}

		@Override
		public boolean requiresUpdateEveryTick() {
			return true;
		}
	}

	/** Statuszeile beim Rechtsklick. */
	@Override
	public Component statusText() {
		return super.statusText();
	}

	@Override
	protected net.minecraft.sounds.SoundEvent helperAmbientSound() {
		return ModSounds.PHOENIX_AMBIENT;
	}
}
