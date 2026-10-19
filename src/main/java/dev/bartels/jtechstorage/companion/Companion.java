package dev.bartels.jtechstorage.companion;

import java.util.EnumSet;

import org.jspecify.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

import dev.bartels.jtechstorage.ModAdvancements;
import dev.bartels.jtechstorage.ModConfig;
import dev.bartels.jtechstorage.ModSounds;

/**
 * Begleiter (v0.25): Mantis, Pilz oder Rabe – ein Geschenk des Geodenhüters. Gehört genau einem Spieler, folgt ihm
 * überallhin (auch durch Portale), sammelt mit ihm Erfahrung (die Hälfte jeder eingesammelten XP) und wächst bei Level 30
 * und 100 zur nächsten Stufe heran. Rechtsklick: Folgen/Bleiben, Shift + Rechtsklick oder Taste K: Begleiter-Menü (v0.26). Stirbt nicht – bei zu
 * viel Schaden zieht er sich erschöpft zurück und erholt sich.
 */
public abstract class Companion extends TamableAnimal {
	public static final int ATTACK_TICKS = 14;
	public static final int ACTION_TICKS = 20;
	private static final int EXHAUSTED_TICKS = 20 * 45;
	private static final EntityDataAccessor<Integer> DATA_XP = SynchedEntityData.defineId(Companion.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Boolean> DATA_EXHAUSTED = SynchedEntityData.defineId(Companion.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Integer> DATA_ATTACK = SynchedEntityData.defineId(Companion.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> DATA_ACTION = SynchedEntityData.defineId(Companion.class, EntityDataSerializers.INT);
	/** Sitzt auf der Schulter des Besitzers (v0.25.1: Pilz und Rabe auf Stufe 1). */
	private static final EntityDataAccessor<Boolean> DATA_SHOULDER = SynchedEntityData.defineId(Companion.class, EntityDataSerializers.BOOLEAN);

	private int exhausted;
	/** Resonanz-Leckerli (v0.25.3): doppelte Erfahrung, solange > 0 (Ticks). */
	private int xpBoost;
	public static final int TREAT_BOOST = 20 * 60 * 5;
	public static final int TREAT_BOOST_MAX = 20 * 60 * 15;
	private int lastStage = 1;
	/** Erst nach dem Konstruktor sind die synchronisierten Daten da (Hitbox wird schon vorher abgefragt). */
	private boolean ready;
	private int cachedXp = -1;
	private int cachedLevel;
	/** Talentränge (v0.26), Index siehe {@link CompanionTalents}. */
	private final int[] talents = new int[CompanionTalents.COUNT];
	/** Haltung (v0.26): aggressiv, defensiv, passiv. */
	public static final int STANCE_AGGRESSIVE = 0;
	public static final int STANCE_DEFENSIVE = 1;
	public static final int STANCE_PASSIVE = 2;
	private int stance = STANCE_AGGRESSIVE;
	/** Client: öffnet das Begleiter-Menü (vom Client gesetzt). */
	public static java.util.function.Consumer<Companion> openMenu = companion -> {
	};

	protected Companion(EntityType<? extends Companion> type, Level level) {
		super(type, level);
		setPersistenceRequired();
		ready = true;
	}

	/** Art des Begleiters (für Texte und die Begleiter-Liste): mantis, shroom, raven. */
	public abstract String species();

	/** Werte je Stufe (1–3): Leben, Schaden, Tempo, Rüstung. */
	protected abstract void applyStageAttributes(int stage);

	/** Hitbox-Faktor je Stufe. */
	protected abstract float stageScale(int stage);

	protected abstract SoundEvent hurtSound();

	/** Grundschaden der Mantis je Stufe – Rabe kämpft mit 50 %, Pilz mit 25 % davon (v0.25.2). */
	public static final double[] MANTIS_DAMAGE = {4, 7, 11};

	/** Anteil am Mantis-Schaden: Mantis 1.0, Rabe 0.5, Pilz 0.25. */
	protected abstract double combatShare();

	/** Schaden auf der aktuellen Stufe. */
	protected double stageDamage(int stage) {
		return MANTIS_DAMAGE[stage - 1] * combatShare();
	}

	protected abstract SoundEvent attackSound();

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DATA_XP, 0);
		builder.define(DATA_EXHAUSTED, false);
		builder.define(DATA_ATTACK, 0);
		builder.define(DATA_ACTION, 0);
		builder.define(DATA_SHOULDER, false);
	}

	// ------------------------------------------------------------------
	// Erfahrung und Stufen
	// ------------------------------------------------------------------

	public int getXp() {
		return entityData.get(DATA_XP);
	}

	/** Level nach der Vanilla-Kurve aus der gesammelten Erfahrung. */
	public int getLevel() {
		int xp = getXp();

		if (xp != cachedXp) {
			cachedXp = xp;
			cachedLevel = levelFor(xp);
		}

		return cachedLevel;
	}

	public int getStage() {
		return stageFor(getLevel());
	}

	public static int stageFor(int level) {
		ModConfig.Companions config = ModConfig.get().companions;
		return level >= config.stage3Level ? 3 : level >= config.stage2Level ? 2 : 1;
	}

	/** Gesamte Erfahrung, die man für ein Level braucht (wie beim Spieler). */
	public static int xpForLevel(int level) {
		if (level <= 16) {
			return level * level + 6 * level;
		}

		if (level <= 31) {
			return (int) (2.5 * level * level - 40.5 * level + 360);
		}

		return (int) Math.min(Integer.MAX_VALUE, 4.5 * level * level - 162.5 * level + 2220);
	}

	public static int levelFor(int xp) {
		// Schätzung über die Umkehrfunktion der oberen Kurve, dann exakt nachjustieren
		int level = xp > 1507 ? (int) ((162.5 + Math.sqrt(162.5 * 162.5 - 4 * 4.5 * (2220 - xp))) / 9.0) : 0;
		level = Math.max(0, Math.min(level, 100000));

		while (level > 0 && xpForLevel(level) > xp) {
			level--;
		}

		while (level < 100000 && xpForLevel(level + 1) <= xp) {
			level++;
		}

		return level;
	}

	public void addXp(int amount) {
		if (amount <= 0) {
			return;
		}

		int oldLevel = getLevel();
		entityData.set(DATA_XP, (int) Math.min(Integer.MAX_VALUE, (long) getXp() + amount));
		int newLevel = getLevel();

		if (newLevel > oldLevel && level() instanceof ServerLevel server) {
			server.sendParticles(ParticleTypes.HAPPY_VILLAGER, getX(), getY() + getBbHeight(), getZ(), 6, 0.3, 0.2, 0.3, 0.0);
			playSound(ModSounds.COMPANION_LEVEL, 0.6f, 1.0f + Math.min(0.6f, newLevel * 0.005f));

			if (getOwner() instanceof ServerPlayer owner) {
				owner.sendOverlayMessage(Component.translatable("jtechstorage.companion.level", getDisplayName(), newLevel)
						.withStyle(ChatFormatting.LIGHT_PURPLE));
			}
		}

		checkStage();
	}

	/** Stufe hat sich geändert: Werte, Hitbox und eine kleine Feier. */
	private void checkStage() {
		int stage = getStage();

		if (stage == lastStage) {
			return;
		}

		boolean grew = stage > lastStage;
		lastStage = stage;
		applyStageAttributes(stage);
		setBase(this, Attributes.ATTACK_DAMAGE, stageDamage(stage));
		applyTalents();
		refreshDimensions();

		if (grew && level() instanceof ServerLevel server) {
			setHealth(getMaxHealth());
			server.sendParticles(ParticleTypes.END_ROD, getX(), getY() + getBbHeight() * 0.5, getZ(), 40, 0.6, 0.8, 0.6, 0.05);
			server.sendParticles(ParticleTypes.WITCH, getX(), getY() + 0.3, getZ(), 30, 0.8, 0.2, 0.8, 0.02);
			playSound(ModSounds.COMPANION_EVOLVE, 1.5f, 1.0f);

			if (getOwner() instanceof ServerPlayer owner) {
				owner.sendSystemMessage(Component.translatable("jtechstorage.companion.evolved", getDisplayName(),
						Component.translatable("jtechstorage.companion.stage." + stage)).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
				ModAdvancements.grant(owner, stage == 3 ? "companion_stage3" : "companion_stage2");
			}
		}
	}

	@Override
	public void onSyncedDataUpdated(EntityDataAccessor<?> accessor) {
		super.onSyncedDataUpdated(accessor);

		if (ready && DATA_XP.equals(accessor)) {
			if (level().isClientSide()) {
				lastStage = getStage();     // der Server merkt Stufenwechsel selbst in checkStage()
			}

			refreshDimensions();
		}
	}

	@Override
	protected EntityDimensions getDefaultDimensions(Pose pose) {
		return ready ? super.getDefaultDimensions(pose).scale(stageScale(getStage())) : super.getDefaultDimensions(pose);
	}

	/** Setzt Leben und Werte passend zur aktuellen Stufe (nach dem Laden oder Erzeugen). */
	public void refreshStage() {
		lastStage = getStage();
		applyStageAttributes(lastStage);
		setBase(this, Attributes.ATTACK_DAMAGE, stageDamage(lastStage));
		applyTalents();
		refreshDimensions();
	}

	protected static void setBase(Companion mob, net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute, double value) {
		AttributeInstance instance = mob.getAttribute(attribute);

		if (instance != null) {
			instance.setBaseValue(value);
		}
	}

	protected void setMaxHealthKeepRatio(double max) {
		float ratio = getMaxHealth() > 0 ? getHealth() / getMaxHealth() : 1.0f;
		setBase(this, Attributes.MAX_HEALTH, max);
		setHealth(Math.max(1.0f, (float) (max * ratio)));
	}

	// ------------------------------------------------------------------
	// Animationen
	// ------------------------------------------------------------------

	public int getAttackTicks() {
		return entityData.get(DATA_ATTACK);
	}

	public int getActionTicks() {
		return entityData.get(DATA_ACTION);
	}

	protected void triggerAttack() {
		entityData.set(DATA_ATTACK, ATTACK_TICKS);
	}

	protected void triggerAction() {
		entityData.set(DATA_ACTION, ACTION_TICKS);
	}

	public boolean isExhausted() {
		return entityData.get(DATA_EXHAUSTED);
	}

	/** Folgt er gerade (nicht auf „Bleiben“, nicht erschöpft)? */
	public boolean isActive() {
		return !isOrderedToSit() && !isExhausted() && getOwner() != null;
	}

	// ------------------------------------------------------------------
	// Ablauf
	// ------------------------------------------------------------------

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(1, new FloatGoal(this));
		goalSelector.addGoal(2, new SitWhenOrderedToGoal(this) {
			@Override
			public boolean canUse() {
				return !isGuarding() && super.canUse();     // Wache in der Basis: bleibt nicht sitzen
			}

			@Override
			public boolean canContinueToUse() {
				return !isGuarding() && super.canContinueToUse();
			}
		});
		goalSelector.addGoal(3, new ExhaustedGoal());
		registerCompanionGoals();
		// Kämpfen können alle Begleiter (Mantis am stärksten, Rabe halb so stark, Pilz ein Viertel)
		goalSelector.addGoal(5, new CompanionMeleeGoal());
		// Haltung (v0.26): aggressiv = alles, defensiv = nur verteidigen, passiv = gar nicht
		targetSelector.addGoal(1, new net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal(this) {
			@Override
			public boolean canUse() {
				return stance != STANCE_PASSIVE && super.canUse();
			}
		});
		targetSelector.addGoal(2, new net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal(this) {
			@Override
			public boolean canUse() {
				return stance == STANCE_AGGRESSIVE && super.canUse();
			}
		});
		targetSelector.addGoal(3, new net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal(this, Player.class) {
			@Override
			public boolean canUse() {
				return stance != STANCE_PASSIVE && super.canUse();
			}
		}.setAlertOthers());
		targetSelector.addGoal(4, new net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal<>(this, LivingEntity.class, 10, true, false,
				(target, level) -> target instanceof net.minecraft.world.entity.monster.Enemy && !(target instanceof net.minecraft.world.entity.monster.Creeper)
						&& (isActive() && nearOwner(target) || isGuarding() && nearBase(target))) {
			@Override
			public boolean canUse() {
				return (stance == STANCE_AGGRESSIVE || isGuarding() && stance != STANCE_PASSIVE) && canFight() && super.canUse();
			}

			@Override
			protected net.minecraft.world.phys.AABB getTargetSearchArea(double distance) {
				return super.getTargetSearchArea(Math.max(distance, huntRadius()));
			}
		});
		goalSelector.addGoal(6, new GuardPatrolGoal());
		goalSelector.addGoal(7, new FollowOwnerGoal(this, 1.25, 6.0f, 2.5f) {
			@Override
			public boolean canUse() {
				return !isOnShoulder() && super.canUse();
			}

			@Override
			public boolean canContinueToUse() {
				return !isOnShoulder() && super.canContinueToUse();
			}
		});
		goalSelector.addGoal(9, new LookAtPlayerGoal(this, Player.class, 8.0f));
		goalSelector.addGoal(10, new RandomLookAroundGoal(this));
	}

	private int attackCooldown;

	/** Ticks zwischen zwei Angriffen (realistischeres Tempo, v0.25.4). */
	protected int attackInterval() {
		return 30;
	}

	/** Nahkampf mit eigener Abklingzeit; nicht, solange jemand auf ihm reitet. */
	private class CompanionMeleeGoal extends net.minecraft.world.entity.ai.goal.MeleeAttackGoal {
		CompanionMeleeGoal() {
			super(Companion.this, 1.25, true);
		}

		@Override
		public boolean canUse() {
			return !isVehicle() && canFight() && super.canUse();
		}

		@Override
		public boolean canContinueToUse() {
			return !isVehicle() && super.canContinueToUse();
		}

		@Override
		protected boolean canPerformAttack(LivingEntity target) {
			return attackCooldown <= 0 && super.canPerformAttack(target);
		}
	}

	private boolean nearOwner(LivingEntity target) {
		LivingEntity owner = getOwner();
		double r = huntRadius();
		return owner != null && owner.level() == level() && target.distanceToSqr(owner) < r * r;
	}

	/** Abstand zum Besitzer, in dem er Monster von sich aus angreift (Talent Jagdinstinkt). */
	protected double huntRadius() {
		return 16.0;
	}

	/** Tatsächliche Ticks zwischen zwei Angriffen (mit Talenten). */
	public int effectiveAttackInterval() {
		return Math.max(8, Math.round(attackInterval() * (float) intervalFactor()));
	}

	private static final net.minecraft.resources.Identifier CRIT_ID = dev.bartels.jtechstorage.JTechStorage.id("companion_crit");

	@Override
	public boolean doHurtTarget(ServerLevel level, Entity target) {
		boolean crit = critChance() > 0 && random.nextDouble() < critChance();
		AttributeInstance damage = getAttribute(Attributes.ATTACK_DAMAGE);
		double bonus = (crit ? 1.0 : 0.0) + (target instanceof LivingEntity living ? bonusDamageAgainst(living) : 0.0);

		if (bonus > 0 && damage != null) {
			damage.addOrUpdateTransientModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(CRIT_ID, bonus,
					net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
		}

		boolean hit;

		try {
			hit = super.doHurtTarget(level, target);
		} finally {
			if (damage != null) {
				damage.removeModifier(CRIT_ID);
			}
		}

		attackCooldown = effectiveAttackInterval();
		triggerAttack();
		playSound(attackSound(), 0.8f, 0.9f + random.nextFloat() * 0.3f);

		if (hit) {
			level.sendParticles(crit ? ParticleTypes.ENCHANTED_HIT : ParticleTypes.CRIT, target.getX(), target.getY() + target.getBbHeight() * 0.6,
					target.getZ(), crit ? 12 : 4, 0.2, 0.2, 0.2, 0.1);

			if (target instanceof LivingEntity living) {
				onTalentHit(level, living);

				if (!living.isAlive()) {
					onKill(level, living);
				}
			}
		}

		return hit;
	}

	// ------------------------------------------------------------------
	// Basis, Wache, Schulter (v0.25.1)
	// ------------------------------------------------------------------

	/** Eigenes Terminal des Besitzers in den 3×3 Chunks um diese Position (oder null). */
	public static net.minecraft.core.@Nullable BlockPos ownTerminalAround(ServerLevel level, net.minecraft.core.BlockPos pos, java.util.@Nullable UUID owner) {
		if (owner == null) {
			return null;
		}

		int cx = pos.getX() >> 4;
		int cz = pos.getZ() >> 4;
		net.minecraft.core.BlockPos best = null;
		double bestDist = Double.MAX_VALUE;

		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				if (!(level.getChunkSource().getChunkNow(cx + dx, cz + dz) instanceof net.minecraft.world.level.chunk.LevelChunk chunk)) {
					continue;
				}

				for (net.minecraft.world.level.block.entity.BlockEntity entity : chunk.getBlockEntities().values()) {
					if (entity instanceof dev.bartels.jtechstorage.block.StorageTerminalBlockEntity terminal && owner.equals(terminal.getOwnerId())) {
						double dist = entity.getBlockPos().distSqr(pos);

						if (dist < bestDist) {
							bestDist = dist;
							best = entity.getBlockPos();
						}
					}
				}
			}
		}

		return best;
	}

