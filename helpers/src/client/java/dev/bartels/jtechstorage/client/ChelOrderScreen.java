package dev.bartels.jtechstorage.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import dev.bartels.jtechstorage.client.gui.GuiTheme;
import dev.bartels.jtechstorage.client.gui.ThemedContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import dev.bartels.jtechstorage.entity.Chel;
import dev.bartels.jtechstorage.menu.ChelOrderMenu;
import dev.bartels.jtechstorage.net.ChelOrderActionPayload;
import dev.bartels.jtechstorage.net.ChelOrdersPayload;

/**
 * Chel's order menu: 8 slots (2 columns × 4 rows). Dropping an item on a slot with the mouse = order
 * ("keep this many in stock"), −/+ changes the amount (Shift: ±16), right-click clears.
 */
public class ChelOrderScreen extends ThemedContainerScreen<ChelOrderMenu> {
	private static final int CELL_X = 8;
	private static final int CELL_Y = 32;
	private static final int CELL_W = 81;
	private static final int CELL_H = 21;
	private static final int BUTTON = 11;

	public ChelOrderScreen(ChelOrderMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title, 176, 210);
		inventoryLabelY = ChelOrderMenu.INVENTORY_Y - 11;
	}

	private int cellX(int slot) {
		return CELL_X + (slot % 2) * CELL_W;
	}

	private int cellY(int slot) {
		return CELL_Y + (slot / 2) * CELL_H;
	}

	private boolean over(int x, int y, int w, int h, double mouseX, double mouseY) {
		return mouseX >= leftPos + x && mouseX < leftPos + x + w && mouseY >= topPos + y && mouseY < topPos + y + h;
	}

	// ------------------------------------------------------------------
	// Drawing
	// ------------------------------------------------------------------

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractBackground(graphics, mouseX, mouseY, delta);
		int x = leftPos;
		int y = topPos;
		int w = imageWidth;
		int h = imageHeight;

		// Vanilla-style window
		GuiTheme.window(graphics, x, y, w, h);

		// Status band in accent color
		GuiTheme.face(graphics, x + 6, y + 16, w - 12, 13, GuiTheme.accentDark());

		for (int slot = 0; slot < Chel.MAX_ORDERS; slot++) {
			slotFrame(graphics, x + cellX(slot), y + cellY(slot));
		}

		for (int row = 0; row < 4; row++) {
			for (int col = 0; col < 9; col++) {
				int sy = row < 3 ? ChelOrderMenu.INVENTORY_Y + row * 18 : ChelOrderMenu.INVENTORY_Y + 58;
				slotFrame(graphics, x + 7 + col * 18, y + sy - 1);
			}
		}
	}

	private static void slotFrame(GuiGraphicsExtractor graphics, int x, int y) {
		GuiTheme.slot(graphics, x, y);
	}

	@Override
	public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractContents(graphics, mouseX, mouseY, delta);
		List<ChelOrdersPayload.Entry> entries = menu.getClientEntries();

		// Status
		int statusColor = GuiTheme.onColor(GuiTheme.accentDark());
		graphics.text(font, GuiTheme.fit(font, menu.getClientStatus().getString(), imageWidth - 20), leftPos + 10, topPos + 19,
				statusColor, statusColor == 0xFFFFFFFF);

		for (int slot = 0; slot < Chel.MAX_ORDERS && slot < entries.size(); slot++) {
			ChelOrdersPayload.Entry entry = entries.get(slot);
			int x = leftPos + cellX(slot);
			int y = topPos + cellY(slot);

			if (entry.prototype().isEmpty()) {
				graphics.text(font, "+", x + 9 - font.width("+") / 2, y + 5, GuiTheme.slotHint(), false);
				graphics.text(font, "—", x + 22, y + 5, GuiTheme.subtle(), false);
				continue;
			}

			graphics.item(entry.prototype(), x + 1, y + 1);
			boolean enough = entry.have() >= entry.target();
			String amount = compact(entry.have()) + "/" + compact(entry.target());

			if (font.width(amount) > 34) {
				amount = compact(entry.have());   // full value is in the tooltip
			}
			graphics.text(font, amount, x + 21, y + 5, enough ? GuiTheme.good() : GuiTheme.bad(), false);
			button(graphics, x + 57, y + 3, "−", over(cellX(slot) + 57, cellY(slot) + 3, BUTTON, BUTTON, mouseX, mouseY));
			button(graphics, x + 69, y + 3, "+", over(cellX(slot) + 69, cellY(slot) + 3, BUTTON, BUTTON, mouseX, mouseY));
		}
	}

	private void button(GuiGraphicsExtractor graphics, int x, int y, String label, boolean hovered) {
		int face = hovered ? GuiTheme.accentHover() : GuiTheme.accent();
		graphics.fill(x, y, x + BUTTON, y + BUTTON, 0xFF000000);
		graphics.fill(x + 1, y + 1, x + BUTTON - 1, y + BUTTON - 1, face);
		int color = GuiTheme.onColor(face);
		graphics.text(font, label, x + (BUTTON - font.width(label) + 1) / 2, y + 2, color, color == 0xFFFFFFFF);
	}

	private static String compact(long value) {
		if (value >= 1_000) {
			return (value / 1000) + "k";
		}

		return Long.toString(value);
	}

	@Override
	protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		super.extractTooltip(graphics, mouseX, mouseY);

		if (!menu.getCarried().isEmpty()) {
			return;
		}

		List<ChelOrdersPayload.Entry> entries = menu.getClientEntries();

		for (int slot = 0; slot < Chel.MAX_ORDERS && slot < entries.size(); slot++) {
			ChelOrdersPayload.Entry entry = entries.get(slot);

			if (over(cellX(slot), cellY(slot), 18, 18, mouseX, mouseY)) {
				List<Component> lines = new ArrayList<>();

				if (entry.prototype().isEmpty()) {
					lines.add(Component.translatable("jtechstorage.chel.menu.empty"));
					lines.add(Component.translatable("jtechstorage.chel.menu.empty_hint").withStyle(ChatFormatting.GRAY));
				} else {
					lines.addAll(Screen.getTooltipFromItem(minecraft, entry.prototype()));
					lines.add(Component.translatable("jtechstorage.chel.menu.stock", entry.have(), entry.target())
							.withStyle(entry.have() >= entry.target() ? ChatFormatting.GREEN : ChatFormatting.GOLD));
					lines.add(Component.translatable("jtechstorage.chel.menu.hint").withStyle(ChatFormatting.GRAY));
				}

				graphics.setComponentTooltipForNextFrame(font, lines, mouseX, mouseY);
				return;
			}

			if (!entry.prototype().isEmpty() && (over(cellX(slot) + 57, cellY(slot) + 3, BUTTON, BUTTON, mouseX, mouseY)
					|| over(cellX(slot) + 69, cellY(slot) + 3, BUTTON, BUTTON, mouseX, mouseY))) {
				graphics.setTooltipForNextFrame(font, Component.translatable("jtechstorage.chel.menu.button_hint"), mouseX, mouseY);
				return;
			}
		}
	}

	// ------------------------------------------------------------------
	// Input
	// ------------------------------------------------------------------

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		double mouseX = event.x();
		double mouseY = event.y();
		List<ChelOrdersPayload.Entry> entries = menu.getClientEntries();

		for (int slot = 0; slot < Chel.MAX_ORDERS; slot++) {
			boolean hasOrder = slot < entries.size() && !entries.get(slot).prototype().isEmpty();
			int step = event.hasShiftDown() ? 16 : 1;

			if (over(cellX(slot), cellY(slot), 18, 18, mouseX, mouseY)) {
				ItemStack carried = menu.getCarried();

				if (event.button() == 1 && hasOrder && carried.isEmpty()) {
					send(slot, ChelOrderActionPayload.CLEAR, 0);
				} else if (!carried.isEmpty()) {
					send(slot, ChelOrderActionPayload.SET, 0);
				}

				return true;
			}

			if (hasOrder && over(cellX(slot) + 57, cellY(slot) + 3, BUTTON, BUTTON, mouseX, mouseY)) {
				send(slot, ChelOrderActionPayload.ADJUST, -step);
				return true;
			}

			if (hasOrder && over(cellX(slot) + 69, cellY(slot) + 3, BUTTON, BUTTON, mouseX, mouseY)) {
				send(slot, ChelOrderActionPayload.ADJUST, step);
				return true;
			}
		}

		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		List<ChelOrdersPayload.Entry> entries = menu.getClientEntries();

		for (int slot = 0; slot < Chel.MAX_ORDERS && slot < entries.size(); slot++) {
			if (!entries.get(slot).prototype().isEmpty() && over(cellX(slot), cellY(slot), CELL_W - 2, 18, mouseX, mouseY)) {
				send(slot, ChelOrderActionPayload.ADJUST, scrollY > 0 ? 1 : -1);
				return true;
			}
		}

		return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}

	private void send(int slot, int action, int amount) {
		ClientPlayNetworking.send(new ChelOrderActionPayload(menu.containerId, slot, action, amount));
	}
}
