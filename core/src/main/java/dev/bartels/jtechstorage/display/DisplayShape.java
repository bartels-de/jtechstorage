package dev.bartels.jtechstorage.display;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;


/**
 * Combined storage screen (v0.34): equally oriented screens side by side form a rectangle up to
 * 4 × 3. The top-left block (from the viewer's perspective) is the origin - it stores the settings and draws
 * the whole image.
 *
 * @param origin top-left block
 * @param width  width in blocks (to the right)
 * @param height height in blocks (downwards)
 */
public record DisplayShape(BlockPos origin, int width, int height) {
	public static final int MAX_WIDTH = 4;
	public static final int MAX_HEIGHT = 3;

	/** Right from the perspective of a viewer looking at the front. */
	public static Direction right(Direction facing) {
		return facing.getCounterClockWise();
	}

	private static boolean same(BlockGetter level, BlockPos pos, Direction facing) {
		BlockState state = level.getBlockState(pos);
		return state.is(DisplayContent.STORAGE_DISPLAY) && state.getValue(BlockStateProperties.HORIZONTAL_FACING) == facing;
	}

	/** Rectangle of the screen at {@code pos} (reads the orientation from the block). */
	public static DisplayShape of(BlockGetter level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		return state.hasProperty(BlockStateProperties.HORIZONTAL_FACING) ? of(level, pos, state.getValue(BlockStateProperties.HORIZONTAL_FACING))
				: new DisplayShape(pos, 1, 1);
	}

	/** Rectangle {@code pos} belongs to; a single block if no valid rectangle fits. */
	public static DisplayShape of(BlockGetter level, BlockPos pos, Direction facing) {
		Direction right = right(facing);
		Direction left = right.getOpposite();

		for (int up = MAX_HEIGHT - 1; up >= 0; up--) {
			for (int back = MAX_WIDTH - 1; back >= 0; back--) {
				BlockPos origin = pos.relative(left, back).above(up);

				if (!same(level, origin, facing) || same(level, origin.relative(left), facing) || same(level, origin.above(), facing)) {
					continue;
				}

				int width = 1;

				while (width < MAX_WIDTH && same(level, origin.relative(right, width), facing)) {
					width++;
				}

				int height = 1;

				rows:
				while (height < MAX_HEIGHT) {
					BlockPos row = origin.below(height);

					for (int k = 0; k < width; k++) {
						if (!same(level, row.relative(right, k), facing)) {
							break rows;
						}
					}

					height++;
				}

				if (back < width && up < height) {
					return new DisplayShape(origin, width, height);
				}
			}
		}

		return new DisplayShape(pos, 1, 1);
	}
}
