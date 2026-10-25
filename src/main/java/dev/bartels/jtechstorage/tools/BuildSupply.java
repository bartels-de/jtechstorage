package dev.bartels.jtechstorage.tools;

import org.jspecify.annotations.Nullable;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import dev.bartels.jtechstorage.block.StorageTerminalBlockEntity;
import dev.bartels.jtechstorage.item.LinkedTerminalItem;
import dev.bartels.jtechstorage.network.StorageNetwork;

/**
 * Nachschub für die Bau-Werkzeuge (v0.39): zuerst aus dem Inventar, dann – wenn das Werkzeug an ein Terminal gekoppelt
 * ist und man in Funkreichweite steht – aus dem Lager. Zurückgegebene Blöcke wandern ins Lager bzw. ins Inventar.
 */
final class BuildSupply {
	private final Player player;
	private final @Nullable StorageNetwork network;

	BuildSupply(Player player, ItemStack tool, Level level) {
		this.player = player;
		StorageTerminalBlockEntity terminal = player.isCreative() ? null : LinkedTerminalItem.quietTerminal(tool, level, player);
		this.network = terminal != null ? terminal.buildNetwork() : null;
	}

	/** Holt bis zu {@code amount} Stück von {@code item}; gibt die erhaltene Anzahl zurück. */
	int take(Item item, int amount) {
		if (player.isCreative()) {
			return amount;
		}

		int got = 0;
		Inventory inv = player.getInventory();

		for (int i = 0; i < inv.getContainerSize() && got < amount; i++) {
			ItemStack stack = inv.getItem(i);

			if (stack.is(item) && !stack.isDamageableItem()) {
				int n = Math.min(stack.getCount(), amount - got);
				stack.shrink(n);
				got += n;
			}
		}

		if (got < amount && network != null) {
			ItemStack pulled = network.extract(new ItemStack(item), amount - got);
			got += pulled.getCount();
		}

		return got;
	}

	/** Wie viele Stück von {@code item} verfügbar sind (Inventar + Lager). */
	long available(Item item) {
		if (player.isCreative()) {
			return Long.MAX_VALUE;
		}

		long n = 0;
		Inventory inv = player.getInventory();

		for (int i = 0; i < inv.getContainerSize(); i++) {
			if (inv.getItem(i).is(item)) {
				n += inv.getItem(i).getCount();
			}
		}

		return network != null ? n + network.count(new ItemStack(item)) : n;
	}

	/** Gibt Gegenstände zurück: ins Lager, sonst ins Inventar, sonst vor die Füße. */
	void give(ItemStack stack) {
		if (player.isCreative() || stack.isEmpty()) {
			return;
		}

		ItemStack rest = network != null ? network.insert(stack) : stack;

		if (!rest.isEmpty() && !player.getInventory().add(rest)) {
			player.drop(rest, false, net.minecraft.util.Prediction.SERVER_ONLY);
		}
	}

	boolean linked() {
		return network != null;
	}
}
