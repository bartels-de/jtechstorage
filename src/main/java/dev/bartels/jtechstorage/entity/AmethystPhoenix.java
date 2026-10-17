package dev.bartels.jtechstorage.entity;

import java.util.List;

import org.jspecify.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.Prediction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemStackWithSlot;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;

import dev.bartels.jtechstorage.ModConfig;
import dev.bartels.jtechstorage.ModSounds;
import dev.bartels.jtechstorage.ModContent;
import dev.bartels.jtechstorage.menu.AmethystPhoenixMenu;

/**
 * Amethyst-Phönix – die Entwicklungsstufe des Phönix (Rechtsklick mit einem Amethyst-Obsidian-Upgrade).
 * Groß, zwei Sitze für Spieler, bis zu vier ausrüstbare Kisten. Mit einer Route-Feder bekommt er zwei Haltestellen
 * und pendelt dann selbstständig: Flug zur Haltestelle, dort eine ganze Nacht Rast, bei Sonnenaufgang zurück.
 * Wer vorne sitzt, kann mit der Sprungtaste früher starten. Je mehr Kisten und Ladung, desto langsamer fliegt er.
 * Im Sitz-Modus bewegt er sich nicht.
 */
public class AmethystPhoenix extends NetworkHelperMob {
	public static final int CHEST_SLOTS = 27;
	public static final int MAX_CHESTS = 4;
	public static final int STATE_IDLE = 0;
	public static final int STATE_FLYING = 1;
	public static final int STATE_RESTING = 2;
	/** Reisegeschwindigkeit ohne Last (Blöcke pro Tick). */
	private static final double BASE_SPEED = ModConfig.get().helpers.amethystPhoenixBlocksPerSecond / 20.0;
	private static final double CRUISE_HEIGHT = 18.0;

