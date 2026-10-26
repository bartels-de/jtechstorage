package dev.bartels.jtechstorage.entity;

import java.util.EnumSet;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import dev.bartels.jtechstorage.ModContent;
import dev.bartels.jtechstorage.ModSounds;
import dev.bartels.jtechstorage.storm.StormContent;
import dev.bartels.jtechstorage.storm.ResonanceStorm;

/**
 * Storm Herald: leader of the Resonance Storm (mini-boss). Hovers just above the ground, keeps its distance, hurls
 * Resonance Bolts and summons new shardlings. Its death ends the storm.
 */
public class StormHerald extends Monster {
	private static final EntityDataAccessor<Integer> DATA_ANIM = SynchedEntityData.defineId(StormHerald.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> DATA_ANIM_TICKS = SynchedEntityData.defineId(StormHerald.class, EntityDataSerializers.INT);
	public static final int ANIM_NONE = 0;
	public static final int ANIM_BOLT = 1;
	public static final int ANIM_SUMMON = 2;
	public static final int BOLT_TICKS = 20;
	public static final int SUMMON_TICKS = 30;

	private final ServerBossEvent bossEvent = new ServerBossEvent(UUID.randomUUID(),
			Component.translatable("entity.jtechstorage.storm_herald").withStyle(ChatFormatting.RED),
			BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.NOTCHED_10);
	private @Nullable BlockPos stormCenter;

	public StormHerald(EntityType<? extends StormHerald> type, Level level) {
		super(type, level);
		this.xpReward = 60;
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
				.add(Attributes.MAX_HEALTH, 120.0)
				.add(Attributes.ATTACK_DAMAGE, 6.0)
				.add(Attributes.MOVEMENT_SPEED, 0.26)
				.add(Attributes.ARMOR, 6.0)
				.add(Attributes.KNOCKBACK_RESISTANCE, 0.8)
				.add(Attributes.FOLLOW_RANGE, 48.0);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DATA_ANIM, ANIM_NONE);
		builder.define(DATA_ANIM_TICKS, 0);
	}

	public int getAnim() {
		return entityData.get(DATA_ANIM);
	}

	public int getAnimTicks() {
		return entityData.get(DATA_ANIM_TICKS);
	}

	private void playAnim(int anim, int ticks) {
		entityData.set(DATA_ANIM, anim);
		entityData.set(DATA_ANIM_TICKS, ticks);
	}

