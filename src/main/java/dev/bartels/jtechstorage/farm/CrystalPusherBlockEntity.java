package dev.bartels.jtechstorage.farm;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.ItemStackWithSlot;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Filter und Takt des Kristall-Schiebers. */
public class CrystalPusherBlockEntity extends BlockEntity {
	public static final int FILTERS = 9;
	/** Filter-Vorlagen (je 1 Stück, werden nie eingelagert). */
	private final SimpleContainer filters = new SimpleContainer(FILTERS) {
		@Override
		public void setChanged() {
			super.setChanged();
			CrystalPusherBlockEntity.this.setChanged();
		}
	};
	private int cooldown;

	public CrystalPusherBlockEntity(BlockPos pos, BlockState state) {
		super(FarmContent.CRYSTAL_PUSHER_BLOCK_ENTITY, pos, state);
	}

	public SimpleContainer getFilters() {
		return filters;
	}

	private boolean accepts(ItemStack stack) {
		boolean any = false;

		for (int i = 0; i < FILTERS; i++) {
			ItemStack filter = filters.getItem(i);

			if (!filter.isEmpty()) {
				any = true;

				if (ItemStack.isSameItem(filter, stack)) {
					return true;
				}
			}
		}

		return !any;
	}

	public static void serverTick(Level level, BlockPos pos, BlockState state, CrystalPusherBlockEntity pusher) {
		if (--pusher.cooldown > 0) {
			return;
		}

		pusher.cooldown = ItemMover.INTERVAL;

		if (level.hasNeighborSignal(pos)) {
			return;
		}

		Direction facing = state.getValue(CrystalPusherBlock.FACING);
		Container source = ItemMover.containerAt(level, pos.relative(facing.getOpposite()));
		Container target = ItemMover.containerAt(level, pos.relative(facing));

		if (source == null || target == null || source == target) {
			return;
		}

		ItemMover.move(source, facing, target, facing.getOpposite(), ItemMover.PER_STEP, pusher::accepts);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		ValueOutput.TypedOutputList<ItemStackWithSlot> list = output.list("Filters", ItemStackWithSlot.CODEC);

		for (int slot = 0; slot < FILTERS; slot++) {
			if (!filters.getItem(slot).isEmpty()) {
				list.add(new ItemStackWithSlot(slot, filters.getItem(slot)));
			}
		}
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);

		for (ItemStackWithSlot item : input.listOrEmpty("Filters", ItemStackWithSlot.CODEC)) {
			if (item.isValidInContainer(FILTERS)) {
				filters.setItem(item.slot(), item.stack());
			}
		}
	}
}
