package dev.bartels.jtechstorage.client;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import dev.bartels.jtechstorage.client.gui.GuiTheme;
import dev.bartels.jtechstorage.entity.SortCategory;
import dev.bartels.jtechstorage.menu.StorageTerminalMenu;
import dev.bartels.jtechstorage.net.TerminalContentsPayload;
import dev.bartels.jtechstorage.net.TerminalStatsPayload;
import dev.bartels.jtechstorage.net.TerminalStatsRequestPayload;
import dev.bartels.jtechstorage.network.NetworkStats;
import dev.bartels.jtechstorage.network.StorageNetwork;

/**
 * Lager-Statistik (aus dem Speicher-Terminal): Füllstand, Verlauf der letzten 14 Spieltage, häufigste Items,
 * Kategorien und die größten Veränderungen seit Tagesbeginn. Aktualisiert sich alle 5 Sekunden.
 */
public class TerminalStatsScreen extends dev.bartels.jtechstorage.client.gui.ThemedScreen {
	private static final int W = 300;
	private static final int H = 212;
	private static final int COL2 = 160;
	private static final int CHART_Y = 70;
	private static final int CHART_H = 46;
	private static final int TOP_Y = 136;
	private static final int CHANGE_Y = 168;
	private static final int CAT_ROWS = 7;

	private final Screen parent;
	private final StorageTerminalMenu menu;
	private @Nullable TerminalStatsPayload stats;
	private int refresh;
	private int left;
	private int top;

	public TerminalStatsScreen(Screen parent, StorageTerminalMenu menu) {
		super(Component.translatable("jtechstorage.stats.title"));
		this.parent = parent;
		this.menu = menu;
	}

	public int containerId() {
		return menu.containerId;
	}

	public void setStats(TerminalStatsPayload stats) {
		this.stats = stats;
	}

	@Override
	protected void init() {
		left = (width - W) / 2;
		top = (height - H) / 2;
		request();
	}

	private void request() {
		refresh = 100;
		ClientPlayNetworking.send(new TerminalStatsRequestPayload(menu.containerId));
	}

	@Override
	public void tick() {
		if (minecraft.player == null || minecraft.player.containerMenu != menu) {
			minecraft.setScreenAndShow(null);
			return;
		}

		if (--refresh <= 0) {
			request();
		}
	}

	@Override
	public void onClose() {
		minecraft.setScreenAndShow(parent);
	}

	// ------------------------------------------------------------------
	// Daten
	// ------------------------------------------------------------------

	private long totalItems() {
		long total = 0;

		for (StorageNetwork.Entry entry : menu.getClientEntries()) {
			total += entry.total();
		}

		return total;
	}

	private static String number(long value) {
		return NumberFormat.getIntegerInstance().format(value);
	}

	private static String signed(long value) {
		return (value > 0 ? "+" : value < 0 ? "−" : "±") + StorageTerminalScreen.compact(Math.abs(value));
	}

	// ------------------------------------------------------------------
	// Zeichnen
	// ------------------------------------------------------------------

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractBackground(graphics, mouseX, mouseY, delta);
		GuiTheme.window(graphics, left, top, W, H);
		GuiTheme.sunken(graphics, left + 8, top + CHART_Y - 2, COL2 - 16, CHART_H + 4);
		GuiTheme.sunken(graphics, left + COL2, top + 20, W - COL2 - 8, CAT_ROWS * 14 + 6);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractRenderState(graphics, mouseX, mouseY, delta);
		int x = left;
		int y = top;
		int text = GuiTheme.text();
		int subtle = GuiTheme.subtle();
		List<Component> tooltip = null;

		graphics.text(font, "✦", x + 8, y + 7, 0xFFE0B0FF, true);
		GuiTheme.label(graphics, font, title, x + 20, y + 7, W - 120, text);
		boolean closeHover = over(W - 22, 4, 14, 12, mouseX, mouseY);
		graphics.text(font, "×", x + W - 17, y + 6, closeHover ? GuiTheme.accentHover() : text, false);
		// Farmen (v0.27.0)
		boolean farmsHover = over(W - 96, 3, 70, 13, mouseX, mouseY);
		GuiTheme.labeled(graphics, font, x + W - 96, y + 3, 70, 13, Component.translatable("jtechstorage.farms.open"),
				farmsHover ? GuiTheme.accentHover() : GuiTheme.accent(), true);

