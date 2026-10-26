package dev.bartels.jtechstorage.client.farm;

import java.text.NumberFormat;

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

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import dev.bartels.jtechstorage.client.gui.GuiTheme;
import dev.bartels.jtechstorage.client.gui.ThemedContainerScreen;
import dev.bartels.jtechstorage.farm.FarmMonitorBlockEntity;
import dev.bartels.jtechstorage.farm.FarmMonitorMenu;
import dev.bartels.jtechstorage.farm.FarmPayloads;

/** Farm Monitor (v0.27.0): name, status, items per hour, history, most common items, warning time, safe zone. */
public class FarmMonitorScreen extends ThemedContainerScreen<FarmMonitorMenu> {
	private static final int W = FarmMonitorMenu.WIDTH;
	private static final int NAME_Y = 18;
	private static final int STATUS_Y = 40;
	private static final int CHART_Y = 66;
	private static final int CHART_H = 38;
	private static final int IDLE_Y = 160;
	private static final int PROTECT_Y = 180;
	private static final int RESET_Y = 200;
	private static final int BTN_H = 14;

	private EditBox nameBox;
	private String savedName;

	public FarmMonitorScreen(FarmMonitorMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title, W, FarmMonitorMenu.HEIGHT);
		savedName = menu.initialName();
		inventoryLabelY = -1000;
	}

	@Override
	protected void init() {
		super.init();
		String value = nameBox != null ? nameBox.getValue() : savedName;
		nameBox = new EditBox(font, leftPos + 10, topPos + NAME_Y + 2, W - 20, 12, Component.translatable("jtechstorage.farm.name"));
		nameBox.setMaxLength(FarmMonitorBlockEntity.MAX_NAME);
		nameBox.setHint(Component.translatable("jtechstorage.farm.name.hint").withStyle(ChatFormatting.GRAY));
		nameBox.setValue(value);
		addRenderableWidget(nameBox);
	}

	private void saveName() {
		String name = FarmMonitorBlockEntity.clean(nameBox.getValue());

		if (!name.equals(savedName)) {
			savedName = name;
			ClientPlayNetworking.send(new FarmPayloads.Rename(menu.pos(), name));
		}
	}

	@Override
	public void removed() {
		if (nameBox != null) {
			saveName();
		}

		super.removed();
	}

	private boolean over(int x, int y, int w, int h, double mouseX, double mouseY) {
		return mouseX >= leftPos + x && mouseX < leftPos + x + w && mouseY >= topPos + y && mouseY < topPos + y + h;
	}

	private static String number(long value) {
		return NumberFormat.getIntegerInstance().format(value);
	}

	private static String compact(long value) {
		return value >= 1_000_000 ? (value / 100_000) / 10.0 + "M" : value >= 10_000 ? value / 1000 + "k" : String.valueOf(value);
	}

	private static int idleX(int i) {
		return 96 + i * 36;
	}

	private static int protectX(int i) {
		return 96 + i * 21;
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractBackground(graphics, mouseX, mouseY, delta);
		int x = leftPos;
		int y = topPos;
		GuiTheme.window(graphics, x, y, imageWidth, imageHeight);
		GuiTheme.sunken(graphics, x + 8, y + NAME_Y, W - 16, 16);
		GuiTheme.sunken(graphics, x + 8, y + CHART_Y - 2, W - 16, CHART_H + 4);

		for (int i = 0; i < FarmMonitorBlockEntity.TOP; i++) {
			GuiTheme.slot(graphics, x + FarmMonitorMenu.TOP_X + i * FarmMonitorMenu.TOP_STEP - 1, y + FarmMonitorMenu.TOP_Y - 1);
		}
	}

	@Override
	public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractContents(graphics, mouseX, mouseY, delta);
		int x = leftPos;
		int y = topPos;
		int text = GuiTheme.text();
		int subtle = GuiTheme.subtle();

		// Status
		int status = menu.status();
		int color = status == FarmMonitorBlockEntity.OK ? 0xFF4CC46C : 0xFFE05A40;
		Component statusText = status == FarmMonitorBlockEntity.IDLE
				? Component.translatable("jtechstorage.farm.status.1", Math.max(0, menu.since()))
				: Component.translatable("jtechstorage.farm.status." + status);
		graphics.text(font, "●", x + 10, y + STATUS_Y, color, false);
		GuiTheme.label(graphics, font, statusText, x + 20, y + STATUS_Y, W - 30, text);
		Component rate = Component.translatable("jtechstorage.farm.rate", number(menu.perHour()), menu.elapsed() >= 60 ? "60+" : String.valueOf(menu.elapsed()));
		GuiTheme.label(graphics, font, rate, x + 10, y + STATUS_Y + 11, W - 20, subtle);

		// History (12 × 5 minutes)
		int max = 1;

		for (int b = 0; b < FarmMonitorBlockEntity.HISTORY; b++) {
			max = Math.max(max, menu.history(b));
		}

		int cw = (W - 24) / FarmMonitorBlockEntity.HISTORY;
		int hovered = -1;

		for (int b = 0; b < FarmMonitorBlockEntity.HISTORY; b++) {
			int v = menu.history(b);
			int h = v == 0 ? 0 : Math.max(1, Math.round((CHART_H - 2) * v / (float) max));
			int bx = x + 12 + b * cw;
			graphics.fill(bx, y + CHART_Y + CHART_H - h, bx + cw - 2, y + CHART_Y + CHART_H, b == FarmMonitorBlockEntity.HISTORY - 1 ? GuiTheme.accentHover() : GuiTheme.accent());

			if (mouseX >= bx && mouseX < bx + cw && mouseY >= y + CHART_Y && mouseY < y + CHART_Y + CHART_H) {
				hovered = b;
			}
		}

		graphics.text(font, Component.translatable("jtechstorage.farm.chart"), x + 10, y + CHART_Y + CHART_H + 5, subtle, false);

		// Most common items
		graphics.text(font, Component.translatable("jtechstorage.farm.top"), x + 10, y + FarmMonitorMenu.TOP_Y - 12, text, false);

		for (int i = 0; i < FarmMonitorBlockEntity.TOP; i++) {
			if (!menu.getSlot(i).getItem().isEmpty()) {
				String count = compact(menu.topCount(i));
				graphics.text(font, count, x + FarmMonitorMenu.TOP_X + i * FarmMonitorMenu.TOP_STEP + 18, y + FarmMonitorMenu.TOP_Y + 5, subtle, false);
			}
		}

		// Warning time
		graphics.text(font, Component.translatable("jtechstorage.farm.idle"), x + 10, y + IDLE_Y + 3, text, false);

		for (int i = 0; i < FarmMonitorBlockEntity.IDLE_MINUTES.length; i++) {
			boolean sel = menu.idle() == i;
			boolean hover = over(idleX(i), IDLE_Y, 34, BTN_H, mouseX, mouseY);
			GuiTheme.labeled(graphics, font, x + idleX(i), y + IDLE_Y, 34, BTN_H, Component.literal(FarmMonitorBlockEntity.IDLE_MINUTES[i] + " min"),
					sel ? GuiTheme.accent() : hover ? GuiTheme.accentDarkHover() : GuiTheme.accentDark(), true);
		}

		// Safe zone
		graphics.text(font, Component.translatable("jtechstorage.farm.protect"), x + 10, y + PROTECT_Y + 3, text, false);

		for (int i = 0; i < FarmMonitorBlockEntity.RADII.length; i++) {
			boolean sel = menu.protect() == i;
			boolean hover = over(protectX(i), PROTECT_Y, 20, BTN_H, mouseX, mouseY);
			Component label = i == 0 ? Component.literal("–") : Component.literal(String.valueOf(FarmMonitorBlockEntity.RADII[i]));
			GuiTheme.labeled(graphics, font, x + protectX(i), y + PROTECT_Y, 20, BTN_H, label,
					sel ? GuiTheme.accent() : hover ? GuiTheme.accentDarkHover() : GuiTheme.accentDark(), true);
		}

		// Reset + connection
		boolean resetHover = over(W - 8 - 80, RESET_Y, 80, BTN_H, mouseX, mouseY);
		GuiTheme.labeled(graphics, font, x + W - 8 - 80, y + RESET_Y, 80, BTN_H, Component.translatable("jtechstorage.farm.reset"),
				resetHover ? GuiTheme.accentDarkHover() : GuiTheme.accentDark(), true);
		Component link = Component.translatable(menu.linked() ? "jtechstorage.farm.link_yes" : "jtechstorage.farm.link_no");
		GuiTheme.label(graphics, font, link, x + 10, y + RESET_Y + 3, W - 110, menu.linked() ? GuiTheme.good() : subtle);

		if (hovered >= 0) {
			int minutesAgo = (FarmMonitorBlockEntity.HISTORY - 1 - hovered) * 5;
			graphics.setTooltipForNextFrame(font, Component.translatable("jtechstorage.farm.chart.tip", menu.history(hovered), minutesAgo + 5, minutesAgo),
					mouseX, mouseY);
		} else if (over(96, PROTECT_Y, 110, BTN_H, mouseX, mouseY)) {
			graphics.setTooltipForNextFrame(font, font.split(Component.translatable("jtechstorage.farm.protect.tip"), 200), mouseX, mouseY);
		} else if (over(96, IDLE_Y, 110, BTN_H, mouseX, mouseY)) {
			graphics.setTooltipForNextFrame(font, font.split(Component.translatable("jtechstorage.farm.idle.tip"), 200), mouseX, mouseY);
		} else if (over(10, RESET_Y, W - 110, BTN_H, mouseX, mouseY) && !menu.linked()) {
			graphics.setTooltipForNextFrame(font, font.split(Component.translatable("jtechstorage.farm.link.tip"), 200), mouseX, mouseY);
		}
	}

	private void button(int id) {
		minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
		minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		for (int i = 0; i < FarmMonitorBlockEntity.IDLE_MINUTES.length; i++) {
			if (over(idleX(i), IDLE_Y, 34, BTN_H, event.x(), event.y())) {
				button(FarmMonitorMenu.BUTTON_IDLE + i);
				return true;
			}
		}

		for (int i = 0; i < FarmMonitorBlockEntity.RADII.length; i++) {
			if (over(protectX(i), PROTECT_Y, 20, BTN_H, event.x(), event.y())) {
				button(FarmMonitorMenu.BUTTON_PROTECT + i);
				return true;
			}
		}

		if (over(W - 8 - 80, RESET_Y, 80, BTN_H, event.x(), event.y())) {
			button(FarmMonitorMenu.BUTTON_RESET);
			return true;
		}

		if (!over(8, NAME_Y, W - 16, 16, event.x(), event.y()) && nameBox.isFocused()) {
			saveName();
			nameBox.setFocused(false);
			setFocused(null);
		}

		return super.mouseClicked(event, doubleClick);
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
