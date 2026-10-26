package dev.bartels.jtechstorage.chest;

import net.minecraft.world.item.ItemStack;

/** Tiers of the amethyst chests (v0.28.0): columns × rows and stack factor. */
public enum AmethystChestTier {
	AMETHYST("amethyst_chest", 9, 6, 2),
	CRYSTAL("crystal_chest", 9, 9, 4),
	RESONANCE("resonance_chest", 12, 9, 8);

	public final String id;
	public final int columns;
	public final int rows;
	/** How many normal stacks fit into one slot. */
	public final int stackFactor;

	AmethystChestTier(String id, int columns, int rows, int stackFactor) {
		this.id = id;
		this.columns = columns;
		this.rows = rows;
		this.stackFactor = stackFactor;
	}

	public int size() {
		return columns * rows;
	}

	/** Largest stack of this item in a slot; unstackable items stay single. */
	public int maxStack(ItemStack stack) {
		int base = stack.getMaxStackSize();
		return base <= 1 ? base : base * stackFactor;
	}
}
