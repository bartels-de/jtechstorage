package dev.bartels.jtechstorage.client;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import dev.bartels.jtechstorage.client.gui.GuiTheme;
import dev.bartels.jtechstorage.client.gui.ThemedContainerScreen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import dev.bartels.jtechstorage.block.PhoenixNestBlockEntity;
import dev.bartels.jtechstorage.mail.PhoenixPost;
import dev.bartels.jtechstorage.menu.NestMenu;
import dev.bartels.jtechstorage.net.NestActionPayload;
import dev.bartels.jtechstorage.net.NestStatePayload;

/**
 * Phönix-Horst: Name + Empfang, Ziel wählen, Paket aus dem Lager zusammenstellen, Abschicken / Dauerauftrag,
 * Phönix-Status und Posteingang.
 */
public class NestScreen extends ThemedContainerScreen<NestMenu> {
	private static final int W = 176;
	private static final int[] FIRE = {0xFFC8361A, 0xFFF2661C, 0xFFFFA424};

	private EditBox nameBox;
	private int seenVersion = -1;

	public NestScreen(NestMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title, W, NestMenu.IMAGE_HEIGHT);
		inventoryLabelY = NestMenu.INVENTORY_Y - 11;
	}

	private NestStatePayload.State state() {
		return menu.getClientState();
	}

	@Override
	protected void init() {
		super.init();
		nameBox = new EditBox(font, leftPos + 9, topPos + 20, 88, 12, Component.translatable("jtechstorage.nest.name"));
		nameBox.setMaxLength(PhoenixNestBlockEntity.MAX_NAME_LENGTH);
		nameBox.setHint(Component.translatable("jtechstorage.nest.name.hint").withStyle(ChatFormatting.GRAY));
		addRenderableWidget(nameBox);
	}

	private boolean over(int x, int y, int w, int h, double mouseX, double mouseY) {
		return mouseX >= leftPos + x && mouseX < leftPos + x + w && mouseY >= topPos + y && mouseY < topPos + y + h;
	}

	// ------------------------------------------------------------------
	// Zeichnen
	// ------------------------------------------------------------------

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractBackground(graphics, mouseX, mouseY, delta);
		int x = leftPos;
		int y = topPos;
		int h = imageHeight;
		GuiTheme.window(graphics, x, y, W, h);

		for (int i = 0; i < 3; i++) {
			graphics.fill(x + 2, y + 2 + i, x + W - 2, y + 3 + i, FIRE[i]);
		}

		// Status-Zeile als abgesetzte Fläche
		GuiTheme.sunken(graphics, x + 6, y + 107, W - 12, 12);

		for (int i = 0; i < 9; i++) {
			slotFrame(graphics, x + 7 + i * 18, y + NestMenu.SHIP_Y - 1);
			slotFrame(graphics, x + 7 + i * 18, y + NestMenu.INBOX_Y - 1);
		}

		for (int row = 0; row < 4; row++) {
			for (int col = 0; col < 9; col++) {
				int sy = row < 3 ? NestMenu.INVENTORY_Y + row * 18 : NestMenu.INVENTORY_Y + 58;
				slotFrame(graphics, x + 7 + col * 18, y + sy - 1);
			}
		}
	}

	private static void slotFrame(GuiGraphicsExtractor graphics, int x, int y) {
		GuiTheme.slot(graphics, x, y);
	}

	@Override
	public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		NestStatePayload.State state = state();

		if (seenVersion != menu.getClientVersion()) {
			seenVersion = menu.getClientVersion();

			if (!nameBox.isFocused()) {
				nameBox.setValue(state.name());
			}

			nameBox.setEditable(state.canSettings());
		}

		super.extractContents(graphics, mouseX, mouseY, delta);
		int x = leftPos;
		int y = topPos;

		// Name speichern + Empfang
		button(graphics, 100, 18, 14, 14, Component.literal("✔"), state.canSettings(), mouseX, mouseY, false);
		PhoenixPost.Accept accept = PhoenixPost.Accept.byId(state.accept());
		button(graphics, 118, 18, 50, 14, Component.translatable(accept.translationKey() + ".short"), state.canSettings(),
				mouseX, mouseY, false);

		// Ziel
		Component target;

		if (state.destinations().isEmpty()) {
			target = Component.translatable("jtechstorage.nest.no_destinations");
		} else if (state.selected() < 0) {
			target = Component.translatable("jtechstorage.nest.choose");
		} else {
			NestStatePayload.Destination d = state.destinations().get(state.selected());
			target = Component.translatable("jtechstorage.nest.to", d.label(), d.seconds());
		}

		boolean destEnabled = state.canSend() && !state.destinations().isEmpty();
		int dx = x + 8;
		int dy = y + 36;
		boolean destHover = destEnabled && over(8, 36, 160, 15, mouseX, mouseY);
		int destFace = destHover ? GuiTheme.accentDarkHover() : GuiTheme.accentDark();
		GuiTheme.face(graphics, dx, dy, 160, 15, destFace);
		int destText = GuiTheme.onColor(destFace);
		int textX = dx + 5;

		if (state.selected() >= 0 && state.selected() < state.destinations().size()) {
			graphics.fill(dx + 4, dy + 4, dx + 11, dy + 11, 0xFF000000);
			graphics.fill(dx + 5, dy + 5, dx + 10, dy + 10, 0xFF000000 | state.destinations().get(state.selected()).color());
			textX = dx + 14;
		}

		String label = GuiTheme.fit(font, (destEnabled ? "◀ " : "") + target.getString() + (destEnabled ? " ▶" : ""), dx + 156 - textX);
		graphics.text(font, label, textX, dy + 4, destText, destText == 0xFFFFFFFF);

		graphics.text(font, Component.translatable("jtechstorage.nest.package"), x + 8, y + 56, GuiTheme.text(), false);

		if (state.canSend()) {
			for (int i = 0; i < 9; i++) {
				if (menu.getSlot(i).getItem().isEmpty()) {
					graphics.text(font, "+", x + 8 + i * 18 + 5, y + NestMenu.SHIP_Y + 4, GuiTheme.slotHint(), false);
				}
			}
		}

		boolean canFly = state.phoenix() == 1 && state.selected() >= 0;
		button(graphics, 8, 88, 80, 16, Component.translatable("jtechstorage.nest.send"), state.canSend() && canFly,
				mouseX, mouseY, true);
		button(graphics, 92, 88, 76, 16, Component.translatable(state.daily() ? "jtechstorage.nest.daily.on" : "jtechstorage.nest.daily.off"),
				state.canSettings(), mouseX, mouseY, state.daily());

		// Status
		Component status = switch (state.phoenix()) {
			case 0 -> Component.translatable("jtechstorage.nest.phoenix.none");
			case 2 -> Component.translatable("jtechstorage.nest.phoenix.away");
			case 3 -> Component.translatable("jtechstorage.nest.phoenix.departing");
			default -> Component.translatable("jtechstorage.nest.phoenix.home");
		};
		// auf der dunklen Statusfläche: kräftige, helle Farben
		boolean darkStrip = GuiTheme.onColor(GuiTheme.slot()) == 0xFFFFFFFF;
		int statusColor = switch (state.phoenix()) {
			case 0 -> darkStrip ? 0xFFFF8F72 : 0xFF6A1000;
			case 1 -> darkStrip ? 0xFF7CEB96 : 0xFF0E3E12;
			default -> darkStrip ? 0xFFFFC56B : 0xFF4A2C00;
		};
		GuiTheme.label(graphics, font, status, x + 9, y + 109, W - 18, statusColor);

		Component inbox = state.incoming() > 0
				? Component.translatable("jtechstorage.nest.inbox.incoming", state.incoming())
				: Component.translatable("jtechstorage.nest.inbox");
		graphics.text(font, inbox, x + 8, y + 121, GuiTheme.text(), false);
	}

	private void button(GuiGraphicsExtractor graphics, int bx, int by, int w, int h, Component label, boolean enabled,
			int mouseX, int mouseY, boolean fire) {
		boolean hover = enabled && over(bx, by, w, h, mouseX, mouseY);
		int face = !enabled ? GuiTheme.DISABLED_FACE : fire ? (hover ? 0xFFE8701E : 0xFFC0501A) : (hover ? GuiTheme.accentHover() : GuiTheme.accent());
		GuiTheme.labeled(graphics, font, leftPos + bx, topPos + by, w, h, label, face, enabled);
	}

	@Override
	protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		super.extractTooltip(graphics, mouseX, mouseY);
		NestStatePayload.State state = state();

		if (over(118, 18, 50, 14, mouseX, mouseY)) {
			PhoenixPost.Accept accept = PhoenixPost.Accept.byId(state.accept());
			graphics.setComponentTooltipForNextFrame(font, List.of(
					Component.translatable("jtechstorage.nest.accept.title"),
					Component.translatable(accept.translationKey()).withStyle(ChatFormatting.GOLD),
					Component.translatable("jtechstorage.nest.accept.tooltip").withStyle(ChatFormatting.GRAY)), mouseX, mouseY);
		} else if (over(92, 88, 76, 16, mouseX, mouseY)) {
			graphics.setComponentTooltipForNextFrame(font, List.of(
					Component.translatable("jtechstorage.nest.daily.tooltip").withStyle(ChatFormatting.GRAY)), mouseX, mouseY);
		} else if (over(8, 36, 160, 14, mouseX, mouseY)) {
			graphics.setComponentTooltipForNextFrame(font, List.of(
					Component.translatable("jtechstorage.nest.destination.tooltip").withStyle(ChatFormatting.GRAY)), mouseX, mouseY);
		} else if (over(8, 54, 160, 10, mouseX, mouseY)) {
			graphics.setComponentTooltipForNextFrame(font, List.of(
					Component.translatable("jtechstorage.nest.package.tooltip").withStyle(ChatFormatting.GRAY)), mouseX, mouseY);
		}
	}

	// ------------------------------------------------------------------
	// Eingabe
	// ------------------------------------------------------------------

	private void send(NestActionPayload.Action action, String text, int value) {
		ClientPlayNetworking.send(new NestActionPayload(menu.containerId, action, text, value));
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		double mx = event.x();
		double my = event.y();
		NestStatePayload.State state = state();

		if (nameBox.isMouseOver(mx, my) && state.canSettings()) {
			setFocused(nameBox);
			nameBox.setFocused(true);
			return nameBox.mouseClicked(event, doubleClick);
		}

		nameBox.setFocused(false);

		if (state.canSettings() && over(100, 18, 14, 14, mx, my)) {
			send(NestActionPayload.Action.RENAME, nameBox.getValue().trim(), 0);
			return true;
		}

		if (state.canSettings() && over(118, 18, 50, 14, mx, my)) {
			send(NestActionPayload.Action.ACCEPT, "", state.accept() + (event.button() == 1 ? -1 : 1));
			return true;
		}

		if (state.canSend() && !state.destinations().isEmpty() && over(8, 36, 160, 14, mx, my)) {
			int size = state.destinations().size();
			int next = state.selected() < 0 ? 0 : Math.floorMod(state.selected() + (event.button() == 1 ? -1 : 1), size);
			send(NestActionPayload.Action.DESTINATION, "", next);
			return true;
		}

		if (state.canSend() && over(8, 88, 80, 16, mx, my)) {
			send(NestActionPayload.Action.SEND, "", 0);
			return true;
		}

		if (state.canSettings() && over(92, 88, 76, 16, mx, my)) {
			send(NestActionPayload.Action.DAILY, "", 0);
			return true;
		}

		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (nameBox.isFocused()) {
			if (event.isEscape()) {
				nameBox.setFocused(false);
				setFocused(null);
			} else if (event.key() == 257 || event.key() == 335) {
				send(NestActionPayload.Action.RENAME, nameBox.getValue().trim(), 0);
				nameBox.setFocused(false);
				setFocused(null);
			} else {
				nameBox.keyPressed(event);
			}

			return true;
		}

		return super.keyPressed(event);
	}

	@Override
	public boolean charTyped(CharacterEvent event) {
		if (nameBox.isFocused()) {
			return nameBox.charTyped(event);
		}

		return super.charTyped(event);
	}
}
