package dev.bartels.jtechstorage.client.gui;

import java.util.Locale;

import org.jspecify.annotations.Nullable;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Oberflächen-Einstellungen (neu in v0.29): Design-Kacheln, Akzentfarben, Effekte (Voreinstellung + einzeln),
 * Terminal-Größe und Zahlengröße – rechts eine Live-Vorschau. Öffnet danach den vorherigen Screen.
 */
public class GuiSettingsScreen extends ThemedScreen {
	private static final int W = 300;
	private static final int H = 228;
	private static final int CHIP_W = 52;
	private static final int CHIP_H = 14;
	private static final int DOT = 12;
	private static final int ROW_H = 11;
	private static final int RIGHT_W = 108;

	/** Zwei Spalten: links hell, rechts dunkel. */
	private static final GuiSettings.Theme[] THEMES = {GuiSettings.Theme.LIGHT, GuiSettings.Theme.DARK, GuiSettings.Theme.FROST,
			GuiSettings.Theme.GLASS, GuiSettings.Theme.MODERN_LIGHT, GuiSettings.Theme.MODERN_DARK};
	private static final int THEME_W = 82;
	private static final GuiSettings.Accent[] ACCENTS = GuiSettings.Accent.values();
	private static final GuiSettings.Effects[] PRESETS = {GuiSettings.Effects.OFF, GuiSettings.Effects.SUBTLE, GuiSettings.Effects.RICH};
	private static final String[] FX = {"fade_in", "hover_glow", "border_shimmer", "sheen", "particles", "count_animation"};
	private static final ItemStack[] PREVIEW_ITEMS = {new ItemStack(Items.AMETHYST_SHARD, 64), new ItemStack(Items.IRON_INGOT, 32),
			new ItemStack(Items.DIAMOND, 7), new ItemStack(Items.GRASS_BLOCK, 48), ItemStack.EMPTY, new ItemStack(Items.REDSTONE, 12),
			new ItemStack(Items.TORCH, 16), ItemStack.EMPTY, new ItemStack(Items.APPLE, 3), new ItemStack(Items.GOLD_INGOT, 21)};

	private final @Nullable Screen parent;

	public GuiSettingsScreen(@Nullable Screen parent) {
		super(Component.translatable("jtechstorage.gui.settings"));
		this.parent = parent;
	}

	private int left() {
		return (width - W) / 2;
	}

	private int top() {
		return (height - H) / 2;
	}

	// ------------------------------------------------------------------
	// Aufteilung
	// ------------------------------------------------------------------

	private int themeX(int i) {
		return left() + 10 + (i % 2) * (THEME_W + 4);
	}

	private int themeY(int i) {
		return top() + 34 + (i / 2) * (CHIP_H + 3);
	}

	private int accentX(int i) {
		return left() + 10 + i * (DOT + 4);
	}

	private int accentY() {
		return top() + 101;
	}

	private int presetX(int i) {
		return left() + 10 + i * (CHIP_W + 3);
	}

	private int presetY() {
		return top() + 135;
	}

	private int fxY(int i) {
		return top() + 154 + i * ROW_H;
	}

	private int rightX() {
		return left() + W - 10 - RIGHT_W;
	}

	private int rowsY() {
		return top() + 140;
	}

	private int countY() {
		return top() + 158;
	}

	private int doneY() {
		return top() + H - 24;
	}

	private static boolean over(int x, int y, int w, int h, double mx, double my) {
		return mx >= x && mx < x + w && my >= y && my < y + h;
	}

	// ------------------------------------------------------------------
	// Zeichnen
	// ------------------------------------------------------------------

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractRenderState(graphics, mouseX, mouseY, delta);
		GuiSettings s = GuiSettings.get();
		int x = left();
		int y = top();
		GuiTheme.window(graphics, x, y, W, H);
		GuiTheme.title(graphics, font, title, x + 10, y + 8);

		// Design
		section(graphics, "design", x + 10, y + 23);

