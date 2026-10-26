package dev.bartels.jtechstorage.farm;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/**
 * Protection zones of the Farm Monitors (v0.27.0): companions do not attack monsters within them. Monitors register
 * themselves while ticking and unregister on removal – only loaded farms count, like the farms themselves.
 */
public final class FarmZones {
	private static final Map<ResourceKey<Level>, Map<BlockPos, Integer>> ZONES = new HashMap<>();

	private FarmZones() {
	}

	static void set(Level level, BlockPos pos, int radius) {
		Map<BlockPos, Integer> zones = ZONES.computeIfAbsent(level.dimension(), k -> new HashMap<>());

		if (radius > 0) {
			zones.put(pos.immutable(), radius);
		} else {
			zones.remove(pos);
		}
	}

	static void remove(Level level, BlockPos pos) {
		Map<BlockPos, Integer> zones = ZONES.get(level.dimension());

		if (zones != null) {
			zones.remove(pos);
		}
	}

	/** Is the position inside a Farm Monitor's protection zone? */
	public static boolean isProtected(Level level, BlockPos pos) {
		if (level.isClientSide()) {
			return false;
		}

		Map<BlockPos, Integer> zones = ZONES.get(level.dimension());

		if (zones == null || zones.isEmpty()) {
			return false;
		}

		for (Map.Entry<BlockPos, Integer> zone : zones.entrySet()) {
			int r = zone.getValue();

			if (zone.getKey().distSqr(pos) <= (double) r * r) {
				return true;
			}
		}

		return false;
	}

	public static void clear() {
		ZONES.clear();
	}
}