	private net.minecraft.core.@Nullable BlockPos baseTerminal;
	private static final net.minecraft.resources.Identifier BASE_ID = dev.bartels.jtechstorage.JTechStorage.id("companion_base");

	/** Terminal der Basis, in der er gerade ist (3×3 Chunks um ein eigenes Terminal des Besitzers), oder null. */
	public net.minecraft.core.@Nullable BlockPos baseTerminal() {
		return baseTerminal;
	}

	public boolean isInBase() {
		return baseTerminal != null;
	}

	private void updateBase() {
		if (!(level() instanceof ServerLevel level)) {
			return;
		}

		net.minecraft.core.BlockPos found = ownTerminalAround(level, blockPosition(), getOwnerReference() != null ? getOwnerReference().getUUID() : null);

		if (java.util.Objects.equals(found, baseTerminal)) {
			return;
		}

		baseTerminal = found;
		AttributeInstance damage = getAttribute(Attributes.ATTACK_DAMAGE);

		if (damage != null) {
			if (found != null && baseDamageBonus() > 0) {
				damage.addOrUpdateTransientModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(BASE_ID, baseDamageBonus(),
						net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
			} else {
				damage.removeModifier(BASE_ID);
			}
		}
	}

	/** Schadensbonus in der eigenen Basis (Mantis: +50 %). */
	protected double baseDamageBonus() {
		return 0;
	}

	/** Kann er die Basis bewachen, wenn er dort „Bleiben“ soll (Mantis)? */
	protected boolean canGuardBase() {
		return false;
	}

	/** Wache: soll in der Basis bleiben und verteidigt sie – auch ohne den Besitzer. */
	public boolean isGuarding() {
		return canGuardBase() && isOrderedToSit() && baseTerminal != null && !isExhausted() && !isVehicle();
	}

	private boolean nearBase(LivingEntity target) {
		return baseTerminal != null && target.blockPosition().distSqr(baseTerminal) < 24 * 24;
	}

	/** Darf er überhaupt kämpfen? (Pilz erst ab Stufe 2) */
	protected boolean canFight() {
		return true;
	}

	/** Wache: läuft zwischen den Kämpfen um das Terminal herum. */
	private class GuardPatrolGoal extends Goal {
		GuardPatrolGoal() {
			setFlags(EnumSet.of(Flag.MOVE));
		}

		@Override
		public boolean canUse() {
			return isGuarding() && getTarget() == null && random.nextInt(40) == 0;
		}

		@Override
		public boolean canContinueToUse() {
			return isGuarding() && getTarget() == null && !getNavigation().isDone();
		}

		@Override
		public void start() {
			net.minecraft.core.BlockPos center = baseTerminal;

			if (center == null) {
				return;
			}

			double angle = random.nextDouble() * Math.PI * 2;
			double r = 3 + random.nextDouble() * 9;
			getNavigation().moveTo(center.getX() + 0.5 + Math.cos(angle) * r, center.getY(), center.getZ() + 0.5 + Math.sin(angle) * r, 0.8);
		}
	}

	public boolean isOnShoulder() {
		return entityData.get(DATA_SHOULDER);
	}

	/** Sitzt diese Art auf der aktuellen Stufe auf der Schulter (Pilz und Rabe auf Stufe 1)? */
	protected boolean shoulderStage() {
		return false;
	}

	/** Möchte er gerade auf die Schulter? (Rabe: nicht im Kampf und nicht unterwegs) */
	protected boolean wantsShoulder(Player owner) {
		return shoulderStage() && !isOrderedToSit() && !isVehicle() && getTarget() == null && owner.level() == level() && owner.isAlive()
				&& !owner.isSpectator() && !owner.isSleeping();
	}

	/** Rechte (+1) oder linke (-1) Schulter. */
	public int shoulderSide() {
		return 1;
	}

	/** Position auf der Schulter (Füße des Begleiters). */
	public static Vec3 shoulderPos(Entity owner, Vec3 ownerPos, float bodyYaw, int side) {
		float yaw = bodyYaw * net.minecraft.util.Mth.DEG_TO_RAD;
		double rx = -net.minecraft.util.Mth.cos(yaw) * side;
		double rz = -net.minecraft.util.Mth.sin(yaw) * side;
		double bx = net.minecraft.util.Mth.sin(yaw) * 0.06;         // ein wenig nach hinten
		double bz = -net.minecraft.util.Mth.cos(yaw) * 0.06;
		return ownerPos.add(rx * 0.38 + bx, owner.getBbHeight() - 0.38, rz * 0.38 + bz);
	}

	private void tickShoulder() {
		LivingEntity owner = getOwner();
		boolean want = owner instanceof Player player && wantsShoulder(player);

		if (want && !isOnShoulder()) {
			// erst hinlaufen bzw. hinfliegen, dann aufspringen
			if (distanceToSqr(owner) > 2.5 * 2.5) {
				if (tickCount % 10 == 0) {
					getNavigation().moveTo(owner, 1.4);
				}

				return;
			}

			entityData.set(DATA_SHOULDER, true);
			playSound(dev.bartels.jtechstorage.ModSounds.COMPANION_COMMAND, 0.5f, 1.5f);
		} else if (!want && isOnShoulder()) {
			entityData.set(DATA_SHOULDER, false);
			noPhysics = false;

			if (owner != null && owner.level() == level()) {
				Vec3 side = shoulderPos(owner, owner.position(), owner.yBodyRot, shoulderSide()).subtract(owner.position());
				snapTo(owner.getX() + side.x * 2.0, owner.getY(), owner.getZ() + side.z * 2.0, getYRot(), 0.0f);
			}

			return;
		}

		if (isOnShoulder() && owner != null) {
			Vec3 pos = shoulderPos(owner, owner.position(), owner.yBodyRot, shoulderSide());
			noPhysics = true;     // Schwerkraft wirkt nicht: die Position wird jeden Tick neu gesetzt
			setDeltaMovement(Vec3.ZERO);
			setPos(pos.x, pos.y, pos.z);
			setYRot(owner.yBodyRot);
			yBodyRot = owner.yBodyRot;
			fallDistance = 0;
			getNavigation().stop();
		}
	}

	@Override
	public boolean isPushable() {
		return !isOnShoulder() && super.isPushable();
	}

	// ------------------------------------------------------------------
	// Spezialangriffe (v0.25.3): einer je Begleiter, per Talent gewählt, Taste (Standard B)
	// ------------------------------------------------------------------

	private int specialCooldown;

	/** Gewählter Spezialangriff (0–2) oder -1. */
	public int specialIndex() {
		return CompanionTalents.chosenSpecial(talents);
	}

	public int specialCooldown() {
		return specialCooldown;
	}

	/** Grund-Abklingzeit eines Spezialangriffs in Ticks. */
	protected int specialBaseCooldown(int which) {
		return 20 * 60;
	}

	/** Abklingzeit mit Meisterschaft (−20 % je Rang). */
	public int specialCooldownMax() {
		int which = specialIndex();
		return which < 0 ? 0 : (int) (specialBaseCooldown(which) * (1.0 - 0.2 * talent("mastery")));
	}

	/** Stärke des Spezialangriffs (Meisterschaft: +25 % je Rang). */
	protected float specialPower() {
		return 1.0f + 0.25f * talent("mastery");
	}

	/** Braucht dieser Spezialangriff ein Ziel? */
	protected boolean specialNeedsTarget(int which) {
		return true;
	}

	/** Führt den Spezialangriff aus; false = ging nicht (dann keine Abklingzeit). */
	protected boolean performSpecial(ServerLevel level, int which, @Nullable LivingEntity target) {
		return false;
	}

	/** Kann er gerade überhaupt einen Spezialangriff ausführen (Rabe: nicht unterwegs)? */
	protected boolean canUseSpecialNow() {
		return true;
	}

	/** Spezialangriff auf Tastendruck (oder automatisch mit „Instinkt“). */
	public void useSpecial(ServerPlayer player, boolean automatic) {
		if (!(level() instanceof ServerLevel level)) {
			return;
		}

		int which = specialIndex();
		String problem = which < 0 ? "jtechstorage.special.none" : isExhausted() ? "jtechstorage.special.exhausted"
				: !canFight() || !canUseSpecialNow() ? "jtechstorage.special.busy"
				: player.level() != level || player.distanceToSqr(this) > 48 * 48 ? "jtechstorage.special.too_far"
				: specialCooldown > 0 ? "jtechstorage.special.cooldown" : null;

		if (problem != null) {
			if (!automatic) {
				player.sendOverlayMessage(Component.translatable(problem, getDisplayName(), (specialCooldown + 19) / 20).withStyle(ChatFormatting.GRAY));
			}

			return;
		}

		LivingEntity target = specialTarget(player);

		if (target == null && specialNeedsTarget(which)) {
			if (!automatic) {
				player.sendOverlayMessage(Component.translatable("jtechstorage.special.no_target").withStyle(ChatFormatting.GRAY));
			}

			return;
		}

		if (!performSpecial(level, which, target)) {
			return;
		}

		specialCooldown = specialCooldownMax();
		triggerAttack();
		String id = CompanionTalents.tree(species()).get(CompanionTalents.SPECIAL + which).id();
		player.sendOverlayMessage(Component.translatable(automatic ? "jtechstorage.special.used_auto" : "jtechstorage.special.used", getDisplayName(),
				Component.translatable("jtechstorage.talent." + id)).withStyle(ChatFormatting.LIGHT_PURPLE));
	}

	/** Ziel: worauf der Besitzer schaut, sonst das Ziel des Begleiters, sonst wen der Besitzer zuletzt angegriffen hat, sonst das nächste Monster. */
	protected @Nullable LivingEntity specialTarget(Player owner) {
		Vec3 eye = owner.getEyePosition();
		Vec3 look = owner.getLookAngle();
		LivingEntity best = null;
		double bestScore = 0.96;

		for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, owner.getBoundingBox().inflate(24.0), this::validSpecialTarget)) {
			Vec3 to = e.getBoundingBox().getCenter().subtract(eye);
			double dot = to.normalize().dot(look);

			if (dot > bestScore && owner.hasLineOfSight(e)) {
				bestScore = dot;
				best = e;
			}
		}

