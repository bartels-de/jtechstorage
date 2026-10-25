package dev.bartels.jtechstorage.client.display;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.jspecify.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import dev.bartels.jtechstorage.client.gui.GuiTheme;
import dev.bartels.jtechstorage.client.gui.ThemedScreen;
import dev.bartels.jtechstorage.net.PanelItemPayload;
import dev.bartels.jtechstorage.net.PanelSettingPayload;

/**
 * Gemeinsame Basis der Menüs von Lager-Bildschirm und Lager-Banner (v0.36): zwei Reiter (Anzeige, Aussehen), ein
 * durchsuchbares Raster mit den Gegenständen des Netzwerks, Farbfelder und Schalter. Die Werte liest das Menü live aus
 * dem Block (der Server schickt nach jeder Änderung ein Block-Update).
 */
public abstract class PanelScreen<T extends BlockEntity> extends ThemedScreen {
	protected static final int W = 330;
	protected static final int H = 196;
	protected static final int GRID_COLS = 6;
	protected static final int GRID_ROWS = 3;

	protected final BlockPos pos;
	private final List<ItemStack> networkItems;
	private final List<Long> networkCounts;
	protected int tab;
	protected @Nullable EditBox search;
	private List<Integer> filtered = new ArrayList<>();
	private int scroll;
	private int gridX;
	private int gridY;
	/** Tooltip für dieses Bild (Zeilen), wird am Ende gezeichnet. */
	private @Nullable List<FormattedCharSequence> tooltip;
	/** Klickflächen dieses Bildes. */
	private final List<Hit> hits = new ArrayList<>();

	private record Hit(int x, int y, int w, int h, Runnable action) {
	}

	protected PanelScreen(Component title, BlockPos pos, List<ItemStack> items, List<Long> counts) {
		super(title);
		this.pos = pos;
		this.networkItems = items;
		this.networkCounts = counts;
	}

	protected int left() {
		return (width - W) / 2;
	}

	protected int top() {
		return (height - H) / 2;
	}

	@SuppressWarnings("unchecked")
	protected @Nullable T block() {
		var level = Minecraft.getInstance().level;

		if (level == null) {
			return null;
		}

		BlockEntity be = level.getBlockEntity(pos);
		return be != null && blockClass().isInstance(be) ? (T) be : null;
	}

	protected abstract Class<T> blockClass();

	/** Wo das Netzwerk-Raster auf dem Reiter „Anzeige“ sitzt (relativ zum Fenster). */
	protected abstract int gridLeft();

	protected abstract int gridTop();

	@Override
	protected void init() {
		gridX = left() + gridLeft();
		gridY = top() + gridTop();
		search = new EditBox(font, gridX + 3, gridY - 15, 98, 9, Component.translatable("jtechstorage.panel.search"));
		search.setBordered(false);
		search.setTextColor(GuiTheme.fieldText());
		search.setHint(Component.translatable("jtechstorage.panel.search").withStyle(ChatFormatting.GRAY));
		search.setResponder(text -> {
			scroll = 0;
			refilter();
		});
		search.setVisible(tab == 0);
		addRenderableWidget(search);
		refilter();
	}

	private void refilter() {
		String q = search == null ? "" : search.getValue().toLowerCase(Locale.ROOT).trim();
		filtered = new ArrayList<>();

		for (int i = 0; i < networkItems.size(); i++) {
			ItemStack s = networkItems.get(i);

			if (q.isEmpty() || s.getHoverName().getString().toLowerCase(Locale.ROOT).contains(q)
					|| BuiltInRegistries.ITEM.getKey(s.getItem()).getPath().contains(q)) {
				filtered.add(i);
			}
		}
	}

	// ------------------------------------------------------------------ Senden

	protected void send(int key, int value) {
		ClientPlayNetworking.send(new PanelSettingPayload(pos, key, value));
	}

	protected void pick(ItemStack stack) {
		ClientPlayNetworking.send(new PanelItemPayload(pos, stack));
	}

	// ------------------------------------------------------------------ Zeichnen

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractRenderState(graphics, mouseX, mouseY, delta);
		hits.clear();
		tooltip = null;
		T be = block();

		if (be == null) {
			onClose();
			return;
		}

		int x = left();
		int y = top();
		GuiTheme.window(graphics, x, y, W, H);
		GuiTheme.title(graphics, font, title, x + 8, y + 8);

		// Reiter
		String[] tabs = {"jtechstorage.panel.tab.show", "jtechstorage.panel.tab.look"};

