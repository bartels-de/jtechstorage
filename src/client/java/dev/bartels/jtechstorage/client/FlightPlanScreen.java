package dev.bartels.jtechstorage.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import dev.bartels.jtechstorage.ModContent;
import dev.bartels.jtechstorage.client.gui.GuiTheme;
import dev.bartels.jtechstorage.item.FlightPlanItem;
import dev.bartels.jtechstorage.net.FlightPlanEditPayload;

/**
 * Flugplan-Fenster: Haltestellen umsortieren (▲/▼) und entfernen (✕). Die Änderungen gehen beim Schließen an den Server.
 */
public class FlightPlanScreen extends dev.bartels.jtechstorage.client.gui.ThemedScreen {
	private static final int W = 236;
	private static final int ROW_H = 24;
	private static final int ROWS_Y = 26;
	private static final int H = ROWS_Y + FlightPlanItem.MAX_STOPS * ROW_H + 44;
	private static final int SMALL = 16;

	private final InteractionHand hand;
	private final List<FlightPlanItem.Stop> original;
	/** Indizes in {@link #original} in neuer Reihenfolge. */
	private final List<Integer> order = new ArrayList<>();
	private boolean changed;
	private int left;
	private int top;

	public FlightPlanScreen(InteractionHand hand, ItemStack stack) {
		super(Component.translatable("item.jtechstorage.flight_plan"));
		this.hand = hand;
		this.original = FlightPlanItem.stops(stack);

		for (int i = 0; i < original.size(); i++) {
			order.add(i);
		}
	}

	@Override
	protected void init() {
		left = (width - W) / 2;
		top = (height - H) / 2;
	}

	private int rowY(int row) {
		return top + ROWS_Y + row * ROW_H;
	}

	/** x-Position der Knöpfe ▲ ▼ ✕ einer Zeile. */
	private int buttonX(int button) {
		return left + W - 10 - (3 - button) * (SMALL + 2);
	}

	private static boolean over(int x, int y, int w, int h, double mouseX, double mouseY) {
		return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
	}

	private boolean enabled(int row, int button) {
		return switch (button) {
			case 0 -> row > 0;
			case 1 -> row < order.size() - 1;
			default -> true;
		};
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractBackground(graphics, mouseX, mouseY, delta);
		GuiTheme.window(graphics, left, top, W, H);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractRenderState(graphics, mouseX, mouseY, delta);
		graphics.text(font, "✦", left + 10, top + 9, 0xFFE0B0FF, true);
		GuiTheme.label(graphics, font, title, left + 22, top + 9, W - 32, GuiTheme.text());
		int infoText = GuiTheme.onColor(GuiTheme.slot());
		int infoSubtle = GuiTheme.mix(infoText, GuiTheme.slot(), 0.3f);
		String[] icons = {"▲", "▼", "✕"};

		for (int row = 0; row < FlightPlanItem.MAX_STOPS; row++) {
			int y = rowY(row);
			GuiTheme.sunken(graphics, left + 8, y, W - 16, ROW_H - 2);

			if (row >= order.size()) {
				GuiTheme.label(graphics, font, Component.translatable("jtechstorage.flight_plan.gui.free"), left + 30, y + 7, W - 60, infoSubtle);
				graphics.text(font, String.valueOf(row + 1), left + 14, y + 7, infoSubtle, false);
				continue;
			}

			FlightPlanItem.Stop stop = original.get(order.get(row));
			BlockPos pos = stop.pos().pos();
			graphics.text(font, String.valueOf(row + 1), left + 14, y + 7, 0xFFE0B0FF, true);
			int textW = buttonX(0) - (left + 30) - 4;
			GuiTheme.label(graphics, font, Component.literal(stop.name()), left + 30, y + 3, textW, infoText);
			net.minecraft.network.chat.MutableComponent where = Component.literal(pos.getX() + ", " + pos.getY() + ", " + pos.getZ());
			String dim = stop.pos().dimension().identifier().getPath();

			if (!"overworld".equals(dim)) {
				where.append(" · ").append(Component.translatableWithFallback("jtechstorage.flight_plan.gui.dim." + dim, dim));
			}

			GuiTheme.label(graphics, font, where, left + 30, y + 12, textW, infoSubtle);

			for (int button = 0; button < 3; button++) {
				boolean on = enabled(row, button);
				int bx = buttonX(button);
				int by = y + 3;
				int color = !on ? GuiTheme.DISABLED_FACE
						: over(bx, by, SMALL, SMALL, mouseX, mouseY) ? (button == 2 ? 0xFFE06050 : GuiTheme.accentHover())
						: (button == 2 ? 0xFFB04438 : GuiTheme.accent());
				GuiTheme.labeled(graphics, font, bx, by, SMALL, SMALL, Component.literal(icons[button]), color, on);
			}
		}

		int hintY = rowY(FlightPlanItem.MAX_STOPS) + 4;
		Component hint = Component.translatable(order.size() < 2 ? "jtechstorage.flight_plan.gui.hint_add" : "jtechstorage.flight_plan.gui.hint_use");
		GuiTheme.label(graphics, font, hint, left + 10, hintY, W - 20, GuiTheme.subtle());

		int doneX = left + (W - 100) / 2;
		int doneY = hintY + 14;
		GuiTheme.button(graphics, font, doneX, doneY, 100, 18, Component.translatable("gui.done"), true, over(doneX, doneY, 100, 18, mouseX, mouseY));

		for (int row = 0; row < order.size(); row++) {
			for (int button = 0; button < 3; button++) {
				if (over(buttonX(button), rowY(row) + 3, SMALL, SMALL, mouseX, mouseY)) {
					graphics.setTooltipForNextFrame(font, Component.translatable("jtechstorage.flight_plan.gui.button." + button), mouseX, mouseY);
				}
			}
		}
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		for (int row = 0; row < order.size(); row++) {
			for (int button = 0; button < 3; button++) {
				if (enabled(row, button) && over(buttonX(button), rowY(row) + 3, SMALL, SMALL, event.x(), event.y())) {
					switch (button) {
						case 0 -> order.add(row - 1, order.remove(row));
						case 1 -> order.add(row + 1, order.remove(row));
						default -> order.remove(row);
					}

					changed = true;
					minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
					return true;
				}
			}
		}

		int doneX = left + (W - 100) / 2;
		int doneY = rowY(FlightPlanItem.MAX_STOPS) + 18;

		if (over(doneX, doneY, 100, 18, event.x(), event.y())) {
			minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
			onClose();
			return true;
		}

		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public void removed() {
		if (changed && minecraft.player != null && minecraft.player.getItemInHand(hand).is(ModContent.FLIGHT_PLAN)) {
			ClientPlayNetworking.send(new FlightPlanEditPayload(hand == InteractionHand.OFF_HAND, List.copyOf(order)));
		}

		super.removed();
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
