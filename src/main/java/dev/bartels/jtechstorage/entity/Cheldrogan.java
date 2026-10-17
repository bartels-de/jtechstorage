package dev.bartels.jtechstorage.entity;

import java.util.EnumSet;

import org.jspecify.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import dev.bartels.jtechstorage.ModConfig;
import dev.bartels.jtechstorage.ModContent;
import dev.bartels.jtechstorage.ModSounds;

/**
 * Cheldrogan – Peters Drachenform (Drachenherz an Peter Chel Parker). Ein aufrecht stehender Amethyst-Drache.
 * <ul>
 *     <li>Unberitten: bewacht seine Basis wie Peter (Klauenhieb mit Schockwelle, Kristall-Odem gegen Ziele, die er nicht
 *     erreicht), heilt sich, indem er Ashley anstupst. Bei 0 Leben wird er wieder zu Peter (das Drachenherz ist verbraucht).</li>
 *     <li>Rechtsklick: aufsitzen. Am Boden läuft er, Leertaste = abheben, in der Luft fliegt er in Blickrichtung,
 *     Leertaste = steigen; landet er, läuft er wieder. Linksklick = Klauenhieb, Taste R = Kristall-Odem.</li>
 *     <li>Shift + Rechtsklick (leere Hand): einpacken.</li>
 * </ul>
 */
public class Cheldrogan extends PeterChelParker {
	public static final int BREATH_TICKS = 30;
	private static final int CLAW_COOLDOWN = 12;
	private static final int AI_BREATH_COOLDOWN = 90;
	private static final double BREATH_RANGE = 14.0;
	private static final double BREATH_COS = Math.cos(Math.toRadians(22.0));
	private static final float BREATH_DAMAGE = 3.5f;
	private static final DustParticleOptions BREATH_DUST = new DustParticleOptions(0xB07CF0, 1.6f);
	private static final EntityDataAccessor<Integer> DATA_BREATH = SynchedEntityData.defineId(Cheldrogan.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Boolean> DATA_FLYING = SynchedEntityData.defineId(Cheldrogan.class, EntityDataSerializers.BOOLEAN);

	private int clawCooldown;
	private int breathCooldown;
	private Vec3 breathDir = Vec3.ZERO;
	private @Nullable Player breathRider;
	private float flyAmount;
	private float flyAmountO;

	public Cheldrogan(EntityType<? extends Cheldrogan> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		ModConfig.Helpers config = ModConfig.get().helpers;
		return createMobAttributes()
				.add(Attributes.MAX_HEALTH, config.cheldroganHealth)
				.add(Attributes.MOVEMENT_SPEED, 0.3)
				.add(Attributes.FLYING_SPEED, 0.07)
				.add(Attributes.ATTACK_DAMAGE, config.cheldroganDamage)
				.add(Attributes.ATTACK_KNOCKBACK, 2.0)
				.add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
				.add(Attributes.ARMOR, 12.0)
				.add(Attributes.ARMOR_TOUGHNESS, 4.0)
				.add(Attributes.STEP_HEIGHT, 1.5)
				.add(Attributes.FOLLOW_RANGE, 40.0);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DATA_BREATH, 0);
		builder.define(DATA_FLYING, false);
	}

	public int getBreathTicks() {
		return entityData.get(DATA_BREATH);
	}

	public boolean isDragonFlying() {
		return entityData.get(DATA_FLYING);
	}

	private void setDragonFlying(boolean flying) {
		entityData.set(DATA_FLYING, flying);
	}

	/** Für die Animation: 0 = Flügel angelegt, 1 = Flug. */
	public float getFlyAmount(float partialTick) {
		return Mth.lerp(partialTick, flyAmountO, flyAmount);
	}

	@Override
	protected String unlinkedStatus() {
		return "jtechstorage.peter.status.guarding";
	}

	@Override
	protected String linkAdvancement() {
		return "cheldrogan";
	}

	@Override
	protected String defeatedKey() {
		return "jtechstorage.cheldrogan.defeated";
	}

	@Override
	protected net.minecraft.world.item.Item packedItem() {
		return ModContent.CHELDROGAN_ITEM;
	}

