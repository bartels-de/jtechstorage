package dev.bartels.jtechstorage.client;

import dev.bartels.jtechstorage.api.JtsHooks;
import dev.bartels.jtechstorage.client.gui.GuiTheme;
import dev.bartels.jtechstorage.client.gui.ThemedScreen;
import dev.bartels.jtechstorage.display.DisplayContent;
import dev.bartels.jtechstorage.farm.FarmContent;
import dev.bartels.jtechstorage.logistics.LogisticsContent;
import dev.bartels.jtechstorage.tools.ToolsContent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import java.util.Set;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import org.jspecify.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;

import dev.bartels.jtechstorage.ModConfig;
import dev.bartels.jtechstorage.ModContent;
import dev.bartels.jtechstorage.ModGuide;
import dev.bartels.jtechstorage.item.GuideBookItem;

/**
 * The mod's books in the style of a vanilla book: storage manual, recipe book, bestiary (with 3D models),
 * item encyclopedia, crystal chronicle and Chel's joke book. Tables of contents are clickable,
 * "☰" at the top left jumps back to the contents.
 */
public class GuideBookScreen extends ThemedScreen {
	private static final Identifier BOOK_TEXTURE = Identifier.withDefaultNamespace("textures/gui/book.png");
	private static final Identifier PAGE_FORWARD = Identifier.withDefaultNamespace("widget/page_forward");
	private static final Identifier PAGE_FORWARD_HIGHLIGHTED = Identifier.withDefaultNamespace("widget/page_forward_highlighted");
	private static final Identifier PAGE_BACKWARD = Identifier.withDefaultNamespace("widget/page_backward");
	private static final Identifier PAGE_BACKWARD_HIGHLIGHTED = Identifier.withDefaultNamespace("widget/page_backward_highlighted");

	private static final int BOOK_SIZE = 192;
	private static final int TEXT_X = 36;
	private static final int TEXT_Y = 30;
	private static final int TEXT_WIDTH = 114;
	/** Text area of a page (down to just above the page-turn arrows). */
	private static final int TEXT_BOTTOM = 152;
	private static final int TEXT_COLOR = 0xFF000000;
	private static final int LINK_COLOR = 0xFF5A2A8A;
	private static final int LINK_HOVER = 0xFFB040E0;
	private static final int MODEL_HEIGHT = 62;
	private static final int LINK_HEIGHT = 11;

	/** Last opened page per book (while the game is running). */
	private static final Map<GuideBookItem.Kind, Integer> LAST_PAGE = new HashMap<>();

	private final GuideBookItem.Kind kind;
	private final ItemStack book;
	private final List<Page> pages = new ArrayList<>();
	/** Displayed pages: long texts are automatically split across several book pages. */
	private final List<Object> display = new ArrayList<>();
	/** First displayed page of each source page (link target). */
	private final Map<Page, Integer> firstDisplay = new IdentityHashMap<>();
	private final Map<EntityType<?>, @Nullable LivingEntity> models = new HashMap<>();
	/** Negative IDs for the book models (never collide with real entities). */
	private static int nextModelId = -1_000_000;
	private @Nullable ContentsPage contents;
	private int page;
	private int left;
	private int top;

	public GuideBookScreen(ItemStack book) {
		super(book.getHoverName());
		this.book = book.copy();
		this.kind = book.getItem() instanceof GuideBookItem item ? item.kind() : GuideBookItem.Kind.HANDBOOK;
		page = LAST_PAGE.getOrDefault(kind, 0);
		buildPages();
	}

	// ------------------------------------------------------------------
	// Page types
	// ------------------------------------------------------------------

	private sealed interface Page permits InfoPage, EntityPage, ItemPage, ChapterPage, ContentsPage, CraftingPage, SmithingPage {
	}

	private record InfoPage(Component title, Component body) implements Page {
	}

	/** Bestiary: 3D model below the title. */
	private record EntityPage(EntityType<?> type, Component title, Component body) implements Page {
	}

	/** Encyclopedia: item icon next to the title. */
	private record ItemPage(ItemStack icon, Component title, Component body) implements Page {
	}

	/** Chapter cover page. */
	private record ChapterPage(ItemStack icon, Component title, Component intro) implements Page {
	}

	private record Link(Component label, Page target) {
	}

	private record ContentsPage(List<Link> links) implements Page {
	}

	/** 3×3 recipe; {@code grid} has 9 entries (null = empty). */
	private record CraftingPage(ItemStack result, @Nullable Item[] grid, boolean shapeless, String descKey) implements Page {
	}

	private record SmithingPage(ItemStack result, Item template, Item base, Item addition, String descKey) implements Page {
	}

	/** A chunk of a text page (the title is repeated on every continuation page). */
	private record TextChunk(List<FormattedCharSequence> title, List<FormattedCharSequence> body, int part, int parts,
			@Nullable ItemStack icon, @Nullable EntityType<?> entity) {
	}

	private record ContentsChunk(List<Link> links, boolean first) {
	}

	// ------------------------------------------------------------------
	// Content
	// ------------------------------------------------------------------

	private static Component tr(String key, Object... args) {
		return Component.translatable(key, args);
	}

	private static Component guideTitle(String name) {
		return tr("jtechstorage.guide." + name + ".title");
	}

	private static Component guideBody(String name, Object... args) {
		return tr("jtechstorage.guide." + name + ".body", args);
	}

	// ------------------------------------------------------------------
	// v0.43: pages of sub-mods only if they are installed
	// ------------------------------------------------------------------

	private static final Set<String> HELPER_CHAPTERS = Set.of("phoenix", "wolves", "companions", "recipes_helpers",
			"helpers", "evolutions", "chronicle_evolutions");
	private static final Set<String> ADVENTURE_CHAPTERS = Set.of("storm", "recipes_end", "bosses", "storm_creatures",
			"depths_creatures", "lexicon_gear", "chronicle_bosses", "chronicle_obsidian", "chronicle_enchants");
	private static final Set<String> ADVENTURE_PAGES = Set.of("crystal_depths", "portal_link", "workshop",
			"depths_animals", "depths_explore", "memory_crystals", "temple", "forge", "storm_legend");
	/** Chapter of a missing sub-mod: skip everything up to the next chapter. */
	private boolean skipping;

	private static Item it(String path) {
		return JtsHooks.item(path);
	}

	private static @Nullable EntityType<?> en(String path) {
		return JtsHooks.entity(path);
	}

	private static boolean missing(@Nullable ItemLike... items) {
		for (ItemLike item : items) {
			if (item != null && item.asItem() == Items.AIR) {
				return true;
			}
		}

		return false;
	}

	private @Nullable InfoPage info(String name, Object... args) {
		if (skipping || ADVENTURE_PAGES.contains(name) && !JtsHooks.adventure()) {
			return null;
		}

		InfoPage info = new InfoPage(guideTitle(name), guideBody(name, args));
		pages.add(info);
		return info;
	}

	private @Nullable ChapterPage chapter(ItemLike icon, String key) {
		skipping = HELPER_CHAPTERS.contains(key) && !JtsHooks.helpers()
				|| ADVENTURE_CHAPTERS.contains(key) && !JtsHooks.adventure();

		if (skipping) {
			return null;
		}

		ChapterPage chapter = new ChapterPage(new ItemStack(icon), tr("jtechstorage.book.chapter." + key),
				tr("jtechstorage.book.chapter." + key + ".intro"));
		pages.add(chapter);

		if (contents != null) {
			contents.links().add(new Link(chapter.title(), chapter));
		}

		return chapter;
	}

	private void contents() {
		contents = new ContentsPage(new ArrayList<>());
		pages.add(contents);
	}

