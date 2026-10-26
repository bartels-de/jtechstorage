package dev.bartels.jtechstorage.entity;

import dev.bartels.jtechstorage.helpers.HelpersContent;
import java.util.EnumSet;
import java.util.Locale;

import net.minecraft.world.item.Item;
import org.jspecify.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
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
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import dev.bartels.jtechstorage.ModConfig;
import dev.bartels.jtechstorage.ModSounds;

/**
 * Cheldrogan v3 – Peter's dragon form (dragon heart on Peter Chel Parker): a floating crystal serpent dragon.
 * <ul>
 *     <li>Unmounted: guards his base like Peter with three attacks – <b>tail sweep</b> (spins once around himself,
 *     hits everything around and hurls it away), <b>crystal breath</b> (beam from the mouth; where it hits, resonance
 *     crystals grow out of the ground and trap monsters) and <b>crystal shard rain</b> (rears up, amethyst shards rain
 *     over a group). Heals at Ashley; at 0 health he turns back into Peter.</li>
 *     <li>Right-click: mount. Space = take off, in the air he flies in the look direction. Left click = tail sweep,
 *     R key = crystal breath (follows the gaze), V key = shard rain on the targeted point.</li>
 *     <li>At night he curls up on the ground. He always hovers – no fall damage, no footsteps.</li>
 *     <li>Shift + right-click (empty hand): pack up.</li>
 * </ul>
 */
