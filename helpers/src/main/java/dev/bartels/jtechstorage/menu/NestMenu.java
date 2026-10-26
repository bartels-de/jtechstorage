package dev.bartels.jtechstorage.menu;

import dev.bartels.jtechstorage.helpers.HelpersContent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import dev.bartels.jtechstorage.block.LinkedPanelBlock;
import dev.bartels.jtechstorage.block.PhoenixNestBlockEntity;
import dev.bartels.jtechstorage.block.StorageTerminalBlockEntity;
import dev.bartels.jtechstorage.block.StorageTerminalBlockEntity.Permission;
import dev.bartels.jtechstorage.entity.Phoenix;
import dev.bartels.jtechstorage.mail.PhoenixPost;
import dev.bartels.jtechstorage.net.NestActionPayload;
import dev.bartels.jtechstorage.net.NestStatePayload;

/**
 * Menu of the phoenix nest: order list (9 ghost fields: what the phoenix takes from storage), destination, send,
 * standing order, receive setting, name and inbox (real slots for overflow).
 */
public class NestMenu extends AbstractContainerMenu {
	public record OpenData(BlockPos pos) {
		public static final StreamCodec<io.netty.buffer.ByteBuf, OpenData> STREAM_CODEC =
				BlockPos.STREAM_CODEC.map(OpenData::new, OpenData::pos);
	}

	public static final int SHIP_Y = 66;
	public static final int INBOX_Y = 132;
	public static final int INVENTORY_Y = 164;
	public static final int IMAGE_HEIGHT = INVENTORY_Y + 82;
	private static final int SHIP = PhoenixNestBlockEntity.SHIPMENT_SLOTS;
	private static final int INBOX_END = SHIP + PhoenixNestBlockEntity.INBOX_SLOTS;
	private static final int REFRESH_INTERVAL = 10;

	private final Player player;
	private final @Nullable PhoenixNestBlockEntity nest;
	private final Container shipment;
	private List<GlobalPos> lastDestinations = List.of();
	private NestStatePayload.@Nullable State lastSent;
	private int ticks = REFRESH_INTERVAL;
	private NestStatePayload.State clientState = NestStatePayload.State.EMPTY;
	private int clientVersion;

	public NestMenu(int containerId, Inventory inventory, OpenData data) {
		this(containerId, inventory, null, new SimpleContainer(SHIP), new SimpleContainer(PhoenixNestBlockEntity.INBOX_SLOTS));
	}

	public NestMenu(int containerId, Inventory inventory, PhoenixNestBlockEntity nest) {
		this(containerId, inventory, nest, nest.getShipment(), nest.getInbox());
	}

	private NestMenu(int containerId, Inventory inventory, @Nullable PhoenixNestBlockEntity nest, Container shipment, Container inbox) {
		super(HelpersContent.NEST_MENU, containerId);
		this.player = inventory.player;
		this.nest = nest;
		this.shipment = shipment;

		for (int i = 0; i < SHIP; i++) {
			addSlot(new GhostSlot(shipment, i, 8 + i * 18, SHIP_Y));
		}

		for (int i = 0; i < PhoenixNestBlockEntity.INBOX_SLOTS; i++) {
			addSlot(new InboxSlot(inbox, i, 8 + i * 18, INBOX_Y));
		}

		addStandardInventorySlots(inventory, 8, INVENTORY_Y);
	}

	private @Nullable StorageTerminalBlockEntity terminal() {
		return nest != null ? nest.terminal() : null;
	}

	/** Server: actual permissions; client: last transmitted. */
	public boolean canSend() {
		if (nest != null) {
			StorageTerminalBlockEntity terminal = terminal();
			return terminal != null && terminal.can(player, Permission.ITEMS);
		}

		return clientState.canSend();
	}

	// ------------------------------------------------------------------
	// Server
	// ------------------------------------------------------------------

	@Override
	public void broadcastChanges() {
		super.broadcastChanges();

		if (nest != null && player instanceof ServerPlayer serverPlayer && ++ticks >= REFRESH_INTERVAL) {
			ticks = 0;
			sync(serverPlayer, false);
		}
	}

	private void sync(ServerPlayer serverPlayer, boolean force) {
		if (nest == null || !(nest.getLevel() instanceof ServerLevel level)) {
			return;
		}

		StorageTerminalBlockEntity terminal = terminal();
		GlobalPos self = GlobalPos.of(level.dimension(), nest.getBlockPos());
		PhoenixPost post = PhoenixPost.get(level.getServer());
		List<NestStatePayload.Destination> rows = new ArrayList<>();
		List<GlobalPos> positions = new ArrayList<>();
		int selected = -1;

		for (Map.Entry<GlobalPos, PhoenixPost.NestInfo> entry : post.destinationsFor(serverPlayer.getUUID(), self)) {
			if (entry.getKey().equals(nest.getDestination())) {
				selected = positions.size();
			}

			positions.add(entry.getKey());
			rows.add(new NestStatePayload.Destination(entry.getValue().label(), entry.getValue().color(),
					PhoenixPost.flightTicks(self, entry.getKey()) / 20));
		}

		lastDestinations = positions;
		Phoenix phoenix = Phoenix.findHome(level, nest.getBlockPos());
		int phoenixState = phoenix == null ? 0 : phoenix.isDeparting() ? 3 : phoenix.isAway() ? 2 : 1;
		boolean canSettings = terminal != null && terminal.can(serverPlayer, Permission.SETTINGS);
		NestStatePayload.State state = new NestStatePayload.State(nest.getName(), nest.getAccept().ordinal(), canSend(),
				canSettings, rows, selected, nest.isDaily(), phoenixState, 0, (int) post.incomingTo(self));

		if (force || !state.equals(lastSent)) {
			lastSent = state;
			ServerPlayNetworking.send(serverPlayer, new NestStatePayload(containerId, state));
		}
	}