	private void buildPages() {
		switch (kind) {
			case HANDBOOK -> buildHandbook();
			case RECIPES -> buildRecipes();
			case BESTIARY -> buildBestiary();
			case LEXICON -> buildLexicon();
			case CHRONICLE -> buildChronicle();
			case JOKES -> buildJokes();
		}
	}

	private void buildHandbook() {
		info("welcome", ModGuide.AUTHOR);
		contents();

		chapter(ModContent.STORAGE_TERMINAL_ITEM, "basics");
		info("start");
		info("terminal");
		ModConfig.Network net = ModConfig.get().network;
		info("upgrades", net.baseRange, net.ironRange, net.goldRange, net.diamondRange, net.netheriteRange, net.enderRange,
				net.baseCapacity, net.ironCapacity, net.goldCapacity, net.diamondCapacity, net.netheriteCapacity);
		info("sorting");
		info("interface");
		info("amethyst_chests");
		info("crystal_depths");
		info("portal_link");
		info("workshop");
		info("depths_animals");
		info("depths_explore");
		info("memory_crystals");
		info("temple");
		crafting(it("memory_case"), "jtechstorage.guide.recipe.memory_case",
				Items.DARK_OAK_SLAB, Items.DARK_OAK_SLAB, Items.DARK_OAK_SLAB, it("crystal_glass"), Items.AMETHYST_SHARD, it("crystal_glass"), Items.GOLD_INGOT, it("polished_crystal_stone"), Items.GOLD_INGOT);
		info("controls");
		info("rest");
		info("farm_tools");
		info("block_placer");
		crafting(FarmContent.BLOCK_PLACER, "jtechstorage.guide.recipe.block_placer",
				Items.COBBLED_DEEPSLATE, Items.COBBLED_DEEPSLATE, Items.COBBLED_DEEPSLATE, Items.COBBLED_DEEPSLATE, Items.PISTON, Items.COBBLED_DEEPSLATE, Items.COBBLED_DEEPSLATE, Items.REDSTONE, Items.COBBLED_DEEPSLATE);
		info("build_tools");
		crafting(ToolsContent.BUILDING_WAND, "jtechstorage.guide.recipe.building_wand",
				null, null, Items.AMETHYST_BLOCK, null, Items.GOLD_INGOT, null, Items.STICK, null, null);
		crafting(ToolsContent.EXCHANGE_WAND, "jtechstorage.guide.recipe.exchange_wand",
				null, null, Items.AMETHYST_BLOCK, null, Items.ENDER_PEARL, null, Items.STICK, null, null);
		shapeless(ToolsContent.MEASURING_TAPE, "jtechstorage.guide.recipe.measuring_tape", Items.IRON_INGOT, Items.STRING, Items.DYE.yellow());
		info("logistics");
		crafting(LogisticsContent.CONVEYOR, "jtechstorage.guide.recipe.crystal_conveyor",
				Items.AMETHYST_SHARD, Items.AMETHYST_SHARD, Items.AMETHYST_SHARD, Items.IRON_INGOT, Items.REDSTONE, Items.IRON_INGOT, null, null, null);
		crafting(LogisticsContent.FAST_CONVEYOR, "jtechstorage.guide.recipe.fast_crystal_conveyor",
				LogisticsContent.CONVEYOR, LogisticsContent.CONVEYOR, LogisticsContent.CONVEYOR, LogisticsContent.CONVEYOR, it("glow_crystal"), LogisticsContent.CONVEYOR, LogisticsContent.CONVEYOR, LogisticsContent.CONVEYOR, LogisticsContent.CONVEYOR);
		crafting(LogisticsContent.SORTER, "jtechstorage.guide.recipe.crystal_sorter",
				null, Items.AMETHYST_SHARD, null, LogisticsContent.CONVEYOR, Items.HOPPER, LogisticsContent.CONVEYOR, null, Items.COMPARATOR, null);
		info("forge");
		crafting(it("crystal_forge"), "jtechstorage.guide.recipe.crystal_forge",
				it("pure_amethyst"), it("pure_amethyst"), it("pure_amethyst"), Items.POLISHED_DEEPSLATE, Items.SMITHING_TABLE, Items.POLISHED_DEEPSLATE, Items.POLISHED_DEEPSLATE, Items.ANVIL, Items.POLISHED_DEEPSLATE);
		info("cosmetics");

		chapter(ModContent.TEAM_TERMINAL_ITEM, "team");
		info("friends");
		info("trade");

		chapter(it("phoenix_nest"), "phoenix");
		info("phoenix");
		info("amethyst_phoenix");
		info("phoenix_stops");
		info("signal_lantern");

		chapter(Items.AMETHYST_CLUSTER, "storm");
		info("storm");

		chapter(it("crystal_wolf"), "wolves");
		info("crystal_wolf");
		info("moon_wolf");
		info("helper_home");

		chapter(Items.AMETHYST_CLUSTER, "companions");
		info("companions");
		info("geode_keeper");
		info("mantis");
		info("shroom");
		info("raven");
		info("companion_look");
		info("companion_bond");
		shapeless(it("resonance_treat"), "jtechstorage.guide.recipe.resonance_treat",
				Items.HONEY_BOTTLE, Items.AMETHYST_SHARD, Items.AMETHYST_SHARD);

		chapter(DisplayContent.STORAGE_DISPLAY, "displays");
		info("storage_display");
		info("display_modes");
		info("storage_banner");
		info("display_menu");
		crafting(DisplayContent.STORAGE_DISPLAY, "jtechstorage.guide.recipe.storage_display",
				Items.AMETHYST_SHARD, Items.POLISHED_DEEPSLATE, Items.AMETHYST_SHARD, Items.POLISHED_DEEPSLATE, Items.TINTED_GLASS, Items.POLISHED_DEEPSLATE, Items.REDSTONE, Items.COMPARATOR, Items.REDSTONE);
		crafting(DisplayContent.STORAGE_BANNER, "jtechstorage.guide.recipe.storage_banner",
				Items.GOLD_NUGGET, Items.STICK, Items.GOLD_NUGGET, Items.WOOL.purple(), Items.AMETHYST_SHARD, Items.WOOL.purple(), Items.WOOL.purple(), Items.REDSTONE, Items.WOOL.purple());

		chapter(Items.AMETHYST_CLUSTER, "cosmetics");
		info("cosmetics");
		info("cosmetics_unlock");

		chapter(ModContent.LEATHER_BACKPACK, "backpacks");
		info("backpack");

		chapter(ModContent.RECIPE_BOOK, "library");
		info("library");
		shapeless(ModContent.GUIDE_BOOK, "jtechstorage.guide.recipe.guide_book", Items.BOOK, Items.AMETHYST_SHARD);
		bookRecipes();
	}

	private void bookRecipes() {
		shapeless(ModContent.RECIPE_BOOK, "jtechstorage.guide.recipe.recipe_book", Items.BOOK, Items.AMETHYST_SHARD, Items.CRAFTING_TABLE);
		shapeless(ModContent.BESTIARY, "jtechstorage.guide.recipe.bestiary", Items.BOOK, Items.AMETHYST_SHARD, Items.FEATHER);
		shapeless(ModContent.ITEM_LEXICON, "jtechstorage.guide.recipe.item_lexicon", Items.BOOK, Items.AMETHYST_SHARD, Items.ITEM_FRAME);
		shapeless(ModContent.CRYSTAL_CHRONICLE, "jtechstorage.guide.recipe.crystal_chronicle", Items.BOOK, Items.AMETHYST_SHARD, Items.ENDER_EYE);
		shapeless(ModContent.JOKE_BOOK, "jtechstorage.guide.recipe.joke_book", Items.BOOK, Items.AMETHYST_SHARD, Items.COOKIE);
	}

	// --- Recipe book (identical to the recipe JSONs) --------------------

