package dev.bartels.jtechstorage.client.display;

import java.util.List;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import dev.bartels.jtechstorage.client.gui.GuiTheme;
import dev.bartels.jtechstorage.display.PanelActions;
import dev.bartels.jtechstorage.display.StorageDisplayBlockEntity;
import dev.bartels.jtechstorage.display.StorageDisplayBlockEntity.Mode;

/** Menü des Lager-Bildschirms (v0.36): Anzeige (Modus, Gegenstand aus dem Netzwerk) und Aussehen (Rahmen, Farben, Leuchten). */
public class StorageDisplayScreen extends PanelScreen<StorageDisplayBlockEntity> {
	private static final ItemStack[] MODE_ICONS = {new ItemStack(Items.DIAMOND), new ItemStack(Items.BUCKET), new ItemStack(Items.CLOCK),
			new ItemStack(Items.GOLD_INGOT)};

	public StorageDisplayScreen(BlockPos pos, List<ItemStack> items, List<Long> counts) {
		super(Component.translatable("block.jtechstorage.storage_display"), pos, items, counts);
	}

	@Override
	protected Class<StorageDisplayBlockEntity> blockClass() {
		return StorageDisplayBlockEntity.class;
	}

	@Override
	protected int gridLeft() {
		return 136;
	}

	@Override
	protected int gridTop() {
		return 95;
	}

	/** Kleine Vorschau des Bildschirms in den gewählten Farben. */
	private void preview(GuiGraphicsExtractor g, StorageDisplayBlockEntity be, int x, int y, int w, int h) {
		int[] frame = PanelLooks.at(PanelLooks.FRAME, be.getFrame());
		int[] bg = PanelLooks.at(PanelLooks.BACKGROUND, be.getBackground());
		int accent = PanelLooks.at(PanelLooks.ACCENT, be.getAccent());
		boolean frameless = be.getFrame() == 5;
		int b = frameless ? 0 : 3;

		if (!frameless) {
			g.fill(x, y, x + w, y + h, frame[1]);
			g.fill(x, y, x + w, y + h / 2, frame[0]);
			g.fill(x, y, x + w, y + 1, frame[2]);
			g.fill(x, y, x + 1, y + h, frame[2]);
		}

		for (int k = 0; k < h - 2 * b; k++) {
			g.fill(x + b, y + b + k, x + w - b, y + b + k + 1, PanelLooks.mix(bg[0], bg[1], k / (float) (h - 2 * b)));
		}

		if (be.hasScanlines()) {
			for (int k = b + 2; k < h - b; k += 3) {
				g.fill(x + b, y + k, x + w - b, y + k + 1, 0x22000000);
			}
		}

		int tx = x + b + 4;
		g.text(font, Component.translatable("jtechstorage.display.screen.storage"), tx, y + b + 4, accent, false);
		g.fill(tx, y + b + 16, x + w - b - 4, y + b + 22, 0xFF07050C);
		g.fill(tx + 1, y + b + 17, tx + 1 + (int) ((w - 2 * b - 10) * 0.72f), y + b + 21, accent);
		g.text(font, "72 %", tx, y + b + 26, 0xFFEDE6FF, false);
		int[] pts = {10, 6, 9, 3, 7, 2, 5};
		int gy = y + h - b - 20;

		for (int i = 0; i + 1 < pts.length; i++) {
			for (int s = 0; s < 8; s++) {
				int px = tx + 2 + i * 8 + s;
				int py = gy + pts[i] + (pts[i + 1] - pts[i]) * s / 8;
				g.fill(px, py, px + 1, py + 1, accent);
			}
		}

		if (!be.isGlow()) {
			g.fill(x + b, y + b, x + w - b, y + h - b, 0x55000000);   // ohne Leuchten wirkt es im Dunkeln dunkler
		}
	}

