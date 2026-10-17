package dev.bartels.jtechstorage.entity;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.Prediction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.allay.Allay;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import dev.bartels.jtechstorage.ModConfig;
import dev.bartels.jtechstorage.ModSounds;
import dev.bartels.jtechstorage.ModContent;

/**
 * Wächter-Falter (früher Sammel-Falter – das Einsammeln macht jetzt der Wichtel): nachtaktiv. Er kreist nachts über
 * seinem Zuhause, lässt Monster im Umkreis von {@link #WATCH_RADIUS} Blöcken leuchten (auch durch Wände sichtbar),
 * warnt seinen Besitzer und meldet die Monster an Peter Chel Parker. Tagsüber schläft er.
 *
 * <ul>
 *     <li>Mit dem Schlüssel verknüpfen: dieser Ort wird sein Zuhause</li>
 *     <li>Rechtsklick (leere Hand): Status · Shift + Rechtsklick: einpacken</li>
 * </ul>
 */
public class CollectorMoth extends NetworkHelperMob {
	public static final int WATCH_RADIUS = ModConfig.get().helpers.mothWatchRadius;
	private static final int ALARM_COOLDOWN = 600;

	private @Nullable BlockPos home;
	private int alarmCooldown;
	private int lastSeen;

	public CollectorMoth(EntityType<? extends CollectorMoth> type, Level level) {
		super(type, level);
		this.moveControl = new FlyingMoveControl<>(this, 20, true);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Allay.createAttributes();
	}

	@Override
	protected boolean isNocturnal() {
		return true;
	}

	@Override
	protected String unlinkedStatus() {
		return "jtechstorage.moth.status.watching";
	}

	@Override
	protected String linkAdvancement() {
		return "collector_moth";
	}

	@Override
	protected void onLinked(Player player) {
		home = blockPosition();
		setStatus("jtechstorage.moth.status.watching", 0);
	}

	@Override
	protected PathNavigation createNavigation(Level level) {
		FlyingPathNavigation navigation = new FlyingPathNavigation(this, level);
		navigation.setCanOpenDoors(false);
		navigation.setCanFloat(true);
		return navigation;
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(1, new PatrolGoal());
		goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 6.0f));
	}

	@Override
	public void travel(Vec3 input) {
		travelFlying(input, getSpeed());
	}

	@Override
	protected InteractionResult interactWith(Player player, InteractionHand hand) {
		if (player.getItemInHand(hand).isEmpty() && player.isSecondaryUseActive()) {
			player.getInventory().placeItemBackInInventory(createPackedItem(ModContent.COLLECTOR_MOTH_ITEM, player),
					Prediction.SERVER_ONLY);
			playSound(SoundEvents.AMETHYST_CLUSTER_BREAK, 1.0f, 1.5f);
			discard();
			return InteractionResult.SUCCESS;
		}

		return super.interactWith(player, hand);
	}

	// ------------------------------------------------------------------
	// Wache
	// ------------------------------------------------------------------

	@Override
	public void aiStep() {
		super.aiStep();

		if (!(level() instanceof ServerLevel level) || tickCount % 20 != 0) {
			return;
		}

		if (home == null) {
			home = blockPosition();
		}

		if (alarmCooldown > 0) {
			alarmCooldown -= 20;
		}

		if (isResting()) {
			lastSeen = 0;
			return;
		}

		List<LivingEntity> enemies = level.getEntitiesOfClass(LivingEntity.class, new AABB(home).inflate(WATCH_RADIUS),
				entity -> entity instanceof Enemy && entity.isAlive());

		for (LivingEntity enemy : enemies) {
			enemy.addEffect(new MobEffectInstance(MobEffects.GLOWING, 60, 0, false, false));
		}

		setStatus(enemies.isEmpty() ? "jtechstorage.moth.status.watching" : "jtechstorage.moth.status.spotted", enemies.size());

		if (!enemies.isEmpty()) {
			level.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 0.2, getZ(), 2, 0.1, 0.1, 0.1, 0.01);
			alertPeter(level, enemies);

			if (enemies.size() > lastSeen && alarmCooldown <= 0) {
				alarm(level, enemies.size());
			}
		}

		lastSeen = enemies.size();
	}

	/** Warnt den Besitzer (Glocke + Meldung), wenn neue Monster auftauchen. */
	private void alarm(ServerLevel level, int count) {
		alarmCooldown = ALARM_COOLDOWN;
		playSound(ModSounds.MOTH_ALARM, 1.0f, 1.0f);

		if (owner != null && level.getPlayerByUUID(owner) instanceof ServerPlayer player && player.distanceToSqr(this) < 96 * 96) {
			player.sendOverlayMessage(Component.translatable("jtechstorage.moth.alarm", getDisplayName(), count));
			player.level().playSound(null, player.blockPosition(), ModSounds.MOTH_ALARM, net.minecraft.sounds.SoundSource.NEUTRAL, 0.5f, 1.2f);
		}

		teamBroadcast(dev.bartels.jtechstorage.block.StorageTerminalBlockEntity.Channel.GUARD,
				Component.translatable("jtechstorage.channel.msg.moth_alarm", getDisplayName(), count));
	}

	/** Gibt Peter Chel Parker ein Ziel, wenn er gerade keins hat. */
	private void alertPeter(ServerLevel level, List<LivingEntity> enemies) {
		for (PeterChelParker peter : level.getEntitiesOfClass(PeterChelParker.class, new AABB(home).inflate(48))) {
			if (peter.getTarget() == null && !peter.wantsToHeal()) {
				enemies.stream()
						.filter(peter::isInGuardArea)
						.min(Comparator.comparingDouble(peter::distanceToSqr))
						.ifPresent(peter::setTarget);
			}
		}
	}

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
		setStatus("jtechstorage.moth.status.watching", 0);
	}

	/** Kreist in ein paar Blöcken Höhe über dem Zuhause. */
	private class PatrolGoal extends Goal {
		private float angle;

		PatrolGoal() {
			setFlags(EnumSet.of(Goal.Flag.MOVE));
		}

		@Override
		public boolean canUse() {
			return !level().isClientSide() && home != null;
		}

		@Override
		public boolean canContinueToUse() {
			return canUse();
		}

		@Override
		public void tick() {
			if (home == null || !getNavigation().isDone()) {
				return;
			}

			angle += 0.7f + random.nextFloat() * 0.4f;
			double radius = 5.0 + random.nextDouble() * 3.0;
			double x = home.getX() + 0.5 + Mth.cos(angle) * radius;
			double z = home.getZ() + 0.5 + Mth.sin(angle) * radius;
			double y = home.getY() + 4.0 + random.nextDouble() * 2.0;
			getNavigation().moveTo(x, y, z, 0.8 * speedFactor());
		}
	}

	@Override
	protected net.minecraft.sounds.SoundEvent helperAmbientSound() {
		return ModSounds.MOTH_AMBIENT;
	}
}
