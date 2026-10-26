package dev.bartels.jtechstorage.client.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Base for mod screens without an inventory (v0.29): background stays put during the slide-in. */
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