	@Override
	public boolean wantsToHeal() {
		return !isVehicle() && super.wantsToHeal();
	}

	/** Übernimmt Besitzer, Netzwerk, Farbe, Hut und Zuhause von Peter. */
	public void inheritFrom(PeterChelParker peter) {
		owner = peter.owner;
		terminalPos = peter.terminalPos;
		home = peter.home != null ? peter.home : peter.blockPosition();
		setAccentColor(peter.getAccentColor());
		ItemStack hat = peter.getItemBySlot(EquipmentSlot.HEAD);

		if (!hat.isEmpty()) {
			setItemSlot(EquipmentSlot.HEAD, hat.copy());
		}

		setStatus(unlinkedStatus(), GUARD_RADIUS);
	}

	/** Bei 0 Leben: das Drachenherz ist verbraucht – er wird wieder zu Peter. */
	@Override
	protected void revert(ServerLevel level) {
		ejectPassengers();
		PeterChelParker peter = ModContent.PETER_CHEL_PARKER.create(level, EntitySpawnReason.CONVERSION);

		if (peter != null) {
			peter.snapTo(getX(), getY(), getZ(), getYRot(), 0.0f);
			peter.owner = owner;
			peter.terminalPos = terminalPos;
			peter.home = home;
			peter.setAccentColor(getAccentColor());
			ItemStack hat = getItemBySlot(EquipmentSlot.HEAD);

			if (!hat.isEmpty()) {
				peter.setItemSlot(EquipmentSlot.HEAD, hat.copy());
			}

			level.addFreshEntity(peter);
		}
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(1, new HealAtAshleyGoal());
		goalSelector.addGoal(2, new BreathGoal());
		goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.2, true));
		goalSelector.addGoal(4, new ReturnHomeGoal());
		goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.6));
		goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 12.0f));
		goalSelector.addGoal(7, new RandomLookAroundGoal(this));
		targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 5, true, false,
				(target, level) -> target instanceof Enemy && !isVehicle() && inGuardArea(target)));
	}

	// ------------------------------------------------------------------
	// Ablauf
	// ------------------------------------------------------------------

	@Override
	public void aiStep() {
		super.aiStep();
		flyAmountO = flyAmount;
		flyAmount = Mth.approach(flyAmount, isDragonFlying() ? 1.0f : 0.0f, 0.12f);

		if (!(level() instanceof ServerLevel level)) {
			if (isDragonFlying() && tickCount % 20 == 0) {
				level().playLocalSound(getX(), getY() + 2.5, getZ(), ModSounds.CHELDROGAN_FLAP, getSoundSource(), 1.2f,
						0.9f + random.nextFloat() * 0.2f, false);
			}

			return;
		}

		if (clawCooldown > 0) {
			clawCooldown--;
		}

		if (breathCooldown > 0) {
			breathCooldown--;
		}

		if (isVehicle()) {
			setTarget(null);
			getNavigation().stop();

			if (getControllingPassenger() instanceof ServerPlayer rider) {
				updateRiderFlight(rider);
			}
		} else if (isDragonFlying()) {
			setDragonFlying(false);        // ohne Reiter gleitet er zu Boden
		}

		if (isDragonFlying() && !isVehicle() && onGround()) {
			setDragonFlying(false);
		}

		tickBreath(level);
	}

	// ------------------------------------------------------------------
	// Kristall-Odem
	// ------------------------------------------------------------------

	/** Startet den Odem in Blickrichtung (Reiter) bzw. zum Ziel. */
	private boolean startBreath(@Nullable Player rider, Vec3 direction) {
		if (getBreathTicks() > 0 || breathCooldown > 0 || direction.lengthSqr() < 1.0E-4) {
			return false;
		}

		breathRider = rider;
		breathDir = direction.normalize();
		entityData.set(DATA_BREATH, BREATH_TICKS);
		breathCooldown = rider != null ? (int) Math.round(ModConfig.get().helpers.cheldroganBreathCooldownSeconds * 20.0) : AI_BREATH_COOLDOWN;
		playSound(ModSounds.CHELDROGAN_BREATH, 2.5f, 1.0f);
		return true;
	}

	/** Maul-Position (aus dem Modell: stehend ca. 3,5 Blöcke hoch, im Flug ca. 1,8 hoch und 1,8 vor ihm). */
	private Vec3 mouthPosition() {
		Vec3 forward = Vec3.directionFromRotation(0, yBodyRot);
		return isDragonFlying() ? position().add(forward.scale(1.9)).add(0, 1.8, 0) : position().add(forward.scale(0.4)).add(0, 3.5, 0);
	}

	private void tickBreath(ServerLevel level) {
		int ticks = getBreathTicks();

		if (ticks <= 0) {
			return;
		}

		entityData.set(DATA_BREATH, ticks - 1);

		if (breathRider != null && getControllingPassenger() == breathRider) {
			breathDir = breathRider.getLookAngle();
		} else if (getTarget() != null) {
			breathDir = getTarget().getEyePosition().subtract(mouthPosition()).normalize();
		}

		if (ticks > BREATH_TICKS - 6) {
			return;       // kurz Luft holen (Maul öffnet sich)
		}

		Vec3 mouth = mouthPosition();

		for (int i = 0; i < 6; i++) {
			double dist = 1.0 + random.nextDouble() * BREATH_RANGE;
			Vec3 spread = new Vec3(random.nextGaussian(), random.nextGaussian(), random.nextGaussian()).scale(dist * 0.12);
			Vec3 p = mouth.add(breathDir.scale(dist)).add(spread);
			level.sendParticles(BREATH_DUST, p.x, p.y, p.z, 1, 0, 0, 0, 0);
			level.sendParticles(random.nextBoolean() ? ParticleTypes.END_ROD : ParticleTypes.WITCH, p.x, p.y, p.z, 1,
					breathDir.x * 0.3, breathDir.y * 0.3, breathDir.z * 0.3, 0.05);
		}

		if (ticks % 3 != 0) {
			return;
		}

		AABB area = new AABB(mouth, mouth.add(breathDir.scale(BREATH_RANGE))).inflate(4.0);

		for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, area, this::isValidVictim)) {
			Vec3 to = victim.position().add(0, victim.getBbHeight() * 0.5, 0).subtract(mouth);
			double distance = to.length();

			if (distance > BREATH_RANGE + 1 || distance < 0.1 || to.normalize().dot(breathDir) < BREATH_COS) {
				continue;
			}

			DamageSource source = breathRider instanceof ServerPlayer rider
					? damageSources().indirectMagic(this, rider) : damageSources().indirectMagic(this, this);

			if (victim.hurtServer(level, source, BREATH_DAMAGE)) {
				victim.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.SLOWNESS, 40, 1));
				level.sendParticles(ParticleTypes.END_ROD, victim.getX(), victim.getY() + victim.getBbHeight() * 0.5, victim.getZ(),
						3, 0.2, 0.3, 0.2, 0.02);
			}
		}
	}

	/** Wen Klaue und Odem treffen dürfen: keine Spieler, Helfer, gezähmten Tiere oder der eigene Reiter. */
	private boolean isValidVictim(LivingEntity entity) {
		if (entity == this || !entity.isAlive() || entity.hasPassenger(this) || isPassengerOfSameVehicle(entity)) {
			return false;
		}

		if (entity instanceof Player || entity instanceof NetworkHelperMob || entity instanceof JbartelsHologram) {
			return false;
		}

		return !(entity instanceof TamableAnimal pet && pet.isTame());
	}

	// ------------------------------------------------------------------
	// Steuerung durch den Reiter (über DragonActionPayload)
	// ------------------------------------------------------------------

	public static final int ACTION_CLAW = 0;
	public static final int ACTION_BREATH = 1;

	public void riderAction(ServerPlayer player, int action) {
		if (getControllingPassenger() != player || !(level() instanceof ServerLevel level)) {
			return;
		}

		if (action == ACTION_CLAW) {
			riderClaw(level, player);
		} else if (action == ACTION_BREATH) {
			if (!startBreath(player, player.getLookAngle())) {
				if (breathCooldown > 0) {
					player.sendOverlayMessage(Component.translatable("jtechstorage.cheldrogan.breath_cooldown",
							String.format(java.util.Locale.ROOT, "%.1f", breathCooldown / 20.0)).withStyle(ChatFormatting.GRAY));
				}
			}
		}
	}

	private void riderClaw(ServerLevel level, ServerPlayer player) {
		if (clawCooldown > 0) {
			return;
		}

		clawCooldown = CLAW_COOLDOWN;
		triggerAttackAnimation();
		playSound(ModSounds.CHELDROGAN_CLAW, 1.6f, 0.9f + random.nextFloat() * 0.2f);
		Vec3 look = player.getLookAngle();
		Vec3 center = position().add(0, isDragonFlying() ? 1.5 : 2.0, 0).add(look.scale(3.2));
		level.sendParticles(ParticleTypes.SWEEP_ATTACK, center.x, center.y, center.z, 3, 1.0, 0.4, 1.0, 0.0);
		level.sendParticles(ParticleTypes.WITCH, center.x, center.y, center.z, 15, 1.2, 0.6, 1.2, 0.05);
		float damage = (float) getAttributeValue(Attributes.ATTACK_DAMAGE);

		for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, new AABB(center, center).inflate(2.6, 2.2, 2.6), this::isValidVictim)) {
			if (victim.hurtServer(level, damageSources().playerAttack(player), damage)) {
				Vec3 push = victim.position().subtract(position()).normalize().scale(1.1);
				victim.push(push.x, 0.4, push.z);
			}
		}
	}

	// ------------------------------------------------------------------
	// Reiten & Fliegen
	// ------------------------------------------------------------------

	@Override
	public @Nullable LivingEntity getControllingPassenger() {
		return getFirstPassenger() instanceof Player player ? player : null;
	}

	@Override
	protected boolean canAddPassenger(Entity passenger) {
		return getPassengers().isEmpty();
	}

	@Override
	public boolean isFlyingVehicle() {
		return isDragonFlying();
	}

	@Override
	protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float scale) {
		// Sattel (aus dem Modell): stehend 2,6 Blöcke hoch, leicht hinten; im Flug 2,2 hoch und 0,7 vor der Mitte
		Vec3 seat = isDragonFlying() ? new Vec3(0.0, 2.2, 0.7) : new Vec3(0.0, 2.55, -0.35);
		return seat.yRot(-getYRot() * Mth.DEG_TO_RAD);
	}

	@Override
	protected Vec3 getRiddenInput(Player player, Vec3 selfInput) {
		float forward = player.zza;
		float strafe = player.xxa * 0.5f;

		if (!isDragonFlying()) {
			return new Vec3(strafe, 0, forward > 0 ? forward : forward * 0.4f);
		}

		float pitch = player.getXRot() * Mth.DEG_TO_RAD;
		double up = forward != 0 ? -Mth.sin(pitch) * forward : 0;

		if (player.isJumping()) {
			up += 0.7;
		}

		double horizontal = forward != 0 ? Mth.cos(pitch) * forward : 0;
		return new Vec3(strafe, up, horizontal);
	}

	@Override
	protected float getRiddenSpeed(Player player) {
		return (float) (isDragonFlying() ? getAttributeValue(Attributes.FLYING_SPEED) * speedFactor() : getAttributeValue(Attributes.MOVEMENT_SPEED));
	}

	@Override
	protected void tickRidden(Player player, Vec3 input) {
		super.tickRidden(player, input);
		setRot(player.getYRot(), player.getXRot() * 0.5f);
		yRotO = getYRot();
		yBodyRot = getYRot();
		yHeadRot = getYRot();

		// Die Bewegung rechnet der Client des Reiters: Absprung zum Abheben hier, den Flug-Zustand setzt der Server
		if (!isDragonFlying() && player.isJumping() && onGround()) {
			setDeltaMovement(getDeltaMovement().x, 0.75, getDeltaMovement().z);
		}
	}

	/** Server: Abheben/Landen anhand der Eingaben des Reiters (Sprungtaste). */
	private void updateRiderFlight(ServerPlayer rider) {
		boolean jump = rider.getLastClientInput().jump();

		if (!isDragonFlying() && jump) {
			setDragonFlying(true);
			playSound(ModSounds.CHELDROGAN_ROAR, 2.0f, 1.0f);
		} else if (isDragonFlying() && onGround() && !jump) {
			setDragonFlying(false);
		}
	}

	@Override
	public void travel(Vec3 input) {
		if (isDragonFlying() && getControllingPassenger() != null) {
			travelFlying(input, getSpeed());
		} else {
			super.travel(input);
		}
	}

	@Override
	protected void removePassenger(Entity passenger) {
		super.removePassenger(passenger);

		// weit weg von zu Hause abgestiegen: hier ist jetzt seine Basis
		if (!level().isClientSide() && (home == null || distanceToSqr(Vec3.atCenterOf(home)) > 48 * 48)) {
			home = blockPosition();
		}
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		Entity attacker = source.getEntity();

		if (attacker != null && (hasPassenger(attacker) || owner != null && owner.equals(attacker.getUUID()))) {
			return false;       // Reiter und Besitzer verletzen ihn nicht
		}

		return super.hurtServer(level, source, amount);
	}

	// ------------------------------------------------------------------
	// Interaktion
	// ------------------------------------------------------------------

	@Override
	protected InteractionResult interactWith(Player player, InteractionHand hand) {
		ItemStack held = player.getItemInHand(hand);

		if (hand == InteractionHand.OFF_HAND && held.isEmpty()) {
			return InteractionResult.PASS;
		}

		if (held.isEmpty() && !player.isSecondaryUseActive()) {
			if (isVehicle()) {
				return InteractionResult.PASS;
			}

			player.startRiding(this);
			player.sendOverlayMessage(Component.translatable("jtechstorage.cheldrogan.controls").withStyle(ChatFormatting.LIGHT_PURPLE));
			return InteractionResult.SUCCESS;
		}

		return super.interactWith(player, hand);
	}

	// ------------------------------------------------------------------
	// Sounds
	// ------------------------------------------------------------------

	@Override
	protected SoundEvent helperAmbientSound() {
		return ModSounds.CHELDROGAN_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return ModSounds.CHELDROGAN_HURT;
	}

	@Override
	protected float getSoundVolume() {
		return 1.4f;
	}

	@Override
	public int getAmbientSoundInterval() {
		return 400;
	}

	/** Kristall-Odem gegen Ziele, die er nicht erreicht oder die weiter weg sind. */
	private class BreathGoal extends Goal {
		BreathGoal() {
			setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
		}

		private boolean wantsBreath(@Nullable LivingEntity target) {
			if (target == null || !target.isAlive() || isVehicle() || breathCooldown > 0) {
				return false;
			}

			double distance = distanceTo(target);
			boolean outOfReach = target.getY() - getY() > 3.5 || target.isNoGravity() && !target.onGround();
			return distance < BREATH_RANGE && (outOfReach || distance > 6.0) && hasLineOfSight(target);
		}

		@Override
		public boolean canUse() {
			return wantsBreath(getTarget()) || getBreathTicks() > 0 && getTarget() != null;
		}

		@Override
		public boolean canContinueToUse() {
			return getBreathTicks() > 0 && getTarget() != null && !isVehicle();
		}

		@Override
		public boolean requiresUpdateEveryTick() {
			return true;
		}

		@Override
		public void start() {
			getNavigation().stop();
			LivingEntity target = getTarget();

			if (target != null) {
				startBreath(null, target.getEyePosition().subtract(mouthPosition()));
			}
		}

		@Override
		public void tick() {
			LivingEntity target = getTarget();

			if (target != null) {
				getLookControl().setLookAt(target, 30.0f, 30.0f);
				Vec3 to = target.position().subtract(position());
				float yaw = (float) (Mth.atan2(to.z, to.x) * Mth.RAD_TO_DEG) - 90.0f;
				setYRot(Mth.approachDegrees(getYRot(), yaw, 8.0f));
				yBodyRot = getYRot();
			}
		}
	}
}
