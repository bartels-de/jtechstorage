package dev.bartels.jtechstorage.entity;

import java.util.EnumSet;

import org.jspecify.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.Prediction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

import dev.bartels.jtechstorage.ModConfig;
import dev.bartels.jtechstorage.ModSounds;
import dev.bartels.jtechstorage.ModContent;

/**
 * Peter Chel Parker – Chels Heldenform (entsteht, wenn man Chel einen Netherstern gibt).
 * Er bleibt in der Basis (Zuhause = Ort der Verwandlung bzw. des Absetzens), patrouilliert dort und
 * zerschmettert jedes Monster im Umkreis von {@link #GUARD_RADIUS} Blöcken mit seinem Kristall-Kriegshammer –
 * inklusive Schockwelle, die auch Monster in der Nähe trifft. Fliegende Monster holt er mit Kristallwürfen herunter.
 * Spieler und Tiere greift er nie an. Er kann verletzt werden: zum Heilen läuft er zu Ashley und tritt sie, bis er
 * wieder volles Leben hat. Fällt sein Leben auf 0, wird er wieder zu Chel.
 *
 * <ul>
 *     <li>Rechtsklick (leere Hand): Status + Heldenspruch</li>
 *     <li>Rechtsklick mit Diamant: Witz (Chel steckt ja noch in ihm)</li>
 *     <li>Shift + Rechtsklick (leere Hand): einpacken; beim Absetzen wird der neue Ort sein Zuhause</li>
 * </ul>
 */
public class PeterChelParker extends NetworkHelperMob {
	public static final int GUARD_RADIUS = ModConfig.get().helpers.peterGuardRadius;
	public static final int ATTACK_ANIMATION = 12;
	private static final int HERO_LINES = 10;
	private static final double SHOCKWAVE_RADIUS = 3.5;
	private static final float SHOCKWAVE_DAMAGE = 9.0f;
	private static final float KICK_HEAL = 15.0f;
	private static final int THROW_INTERVAL = 25;
	private static final EntityDataAccessor<Integer> DATA_ATTACK = SynchedEntityData.defineId(PeterChelParker.class,
			EntityDataSerializers.INT);

	protected @Nullable BlockPos home;
	private @Nullable Ashley healTarget;
	private int lastJoke = -1;

	public PeterChelParker(EntityType<? extends PeterChelParker> type, Level level) {
		super(type, level);
		setPermanentlyInvulnerable(false);   // der einzige Helfer, der verletzt werden kann
	}

	/** Ein Held macht keinen Feierabend. */
	@Override
	protected boolean canRest() {
		return false;
	}

	/** Will er sich heilen? (Ohne Ziel bei jedem Schaden, im Kampf erst unter 35 % Leben.) */
	public boolean wantsToHeal() {
		return getHealth() < getMaxHealth() && (getTarget() == null || getHealth() < getMaxHealth() * 0.35f);
	}

	@Override
	public Component statusText() {
		return Component.empty().append(super.statusText())
				.append(Component.literal("  ❤ " + Mth.ceil(getHealth()) + "/" + Mth.ceil(getMaxHealth()))
						.withStyle(getHealth() < getMaxHealth() * 0.35f ? ChatFormatting.RED : ChatFormatting.GREEN));
	}