	private void buildRecipes() {
		info("recipes");
		contents();

		chapter(ModContent.STORAGE_TERMINAL_ITEM, "recipes_blocks");
		crafting(ModContent.STORAGE_TERMINAL_ITEM, "jtechstorage.guide.recipe.storage_terminal",
				Items.POLISHED_DEEPSLATE, Items.AMETHYST_BLOCK, Items.POLISHED_DEEPSLATE, Items.REDSTONE_BLOCK, Items.CHEST, Items.REDSTONE_BLOCK, Items.POLISHED_DEEPSLATE, Items.DIAMOND, Items.POLISHED_DEEPSLATE);
		shapeless(ModContent.CRAFTING_TERMINAL_ITEM, "jtechstorage.guide.recipe.crafting_terminal",
				ModContent.STORAGE_TERMINAL_ITEM, Items.CRAFTER);
		crafting(ModContent.TEAM_TERMINAL_ITEM, "jtechstorage.guide.recipe.team_terminal",
				Items.GOLD_INGOT, Items.BELL, Items.GOLD_INGOT, Items.AMETHYST_BLOCK, Items.LECTERN, Items.AMETHYST_BLOCK, Items.POLISHED_DEEPSLATE, Items.REDSTONE_BLOCK, Items.POLISHED_DEEPSLATE);
		crafting(ModContent.TRADE_TERMINAL_ITEM, "jtechstorage.guide.recipe.trade_terminal",
				Items.POLISHED_DEEPSLATE, Items.EMERALD_BLOCK, Items.POLISHED_DEEPSLATE, Items.AMETHYST_BLOCK, Items.BARREL, Items.AMETHYST_BLOCK, Items.POLISHED_DEEPSLATE, Items.REDSTONE_BLOCK, Items.POLISHED_DEEPSLATE);
		crafting(it("phoenix_nest"), "jtechstorage.guide.recipe.phoenix_nest",
				Items.STICK, Items.AMETHYST_BLOCK, Items.STICK, Items.HAY_BLOCK, Items.BARREL, Items.HAY_BLOCK, Items.STICK, Items.STICK, Items.STICK);
		crafting(it("phoenix_stop"), "jtechstorage.guide.recipe.phoenix_stop",
				null, Items.AMETHYST_SHARD, null, Items.GOLD_INGOT, Items.LODESTONE, Items.GOLD_INGOT, Items.POLISHED_DEEPSLATE, Items.POLISHED_DEEPSLATE, Items.POLISHED_DEEPSLATE);
		crafting(it("wolf_basket"), "jtechstorage.guide.recipe.wolf_basket",
				Items.STICK, null, Items.STICK, Items.WHEAT, Items.WOOL.white(), Items.WHEAT, Items.WHEAT, Items.AMETHYST_SHARD, Items.WHEAT);
		crafting(it("helper_bed"), "jtechstorage.guide.recipe.helper_bed",
				Items.AMETHYST_SHARD, null, null, Items.WOOL.white(), Items.WOOL.white(), Items.GOLD_NUGGET, Items.OAK_PLANKS, Items.OAK_PLANKS, Items.OAK_PLANKS);
		crafting(it("amethyst_bone"), "jtechstorage.guide.recipe.amethyst_bone",
				Items.GLOWSTONE_DUST, Items.AMETHYST_SHARD, Items.GLOWSTONE_DUST, Items.AMETHYST_SHARD, Items.BONE, Items.AMETHYST_SHARD, Items.GLOWSTONE_DUST, Items.AMETHYST_SHARD, Items.GLOWSTONE_DUST);
		crafting(it("signal_lantern"), "jtechstorage.guide.recipe.signal_lantern",
				null, Items.GOLD_NUGGET, null, Items.POLISHED_DEEPSLATE, Items.AMETHYST_SHARD, Items.POLISHED_DEEPSLATE, Items.POLISHED_DEEPSLATE, Items.REDSTONE, Items.POLISHED_DEEPSLATE);

		crafting(DisplayContent.STORAGE_DISPLAY, "jtechstorage.guide.recipe.storage_display",
				Items.AMETHYST_SHARD, Items.POLISHED_DEEPSLATE, Items.AMETHYST_SHARD, Items.POLISHED_DEEPSLATE, Items.TINTED_GLASS, Items.POLISHED_DEEPSLATE, Items.REDSTONE, Items.COMPARATOR, Items.REDSTONE);
		crafting(DisplayContent.STORAGE_BANNER, "jtechstorage.guide.recipe.storage_banner",
				Items.GOLD_NUGGET, Items.STICK, Items.GOLD_NUGGET, Items.WOOL.purple(), Items.AMETHYST_SHARD, Items.WOOL.purple(), Items.WOOL.purple(), Items.REDSTONE, Items.WOOL.purple());
		crafting(it("memory_case"), "jtechstorage.guide.recipe.memory_case",
				Items.DARK_OAK_SLAB, Items.DARK_OAK_SLAB, Items.DARK_OAK_SLAB, it("crystal_glass"), Items.AMETHYST_SHARD, it("crystal_glass"), Items.GOLD_INGOT, it("polished_crystal_stone"), Items.GOLD_INGOT);

		crafting(FarmContent.BLOCK_PLACER, "jtechstorage.guide.recipe.block_placer",
				Items.COBBLED_DEEPSLATE, Items.COBBLED_DEEPSLATE, Items.COBBLED_DEEPSLATE, Items.COBBLED_DEEPSLATE, Items.PISTON, Items.COBBLED_DEEPSLATE, Items.COBBLED_DEEPSLATE, Items.REDSTONE, Items.COBBLED_DEEPSLATE);
		crafting(LogisticsContent.CONVEYOR, "jtechstorage.guide.recipe.crystal_conveyor",
				Items.AMETHYST_SHARD, Items.AMETHYST_SHARD, Items.AMETHYST_SHARD, Items.IRON_INGOT, Items.REDSTONE, Items.IRON_INGOT, null, null, null);
		crafting(LogisticsContent.FAST_CONVEYOR, "jtechstorage.guide.recipe.fast_crystal_conveyor",
				LogisticsContent.CONVEYOR, LogisticsContent.CONVEYOR, LogisticsContent.CONVEYOR, LogisticsContent.CONVEYOR, it("glow_crystal"), LogisticsContent.CONVEYOR, LogisticsContent.CONVEYOR, LogisticsContent.CONVEYOR, LogisticsContent.CONVEYOR);
		crafting(LogisticsContent.SORTER, "jtechstorage.guide.recipe.crystal_sorter",
				null, Items.AMETHYST_SHARD, null, LogisticsContent.CONVEYOR, Items.HOPPER, LogisticsContent.CONVEYOR, null, Items.COMPARATOR, null);
		crafting(it("crystal_forge"), "jtechstorage.guide.recipe.crystal_forge",
				it("pure_amethyst"), it("pure_amethyst"), it("pure_amethyst"), Items.POLISHED_DEEPSLATE, Items.SMITHING_TABLE, Items.POLISHED_DEEPSLATE, Items.POLISHED_DEEPSLATE, Items.ANVIL, Items.POLISHED_DEEPSLATE);

		chapter(ModContent.STORAGE_KEY, "recipes_tools");
		crafting(ToolsContent.BUILDING_WAND, "jtechstorage.guide.recipe.building_wand",
				null, null, Items.AMETHYST_BLOCK, null, Items.GOLD_INGOT, null, Items.STICK, null, null);
		crafting(ToolsContent.EXCHANGE_WAND, "jtechstorage.guide.recipe.exchange_wand",
				null, null, Items.AMETHYST_BLOCK, null, Items.ENDER_PEARL, null, Items.STICK, null, null);
		shapeless(ToolsContent.MEASURING_TAPE, "jtechstorage.guide.recipe.measuring_tape", Items.IRON_INGOT, Items.STRING, Items.DYE.yellow());
		crafting(ModContent.STORAGE_KEY, "jtechstorage.guide.recipe.storage_key",
				null, Items.AMETHYST_SHARD, null, null, Items.ENDER_PEARL, null, null, Items.GOLD_INGOT, null);
		crafting(ModContent.WIRELESS_TERMINAL, "jtechstorage.guide.recipe.wireless_terminal",
				Items.AMETHYST_BLOCK, Items.ENDER_EYE, Items.AMETHYST_BLOCK, Items.POLISHED_DEEPSLATE, Items.DIAMOND, Items.POLISHED_DEEPSLATE, Items.POLISHED_DEEPSLATE, Items.REDSTONE_BLOCK, Items.POLISHED_DEEPSLATE);
		shapeless(ModContent.WIRELESS_TERMINAL, "jtechstorage.guide.recipe.wireless_core",
				ModContent.WIRELESS_TERMINAL, ModContent.WIRELESS_CORE);
		shapeless(it("flight_plan"), "jtechstorage.guide.recipe.flight_plan", Items.PAPER, Items.FEATHER, Items.AMETHYST_SHARD, Items.ENDER_PEARL);
		crafting(it("amethyst_draught"), "jtechstorage.guide.recipe.amethyst_draught",
				Items.AMETHYST_BLOCK, Items.GHAST_TEAR, Items.AMETHYST_BLOCK, Items.DIAMOND, Items.GLASS_BOTTLE, Items.DIAMOND,
				Items.AMETHYST_BLOCK, Items.REDSTONE_BLOCK, Items.AMETHYST_BLOCK);
		crafting(it("lair_compass"), "jtechstorage.guide.recipe.lair_compass",
				Items.AMETHYST_SHARD, Items.ENDER_EYE, Items.AMETHYST_SHARD, Items.POPPED_CHORUS_FRUIT, Items.COMPASS, Items.POPPED_CHORUS_FRUIT,
				Items.AMETHYST_SHARD, Items.ENDER_EYE, Items.AMETHYST_SHARD);
		crafting(it("crystal_cocoon"), "jtechstorage.guide.recipe.crystal_cocoon",
				Items.STRING, Items.AMETHYST_BLOCK, Items.STRING, Items.CRYING_OBSIDIAN, Items.FERMENTED_SPIDER_EYE, Items.CRYING_OBSIDIAN,
				Items.STRING, Items.ENDER_EYE, Items.STRING);
		shapeless(ModContent.GUIDE_BOOK, "jtechstorage.guide.recipe.guide_book", Items.BOOK, Items.AMETHYST_SHARD);
		bookRecipes();

		chapter(it("amethyst_sprite"), "recipes_helpers");
		crafting(it("amethyst_sprite"), "jtechstorage.guide.recipe.amethyst_sprite",
				Items.AMETHYST_BLOCK, Items.ENDER_EYE, Items.AMETHYST_BLOCK, Items.AMETHYST_BLOCK, ModContent.AMETHYST_CORE, Items.AMETHYST_BLOCK, Items.AMETHYST_BLOCK, Items.REDSTONE_BLOCK, Items.AMETHYST_BLOCK);
		crafting(it("collector_moth"), "jtechstorage.guide.recipe.collector_moth",
				Items.PHANTOM_MEMBRANE, Items.SPYGLASS, Items.PHANTOM_MEMBRANE, Items.GLOW_INK_SAC, ModContent.AMETHYST_CORE, Items.GLOW_INK_SAC, Items.PHANTOM_MEMBRANE, Items.LANTERN, Items.PHANTOM_MEMBRANE);
		crafting(it("storage_griffin"), "jtechstorage.guide.recipe.storage_griffin",
				Items.PHANTOM_MEMBRANE, ModContent.AMETHYST_CORE, Items.PHANTOM_MEMBRANE, Items.FEATHER, Items.ENDER_CHEST, Items.FEATHER, Items.FEATHER, Items.SADDLE, Items.FEATHER);
		crafting(it("spark_beetle"), "jtechstorage.guide.recipe.spark_beetle",
				Items.REDSTONE_BLOCK, byId("lightning_rod"), Items.REDSTONE_BLOCK, byId("copper_block"), ModContent.AMETHYST_CORE, byId("copper_block"), byId("copper_block"), Items.AMETHYST_BLOCK, byId("copper_block"));
		crafting(it("chel"), "jtechstorage.guide.recipe.chel",
				Items.AMETHYST_BLOCK, ModContent.AMETHYST_CORE, Items.AMETHYST_BLOCK, Items.COOKIE, Items.CRAFTER, Items.COOKIE, Items.POLISHED_DEEPSLATE, Items.REDSTONE_BLOCK, Items.POLISHED_DEEPSLATE);
		crafting(it("ashley"), "jtechstorage.guide.recipe.ashley",
				Items.COAL_BLOCK, ModContent.AMETHYST_CORE, Items.COAL_BLOCK, Items.MUD, Items.CAMPFIRE, Items.MUD, Items.COAL_BLOCK, Items.MUD, Items.COAL_BLOCK);
		crafting(it("amethyst_fox"), "jtechstorage.guide.recipe.amethyst_fox",
				Items.AMETHYST_BLOCK, Items.GLOW_BERRIES, Items.AMETHYST_BLOCK, Items.LAPIS_BLOCK, ModContent.AMETHYST_CORE, Items.LAPIS_BLOCK, Items.AMETHYST_BLOCK, Items.BOOK, Items.AMETHYST_BLOCK);
		crafting(it("phoenix"), "jtechstorage.guide.recipe.phoenix",
				Items.BLAZE_ROD, Items.GOLD_BLOCK, Items.BLAZE_ROD, Items.FEATHER, Items.TOTEM_OF_UNDYING, Items.FEATHER, Items.BLAZE_ROD, ModContent.AMETHYST_CORE, Items.BLAZE_ROD);
		crafting(it("dragon_heart"), "jtechstorage.guide.recipe.dragon_heart",
				Items.DRAGON_BREATH, ModContent.CRYSTAL_OBSIDIAN, Items.DRAGON_BREATH, Items.AMETHYST_BLOCK, Items.NETHER_STAR, Items.AMETHYST_BLOCK,
				Items.DRAGON_BREATH, ModContent.AMETHYST_OBSIDIAN_UPGRADE, Items.DRAGON_BREATH);

		chapter(ModContent.GOLD_RANGE_UPGRADE, "recipes_upgrades");
		crafting(ModContent.GOLD_RANGE_UPGRADE, "jtechstorage.guide.recipe.range_upgrade",
				Items.AMETHYST_SHARD, Items.GOLD_BLOCK, Items.AMETHYST_SHARD, Items.GOLD_BLOCK, ModContent.IRON_RANGE_UPGRADE, Items.GOLD_BLOCK, Items.AMETHYST_SHARD, Items.GOLD_BLOCK, Items.AMETHYST_SHARD);
		crafting(ModContent.DIAMOND_RANGE_UPGRADE, "jtechstorage.guide.recipe.range_upgrade",
				Items.AMETHYST_BLOCK, Items.DIAMOND, Items.AMETHYST_BLOCK, Items.DIAMOND, ModContent.GOLD_RANGE_UPGRADE, Items.DIAMOND, Items.AMETHYST_BLOCK, Items.DIAMOND, Items.AMETHYST_BLOCK);
		smithing(ModContent.NETHERITE_RANGE_UPGRADE, ModContent.DIAMOND_RANGE_UPGRADE, "jtechstorage.guide.recipe.range_upgrade");
		crafting(ModContent.ENDER_RANGE_UPGRADE, "jtechstorage.guide.recipe.ender_range_upgrade",
				Items.ENDER_EYE, Items.SHULKER_SHELL, Items.ENDER_EYE,
				Items.SHULKER_SHELL, ModContent.NETHERITE_RANGE_UPGRADE, Items.SHULKER_SHELL,
				Items.ENDER_EYE, Items.NETHER_STAR, Items.ENDER_EYE);
		crafting(ModContent.GOLD_CAPACITY_UPGRADE, "jtechstorage.guide.recipe.capacity_upgrade",
				Items.CHEST, Items.GOLD_BLOCK, Items.CHEST, Items.GOLD_BLOCK, ModContent.IRON_CAPACITY_UPGRADE, Items.GOLD_BLOCK, Items.CHEST, Items.GOLD_BLOCK, Items.CHEST);
		crafting(ModContent.DIAMOND_CAPACITY_UPGRADE, "jtechstorage.guide.recipe.capacity_upgrade",
				Items.AMETHYST_BLOCK, Items.DIAMOND, Items.AMETHYST_BLOCK, Items.DIAMOND, ModContent.GOLD_CAPACITY_UPGRADE, Items.DIAMOND, Items.AMETHYST_BLOCK, Items.DIAMOND, Items.AMETHYST_BLOCK);
		smithing(ModContent.NETHERITE_CAPACITY_UPGRADE, ModContent.DIAMOND_CAPACITY_UPGRADE, "jtechstorage.guide.recipe.capacity_upgrade");

		chapter(ModContent.LEATHER_BACKPACK, "recipes_backpacks");
		crafting(ModContent.LEATHER_BACKPACK, "jtechstorage.guide.recipe.backpack",
				Items.LEATHER, Items.STRING, Items.LEATHER, Items.LEATHER, Items.CHEST, Items.LEATHER, Items.LEATHER, Items.LEATHER, Items.LEATHER);
		shapeless(ModContent.IRON_BACKPACK, "jtechstorage.guide.recipe.backpack", ModContent.LEATHER_BACKPACK, Items.IRON_BLOCK);
		shapeless(ModContent.GOLD_BACKPACK, "jtechstorage.guide.recipe.backpack", ModContent.IRON_BACKPACK, Items.GOLD_BLOCK);
		shapeless(ModContent.DIAMOND_BACKPACK, "jtechstorage.guide.recipe.backpack", ModContent.GOLD_BACKPACK, Items.DIAMOND_BLOCK);
		smithing(ModContent.NETHERITE_BACKPACK, ModContent.DIAMOND_BACKPACK, "jtechstorage.guide.recipe.backpack");

		chapter(it("amethyst_obsidian_chestplate"), "recipes_end");
		crafting(ModContent.CRYSTAL_OBSIDIAN, "jtechstorage.guide.recipe.crystal_obsidian",
				Items.CRYING_OBSIDIAN, Items.AMETHYST_BLOCK, Items.CRYING_OBSIDIAN, Items.AMETHYST_BLOCK, Items.NETHERITE_SCRAP, Items.AMETHYST_BLOCK,
				Items.CRYING_OBSIDIAN, Items.AMETHYST_BLOCK, Items.CRYING_OBSIDIAN);
		amethystObsidian(it("amethyst_obsidian_sword"), Items.NETHERITE_SWORD);
		amethystObsidian(it("amethyst_obsidian_pickaxe"), Items.NETHERITE_PICKAXE);
		amethystObsidian(it("amethyst_obsidian_axe"), Items.NETHERITE_AXE);
		amethystObsidian(it("amethyst_obsidian_shovel"), Items.NETHERITE_SHOVEL);
		amethystObsidian(it("amethyst_obsidian_hoe"), Items.NETHERITE_HOE);
		amethystObsidian(it("amethyst_obsidian_helmet"), Items.NETHERITE_HELMET);
		amethystObsidian(it("amethyst_obsidian_chestplate"), Items.NETHERITE_CHESTPLATE);
		amethystObsidian(it("amethyst_obsidian_leggings"), Items.NETHERITE_LEGGINGS);
		amethystObsidian(it("amethyst_obsidian_boots"), Items.NETHERITE_BOOTS);
	}

