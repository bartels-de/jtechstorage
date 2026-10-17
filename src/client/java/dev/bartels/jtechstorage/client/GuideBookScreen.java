package dev.bartels.jtechstorage.client;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.resources.Identifier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;

import dev.bartels.jtechstorage.ModContent;
import dev.bartels.jtechstorage.end.EndContent;
import dev.bartels.jtechstorage.ModGuide;

/**
 * Speicher-Handbuch: Begrüßung, kurze Anleitung und alle Rezepte der Mod –
 * im Stil eines Vanilla-Buchs, Rezepte mit echten Item-Icons und Tooltips.
 */
public class GuideBookScreen extends Screen {
	private static final Identifier BOOK_TEXTURE = Identifier.withDefaultNamespace("textures/gui/book.png");
	private static final Identifier PAGE_FORWARD = Identifier.withDefaultNamespace("widget/page_forward");
	private static final Identifier PAGE_FORWARD_HIGHLIGHTED = Identifier.withDefaultNamespace("widget/page_forward_highlighted");
	private static final Identifier PAGE_BACKWARD = Identifier.withDefaultNamespace("widget/page_backward");
	private static final Identifier PAGE_BACKWARD_HIGHLIGHTED = Identifier.withDefaultNamespace("widget/page_backward_highlighted");

	private static final int BOOK_SIZE = 192;
	private static final int TEXT_X = 36;
	private static final int TEXT_Y = 30;
	private static final int TEXT_WIDTH = 114;
	private static final int TEXT_COLOR = 0xFF000000;

	private final List<Page> pages = new ArrayList<>();
	/** Angezeigte Seiten: lange Texte werden automatisch auf mehrere Buchseiten verteilt. */
	private final List<Object> display = new ArrayList<>();
	/** Textbereich einer Seite (bis knapp über die Blätter-Pfeile). */
	private static final int TEXT_BOTTOM = 152;
	private int page;
	private int left;
	private int top;

	public GuideBookScreen() {
		super(Component.translatable("item.jtechstorage.guide_book"));
		buildPages();
	}

	// ------------------------------------------------------------------
	// Inhalt
	// ------------------------------------------------------------------

	private sealed interface Page permits InfoPage, CraftingPage, SmithingPage {
	}

	private record InfoPage(String titleKey, String bodyKey, Object... args) implements Page {
	}

	/** 3×3-Rezept; {@code grid} hat 9 Einträge (null = leer). */
	private record CraftingPage(ItemStack result, @Nullable Item[] grid, boolean shapeless, String descKey) implements Page {
	}

	private record SmithingPage(ItemStack result, Item template, Item base, Item addition, String descKey) implements Page {
	}

	/** Ein Stück einer Infoseite (Titel wird auf jeder Fortsetzungsseite wiederholt). */
	private record InfoChunk(List<FormattedCharSequence> title, List<FormattedCharSequence> body, int part, int parts) {
	}

	private void layoutPages() {
		display.clear();

		for (Page source : pages) {
			if (!(source instanceof InfoPage info)) {
				display.add(source);
				continue;
			}

			List<FormattedCharSequence> title = font.split(
					Component.translatable(info.titleKey()).withStyle(ChatFormatting.BOLD, ChatFormatting.DARK_PURPLE), TEXT_WIDTH);
			List<FormattedCharSequence> body = font.split(Component.translatable(info.bodyKey(), info.args()), TEXT_WIDTH);
			int perPage = Math.max(3, (TEXT_BOTTOM - TEXT_Y - title.size() * 10 - 4) / 9);
			int parts = Math.max(1, (body.size() + perPage - 1) / perPage);

			for (int part = 0; part < parts; part++) {
				List<FormattedCharSequence> lines = body.subList(part * perPage, Math.min(body.size(), (part + 1) * perPage));
				display.add(new InfoChunk(title, lines, part + 1, parts));
			}
		}

		page = Math.clamp(page, 0, display.size() - 1);
	}

