package dev.bartels.jtechstorage.client.display;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;

import dev.bartels.jtechstorage.client.gui.GuiTheme;
import dev.bartels.jtechstorage.display.PanelActions;
import dev.bartels.jtechstorage.display.StorageBannerBlockEntity;

/** Menü des Lager-Banners (v0.36): Anzeige (bis zu 8 Gegenstände aus dem Netzwerk) und Aussehen (Stoff, Saum, Muster, Schrift, Wind). */
public class StorageBannerScreen extends PanelScreen<StorageBannerBlockEntity> {
	private static final int[] DYES = new int[16];

	static {
		for (DyeColor c : DyeColor.values()) {
			DYES[c.getId()] = 0xFF000000 | c.getTextureDiffuseColor();
		}
	}

	public StorageBannerScreen(BlockPos pos, List<ItemStack> items, List<Long> counts) {
		super(Component.translatable("block.jtechstorage.storage_banner"), pos, items, counts);
	}

	@Override
	protected Class<StorageBannerBlockEntity> blockClass() {
		return StorageBannerBlockEntity.class;
	}

	@Override
	protected int gridLeft() {
		return 104;
	}

	@Override
	protected int gridTop() {
		return 88;
	}

	/** Kleine Vorschau des Banners in den gewählten Farben. */
	private void preview(GuiGraphicsExtractor g, StorageBannerBlockEntity be, int x, int y) {
		int cloth = DYES[Math.floorMod(be.getCloth(), 16)];
		int trim = PanelLooks.at(PanelLooks.TRIM, be.getTrim());
		int text = PanelLooks.at(PanelLooks.TEXT, be.getTextColor());
		int h = 128;
		g.fill(x - 3, y, x + 59, y + 3, 0xFF4A3424);
		g.fill(x - 4, y - 1, x - 1, y + 4, 0xFFE8BB4A);
		g.fill(x + 57, y - 1, x + 60, y + 4, 0xFFE8BB4A);
		g.fill(x, y + 3, x + 56, y + h, cloth);
		int light = PanelLooks.mix(cloth, 0xFFFFFFFF, 0.22f);
		int dark = PanelLooks.mix(cloth, 0xFF000000, 0.35f);

		switch (be.getPattern()) {
			case 1 -> {
				for (int xx = x + 4; xx < x + 54; xx += 8) {
					g.fill(xx, y + 3, xx + 2, y + h, light);
				}
			}
			case 2 -> {
				for (int yy = y + 8; yy < y + h - 12; yy += 12) {
					for (int xx = x + 6; xx < x + 52; xx += 12) {
						for (int k = 0; k < 4; k++) {
							g.fill(xx + 4 - k, yy + k, xx + 5 + k, yy + k + 1, light);
							g.fill(xx + 4 - k, yy + 8 - k, xx + 5 + k, yy + 9 - k, light);
						}
					}
				}
			}
			case 3 -> {
				for (int yy = y + 7; yy < y + h - 8; yy += 10) {
					for (int xx = x + 5 + ((yy / 10) % 2) * 5; xx < x + 52; xx += 10) {
						g.fill(xx, yy - 1, xx + 1, yy + 2, light);
						g.fill(xx - 1, yy, xx + 2, yy + 1, light);
					}
				}
			}
			case 4 -> {
				for (int xx = x + 6; xx < x + 52; xx += 9) {
					g.fill(xx, y + 6, xx + 2, y + 13, light);
					g.fill(xx, y + h - 14, xx + 2, y + h - 8, light);
				}
			}
			default -> {
			}
		}

		if (trim != 0) {
			g.fill(x + 2, y + 3, x + 3, y + h, trim);
			g.fill(x + 53, y + 3, x + 54, y + h, trim);
			g.fill(x + 2, y + h - 6, x + 54, y + h - 5, trim);
		}

		g.fill(x + 5, y + 26, x + 51, y + h - 10, dark);
		String title = Component.translatable("jtechstorage.banner.title").getString().toUpperCase(PanelCanvas.locale());
		String t = GuiTheme.fit(font, title, 50);
		g.text(font, t, x + 28 - font.width(t) / 2, y + 12, text, false);
		List<ItemStack> items = be.getItems();
		List<Long> counts = be.getCounts();

		for (int i = 0; i < Math.min(4, items.size()); i++) {
			g.item(items.get(i), x + 7, y + 30 + i * 20);
			long c = i < counts.size() ? counts.get(i) : 0;
			g.text(font, PanelCanvas.compact(c), x + 25, y + 34 + i * 20, text, false);
		}

		int tassel = trim != 0 ? trim : dark;

		for (int xx = x + 1; xx < x + 55; xx += 3) {
			g.fill(xx, y + h, xx + 2, y + h + 6, tassel);
		}
	}