	public void setStormCenter(@Nullable BlockPos center) {
		this.stormCenter = center;
		if (center != null) {
			setPersistenceRequired();
		}
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(1, new HeraldAttackGoal());
		goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.7));
		goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 24.0f));
		goalSelector.addGoal(7, new RandomLookAroundGoal(this));
		targetSelector.addGoal(1, new HurtByTargetGoal(this));
		targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
		targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10, true, false,
				(target, level) -> target instanceof StormTarget hero && !hero.isStormTargetHologram() || target instanceof IronGolem));
	}

	@Override
	public void aiStep() {
		super.aiStep();

		if (level().isClientSide()) {
			if (random.nextInt(3) == 0) {
				level().addParticle(new DustParticleOptions(CrystalShardling.COLOR, 1.0f), getRandomX(0.8), getY() + random.nextDouble() * 0.4,
						getRandomZ(0.8), 0.0, -0.02, 0.0);
			}
			return;
		}

		int ticks = getAnimTicks();

		if (ticks > 0) {
			entityData.set(DATA_ANIM_TICKS, ticks - 1);
			if (ticks - 1 == 0) {
				entityData.set(DATA_ANIM, ANIM_NONE);
			}
		}

		bossEvent.setProgress(getHealth() / getMaxHealth());
	}

	@Override
	public void startSeenByPlayer(ServerPlayer player) {
		super.startSeenByPlayer(player);
		bossEvent.addPlayer(player);
	}

	@Override
	public void stopSeenByPlayer(ServerPlayer player) {
		super.stopSeenByPlayer(player);
		bossEvent.removePlayer(player);
	}

	@Override
	public void die(DamageSource source) {
		super.die(source);

		if (level() instanceof ServerLevel level) {
			level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.AMETHYST_BLOCK.defaultBlockState()),
					getX(), getY() + 1.5, getZ(), 80, 0.6, 1.2, 0.6, 0.15);
			level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, getX(), getY() + 1.5, getZ(), 1, 0, 0, 0, 0);
		}
	}

	@Override
	protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
		super.dropCustomDeathLoot(level, source, recentlyHit);
		spawnAtLocation(level, new ItemStack(Items.AMETHYST_SHARD, 6 + random.nextInt(7)));
		spawnAtLocation(level, new ItemStack(Items.AMETHYST_BLOCK, 1 + random.nextInt(3)));

		if (random.nextFloat() < 0.2f) {
			spawnAtLocation(level, new ItemStack(ModContent.AMETHYST_CORE));
		}
	}

	@Override
	public boolean shouldBeSaved() {
		return stormCenter == null && super.shouldBeSaved();
	}

	@Override
	public boolean removeWhenFarAway(double distance) {
		return false;
	}

	@Override
	protected @Nullable SoundEvent getAmbientSound() {
		return ModSounds.HERALD_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return ModSounds.HERALD_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return ModSounds.HERALD_DEATH;
	}

	@Override
	public int getAmbientSoundInterval() {
		return 200;
	}

	@Override
	protected float getSoundVolume() {
		return 2.0f;
	}

	// ------------------------------------------------------------------
	// Combat
	// ------------------------------------------------------------------

	private void fireBolt(ServerLevel level, LivingEntity target) {
		Vec3 from = position().add(0, 3.0, 0);
		Vec3 to = target.position().add(0, target.getBbHeight() * 0.5, 0);
		Vec3 step = to.subtract(from);
		int points = Mth.ceil(step.length() * 3);

		for (int i = 0; i <= points; i++) {
			Vec3 p = from.add(step.scale(i / (double) points));
			double jitter = (i % 2 == 0 ? 1 : -1) * 0.15;
			level.sendParticles(new DustParticleOptions(CrystalShardling.COLOR, 1.3f), p.x + jitter, p.y, p.z - jitter, 1, 0, 0, 0, 0);
			if (i % 3 == 0) {
				level.sendParticles(ParticleTypes.ELECTRIC_SPARK, p.x, p.y, p.z, 1, 0.05, 0.05, 0.05, 0.02);
			}
		}

		target.hurtServer(level, damageSources().indirectMagic(this, this), 7.0f);
		playSound(ModSounds.HERALD_BOLT, 2.0f, 0.9f + random.nextFloat() * 0.2f);
	}

	private void summon(ServerLevel level) {
		playSound(ModSounds.HERALD_SUMMON, 2.0f, 1.0f);

		for (int i = 0; i < 3; i++) {
			CrystalShardling shardling = StormContent.CRYSTAL_SHARDLING.create(level, EntitySpawnReason.MOB_SUMMONED);

			if (shardling == null) {
				continue;
			}

			double angle = random.nextDouble() * Math.PI * 2;
			shardling.snapTo(getX() + Math.cos(angle) * 2.5, getY(), getZ() + Math.sin(angle) * 2.5, random.nextFloat() * 360, 0);
			shardling.setStormCenter(stormCenter);
			shardling.setTarget(getTarget());
			level.addFreshEntity(shardling);
			ResonanceStorm.adopt(level, shardling);
			level.sendParticles(ParticleTypes.REVERSE_PORTAL, shardling.getX(), shardling.getY() + 0.3, shardling.getZ(), 20, 0.3, 0.3, 0.3, 0.05);
		}
	}

	/** Keeps 6–14 blocks of distance, bolt every 2.5 seconds, summon every 12 seconds (when few shardlings are around). */
	private class HeraldAttackGoal extends Goal {
		private int boltCooldown = 40;
		private int summonCooldown = 100;

		HeraldAttackGoal() {
			setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
		}

		@Override
		public boolean canUse() {
			LivingEntity target = getTarget();
			return target != null && target.isAlive();
		}

		@Override
		public void stop() {
			getNavigation().stop();
		}

		@Override
		public boolean requiresUpdateEveryTick() {
			return true;
		}

		@Override
		public void tick() {
			LivingEntity target = getTarget();

			if (target == null || !(level() instanceof ServerLevel level)) {
				return;
			}

			getLookControl().setLookAt(target, 30.0f, 30.0f);
			double dist = distanceToSqr(target);

			if (dist > 14 * 14 || !hasLineOfSight(target)) {
				getNavigation().moveTo(target, 1.0);
			} else if (dist < 6 * 6) {
				Vec3 away = position().subtract(target.position()).normalize().scale(5);
				getNavigation().moveTo(getX() + away.x, getY(), getZ() + away.z, 1.1);
			} else {
				getNavigation().stop();
			}

			boltCooldown--;
			summonCooldown--;
			int anim = getAnim();
			int ticks = getAnimTicks();

			if (anim == ANIM_BOLT && ticks == BOLT_TICKS / 2 && hasLineOfSight(target) && dist < 24 * 24) {
				fireBolt(level, target);
			} else if (anim == ANIM_SUMMON && ticks == SUMMON_TICKS / 2) {
				summon(level);
			}

			if (anim != ANIM_NONE) {
				return;
			}

			if (summonCooldown <= 0) {
				int near = level.getEntitiesOfClass(CrystalShardling.class, getBoundingBox().inflate(16)).size();
				summonCooldown = 240;

				if (near < 8) {
					playAnim(ANIM_SUMMON, SUMMON_TICKS);
					return;
				}
			}

			if (boltCooldown <= 0 && dist < 24 * 24 && hasLineOfSight(target)) {
				boltCooldown = 50;
				playAnim(ANIM_BOLT, BOLT_TICKS);
				playSound(ModSounds.HERALD_CHARGE, 1.5f, 1.0f);
			}
		}
	}
}
