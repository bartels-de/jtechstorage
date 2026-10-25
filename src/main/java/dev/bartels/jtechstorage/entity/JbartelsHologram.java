package dev.bartels.jtechstorage.entity;

import java.util.UUID;

import org.jspecify.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import dev.bartels.jtechstorage.ModContent;
import dev.bartels.jtechstorage.ModGuide;
import dev.bartels.jtechstorage.end.EndContent;

/**
 * Hologramm von Jbartels, dem Erfinder der Mod. Erscheint beim ersten Betreten einer Welt (oder per
 * Shift + Rechtsklick mit dem Handbuch) und führt durch die Mod: Sprechblasen über dem Kopf, dazu jeder Helfer-Mob
 * als Hologramm neben ihm. Rechtsklick = weiter, Shift + Rechtsklick = überspringen. Wird nie gespeichert.
 */
public class JbartelsHologram extends PathfinderMob {
	private static final EntityDataAccessor<Integer> DATA_STEP = SynchedEntityData.defineId(JbartelsHologram.class, EntityDataSerializers.INT);
	/** v0.35: Meilenstein-Auftritt – leer = Rundgang, sonst die ID des Meilensteins (Sprechblase „jtechstorage.milestone.&lt;id&gt;“). */
	private static final EntityDataAccessor<String> DATA_MILESTONE = SynchedEntityData.defineId(JbartelsHologram.class, EntityDataSerializers.STRING);
	/** Wie lange ein Meilenstein-Auftritt dauert (Ticks). */
	public static final int MILESTONE_TICKS = 170;

	/** Ein Schritt des Rundgangs: Dauer in Ticks, Item in der Hand, gezeigter Mob. */
	private record Step(int ticks, @Nullable ItemStack held, @Nullable EntityType<?> show) {
	}

	private static Step[] steps() {
		return new Step[] {
				new Step(160, new ItemStack(ModContent.GUIDE_BOOK), null),
				new Step(220, new ItemStack(ModContent.STORAGE_TERMINAL_ITEM), null),
				new Step(220, new ItemStack(ModContent.STORAGE_KEY), null),
				new Step(260, new ItemStack(ModContent.CRAFTING_TERMINAL_ITEM), null),
				new Step(260, new ItemStack(ModContent.AMETHYST_DRAUGHT), ModContent.AMETHYST_ROBOT),
				new Step(200, new ItemStack(ModContent.WIRELESS_TERMINAL), null),
				new Step(220, null, ModContent.AMETHYST_SPRITE),
				new Step(200, null, ModContent.COLLECTOR_MOTH),
				new Step(180, null, ModContent.STORAGE_GRIFFIN),
				new Step(240, null, ModContent.SPARK_BEETLE),
				new Step(220, null, ModContent.CHEL),
				new Step(200, null, ModContent.PETER_CHEL_PARKER),
				new Step(180, null, ModContent.ASHLEY),
				new Step(220, new ItemStack(ModContent.PHOENIX_NEST_ITEM), ModContent.PHOENIX),
				new Step(260, new ItemStack(ModContent.FLIGHT_PLAN), ModContent.AMETHYST_PHOENIX),
				new Step(280, new ItemStack(EndContent.LAIR_COMPASS), EndContent.AMETHYST_SPIDER),
				new Step(240, new ItemStack(Items.ENCHANTED_BOOK), ModContent.AMETHYST_FOX),
				new Step(200, new ItemStack(ModContent.LEATHER_BACKPACK), null),
				new Step(220, new ItemStack(Items.CARVED_PUMPKIN), null),
				new Step(160, new ItemStack(ModContent.GUIDE_BOOK), null),
		};
	}

	/** Anzahl der Schritte (muss zu {@link #steps()} und den Sprachdateien passen). */
	public static final int STEP_COUNT = 20;

	private @Nullable UUID viewer;
	private int stepTicks;
	private @Nullable Entity shown;

	public JbartelsHologram(EntityType<? extends JbartelsHologram> type, Level level) {
		super(type, level);
		setNoGravity(true);
		setPermanentlyInvulnerable(true);
		setSilent(true);
		setPersistenceRequired();
	}

