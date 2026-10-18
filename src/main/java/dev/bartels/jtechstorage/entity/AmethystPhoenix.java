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
 * Groß, zwei Sitze für Spieler, bis zu vier ausrüstbare Kisten. Mit einem Flugplan bekommt er 2–4 Haltestellen
 * und fliegt sie dann selbstständig der Reihe nach an; die Rastdauer stellt jede Haltestelle selbst ein.
 * Wer vorne sitzt, kann mit der Sprungtaste früher starten. Je mehr Kisten und Ladung, desto langsamer fliegt er.
 * Im Sitz-Modus bewegt er sich nicht.
 */
public class AmethystPhoenix extends NetworkHelperMob implements net.minecraft.world.entity.HasCustomInventoryScreen {
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
	/** Haltestelle der Route (Name als Kopie, falls der Block gerade nicht geladen ist). */
	public record RouteStop(BlockPos pos, String name) {
		public static final com.mojang.serialization.Codec<RouteStop> CODEC = com.mojang.serialization.codecs.RecordCodecBuilder.create(
				instance -> instance.group(
						BlockPos.CODEC.fieldOf("pos").forGetter(RouteStop::pos),
						com.mojang.serialization.Codec.STRING.fieldOf("name").forGetter(RouteStop::name)
				).apply(instance, RouteStop::new));
	}

	private final List<RouteStop> stops = new java.util.ArrayList<>();
	/** Index der Haltestelle, zu der er fliegt bzw. an der er rastet. */
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

	public List<RouteStop> getStops() {
		return List.copyOf(stops);
	}

	private boolean hasRoute() {
		return stops.size() >= 2;
	}

	private @Nullable RouteStop targetStop() {
		return target >= 0 && target < stops.size() ? stops.get(target) : null;
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

	/** Neue Route vom Flugplan. Er fliegt zuerst zur ersten Haltestelle (ist er schon dort, rastet er gleich). */
	public void setRoute(List<RouteStop> route) {
		stops.clear();

		for (RouteStop stop : route) {
			if (stops.size() < dev.bartels.jtechstorage.item.FlightPlanItem.MAX_STOPS) {
				stops.add(new RouteStop(stop.pos().immutable(), stop.name()));
			}
		}

		target = 0;

		if (!hasRoute()) {
			setState(STATE_IDLE);
		} else if (position().distanceTo(landing(stops.getFirst().pos())) < 8.0) {
			arrive();
		} else {
			takeOff();
		}
	}

	/** Route aus einem Flugplan (alle Haltestellen müssen in dieser Dimension liegen). */
	private @Nullable List<RouteStop> routeFrom(List<dev.bartels.jtechstorage.item.FlightPlanItem.Stop> plan) {
		List<RouteStop> route = new java.util.ArrayList<>();

		for (dev.bartels.jtechstorage.item.FlightPlanItem.Stop stop : plan) {
			if (!stop.pos().dimension().equals(level().dimension())) {
				return null;
			}

			route.add(new RouteStop(stop.pos().pos(), stop.name()));
		}

		return route;
	}

	private List<dev.bartels.jtechstorage.item.FlightPlanItem.Stop> toPlan() {
		List<dev.bartels.jtechstorage.item.FlightPlanItem.Stop> plan = new java.util.ArrayList<>();

		for (RouteStop stop : stops) {
			plan.add(new dev.bartels.jtechstorage.item.FlightPlanItem.Stop(GlobalPos.of(level().dimension(), stop.pos()), stop.name()));
		}

		return plan;
	}

	/** Route als Text: „Hafen → Mine → Farm → …“. */
	public Component routeText() {
		net.minecraft.network.chat.MutableComponent text = Component.empty();

		for (int i = 0; i < stops.size(); i++) {
			if (i > 0) {
				text.append(" → ");
			}

			text.append(stopName(i));
		}

		return text.append(" ↺");
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
		if (getState() != STATE_RESTING || isSitting() || !hasRoute()) {
			return false;
		}

		target = (target + 1) % stops.size();
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
		dev.bartels.jtechstorage.block.PhoenixStopBlockEntity.Rest rest = dev.bartels.jtechstorage.block.PhoenixStopBlockEntity.Rest.NIGHT;
		RouteStop stop = targetStop();

		// Rastdauer (und aktuellen Namen) von der Haltestelle lesen – fehlt der Block, rastet er eine Nacht
		if (stop != null && level().getBlockEntity(stop.pos()) instanceof dev.bartels.jtechstorage.block.PhoenixStopBlockEntity block) {
			rest = block.getRest();

			if (block.hasName()) {
				stops.set(target, new RouteStop(stop.pos(), block.getStopName()));
			}

			// Lieferauftrag: mit den Kisten neben der Haltestelle tauschen
			if (level() instanceof ServerLevel serverLevel && getChests() > 0) {
				dev.bartels.jtechstorage.block.PhoenixStopBlockEntity.Exchange exchange = block.exchange(serverLevel, cargo, getChests() * CHEST_SLOTS);

				if (exchange.unloaded() > 0 || exchange.loaded() > 0) {
					tellPassengers(Component.translatable("jtechstorage.amethyst_phoenix.exchange", exchange.unloaded(), exchange.loaded()));
					serverLevel.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 0.6, getZ(), 12, 0.8, 0.3, 0.8, 0.02);
				}
			}
		}

		departAt = nextDeparture(level().getDefaultClockTime(), rest);
		playSound(SoundEvents.AMETHYST_BLOCK_CHIME, 1.2f, 0.8f);
		tellPassengers(Component.translatable("jtechstorage.amethyst_phoenix.arrived", stopName(target), Component.translatable(rest.key())));
	}