	// --- Bestiary -----------------------------------------------------

	private void buildBestiary() {
		info("bestiary");
		contents();

		chapter(it("amethyst_sprite"), "helpers");
		creature(en("amethyst_sprite"), "sprite");
		creature(en("collector_moth"), "moth");
		creature(en("storage_griffin"), "griffin");
		creature(en("spark_beetle"), "beetle");
		creature(en("chel"), "chel");
		creature(en("ashley"), "ashley");
		creature(en("amethyst_fox"), "fox");
		creature(en("phoenix"), "phoenix");
		creature(en("crystal_wolf"), "crystal_wolf");

		chapter(it("dragon_heart"), "evolutions");
		creature(en("peter_chel_parker"), "peter");
		creature(en("cheldrogan"), "cheldrogan");
		creature(en("amethyst_phoenix"), "amethyst_phoenix");
		creature(en("moon_wolf"), "moon_wolf");

		chapter(Items.AMETHYST_CLUSTER, "companions");
		creature(en("geode_keeper"), "geode_keeper");
		creature(en("mantis"), "mantis");
		creature(en("shroom"), "shroom");
		creature(en("raven"), "raven");

		chapter(it("amethyst_draught"), "bosses");
		creature(en("amethyst_robot"), "boss");
		creature(en("amethyst_spider"), "spider");
		creature(en("last_master"), "last_master");

		chapter(Items.AMETHYST_CLUSTER, "storm_creatures");
		creature(en("crystal_shardling"), "shardling");
		creature(en("storm_herald"), "herald");

		chapter(it("glow_crystal"), "depths_creatures");
		creature(en("crystal_lurker"), "crystal_lurker");
		creature(en("swarm_shard"), "swarm_shard");
		creature(en("lake_wraith"), "lake_wraith");
		creature(en("vein_crawler"), "vein_crawler");
		creature(en("crystal_snail"), "crystal_snail");
		creature(en("glow_fish"), "glow_fish");
		creature(en("quokka"), "quokka");
		creature(en("crystal_deer"), "crystal_deer");
		creature(en("geode_sheep"), "geode_sheep");
		creature(en("crystal_crab"), "crystal_crab");

		chapter(ModContent.GUIDE_BOOK, "others");
		creature(ModContent.JBARTELS_HOLOGRAM, "jbartels");
	}

