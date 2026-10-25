package dev.bartels.jtechstorage.display;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;


/**
 * Zusammengesetzter Lager-Bildschirm (v0.34): gleich ausgerichtete Bildschirme nebeneinander bilden ein Rechteck bis
 * 4 × 3. Der Block oben links (aus Sicht des Betrachters) ist der Ursprung – er speichert die Einstellungen und zeichnet
 * das ganze Bild.
 *
 * @param origin Block oben links
 * @param width  Breite in Blöcken (nach rechts)
 * @param height Höhe in Blöcken (nach unten)
 */
public record DisplayShape(BlockPos origin, int width, int height) {
	public static final int MAX_WIDTH = 4;
	public static final int MAX_HEIGHT = 3;

	/** Rechts aus Sicht des Betrachters, der auf die Vorderseite schaut. */
	public static Direction right(Direction facing) {
		return facing.getCounterClockWise();
	}

	private static boolean same(BlockGetter level, BlockPos pos, Direction facing) {
		BlockState state = level.getBlockState(pos);
		return state.is(DisplayContent.STORAGE_DISPLAY) && state.getValue(BlockStateProperties.HORIZONTAL_FACING) == facing;
	}

	/** Rechteck des Bildschirms an {@code pos} (liest die Ausrichtung aus dem Block). */
	public static DisplayShape of(BlockGetter level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		return state.hasProperty(BlockStateProperties.HORIZONTAL_FACING) ? of(level, pos, state.getValue(BlockStateProperties.HORIZONTAL_FACING))
				: new DisplayShape(pos, 1, 1);
	}

	/** Rechteck, zu dem {@code pos} gehört; ein einzelner Block, wenn kein gültiges Rechteck passt. */
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
