package dev.bartels.jtechstorage.entity;

import dev.bartels.jtechstorage.ModSounds;
import dev.bartels.jtechstorage.api.JtsHooks;
import java.util.UUID;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
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

/**
 * Hologram of Jbartels, the mod's inventor. Appears the first time a world is entered (or via
 * Shift + right-click with the guide book) and guides through the mod: speech bubbles above the head, plus every helper mob
 * as a hologram next to him. Right-click = next, Shift + right-click = skip. Never saved.
 */
public class JbartelsHologram extends PathfinderMob {
	private static final EntityDataAccessor<Integer> DATA_STEP = SynchedEntityData.defineId(JbartelsHologram.class, EntityDataSerializers.INT);
	/** v0.35: Milestone appearance – empty = tour, otherwise the milestone ID (speech bubble "jtechstorage.milestone.&lt;id&gt;"). */
	private static final EntityDataAccessor<String> DATA_MILESTONE = SynchedEntityData.defineId(JbartelsHologram.class, EntityDataSerializers.STRING);
	/** How long a milestone appearance lasts (ticks). */
	public static final int MILESTONE_TICKS = 170;

	/** One tour step: duration in ticks, item in hand, mob shown. */
	private record Step(int ticks, @Nullable ItemStack held, @Nullable EntityType<?> show) {
	}

	/** Item/mob by ID – if the sub-mod is missing (v0.43), the hand stays empty / nothing is shown. */
	private static @Nullable ItemStack held(String id) {
		Item item = JtsHooks.item(id);
		return item == Items.AIR ? null : new ItemStack(item);
	}

	private static @Nullable EntityType<?> show(String id) {
		return JtsHooks.entity(id);
	}

	private static Step[] steps() {
		return new Step[] {
				new Step(160, new ItemStack(ModContent.GUIDE_BOOK), null),
				new Step(220, new ItemStack(ModContent.STORAGE_TERMINAL_ITEM), null),
				new Step(220, new ItemStack(ModContent.STORAGE_KEY), null),
				new Step(260, new ItemStack(ModContent.CRAFTING_TERMINAL_ITEM), null),
				new Step(260, held("amethyst_draught"), show("amethyst_robot")),
				new Step(200, new ItemStack(ModContent.WIRELESS_TERMINAL), null),
				new Step(220, null, show("amethyst_sprite")),
				new Step(200, null, show("collector_moth")),
				new Step(180, null, show("storage_griffin")),
				new Step(240, null, show("spark_beetle")),
				new Step(220, null, show("chel")),
				new Step(200, null, show("peter_chel_parker")),
				new Step(180, null, show("ashley")),
				new Step(220, held("phoenix_nest"), show("phoenix")),
				new Step(260, held("flight_plan"), show("amethyst_phoenix")),
				new Step(280, held("lair_compass"), show("amethyst_spider")),
				new Step(240, new ItemStack(Items.ENCHANTED_BOOK), show("amethyst_fox")),
				new Step(200, new ItemStack(ModContent.LEATHER_BACKPACK), null),
				new Step(220, new ItemStack(Items.CARVED_PUMPKIN), null),
				new Step(160, new ItemStack(ModContent.GUIDE_BOOK), null),
		};
	}

	/** Number of steps (must match {@link #steps()} and the language files). */
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

	/** Does this hologram (tour or milestone) belong to that player? */
	public boolean isFor(ServerPlayer player) {
		return player.getUUID().equals(viewer);
	}

	/**
	 * Short appearance at a milestone (v0.35): appears diagonally in front of the player, says one sentence (speech bubble and
	 * chat) and disappears again after a few seconds. Right-click sends it away early.
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

		// don't place inside a wall: otherwise right next to the player
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
		hologram.playSound(ModSounds.JBARTELS_APPEAR, 0.8f, 1.05f);
		hologram.playSound(ModSounds.JBARTELS_TALK, 0.8f, 1.0f);
		player.sendSystemMessage(Component.literal("[" + ModGuide.AUTHOR + "] ").withStyle(ChatFormatting.LIGHT_PURPLE)
				.append(Component.translatable("jtechstorage.milestone." + id).withStyle(ChatFormatting.WHITE)));
	}

	/** Starts the tour for a player: the hologram appears a few blocks in front of them. */
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
		hologram.playSound(ModSounds.JBARTELS_APPEAR, 0.9f, 1.0f);
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
				String id = BuiltInRegistries.ENTITY_TYPE.getKey(type).getPath();
				double distance = id.equals("amethyst_spider") ? 4.2
						: id.equals("amethyst_robot") || id.equals("storage_griffin") || id.equals("amethyst_phoenix") ? 3.5 : 2.2;
				Vec3 pos = position().add(side.scale(distance));
				entity.snapTo(pos.x, getY() + (id.equals("amethyst_sprite") || id.equals("collector_moth") ? 0.8 : 0.0),
						pos.z, getYRot(), 0.0f);

				if (entity instanceof Hologrammable hologram) {
					hologram.makeHologram();
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

		playSound(ModSounds.JBARTELS_TALK, 0.8f, 0.95f + random.nextFloat() * 0.1f);
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

		playSound(ModSounds.JBARTELS_VANISH, 0.9f, 1.0f);
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

	/** Looks at the player and stands still. */
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
