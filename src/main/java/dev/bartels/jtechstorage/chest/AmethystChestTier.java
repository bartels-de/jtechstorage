package dev.bartels.jtechstorage.chest;

/** Stufen der Amethyst-Kisten (v0.28.0): Spalten × Reihen und Stapel-Faktor. */
public enum AmethystChestTier {
	AMETHYST("amethyst_chest", 9, 6, 2),
	CRYSTAL("crystal_chest", 9, 9, 4),
	RESONANCE("resonance_chest", 12, 9, 8);

	public final String id;
	public final int columns;
	public final int rows;
	/** Wie viele normale Stapel in einen Platz passen. */
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

	/** Größter Stapel dieses Items in einem Platz; Unstapelbares bleibt einzeln. */
	public int maxStack(net.minecraft.world.item.ItemStack stack) {
		int base = stack.getMaxStackSize();
		return base <= 1 ? base : base * stackFactor;
	}
}