	public static AttributeSupplier.Builder createAttributes() {
		return createMobAttributes()
				.add(Attributes.MAX_HEALTH, 100.0)
				.add(Attributes.MOVEMENT_SPEED, 0.28)
				.add(Attributes.ATTACK_DAMAGE, 20.0)
				.add(Attributes.ATTACK_KNOCKBACK, 1.5)
				.add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
				.add(Attributes.FOLLOW_RANGE, 32.0);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DATA_ATTACK, 0);
	}

	/** Startet die Schlag-Animation. */
	protected void triggerAttackAnimation() {
		entityData.set(DATA_ATTACK, ATTACK_ANIMATION);
	}

	/** Verbleibende Ticks der Hammerschlag-Animation. */
	public int getAttackTicks() {
		return entityData.get(DATA_ATTACK);
	}

	@Override
	protected String unlinkedStatus() {
		return "jtechstorage.peter.status.guarding";
	}

	@Override
	protected String linkAdvancement() {
		return "peter_chel_parker";
	}

	/** Übernimmt Besitzer, Netzwerk und Farbe von Chel; der Ort der Verwandlung wird sein Zuhause. */
	public void inheritFrom(Chel chel, Player player) {
		owner = chel.owner != null ? chel.owner : player.getUUID();
		terminalPos = chel.terminalPos;
		setAccentColor(chel.getAccentColor());
		home = chel.blockPosition();
		setStatus(unlinkedStatus(), GUARD_RADIUS);
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(1, new HealAtAshleyGoal());
		goalSelector.addGoal(2, new CrystalThrowGoal());
		goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.25, true));
		goalSelector.addGoal(4, new ReturnHomeGoal());
		goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.6));
		goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 10.0f));
		goalSelector.addGoal(7, new RandomLookAroundGoal(this));
		targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 5, true, false,
				(target, level) -> target instanceof Enemy && inGuardArea(target)));
	}

	public boolean isInGuardArea(Entity entity) {
		return inGuardArea(entity);
	}

	protected boolean inGuardArea(Entity entity) {
		BlockPos center = home != null ? home : blockPosition();
		return entity.distanceToSqr(Vec3.atCenterOf(center)) <= (GUARD_RADIUS + 4) * (GUARD_RADIUS + 4);
	}

	// ------------------------------------------------------------------
	// Kampf
	// ------------------------------------------------------------------

	@Override
	public boolean doHurtTarget(ServerLevel level, Entity target) {
		boolean hit = super.doHurtTarget(level, target);
		entityData.set(DATA_ATTACK, ATTACK_ANIMATION);

		if (hit) {
			shockwave(level, target);

			if (random.nextInt(4) == 0) {
				sayToNearby(level, Component.translatable("jtechstorage.peter.battle." + random.nextInt(4)));
			}
		}

		return hit;
	}

	/** Schockwelle: trifft alle Monster rund um das Ziel und schleudert sie zurück. */
	private void shockwave(ServerLevel level, Entity center) {
		level.sendParticles(ParticleTypes.EXPLOSION, center.getX(), center.getY() + 0.2, center.getZ(), 1, 0, 0, 0, 0);
		level.sendParticles(ParticleTypes.WITCH, center.getX(), center.getY() + 0.3, center.getZ(), 30, 1.2, 0.2, 1.2, 0.05);
		level.sendParticles(ParticleTypes.END_ROD, center.getX(), center.getY() + 0.5, center.getZ(), 15, 0.8, 0.4, 0.8, 0.1);
		playSound(ModSounds.PETER_ATTACK, 1.2f, 0.9f + random.nextFloat() * 0.2f);

		for (LivingEntity other : level.getEntitiesOfClass(LivingEntity.class, center.getBoundingBox().inflate(SHOCKWAVE_RADIUS),
				entity -> entity != center && entity instanceof Enemy && entity.isAlive())) {
			other.hurtServer(level, damageSources().mobAttack(this), SHOCKWAVE_DAMAGE);
			Vec3 push = other.position().subtract(center.position()).normalize().scale(0.8);
			other.push(push.x, 0.35, push.z);
		}
	}

	@Override
	public void aiStep() {
		super.aiStep();

		if (!(level() instanceof ServerLevel serverLevel)) {
			return;
		}

		int attack = getAttackTicks();

		if (attack > 0) {
			entityData.set(DATA_ATTACK, attack - 1);
		}

		if (home == null) {
			home = blockPosition();
		}

		LivingEntity target = getTarget();

		if (target != null && (!target.isAlive() || !inGuardArea(target))) {
			setTarget(null);
			target = null;
		}

		if (tickCount % 20 == 0) {
			if (wantsToHeal() && healTarget != null) {
				setStatus("jtechstorage.peter.status.healing");
			} else if (target != null) {
				setStatus("jtechstorage.peter.status.fighting", target.getDisplayName());
			} else {
				setStatus("jtechstorage.peter.status.guarding", GUARD_RADIUS);
			}

			// Kern-Kristall pulsiert sichtbar
			serverLevel.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 1.6, getZ(), 1, 0.15, 0.1, 0.15, 0.0);
		}
	}

	/** Leben auf 0: die Kraft ist verbraucht – er wird zu seiner vorherigen Form. */
	@Override
	public void die(net.minecraft.world.damagesource.DamageSource source) {
		if (!(level() instanceof ServerLevel level)) {
			super.die(source);
			return;
		}

		revert(level);
		level.sendParticles(ParticleTypes.POOF, getX(), getY() + 1.0, getZ(), 40, 0.5, 0.8, 0.5, 0.05);
		level.sendParticles(ParticleTypes.WITCH, getX(), getY() + 1.0, getZ(), 30, 0.5, 0.8, 0.5, 0.05);
		playSound(SoundEvents.BEACON_DEACTIVATE, 1.0f, 1.0f);
		Component message = Component.translatable(defeatedKey());

		for (ServerPlayer player : level.players()) {
			if (player.distanceToSqr(this) < 48 * 48) {
				player.sendSystemMessage(message.copy().withStyle(ChatFormatting.GOLD));
			}
		}

		teamBroadcast(dev.bartels.jtechstorage.block.StorageTerminalBlockEntity.Channel.GUARD, message);
		discard();
	}

	/** Übersetzungs-Schlüssel der Meldung, wenn er besiegt wurde. */
	protected String defeatedKey() {
		return "jtechstorage.peter.defeated";
	}

	/** Item, in das er beim Einpacken (Shift + Rechtsklick) verwandelt wird. */
	protected net.minecraft.world.item.Item packedItem() {
		return ModContent.PETER_CHEL_PARKER_ITEM;
	}

	/** Erzeugt die vorherige Form (Peter → Chel). */
	protected void revert(ServerLevel level) {
		Chel chel = ModContent.CHEL.create(level, net.minecraft.world.entity.EntitySpawnReason.CONVERSION);

		if (chel != null) {
			chel.snapTo(getX(), getY(), getZ(), getYRot(), 0.0f);
			chel.owner = owner;
			chel.terminalPos = terminalPos;
			chel.setAccentColor(getAccentColor());
			ItemStack hat = getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD);

			if (!hat.isEmpty()) {
				chel.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, hat.copy());
			}

			level.addFreshEntity(chel);
		}
	}

	// ------------------------------------------------------------------
	// Interaktion
	// ------------------------------------------------------------------

	@Override
	protected InteractionResult interactWith(Player player, InteractionHand hand) {
		ItemStack held = player.getItemInHand(hand);

		if (held.isEmpty() && player.isSecondaryUseActive()) {
			player.getInventory().placeItemBackInInventory(createPackedItem(packedItem(), player),
					Prediction.SERVER_ONLY);
			playSound(SoundEvents.ARMOR_EQUIP_NETHERITE.value(), 1.0f, 0.8f);
			discard();
			return InteractionResult.SUCCESS;
		}

		if (held.isEmpty()) {
			player.sendOverlayMessage(statusText());
			player.sendSystemMessage(Component.translatable("jtechstorage.chel.joke_format", getDisplayName(),
					Component.translatable("jtechstorage.peter.say." + random.nextInt(HERO_LINES))));
			return InteractionResult.SUCCESS;
		}

		if (held.is(ModContent.DRAGON_HEART) && !(this instanceof Cheldrogan) && level() instanceof ServerLevel level) {
			becomeDragon(level, player);
			held.consume(1, player);
			return InteractionResult.SUCCESS;
		}

		if (held.is(Items.DIAMOND) && level() instanceof ServerLevel) {
			int joke = Chel.pickJoke(player, lastJoke, random);
			lastJoke = joke;
			player.sendSystemMessage(Component.translatable("jtechstorage.chel.joke_format", getDisplayName(),
					Component.translatable("jtechstorage.chel.joke." + joke)));

			if (player instanceof ServerPlayer serverPlayer) {
				Chel.recordJoke(serverPlayer, joke);
			}
			playSound(SoundEvents.WITCH_CELEBRATE, 1.0f, 0.9f);
			held.consume(1, player);
			return InteractionResult.SUCCESS;
		}

		return InteractionResult.PASS;
	}

	@Override
	protected InteractionResult mobInteract(Player player, InteractionHand hand) {
		if (level().isClientSide() && player.getItemInHand(hand).is(ModContent.DRAGON_HEART) && !(this instanceof Cheldrogan)) {
			return InteractionResult.SUCCESS;
		}

		return super.mobInteract(player, hand);
	}

	/** Drachenherz: Peter wird zu Cheldrogan. */
	private void becomeDragon(ServerLevel level, Player player) {
		Cheldrogan dragon = ModContent.CHELDROGAN.create(level, net.minecraft.world.entity.EntitySpawnReason.CONVERSION);

		if (dragon == null) {
			return;
		}

		net.minecraft.world.entity.LightningBolt bolt = net.minecraft.world.entity.EntityTypes.LIGHTNING_BOLT.create(level,
				net.minecraft.world.entity.EntitySpawnReason.TRIGGERED);

		if (bolt != null) {
			bolt.snapTo(getX(), getY(), getZ());
			bolt.setVisualOnly(true);
			level.addFreshEntity(bolt);
		}

		dragon.snapTo(getX(), getY(), getZ(), getYRot(), 0.0f);
		dragon.inheritFrom(this);
		level.addFreshEntity(dragon);
		level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, getX(), getY() + 2.0, getZ(), 120, 1.0, 1.6, 1.0, 0.7);
		level.sendParticles(ParticleTypes.REVERSE_PORTAL, getX(), getY() + 2.0, getZ(), 120, 1.4, 2.0, 1.4, 0.2);
		level.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 2.0, getZ(), 60, 0.8, 2.0, 0.8, 0.2);
		dragon.playSound(ModSounds.CHELDROGAN_TRANSFORM, 3.0f, 1.0f);

		for (ServerPlayer listener : level.players()) {
			if (listener == player || listener.distanceToSqr(this) < 48 * 48) {
				listener.sendSystemMessage(Component.translatable("jtechstorage.cheldrogan.transform").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
			}
		}

		if (player instanceof ServerPlayer serverPlayer) {
			dev.bartels.jtechstorage.ModAdvancements.grant(serverPlayer, "cheldrogan");
		}

		discard();
	}

	private void sayToNearby(ServerLevel level, Component text) {
		for (ServerPlayer player : level.players()) {
			if (player.distanceToSqr(this) < 16 * 16) {
				player.sendOverlayMessage(Component.translatable("jtechstorage.chel.says", getDisplayName(), text));
			}
		}
	}

	// ------------------------------------------------------------------
	// Speichern
	// ------------------------------------------------------------------

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);

		if (home != null) {
			output.store("Home", BlockPos.CODEC, home);
		}
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		home = input.read("Home", BlockPos.CODEC).orElse(null);
		setStatus(unlinkedStatus(), GUARD_RADIUS);
	}

	/** Läuft zu Ashley und tritt sie, bis er wieder volles Leben hat. */
	protected class HealAtAshleyGoal extends Goal {
		private int kickCooldown;

		HealAtAshleyGoal() {
			setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
		}

		@Override
		public boolean canUse() {
			if (!wantsToHeal()) {
				return false;
			}

			healTarget = level().getEntitiesOfClass(Ashley.class, getBoundingBox().inflate(48), Ashley::isAlive).stream()
					.min(java.util.Comparator.comparingDouble(PeterChelParker.this::distanceToSqr)).orElse(null);
			return healTarget != null;
		}

		@Override
		public boolean canContinueToUse() {
			return healTarget != null && healTarget.isAlive() && getHealth() < getMaxHealth();
		}

		@Override
		public void stop() {
			healTarget = null;
			getNavigation().stop();
		}

		@Override
		public boolean requiresUpdateEveryTick() {
			return true;
		}

		@Override
		public void tick() {
			if (healTarget == null) {
				return;
			}

			getLookControl().setLookAt(healTarget, 30.0f, 30.0f);

			if (distanceToSqr(healTarget) > 2.2 * 2.2) {
				if (getNavigation().isDone() || tickCount % 10 == 0) {
					getNavigation().moveTo(healTarget, 1.2);
				}

				return;
			}

			getNavigation().stop();

			if (--kickCooldown <= 0) {
				kickCooldown = 15;
				entityData.set(DATA_ATTACK, ATTACK_ANIMATION);
				heal(KICK_HEAL);
				healTarget.onKicked(PeterChelParker.this);

				if (level() instanceof ServerLevel level) {
					level.sendParticles(ParticleTypes.HEART, getX(), getY() + 2.4, getZ(), 2, 0.3, 0.2, 0.3, 0.0);
				}
			}
		}
	}

	/** Kristallwurf gegen Monster, die er nicht erreicht (Phantome, Ghasts, Blazes …). */
	protected class CrystalThrowGoal extends Goal {
		private int cooldown;

		CrystalThrowGoal() {
			setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
		}

		private boolean outOfReach(@Nullable LivingEntity target) {
			return target != null && target.isAlive() && distanceToSqr(target) < 28 * 28
					&& (target.getY() - getY() > 2.5 || target.isNoGravity() && !target.onGround());
		}

		@Override
		public boolean canUse() {
			return outOfReach(getTarget());
		}

		@Override
		public boolean requiresUpdateEveryTick() {
			return true;
		}

		@Override
		public void start() {
			getNavigation().stop();
		}

		@Override
		public void tick() {
			LivingEntity target = getTarget();

			if (target == null) {
				return;
			}

			getLookControl().setLookAt(target, 30.0f, 30.0f);

			if (--cooldown > 0 || !hasLineOfSight(target)) {
				return;
			}

			cooldown = THROW_INTERVAL;
			CrystalShard shard = new CrystalShard(level(), PeterChelParker.this);
			double dx = target.getX() - getX();
			double dy = target.getY(0.5) - shard.getY();
			double dz = target.getZ() - getZ();
			double horizontal = Math.sqrt(dx * dx + dz * dz);
			shard.shoot(dx, dy + horizontal * 0.03, dz, 1.8f, 1.0f);
			level().addFreshEntity(shard);
			entityData.set(DATA_ATTACK, ATTACK_ANIMATION);
			playSound(SoundEvents.AMETHYST_BLOCK_RESONATE, 1.0f, 1.6f);
		}
	}

	/** Kehrt zur Basis zurück, wenn er sich (z. B. bei einer Verfolgung) zu weit entfernt hat. */
	protected class ReturnHomeGoal extends Goal {
		ReturnHomeGoal() {
			setFlags(EnumSet.of(Goal.Flag.MOVE));
		}

		@Override
		public boolean canUse() {
			return home != null && getTarget() == null
					&& distanceToSqr(Vec3.atCenterOf(home)) > (GUARD_RADIUS / 2.0) * (GUARD_RADIUS / 2.0);
		}

		@Override
		public boolean canContinueToUse() {
			return home != null && getTarget() == null && distanceToSqr(Vec3.atCenterOf(home)) > 3 * 3
					&& !getNavigation().isDone();
		}

		@Override
		public void start() {
			BlockPos pos = home;

			if (pos != null) {
				getNavigation().moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 1.0);
				setStatus("jtechstorage.peter.status.returning");
			}
		}
	}

	@Override
	protected net.minecraft.sounds.SoundEvent helperAmbientSound() {
		return isNightGuard() ? ModSounds.PETER_DOZE : ModSounds.PETER_AMBIENT;
	}

	/** Nachtwache: nachts, ohne Ziel und im Stillstand döst er auf seinen Hammer gestützt (rein optisch, er bleibt wachsam). */
	public boolean isNightGuard() {
		long time = Math.floorMod(level().getDefaultClockTime(), 24000L);
		return !isHologram() && time >= 13000L && time <= 23000L && !isAggressive() && getAttackTicks() == 0
				&& getDeltaMovement().horizontalDistanceSqr() < 1.0E-4 && !isVehicle() && !isPassenger();
	}

	@Override
	protected void playStepSound(net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
		playSound(ModSounds.PETER_STEP, 0.5f, 0.9f + random.nextFloat() * 0.2f);
	}

	/** Ziel-Status für den Client (Ruhe-/Lauerpose nur ohne Ziel). */
	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		setAggressive(getTarget() != null);
	}
}