	public void handleAction(ServerPlayer player, NestActionPayload.Action action, String text, int value) {
		StorageTerminalBlockEntity terminal = terminal();

		if (nest == null || terminal == null) {
			player.sendOverlayMessage(PhoenixNestBlockEntity.message(PhoenixNestBlockEntity.SendResult.NOT_LINKED));
			return;
		}

		boolean settings = terminal.can(player, Permission.SETTINGS);

		switch (action) {
			case DESTINATION -> {
				if (canSend() && value >= 0 && value < lastDestinations.size()) {
					nest.setDestination(lastDestinations.get(value));
				}
			}
			case SEND -> {
				if (!canSend()) {
					player.sendOverlayMessage(Component.translatable("jtechstorage.message.no_permission",
							StorageTerminalBlockEntity.Role.MEMBER.displayName()));
					return;
				}

				PhoenixNestBlockEntity.SendResult result = nest.send(player, player.getUUID(), false);

				if (result != PhoenixNestBlockEntity.SendResult.SENT) {
					player.sendOverlayMessage(PhoenixNestBlockEntity.message(result));
				}
			}
			case DAILY -> {
				if (settings) {
					nest.setDaily(!nest.isDaily());
				}
			}
			case RENAME -> {
				if (settings) {
					nest.setName(text);
				}
			}
			case ACCEPT -> {
				if (settings) {
					nest.setAccept(PhoenixPost.Accept.byId(value));
				}
			}
		}

		sync(player, true);
	}

	// ------------------------------------------------------------------
	// Ghost fields & inbox
	// ------------------------------------------------------------------

	@Override
	public void clicked(int slotId, int button, ContainerInput input, Player player) {
		if (slotId < 0 || slotId >= SHIP) {
			super.clicked(slotId, button, input, player);
			return;
		}

		if (!canSend() || (input != ContainerInput.PICKUP && input != ContainerInput.QUICK_MOVE)) {
			return;
		}

		ItemStack carried = getCarried();
		ItemStack current = shipment.getItem(slotId);

		if (input == ContainerInput.PICKUP && !carried.isEmpty()) {
			int count = button == 0 ? carried.getCount() : 1;
			shipment.setItem(slotId, carried.copyWithCount(Math.min(count, carried.getMaxStackSize())));
		} else if (!current.isEmpty()) {
			int step = input == ContainerInput.QUICK_MOVE ? 8 : 1;
			int count = current.getCount() + (button == 0 ? step : -step);
			shipment.setItem(slotId, count <= 0 ? ItemStack.EMPTY : current.copyWithCount(Math.min(count, current.getMaxStackSize())));
		}

		shipment.setChanged();
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		if (index < SHIP || index >= INBOX_END) {
			return ItemStack.EMPTY;
		}

		Slot slot = getSlot(index);

		if (!slot.hasItem() || !slot.mayPickup(player)) {
			return ItemStack.EMPTY;
		}

		ItemStack stack = slot.getItem();
		ItemStack original = stack.copy();

		if (!moveItemStackTo(stack, INBOX_END, INBOX_END + 36, true)) {
			return ItemStack.EMPTY;
		}

		slot.setChanged();
		return original;
	}

	@Override
	public boolean stillValid(Player player) {
		if (nest == null) {
			return true;
		}

		StorageTerminalBlockEntity terminal = terminal();
		return LinkedPanelBlock.inReach(nest, player) && terminal != null && terminal.canAccess(player);
	}

	public void setClientState(NestStatePayload.State state) {
		clientState = state;
		clientVersion++;
	}

	public NestStatePayload.State getClientState() {
		return clientState;
	}

	public int getClientVersion() {
		return clientVersion;
	}

	private static class GhostSlot extends Slot {
		GhostSlot(Container container, int index, int x, int y) {
			super(container, index, x, y);
		}

		@Override
		public boolean mayPickup(Player player) {
			return false;
		}

		@Override
		public boolean mayPlace(ItemStack stack) {
			return false;
		}
	}

	private class InboxSlot extends Slot {
		InboxSlot(Container container, int index, int x, int y) {
			super(container, index, x, y);
		}

		@Override
		public boolean mayPickup(Player player) {
			return canSend();
		}

		@Override
		public boolean mayPlace(ItemStack stack) {
			return false;
		}
	}
}
