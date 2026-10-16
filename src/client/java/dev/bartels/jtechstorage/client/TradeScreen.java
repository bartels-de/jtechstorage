package dev.bartels.jtechstorage.client;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import dev.bartels.jtechstorage.block.TradeTerminalBlockEntity;
import dev.bartels.jtechstorage.client.gui.GuiTheme;
import dev.bartels.jtechstorage.client.gui.ThemedContainerScreen;
import dev.bartels.jtechstorage.menu.TradeMenu;

/**
 * Laden-Oberfläche: Kopfzeile in Team-Farbe, jedes Angebot als Karte mit Ware → Preis, Item-Namen, Vorrat,
 * „Du hast …“ und großem Kaufen-Knopf (Shift = bis zu 10×). Verwalter bearbeiten die Felder direkt mit der Maus.
 */
public class TradeScreen extends ThemedContainerScreen<TradeMenu> {
	private static final int W = TradeMenu.WIDTH;
	private static final int CARD_X = 6;
	private static final int CARD_W = W - 12;
	private static final int CARD_H = TradeMenu.ROW_HEIGHT - 2;
	private static final int GOODS_TEXT_X = TradeMenu.GOODS_X + 21;
	private static final int ARROW_X = TradeMenu.PRICE_X - 15;
	private static final int PRICE_TEXT_X = TradeMenu.PRICE_X + 21;
	private static final int BUY_W = 44;
	private static final int BUY_X = W - 8 - BUY_W;
	private static final int BUY_H = 17;
	private static final int GOODS_TEXT_W = ARROW_X - GOODS_TEXT_X - 3;
	private static final int PRICE_TEXT_W = BUY_X - PRICE_TEXT_X - 4;

