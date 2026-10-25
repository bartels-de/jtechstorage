package dev.bartels.jtechstorage.tools;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/** Zusammenhängende Fläche in der Ebene quer zu einer Blockseite (v0.39), höchstens {@code radius} vom Start entfernt. */
final class WandArea {
	private WandArea() {
	}

	static List<BlockPos> collect(BlockPos start, Direction face, int radius, Predicate<BlockPos> accept) {
		List<BlockPos> out = new ArrayList<>();
		Set<BlockPos> seen = new HashSet<>();
		ArrayDeque<BlockPos> queue = new ArrayDeque<>();
		queue.add(start);
		seen.add(start);
		List<Direction> spread = new ArrayList<>();

		for (Direction d : Direction.values()) {
			if (d.getAxis() != face.getAxis()) {
				spread.add(d);
			}
		}

		while (!queue.isEmpty()) {
			BlockPos p = queue.poll();

			if (!accept.test(p)) {
				continue;
			}

			out.add(p);

			for (Direction d : spread) {
				BlockPos n = p.relative(d);

				if (seen.add(n) && Math.abs(n.getX() - start.getX()) <= radius && Math.abs(n.getY() - start.getY()) <= radius
						&& Math.abs(n.getZ() - start.getZ()) <= radius) {
					queue.add(n);
				}
			}
		}

		return out;
	}
}
