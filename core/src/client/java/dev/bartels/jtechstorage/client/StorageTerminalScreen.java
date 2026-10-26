package dev.bartels.jtechstorage.client;

import dev.bartels.jtechstorage.ModContent;
import dev.bartels.jtechstorage.client.gui.GuiFx;
import dev.bartels.jtechstorage.net.TerminalViewPayload;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.inventory.Slot;
import org.jspecify.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import dev.bartels.jtechstorage.client.gui.GuiSettings;
import dev.bartels.jtechstorage.client.gui.GuiSettingsScreen;
import dev.bartels.jtechstorage.client.gui.GuiTheme;
import dev.bartels.jtechstorage.client.gui.ThemedContainerScreen;
import dev.bartels.jtechstorage.entity.SortCategory;
import dev.bartels.jtechstorage.menu.StorageTerminalMenu;
import dev.bartels.jtechstorage.net.TerminalActionPayload;
import dev.bartels.jtechstorage.net.TerminalContentsPayload;
import dev.bartels.jtechstorage.network.StorageNetwork;

/**
 * Terminal UI (Storage and Crafting Terminal): search field, scrollable 9×6 grid with exact counts,
 * sorting (count/name/mod, ascending/descending) and "enchanted only/named only" filters.
 */
public class StorageTerminalScreen extends ThemedContainerScreen<StorageTerminalMenu> {
	private static final int SETTINGS_BUTTON_X = 172;
	private static final Identifier SCROLLER = Identifier.withDefaultNamespace("container/creative_inventory/scroller");
	private static final Identifier SCROLLER_DISABLED = Identifier.withDefaultNamespace("container/creative_inventory/scroller_disabled");

	private static final int IMAGE_WIDTH = 195;

	private static final int COLUMNS = 9;
	private static final int GRID_X = 8;
	private static final int GRID_Y = 18;

	private static final int SCROLL_X = 175;
	private static final int SCROLL_Y = 18;
	private static final int SCROLLER_WIDTH = 12;
	private static final int SCROLLER_HEIGHT = 15;

	private static final int BUTTON_SIZE = 18;
	private static final int SORT_BUTTON_X = 7;
	private static final int DIRECTION_BUTTON_X = 27;
	private static final int FILTER_BUTTON_X = 47;
	private static final int INFO_X = 70;
	private static final int CLEAR_BUTTON_X = 9;
	private static final int CLEAR_BUTTON_SIZE = 12;
	private static final String[] ACCESS_KEYS = {"jtechstorage.access.private", "jtechstorage.access.friends", "jtechstorage.access.public"};
	private static final String[] ROLE_KEYS = {"jtechstorage.role.guest", "jtechstorage.role.member", "jtechstorage.role.builder", "jtechstorage.role.manager", "jtechstorage.role.owner"};

	private static final int SEARCH_MIN_X = 82;
	private static final int SEARCH_RIGHT = 170;
	private static final int SEARCH_Y = 6;

	// settings persist for the game session
	private static SortMode sortMode = SortMode.AMOUNT;
	private static boolean descending = true;
	private static boolean onlySpecial = false;
	private static String lastSearch = "";
	/** Category tab to the left of the terminal (null = all). */
	private static @Nullable SortCategory category = null;
	private static final int TAB_SIZE = 20;
	private static final int TAB_GAP = 1;
	private static final int TAB_ROWS = 8;
	static final ItemStack[] CATEGORY_ICONS = {
			new ItemStack(Items.BREAD), new ItemStack(Items.ENCHANTED_BOOK), new ItemStack(Items.IRON_CHESTPLATE),
			new ItemStack(Items.IRON_PICKAXE), new ItemStack(Items.REDSTONE), new ItemStack(Items.RAW_IRON),
			new ItemStack(Items.IRON_INGOT), new ItemStack(Items.FLOWER_POT), new ItemStack(Items.WHEAT),
			new ItemStack(Items.OAK_SAPLING), new ItemStack(Items.OAK_LOG), new ItemStack(Items.BONE),
			new ItemStack(Items.DIRT), new ItemStack(Items.COBBLESTONE), new ItemStack(Items.CHEST)};
	private final EnumMap<SortCategory, Integer> categoryCounts =
			new EnumMap<>(SortCategory.class);

	private final StorageTerminalMenu.Layout layout;
	private final int rows;
	private final int buttonY;
	private final int scrollHeight;

	private EditBox searchBox;
	private int searchX = SEARCH_MIN_X;
	private List<StorageNetwork.@Nullable Entry> filtered = List.of();
	/** Order currently frozen (mouse over the grid); re-sorted when the mouse leaves. */
	private boolean orderFrozen;
	private int seenVersion = -1;
	private int scrollRow;
	private boolean draggingScroller;
	/** Last mouse position (for keys pressed over the grid). */
	private double lastMouseX = -1;
	/** Templates of the visible cells last reported to the server. */
	private List<ItemStack> sentView = List.of();
	private double lastMouseY = -1;

