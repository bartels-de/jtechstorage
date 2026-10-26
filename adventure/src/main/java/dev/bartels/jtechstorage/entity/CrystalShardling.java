package dev.bartels.jtechstorage.entity;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

import dev.bartels.jtechstorage.ModSounds;

/**
 * Crystal Shardling: small, fast crawler of corrupted amethyst, comes in swarms with the Resonance Storm.
 * Leaps at its targets (players, Peter, iron golems) and without a target runs toward the storm's terminal.
 * Shatters into shards on death. Storm shardlings are not saved.
 */
public class CrystalShardling extends Monster {
	private static final EntityDataAccessor<Integer> DATA_POUNCE = SynchedEntityData.defineId(CrystalShardling.class, EntityDataSerializers.INT);
	public static final int POUNCE_TICKS = 10;
	public static final int COLOR = 0xE0307A;

	/** Terminal of the storm (target when there is nothing else to do); null = spawned normally. */
	private @Nullable BlockPos stormCenter;

	public CrystalShardling(EntityType<? extends CrystalShardling> type, Level level) {
		super(type, level);
		this.xpReward = 5;
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
				.add(Attributes.MAX_HEALTH, 14.0)
				.add(Attributes.ATTACK_DAMAGE, 4.0)
				.add(Attributes.MOVEMENT_SPEED, 0.33)
				.add(Attributes.ARMOR, 2.0)
				.add(Attributes.FOLLOW_RANGE, 40.0);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DATA_POUNCE, 0);
	}

	public int getPounceTicks() {
		return entityData.get(DATA_POUNCE);
	}

	public void setStormCenter(@Nullable BlockPos center) {
		this.stormCenter = center;
		if (center != null) {
			setPersistenceRequired();
		}
	}

	public @Nullable BlockPos getStormCenter() {
		return stormCenter;
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(2, new LeapAtTargetGoal(this, 0.4f));
		goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.2, false));
		goalSelector.addGoal(4, new MarchOnTerminalGoal());
		goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8));
		goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 10.0f));
		goalSelector.addGoal(8, new RandomLookAroundGoal(this));
		targetSelector.addGoal(1, new HurtByTargetGoal(this));
		targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
		targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10, true, false,
				(target, level) -> target instanceof StormTarget hero && !hero.isStormTargetHologram() || target instanceof IronGolem));
	}

	@Override
	public void aiStep() {
		super.aiStep();
		int pounce = getPounceTicks();

		if (pounce > 0 && !level().isClientSide()) {
			entityData.set(DATA_POUNCE, pounce - 1);
		}

		if (level().isClientSide() && random.nextInt(6) == 0) {
			level().addParticle(new DustParticleOptions(COLOR, 0.7f), getRandomX(0.5), getY() + random.nextDouble() * 0.6,
					getRandomZ(0.5), 0.0, 0.02, 0.0);
		}
	}

	@Override
	public boolean doHurtTarget(ServerLevel level, Entity target) {
		boolean hit = super.doHurtTarget(level, target);
		entityData.set(DATA_POUNCE, POUNCE_TICKS);
		playSound(ModSounds.SHARDLING_ATTACK, 0.8f, 0.9f + random.nextFloat() * 0.25f);
		return hit;
	}

	@Override
	public void die(DamageSource source) {
		super.die(source);

		if (level() instanceof ServerLevel level) {
			shatter(level, this);
		}
	}

	/** Shatters into crystal shards (also when the storm ends). */
	public static void shatter(ServerLevel level, Entity entity) {
		level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.AMETHYST_CLUSTER.defaultBlockState()),
				entity.getX(), entity.getY() + 0.3, entity.getZ(), 24, 0.3, 0.3, 0.3, 0.1);
		level.sendParticles(new DustParticleOptions(COLOR, 1.0f), entity.getX(), entity.getY() + 0.3, entity.getZ(), 10, 0.3, 0.3, 0.3, 0.0);
	}

	@Override
	public boolean shouldBeSaved() {
		return stormCenter == null && super.shouldBeSaved();
	}

	@Override
	public boolean removeWhenFarAway(double distance) {
		return stormCenter == null && super.removeWhenFarAway(distance);
	}

	// ------------------------------------------------------------------
	// Sounds
	// ------------------------------------------------------------------

	@Override
	protected @Nullable SoundEvent getAmbientSound() {
		return ModSounds.SHARDLING_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return ModSounds.SHARDLING_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return ModSounds.SHARDLING_DEATH;
	}

	@Override
	protected void playStepSound(BlockPos pos, BlockState state) {
		playSound(ModSounds.SHARDLING_STEP, 0.35f, 0.9f + random.nextFloat() * 0.3f);
	}

	@Override
	public int getAmbientSoundInterval() {
		return 140;
	}

	/** Without a target: march toward the storm's terminal. */
	private class MarchOnTerminalGoal extends Goal {
		MarchOnTerminalGoal() {
			setFlags(EnumSet.of(Flag.MOVE));
		}

		@Override
		public boolean canUse() {
			return stormCenter != null && getTarget() == null && distanceToSqr(Vec3.atCenterOf(stormCenter)) > 6 * 6;
		}

		@Override
		public boolean canContinueToUse() {
			return canUse() && !getNavigation().isDone();
		}

		@Override
		public void start() {
			getNavigation().moveTo(stormCenter.getX() + 0.5, stormCenter.getY(), stormCenter.getZ() + 0.5, 1.0);
		}
	}
}
