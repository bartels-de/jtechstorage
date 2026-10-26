package dev.bartels.jtechstorage.entity;

import java.util.Map;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;

/**
 * Fixed category assignment of a network's chests (stored in the terminal).
 * The key is the linked position (for double chests it applies to both halves).
 */
public interface SortAssignments {
	@Nullable SortCategory get(BlockPos key);

	void assign(BlockPos key, SortCategory category);

	void release(BlockPos key);

	Map<BlockPos, SortCategory> asMap();
}