	private void creature(@Nullable EntityType<?> type, String guide) {
		if (skipping || type == null) {
			return;
		}

		// bestiary text if present – otherwise the manual text
		String key = "jtechstorage.bestiary." + guide;
		Component body = Language.getInstance().has(key) ? tr(key) : guideBody(guide);
		pages.add(new EntityPage(type, type.getDescription(), body));
	}

	// --- Item encyclopedia --------------------------------------------

	private void buildLexicon() {
		info("lexicon");
		contents();

		chapter(ModContent.STORAGE_TERMINAL_ITEM, "lexicon_blocks");
		lexicon(ModContent.STORAGE_TERMINAL_ITEM, ModContent.CRAFTING_TERMINAL_ITEM, ModContent.TEAM_TERMINAL_ITEM,
				ModContent.TRADE_TERMINAL_ITEM, it("phoenix_nest"), it("phoenix_stop"),
				it("signal_lantern"), it("wolf_basket"),
				it("helper_bed"), it("crystal_altar"),
				DisplayContent.STORAGE_DISPLAY.asItem(), DisplayContent.STORAGE_BANNER.asItem(),
				it("memory_case"), FarmContent.BLOCK_PLACER_ITEM);

		chapter(ModContent.STORAGE_KEY, "lexicon_tools");
		lexicon(ToolsContent.BUILDING_WAND, ToolsContent.EXCHANGE_WAND, ToolsContent.MEASURING_TAPE, it("master_hammer"));
		lexicon(LogisticsContent.CONVEYOR, LogisticsContent.FAST_CONVEYOR, LogisticsContent.SORTER, it("crystal_forge"));
		lexicon(ModContent.STORAGE_KEY, ModContent.WIRELESS_TERMINAL, it("flight_plan"), ModContent.LEATHER_BACKPACK,
				it("amethyst_draught"), it("lair_compass"), it("crystal_cocoon"));

		chapter(ModContent.AMETHYST_CORE, "lexicon_materials");
		lexicon(ModContent.AMETHYST_CORE, it("amethyst_bone"), it("resonance_treat"), ModContent.WIRELESS_CORE, it("dragon_heart"), ModContent.CRYSTAL_OBSIDIAN,
				ModContent.AMETHYST_OBSIDIAN_UPGRADE);

		chapter(ModContent.DIAMOND_RANGE_UPGRADE, "lexicon_upgrades");
		lexicon(ModContent.IRON_RANGE_UPGRADE, ModContent.IRON_CAPACITY_UPGRADE, ModContent.ENDER_RANGE_UPGRADE);

		chapter(it("amethyst_obsidian_sword"), "lexicon_gear");
		lexicon(it("amethyst_obsidian_chestplate"), it("amethyst_obsidian_pickaxe"));

		chapter(ModContent.GUIDE_BOOK, "lexicon_books");
		lexicon(ModContent.GUIDE_BOOK, ModContent.RECIPE_BOOK, ModContent.BESTIARY, ModContent.ITEM_LEXICON,
				ModContent.CRYSTAL_CHRONICLE, ModContent.JOKE_BOOK);
	}

