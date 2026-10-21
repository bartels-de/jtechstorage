package dev.bartels.jtechstorage.client.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Basis für Mod-Bildschirme ohne Inventar (v0.29): Hintergrund bleibt beim Hereingleiten stehen. */
public abstract class ThemedScreen extends Screen {
	protected ThemedScreen(Component title) {
		super(title);
	}

	@Override
	public void extractTransparentBackground(GuiGraphicsExtractor graphics) {
		GuiFx.beginBackground(this, graphics);
		super.extractTransparentBackground(graphics);
		GuiFx.endBackground(graphics);
	}
}