	private void buildPages() {
		pages.add(new InfoPage("jtechstorage.guide.welcome.title", "jtechstorage.guide.welcome.body", ModGuide.AUTHOR));
		pages.add(new InfoPage("jtechstorage.guide.contents.title", "jtechstorage.guide.contents.body"));
		pages.add(new InfoPage("jtechstorage.guide.start.title", "jtechstorage.guide.start.body"));
		pages.add(new InfoPage("jtechstorage.guide.terminal.title", "jtechstorage.guide.terminal.body"));
		dev.bartels.jtechstorage.ModConfig.Network net = dev.bartels.jtechstorage.ModConfig.get().network;
		pages.add(new InfoPage("jtechstorage.guide.upgrades.title", "jtechstorage.guide.upgrades.body",
				net.baseRange, net.ironRange, net.goldRange, net.diamondRange, net.netheriteRange, net.enderRange,
				net.baseCapacity, net.ironCapacity, net.goldCapacity, net.diamondCapacity, net.netheriteCapacity));
		pages.add(new InfoPage("jtechstorage.guide.sorting.title", "jtechstorage.guide.sorting.body"));
		pages.add(new InfoPage("jtechstorage.guide.interface.title", "jtechstorage.guide.interface.body"));
		pages.add(new InfoPage("jtechstorage.guide.friends.title", "jtechstorage.guide.friends.body"));
		pages.add(new InfoPage("jtechstorage.guide.trade.title", "jtechstorage.guide.trade.body"));
		pages.add(new InfoPage("jtechstorage.guide.boss.title", "jtechstorage.guide.boss.body"));
		pages.add(new InfoPage("jtechstorage.guide.spider.title", "jtechstorage.guide.spider.body"));
		pages.add(new InfoPage("jtechstorage.guide.amethyst_obsidian.title", "jtechstorage.guide.amethyst_obsidian.body"));
		pages.add(new InfoPage("jtechstorage.guide.sprite.title", "jtechstorage.guide.sprite.body"));
		pages.add(new InfoPage("jtechstorage.guide.moth.title", "jtechstorage.guide.moth.body"));
		pages.add(new InfoPage("jtechstorage.guide.griffin.title", "jtechstorage.guide.griffin.body"));
		pages.add(new InfoPage("jtechstorage.guide.beetle.title", "jtechstorage.guide.beetle.body"));
		pages.add(new InfoPage("jtechstorage.guide.chel.title", "jtechstorage.guide.chel.body"));
		pages.add(new InfoPage("jtechstorage.guide.peter.title", "jtechstorage.guide.peter.body"));
		pages.add(new InfoPage("jtechstorage.guide.ashley.title", "jtechstorage.guide.ashley.body"));
		pages.add(new InfoPage("jtechstorage.guide.rest.title", "jtechstorage.guide.rest.body"));
		pages.add(new InfoPage("jtechstorage.guide.cosmetics.title", "jtechstorage.guide.cosmetics.body"));
		pages.add(new InfoPage("jtechstorage.guide.fox.title", "jtechstorage.guide.fox.body"));
		pages.add(new InfoPage("jtechstorage.guide.crystal_enchants.title", "jtechstorage.guide.crystal_enchants.body"));
		pages.add(new InfoPage("jtechstorage.guide.phoenix.title", "jtechstorage.guide.phoenix.body"));
		pages.add(new InfoPage("jtechstorage.guide.amethyst_phoenix.title", "jtechstorage.guide.amethyst_phoenix.body"));
		pages.add(new InfoPage("jtechstorage.guide.backpack.title", "jtechstorage.guide.backpack.body"));
		pages.add(new InfoPage("jtechstorage.guide.recipes.title", "jtechstorage.guide.recipes.body"));

		// identisch mit den Rezept-JSONs
		crafting(ModContent.STORAGE_TERMINAL_ITEM, "jtechstorage.guide.recipe.storage_terminal",
				Items.POLISHED_DEEPSLATE, Items.AMETHYST_BLOCK, Items.POLISHED_DEEPSLATE, Items.REDSTONE_BLOCK, Items.CHEST, Items.REDSTONE_BLOCK, Items.POLISHED_DEEPSLATE, Items.DIAMOND, Items.POLISHED_DEEPSLATE);
		shapeless(ModContent.CRAFTING_TERMINAL_ITEM, "jtechstorage.guide.recipe.crafting_terminal",
				ModContent.STORAGE_TERMINAL_ITEM, Items.CRAFTER);
		crafting(ModContent.TEAM_TERMINAL_ITEM, "jtechstorage.guide.recipe.team_terminal",
				Items.GOLD_INGOT, Items.BELL, Items.GOLD_INGOT, Items.AMETHYST_BLOCK, Items.LECTERN, Items.AMETHYST_BLOCK, Items.POLISHED_DEEPSLATE, Items.REDSTONE_BLOCK, Items.POLISHED_DEEPSLATE);
		crafting(ModContent.TRADE_TERMINAL_ITEM, "jtechstorage.guide.recipe.trade_terminal",
				Items.POLISHED_DEEPSLATE, Items.EMERALD_BLOCK, Items.POLISHED_DEEPSLATE, Items.AMETHYST_BLOCK, Items.BARREL, Items.AMETHYST_BLOCK, Items.POLISHED_DEEPSLATE, Items.REDSTONE_BLOCK, Items.POLISHED_DEEPSLATE);
		crafting(ModContent.PHOENIX_NEST_ITEM, "jtechstorage.guide.recipe.phoenix_nest",
				Items.STICK, Items.AMETHYST_BLOCK, Items.STICK, Items.HAY_BLOCK, Items.BARREL, Items.HAY_BLOCK, Items.STICK, Items.STICK, Items.STICK);
		crafting(ModContent.PHOENIX_ITEM, "jtechstorage.guide.recipe.phoenix",
				Items.BLAZE_ROD, Items.GOLD_BLOCK, Items.BLAZE_ROD, Items.FEATHER, Items.TOTEM_OF_UNDYING, Items.FEATHER, Items.BLAZE_ROD, ModContent.AMETHYST_CORE, Items.BLAZE_ROD);
		crafting(ModContent.AMETHYST_FOX_ITEM, "jtechstorage.guide.recipe.amethyst_fox",
				Items.AMETHYST_BLOCK, Items.GLOW_BERRIES, Items.AMETHYST_BLOCK, Items.LAPIS_BLOCK, ModContent.AMETHYST_CORE, Items.LAPIS_BLOCK, Items.AMETHYST_BLOCK, Items.BOOK, Items.AMETHYST_BLOCK);
		crafting(ModContent.STORAGE_KEY, "jtechstorage.guide.recipe.storage_key",
				null, Items.AMETHYST_SHARD, null, null, Items.ENDER_PEARL, null, null, Items.GOLD_INGOT, null);
		crafting(ModContent.WIRELESS_TERMINAL, "jtechstorage.guide.recipe.wireless_terminal",
				Items.AMETHYST_BLOCK, Items.ENDER_EYE, Items.AMETHYST_BLOCK, Items.POLISHED_DEEPSLATE, Items.DIAMOND, Items.POLISHED_DEEPSLATE, Items.POLISHED_DEEPSLATE, Items.REDSTONE_BLOCK, Items.POLISHED_DEEPSLATE);
		crafting(ModContent.AMETHYST_SPRITE_ITEM, "jtechstorage.guide.recipe.amethyst_sprite",
				Items.AMETHYST_BLOCK, Items.ENDER_EYE, Items.AMETHYST_BLOCK, Items.AMETHYST_BLOCK, ModContent.AMETHYST_CORE, Items.AMETHYST_BLOCK, Items.AMETHYST_BLOCK, Items.REDSTONE_BLOCK, Items.AMETHYST_BLOCK);
		crafting(ModContent.COLLECTOR_MOTH_ITEM, "jtechstorage.guide.recipe.collector_moth",
				Items.PHANTOM_MEMBRANE, Items.SPYGLASS, Items.PHANTOM_MEMBRANE, Items.GLOW_INK_SAC, ModContent.AMETHYST_CORE, Items.GLOW_INK_SAC, Items.PHANTOM_MEMBRANE, Items.LANTERN, Items.PHANTOM_MEMBRANE);
		crafting(ModContent.STORAGE_GRIFFIN_ITEM, "jtechstorage.guide.recipe.storage_griffin",
				Items.PHANTOM_MEMBRANE, ModContent.AMETHYST_CORE, Items.PHANTOM_MEMBRANE, Items.FEATHER, Items.ENDER_CHEST, Items.FEATHER, Items.FEATHER, Items.SADDLE, Items.FEATHER);
		crafting(ModContent.SPARK_BEETLE_ITEM, "jtechstorage.guide.recipe.spark_beetle",
				Items.REDSTONE_BLOCK, byId("lightning_rod"), Items.REDSTONE_BLOCK, byId("copper_block"), ModContent.AMETHYST_CORE, byId("copper_block"), byId("copper_block"), Items.AMETHYST_BLOCK, byId("copper_block"));
		crafting(ModContent.CHEL_ITEM, "jtechstorage.guide.recipe.chel",
				Items.AMETHYST_BLOCK, ModContent.AMETHYST_CORE, Items.AMETHYST_BLOCK, Items.COOKIE, Items.CRAFTER, Items.COOKIE, Items.POLISHED_DEEPSLATE, Items.REDSTONE_BLOCK, Items.POLISHED_DEEPSLATE);
		crafting(ModContent.ASHLEY_ITEM, "jtechstorage.guide.recipe.ashley",
				Items.COAL_BLOCK, ModContent.AMETHYST_CORE, Items.COAL_BLOCK, Items.MUD, Items.CAMPFIRE, Items.MUD, Items.COAL_BLOCK, Items.MUD, Items.COAL_BLOCK);
		crafting(ModContent.LEATHER_BACKPACK, "jtechstorage.guide.recipe.backpack",
				Items.LEATHER, Items.STRING, Items.LEATHER, Items.LEATHER, Items.CHEST, Items.LEATHER, Items.LEATHER, Items.LEATHER, Items.LEATHER);
		shapeless(ModContent.IRON_BACKPACK, "jtechstorage.guide.recipe.backpack", ModContent.LEATHER_BACKPACK, Items.IRON_BLOCK);
		shapeless(ModContent.GOLD_BACKPACK, "jtechstorage.guide.recipe.backpack", ModContent.IRON_BACKPACK, Items.GOLD_BLOCK);
		shapeless(ModContent.DIAMOND_BACKPACK, "jtechstorage.guide.recipe.backpack", ModContent.GOLD_BACKPACK, Items.DIAMOND_BLOCK);
		smithing(ModContent.NETHERITE_BACKPACK, ModContent.DIAMOND_BACKPACK, "jtechstorage.guide.recipe.backpack");
		shapeless(ModContent.WIRELESS_TERMINAL, "jtechstorage.guide.recipe.wireless_core",
				ModContent.WIRELESS_TERMINAL, ModContent.WIRELESS_CORE);
		crafting(ModContent.AMETHYST_DRAUGHT, "jtechstorage.guide.recipe.amethyst_draught",
				Items.AMETHYST_BLOCK, Items.GHAST_TEAR, Items.AMETHYST_BLOCK, Items.DIAMOND, Items.GLASS_BOTTLE, Items.DIAMOND,
				Items.AMETHYST_BLOCK, Items.REDSTONE_BLOCK, Items.AMETHYST_BLOCK);
		shapeless(ModContent.GUIDE_BOOK, "jtechstorage.guide.recipe.guide_book", Items.BOOK, Items.AMETHYST_SHARD);
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
		shapeless(ModContent.ROUTE_FEATHER, "jtechstorage.guide.recipe.route_feather", Items.FEATHER, Items.AMETHYST_SHARD, Items.ENDER_PEARL);
		crafting(EndContent.LAIR_COMPASS, "jtechstorage.guide.recipe.lair_compass",
				Items.AMETHYST_SHARD, Items.ENDER_EYE, Items.AMETHYST_SHARD, Items.POPPED_CHORUS_FRUIT, Items.COMPASS, Items.POPPED_CHORUS_FRUIT,
				Items.AMETHYST_SHARD, Items.ENDER_EYE, Items.AMETHYST_SHARD);
		crafting(EndContent.CRYSTAL_COCOON, "jtechstorage.guide.recipe.crystal_cocoon",
				Items.STRING, Items.AMETHYST_BLOCK, Items.STRING, Items.CRYING_OBSIDIAN, Items.FERMENTED_SPIDER_EYE, Items.CRYING_OBSIDIAN,
				Items.STRING, Items.ENDER_EYE, Items.STRING);
		crafting(EndContent.CRYSTAL_OBSIDIAN, "jtechstorage.guide.recipe.crystal_obsidian",
				Items.CRYING_OBSIDIAN, Items.AMETHYST_BLOCK, Items.CRYING_OBSIDIAN, Items.AMETHYST_BLOCK, Items.NETHERITE_SCRAP, Items.AMETHYST_BLOCK,
				Items.CRYING_OBSIDIAN, Items.AMETHYST_BLOCK, Items.CRYING_OBSIDIAN);
		amethystObsidian(EndContent.AMETHYST_OBSIDIAN_SWORD, Items.NETHERITE_SWORD);
		amethystObsidian(EndContent.AMETHYST_OBSIDIAN_PICKAXE, Items.NETHERITE_PICKAXE);
		amethystObsidian(EndContent.AMETHYST_OBSIDIAN_AXE, Items.NETHERITE_AXE);
		amethystObsidian(EndContent.AMETHYST_OBSIDIAN_SHOVEL, Items.NETHERITE_SHOVEL);
		amethystObsidian(EndContent.AMETHYST_OBSIDIAN_HOE, Items.NETHERITE_HOE);
		amethystObsidian(EndContent.AMETHYST_OBSIDIAN_HELMET, Items.NETHERITE_HELMET);
		amethystObsidian(EndContent.AMETHYST_OBSIDIAN_CHESTPLATE, Items.NETHERITE_CHESTPLATE);
		amethystObsidian(EndContent.AMETHYST_OBSIDIAN_LEGGINGS, Items.NETHERITE_LEGGINGS);
		amethystObsidian(EndContent.AMETHYST_OBSIDIAN_BOOTS, Items.NETHERITE_BOOTS);
	}

