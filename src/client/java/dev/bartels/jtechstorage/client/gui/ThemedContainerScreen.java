package dev.bartels.jtechstorage.client.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Basis für alle Mod-Menüs: Beschriftungen im gewählten Design, Leuchten unter der Maus, lesbare Mengen auf hellen
 * Slots, Hintergrund bleibt beim Hereingleiten stehen.
 */
public abstract class ThemedContainerScreen<T extends AbstractContainerMenu> extends AbstractContainerScreen<T> {
	protected ThemedContainerScreen(T menu, Inventory inventory, Component title, int width, int height) {
		super(menu, inventory, title, width, height);
	}

	@Override
	protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		GuiTheme.title(graphics, font, title, titleLabelX, titleLabelY);
		graphics.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, GuiTheme.text(), false);
	}

	@Override
	protected void extractSlot(GuiGraphicsExtractor graphics, Slot slot, int mouseX, int mouseY) {
		if (slot == hoveredSlot && GuiSettings.get().hoverGlow) {
			GuiTheme.slotGlow(graphics, slot.x, slot.y);
		}

		ItemStack stack = slot.getItem();

		if (GuiTheme.outlineCounts() && !isQuickCrafting && stack.getCount() > 1) {
			// Kontur zuerst – die weiße Zahl von Vanilla landet darauf
			String count = Integer.toString(stack.getCount());
			int x = slot.x + 17 - font.width(count);
			int y = slot.y + 9;
			int outline = 0xFF1A1424;
			graphics.text(font, count, x - 1, y, outline, false);
			graphics.text(font, count, x + 1, y, outline, false);
			graphics.text(font, count, x, y - 1, outline, false);
			graphics.text(font, count, x, y + 1, outline, false);
		}

		super.extractSlot(graphics, slot, mouseX, mouseY);
	}

	@Override
	public void extractTransparentBackground(GuiGraphicsExtractor graphics) {
		GuiFx.beginBackground(this, graphics);
		super.extractTransparentBackground(graphics);
		GuiFx.endBackground(graphics);
	}
}
