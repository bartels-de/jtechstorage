package dev.bartels.jtechstorage.entity;

import java.util.EnumSet;
import java.util.List;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Prediction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

import dev.bartels.jtechstorage.ModSounds;
import dev.bartels.jtechstorage.block.HelperBedBlock;
import dev.bartels.jtechstorage.block.StorageTerminalBlockEntity;
import dev.bartels.jtechstorage.storm.ResonanceStorm;
import dev.bartels.jtechstorage.wolf.WolfContent;

/**
 * Kristallwolf (v0.26): Wächter und Jäger der Basis. Entsteht, wenn man einem eigenen, gezähmten Wolf einen
 * Amethyst-Kern füttert. Läuft tagsüber Patrouille um sein Zuhause, greift Monster an, treibt ausgebüxte Nutztiere zurück
 * und bringt Monster-Beute ins Lager. Im Rudel (und beim Mondwolf) kämpft er stärker. Nachts schläft er im Wolfskorb –
 * es sei denn, Monster kommen näher oder ein Resonanz-Sturm tobt. Sinkt sein Leben zu tief, zieht er sich erschöpft zurück
 * und erholt sich (er stirbt nicht).
 */
public class CrystalWolf extends NetworkHelperMob {
	private static final EntityDataAccessor<Integer> DATA_BITE = SynchedEntityData.defineId(CrystalWolf.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> DATA_HOWL = SynchedEntityData.defineId(CrystalWolf.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Boolean> DATA_EXHAUSTED = SynchedEntityData.defineId(CrystalWolf.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Boolean> DATA_FOLLOW = SynchedEntityData.defineId(CrystalWolf.class, EntityDataSerializers.BOOLEAN);

	public static final int BITE_TICKS = 8;
	public static final int HOWL_TICKS = 50;
	public static final int GUARD_RADIUS = 20;
	private static final int HERD_RADIUS = 16;
	private static final int EXHAUSTED_TICKS = 20 * 45;
	private static final List<Item> LOOT = List.of(Items.BONE, Items.STRING, Items.ROTTEN_FLESH, Items.GUNPOWDER, Items.SPIDER_EYE,
			Items.ARROW, Items.LEATHER, Items.FEATHER, Items.ENDER_PEARL, Items.SLIME_BALL, Items.PHANTOM_MEMBRANE, Items.BLAZE_ROD,
			Items.AMETHYST_SHARD, Items.AMETHYST_BLOCK);

	protected @Nullable BlockPos home;
	private int exhausted;
	private boolean threatNear;
	private ItemStack carried = ItemStack.EMPTY;

	public CrystalWolf(EntityType<? extends CrystalWolf> type, Level level) {
		super(type, level);
		setPermanentlyInvulnerable(false);   // kämpft wirklich – wird aber nur erschöpft, nie getötet
	}

	public static AttributeSupplier.Builder createAttributes() {
		return createMobAttributes()
				.add(Attributes.MAX_HEALTH, 30.0)
				.add(Attributes.ATTACK_DAMAGE, 5.0)
				.add(Attributes.MOVEMENT_SPEED, 0.32)
				.add(Attributes.ARMOR, 4.0)
				.add(Attributes.FOLLOW_RANGE, 28.0);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DATA_BITE, 0);
		builder.define(DATA_HOWL, 0);
		builder.define(DATA_EXHAUSTED, false);
		builder.define(DATA_FOLLOW, false);
	}

	public int getBiteTicks() {
		return entityData.get(DATA_BITE);
	}

	public int getHowlTicks() {
		return entityData.get(DATA_HOWL);
	}

	public boolean isExhausted() {
		return entityData.get(DATA_EXHAUSTED);
	}

	public boolean isFollowing() {
		return entityData.get(DATA_FOLLOW);
	}

	protected void startHowl() {
		entityData.set(DATA_HOWL, HOWL_TICKS);
		playSound(howlSound(), 2.0f, 0.95f + random.nextFloat() * 0.1f);
	}

	protected SoundEvent howlSound() {
		return ModSounds.CRYSTAL_WOLF_HOWL;
	}

	@Override
	protected String unlinkedStatus() {
		return "jtechstorage.crystal_wolf.status.unlinked";
	}

	@Override
	protected String linkAdvancement() {
		return "crystal_wolf";
	}

	@Override
	protected int bedKind() {
		return HelperBedBlock.KIND_BASKET;
	}

	/** Schläft nicht, solange Monster nah sind oder ein Resonanz-Sturm tobt. */
	@Override
	protected boolean canRest() {
		return !threatNear && !isFollowing() && ResonanceStorm.at(level(), blockPosition()) == null;
	}

	/** Gegen Stürme und Monster wach auch bei Resonanz – Wölfe schlafen ganz normal nachts. */
	@Override
	protected boolean restsEvenWhenCharged() {
		return true;
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(1, new ExhaustedGoal());
		goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.35, true));
		goalSelector.addGoal(3, new FollowOwnerGoal());
		goalSelector.addGoal(4, new DeliverGoal());
		goalSelector.addGoal(5, new HerdGoal());
		goalSelector.addGoal(6, new PatrolGoal());
		goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 10.0f));
		goalSelector.addGoal(8, new RandomLookAroundGoal(this));
		targetSelector.addGoal(1, new HurtByTargetGoal(this, Player.class));
		targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 5, true, false,
				(target, level) -> target instanceof Enemy && !(target instanceof NetworkHelperMob) && guards(target)));
	}

	/** Bewacht dieses Gebiet? (Zuhause, oder beim Begleiten der Besitzer) */
	protected boolean guards(Entity entity) {
		if (isExhausted()) {
			return false;
		}

		if (isFollowing()) {
			return entity.distanceToSqr(this) <= 16 * 16;
		}

		BlockPos center = home();
		return entity.distanceToSqr(Vec3.atCenterOf(center)) <= (GUARD_RADIUS + 4) * (GUARD_RADIUS + 4);
	}

	protected BlockPos home() {
		if (terminalPos != null) {
			return terminalPos;
		}

		return home != null ? home : blockPosition();
	}

	// ------------------------------------------------------------------
	// Ablauf
	// ------------------------------------------------------------------

	@Override
	public void aiStep() {
		super.aiStep();

		if (!(level() instanceof ServerLevel level) || isHologram()) {
			if (level().isClientSide() && !isHologram() && !isResting() && random.nextInt(14) == 0) {
				level().addParticle(ParticleTypes.WITCH, getRandomX(0.6), getY() + 0.9 + random.nextDouble() * 0.3, getRandomZ(0.6), 0, 0.01, 0);
			}
			return;
		}

		if (home == null) {
			home = blockPosition();
		}

		tickTimer(DATA_BITE);
		tickTimer(DATA_HOWL);

		if (exhausted > 0 && --exhausted == 0) {
			entityData.set(DATA_EXHAUSTED, false);
		}

		// Erholung: langsam ohne Kampf, schneller wenn erschöpft oder schlafend
		if (getHealth() < getMaxHealth() && getTarget() == null && tickCount % (isExhausted() || isResting() ? 20 : 60) == 0) {
			heal(1.0f);
		}

		if (tickCount % 20 == 3) {
			threatNear = !level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(10),
					e -> e instanceof Enemy && e.isAlive() && !(e instanceof NetworkHelperMob)).isEmpty();
			updateStatus();
		}

		// nachts gelegentlich heulen (Mondwolf: bei Vollmond)
		if (!isResting() && level.isDarkOutside() && getHowlTicks() == 0 && getTarget() == null && random.nextInt(howlChance()) == 0) {
			startHowl();
		}
	}

	protected int howlChance() {
		return 6000;
	}

	private void tickTimer(EntityDataAccessor<Integer> data) {
		int value = entityData.get(data);

		if (value > 0) {
			entityData.set(data, value - 1);
		}
	}

	private void updateStatus() {
		if (isExhausted()) {
			setStatus("jtechstorage.crystal_wolf.status.exhausted");
		} else if (getTarget() != null) {
			setStatus("jtechstorage.crystal_wolf.status.fighting");
		} else if (isFollowing()) {
			setStatus("jtechstorage.crystal_wolf.status.following");
		} else if (terminalPos == null) {
			setStatus(unlinkedStatus());
		} else {
			setStatus("jtechstorage.crystal_wolf.status.patrol", packSize());
		}
	}

	/** Anzahl der Rudelmitglieder (andere Kristallwölfe) in der Nähe. */
	public int packSize() {
		return level().getEntitiesOfClass(CrystalWolf.class, getBoundingBox().inflate(12),
				w -> w != this && !(w instanceof MoonWolf) && !w.isHologram()).size();
	}

	/** Führt ein Mondwolf in der Nähe das Rudel an? */
	protected boolean alphaNear() {
		return !level().getEntitiesOfClass(MoonWolf.class, getBoundingBox().inflate(16), w -> w != this && !w.isWild()).isEmpty();
	}

	@Override
	public boolean doHurtTarget(ServerLevel level, Entity target) {
		float base = (float) getAttributeValue(Attributes.ATTACK_DAMAGE);
		float bonus = Math.min(3, packSize());   // Rudel-Bonus
		float damage = (base + bonus) * (alphaNear() ? 1.5f : 1.0f);
		boolean hit = target.hurtServer(level, damageSources().mobAttack(this), damage);
		entityData.set(DATA_BITE, BITE_TICKS);
		playSound(ModSounds.CRYSTAL_WOLF_GROWL, 1.0f, 0.9f + random.nextFloat() * 0.2f);

		if (hit) {
			level.sendParticles(ParticleTypes.WITCH, target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(), 8, 0.3, 0.3, 0.3, 0.02);
		}

		return hit;
	}

	/** Stirbt nicht: fällt das Leben zu tief, zieht er sich erschöpft zurück. */
	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		if (isHologram() || source.getEntity() instanceof NetworkHelperMob) {
			return false;
		}

		if (source.getEntity() instanceof Player) {
			return false;   // Spieler können ihn nicht verletzen (Wildlinge: siehe Mondwolf)
		}

		if (getHealth() - amount < 2.0f && !source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) {
			setHealth(2.0f);
			exhaust();
			return true;
		}

		return super.hurtServer(level, source, amount);
	}

	private void exhaust() {
		exhausted = EXHAUSTED_TICKS;
		entityData.set(DATA_EXHAUSTED, true);
		setTarget(null);
		playSound(ModSounds.CRYSTAL_WOLF_HURT, 1.0f, 1.3f);
	}

	// ------------------------------------------------------------------
	// Interaktion
	// ------------------------------------------------------------------

	@Override
	protected InteractionResult interactWith(Player player, InteractionHand hand) {
		ItemStack held = player.getItemInHand(hand);

		if (held.isEmpty() && player.isSecondaryUseActive() && hand == InteractionHand.MAIN_HAND) {
			if (!carried.isEmpty()) {
				spawnAtLocation((ServerLevel) level(), carried);
				carried = ItemStack.EMPTY;
			}
			player.getInventory().placeItemBackInInventory(createPackedItem(packedItem(), player), Prediction.SERVER_ONLY);
			playSound(ModSounds.CRYSTAL_WOLF_AMBIENT, 1.0f, 1.2f);
			discard();
			return InteractionResult.SUCCESS;
		}

		// Knochen: Begleiten / Zuhause bewachen
		if (held.is(Items.BONE)) {
			boolean follow = !isFollowing();
			entityData.set(DATA_FOLLOW, follow);
			getNavigation().stop();

			if (!follow) {
				home = blockPosition();
			}

			player.sendOverlayMessage(Component.translatable(follow ? "jtechstorage.crystal_wolf.follow" : "jtechstorage.crystal_wolf.stay", getDisplayName()));
			playSound(ModSounds.CRYSTAL_WOLF_AMBIENT, 1.0f, follow ? 1.2f : 0.9f);
			updateStatus();
			return InteractionResult.SUCCESS;
		}

		// Fleisch: heilen
		if (held.is(ItemTags.WOLF_FOOD) && getHealth() < getMaxHealth()) {
			heal(8.0f);
			held.consume(1, player);
			playSound(net.minecraft.sounds.SoundEvents.GENERIC_EAT.value(), 1.0f, 1.0f);
			((ServerLevel) level()).sendParticles(ParticleTypes.HEART, getX(), getY() + 1.0, getZ(), 3, 0.3, 0.2, 0.3, 0.0);
			return InteractionResult.SUCCESS;
		}

		return super.interactWith(player, hand);
	}

	protected Item packedItem() {
		return WolfContent.CRYSTAL_WOLF_ITEM;
	}

	@Override
	protected void onLinked(Player player) {
		home = blockPosition();
		entityData.set(DATA_FOLLOW, false);
	}

	// ------------------------------------------------------------------
	// Sounds
	// ------------------------------------------------------------------

	@Override
	protected @Nullable SoundEvent helperAmbientSound() {
		return getTarget() != null ? ModSounds.CRYSTAL_WOLF_GROWL : ModSounds.CRYSTAL_WOLF_AMBIENT;
	}

	@Override
	protected @Nullable SoundEvent helperSleepSound() {
		return ModSounds.CRYSTAL_WOLF_SLEEP;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return ModSounds.CRYSTAL_WOLF_HURT;
	}

	@Override
	protected void playStepSound(BlockPos pos, BlockState state) {
		playSound(ModSounds.CRYSTAL_WOLF_STEP, 0.35f, 0.9f + random.nextFloat() * 0.2f);
	}

	@Override
	public int getAmbientSoundInterval() {
		return 400;
	}

	// ------------------------------------------------------------------
	// Speichern
	// ------------------------------------------------------------------

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putBoolean("Follow", isFollowing());
		output.putInt("Exhausted", exhausted);

		if (home != null) {
			output.store("Home", BlockPos.CODEC, home);
		}

		if (!carried.isEmpty()) {
			output.store("Carried", ItemStack.CODEC, carried);
		}
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		entityData.set(DATA_FOLLOW, input.getBooleanOr("Follow", false));
		exhausted = input.getIntOr("Exhausted", 0);
		entityData.set(DATA_EXHAUSTED, exhausted > 0);
		home = input.read("Home", BlockPos.CODEC).orElse(null);
		carried = input.read("Carried", ItemStack.CODEC).orElse(ItemStack.EMPTY);
	}

	// ------------------------------------------------------------------
	// Ziele
	// ------------------------------------------------------------------

	/** Erschöpft: zurück zum Zuhause (Korb) und ausruhen. */
	private class ExhaustedGoal extends Goal {
		ExhaustedGoal() {
			setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
		}

		@Override
		public boolean canUse() {
			return isExhausted();
		}

		@Override
		public void start() {
			BlockPos bed = findBed();
			BlockPos target = bed != null ? bed : home();
			getNavigation().moveTo(target.getX() + 0.5, target.getY(), target.getZ() + 0.5, 0.8);
		}

		@Override
		public void tick() {
			if (getNavigation().isDone()) {
				getNavigation().stop();
			}
		}
	}

	/** Begleitet seinen Besitzer (Knochen-Modus). */
	private class FollowOwnerGoal extends Goal {
		private @Nullable Player player;

		FollowOwnerGoal() {
			setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
		}

		@Override
		public boolean canUse() {
			if (!isFollowing() || owner == null || isResting()) {
				return false;
			}

			player = level().getPlayerByUUID(owner);
			return player != null && !player.isSpectator() && distanceToSqr(player) > 4 * 4;
		}

		@Override
		public boolean canContinueToUse() {
			return player != null && isFollowing() && distanceToSqr(player) > 2.5 * 2.5 && getTarget() == null;
		}

		@Override
		public void tick() {
			if (player == null) {
				return;
			}

			getLookControl().setLookAt(player, 10.0f, getMaxHeadXRot());

			if (distanceToSqr(player) > 24 * 24 && player.onGround()) {
				// zu weit weg: hinterherspringen wie ein zahmer Wolf
				snapTo(player.getX() - player.getLookAngle().x * 1.5, player.getY(), player.getZ() - player.getLookAngle().z * 1.5, getYRot(), getXRot());
				getNavigation().stop();
			} else if (tickCount % 10 == 0) {
				getNavigation().moveTo(player, 1.25);
			}
		}

		@Override
		public void stop() {
			player = null;
			getNavigation().stop();
		}
	}

	/** Sammelt Monster-Beute in der Nähe auf und bringt sie zum Terminal ins Lager. */
	private class DeliverGoal extends Goal {
		private @Nullable ItemEntity item;
		private int ticks;

		DeliverGoal() {
			setFlags(EnumSet.of(Flag.MOVE));
		}

		@Override
		public boolean canUse() {
			if (terminalPos == null || isFollowing() || isResting() || getTarget() != null || isExhausted()) {
				return false;
			}

			if (!carried.isEmpty()) {
				return true;
			}

			if (tickCount % 40 != 0) {
				return false;
			}

			List<ItemEntity> items = level().getEntitiesOfClass(ItemEntity.class, getBoundingBox().inflate(12),
					e -> !e.hasPickUpDelay() && LOOT.contains(e.getItem().getItem())
							&& e.distanceToSqr(Vec3.atCenterOf(home())) <= (GUARD_RADIUS + 6) * (GUARD_RADIUS + 6));
			item = items.isEmpty() ? null : items.get(0);
			return item != null;
		}

		@Override
		public boolean canContinueToUse() {
			return ticks > 0 && !isResting() && getTarget() == null && (item != null && item.isAlive() || !carried.isEmpty());
		}

		@Override
		public void start() {
			ticks = 400;
		}

		@Override
		public void tick() {
			ticks--;

			if (carried.isEmpty()) {
				if (item == null || !item.isAlive()) {
					ticks = 0;
					return;
				}

				if (distanceToSqr(item) < 1.6 * 1.6) {
					carried = item.getItem().copy();
					item.discard();
					playSound(net.minecraft.sounds.SoundEvents.ITEM_PICKUP, 0.5f, 0.9f);
				} else if (tickCount % 10 == 0) {
					getNavigation().moveTo(item, 1.15);
				}
				return;
			}

			BlockPos terminal = terminalPos;

			if (terminal == null) {
				ticks = 0;
				return;
			}

			if (terminal.distToCenterSqr(position()) < 3.0 * 3.0) {
				StorageTerminalBlockEntity entity = terminal();
				ItemStack rest = entity != null ? entity.buildNetwork().insert(carried) : carried;

				if (!rest.isEmpty()) {
					spawnAtLocation((ServerLevel) level(), rest);
				}

				carried = ItemStack.EMPTY;
				playSound(net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_CHIME, 0.6f, 1.4f);
				ticks = 0;
			} else if (tickCount % 10 == 0) {
				getNavigation().moveTo(terminal.getX() + 0.5, terminal.getY(), terminal.getZ() + 0.5, 1.1);
			}
		}

		@Override
		public void stop() {
			item = null;
			getNavigation().stop();
		}
	}

	/** Treibt ausgebüxte Nutztiere zurück ins Zuhause-Gebiet. */
	private class HerdGoal extends Goal {
		private @Nullable Animal animal;
		private int ticks;

		HerdGoal() {
			setFlags(EnumSet.of(Flag.MOVE));
		}

		@Override
		public boolean canUse() {
			if (isFollowing() || isResting() || getTarget() != null || isExhausted() || tickCount % 60 != 0) {
				return false;
			}

			Vec3 center = Vec3.atCenterOf(home());
			List<Animal> strays = level().getEntitiesOfClass(Animal.class, getBoundingBox().inflate(24),
					a -> !(a instanceof TamableAnimal) && a.isAlive() && !a.isLeashed()
							&& a.distanceToSqr(center) > HERD_RADIUS * HERD_RADIUS && a.distanceToSqr(center) < (HERD_RADIUS + 16) * (HERD_RADIUS + 16));
			animal = strays.isEmpty() ? null : strays.get(random.nextInt(strays.size()));
			return animal != null;
		}

		@Override
		public boolean canContinueToUse() {
			return animal != null && animal.isAlive() && ticks > 0 && getTarget() == null && !isResting()
					&& animal.distanceToSqr(Vec3.atCenterOf(home())) > (HERD_RADIUS - 4) * (HERD_RADIUS - 4);
		}

		@Override
		public void start() {
			ticks = 300;
		}

		@Override
		public void tick() {
			ticks--;

			if (animal == null) {
				return;
			}

			BlockPos center = home();
			Vec3 toHome = Vec3.atCenterOf(center).subtract(animal.position()).normalize();
			// hinter das Tier laufen (von der Basis aus gesehen) und es Richtung Zuhause scheuchen
			Vec3 behind = animal.position().subtract(toHome.scale(2.5));

			if (tickCount % 10 == 0) {
				getNavigation().moveTo(behind.x, behind.y, behind.z, 1.2);
			}

			if (distanceToSqr(animal) < 4 * 4 && tickCount % 20 == 0) {
				Vec3 goal = animal.position().add(toHome.scale(6));
				animal.getNavigation().moveTo(goal.x, goal.y, goal.z, 1.3);

				if (random.nextInt(3) == 0) {
					playSound(ModSounds.CRYSTAL_WOLF_AMBIENT, 0.7f, 1.3f);
				}
			}
		}

		@Override
		public void stop() {
			animal = null;
			getNavigation().stop();
		}
	}

	/** Patrouille im Umkreis um das Zuhause. */
	private class PatrolGoal extends Goal {
		private int cooldown;

		PatrolGoal() {
			setFlags(EnumSet.of(Flag.MOVE));
		}

		@Override
		public boolean canUse() {
			if (isResting() || isFollowing() || isExhausted() || --cooldown > 0) {
				return false;
			}

			cooldown = 60 + random.nextInt(100);
			return true;
		}

		@Override
		public boolean canContinueToUse() {
			return !getNavigation().isDone() && !isResting() && getTarget() == null;
		}

		@Override
		public void start() {
			BlockPos center = home();
			double angle = random.nextDouble() * Math.PI * 2;
			double radius = 4 + random.nextDouble() * (GUARD_RADIUS - 6);
			getNavigation().moveTo(center.getX() + 0.5 + Math.cos(angle) * radius, center.getY(), center.getZ() + 0.5 + Math.sin(angle) * radius, 0.85);
		}
	}

	/** Für Status-Nachrichten an den Besitzer. */
	protected @Nullable ServerPlayer ownerPlayer(ServerLevel level) {
		return owner == null ? null : level.getServer().getPlayerList().getPlayer(owner);
	}
}
