package dev.bartels.jtechstorage.depths.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.SmoothSwimmingLookControl;
import net.minecraft.world.entity.ai.control.SmoothSwimmingMoveControl;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomSwimmingGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.navigation.WaterBoundPathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;

/**
 * Seegeist (Kristalltiefe, v0.31): leuchtendes Quallenwesen der Kristallseen. Greift nur Ziele im Wasser an, packt sie
 * mit seinen Fangarmen und zieht sie in die Tiefe. Außerhalb des Wassers trocknet er aus.
 */
public class LakeWraith extends Monster {
	private static final EntityDataAccessor<Integer> DATA_GRAB = SynchedEntityData.defineId(LakeWraith.class, EntityDataSerializers.INT);
	public static final int GRAB_TICKS = 14;
	private int dryTicks;

	public LakeWraith(EntityType<? extends LakeWraith> type, Level level) {
		super(type, level);
		this.moveControl = new SmoothSwimmingMoveControl(this, 85, 10, 0.06f, 0.1f, true);
		this.lookControl = new SmoothSwimmingLookControl(this, 10);
		setPathfindingMalus(PathType.WATER, 0.0f);
		this.xpReward = 8;
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
				.add(Attributes.MAX_HEALTH, 24.0)
				.add(Attributes.ATTACK_DAMAGE, 4.0)
				.add(Attributes.MOVEMENT_SPEED, 1.0)
				.add(Attributes.FOLLOW_RANGE, 20.0);
	}

	public static boolean checkSpawn(EntityType<LakeWraith> type, ServerLevelAccessor level, EntitySpawnReason reason, BlockPos pos,
			RandomSource random) {
		return level.getDifficulty() != Difficulty.PEACEFUL && level.getFluidState(pos).is(FluidTags.WATER)
				&& level.getFluidState(pos.above()).is(FluidTags.WATER);
	}

	@Override
	protected PathNavigation createNavigation(Level level) {
		return new WaterBoundPathNavigation(this, level);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DATA_GRAB, 0);
	}

	public int getGrabTicks() {
		return entityData.get(DATA_GRAB);
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.3, true) {
			@Override
			public boolean canUse() {
				LivingEntity target = mob.getTarget();
				return target != null && target.isInWater() && super.canUse();
			}
		});
		goalSelector.addGoal(5, new RandomSwimmingGoal(this, 1.0, 30));
		targetSelector.addGoal(1, new HurtByTargetGoal(this));
		targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false,
				(target, level) -> target.isInWater()));
	}

	@Override
	public boolean canBreatheUnderwater() {
		return true;
	}

	@Override
	public boolean isPushedByFluid() {
		return false;
	}

	@Override
	public void travel(Vec3 input) {
		if (isInWater()) {
			moveRelative(getSpeed() * 0.08f, input);
			move(MoverType.SELF, getDeltaMovement());
			setDeltaMovement(getDeltaMovement().scale(0.9));
		} else {
			super.travel(input);
		}
	}

	@Override
	public void aiStep() {
		super.aiStep();
		int grab = getGrabTicks();

		if (level().isClientSide()) {
			if (random.nextInt(3) == 0) {
				level().addParticle(isInWater() ? ParticleTypes.GLOW : ParticleTypes.DRIPPING_WATER, getRandomX(0.6), getRandomY(),
						getRandomZ(0.6), 0, 0.01, 0);
			}

			return;
		}

		if (grab > 0) {
			entityData.set(DATA_GRAB, grab - 1);
		}

		// an Land trocknet er aus
		if (isInWater()) {
			dryTicks = 0;
		} else if (++dryTicks > 100 && tickCount % 20 == 0 && level() instanceof ServerLevel server) {
			hurtServer(server, damageSources().dryOut(), 2.0f);
		}
	}

	@Override
	public boolean doHurtTarget(ServerLevel level, Entity target) {
		boolean hit = super.doHurtTarget(level, target);
		entityData.set(DATA_GRAB, GRAB_TICKS);
		playSound(SoundEvents.GLOW_SQUID_SQUIRT, 1.0f, 0.7f);

		if (hit && target instanceof LivingEntity living) {
			// in die Tiefe ziehen
			Vec3 pull = position().subtract(living.position()).normalize().scale(0.25);
			living.setDeltaMovement(living.getDeltaMovement().add(pull.x, -0.45, pull.z));
			living.needsSync = true;
			living.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 1));
		}

		return hit;
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return isInWater() ? SoundEvents.GLOW_SQUID_AMBIENT : SoundEvents.GLOW_SQUID_SQUIRT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.GLOW_SQUID_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.GLOW_SQUID_DEATH;
	}
}
