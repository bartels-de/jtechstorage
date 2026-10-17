package dev.bartels.jtechstorage.item;

import java.util.function.Consumer;
import java.util.function.IntSupplier;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/**
 * Upgrade für das Speicher-Terminal: erhöht entweder die Reichweite oder die Anzahl verwaltbarer Kisten.
 * Der Wert kommt aus der Konfiguration ({@code config/jtechstorage.json}).
 */
public class UpgradeItem extends Item {
	public enum Kind {
		/** Reichweite in Blöcken. */
		RANGE,
		/** Anzahl verknüpfbarer Kisten. */
		CAPACITY
	}

	private final Kind kind;
	private final IntSupplier value;
	private final boolean ender;

	public UpgradeItem(Kind kind, IntSupplier value, Properties properties) {
		this(kind, value, false, properties);
	}

	/** @param ender Ender-Upgrade: Funk-Terminal ohne Reichweitenlimit und über Dimensionen hinweg */
	public UpgradeItem(Kind kind, IntSupplier value, boolean ender, Properties properties) {
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
		return value.getAsInt();
	}

	public static boolean is(ItemStack stack, Kind kind) {
		return stack.getItem() instanceof UpgradeItem upgrade && upgrade.kind == kind;
	}

	/** Wert eines Upgrade-Slots (leer oder falscher Typ = Grundwert). */
	public static int valueOf(ItemStack stack, Kind kind, int base) {
		return stack.getItem() instanceof UpgradeItem upgrade && upgrade.kind == kind ? Math.max(base, upgrade.value()) : base;
	}

	@Override
	@SuppressWarnings("deprecation")   // in 26.x als veraltet markiert, funktioniert aber weiterhin
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
		String key = kind == Kind.RANGE ? "item.jtechstorage.range_upgrade.lore" : "item.jtechstorage.capacity_upgrade.lore";
		tooltip.accept(Component.translatable(key, value()).withStyle(ChatFormatting.GRAY));

		if (ender) {
			tooltip.accept(Component.translatable("item.jtechstorage.ender_range_upgrade.lore").withStyle(ChatFormatting.DARK_PURPLE));
		}
	}
}