		for (int i = 0; i < 2; i++) {
			int tx = x + W - 128 + i * 62;
			final int index = i;
			boolean hover = over(tx, y + 5, 60, 14, mouseX, mouseY);
			GuiTheme.toggle(graphics, tx, y + 5, 60, 14, tab == i, hover);
			String label = GuiTheme.fit(font, Component.translatable(tabs[i]).getString(), 56);
			graphics.text(font, label, tx + (60 - font.width(label)) / 2, y + 9, tab == i ? GuiTheme.onColor(GuiTheme.accent()) : GuiTheme.text(), false);
			hits.add(new Hit(tx, y + 5, 60, 14, () -> {
				tab = index;

				if (search != null) {
					search.setVisible(index == 0);
				}
			}));
		}

		if (tab == 0) {
			drawShow(graphics, be, x, y, mouseX, mouseY);
		} else {
			drawLook(graphics, be, x, y, mouseX, mouseY);
		}

		boolean doneHover = over(x + W - 70, y + H - 20, 62, 14, mouseX, mouseY);
		GuiTheme.button(graphics, font, x + W - 70, y + H - 20, 62, 14, CommonComponents.GUI_DONE, true, doneHover);
		hits.add(new Hit(x + W - 70, y + H - 20, 62, 14, this::onClose));

