package dev.bartels.jtechstorage.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import dev.bartels.jtechstorage.client.gui.GuiTheme;
import dev.bartels.jtechstorage.client.gui.ThemedContainerScreen;
import dev.bartels.jtechstorage.entity.AmethystPhoenix;
import dev.bartels.jtechstorage.menu.AmethystPhoenixMenu;

/**
 * Oberfläche des Amethyst-Phönix. Kopf: Titel, Status, Route und Ladung je in einer eigenen Zeile (nichts überlappt),
 * darunter vier gleich breite Knöpfe; dann vier Kisten (2 × 2) und das Inventar.
 */
public class AmethystPhoenixScreen extends ThemedContainerScreen<AmethystPhoenixMenu> {
	private static final int W = AmethystPhoenixMenu.WIDTH;
	private static final int LINE_STATUS = 19;
	private static final int LINE_ROUTE = 29;
	private static final int LINE_LOAD = 39;
	private static final int BTN_Y = 53;
	private static final int BTN_H = 15;
	private static final int BTN_GAP = 4;
	private static final int BTN_W = (W - 16 - 3 * BTN_GAP) / 4;
	private static final int[] BUTTON_IDS = {AmethystPhoenixMenu.BUTTON_SIT, AmethystPhoenixMenu.BUTTON_DEPART,
			AmethystPhoenixMenu.BUTTON_REMOVE_CHEST, AmethystPhoenixMenu.BUTTON_PACK};

