package dev.bartels.jtechstorage.entity;

import java.util.EnumSet;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
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
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

import dev.bartels.jtechstorage.ModConfig;
import dev.bartels.jtechstorage.ModSounds;
import dev.bartels.jtechstorage.ModContent;

/**
 * Der Amethyst-Roboter – Bossgegner, aktiviert mit dem Resonanz-Zünder.
 * 400 Leben, Rüstung, Bossleiste. Angriffe: Kristallfaust, Armschwung (seitlich/hinten), Resonanz-Impuls
 * (Verlangsamung + Schwäche), Stampf-Schockwelle (schleudert hoch), Düsen-Sturm (mit Ankündigung) und
 * Laserstrahl aus der Armkanone (lädt sichtbar auf – ausweichen!). Ab 50 % Leben: Überlast – schneller,
 * kürzere Abklingzeiten, stärkere Faust und Laser. Beute: Eisen-Reichweiten- und Eisen-Kapazitäts-Upgrade, mit 30 % ein Kern (Amethyst- oder Funk-Kern).
 */
public class AmethystRobot extends Monster {
	public static final int ANIM_NONE = 0;
	public static final int ANIM_SLAM = 1;
	public static final int ANIM_SWEEP = 2;
	public static final int ANIM_PULSE = 3;
	public static final int ANIM_STOMP = 4;
	public static final int ANIM_DASH = 5;
	public static final int ANIM_LASER = 6;
	public static final int LASER_CHARGE = 30;
	private static final double LASER_RANGE = 26.0;
	/** Chance auf einen Kern (Amethyst- oder Funk-Kern, je zur Hälfte). */
	public static final float CORE_CHANCE = (float) ModConfig.get().bosses.robotCoreChance;

