package dev.bartels.jtechstorage.farm;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.HopperMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

/** Contents and tick rate of the Amethyst Hopper. */
public class AmethystHopperBlockEntity extends BaseContainerBlockEntity {
	public static final int SIZE = 5;
	private NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
	private int cooldown;

	public AmethystHopperBlockEntity(BlockPos pos, BlockState state) {
		super(FarmContent.AMETHYST_HOPPER_BLOCK_ENTITY, pos, state);
	}

	public static void serverTick(Level level, BlockPos pos, BlockState state, AmethystHopperBlockEntity hopper) {
		if (--hopper.cooldown > 0 || !state.getValue(HopperBlock.ENABLED)) {
			return;
		}

		hopper.cooldown = ItemMover.INTERVAL;
		Direction facing = state.getValue(HopperBlock.FACING);
		boolean changed = false;

		// output
		Container target = ItemMover.containerAt(level, pos.relative(facing));

		if (target != null && target != hopper) {
			changed |= ItemMover.move(hopper, facing, target, facing.getOpposite(), ItemMover.PER_STEP, stack -> true) > 0;
		}

		// pull from the container above
		Container above = ItemMover.containerAt(level, pos.above());

		if (above != null) {
			changed |= ItemMover.move(above, Direction.DOWN, hopper, Direction.UP, ItemMover.PER_STEP, stack -> true) > 0;
		} else {
			// suck in loose items in the 3×3 area above the hopper
			AABB area = new AABB(pos.getX() - 1, pos.getY() + 0.5, pos.getZ() - 1, pos.getX() + 2, pos.getY() + 2.0, pos.getZ() + 2);

			for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, area, ItemEntity::isAlive)) {
				changed |= HopperBlockEntity.addItem(hopper, item);
			}
		}

		if (changed) {
			hopper.setChanged();
		}
	}

	@Override
	protected Component getDefaultName() {
		return Component.translatable("block.jtechstorage.amethyst_hopper");
	}

	@Override
	protected NonNullList<ItemStack> getItems() {
		return items;
	}

	@Override
	protected void setItems(NonNullList<ItemStack> items) {
		this.items = items;
	}

	@Override
	protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
		return new HopperMenu(containerId, inventory, this);
	}

	@Override
	public int getContainerSize() {
		return SIZE;
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
		ContainerHelper.loadAllItems(input, items);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		ContainerHelper.saveAllItems(output, items);
	}
}
