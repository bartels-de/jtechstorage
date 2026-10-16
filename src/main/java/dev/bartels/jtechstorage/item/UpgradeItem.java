package dev.bartels.jtechstorage.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Upgrade für das Speicher-Terminal: erhöht entweder die Reichweite oder die Anzahl verwaltbarer Kisten. */
public class UpgradeItem extends Item {
	public enum Kind {
		/** Reichweite in Blöcken. */
		RANGE,
		/** Anzahl verknüpfbarer Kisten. */
		CAPACITY
	}

	private final Kind kind;
	private final int value;
	private final boolean ender;

	public UpgradeItem(Kind kind, int value, Properties properties) {
		this(kind, value, false, properties);
	}

	/** @param ender Ender-Upgrade: Funk-Terminal ohne Reichweitenlimit und über Dimensionen hinweg */
	public UpgradeItem(Kind kind, int value, boolean ender, Properties properties) {
		super(properties);
		this.kind = kind;
		this.value = value;
		this.ender = ender;
	}

	public boolean isEnder() {
		return ender;
	}

	public static boolean isEnder(ItemStack stack) {
		return stack.getItem() instanceof UpgradeItem upgrade && upgrade.ender;
	}

	public Kind kind() {
		return kind;
	}

	public int value() {
		return value;
	}

	public static boolean is(ItemStack stack, Kind kind) {
		return stack.getItem() instanceof UpgradeItem upgrade && upgrade.kind == kind;
	}

	/** Wert eines Upgrade-Slots (leer oder falscher Typ = Grundwert). */
	public static int valueOf(ItemStack stack, Kind kind, int base) {
		return stack.getItem() instanceof UpgradeItem upgrade && upgrade.kind == kind ? Math.max(base, upgrade.value) : base;
	}
}
