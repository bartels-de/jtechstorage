package dev.bartels.jtechstorage.companion;

import java.util.EnumSet;
import java.util.UUID;
import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import dev.bartels.jtechstorage.JTechStorage;
import dev.bartels.jtechstorage.ModAdvancements;
import dev.bartels.jtechstorage.ModConfig;
import dev.bartels.jtechstorage.ModContent;
import dev.bartels.jtechstorage.ModSounds;
import dev.bartels.jtechstorage.block.StorageTerminalBlockEntity;
import dev.bartels.jtechstorage.farm.FarmZones;

/**
 * Companion (v0.25): Mantis, Mushroom or Raven - a gift from the geode keeper. Belongs to exactly one player, follows them
 * everywhere (also through portals), gains experience with them (half of every XP collected) and grows into the next tier at level 30
 * and 100. Right-click: follow/stay, Shift + right-click or key K: companion menu (v0.26). Does not die - when it takes too
 * much damage it retreats exhausted and recovers.
 */
public abstract class Companion extends TamableAnimal {
	public static final int ATTACK_TICKS = 14;
	public static final int ACTION_TICKS = 20;
	private static final int EXHAUSTED_TICKS = 20 * 45;
	private static final EntityDataAccessor<Integer> DATA_XP = SynchedEntityData.defineId(Companion.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Boolean> DATA_EXHAUSTED = SynchedEntityData.defineId(Companion.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Integer> DATA_ATTACK = SynchedEntityData.defineId(Companion.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> DATA_ACTION = SynchedEntityData.defineId(Companion.class, EntityDataSerializers.INT);
	/** Sits on the owner's shoulder (v0.25.1: mushroom and raven at tier 1). */
	private static final EntityDataAccessor<Boolean> DATA_SHOULDER = SynchedEntityData.defineId(Companion.class, EntityDataSerializers.BOOLEAN);
	/** Appearance (v0.25.4): packed, see {@link CompanionLooks}, and glow colour (-1 = default). */
	private static final EntityDataAccessor<Integer> DATA_LOOK = SynchedEntityData.defineId(Companion.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> DATA_GLOW = SynchedEntityData.defineId(Companion.class, EntityDataSerializers.INT);
	/** Defeated monsters (unlocks the ember skin). */
	private int kills;
	/** Bond (v0.26.0): points (synchronised) and gesture ((counter << 2) | gesture, so that the same gesture can start again). */
	private static final EntityDataAccessor<Integer> DATA_BOND = SynchedEntityData.defineId(Companion.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> DATA_GESTURE = SynchedEntityData.defineId(Companion.class, EntityDataSerializers.INT);
	private long bondDay = -1;
	private int fedToday;
	private boolean favoriteToday;
	private int petsToday;
	private int playsToday;
	private long lastPet = -100000;
	/** Client: start of the running gesture. */
	private int gestureStart = -1000;
	private int gesture;
	/** Currently fetching a thrown toy. */
	private boolean fetching;

	private int exhausted;
	/** Resonance treat (v0.25.3): double experience while > 0 (ticks). */
	private int xpBoost;
	public static final int TREAT_BOOST = 20 * 60 * 5;
	public static final int TREAT_BOOST_MAX = 20 * 60 * 15;
	private int lastStage = 1;
	/** Only after the constructor is the synchronised data available (the hitbox is queried earlier). */
	private boolean ready;
	private int cachedXp = -1;
	private int cachedLevel;
	/** Talent ranks (v0.26), index see {@link CompanionTalents}. */
	private final int[] talents = new int[CompanionTalents.COUNT];
	/** Stance (v0.26): aggressive, defensive, passive. */
	public static final int STANCE_AGGRESSIVE = 0;
	public static final int STANCE_DEFENSIVE = 1;
	public static final int STANCE_PASSIVE = 2;
	private int stance = STANCE_AGGRESSIVE;
	/** Client: opens the companion menu (set by the client). */
	public static Consumer<Companion> openMenu = companion -> {
	};

	protected Companion(EntityType<? extends Companion> type, Level level) {
		super(type, level);
		setPersistenceRequired();
		ready = true;
	}

	/** Species of the companion (for texts and the companion list): mantis, shroom, raven. */
	public abstract String species();

	/** Values per tier (1-3): health, damage, speed, armour. */
	protected abstract void applyStageAttributes(int stage);

	/** Hitbox factor per tier. */
	protected abstract float stageScale(int stage);

	protected abstract SoundEvent hurtSound();

	/** Base damage of the mantis per tier - the raven fights with 50%, the mushroom with 25% of it (v0.25.2). */
	public static final double[] MANTIS_DAMAGE = {4, 7, 11};

	/** Share of the mantis damage: mantis 1.0, raven 0.7 (v0.26.2), mushroom 0.25. */
	protected abstract double combatShare();

	/** Damage at the current tier. */
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
		builder.define(DATA_LOOK, CompanionLooks.DEFAULT);
		builder.define(DATA_GLOW, CompanionLooks.GLOW_DEFAULT);
		builder.define(DATA_BOND, 0);
		builder.define(DATA_GESTURE, 0);
	}

	// ------------------------------------------------------------------
	// Bond (v0.26.0): feeding, petting, playing -> gestures, skin, aura
	// ------------------------------------------------------------------

	public int getBond() {
		return entityData.get(DATA_BOND);
	}

	public int bondStage() {
		return CompanionBond.stage(getBond());
	}

	private long today() {
		return Math.floorDiv(level().getDefaultClockTime(), 24000L);
	}

	/** New day: reset the daily counters. */
	private void rollBondDay() {
		long day = today();

		if (day != bondDay) {
			bondDay = day;
			fedToday = 0;
			favoriteToday = false;
			petsToday = 0;
			playsToday = 0;
		}
	}

	public Item favoriteFood() {
		return CompanionBond.favorite(species(), today(), getUUID());
	}

	private void addBond(int amount) {
		int old = getBond();
		int now = Math.min(CompanionBond.MAX, old + amount);
		entityData.set(DATA_BOND, now);
		int stage = CompanionBond.stage(now);

		if (stage > CompanionBond.stage(old) && level() instanceof ServerLevel server) {
			server.sendParticles(ParticleTypes.HEART, getX(), getY() + getBbHeight() + 0.3, getZ(), 12, 0.5, 0.3, 0.5, 0.0);
			playSound(ModSounds.COMPANION_LEVEL, 1.0f, 1.3f);

			if (getOwner() instanceof ServerPlayer owner) {
				owner.sendSystemMessage(Component.translatable("jtechstorage.bond.stage_up", getDisplayName(),
						Component.translatable("jtechstorage.bond.stage." + CompanionBond.STAGE_KEYS[stage])).withStyle(ChatFormatting.LIGHT_PURPLE));
				owner.sendSystemMessage(Component.translatable("jtechstorage.bond.unlock." + stage,
						Component.translatable("jtechstorage.gesture." + CompanionBond.gestureKey(species(), Math.min(stage, 3)))).withStyle(ChatFormatting.GRAY));
			}
		}
	}

	private void hearts(int count) {
		if (level() instanceof ServerLevel server) {
			server.sendParticles(ParticleTypes.HEART, getX(), getY() + getBbHeight() + 0.2, getZ(), count, 0.3, 0.2, 0.3, 0.0);
		}
	}

	/** Plays a gesture (server -> all clients). */
	public void playGesture(int which) {
		int counter = (entityData.get(DATA_GESTURE) >> 2) + 1;
		entityData.set(DATA_GESTURE, (counter << 2) | (which & 3));
	}

	/** The best joy gesture it already knows (0 = none yet). */
	private int happyGesture(boolean cuddle) {
		int stage = bondStage();
		return cuddle && stage >= 2 ? CompanionBond.NUZZLE : stage >= 1 ? CompanionBond.JOY : 0;
	}

	/** Gesture from the menu. */
	public void requestGesture(ServerPlayer player, int which) {
		if (which < 1 || which > 3 || bondStage() < which || isExhausted()) {
			return;
		}

		playGesture(which);
	}

	/** Feeding with the food of its species: favourite food +25 (once a day), otherwise +3 (five times a day). */
	private void feed(Player player, ItemStack held) {
		rollBondDay();
		boolean favorite = held.is(favoriteFood()) && !favoriteToday;

		if (!favorite && fedToday >= CompanionBond.FOOD_PER_DAY) {
			player.sendOverlayMessage(Component.translatable("jtechstorage.bond.full", getDisplayName()).withStyle(ChatFormatting.GRAY));
			return;
		}

		Item food = held.getItem();
		held.consume(1, player);
		heal(2.0f);
		playSound(SoundEvents.GENERIC_EAT.value(), 0.8f, 1.2f);

		if (level() instanceof ServerLevel server) {
			server.sendParticles(new ItemParticleOption(ParticleTypes.ITEM,
					food),
					getX(), getY() + getBbHeight() * 0.8, getZ(), 8, 0.2, 0.2, 0.2, 0.05);
		}

		if (favorite) {
			favoriteToday = true;
			addBond(CompanionBond.FAVORITE);
			hearts(6);
			player.sendOverlayMessage(Component.translatable("jtechstorage.bond.favorite", getDisplayName()).withStyle(ChatFormatting.LIGHT_PURPLE));
		} else {
			fedToday++;
			addBond(CompanionBond.FOOD);
			hearts(2);
		}

		int g = happyGesture(false);

		if (g > 0) {
			playGesture(g);
		}
	}

	/** Petting (menu): +5, once per minute, ten times a day. */
	public void pet(ServerPlayer player) {
		if (player.level() != level() || !isOnShoulder() && distanceToSqr(player) > 8 * 8) {
			player.sendOverlayMessage(Component.translatable("jtechstorage.bond.too_far", getDisplayName()).withStyle(ChatFormatting.GRAY));
			return;
		}

		rollBondDay();
		long now = level().getGameTime();

		if (now - lastPet < CompanionBond.PET_COOLDOWN) {
			player.sendOverlayMessage(Component.translatable("jtechstorage.bond.pet_wait", getDisplayName(),
					(CompanionBond.PET_COOLDOWN - (now - lastPet) + 19) / 20).withStyle(ChatFormatting.GRAY));
			return;
		}

		lastPet = now;
		getNavigation().stop();
		getLookControl().setLookAt(player);
		hearts(4);
		playSound(ModSounds.COMPANION_COMMAND, 0.6f, 1.4f);

		if (petsToday < CompanionBond.PETS_PER_DAY) {
			petsToday++;
			addBond(CompanionBond.PET);
		}

		int g = happyGesture(true);

		if (g > 0) {
			playGesture(g);
		}
	}

	/** Client: running gesture (0 = none) and its progress 0..1. */
	public int gestureId(float partialTick) {
		return gestureProgress(partialTick) < 1.0f ? gesture : 0;
	}

	public float gestureProgress(float partialTick) {
		return Math.clamp((tickCount - gestureStart + partialTick) / CompanionBond.GESTURE_TICKS, 0.0f, 1.0f);
	}

	/** Currently playing with the player (fetching a toy). */
	public boolean isFetching() {
		return fetching;
	}

	/** Fetch game: when the owner throws a stick or an amethyst shard, it brings it back (+10, ten times a day). */
	private class FetchGoal extends Goal {
		private net.minecraft.world.entity.item.@org.jspecify.annotations.Nullable ItemEntity toy;
		private ItemStack carried = ItemStack.EMPTY;
		private int timeout;

		FetchGoal() {
			setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
		}

		@Override
		public boolean canUse() {
			if ((tickCount + getId()) % 10 != 0 || !isActive() || getTarget() != null || isVehicle() || !(getOwner() instanceof Player owner)
					|| owner.level() != level()) {
				return false;
			}

			for (ItemEntity item : level().getEntitiesOfClass(ItemEntity.class,
					owner.getBoundingBox().inflate(16.0), e -> e.getOwner() == owner && e.getAge() < 300 && CompanionBond.isToy(e.getItem()))) {
				toy = item;
				return true;
			}

			return false;
		}

		@Override
		public void start() {
			fetching = true;
			timeout = 400;
			carried = ItemStack.EMPTY;
		}

		@Override
		public boolean canContinueToUse() {
			return timeout > 0 && !isExhausted() && getTarget() == null && getOwner() instanceof Player owner && owner.level() == level()
					&& (!carried.isEmpty() || toy != null && toy.isAlive());
		}

		@Override
		public void tick() {
			timeout--;

			if (!(getOwner() instanceof Player owner)) {
				return;
			}

			if (carried.isEmpty() && toy != null) {
				getLookControl().setLookAt(toy);

				if (timeout % 10 == 0) {
					getNavigation().moveTo(toy, 1.4);
				}

				if (distanceToSqr(toy) < 2.0 * 2.0) {
					carried = toy.getItem().copy();
					toy.discard();
					toy = null;
					playSound(SoundEvents.ITEM_PICKUP, 0.6f, 1.2f);
					getNavigation().stop();
				}

				return;
			}

			getLookControl().setLookAt(owner);

			if (timeout % 10 == 0) {
				getNavigation().moveTo(owner, 1.4);
			}

			if (distanceToSqr(owner) < 2.5 * 2.5) {
				if (!owner.getInventory().add(carried) && level() instanceof ServerLevel server) {
					owner.spawnAtLocation(server, carried);
				}

				carried = ItemStack.EMPTY;
				rollBondDay();
				hearts(3);
				playSound(ModSounds.COMPANION_COMMAND, 0.6f, 1.6f);

				if (playsToday < CompanionBond.PLAYS_PER_DAY) {
					playsToday++;
					addBond(CompanionBond.PLAY);
				}

				int g = happyGesture(false);

				if (g > 0) {
					playGesture(g);
				}

				timeout = 0;
			}
		}

		@Override
		public void stop() {
			fetching = false;
			toy = null;
			getNavigation().stop();

			if (!carried.isEmpty() && level() instanceof ServerLevel server) {
				spawnAtLocation(server, carried);
			}

			carried = ItemStack.EMPTY;
		}
	}

	// ------------------------------------------------------------------
	// Appearance (v0.25.4)
	// ------------------------------------------------------------------

	public int getLook() {
		return entityData.get(DATA_LOOK);
	}

	public int getGlowColor() {
		return entityData.get(DATA_GLOW);
	}

	public int getKills() {
		return kills;
	}

	/** Set one appearance field (from the menu) - only what is unlocked. */
	public void setLook(ServerPlayer player, int field, int value) {
		if (!CompanionLooks.valid(field, value)) {
			return;
		}

		Component missing = CompanionLooks.requirement(field, value, getLevel(), getStage(), kills, getBond());

		if (missing != null) {
			player.sendOverlayMessage(Component.translatable("jtechstorage.look.locked", missing).withStyle(ChatFormatting.RED));
			return;
		}

		entityData.set(DATA_LOOK, CompanionLooks.with(getLook(), field, value));
	}

	/** Set the glow colour (RGB, -1 = default). */
	public void setGlowColor(ServerPlayer player, int rgb) {
		Component missing = CompanionLooks.glowRequirement(getLevel());

		if (missing != null && rgb != CompanionLooks.GLOW_DEFAULT) {
			player.sendOverlayMessage(Component.translatable("jtechstorage.look.locked", missing).withStyle(ChatFormatting.RED));
			return;
		}

		entityData.set(DATA_GLOW, rgb < 0 ? CompanionLooks.GLOW_DEFAULT : rgb & 0xFFFFFF);
	}

	/** Equipment tier 0-5 (best unlocked material) - determines the bonuses of collar, amulet and armour. */
	public int equipmentTier() {
		return ModConfig.get().companions.equipment ? CompanionLooks.bestMaterial(getLevel(), getStage()) : 0;
	}

	@Override
	public boolean killedEntity(ServerLevel level, LivingEntity entity, DamageSource source) {
		if (entity instanceof Enemy) {
			kills++;
		}

		return super.killedEntity(level, entity, source);
	}

	// ------------------------------------------------------------------
	// Experience and tiers
	// ------------------------------------------------------------------

	public int getXp() {
		return entityData.get(DATA_XP);
	}

	/** Level on the vanilla curve from the collected experience. */
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

	/** Total experience needed for a level (as for the player). */
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
		// estimate via the inverse of the upper curve, then adjust exactly
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
			if (CompanionLooks.bestMaterial(newLevel, getStage()) != CompanionLooks.bestMaterial(oldLevel, getStage())) {
				applyTalents();                          // new equipment material unlocked
			}

			server.sendParticles(ParticleTypes.HAPPY_VILLAGER, getX(), getY() + getBbHeight(), getZ(), 6, 0.3, 0.2, 0.3, 0.0);
			playSound(ModSounds.COMPANION_LEVEL, 0.6f, 1.0f + Math.min(0.6f, newLevel * 0.005f));

			if (getOwner() instanceof ServerPlayer owner) {
				owner.sendOverlayMessage(Component.translatable("jtechstorage.companion.level", getDisplayName(), newLevel)
						.withStyle(ChatFormatting.LIGHT_PURPLE));
			}
		}

		checkStage();
	}

	/** Tier has changed: stats, hitbox and a small celebration. */
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

		if (ready && DATA_GESTURE.equals(accessor) && level().isClientSide()) {
			int value = entityData.get(DATA_GESTURE);
			gesture = value & 3;
			gestureStart = value == 0 ? -1000 : tickCount;
		}

		if (ready && DATA_XP.equals(accessor)) {
			if (level().isClientSide()) {
				lastStage = getStage();     // the server notices tier changes itself in checkStage()
			}

			refreshDimensions();
		}
	}

	@Override
	protected EntityDimensions getDefaultDimensions(Pose pose) {
		return ready ? super.getDefaultDimensions(pose).scale(stageScale(getStage())) : super.getDefaultDimensions(pose);
	}

	/** Sets health and stats to match the current tier (after loading or creating). */
	public void refreshStage() {
		lastStage = getStage();
		applyStageAttributes(lastStage);
		setBase(this, Attributes.ATTACK_DAMAGE, stageDamage(lastStage));
		applyTalents();
		refreshDimensions();
	}

	protected static void setBase(Companion mob, Holder<Attribute> attribute, double value) {
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
	// Animations
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

	/** Is it currently following (not on "Stay", not exhausted)? */
	public boolean isActive() {
		return !isOrderedToSit() && !isExhausted() && getOwner() != null;
	}

	// ------------------------------------------------------------------
	// Sequence
	// ------------------------------------------------------------------

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(1, new FloatGoal(this));
		goalSelector.addGoal(2, new SitWhenOrderedToGoal(this) {
			@Override
			public boolean canUse() {
				return !isGuarding() && super.canUse();     // guard in the base: does not stay seated
			}

			@Override
			public boolean canContinueToUse() {
				return !isGuarding() && super.canContinueToUse();
			}
		});
		goalSelector.addGoal(3, new ExhaustedGoal());
		registerCompanionGoals();
		// All companions can fight (mantis strongest, raven half as strong, mushroom a quarter)
		goalSelector.addGoal(5, new CompanionMeleeGoal());
		goalSelector.addGoal(5, new CompanionRangedGoal());
		// Stance (v0.26): aggressive = everything, defensive = only defend, passive = not at all
		targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this) {
			@Override
			public boolean canUse() {
				return stance != STANCE_PASSIVE && super.canUse();
			}
		});
		targetSelector.addGoal(2, new OwnerHurtTargetGoal(this) {
			@Override
			public boolean canUse() {
				return stance == STANCE_AGGRESSIVE && super.canUse();
			}
		});
		targetSelector.addGoal(3, new HurtByTargetGoal(this, Player.class) {
			@Override
			public boolean canUse() {
				return stance != STANCE_PASSIVE && super.canUse();
			}
		}.setAlertOthers());
		targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10, true, false,
				(target, level) -> target instanceof Enemy && !(target instanceof Creeper)
						&& !FarmZones.isProtected(level, target.blockPosition())
						&& (isActive() && nearOwner(target) || isGuarding() && nearBase(target))) {
			@Override
			public boolean canUse() {
				return (stance == STANCE_AGGRESSIVE || isGuarding() && stance != STANCE_PASSIVE) && canFight() && super.canUse();
			}

			@Override
			protected AABB getTargetSearchArea(double distance) {
				return super.getTargetSearchArea(Math.max(distance, huntRadius()));
			}
		});
		goalSelector.addGoal(6, new GuardPatrolGoal());
		goalSelector.addGoal(6, new FetchGoal());
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

	/** Ticks between two attacks (more realistic pace, v0.25.4). */
	protected int attackInterval() {
		return 30;
	}

	/** Melee with its own cooldown; not while someone is riding it. */
	private class CompanionMeleeGoal extends MeleeAttackGoal {
		CompanionMeleeGoal() {
			super(Companion.this, 1.25, true);
		}

		@Override
		public boolean canUse() {
			return !isVehicle() && canFight() && !isRanged() && super.canUse();
		}

		@Override
		public boolean canContinueToUse() {
			return !isVehicle() && !isRanged() && super.canContinueToUse();
		}

		@Override
		protected boolean canPerformAttack(LivingEntity target) {
			return attackCooldown <= 0 && super.canPerformAttack(target);
		}
	}

	/** Hostile, living monster outside the protected zones of farm monitors (v0.27.0) - for area attacks. */
	public static boolean hostile(LivingEntity e) {
		return e instanceof Enemy && e.isAlive() && !FarmZones.isProtected(e.level(), e.blockPosition());
	}

	/** Fights only from a distance (v0.26.1: mushroom)? */
	protected boolean isRanged() {
		return false;
	}

	/** Range of the ranged attack in blocks. */
	protected double rangedRange() {
		return 10.0;
	}

	/** Effect of the ranged attack (particle trail etc.), before the hit. */
	protected void rangedEffect(ServerLevel level, LivingEntity target) {
	}

	/** Ranged combat (v0.26.1): keeps its distance, needs a clear line of sight and hits from up to {@link #rangedRange()} blocks. */
	private class CompanionRangedGoal extends Goal {
		private int repath;

		CompanionRangedGoal() {
			setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
		}

		@Override
		public boolean canUse() {
			return isRanged() && !isVehicle() && canFight() && getTarget() != null && getTarget().isAlive();
		}

		@Override
		public boolean canContinueToUse() {
			return canUse();
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
			double dist = distanceTo(target);
			boolean sees = getSensing().hasLineOfSight(target);
			double range = rangedRange();

			if (dist > range * 0.85 || !sees) {
				if (--repath <= 0) {
					repath = 10;
					getNavigation().moveTo(target, 1.15);
				}
			} else if (dist < 3.5) {
				// too close: back away
				if (--repath <= 0) {
					repath = 10;
					Vec3 away = DefaultRandomPos.getPosAway(Companion.this, 8, 4, target.position());

					if (away != null) {
						getNavigation().moveTo(away.x, away.y, away.z, 1.3);
					}
				}
			} else {
				getNavigation().stop();
				repath = 0;
			}

			if (sees && dist <= range && attackCooldown <= 0) {
				rangedEffect(level, target);
				doHurtTarget(level, target);
			}
		}
	}

	private boolean nearOwner(LivingEntity target) {
		LivingEntity owner = getOwner();
		double r = huntRadius();
		return owner != null && owner.level() == level() && target.distanceToSqr(owner) < r * r;
	}

	/** Distance to the owner within which it attacks monsters on its own (talent Hunting Instinct). */
	protected double huntRadius() {
		return 16.0;
	}

	/** Actual ticks between two attacks (with talents). */
	public int effectiveAttackInterval() {
		return Math.max(8, Math.round(attackInterval() * (float) intervalFactor()));
	}

	private static final Identifier CRIT_ID = JTechStorage.id("companion_crit");

	@Override
	public boolean doHurtTarget(ServerLevel level, Entity target) {
		boolean crit = critChance() > 0 && random.nextDouble() < critChance();
		AttributeInstance damage = getAttribute(Attributes.ATTACK_DAMAGE);
		double bonus = (crit ? 1.0 : 0.0) + (target instanceof LivingEntity living ? bonusDamageAgainst(living) : 0.0);

		if (bonus > 0 && damage != null) {
			damage.addOrUpdateTransientModifier(new AttributeModifier(CRIT_ID, bonus,
					AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
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
	// Base, guard, shoulder (v0.25.1)
	// ------------------------------------------------------------------

	/** Owner's own terminal within the 3x3 chunks around this position (or null). */
	public static @Nullable BlockPos ownTerminalAround(ServerLevel level, BlockPos pos, @Nullable UUID owner) {
		if (owner == null) {
			return null;
		}

		int cx = pos.getX() >> 4;
		int cz = pos.getZ() >> 4;
		BlockPos best = null;
		double bestDist = Double.MAX_VALUE;

		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				if (!(level.getChunkSource().getChunkNow(cx + dx, cz + dz) instanceof LevelChunk chunk)) {
					continue;
				}

				for (BlockEntity entity : chunk.getBlockEntities().values()) {
					if (entity instanceof StorageTerminalBlockEntity terminal && owner.equals(terminal.getOwnerId())) {
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

	private @Nullable BlockPos baseTerminal;
	private static final Identifier BASE_ID = JTechStorage.id("companion_base");

	/** Terminal of the base it is currently in (3x3 chunks around one of the owner's terminals), or null. */
	public @Nullable BlockPos baseTerminal() {
		return baseTerminal;
	}

	public boolean isInBase() {
		return baseTerminal != null;
	}

	private void updateBase() {
		if (!(level() instanceof ServerLevel level)) {
			return;
		}

		BlockPos found = ownTerminalAround(level, blockPosition(), getOwnerReference() != null ? getOwnerReference().getUUID() : null);

		if (java.util.Objects.equals(found, baseTerminal)) {
			return;
		}

		baseTerminal = found;
		AttributeInstance damage = getAttribute(Attributes.ATTACK_DAMAGE);

		if (damage != null) {
			if (found != null && baseDamageBonus() > 0) {
				damage.addOrUpdateTransientModifier(new AttributeModifier(BASE_ID, baseDamageBonus(),
						AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
			} else {
				damage.removeModifier(BASE_ID);
			}
		}
	}

	/** Damage bonus in the own base (mantis: +50%). */
	protected double baseDamageBonus() {
		return 0;
	}

	/** Can it guard the base when it is supposed to "Stay" there (mantis)? */
	protected boolean canGuardBase() {
		return false;
	}

	/** Guard: should stay in the base and defends it - even without the owner. */
	public boolean isGuarding() {
		return canGuardBase() && isOrderedToSit() && baseTerminal != null && !isExhausted() && !isVehicle();
	}

	private boolean nearBase(LivingEntity target) {
		return baseTerminal != null && target.blockPosition().distSqr(baseTerminal) < 24 * 24;
	}

	/** May it fight at all? (mushroom only from tier 2) */
	protected boolean canFight() {
		return true;
	}

	/** Guard: walks around the terminal between fights. */
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
			BlockPos center = baseTerminal;

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

	/** Does this species sit on the shoulder at the current tier (mushroom and raven at tier 1)? */
	protected boolean shoulderStage() {
		return false;
	}

	/** Does it currently want to go to the shoulder? (raven: not in combat and not away) */
	protected boolean wantsShoulder(Player owner) {
		return shoulderStage() && !fetching && !isOrderedToSit() && !isVehicle() && getTarget() == null && owner.level() == level() && owner.isAlive()
				&& !owner.isSpectator() && !owner.isSleeping();
	}

	/** Right (+1) or left (-1) shoulder. */
	public int shoulderSide() {
		return 1;
	}

	/** Position on the shoulder (feet of the companion). */
	public static Vec3 shoulderPos(Entity owner, Vec3 ownerPos, float bodyYaw, int side) {
		float yaw = bodyYaw * Mth.DEG_TO_RAD;
		double rx = -Mth.cos(yaw) * side;
		double rz = -Mth.sin(yaw) * side;
		double bx = Mth.sin(yaw) * 0.06;         // slightly backwards
		double bz = -Mth.cos(yaw) * 0.06;
		return ownerPos.add(rx * 0.38 + bx, owner.getBbHeight() - 0.38, rz * 0.38 + bz);
	}

	private void tickShoulder() {
		LivingEntity owner = getOwner();
		boolean want = owner instanceof Player player && wantsShoulder(player);

		if (want && !isOnShoulder()) {
			// first walk/fly there, then jump up
			if (distanceToSqr(owner) > 2.5 * 2.5) {
				if (tickCount % 10 == 0) {
					getNavigation().moveTo(owner, 1.4);
				}

				return;
			}

			entityData.set(DATA_SHOULDER, true);
			playSound(ModSounds.COMPANION_COMMAND, 0.5f, 1.5f);
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
			noPhysics = true;     // gravity does not apply: the position is reset every tick
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

	/** On the shoulder it pushes nobody - otherwise it would push its owner away (especially on the client). */
	@Override
	protected void pushEntities() {
		if (!isOnShoulder()) {
			super.pushEntities();
		}
	}

	@Override
	protected void doPush(Entity entity) {
		if (!isOnShoulder()) {
			super.doPush(entity);
		}
	}

	@Override
	public void tick() {
		if (level().isClientSide()) {
			noPhysics = isOnShoulder();       // on the client noPhysics is not set otherwise

			// Soulmate (v0.26.0): gentle pink aura, can be switched off in the appearance tab
			if (bondStage() >= 4 && CompanionLooks.get(getLook(), CompanionLooks.AURA) == 0 && !isInvisible() && random.nextInt(6) == 0) {
				level().addParticle(new DustParticleOptions(random.nextBoolean() ? 0xFF8AC0 : 0xFFC4DC, 0.7f),
						getRandomX(0.7), getY() + random.nextDouble() * getBbHeight(), getRandomZ(0.7), 0.0, 0.01, 0.0);

				if (random.nextInt(40) == 0) {
					level().addParticle(ParticleTypes.HEART, getRandomX(0.5), getY() + getBbHeight() + 0.2, getRandomZ(0.5), 0.0, 0.02, 0.0);
				}
			}
		}

		super.tick();
	}

	/** When exhausted (or hidden) it is not a target for monsters (v0.25.5). */
	@Override
	public boolean canBeSeenAsEnemy() {
		return !isExhausted() && !isHidden() && super.canBeSeenAsEnemy();
	}

	/** Mushroom: invisible to monsters during the Spore Veil. */
	protected boolean isHidden() {
		return false;
	}

	/** Monsters that attack {@code entity} let go of it. */
	public static void calmAttackers(ServerLevel level, LivingEntity entity, double radius) {
		for (Mob mob : level.getEntitiesOfClass(Mob.class, entity.getBoundingBox().inflate(radius),
				m -> m.getTarget() == entity)) {
			mob.setTarget(null);
			mob.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);

			if (mob.getLastHurtByMob() == entity) {
				mob.setLastHurtByMob(null);
			}
		}
	}

	// ------------------------------------------------------------------
	// Special attacks (v0.25.3): one per companion, chosen via talent, key (default B)
	// ------------------------------------------------------------------

	private int specialCooldown;

	/** Chosen special attack (0-2) or -1. */
	public int specialIndex() {
		return CompanionTalents.chosenSpecial(talents);
	}

	public int specialCooldown() {
		return specialCooldown;
	}

	/** Base cooldown of a special attack in ticks. */
	protected int specialBaseCooldown(int which) {
		return 20 * 60;
	}

	/** Cooldown with Mastery (-20% per rank). */
	public int specialCooldownMax() {
		int which = specialIndex();
		return which < 0 ? 0 : (int) (specialBaseCooldown(which) * (1.0 - 0.2 * talent("mastery")));
	}

	/** Strength of the special attack (Mastery: +25% per rank). */
	protected float specialPower() {
		return 1.0f + 0.25f * talent("mastery");
	}

	/** Does this special attack need a target? */
	protected boolean specialNeedsTarget(int which) {
		return true;
	}

	/** Executes the special attack; false = did not work (then no cooldown). */
	protected boolean performSpecial(ServerLevel level, int which, @Nullable LivingEntity target) {
		return false;
	}

	/** Can it currently perform a special attack at all (raven: not while away)? */
	protected boolean canUseSpecialNow() {
		return true;
	}

	/** Special attack on key press (or automatically with "Instinct"). */
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

	/** Target: what the owner is looking at, otherwise the companion's target, otherwise whom the owner last attacked, otherwise the nearest monster. */
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
				&& (e instanceof Enemy || e instanceof Mob mob && mob.getTarget() == getOwner())
				&& !(e instanceof TamableAnimal pet && getOwner() != null && pet.isOwnedBy(getOwner()))
				&& !FarmZones.isProtected(level(), e.blockPosition());
	}

	/** Cooldown, "Instinct" (automatic when in danger) and aftereffects of the species. */
	private void tickSpecial(ServerLevel level) {
		if (specialCooldown > 0) {
			specialCooldown--;
		}

		tickSpecialEffects(level);

		if (specialCooldown == 0 && tickCount % 20 == 0 && talent("instinct") > 0 && specialIndex() >= 0 && !isExhausted()
				&& getOwner() instanceof ServerPlayer owner && owner.level() == level && owner.distanceToSqr(this) < 24 * 24) {
			boolean lowHealth = owner.getHealth() < owner.getMaxHealth() * 0.3f;
			int enemies = level.getEntitiesOfClass(LivingEntity.class, owner.getBoundingBox().inflate(6.0),
					e -> Companion.hostile(e)).size();

			if (lowHealth && enemies > 0 || enemies >= 3) {
				useSpecial(owner, true);
			}
		}
	}

	/** Leaps/dives to {@code at} - only if there is room; false = no room (then it stays put). */
	protected boolean leapTo(ServerLevel level, Vec3 at) {
		if (!level.noCollision(this, getBoundingBox().move(at.subtract(position())))) {
			return false;
		}

		snapTo(at.x, at.y, at.z, getYRot(), getXRot());
		return true;
	}

	/** Running special attacks of the species (e.g. Blade Storm over several ticks). */
	protected void tickSpecialEffects(ServerLevel level) {
	}

	/** Reset the special attack when the talents are reset. */
	public void clearSpecialCooldown() {
		specialCooldown = 0;
	}

	// ------------------------------------------------------------------
	// Stun (mantis: Paralysing Sting): cannot move and cannot attack
	// ------------------------------------------------------------------

	private static final java.util.Map<LivingEntity, Integer> STUNNED = new java.util.WeakHashMap<>();

	public static void stun(ServerLevel level, LivingEntity target, int ticks) {
		STUNNED.merge(target, ticks, Math::max);
		target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, ticks, 9, false, false));
		target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, ticks, 9, false, false));
		target.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, ticks, 4, false, false));
	}

	public static boolean isStunned(LivingEntity entity) {
		return STUNNED.containsKey(entity);
	}

	/** Every server tick: stunned ones hold still. */
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

			if (e instanceof Mob mob) {
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
	// Talents (v0.26): values the species compute from their ranks
	// ------------------------------------------------------------------

	/** Rank of a talent of this species (0 if the talent does not exist). */
	public int talent(String id) {
		int index = CompanionTalents.index(species(), id);
		return index < 0 ? 0 : talents[index];
	}

	public int[] talentRanks() {
		return talents.clone();
	}

	/** Additional damage in percent of the base values (0.1 = +10%). */
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

	/** Factor for the time between two attacks. */
	protected double intervalFactor() {
		return 1.0;
	}

	/** Chance of double damage. */
	protected double critChance() {
		return 0;
	}

	/** Share of the melee damage that is reflected back to the attacker. */
	protected double thornsFraction() {
		return 0;
	}

	/** Hit landed: poison, slowness etc. */
	protected void onTalentHit(ServerLevel level, LivingEntity target) {
	}

	/** Additional damage against this target (share, 0.3 = +30%). */
	protected double bonusDamageAgainst(LivingEntity target) {
		return 0;
	}

	/** A target was defeated. */
	protected void onKill(ServerLevel level, LivingEntity target) {
	}

	/** Block a hit completely (mantis: Crystal Skin)? */
	protected boolean blockDamage(ServerLevel level, DamageSource source) {
		return false;
	}

	/** May it escape exhaustion once (mantis: Unyielding)? */
	protected boolean resistExhaustion(ServerLevel level) {
		return false;
	}

	private static final Identifier TALENT_ID = JTechStorage.id("companion_talents");

	/** Applies the talent values as attribute modifiers (after loading, learning and on every tier). */
	protected void applyTalents() {
		float ratio = getMaxHealth() > 0 ? getHealth() / getMaxHealth() : 1.0f;
		talentModifier(Attributes.ATTACK_DAMAGE, damageBonus(), AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
		talentModifier(Attributes.ARMOR, armorBonus(), AttributeModifier.Operation.ADD_VALUE);
		talentModifier(Attributes.MAX_HEALTH, healthBonus(), AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
		talentModifier(Attributes.MOVEMENT_SPEED, speedBonus(), AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
		// Equipment (v0.25.4): collar +10% health per material, armour +2 armour (+ toughness from netherite), amulet per species
		int tier = equipmentTier();
		equipModifier(EQUIP_HEALTH_ID, Attributes.MAX_HEALTH, 0.1 * tier, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
		equipModifier(EQUIP_ARMOR_ID, Attributes.ARMOR, 2.0 * tier, AttributeModifier.Operation.ADD_VALUE);
		equipModifier(EQUIP_ARMOR_ID, Attributes.ARMOR_TOUGHNESS, tier >= 5 ? 3.0 : tier == 4 ? 2.0 : 0.0,
				AttributeModifier.Operation.ADD_VALUE);
		equipModifier(EQUIP_DAMAGE_ID, Attributes.ATTACK_DAMAGE, amuletDamage(tier), AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
		setHealth(Math.max(1.0f, Math.min(getMaxHealth(), getMaxHealth() * ratio)));
	}

	private static final Identifier EQUIP_HEALTH_ID = JTechStorage.id("companion_collar");
	private static final Identifier EQUIP_ARMOR_ID = JTechStorage.id("companion_armor");
	private static final Identifier EQUIP_DAMAGE_ID = JTechStorage.id("companion_amulet");

	/** Amulet: additional damage (mantis +5% per material), the other species have their own amulet bonuses. */
	protected double amuletDamage(int tier) {
		return 0.0;
	}

	private void equipModifier(Identifier id, Holder<Attribute> attribute,
			double amount, AttributeModifier.Operation operation) {
		AttributeInstance instance = getAttribute(attribute);

		if (instance == null) {
			return;
		}

		if (amount == 0) {
			instance.removeModifier(id);
		} else {
			instance.addOrUpdateTransientModifier(new AttributeModifier(id, amount, operation));
		}
	}

	private void talentModifier(Holder<Attribute> attribute, double amount,
			AttributeModifier.Operation operation) {
		AttributeInstance instance = getAttribute(attribute);

		if (instance == null) {
			return;
		}

		if (amount == 0) {
			instance.removeModifier(TALENT_ID);
		} else {
			instance.addOrUpdateTransientModifier(new AttributeModifier(TALENT_ID, amount, operation));
		}
	}

	/** Spend a talent point (from the menu). */
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

	/** Return all talent points - costs one amethyst core (nothing in creative mode). */
	public void resetTalents(ServerPlayer player) {
		if (CompanionTalents.spent(talents) == 0) {
			player.sendOverlayMessage(Component.translatable("jtechstorage.talent.reset_nothing").withStyle(ChatFormatting.GRAY));
			return;
		}

		if (!player.getAbilities().instabuild) {
			Inventory inventory = player.getInventory();
			int slot = -1;

			for (int i = 0; i < inventory.getContainerSize(); i++) {
				if (inventory.getItem(i).is(ModContent.AMETHYST_CORE)) {
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
	// Stance (v0.26)
	// ------------------------------------------------------------------

	public int getStance() {
		return stance;
	}

	public void setStance(ServerPlayer player, int value) {
		stance = Math.floorMod(value, 3);

		if (stance == STANCE_PASSIVE || stance == STANCE_DEFENSIVE && getTarget() instanceof Enemy) {
			setTarget(null);
		}

		playSound(ModSounds.COMPANION_COMMAND, 0.8f, 1.0f + stance * 0.15f);
		player.sendOverlayMessage(Component.translatable("jtechstorage.companion.stance_set", getDisplayName(),
				Component.translatable("jtechstorage.companion.stance." + stance)).withStyle(ChatFormatting.LIGHT_PURPLE));
	}

	/** Own targets of the species (priorities 4-6 or target selection). */
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

		if (tickCount % 5 == 0 && (isExhausted() || isHidden())) {
			calmAttackers((ServerLevel) level(), this, 32.0);
		}

		// remember the last known position: this way it can also be fetched from unloaded chunks (v0.25.5)
		if ((tickCount + getId()) % 40 == 0 && getOwnerReference() != null) {
			CompanionData.get(((ServerLevel) level()).getServer()).setLast(getOwnerReference().getUUID(),
					GlobalPos.of(level().dimension(), blockPosition()), !isOrderedToSit());
		}

		if (xpBoost > 0) {
			xpBoost--;

			if (tickCount % 20 == 0 && level() instanceof ServerLevel server) {
				server.sendParticles(ParticleTypes.WAX_ON, getX(), getY() + getBbHeight() * 0.7, getZ(), 1, 0.25, 0.2, 0.25, 0.0);
			}
		}

		if (exhausted > 0 && --exhausted == 0) {
			entityData.set(DATA_EXHAUSTED, false);
		}

		// recovers on its own, faster when exhausted
		if (getHealth() < getMaxHealth() && tickCount % (isExhausted() ? 20 : 80) == 0) {
			heal(1.0f);
		}

		boolean sitPose = isOrderedToSit() && !isGuarding();

		if (isInSittingPose() != sitPose) {
			setInSittingPose(sitPose);
		}

		// No longer the companion of its owner (released, replaced)? Then it says goodbye.
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
			return false;      // players (including the owner) and friendly animals do not hurt it
		}

		if (source.is(DamageTypeTags.IS_FALL) || source.is(DamageTypes.IN_WALL) || source.is(DamageTypes.FLY_INTO_WALL)) {
			return false;      // no fall damage or suffocation after a jump to the owner (v0.25.5)
		}

		if (isExhausted() && !source.is(DamageTypes.GENERIC_KILL) && !source.is(DamageTypes.FELL_OUT_OF_WORLD)) {
			// exhausted: monsters let go of it, it takes no further damage (otherwise it would never recover)
			if (attacker instanceof Mob mob && mob.getTarget() == this) {
				mob.setTarget(null);
			}

			return false;
		}

		if (source.is(DamageTypes.FELL_OUT_OF_WORLD)) {
			// fell into the void: returns exhausted to the owner (or to the world spawn) instead of dying
			setHealth(Math.min(getHealth(), 2.0f));
			exhaust();
			rescueFromVoid(level);
			return true;
		}

		if (attacker != null && !source.is(DamageTypes.GENERIC_KILL) && blockDamage(level, source)) {
			level.sendParticles(ParticleTypes.ELECTRIC_SPARK, getX(), getY() + getBbHeight() * 0.6, getZ(), 12, 0.3, 0.3, 0.3, 0.1);
			playSound(SoundEvents.AMETHYST_BLOCK_HIT, 1.0f, 1.4f);
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

		// Thorns: part of the melee damage is reflected
		if (hurt && thornsFraction() > 0 && attacker instanceof LivingEntity living && source.getDirectEntity() == attacker && living.isAlive()) {
			living.hurtServer(level, damageSources().thorns(this), (float) (amount * thornsFraction()));
		}

		return hurt;
	}

	private void rescueFromVoid(ServerLevel level) {
		setDeltaMovement(Vec3.ZERO);
		fallDistance = 0;

		Vec3 spot = getOwner() instanceof ServerPlayer owner && owner.level() == level ? CompanionContent.safeSpot(level, owner, this) : null;

		if (spot != null) {
			snapTo(spot.x, spot.y, spot.z, getYRot(), 0.0f);
		} else if (level.dimension() == Level.END) {
			// over the void: onto the main island, the owner fetches it later
			snapTo(0.5, level.getHeight(Heightmap.Types.MOTION_BLOCKING, 0, 0), 0.5, getYRot(), 0.0f);
		} else {
			BlockPos spawn = level.getRespawnData().pos();
			snapTo(spawn.getX() + 0.5, level.getHeight(Heightmap.Types.MOTION_BLOCKING, spawn.getX(), spawn.getZ()),
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
		return canFight() && !(target instanceof Player) && !(target instanceof Companion)
				&& !FarmZones.isProtected(level(), target.blockPosition()) && super.canAttack(target);
	}

	@Override
	public boolean wantsToAttack(LivingEntity target, LivingEntity owner) {
		if (target instanceof Player || target instanceof Companion || target instanceof Creeper) {
			return false;
		}

		return !(target instanceof TamableAnimal pet && pet.isTame() && pet.isOwnedBy(owner));
	}

	@Override
	public void remove(RemovalReason reason) {
		// Really gone (e.g. /kill), not just unloaded or moved to another dimension: room for a new one
		if (level() instanceof ServerLevel server && (reason == RemovalReason.KILLED || reason == RemovalReason.DISCARDED)) {
			CompanionData.get(server.getServer()).forgetCompanion(getUUID());
		}

		super.remove(reason);
	}

	// ------------------------------------------------------------------
	// Interaction
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

		if (CompanionBond.isFood(species(), held)) {
			if (!level().isClientSide()) {
				feed(player, held);
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
			// Shift + right-click: companion menu (v0.26)
			if (level().isClientSide()) {
				openMenu.accept(this);
			}

			return InteractionResult.SUCCESS;
		}

		if (level().isClientSide()) {
			return InteractionResult.SUCCESS;
		}

		if (canBeRidden() && !isVehicle()) {
			// Tier 3 (mantis, raven): mount - follow/stay then via key (default G)
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

	/** Resonance treat: double XP (5 min, up to 15 min), speed, 2 hearts and an end to exhaustion. */
	private void feedTreat(Player player, ItemStack held) {
		if (xpBoost >= TREAT_BOOST_MAX - 20 * 60) {
			player.sendOverlayMessage(Component.translatable("jtechstorage.treat.full", getDisplayName()).withStyle(ChatFormatting.GRAY));
			return;
		}

		held.consume(1, player);
		xpBoost = Math.min(TREAT_BOOST_MAX, xpBoost + TREAT_BOOST);
		addEffect(new MobEffectInstance(MobEffects.SPEED, 20 * 60, 1));
		heal(4.0f);

		if (isExhausted()) {
			exhausted = 0;
			entityData.set(DATA_EXHAUSTED, false);
		}

		triggerAction();
		playSound(SoundEvents.GENERIC_EAT.value(), 0.8f, 1.2f);
		playSound(ModSounds.COMPANION_LEVEL, 0.6f, 1.3f);

		if (level() instanceof ServerLevel server) {
			server.sendParticles(ParticleTypes.HEART, getX(), getY() + getBbHeight() + 0.2, getZ(), 3, 0.3, 0.2, 0.3, 0.0);
			server.sendParticles(ParticleTypes.WAX_ON, getX(), getY() + getBbHeight() * 0.6, getZ(), 12, 0.4, 0.4, 0.4, 0.05);
		}

		player.sendOverlayMessage(Component.translatable("jtechstorage.treat.fed", getDisplayName(), (xpBoost + 1199) / 1200)
				.withStyle(ChatFormatting.GOLD));
	}

	/** Shared experience from the owner - doubled with a treat (the bonus comes on top, not from the player). */
	public void addSharedXp(int amount) {
		addXp(xpBoost > 0 ? amount * 2 : amount);
	}

	public int getXpBoostTicks() {
		return xpBoost;
	}

	/** Follow <-> stay (right-click or key G). */
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
	// Riding (v0.25.4): mantis and raven at tier 3
	// ------------------------------------------------------------------

	/** Can it currently be ridden? */
	protected boolean canBeRidden() {
		return false;
	}

	/** Seat (blocks, relative, before rotation). */
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
		return seat().yRot(-getYRot() * Mth.DEG_TO_RAD);
	}

	@Override
	protected void tickRidden(Player player, Vec3 input) {
		super.tickRidden(player, input);
		setRot(player.getYRot(), player.getXRot() * 0.5f);
		yRotO = getYRot();
		yBodyRot = getYRot();
		yHeadRot = getYRot();
	}

	/** Species-specific interaction (raven: bag, storage key) - null = none. */
	protected @Nullable InteractionResult interactSpecial(Player player, ItemStack held) {
		return null;
	}

	/** Status in chat: tier, level, experience to the next level and to the next tier. */
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
	// Vanilla tame-animal stuff that does not apply to companions
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
	// Saving
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
		output.putInt("Look", getLook());
		output.putInt("GlowColor", getGlowColor());
		output.putInt("Kills", kills);
		output.putInt("Bond", getBond());
		output.putLong("BondDay", bondDay);
		output.putInt("BondFed", fedToday);
		output.putBoolean("BondFavorite", favoriteToday);
		output.putInt("BondPets", petsToday);
		output.putInt("BondPlays", playsToday);
		output.putLong("BondLastPet", lastPet);
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
		entityData.set(DATA_LOOK, input.getIntOr("Look", CompanionLooks.DEFAULT));
		entityData.set(DATA_GLOW, input.getIntOr("GlowColor", CompanionLooks.GLOW_DEFAULT));
		kills = input.getIntOr("Kills", 0);
		entityData.set(DATA_BOND, Math.clamp(input.getIntOr("Bond", 0), 0, CompanionBond.MAX));
		bondDay = input.getLongOr("BondDay", -1L);
		fedToday = input.getIntOr("BondFed", 0);
		favoriteToday = input.getBooleanOr("BondFavorite", false);
		petsToday = input.getIntOr("BondPets", 0);
		playsToday = input.getIntOr("BondPlays", 0);
		lastPet = input.getLongOr("BondLastPet", -100000L);

		stance = Math.floorMod(input.getIntOr("Stance", STANCE_AGGRESSIVE), 3);
		float health = input.getFloatOr("Health", getHealth());
		refreshStage();
		setHealth(Math.min(getMaxHealth(), health));     // set health only after the talent bonuses (otherwise capped)
	}

	/** Exhausted: does not fight or work, trudges slowly behind. */
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
