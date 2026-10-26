package dev.bartels.jtechstorage.client.cosmetic;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import dev.bartels.jtechstorage.JTechStorage;
import dev.bartels.jtechstorage.client.gui.GuiFx;
import dev.bartels.jtechstorage.client.gui.GuiTheme;
import dev.bartels.jtechstorage.client.gui.ThemedScreen;
import dev.bartels.jtechstorage.cosmetic.Cosmetic;
import dev.bartels.jtechstorage.cosmetic.Cosmetics;
import dev.bartels.jtechstorage.net.CosmeticEquipPayload;

/**
 * Cosmetics menu (v0.34): the player's own figure on the left (spins; drag with the mouse to rotate), the slots as
 * tabs on the right and the cosmetics as cards. Locked cards name the progress that unlocks them.
 */
public class CosmeticsScreen extends ThemedScreen {
	private static final int W = 340;
	private static final int H = 214;
	private static final int PREVIEW_W = 112;
	private static final int CARD_W = 101;
	private static final int CARD_H = 30;
	private static final int GAP = 4;
	private static final Cosmetic.Slot[] SLOTS = Cosmetic.Slot.values();

	private static Cosmetic.Slot slot = Cosmetic.Slot.BACK;
	private float spin = 200.0f;
	private boolean dragging;
	private double lastDragX;
	private long lastInput;

	public CosmeticsScreen() {
		super(Component.translatable("jtechstorage.cosmetics.title"));
	}

	@Override
	protected void init() {
		CosmeticsClient.requestUnlocks();
	}

	private int left() {
		return (width - W) / 2;
	}

	private int top() {
		return (height - H) / 2;
	}

	private int gridX() {
		return left() + PREVIEW_W + 14;
	}

	private int tabY() {
		return top() + 24;
	}

	private int gridY() {
		return top() + 46;
	}

	private int tabW() {
		return (W - PREVIEW_W - 22 - (SLOTS.length - 1) * 2) / SLOTS.length;
	}

	private List<Cosmetic> current() {
		List<Cosmetic> list = new ArrayList<>();

		for (Cosmetic c : Cosmetic.values()) {
			if (c.slot == slot) {
				list.add(c);
			}
		}

		return list;
	}

	private static boolean unlocked(Cosmetic c) {
		return c.advancement == null || CosmeticsClient.UNLOCKED.contains(c.id());
	}

	private static boolean over(int x, int y, int w, int h, double mx, double my) {
		return mx >= x && mx < x + w && my >= y && my < y + h;
	}

	// ------------------------------------------------------------------ Drawing

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractRenderState(graphics, mouseX, mouseY, delta);
		Player player = Minecraft.getInstance().player;
		int x = left();
		int y = top();
		GuiTheme.window(graphics, x, y, W, H);
		GuiTheme.title(graphics, font, title, x + 10, y + 8);

		// Figure
		int px = x + 8;
		int py = y + 22;
		int ph = H - 30;
		GuiTheme.sunken(graphics, px, py, PREVIEW_W, ph);
		GuiFx.shimmer(graphics, px, py, PREVIEW_W, ph);

		if (!dragging && System.currentTimeMillis() - lastInput > 2500) {
			spin += delta * 1.2f;
		}

		if (player != null) {
			preview(graphics, player, px + 2, py + 2, px + PREVIEW_W - 2, py + ph - 14);
			graphics.text(font, Component.translatable("jtechstorage.cosmetics.drag"), px + (PREVIEW_W - font.width(Component.translatable("jtechstorage.cosmetics.drag"))) / 2,
					py + ph - 11, GuiTheme.subtle(), false);
		}

		// Tabs
		int tw = tabW();

		for (int i = 0; i < SLOTS.length; i++) {
			int tx = gridX() + i * (tw + 2);
			boolean hover = over(tx, tabY(), tw, 16, mouseX, mouseY);
			GuiTheme.toggle(graphics, tx, tabY(), tw, 16, SLOTS[i] == slot, hover);
			Component label = Component.translatable("jtechstorage.cosmetics.slot." + SLOTS[i].id());
			String text = GuiTheme.fit(font, label.getString(), tw - 4);
			int color = SLOTS[i] == slot ? GuiTheme.onColor(GuiTheme.accent()) : GuiTheme.text();
			graphics.text(font, text, tx + (tw - font.width(text) + 1) / 2, tabY() + 4, color, false);
		}

		// Cards
		List<Cosmetic> list = current();
		Cosmetic equipped = player == null ? null : Cosmetics.equipped(player, slot);
		Cosmetic hovered = null;

		for (int i = 0; i < list.size(); i++) {
			Cosmetic c = list.get(i);
			int cx = gridX() + (i % 2) * (CARD_W + GAP);
			int cy = gridY() + (i / 2) * (CARD_H + GAP);
			boolean open = unlocked(c);
			boolean on = c == equipped;
			boolean hover = over(cx, cy, CARD_W, CARD_H, mouseX, mouseY);

			if (hover) {
				hovered = c;
			}

			card(graphics, c, cx, cy, open, on, hover);
		}

