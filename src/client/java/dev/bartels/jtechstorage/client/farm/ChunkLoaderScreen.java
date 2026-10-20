package dev.bartels.jtechstorage.client.farm;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;

import dev.bartels.jtechstorage.ModConfig;
import dev.bartels.jtechstorage.client.gui.GuiTheme;
import dev.bartels.jtechstorage.client.gui.ThemedContainerScreen;
import dev.bartels.jtechstorage.farm.ChunkLoaderBlockEntity;
import dev.bartels.jtechstorage.farm.ChunkLoaderMenu;

/** Chunk Loader (v0.27.1): Treibstoff, Status, Restlaufzeit, Anzahl, Chunk-Grenzen. */
public class ChunkLoaderScreen extends ThemedContainerScreen<ChunkLoaderMenu> {
	private static final int BTN_X = 76;
	private static final int BTN_Y = 76;
	private static final int BTN_W = 92;

	public ChunkLoaderScreen(ChunkLoaderMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title, ChunkLoaderMenu.WIDTH, ChunkLoaderMenu.HEIGHT);
		inventoryLabelY = ChunkLoaderMenu.INVENTORY_Y - 11;
	}

	private static String duration(int minutes) {
		int days = minutes / 1440;
		int hours = minutes % 1440 / 60;
		int mins = minutes % 60;
		return days > 0 ? days + " d " + hours + " h" : hours > 0 ? hours + " h " + mins + " min" : mins + " min";
	}

	private boolean overButton(double mouseX, double mouseY) {
		return mouseX >= leftPos + BTN_X && mouseX < leftPos + BTN_X + BTN_W && mouseY >= topPos + BTN_Y && mouseY < topPos + BTN_Y + 14;
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractBackground(graphics, mouseX, mouseY, delta);
		GuiTheme.panel(graphics, leftPos, topPos, imageWidth, imageHeight);
		GuiTheme.slot(graphics, leftPos + ChunkLoaderMenu.FUEL_X - 1, topPos + ChunkLoaderMenu.FUEL_Y - 1);

		for (int row = 0; row < 4; row++) {
			for (int col = 0; col < 9; col++) {
				int sy = row < 3 ? ChunkLoaderMenu.INVENTORY_Y + row * 18 : ChunkLoaderMenu.INVENTORY_Y + 58;
				GuiTheme.slot(graphics, leftPos + 8 + col * 18 - 1, topPos + sy - 1);
			}
		}
	}

	@Override
	public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractContents(graphics, mouseX, mouseY, delta);
		int x = leftPos;
		int y = topPos;
		int text = GuiTheme.text();
		int subtle = GuiTheme.subtle();
		GuiTheme.label(graphics, font, Component.translatable("jtechstorage.chunk_loader.fuel", ModConfig.get().farm.chunkLoaderMinutesPerShard),
				x + 34, y + 20, imageWidth - 42, text);
		GuiTheme.label(graphics, font, Component.translatable("jtechstorage.chunk_loader.fuel_block"), x + 34, y + 30, imageWidth - 42, subtle);

		int status = menu.status();
		int color = status == ChunkLoaderBlockEntity.ACTIVE ? 0xFF4CC46C : 0xFFE05A40;
		Component state = status == ChunkLoaderBlockEntity.ACTIVE
				? Component.translatable("jtechstorage.chunk_loader.status.0", menu.chunkX(), menu.chunkZ())
				: Component.translatable("jtechstorage.chunk_loader.status." + status);
		graphics.text(font, "●", x + 8, y + 44, color, false);
		GuiTheme.label(graphics, font, state, x + 18, y + 44, imageWidth - 26, text);
		int minutes = menu.minutesLeft();
		GuiTheme.label(graphics, font, Component.translatable("jtechstorage.chunk_loader.left", duration(minutes)), x + 8, y + 56, imageWidth - 16, text);
		int barW = imageWidth - 16;
		graphics.fill(x + 8, y + 67, x + 8 + barW, y + 72, 0xFF000000);
		int max = 64 * 4 * ModConfig.get().farm.chunkLoaderMinutesPerShard;
		int fill = (int) Math.round((barW - 2) * Math.min(1.0, minutes / (double) Math.max(1, max)));
		graphics.fill(x + 9, y + 68, x + 9 + fill, y + 71, 0xFFA064E6);
		GuiTheme.label(graphics, font, Component.translatable("jtechstorage.chunk_loader.count", menu.count(), menu.limit()), x + 8, y + 79, 66, subtle);
		GuiTheme.labeled(graphics, font, x + BTN_X, y + BTN_Y, BTN_W, 14, Component.translatable("jtechstorage.chunk_loader.bounds"),
				overButton(mouseX, mouseY) ? GuiTheme.accentHover() : GuiTheme.accent(), true);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (overButton(event.x(), event.y())) {
			minecraft.gameMode.handleInventoryButtonClick(menu.containerId, ChunkLoaderMenu.BUTTON_BOUNDS);
			minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
			return true;
		}

		return super.mouseClicked(event, doubleClick);
	}
}
