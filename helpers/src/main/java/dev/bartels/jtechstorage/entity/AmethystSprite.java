package dev.bartels.jtechstorage.entity;

import dev.bartels.jtechstorage.backpack.PlacedBackpackBlockEntity;
import dev.bartels.jtechstorage.helpers.HelpersContent;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;

import net.minecraft.sounds.SoundEvent;
import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Prediction;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.allay.Allay;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import dev.bartels.jtechstorage.ModConfig;
import dev.bartels.jtechstorage.ModSounds;
import dev.bartels.jtechstorage.block.StorageTerminalBlockEntity;
import dev.bartels.jtechstorage.network.StorageNetwork;

/**
 * Amethyst sprite: a small floating helper that sorts the chests of a storage network.
 * It flies to the source chest, takes a stack, visibly carries it to the target chest and puts it down there.
 *
 * <ul>
 *     <li>Click with a linked storage key: sprite works for that network</li>
 *     <li>Right-click with an empty hand: show status</li>
 *     <li>Shift + right-click with an empty hand: pack the sprite up again</li>
 *     <li>Dye / hat / shears: cosmetics (see {@link NetworkHelperMob})</li>
 * </ul>
 */
public class AmethystSprite extends NetworkHelperMob {
	private static final int IDLE_DELAY = Math.max(1, (int) Math.round(ModConfig.get().helpers.spritePauseSeconds * 20.0));
	/** Base speed (deliberately leisurely – much faster with resonance). */
	private static final double FLY_SPEED = 1.0;
	private static final int LEG_TIMEOUT = 600;
	private static final double REACH_SQR = 2.5 * 2.5;
	private static final double PICKUP_SQR = 1.6 * 1.6;
	public static final int COLLECT_RADIUS = ModConfig.get().helpers.spriteCollectRadius;
	/** Parked backpacks within this radius are emptied. */
	public static final int BACKPACK_RADIUS = ModConfig.get().helpers.spriteBackpackRadius;

	public AmethystSprite(EntityType<? extends AmethystSprite> type, Level level) {
		super(type, level);
		this.moveControl = new FlyingMoveControl<>(this, 20, true);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Allay.createAttributes();
	}

	@Override
	protected String unlinkedStatus() {
		return "jtechstorage.sprite.status.unlinked";
	}

	@Override
	protected String linkAdvancement() {
		return "sprite";
	}

