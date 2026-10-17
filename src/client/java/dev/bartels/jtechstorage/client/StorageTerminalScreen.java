package dev.bartels.jtechstorage.client;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.jspecify.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
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

import dev.bartels.jtechstorage.JTechStorage;
import dev.bartels.jtechstorage.client.gui.GuiSettings;
import dev.bartels.jtechstorage.client.gui.GuiSettingsScreen;
import dev.bartels.jtechstorage.client.gui.GuiTheme;
import dev.bartels.jtechstorage.client.gui.ThemedContainerScreen;
import dev.bartels.jtechstorage.block.StorageTerminalBlockEntity;
import dev.bartels.jtechstorage.menu.StorageTerminalMenu;
import dev.bartels.jtechstorage.net.TerminalActionPayload;
import dev.bartels.jtechstorage.net.TerminalContentsPayload;
import dev.bartels.jtechstorage.network.StorageNetwork;

/**
 * Terminal-Oberfläche (Speicher- und Crafting-Terminal): Suchfeld, scrollbares 9×6-Raster mit exakten Mengen,
 * Sortierung (Menge/Name/Mod, auf-/absteigend) und Filter „nur verzaubert/benannt“.
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

	// Einstellungen bleiben für die Spielsitzung erhalten
	private static SortMode sortMode = SortMode.AMOUNT;
	private static boolean descending = true;
	private static boolean onlySpecial = false;
	private static String lastSearch = "";
	/** Kategorie-Reiter links neben dem Terminal (null = alle). */
	private static dev.bartels.jtechstorage.entity.@Nullable SortCategory category = null;
	private static final int TAB_SIZE = 20;
	private static final int TAB_GAP = 1;
	private static final int TAB_ROWS = 8;
	static final ItemStack[] CATEGORY_ICONS = {
			new ItemStack(Items.BREAD), new ItemStack(Items.ENCHANTED_BOOK), new ItemStack(Items.IRON_CHESTPLATE),
			new ItemStack(Items.IRON_PICKAXE), new ItemStack(Items.REDSTONE), new ItemStack(Items.RAW_IRON),
			new ItemStack(Items.IRON_INGOT), new ItemStack(Items.FLOWER_POT), new ItemStack(Items.WHEAT),
			new ItemStack(Items.OAK_SAPLING), new ItemStack(Items.OAK_LOG), new ItemStack(Items.BONE),
			new ItemStack(Items.DIRT), new ItemStack(Items.COBBLESTONE), new ItemStack(Items.CHEST)};
	private final java.util.EnumMap<dev.bartels.jtechstorage.entity.SortCategory, Integer> categoryCounts =
			new java.util.EnumMap<>(dev.bartels.jtechstorage.entity.SortCategory.class);

	private final StorageTerminalMenu.Layout layout;
	private final int rows;
	private final int buttonY;
	private final int scrollHeight;

	private EditBox searchBox;
	private int searchX = SEARCH_MIN_X;
	private List<StorageNetwork.Entry> filtered = List.of();
	private int seenVersion = -1;
	private int scrollRow;
	private boolean draggingScroller;

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

		// Suchfeld beginnt hinter dem Titel, damit sich nichts überlappt
		searchX = Math.max(SEARCH_MIN_X, titleLabelX + font.width(title) + 8);
		searchBox = new EditBox(font, leftPos + searchX + 2, topPos + SEARCH_Y, SEARCH_RIGHT - searchX - 4, 9,
				Component.translatable("jtechstorage.terminal.search"));
		searchBox.setMaxLength(64);
		searchBox.setBordered(false);
		searchBox.setTextColor(0xFFFFFFFF);
		searchBox.setHint(Component.translatable("jtechstorage.terminal.search.hint").withStyle(ChatFormatting.GRAY));
		searchBox.setValue(lastSearch);
		searchBox.setResponder(value -> {
			lastSearch = value;
			scrollRow = 0;
			refilter();
		});
		addRenderableWidget(searchBox);

		refilter();
	}

	// ------------------------------------------------------------------
	// Filtern & Sortieren
	// ------------------------------------------------------------------

	private void refilter() {
		seenVersion = menu.getClientVersion();
		String phrase = lastSearch.toLowerCase(Locale.ROOT).trim();
		String[] terms = phrase.split("\\s+");
		List<StorageNetwork.Entry> result = new ArrayList<>();
		categoryCounts.clear();

		for (StorageNetwork.Entry entry : menu.getClientEntries()) {
			dev.bartels.jtechstorage.entity.SortCategory entryCategory = dev.bartels.jtechstorage.entity.SortCategory.of(entry.prototype());
			categoryCounts.merge(entryCategory, 1, Integer::sum);

			if (category != null && entryCategory != category) {
				continue;
			}

			if (onlySpecial && !isSpecial(entry.prototype())) {
				continue;
			}

			// "schärfe v" soll auch als ganze Phrase eine Verzauberung finden
			if (matchesAll(entry.prototype(), terms)
					|| (phrase.contains(" ") && enchantmentNames(entry.prototype()).stream().anyMatch(n -> n.contains(phrase)))) {
				result.add(entry);
			}
		}

		Comparator<StorageNetwork.Entry> comparator = sortMode.comparator;
		result.sort(descending ? comparator.reversed() : comparator);
		filtered = result;
		scrollRow = Mth.clamp(scrollRow, 0, maxScroll());
	}

	private static boolean isSpecial(ItemStack stack) {
		return stack.isEnchanted()
				|| stack.has(DataComponents.STORED_ENCHANTMENTS)
				|| stack.has(DataComponents.CUSTOM_NAME);
	}

	/**
	 * Alle Suchbegriffe müssen passen. {@code @mod} = Mod-ID, {@code #tag} = Item-Tag,
	 * {@code $verzauberung} = nur Verzauberungen, sonst Name oder Verzauberung.
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

	/** Namen aller Verzauberungen (auf dem Item und in Büchern), z. B. "reparatur", "schärfe v". */
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
		GuiTheme.panel(graphics, x, y, imageWidth, imageHeight);

		// Netzwerk-Raster + Scrollbalken
		for (int row = 0; row < rows; row++) {
			for (int column = 0; column < COLUMNS; column++) {
				GuiTheme.slot(graphics, x + GRID_X - 1 + column * 18, y + GRID_Y - 1 + row * 18);
			}
		}

		GuiTheme.sunken(graphics, x + SCROLL_X - 1, y + SCROLL_Y - 1, SCROLLER_WIDTH + 2, scrollHeight + 2);

		// Crafting-Raster, Pfeil, Ergebnis
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

		// Upgrade-Slots mit Akzent-Rahmen
		for (int i = 0; i < 2; i++) {
			int sx = x + StorageTerminalMenu.UPGRADE_SLOT_X - 1;
			int sy = y + layout.inventoryY() - 1 + i * 18;
			graphics.fill(sx - 1, sy - 1, sx + 19, sy + 19, GuiTheme.accent());
			GuiTheme.slot(graphics, sx, sy);
		}

		// Spieler-Inventar
		for (int row = 0; row < 4; row++) {
			for (int column = 0; column < 9; column++) {
				int sy = row < 3 ? layout.inventoryY() + row * 18 : layout.inventoryY() + 58;
				GuiTheme.slot(graphics, x + 7 + column * 18, y + sy - 1);
			}
		}

		// Suchfeld-Hintergrund
		int sx = leftPos + searchX;
		int sy = topPos + SEARCH_Y - 2;
		graphics.fill(sx, sy, leftPos + SEARCH_RIGHT, sy + 12, searchBox.isFocused() ? 0xFFFFFFFF : 0xFFA0A0A0);
		graphics.fill(sx + 1, sy + 1, leftPos + SEARCH_RIGHT - 1, sy + 11, 0xFF000000);
	}

	@Override
	public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		if (seenVersion != menu.getClientVersion()) {
			refilter();
			GuiTheme.setTeamColor(menu.getClientAccess().color());
		}

		super.extractContents(graphics, mouseX, mouseY, delta);

		extractGrid(graphics, mouseX, mouseY);
		extractButtonAt(graphics, SETTINGS_BUTTON_X, layout.inventoryY() + 58, mouseX, mouseY, false);
		// kleines Balkendiagramm als Symbol
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
	// Kategorie-Reiter (zwei Spalten links neben dem Terminal)
	// ------------------------------------------------------------------

	/** Reiter 0 = alle, 1..15 = Kategorien in Wichtel-Reihenfolge. */
	private int tabX(int index) {
		int column = index / TAB_ROWS;
		return leftPos - 2 - (2 - column) * (TAB_SIZE + TAB_GAP);
	}

	private int tabY(int index) {
		return topPos + 4 + (index % TAB_ROWS) * (TAB_SIZE + TAB_GAP);
	}

	private static dev.bartels.jtechstorage.entity.@Nullable SortCategory tabCategory(int index) {
		return index == 0 ? null : dev.bartels.jtechstorage.entity.SortCategory.values()[index - 1];
	}

	private int tabAt(double mouseX, double mouseY) {
		for (int index = 0; index <= dev.bartels.jtechstorage.entity.SortCategory.values().length; index++) {
			int x = tabX(index);
			int y = tabY(index);

			if (mouseX >= x && mouseX < x + TAB_SIZE && mouseY >= y && mouseY < y + TAB_SIZE) {
				return index;
			}
		}

		return -1;
	}

	/** Für JEI: Bereich der Reiter freihalten. */
	public List<net.minecraft.client.renderer.Rect2i> getExtraAreas() {
		int x = tabX(0);
		return List.of(new net.minecraft.client.renderer.Rect2i(x, topPos + 4, leftPos - x, TAB_ROWS * (TAB_SIZE + TAB_GAP)));
	}

	private void extractCategoryTabs(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		int hovered = tabAt(mouseX, mouseY);

		for (int index = 0; index <= dev.bartels.jtechstorage.entity.SortCategory.values().length; index++) {
			dev.bartels.jtechstorage.entity.SortCategory tab = tabCategory(index);
			boolean selected = tab == category;
			boolean empty = tab != null && categoryCounts.getOrDefault(tab, 0) == 0;
			int x = tabX(index);
			int y = tabY(index);
			GuiTheme.face(graphics, x, y, TAB_SIZE, TAB_SIZE, selected ? GuiTheme.accent()
					: index == hovered ? GuiTheme.accentHover() : empty ? GuiTheme.DISABLED_FACE : GuiTheme.accentDark());
			ItemStack icon = tab == null ? new ItemStack(dev.bartels.jtechstorage.ModContent.STORAGE_TERMINAL_ITEM) : CATEGORY_ICONS[tab.ordinal()];
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

				if (index < filtered.size()) {
					StorageNetwork.Entry entry = filtered.get(index);
					ItemStack stack = entry.prototype();
					graphics.item(stack, x, y);
					graphics.itemDecorations(font, stack, x, y, "");
					extractCount(graphics, entry.total(), x, y);
				}

				if (index == hovered) {
					graphics.fill(x, y, x + 16, y + 16, 0x80FFFFFF);
				}
			}
		}
	}

	/** Exakte Menge, kompakt formatiert und bei Bedarf verkleinert (wie bei Speichermods üblich). */
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

		if (scale == 1.0f) {
			graphics.text(font, text, x + 17 - font.width(text), y + 9, 0xFFFFFFFF, true);
			return;
		}

		graphics.pose().pushMatrix();
		graphics.pose().translate(x + 16, y + 16);
		graphics.pose().scale(scale, scale);
		graphics.text(font, text, -font.width(text), -8, 0xFFFFFFFF, true);
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
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, max == 0 ? SCROLLER_DISABLED : SCROLLER,
				leftPos + SCROLL_X, topPos + SCROLL_Y + offset, SCROLLER_WIDTH, SCROLLER_HEIGHT);
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
			// ⚡ hinter der Info-Zeile: Netzwerk ist mit Resonanz aufgeladen
			graphics.text(font, "⚡", leftPos + INFO_X + font.width(shown) + 3, topPos + buttonY + 5, GuiTheme.warn(), false);
		}

		if (layout.hasCrafting()) {
			int x = leftPos + CLEAR_BUTTON_X;
			int y = topPos + layout.craftingY() + 21;
			boolean hovered = isOver(CLEAR_BUTTON_X, layout.craftingY() + 21, CLEAR_BUTTON_SIZE, CLEAR_BUTTON_SIZE, mouseX, mouseY);
			graphics.fill(x, y, x + CLEAR_BUTTON_SIZE, y + CLEAR_BUTTON_SIZE, 0xFF000000);
			graphics.fill(x + 1, y + 1, x + CLEAR_BUTTON_SIZE - 1, y + CLEAR_BUTTON_SIZE - 1, hovered ? GuiTheme.mix(GuiTheme.face(), 0xFFFFFFFF, 0.15f) : GuiTheme.face());
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

		int light = active ? GuiTheme.accentHover() : GuiTheme.highlight();
		int dark = active ? GuiTheme.accentDark() : GuiTheme.shadow();
		int face = active ? GuiTheme.accent() : hovered ? GuiTheme.mix(GuiTheme.face(), 0xFFFFFFFF, 0.15f) : GuiTheme.face();

		graphics.fill(x, y, x + BUTTON_SIZE, y + BUTTON_SIZE, 0xFF000000);
		graphics.fill(x + 1, y + 1, x + BUTTON_SIZE - 1, y + BUTTON_SIZE - 1, dark);
		graphics.fill(x + 1, y + 1, x + BUTTON_SIZE - 2, y + BUTTON_SIZE - 2, light);
		graphics.fill(x + 2, y + 2, x + BUTTON_SIZE - 2, y + BUTTON_SIZE - 2, face);
	}

	@Override
	protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		super.extractTooltip(graphics, mouseX, mouseY);

		if (!menu.getCarried().isEmpty()) {
			return;
		}

		int tab = tabAt(mouseX, mouseY);

		if (tab >= 0) {
			dev.bartels.jtechstorage.entity.SortCategory tabCategory = tabCategory(tab);
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
	// Eingabe
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

	/** Item im Netzwerk-Raster unter der Maus (für JEI: R/U-Tasten, Lesezeichen). */
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
			dev.bartels.jtechstorage.entity.SortCategory clicked = tabCategory(tab);
			category = event.button() == 1 || clicked == category ? null : clicked;
			scrollRow = 0;
			refilter();
			minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0f));
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

		int index = gridIndexAt(mouseX, mouseY);

		if (index >= 0) {
			handleGridClick(event, entryAt(index));
			return true;
		}

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

	private void handleGridClick(MouseButtonEvent event, StorageNetwork.@Nullable Entry entry) {
		ItemStack carried = menu.getCarried();
		TerminalActionPayload.Action action;
		ItemStack prototype = ItemStack.EMPTY;

		if (!carried.isEmpty()) {
			action = event.button() == 1 ? TerminalActionPayload.Action.INSERT_ONE : TerminalActionPayload.Action.INSERT_ALL;
		} else if (entry != null) {
			prototype = entry.prototype();

			if (event.hasShiftDown()) {
				action = TerminalActionPayload.Action.TAKE_TO_INVENTORY;
			} else if (event.button() == 1) {
				action = TerminalActionPayload.Action.TAKE_HALF;
			} else if (event.button() == 0) {
				action = TerminalActionPayload.Action.TAKE_STACK;
			} else {
				return;
			}
		} else {
			return;
		}

		if (!menu.hasPermission(StorageTerminalBlockEntity.Permission.ITEMS)) {
			minecraft.player.sendOverlayMessage(Component.translatable("jtechstorage.message.no_permission",
					Component.translatable("jtechstorage.role.member")));
			return;
		}

		ClientPlayNetworking.send(new TerminalActionPayload(menu.containerId, action, prototype));
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
				searchBox.setFocused(false);
				setFocused(null);
				return true;
			}

			searchBox.keyPressed(event);
			return true; // Tasten wie "E" sollen beim Tippen das Terminal nicht schließen
		}

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