	private void amethystObsidian(ItemLike result, Item base) {
		pages.add(new SmithingPage(new ItemStack(result), EndContent.AMETHYST_OBSIDIAN_UPGRADE, base, EndContent.CRYSTAL_OBSIDIAN,
				"jtechstorage.guide.recipe.amethyst_obsidian"));
	}

	private static Item byId(String path) {
		return BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace(path));
	}

	private void crafting(ItemLike result, String descKey, @Nullable ItemLike... grid) {
		Item[] items = new Item[9];

		for (int index = 0; index < 9; index++) {
			items[index] = grid[index] == null ? null : grid[index].asItem();
		}

		pages.add(new CraftingPage(new ItemStack(result), items, false, descKey));
	}

	private void shapeless(ItemLike result, String descKey, ItemLike... ingredients) {
		Item[] items = new Item[9];

		for (int index = 0; index < ingredients.length; index++) {
			items[index] = ingredients[index].asItem();
		}

		pages.add(new CraftingPage(new ItemStack(result), items, true, descKey));
	}

	private void smithing(ItemLike result, ItemLike base, String descKey) {
		pages.add(new SmithingPage(new ItemStack(result), Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE, base.asItem(),
				Items.NETHERITE_INGOT, descKey));
	}

	// ------------------------------------------------------------------
	// Aufbau & Rendering
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
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractBackground(graphics, mouseX, mouseY, delta);
		graphics.blit(RenderPipelines.GUI_TEXTURED, BOOK_TEXTURE, left, top, 0, 0, BOOK_SIZE, BOOK_SIZE, 256, 256);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractRenderState(graphics, mouseX, mouseY, delta);

		Component pageNumber = Component.translatable("book.pageIndicator", page + 1, display.size());
		graphics.text(font, pageNumber, left + BOOK_SIZE - 44 - font.width(pageNumber), top + 18, 0xFF404040, false);

		ItemStack hovered = switch (display.get(page)) {
			case InfoChunk info -> {
				extractInfo(graphics, info);
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

	private void extractInfo(GuiGraphicsExtractor graphics, InfoChunk info) {
		int x = left + TEXT_X;
		int y = top + TEXT_Y;

		for (FormattedCharSequence line : info.title()) {
			graphics.text(font, line, x, y, TEXT_COLOR, false);
			y += 10;
		}

		y += 4;

		for (FormattedCharSequence line : info.body()) {
			graphics.text(font, line, x, y, TEXT_COLOR, false);
			y += 9;
		}

		if (info.part() < info.parts()) {
			Component more = Component.translatable("jtechstorage.guide.continued");
			graphics.text(font, more, left + TEXT_X + TEXT_WIDTH - font.width(more), top + TEXT_BOTTOM - 2, 0xFF6A3A9A, false);
		}
	}

	/** Titel (fett, bis zu 2 Zeilen) – gibt die Höhe zurück. */
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
	// Blättern
	// ------------------------------------------------------------------

	private boolean isOverBackward(double mouseX, double mouseY) {
		return mouseX >= left + 43 && mouseX < left + 66 && mouseY >= top + 157 && mouseY < top + 170;
	}

	private boolean isOverForward(double mouseX, double mouseY) {
		return mouseX >= left + 116 && mouseX < left + 139 && mouseY >= top + 157 && mouseY < top + 170;
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (page > 0 && isOverBackward(event.x(), event.y())) {
			page--;
			return true;
		}

		if (page < display.size() - 1 && isOverForward(event.x(), event.y())) {
			page++;
			return true;
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
			page--;
			return true;
		}

		if (event.isRight() && page < display.size() - 1) {
			page++;
			return true;
		}

		return super.keyPressed(event);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
