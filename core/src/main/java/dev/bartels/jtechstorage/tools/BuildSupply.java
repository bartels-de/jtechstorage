package dev.bartels.jtechstorage.tools;

import net.minecraft.util.Prediction;
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
 * Supply for the building tools (v0.39): first from the inventory, then – if the tool is linked to a terminal
 * and you are within wireless range – from storage. Returned blocks go to storage or the inventory.
 */
final class BuildSupply {
	private final Player player;
	private final @Nullable StorageNetwork network;

	BuildSupply(Player player, ItemStack tool, Level level) {
		this.player = player;
		StorageTerminalBlockEntity terminal = player.isCreative() ? null : LinkedTerminalItem.quietTerminal(tool, level, player);
		this.network = terminal != null ? terminal.buildNetwork() : null;
	}

	/** Fetches up to {@code amount} of {@code item}; returns the amount obtained. */
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

	/** How many of {@code item} are available (inventory + storage). */
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

	/** Returns items: to storage, otherwise to the inventory, otherwise at the feet. */
	void give(ItemStack stack) {
		if (player.isCreative() || stack.isEmpty()) {
			return;
		}

		ItemStack rest = network != null ? network.insert(stack) : stack;

		if (!rest.isEmpty() && !player.getInventory().add(rest)) {
			player.drop(rest, false, Prediction.SERVER_ONLY);
		}
	}

	boolean linked() {
		return network != null;
	}
}
