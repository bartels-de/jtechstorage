package dev.bartels.jtechstorage.depths.temple;

import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
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
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import dev.bartels.jtechstorage.ModContent;
import dev.bartels.jtechstorage.ModSounds;
import dev.bartels.jtechstorage.depths.DepthsContent;
import dev.bartels.jtechstorage.entity.CrystalShard;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;

/**
 * The Last Storage Master (v0.39): final boss of the Crystal Depths, a ghost driven mad by waiting, in the Storage Masters'
 * Temple. Hovers, keeps its distance and fights in three phases:
 * <ol>
 * <li><b>Shard Fan</b> (above 2/3 health): throws fans of crystal shards and dodges melee attackers with a leap.</li>
 * <li><b>Seal Guard</b> (down to 1/3): summons Lurkers and Swarm Shards; as long as its servants live, a seal blocks 75 %
 * of the damage.</li>
 * <li><b>Storage Storm</b> (below that): shard rain above the target and a shockwave when approached too closely.</li>
 * </ol>
 * Speaks (chat) during the fight and drops the Storage Masters' Hammer.
 */
public class LastMaster extends Monster {
	private static final EntityDataAccessor<Integer> DATA_ANIM = SynchedEntityData.defineId(LastMaster.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> DATA_ANIM_TICKS = SynchedEntityData.defineId(LastMaster.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Boolean> DATA_SHIELD = SynchedEntityData.defineId(LastMaster.class, EntityDataSerializers.BOOLEAN);
	public static final int ANIM_NONE = 0;
	public static final int ANIM_CAST = 1;
	public static final int ANIM_SUMMON = 2;
	public static final int ANIM_SLAM = 3;
	public static final int CAST_TICKS = 16;
	public static final int SUMMON_TICKS = 30;
	public static final int SLAM_TICKS = 14;
	public static final int COLOR = 0x86CEDE;

	private final ServerBossEvent bossEvent = new ServerBossEvent(UUID.randomUUID(),
			Component.translatable("entity.jtechstorage.last_master").withStyle(ChatFormatting.LIGHT_PURPLE),
			BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.NOTCHED_12);
	private int phase = 0;
	private boolean greeted;
	private @Nullable BlockPos home;

	public LastMaster(EntityType<? extends LastMaster> type, Level level) {
		super(type, level);
		this.xpReward = 500;
		setNoGravity(true);
		setPersistenceRequired();
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
				.add(Attributes.MAX_HEALTH, 500.0)
				.add(Attributes.ATTACK_DAMAGE, 10.0)
				.add(Attributes.MOVEMENT_SPEED, 0.3)
				.add(Attributes.FLYING_SPEED, 0.3)
				.add(Attributes.ARMOR, 10.0)
				.add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
				.add(Attributes.FOLLOW_RANGE, 48.0);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DATA_ANIM, ANIM_NONE);
		builder.define(DATA_ANIM_TICKS, 0);
		builder.define(DATA_SHIELD, false);
	}

	public int getAnim() {
		return entityData.get(DATA_ANIM);
	}

	public int getAnimTicks() {
		return entityData.get(DATA_ANIM_TICKS);
	}

	public boolean isShielded() {
		return entityData.get(DATA_SHIELD);
	}

	private void playAnim(int anim, int ticks) {
		entityData.set(DATA_ANIM, anim);
		entityData.set(DATA_ANIM_TICKS, ticks);
	}

	/** Center of the arena (the seal block); it does not move more than 24 blocks away from it. */
	public void setHome(@Nullable BlockPos home) {
		this.home = home;
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(1, new MasterFightGoal());
		goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 32.0f));
		targetSelector.addGoal(1, new HurtByTargetGoal(this));
		targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, false));
	}

	@Override
	public boolean causeFallDamage(double distance, float multiplier, DamageSource source) {
		return false;
	}

	@Override
	public void aiStep() {
		super.aiStep();

		if (level().isClientSide()) {
			if (random.nextInt(2) == 0) {
				level().addParticle(new DustParticleOptions(COLOR, 1.2f), getRandomX(0.9), getY() + random.nextDouble() * 0.8,
						getRandomZ(0.9), 0.0, -0.03, 0.0);
			}

			if (isShielded() && random.nextInt(2) == 0) {
				double a = random.nextDouble() * Math.PI * 2;
				level().addParticle(ParticleTypes.END_ROD, getX() + Math.cos(a) * 1.6, getY() + 0.5 + random.nextDouble() * 2.4,
						getZ() + Math.sin(a) * 1.6, 0, 0.01, 0);
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

		float frac = getHealth() / getMaxHealth();
		int newPhase = frac > 2.0f / 3.0f ? 0 : (frac > 1.0f / 3.0f ? 1 : 2);

		if (newPhase > phase) {
			phase = newPhase;
			say(phase == 1 ? 2 : 3);
			playSound(SoundEvents.EVOKER_PREPARE_SUMMON, 3.0f, 0.6f);
		}

		if (!greeted && getTarget() != null) {
			greeted = true;
			say(1);
		}

		entityData.set(DATA_SHIELD, phase >= 1 && servants((ServerLevel) level()) > 0);
		bossEvent.setProgress(frac);
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		if (isShielded() && !source.isCreativePlayer()) {
			amount *= 0.25f;
			level.sendParticles(ParticleTypes.ENCHANT, getX(), getY() + 1.5, getZ(), 20, 0.6, 0.8, 0.6, 0.4);
		}

		return super.hurtServer(level, source, amount);
	}

	private int servants(ServerLevel level) {
		return level.getEntitiesOfClass(Mob.class, getBoundingBox().inflate(32), m -> m.entityTags().contains("jts_master_servant") && m.isAlive()).size();
	}

	/** Speaks to all players nearby (line n from the language file). */
	private void say(int line) {
		if (!(level() instanceof ServerLevel level)) {
			return;
		}

		Component text = Component.translatable("jtechstorage.last_master.say." + line).withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.ITALIC);
		Component name = Component.translatable("entity.jtechstorage.last_master").withStyle(ChatFormatting.DARK_PURPLE);

		for (ServerPlayer player : level.players()) {
			if (player.distanceToSqr(this) < 64 * 64) {
				player.sendSystemMessage(Component.literal("<").append(name).append("> ").append(text));
			}
		}
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
		say(4);
		super.die(source);

		if (level() instanceof ServerLevel level) {
			level.sendParticles(ParticleTypes.SOUL, getX(), getY() + 1.5, getZ(), 120, 0.8, 1.4, 0.8, 0.08);
			level.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 1.5, getZ(), 80, 0.6, 1.2, 0.6, 0.2);

			for (Mob servant : level.getEntitiesOfClass(Mob.class, getBoundingBox().inflate(48), m -> m.entityTags().contains("jts_master_servant"))) {
				servant.discard();
			}
		}
	}

	@Override
	protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
		super.dropCustomDeathLoot(level, source, recentlyHit);
		spawnAtLocation(level, new ItemStack(TempleContent.MASTER_HAMMER));
		spawnAtLocation(level, new ItemStack(DepthsContent.PURE_AMETHYST, 4 + random.nextInt(4)));
		spawnAtLocation(level, new ItemStack(ModContent.AMETHYST_CORE));
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
	public float getVoicePitch() {
		return 0.6f;
	}

	@Override
	protected float getSoundVolume() {
		return 3.0f;
	}

	@Override
	public int getAmbientSoundInterval() {
		return 160;
	}

	@Override
	public void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putInt("Phase", phase);
		output.putBoolean("Greeted", greeted);

		if (home != null) {
			output.store("Home", BlockPos.CODEC, home);
		}
	}

	@Override
	public void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		phase = input.getIntOr("Phase", 0);
		greeted = input.getBooleanOr("Greeted", false);
		home = input.read("Home", BlockPos.CODEC).orElse(null);
	}

	// ------------------------------------------------------------------
	// Attacks
	// ------------------------------------------------------------------

	/** Fan of 3 (phase 1) or 5 shards at the target. */
	private void shardFan(ServerLevel level, LivingEntity target) {
		int n = phase == 0 ? 3 : 5;
		Vec3 eye = position().add(0, 2.4, 0);
		Vec3 dir = target.getEyePosition().subtract(eye).normalize();

		for (int i = 0; i < n; i++) {
			double spread = (i - (n - 1) / 2.0) * 0.16;
			Vec3 d = dir.yRot((float) spread);
			CrystalShard shard = new CrystalShard(level, this).withDamage(phase == 0 ? 6.0f : 7.0f);
			shard.setPos(eye.x, eye.y, eye.z);
			shard.shoot(d.x, d.y, d.z, 1.3f, 2.0f);
			level.addFreshEntity(shard);
		}

		playSound(SoundEvents.AMETHYST_CLUSTER_BREAK, 2.0f, 0.6f);
	}

	/** Shard rain: 7 shards fall around the target. */
	private void shardRain(ServerLevel level, LivingEntity target) {
		for (int i = 0; i < 7; i++) {
			double x = target.getX() + (random.nextDouble() - 0.5) * 6.0;
			double z = target.getZ() + (random.nextDouble() - 0.5) * 6.0;
			CrystalShard shard = new CrystalShard(level, this).withDamage(6.0f);
			shard.setPos(x, target.getY() + 9.0 + random.nextDouble() * 3.0, z);
			shard.shoot(0, -1, 0, 0.9f, 1.0f);
			level.addFreshEntity(shard);
			level.sendParticles(ParticleTypes.REVERSE_PORTAL, x, target.getY() + 9.0, z, 6, 0.2, 0.2, 0.2, 0.02);
		}

		playSound(SoundEvents.AMETHYST_BLOCK_RESONATE, 3.0f, 0.5f);
	}

	/** Shockwave: pushes away all players within 6 blocks. */
	private void shockwave(ServerLevel level) {
		List<Player> near = level.getEntitiesOfClass(Player.class, getBoundingBox().inflate(6.0), p -> !p.isSpectator() && !p.isCreative());

		for (Player player : near) {
			Vec3 push = player.position().subtract(position()).normalize().scale(1.6);
			player.hurtServer(level, damageSources().mobAttack(this), 9.0f);
			player.push(push.x, 0.6, push.z);
			if (player instanceof ServerPlayer sp) {
				sp.connection.send(new ClientboundSetEntityMotionPacket(sp));
			}
		}

		level.sendParticles(ParticleTypes.SONIC_BOOM, getX(), getY() + 1.0, getZ(), 1, 0, 0, 0, 0);
		level.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 0.5, getZ(), 60, 3.0, 0.3, 3.0, 0.15);
		playSound(SoundEvents.WARDEN_SONIC_BOOM, 2.0f, 1.4f);
	}

	/** Summons 2 Lurkers and 3 Swarm Shards as servants. */
	private void summonServants(ServerLevel level) {
		playSound(SoundEvents.EVOKER_CAST_SPELL, 3.0f, 0.7f);

		for (int i = 0; i < 5; i++) {
			EntityType<? extends Mob> type = i < 2 ? DepthsContent.CRYSTAL_LURKER : DepthsContent.SWARM_SHARD;
			Mob servant = type.create(level, EntitySpawnReason.MOB_SUMMONED);

			if (servant == null) {
				continue;
			}

			double a = random.nextDouble() * Math.PI * 2;
			double r = 3.0 + random.nextDouble() * 3.0;
			double y = i < 2 ? floorBelow(level, getX() + Math.cos(a) * r, getZ() + Math.sin(a) * r) : getY() + 1.0;
			servant.snapTo(getX() + Math.cos(a) * r, y, getZ() + Math.sin(a) * r, random.nextFloat() * 360, 0);
			servant.addTag("jts_master_servant");
			servant.setPersistenceRequired();
			servant.setTarget(getTarget());
			level.addFreshEntity(servant);
			level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, servant.getX(), servant.getY() + 0.5, servant.getZ(), 20, 0.4, 0.5, 0.4, 0.03);
		}
	}

	private double floorBelow(ServerLevel level, double x, double z) {
		BlockPos.MutableBlockPos p = BlockPos.containing(x, getY() + 1, z).mutable();

		for (int i = 0; i < 12; i++) {
			if (!level.getBlockState(p.below()).getCollisionShape(level, p.below()).isEmpty()) {
				return p.getY();
			}

			p.move(0, -1, 0);
		}

		return getY();
	}

	/** Short leap away from the target (6–10 blocks), only onto free spots. */
	private void blink(ServerLevel level, LivingEntity target) {
		for (int tries = 0; tries < 12; tries++) {
			Vec3 away = position().subtract(target.position()).normalize();
			double a = Math.atan2(away.z, away.x) + (random.nextDouble() - 0.5) * 2.0;
			double r = 6.0 + random.nextDouble() * 4.0;
			double x = getX() + Math.cos(a) * r;
			double z = getZ() + Math.sin(a) * r;
			double y = floorBelow(level, x, z) + 1.5;

			if (home != null && home.distToCenterSqr(x, y, z) > 24 * 24) {
				continue;
			}

			Vec3 old = position();

			if (randomTeleport(x, y, z, true, st -> true)) {
				level.sendParticles(ParticleTypes.REVERSE_PORTAL, old.x, old.y + 1.5, old.z, 40, 0.5, 1.0, 0.5, 0.1);
				playSound(SoundEvents.ENDERMAN_TELEPORT, 2.0f, 0.6f);
				return;
			}
		}
	}

	/** Hovering: keeps 7–12 blocks of distance from the target and about 2 blocks of height above the ground. */
	private void hover(ServerLevel level, @Nullable LivingEntity target) {
		Vec3 want = position();

		if (target != null) {
			Vec3 flat = new Vec3(getX() - target.getX(), 0, getZ() - target.getZ());
			double dist = flat.length();
			double ideal = phase == 2 ? 6.0 : 9.0;
			Vec3 dir = dist < 0.01 ? new Vec3(1, 0, 0) : flat.normalize();
			want = target.position().add(dir.scale(Mth.clamp(dist, ideal - 2, ideal + 3)));
			// slowly circle around the target
			want = want.add(dir.yRot((float) (Math.PI / 2)).scale(1.2));
		} else if (home != null) {
			want = Vec3.atCenterOf(home).add(0, 2, 0);
		}

		if (home != null && Vec3.atCenterOf(home).distanceToSqr(want) > 22 * 22) {
			want = Vec3.atCenterOf(home).add(want.subtract(Vec3.atCenterOf(home)).normalize().scale(22));
		}

		double ground = floorBelow(level, want.x, want.z);
		double targetY = Math.max(ground + 2.0, target != null ? target.getY() + 1.0 : ground + 2.0);
		Vec3 delta = new Vec3(want.x - getX(), targetY - getY(), want.z - getZ());
		Vec3 move = delta.lengthSqr() > 0.04 ? delta.normalize().scale(Math.min(0.28, delta.length() * 0.2)) : Vec3.ZERO;
		setDeltaMovement(getDeltaMovement().scale(0.6).add(move.scale(0.4)));
	}

	/** Fight flow: keep distance, attacks per phase. */
	private class MasterFightGoal extends Goal {
		private int castCooldown = 40;
		private int summonCooldown = 20;
		private int blinkCooldown = 100;
		private int rainCooldown = 80;
		private int slamCooldown = 60;

		MasterFightGoal() {
			setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
		}

		@Override
		public boolean canUse() {
			return true;
		}

		@Override
		public boolean requiresUpdateEveryTick() {
			return true;
		}

		@Override
		public void tick() {
			if (!(level() instanceof ServerLevel level)) {
				return;
			}

			LivingEntity target = getTarget();
			hover(level, target);

			if (target == null || !target.isAlive()) {
				return;
			}

			getLookControl().setLookAt(target, 30.0f, 30.0f);
			double dist = distanceToSqr(target);
			castCooldown--;
			summonCooldown--;
			blinkCooldown--;
			rainCooldown--;
			slamCooldown--;
			int anim = getAnim();
			int ticks = getAnimTicks();

			if (anim == ANIM_CAST && ticks == CAST_TICKS / 2 && hasLineOfSight(target)) {
				shardFan(level, target);
			} else if (anim == ANIM_SUMMON && ticks == SUMMON_TICKS / 2) {
				if (phase >= 1 && servants(level) < 2) {
					summonServants(level);
				} else {
					shardRain(level, target);
				}
			} else if (anim == ANIM_SLAM && ticks == SLAM_TICKS / 2) {
				shockwave(level);
			}

			if (anim != ANIM_NONE) {
				return;
			}

			if (phase >= 2 && slamCooldown <= 0 && dist < 5.5 * 5.5) {
				slamCooldown = 70;
				playAnim(ANIM_SLAM, SLAM_TICKS);
				return;
			}

			if (blinkCooldown <= 0 && dist < 4 * 4) {
				blinkCooldown = phase == 0 ? 120 : 180;
				blink(level, target);
				return;
			}

			if (phase >= 1 && summonCooldown <= 0 && servants(level) < 2) {
				summonCooldown = 500;
				playAnim(ANIM_SUMMON, SUMMON_TICKS);
				return;
			}

			if (phase >= 2 && rainCooldown <= 0) {
				rainCooldown = 120;
				playAnim(ANIM_SUMMON, SUMMON_TICKS);
				return;
			}

			if (castCooldown <= 0 && dist < 32 * 32 && hasLineOfSight(target)) {
				castCooldown = phase == 0 ? 45 : 35;
				playAnim(ANIM_CAST, CAST_TICKS);
				playSound(ModSounds.HERALD_CHARGE, 1.5f, 0.6f);
			}
		}
	}

	/** When summoned, it stands next to the seal and looks around. */
	public void onSummoned(BlockPos seal) {
		setHome(seal);
		playSound(SoundEvents.WITHER_SPAWN, 2.0f, 1.3f);
	}

	@Override
	public boolean isPushable() {
		return false;
	}

	@Override
	protected void doPush(Entity entity) {
	}
}