		// Hint at the bottom
		Component hint = slot == Cosmetic.Slot.EMOTE
				? Component.translatable("jtechstorage.cosmetics.emote_hint", CosmeticsClient.EMOTE_KEY.getTranslatedKeyMessage())
				: Component.translatable("jtechstorage.cosmetics.hint");
		graphics.text(font, GuiTheme.fit(font, hint.getString(), W - PREVIEW_W - 26), gridX(), y + H - 32, GuiTheme.subtle(), false);
		boolean doneHover = over(x + W - 72, y + H - 22, 62, 16, mouseX, mouseY);
		GuiTheme.button(graphics, font, x + W - 72, y + H - 22, 62, 16, CommonComponents.GUI_DONE, true, doneHover);

		if (hovered != null) {
			List<FormattedCharSequence> lines = new ArrayList<>();
			lines.add(Component.translatable(hovered.translationKey()).withStyle(ChatFormatting.LIGHT_PURPLE).getVisualOrderText());
			lines.addAll(font.split(Component.translatable(hovered.translationKey() + ".desc").withStyle(ChatFormatting.GRAY), 180));

			if (!unlocked(hovered)) {
				lines.add(Component.translatable("jtechstorage.cosmetics.locked",
						Component.translatable("advancements.jtechstorage." + hovered.advancement + ".title")).withStyle(ChatFormatting.RED).getVisualOrderText());
			}

			graphics.setTooltipForNextFrame(font, lines, mouseX, mouseY);
		}
	}

	private void card(GuiGraphicsExtractor graphics, Cosmetic c, int x, int y, boolean open, boolean on, boolean hover) {
		if (on) {
			GuiTheme.round(graphics, x - 1, y - 1, CARD_W + 2, CARD_H + 2, 3, GuiTheme.accent());
		}

		GuiTheme.panel(graphics, x, y, CARD_W, CARD_H);

		if (hover && open) {
			GuiFx.sheen(graphics, x, y, CARD_W, CARD_H);
		}

		Identifier icon = JTechStorage.id("textures/gui/cosmetics/" + c.id() + ".png");
		graphics.blit(RenderPipelines.GUI_TEXTURED, icon, x + 3, y + 3, 0, 0, 24, 24, 32, 32, 32, 32, open ? 0xFFFFFFFF : 0xFF404040);
		Component name = Component.translatable(c.translationKey());
		int textColor = open ? (on ? GuiTheme.accentText() : GuiTheme.text()) : GuiTheme.subtle();
		graphics.text(font, GuiTheme.fit(font, name.getString(), CARD_W - 32), x + 30, y + 6, textColor, false);
		Component state = !open ? Component.translatable("jtechstorage.cosmetics.state.locked")
				: on ? Component.translatable("jtechstorage.cosmetics.state.on") : Component.translatable("jtechstorage.cosmetics.state.off");
		graphics.text(font, state, x + 30, y + 17, !open ? GuiTheme.bad() : on ? GuiTheme.good() : GuiTheme.subtle(), false);
	}

	/** The player's own figure, rotated by {@link #spin} (also from behind, for wings and cape). */
	private void preview(GuiGraphicsExtractor graphics, Player player, int x0, int y0, int x1, int y1) {
		try {
			EntityRenderState state = Minecraft.getInstance().getEntityRenderDispatcher().extractEntity(player, 1.0f);

			if (state instanceof LivingEntityRenderState living) {
				living.bodyRot = 180.0f + spin;
				living.yRot = 0.0f;
				living.xRot = 0.0f;
				living.nameTag = null;
			}

			float scale = (y1 - y0) / 2.6f;
			Vector3f offset = new Vector3f(0.0f, state.boundingBoxHeight / 2.0f + 0.15f, 0.0f);
			graphics.entity(state, scale, offset, new Quaternionf().rotateZ(Mth.PI), new Quaternionf().rotateX(-0.12f), x0, y0, x1, y1);
		} catch (RuntimeException ignored) {
			// preview cannot be drawn – the menu stays usable
		}
	}

	// ------------------------------------------------------------------ Input

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		double mx = event.x();
		double my = event.y();
		int tw = tabW();

		for (int i = 0; i < SLOTS.length; i++) {
			if (over(gridX() + i * (tw + 2), tabY(), tw, 16, mx, my)) {
				slot = SLOTS[i];
				click();
				return true;
			}
		}

		List<Cosmetic> list = current();
		Player player = Minecraft.getInstance().player;

		for (int i = 0; i < list.size(); i++) {
			int cx = gridX() + (i % 2) * (CARD_W + GAP);
			int cy = gridY() + (i / 2) * (CARD_H + GAP);

			if (over(cx, cy, CARD_W, CARD_H, mx, my)) {
				Cosmetic c = list.get(i);

				if (unlocked(c) && player != null) {
					boolean on = Cosmetics.equipped(player, slot) == c;
					ClientPlayNetworking.send(new CosmeticEquipPayload(slot.id(), on ? "" : c.id()));
					click();
				}

				return true;
			}
		}

		if (over(left() + W - 72, top() + H - 22, 62, 16, mx, my)) {
			onClose();
			return true;
		}

		if (over(left() + 8, top() + 22, PREVIEW_W, H - 30, mx, my)) {
			dragging = true;
			lastDragX = mx;
			lastInput = System.currentTimeMillis();
			return true;
		}

		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
		if (dragging) {
			spin += (float) (event.x() - lastDragX) * 2.0f;
			lastDragX = event.x();
			lastInput = System.currentTimeMillis();
			return true;
		}

		return super.mouseDragged(event, dx, dy);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		dragging = false;
		return super.mouseReleased(event);
	}

	private void click() {
		Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(
				SoundEvents.AMETHYST_BLOCK_CHIME, 1.4f));
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
