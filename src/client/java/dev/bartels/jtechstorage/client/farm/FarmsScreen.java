package dev.bartels.jtechstorage.client.farm;

import java.text.NumberFormat;
import java.util.List;

import org.jspecify.annotations.Nullable;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import dev.bartels.jtechstorage.client.gui.GuiTheme;
import dev.bartels.jtechstorage.farm.FarmMonitorBlockEntity;
import dev.bartels.jtechstorage.farm.FarmPayloads;
import dev.bartels.jtechstorage.menu.StorageTerminalMenu;

/** „Farmen“ im Terminal (v0.27.0): alle mit diesem Terminal verbundenen Farm-Monitore. */
public class FarmsScreen extends dev.bartels.jtechstorage.client.gui.ThemedScreen {
	private static final int W = 300;
	private static final int H = 212;
	private static final int ROW = 22;
	private static final int ROWS = 8;

	private final Screen parent;
	private final StorageTerminalMenu menu;
	private @Nullable List<FarmPayloads.Farm> farms;
	private int scroll;
	private int refresh;
	private int left;
	private int top;

	public FarmsScreen(Screen parent, StorageTerminalMenu menu) {
		super(Component.translatable("jtechstorage.farms.title"));
		this.parent = parent;
		this.menu = menu;
	}

	public int containerId() {
		return menu.containerId;
	}

	public void setFarms(List<FarmPayloads.Farm> farms) {
		this.farms = farms;
		scroll = Math.clamp(scroll, 0, Math.max(0, farms.size() - ROWS));
	}

	@Override
	protected void init() {
		left = (width - W) / 2;
		top = (height - H) / 2;
		request();
	}

	private void request() {
		refresh = 100;
		ClientPlayNetworking.send(new FarmPayloads.Request(menu.containerId));
	}

	@Override
	public void tick() {
		if (minecraft.player == null || minecraft.player.containerMenu != menu) {
			minecraft.setScreenAndShow(null);
			return;
		}

		if (--refresh <= 0) {
			request();
		}
	}

	@Override
	public void onClose() {
		minecraft.setScreenAndShow(parent);
	}

	private boolean over(int x, int y, int w, int h, double mouseX, double mouseY) {
		return mouseX >= left + x && mouseX < left + x + w && mouseY >= top + y && mouseY < top + y + h;
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractBackground(graphics, mouseX, mouseY, delta);
		GuiTheme.window(graphics, left, top, W, H);
		GuiTheme.sunken(graphics, left + 8, top + 22, W - 16, ROWS * ROW + 4);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractRenderState(graphics, mouseX, mouseY, delta);
		int x = left;
		int y = top;
		int text = GuiTheme.text();
		int info = GuiTheme.onColor(GuiTheme.slot());
		graphics.text(font, "⚙", x + 8, y + 7, 0xFFE0B0FF, true);
		GuiTheme.label(graphics, font, title, x + 20, y + 7, W - 50, text);
		boolean closeHover = over(W - 22, 4, 14, 12, mouseX, mouseY);
		graphics.text(font, "×", x + W - 17, y + 6, closeHover ? GuiTheme.accentHover() : text, false);

		if (farms == null) {
			return;
		}

		if (farms.isEmpty()) {
			for (int i = 0; i < 3; i++) {
				GuiTheme.label(graphics, font, Component.translatable("jtechstorage.farms.none." + i), x + 14, y + 30 + i * 12, W - 28, info);
			}

			return;
		}

		NumberFormat format = NumberFormat.getIntegerInstance();

		for (int i = 0; i < Math.min(ROWS, farms.size() - scroll); i++) {
			FarmPayloads.Farm farm = farms.get(scroll + i);
			int ry = y + 26 + i * ROW;
			Identifier id = Identifier.tryParse(farm.topItem());

			if (id != null && !farm.topItem().isEmpty()) {
				graphics.item(new ItemStack(BuiltInRegistries.ITEM.getValue(id)), x + 12, ry + 2);
			}

			int color = farm.status() == FarmMonitorBlockEntity.OK ? 0xFF4CC46C : 0xFFE05A40;
			graphics.text(font, "●", x + 32, ry + 2, color, false);
			GuiTheme.label(graphics, font, Component.literal(farm.name()), x + 42, ry + 2, 140, info);
			String rate = format.format(farm.perHour()) + " /h";
			graphics.text(font, rate, x + W - 14 - font.width(rate), ry + 2, info, false);
			String dim = farm.dimension().replace("minecraft:", "");
			Component where = Component.translatable("jtechstorage.farms.where", farm.pos().getX(), farm.pos().getY(), farm.pos().getZ(), dim);
			GuiTheme.label(graphics, font, where, x + 42, ry + 12, 150, GuiTheme.mix(info, 0xFF808080, 0.4f));
			Component state = Component.translatable("jtechstorage.farm.status.short." + farm.status());
			graphics.text(font, state, x + W - 14 - font.width(state), ry + 12, color, false);

			if (farm.minutesAgo() > 2 && mouseX >= x + 8 && mouseX < x + W - 8 && mouseY >= ry && mouseY < ry + ROW) {
				graphics.setTooltipForNextFrame(font, Component.translatable("jtechstorage.farms.stale", farm.minutesAgo()), mouseX, mouseY);
			}
		}
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (farms != null && farms.size() > ROWS) {
			scroll = Math.clamp(scroll - (int) Math.signum(scrollY), 0, farms.size() - ROWS);
			return true;
		}

		return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (over(W - 22, 4, 14, 12, event.x(), event.y())) {
			minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
			onClose();
			return true;
		}

		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
