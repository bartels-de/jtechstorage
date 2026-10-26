package dev.bartels.jtechstorage.farm;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.DirectionalPlaceContext;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Block Placer (v0.38): looks and behaves like a dispenser (9 slots, redstone pulse, orientation in all six
 * directions), but places the next block from its inventory directly in front of it into the world – also saplings, seeds,
 * mushrooms, sugar cane and so on, as long as they can stand there. If the spot is occupied or nothing fits, it just clicks.
 */
public class BlockPlacerBlock extends DispenserBlock {
	public BlockPlacerBlock(Properties properties) {
		super(properties);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new BlockPlacerBlockEntity(pos, state);
	}

	@Override
	protected void dispenseFrom(ServerLevel level, BlockState state, BlockPos pos) {
		if (!(level.getBlockEntity(pos) instanceof BlockPlacerBlockEntity placer)) {
			return;
		}

		Direction facing = state.getValue(FACING);
		BlockPos target = pos.relative(facing);

		if (level.getBlockState(target).canBeReplaced()) {
			for (int slot = 0; slot < placer.getContainerSize(); slot++) {
				ItemStack stack = placer.getItem(slot);

				if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem blockItem)) {
					continue;
				}

				InteractionResult result = blockItem.place(new DirectionalPlaceContext(level, target, facing, stack, facing.getOpposite()));

				if (result.consumesAction()) {
					placer.setChanged();
					level.levelEvent(1000, pos, 0);   // click like the dispenser
					return;
				}
			}
		}

		level.levelEvent(1001, pos, 0);   // nothing to place
	}
}
