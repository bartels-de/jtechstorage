package dev.bartels.jtechstorage.depths.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Schwarmsplitter (Kristalltiefe, v0.31): kleine fliegende Kristallsplitter, die in Schwärmen kommen. Schwach, aber
 * schnell – sie stürzen sich auf dich und schwirren gleich wieder weg.
 */
public class SwarmShard extends Monster {
	private static final EntityDataAccessor<Integer> DATA_DASH = SynchedEntityData.defineId(SwarmShard.class, EntityDataSerializers.INT);
	public static final int DASH_TICKS = 8;
	private static final int COLOR = 0xB48AF4;

	public SwarmShard(EntityType<? extends SwarmShard> type, Level level) {
		super(type, level);
		this.moveControl = new FlyingMoveControl(this, 20, true);
		this.xpReward = 3;
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
				.add(Attributes.MAX_HEALTH, 6.0)
				.add(Attributes.ATTACK_DAMAGE, 2.5)
				.add(Attributes.MOVEMENT_SPEED, 0.3)
				.add(Attributes.FLYING_SPEED, 0.65)
				.add(Attributes.FOLLOW_RANGE, 24.0);
	}

	@Override
	protected PathNavigation createNavigation(Level level) {
		FlyingPathNavigation navigation = new FlyingPathNavigation(this, level);
		navigation.setCanFloat(true);
		return navigation;
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DATA_DASH, 0);
	}

	public int getDashTicks() {
		return entityData.get(DATA_DASH);
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.4, true));
		goalSelector.addGoal(6, new WaterAvoidingRandomFlyingGoal(this, 1.0));
		goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0f));
		targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
		targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
	}

	@Override
	public void aiStep() {
		super.aiStep();
		int dash = getDashTicks();

		if (dash > 0 && !level().isClientSide()) {
			entityData.set(DATA_DASH, dash - 1);
		}

		if (level().isClientSide() && random.nextInt(4) == 0) {
			level().addParticle(new DustParticleOptions(COLOR, 0.6f), getRandomX(0.4), getRandomY(), getRandomZ(0.4), 0, 0, 0);
		}
	}

	@Override
	public boolean doHurtTarget(ServerLevel level, Entity target) {
		boolean hit = super.doHurtTarget(level, target);
		entityData.set(DATA_DASH, DASH_TICKS);
		playSound(SoundEvents.AMETHYST_CLUSTER_HIT, 1.0f, 1.4f + random.nextFloat() * 0.3f);
		// nach dem Treffer kurz zurückweichen
		setDeltaMovement(getDeltaMovement().add(position().subtract(target.position()).normalize().scale(0.5)).add(0, 0.2, 0));
		return hit;
	}

	@Override
	public boolean causeFallDamage(double distance, float multiplier, DamageSource source) {
		return false;
	}

	@Override
	protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.AMETHYST_BLOCK_CHIME;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.AMETHYST_CLUSTER_HIT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.AMETHYST_CLUSTER_BREAK;
	}

	@Override
	protected float getSoundVolume() {
		return 0.5f;
	}
}
