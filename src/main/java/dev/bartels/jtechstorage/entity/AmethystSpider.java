package dev.bartels.jtechstorage.entity;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
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
import net.minecraft.world.entity.monster.spider.CaveSpider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

import dev.bartels.jtechstorage.ModConfig;
import dev.bartels.jtechstorage.ModSounds;
import dev.bartels.jtechstorage.end.EndContent;

/**
 * Große Amethyst-Spinne – Endboss im Kristallhort. 600 Leben, Obsidian-Panzer mit Kristallen.
 * Angriffe: Giftbiss, Sprungangriff mit Aufprallwelle, Kristallnetze (verlangsamen, kurz Spinnennetz),
 * Kristalldornen-Linie aus dem Boden, Resonanz-Schrei (Dunkelheit + Langsamkeit), ab 50 % Kristall-Spinnlinge.
 * Beute: genau ein Amethyst-Obsidian-Upgrade pro Sieg.
 */
public class AmethystSpider extends Monster {
	public static final int ANIM_NONE = 0;
	public static final int ANIM_BITE = 1;
	public static final int ANIM_LEAP = 2;
	public static final int ANIM_WEB = 3;
	public static final int ANIM_SPIKES = 4;
	public static final int ANIM_SHRIEK = 5;
	public static final int ANIM_BROOD = 6;
	/** Wie weit sich die Spinne von ihrem Altar entfernt, bevor sie umkehrt. */
	private static final double LEASH = 36.0;
	/** Sprünge landen höchstens so weit vom Altar entfernt (Arena-Radius des Horts ≈ 19). */
	private static final double ARENA_RADIUS = 14.0;

	private static final EntityDataAccessor<Integer> DATA_ANIM = SynchedEntityData.defineId(AmethystSpider.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> DATA_ANIM_TICKS = SynchedEntityData.defineId(AmethystSpider.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Boolean> DATA_ENRAGED = SynchedEntityData.defineId(AmethystSpider.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Boolean> DATA_LEAPING = SynchedEntityData.defineId(AmethystSpider.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Boolean> DATA_HOLOGRAM = SynchedEntityData.defineId(AmethystSpider.class, EntityDataSerializers.BOOLEAN);

	private final ServerBossEvent bossEvent = new ServerBossEvent(UUID.randomUUID(),
			Component.translatable("entity.jtechstorage.amethyst_spider").withStyle(ChatFormatting.LIGHT_PURPLE),
			BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.NOTCHED_12);

	/** Noch ausstehende Kristalldornen (Ort + Tick). */
	private final List<Spike> spikes = new ArrayList<>();
	/** Vorübergehende Spinnennetze (Ort + Ablauf-Tick). */
	private final List<Spike> webs = new ArrayList<>();
	private @Nullable BlockPos home;
	private int airTicks;

	private record Spike(BlockPos pos, long tick) {
	}

	public AmethystSpider(EntityType<? extends AmethystSpider> type, Level level) {
		super(type, level);
		this.xpReward = 250;
		setPersistenceRequired();
		bossEvent.setDarkenScreen(true);
	}

	/** Schaden der Spezialangriffe, skaliert mit {@code bosses.spider.damageMultiplier}. */
	private static float scaled(double amount) {
		return (float) (amount * ModConfig.get().bosses.spider.damageMultiplier);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
				.add(Attributes.MAX_HEALTH, ModConfig.get().bosses.spider.health)
				.add(Attributes.ATTACK_DAMAGE, 18.0 * ModConfig.get().bosses.spider.damageMultiplier)
				.add(Attributes.ATTACK_KNOCKBACK, 1.0)
				.add(Attributes.MOVEMENT_SPEED, 0.32)
				.add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
				.add(Attributes.ARMOR, ModConfig.get().bosses.spider.armor)
				.add(Attributes.ARMOR_TOUGHNESS, 6.0)
				.add(Attributes.STEP_HEIGHT, 2.0)
				.add(Attributes.FOLLOW_RANGE, 64.0);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DATA_ANIM, ANIM_NONE);
		builder.define(DATA_ANIM_TICKS, 0);
		builder.define(DATA_ENRAGED, false);
		builder.define(DATA_LEAPING, false);
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

	public boolean isLeaping() {
		return entityData.get(DATA_LEAPING);
	}

	public boolean isHologram() {
		return entityData.get(DATA_HOLOGRAM);
	}

	/** Für den Rundgang: kein Verhalten, keine Bossleiste, wird nie gespeichert. */
	public void makeHologram() {
		entityData.set(DATA_HOLOGRAM, true);
		setNoAi(true);
		setNoGravity(true);
		setPermanentlyInvulnerable(true);
		setSilent(true);
		bossEvent.setVisible(false);
	}

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

	@Override
	public boolean shouldBeSaved() {
		return !isHologram() && super.shouldBeSaved();
	}

	@Override
	public boolean isPushable() {
		return !isHologram();
	}

	public void setHome(BlockPos pos) {
		this.home = pos;
	}

	private void playAnim(int anim, int ticks) {
		entityData.set(DATA_ANIM, anim);
		entityData.set(DATA_ANIM_TICKS, ticks);
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(1, new BossAttackGoal());
		goalSelector.addGoal(2, new ReturnHomeGoal());
		goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.6));
		goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 20.0f));
		goalSelector.addGoal(7, new RandomLookAroundGoal(this));
		targetSelector.addGoal(1, new HurtByTargetGoal(this));
		targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
	}

