package dev.bartels.jtechstorage.client.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

/** Basis für alle Mod-Menüs: Beschriftungen in der Text-Farbe des gewählten Designs. */
public abstract class ThemedContainerScreen<T extends AbstractContainerMenu> extends AbstractContainerScreen<T> {
	protected ThemedContainerScreen(T menu, Inventory inventory, Component title, int width, int height) {
		super(menu, inventory, title, width, height);
	}

	@Override
	protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		graphics.text(font, title, titleLabelX, titleLabelY, GuiTheme.text(), false);
		graphics.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, GuiTheme.text(), false);
	}
}
