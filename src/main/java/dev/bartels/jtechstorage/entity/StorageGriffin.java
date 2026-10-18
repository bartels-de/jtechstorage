package dev.bartels.jtechstorage.entity;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.Prediction;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

import dev.bartels.jtechstorage.ModSounds;
import dev.bartels.jtechstorage.ModAdvancements;
import dev.bartels.jtechstorage.ModContent;
import dev.bartels.jtechstorage.block.StorageTerminalBlockEntity;

/**
 * Lager-Greif: Flug-Reittier mit Truhe auf dem Rücken (27 Slots).
 * Gelandet in Reichweite seines Terminals lädt er seinen Inhalt automatisch ins Netzwerk ab.
 *
 * <ul>
 *     <li>Rechtsklick: aufsteigen – Vorwärts fliegt in Blickrichtung, Leertaste steigt</li>
 *     <li>Shift + Rechtsklick: Truhe öffnen</li>
 *     <li>Shift + Rechtsklick mit Leine: einpacken (Inhalt bleibt im Item)</li>
 * </ul>
 */
public class StorageGriffin extends NetworkHelperMob implements net.minecraft.world.entity.HasCustomInventoryScreen {
	public static final int SIZE = 27;
	private static final int UNLOAD_INTERVAL = 40;

	private final SimpleContainer inventory = new SimpleContainer(SIZE) {
		@Override
		public boolean stillValid(Player player) {
			return isAlive() && !isRemoved() && player.distanceToSqr(StorageGriffin.this) < 64;
		}
	};

	public StorageGriffin(EntityType<? extends StorageGriffin> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return createMobAttributes()
				.add(Attributes.MAX_HEALTH, 30.0)
				.add(Attributes.MOVEMENT_SPEED, 0.2)
				.add(Attributes.FLYING_SPEED, 0.036);
	}

	@Override
	protected String unlinkedStatus() {
		return "jtechstorage.griffin.status.unlinked";
	}

	@Override
	protected String linkAdvancement() {
		return "griffin";
	}

