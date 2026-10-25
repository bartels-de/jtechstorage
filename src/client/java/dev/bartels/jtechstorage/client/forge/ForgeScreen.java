package dev.bartels.jtechstorage.client.forge;

import java.util.ArrayList;
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
import dev.bartels.jtechstorage.forge.ForgeMenu;
import dev.bartels.jtechstorage.forge.ForgeUpgrade;

/** Kristall-Schmiede (v0.40): Ausrüstung + Material links, ein Knopf je Verbesserung rechts. */
public class ForgeScreen extends ThemedContainerScreen<ForgeMenu> {
	private static final String[] ROMAN = {"0", "I", "II", "III", "IV", "V"};

	public ForgeScreen(ForgeMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title, ForgeMenu.WIDTH, ForgeMenu.HEIGHT);
		inventoryLabelY = ForgeMenu.INVENTORY_Y - 11;
	}

	private static int buttonY(int i) {
		return ForgeMenu.BUTTON_Y + i * ForgeMenu.BUTTON_GAP;
	}

	private boolean over(int i, double mouseX, double mouseY) {
		int y = topPos + buttonY(i);
		return mouseX >= leftPos + ForgeMenu.BUTTON_X && mouseX < leftPos + ForgeMenu.BUTTON_X + ForgeMenu.BUTTON_W && mouseY >= y
				&& mouseY < y + ForgeMenu.BUTTON_H;
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractBackground(graphics, mouseX, mouseY, delta);
		GuiTheme.window(graphics, leftPos, topPos, imageWidth, imageHeight);
		GuiTheme.slot(graphics, leftPos + ForgeMenu.ITEM_X - 1, topPos + ForgeMenu.ITEM_Y - 1);
		GuiTheme.slot(graphics, leftPos + ForgeMenu.MATERIAL_X - 1, topPos + ForgeMenu.MATERIAL_Y - 1);

		for (int row = 0; row < 4; row++) {
			for (int col = 0; col < 9; col++) {
				int sy = row < 3 ? ForgeMenu.INVENTORY_Y + row * 18 : ForgeMenu.INVENTORY_Y + 58;
				GuiTheme.slot(graphics, leftPos + 8 + col * 18 - 1, topPos + sy - 1);
			}
		}
	}

	@Override
	public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractContents(graphics, mouseX, mouseY, delta);
		ItemStack item = menu.equipment();
		ForgeUpgrade[] all = ForgeUpgrade.values();

		GuiTheme.label(graphics, font, Component.translatable("jtechstorage.forge.item"), leftPos + 6, topPos + ForgeMenu.ITEM_Y - 10, 36,
				GuiTheme.subtle());
		GuiTheme.label(graphics, font, Component.translatable("jtechstorage.forge.material"), leftPos + 6, topPos + ForgeMenu.MATERIAL_Y - 10, 36,
				GuiTheme.subtle());

		for (int i = 0; i < all.length; i++) {
			ForgeUpgrade up = all[i];
			int level = item.isEmpty() ? 0 : ForgeUpgrade.level(item, up);
			Component label = Component.translatable("jtechstorage.forge.upgrade." + up.id())
					.append(" " + ROMAN[Math.min(level, 5)] + "/" + ROMAN[up.maxLevel]);
			int x = leftPos + ForgeMenu.BUTTON_X;
			int y = topPos + buttonY(i);
			GuiTheme.button(graphics, font, x, y, ForgeMenu.BUTTON_W - 18, ForgeMenu.BUTTON_H, label, menu.canApply(up), over(i, mouseX, mouseY));

			if (!item.isEmpty() && up.appliesTo(item) && level < up.maxLevel) {
				graphics.item(up.cost(level + 1), x + ForgeMenu.BUTTON_W - 16, y);
				graphics.itemDecorations(font, up.cost(level + 1), x + ForgeMenu.BUTTON_W - 16, y);
			}
		}
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		for (int i = 0; i < ForgeUpgrade.values().length; i++) {
			if (over(i, event.x(), event.y()) && menu.canApply(ForgeUpgrade.values()[i])) {
				minecraft.gameMode.handleInventoryButtonClick(menu.containerId, i);
				minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
				return true;
			}
		}

		return super.mouseClicked(event, doubleClick);
	}

	@Override
	protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		ItemStack item = menu.equipment();

		for (int i = 0; i < ForgeUpgrade.values().length; i++) {
			if (!over(i, mouseX, mouseY) || !menu.getCarried().isEmpty()) {
				continue;
			}

			ForgeUpgrade up = ForgeUpgrade.values()[i];
			List<Component> lines = new ArrayList<>();
			lines.add(Component.translatable("jtechstorage.forge.upgrade." + up.id()).withStyle(ChatFormatting.LIGHT_PURPLE));
			lines.add(Component.translatable("jtechstorage.forge.upgrade." + up.id() + ".desc").withStyle(ChatFormatting.GRAY));

			if (item.isEmpty()) {
				lines.add(Component.translatable("jtechstorage.forge.no_item").withStyle(ChatFormatting.DARK_GRAY));
			} else if (!up.appliesTo(item)) {
				lines.add(Component.translatable("jtechstorage.forge.wrong_type").withStyle(ChatFormatting.RED));
			} else {
				int level = ForgeUpgrade.level(item, up);

				if (level >= up.maxLevel) {
					lines.add(Component.translatable("jtechstorage.forge.maxed").withStyle(ChatFormatting.GOLD));
				} else {
					ItemStack cost = up.cost(level + 1);
					lines.add(Component.translatable("jtechstorage.forge.cost", cost.getCount(), cost.getHoverName())
							.withStyle(menu.canApply(up) ? ChatFormatting.GREEN : ChatFormatting.RED));
				}
			}

			graphics.setComponentTooltipForNextFrame(font, lines, mouseX, mouseY);
			return;
		}

		super.extractTooltip(graphics, mouseX, mouseY);
	}
}