	public StorageTerminalScreen(StorageTerminalMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title, IMAGE_WIDTH, menu.getLayout().imageHeight());
		layout = menu.getLayout();
		rows = layout.gridRows();
		buttonY = layout.toolbarY();
		scrollHeight = rows * 18;
		inventoryLabelY = layout.inventoryY() - 11;
	}

	@Override
	protected void init() {
		super.init();

		// search field starts after the title so nothing overlaps
		searchX = Math.max(SEARCH_MIN_X, titleLabelX + font.width(title) + 8);
		int iconPad = GuiTheme.classic() ? 0 : 10; // room for the magnifier (new themes)
		searchBox = new EditBox(font, leftPos + searchX + 2 + iconPad, topPos + SEARCH_Y, SEARCH_RIGHT - searchX - 4 - iconPad, 9,
				Component.translatable("jtechstorage.terminal.search"));
		searchBox.setMaxLength(64);
		searchBox.setBordered(false);
		searchBox.setTextColor(GuiTheme.fieldText());
		searchBox.setHint(Component.translatable("jtechstorage.terminal.search.hint").withStyle(ChatFormatting.GRAY));
		searchBox.setValue(lastSearch);
		searchBox.setResponder(value -> {
			lastSearch = value;
			scrollRow = 0;
			refilter();
		});
		addRenderableWidget(searchBox);
		// search field active right away: just start typing (a click into the grid releases the keyboard)
		setInitialFocus(searchBox);

		refilter();
	}

	// ------------------------------------------------------------------
	// Filtering & sorting
	// ------------------------------------------------------------------

	private void refilter() {
		refilter(false);
	}

	/**
	 * Rebuild the list. {@code keepOrder}: only update counts, keep the order (while the mouse is over the
	 * grid) – otherwise items jump away from under the cursor when taken out.
	 */
	private void refilter(boolean keepOrder) {
		seenVersion = menu.getClientVersion();
		String phrase = lastSearch.toLowerCase(Locale.ROOT).trim();
		String[] terms = phrase.split("\\s+");
		List<StorageNetwork.Entry> result = new ArrayList<>();
		categoryCounts.clear();

		for (StorageNetwork.Entry entry : menu.getClientEntries()) {
			SortCategory entryCategory = SortCategory.of(entry.prototype());
			categoryCounts.merge(entryCategory, 1, Integer::sum);

			if (category != null && entryCategory != category) {
				continue;
			}

			if (onlySpecial && !isSpecial(entry.prototype())) {
				continue;
			}

			// "schärfe v" (German enchantment name) should also match as a whole phrase
			if (matchesAll(entry.prototype(), terms)
					|| (phrase.contains(" ") && enchantmentNames(entry.prototype()).stream().anyMatch(n -> n.contains(phrase)))) {
				result.add(entry);
			}
		}

		Comparator<StorageNetwork.Entry> comparator = sortMode.comparator;
		result.sort(descending ? comparator.reversed() : comparator);
		filtered = keepOrder ? stableOrder(result) : result;
		orderFrozen = keepOrder;
		scrollRow = Mth.clamp(scrollRow, 0, maxScroll());
	}

	/**
	 * Keeps the previous order: known items stay in place (used-up ones leave a gap),
	 * new ones go to the end.
	 */
	private List<StorageNetwork.@Nullable Entry> stableOrder(List<StorageNetwork.Entry> sorted) {
		Map<Integer, List<Integer>> byHash = new HashMap<>();

		for (int i = 0; i < sorted.size(); i++) {
			byHash.computeIfAbsent(ItemStack.hashItemAndComponents(sorted.get(i).prototype()), k -> new ArrayList<>()).add(i);
		}

		boolean[] used = new boolean[sorted.size()];
		List<StorageNetwork.@Nullable Entry> stable = new ArrayList<>();

		for (StorageNetwork.@Nullable Entry old : filtered) {
			StorageNetwork.Entry match = null;

			if (old != null) {
				for (int i : byHash.getOrDefault(ItemStack.hashItemAndComponents(old.prototype()), List.of())) {
					if (!used[i] && ItemStack.isSameItemSameComponents(sorted.get(i).prototype(), old.prototype())) {
						used[i] = true;
						match = sorted.get(i);
						break;
					}
				}
			}

			stable.add(match);
		}

		for (int i = 0; i < sorted.size(); i++) {
			if (!used[i]) {
				stable.add(sorted.get(i));
			}
		}

		while (!stable.isEmpty() && stable.get(stable.size() - 1) == null) {
			stable.remove(stable.size() - 1);
		}

		return stable;
	}

	/**
	 * Hand the visible cells to the menu's vanilla slots (v0.29.4): client preview immediately, server via packet when
	 * something changed. This way all clicks and keys in the grid behave exactly like in a chest.
	 */
	private void updateView() {
		int visible = rows * COLUMNS;
		List<ItemStack> prototypes = new ArrayList<>(visible);
		List<ItemStack> displays = new ArrayList<>(visible);

		for (int i = 0; i < visible; i++) {
			StorageNetwork.@Nullable Entry entry = entryAt(scrollRow * COLUMNS + i);

			if (entry == null) {
				prototypes.add(ItemStack.EMPTY);
				displays.add(ItemStack.EMPTY);
			} else {
				prototypes.add(entry.prototype().copyWithCount(1));
				displays.add(entry.prototype().copyWithCount((int) Math.min(entry.total(), entry.prototype().getMaxStackSize())));
			}
		}

		if (!sameView(prototypes, sentView)) {
			sentView = prototypes;
			ClientPlayNetworking.send(new TerminalViewPayload(menu.containerId, prototypes));
		}

		menu.setClientView(displays);
	}

	private static boolean sameView(List<ItemStack> a, List<ItemStack> b) {
		if (a.size() != b.size()) {
			return false;
		}

		for (int i = 0; i < a.size(); i++) {
			if (!ItemStack.isSameItemSameComponents(a.get(i), b.get(i)) || a.get(i).isEmpty() != b.get(i).isEmpty()) {
				return false;
			}
		}

		return true;
	}

	@Override
	protected void extractSlot(GuiGraphicsExtractor graphics, Slot slot, int mouseX, int mouseY) {
		if (!menu.isViewSlot(slot)) {
			super.extractSlot(graphics, slot, mouseX, mouseY); // grid cells are drawn by extractGrid (with total count)
		}
	}

	private boolean mouseOverGrid() {
		return gridIndexAt(lastMouseX, lastMouseY) >= 0;
	}

	private static boolean isSpecial(ItemStack stack) {
		return stack.isEnchanted()
				|| stack.has(DataComponents.STORED_ENCHANTMENTS)
				|| stack.has(DataComponents.CUSTOM_NAME);
	}

	/**
	 * All search terms must match. {@code @mod} = mod ID, {@code #tag} = item tag,
	 * {@code $verzauberung} = enchantments only, otherwise name or enchantment.
	 */
	private static boolean matchesAll(ItemStack stack, String[] terms) {
		for (String term : terms) {
			if (term.isEmpty()) {
				continue;
			}

			boolean match;

			if (term.startsWith("@")) {
				match = modId(stack).contains(term.substring(1));
			} else if (term.startsWith("#")) {
				String tag = term.substring(1);
				match = stack.typeHolder().tags().anyMatch(key -> key.location().toString().contains(tag));
			} else if (term.startsWith("$")) {
				String enchantment = term.substring(1);
				match = enchantmentNames(stack).stream().anyMatch(name -> name.contains(enchantment));
			} else {
				match = stack.getHoverName().getString().toLowerCase(Locale.ROOT).contains(term)
						|| enchantmentNames(stack).stream().anyMatch(name -> name.contains(term));
			}

			if (!match) {
				return false;
			}
		}

		return true;
	}

	/** Names of all enchantments (on the item and in books), e.g. "reparatur", "schärfe v". */
	private static List<String> enchantmentNames(ItemStack stack) {
		List<String> names = new ArrayList<>();
		addEnchantmentNames(stack.get(DataComponents.ENCHANTMENTS), names);
		addEnchantmentNames(stack.get(DataComponents.STORED_ENCHANTMENTS), names);
		return names;
	}

	private static void addEnchantmentNames(@Nullable ItemEnchantments enchantments, List<String> names) {
		if (enchantments == null || enchantments.isEmpty()) {
			return;
		}

		for (Holder<Enchantment> enchantment : enchantments.keySet()) {
			names.add(Enchantment.getFullname(enchantment, enchantments.getLevel(enchantment)).getString().toLowerCase(Locale.ROOT));
		}
	}

	private static String modId(ItemStack stack) {
		return BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace();
	}

	private static String name(StorageNetwork.Entry entry) {
		return entry.prototype().getHoverName().getString().toLowerCase(Locale.ROOT);
	}

	private int totalRows() {
		return (filtered.size() + COLUMNS - 1) / COLUMNS;
	}

	private int maxScroll() {
		return Math.max(0, totalRows() - rows);
	}

	// ------------------------------------------------------------------
	// Rendering
	// ------------------------------------------------------------------

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractBackground(graphics, mouseX, mouseY, delta);
		int x = leftPos;
		int y = topPos;
		GuiTheme.window(graphics, x, y, imageWidth, imageHeight);

		// network grid + scrollbar
		for (int row = 0; row < rows; row++) {
			for (int column = 0; column < COLUMNS; column++) {
				GuiTheme.slot(graphics, x + GRID_X - 1 + column * 18, y + GRID_Y - 1 + row * 18);
			}
		}

		GuiTheme.sunken(graphics, x + SCROLL_X - 1, y + SCROLL_Y - 1, SCROLLER_WIDTH + 2, scrollHeight + 2);

		// crafting grid, arrow, result
		if (layout.hasCrafting()) {
			int top = layout.craftingY();

			for (int row = 0; row < 3; row++) {
				for (int column = 0; column < 3; column++) {
					GuiTheme.slot(graphics, x + StorageTerminalMenu.CRAFT_GRID_X - 1 + column * 18, y + top - 1 + row * 18);
				}
			}

			int ax = x + 92;
			int ay = y + top + 22;
			graphics.fill(ax, ay, ax + 14, ay + 3, GuiTheme.subtle());
			graphics.fill(ax + 14, ay - 2, ax + 16, ay + 5, GuiTheme.subtle());
			graphics.fill(ax + 16, ay - 1, ax + 18, ay + 4, GuiTheme.subtle());
			graphics.fill(ax + 18, ay, ax + 19, ay + 3, GuiTheme.subtle());
			GuiTheme.sunken(graphics, x + StorageTerminalMenu.CRAFT_RESULT_X - 5, y + top + 13, 26, 26);
		}

		// upgrade slots with accent frame
		for (int i = 0; i < 2; i++) {
			int sx = x + StorageTerminalMenu.UPGRADE_SLOT_X - 1;
			int sy = y + layout.inventoryY() - 1 + i * 18;
			if (GuiTheme.classic()) {
				graphics.fill(sx - 1, sy - 1, sx + 19, sy + 19, GuiTheme.accent());
			} else {
				GuiTheme.roundOutline(graphics, sx - 1, sy - 1, 20, 20, 2, GuiTheme.accent());
			}

			GuiTheme.slot(graphics, sx, sy);
		}

		// player inventory
		for (int row = 0; row < 4; row++) {
			for (int column = 0; column < 9; column++) {
				int sy = row < 3 ? layout.inventoryY() + row * 18 : layout.inventoryY() + 58;
				GuiTheme.slot(graphics, x + 7 + column * 18, y + sy - 1);
			}
		}

		// search field background
		int sx = leftPos + searchX;
		int sy = topPos + SEARCH_Y - 2;
		GuiTheme.field(graphics, sx, sy, leftPos + SEARCH_RIGHT - sx, 12, searchBox.isFocused());

		if (!GuiTheme.classic()) {
			GuiTheme.searchIcon(graphics, sx + 3, sy + 3);
		}
	}

	@Override
	public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		if (seenVersion != menu.getClientVersion()) {
			refilter(mouseOverGrid() && !filtered.isEmpty());
			GuiTheme.setTeamColor(menu.getClientAccess().color());
		}

		super.extractContents(graphics, mouseX, mouseY, delta);
		lastMouseX = mouseX;
		lastMouseY = mouseY;
		updateView();

		if (orderFrozen && !mouseOverGrid()) {
			refilter(); // mouse left the grid: re-sort now
		}

		extractGrid(graphics, mouseX, mouseY);
		extractButtonAt(graphics, SETTINGS_BUTTON_X, layout.inventoryY() + 58, mouseX, mouseY, false);
		// small bar chart as an icon
		int chartX = leftPos + SETTINGS_BUTTON_X + 4;
		int chartBottom = topPos + layout.inventoryY() + 58 + 14;
		graphics.fill(chartX, chartBottom - 4, chartX + 3, chartBottom, GuiTheme.text());
		graphics.fill(chartX + 4, chartBottom - 9, chartX + 7, chartBottom, GuiTheme.text());
		graphics.fill(chartX + 8, chartBottom - 6, chartX + 11, chartBottom, GuiTheme.text());
		extractButtonAt(graphics, SETTINGS_BUTTON_X, layout.inventoryY() + 38, mouseX, mouseY, false);
		graphics.text(font, "⚙", leftPos + SETTINGS_BUTTON_X + 9 - font.width("⚙") / 2, topPos + layout.inventoryY() + 43, GuiTheme.text(), false);
		extractScrollbar(graphics);
		extractToolbar(graphics, mouseX, mouseY);
		extractCategoryTabs(graphics, mouseX, mouseY);
	}

	// ------------------------------------------------------------------
	// Category tabs (two columns to the left of the terminal)
	// ------------------------------------------------------------------

	/** Tab 0 = all, 1..15 = categories in sprite order. */
	private int tabX(int index) {
		int column = index / TAB_ROWS;
		return leftPos - 2 - (2 - column) * (TAB_SIZE + TAB_GAP);
	}

	private int tabY(int index) {
		return topPos + 4 + (index % TAB_ROWS) * (TAB_SIZE + TAB_GAP);
	}

	private static @Nullable SortCategory tabCategory(int index) {
		return index == 0 ? null : SortCategory.values()[index - 1];
	}

	private int tabAt(double mouseX, double mouseY) {
		for (int index = 0; index <= SortCategory.values().length; index++) {
			int x = tabX(index);
			int y = tabY(index);

			if (mouseX >= x && mouseX < x + TAB_SIZE && mouseY >= y && mouseY < y + TAB_SIZE) {
				return index;
			}
		}

		return -1;
	}

	/** For JEI: keep the tab area free. */
	public List<Rect2i> getExtraAreas() {
		int x = tabX(0);
		return List.of(new Rect2i(x, topPos + 4, leftPos - x, TAB_ROWS * (TAB_SIZE + TAB_GAP)));
	}

	private void extractCategoryTabs(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		int hovered = tabAt(mouseX, mouseY);

		for (int index = 0; index <= SortCategory.values().length; index++) {
			SortCategory tab = tabCategory(index);
			boolean selected = tab == category;
			boolean empty = tab != null && categoryCounts.getOrDefault(tab, 0) == 0;
			int x = tabX(index);
			int y = tabY(index);
			if (GuiTheme.classic()) {
				GuiTheme.face(graphics, x, y, TAB_SIZE, TAB_SIZE, selected ? GuiTheme.accent()
						: index == hovered ? GuiTheme.accentHover() : empty ? GuiTheme.DISABLED_FACE : GuiTheme.accentDark());
			} else if (empty && !selected) {
				GuiTheme.face(graphics, x, y, TAB_SIZE, TAB_SIZE, GuiTheme.alpha(GuiTheme.DISABLED_FACE, 0xB0));
			} else {
				GuiTheme.toggle(graphics, x, y, TAB_SIZE, TAB_SIZE, selected, index == hovered);
			}
			ItemStack icon = tab == null ? new ItemStack(ModContent.STORAGE_TERMINAL_ITEM) : CATEGORY_ICONS[tab.ordinal()];
			graphics.item(icon, x + 2, y + 2);
		}
	}

	private void extractGrid(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		int hovered = gridIndexAt(mouseX, mouseY);

		for (int row = 0; row < rows; row++) {
			for (int column = 0; column < COLUMNS; column++) {
				int index = (scrollRow + row) * COLUMNS + column;
				int x = leftPos + GRID_X + column * 18;
				int y = topPos + GRID_Y + row * 18;

				if (index == hovered && GuiSettings.get().hoverGlow) {
					GuiTheme.slotGlow(graphics, x, y);
				}

				// number animation: items appear one after another, counts count up
				float progress = GuiFx.countProgress(this, (row * COLUMNS + column) * 12);

				StorageNetwork.@Nullable Entry entry = index < filtered.size() ? filtered.get(index) : null;

				if (entry != null && progress > 0) {
					ItemStack stack = entry.prototype();
					graphics.item(stack, x, y);
					graphics.itemDecorations(font, stack, x, y, "");
					extractCount(graphics, progress >= 1 ? entry.total() : Math.max(1, Math.round(entry.total() * (double) progress)), x, y);
				}

			}
		}
	}

	/** Exact count, compactly formatted and scaled down if needed (as is usual in storage mods). */
	private void extractCount(GuiGraphicsExtractor graphics, long total, int x, int y) {
		if (total <= 1) {
			return;
		}

		String text = compact(total);
		GuiSettings.CountSize size = GuiSettings.get().countSize;
		float scale = switch (size) {
			case SMALL -> text.length() <= 2 ? 0.75f : 0.5f;
			case NORMAL -> text.length() <= 2 ? 1.0f : 0.75f;
			case LARGE -> text.length() <= 3 ? 1.0f : 0.85f;
		};

		boolean outline = GuiTheme.outlineCounts();

		if (scale == 1.0f) {
			if (outline) {
				GuiTheme.outlinedNumber(graphics, font, text, x + 17 - font.width(text), y + 9);
			} else {
				graphics.text(font, text, x + 17 - font.width(text), y + 9, 0xFFFFFFFF, true);
			}

			return;
		}

		graphics.pose().pushMatrix();
		graphics.pose().translate(x + 16, y + 16);
		graphics.pose().scale(scale, scale);

		if (outline) {
			GuiTheme.outlinedNumber(graphics, font, text, -font.width(text), -8);
		} else {
			graphics.text(font, text, -font.width(text), -8, 0xFFFFFFFF, true);
		}

		graphics.pose().popMatrix();
	}

	static String compact(long value) {
		if (value < 1_000) {
			return Long.toString(value);
		}

		if (value < 1_000_000) {
			return trim(value / 1_000.0) + "k";
		}

		if (value < 1_000_000_000) {
			return trim(value / 1_000_000.0) + "M";
		}

		return trim(value / 1_000_000_000.0) + "G";
	}

	private static String trim(double value) {
		return value >= 100 ? Long.toString((long) value) : String.format(Locale.ROOT, "%.1f", Math.floor(value * 10) / 10).replace(".0", "");
	}

	private void extractScrollbar(GuiGraphicsExtractor graphics) {
		int max = maxScroll();
		int travel = scrollHeight - SCROLLER_HEIGHT;
		int offset = max == 0 ? 0 : Math.round(travel * (scrollRow / (float) max));
		if (GuiTheme.classic()) {
			graphics.blitSprite(RenderPipelines.GUI_TEXTURED, max == 0 ? SCROLLER_DISABLED : SCROLLER,
					leftPos + SCROLL_X, topPos + SCROLL_Y + offset, SCROLLER_WIDTH, SCROLLER_HEIGHT);
			return;
		}

		GuiTheme.face(graphics, leftPos + SCROLL_X + 1, topPos + SCROLL_Y + offset, SCROLLER_WIDTH - 2, SCROLLER_HEIGHT,
				max == 0 ? GuiTheme.DISABLED_FACE : draggingScroller ? GuiTheme.accentHover() : GuiTheme.accent());
	}

	private void extractToolbar(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		extractButton(graphics, SORT_BUTTON_X, mouseX, mouseY, false);
		graphics.item(sortMode.icon, leftPos + SORT_BUTTON_X + 1, topPos + buttonY + 1);

		extractButton(graphics, DIRECTION_BUTTON_X, mouseX, mouseY, false);
		String arrow = descending ? "▼" : "▲";
		graphics.text(font, arrow, leftPos + DIRECTION_BUTTON_X + 9 - font.width(arrow) / 2, topPos + buttonY + 5, GuiTheme.text(), false);

		extractButton(graphics, FILTER_BUTTON_X, mouseX, mouseY, onlySpecial);
		graphics.item(new ItemStack(Items.ENCHANTED_BOOK), leftPos + FILTER_BUTTON_X + 1, topPos + buttonY + 1);

		TerminalContentsPayload.NetworkInfo info = menu.getClientInfo();
		Component line = Component.translatable("jtechstorage.terminal.info.short", info.linked(), info.capacity(), info.range());
		boolean warning = info.unavailable() > 0 || info.overCapacity() > 0;
		int infoWidth = imageWidth - 8 - INFO_X - (info.resonancePercent() > 0 ? 10 : 0);
		String shown = GuiTheme.fit(font, line.getString(), infoWidth);
		graphics.text(font, shown, leftPos + INFO_X, topPos + buttonY + 5, warning ? GuiTheme.bad() : GuiTheme.text(), false);

		if (info.resonancePercent() > 0) {
			// ⚡ after the info line: network is charged with resonance
			graphics.text(font, "⚡", leftPos + INFO_X + font.width(shown) + 3, topPos + buttonY + 5, GuiTheme.warn(), false);
		}

		if (layout.hasCrafting()) {
			int x = leftPos + CLEAR_BUTTON_X;
			int y = topPos + layout.craftingY() + 21;
			boolean hovered = isOver(CLEAR_BUTTON_X, layout.craftingY() + 21, CLEAR_BUTTON_SIZE, CLEAR_BUTTON_SIZE, mouseX, mouseY);
			if (GuiTheme.classic()) {
				graphics.fill(x, y, x + CLEAR_BUTTON_SIZE, y + CLEAR_BUTTON_SIZE, 0xFF000000);
				graphics.fill(x + 1, y + 1, x + CLEAR_BUTTON_SIZE - 1, y + CLEAR_BUTTON_SIZE - 1, hovered ? GuiTheme.mix(GuiTheme.face(), 0xFFFFFFFF, 0.15f) : GuiTheme.face());
			} else {
				GuiTheme.toggle(graphics, x, y, CLEAR_BUTTON_SIZE, CLEAR_BUTTON_SIZE, false, hovered);
			}
			graphics.text(font, "×", x + 6 - font.width("×") / 2, y + 2, GuiTheme.text(), false);
		}
	}

	private void extractButton(GuiGraphicsExtractor graphics, int buttonX, int mouseX, int mouseY, boolean active) {
		extractButtonAt(graphics, buttonX, buttonY, mouseX, mouseY, active);
	}

	private void extractButtonAt(GuiGraphicsExtractor graphics, int buttonX, int buttonTop, int mouseX, int mouseY, boolean active) {
		int x = leftPos + buttonX;
		int y = topPos + buttonTop;
		boolean hovered = isOver(buttonX, buttonTop, BUTTON_SIZE, BUTTON_SIZE, mouseX, mouseY);

		if (GuiTheme.classic()) {
			int light = active ? GuiTheme.accentHover() : GuiTheme.highlight();
			int dark = active ? GuiTheme.accentDark() : GuiTheme.shadow();
			int face = active ? GuiTheme.accent() : hovered ? GuiTheme.mix(GuiTheme.face(), 0xFFFFFFFF, 0.15f) : GuiTheme.face();

			graphics.fill(x, y, x + BUTTON_SIZE, y + BUTTON_SIZE, 0xFF000000);
			graphics.fill(x + 1, y + 1, x + BUTTON_SIZE - 1, y + BUTTON_SIZE - 1, dark);
			graphics.fill(x + 1, y + 1, x + BUTTON_SIZE - 2, y + BUTTON_SIZE - 2, light);
			graphics.fill(x + 2, y + 2, x + BUTTON_SIZE - 2, y + BUTTON_SIZE - 2, face);
			return;
		}

		GuiTheme.toggle(graphics, x, y, BUTTON_SIZE, BUTTON_SIZE, active, hovered);
	}

	@Override
	protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		if (hoveredSlot == null || !menu.isViewSlot(hoveredSlot)) {
			super.extractTooltip(graphics, mouseX, mouseY); // grid cells: custom tooltip with total count
		}

		if (!menu.getCarried().isEmpty()) {
			return;
		}

		int tab = tabAt(mouseX, mouseY);

		if (tab >= 0) {
			SortCategory tabCategory = tabCategory(tab);
			Component name = tabCategory == null ? Component.translatable("jtechstorage.terminal.category.all")
					: Component.translatable(tabCategory.translationKey());
			int count = tabCategory == null ? menu.getClientEntries().size() : categoryCounts.getOrDefault(tabCategory, 0);
			graphics.setComponentTooltipForNextFrame(font, List.of(name,
					Component.translatable("jtechstorage.terminal.category.count", count).withStyle(ChatFormatting.GRAY)), mouseX, mouseY);
			return;
		}

		StorageNetwork.Entry entry = entryAt(gridIndexAt(mouseX, mouseY));

		if (entry != null) {
			List<Component> lines = new ArrayList<>(Screen.getTooltipFromItem(minecraft, entry.prototype()));
			lines.add(Component.translatable("jtechstorage.terminal.amount",
					String.format(Locale.ROOT, "%,d", entry.total()).replace(',', '.')).withStyle(ChatFormatting.GOLD));
			graphics.setTooltipForNextFrame(font, lines, entry.prototype().getTooltipImage(), mouseX, mouseY);
			return;
		}

		if (isOverButton(SORT_BUTTON_X, mouseX, mouseY)) {
			graphics.setTooltipForNextFrame(font, Component.translatable("jtechstorage.terminal.sort",
					Component.translatable(sortMode.translationKey)), mouseX, mouseY);
		} else if (isOverButton(DIRECTION_BUTTON_X, mouseX, mouseY)) {
			graphics.setTooltipForNextFrame(font, Component.translatable(descending
					? "jtechstorage.terminal.sort.descending" : "jtechstorage.terminal.sort.ascending"), mouseX, mouseY);
		} else if (isOverButton(FILTER_BUTTON_X, mouseX, mouseY)) {
			graphics.setTooltipForNextFrame(font, Component.translatable(onlySpecial
					? "jtechstorage.terminal.filter.special.on" : "jtechstorage.terminal.filter.special.off"), mouseX, mouseY);
		} else if (isOver(INFO_X, buttonY, 100, BUTTON_SIZE, mouseX, mouseY)) {
			TerminalContentsPayload.NetworkInfo info = menu.getClientInfo();
			List<Component> lines = new ArrayList<>();
			lines.add(Component.translatable("jtechstorage.terminal.info"));
			TerminalContentsPayload.AccessInfo access = menu.getClientAccess();
			if (!access.networkName().isEmpty()) {
				lines.add(Component.translatable("jtechstorage.terminal.info.network_name", access.networkName())
						.withStyle(style -> style.withColor(access.color())));
			}

			lines.add(Component.translatable("jtechstorage.terminal.info.owner", access.ownerName()).withStyle(ChatFormatting.GRAY));
			lines.add(Component.translatable("jtechstorage.terminal.info.your_role",
					Component.translatable(ROLE_KEYS[Mth.clamp(access.role(), 0, ROLE_KEYS.length - 1)])).withStyle(ChatFormatting.GRAY));
			lines.add(Component.translatable("jtechstorage.friends.access",
					Component.translatable(ACCESS_KEYS[Math.floorMod(access.mode(), ACCESS_KEYS.length)])).withStyle(ChatFormatting.GRAY));

			if (!access.members().isEmpty()) {
				lines.add(Component.translatable("jtechstorage.terminal.info.members", String.join(", ", access.members())).withStyle(ChatFormatting.GRAY));
			}

			lines.add(Component.translatable("jtechstorage.terminal.info.containers", info.linked(), info.capacity()).withStyle(ChatFormatting.GRAY));
			lines.add(Component.translatable("jtechstorage.terminal.info.slots", info.usedSlots(), info.totalSlots()).withStyle(ChatFormatting.GRAY));
			lines.add(Component.translatable("jtechstorage.terminal.info.types", menu.getClientEntries().size()).withStyle(ChatFormatting.GRAY));
			lines.add(Component.translatable("jtechstorage.terminal.info.range", info.range()).withStyle(ChatFormatting.GRAY));

			if (info.sortRules() > 0) {
				lines.add(Component.translatable("jtechstorage.terminal.info.sort_rules", info.sortRules()).withStyle(ChatFormatting.GRAY));
			}

			if (info.resonancePercent() > 0) {
				lines.add(Component.translatable("jtechstorage.terminal.info.resonance", info.resonancePercent()).withStyle(ChatFormatting.GOLD));
				lines.add(Component.translatable("jtechstorage.terminal.info.resonance_bonus").withStyle(ChatFormatting.YELLOW));
			} else {
				lines.add(Component.translatable("jtechstorage.terminal.info.resonance_hint").withStyle(ChatFormatting.GRAY));
			}

			if (info.unavailable() > 0) {
				lines.add(Component.translatable("jtechstorage.terminal.info.unavailable", info.unavailable()).withStyle(ChatFormatting.RED));
			}

			if (info.overCapacity() > 0) {
				lines.add(Component.translatable("jtechstorage.terminal.info.over_capacity", info.overCapacity()).withStyle(ChatFormatting.RED));
			}

			lines.add(Component.translatable("jtechstorage.terminal.info.key_hint").withStyle(ChatFormatting.DARK_PURPLE));
			lines.add(Component.translatable("jtechstorage.terminal.search.help").withStyle(ChatFormatting.GRAY));
			graphics.setComponentTooltipForNextFrame(font, lines, mouseX, mouseY);
		} else if (layout.hasCrafting()
				&& isOver(CLEAR_BUTTON_X, layout.craftingY() + 21, CLEAR_BUTTON_SIZE, CLEAR_BUTTON_SIZE, mouseX, mouseY)) {
			graphics.setTooltipForNextFrame(font, Component.translatable("jtechstorage.terminal.clear_grid"), mouseX, mouseY);
		} else if (isOver(SETTINGS_BUTTON_X, layout.inventoryY() + 38, BUTTON_SIZE, BUTTON_SIZE, mouseX, mouseY)) {
			graphics.setTooltipForNextFrame(font, Component.translatable("jtechstorage.gui.settings"), mouseX, mouseY);
		} else if (isOver(SETTINGS_BUTTON_X, layout.inventoryY() + 58, BUTTON_SIZE, BUTTON_SIZE, mouseX, mouseY)) {
			graphics.setTooltipForNextFrame(font, Component.translatable("jtechstorage.stats.open"), mouseX, mouseY);
		} else if (hoveredSlot != null && !hoveredSlot.hasItem() && hoveredSlot.index == StorageTerminalMenu.RANGE_SLOT) {
			graphics.setTooltipForNextFrame(font, Component.translatable("jtechstorage.terminal.upgrade_slot.range"), mouseX, mouseY);
		} else if (hoveredSlot != null && !hoveredSlot.hasItem() && hoveredSlot.index == StorageTerminalMenu.CAPACITY_SLOT) {
			graphics.setTooltipForNextFrame(font, Component.translatable("jtechstorage.terminal.upgrade_slot.capacity"), mouseX, mouseY);
		}
	}

	// ------------------------------------------------------------------
	// Input
	// ------------------------------------------------------------------

	private boolean isOver(int x, int y, int width, int height, double mouseX, double mouseY) {
		return mouseX >= leftPos + x && mouseX < leftPos + x + width && mouseY >= topPos + y && mouseY < topPos + y + height;
	}

	private boolean isOverButton(int buttonX, double mouseX, double mouseY) {
		return isOver(buttonX, buttonY, BUTTON_SIZE, BUTTON_SIZE, mouseX, mouseY);
	}

	private int gridIndexAt(double mouseX, double mouseY) {
		if (!isOver(GRID_X, GRID_Y, COLUMNS * 18, rows * 18, mouseX, mouseY)) {
			return -1;
		}

		int column = (int) (mouseX - leftPos - GRID_X) / 18;
		int row = (int) (mouseY - topPos - GRID_Y) / 18;
		return (scrollRow + row) * COLUMNS + column;
	}

	/** Item in the network grid under the mouse (for JEI: R/U keys, bookmarks). */
	public record HoveredIngredient(ItemStack stack, int x, int y) {
	}

	public @Nullable HoveredIngredient getIngredientUnderMouse(double mouseX, double mouseY) {
		int index = gridIndexAt(mouseX, mouseY);
		StorageNetwork.Entry entry = entryAt(index);

		if (entry == null) {
			return null;
		}

		int visible = index - scrollRow * COLUMNS;
		int x = leftPos + GRID_X + (visible % COLUMNS) * 18;
		int y = topPos + GRID_Y + (visible / COLUMNS) * 18;
		return new HoveredIngredient(entry.prototype(), x, y);
	}

	private StorageNetwork.@Nullable Entry entryAt(int index) {
		return index >= 0 && index < filtered.size() ? filtered.get(index) : null;
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		double mouseX = event.x();
		double mouseY = event.y();
		updateView(); // server must know which item is in which cell before the click arrives

		if (isOver(SETTINGS_BUTTON_X, layout.inventoryY() + 38, BUTTON_SIZE, BUTTON_SIZE, mouseX, mouseY)) {
			minecraft.setScreenAndShow(new GuiSettingsScreen(this));
			return true;
		}

		if (isOver(SETTINGS_BUTTON_X, layout.inventoryY() + 58, BUTTON_SIZE, BUTTON_SIZE, mouseX, mouseY)) {
			minecraft.setScreenAndShow(new TerminalStatsScreen(this, menu));
			return true;
		}

		int tab = tabAt(mouseX, mouseY);

		if (tab >= 0) {
			SortCategory clicked = tabCategory(tab);
			category = event.button() == 1 || clicked == category ? null : clicked;
			scrollRow = 0;
			refilter();
			minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
			return true;
		}

		if (searchBox.isMouseOver(mouseX, mouseY)) {
			if (event.button() == 1) {
				searchBox.setValue("");
			}

			setFocused(searchBox);
			searchBox.setFocused(true);
			searchBox.mouseClicked(event, doubleClick);
			return true;
		}

		searchBox.setFocused(false);

		if (isOver(SCROLL_X, SCROLL_Y, SCROLLER_WIDTH, scrollHeight, mouseX, mouseY)) {
			draggingScroller = maxScroll() > 0;
			scrollTo(mouseY);
			return true;
		}

		if (isOverButton(SORT_BUTTON_X, mouseX, mouseY)) {
			sortMode = sortMode.next();
			refilter();
			return true;
		}

		if (isOverButton(DIRECTION_BUTTON_X, mouseX, mouseY)) {
			descending = !descending;
			refilter();
			return true;
		}

		if (layout.hasCrafting()
				&& isOver(CLEAR_BUTTON_X, layout.craftingY() + 21, CLEAR_BUTTON_SIZE, CLEAR_BUTTON_SIZE, mouseX, mouseY)) {
			ClientPlayNetworking.send(new TerminalActionPayload(menu.containerId, TerminalActionPayload.Action.CLEAR_GRID, ItemStack.EMPTY));
			return true;
		}

		if (isOverButton(FILTER_BUTTON_X, mouseX, mouseY)) {
			onlySpecial = !onlySpecial;
			scrollRow = 0;
			refilter();
			return true;
		}

		return super.mouseClicked(event, doubleClick);
	}

	private void scrollTo(double mouseY) {
		int max = maxScroll();

		if (max == 0) {
			scrollRow = 0;
			return;
		}

		float progress = (float) ((mouseY - topPos - SCROLL_Y - SCROLLER_HEIGHT / 2.0) / (scrollHeight - SCROLLER_HEIGHT));
		scrollRow = Mth.clamp(Math.round(progress * max), 0, max);
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
		if (draggingScroller) {
			scrollTo(event.y());
			return true;
		}

		return super.mouseDragged(event, dragX, dragY);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		draggingScroller = false;
		updateView();
		return super.mouseReleased(event);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (maxScroll() > 0 && isOver(0, 0, imageWidth, buttonY, mouseX, mouseY)) {
			scrollRow = Mth.clamp(scrollRow - (int) Math.signum(scrollY), 0, maxScroll());
			return true;
		}

		return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (searchBox.isFocused()) {
			if (event.isEscape()) {
				onClose();
				return true;
			}

			searchBox.keyPressed(event);
			return true; // keys like "E" must not close the terminal while typing
		}

		updateView(); // keys (1–9, Q …) act via vanilla on the cell under the mouse
		return super.keyPressed(event);
	}

	@Override
	public boolean charTyped(CharacterEvent event) {
		if (searchBox.isFocused()) {
			return searchBox.charTyped(event);
		}

		return super.charTyped(event);
	}

	// ------------------------------------------------------------------

	private enum SortMode {
		AMOUNT("jtechstorage.terminal.sort.amount", new ItemStack(Items.CHEST),
				Comparator.comparingLong(StorageNetwork.Entry::total).thenComparing(StorageTerminalScreen::name)),
		NAME("jtechstorage.terminal.sort.name", new ItemStack(Items.NAME_TAG),
				Comparator.comparing(StorageTerminalScreen::name).thenComparingLong(StorageNetwork.Entry::total)),
		MOD("jtechstorage.terminal.sort.mod", new ItemStack(Items.BOOK),
				Comparator.<StorageNetwork.Entry, String>comparing(e -> modId(e.prototype()))
						.thenComparing(StorageTerminalScreen::name));

		private final String translationKey;
		private final ItemStack icon;
		private final Comparator<StorageNetwork.Entry> comparator;

		SortMode(String translationKey, ItemStack icon, Comparator<StorageNetwork.Entry> comparator) {
			this.translationKey = translationKey;
			this.icon = icon;
			this.comparator = comparator;
		}

		SortMode next() {
			return values()[(ordinal() + 1) % values().length];
		}
	}
}
