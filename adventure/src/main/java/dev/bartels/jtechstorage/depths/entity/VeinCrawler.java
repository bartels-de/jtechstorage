package dev.bartels.jtechstorage.depths.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import dev.bartels.jtechstorage.entity.CrystalShard;

/**
 * Vein Crawler (Crystal Depths, v0.31): six-legged crawler of the Vein Gorge, immune to fire and lava. Spits glowing
 * crystal shards from a safe distance that briefly set targets on fire.
 */
public class VeinCrawler extends Monster implements RangedAttackMob {
	private static final EntityDataAccessor<Integer> DATA_SPIT = SynchedEntityData.defineId(VeinCrawler.class, EntityDataSerializers.INT);
	public static final int SPIT_TICKS = 10;

	public VeinCrawler(EntityType<? extends VeinCrawler> type, Level level) {
		super(type, level);
		this.xpReward = 10;
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
				.add(Attributes.MAX_HEALTH, 26.0)
				.add(Attributes.ATTACK_DAMAGE, 4.0)
				.add(Attributes.MOVEMENT_SPEED, 0.27)
				.add(Attributes.ARMOR, 4.0)
				.add(Attributes.FOLLOW_RANGE, 28.0);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DATA_SPIT, 0);
	}

	public int getSpitTicks() {
		return entityData.get(DATA_SPIT);
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(2, new RangedAttackGoal(this, 1.0, 35, 50, 14.0f));
		goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8));
		goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 12.0f));
		targetSelector.addGoal(1, new HurtByTargetGoal(this));
		targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
	}

	@Override
	public void performRangedAttack(LivingEntity target, float power) {
		CrystalShard shard = new CrystalShard(level(), this).withDamage(4.0f);
		double dx = target.getX() - getX();
		double dy = target.getY(0.4) - shard.getY();
		double dz = target.getZ() - getZ();
		double dist = Math.sqrt(dx * dx + dz * dz);
		shard.shoot(dx, dy + dist * 0.12, dz, 1.3f, 6.0f);
		shard.igniteForSeconds(4.0f);
		level().addFreshEntity(shard);
		entityData.set(DATA_SPIT, SPIT_TICKS);
		playSound(SoundEvents.BLAZE_SHOOT, 0.8f, 1.3f);
	}

	@Override
	public void aiStep() {
		super.aiStep();
		int spit = getSpitTicks();

		if (spit > 0 && !level().isClientSide()) {
			entityData.set(DATA_SPIT, spit - 1);
		}

		if (level().isClientSide() && random.nextInt(10) == 0) {
			level().addParticle(ParticleTypes.SMALL_FLAME, getRandomX(0.5), getY() + 0.8, getRandomZ(0.5), 0, 0.01, 0);
		}
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.SPIDER_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.SPIDER_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.SPIDER_DEATH;
	}

	@Override
	public float getVoicePitch() {
		return 0.6f + random.nextFloat() * 0.1f;
	}

	@Override
	protected void playStepSound(BlockPos pos, BlockState state) {
		playSound(SoundEvents.SPIDER_STEP, 0.15f, 0.8f);
	}
}