	public AmethystPhoenixScreen(AmethystPhoenixMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title, W, AmethystPhoenixMenu.HEIGHT);
		titleLabelX = 8;
		titleLabelY = 7;
		inventoryLabelX = AmethystPhoenixMenu.INVENTORY_X;
		inventoryLabelY = AmethystPhoenixMenu.INVENTORY_Y - 10;
	}

	private static int buttonX(int index) {
		return 8 + index * (BTN_W + BTN_GAP);
	}

	private int chestX(int chest) {
		return chest % 2 == 0 ? AmethystPhoenixMenu.CHEST_X0 : AmethystPhoenixMenu.CHEST_X1;
	}

	private int chestY(int chest) {
		return chest < 2 ? AmethystPhoenixMenu.CHEST_Y0 : AmethystPhoenixMenu.CHEST_Y1;
	}

	private boolean enabled(int id) {
		return switch (id) {
			case AmethystPhoenixMenu.BUTTON_DEPART -> menu.state() == AmethystPhoenix.STATE_RESTING && !menu.sitting();
			case AmethystPhoenixMenu.BUTTON_REMOVE_CHEST -> menu.chests() > 0;
			case AmethystPhoenixMenu.BUTTON_PACK -> menu.chests() == 0;
			default -> true;
		};
	}

	private Component label(int id) {
		return switch (id) {
			case AmethystPhoenixMenu.BUTTON_SIT -> Component.translatable(menu.sitting()
					? "jtechstorage.amethyst_phoenix.button.stand" : "jtechstorage.amethyst_phoenix.button.sit");
			case AmethystPhoenixMenu.BUTTON_DEPART -> Component.translatable("jtechstorage.amethyst_phoenix.button.depart");
			case AmethystPhoenixMenu.BUTTON_REMOVE_CHEST -> Component.translatable("jtechstorage.amethyst_phoenix.button.remove_chest");
			default -> Component.translatable("jtechstorage.amethyst_phoenix.button.pack");
		};
	}

	private boolean over(int x, int y, int w, int h, double mouseX, double mouseY) {
		return mouseX >= leftPos + x && mouseX < leftPos + x + w && mouseY >= topPos + y && mouseY < topPos + y + h;
	}

	@Override
	protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		GuiTheme.label(graphics, font, title, titleLabelX, titleLabelY, W - 16, GuiTheme.text());
		graphics.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, GuiTheme.text(), false);
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractBackground(graphics, mouseX, mouseY, delta);
		int x = leftPos;
		int y = topPos;
		GuiTheme.panel(graphics, x, y, W, imageHeight);
		// Infobereich als abgesetzte Fläche
		GuiTheme.sunken(graphics, x + 6, y + LINE_STATUS - 3, W - 12, LINE_LOAD + 11 - LINE_STATUS + 3);

		for (int chest = 0; chest < AmethystPhoenix.MAX_CHESTS; chest++) {
			int cx = x + chestX(chest);
			int cy = y + chestY(chest);

			if (chest < menu.chests()) {
				for (int row = 0; row < 3; row++) {
					for (int col = 0; col < 9; col++) {
						GuiTheme.slot(graphics, cx + col * 18, cy + 10 + row * 18);
					}
				}
			} else {
				GuiTheme.sunken(graphics, cx, cy + 10, AmethystPhoenixMenu.CHEST_W, 3 * 18);
			}
		}

		for (int row = 0; row < 4; row++) {
			for (int col = 0; col < 9; col++) {
				int sy = row < 3 ? AmethystPhoenixMenu.INVENTORY_Y + row * 18 : AmethystPhoenixMenu.INVENTORY_Y + 58;
				GuiTheme.slot(graphics, x + AmethystPhoenixMenu.INVENTORY_X + col * 18, y + sy);
			}
		}
	}

	@Override
	public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractContents(graphics, mouseX, mouseY, delta);
		int x = leftPos;
		int y = topPos;
		int infoText = GuiTheme.onColor(GuiTheme.slot());
		int infoSubtle = GuiTheme.mix(infoText, GuiTheme.slot(), 0.25f);

		// Status
		Component status;

		if (menu.sitting()) {
			status = Component.translatable("jtechstorage.amethyst_phoenix.gui.sitting");
		} else if (menu.state() == AmethystPhoenix.STATE_FLYING) {
			status = Component.translatable("jtechstorage.amethyst_phoenix.gui.flying");
		} else if (menu.state() == AmethystPhoenix.STATE_RESTING) {
			status = Component.translatable("jtechstorage.amethyst_phoenix.gui.resting", Math.max(0, menu.minutesLeft()));
		} else {
			status = Component.translatable("jtechstorage.amethyst_phoenix.gui.no_route");
		}

		graphics.text(font, "✦", x + 10, y + LINE_STATUS, 0xFFE0B0FF, true);
		GuiTheme.label(graphics, font, status, x + 20, y + LINE_STATUS, W - 32, infoText);
		GuiTheme.label(graphics, font, routeText(), x + 20, y + LINE_ROUTE, W - 32, infoSubtle);
		Component load = Component.translatable("jtechstorage.amethyst_phoenix.gui.load", menu.chests(), AmethystPhoenix.MAX_CHESTS,
				menu.fillPercent(), menu.speedPercent());
		GuiTheme.label(graphics, font, load, x + 20, y + LINE_LOAD, W - 32 - 64, infoSubtle);

		// Tempo-Balken rechts in der Ladungszeile
		int barX = x + W - 12 - 60;
		int barY = y + LINE_LOAD + 1;
		graphics.fill(barX, barY, barX + 60, barY + 6, 0xFF000000);
		int fill = Math.round(58 * Math.clamp(menu.speedPercent(), 0, 100) / 100.0f);
		int barColor = menu.speedPercent() >= 70 ? 0xFF4CC46C : menu.speedPercent() >= 50 ? 0xFFE0B040 : 0xFFE05A40;
		graphics.fill(barX + 1, barY + 1, barX + 1 + fill, barY + 5, barColor);

		for (int i = 0; i < BUTTON_IDS.length; i++) {
			int id = BUTTON_IDS[i];
			boolean on = enabled(id);
			GuiTheme.button(graphics, font, x + buttonX(i), y + BTN_Y, BTN_W, BTN_H, label(id), on,
					on && over(buttonX(i), BTN_Y, BTN_W, BTN_H, mouseX, mouseY));
		}

		for (int chest = 0; chest < AmethystPhoenix.MAX_CHESTS; chest++) {
			int cx = x + chestX(chest);
			int cy = y + chestY(chest);
			boolean active = chest < menu.chests();
			graphics.text(font, Component.translatable("jtechstorage.amethyst_phoenix.gui.chest", chest + 1), cx + 1, cy,
					active ? GuiTheme.text() : GuiTheme.subtle(), false);

			if (!active) {
				int hintText = GuiTheme.onColor(GuiTheme.slot());
				Component line1 = Component.translatable("jtechstorage.amethyst_phoenix.gui.no_chest");
				Component line2 = Component.translatable("jtechstorage.amethyst_phoenix.gui.no_chest_hint");
				int mid = cx + AmethystPhoenixMenu.CHEST_W / 2;
				String a = GuiTheme.fit(font, line1.getString(), AmethystPhoenixMenu.CHEST_W - 8);
				String b = GuiTheme.fit(font, line2.getString(), AmethystPhoenixMenu.CHEST_W - 8);
				graphics.text(font, a, mid - font.width(a) / 2, cy + 10 + 17, hintText, false);
				graphics.text(font, b, mid - font.width(b) / 2, cy + 10 + 29, GuiTheme.mix(hintText, GuiTheme.slot(), 0.3f), false);
			}
		}
	}

	private Component routeText() {
		java.util.List<String> stops = menu.stops();

		if (stops.size() < 2) {
			return Component.translatable("jtechstorage.amethyst_phoenix.gui.route_none");
		}

		net.minecraft.network.chat.MutableComponent route = Component.empty();

		for (int i = 0; i < stops.size(); i++) {
			if (i > 0) {
				route.append(" → ");
			}

			Component name = Component.literal(stops.get(i));
			route.append(i == menu.target() ? name.copy().withStyle(net.minecraft.ChatFormatting.BOLD) : name);
		}

		return Component.translatable("jtechstorage.amethyst_phoenix.gui.route", route);
	}

	@Override
	protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		super.extractTooltip(graphics, mouseX, mouseY);

		for (int i = 0; i < BUTTON_IDS.length; i++) {
			if (over(buttonX(i), BTN_Y, BTN_W, BTN_H, mouseX, mouseY)) {
				graphics.setTooltipForNextFrame(font, Component.translatable("jtechstorage.amethyst_phoenix.button.tooltip." + BUTTON_IDS[i]),
						mouseX, mouseY);
				return;
			}
		}
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		for (int i = 0; i < BUTTON_IDS.length; i++) {
			if (enabled(BUTTON_IDS[i]) && over(buttonX(i), BTN_Y, BTN_W, BTN_H, event.x(), event.y())) {
				minecraft.gameMode.handleInventoryButtonClick(menu.containerId, BUTTON_IDS[i]);
				return true;
			}
		}

		return super.mouseClicked(event, doubleClick);
	}
}