	private static final EntityDataAccessor<Integer> DATA_STATE = SynchedEntityData.defineId(AmethystPhoenix.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> DATA_CHESTS = SynchedEntityData.defineId(AmethystPhoenix.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Boolean> DATA_SITTING = SynchedEntityData.defineId(AmethystPhoenix.class, EntityDataSerializers.BOOLEAN);

	private final SimpleContainer cargo = new SimpleContainer(CHEST_SLOTS * MAX_CHESTS) {
		@Override
		public void setChanged() {
			super.setChanged();
			updateLoad();
		}
	};
	private @Nullable BlockPos stopA;
	private @Nullable BlockPos stopB;
	/** Ziel des aktuellen bzw. nächsten Flugs: 0 = A, 1 = B. */
	private int target;
	private long departAt;
	private double cruiseY;
	private double speedFactor = 1.0;
	private int fillPercent;

	public AmethystPhoenix(EntityType<? extends AmethystPhoenix> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return createMobAttributes()
				.add(Attributes.MAX_HEALTH, 80.0)
				.add(Attributes.MOVEMENT_SPEED, 0.18)
				.add(Attributes.FLYING_SPEED, 0.3)
				.add(Attributes.FOLLOW_RANGE, 16.0)
				.add(Attributes.STEP_HEIGHT, 1.0);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DATA_STATE, STATE_IDLE);
		builder.define(DATA_CHESTS, 0);
		builder.define(DATA_SITTING, false);
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 8.0f) {
			@Override
			public boolean canUse() {
				return getState() != STATE_FLYING && super.canUse();
			}
		});
	}

	// ------------------------------------------------------------------
	// Zustand
	// ------------------------------------------------------------------

	public int getState() {
		return entityData.get(DATA_STATE);
	}

	public boolean isFlying() {
		return getState() == STATE_FLYING && !isSitting();
	}

	public int getChests() {
		return entityData.get(DATA_CHESTS);
	}

	public boolean isSitting() {
		return entityData.get(DATA_SITTING);
	}

	public SimpleContainer getCargo() {
		return cargo;
	}

	public @Nullable BlockPos getStop(int index) {
		return index == 0 ? stopA : stopB;
	}

	public int getTargetStop() {
		return target;
	}

	/** Tempo in Prozent (100 = ohne Last). */
	public int getSpeedPercent() {
		return (int) Math.round(speedFactor * 100);
	}

	public int getFillPercent() {
		return fillPercent;
	}

	/** Minuten bis zum Abflug (nur während der Rast), sonst -1. */
	public int getMinutesUntilDeparture() {
		if (getState() != STATE_RESTING || isSitting()) {
			return -1;
		}

		return (int) Math.max(0, (departAt - level().getDefaultClockTime() + 1199) / 1200);
	}

	private void setState(int state) {
		entityData.set(DATA_STATE, state);
		setNoGravity(state == STATE_FLYING);
	}

	public void setSitting(boolean sitting) {
		entityData.set(DATA_SITTING, sitting);

		if (sitting) {
			setNoGravity(false);
			getNavigation().stop();
		} else {
			setNoGravity(getState() == STATE_FLYING);
			cruiseY = getY();
		}
	}

	@Override
	protected boolean canRest() {
		return false;
	}

	@Override
	protected String unlinkedStatus() {
		return "jtechstorage.amethyst_phoenix.status.no_route";
	}

	@Override
	protected String linkAdvancement() {
		return "phoenix";
	}

	/** Tempo-Faktor aus Kistenzahl und Füllstand. */
	private void updateLoad() {
		int chests = getChests();
		double units = 0;

		for (int slot = 0; slot < chests * CHEST_SLOTS; slot++) {
			ItemStack stack = cargo.getItem(slot);

			if (!stack.isEmpty()) {
				units += (double) stack.getCount() / stack.getMaxStackSize();
			}
		}

		double fill = chests == 0 ? 0.0 : units / (chests * CHEST_SLOTS);
		fillPercent = (int) Math.round(fill * 100);
		speedFactor = Math.max(0.35, 1.0 - 0.08 * chests - 0.3 * fill);
	}

	// ------------------------------------------------------------------
	// Route
	// ------------------------------------------------------------------

	/** Neue Route von der Route-Feder. Er fliegt zuerst zu A (ist er schon dort, rastet er gleich). */
	public void setRoute(BlockPos a, BlockPos b) {
		stopA = a.immutable();
		stopB = b.immutable();
		target = 0;

		if (position().distanceTo(landing(stopA)) < 8.0) {
			arrive();
		} else {
			takeOff();
		}
	}

	private static Vec3 landing(BlockPos stop) {
		return new Vec3(stop.getX() + 0.5, stop.getY() + 1.0, stop.getZ() + 0.5);
	}

	private void takeOff() {
		setState(STATE_FLYING);
		cruiseY = getY();
		playSound(ModSounds.AMETHYST_PHOENIX_DEPART, 1.5f, 1.0f);
	}

	/** Abflug von der Haltestelle, an der er gerade rastet. */
	public boolean departNow() {
		if (getState() != STATE_RESTING || isSitting() || stopA == null || stopB == null) {
			return false;
		}

		target = 1 - target;
		takeOff();
		tellPassengers(Component.translatable("jtechstorage.amethyst_phoenix.depart", stopName(target)));

		if (level() instanceof ServerLevel level) {
			level.sendParticles(ParticleTypes.FLAME, getX(), getY() + 0.5, getZ(), 30, 0.8, 0.3, 0.8, 0.05);
			level.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 0.5, getZ(), 15, 0.8, 0.3, 0.8, 0.05);
		}

		return true;
	}

	private void arrive() {
		setState(STATE_RESTING);
		setDeltaMovement(Vec3.ZERO);
		departAt = nextDeparture(level().getDefaultClockTime());
		playSound(SoundEvents.AMETHYST_BLOCK_CHIME, 1.2f, 0.8f);
		tellPassengers(Component.translatable("jtechstorage.amethyst_phoenix.arrived", stopName(target)));
	}

	/** Nächster Sonnenaufgang, nachdem eine ganze Nacht (13000–24000) vergangen ist. */
	static long nextDeparture(long time) {
		long day = Math.floorDiv(time, 24000L);
		long nightStart = day * 24000L + 13000L;

		if (Math.floorMod(time, 24000L) >= 13000L) {
			nightStart += 24000L;
		}

		return nightStart + 11000L;
	}

	private Component stopName(int index) {
		BlockPos stop = getStop(index);
		String letter = index == 0 ? "A" : "B";
		return stop == null ? Component.literal(letter)
				: Component.translatable("jtechstorage.amethyst_phoenix.stop", letter, stop.getX(), stop.getY(), stop.getZ());
	}

	private void tellPassengers(Component message) {
		for (Entity passenger : getPassengers()) {
			if (passenger instanceof Player player) {
				player.sendSystemMessage(message.copy().withStyle(ChatFormatting.LIGHT_PURPLE));
			}
		}
	}

	// ------------------------------------------------------------------
	// Ablauf
	// ------------------------------------------------------------------

	@Override
	public void aiStep() {
		if (level() instanceof ServerLevel level && !isHologram()) {
			serverMovement(level);
		}

		super.aiStep();

		if (level() instanceof ServerLevel level && !isHologram() && tickCount % 20 == 3) {
			updateStatus();

			// Chunks entlang der Route geladen halten (wie bei einer Enderperle), damit er auch ohne Spieler weiterpendelt
			if (stopA != null && !isSitting()) {
				level.getChunkSource().addTicketWithRadius(TicketType.ENDER_PEARL, ChunkPos.containing(blockPosition()), 2);
			}
		}

		if (level().isClientSide() && isFlying() && tickCount % 3 == 0) {
			level().addParticle(ParticleTypes.FLAME, getX() - Mth.sin(yBodyRot * Mth.DEG_TO_RAD) * -1.4, getY() + 0.8,
					getZ() + Mth.cos(yBodyRot * Mth.DEG_TO_RAD) * -1.4, 0.0, -0.02, 0.0);
		}
	}

	private void serverMovement(ServerLevel level) {
		if (isSitting()) {
			// sinkt sanft zu Boden und bleibt dort
			Vec3 motion = getDeltaMovement();
			setDeltaMovement(motion.x * 0.6, Math.max(motion.y, -0.25), motion.z * 0.6);
			return;
		}

		int state = getState();

		if (state == STATE_RESTING) {
			if (stopA != null && stopB != null && level.getDefaultClockTime() >= departAt) {
				departNow();
			}

			return;
		}

		if (state != STATE_FLYING) {
			return;
		}

		BlockPos stop = getStop(target);

		if (stop == null) {
			setState(STATE_IDLE);
			return;
		}

		Vec3 goal = landing(stop);
		Vec3 pos = position();
		Vec3 flat = new Vec3(goal.x - pos.x, 0, goal.z - pos.z);
		double distance = flat.length();
		double speed = BASE_SPEED * speedFactor;

		if (distance < 0.6 && Math.abs(goal.y - pos.y) < 0.8) {
			setPos(goal.x, goal.y, goal.z);
			arrive();
			return;
		}

		Vec3 dir = distance > 1.0E-4 ? flat.scale(1.0 / distance) : Vec3.ZERO;
		double desiredY;

		if (distance > 14.0) {
			// Reiseflughöhe: über dem Gelände voraus, mindestens CRUISE_HEIGHT über Start/Ziel
			int ahead = level.getHeight(Heightmap.Types.MOTION_BLOCKING, Mth.floor(pos.x + dir.x * 8), Mth.floor(pos.z + dir.z * 8));
			int below = level.getHeight(Heightmap.Types.MOTION_BLOCKING, Mth.floor(pos.x), Mth.floor(pos.z));
			cruiseY = Math.max(cruiseY, Math.max(goal.y + CRUISE_HEIGHT, Math.max(ahead, below) + 8.0));
			cruiseY = Math.min(cruiseY, level.getMaxY() - 4);
			desiredY = cruiseY;
		} else {
			desiredY = goal.y;
		}

		double horizontal = speed * Math.min(1.0, distance / 6.0);

		// erst steigen, dann fliegen: ist er weit unter der Reiseflughöhe, kaum vorwärts
		if (desiredY - pos.y > 6.0 && distance > 14.0) {
			horizontal *= 0.35;
		}

		double vertical = Mth.clamp((desiredY - pos.y) * 0.12, -speed, speed);

		if (horizontalCollision) {
			vertical = speed;
			cruiseY = pos.y + 6.0;
		}

		setDeltaMovement(dir.x * horizontal, vertical, dir.z * horizontal);

		if (distance > 0.5) {
			float yaw = (float) (Mth.atan2(dir.z, dir.x) * Mth.RAD_TO_DEG) - 90.0f;
			setYRot(Mth.approachDegrees(getYRot(), yaw, 6.0f));
			yBodyRot = getYRot();
			yHeadRot = getYRot();
		}
	}

	@Override
	public void travel(Vec3 input) {
		if (isFlying()) {
			move(net.minecraft.world.entity.MoverType.SELF, getDeltaMovement());
			return;
		}

		super.travel(input);
	}

	private void updateStatus() {
		if (isSitting()) {
			setStatus("jtechstorage.amethyst_phoenix.status.sitting");
		} else if (stopA == null || stopB == null) {
			setStatus(unlinkedStatus());
		} else if (getState() == STATE_FLYING) {
			BlockPos stop = getStop(target);
			int distance = stop == null ? 0 : (int) Math.sqrt(stop.distToCenterSqr(position()));
			setStatus("jtechstorage.amethyst_phoenix.status.flying", stopName(target), distance, getSpeedPercent());
		} else if (getState() == STATE_RESTING) {
			setStatus("jtechstorage.amethyst_phoenix.status.resting", stopName(target), getMinutesUntilDeparture());
		} else {
			setStatus(unlinkedStatus());
		}
	}

	// ------------------------------------------------------------------
	// Sitze
	// ------------------------------------------------------------------

	@Override
	protected boolean canAddPassenger(Entity passenger) {
		return getPassengers().size() < 2;
	}

	@Override
	protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float scale) {
		int index = Math.max(0, getPassengers().indexOf(passenger));
		double z = index == 0 ? 0.25 : -0.5;
		return new Vec3(0.0, dimensions.height() * 0.85, z).yRot(-getYRot() * Mth.DEG_TO_RAD);
	}

	@Override
	public net.minecraft.world.entity.@Nullable LivingEntity getControllingPassenger() {
		return null;   // fliegt immer selbst – Spieler reisen nur mit
	}

	// ------------------------------------------------------------------
	// Interaktion
	// ------------------------------------------------------------------

	/** Items, die eine eigene Aktion auslösen – dabei darf die zweite Hand nicht zusätzlich „aufsitzen“ auslösen. */
	private static boolean isActionItem(ItemStack stack) {
		return stack.is(Items.CHEST) || stack.is(ModContent.ROUTE_FEATHER);
	}

	@Override
	protected InteractionResult mobInteract(Player player, InteractionHand hand) {
		ItemStack held = player.getItemInHand(hand);

		// Client: Truhe/Feder sofort als „verbraucht“ melden, sonst probiert das Spiel danach die (leere) Zweithand
		// und der Spieler säße plötzlich auf dem Phönix.
		if (level().isClientSide() && isActionItem(held)) {
			return InteractionResult.SUCCESS;
		}

		// Aufsitzen / Menü nur mit der Haupthand – nie über eine leere Zweithand
		if (hand == InteractionHand.OFF_HAND && held.isEmpty()) {
			return InteractionResult.PASS;
		}

		return super.mobInteract(player, hand);
	}

	@Override
	protected InteractionResult interactWith(Player player, InteractionHand hand) {
		ItemStack held = player.getItemInHand(hand);

		if (held.is(Items.CHEST)) {
			if (getChests() >= MAX_CHESTS) {
				player.sendOverlayMessage(Component.translatable("jtechstorage.amethyst_phoenix.chests_full"));
				return InteractionResult.FAIL;
			}

			entityData.set(DATA_CHESTS, getChests() + 1);
			updateLoad();
			held.consume(1, player);
			playSound(SoundEvents.ARMOR_EQUIP_LEATHER.value(), 1.0f, 0.9f);
			player.sendOverlayMessage(Component.translatable("jtechstorage.amethyst_phoenix.chest_added", getChests(), MAX_CHESTS));
			return InteractionResult.SUCCESS;
		}

		if (held.is(ModContent.ROUTE_FEATHER)) {
			GlobalPos a = held.get(ModContent.ROUTE_A);
			GlobalPos b = held.get(ModContent.ROUTE_B);

			if (a == null || b == null) {
				player.sendOverlayMessage(Component.translatable("jtechstorage.route_feather.incomplete"));
				return InteractionResult.FAIL;
			}

			if (!a.dimension().equals(level().dimension()) || !b.dimension().equals(level().dimension())) {
				player.sendOverlayMessage(Component.translatable("jtechstorage.route_feather.dimension"));
				return InteractionResult.FAIL;
			}

			setRoute(a.pos(), b.pos());
			playSound(SoundEvents.AMETHYST_BLOCK_CHIME, 1.0f, 1.4f);
			player.sendOverlayMessage(Component.translatable("jtechstorage.amethyst_phoenix.route_set", stopName(0), stopName(1)));
			return InteractionResult.SUCCESS;
		}

		if (hand != InteractionHand.MAIN_HAND) {
			return InteractionResult.PASS;
		}

		if (held.isEmpty() && player.isSecondaryUseActive()) {
			if (player instanceof ServerPlayer serverPlayer) {
				serverPlayer.openMenu(new MenuProvider());
			}

			return InteractionResult.SUCCESS;
		}

		if (held.isEmpty()) {
			if (player.isPassenger()) {
				return InteractionResult.PASS;
			}

			if (getPassengers().size() < 2) {
				player.startRiding(this);
				player.sendOverlayMessage(statusText());
				return InteractionResult.SUCCESS;
			}

			player.sendOverlayMessage(Component.translatable("jtechstorage.amethyst_phoenix.seats_full"));
			return InteractionResult.SUCCESS;
		}

		return InteractionResult.PASS;
	}

	/** Letzte Kiste abnehmen (nur wenn leer). */
	public boolean removeChest(Player player) {
		int chests = getChests();

		if (chests == 0) {
			return false;
		}

		for (int slot = (chests - 1) * CHEST_SLOTS; slot < chests * CHEST_SLOTS; slot++) {
			if (!cargo.getItem(slot).isEmpty()) {
				player.sendOverlayMessage(Component.translatable("jtechstorage.amethyst_phoenix.chest_not_empty"));
				return false;
			}
		}

		entityData.set(DATA_CHESTS, chests - 1);
		updateLoad();
		player.getInventory().placeItemBackInInventory(new ItemStack(Items.CHEST), Prediction.SERVER_ONLY);
		playSound(SoundEvents.ARMOR_EQUIP_LEATHER.value(), 1.0f, 1.1f);
		return true;
	}

	/** Einpacken (ohne Kisten und Mitreisende). */
	public boolean pack(Player player) {
		if (getChests() > 0 || isVehicle()) {
			player.sendOverlayMessage(Component.translatable("jtechstorage.amethyst_phoenix.cannot_pack"));
			return false;
		}

		player.getInventory().placeItemBackInInventory(createPackedItem(ModContent.AMETHYST_PHOENIX_ITEM, player), Prediction.SERVER_ONLY);
		playSound(SoundEvents.FIRECHARGE_USE, 0.6f, 1.4f);
		discard();
		return true;
	}

	/** Entwicklung: aus einem Phönix wird ein Amethyst-Phönix (Besitzer, Netzwerk, Farbe, Name und Hut bleiben). */
	public static void evolve(Phoenix phoenix, ServerLevel level, Player player) {
		AmethystPhoenix grown = ModContent.AMETHYST_PHOENIX.create(level, net.minecraft.world.entity.EntitySpawnReason.CONVERSION);

		if (grown == null) {
			return;
		}

		grown.snapTo(phoenix.getX(), phoenix.getY(), phoenix.getZ(), phoenix.getYRot(), 0.0f);
		grown.owner = phoenix.owner != null ? phoenix.owner : player.getUUID();
		grown.terminalPos = phoenix.terminalPos;
		grown.setAccentColor(phoenix.getAccentColor());

		if (phoenix.hasCustomName()) {
			grown.setCustomName(phoenix.getCustomName());
		}

		ItemStack hat = phoenix.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD);

		if (!hat.isEmpty()) {
			grown.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, hat.copy());
		}

		level.addFreshEntity(grown);
		phoenix.discard();
		level.sendParticles(ParticleTypes.FLAME, grown.getX(), grown.getY() + 1, grown.getZ(), 80, 1.0, 1.0, 1.0, 0.1);
		level.sendParticles(ParticleTypes.END_ROD, grown.getX(), grown.getY() + 1, grown.getZ(), 40, 1.0, 1.0, 1.0, 0.1);
		level.sendParticles(ParticleTypes.WITCH, grown.getX(), grown.getY() + 1, grown.getZ(), 40, 1.0, 1.0, 1.0, 0.1);
		grown.playSound(SoundEvents.TOTEM_USE, 1.0f, 1.2f);
		grown.playSound(ModSounds.AMETHYST_PHOENIX_EVOLVE, 2.0f, 1.0f);
		player.sendSystemMessage(Component.translatable("jtechstorage.amethyst_phoenix.evolved").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));

		if (player instanceof ServerPlayer serverPlayer) {
			dev.bartels.jtechstorage.ModAdvancements.grant(serverPlayer, "amethyst_phoenix");
		}
	}

	// ------------------------------------------------------------------
	// Kleinkram & Speichern
	// ------------------------------------------------------------------

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		return false;
	}

	@Override
	public boolean isPushable() {
		return !isFlying() && super.isPushable();
	}

	@Override
	protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
		super.dropCustomDeathLoot(level, source, recentlyHit);

		for (ItemStack stack : cargo.removeAllItems()) {
			spawnAtLocation(level, stack);
		}

		if (getChests() > 0) {
			spawnAtLocation(level, new ItemStack(Items.CHEST, getChests()));
		}
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putInt("Chests", getChests());
		output.putInt("State", getState());
		output.putBoolean("Sitting", isSitting());
		output.putInt("Target", target);
		output.putLong("DepartAt", departAt);
		output.putDouble("CruiseY", cruiseY);

		if (stopA != null) {
			output.store("StopA", BlockPos.CODEC, stopA);
		}

		if (stopB != null) {
			output.store("StopB", BlockPos.CODEC, stopB);
		}

		ValueOutput.TypedOutputList<ItemStackWithSlot> items = output.list("Cargo", ItemStackWithSlot.CODEC);

		for (int slot = 0; slot < cargo.getContainerSize(); slot++) {
			ItemStack stack = cargo.getItem(slot);

			if (!stack.isEmpty()) {
				items.add(new ItemStackWithSlot(slot, stack));
			}
		}
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		entityData.set(DATA_CHESTS, Math.clamp(input.getIntOr("Chests", 0), 0, MAX_CHESTS));
		target = input.getIntOr("Target", 0);
		departAt = input.getLongOr("DepartAt", 0L);
		cruiseY = input.getDoubleOr("CruiseY", getY());
		stopA = input.read("StopA", BlockPos.CODEC).orElse(null);
		stopB = input.read("StopB", BlockPos.CODEC).orElse(null);
		setState(input.getIntOr("State", STATE_IDLE));
		entityData.set(DATA_SITTING, input.getBooleanOr("Sitting", false));

		if (isSitting()) {
			setNoGravity(false);
		}

		for (ItemStackWithSlot item : input.listOrEmpty("Cargo", ItemStackWithSlot.CODEC)) {
			if (item.isValidInContainer(cargo.getContainerSize())) {
				cargo.setItem(item.slot(), item.stack());
			}
		}

		updateLoad();
	}

	private class MenuProvider implements ExtendedMenuProvider<AmethystPhoenixMenu.OpenData> {
		@Override
		public AmethystPhoenixMenu.OpenData getScreenOpeningData(ServerPlayer player) {
			return new AmethystPhoenixMenu.OpenData(getId(), java.util.Optional.ofNullable(stopA), java.util.Optional.ofNullable(stopB));
		}

		@Override
		public Component getDisplayName() {
			return AmethystPhoenix.this.getDisplayName();
		}

		@Override
		public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
			return new AmethystPhoenixMenu(containerId, inventory, AmethystPhoenix.this);
		}
	}

	/** Für Statusanzeigen. */
	public List<Component> routeLines() {
		return List.of(stopName(0), stopName(1));
	}

	@Override
	protected net.minecraft.sounds.SoundEvent helperAmbientSound() {
		return ModSounds.AMETHYST_PHOENIX_AMBIENT;
	}
}
