package dev.bartels.jtechstorage.entity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/**
 * Spawn protection of the watch moths. While a moth is awake it refreshes its ward every second; inside the ward no
 * monsters spawn naturally (see {@code NaturalSpawnerMixin}). Wards expire on their own when the moth falls asleep, is
 * packed up or unloaded. Server thread only.
 *
 * <p>Keyed by the level instance (weakly), so wards never leak into another world or survive a server restart.
 */
public final class MothWards {
	/** Ticks a ward stays valid without being refreshed (the moth refreshes every 20 ticks). */
	private static final int LIFETIME = 60;

	private static final Map<ServerLevel, Map<UUID, Ward>> WARDS = new WeakHashMap<>();

	private MothWards() {
	}

	/** Called by an awake moth: protects a cube of {@code radius} blocks around {@code home}. */
	public static void refresh(ServerLevel level, UUID moth, BlockPos home, int radius) {
		WARDS.computeIfAbsent(level, k -> new HashMap<>()).put(moth, new Ward(home.immutable(), radius, level.getGameTime() + LIFETIME));
	}

	/** The moth fell asleep or was removed: lift its ward immediately. */
	public static void remove(ServerLevel level, UUID moth) {
		Map<UUID, Ward> wards = WARDS.get(level);

		if (wards != null) {
			wards.remove(moth);
		}
	}

	/** Whether {@code pos} lies inside the ward of an awake moth. Called for every natural monster spawn attempt. */
	public static boolean protects(ServerLevel level, BlockPos pos) {
		Map<UUID, Ward> wards = WARDS.get(level);

		if (wards == null || wards.isEmpty()) {
			return false;
		}

		long now = level.getGameTime();
		wards.values().removeIf(ward -> ward.until() < now);

		for (Ward ward : wards.values()) {
			if (ward.contains(pos)) {
				return true;
			}
		}

		return false;
	}

	private record Ward(BlockPos home, int radius, long until) {
		/** Same shape as the moth's watch area: a cube around its home. */
		boolean contains(BlockPos pos) {
			return Math.abs(pos.getX() - home.getX()) <= radius && Math.abs(pos.getY() - home.getY()) <= radius
					&& Math.abs(pos.getZ() - home.getZ()) <= radius;
		}
	}
}
