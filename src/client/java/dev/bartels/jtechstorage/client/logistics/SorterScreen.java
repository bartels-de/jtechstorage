package dev.bartels.jtechstorage.client.logistics;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import dev.bartels.jtechstorage.client.gui.GuiTheme;
import dev.bartels.jtechstorage.client.gui.ThemedContainerScreen;
import dev.bartels.jtechstorage.logistics.SorterBlockEntity;
import dev.bartels.jtechstorage.logistics.SorterMenu;

/** Filter der Sortier-Weiche (v0.40): drei Reihen (links, geradeaus, rechts) und der Rest-Schalter. */
public class SorterScreen extends ThemedContainerScreen<SorterMenu> {
	private static final String[] ROWS = {"left", "straight", "right"};
	private static final int BTN_X = 8;
	private static final int BTN_Y = SorterMenu.FILTER_Y + 3 * SorterMenu.ROW_GAP + 2;
	private static final int BTN_W = 160;
	private static final int BTN_H = 14;

	public SorterScreen(SorterMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title, SorterMenu.WIDTH, SorterMenu.HEIGHT);
		inventoryLabelY = SorterMenu.INVENTORY_Y - 11;
	}

	private boolean over(int x, int y, int w, int h, double mouseX, double mouseY) {
		return mouseX >= leftPos + x && mouseX < leftPos + x + w && mouseY >= topPos + y && mouseY < topPos + y + h;
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractBackground(graphics, mouseX, mouseY, delta);
		GuiTheme.window(graphics, leftPos, topPos, imageWidth, imageHeight);

		for (int row = 0; row < 3; row++) {
			for (int col = 0; col < SorterBlockEntity.ROW; col++) {
				GuiTheme.slot(graphics, leftPos + SorterMenu.FILTER_X + col * 18 - 1, topPos + SorterMenu.FILTER_Y + row * SorterMenu.ROW_GAP - 1);
			}
		}

		for (int row = 0; row < 4; row++) {
			for (int col = 0; col < 9; col++) {
				int sy = row < 3 ? SorterMenu.INVENTORY_Y + row * 18 : SorterMenu.INVENTORY_Y + 58;
				GuiTheme.slot(graphics, leftPos + 8 + col * 18 - 1, topPos + sy - 1);
			}
		}
	}

	@Override
	public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractContents(graphics, mouseX, mouseY, delta);

		for (int row = 0; row < 3; row++) {
			GuiTheme.label(graphics, font, Component.translatable("jtechstorage.sorter." + ROWS[row]), leftPos + 8,
					topPos + SorterMenu.FILTER_Y + row * SorterMenu.ROW_GAP - 10, imageWidth - 16, GuiTheme.subtle());
		}

		boolean hover = over(BTN_X, BTN_Y, BTN_W, BTN_H, mouseX, mouseY);
		GuiTheme.button(graphics, font, leftPos + BTN_X, topPos + BTN_Y, BTN_W, BTN_H,
				Component.translatable(menu.holdsRest() ? "jtechstorage.sorter.rest.hold" : "jtechstorage.sorter.rest.straight"), true, hover);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (over(BTN_X, BTN_Y, BTN_W, BTN_H, event.x(), event.y())) {
			minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 0);
			minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
			return true;
		}

		return super.mouseClicked(event, doubleClick);
	}

	@Override
	protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		for (int slot = 0; slot < SorterBlockEntity.ROW * 3; slot++) {
			int sx = leftPos + SorterMenu.FILTER_X + (slot % SorterBlockEntity.ROW) * 18;
			int sy = topPos + SorterMenu.FILTER_Y + (slot / SorterBlockEntity.ROW) * SorterMenu.ROW_GAP;

			if (mouseX >= sx - 1 && mouseX < sx + 17 && mouseY >= sy - 1 && mouseY < sy + 17 && menu.getCarried().isEmpty()) {
				ItemStack stack = menu.getSlot(slot).getItem();
				graphics.setComponentTooltipForNextFrame(font, List.of(
						stack.isEmpty() ? Component.translatable("jtechstorage.pusher.filter.empty") : stack.getHoverName(),
						Component.translatable("jtechstorage.pusher.filter.help").withStyle(ChatFormatting.GRAY)), mouseX, mouseY);
				return;
			}
		}

		if (over(BTN_X, BTN_Y, BTN_W, BTN_H, mouseX, mouseY)) {
			graphics.setComponentTooltipForNextFrame(font, List.of(Component.translatable("jtechstorage.sorter.rest.help").withStyle(ChatFormatting.GRAY)),
					mouseX, mouseY);
			return;
		}

		super.extractTooltip(graphics, mouseX, mouseY);
	}
}