		// --- Füllstand ---------------------------------------------------
		TerminalContentsPayload.NetworkInfo info = menu.getClientInfo();
		long items = totalItems();
		int used = info.usedSlots();
		int total = Math.max(1, info.totalSlots());
		int percent = used * 100 / total;
		GuiTheme.label(graphics, font, Component.translatable("jtechstorage.stats.slots", number(used), number(info.totalSlots()), percent),
				x + 8, y + 22, COL2 - 16, text);
		int barW = COL2 - 16;
		graphics.fill(x + 8, y + 33, x + 8 + barW, y + 39, 0xFF000000);
		int fill = Math.round((barW - 2) * Math.min(1.0f, used / (float) total));
		int barColor = percent >= 90 ? 0xFFE05A40 : percent >= 70 ? 0xFFE0B040 : 0xFF4CC46C;
		graphics.fill(x + 9, y + 34, x + 9 + fill, y + 38, barColor);
		GuiTheme.label(graphics, font, Component.translatable("jtechstorage.stats.summary", info.linked(), info.capacity(),
				number(menu.getClientEntries().size()), number(items)), x + 8, y + 44, COL2 - 16, subtle);

		// --- Heute ------------------------------------------------------
		long todayStart = stats != null ? stats.todayStart() : -1;
		Component today = todayStart < 0 ? Component.translatable("jtechstorage.stats.today_unknown")
				: Component.translatable("jtechstorage.stats.today", signed(items - todayStart));
		GuiTheme.label(graphics, font, today, x + 8, y + 56, COL2 - 16, text);

		// --- Verlauf ----------------------------------------------------
		tooltip = extractChart(graphics, x + 10, y + CHART_Y, COL2 - 20, CHART_H, items, used, total, mouseX, mouseY);

		// --- Kategorien -------------------------------------------------
		graphics.text(font, Component.translatable("jtechstorage.stats.categories"), x + COL2, y + 10, text, false);
		Map<SortCategory, Long> perCategory = new EnumMap<>(SortCategory.class);

		for (StorageNetwork.Entry entry : menu.getClientEntries()) {
			perCategory.merge(SortCategory.of(entry.prototype()), entry.total(), Long::sum);
		}

		List<Map.Entry<SortCategory, Long>> categories = new ArrayList<>(perCategory.entrySet());
		categories.sort(Map.Entry.<SortCategory, Long>comparingByValue().reversed());
		long maxCategory = categories.isEmpty() ? 1 : Math.max(1, categories.getFirst().getValue());
		int infoText = GuiTheme.onColor(GuiTheme.slot());
		int catW = W - COL2 - 8;

		for (int i = 0; i < Math.min(CAT_ROWS, categories.size()); i++) {
			SortCategory category = categories.get(i).getKey();
			long count = categories.get(i).getValue();
			int ry = y + 23 + i * 14;
			graphics.item(StorageTerminalScreen.CATEGORY_ICONS[category.ordinal()], x + COL2 + 3, ry - 2);
			int bx = x + COL2 + 22;
			int bw = catW - 26;
			int len = Math.max(1, Math.round(bw * (count / (float) maxCategory)));
			graphics.fill(bx, ry + 9, bx + len, ry + 11, GuiTheme.accent());
			String amount = StorageTerminalScreen.compact(count);
			GuiTheme.label(graphics, font, Component.translatable(category.translationKey()), bx, ry, bw - font.width(amount) - 4, infoText);
			graphics.text(font, amount, bx + bw - font.width(amount), ry, infoText, false);

			if (mouseX >= x + COL2 && mouseX < x + W - 8 && mouseY >= ry - 2 && mouseY < ry + 12) {
				tooltip = List.of(Component.translatable(category.translationKey()),
						Component.translatable("jtechstorage.stats.items", number(count)).withStyle(ChatFormatting.GRAY));
			}
		}

		if (categories.isEmpty()) {
			GuiTheme.label(graphics, font, Component.translatable("jtechstorage.stats.empty"), x + COL2 + 4, y + 24, catW - 8, infoText);
		}

		// --- Häufigste Items ---------------------------------------------
		graphics.text(font, Component.translatable("jtechstorage.stats.top"), x + 8, y + TOP_Y - 11, text, false);
		List<StorageNetwork.Entry> top = new ArrayList<>(menu.getClientEntries());
		top.sort(Comparator.comparingLong(StorageNetwork.Entry::total).reversed());
		ItemStack hovered = null;

		for (int i = 0; i < Math.min(14, top.size()); i++) {
			int ix = x + 8 + i * 20;
			graphics.item(top.get(i).prototype(), ix, y + TOP_Y);
			String count = StorageTerminalScreen.compact(top.get(i).total());
			graphics.text(font, count, ix + 17 - font.width(count), y + TOP_Y + 9, 0xFFFFFFFF, true);

			if (mouseX >= ix && mouseX < ix + 16 && mouseY >= y + TOP_Y && mouseY < y + TOP_Y + 16) {
				hovered = top.get(i).prototype();
				tooltip = List.of(hovered.getHoverName(), Component.translatable("jtechstorage.stats.items", number(top.get(i).total()))
						.withStyle(ChatFormatting.GRAY));
			}
		}