	static long nextDeparture(long time, dev.bartels.jtechstorage.block.PhoenixStopBlockEntity.Rest rest) {
		return rest.ticks > 0 ? time + rest.ticks : nextDeparture(time);
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
		if (index < 0 || index >= stops.size()) {
			return Component.literal("?");
		}

		String name = stops.get(index).name();
		return name.isBlank() ? Component.translatable("jtechstorage.amethyst_phoenix.stop", index + 1) : Component.literal(name);
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
		if (!isHologram() && isFlying() && tickCount % 30 == 0) {
			playSound(ModSounds.AMETHYST_PHOENIX_FLAP, 0.9f, 0.9f + random.nextFloat() * 0.15f);
		}

		if (level() instanceof ServerLevel level && !isHologram()) {
			serverMovement(level);
		}

		super.aiStep();

		if (level() instanceof ServerLevel level && !isHologram() && tickCount % 20 == 3) {
			updateStatus();

			// Chunks entlang der Route geladen halten (wie bei einer Enderperle), damit er auch ohne Spieler weiterpendelt
			if (hasRoute() && !isSitting()) {
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
			if (hasRoute() && level.getDefaultClockTime() >= departAt) {
				departNow();
			}

			return;
		}

		if (state != STATE_FLYING) {
			return;
		}

		RouteStop stop = targetStop();

		if (stop == null) {
			setState(STATE_IDLE);
			return;
		}

		Vec3 goal = landing(stop.pos());
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
		} else if (!hasRoute()) {
			setStatus(unlinkedStatus());
		} else if (getState() == STATE_FLYING) {
			RouteStop stop = targetStop();
			int distance = stop == null ? 0 : (int) Math.sqrt(stop.pos().distToCenterSqr(position()));
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
		double drop = isSitting() || isNightRest() ? 0.62 : 0.0;   // sitzt/schläft er, liegt der Bauch am Boden
		return new Vec3(0.0, dimensions.height() * 0.85 - drop, z).yRot(-getYRot() * Mth.DEG_TO_RAD);
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
		return stack.is(Items.CHEST) || stack.is(ModContent.FLIGHT_PLAN);
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

		if (held.is(ModContent.FLIGHT_PLAN)) {
			List<dev.bartels.jtechstorage.item.FlightPlanItem.Stop> plan = dev.bartels.jtechstorage.item.FlightPlanItem.stops(held);

			if (plan.size() < 2) {
				player.sendOverlayMessage(Component.translatable("jtechstorage.flight_plan.too_short"));
				return InteractionResult.FAIL;
			}

			List<RouteStop> route = routeFrom(plan);

			if (route == null) {
				player.sendOverlayMessage(Component.translatable("jtechstorage.flight_plan.dimension"));
				return InteractionResult.FAIL;
			}

			setRoute(route);
			playSound(SoundEvents.AMETHYST_BLOCK_CHIME, 1.0f, 1.4f);
			player.sendOverlayMessage(Component.translatable("jtechstorage.amethyst_phoenix.route_set", routeText()));
			return InteractionResult.SUCCESS;
		}

		if (hand != InteractionHand.MAIN_HAND) {
			return InteractionResult.PASS;
		}

		if (held.isEmpty() && player.isSecondaryUseActive()) {
			pack(player);
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

	/** Inventar-Taste beim Reiten: Menü mit Kisten, Status und Knöpfen. */
	@Override
	public void openCustomInventoryScreen(Player player) {
		if (player instanceof ServerPlayer serverPlayer && mayInteract(player)) {
			serverPlayer.openMenu(new MenuProvider());
		}
	}

	/** Einpacken: Kisten samt Ladung und die Route wandern mit ins Item; Mitreisende steigen ab. */
	public boolean pack(Player player) {
		ejectPassengers();
		ItemStack item = createPackedItem(ModContent.AMETHYST_PHOENIX_ITEM, player);

		if (getChests() > 0) {
			java.util.List<ItemStack> items = new java.util.ArrayList<>();

			for (int slot = 0; slot < cargo.getContainerSize(); slot++) {
				items.add(cargo.getItem(slot).copy());
			}

			item.set(net.minecraft.core.component.DataComponents.CONTAINER, net.minecraft.world.item.component.ItemContainerContents.fromItems(items));
			net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
			tag.putInt("Chests", getChests());
			item.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(tag));
			cargo.clearContent();
		}

		if (hasRoute()) {
			item.set(ModContent.FLIGHT_PLAN_STOPS, List.copyOf(toPlan()));
		}

		player.getInventory().placeItemBackInInventory(item, Prediction.SERVER_ONLY);
		playSound(SoundEvents.FIRECHARGE_USE, 0.6f, 1.4f);
		discard();
		return true;
	}

	/** Beim Absetzen aus dem Item: Kisten, Ladung und Route übernehmen. */
	public void loadFromItem(ItemStack stack) {
		net.minecraft.world.item.component.CustomData data = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
		int chests = data != null ? Math.clamp(data.copyTag().getIntOr("Chests", 0), 0, MAX_CHESTS) : 0;

		if (chests > 0) {
			entityData.set(DATA_CHESTS, chests);
			net.minecraft.world.item.component.ItemContainerContents contents = stack.get(net.minecraft.core.component.DataComponents.CONTAINER);

			if (contents != null) {
				net.minecraft.core.NonNullList<ItemStack> items = net.minecraft.core.NonNullList.withSize(cargo.getContainerSize(), ItemStack.EMPTY);
				contents.copyInto(items);

				for (int slot = 0; slot < items.size(); slot++) {
					cargo.setItem(slot, items.get(slot));
				}
			}

			updateLoad();
		}

		List<dev.bartels.jtechstorage.item.FlightPlanItem.Stop> plan = stack.get(ModContent.FLIGHT_PLAN_STOPS);
		GlobalPos a = stack.get(ModContent.ROUTE_A);
		GlobalPos b = stack.get(ModContent.ROUTE_B);

		if (plan == null && a != null && b != null) {
			// Phönix-Item aus v0.20–v0.24 (Route-Feder)
			plan = List.of(new dev.bartels.jtechstorage.item.FlightPlanItem.Stop(a, "A"), new dev.bartels.jtechstorage.item.FlightPlanItem.Stop(b, "B"));
		}

		List<RouteStop> route = plan == null ? null : routeFrom(plan);

		if (route != null && route.size() >= 2) {
			setRoute(route);
		}
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

		output.store("Stops", RouteStop.CODEC.listOf(), List.copyOf(stops));

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
		stops.clear();
		stops.addAll(input.read("Stops", RouteStop.CODEC.listOf()).orElse(List.of()));
		BlockPos legacyA = input.read("StopA", BlockPos.CODEC).orElse(null);
		BlockPos legacyB = input.read("StopB", BlockPos.CODEC).orElse(null);

		if (stops.isEmpty() && legacyA != null && legacyB != null) {
			stops.add(new RouteStop(legacyA, "A"));
			stops.add(new RouteStop(legacyB, "B"));
		}

		target = stops.isEmpty() ? 0 : Math.floorMod(target, stops.size());
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
			List<String> names = new java.util.ArrayList<>();

			for (int i = 0; i < stops.size(); i++) {
				names.add(stopName(i).getString());
			}

			return new AmethystPhoenixMenu.OpenData(getId(), names, target);
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


	@Override
	protected net.minecraft.sounds.SoundEvent helperAmbientSound() {
		return isNightRest() ? ModSounds.AMETHYST_PHOENIX_SLEEP : ModSounds.AMETHYST_PHOENIX_AMBIENT;
	}

	/** Nachts am Boden (Haltestelle, Horst oder sitzend): schläft mit dem Kopf im Rückengefieder – rein optisch. */
	public boolean isNightRest() {
		long time = Math.floorMod(level().getDefaultClockTime(), 24000L);
		return !isHologram() && getState() != STATE_FLYING && onGround() && time >= 13000L && time <= 23000L;
	}

	/** Zu groß bzw. hat einen eigenen Schlafplatz – kein Helferbett. */
	@Override
	protected int bedKind() {
		return 0;
	}
}