		if (best != null) {
			return best;
		}

		if (getTarget() != null && validSpecialTarget(getTarget()) && hasLineOfSight(getTarget())) {
			return getTarget();
		}

		if (owner.getLastHurtMob() != null && validSpecialTarget(owner.getLastHurtMob()) && owner.getLastHurtMob().distanceToSqr(owner) < 24 * 24
				&& hasLineOfSight(owner.getLastHurtMob())) {
			return owner.getLastHurtMob();
		}

		LivingEntity nearest = null;
		double nearestDist = Double.MAX_VALUE;

		for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(16.0), e -> validSpecialTarget(e) && hasLineOfSight(e))) {
			double d = e.distanceToSqr(this);

			if (d < nearestDist) {
				nearestDist = d;
				nearest = e;
			}
		}

		return nearest;
	}

	private boolean validSpecialTarget(LivingEntity e) {
		return e.isAlive() && e != this && e != getOwner() && !(e instanceof Player) && !(e instanceof Companion)
				&& (e instanceof net.minecraft.world.entity.monster.Enemy || e instanceof net.minecraft.world.entity.Mob mob && mob.getTarget() == getOwner())
				&& !(e instanceof TamableAnimal pet && getOwner() != null && pet.isOwnedBy(getOwner()));
	}

	/** Abklingzeit, „Instinkt“ (automatisch bei Gefahr) und Nachwirkungen der Art. */
	private void tickSpecial(ServerLevel level) {
		if (specialCooldown > 0) {
			specialCooldown--;
		}

		tickSpecialEffects(level);

		if (specialCooldown == 0 && tickCount % 20 == 0 && talent("instinct") > 0 && specialIndex() >= 0 && !isExhausted()
				&& getOwner() instanceof ServerPlayer owner && owner.level() == level && owner.distanceToSqr(this) < 24 * 24) {
			boolean lowHealth = owner.getHealth() < owner.getMaxHealth() * 0.3f;
			int enemies = level.getEntitiesOfClass(LivingEntity.class, owner.getBoundingBox().inflate(6.0),
					e -> e instanceof net.minecraft.world.entity.monster.Enemy && e.isAlive()).size();

			if (lowHealth && enemies > 0 || enemies >= 3) {
				useSpecial(owner, true);
			}
		}
	}

	/** Springt bzw. stürzt auf {@code at} – nur, wenn dort Platz ist; false = kein Platz (dann bleibt er stehen). */
	protected boolean leapTo(ServerLevel level, Vec3 at) {
		if (!level.noCollision(this, getBoundingBox().move(at.subtract(position())))) {
			return false;
		}

		snapTo(at.x, at.y, at.z, getYRot(), getXRot());
		return true;
	}

	/** Laufende Spezialangriffe der Art (z. B. Klingensturm über mehrere Ticks). */
	protected void tickSpecialEffects(ServerLevel level) {
	}

	/** Spezialangriff zurücksetzen, wenn die Talente zurückgesetzt werden. */
	public void clearSpecialCooldown() {
		specialCooldown = 0;
	}

	// ------------------------------------------------------------------
	// Betäubung (Mantis: Lähmender Stich): kann sich nicht bewegen und nicht angreifen
	// ------------------------------------------------------------------

	private static final java.util.Map<LivingEntity, Integer> STUNNED = new java.util.WeakHashMap<>();

	public static void stun(ServerLevel level, LivingEntity target, int ticks) {
		STUNNED.merge(target, ticks, Math::max);
		target.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.SLOWNESS, ticks, 9, false, false));
		target.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.WEAKNESS, ticks, 9, false, false));
		target.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.MINING_FATIGUE, ticks, 4, false, false));
	}

	public static boolean isStunned(LivingEntity entity) {
		return STUNNED.containsKey(entity);
	}

	/** Jeden Server-Tick: Betäubte halten still. */
	public static void tickStunned() {
		var it = STUNNED.entrySet().iterator();

		while (it.hasNext()) {
			var entry = it.next();
			LivingEntity e = entry.getKey();
			int left = entry.getValue() - 1;

			if (left <= 0 || !e.isAlive() || e.isRemoved()) {
				it.remove();
				continue;
			}

			entry.setValue(left);
			e.setDeltaMovement(0, Math.min(0, e.getDeltaMovement().y), 0);

			if (e instanceof net.minecraft.world.entity.Mob mob) {
				mob.getNavigation().stop();
				mob.setTarget(null);
			}

			if (left % 8 == 0 && e.level() instanceof ServerLevel level) {
				level.sendParticles(ParticleTypes.CRIT, e.getX(), e.getY() + e.getBbHeight() + 0.3, e.getZ(), 4, 0.25, 0.05, 0.25, 0.0);
				level.sendParticles(ParticleTypes.WAX_OFF, e.getX(), e.getY() + e.getBbHeight() + 0.2, e.getZ(), 1, 0.2, 0.05, 0.2, 0.0);
			}
		}
	}

	// ------------------------------------------------------------------
	// Talente (v0.26): Werte, die die Arten aus ihren Rängen berechnen
	// ------------------------------------------------------------------

	/** Rang eines Talents dieser Art (0, wenn es das Talent nicht gibt). */
	public int talent(String id) {
		int index = CompanionTalents.index(species(), id);
		return index < 0 ? 0 : talents[index];
	}

	public int[] talentRanks() {
		return talents.clone();
	}

	/** Zusätzlicher Schaden in Prozent der Grundwerte (0.1 = +10 %). */
	protected double damageBonus() {
		return 0;
	}

	protected double armorBonus() {
		return 0;
	}

	protected double healthBonus() {
		return 0;
	}

	protected double speedBonus() {
		return 0;
	}

	/** Faktor für die Zeit zwischen zwei Angriffen. */
	protected double intervalFactor() {
		return 1.0;
	}

	/** Chance auf doppelten Schaden. */
	protected double critChance() {
		return 0;
	}

	/** Anteil des Nahkampfschadens, der auf den Angreifer zurückgeht. */
	protected double thornsFraction() {
		return 0;
	}

	/** Treffer gelandet: Gift, Verlangsamung usw. */
	protected void onTalentHit(ServerLevel level, LivingEntity target) {
	}

	/** Zusätzlicher Schaden gegen dieses Ziel (Anteil, 0.3 = +30 %). */
	protected double bonusDamageAgainst(LivingEntity target) {
		return 0;
	}

	/** Ein Ziel wurde besiegt. */
	protected void onKill(ServerLevel level, LivingEntity target) {
	}

	/** Treffer komplett abwehren (Mantis: Kristallhaut)? */
	protected boolean blockDamage(ServerLevel level, DamageSource source) {
		return false;
	}

	/** Darf er der Erschöpfung gerade einmal entgehen (Mantis: Unbeugsam)? */
	protected boolean resistExhaustion(ServerLevel level) {
		return false;
	}

	private static final net.minecraft.resources.Identifier TALENT_ID = dev.bartels.jtechstorage.JTechStorage.id("companion_talents");

	/** Wendet die Talentwerte als Attribut-Modifikatoren an (nach dem Laden, Lernen und bei jeder Stufe). */
	protected void applyTalents() {
		float ratio = getMaxHealth() > 0 ? getHealth() / getMaxHealth() : 1.0f;
		talentModifier(Attributes.ATTACK_DAMAGE, damageBonus(), net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
		talentModifier(Attributes.ARMOR, armorBonus(), net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE);
		talentModifier(Attributes.MAX_HEALTH, healthBonus(), net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
		talentModifier(Attributes.MOVEMENT_SPEED, speedBonus(), net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
		setHealth(Math.max(1.0f, Math.min(getMaxHealth(), getMaxHealth() * ratio)));
	}

	private void talentModifier(net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute, double amount,
			net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation operation) {
		AttributeInstance instance = getAttribute(attribute);

		if (instance == null) {
			return;
		}

		if (amount == 0) {
			instance.removeModifier(TALENT_ID);
		} else {
			instance.addOrUpdateTransientModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(TALENT_ID, amount, operation));
		}
	}

	/** Talentpunkt ausgeben (aus dem Menü). */
	public void learnTalent(ServerPlayer player, int index) {
		String blocker = CompanionTalents.blocker(species(), talents, index, getStage(), getLevel());

		if (blocker != null) {
			player.sendOverlayMessage(Component.translatable("jtechstorage.talent.blocked." + blocker).withStyle(ChatFormatting.RED));
			return;
		}

		talents[index]++;
		applyTalents();
		CompanionTalents.Talent talent = CompanionTalents.tree(species()).get(index);
		playSound(ModSounds.COMPANION_LEVEL, 0.7f, 1.4f + talents[index] * 0.1f);

		if (level() instanceof ServerLevel server) {
			server.sendParticles(ParticleTypes.END_ROD, getX(), getY() + getBbHeight() * 0.6, getZ(), 12, 0.3, 0.4, 0.3, 0.03);
		}

		player.sendOverlayMessage(Component.translatable("jtechstorage.talent.learned", Component.translatable("jtechstorage.talent." + talent.id()),
				talents[index], talent.max()).withStyle(ChatFormatting.LIGHT_PURPLE));
	}

	/** Alle Talentpunkte zurück – kostet einen Amethyst-Kern (im Kreativmodus nichts). */
	public void resetTalents(ServerPlayer player) {
		if (CompanionTalents.spent(talents) == 0) {
			player.sendOverlayMessage(Component.translatable("jtechstorage.talent.reset_nothing").withStyle(ChatFormatting.GRAY));
			return;
		}

		if (!player.getAbilities().instabuild) {
			net.minecraft.world.entity.player.Inventory inventory = player.getInventory();
			int slot = -1;

			for (int i = 0; i < inventory.getContainerSize(); i++) {
				if (inventory.getItem(i).is(dev.bartels.jtechstorage.ModContent.AMETHYST_CORE)) {
					slot = i;
					break;
				}
			}

			if (slot < 0) {
				player.sendOverlayMessage(Component.translatable("jtechstorage.talent.reset_cost").withStyle(ChatFormatting.RED));
				return;
			}

			inventory.getItem(slot).shrink(1);
		}

		java.util.Arrays.fill(talents, 0);
		clearSpecialCooldown();
		applyTalents();
		playSound(ModSounds.COMPANION_EVOLVE, 0.8f, 1.4f);

		if (level() instanceof ServerLevel server) {
			server.sendParticles(ParticleTypes.WITCH, getX(), getY() + getBbHeight() * 0.5, getZ(), 25, 0.4, 0.5, 0.4, 0.05);
		}

		player.sendOverlayMessage(Component.translatable("jtechstorage.talent.reset_done", getDisplayName()).withStyle(ChatFormatting.LIGHT_PURPLE));
	}

	// ------------------------------------------------------------------
	// Haltung (v0.26)
	// ------------------------------------------------------------------

	public int getStance() {
		return stance;
	}

	public void setStance(ServerPlayer player, int value) {
		stance = Math.floorMod(value, 3);

		if (stance == STANCE_PASSIVE || stance == STANCE_DEFENSIVE && getTarget() instanceof net.minecraft.world.entity.monster.Enemy) {
			setTarget(null);
		}

		playSound(ModSounds.COMPANION_COMMAND, 0.8f, 1.0f + stance * 0.15f);
		player.sendOverlayMessage(Component.translatable("jtechstorage.companion.stance_set", getDisplayName(),
				Component.translatable("jtechstorage.companion.stance." + stance)).withStyle(ChatFormatting.LIGHT_PURPLE));
	}

	/** Eigene Ziele der Art (Prioritäten 4–6 bzw. Zielauswahl). */
	protected abstract void registerCompanionGoals();

	@Override
	public void aiStep() {
		super.aiStep();

		if (!(level() instanceof ServerLevel)) {
			return;
		}

		int attack = getAttackTicks();

		if (attack > 0) {
			entityData.set(DATA_ATTACK, attack - 1);
		}

		int action = getActionTicks();

		if (action > 0) {
			entityData.set(DATA_ACTION, action - 1);
		}

		if (attackCooldown > 0) {
			attackCooldown--;
		}

		if (isVehicle() && getTarget() != null) {
			setTarget(null);
		}

		setAggressive(getTarget() != null);

		if ((!isActive() && !isGuarding() || !canFight()) && getTarget() != null) {
			setTarget(null);
		}

		if ((tickCount + getId()) % 40 == 0) {
			updateBase();
		}

		tickShoulder();
		tickSpecial((ServerLevel) level());

		if (xpBoost > 0) {
			xpBoost--;

			if (tickCount % 20 == 0 && level() instanceof ServerLevel server) {
				server.sendParticles(ParticleTypes.WAX_ON, getX(), getY() + getBbHeight() * 0.7, getZ(), 1, 0.25, 0.2, 0.25, 0.0);
			}
		}

		if (exhausted > 0 && --exhausted == 0) {
			entityData.set(DATA_EXHAUSTED, false);
		}

		// erholt sich von selbst, erschöpft schneller
		if (getHealth() < getMaxHealth() && tickCount % (isExhausted() ? 20 : 80) == 0) {
			heal(1.0f);
		}

		boolean sitPose = isOrderedToSit() && !isGuarding();

		if (isInSittingPose() != sitPose) {
			setInSittingPose(sitPose);
		}

		// Nicht (mehr) der Begleiter seines Besitzers (freigelassen, ersetzt)? Dann verabschiedet er sich.
		if (tickCount % 100 == 0 && getOwnerReference() != null && level() instanceof ServerLevel server) {
			CompanionData.Entry entry = CompanionData.get(server.getServer()).of(getOwnerReference().getUUID());

			if (entry == null || !entry.companion().equals(getUUID())) {
				server.sendParticles(ParticleTypes.WITCH, getX(), getY() + 0.5, getZ(), 20, 0.3, 0.4, 0.3, 0.05);
				discard();
			}
		}
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		Entity attacker = source.getEntity();

		if (attacker instanceof Player || attacker instanceof Companion
				|| attacker instanceof TamableAnimal pet && getOwner() != null && pet.isOwnedBy(getOwner())) {
			return false;      // Spieler (auch der Besitzer) und befreundete Tiere verletzen ihn nicht
		}

		if (source.is(DamageTypes.FELL_OUT_OF_WORLD)) {
			// ins Leere gefallen: erschöpft zurück zum Besitzer (oder zum Weltspawn) statt zu sterben
			setHealth(Math.min(getHealth(), 2.0f));
			exhaust();
			rescueFromVoid(level);
			return true;
		}

		if (attacker != null && !source.is(DamageTypes.GENERIC_KILL) && blockDamage(level, source)) {
			level.sendParticles(ParticleTypes.ELECTRIC_SPARK, getX(), getY() + getBbHeight() * 0.6, getZ(), 12, 0.3, 0.3, 0.3, 0.1);
			playSound(net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_HIT, 1.0f, 1.4f);
			return false;
		}

		if (getHealth() - amount < 2.0f && !source.is(DamageTypes.GENERIC_KILL)) {
			if (!isExhausted() && resistExhaustion(level)) {
				return true;
			}

			setHealth(2.0f);
			exhaust();
			return true;
		}

		boolean hurt = super.hurtServer(level, source, amount);

		// Dornen: ein Teil des Nahkampfschadens geht zurück
		if (hurt && thornsFraction() > 0 && attacker instanceof LivingEntity living && source.getDirectEntity() == attacker && living.isAlive()) {
			living.hurtServer(level, damageSources().thorns(this), (float) (amount * thornsFraction()));
		}

		return hurt;
	}

	private void rescueFromVoid(ServerLevel level) {
		setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
		fallDistance = 0;

		if (getOwner() instanceof ServerPlayer owner && owner.level() == level && owner.getY() > level.getMinY()) {
			snapTo(owner.getX(), owner.getY(), owner.getZ(), getYRot(), 0.0f);
		} else {
			net.minecraft.core.BlockPos spawn = level.getRespawnData().pos();
			snapTo(spawn.getX() + 0.5, level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, spawn.getX(), spawn.getZ()),
					spawn.getZ() + 0.5, getYRot(), 0.0f);
		}
	}

	private void exhaust() {
		if (!isExhausted()) {
			playSound(hurtSound(), 1.0f, 0.7f);

			if (getOwner() instanceof ServerPlayer owner) {
				owner.sendOverlayMessage(Component.translatable("jtechstorage.companion.exhausted", getDisplayName()).withStyle(ChatFormatting.RED));
			}
		}

		exhausted = EXHAUSTED_TICKS;
		entityData.set(DATA_EXHAUSTED, true);
		setTarget(null);
		getNavigation().stop();
	}

	@Override
	public boolean causeFallDamage(double fallDistance, float multiplier, DamageSource source) {
		return false;
	}

	@Override
	public boolean canAttack(LivingEntity target) {
		return canFight() && !(target instanceof Player) && !(target instanceof Companion) && super.canAttack(target);
	}

	@Override
	public boolean wantsToAttack(LivingEntity target, LivingEntity owner) {
		if (target instanceof Player || target instanceof Companion || target instanceof net.minecraft.world.entity.monster.Creeper) {
			return false;
		}

		return !(target instanceof TamableAnimal pet && pet.isTame() && pet.isOwnedBy(owner));
	}

	@Override
	public void remove(RemovalReason reason) {
		// Wirklich weg (z. B. /kill), nicht nur entladen oder in eine andere Dimension gewechselt: Platz für einen neuen
		if (level() instanceof ServerLevel server && (reason == RemovalReason.KILLED || reason == RemovalReason.DISCARDED)) {
			CompanionData.get(server.getServer()).forgetCompanion(getUUID());
		}

		super.remove(reason);
	}

	// ------------------------------------------------------------------
	// Interaktion
	// ------------------------------------------------------------------

	@Override
	public InteractionResult mobInteract(Player player, InteractionHand hand) {
		ItemStack held = player.getItemInHand(hand);

		if (hand != InteractionHand.MAIN_HAND) {
			return InteractionResult.PASS;
		}

		if (!isOwnedBy(player)) {
			if (!level().isClientSide()) {
				LivingEntity owner = getOwner();
				player.sendOverlayMessage(Component.translatable("jtechstorage.companion.foreign",
						owner != null ? owner.getDisplayName() : Component.literal("?")));
			}

			return InteractionResult.PASS;
		}

		if (held.is(CompanionContent.RESONANCE_TREAT)) {
			if (!level().isClientSide()) {
				feedTreat(player, held);
			}

			return InteractionResult.SUCCESS;
		}

		InteractionResult special = interactSpecial(player, held);

		if (special != null) {
			return special;
		}

		if (!held.isEmpty()) {
			return InteractionResult.PASS;
		}

		if (player.isSecondaryUseActive()) {
			// Shift + Rechtsklick: Begleiter-Menü (v0.26)
			if (level().isClientSide()) {
				openMenu.accept(this);
			}

			return InteractionResult.SUCCESS;
		}

		if (level().isClientSide()) {
			return InteractionResult.SUCCESS;
		}

		if (canBeRidden() && !isVehicle()) {
			// Stufe 3 (Mantis, Rabe): aufsitzen – Folgen/Bleiben dann per Taste (Standard G)
			setOrderedToSit(false);
			setInSittingPose(false);
			getNavigation().stop();
			setTarget(null);
			player.startRiding(this);
			player.sendOverlayMessage(Component.translatable("jtechstorage.companion.ride." + species()).withStyle(ChatFormatting.LIGHT_PURPLE));
			return InteractionResult.SUCCESS;
		}

		toggleStay(player);
		return InteractionResult.SUCCESS;
	}

	/** Resonanz-Leckerli: doppelte XP (5 min, bis 15 min), Tempo, 2 Herzen und Schluss mit der Erschöpfung. */
	private void feedTreat(Player player, ItemStack held) {
		if (xpBoost >= TREAT_BOOST_MAX - 20 * 60) {
			player.sendOverlayMessage(Component.translatable("jtechstorage.treat.full", getDisplayName()).withStyle(ChatFormatting.GRAY));
			return;
		}

		held.consume(1, player);
		xpBoost = Math.min(TREAT_BOOST_MAX, xpBoost + TREAT_BOOST);
		addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.SPEED, 20 * 60, 1));
		heal(4.0f);

		if (isExhausted()) {
			exhausted = 0;
			entityData.set(DATA_EXHAUSTED, false);
		}

		triggerAction();
		playSound(net.minecraft.sounds.SoundEvents.GENERIC_EAT.value(), 0.8f, 1.2f);
		playSound(ModSounds.COMPANION_LEVEL, 0.6f, 1.3f);

		if (level() instanceof ServerLevel server) {
			server.sendParticles(ParticleTypes.HEART, getX(), getY() + getBbHeight() + 0.2, getZ(), 3, 0.3, 0.2, 0.3, 0.0);
			server.sendParticles(ParticleTypes.WAX_ON, getX(), getY() + getBbHeight() * 0.6, getZ(), 12, 0.4, 0.4, 0.4, 0.05);
		}

		player.sendOverlayMessage(Component.translatable("jtechstorage.treat.fed", getDisplayName(), (xpBoost + 1199) / 1200)
				.withStyle(ChatFormatting.GOLD));
	}

	/** Geteilte Erfahrung vom Besitzer – mit Leckerli doppelt (der Bonus kommt obendrauf, nicht vom Spieler). */
	public void addSharedXp(int amount) {
		addXp(xpBoost > 0 ? amount * 2 : amount);
	}

	public int getXpBoostTicks() {
		return xpBoost;
	}

	/** Folgen ↔ Bleiben (Rechtsklick oder Taste G). */
	public void toggleStay(Player player) {
		if (isVehicle()) {
			return;
		}

		boolean stay = !isOrderedToSit();
		setOrderedToSit(stay);
		setInSittingPose(stay);
		getNavigation().stop();
		setTarget(null);
		playSound(ModSounds.COMPANION_COMMAND, 0.8f, stay ? 0.8f : 1.2f);

		if (stay && level() instanceof ServerLevel) {
			updateBase();
		}

		String key = stay ? (canGuardBase() && isInBase() ? "jtechstorage.companion.guard" : "jtechstorage.companion.stay") : "jtechstorage.companion.follow";
		player.sendOverlayMessage(Component.translatable(key, getDisplayName()).withStyle(ChatFormatting.LIGHT_PURPLE));
	}

	// ------------------------------------------------------------------
	// Reiten (v0.25.4): Mantis und Rabe auf Stufe 3
	// ------------------------------------------------------------------

	/** Kann man ihn gerade reiten? */
	protected boolean canBeRidden() {
		return false;
	}

	/** Sitzplatz (Blöcke, relativ, vor der Drehung). */
	protected Vec3 seat() {
		return new Vec3(0, getBbHeight(), 0);
	}

	@Override
	public @Nullable LivingEntity getControllingPassenger() {
		return getFirstPassenger() instanceof Player player ? player : null;
	}

	@Override
	protected boolean canAddPassenger(Entity passenger) {
		return getPassengers().isEmpty() && canBeRidden();
	}

	@Override
	protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float scale) {
		return seat().yRot(-getYRot() * net.minecraft.util.Mth.DEG_TO_RAD);
	}

	@Override
	protected void tickRidden(Player player, Vec3 input) {
		super.tickRidden(player, input);
		setRot(player.getYRot(), player.getXRot() * 0.5f);
		yRotO = getYRot();
		yBodyRot = getYRot();
		yHeadRot = getYRot();
	}

	/** Art-eigene Interaktion (Rabe: Tasche, Speicher-Schlüssel) – null = keine. */
	protected @Nullable InteractionResult interactSpecial(Player player, ItemStack held) {
		return null;
	}

	/** Status im Chat: Stufe, Level, Erfahrung bis zum nächsten Level und zur nächsten Stufe. */
	public void sendStatus(Player player) {
		int level = getLevel();
		int stage = getStage();
		int xp = getXp();
		int next = xpForLevel(level + 1) - xp;
		player.sendSystemMessage(Component.translatable("jtechstorage.companion.status", getDisplayName(),
				Component.translatable("jtechstorage.companion.stage." + stage), level, next).withStyle(ChatFormatting.LIGHT_PURPLE));

		if (stage < 3) {
			ModConfig.Companions config = ModConfig.get().companions;
			int target = stage == 1 ? config.stage2Level : config.stage3Level;
			player.sendSystemMessage(Component.translatable("jtechstorage.companion.status_next", target, xpForLevel(target) - xp)
					.withStyle(ChatFormatting.GRAY));
		}

		if (xpBoost > 0) {
			player.sendSystemMessage(Component.translatable("jtechstorage.treat.status", (xpBoost + 1199) / 1200).withStyle(ChatFormatting.GOLD));
		}

		player.sendSystemMessage(Component.translatable(isOrderedToSit() ? "jtechstorage.companion.status_stay" : "jtechstorage.companion.status_follow")
				.withStyle(ChatFormatting.GRAY));
	}

	// ------------------------------------------------------------------
	// Vanilla-Tier-Kram, der für Begleiter nicht gilt
	// ------------------------------------------------------------------

	@Override
	public boolean isFood(ItemStack stack) {
		return false;
	}

	@Override
	public boolean canMate(Animal other) {
		return false;
	}

	@Override
	public @Nullable AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
		return null;
	}

	@Override
	public boolean removeWhenFarAway(double distance) {
		return false;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return hurtSound();
	}

	// ------------------------------------------------------------------
	// Speichern
	// ------------------------------------------------------------------

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putInt("CompanionXp", getXp());
		output.putInt("Exhausted", exhausted);
		output.putInt("XpBoost", xpBoost);
		output.putIntArray("Talents", talents.clone());
		output.putInt("Stance", stance);
		output.putInt("SpecialCooldown", specialCooldown);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		entityData.set(DATA_XP, input.getIntOr("CompanionXp", 0));
		exhausted = input.getIntOr("Exhausted", 0);
		xpBoost = input.getIntOr("XpBoost", 0);
		entityData.set(DATA_EXHAUSTED, exhausted > 0);
		int[] saved = CompanionTalents.migrate(species(), input.getIntArray("Talents").orElse(new int[0]));
		System.arraycopy(saved, 0, talents, 0, talents.length);
		specialCooldown = input.getIntOr("SpecialCooldown", 0);

		stance = Math.floorMod(input.getIntOr("Stance", STANCE_AGGRESSIVE), 3);
		float health = input.getFloatOr("Health", getHealth());
		refreshStage();
		setHealth(Math.min(getMaxHealth(), health));     // Leben erst nach den Talent-Boni setzen (sonst gekappt)
	}

	/** Erschöpft: kämpft und arbeitet nicht, trottet langsam hinterher. */
	private class ExhaustedGoal extends Goal {
		ExhaustedGoal() {
			setFlags(EnumSet.of(Flag.JUMP));
		}

		@Override
		public boolean canUse() {
			return isExhausted();
		}

		@Override
		public void tick() {
			setTarget(null);
		}
	}
}