	@Override
	protected void onLinked(Player player) {
		setStatus("jtechstorage.sprite.status.idle");
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
		goalSelector.addGoal(1, new SortGoal());
		goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 6.0f));
		goalSelector.addGoal(6, new RandomLookAroundGoal(this));
	}

	@Override
	public void travel(Vec3 input) {
		travelFlying(input, getSpeed());
	}

	@Override
	protected InteractionResult interactWith(Player player, InteractionHand hand) {
		if (player.getItemInHand(hand).isEmpty() && player.isSecondaryUseActive()) {
			// Pack up: return carried item, sprite back as an item
			ItemStack carried = getMainHandItem();

			if (!carried.isEmpty()) {
				player.getInventory().placeItemBackInInventory(carried.copy(), Prediction.SERVER_ONLY);
				setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
			}

			player.getInventory().placeItemBackInInventory(createPackedItem(HelpersContent.AMETHYST_SPRITE_ITEM, player),
					Prediction.SERVER_ONLY);
			playSound(SoundEvents.AMETHYST_CLUSTER_BREAK, 1.0f, 1.2f);
			discard();
			return InteractionResult.SUCCESS;
		}

		return super.interactWith(player, hand);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		setStatus(terminalPos == null ? unlinkedStatus() : "jtechstorage.sprite.status.idle");
	}

	// ------------------------------------------------------------------
	// Sorting behavior
	// ------------------------------------------------------------------

	private enum Phase {
		IDLE, TO_SOURCE, TO_TARGET, TO_TIDY, TO_ITEM, TO_BACKPACK, DELIVER
	}

	/** State machine: plan task → fly to source → take stack → fly to target → put down. */
	private class SortGoal extends Goal {
		private Phase phase = Phase.IDLE;
		private SortingPlanner.@Nullable Task task;
		private SortingPlanner.@Nullable TidyTask tidyTask;
		private @Nullable ItemEntity itemTarget;
		private @Nullable BlockPos backpackTarget;
		private int tidyOffset;
		private @Nullable BlockPos targetPos;
		private @Nullable Container target;
		private int cooldown;
		private int legTicks;
		private int planOffset;

		SortGoal() {
			setFlags(EnumSet.of(Goal.Flag.MOVE));
		}

		@Override
		public boolean canUse() {
			return !level().isClientSide() && terminalPos != null;
		}

		@Override
		public boolean canContinueToUse() {
			return canUse();
		}

		@Override
		public boolean requiresUpdateEveryTick() {
			return true;
		}

		@Override
		public void stop() {
			returnCarriedToNetwork();
			phase = Phase.IDLE;
			getNavigation().stop();
		}

		@Override
		public void tick() {
			StorageTerminalBlockEntity terminal = terminal();

			if (terminal == null) {
				setStatus("jtechstorage.sprite.status.no_terminal");
				return;
			}

			switch (phase) {
				case IDLE -> tickIdle(terminal);
				case TO_SOURCE -> tickToSource(terminal);
				case TO_TARGET -> tickToTarget(terminal);
				case TO_TIDY -> tickToTidy(terminal);
				case TO_ITEM -> tickToItem();
				case TO_BACKPACK -> tickToBackpack();
				case DELIVER -> tickDeliver(terminal);
			}
		}

		private void tickIdle(StorageTerminalBlockEntity terminal) {
			if (!getMainHandItem().isEmpty()) {
				// still carrying something (e.g. after loading) → look for a new target
				retarget(terminal);
				return;
			}

			if (--cooldown > 0) {
				hoverNear(terminalPos);
				return;
			}

			cooldown = (int) Math.round(IDLE_DELAY / speedFactor());
			StorageNetwork network = terminal.buildNetwork();
			task = SortingPlanner.plan(network, terminal, planOffset++);

			if (task == null) {
				// Nothing left to rearrange → collect items lying around (taken over from the former collector moth)
				itemTarget = findItem();

				if (itemTarget != null) {
					phase = Phase.TO_ITEM;
					legTicks = 0;
					setStatus("jtechstorage.sprite.status.collecting", itemTarget.getItem().getHoverName());
					return;
				}

				// … empty parked backpacks nearby
				backpackTarget = findBackpack();

				if (backpackTarget != null) {
					phase = Phase.TO_BACKPACK;
					legTicks = 0;
					setStatus("jtechstorage.sprite.status.backpack", backpackTarget.getX(), backpackTarget.getY(), backpackTarget.getZ());
					return;
				}

				// … then tidy up the chests inside (merge stacks, sort)
				tidyTask = SortingPlanner.planTidy(network, terminal, tidyOffset++);

				if (tidyTask != null) {
					phase = Phase.TO_TIDY;
					legTicks = 0;
					setStatus("jtechstorage.sprite.status.tidying", tidyTask.pos().getX(), tidyTask.pos().getY(), tidyTask.pos().getZ());
					return;
				}

				setStatus("jtechstorage.sprite.status.done");
				hoverNear(terminalPos);
				return;
			}

			phase = Phase.TO_SOURCE;
			legTicks = 0;
			setStatus("jtechstorage.sprite.status.working", Component.translatable(task.category().translationKey()),
					task.targetPos().getX(), task.targetPos().getY(), task.targetPos().getZ());
		}

		private void tickToSource(StorageTerminalBlockEntity terminal) {
			if (task == null || ++legTicks > LEG_TIMEOUT) {
				phase = Phase.IDLE;
				return;
			}

			if (!arrived(task.sourcePos())) {
				flyTo(task.sourcePos());
				return;
			}

			if (isGone(task.source())) {
				phase = Phase.IDLE;
				return;
			}

			ItemStack taken = takeFrom(task.source(), task.prototype());

			if (taken.isEmpty()) {
				phase = Phase.IDLE; // someone was faster
				return;
			}

			setItemInHand(InteractionHand.MAIN_HAND, taken);
			playSound(SoundEvents.ITEM_PICKUP, 0.6f, 1.6f);
			target = task.target();
			targetPos = task.targetPos();
			phase = Phase.TO_TARGET;
			legTicks = 0;
		}

		private void tickToTarget(StorageTerminalBlockEntity terminal) {
			ItemStack carried = getMainHandItem();

			if (carried.isEmpty()) {
				phase = Phase.IDLE;
				return;
			}

			if (target == null || targetPos == null || ++legTicks > LEG_TIMEOUT) {
				returnCarriedToNetwork();
				phase = Phase.IDLE;
				return;
			}

			if (!arrived(targetPos)) {
				flyTo(targetPos);
				return;
			}

			if (isGone(target)) {
				retarget(terminal); // chest was broken
				return;
			}

			StorageNetwork.insertInto(target, carried);

			if (carried.isEmpty()) {
				setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
				playSound(ModSounds.SPRITE_WORK, 0.7f, 0.95f + random.nextFloat() * 0.1f);
				playAction();
				((ServerLevel) level()).sendParticles(ParticleTypes.WITCH, targetPos.getX() + 0.5, targetPos.getY() + 1.1,
						targetPos.getZ() + 0.5, 6, 0.25, 0.1, 0.25, 0.01);
				phase = Phase.IDLE;
				cooldown = 5;
			} else {
				// Target chest full → another target (same category or empty chest)
				setItemInHand(InteractionHand.MAIN_HAND, carried);
				retarget(terminal);
			}
		}

		private @Nullable ItemEntity findItem() {
			if (terminalPos == null) {
				return null;
			}

			AABB area = new AABB(terminalPos).inflate(COLLECT_RADIUS);
			List<ItemEntity> items = level().getEntitiesOfClass(ItemEntity.class, area,
					item -> item.isAlive() && !item.hasPickUpDelay() && !item.getItem().isEmpty());
			return items.stream().min(Comparator.comparingDouble(AmethystSprite.this::distanceToSqr)).orElse(null);
		}

		private void tickToItem() {
			if (itemTarget == null || !itemTarget.isAlive() || itemTarget.getItem().isEmpty() || ++legTicks > LEG_TIMEOUT) {
				itemTarget = null;
				phase = Phase.IDLE;
				return;
			}

			if (distanceToSqr(itemTarget) > PICKUP_SQR) {
				if (legTicks % 10 == 1 || getNavigation().isDone()) {
					getNavigation().moveTo(itemTarget.getX(), itemTarget.getY() + 0.5, itemTarget.getZ(), FLY_SPEED * speedFactor());
				}

				return;
			}

			ItemStack stack = itemTarget.getItem();
			ItemStack taken = stack.split(Math.min(stack.getCount(), stack.getMaxStackSize()));
			itemTarget.setItem(stack);

			if (stack.isEmpty()) {
				itemTarget.discard();
			}

			setItemInHand(InteractionHand.MAIN_HAND, taken);
			playSound(SoundEvents.ITEM_PICKUP, 0.5f, 1.8f);
			itemTarget = null;
			phase = Phase.DELIVER;
			legTicks = 0;
			setStatus("jtechstorage.sprite.status.delivering", taken.getHoverName());
		}

		/** Parked backpack with contents within {@link #BACKPACK_RADIUS} blocks (around the sprite or terminal). */
		private @Nullable BlockPos findBackpack() {
			BlockPos best = null;
			double bestDistance = Double.MAX_VALUE;

			for (BlockPos center : terminalPos == null ? List.of(blockPosition()) : List.of(blockPosition(), terminalPos)) {
				int minX = (center.getX() - BACKPACK_RADIUS) >> 4;
				int maxX = (center.getX() + BACKPACK_RADIUS) >> 4;
				int minZ = (center.getZ() - BACKPACK_RADIUS) >> 4;
				int maxZ = (center.getZ() + BACKPACK_RADIUS) >> 4;

				for (int cx = minX; cx <= maxX; cx++) {
					for (int cz = minZ; cz <= maxZ; cz++) {
						if (!level().hasChunk(cx, cz)) {
							continue;
						}

						for (BlockEntity entity : level().getChunk(cx, cz).getBlockEntities().values()) {
							if (entity instanceof PlacedBackpackBlockEntity backpack && !backpack.isEmpty()) {
								double distance = backpack.getBlockPos().distSqr(center);

								if (distance <= BACKPACK_RADIUS * BACKPACK_RADIUS && distance < bestDistance) {
									bestDistance = distance;
									best = backpack.getBlockPos();
								}
							}
						}
					}
				}
			}

			return best;
		}

		private void tickToBackpack() {
			if (backpackTarget == null || ++legTicks > LEG_TIMEOUT) {
				backpackTarget = null;
				phase = Phase.IDLE;
				return;
			}

			if (!arrived(backpackTarget)) {
				flyTo(backpackTarget);
				return;
			}

			if (!(level().getBlockEntity(backpackTarget) instanceof PlacedBackpackBlockEntity backpack)) {
				backpackTarget = null;
				phase = Phase.IDLE;
				return;
			}

			for (int slot = 0; slot < backpack.getContainerSize(); slot++) {
				ItemStack stack = backpack.getItem(slot);

				if (!stack.isEmpty()) {
					setItemInHand(InteractionHand.MAIN_HAND, backpack.removeItem(slot, stack.getCount()));
					playSound(SoundEvents.BUNDLE_REMOVE_ONE, 0.7f, 1.4f);
					((ServerLevel) level()).sendParticles(ParticleTypes.WITCH, backpackTarget.getX() + 0.5, backpackTarget.getY() + 0.9,
							backpackTarget.getZ() + 0.5, 5, 0.2, 0.1, 0.2, 0.01);
					phase = Phase.DELIVER;
					legTicks = 0;
					setStatus("jtechstorage.sprite.status.delivering", getMainHandItem().getHoverName());
					backpackTarget = null;
					return;
				}
			}

			backpackTarget = null;
			phase = Phase.IDLE;
		}

		private void tickDeliver(StorageTerminalBlockEntity terminal) {
			ItemStack carried = getMainHandItem();

			if (carried.isEmpty() || ++legTicks > LEG_TIMEOUT) {
				returnCarriedToNetwork();
				phase = Phase.IDLE;
				return;
			}

			BlockPos pos = terminal.getBlockPos();

			if (!arrived(pos)) {
				flyTo(pos);
				return;
			}

			ItemStack rest = terminal.buildNetwork().insert(carried.copy());
			setItemInHand(InteractionHand.MAIN_HAND, rest);
			playSound(ModSounds.SPRITE_WORK, 0.6f, 1.1f);
			playAction();
			((ServerLevel) level()).sendParticles(ParticleTypes.WITCH, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5,
					6, 0.25, 0.1, 0.25, 0.01);
			phase = Phase.IDLE;
			cooldown = 5;

			if (!rest.isEmpty()) {
				setStatus("jtechstorage.sprite.status.full");
			}
		}

		private void tickToTidy(StorageTerminalBlockEntity terminal) {
			if (tidyTask == null || ++legTicks > LEG_TIMEOUT) {
				phase = Phase.IDLE;
				return;
			}

			BlockPos pos = tidyTask.pos();

			if (!arrived(pos)) {
				flyTo(pos);
				return;
			}

			int moved = SortingPlanner.tidy(terminal.buildNetwork(), pos);

			if (moved > 0) {
				playSound(SoundEvents.BUNDLE_INSERT, 0.8f, 1.2f);
				playSound(ModSounds.SPRITE_WORK, 0.5f, 1.2f);
				playAction();
				((ServerLevel) level()).sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 1.1,
						pos.getZ() + 0.5, 8, 0.3, 0.1, 0.3, 0.0);
			}

			setStatus("jtechstorage.sprite.status.tidied");
			tidyTask = null;
			phase = Phase.IDLE;
			cooldown = 10;
		}

		private void retarget(StorageTerminalBlockEntity terminal) {
			ItemStack carried = getMainHandItem();
			SortingPlanner.Destination destination = SortingPlanner.destinationFor(terminal.buildNetwork(), terminal, carried);

			if (destination == null) {
				// No room anywhere by the rules → anywhere in the network so nothing is lost
				returnCarriedToNetwork();
				phase = Phase.IDLE;
				setStatus("jtechstorage.sprite.status.full");
				return;
			}

			target = destination.container();
			targetPos = destination.pos();
			phase = Phase.TO_TARGET;
			legTicks = 0;
		}

		private boolean arrived(BlockPos pos) {
			return distanceToSqr(Vec3.atCenterOf(pos)) <= REACH_SQR;
		}

		private void flyTo(BlockPos pos) {
			if (legTicks % 20 == 1 || getNavigation().isDone()) {
				getNavigation().moveTo(pos.getX() + 0.5, pos.getY() + 1.3, pos.getZ() + 0.5, FLY_SPEED * speedFactor());
			}
		}

		private void hoverNear(@Nullable BlockPos pos) {
			if (pos != null && distanceToSqr(Vec3.atCenterOf(pos)) > 36 && getNavigation().isDone()) {
				getNavigation().moveTo(pos.getX() + 0.5, pos.getY() + 2.0, pos.getZ() + 0.5, 0.7);
			}
		}
	}

	private static boolean isGone(Container container) {
		return container instanceof BlockEntity blockEntity && blockEntity.isRemoved();
	}

	/** Takes a stack of the prototype from exactly this container (up to the maximum stack size). */
	private static ItemStack takeFrom(Container container, ItemStack prototype) {
		ItemStack result = ItemStack.EMPTY;
		int remaining = prototype.getMaxStackSize();

		for (int slot = 0; slot < container.getContainerSize() && remaining > 0; slot++) {
			ItemStack stack = container.getItem(slot);

			if (stack.isEmpty() || !ItemStack.isSameItemSameComponents(stack, prototype)) {
				continue;
			}

			ItemStack taken = container.removeItem(slot, Math.min(remaining, stack.getCount()));
			remaining -= taken.getCount();

			if (result.isEmpty()) {
				result = taken;
			} else {
				result.grow(taken.getCount());
			}
		}

		if (!result.isEmpty()) {
			container.setChanged();
		}

		return result;
	}

	/** Carried item back into the network (or dropped if everything is really full). */
	private void returnCarriedToNetwork() {
		ItemStack carried = getMainHandItem();

		if (carried.isEmpty() || !(level() instanceof ServerLevel serverLevel)) {
			return;
		}

		StorageTerminalBlockEntity terminal = terminal();
		ItemStack rest = terminal != null ? terminal.buildNetwork().insert(carried.copy()) : carried.copy();

		if (!rest.isEmpty()) {
			spawnAtLocation(serverLevel, rest);
		}

		setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
	}

	@Override
	protected SoundEvent helperSleepSound() {
		return ModSounds.SPRITE_SLEEP;
	}

	@Override
	protected SoundEvent helperAmbientSound() {
		return ModSounds.SPRITE_AMBIENT;
	}
}
