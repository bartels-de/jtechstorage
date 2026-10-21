package dev.bartels.jtechstorage.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import dev.bartels.jtechstorage.block.PhoenixStopBlockEntity;
import dev.bartels.jtechstorage.client.gui.GuiTheme;
import dev.bartels.jtechstorage.client.gui.ThemedContainerScreen;
import dev.bartels.jtechstorage.menu.PhoenixStopMenu;
import dev.bartels.jtechstorage.net.PhoenixStopEditPayload;

/**
 * Fenster einer Phönix-Haltestelle: Name (Pflicht), Rastdauer und Lieferauftrag – Modus und 9 Filter mit Menge pro
 * Besuch (Mausrad über dem Filter, Shift = ×16, Strg = ×64, Rechtsklick = alle).
 */
public class PhoenixStopScreen extends ThemedContainerScreen<PhoenixStopMenu> {
	private static final int W = PhoenixStopMenu.WIDTH;
	private static final int NAME_Y = 18;
	private static final int REST_LABEL_Y = 37;
	private static final int REST_Y = 47;
	private static final int MODE_LABEL_Y = 67;
	private static final int MODE_Y = 77;
	private static final int FILTER_LABEL_Y = 97;
	private static final int STATUS_Y = 137;
	private static final int BTN_H = 16;

	private EditBox nameBox;
	private String savedName;