	@Override
	protected void onLinked(Player player) {
		setStatus("jtechstorage.griffin.status.linked");
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0f));
		goalSelector.addGoal(7, new RandomLookAroundGoal(this));
	}

	/** Fliegt der Greif gerade (für die Animation)? */
	public boolean isFlying() {
		return !onGround() && !isInWater();
	}

	// ------------------------------------------------------------------
	// Interaktion
	// ------------------------------------------------------------------

	@Override
	protected InteractionResult interactWith(Player player, InteractionHand hand) {
		ItemStack held = player.getItemInHand(hand);

		if (player.isSecondaryUseActive() && held.is(Items.LEAD)) {
			packUp(player);
			return InteractionResult.SUCCESS;
		}

		if (player.isSecondaryUseActive() && held.isEmpty()) {
			packUp(player);
			return InteractionResult.SUCCESS;
		}

		if (held.isEmpty() && !isVehicle()) {
			if (owner == null) {
				setOwner(player);
			}

			player.startRiding(this);
			player.sendOverlayMessage(Component.translatable("jtechstorage.griffin.controls"));

			if (player instanceof ServerPlayer serverPlayer && terminalPos != null) {
				ModAdvancements.grant(serverPlayer, "griffin");
			}

			return InteractionResult.SUCCESS;
		}

		return InteractionResult.PASS;
	}

	/** Inventar-Taste beim Reiten öffnet die Truhe auf dem Rücken (wie beim Esel). */
	@Override
	public void openCustomInventoryScreen(Player player) {
		if (!level().isClientSide() && mayInteract(player)) {
			player.openMenu(new SimpleMenuProvider((id, playerInventory, p) -> ChestMenu.threeRows(id, playerInventory, inventory),
					getDisplayName()));
			playSound(SoundEvents.CHEST_OPEN, 0.6f, 1.2f);
		}
	}

	private void packUp(Player player) {
		ejectPassengers();
		ItemStack item = createPackedItem(ModContent.STORAGE_GRIFFIN_ITEM, player);
		item.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(inventory.getItems()));

		inventory.clearContent();
		player.getInventory().placeItemBackInInventory(item, Prediction.SERVER_ONLY);
		playSound(SoundEvents.CHEST_CLOSE, 1.0f, 1.0f);
		discard();
	}

	/** Beim Absetzen aus dem Item: Inhalt übernehmen. */
	public void loadFromItem(ItemStack stack) {
		ItemContainerContents contents = stack.get(DataComponents.CONTAINER);

		if (contents != null) {
			NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
			contents.copyInto(items);

			for (int slot = 0; slot < SIZE; slot++) {
				inventory.setItem(slot, items.get(slot));
			}
		}
	}

	// ------------------------------------------------------------------
	// Reiten & Fliegen
	// ------------------------------------------------------------------

	@Override
	public @Nullable LivingEntity getControllingPassenger() {
		return getFirstPassenger() instanceof Player player ? player : null;
	}

	/** Die Leine dient zum Einpacken, nicht zum Anleinen. */
	@Override
	public boolean canBeLeashed() {
		return false;
	}

	@Override
	protected boolean canAddPassenger(Entity passenger) {
		return getPassengers().isEmpty();
	}

	@Override
	public boolean isFlyingVehicle() {
		return true;
	}

	@Override
	protected Vec3 getRiddenInput(Player player, Vec3 selfInput) {
		float forward = player.zza;
		float strafe = player.xxa * 0.6f;
		float pitch = player.getXRot() * Mth.DEG_TO_RAD;
		double up = forward != 0 ? -Mth.sin(pitch) * forward : 0;

		if (player.isJumping()) {
			up += 0.6;
		}

		double horizontal = forward != 0 ? Mth.cos(pitch) * forward : 0;
		return new Vec3(strafe, up, horizontal);
	}

	@Override
	protected float getRiddenSpeed(Player player) {
		return (float) (getAttributeValue(Attributes.FLYING_SPEED) * speedFactor());
	}

	@Override
	protected void tickRidden(Player player, Vec3 input) {
		super.tickRidden(player, input);
		setRot(player.getYRot(), player.getXRot() * 0.5f);
		yRotO = getYRot();
		yBodyRot = getYRot();
		yHeadRot = getYRot();
	}

	@Override
	public void travel(Vec3 input) {
		if (getControllingPassenger() != null) {
			travelFlying(input, getSpeed());
		} else {
			super.travel(input);
		}
	}

	// ------------------------------------------------------------------
	// Automatisch abladen
	// ------------------------------------------------------------------

	@Override
	public void aiStep() {
		super.aiStep();

		// Flügelschlag im Flug
		if (!level().isClientSide() && isFlying() && !isHologram() && tickCount % 14 == 0) {
			playSound(dev.bartels.jtechstorage.ModSounds.GRIFFIN_FLAP, 0.6f, 0.9f + random.nextFloat() * 0.2f);
		}

		if (level() instanceof ServerLevel serverLevel && tickCount % UNLOAD_INTERVAL == 0
				&& onGround() && !isVehicle() && !isResting() && !inventory.isEmpty()) {
			tryUnload(serverLevel);
		}
	}

	private void tryUnload(ServerLevel level) {
		StorageTerminalBlockEntity terminal = terminal();

		if (terminal == null) {
			return;
		}

		double range = terminal.getRange();

		if (distanceToSqr(Vec3.atCenterOf(terminal.getBlockPos())) > range * range) {
			return;
		}

		var network = terminal.buildNetwork();
		int delivered = 0;

		for (int slot = 0; slot < SIZE; slot++) {
			ItemStack stack = inventory.getItem(slot);

			if (stack.isEmpty()) {
				continue;
			}

			int before = stack.getCount();
			ItemStack rest = network.insert(stack.copy());
			delivered += before - rest.getCount();
			inventory.setItem(slot, rest);
		}

		if (delivered > 0) {
			level.sendParticles(ParticleTypes.HAPPY_VILLAGER, getX(), getY() + 1.2, getZ(), 10, 0.6, 0.4, 0.6, 0.0);
			playSound(ModSounds.GRIFFIN_UNLOAD, 0.8f, 1.0f);
			setStatus("jtechstorage.griffin.status.delivered", delivered);
			Player ownerPlayer = owner != null ? level.getPlayerByUUID(owner) : null;

			if (ownerPlayer != null && ownerPlayer.distanceToSqr(this) < 32 * 32) {
				ownerPlayer.sendOverlayMessage(Component.translatable("jtechstorage.griffin.delivered", getDisplayName(), delivered));
			}
		} else {
			setStatus("jtechstorage.griffin.status.network_full");
		}
	}

	// ------------------------------------------------------------------
	// Speichern
	// ------------------------------------------------------------------

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		ContainerHelper.saveAllItems(output.child("GriffinInventory"), inventory.getItems());
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		ContainerHelper.loadAllItems(input.childOrEmpty("GriffinInventory"), inventory.getItems());
	}

	@Override
	protected net.minecraft.sounds.SoundEvent helperSleepSound() {
		return dev.bartels.jtechstorage.ModSounds.GRIFFIN_SLEEP;
	}

	@Override
	protected void playStepSound(net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
		playSound(dev.bartels.jtechstorage.ModSounds.GRIFFIN_STEP, 0.4f, 0.9f + random.nextFloat() * 0.2f);
	}

	@Override
	protected net.minecraft.sounds.SoundEvent helperAmbientSound() {
		return ModSounds.GRIFFIN_AMBIENT;
	}

	/** Zu groß bzw. hat einen eigenen Schlafplatz – kein Helferbett. */
	@Override
	protected int bedKind() {
		return 0;
	}
}
