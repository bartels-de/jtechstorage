package dev.bartels.jtechstorage.entity;

import java.util.Map;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;

/**
 * Feste Kategorie-Zuordnung der Kisten eines Netzwerks (gespeichert im Terminal).
 * Schlüssel ist die verknüpfte Position (bei Doppeltruhen gilt sie für beide Hälften).
 */
public interface SortAssignments {
	@Nullable SortCategory get(BlockPos key);

	void assign(BlockPos key, SortCategory category);

	void release(BlockPos key);

	Map<BlockPos, SortCategory> asMap();
}