	public PhoenixStopScreen(PhoenixStopMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title, W, PhoenixStopMenu.HEIGHT);
		inventoryLabelX = PhoenixStopMenu.INVENTORY_X - 1;
		inventoryLabelY = PhoenixStopMenu.INVENTORY_Y - 11;
		savedName = menu.initialName();
	}

	@Override
	protected void init() {
		super.init();
		String value = nameBox != null ? nameBox.getValue() : savedName;
		nameBox = new EditBox(font, leftPos + 9, topPos + NAME_Y + 2, W - 18, 12, Component.translatable("jtechstorage.phoenix_stop.name"));
		nameBox.setMaxLength(PhoenixStopBlockEntity.MAX_NAME);
		nameBox.setHint(Component.translatable("jtechstorage.phoenix_stop.name.hint").withStyle(ChatFormatting.GRAY));
		nameBox.setValue(value);
		addRenderableWidget(nameBox);

		if (savedName.isBlank()) {
			setInitialFocus(nameBox);
		}
	}

	private void saveName() {
		String name = PhoenixStopBlockEntity.clean(nameBox.getValue());

		if (!name.isEmpty() && !name.equals(savedName)) {
			savedName = name;
			ClientPlayNetworking.send(new PhoenixStopEditPayload(menu.pos(), name, menu.rest().ordinal()));
		}
	}

	@Override
	public void removed() {
		if (nameBox != null) {
			saveName();
		}

		super.removed();
	}

	// ------------------------------------------------------------------
	// Layout
	// ------------------------------------------------------------------

	private static int restX(int index) {
		return 8 + index * 76;
	}

	private static int modeX(int index) {
		return 8 + index * 57;
	}

	private boolean over(int x, int y, int w, int h, double mouseX, double mouseY) {
		return mouseX >= leftPos + x && mouseX < leftPos + x + w && mouseY >= topPos + y && mouseY < topPos + y + h;
	}

	private int filterAt(double mouseX, double mouseY) {
		for (int slot = 0; slot < PhoenixStopBlockEntity.FILTERS; slot++) {
			if (over(PhoenixStopMenu.FILTER_X + slot * 18 - 1, PhoenixStopMenu.FILTER_Y - 1, 18, 18, mouseX, mouseY)) {
				return slot;
			}
		}

		return -1;
	}

	private static String amountText(int amount) {
		if (amount <= 0) {
			return "∞";
		}

		return amount >= 1000 ? (amount / 1000) + "k" + (amount % 1000 >= 100 ? (amount % 1000) / 100 : "") : String.valueOf(amount);
	}

	// ------------------------------------------------------------------
	// Zeichnen
	// ------------------------------------------------------------------

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractBackground(graphics, mouseX, mouseY, delta);
		int x = leftPos;
		int y = topPos;
		GuiTheme.window(graphics, x, y, imageWidth, imageHeight);
		GuiTheme.sunken(graphics, x + 7, y + NAME_Y, W - 14, 16);
		GuiTheme.sunken(graphics, x + 7, y + STATUS_Y - 2, W - 14, 12);

		for (int slot = 0; slot < PhoenixStopBlockEntity.FILTERS; slot++) {
			GuiTheme.slot(graphics, x + PhoenixStopMenu.FILTER_X + slot * 18 - 1, y + PhoenixStopMenu.FILTER_Y - 1);
		}

		for (int row = 0; row < 4; row++) {
			for (int col = 0; col < 9; col++) {
				int sy = row < 3 ? PhoenixStopMenu.INVENTORY_Y + row * 18 : PhoenixStopMenu.INVENTORY_Y + 58;
				GuiTheme.slot(graphics, x + PhoenixStopMenu.INVENTORY_X + col * 18 - 1, y + sy - 1);
			}
		}
	}

	@Override
	public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractContents(graphics, mouseX, mouseY, delta);
		int x = leftPos;
		int y = topPos;

		graphics.text(font, Component.translatable("jtechstorage.phoenix_stop.rest"), x + 8, y + REST_LABEL_Y, GuiTheme.text(), false);
		PhoenixStopBlockEntity.Rest[] rests = PhoenixStopBlockEntity.Rest.values();

		for (int i = 0; i < rests.length; i++) {
			boolean selected = rests[i] == menu.rest();
			boolean hover = over(restX(i), REST_Y, 72, BTN_H, mouseX, mouseY);
			int color = selected ? GuiTheme.accent() : hover ? GuiTheme.accentDarkHover() : GuiTheme.accentDark();
			GuiTheme.labeled(graphics, font, x + restX(i), y + REST_Y, 72, BTN_H, Component.translatable(rests[i].key()), color, true);
		}

		graphics.text(font, Component.translatable("jtechstorage.phoenix_stop.order"), x + 8, y + MODE_LABEL_Y, GuiTheme.text(), false);
		PhoenixStopBlockEntity.Mode[] modes = PhoenixStopBlockEntity.Mode.values();

		for (int i = 0; i < modes.length; i++) {
			boolean selected = modes[i] == menu.mode();
			boolean hover = over(modeX(i), MODE_Y, 53, BTN_H, mouseX, mouseY);
			int color = selected ? GuiTheme.accent() : hover ? GuiTheme.accentDarkHover() : GuiTheme.accentDark();
			GuiTheme.labeled(graphics, font, x + modeX(i), y + MODE_Y, 53, BTN_H, Component.translatable(modes[i].key()), color, true);
		}

		boolean active = menu.mode() != PhoenixStopBlockEntity.Mode.NONE;
		int labelColor = active ? GuiTheme.text() : GuiTheme.subtle();
		GuiTheme.label(graphics, font, Component.translatable("jtechstorage.phoenix_stop.filters"), x + 8, y + FILTER_LABEL_Y, W - 16, labelColor);

		for (int slot = 0; slot < PhoenixStopBlockEntity.FILTERS; slot++) {
			ItemStack filter = menu.getSlot(slot).getItem();

			if (filter.isEmpty()) {
				continue;
			}

			String text = amountText(menu.amount(slot));
			int sx = x + PhoenixStopMenu.FILTER_X + slot * 18;
			int sy = y + PhoenixStopMenu.FILTER_Y;
			graphics.text(font, text, sx + 8 - font.width(text) / 2, sy + 19, labelColor, false);
		}

		int infoText = GuiTheme.onColor(GuiTheme.slot());
		Component status;

		if (savedName.isBlank()) {
			status = Component.translatable("jtechstorage.phoenix_stop.name_required").withStyle(ChatFormatting.RED);
		} else if (menu.minutesSinceVisit() < 0) {
			status = Component.translatable("jtechstorage.phoenix_stop.never_visited");
		} else {
			status = Component.translatable("jtechstorage.phoenix_stop.last_visit", menu.minutesSinceVisit(), menu.lastUnloaded(), menu.lastLoaded());
		}

		GuiTheme.label(graphics, font, status, x + 10, y + STATUS_Y, W - 20, infoText);
	}

	@Override
	protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		int filter = filterAt(mouseX, mouseY);

		if (filter >= 0 && menu.getCarried().isEmpty()) {
			List<Component> lines = new ArrayList<>();
			ItemStack stack = menu.getSlot(filter).getItem();

			if (stack.isEmpty()) {
				lines.add(Component.translatable("jtechstorage.phoenix_stop.filter.empty"));
			} else {
				lines.add(stack.getHoverName());
				int amount = menu.amount(filter);
				lines.add(Component.translatable(amount > 0 ? "jtechstorage.phoenix_stop.filter.amount" : "jtechstorage.phoenix_stop.filter.all", amount)
						.withStyle(ChatFormatting.LIGHT_PURPLE));
			}

			lines.add(Component.translatable("jtechstorage.phoenix_stop.filter.help.0").withStyle(ChatFormatting.GRAY));
			lines.add(Component.translatable("jtechstorage.phoenix_stop.filter.help.1").withStyle(ChatFormatting.GRAY));
			graphics.setComponentTooltipForNextFrame(font, lines, mouseX, mouseY);
			return;
		}

		PhoenixStopBlockEntity.Rest[] rests = PhoenixStopBlockEntity.Rest.values();

		for (int i = 0; i < rests.length; i++) {
			if (over(restX(i), REST_Y, 72, BTN_H, mouseX, mouseY)) {
				graphics.setTooltipForNextFrame(font, Component.translatable(rests[i].key() + ".tooltip"), mouseX, mouseY);
				return;
			}
		}

		PhoenixStopBlockEntity.Mode[] modes = PhoenixStopBlockEntity.Mode.values();

		for (int i = 0; i < modes.length; i++) {
			if (over(modeX(i), MODE_Y, 53, BTN_H, mouseX, mouseY)) {
				graphics.setTooltipForNextFrame(font, Component.translatable(modes[i].key() + ".tooltip"), mouseX, mouseY);
				return;
			}
		}

		super.extractTooltip(graphics, mouseX, mouseY);
	}

	// ------------------------------------------------------------------
	// Eingabe
	// ------------------------------------------------------------------

	private void button(int id) {
		minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
	}

	private void click() {
		minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		for (int i = 0; i < PhoenixStopBlockEntity.Rest.values().length; i++) {
			if (over(restX(i), REST_Y, 72, BTN_H, event.x(), event.y())) {
				button(PhoenixStopMenu.BUTTON_REST + i);
				click();
				return true;
			}
		}

		for (int i = 0; i < PhoenixStopBlockEntity.Mode.values().length; i++) {
			if (over(modeX(i), MODE_Y, 53, BTN_H, event.x(), event.y())) {
				button(PhoenixStopMenu.BUTTON_MODE + i);
				click();
				return true;
			}
		}

		if (!over(9, NAME_Y, W - 18, 16, event.x(), event.y()) && nameBox.isFocused()) {
			saveName();
			nameBox.setFocused(false);
			setFocused(null);
		}

		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		int filter = filterAt(mouseX, mouseY);

		if (filter >= 0 && scrollY != 0 && !menu.getSlot(filter).getItem().isEmpty()) {
			boolean up = scrollY > 0;
			int step = minecraft.hasControlDown() ? (up ? 4 : 5) : minecraft.hasShiftDown() ? (up ? 2 : 3) : (up ? 0 : 1);
			button(PhoenixStopMenu.amountButton(filter, step));
			return true;
		}

		return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (nameBox.isFocused()) {
			if (event.isEscape()) {
				nameBox.setFocused(false);
				setFocused(null);
			} else if (event.key() == 257 || event.key() == 335) {
				saveName();
				nameBox.setFocused(false);
				setFocused(null);
			} else {
				nameBox.keyPressed(event);
			}

			return true;
		}

		return super.keyPressed(event);
	}

	@Override
	public boolean charTyped(CharacterEvent event) {
		if (nameBox.isFocused()) {
			return nameBox.charTyped(event);
		}

		return super.charTyped(event);
	}
}