		// --- Veränderungen seit Tagesbeginn ------------------------------
		graphics.text(font, Component.translatable("jtechstorage.stats.gained"), x + 8, y + CHANGE_Y - 11, text, false);
		graphics.text(font, Component.translatable("jtechstorage.stats.lost"), x + COL2, y + CHANGE_Y - 11, text, false);
		List<NetworkStats.Change> changes = stats != null ? stats.changes() : List.of();
		int gained = 0;
		int lost = 0;

		for (NetworkStats.Change change : changes) {
			boolean plus = change.delta() > 0;
			int index = plus ? gained++ : lost++;
			int ix = x + (plus ? 8 : COL2) + index * 26;
			ItemStack icon = new ItemStack(change.item());
			graphics.item(icon, ix, y + CHANGE_Y);
			String value = signed(change.delta());
			graphics.text(font, value, ix + 8 - font.width(value) / 2, y + CHANGE_Y + 18, plus ? 0xFF4CC46C : 0xFFE05A40, false);

			if (mouseX >= ix && mouseX < ix + 16 && mouseY >= y + CHANGE_Y && mouseY < y + CHANGE_Y + 16) {
				tooltip = List.of(icon.getHoverName(), Component.translatable(plus ? "jtechstorage.stats.change_plus" : "jtechstorage.stats.change_minus",
						number(Math.abs(change.delta()))).withStyle(plus ? ChatFormatting.GREEN : ChatFormatting.RED));
			}
		}

		if (gained == 0) {
			GuiTheme.label(graphics, font, Component.translatable("jtechstorage.stats.none"), x + 8, y + CHANGE_Y + 4, COL2 - 16, subtle);
		}

		if (lost == 0) {
			GuiTheme.label(graphics, font, Component.translatable("jtechstorage.stats.none"), x + COL2, y + CHANGE_Y + 4, W - COL2 - 8, subtle);
		}

		if (tooltip != null) {
			graphics.setComponentTooltipForNextFrame(font, tooltip, mouseX, mouseY);
		}
	}

	/** Säulen: Items zu Beginn jedes Spieltags, ganz rechts „jetzt“. Gibt einen Tooltip zurück, falls die Maus darauf zeigt. */
	private @Nullable List<Component> extractChart(GuiGraphicsExtractor graphics, int x, int y, int w, int h, long itemsNow,
			int usedNow, int totalNow, int mouseX, int mouseY) {
		List<NetworkStats.Day> days = new ArrayList<>(stats != null ? stats.history() : List.of());
		long dayNow = days.isEmpty() ? 0 : days.getLast().day();
		days.add(new NetworkStats.Day(Long.MAX_VALUE, itemsNow, usedNow, totalNow));
		long max = 1;

		for (NetworkStats.Day day : days) {
			max = Math.max(max, day.items());
		}

		int slots = NetworkStats.DAYS + 1;
		int barW = Math.max(3, w / slots - 2);
		int start = x + w - days.size() * (barW + 2);
		List<Component> tooltip = null;

		for (int i = 0; i < days.size(); i++) {
			NetworkStats.Day day = days.get(i);
			boolean now = day.day() == Long.MAX_VALUE;
			int bx = start + i * (barW + 2);
			int bh = Math.max(1, Math.round((h - 2) * (day.items() / (float) max)));
			int color = now ? GuiTheme.accentHover() : GuiTheme.accent();
			graphics.fill(bx, y + h - bh, bx + barW, y + h, color);

			if (mouseX >= bx && mouseX < bx + barW + 2 && mouseY >= y && mouseY < y + h) {
				int percent = day.total() <= 0 ? 0 : day.used() * 100 / day.total();
				Component label = now ? Component.translatable("jtechstorage.stats.now")
						: Component.translatable("jtechstorage.stats.day", day.day() + 1, dayNow - day.day() == 0
						? Component.translatable("jtechstorage.stats.day_today") : Component.translatable("jtechstorage.stats.days_ago", dayNow - day.day()));
				tooltip = List.of(label, Component.translatable("jtechstorage.stats.items", number(day.items())).withStyle(ChatFormatting.GRAY),
						Component.translatable("jtechstorage.stats.fill", percent).withStyle(ChatFormatting.GRAY));
			}
		}

		if (days.size() <= 1) {
			GuiTheme.label(graphics, font, Component.translatable("jtechstorage.stats.history_soon"), x + 2, y + 2, w - 4, GuiTheme.onColor(GuiTheme.slot()));
		}

		return tooltip;
	}

	private boolean over(int x, int y, int w, int h, double mouseX, double mouseY) {
		return mouseX >= left + x && mouseX < left + x + w && mouseY >= top + y && mouseY < top + y + h;
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (over(W - 22, 4, 14, 12, event.x(), event.y())) {
			minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
			onClose();
			return true;
		}

		if (over(W - 96, 3, 70, 13, event.x(), event.y())) {
			minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
			minecraft.setScreenAndShow(new dev.bartels.jtechstorage.client.farm.FarmsScreen(this, menu));
			return true;
		}

		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