	public static AttributeSupplier.Builder createAttributes() {
		return createMobAttributes().add(Attributes.MAX_HEALTH, 20.0).add(Attributes.MOVEMENT_SPEED, 0.0);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DATA_STEP, 0);
		builder.define(DATA_MILESTONE, "");
	}

	public int getStep() {
		return entityData.get(DATA_STEP);
	}

	public String getMilestone() {
		return entityData.get(DATA_MILESTONE);
	}

	public boolean isMilestone() {
		return !getMilestone().isEmpty();
	}

	/** Gehört dieses Hologramm (Rundgang oder Meilenstein) zu dem Spieler? */
	public boolean isFor(ServerPlayer player) {
		return player.getUUID().equals(viewer);
	}

	/**
	 * Kurzer Auftritt bei einem Meilenstein (v0.35): erscheint schräg vor dem Spieler, sagt einen Satz (Sprechblase und
	 * Chat) und verschwindet nach ein paar Sekunden wieder. Rechtsklick schickt es früher weg.
	 */
	public static void milestone(ServerPlayer player, String id) {
		ServerLevel level = player.level();
		JbartelsHologram hologram = ModContent.JBARTELS_HOLOGRAM.create(level, EntitySpawnReason.TRIGGERED);

		if (hologram == null) {
			return;
		}

		Vec3 look = player.getLookAngle();
		Vec3 flat = new Vec3(look.x, 0, look.z);
		flat = flat.lengthSqr() < 1.0e-4 ? new Vec3(0, 0, 1) : flat.normalize();
		Vec3 side = new Vec3(-flat.z, 0, flat.x);
		Vec3 pos = player.position().add(flat.scale(2.6)).add(side.scale(1.2));

		// nicht in eine Wand stellen: sonst direkt neben den Spieler
		if (!level.noCollision(hologram.getType().getSpawnAABB(pos.x, player.getY(), pos.z))) {
			pos = player.position().add(side.scale(1.1));
		}

		hologram.snapTo(pos.x, player.getY(), pos.z, player.getYRot() + 180.0f, 0.0f);
		hologram.viewer = player.getUUID();
		hologram.entityData.set(DATA_MILESTONE, id);
		hologram.setCustomName(Component.literal(ModGuide.AUTHOR).withStyle(ChatFormatting.LIGHT_PURPLE));
		hologram.setCustomNameVisible(true);
		level.addFreshEntity(hologram);
		level.sendParticles(ParticleTypes.END_ROD, pos.x, player.getY() + 1.0, pos.z, 30, 0.35, 0.9, 0.35, 0.04);
		hologram.playSound(dev.bartels.jtechstorage.ModSounds.JBARTELS_APPEAR, 0.8f, 1.05f);
		hologram.playSound(dev.bartels.jtechstorage.ModSounds.JBARTELS_TALK, 0.8f, 1.0f);
		player.sendSystemMessage(Component.literal("[" + ModGuide.AUTHOR + "] ").withStyle(ChatFormatting.LIGHT_PURPLE)
				.append(Component.translatable("jtechstorage.milestone." + id).withStyle(ChatFormatting.WHITE)));
	}

	/** Startet den Rundgang für einen Spieler: Hologramm erscheint ein paar Blöcke vor ihm. */
	public static void start(ServerPlayer player) {
		ServerLevel level = player.level();

		for (JbartelsHologram old : level.getEntitiesOfClass(JbartelsHologram.class, player.getBoundingBox().inflate(64),
				h -> player.getUUID().equals(h.viewer))) {
			old.finish(false);
		}

		JbartelsHologram hologram = ModContent.JBARTELS_HOLOGRAM.create(level, EntitySpawnReason.TRIGGERED);

		if (hologram == null) {
			return;
		}

		Vec3 look = player.getLookAngle();
		Vec3 flat = new Vec3(look.x, 0, look.z).normalize();
		Vec3 pos = player.position().add(flat.scale(3.5));
		hologram.snapTo(pos.x, player.getY(), pos.z, player.getYRot() + 180.0f, 0.0f);
		hologram.viewer = player.getUUID();
		hologram.setCustomName(Component.literal(ModGuide.AUTHOR).withStyle(ChatFormatting.LIGHT_PURPLE));
		hologram.setCustomNameVisible(true);
		level.addFreshEntity(hologram);
		hologram.enterStep(0);
		level.sendParticles(ParticleTypes.END_ROD, pos.x, player.getY() + 1.0, pos.z, 40, 0.4, 1.0, 0.4, 0.05);
		hologram.playSound(dev.bartels.jtechstorage.ModSounds.JBARTELS_APPEAR, 0.9f, 1.0f);
	}

	private @Nullable ServerPlayer viewerPlayer() {
		return viewer != null && level() instanceof ServerLevel level && level.getPlayerByUUID(viewer) instanceof ServerPlayer player
				? player : null;
	}

	private void enterStep(int step) {
		if (!(level() instanceof ServerLevel level)) {
			return;
		}

		Step[] all = steps();

		if (step >= all.length) {
			finish(true);
			return;
		}

		entityData.set(DATA_STEP, step);
		stepTicks = 0;
		Step current = all[step];
		setItemInHand(InteractionHand.MAIN_HAND, current.held() != null ? current.held().copy() : ItemStack.EMPTY);

		if (shown != null) {
			shown.discard();
			shown = null;
		}

		if (current.show() != null) {
			Entity entity = current.show().create(level, EntitySpawnReason.TRIGGERED);

			if (entity != null) {
				Vec3 side = Vec3.directionFromRotation(0, getYRot() - 90.0f);
				EntityType<?> type = current.show();
				double distance = type == EndContent.AMETHYST_SPIDER ? 4.2
						: type == ModContent.AMETHYST_ROBOT || type == ModContent.STORAGE_GRIFFIN || type == ModContent.AMETHYST_PHOENIX ? 3.5 : 2.2;
				Vec3 pos = position().add(side.scale(distance));
				entity.snapTo(pos.x, getY() + (current.show() == ModContent.AMETHYST_SPRITE || current.show() == ModContent.COLLECTOR_MOTH ? 0.8 : 0.0),
						pos.z, getYRot(), 0.0f);

				if (entity instanceof NetworkHelperMob helper) {
					helper.makeHologram();
				} else if (entity instanceof AmethystRobot robot) {
					robot.makeHologram();
				} else if (entity instanceof AmethystSpider spider) {
					spider.makeHologram();
				}

				level.addFreshEntity(entity);
				level.sendParticles(ParticleTypes.END_ROD, pos.x, getY() + 0.8, pos.z, 20, 0.4, 0.6, 0.4, 0.03);
				shown = entity;
			}
		}

		ServerPlayer player = viewerPlayer();

		if (player != null) {
			player.sendSystemMessage(Component.literal("[" + ModGuide.AUTHOR + "] ").withStyle(ChatFormatting.LIGHT_PURPLE)
					.append(Component.translatable("jtechstorage.tutorial.step." + step).withStyle(ChatFormatting.WHITE)));
		}

		playSound(dev.bartels.jtechstorage.ModSounds.JBARTELS_TALK, 0.8f, 0.95f + random.nextFloat() * 0.1f);
	}

	private void finish(boolean completed) {
		if (shown != null) {
			shown.discard();
			shown = null;
		}

		ServerPlayer player = viewerPlayer();

		if (player != null && completed) {
			player.sendSystemMessage(Component.translatable("jtechstorage.tutorial.end").withStyle(ChatFormatting.GRAY));
		}

		if (level() instanceof ServerLevel level) {
			level.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 1.0, getZ(), 30, 0.4, 1.0, 0.4, 0.05);
		}

		playSound(dev.bartels.jtechstorage.ModSounds.JBARTELS_VANISH, 0.9f, 1.0f);
		discard();
	}

	@Override
	public void tick() {
		super.tick();

		if (!(level() instanceof ServerLevel level)) {
			return;
		}

		ServerPlayer player = viewerPlayer();

		if (player == null || player.level() != level || player.distanceToSqr(this) > 48 * 48) {
			finish(false);
			return;
		}

		if (isMilestone()) {
			lookAt(player);

			if (tickCount % 12 == 0) {
				level.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 0.05, getZ(), 1, 0.4, 0.0, 0.4, 0.0);
			}

			if (tickCount >= MILESTONE_TICKS) {
				finish(false);
			}

			return;
		}

		lookAt(player);

		if (tickCount % 15 == 0) {
			level.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 0.05, getZ(), 1, 0.4, 0.0, 0.4, 0.0);
		}

		if (shown != null && !shown.isAlive()) {
			shown = null;
		}

		Step[] all = steps();
		int step = getStep();

		if (++stepTicks >= all[Math.min(step, all.length - 1)].ticks()) {
			enterStep(step + 1);
		}
	}

	/** Schaut den Spieler an und bleibt stehen. */
	private void lookAt(ServerPlayer player) {
		double dx = player.getX() - getX();
		double dz = player.getZ() - getZ();
		float yaw = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90.0f;
		setYRot(yaw);
		yBodyRot = yaw;
		yHeadRot = yaw;
		setDeltaMovement(Vec3.ZERO);
	}

	@Override
	protected InteractionResult mobInteract(Player player, InteractionHand hand) {
		if (level().isClientSide()) {
			return InteractionResult.SUCCESS;
		}

		if (isMilestone()) {
			finish(false);
			return InteractionResult.SUCCESS;
		}

		if (player.isSecondaryUseActive()) {
			player.sendSystemMessage(Component.translatable("jtechstorage.tutorial.skipped").withStyle(ChatFormatting.GRAY));
			finish(false);
		} else {
			enterStep(getStep() + 1);
		}

		return InteractionResult.SUCCESS;
	}

	@Override
	public boolean shouldBeSaved() {
		return false;
	}

	@Override
	public boolean isPushable() {
		return false;
	}

	@Override
	protected void doPush(Entity entity) {
	}

	@Override
	public boolean removeWhenFarAway(double distance) {
		return false;
	}
}
