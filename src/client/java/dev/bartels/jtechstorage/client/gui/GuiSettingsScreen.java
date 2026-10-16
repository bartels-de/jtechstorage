package dev.bartels.jtechstorage.client.gui;

import java.util.Locale;

import org.jspecify.annotations.Nullable;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

/** Oberflächen-Einstellungen: Design, Akzentfarbe, Terminal-Größe, Zahlengröße. Öffnet danach den vorherigen Screen. */
public class GuiSettingsScreen extends Screen {
	private static final int W = 220;
	private final @Nullable Screen parent;
	private Button themeButton;
	private Button accentButton;
	private Button rowsButton;
	private Button countButton;

	public GuiSettingsScreen(@Nullable Screen parent) {
		super(Component.translatable("jtechstorage.gui.settings"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		int x = width / 2 - W / 2;
		int y = height / 2 - 70;
		GuiSettings s = GuiSettings.get();

		themeButton = addRenderableWidget(Button.builder(label("theme", s.theme), b -> {
			s.theme = GuiSettings.next(s.theme);
			refresh();
		}).bounds(x, y, W, 20).build());
		accentButton = addRenderableWidget(Button.builder(label("accent", s.accent), b -> {
			s.accent = GuiSettings.next(s.accent);
			refresh();
		}).bounds(x, y + 24, W, 20).build());
		rowsButton = addRenderableWidget(Button.builder(label("rows", s.rows), b -> {
			s.rows = GuiSettings.next(s.rows);
			refresh();
		}).bounds(x, y + 48, W, 20).build());
		countButton = addRenderableWidget(Button.builder(label("count", s.countSize), b -> {
			s.countSize = GuiSettings.next(s.countSize);
			refresh();
		}).bounds(x, y + 72, W, 20).build());
		addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> onClose()).bounds(x, y + 110, W, 20).build());
	}

	private static Component label(String key, Enum<?> value) {
		return Component.translatable("jtechstorage.gui." + key,
				Component.translatable("jtechstorage.gui." + key + "." + value.name().toLowerCase(Locale.ROOT)));
	}

	private void refresh() {
		GuiSettings s = GuiSettings.get();
		themeButton.setMessage(label("theme", s.theme));
		accentButton.setMessage(label("accent", s.accent));
		rowsButton.setMessage(label("rows", s.rows));
		countButton.setMessage(label("count", s.countSize));
		s.save();
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractRenderState(graphics, mouseX, mouseY, delta);
		int x = width / 2 - W / 2;
		int y = height / 2 - 70;
		graphics.text(font, title, width / 2 - font.width(title) / 2, y - 30, 0xFFFFFFFF, true);

		// kleine Vorschau des Designs
		GuiTheme.panel(graphics, x, y - 18, W, 14);
		GuiTheme.button(graphics, x + 4, y - 15, 40, 8, true, false);
		GuiTheme.slot(graphics, x + 50, y - 17);
		GuiTheme.label(graphics, font, Component.translatable("jtechstorage.gui.preview"), x + 74, y - 14, W - 80, GuiTheme.text());
		graphics.textWithWordWrap(font, Component.translatable("jtechstorage.gui.rows.hint"), x, y + 96, W, 0xFFC8C8C8, true);
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