		for (int i = 0; i < THEMES.length; i++) {
			GuiSettings.Theme theme = THEMES[i];
			boolean selected = s.theme == theme;
			boolean hover = over(themeX(i), themeY(i), THEME_W, CHIP_H, mouseX, mouseY);
			chip(graphics, themeX(i), themeY(i), THEME_W, Component.translatable("jtechstorage.gui.theme." + key(theme)), selected, hover);
		}

		// Akzentfarbe
		section(graphics, "accent", x + 10, accentY() - 11);

		for (int i = 0; i < ACCENTS.length; i++) {
			GuiSettings.Accent accent = ACCENTS[i];
			int color = accent == GuiSettings.Accent.TEAM ? GuiTheme.accentOf(accent) : accent.color;
			int dx = accentX(i);
			int dy = accentY();

			if (s.accent == accent) {
				GuiTheme.roundOutline(graphics, dx - 2, dy - 2, DOT + 4, DOT + 4, 2, GuiTheme.text());
			}

			GuiTheme.round(graphics, dx, dy, DOT, DOT, 2, color);

			if (accent == GuiSettings.Accent.TEAM) {
				graphics.text(font, "T", dx + 4, dy + 2, GuiTheme.onColor(color), false);
			}
		}

		// Effekte
		section(graphics, "effects", x + 10, presetY() - 11);
		GuiSettings.Effects current = s.effects();

		for (int i = 0; i < PRESETS.length; i++) {
			boolean hover = over(presetX(i), presetY(), CHIP_W, CHIP_H, mouseX, mouseY);
			chip(graphics, presetX(i), presetY(), CHIP_W, Component.translatable("jtechstorage.gui.effects." + key(PRESETS[i])),
					current == PRESETS[i], hover);
		}

		if (current == GuiSettings.Effects.CUSTOM) {
			Component heading = Component.translatable("jtechstorage.gui.section.effects");
			graphics.text(font, Component.translatable("jtechstorage.gui.effects.custom"), x + 14 + font.width(heading), presetY() - 11,
					GuiTheme.accentText(), false);
		}

		boolean[] values = values(s);

		for (int i = 0; i < FX.length; i++) {
			int ry = fxY(i);
			boolean hover = over(x + 10, ry, 160, ROW_H, mouseX, mouseY);
			checkbox(graphics, x + 10, ry + 1, values[i], hover);
			graphics.text(font, Component.translatable("jtechstorage.gui.fx." + FX[i]), x + 23, ry + 1, hover ? GuiTheme.accentText() : GuiTheme.text(), false);
		}

		// rechte Spalte: Vorschau, Terminal-Größe, Zahlengröße, Fertig
		preview(graphics, rightX(), y + 23, mouseX, mouseY);
		Component rows = Component.translatable("jtechstorage.gui.rows.short",
				Component.translatable("jtechstorage.gui.rows." + s.rows.name().toLowerCase(Locale.ROOT)));
		Component count = Component.translatable("jtechstorage.gui.count.short",
				Component.translatable("jtechstorage.gui.count." + s.countSize.name().toLowerCase(Locale.ROOT)));
		chip(graphics, rightX(), rowsY(), RIGHT_W, rows, false, over(rightX(), rowsY(), RIGHT_W, CHIP_H, mouseX, mouseY));
		chip(graphics, rightX(), countY(), RIGHT_W, count, false, over(rightX(), countY(), RIGHT_W, CHIP_H, mouseX, mouseY));
		graphics.textWithWordWrap(font, Component.translatable("jtechstorage.gui.rows.hint"), rightX(), countY() + 18, RIGHT_W,
				GuiTheme.subtle(), false);
		GuiTheme.button(graphics, font, rightX(), doneY(), RIGHT_W, 16, CommonComponents.GUI_DONE, true,
				over(rightX(), doneY(), RIGHT_W, 16, mouseX, mouseY));