	private void lexicon(ItemLike... items) {
		for (ItemLike like : items) {
			Item item = like.asItem();

			if (skipping || item == Items.AIR) {
				continue;
			}

			String path = BuiltInRegistries.ITEM.getKey(item).getPath();
			String tooltip = "tooltip.jtechstorage." + (path.endsWith("_backpack") ? "backpack" : path);
			MutableComponent body = Component.empty().append(tr("jtechstorage.lexicon." + path));

			if (Language.getInstance().has(tooltip + ".controls.0")) {
				body.append("\n\n").append(tr("jtechstorage.lexicon.controls").copy().withStyle(ChatFormatting.BOLD));

				for (int i = 0; Language.getInstance().has(tooltip + ".controls." + i); i++) {
					body.append("\n• ").append(tr(tooltip + ".controls." + i));
				}
			}

			ItemStack icon = new ItemStack(item);
			pages.add(new ItemPage(icon, icon.getHoverName(), body));
		}
	}

	// --- Crystal chronicle -----------------------------------------------

	private void buildChronicle() {
		info("chronicle");
		contents();

		chapter(Items.AMETHYST_CLUSTER, "chronicle_legend");
		info("legend");
		info("storm_legend");

		chapter(it("amethyst_draught"), "chronicle_bosses");
		info("boss");
		info("spider");

		chapter(ModContent.CRYSTAL_OBSIDIAN, "chronicle_obsidian");
		info("amethyst_obsidian");

		chapter(Items.ENCHANTED_BOOK, "chronicle_enchants");
		info("crystal_enchants");
		info("crystal_enchants2");

		chapter(it("dragon_heart"), "chronicle_evolutions");
		info("evolutions");

		chapter(Items.NETHER_STAR, "chronicle_roadmap");
		info("roadmap");
	}

	// --- Chel's joke book -------------------------------------------------

	private void buildJokes() {
		List<Integer> heard = book.getOrDefault(ModContent.JOKES_HEARD, List.of());
		pages.add(new InfoPage(tr("jtechstorage.joke_book.title"), tr("jtechstorage.joke_book.intro", heard.size(), GuideBookItem.JOKE_COUNT)));

		if (heard.isEmpty()) {
			return;
		}

		MutableComponent body = Component.empty();

		for (int i = 0; i < heard.size(); i++) {
			int joke = heard.get(i);

			if (i > 0) {
				body.append("\n\n");
			}

			body.append(tr("jtechstorage.joke_book.number", joke + 1).copy().withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD))
					.append("\n")
					.append(tr("jtechstorage.chel.joke." + joke));
		}

