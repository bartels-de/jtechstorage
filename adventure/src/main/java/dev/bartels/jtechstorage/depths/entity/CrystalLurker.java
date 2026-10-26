package dev.bartels.jtechstorage.depths.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

import dev.bartels.jtechstorage.depths.DepthsContent;

/**
 * Crystal Lurker (Crystal Depths, v0.31): a golem of crystal rock that waits crouched like a boulder.
 * When a player comes within 6 blocks (or it is hit), it stands up and strikes with both fists.
 * Without a target it crouches down again after a while.
 */
public class CrystalLurker extends Monster {
	private static final EntityDataAccessor<Boolean> DATA_DORMANT = SynchedEntityData.defineId(CrystalLurker.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Integer> DATA_SLAM = SynchedEntityData.defineId(CrystalLurker.class, EntityDataSerializers.INT);
	public static final int SLAM_TICKS = 12;
	private static final double WAKE_RANGE = 6.0;

	private int idleTicks;
	/** Client only: smooth transition between boulder (1) and standing (0). */
	public float dormantAmount = 1.0f;
	public float prevDormantAmount = 1.0f;

	public CrystalLurker(EntityType<? extends CrystalLurker> type, Level level) {
		super(type, level);
		this.xpReward = 12;
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
				.add(Attributes.MAX_HEALTH, 40.0)
				.add(Attributes.ATTACK_DAMAGE, 8.0)
				.add(Attributes.MOVEMENT_SPEED, 0.24)
				.add(Attributes.ARMOR, 8.0)
				.add(Attributes.KNOCKBACK_RESISTANCE, 0.8)
				.add(Attributes.FOLLOW_RANGE, 24.0);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DATA_DORMANT, true);
		builder.define(DATA_SLAM, 0);
	}

	public boolean isDormant() {
		return entityData.get(DATA_DORMANT);
	}

	public int getSlamTicks() {
		return entityData.get(DATA_SLAM);
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.1, false) {
			@Override
			public boolean canUse() {
				return !isDormant() && super.canUse();
			}

			@Override
			public boolean canContinueToUse() {
				return !isDormant() && super.canContinueToUse();
			}
		});
		goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.6) {
			@Override
			public boolean canUse() {
				return !isDormant() && super.canUse();
			}
		});
		goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 10.0f) {
			@Override
			public boolean canUse() {
				return !isDormant() && super.canUse();
			}
		});
		targetSelector.addGoal(1, new HurtByTargetGoal(this));
		targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
	}

	@Override
	public void tick() {
		super.tick();

		if (level().isClientSide()) {
			prevDormantAmount = dormantAmount;
			dormantAmount += ((isDormant() ? 1.0f : 0.0f) - dormantAmount) * 0.15f;
		}
	}

	@Override
	public void aiStep() {
		super.aiStep();

		if (level().isClientSide()) {
			if (!isDormant() && random.nextInt(8) == 0) {
				level().addParticle(ParticleTypes.END_ROD, getRandomX(0.6), getY() + 1.0 + random.nextDouble(), getRandomZ(0.6), 0, 0.01, 0);
			}

			return;
		}

		int slam = getSlamTicks();

		if (slam > 0) {
			entityData.set(DATA_SLAM, slam - 1);
		}

		if (isDormant()) {
			getNavigation().stop();
			setDeltaMovement(0, getDeltaMovement().y, 0);
			LivingEntity target = getTarget();

			if (target != null && distanceTo(target) <= WAKE_RANGE) {
				wake();
			}

			return;
		}

		// with no target and no player nearby, turn back into a boulder
		Player near = level().getNearestPlayer(this, 12.0);

		if (getTarget() == null && (near == null || near.isCreative() || near.isSpectator())) {
			if (++idleTicks > 200) {
				entityData.set(DATA_DORMANT, true);
				idleTicks = 0;
			}
		} else {
			idleTicks = 0;
		}
	}

	private void wake() {
		entityData.set(DATA_DORMANT, false);
		idleTicks = 0;
		playSound(SoundEvents.AMETHYST_BLOCK_RESONATE, 1.5f, 0.6f);
		playSound(SoundEvents.DEEPSLATE_BREAK, 1.2f, 0.7f);

		if (level() instanceof ServerLevel server) {
			server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, DepthsContent.CRYSTAL_STONE.defaultBlockState()),
					getX(), getY() + 0.5, getZ(), 40, 0.6, 0.4, 0.6, 0.1);
		}
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		boolean hurt = super.hurtServer(level, source, amount);

		if (hurt && isDormant()) {
			wake();
		}

		return hurt;
	}

	@Override
	public boolean doHurtTarget(ServerLevel level, Entity target) {
		boolean hit = super.doHurtTarget(level, target);
		entityData.set(DATA_SLAM, SLAM_TICKS);
		playSound(SoundEvents.IRON_GOLEM_ATTACK, 1.0f, 0.7f);

		if (hit && target instanceof LivingEntity living) {
			Vec3 away = living.position().subtract(position()).normalize();
			living.push(away.x * 0.6, 0.35, away.z * 0.6);
			living.needsSync = true;
		}

		return hit;
	}

	@Override
	public boolean isPushable() {
		return !isDormant() && super.isPushable();
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return isDormant() ? null : SoundEvents.AMETHYST_BLOCK_CHIME;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.DEEPSLATE_HIT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.AMETHYST_BLOCK_BREAK;
	}

	@Override
	protected void playStepSound(BlockPos pos, BlockState state) {
		playSound(SoundEvents.DEEPSLATE_STEP, 0.8f, 0.7f);
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putBoolean("Dormant", isDormant());
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		entityData.set(DATA_DORMANT, input.getBooleanOr("Dormant", true));
	}
}