		tooltips(graphics, mouseX, mouseY);
	}

	private void section(GuiGraphicsExtractor graphics, String key, int x, int y) {
		graphics.text(font, Component.translatable("jtechstorage.gui.section." + key), x, y, GuiTheme.subtle(), false);
	}

	private void chip(GuiGraphicsExtractor graphics, int x, int y, int w, Component label, boolean selected, boolean hover) {
		GuiTheme.toggle(graphics, x, y, w, CHIP_H, selected, hover);
		int color = selected ? GuiTheme.onColor(hover ? GuiTheme.accentHover() : GuiTheme.accent()) : GuiTheme.text();
		String text = GuiTheme.fit(font, label.getString(), w - 6);
		graphics.text(font, text, x + (w - font.width(text) + 1) / 2, y + 3, color, false);
	}

	private void checkbox(GuiGraphicsExtractor graphics, int x, int y, boolean on, boolean hover) {
		if (on) {
			GuiTheme.face(graphics, x, y, 9, 9, hover ? GuiTheme.accentHover() : GuiTheme.accent());
			int c = GuiTheme.onColor(GuiTheme.accent());
			graphics.fill(x + 2, y + 4, x + 3, y + 6, c);
			graphics.fill(x + 3, y + 5, x + 4, y + 7, c);
			graphics.fill(x + 4, y + 4, x + 5, y + 6, c);
			graphics.fill(x + 5, y + 3, x + 6, y + 5, c);
			graphics.fill(x + 6, y + 2, x + 7, y + 4, c);
		} else {
			GuiTheme.sunken(graphics, x, y, 9, 9);
		}
	}

	/** Kleines Musterfenster im gewählten Design – Effekte laufen mit. */
	private void preview(GuiGraphicsExtractor graphics, int x, int y, int mouseX, int mouseY) {
		int w = RIGHT_W;
		int h = 96;
		GuiTheme.panel(graphics, x, y, w, h);
		GuiFx.shimmer(graphics, x, y, w, h);
		GuiFx.sheen(graphics, x, y, w, h);
		GuiTheme.title(graphics, font, Component.translatable("jtechstorage.gui.preview"), x + 7, y + 6);

		for (int i = 0; i < PREVIEW_ITEMS.length; i++) {
			int sx = x + 9 + (i % 5) * 18;
			int sy = y + 20 + (i / 5) * 18;
			GuiTheme.slot(graphics, sx - 1, sy - 1);

			if (GuiSettings.get().hoverGlow && over(sx, sy, 16, 16, mouseX, mouseY)) {
				GuiTheme.slotGlow(graphics, sx, sy);
			}

			ItemStack stack = PREVIEW_ITEMS[i];

			if (!stack.isEmpty()) {
				graphics.item(stack, sx, sy);
				String n = Integer.toString(stack.getCount());

				if (stack.getCount() > 1) {
					if (GuiTheme.outlineCounts()) {
						GuiTheme.outlinedNumber(graphics, font, n, sx + 17 - font.width(n), sy + 9);
					} else {
						graphics.text(font, n, sx + 17 - font.width(n), sy + 9, 0xFFFFFFFF, true);
					}
				}
			}
		}

		int fy = y + 60;
		GuiTheme.field(graphics, x + 8, fy, w - 16, 12, false);
		int tx = x + 11;

		if (!GuiTheme.classic()) {
			GuiTheme.searchIcon(graphics, x + 11, fy + 3);
			tx += 10;
		}

		graphics.text(font, Component.translatable("jtechstorage.terminal.search.hint"), tx, fy + 2, 0xFF8A8A96, false);
		int by = y + 77;
		boolean hover = over(x + 8, by, 44, 13, mouseX, mouseY);
		GuiTheme.button(graphics, font, x + 8, by, 44, 13, Component.translatable("jtechstorage.gui.preview.button"), true, hover);
		boolean hover2 = over(x + 56, by, 44, 13, mouseX, mouseY);
		GuiTheme.toggle(graphics, x + 56, by, 44, 13, false, hover2);
		Component other = Component.translatable("jtechstorage.gui.preview.toggle");
		graphics.text(font, other, x + 56 + (44 - font.width(other)) / 2, by + 3, GuiTheme.text(), false);
	}

	private void tooltips(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		for (int i = 0; i < FX.length; i++) {
			if (over(left() + 10, fxY(i), 160, ROW_H, mouseX, mouseY)) {
				graphics.setTooltipForNextFrame(font, font.split(Component.translatable("jtechstorage.gui.fx." + FX[i] + ".desc"), 180),
						mouseX, mouseY);
				return;
			}
		}

		for (int i = 0; i < ACCENTS.length; i++) {
			if (over(accentX(i), accentY(), DOT, DOT, mouseX, mouseY)) {
				graphics.setTooltipForNextFrame(font, Component.translatable("jtechstorage.gui.accent." + key(ACCENTS[i])), mouseX, mouseY);
				return;
			}
		}

		for (int i = 0; i < PRESETS.length; i++) {
			if (over(presetX(i), presetY(), CHIP_W, CHIP_H, mouseX, mouseY)) {
				graphics.setTooltipForNextFrame(font, font.split(Component.translatable("jtechstorage.gui.effects." + key(PRESETS[i]) + ".desc"), 180),
						mouseX, mouseY);
				return;
			}
		}
	}

	private static boolean[] values(GuiSettings s) {
		return new boolean[] {s.fadeIn, s.hoverGlow, s.borderShimmer, s.sheen, s.particles, s.countAnimation};
	}

	private static void toggleFx(GuiSettings s, int index) {
		switch (index) {
			case 0 -> s.fadeIn = !s.fadeIn;
			case 1 -> s.hoverGlow = !s.hoverGlow;
			case 2 -> s.borderShimmer = !s.borderShimmer;
			case 3 -> s.sheen = !s.sheen;
			case 4 -> s.particles = !s.particles;
			default -> s.countAnimation = !s.countAnimation;
		}
	}

	private static String key(Enum<?> value) {
		return value.name().toLowerCase(Locale.ROOT);
	}

	// ------------------------------------------------------------------
	// Eingabe
	// ------------------------------------------------------------------

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		double mx = event.x();
		double my = event.y();
		GuiSettings s = GuiSettings.get();

		for (int i = 0; i < THEMES.length; i++) {
			if (over(themeX(i), themeY(i), THEME_W, CHIP_H, mx, my)) {
				s.theme = THEMES[i];
				return changed();
			}
		}

		for (int i = 0; i < ACCENTS.length; i++) {
			if (over(accentX(i), accentY(), DOT, DOT, mx, my)) {
				s.accent = ACCENTS[i];
				return changed();
			}
		}

		for (int i = 0; i < PRESETS.length; i++) {
			if (over(presetX(i), presetY(), CHIP_W, CHIP_H, mx, my)) {
				s.applyEffects(PRESETS[i]);
				return changed();
			}
		}

		for (int i = 0; i < FX.length; i++) {
			if (over(left() + 10, fxY(i), 160, ROW_H, mx, my)) {
				toggleFx(s, i);
				return changed();
			}
		}

		if (over(rightX(), rowsY(), RIGHT_W, CHIP_H, mx, my)) {
			s.rows = event.button() == 1 ? previous(s.rows) : GuiSettings.next(s.rows);
			return changed();
		}

		if (over(rightX(), countY(), RIGHT_W, CHIP_H, mx, my)) {
			s.countSize = event.button() == 1 ? previous(s.countSize) : GuiSettings.next(s.countSize);
			return changed();
		}

		if (over(rightX(), doneY(), RIGHT_W, 16, mx, my)) {
			click();
			onClose();
			return true;
		}

		return super.mouseClicked(event, doubleClick);
	}

	private static <E extends Enum<E>> E previous(E value) {
		E[] values = value.getDeclaringClass().getEnumConstants();
		return values[(value.ordinal() + values.length - 1) % values.length];
	}

	private boolean changed() {
		click();
		GuiSettings.get().save();
		return true;
	}

	private void click() {
		minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
	}

	@Override
	public void onClose() {
		GuiSettings.get().save();
		minecraft.setScreenAndShow(parent);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
