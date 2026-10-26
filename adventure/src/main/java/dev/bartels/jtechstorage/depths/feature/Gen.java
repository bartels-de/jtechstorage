package dev.bartels.jtechstorage.depths.feature;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Shared helpers for the Crystal Depths structures (v0.30). */
final class Gen {
	private Gen() {
	}

	static boolean solid(BlockState state) {
		return !state.isAir() && state.getFluidState().isEmpty() && !state.canBeReplaced();
	}

	static boolean open(BlockState state) {
		return state.isAir() || (state.canBeReplaced() && state.getFluidState().isEmpty());
	}

	/** First free position above solid ground, searched downward from {@code pos} (null = nothing found). */
	static @Nullable BlockPos floor(WorldGenLevel level, BlockPos pos, int maxDown) {
		BlockPos.MutableBlockPos p = pos.mutable();

		if (!open(level.getBlockState(p))) {
			return null;
		}

		for (int i = 0; i < maxDown && p.getY() > level.getMinY() + 1; i++) {
			if (solid(level.getBlockState(p.below()))) {
				return p.immutable();
			}

			p.move(Direction.DOWN);
		}

		return null;
	}

	/** First free position below a solid ceiling, searched upward from {@code pos}. */
	static @Nullable BlockPos ceiling(WorldGenLevel level, BlockPos pos, int maxUp) {
		BlockPos.MutableBlockPos p = pos.mutable();

		if (!open(level.getBlockState(p))) {
			return null;
		}

		for (int i = 0; i < maxUp && p.getY() < level.getMaxY() - 1; i++) {
			if (solid(level.getBlockState(p.above()))) {
				return p.immutable();
			}

			p.move(Direction.UP);
		}

		return null;
	}

	/** Free height above {@code floor} up to the ceiling (at most {@code max}). */
	static int space(WorldGenLevel level, BlockPos floor, int max) {
		int h = 0;

		while (h < max && open(level.getBlockState(floor.above(h)))) {
			h++;
		}

		return h;
	}

	static void set(WorldGenLevel level, BlockPos pos, BlockState state) {
		level.setBlock(pos, state, 2);
	}

	/** Only sets where there is air or something replaceable. */
	static void place(WorldGenLevel level, BlockPos pos, BlockState state) {
		if (open(level.getBlockState(pos))) {
			level.setBlock(pos, state, 2);
		}
	}

	/** Amethyst bud or cluster pointing away from the solid surface. */
	static BlockState cluster(RandomSource random, Direction facing) {
		BlockState base = switch (random.nextInt(6)) {
			case 0 -> Blocks.SMALL_AMETHYST_BUD.defaultBlockState();
			case 1 -> Blocks.MEDIUM_AMETHYST_BUD.defaultBlockState();
			case 2, 3 -> Blocks.LARGE_AMETHYST_BUD.defaultBlockState();
			default -> Blocks.AMETHYST_CLUSTER.defaultBlockState();
		};
		return base.setValue(AmethystClusterBlock.FACING, facing);
	}
}