	@Override
	protected void drawShow(GuiGraphicsExtractor g, StorageBannerBlockEntity be, int x, int y, int mouseX, int mouseY) {
		preview(g, be, x + 20, y + 26);
		List<ItemStack> items = be.getItems();
		int rx = x + 104;
		text(g, Component.translatable("jtechstorage.panel.on_banner", items.size(), StorageBannerBlockEntity.MAX), rx, y + 24, GuiTheme.subtle());

		for (int i = 0; i < StorageBannerBlockEntity.MAX; i++) {
			int sx = rx + i * 18;
			GuiTheme.slot(g, sx, y + 35);

			if (i < items.size()) {
				ItemStack stack = items.get(i);
				final int index = i;
				g.item(stack, sx + 1, y + 36);

				if (over(sx, y + 35, 18, 18, mouseX, mouseY)) {
					g.fill(sx + 1, y + 36, sx + 17, y + 52, 0x40FF6A6A);
					tooltip(List.of(stack.getHoverName().getVisualOrderText(),
							Component.translatable("jtechstorage.panel.click_remove").withStyle(ChatFormatting.GRAY).getVisualOrderText()));
				}

				hit(sx, y + 35, 18, 18, () -> send(PanelActions.BANNER_REMOVE, index));
			}
		}

		drawGrid(g, items, mouseX, mouseY);
		text(g, Component.translatable("jtechstorage.panel.add_hint"), rx, y + H - 34, GuiTheme.subtle());
		button(g, x + W - 102, y + H - 38, 94, 14, Component.translatable("jtechstorage.panel.remove_all"), () -> send(PanelActions.BANNER_CLEAR, 0),
				mouseX, mouseY);
	}

	@Override
	protected void drawLook(GuiGraphicsExtractor g, StorageBannerBlockEntity be, int x, int y, int mouseX, int mouseY) {
		preview(g, be, x + 20, y + 26);
		int rx = x + 104;
		String[] dyeNames = new String[16];

		for (DyeColor c : DyeColor.values()) {
			dyeNames[c.getId()] = c.getName();
		}

		label(g, "jtechstorage.panel.cloth", rx, y + 22);
		swatches(g, rx, y + 33, DYES, Math.floorMod(be.getCloth(), 16), 11, 8, PanelActions.CLOTH, "color.minecraft.", dyeNames, mouseX, mouseY);
		label(g, "jtechstorage.panel.trim", rx, y + 61);
		choices(g, rx, y + 71, 64, 13, 3, "jtechstorage.panel.trim.", PanelLooks.TRIM_NAMES, be.getTrim(), PanelActions.TRIM, mouseX, mouseY);
		label(g, "jtechstorage.panel.pattern", rx, y + 103);
		choices(g, rx, y + 113, 64, 13, 3, "jtechstorage.panel.pattern.", PanelLooks.PATTERN_NAMES, be.getPattern(), PanelActions.PATTERN, mouseX, mouseY);
		label(g, "jtechstorage.panel.text", rx, y + 146);
		swatches(g, rx, y + 157, PanelLooks.TEXT, be.getTextColor(), 12, 8, PanelActions.TEXT, "jtechstorage.panel.text.", PanelLooks.TEXT_NAMES,
				mouseX, mouseY);
		label(g, "jtechstorage.panel.wind", rx + 64, y + 146);
		choices(g, rx + 64, y + 156, 40, 13, "jtechstorage.panel.wind.", PanelLooks.WIND_NAMES, be.getWind(), PanelActions.WIND, mouseX, mouseY);
	}
}