	// ------------------------------------------------------------------
	// Tick
	// ------------------------------------------------------------------

	@Override
	public void aiStep() {
		super.aiStep();

		if (!(level() instanceof ServerLevel level)) {
			ambientEffects();
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

		if (home != null && getY() < home.getY() - 12) {
			// von der Insel gefallen: zurück auf den Altar
			level.sendParticles(ParticleTypes.REVERSE_PORTAL, getX(), getY(), getZ(), 60, 1.5, 1.5, 1.5, 0.1);
			teleportTo(home.getX() + 0.5, home.getY() + 6.0, home.getZ() + 0.5);
			setDeltaMovement(Vec3.ZERO);
			entityData.set(DATA_LEAPING, false);
			playSound(SoundEvents.ENDERMAN_TELEPORT, 2.0f, 0.5f);
		}

		tickSpikes(level);
		tickWebs(level, false);
		tickLeap(level);

		if (!isEnraged() && getHealth() < getMaxHealth() * 0.5f) {
			entityData.set(DATA_ENRAGED, true);
			getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.42);
			getAttribute(Attributes.ARMOR).setBaseValue(Math.min(30.0, ModConfig.get().bosses.spider.armor + 6.0));
			shriek(level);
			brood(level);
			level.sendParticles(ParticleTypes.REVERSE_PORTAL, getX(), getY() + 1.5, getZ(), 120, 2.5, 1.0, 2.5, 0.15);
			tell(level, Component.translatable("jtechstorage.spider.enraged").withStyle(ChatFormatting.DARK_PURPLE));
		}

		if (isEnraged() && tickCount % 8 == 0) {
			level.sendParticles(ParticleTypes.WITCH, getX(), getY() + 2.0, getZ(), 2, 1.5, 0.4, 1.5, 0.02);
		}
	}

	/** Client: Kristallglitzern auf dem Hinterleib, Portalstaub. */
	private void ambientEffects() {
		float yaw = yBodyRot * Mth.DEG_TO_RAD;
		double bx = Mth.sin(yaw) * 1.6;
		double bz = -Mth.cos(yaw) * 1.6;

		if (random.nextInt(3) == 0) {
			level().addParticle(ParticleTypes.END_ROD, getX() + bx + (random.nextDouble() - 0.5) * 1.8, getY() + 2.4 + random.nextDouble() * 0.6,
					getZ() + bz + (random.nextDouble() - 0.5) * 1.8, 0.0, 0.02, 0.0);
		}

		if (random.nextInt(5) == 0) {
			level().addParticle(ParticleTypes.PORTAL, getX() + (random.nextDouble() - 0.5) * 4, getY() + random.nextDouble() * 2,
					getZ() + (random.nextDouble() - 0.5) * 4, 0.0, -0.2, 0.0);
		}
	}

