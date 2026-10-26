package dev.bartels.jtechstorage.menu;

import dev.bartels.jtechstorage.helpers.HelpersContent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jspecify.annotations.Nullable;

import io.netty.buffer.ByteBuf;

import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import dev.bartels.jtechstorage.entity.Chel;
import dev.bartels.jtechstorage.net.ChelOrderActionPayload;
import dev.bartels.jtechstorage.net.ChelOrdersPayload;

/**
 * Chel's order menu: 8 order fields (not real slots – just an item template + target amount) and the player inventory,
 * from which items are placed onto a field with the cursor.
 */
public class ChelOrderMenu extends AbstractContainerMenu {
	public static final int INVENTORY_Y = 128;

	public record OpenData(int entityId) {
		public static final StreamCodec<ByteBuf, OpenData> STREAM_CODEC = ByteBufCodecs.VAR_INT.map(OpenData::new, OpenData::entityId);
	}

	private final @Nullable Chel chel;
	private final Player player;
	private List<ChelOrdersPayload.Entry> lastSent = List.of();
	private @Nullable Component lastStatus;
	private boolean forceSync = true;
	private int ticks;

	private List<ChelOrdersPayload.Entry> clientEntries = Collections.nCopies(Chel.MAX_ORDERS, ChelOrdersPayload.Entry.EMPTY);
	private Component clientStatus = Component.empty();

	/** Client constructor. */
	public ChelOrderMenu(int containerId, Inventory inventory, OpenData data) {
		this(containerId, inventory, (Chel) null);
	}

	/** Server constructor. */
	public ChelOrderMenu(int containerId, Inventory inventory, @Nullable Chel chel) {
		super(HelpersContent.CHEL_ORDER_MENU, containerId);
		this.chel = chel;
		this.player = inventory.player;

		for (int row = 0; row < 3; row++) {
			for (int col = 0; col < 9; col++) {
				addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, INVENTORY_Y + row * 18));
			}
		}

		for (int col = 0; col < 9; col++) {
			addSlot(new Slot(inventory, col, 8 + col * 18, INVENTORY_Y + 58));
		}
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		return ItemStack.EMPTY;
	}

	@Override
	public boolean stillValid(Player player) {
		return chel == null || chel.isAlive() && !chel.isRemoved() && player.distanceToSqr(chel) < 8 * 8;
	}

	@Override
	public void broadcastChanges() {
		super.broadcastChanges();

		if (chel == null || !(this.player instanceof ServerPlayer player)) {
			return;
		}

		if (!forceSync && ++ticks % 10 != 0) {
			return;
		}

		forceSync = false;
		long[] stock = chel.stockForOrders();
		List<ChelOrdersPayload.Entry> entries = new ArrayList<>();

		for (int slot = 0; slot < Chel.MAX_ORDERS; slot++) {
			entries.add(new ChelOrdersPayload.Entry(chel.orderPrototype(slot), chel.orderTarget(slot), stock[slot]));
		}

		Component status = chel.statusText();
		boolean same = lastSent.size() == entries.size() && status.equals(lastStatus);

		for (int i = 0; same && i < entries.size(); i++) {
			same = entries.get(i).sameAs(lastSent.get(i));
		}

		if (!same) {
			lastSent = entries;
			lastStatus = status;
			ServerPlayNetworking.send(player, new ChelOrdersPayload(containerId, entries, status));
		}
	}

	public void handleAction(ServerPlayer player, int slot, int action, int amount) {
		if (chel == null || slot < 0 || slot >= Chel.MAX_ORDERS) {
			return;
		}

		switch (action) {
			case ChelOrderActionPayload.SET -> {
				ItemStack carried = getCarried();

				if (!carried.isEmpty()) {
					Component error = chel.setOrder(slot, carried);

					if (error != null) {
						player.sendOverlayMessage(error);
					}
				}
			}
			case ChelOrderActionPayload.ADJUST -> chel.adjustOrder(slot, amount);
			case ChelOrderActionPayload.CLEAR -> chel.clearOrder(slot);
			default -> {
			}
		}

		forceSync = true;
		broadcastChanges();
	}

	// --- Client ---------------------------------------------------------

	public void setClientData(List<ChelOrdersPayload.Entry> entries, Component status) {
		clientEntries = entries;
		clientStatus = status;
	}

	public List<ChelOrdersPayload.Entry> getClientEntries() {
		return clientEntries;
	}

	public Component getClientStatus() {
		return clientStatus;
	}
}
