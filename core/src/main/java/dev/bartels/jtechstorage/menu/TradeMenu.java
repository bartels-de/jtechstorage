package dev.bartels.jtechstorage.menu;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import dev.bartels.jtechstorage.ModContent;
import dev.bartels.jtechstorage.block.LinkedPanelBlock;
import dev.bartels.jtechstorage.block.TradeTerminalBlockEntity;

/**
 * Menu of the Trade Terminal. The offer fields are "ghost slots": whoever may edit places an item on them
 * (left-click = whole stack as amount, right-click = 1 item), click with an empty hand = +1 / −1, Shift = ±8.
 * Buying goes via {@link #clickMenuButton}: button i = 1×, button 10+i = up to 10×.
 */
public class TradeMenu extends AbstractContainerMenu {
	public record OpenData(BlockPos pos, boolean editable, String shopName, int color) {
		public static final StreamCodec<RegistryFriendlyByteBuf, OpenData> STREAM_CODEC = StreamCodec.composite(
				BlockPos.STREAM_CODEC, OpenData::pos,
				ByteBufCodecs.BOOL, OpenData::editable,
				ByteBufCodecs.STRING_UTF8, OpenData::shopName,
				ByteBufCodecs.INT, OpenData::color,
				OpenData::new);
	}

	public static final int OFFERS = TradeTerminalBlockEntity.OFFERS;
	public static final int WIDTH = 272;
	public static final int ROW_Y = 26;
	public static final int ROW_HEIGHT = 25;
	public static final int GOODS_X = 12;
	public static final int PRICE_X = 140;
	public static final int INVENTORY_X = 55;
	public static final int INVENTORY_Y = ROW_Y + OFFERS * ROW_HEIGHT + 16;
	public static final int IMAGE_HEIGHT = INVENTORY_Y + 82;
	public static final int BULK = 10;
	private static final int REFRESH_INTERVAL = 20;

	private final @Nullable TradeTerminalBlockEntity trade;
	private final boolean editable;
	private final String shopName;
	private final int color;
	private final Container offers;
	private final ContainerData stocks;
	private int ticks = REFRESH_INTERVAL;

	/** Client. */
	public TradeMenu(int containerId, Inventory inventory, OpenData data) {
		this(containerId, inventory, null, new SimpleContainer(OFFERS * 2), data.editable(), data.shopName(), data.color());
	}

	/** Server. */
	public TradeMenu(int containerId, Inventory inventory, TradeTerminalBlockEntity trade, boolean editable) {
		this(containerId, inventory, trade, trade.getOffers(), editable, "", 0);
	}

	private TradeMenu(int containerId, Inventory inventory, @Nullable TradeTerminalBlockEntity trade, Container offers,
			boolean editable, String shopName, int color) {
		super(ModContent.TRADE_MENU, containerId);
		this.trade = trade;
		this.editable = editable;
		this.shopName = shopName;
		this.color = color;
		this.offers = offers;
		this.stocks = new SimpleContainerData(OFFERS);

		for (int i = 0; i < OFFERS; i++) {
			int y = ROW_Y + i * ROW_HEIGHT + 4;
			addSlot(new OfferSlot(offers, i * 2, GOODS_X, y));
			addSlot(new OfferSlot(offers, i * 2 + 1, PRICE_X, y));
		}

		addStandardInventorySlots(inventory, INVENTORY_X, INVENTORY_Y);
		addDataSlots(stocks);
	}

	public boolean isEditable() {
		return editable;
	}

	public String getShopName() {
		return shopName;
	}

	public int getColor() {
		return color;
	}

	/** Stock (how often it can be bought) or {@link TradeTerminalBlockEntity#STOCK_NONE}/{@code STOCK_UNREACHABLE}. */
	public int stock(int index) {
		return stocks.get(index);
	}

	public ItemStack goods(int index) {
		return offers.getItem(index * 2);
	}

	public ItemStack price(int index) {
		return offers.getItem(index * 2 + 1);
	}

	@Override
	public void broadcastChanges() {
		if (trade != null && ++ticks >= REFRESH_INTERVAL) {
			ticks = 0;
			int[] values = trade.stocks();

			for (int i = 0; i < OFFERS; i++) {
				stocks.set(i, values[i]);
			}
		}

		super.broadcastChanges();
	}

	@Override
	public void clicked(int slotId, int button, ContainerInput input, Player player) {
		if (slotId < 0 || slotId >= OFFERS * 2) {
			super.clicked(slotId, button, input, player);
			return;
		}

		if (!editable || (input != ContainerInput.PICKUP && input != ContainerInput.QUICK_MOVE)) {
			return;
		}

		ItemStack carried = getCarried();
		ItemStack current = offers.getItem(slotId);

		if (input == ContainerInput.PICKUP && !carried.isEmpty()) {
			int count = button == 0 ? carried.getCount() : 1;
			offers.setItem(slotId, carried.copyWithCount(Math.min(count, carried.getMaxStackSize())));
		} else if (!current.isEmpty()) {
			int step = input == ContainerInput.QUICK_MOVE ? 8 : 1;
			int count = current.getCount() + (button == 0 ? step : -step);

			if (count <= 0) {
				offers.setItem(slotId, ItemStack.EMPTY);
			} else {
				offers.setItem(slotId, current.copyWithCount(Math.min(count, current.getMaxStackSize())));
			}
		}

		offers.setChanged();
		ticks = REFRESH_INTERVAL;
	}

	@Override
	public boolean clickMenuButton(Player player, int id) {
		if (trade == null || !(player instanceof ServerPlayer serverPlayer)) {
			return true;
		}

		if (id >= 0 && id < OFFERS) {
			trade.buy(serverPlayer, id, 1);
		} else if (id >= 10 && id < 10 + OFFERS) {
			trade.buy(serverPlayer, id - 10, BULK);
		}

		ticks = REFRESH_INTERVAL;
		return true;
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		return ItemStack.EMPTY;
	}

	@Override
	public boolean stillValid(Player player) {
		return trade == null || LinkedPanelBlock.inReach(trade, player);
	}

	/** Display field of an offer: not touchable, edited via {@link #clicked}. */
	private static class OfferSlot extends Slot {
		OfferSlot(Container container, int index, int x, int y) {
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

		@Override
		public int getMaxStackSize() {
			return 99;
		}
	}
}