	private void tell(ServerLevel level, Component message) {
		for (ServerPlayer player : level.players()) {
			if (player.distanceToSqr(this) < 64 * 64) {
				player.sendSystemMessage(message);
			}
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

		if (level() instanceof ServerLevel level) {
			tickWebs(level, true);
		}

		super.remove(reason);
	}

	@Override
	public boolean removeWhenFarAway(double distance) {
		return false;
	}

	@Override
	protected void checkFallDamage(double distance, boolean onGround, BlockState state, BlockPos pos) {
		// landet nach jedem Sprung mit voller Wucht – Fallschaden gibt es nicht
	}

	@Override
	public void makeStuckInBlock(BlockState state, Vec3 speed) {
		if (!state.is(Blocks.COBWEB)) {
			super.makeStuckInBlock(state, speed);
		}
	}

	@Override
	public boolean canBeAffected(MobEffectInstance effect) {
		return !effect.is(MobEffects.POISON) && !effect.is(MobEffects.SLOWNESS) && super.canBeAffected(effect);
	}

	// ------------------------------------------------------------------
	// Angriffe
	// ------------------------------------------------------------------

	private boolean isFoe(Entity e) {
		return e != this && e.isAlive() && !(e instanceof AmethystSpider) && !(e instanceof CaveSpider)
				&& !(e instanceof Player player && (player.isCreative() || player.isSpectator()));
	}

	/** Resonanz-Schrei: Dunkelheit + Langsamkeit, schiebt leicht weg. */
	private void shriek(ServerLevel level) {
		playAnim(ANIM_SHRIEK, 30);
		playSound(ModSounds.SPIDER_SHRIEK, 4.0f, 1.0f);
		level.sendParticles(ParticleTypes.SONIC_BOOM, getX(), getY() + 1.5, getZ(), 1, 0, 0, 0, 0);

		for (int i = 0; i < 40; i++) {
			double angle = i / 40.0 * Math.PI * 2;
			level.sendParticles(ParticleTypes.END_ROD, getX() + Math.cos(angle) * 3, getY() + 1.0, getZ() + Math.sin(angle) * 3,
					1, Math.cos(angle) * 0.5, 0, Math.sin(angle) * 0.5, 0.35);
		}

		for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(16), this::isFoe)) {
			target.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 80, 0));
			target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 80, 1));
			Vec3 push = target.position().subtract(position()).normalize().scale(0.8);
			target.push(push.x, 0.2, push.z);
		}
	}

	/** Ruft bis zu drei Kristall-Spinnlinge (Höhlenspinnen), höchstens sechs gleichzeitig. */
	private void brood(ServerLevel level) {
		int alive = level.getEntitiesOfClass(CaveSpider.class, getBoundingBox().inflate(32)).size();

		for (int i = 0; i < Math.min(3, 6 - alive); i++) {
			CaveSpider spiderling = EntityTypes.CAVE_SPIDER.create(level, EntitySpawnReason.MOB_SUMMONED);

			if (spiderling == null) {
				continue;
			}

			double angle = random.nextDouble() * Math.PI * 2;
			spiderling.snapTo(getX() + Math.cos(angle) * 2.5, getY() + 0.5, getZ() + Math.sin(angle) * 2.5, random.nextFloat() * 360, 0);
			spiderling.setCustomName(Component.translatable("entity.jtechstorage.crystal_spiderling"));
			spiderling.setTarget(getTarget());
			level.addFreshEntity(spiderling);
			level.sendParticles(ParticleTypes.REVERSE_PORTAL, spiderling.getX(), spiderling.getY() + 0.3, spiderling.getZ(), 15, 0.3, 0.2, 0.3, 0.05);
		}

		playSound(SoundEvents.SPIDER_AMBIENT, 2.0f, 1.6f);
	}

	/** Kristallnetze: drei Netz-Kugeln im Fächer. */
	private void shootWebs(ServerLevel level, LivingEntity target) {
		Vec3 mouth = position().add(Vec3.directionFromRotation(0, yBodyRot).scale(2.0)).add(0, 1.2, 0);
		Vec3 aim = target.getEyePosition().subtract(mouth);
		int count = isEnraged() ? 5 : 3;

		for (int i = 0; i < count; i++) {
			float spread = (i - (count - 1) / 2.0f) * 9.0f;
			Vec3 dir = aim.yRot(spread * Mth.DEG_TO_RAD).normalize();
			WebShot shot = new WebShot(level, this);
			shot.setPos(mouth.x, mouth.y, mouth.z);
			shot.shoot(dir.x, dir.y + aim.horizontalDistance() * 0.012, dir.z, 1.4f, 2.0f);
			level.addFreshEntity(shot);
		}

		playSound(ModSounds.SPIDER_WEB, 2.0f, 0.9f + random.nextFloat() * 0.2f);
	}

	/** Spinnennetz für 5 Sekunden (nur in Luft). */
	public void placeTemporaryWeb(ServerLevel level, BlockPos pos) {
		if (level.getBlockState(pos).isAir()) {
			level.setBlockAndUpdate(pos, Blocks.COBWEB.defaultBlockState());
			webs.add(new Spike(pos.immutable(), level.getGameTime() + 100));
		}
	}

	private void tickWebs(ServerLevel level, boolean all) {
		Iterator<Spike> it = webs.iterator();

		while (it.hasNext()) {
			Spike web = it.next();

			if (all || level.getGameTime() >= web.tick()) {
				if (level.getBlockState(web.pos()).is(Blocks.COBWEB)) {
					level.removeBlock(web.pos(), false);
				}

				it.remove();
			}
		}
	}

	/** Kristalldornen brechen nacheinander in einer Linie zum Ziel aus dem Boden. */
	private void startSpikes(ServerLevel level, LivingEntity target) {
		Vec3 dir = target.position().subtract(position());
		dir = new Vec3(dir.x, 0, dir.z).normalize();
		long now = level.getGameTime();
		int count = isEnraged() ? 16 : 12;

		for (int k = 0; k < count; k++) {
			Vec3 p = position().add(dir.scale(2.5 + k * 1.4));
			spikes.add(new Spike(BlockPos.containing(p.x, getY(), p.z), now + k * 2L));
		}

		if (isEnraged()) {
			// zweite und dritte Linie schräg
			for (float side : new float[] {-25.0f, 25.0f}) {
				Vec3 d2 = dir.yRot(side * Mth.DEG_TO_RAD);

				for (int k = 0; k < 9; k++) {
					Vec3 p = position().add(d2.scale(2.5 + k * 1.4));
					spikes.add(new Spike(BlockPos.containing(p.x, getY(), p.z), now + 4 + k * 2L));
				}
			}
		}
	}

	private void tickSpikes(ServerLevel level) {
		long now = level.getGameTime();
		Iterator<Spike> it = spikes.iterator();
		BlockParticleOption crystal = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.AMETHYST_CLUSTER.defaultBlockState());
		float damage = isEnraged() ? 16.0f : 12.0f;

		while (it.hasNext()) {
			Spike spike = it.next();

			if (now < spike.tick()) {
				continue;
			}

			it.remove();
			double x = spike.pos().getX() + 0.5;
			double y = spike.pos().getY();
			double z = spike.pos().getZ() + 0.5;
			level.sendParticles(crystal, x, y + 0.5, z, 18, 0.3, 0.6, 0.3, 0.15);
			level.sendParticles(ParticleTypes.END_ROD, x, y + 0.3, z, 6, 0.15, 0.8, 0.15, 0.08);
			level.playSound(null, x, y, z, SoundEvents.AMETHYST_CLUSTER_BREAK, getSoundSource(), 1.4f, 0.6f + random.nextFloat() * 0.3f);
			level.playSound(null, x, y, z, SoundEvents.EVOKER_FANGS_ATTACK, getSoundSource(), 0.8f, 1.3f);

			for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class,
					new net.minecraft.world.phys.AABB(x - 1.3, y - 1, z - 1.3, x + 1.3, y + 2.5, z + 1.3), this::isFoe)) {
				target.hurtServer(level, damageSources().indirectMagic(this, this), scaled(damage));
				target.push(0, 0.6, 0);
			}
		}
	}

	private void startLeap(LivingEntity target) {
		Vec3 goal = target.position();

		if (home != null) {
			// nie über den Rand der schwebenden Insel hinaus springen
			Vec3 center = Vec3.atBottomCenterOf(home);
			Vec3 offset = new Vec3(goal.x - center.x, 0, goal.z - center.z);

			if (offset.length() > ARENA_RADIUS) {
				offset = offset.normalize().scale(ARENA_RADIUS);
				goal = new Vec3(center.x + offset.x, goal.y, center.z + offset.z);
			}
		}

		Vec3 dir = goal.subtract(position());
		double distance = dir.horizontalDistance();
		Vec3 flat = new Vec3(dir.x, 0, dir.z).normalize();
		double speed = Math.min(2.4, 0.35 + distance * 0.11);
		setDeltaMovement(flat.x * speed, 0.95, flat.z * speed);
		entityData.set(DATA_LEAPING, true);
		airTicks = 0;
		playSound(ModSounds.SPIDER_AMBIENT, 3.0f, 0.8f);
		playSound(SoundEvents.RAVAGER_ROAR, 0.8f, 1.5f);
	}

	private void tickLeap(ServerLevel level) {
		if (!isLeaping()) {
			return;
		}

		airTicks++;

		if (airTicks % 2 == 0) {
			level.sendParticles(ParticleTypes.REVERSE_PORTAL, getX(), getY() + 1, getZ(), 6, 1.2, 0.5, 1.2, 0.02);
		}

		if ((onGround() && airTicks > 4) || airTicks > 80) {
			entityData.set(DATA_LEAPING, false);
			land(level);
		}
	}

	/** Aufprall: Schockwelle mit Kristallsplittern. */
	private void land(ServerLevel level) {
		playSound(SoundEvents.GENERIC_EXPLODE.value(), 1.4f, 0.7f);
		playSound(SoundEvents.AMETHYST_BLOCK_BREAK, 3.0f, 0.5f);
		BlockParticleOption crystal = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.AMETHYST_BLOCK.defaultBlockState());

		for (int i = 0; i < 32; i++) {
			double angle = i / 32.0 * Math.PI * 2;
			level.sendParticles(crystal, getX() + Math.cos(angle) * 4.5, getY() + 0.2, getZ() + Math.sin(angle) * 4.5, 3, 0.3, 0.1, 0.3, 0.1);
		}

		level.sendParticles(ParticleTypes.EXPLOSION, getX(), getY() + 0.3, getZ(), 4, 2.0, 0.1, 2.0, 0);
		float base = isEnraged() ? 18.0f : 14.0f;

		for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(4.5, 2, 4.5), this::isFoe)) {
			double distance = target.distanceTo(this);
			target.hurtServer(level, damageSources().mobAttack(this), scaled((float) Math.max(5.0, base - distance * 1.5)));
			Vec3 push = target.position().subtract(position()).normalize().scale(1.1);
			target.push(push.x, 0.7, push.z);
		}
	}

	@Override
	public boolean doHurtTarget(ServerLevel level, Entity target) {
		boolean hit = super.doHurtTarget(level, target);

		if (hit && target instanceof LivingEntity living) {
			living.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 1));

			if (isEnraged()) {
				living.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, 0));
			}
		}

		return hit;
	}

	@Override
	protected @Nullable SoundEvent getAmbientSound() {
		return ModSounds.SPIDER_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return ModSounds.SPIDER_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return ModSounds.SPIDER_DEATH;
	}

	@Override
	public float getVoicePitch() {
		return 0.45f;
	}

	@Override
	protected float getSoundVolume() {
		return 2.5f;
	}

	@Override
	protected void playStepSound(BlockPos pos, BlockState state) {
		playSound(ModSounds.SPIDER_STEP, 1.2f, 0.9f + random.nextFloat() * 0.2f);
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
		// genau ein Upgrade pro Sieg
		spawnAtLocation(level, new ItemStack(EndContent.AMETHYST_OBSIDIAN_UPGRADE));
		spawnAtLocation(level, new ItemStack(EndContent.CRYSTAL_OBSIDIAN));
		spawnAtLocation(level, new ItemStack(Items.AMETHYST_SHARD, 16 + random.nextInt(16)));
		spawnAtLocation(level, new ItemStack(Items.STRING, 8 + random.nextInt(8)));
		level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, getX(), getY() + 1.5, getZ(), 80, 2.0, 1.0, 2.0, 0.4);
		level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, getX(), getY() + 1, getZ(), 1, 0, 0, 0, 0);
		tell(level, Component.translatable("jtechstorage.spider.defeated").withStyle(ChatFormatting.GOLD));
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putBoolean("Enraged", isEnraged());

		if (home != null) {
			output.putInt("HomeX", home.getX());
			output.putInt("HomeY", home.getY());
			output.putInt("HomeZ", home.getZ());
		}
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		entityData.set(DATA_ENRAGED, input.getBooleanOr("Enraged", false));
		int hy = input.getIntOr("HomeY", Integer.MIN_VALUE);

		if (hy != Integer.MIN_VALUE) {
			home = new BlockPos(input.getIntOr("HomeX", 0), hy, input.getIntOr("HomeZ", 0));
		}

		if (hasCustomName()) {
			bossEvent.setName(getDisplayName());
		}
	}

	/** Ohne Ziel und weit weg vom Altar: zurück in den Hort. */
	private class ReturnHomeGoal extends Goal {
		ReturnHomeGoal() {
			setFlags(EnumSet.of(Goal.Flag.MOVE));
		}

		@Override
		public boolean canUse() {
			return home != null && (getTarget() == null || home.distToCenterSqr(position()) > (LEASH + 12) * (LEASH + 12))
					&& home.distToCenterSqr(position()) > 12 * 12;
		}

		@Override
		public void tick() {
			if (home != null && (tickCount % 20 == 0 || getNavigation().isDone())) {
				getNavigation().moveTo(home.getX() + 0.5, home.getY() + 1, home.getZ() + 0.5, 1.0);
			}

			if (home != null && home.distToCenterSqr(position()) > (LEASH + 12) * (LEASH + 12)) {
				setTarget(null);
			}
		}
	}

	/** Zustandsautomat für alle Angriffe. */
	private class BossAttackGoal extends Goal {
		private int biteCd;
		private int leapCd = 60;
		private int webCd = 40;
		private int spikeCd = 100;
		private int shriekCd = 240;
		private int broodCd = 200;
		private int busy;
		private int action = ANIM_NONE;

		BossAttackGoal() {
			setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
		}

		@Override
		public boolean canUse() {
			LivingEntity target = getTarget();
			return target != null && target.isAlive() && (home == null || home.distToCenterSqr(target.position()) < (LEASH + 12) * (LEASH + 12));
		}

		@Override
		public boolean canContinueToUse() {
			return canUse() || busy > 0 || isLeaping();
		}

		@Override
		public boolean requiresUpdateEveryTick() {
			return true;
		}

		@Override
		public void stop() {
			getNavigation().stop();
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
			leapCd--;
			webCd--;
			spikeCd--;
			shriekCd--;
			broodCd--;
			LivingEntity target = getTarget();

			if (isLeaping()) {
				return;
			}

			if (busy > 0) {
				busy--;
				getNavigation().stop();

				if (target != null) {
					getLookControl().setLookAt(target, 30.0f, 30.0f);
				}

				if (target != null) {
					if (action == ANIM_BITE && busy == 4 && distanceTo(target) < reach(target) + 1.0) {
						doHurtTarget(level, target);
						playSound(SoundEvents.SPIDER_HURT, 2.0f, 0.4f);
					} else if (action == ANIM_LEAP && busy == 0) {
						startLeap(target);
					} else if (action == ANIM_WEB && busy == 8) {
						shootWebs(level, target);
					} else if (action == ANIM_SPIKES && busy == 10) {
						startSpikes(level, target);
					}
				}

				return;
			}

			if (target == null) {
				return;
			}

			getLookControl().setLookAt(target, 30.0f, 30.0f);
			double distance = distanceTo(target);
			boolean sight = hasLineOfSight(target);

			if (shriekCd <= 0 && distance < 14) {
				shriek(level);
				act(ANIM_SHRIEK, 30);
				shriekCd = cd(480);
			} else if (isEnraged() && broodCd <= 0) {
				brood(level);
				act(ANIM_BROOD, 25);
				broodCd = cd(500);
			} else if (leapCd <= 0 && distance > 7 && distance < 24 && sight) {
				act(ANIM_LEAP, 12);
				playSound(SoundEvents.SPIDER_STEP, 2.0f, 0.4f);
				leapCd = cd(200);
			} else if (spikeCd <= 0 && distance > 4 && distance < 20) {
				act(ANIM_SPIKES, 20);
				playSound(SoundEvents.AMETHYST_BLOCK_RESONATE, 3.0f, 0.4f);
				spikeCd = cd(180);
			} else if (webCd <= 0 && distance > 5 && distance < 28 && sight) {
				act(ANIM_WEB, 16);
				webCd = cd(130);
			} else if (biteCd <= 0 && distance < reach(target)) {
				act(ANIM_BITE, 10);
				biteCd = cd(24);
			} else if (tickCount % 8 == 0 || getNavigation().isDone()) {
				getNavigation().moveTo(target, isEnraged() ? 1.3 : 1.05);
			}
		}

		private void act(int anim, int ticks) {
			action = anim;
			busy = ticks;
			playAnim(anim, ticks);
			getNavigation().stop();
		}

		private double reach(LivingEntity target) {
			return getBbWidth() / 2.0 + target.getBbWidth() / 2.0 + 1.6;
		}
	}
}