	public TradeScreen(TradeMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title, W, TradeMenu.IMAGE_HEIGHT);
		titleLabelY = -10000;
		inventoryLabelX = TradeMenu.INVENTORY_X;
		inventoryLabelY = TradeMenu.INVENTORY_Y - 11;
	}

	private int rowY(int i) {
		return TradeMenu.ROW_Y + i * TradeMenu.ROW_HEIGHT;
	}

	private boolean over(int x, int y, int w, int h, double mouseX, double mouseY) {
		return mouseX >= leftPos + x && mouseX < leftPos + x + w && mouseY >= topPos + y && mouseY < topPos + y + h;
	}

	private boolean hasOffer(int i) {
		return !menu.goods(i).isEmpty() && !menu.price(i).isEmpty();
	}

	/** Wie viele Preis-Items der Spieler dabeihat. */
	private int owned(ItemStack price) {
		int total = 0;
		Inventory inventory = minecraft.player.getInventory();

		for (int slot = 0; slot < Inventory.INVENTORY_SIZE; slot++) {
			ItemStack stack = inventory.getItem(slot);

			if (ItemStack.isSameItemSameComponents(stack, price)) {
				total += stack.getCount();
			}
		}

		return total;
	}

	private boolean canBuy(int i) {
		return hasOffer(i) && menu.stock(i) > 0 && owned(menu.price(i)) >= menu.price(i).getCount();
	}

	// ------------------------------------------------------------------
	// Zeichnen
	// ------------------------------------------------------------------

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractBackground(graphics, mouseX, mouseY, delta);
		int x = leftPos;
		int y = topPos;
		GuiTheme.panel(graphics, x, y, W, imageHeight);

		// Kopfzeile in Team-Farbe
		int team = 0xFF000000 | menu.getColor();
		GuiTheme.face(graphics, x + 4, y + 4, W - 8, 18, headerColor());

		for (int i = 0; i < TradeMenu.OFFERS; i++) {
			int cy = y + rowY(i);
			boolean active = hasOffer(i);
			int card = active ? GuiTheme.inset() : GuiTheme.mix(GuiTheme.inset(), GuiTheme.face(), 0.5f);
			graphics.fill(x + CARD_X, cy, x + CARD_X + CARD_W, cy + CARD_H, GuiTheme.shadow());
			graphics.fill(x + CARD_X, cy, x + CARD_X + CARD_W - 1, cy + CARD_H - 1, card);

			if (active) {
				graphics.fill(x + CARD_X, cy, x + CARD_X + 3, cy + CARD_H - 1, team);   // Farbstreifen links
			}

			GuiTheme.slot(graphics, x + TradeMenu.GOODS_X - 1, cy + 3);
			GuiTheme.slot(graphics, x + TradeMenu.PRICE_X - 1, cy + 3);
		}

		for (int row = 0; row < 4; row++) {
			for (int col = 0; col < 9; col++) {
				int sy = row < 3 ? TradeMenu.INVENTORY_Y + row * 18 : TradeMenu.INVENTORY_Y + 58;
				GuiTheme.slot(graphics, x + TradeMenu.INVENTORY_X - 1 + col * 18, y + sy - 1);
			}
		}
	}

	private int headerColor() {
		return GuiTheme.mix(0xFF000000 | menu.getColor(), 0xFF000000, 0.35f);
	}

	@Override
	public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractContents(graphics, mouseX, mouseY, delta);
		int x = leftPos;
		int y = topPos;
		int headerText = GuiTheme.onColor(headerColor());

		// Kopfzeile
		Component badge = Component.translatable("jtechstorage.trade.edit_badge");
		int badgeW = menu.isEditable() ? font.width(badge) + 8 : 0;
		Component heading = Component.translatable("jtechstorage.trade.title", menu.getShopName());
		graphics.text(font, GuiTheme.fit(font, heading.getString(), W - 24 - badgeW), x + 10, y + 9, headerText, headerText == 0xFFFFFFFF);

		if (menu.isEditable()) {
			graphics.text(font, badge, x + W - 10 - font.width(badge), y + 9, headerText == 0xFFFFFFFF ? 0xFFFFE08A : 0xFF5A3A00,
					headerText == 0xFFFFFFFF);
		}

		boolean anyOffer = false;

		for (int i = 0; i < TradeMenu.OFFERS; i++) {
			int cy = y + rowY(i);
			ItemStack goods = menu.goods(i);
			ItemStack price = menu.price(i);

			if (!hasOffer(i)) {
				if (menu.isEditable()) {
					if (goods.isEmpty()) {
						graphics.text(font, "+", x + TradeMenu.GOODS_X + 5, cy + 8, GuiTheme.slotHint(), false);
					}

					if (price.isEmpty()) {
						graphics.text(font, "+", x + TradeMenu.PRICE_X + 5, cy + 8, GuiTheme.slotHint(), false);
					}

					GuiTheme.label(graphics, font, Component.translatable("jtechstorage.trade.empty_edit"), x + GOODS_TEXT_X, cy + 8,
							TradeMenu.PRICE_X - 5 - GOODS_TEXT_X, GuiTheme.subtle());
				}

				continue;
			}

			anyOffer = true;

			// Ware: Menge × Name, darunter Vorrat
			GuiTheme.label(graphics, font, Component.literal(goods.getCount() + "× " + goods.getHoverName().getString()),
					x + GOODS_TEXT_X, cy + 3, GOODS_TEXT_W, GuiTheme.text());
			int stock = menu.stock(i);
			Component stockText;
			int stockColor;

			if (stock == TradeTerminalBlockEntity.STOCK_UNREACHABLE) {
				stockText = Component.translatable("jtechstorage.trade.stock.unreachable");
				stockColor = GuiTheme.bad();
			} else if (stock <= 0) {
				stockText = Component.translatable("jtechstorage.trade.stock.empty");
				stockColor = GuiTheme.bad();
			} else {
				stockText = Component.translatable("jtechstorage.trade.stock",
						stock >= TradeTerminalBlockEntity.MAX_SHOWN_STOCK ? stock + "+" : String.valueOf(stock));
				stockColor = GuiTheme.good();
			}

			GuiTheme.label(graphics, font, stockText, x + GOODS_TEXT_X, cy + 13, GOODS_TEXT_W, stockColor);

			// Pfeil
			graphics.text(font, "➜", x + ARROW_X, cy + 8, GuiTheme.accentText(), false);

			// Preis: Menge × Name, darunter „Du hast …“
			GuiTheme.label(graphics, font, Component.literal(price.getCount() + "× " + price.getHoverName().getString()),
					x + PRICE_TEXT_X, cy + 3, PRICE_TEXT_W, GuiTheme.text());
			int have = owned(price);
			GuiTheme.label(graphics, font, Component.translatable("jtechstorage.trade.have", have), x + PRICE_TEXT_X, cy + 13,
					PRICE_TEXT_W, have >= price.getCount() ? GuiTheme.subtle() : GuiTheme.bad());

			// Kaufen
			boolean enabled = canBuy(i);
			boolean hover = enabled && over(BUY_X, rowY(i) + 3, BUY_W, BUY_H, mouseX, mouseY);
			GuiTheme.labeled(graphics, font, x + BUY_X, cy + 3, BUY_W, BUY_H, Component.translatable("jtechstorage.trade.buy"),
					!enabled ? GuiTheme.DISABLED_FACE : hover ? 0xFF4CC46C : 0xFF23803F, enabled);
		}

		if (!anyOffer && !menu.isEditable()) {
			Component none = Component.translatable("jtechstorage.trade.no_offers");
			graphics.text(font, none, x + W / 2 - font.width(none) / 2, y + TradeMenu.ROW_Y + 70, GuiTheme.subtle(), false);
		}

		Component hint = Component.translatable(menu.isEditable() ? "jtechstorage.trade.hint.edit" : "jtechstorage.trade.hint.buy");
		GuiTheme.label(graphics, font, hint, x + 8, y + TradeMenu.ROW_Y + TradeMenu.OFFERS * TradeMenu.ROW_HEIGHT + 3, W - 16,
				GuiTheme.accentText());
	}

	@Override
	protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		super.extractTooltip(graphics, mouseX, mouseY);

		if (hoveredSlot != null && hoveredSlot.hasItem()) {
			return;
		}

		for (int i = 0; i < TradeMenu.OFFERS; i++) {
			if (hasOffer(i) && over(GOODS_TEXT_X, rowY(i), GOODS_TEXT_W, CARD_H, mouseX, mouseY)) {
				graphics.setTooltipForNextFrame(font, menu.goods(i), mouseX, mouseY);
				return;
			}

			if (hasOffer(i) && over(PRICE_TEXT_X, rowY(i), PRICE_TEXT_W, CARD_H, mouseX, mouseY)) {
				graphics.setTooltipForNextFrame(font, menu.price(i), mouseX, mouseY);
				return;
			}
		}

		for (int i = 0; i < TradeMenu.OFFERS; i++) {
			if (hasOffer(i) && over(BUY_X, rowY(i) + 3, BUY_W, BUY_H, mouseX, mouseY)) {
				ItemStack goods = menu.goods(i);
				ItemStack price = menu.price(i);
				graphics.setComponentTooltipForNextFrame(font, List.of(
						Component.translatable("jtechstorage.trade.deal", 1, goods.getCount(), goods.getHoverName(),
								price.getCount(), price.getHoverName()),
						Component.translatable("jtechstorage.trade.buy.tooltip", TradeMenu.BULK).withStyle(ChatFormatting.GRAY)),
						mouseX, mouseY);
				return;
			}
		}
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		for (int i = 0; i < TradeMenu.OFFERS; i++) {
			if (canBuy(i) && over(BUY_X, rowY(i) + 3, BUY_W, BUY_H, event.x(), event.y())) {
				minecraft.gameMode.handleInventoryButtonClick(menu.containerId, event.hasShiftDown() ? 10 + i : i);
				return true;
			}
		}

		return super.mouseClicked(event, doubleClick);
	}
}