	@Override
	protected void drawShow(GuiGraphicsExtractor g, StorageDisplayBlockEntity be, int x, int y, int mouseX, int mouseY) {
		preview(g, be, x + 8, y + 22, 116, 74);
		label(g, "jtechstorage.panel.mode", x + 8, y + 102);

		for (int i = 0; i < 4; i++) {
			int bx = x + 8;
			int by = y + 112 + i * 18;
			Mode mode = Mode.values()[i];
			boolean active = be.getMode() == mode;
			final int value = i;
			GuiTheme.toggle(g, bx, by, 116, 17, active, over(bx, by, 116, 17, mouseX, mouseY));
			g.item(MODE_ICONS[i], bx + 2, by + 1);
			String label = GuiTheme.fit(font, Component.translatable("jtechstorage.display.mode." + mode.id()).getString(), 92);
			g.text(font, label, bx + 21, by + 5, active ? GuiTheme.onColor(GuiTheme.accent()) : GuiTheme.text(), false);
			hit(bx, by, 116, 17, () -> send(PanelActions.MODE, value));
		}

		int rx = x + 136;
		label(g, "jtechstorage.panel.item", rx, y + 24);
		GuiTheme.slot(g, rx, y + 35);
		ItemStack tracked = be.getTracked();

		if (!tracked.isEmpty()) {
			g.item(tracked, rx + 1, y + 36);
			g.text(font, GuiTheme.fit(font, tracked.getHoverName().getString(), W - 166), rx + 22, y + 36, GuiTheme.text(), false);
			g.text(font, Component.translatable("jtechstorage.panel.in_storage", PanelCanvas.full(be.getCount())), rx + 22, y + 46, GuiTheme.subtle(), false);
		} else {
			g.text(font, Component.translatable("jtechstorage.panel.no_item"), rx + 22, y + 41, GuiTheme.subtle(), false);
		}

		button(g, rx, y + 57, 86, 14, Component.translatable("jtechstorage.panel.from_hand"), () -> send(PanelActions.DISPLAY_FROM_HAND, 0), mouseX, mouseY);
		button(g, rx + 90, y + 57, 70, 14, Component.translatable("jtechstorage.panel.clear"), () -> send(PanelActions.DISPLAY_CLEAR, 0), mouseX, mouseY);
		drawGrid(g, tracked.isEmpty() ? List.of() : List.of(tracked), mouseX, mouseY);
		text(g, Component.translatable("jtechstorage.panel.pick_hint"), rx, y + H - 34, GuiTheme.subtle());
	}

	@Override
	protected void drawLook(GuiGraphicsExtractor g, StorageDisplayBlockEntity be, int x, int y, int mouseX, int mouseY) {
		preview(g, be, x + 8, y + 22, 116, 74);
		label(g, "jtechstorage.panel.preview", x + 8, y + 100);
		int rx = x + 136;
		int[] frames = new int[PanelLooks.FRAME.length];

		for (int i = 0; i < frames.length; i++) {
			frames[i] = i == 5 ? 0 : PanelLooks.FRAME[i][0];
		}

		int[] bgs = new int[PanelLooks.BACKGROUND.length];

		for (int i = 0; i < bgs.length; i++) {
			bgs[i] = PanelLooks.BACKGROUND[i][0];
		}

		label(g, "jtechstorage.panel.frame", rx, y + 24);
		swatches(g, rx, y + 35, frames, be.getFrame(), 14, 8, PanelActions.FRAME, "jtechstorage.panel.frame.", PanelLooks.FRAME_NAMES, mouseX, mouseY);
		text(g, Component.translatable("jtechstorage.panel.frame." + PanelLooks.FRAME_NAMES[be.getFrame()]), rx + 102, y + 38, GuiTheme.text());
		label(g, "jtechstorage.panel.background", rx, y + 56);
		swatches(g, rx, y + 67, bgs, be.getBackground(), 14, 8, PanelActions.BACKGROUND, "jtechstorage.panel.background.", PanelLooks.BACKGROUND_NAMES,
				mouseX, mouseY);
		text(g, Component.translatable("jtechstorage.panel.background." + PanelLooks.BACKGROUND_NAMES[be.getBackground()]), rx + 102, y + 70, GuiTheme.text());
		label(g, "jtechstorage.panel.accent", rx, y + 88);
		swatches(g, rx, y + 99, PanelLooks.ACCENT, be.getAccent(), 12, 8, PanelActions.ACCENT, "jtechstorage.panel.accent.", PanelLooks.ACCENT_NAMES,
				mouseX, mouseY);
		toggle(g, rx, y + 122, Component.translatable("jtechstorage.panel.glow"), be.isGlow(), PanelActions.GLOW, mouseX, mouseY);
		toggle(g, rx, y + 139, Component.translatable("jtechstorage.panel.scanlines"), be.hasScanlines(), PanelActions.SCANLINES, mouseX, mouseY);
	}
}
