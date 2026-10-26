package dev.bartels.jtechstorage.client.gui;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import dev.bartels.jtechstorage.depths.AncientNoteItem;

/** Reading screen for Ancient Notes from the Crystal Depths (v0.32): yellowed parchment with title and text. */
public class NoteScreen extends ThemedScreen {
	private static final int W = 216;
	private static final int TEXT_W = W - 32;
	private static final int PAPER = 0xFFE9DCB4;
	private static final int PAPER_DARK = 0xFFC9B78E;
	private static final int EDGE = 0xFF8A7650;
	private static final int INK = 0xFF3A2E4A;

	private final int note;
	private List<FormattedCharSequence> title = List.of();
	private List<FormattedCharSequence> body = List.of();
	private int left;
	private int top;
	private int height2;

	public NoteScreen(int note) {
		super(Component.translatable("jtechstorage.note." + note + ".title"));
		this.note = Math.max(1, Math.min(AncientNoteItem.COUNT, note));
	}

	@Override
	protected void init() {
		title = font.split(Component.translatable("jtechstorage.note." + note + ".title").withStyle(ChatFormatting.BOLD), TEXT_W);
		body = font.split(Component.translatable("jtechstorage.note." + note + ".body"), TEXT_W);
		height2 = 20 + title.size() * 10 + 10 + body.size() * 10 + 22 + 28;
		left = (width - W) / 2;
		top = Math.max(8, (height - height2) / 2);
		addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> onClose())
				.bounds(width / 2 - 50, top + height2 + 6, 100, 20).build());
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractRenderState(graphics, mouseX, mouseY, delta);
		int right = left + W;
		int bottom = top + height2;
		// parchment with a frayed edge
		graphics.fill(left - 1, top - 1, right + 1, bottom + 1, EDGE);
		graphics.fill(left, top, right, bottom, PAPER);
		for (int x = left; x < right; x += 6) {
			int k = (x * 31 + note * 7) % 3;
			graphics.fill(x, top, x + 3, top + 1 + k, PAPER_DARK);
			graphics.fill(x + 2, bottom - 1 - (k ^ 1), x + 5, bottom, PAPER_DARK);
		}
		for (int y = top; y < bottom; y += 7) {
			int k = (y * 17 + note) % 3;
			graphics.fill(left, y, left + 1 + k, y + 4, PAPER_DARK);
			graphics.fill(right - 1 - k, y + 3, right, y + 6, PAPER_DARK);
		}
		// amethyst stain at the bottom right
		graphics.fill(right - 22, bottom - 18, right - 14, bottom - 10, 0x559A7EE0);

		int y = top + 16;
		for (FormattedCharSequence line : title) {
			graphics.text(font, line, left + W / 2 - font.width(line) / 2, y, 0xFF5A3A8A, false);
			y += 10;
		}
		graphics.fill(left + W / 2 - 40, y + 2, left + W / 2 + 40, y + 3, 0xFF9A7EE0);
		y += 10;
		for (FormattedCharSequence line : body) {
			graphics.text(font, line, left + 16, y, INK, false);
			y += 10;
		}
		Component page = Component.literal(note + " / " + AncientNoteItem.COUNT);
		graphics.text(font, page, right - 16 - font.width(page), bottom - 16, 0xFF7A6A50, false);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