public class Cheldrogan extends PeterChelParker {
	public static final int SWEEP_TICKS = 16;
	public static final int BREATH_TICKS = 40;
	public static final int RAIN_TICKS = 50;
	private static final int SWEEP_HIT = 9;            // remaining ticks in which the tail hits
	private static final int BREATH_WINDUP = 8;
	private static final int RAIN_WINDUP = 14;
	private static final double SWEEP_RADIUS = 5.5;
	private static final double BREATH_RANGE = 18.0;
	private static final double BEAM_WIDTH = 1.3;
	private static final float BREATH_DAMAGE = 4.0f;
	private static final double RAIN_RADIUS = 4.5;
	private static final float RAIN_DAMAGE = 3.0f;
	private static final int RIDER_SWEEP_COOLDOWN = 14;
	private static final int AI_SWEEP_COOLDOWN = 30;
	private static final int AI_BREATH_COOLDOWN = 100;
	private static final int AI_RAIN_COOLDOWN = 260;
	private static final DustParticleOptions BEAM_DUST = new DustParticleOptions(0xC79BFF, 1.4f);
	private static final DustParticleOptions BEAM_CORE = new DustParticleOptions(0xF4ECFF, 0.9f);
	private static final EntityDataAccessor<Integer> DATA_BREATH = SynchedEntityData.defineId(Cheldrogan.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> DATA_SWEEP = SynchedEntityData.defineId(Cheldrogan.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> DATA_RAIN = SynchedEntityData.defineId(Cheldrogan.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Boolean> DATA_FLYING = SynchedEntityData.defineId(Cheldrogan.class, EntityDataSerializers.BOOLEAN);

	private int sweepCooldown;
	private int breathCooldown;
	private int rainCooldown;
	private Vec3 breathDir = Vec3.ZERO;
	private Vec3 rainCenter = Vec3.ZERO;
	private @Nullable Player attackRider;
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
		builder.define(DATA_SWEEP, 0);
		builder.define(DATA_RAIN, 0);
		builder.define(DATA_FLYING, false);
	}

	public int getBreathTicks() {
		return entityData.get(DATA_BREATH);
	}

	public int getSweepTicks() {
		return entityData.get(DATA_SWEEP);
	}

	public int getRainTicks() {
		return entityData.get(DATA_RAIN);
	}

	/** Is he currently performing an attack? */
	public boolean isBusy() {
		return getBreathTicks() > 0 || getSweepTicks() > 0 || getRainTicks() > 0;
	}

	public boolean isDragonFlying() {
		return entityData.get(DATA_FLYING);
	}

	private void setDragonFlying(boolean flying) {
		entityData.set(DATA_FLYING, flying);
	}

	/** For animation: 0 = hovers near the ground, 1 = flight with rider. */
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
	protected Item packedItem() {
		return HelpersContent.CHELDROGAN_ITEM;
	}

	@Override
	public boolean wantsToHeal() {
		return !isVehicle() && super.wantsToHeal();
	}

	/** Takes over owner, network, color, hat and home from Peter. */
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

	/** At 0 health: the dragon heart is used up – he turns back into Peter. */
	@Override
	protected void revert(ServerLevel level) {
		ejectPassengers();
		PeterChelParker peter = HelpersContent.PETER_CHEL_PARKER.create(level, EntitySpawnReason.CONVERSION);

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
		goalSelector.addGoal(2, new DragonCombatGoal());
		goalSelector.addGoal(4, new ReturnHomeGoal());
		goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.6));
		goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 12.0f));
		goalSelector.addGoal(7, new RandomLookAroundGoal(this));
		targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 5, true, false,
				(target, level) -> target instanceof Enemy && !isVehicle() && inGuardArea(target)));
	}

	// ------------------------------------------------------------------
	// Behavior
	// ------------------------------------------------------------------

	@Override
	public void aiStep() {
		super.aiStep();
		flyAmountO = flyAmount;
		flyAmount = Mth.approach(flyAmount, isDragonFlying() ? 1.0f : 0.0f, 0.08f);

		// He hovers: without a rider he only sinks slowly to the ground
		Vec3 motion = getDeltaMovement();

		if (!onGround() && motion.y < -0.2) {
			setDeltaMovement(motion.x, -0.2, motion.z);
		}

		if (!(level() instanceof ServerLevel level)) {
			double moving = motion.horizontalDistanceSqr();

			if ((isDragonFlying() || moving > 0.002) && tickCount % 30 == 0) {
				level().playLocalSound(getX(), getY() + 1.5, getZ(), ModSounds.CHELDROGAN_FLAP, getSoundSource(), isDragonFlying() ? 1.0f : 0.6f,
						0.9f + random.nextFloat() * 0.2f, false);
			}

			return;
		}

		if (sweepCooldown > 0) {
			sweepCooldown--;
		}

		if (breathCooldown > 0) {
			breathCooldown--;
		}

		if (rainCooldown > 0) {
			rainCooldown--;
		}

		if (isVehicle()) {
			setTarget(null);
			getNavigation().stop();

			if (getControllingPassenger() instanceof ServerPlayer rider) {
				updateRiderFlight(rider);
			}
		} else if (isDragonFlying()) {
			setDragonFlying(false);        // without a rider he glides to the ground
		}

		if (isDragonFlying() && !isVehicle() && onGround()) {
			setDragonFlying(false);
		}

		tickSweep(level);
		tickBreath(level);
		tickRain(level);
	}

	@Override
	public boolean causeFallDamage(double fallDistance, float multiplier, DamageSource source) {
		return false;
	}

	/** Damage source: via the rider, otherwise himself. */
	private DamageSource attackSource() {
		return attackRider instanceof ServerPlayer rider && getControllingPassenger() == rider
				? damageSources().playerAttack(rider) : damageSources().mobAttack(this);
	}

	/** Whom his attacks may hit: no players, helpers, tamed animals or his own rider. */
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
	// Tail sweep: once around his own axis, the tail sweeps in a circle
	// ------------------------------------------------------------------

	private boolean startSweep(@Nullable Player rider) {
		if (isBusy() || sweepCooldown > 0) {
			return false;
		}

		attackRider = rider;
		entityData.set(DATA_SWEEP, SWEEP_TICKS);
		sweepCooldown = rider != null ? RIDER_SWEEP_COOLDOWN : AI_SWEEP_COOLDOWN;
		playSound(ModSounds.CHELDROGAN_CLAW, 1.8f, 0.9f + random.nextFloat() * 0.2f);
		return true;
	}

	private void tickSweep(ServerLevel level) {
		int ticks = getSweepTicks();

		if (ticks <= 0) {
			return;
		}

		entityData.set(DATA_SWEEP, ticks - 1);

		if (ticks <= SWEEP_HIT + 3 && ticks > SWEEP_HIT - 3) {
			// Tail trail in a circle
			double angle = (SWEEP_HIT + 3 - ticks) / 6.0 * Math.PI * 2.0 + yBodyRot * Mth.DEG_TO_RAD;

			for (int i = 0; i < 3; i++) {
				double r = 2.5 + i * 1.2;
				double a = angle + i * 0.15;
				level.sendParticles(ParticleTypes.SWEEP_ATTACK, getX() - Math.sin(a) * r, getY() + 1.0, getZ() + Math.cos(a) * r, 1, 0, 0, 0, 0);
				level.sendParticles(ParticleTypes.WITCH, getX() - Math.sin(a) * r, getY() + 1.2, getZ() + Math.cos(a) * r, 2, 0.3, 0.3, 0.3, 0.02);
			}
		}

		if (ticks != SWEEP_HIT) {
			return;
		}

		float damage = (float) getAttributeValue(Attributes.ATTACK_DAMAGE);
		AABB area = getBoundingBox().inflate(SWEEP_RADIUS, 2.5, SWEEP_RADIUS);

		for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, area, this::isValidVictim)) {
			Vec3 away = new Vec3(victim.getX() - getX(), 0, victim.getZ() - getZ());

			if (away.length() > SWEEP_RADIUS + victim.getBbWidth() * 0.5) {
				continue;
			}

			if (victim.hurtServer(level, attackSource(), damage)) {
				Vec3 push = away.lengthSqr() < 1.0E-4 ? Vec3.directionFromRotation(0, yBodyRot) : away.normalize();
				victim.push(push.x * 1.5, 0.5, push.z * 1.5);
				victim.needsSync = true;
				level.sendParticles(ParticleTypes.CRIT, victim.getX(), victim.getY() + victim.getBbHeight() * 0.6, victim.getZ(), 6, 0.3, 0.3, 0.3, 0.2);
			}
		}

		level.sendParticles(ParticleTypes.EXPLOSION, getX(), getY() + 0.5, getZ(), 1, 0, 0, 0, 0);
	}

	// ------------------------------------------------------------------
	// Crystal breath: beam from the mouth, resonance crystals trap monsters
	// ------------------------------------------------------------------

	/** Starts the breath in the look direction (rider) or toward the target. */
	private boolean startBreath(@Nullable Player rider, Vec3 direction) {
		if (isBusy() || breathCooldown > 0 || direction.lengthSqr() < 1.0E-4) {
			return false;
		}

		attackRider = rider;
		breathDir = direction.normalize();
		entityData.set(DATA_BREATH, BREATH_TICKS);
		breathCooldown = rider != null ? (int) Math.round(ModConfig.get().helpers.cheldroganBreathCooldownSeconds * 20.0) : AI_BREATH_COOLDOWN;
		playSound(ModSounds.CHELDROGAN_BREATH, 2.5f, 1.0f);
		return true;
	}

	/** Mouth position from the model (breath pose): 2 blocks in front of him, 1.65 blocks high. */
	private Vec3 mouthPosition() {
		return position().add(Vec3.directionFromRotation(0, yBodyRot).scale(2.0)).add(0, 1.65, 0);
	}

	private void tickBreath(ServerLevel level) {
		int ticks = getBreathTicks();

		if (ticks <= 0) {
			return;
		}

		entityData.set(DATA_BREATH, ticks - 1);

		if (attackRider != null && getControllingPassenger() == attackRider) {
			breathDir = attackRider.getLookAngle();
		} else if (getTarget() != null) {
			breathDir = getTarget().position().add(0, getTarget().getBbHeight() * 0.5, 0).subtract(mouthPosition()).normalize();
		}

		Vec3 mouth = mouthPosition();

		if (ticks > BREATH_TICKS - BREATH_WINDUP) {
			// Take a breath: light gathers in front of the mouth
			level.sendParticles(ParticleTypes.END_ROD, mouth.x, mouth.y, mouth.z, 3, 0.6, 0.6, 0.6, 0.0);
			level.sendParticles(BEAM_CORE, mouth.x, mouth.y, mouth.z, 2, 0.2, 0.2, 0.2, 0.0);
			return;
		}

		Vec3 end = mouth.add(breathDir.scale(BREATH_RANGE));
		BlockHitResult hit = level.clip(new ClipContext(mouth, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));

		if (hit.getType() != HitResult.Type.MISS) {
			end = hit.getLocation();
		}

		double length = end.distanceTo(mouth);

		for (double d = 0.4; d < length; d += 0.55) {
			Vec3 p = mouth.add(breathDir.scale(d));
			level.sendParticles(BEAM_DUST, p.x, p.y, p.z, 1, 0.06, 0.06, 0.06, 0.0);

			if (random.nextInt(3) == 0) {
				level.sendParticles(BEAM_CORE, p.x, p.y, p.z, 1, 0.02, 0.02, 0.02, 0.0);
			}
		}

		level.sendParticles(ParticleTypes.END_ROD, end.x, end.y, end.z, 3, 0.25, 0.25, 0.25, 0.05);

		if (hit.getType() == HitResult.Type.BLOCK) {
			level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.AMETHYST_BLOCK.defaultBlockState()), end.x, end.y, end.z, 4,
					0.2, 0.2, 0.2, 0.1);

			// Where the beam hits the ground, crystals grow
			if (ticks % 6 == 0) {
				growCrystal(level, hit.getBlockPos().relative(hit.getDirection()));
			}
		}

		if (ticks % 4 != 0) {
			return;
		}

		AABB area = new AABB(mouth, end).inflate(BEAM_WIDTH);

		for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, area, this::isValidVictim)) {
			Vec3 center = victim.position().add(0, victim.getBbHeight() * 0.5, 0);
			double along = Mth.clamp(center.subtract(mouth).dot(breathDir), 0.0, length);

			if (center.distanceTo(mouth.add(breathDir.scale(along))) > BEAM_WIDTH + victim.getBbWidth() * 0.5) {
				continue;
			}

			if (victim.hurtServer(level, attackSource(), BREATH_DAMAGE)) {
				encase(level, victim);
			}
		}
	}

	/** Resonance crystals grow out of the ground around the victim and hold it in place. */
	private void encase(ServerLevel level, LivingEntity victim) {
		MobEffectInstance slow = victim.getEffect(MobEffects.SLOWNESS);

		if (slow != null && slow.getAmplifier() >= 5) {
			return;      // already trapped
		}

		victim.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 6));
		BlockPos base = victim.blockPosition();
		int r = victim.getBbWidth() > 1.2f ? 2 : 1;
		int height = victim.getBbHeight() > 1.2f ? 2 : 1;
		boolean grown = false;

		for (int dx = -r; dx <= r; dx++) {
			for (int dz = -r; dz <= r; dz++) {
				boolean ring = Math.max(Math.abs(dx), Math.abs(dz)) == r && (r > 1 || Math.abs(dx) + Math.abs(dz) == 1);

				if (!ring) {
					continue;
				}

				for (int dy = 0; dy < height; dy++) {
					if (!growCrystal(level, base.offset(dx, dy, dz))) {
						break;
					}

					grown = true;
				}
			}
		}

		level.sendParticles(ParticleTypes.END_ROD, victim.getX(), victim.getY() + victim.getBbHeight() * 0.5, victim.getZ(), 10, 0.5, 0.6, 0.5, 0.05);

		if (grown) {
			level.playSound(null, victim.getX(), victim.getY(), victim.getZ(), ModSounds.CHELDROGAN_CRYSTAL, getSoundSource(), 1.2f,
					0.9f + random.nextFloat() * 0.2f);
		}
	}

	/** Lets a resonance crystal grow – only in air and on solid ground (or on another crystal). */
	private boolean growCrystal(ServerLevel level, BlockPos pos) {
		if (!level.getBlockState(pos).isAir()) {
			return false;
		}

		BlockPos below = pos.below();
		BlockState ground = level.getBlockState(below);

		if (!ground.is(HelpersContent.RESONANCE_CRYSTAL) && !ground.isFaceSturdy(level, below, Direction.UP)) {
			return false;
		}

		level.setBlockAndUpdate(pos, HelpersContent.RESONANCE_CRYSTAL.defaultBlockState());
		level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.AMETHYST_CLUSTER.defaultBlockState()),
				pos.getX() + 0.5, pos.getY() + 0.2, pos.getZ() + 0.5, 6, 0.3, 0.1, 0.3, 0.05);
		return true;
	}

	// ------------------------------------------------------------------
	// Crystal shard rain: rear up, shards rain over the target
	// ------------------------------------------------------------------

	private boolean startRain(@Nullable Player rider, Vec3 center) {
		if (isBusy() || rainCooldown > 0) {
			return false;
		}

		attackRider = rider;
		rainCenter = center;
		entityData.set(DATA_RAIN, RAIN_TICKS);
		rainCooldown = rider != null ? (int) Math.round(ModConfig.get().helpers.cheldroganRainCooldownSeconds * 20.0) : AI_RAIN_COOLDOWN;
		playSound(ModSounds.CHELDROGAN_ROAR, 2.5f, 1.1f);
		return true;
	}

	private void tickRain(ServerLevel level) {
		int ticks = getRainTicks();

		if (ticks <= 0) {
			return;
		}

		entityData.set(DATA_RAIN, ticks - 1);

		if (ticks > RAIN_TICKS - RAIN_WINDUP) {
			// Rearing up: pillar of light rises from the pearl into the sky
			Vec3 pearl = position().add(Vec3.directionFromRotation(0, yBodyRot).scale(1.9)).add(0, 1.9, 0);
			level.sendParticles(ParticleTypes.END_ROD, pearl.x, pearl.y, pearl.z, 0, 0, 1, 0, 0.8);
			level.sendParticles(ParticleTypes.WITCH, rainCenter.x, rainCenter.y + 0.2, rainCenter.z, 4, RAIN_RADIUS * 0.5, 0.1, RAIN_RADIUS * 0.5, 0.0);
			return;
		}

		if (ticks == RAIN_TICKS - RAIN_WINDUP) {
			level.playSound(null, rainCenter.x, rainCenter.y, rainCenter.z, ModSounds.CHELDROGAN_RAIN, getSoundSource(), 2.5f, 1.0f);
		}

		if (ticks <= 6) {
			return;
		}

		// two shards per tick, evenly distributed over the area
		for (int i = 0; i < 2; i++) {
			double angle = random.nextDouble() * Math.PI * 2.0;
			double r = Math.sqrt(random.nextDouble()) * RAIN_RADIUS;
			double x = rainCenter.x + Math.cos(angle) * r;
			double z = rainCenter.z + Math.sin(angle) * r;
			double y = rainCenter.y + 10.0 + random.nextDouble() * 3.0;
			CrystalShard shard = new CrystalShard(level, this).withDamage(RAIN_DAMAGE);
			shard.setPos(x, y, z);
			shard.shoot(random.nextGaussian() * 0.03, -1.0, random.nextGaussian() * 0.03, 1.1f, 0.0f);
			level.addFreshEntity(shard);
			level.sendParticles(ParticleTypes.END_ROD, x, y, z, 1, 0.1, 0.1, 0.1, 0.0);
		}
	}

	/** Center for the shard rain: where the monsters stand densest (around the target). */
	private Vec3 crowdCenter(LivingEntity target) {
		Vec3 sum = Vec3.ZERO;
		int n = 0;

		for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, target.getBoundingBox().inflate(RAIN_RADIUS),
				e -> e instanceof Enemy && e.isAlive())) {
			sum = sum.add(e.position());
			n++;
		}

		return n > 0 ? sum.scale(1.0 / n) : target.position();
	}

	private int crowdSize(LivingEntity target) {
		return level().getEntitiesOfClass(LivingEntity.class, target.getBoundingBox().inflate(RAIN_RADIUS), e -> e instanceof Enemy && e.isAlive()).size();
	}

	// ------------------------------------------------------------------
	// Control by the rider (via DragonActionPayload)
	// ------------------------------------------------------------------

	public static final int ACTION_CLAW = 0;
	public static final int ACTION_BREATH = 1;
	public static final int ACTION_RAIN = 2;

	public void riderAction(ServerPlayer player, int action) {
		if (getControllingPassenger() != player || !(level() instanceof ServerLevel)) {
			return;
		}

		if (action == ACTION_CLAW) {
			startSweep(player);
		} else if (action == ACTION_BREATH) {
			if (!startBreath(player, player.getLookAngle()) && breathCooldown > 0 && !isBusy()) {
				cooldownMessage(player, breathCooldown);
			}
		} else if (action == ACTION_RAIN) {
			Vec3 aim = player.pick(40.0, 1.0f, false).getLocation();

			if (!startRain(player, aim) && rainCooldown > 0 && !isBusy()) {
				cooldownMessage(player, rainCooldown);
			}
		}
	}

	private static void cooldownMessage(ServerPlayer player, int ticks) {
		player.sendOverlayMessage(Component.translatable("jtechstorage.cheldrogan.breath_cooldown",
				String.format(Locale.ROOT, "%.1f", ticks / 20.0)).withStyle(ChatFormatting.GRAY));
	}

	// ------------------------------------------------------------------
	// Riding & flying
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
		// Seat (from the model): on the second body segment, 1.7 blocks high and half a block behind the center
		Vec3 seat = new Vec3(0.0, 1.75, -0.5);
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

		// Movement is computed by the rider's client: takeoff jump here, the server sets the flight state
		if (!isDragonFlying() && player.isJumping() && onGround()) {
			setDeltaMovement(getDeltaMovement().x, 0.75, getDeltaMovement().z);
		}
	}

	/** Server: take off/land based on the rider's input (jump key). */
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

		// dismounted far from home: his base is now here
		if (!level().isClientSide() && (home == null || distanceToSqr(Vec3.atCenterOf(home)) > 48 * 48)) {
			home = blockPosition();
		}
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		Entity attacker = source.getEntity();

		if (attacker != null && (hasPassenger(attacker) || owner != null && owner.equals(attacker.getUUID()))) {
			return false;       // rider and owner do not hurt him
		}

		return super.hurtServer(level, source, amount);
	}

	// ------------------------------------------------------------------
	// Interaction
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
		return isNightGuard() ? ModSounds.CHELDROGAN_SLEEP : ModSounds.CHELDROGAN_AMBIENT;
	}

	@Override
	public boolean isNightGuard() {
		return super.isNightGuard() && !isDragonFlying() && !isBusy();
	}

	@Override
	protected void playStepSound(BlockPos pos, BlockState state) {
		// hovers – no footsteps
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

	/**
	 * Combat without a rider: shard rain against groups or distant targets, tail sweep up close, crystal breath at
	 * range and against targets he cannot reach. In between he hovers toward the target.
	 */
	private class DragonCombatGoal extends Goal {
		DragonCombatGoal() {
			setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
		}

		@Override
		public boolean canUse() {
			LivingEntity target = getTarget();
			return target != null && target.isAlive() && !isVehicle();
		}

		@Override
		public boolean canContinueToUse() {
			return !isVehicle() && (canUse() || isBusy());
		}

		@Override
		public boolean requiresUpdateEveryTick() {
			return true;
		}

		@Override
		public void stop() {
			getNavigation().stop();
		}

		@Override
		public void tick() {
			LivingEntity target = getTarget();

			if (target == null) {
				return;
			}

			getLookControl().setLookAt(target, 30.0f, 30.0f);

			if (isBusy()) {
				getNavigation().stop();

				if (getSweepTicks() == 0) {
					Vec3 to = target.position().subtract(position());
					float yaw = (float) (Mth.atan2(to.z, to.x) * Mth.RAD_TO_DEG) - 90.0f;
					setYRot(Mth.approachDegrees(getYRot(), yaw, 8.0f));
					yBodyRot = getYRot();
				}

				return;
			}

			double distance = distanceTo(target);
			boolean outOfReach = target.getY() - getY() > 3.0 || target.isNoGravity() && !target.onGround();
			boolean sight = hasLineOfSight(target);

			if (rainCooldown == 0 && sight && distance < 24.0 && !outOfReach && (crowdSize(target) >= 3 || distance > 10.0)) {
				startRain(null, crowdCenter(target));
			} else if (sweepCooldown == 0 && !outOfReach && distance < SWEEP_RADIUS - 0.5) {
				startSweep(null);
			} else if (breathCooldown == 0 && sight && distance < BREATH_RANGE - 2.0 && (distance > 5.0 || outOfReach)) {
				startBreath(null, target.position().add(0, target.getBbHeight() * 0.5, 0).subtract(mouthPosition()));
			} else if (!outOfReach && distance > 3.5) {
				if (getNavigation().isDone() || tickCount % 10 == 0) {
					getNavigation().moveTo(target, 1.2);
				}
			} else {
				getNavigation().stop();
			}
		}
	}
}
