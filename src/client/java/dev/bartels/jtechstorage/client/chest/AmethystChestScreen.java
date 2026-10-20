package dev.bartels.jtechstorage.client.chest;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import dev.bartels.jtechstorage.chest.AmethystChestMenu;
import dev.bartels.jtechstorage.chest.AmethystChestTier;
import dev.bartels.jtechstorage.client.gui.GuiTheme;
import dev.bartels.jtechstorage.client.gui.ThemedContainerScreen;

/** Fenster der Amethyst-Kisten (v0.28.0): Raster je Stufe + Spieler-Inventar, Stapel-Faktor rechts oben. */
public class AmethystChestScreen extends ThemedContainerScreen<AmethystChestMenu> {
	public AmethystChestScreen(AmethystChestMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title, AmethystChestMenu.width(menu.tier()), AmethystChestMenu.height(menu.tier()));
		inventoryLabelX = AmethystChestMenu.inventoryLeft(menu.tier());
		inventoryLabelY = AmethystChestMenu.inventoryTop(menu.tier()) - 11;
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractBackground(graphics, mouseX, mouseY, delta);
		AmethystChestTier tier = menu.tier();
		GuiTheme.panel(graphics, leftPos, topPos, imageWidth, imageHeight);

		for (int row = 0; row < tier.rows; row++) {
			for (int col = 0; col < tier.columns; col++) {
				GuiTheme.slot(graphics, leftPos + 8 + col * 18 - 1, topPos + AmethystChestMenu.TOP + row * 18 - 1);
			}
		}

		int left = AmethystChestMenu.inventoryLeft(tier);
		int top = AmethystChestMenu.inventoryTop(tier);

		for (int row = 0; row < 4; row++) {
			for (int col = 0; col < 9; col++) {
				int sy = row < 3 ? top + row * 18 : top + 58;
				GuiTheme.slot(graphics, leftPos + left + col * 18 - 1, topPos + sy - 1);
			}
		}
	}

	@Override
	protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		super.extractLabels(graphics, mouseX, mouseY);
		Component factor = Component.translatable("jtechstorage.amethyst_chest.factor", menu.tier().stackFactor);
		graphics.text(font, factor, imageWidth - 8 - font.width(factor), titleLabelY, GuiTheme.subtle(), false);
	}
}
