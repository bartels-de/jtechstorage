package dev.bartels.jtechstorage.logistics;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.ItemStackWithSlot;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Puffer (1 Stapel), drei Filterreihen (links, geradeaus, rechts) und der Rest-Modus der Sortier-Weiche. */
public class SorterBlockEntity extends BlockEntity implements Container {
	public static final int ROW = 9;
	public static final int LEFT = 0;
	public static final int STRAIGHT = 1;
	public static final int RIGHT = 2;
	private static final int INTERVAL = 4;
	private static final int PER_STEP = 8;

	private ItemStack buffer = ItemStack.EMPTY;
	private final SimpleContainer filters = new SimpleContainer(ROW * 3) {
		@Override
		public void setChanged() {
			super.setChanged();
			SorterBlockEntity.this.setChanged();
		}
	};
	/** 0 = Rest geradeaus, 1 = Rest zurückhalten. */
	private int holdRest;
	private int cooldown;
	private final ContainerData data = new ContainerData() {
		@Override
		public int get(int index) {
			return holdRest;
		}

		@Override
		public void set(int index, int value) {
			holdRest = value;
		}

		@Override
		public int getCount() {
			return 1;
		}
	};

	public SorterBlockEntity(BlockPos pos, BlockState state) {
		super(LogisticsContent.SORTER_BLOCK_ENTITY, pos, state);
	}

	public SimpleContainer getFilters() {
		return filters;
	}

	public ContainerData getData() {
		return data;
	}

	public void toggleRest() {
		holdRest = 1 - holdRest;
		setChanged();
	}

	/** Reihe des ersten passenden Filters oder -1. */
	private int row(ItemStack stack) {
		for (int r = 0; r < 3; r++) {
			for (int i = 0; i < ROW; i++) {
				ItemStack filter = filters.getItem(r * ROW + i);

				if (!filter.isEmpty() && ItemStack.isSameItem(filter, stack)) {
					return r;
				}
			}
		}

		return -1;
	}

	public static void serverTick(Level level, BlockPos pos, BlockState state, SorterBlockEntity sorter) {
		if (sorter.buffer.isEmpty() || --sorter.cooldown > 0) {
			return;
		}

		sorter.cooldown = INTERVAL;

		if (level.hasNeighborSignal(pos)) {
			return;
		}

		int row = sorter.row(sorter.buffer);

		if (row < 0) {
			if (sorter.holdRest == 1) {
				return;
			}

			row = STRAIGHT;
		}

		Direction facing = state.getValue(SorterBlock.FACING);
		Direction out = row == LEFT ? facing.getCounterClockWise() : row == RIGHT ? facing.getClockWise() : facing;
		BlockPos target = pos.relative(out);
		int n = Math.min(PER_STEP, sorter.buffer.getCount());
		Container container = HopperBlockEntity.getContainerAt(level, target);
		int done;

		if (container != null) {
			ItemStack rest = HopperBlockEntity.addItem(sorter, container, sorter.buffer.copyWithCount(n), out.getOpposite());
			done = n - rest.getCount();
		} else if (level.getBlockState(target).getCollisionShape(level, target).isEmpty()
				|| level.getBlockState(target).getBlock() instanceof ConveyorBlock) {
			ItemEntity item = new ItemEntity(level, pos.getX() + 0.5 + out.getStepX() * 0.75, pos.getY() + 0.25, pos.getZ() + 0.5 + out.getStepZ() * 0.75,
					sorter.buffer.copyWithCount(n), out.getStepX() * 0.1, 0.0, out.getStepZ() * 0.1);
			item.setDefaultPickUpDelay();
			level.addFreshEntity(item);
			done = n;
		} else {
			done = 0;
		}

		if (done > 0) {
			sorter.buffer.shrink(done);
			sorter.setChanged();
		}
	}

	// ------------------------------------------------------------------ Container (nur der Puffer)

	@Override
	public int getContainerSize() {
		return 1;
	}

	@Override
	public boolean isEmpty() {
		return buffer.isEmpty();
	}

	@Override
	public ItemStack getItem(int slot) {
		return slot == 0 ? buffer : ItemStack.EMPTY;
	}

	@Override
	public ItemStack removeItem(int slot, int amount) {
		ItemStack taken = buffer.split(amount);
		setChanged();
		return taken;
	}

	@Override
	public ItemStack removeItemNoUpdate(int slot) {
		ItemStack taken = buffer;
		buffer = ItemStack.EMPTY;
		return taken;
	}

	@Override
	public void setItem(int slot, ItemStack stack) {
		buffer = stack;
		setChanged();
	}

	@Override
	public boolean stillValid(Player player) {
		return false;
	}

	@Override
	public void clearContent() {
		buffer = ItemStack.EMPTY;
	}

	@Override
	public void preRemoveSideEffects(BlockPos pos, BlockState state) {
		super.preRemoveSideEffects(pos, state);

		if (level != null && !level.isClientSide() && !buffer.isEmpty()) {
			Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, buffer);
			buffer = ItemStack.EMPTY;
		}
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);

		if (!buffer.isEmpty()) {
			output.store("Buffer", ItemStack.CODEC, buffer);
		}

		ValueOutput.TypedOutputList<ItemStackWithSlot> list = output.list("Filters", ItemStackWithSlot.CODEC);

		for (int slot = 0; slot < ROW * 3; slot++) {
			if (!filters.getItem(slot).isEmpty()) {
				list.add(new ItemStackWithSlot(slot, filters.getItem(slot)));
			}
		}

		output.putInt("HoldRest", holdRest);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		buffer = input.read("Buffer", ItemStack.CODEC).orElse(ItemStack.EMPTY);

		for (ItemStackWithSlot item : input.listOrEmpty("Filters", ItemStackWithSlot.CODEC)) {
			if (item.isValidInContainer(ROW * 3)) {
				filters.setItem(item.slot(), item.stack());
			}
		}

		holdRest = input.getIntOr("HoldRest", 0);
	}
}