	private static final EntityDataAccessor<Integer> DATA_ANIM = SynchedEntityData.defineId(AmethystRobot.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> DATA_ANIM_TICKS = SynchedEntityData.defineId(AmethystRobot.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Boolean> DATA_ENRAGED = SynchedEntityData.defineId(AmethystRobot.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Boolean> DATA_HOLOGRAM = SynchedEntityData.defineId(AmethystRobot.class, EntityDataSerializers.BOOLEAN);

	private final ServerBossEvent bossEvent = new ServerBossEvent(UUID.randomUUID(),
			Component.translatable("entity.jtechstorage.amethyst_robot").withStyle(ChatFormatting.LIGHT_PURPLE),
			BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.NOTCHED_10);

	public AmethystRobot(EntityType<? extends AmethystRobot> type, Level level) {
		super(type, level);
		this.xpReward = 150;
		setPersistenceRequired();
	}

	/** Schaden der Spezialangriffe, skaliert mit {@code bosses.robot.damageMultiplier}. */
	private static float scaled(double amount) {
		return (float) (amount * ModConfig.get().bosses.robot.damageMultiplier);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
				.add(Attributes.MAX_HEALTH, ModConfig.get().bosses.robot.health)
				.add(Attributes.ATTACK_DAMAGE, 16.0 * ModConfig.get().bosses.robot.damageMultiplier)
				.add(Attributes.ATTACK_KNOCKBACK, 1.5)
				.add(Attributes.MOVEMENT_SPEED, 0.28)
				.add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
				.add(Attributes.ARMOR, ModConfig.get().bosses.robot.armor)
				.add(Attributes.ARMOR_TOUGHNESS, 4.0)
				.add(Attributes.STEP_HEIGHT, 1.6)
				.add(Attributes.FOLLOW_RANGE, 48.0);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DATA_ANIM, ANIM_NONE);
		builder.define(DATA_ANIM_TICKS, 0);
		builder.define(DATA_ENRAGED, false);
		builder.define(DATA_HOLOGRAM, false);
	}

	public int getAnim() {
		return entityData.get(DATA_ANIM);
	}

	public int getAnimTicks() {
		return entityData.get(DATA_ANIM_TICKS);
	}

	public boolean isEnraged() {
		return entityData.get(DATA_ENRAGED);
	}

	public boolean isHologram() {
		return entityData.get(DATA_HOLOGRAM);
	}

	/** Nur für Jbartels' Rundgang: harmloses, durchscheinendes Abbild. */
	public void makeHologram() {
		entityData.set(DATA_HOLOGRAM, true);
		setNoAi(true);
		setNoGravity(true);
		setPermanentlyInvulnerable(true);
		setSilent(true);
		bossEvent.setVisible(false);
	}

	private void playAnim(int anim, int ticks) {
		entityData.set(DATA_ANIM, anim);
		entityData.set(DATA_ANIM_TICKS, ticks);
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(1, new BossAttackGoal());
		goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.7));
		goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 16.0f));
		goalSelector.addGoal(7, new RandomLookAroundGoal(this));
		targetSelector.addGoal(1, new HurtByTargetGoal(this));
		targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
	}

	// ------------------------------------------------------------------
	// Tick, Bossleiste, Wut-Phase
	// ------------------------------------------------------------------

	@Override
	public void tick() {
		if (isHologram() && !level().isClientSide()) {
			tickCount++;
			setYRot(getYRot() + 1.5f);
			yBodyRot = getYRot();
			yHeadRot = getYRot();
			return;
		}

		super.tick();
	}

	/** Client: Funken am Reaktor, Dampf aus den Schubdüsen, Kristallstaub an den Pylonen. */
	private void ambientEffects() {
		float yaw = yBodyRot * Mth.DEG_TO_RAD;
		double fx = -Mth.sin(yaw);
		double fz = Mth.cos(yaw);

		if (random.nextInt(3) == 0) {
			// Reaktor vorne auf der Brust
			level().addParticle(ParticleTypes.ELECTRIC_SPARK, getX() + fx * 1.0 + (random.nextDouble() - 0.5) * 0.6,
					getY() + 3.4 + (random.nextDouble() - 0.5) * 0.6, getZ() + fz * 1.0 + (random.nextDouble() - 0.5) * 0.6,
					fx * 0.05, 0.02, fz * 0.05);
		}

		if (random.nextInt(4) == 0) {
			// Schubdüsen hinten
			double side = random.nextBoolean() ? 0.6 : -0.6;
			double sx = Mth.cos(yaw) * side;
			double sz = Mth.sin(yaw) * side;
			level().addParticle(getAnim() == ANIM_DASH ? ParticleTypes.FLAME : ParticleTypes.SMOKE,
					getX() - fx * 1.0 + sx, getY() + 2.4, getZ() - fz * 1.0 + sz, -fx * 0.03, -0.05, -fz * 0.03);
		}

		if (random.nextInt(6) == 0) {
			// Kristall-Pylone
			level().addParticle(ParticleTypes.END_ROD, getX() - fx * 1.4 + (random.nextDouble() - 0.5) * 2.0,
					getY() + 5.2 + random.nextDouble() * 0.5, getZ() - fz * 1.4 + (random.nextDouble() - 0.5) * 2.0,
					0.0, 0.01, 0.0);
		}
	}

	@Override
	public void aiStep() {
		super.aiStep();

		if (!(level() instanceof ServerLevel level)) {
			if (!isHologram()) {
				ambientEffects();
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

		if (!isEnraged() && getHealth() < getMaxHealth() * 0.5f) {
			entityData.set(DATA_ENRAGED, true);
			getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.36);
			roar(level);
			level.sendParticles(ParticleTypes.WITCH, getX(), getY() + 3.0, getZ(), 80, 2.0, 1.5, 2.0, 0.1);

			for (ServerPlayer player : level.players()) {
				if (player.distanceToSqr(this) < 48 * 48) {
					player.sendSystemMessage(Component.translatable("jtechstorage.robot.enraged").withStyle(ChatFormatting.DARK_PURPLE));
				}
			}
		}

		if (isEnraged() && tickCount % 10 == 0) {
			level.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 3.5, getZ(), 2, 1.5, 0.5, 1.5, 0.02);
		}
	}

	@Override
	public void startSeenByPlayer(ServerPlayer player) {
		super.startSeenByPlayer(player);

		if (!isHologram()) {
			bossEvent.addPlayer(player);
		}
	}

	@Override
	public void stopSeenByPlayer(ServerPlayer player) {
		super.stopSeenByPlayer(player);
		bossEvent.removePlayer(player);
	}

	@Override
	public void remove(Entity.RemovalReason reason) {
		bossEvent.removeAllPlayers();
		super.remove(reason);
	}

	@Override
	public boolean shouldBeSaved() {
		return !isHologram() && super.shouldBeSaved();
	}

	@Override
	public boolean removeWhenFarAway(double distance) {
		return false;
	}

	@Override
	protected void checkFallDamage(double distance, boolean onGround, BlockState state, BlockPos pos) {
		// riesig und robust
	}

	@Override
	public boolean isPushable() {
		return !isHologram();
	}

	// ------------------------------------------------------------------
	// Angriffe
	// ------------------------------------------------------------------

	/** Resonanz-Impuls: Welle aus dem Kristallkern, verlangsamt und schwächt. */
	private void roar(ServerLevel level) {
		playAnim(ANIM_PULSE, 30);
		playSound(SoundEvents.BEACON_POWER_SELECT, 3.0f, 0.5f);
		playSound(SoundEvents.AMETHYST_BLOCK_RESONATE, 3.0f, 0.4f);
		level.sendParticles(ParticleTypes.SONIC_BOOM, getX(), getY() + 3.0, getZ(), 1, 0, 0, 0, 0);

		for (int i = 0; i < 36; i++) {
			double angle = i / 36.0 * Math.PI * 2;
			level.sendParticles(ParticleTypes.END_ROD, getX() + Math.cos(angle) * 3, getY() + 2.5, getZ() + Math.sin(angle) * 3,
					1, Math.cos(angle) * 0.4, 0, Math.sin(angle) * 0.4, 0.3);
		}

		for (Player player : level.getEntitiesOfClass(Player.class, getBoundingBox().inflate(18))) {
			if (!player.isCreative() && !player.isSpectator()) {
				player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 100, 1));
				player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0));
			}
		}
	}

	private void stomp(ServerLevel level) {
		playSound(SoundEvents.GENERIC_EXPLODE.value(), 0.8f, 0.6f);
		playSound(ModSounds.ROBOT_SLAM, 2.5f, 0.9f);

		for (int i = 0; i < 24; i++) {
			double angle = i / 24.0 * Math.PI * 2;
			level.sendParticles(ParticleTypes.CLOUD, getX() + Math.cos(angle) * 4, getY() + 0.2, getZ() + Math.sin(angle) * 4,
					2, 0.2, 0.05, 0.2, 0.05);
		}

		level.sendParticles(ParticleTypes.EXPLOSION, getX(), getY() + 0.3, getZ(), 3, 1.5, 0.1, 1.5, 0);

		for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(7, 2, 7),
				e -> e != this && !(e instanceof AmethystRobot) && e.isAlive())) {
			double distance = target.distanceTo(this);
			float damage = (float) Math.max(4.0, 12.0 - distance);
			target.hurtServer(level, damageSources().mobAttack(this), scaled(damage));
			Vec3 push = target.position().subtract(position()).normalize().scale(0.9);
			target.push(push.x, 0.9, push.z);
		}
	}

	private void tailSwipe(ServerLevel level) {
		playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 2.0f, 0.5f);
		playSound(SoundEvents.PISTON_EXTEND, 1.5f, 0.6f);
		level.sendParticles(ParticleTypes.SWEEP_ATTACK, getX(), getY() + 1.5, getZ(), 6, 3.0, 0.5, 3.0, 0);
		Vec3 facing = Vec3.directionFromRotation(0, yBodyRot);

		for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(5.5, 1.5, 5.5),
				e -> e != this && !(e instanceof AmethystRobot) && e.isAlive())) {
			Vec3 to = target.position().subtract(position()).normalize();

			if (to.dot(facing) > 0.5) {
				continue;   // vorne schlägt er lieber mit der Faust
			}

			target.hurtServer(level, damageSources().mobAttack(this), scaled(12.0f));
			Vec3 push = to.scale(2.0);
			target.push(push.x, 0.5, push.z);
		}
	}

	/** Feuert den Laser entlang {@code direction} (vom Kanonen-Ende aus) – trifft alles auf der Linie bis zur ersten Wand. */
	private void fireLaser(ServerLevel level, Vec3 direction) {
		Vec3 start = position().add(0, 3.6, 0).add(Vec3.directionFromRotation(0, yBodyRot - 90.0f).scale(1.6));
		Vec3 end = start.add(direction.scale(LASER_RANGE));
		net.minecraft.world.phys.BlockHitResult wall = level.clip(new net.minecraft.world.level.ClipContext(start, end,
				net.minecraft.world.level.ClipContext.Block.COLLIDER, net.minecraft.world.level.ClipContext.Fluid.NONE, this));

		if (wall.getType() != net.minecraft.world.phys.HitResult.Type.MISS) {
			end = wall.getLocation();
		}

		double length = start.distanceTo(end);

		for (double d = 0; d < length; d += 0.5) {
			Vec3 point = start.add(direction.scale(d));
			level.sendParticles(ParticleTypes.END_ROD, point.x, point.y, point.z, 1, 0.03, 0.03, 0.03, 0.0);

			if (((int) (d * 2)) % 3 == 0) {
				level.sendParticles(ParticleTypes.WITCH, point.x, point.y, point.z, 1, 0.1, 0.1, 0.1, 0.0);
			}
		}

		level.sendParticles(ParticleTypes.EXPLOSION, end.x, end.y, end.z, 1, 0, 0, 0, 0);
		playSound(ModSounds.ROBOT_LASER, 3.0f, 1.0f);
		float damage = isEnraged() ? 18.0f : 13.0f;
		Vec3 finalEnd = end;

		for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, new net.minecraft.world.phys.AABB(start, end).inflate(1.5),
				e -> e != this && !(e instanceof AmethystRobot) && e.isAlive())) {
			Vec3 center = target.position().add(0, target.getBbHeight() / 2.0, 0);
			Vec3 line = finalEnd.subtract(start);
			double t = Math.clamp(center.subtract(start).dot(line) / line.lengthSqr(), 0.0, 1.0);
			Vec3 closest = start.add(line.scale(t));

			if (closest.distanceTo(center) < target.getBbWidth() / 2.0 + 0.8) {
				target.hurtServer(level, damageSources().indirectMagic(this, this), scaled(damage));
				target.push(direction.x * 0.8, 0.3, direction.z * 0.8);
			}
		}
	}

	@Override
	protected net.minecraft.sounds.SoundEvent getAmbientSound() {
		return ModSounds.ROBOT_AMBIENT;
	}

	@Override
	protected net.minecraft.sounds.SoundEvent getHurtSound(DamageSource source) {
		return ModSounds.ROBOT_HURT;
	}

	@Override
	protected net.minecraft.sounds.SoundEvent getDeathSound() {
		return ModSounds.ROBOT_DEATH;
	}

	@Override
	protected void playStepSound(BlockPos pos, BlockState state) {
		playSound(ModSounds.ROBOT_STEP, 1.5f, 0.9f + random.nextFloat() * 0.2f);
	}

	@Override
	public boolean doHurtTarget(ServerLevel level, Entity target) {
		boolean hit = super.doHurtTarget(level, target);

		if (hit && isEnraged() && target instanceof LivingEntity living) {
			living.hurtServer(level, damageSources().mobAttack(this), scaled(6.0f));
		}

		return hit;
	}

	// ------------------------------------------------------------------
	// Beute
	// ------------------------------------------------------------------

	@Override
	protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
		super.dropCustomDeathLoot(level, source, recentlyHit);

		if (isHologram()) {
			return;
		}

		// 30 %: ein Kern – zufällig Amethyst-Kern (für Helfer-Rezepte) oder Funk-Kern
		if (random.nextFloat() < CORE_CHANCE) {
			boolean amethyst = random.nextBoolean();
			spawnAtLocation(level, new ItemStack(amethyst ? ModContent.AMETHYST_CORE : ModContent.WIRELESS_CORE));
			Component core = Component.translatable(amethyst ? "item.jtechstorage.amethyst_core" : "item.jtechstorage.wireless_core");

			for (ServerPlayer player : level.players()) {
				if (player.distanceToSqr(this) < 64 * 64) {
					player.sendSystemMessage(Component.translatable("jtechstorage.robot.core_drop", core).withStyle(ChatFormatting.LIGHT_PURPLE));
				}
			}
		}

		spawnAtLocation(level, new ItemStack(ModContent.IRON_RANGE_UPGRADE));
		spawnAtLocation(level, new ItemStack(ModContent.IRON_CAPACITY_UPGRADE));
		spawnAtLocation(level, new ItemStack(Items.AMETHYST_SHARD, 12 + random.nextInt(12)));
		level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, getX(), getY() + 2, getZ(), 60, 1.5, 1.5, 1.5, 0.4);
		level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, getX(), getY() + 2, getZ(), 1, 0, 0, 0, 0);

		for (ServerPlayer player : level.players()) {
			if (player.distanceToSqr(this) < 64 * 64) {
				player.sendSystemMessage(Component.translatable("jtechstorage.robot.defeated").withStyle(ChatFormatting.GOLD));
			}
		}
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putBoolean("Enraged", isEnraged());
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		entityData.set(DATA_ENRAGED, input.getBooleanOr("Enraged", false));

		if (hasCustomName()) {
			bossEvent.setName(getDisplayName());
		}
	}

	/** Zustandsautomat für alle Angriffe. */
	private class BossAttackGoal extends Goal {
		private int biteCd;
		private int tailCd = 60;
		private int roarCd = 80;
		private int stompCd = 120;
		private int chargeCd = 160;
		private int laserCd = 100;
		private Vec3 laserDir = Vec3.ZERO;
		private int busy;
		private int action = ANIM_NONE;
		private int charging;
		private Vec3 chargeDir = Vec3.ZERO;
		private boolean chargeHit;

		BossAttackGoal() {
			setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
		}

		@Override
		public boolean canUse() {
			LivingEntity target = getTarget();
			return target != null && target.isAlive();
		}

		@Override
		public boolean canContinueToUse() {
			return canUse() || busy > 0 || charging > 0;
		}

		@Override
		public boolean requiresUpdateEveryTick() {
			return true;
		}

		@Override
		public void stop() {
			getNavigation().stop();
			charging = 0;
			busy = 0;
		}

		private int cd(int base) {
			return (int) (base * (isEnraged() ? 0.6 : 1.0));
		}

		@Override
		public void tick() {
			if (!(level() instanceof ServerLevel level)) {
				return;
			}

			biteCd--;
			tailCd--;
			roarCd--;
			stompCd--;
			chargeCd--;
			laserCd--;
			LivingEntity target = getTarget();

			if (charging > 0) {
				tickCharge(level);
				return;
			}

			if (busy > 0) {
				busy--;
				getNavigation().stop();

				if (target != null && action != ANIM_SWEEP && action != ANIM_LASER) {
					getLookControl().setLookAt(target, 30.0f, 30.0f);
				}

				if (action == ANIM_LASER) {
					tickLaserCharge(level, target);
				} else if (action == ANIM_STOMP && busy == 10) {
					stomp(level);
				} else if (action == ANIM_SWEEP && busy == 7) {
					tailSwipe(level);
				} else if (action == ANIM_SLAM && busy == 5 && target != null && distanceTo(target) < reach(target) + 1.0) {
					doHurtTarget(level, target);
					playSound(ModSounds.ROBOT_SLAM, 2.0f, 1.1f);
				} else if (action == ANIM_DASH && busy == 0 && target != null) {
					Vec3 dir = target.position().subtract(position());
					chargeDir = new Vec3(dir.x, 0, dir.z).normalize();
					charging = 40;
					chargeHit = false;
					playSound(SoundEvents.FIRECHARGE_USE, 2.0f, 0.6f);
				}

				return;
			}

			if (target == null) {
				return;
			}

			getLookControl().setLookAt(target, 30.0f, 30.0f);
			double distance = distanceTo(target);

			if (roarCd <= 0 && distance < 20) {
				roar(level);
				act(ANIM_PULSE, 30);
				roarCd = cd(420);
			} else if (chargeCd <= 0 && distance > 8 && distance < 26 && hasLineOfSight(target)) {
				act(ANIM_DASH, 18);
				playSound(SoundEvents.PISTON_CONTRACT, 2.0f, 0.5f);
				level.sendParticles(ParticleTypes.FLAME, getX(), getY() + 2.5, getZ(), 25, 1.0, 0.5, 1.0, 0.02);
				chargeCd = cd(320);
			} else if (laserCd <= 0 && distance > 5 && distance < LASER_RANGE - 2 && hasLineOfSight(target)) {
				act(ANIM_LASER, LASER_CHARGE);
				laserDir = target.getEyePosition().subtract(position().add(0, 3.6, 0)).normalize();
				playSound(ModSounds.ROBOT_LASER_CHARGE, 3.0f, 1.0f);
				laserCd = cd(140);
			} else if (stompCd <= 0 && distance < 7) {
				act(ANIM_STOMP, 20);
				stompCd = cd(220);
			} else if (tailCd <= 0 && distance < 6 && isBesideOrBehind(target)) {
				act(ANIM_SWEEP, 15);
				tailCd = cd(100);
			} else if (biteCd <= 0 && distance < reach(target)) {
				act(ANIM_SLAM, 10);
				biteCd = cd(28);
			} else if (tickCount % 8 == 0 || getNavigation().isDone()) {
				getNavigation().moveTo(target, isEnraged() ? 1.35 : 1.1);
			}
		}

		private void act(int anim, int ticks) {
			action = anim;
			busy = ticks;
			playAnim(anim, ticks);
			getNavigation().stop();
		}

		private double reach(LivingEntity target) {
			return getBbWidth() / 2.0 + target.getBbWidth() / 2.0 + 2.8;
		}

		private boolean isBesideOrBehind(LivingEntity target) {
			Vec3 facing = Vec3.directionFromRotation(0, yBodyRot);
			Vec3 to = target.position().subtract(position()).normalize();
			return to.dot(facing) < 0.4;
		}

		/** Laser lädt auf: Kanone folgt dem Ziel langsam, Funken am Lauf, am Ende Schuss. */
		private void tickLaserCharge(ServerLevel level, @Nullable LivingEntity target) {
			if (target != null) {
				Vec3 wanted = target.getEyePosition().subtract(position().add(0, 3.6, 0)).normalize();
				laserDir = laserDir.lerp(wanted, isEnraged() ? 0.12 : 0.07).normalize();   // folgt träge – ausweichen geht
				getLookControl().setLookAt(target, 30.0f, 30.0f);
			}

			Vec3 muzzle = position().add(0, 3.6, 0).add(Vec3.directionFromRotation(0, yBodyRot - 90.0f).scale(1.6)).add(laserDir.scale(1.5));
			level.sendParticles(ParticleTypes.ELECTRIC_SPARK, muzzle.x, muzzle.y, muzzle.z, 3, 0.15, 0.15, 0.15, 0.05);

			if (busy % 4 == 0) {
				// Ziel-Linie andeuten (wird dichter, je näher der Schuss)
				for (double d = 2; d < LASER_RANGE; d += 3.0 - (LASER_CHARGE - busy) / 15.0) {
					Vec3 p = muzzle.add(laserDir.scale(d));
					level.sendParticles(ParticleTypes.WITCH, p.x, p.y, p.z, 1, 0, 0, 0, 0);
				}
			}

			if (busy == 0) {
				fireLaser(level, laserDir);
			}
		}

		private void tickCharge(ServerLevel level) {
			charging--;
			playAnim(ANIM_DASH, 2);
			double speed = isEnraged() ? 1.0 : 0.8;
			setDeltaMovement(chargeDir.x * speed, getDeltaMovement().y, chargeDir.z * speed);
			setYRot((float) (Mth.atan2(chargeDir.z, chargeDir.x) * Mth.RAD_TO_DEG) - 90.0f);
			yBodyRot = getYRot();

			if (tickCount % 2 == 0) {
				level.sendParticles(ParticleTypes.FLAME, getX() - chargeDir.x * 1.5, getY() + 2.5, getZ() - chargeDir.z * 1.5, 6, 0.4, 0.3, 0.4, 0.02);
				level.sendParticles(ParticleTypes.LARGE_SMOKE, getX() - chargeDir.x * 2, getY() + 2.5, getZ() - chargeDir.z * 2, 2, 0.3, 0.3, 0.3, 0.01);
				playSound(SoundEvents.FIRECHARGE_USE, 0.8f, 0.6f);
			}

			if (!chargeHit) {
				for (LivingEntity hit : level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(0.8),
						e -> e != AmethystRobot.this && !(e instanceof AmethystRobot) && e.isAlive())) {
					hit.hurtServer(level, damageSources().mobAttack(AmethystRobot.this), scaled(isEnraged() ? 24.0f : 18.0f));
					hit.push(chargeDir.x * 2.2, 0.7, chargeDir.z * 2.2);
					chargeHit = true;
					playSound(SoundEvents.ANVIL_LAND, 1.2f, 0.7f);
				}
			}

			if (horizontalCollision || chargeHit && charging > 10) {
				charging = Math.min(charging, 5);
			}
		}
	}
}
