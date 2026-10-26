package dev.bartels.jtechstorage.client.farm;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import dev.bartels.jtechstorage.client.gui.GuiTheme;
import dev.bartels.jtechstorage.client.gui.ThemedContainerScreen;
import dev.bartels.jtechstorage.farm.CrystalPusherBlockEntity;
import dev.bartels.jtechstorage.farm.CrystalPusherMenu;

/** Filter of the Crystal Pusher (v0.27.0). */
public class CrystalPusherScreen extends ThemedContainerScreen<CrystalPusherMenu> {
	public CrystalPusherScreen(CrystalPusherMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title, CrystalPusherMenu.WIDTH, CrystalPusherMenu.HEIGHT);
		inventoryLabelY = CrystalPusherMenu.INVENTORY_Y - 11;
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractBackground(graphics, mouseX, mouseY, delta);
		GuiTheme.window(graphics, leftPos, topPos, imageWidth, imageHeight);

		for (int slot = 0; slot < CrystalPusherBlockEntity.FILTERS; slot++) {
			GuiTheme.slot(graphics, leftPos + CrystalPusherMenu.FILTER_X + slot * 18 - 1, topPos + CrystalPusherMenu.FILTER_Y - 1);
		}

		for (int row = 0; row < 4; row++) {
			for (int col = 0; col < 9; col++) {
				int sy = row < 3 ? CrystalPusherMenu.INVENTORY_Y + row * 18 : CrystalPusherMenu.INVENTORY_Y + 58;
				GuiTheme.slot(graphics, leftPos + 8 + col * 18 - 1, topPos + sy - 1);
			}
		}
	}

	@Override
	public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractContents(graphics, mouseX, mouseY, delta);
		boolean empty = true;

		for (int slot = 0; slot < CrystalPusherBlockEntity.FILTERS; slot++) {
			empty &= menu.getSlot(slot).getItem().isEmpty();
		}

		Component hint = Component.translatable(empty ? "jtechstorage.pusher.filter_all" : "jtechstorage.pusher.filter_some");
		GuiTheme.label(graphics, font, Component.translatable("jtechstorage.pusher.filter"), leftPos + 8, topPos + 22, imageWidth - 16, GuiTheme.text());
		GuiTheme.label(graphics, font, hint, leftPos + 8, topPos + 56, imageWidth - 16, GuiTheme.subtle());
	}

	@Override
	protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		for (int slot = 0; slot < CrystalPusherBlockEntity.FILTERS; slot++) {
			int sx = leftPos + CrystalPusherMenu.FILTER_X + slot * 18;
			int sy = topPos + CrystalPusherMenu.FILTER_Y;

			if (mouseX >= sx - 1 && mouseX < sx + 17 && mouseY >= sy - 1 && mouseY < sy + 17 && menu.getCarried().isEmpty()) {
				ItemStack stack = menu.getSlot(slot).getItem();
				graphics.setComponentTooltipForNextFrame(font, List.of(
						stack.isEmpty() ? Component.translatable("jtechstorage.pusher.filter.empty") : stack.getHoverName(),
						Component.translatable("jtechstorage.pusher.filter.help").withStyle(ChatFormatting.GRAY)), mouseX, mouseY);
				return;
			}
		}

		super.extractTooltip(graphics, mouseX, mouseY);
	}
}