		pages.add(new InfoPage(tr("jtechstorage.joke_book.collected", heard.size(), GuideBookItem.JOKE_COUNT), body));
	}

	// --- Recipe helpers --------------------------------------------------

	private void amethystObsidian(ItemLike result, Item base) {
		if (skipping || missing(result)) {
			return;
		}

		pages.add(new SmithingPage(new ItemStack(result), ModContent.AMETHYST_OBSIDIAN_UPGRADE, base, ModContent.CRYSTAL_OBSIDIAN,
				"jtechstorage.guide.recipe.amethyst_obsidian"));
	}

	private static Item byId(String path) {
		return BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace(path));
	}

	private void crafting(ItemLike result, String descKey, @Nullable ItemLike... grid) {
		if (skipping || missing(result) || missing(grid)) {
			return;
		}

		Item[] items = new Item[9];

		for (int index = 0; index < 9; index++) {
			items[index] = grid[index] == null ? null : grid[index].asItem();
		}

		pages.add(new CraftingPage(new ItemStack(result), items, false, descKey));
	}

	private void shapeless(ItemLike result, String descKey, ItemLike... ingredients) {
		if (skipping || missing(result) || missing(ingredients)) {
			return;
		}

		Item[] items = new Item[9];

		for (int index = 0; index < ingredients.length; index++) {
			items[index] = ingredients[index].asItem();
		}

		pages.add(new CraftingPage(new ItemStack(result), items, true, descKey));
	}

	private void smithing(ItemLike result, ItemLike base, String descKey) {
		if (skipping || missing(result, base)) {
			return;
		}

		pages.add(new SmithingPage(new ItemStack(result), Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE, base.asItem(),
				Items.NETHERITE_INGOT, descKey));
	}

	// ------------------------------------------------------------------
	// Wrapping onto book pages
	// ------------------------------------------------------------------

	private void layoutPages() {
		display.clear();
		firstDisplay.clear();

		for (Page source : pages) {
			firstDisplay.put(source, display.size());

			switch (source) {
				case InfoPage info -> addText(info.title(), info.body(), null, null);
				case EntityPage entity -> addText(entity.title(), withStats(entity), null, entity.type());
				case ItemPage item -> addText(item.title(), item.body(), item.icon(), null);
				case ContentsPage list -> {
					int titleH = font.split(contentsTitle(), TEXT_WIDTH).size() * 10 + 4;
					int perPage = Math.max(1, (TEXT_BOTTOM - TEXT_Y - titleH) / LINK_HEIGHT);

					for (int start = 0; start < Math.max(1, list.links().size()); start += perPage) {
						display.add(new ContentsChunk(list.links().subList(Math.min(start, list.links().size()),
								Math.min(list.links().size(), start + perPage)), start == 0));
					}
				}
				default -> display.add(source);
			}
		}

		page = Math.clamp(page, 0, display.size() - 1);
	}

	private Component withStats(EntityPage entity) {
		LivingEntity model = model(entity.type());

		if (model == null) {
			return entity.body();
		}

		return Component.empty()
				.append(tr("jtechstorage.bestiary.health", Math.round(model.getMaxHealth())).copy().withStyle(ChatFormatting.DARK_RED))
				.append("\n\n")
				.append(entity.body());
	}

	private void addText(Component titleText, Component bodyText, @Nullable ItemStack icon, @Nullable EntityType<?> entity) {
		int titleWidth = icon != null ? TEXT_WIDTH - 20 : TEXT_WIDTH;
		List<FormattedCharSequence> title = font.split(titleText.copy().withStyle(ChatFormatting.BOLD, ChatFormatting.DARK_PURPLE), titleWidth);
		List<FormattedCharSequence> body = font.split(bodyText, TEXT_WIDTH);
		int titleH = Math.max(title.size() * 10, icon != null ? 18 : 0);
		int perPage = Math.max(3, (TEXT_BOTTOM - TEXT_Y - titleH - 4) / 9);
		int firstPage = entity != null ? Math.max(1, perPage - MODEL_HEIGHT / 9 - 1) : perPage;
		List<List<FormattedCharSequence>> chunks = new ArrayList<>();
		int index = 0;

		do {
			int count = chunks.isEmpty() ? firstPage : perPage;
			chunks.add(body.subList(index, Math.min(body.size(), index + count)));
			index += count;
		} while (index < body.size());

		for (int part = 0; part < chunks.size(); part++) {
			display.add(new TextChunk(title, chunks.get(part), part + 1, chunks.size(), icon, entity));
		}
	}

	private static Component contentsTitle() {
		return Component.translatable("jtechstorage.book.contents").withStyle(ChatFormatting.BOLD, ChatFormatting.DARK_PURPLE);
	}

	private @Nullable LivingEntity model(EntityType<?> type) {
		if (models.containsKey(type)) {
			return models.get(type);
		}

		LivingEntity living = null;

		if (minecraft != null && minecraft.level != null) {
			try {
				Entity entity = type.create(minecraft.level, EntitySpawnReason.LOAD);

				if (entity instanceof LivingEntity created) {
					// models don't exist in any world – without their own ID rendering crashes (e.g. with the sprite)
					created.setId(nextModelId--);
					living = created;
				}
			} catch (RuntimeException ignored) {
				// model is decoration only – better without it than a crash
			}
		}

		models.put(type, living);
		return living;
	}

	// ------------------------------------------------------------------
	// Setup & rendering
	// ------------------------------------------------------------------

	@Override
	protected void init() {
		left = (width - BOOK_SIZE) / 2;
		top = 2;
		layoutPages();
		addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> onClose())
				.bounds(width / 2 - 100, top + BOOK_SIZE + 4, 200, 20)
				.build());
	}

	@Override
	public void removed() {
		LAST_PAGE.put(kind, page);
		super.removed();
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractBackground(graphics, mouseX, mouseY, delta);
		graphics.blit(RenderPipelines.GUI_TEXTURED, BOOK_TEXTURE, left, top, 0, 0, BOOK_SIZE, BOOK_SIZE, 256, 256);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractRenderState(graphics, mouseX, mouseY, delta);

		Component pageNumber = Component.translatable("book.pageIndicator", page + 1, display.size());
		graphics.text(font, pageNumber, left + BOOK_SIZE - 44 - font.width(pageNumber), top + 18, 0xFF404040, false);

		if (hasContentsLink()) {
			graphics.text(font, "☰", left + TEXT_X, top + 18, isOverContentsLink(mouseX, mouseY) ? LINK_HOVER : LINK_COLOR, false);
		}

		ItemStack hovered = switch (display.get(page)) {
			case TextChunk text -> extractText(graphics, text, mouseX, mouseY);
			case ContentsChunk list -> {
				extractContents(graphics, list, mouseX, mouseY);
				yield null;
			}
			case ChapterPage chapter -> {
				extractChapter(graphics, chapter);
				yield null;
			}
			case CraftingPage crafting -> extractCrafting(graphics, crafting, mouseX, mouseY);
			case SmithingPage smithing -> extractSmithing(graphics, smithing, mouseX, mouseY);
			default -> null;
		};

		if (page > 0) {
			boolean hover = isOverBackward(mouseX, mouseY);
			graphics.blitSprite(RenderPipelines.GUI_TEXTURED, hover ? PAGE_BACKWARD_HIGHLIGHTED : PAGE_BACKWARD,
					left + 43, top + 157, 23, 13);
		}

		if (page < display.size() - 1) {
			boolean hover = isOverForward(mouseX, mouseY);
			graphics.blitSprite(RenderPipelines.GUI_TEXTURED, hover ? PAGE_FORWARD_HIGHLIGHTED : PAGE_FORWARD,
					left + 116, top + 157, 23, 13);
		}

		if (hovered != null && !hovered.isEmpty()) {
			graphics.setTooltipForNextFrame(font, hovered, mouseX, mouseY);
		}
	}

	private @Nullable ItemStack extractText(GuiGraphicsExtractor graphics, TextChunk text, int mouseX, int mouseY) {
		int x = left + TEXT_X;
		int y = top + TEXT_Y;
		ItemStack hovered = null;
		int titleX = x;

		if (text.icon() != null) {
			graphics.item(text.icon(), x, y);
			titleX += 20;

			if (isOver(x, y, mouseX, mouseY)) {
				hovered = text.icon();
			}
		}

		int titleY = y + (text.icon() != null && text.title().size() == 1 ? 4 : 0);

		for (FormattedCharSequence line : text.title()) {
			graphics.text(font, line, titleX, titleY, TEXT_COLOR, false);
			titleY += 10;
		}

		y += Math.max(text.title().size() * 10, text.icon() != null ? 18 : 0) + 4;

		if (text.entity() != null && text.part() == 1) {
			LivingEntity model = model(text.entity());

			if (model != null) {
				float height = Math.max(0.3f, model.getBbHeight());
				float widthBlocks = Math.max(0.3f, model.getBbWidth());
				int scale = (int) Math.clamp(Math.min((MODEL_HEIGHT - 6) / height, 90 / widthBlocks), 6.0f, 48.0f);
				try {
					InventoryScreen.extractEntityInInventoryFollowsMouse(graphics, x + 4, y, x + TEXT_WIDTH - 4, y + MODEL_HEIGHT,
							scale, 0.0625f, mouseX, mouseY, model);
				} catch (RuntimeException exception) {
					// model cannot be drawn – page without model instead of a crash
					models.put(text.entity(), null);
				}
			}

			y += MODEL_HEIGHT + 2;
		}

		for (FormattedCharSequence line : text.body()) {
			graphics.text(font, line, x, y, TEXT_COLOR, false);
			y += 9;
		}

		if (text.part() < text.parts()) {
			Component more = Component.translatable("jtechstorage.guide.continued");
			graphics.text(font, more, left + TEXT_X + TEXT_WIDTH - font.width(more), top + TEXT_BOTTOM - 2, 0xFF6A3A9A, false);
		}

		return hovered;
	}

	private void extractContents(GuiGraphicsExtractor graphics, ContentsChunk list, int mouseX, int mouseY) {
		int x = left + TEXT_X;
		int y = top + TEXT_Y;

		for (FormattedCharSequence line : font.split(contentsTitle(), TEXT_WIDTH)) {
			graphics.text(font, line, x, y, TEXT_COLOR, false);
			y += 10;
		}

		y += 4;

		for (Link link : list.links()) {
			boolean hover = mouseX >= x && mouseX < x + TEXT_WIDTH && mouseY >= y - 1 && mouseY < y + LINK_HEIGHT - 1;
			Integer target = firstDisplay.get(link.target());
			String label = GuiTheme.fit(font, "› " + link.label().getString(), TEXT_WIDTH - 18);
			graphics.text(font, label, x, y, hover ? LINK_HOVER : LINK_COLOR, false);

			if (target != null) {
				String number = String.valueOf(target + 1);
				graphics.text(font, number, x + TEXT_WIDTH - font.width(number), y, 0xFF606060, false);
			}

			y += LINK_HEIGHT;
		}
	}

	private void extractChapter(GuiGraphicsExtractor graphics, ChapterPage chapter) {
		int x = left + TEXT_X;
		int mid = x + TEXT_WIDTH / 2;
		int y = top + TEXT_Y + 6;

		graphics.pose().pushMatrix();
		graphics.pose().translate(mid - 16, y);
		graphics.pose().scale(2.0f, 2.0f);
		graphics.item(chapter.icon(), 0, 0);
		graphics.pose().popMatrix();
		y += 40;

		for (FormattedCharSequence line : font.split(chapter.title().copy().withStyle(ChatFormatting.BOLD, ChatFormatting.DARK_PURPLE), TEXT_WIDTH)) {
			graphics.text(font, line, mid - font.width(line) / 2, y, TEXT_COLOR, false);
			y += 10;
		}

		graphics.fill(mid - 30, y + 2, mid + 30, y + 3, 0xFF9A6AC8);
		y += 8;

		for (FormattedCharSequence line : font.split(chapter.intro(), TEXT_WIDTH)) {
			graphics.text(font, line, mid - font.width(line) / 2, y, 0xFF303030, false);
			y += 9;
		}
	}

	/** Title (bold, up to 2 lines) – returns the height. */
	private int extractTitle(GuiGraphicsExtractor graphics, Component title, int x, int y) {
		List<FormattedCharSequence> lines = font.split(title.copy().withStyle(ChatFormatting.BOLD), TEXT_WIDTH);
		int count = Math.min(2, lines.size());

		for (int i = 0; i < count; i++) {
			graphics.text(font, lines.get(i), x, y + i * 10, TEXT_COLOR, false);
		}

		return count * 10;
	}

	private @Nullable ItemStack extractCrafting(GuiGraphicsExtractor graphics, CraftingPage recipe, int mouseX, int mouseY) {
		int x = left + TEXT_X;
		int y = top + TEXT_Y;
		ItemStack hovered = null;

		int titleH = extractTitle(graphics, recipe.result().getHoverName(), x, y);

		int gridX = x + 2;
		int gridY = y + titleH + 6;

		for (int index = 0; index < 9; index++) {
			int sx = gridX + (index % 3) * 18;
			int sy = gridY + (index / 3) * 18;
			extractSlot(graphics, sx, sy);
			Item item = recipe.grid()[index];

			if (item != null) {
				ItemStack stack = new ItemStack(item);
				graphics.item(stack, sx, sy);

				if (isOver(sx, sy, mouseX, mouseY)) {
					hovered = stack;
				}
			}
		}

		graphics.text(font, "→", gridX + 60, gridY + 22, 0xFF404040, false);
		int resultX = gridX + 76;
		int resultY = gridY + 18;
		extractSlot(graphics, resultX, resultY);
		graphics.item(recipe.result(), resultX, resultY);
		graphics.itemDecorations(font, recipe.result(), resultX, resultY);

		if (isOver(resultX, resultY, mouseX, mouseY)) {
			hovered = recipe.result();
		}

		int textY = gridY + 58;

		if (recipe.shapeless()) {
			graphics.text(font, Component.translatable("jtechstorage.guide.shapeless").withStyle(ChatFormatting.ITALIC),
					x, textY, 0xFF404040, false);
			textY += 11;
		}

		graphics.textWithWordWrap(font, Component.translatable(recipe.descKey()), x, textY, TEXT_WIDTH, TEXT_COLOR, false);
		return hovered;
	}

	private @Nullable ItemStack extractSmithing(GuiGraphicsExtractor graphics, SmithingPage recipe, int mouseX, int mouseY) {
		int x = left + TEXT_X;
		int y = top + TEXT_Y;
		ItemStack hovered = null;

		int titleH = extractTitle(graphics, recipe.result().getHoverName(), x, y);
		graphics.text(font, Component.translatable("jtechstorage.guide.smithing").withStyle(ChatFormatting.ITALIC),
				x, y + titleH + 2, 0xFF404040, false);

		int rowY = y + titleH + 20;
		Item[] inputs = {recipe.template(), recipe.base(), recipe.addition()};

		for (int index = 0; index < inputs.length; index++) {
			int sx = x + 2 + index * 20;
			extractSlot(graphics, sx, rowY);
			ItemStack stack = new ItemStack(inputs[index]);
			graphics.item(stack, sx, rowY);

			if (isOver(sx, rowY, mouseX, mouseY)) {
				hovered = stack;
			}
		}

		graphics.text(font, "→", x + 66, rowY + 4, 0xFF404040, false);
		int resultX = x + 80;
		extractSlot(graphics, resultX, rowY);
		graphics.item(recipe.result(), resultX, rowY);

		if (isOver(resultX, rowY, mouseX, mouseY)) {
			hovered = recipe.result();
		}

		graphics.textWithWordWrap(font, Component.translatable(recipe.descKey()), x, rowY + 26, TEXT_WIDTH, TEXT_COLOR, false);
		return hovered;
	}

	private static void extractSlot(GuiGraphicsExtractor graphics, int x, int y) {
		graphics.fill(x - 1, y - 1, x + 17, y + 17, 0xFF8B8B8B);
		graphics.fill(x - 1, y - 1, x + 16, y, 0xFF373737);
		graphics.fill(x - 1, y - 1, x, y + 16, 0xFF373737);
		graphics.fill(x, y + 16, x + 17, y + 17, 0xFFFFFFFF);
		graphics.fill(x + 16, y, x + 17, y + 17, 0xFFFFFFFF);
	}

	private static boolean isOver(int x, int y, double mouseX, double mouseY) {
		return mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16;
	}

	// ------------------------------------------------------------------
	// Page turning & links
	// ------------------------------------------------------------------

	private boolean hasContentsLink() {
		return contents != null && !(display.get(page) instanceof ContentsChunk);
	}

	private boolean isOverContentsLink(double mouseX, double mouseY) {
		return mouseX >= left + TEXT_X - 2 && mouseX < left + TEXT_X + 10 && mouseY >= top + 16 && mouseY < top + 27;
	}

	private boolean isOverBackward(double mouseX, double mouseY) {
		return mouseX >= left + 43 && mouseX < left + 66 && mouseY >= top + 157 && mouseY < top + 170;
	}

	private boolean isOverForward(double mouseX, double mouseY) {
		return mouseX >= left + 116 && mouseX < left + 139 && mouseY >= top + 157 && mouseY < top + 170;
	}

	private void turnTo(int target) {
		page = Math.clamp(target, 0, display.size() - 1);
		playPageSound();
	}

	private void playPageSound() {
		if (minecraft != null) {
			minecraft.getSoundManager().play(SimpleSoundInstance.forUI(
					SoundEvents.BOOK_PAGE_TURN, 1.0f));
		}
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		double mouseX = event.x();
		double mouseY = event.y();

		if (page > 0 && isOverBackward(mouseX, mouseY)) {
			turnTo(page - 1);
			return true;
		}

		if (page < display.size() - 1 && isOverForward(mouseX, mouseY)) {
			turnTo(page + 1);
			return true;
		}

		if (hasContentsLink() && isOverContentsLink(mouseX, mouseY)) {
			turnTo(firstDisplay.getOrDefault(contents, 0));
			return true;
		}

		if (display.get(page) instanceof ContentsChunk list) {
			int x = left + TEXT_X;
			int y = top + TEXT_Y + font.split(contentsTitle(), TEXT_WIDTH).size() * 10 + 4;

			for (Link link : list.links()) {
				if (mouseX >= x && mouseX < x + TEXT_WIDTH && mouseY >= y - 1 && mouseY < y + LINK_HEIGHT - 1) {
					turnTo(firstDisplay.getOrDefault(link.target(), page));
					return true;
				}

				y += LINK_HEIGHT;
			}
		}

		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		page = Math.clamp(page - (int) Math.signum(scrollY), 0, display.size() - 1);
		return true;
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (event.isLeft() && page > 0) {
			turnTo(page - 1);
			return true;
		}

		if (event.isRight() && page < display.size() - 1) {
			turnTo(page + 1);
			return true;
		}

		return super.keyPressed(event);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