		if (tooltip != null) {
			graphics.setTooltipForNextFrame(font, tooltip, mouseX, mouseY);
		}
	}

	protected abstract void drawShow(GuiGraphicsExtractor graphics, T be, int x, int y, int mouseX, int mouseY);

	protected abstract void drawLook(GuiGraphicsExtractor graphics, T be, int x, int y, int mouseX, int mouseY);

	/** Netzwerk-Raster mit Suchfeld; {@code picked}: hervorgehobene Gegenstände. */
	protected void drawGrid(GuiGraphicsExtractor graphics, List<ItemStack> picked, int mouseX, int mouseY) {
		GuiTheme.sunken(graphics, gridX, gridY - 18, 104, 14);
		int rows = Math.max(0, (filtered.size() + GRID_COLS - 1) / GRID_COLS - GRID_ROWS);
		scroll = Math.max(0, Math.min(scroll, rows));

		for (int i = 0; i < GRID_COLS * GRID_ROWS; i++) {
			int sx = gridX + (i % GRID_COLS) * 18;
			int sy = gridY + (i / GRID_COLS) * 18;
			GuiTheme.slot(graphics, sx, sy);
			int index = (scroll * GRID_COLS) + i;

			if (index >= filtered.size()) {
				continue;
			}

			int n = filtered.get(index);
			ItemStack stack = networkItems.get(n);
			boolean isPicked = picked.stream().anyMatch(p -> ItemStack.isSameItemSameComponents(p, stack));

			if (isPicked) {
				graphics.fill(sx + 1, sy + 1, sx + 17, sy + 17, GuiTheme.alpha(GuiTheme.accent(), 0x70));
			}

			graphics.item(stack, sx + 1, sy + 1);

			if (over(sx, sy, 18, 18, mouseX, mouseY)) {
				graphics.fill(sx + 1, sy + 1, sx + 17, sy + 17, 0x40FFFFFF);
				tooltip = List.of(stack.getHoverName().getVisualOrderText(),
						Component.translatable("jtechstorage.panel.in_storage", PanelCanvas.full(networkCounts.get(n))).withStyle(ChatFormatting.GRAY)
								.getVisualOrderText());
			}

			hits.add(new Hit(sx, sy, 18, 18, () -> pick(stack)));
		}

		// Bildlaufleiste
		int barX = gridX + GRID_COLS * 18 + 3;
		graphics.fill(barX, gridY, barX + 4, gridY + GRID_ROWS * 18, GuiTheme.slotDark());

		if (rows > 0) {
			int knob = Math.max(8, GRID_ROWS * 18 * GRID_ROWS / (rows + GRID_ROWS));
			int ky = gridY + (GRID_ROWS * 18 - knob) * scroll / rows;
			graphics.fill(barX, ky, barX + 4, ky + knob, GuiTheme.accent());
		}

		if (networkItems.isEmpty()) {
			text(graphics, Component.translatable("jtechstorage.panel.no_network"), gridX, gridY + GRID_ROWS * 18 + 4, GuiTheme.subtle());
		}
	}

	protected boolean overGrid(double mx, double my) {
		return mx >= gridX && mx < gridX + GRID_COLS * 18 + 8 && my >= gridY && my < gridY + GRID_ROWS * 18;
	}

	@Override
	public boolean mouseScrolled(double mx, double my, double sx, double sy) {
		if (tab == 0 && overGrid(mx, my)) {
			scroll = Math.max(0, scroll - (int) Math.signum(sy));
			return true;
		}

		return super.mouseScrolled(mx, my, sx, sy);
	}

	/** Farbfelder in einer Reihe; Klick sendet den Index. */
	protected void swatches(GuiGraphicsExtractor graphics, int x, int y, int[] colors, int selected, int size, int perRow, int key,
			String namePrefix, String[] names, int mouseX, int mouseY) {
		for (int i = 0; i < colors.length; i++) {
			int cx = x + (i % perRow) * (size + 2);
			int cy = y + (i / perRow) * (size + 2);
			final int value = i;

			if (i == selected) {
				graphics.fill(cx - 1, cy - 1, cx + size + 1, cy + size + 1, 0xFFFFFFFF);
			}

			graphics.fill(cx, cy, cx + size, cy + size, 0xFF000000);
			graphics.fill(cx + 1, cy + 1, cx + size - 1, cy + size - 1, colors[i] == 0 ? 0xFF3A3A44 : colors[i]);

			if (colors[i] == 0) {                    // „ohne“: diagonal durchgestrichen
				for (int k = 1; k < size - 1; k++) {
					graphics.fill(cx + k, cy + size - 1 - k, cx + k + 1, cy + size - k, 0xFFFF6A6A);
				}
			}

			if (over(cx, cy, size, size, mouseX, mouseY)) {
				tooltip = List.of(Component.translatable(namePrefix + names[i]).getVisualOrderText());
			}

			hits.add(new Hit(cx, cy, size, size, () -> send(key, value)));
		}
	}

	/** Knopfreihe mit Beschriftung; Klick sendet den Index. */
	protected void choices(GuiGraphicsExtractor graphics, int x, int y, int w, int h, String namePrefix, String[] names, int selected, int key,
			int mouseX, int mouseY) {
		choices(graphics, x, y, w, h, names.length, namePrefix, names, selected, key, mouseX, mouseY);
	}

	/** Knopfreihen mit höchstens {@code perRow} Knöpfen pro Zeile. */
	protected void choices(GuiGraphicsExtractor graphics, int x, int y, int w, int h, int perRow, String namePrefix, String[] names, int selected,
			int key, int mouseX, int mouseY) {
		for (int i = 0; i < names.length; i++) {
			int bx = x + (i % perRow) * (w + 2);
			int by = y + (i / perRow) * (h + 2);
			final int value = i;
			boolean hover = over(bx, by, w, h, mouseX, mouseY);
			GuiTheme.toggle(graphics, bx, by, w, h, i == selected, hover);
			String label = Component.translatable(namePrefix + names[i]).getString();
			String fitted = GuiTheme.fit(font, label, w - 4);
			graphics.text(font, fitted, bx + (w - font.width(fitted) + 1) / 2, by + (h - 7) / 2, i == selected ? GuiTheme.onColor(GuiTheme.accent())
					: GuiTheme.text(), false);

			if (hover && !fitted.equals(label)) {
				tooltip = List.of(Component.literal(label).getVisualOrderText());
			}

			hits.add(new Hit(bx, by, w, h, () -> send(key, value)));
		}
	}

	/** Ein/aus-Schalter mit Beschriftung. */
	protected void toggle(GuiGraphicsExtractor graphics, int x, int y, Component label, boolean on, int key, int mouseX, int mouseY) {
		graphics.text(font, label, x, y + 3, GuiTheme.text(), false);
		int bx = x + 96;
		boolean hover = over(bx, y, 30, 13, mouseX, mouseY);
		GuiTheme.toggle(graphics, bx, y, 30, 13, on, hover);
		Component state = Component.translatable(on ? "jtechstorage.panel.on" : "jtechstorage.panel.off");
		graphics.text(font, state, bx + (30 - font.width(state)) / 2, y + 3, on ? GuiTheme.onColor(GuiTheme.accent()) : GuiTheme.text(), false);
		hits.add(new Hit(bx, y, 30, 13, () -> send(key, on ? 0 : 1)));
	}

	protected void button(GuiGraphicsExtractor graphics, int x, int y, int w, int h, Component label, Runnable action, int mouseX, int mouseY) {
		GuiTheme.button(graphics, font, x, y, w, h, label, true, over(x, y, w, h, mouseX, mouseY));
		hits.add(new Hit(x, y, w, h, action));
	}

	protected void text(GuiGraphicsExtractor graphics, Component text, int x, int y, int color) {
		graphics.text(font, text, x, y, color, false);
	}

	protected void label(GuiGraphicsExtractor graphics, String key, int x, int y) {
		graphics.text(font, Component.translatable(key), x, y, GuiTheme.subtle(), false);
	}

	protected void hit(int x, int y, int w, int h, Runnable action) {
		hits.add(new Hit(x, y, w, h, action));
	}

	protected void tooltip(List<FormattedCharSequence> lines) {
		tooltip = lines;
	}

	protected static boolean over(int x, int y, int w, int h, double mx, double my) {
		return mx >= x && mx < x + w && my >= y && my < y + h;
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		for (int i = hits.size() - 1; i >= 0; i--) {
			Hit h = hits.get(i);

			if (over(h.x(), h.y(), h.w(), h.h(), event.x(), event.y())) {
				h.action().run();
				return true;
			}
		}

		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
