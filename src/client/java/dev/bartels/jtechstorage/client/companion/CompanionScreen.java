package dev.bartels.jtechstorage.client.companion;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

import dev.bartels.jtechstorage.ModConfig;
import dev.bartels.jtechstorage.client.gui.GuiSettings;
import dev.bartels.jtechstorage.client.gui.GuiTheme;
import dev.bartels.jtechstorage.companion.Companion;
import dev.bartels.jtechstorage.companion.CompanionTalents;
import dev.bartels.jtechstorage.net.CompanionActionPayload;
import dev.bartels.jtechstorage.net.CompanionStatusPayload;

/**
 * Begleiter-Menü (v0.26, Taste K oder Shift + Rechtsklick): Übersicht mit Porträt, Werten und Befehlen sowie der
 * Talentbaum. Alle Werte kommen vom Server ({@link CompanionStatusPayload}), Klicks gehen als {@link CompanionActionPayload} zurück.
 */
public class CompanionScreen extends Screen {
	private static final int W = 256;
	private static final int H = 240;
	private static final int NODE = 22;

	private int tab;
	private int left;
	private int top;

	private record Btn(int x, int y, int w, int h, Component label, boolean enabled, Runnable action, Component tooltip) {
		boolean over(double mx, double my) {
			return mx >= x && mx < x + w && my >= y && my < y + h;
		}
	}

	public CompanionScreen(int tab) {
		super(Component.translatable("jtechstorage.companion.menu"));
		this.tab = tab;
	}

	@Override
	protected void init() {
		left = (width - W) / 2;
		top = (height - H) / 2;
	}

	private CompanionStatusPayload s() {
		return CompanionClient.status();
	}

	private void click() {
		minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
	}

	// ------------------------------------------------------------------
	// Knöpfe (werden jedes Bild neu aufgebaut, damit sie zum Zustand passen)
	// ------------------------------------------------------------------

	private List<Btn> buttons() {
		List<Btn> list = new ArrayList<>();
		CompanionStatusPayload s = s();

		for (int i = 0; i < 2; i++) {
			int t = i;
			list.add(new Btn(left + 8 + i * 82, top + 20, 78, 14, Component.translatable(i == 0 ? "jtechstorage.companion.tab.overview"
					: "jtechstorage.companion.tab.talents"), s.present() && s.loaded() || i == 0, () -> tab = t, null));
		}

		if (!s.present()) {
			return list;
		}

		if (!s.loaded()) {
			list.add(new Btn(left + (W - 100) / 2, top + 110, 100, 16, Component.translatable("jtechstorage.companion.btn.call"), true,
					() -> CompanionClient.send(CompanionActionPayload.CALL, 0), Component.translatable("jtechstorage.companion.btn.call.tip")));
			return list;
		}

		if (tab == 0) {
			int bw = (W - 16 - 8) / 3;
			int by = top + 160;
			list.add(new Btn(left + 8, by, bw, 16, Component.translatable(s.flag(0) ? "jtechstorage.companion.btn.follow" : "jtechstorage.companion.btn.stay"),
					!s.flag(5), () -> CompanionClient.send(CompanionActionPayload.STAY, 0), Component.translatable("jtechstorage.companion.btn.stay.tip")));
			list.add(new Btn(left + 12 + bw, by, bw, 16, Component.translatable("jtechstorage.companion.stance." + s.stance()), true,
					() -> CompanionClient.send(CompanionActionPayload.STANCE, s.stance() + 1),
					Component.translatable("jtechstorage.companion.btn.stance.tip." + s.stance())));
			list.add(new Btn(left + 16 + 2 * bw, by, bw, 16, Component.translatable("jtechstorage.companion.btn.call"), true,
					() -> CompanionClient.send(CompanionActionPayload.CALL, 0), Component.translatable("jtechstorage.companion.btn.call.tip")));
			int by2 = by + 20;

			switch (s.species()) {
				case "shroom" -> {
					list.add(new Btn(left + 8, by2, bw, 16, Component.translatable("jtechstorage.companion.btn.heal"), s.special() == 0,
							() -> CompanionClient.send(CompanionActionPayload.HEAL, 0), Component.translatable("jtechstorage.companion.btn.heal.tip")));
					list.add(new Btn(left + 12 + bw, by2, bw, 16, Component.translatable(s.flag(2) ? "jtechstorage.companion.btn.heal_auto"
							: "jtechstorage.companion.btn.heal_manual"), true, () -> CompanionClient.send(CompanionActionPayload.HEAL_MODE, 0),
							Component.translatable("jtechstorage.companion.btn.heal_mode.tip")));
				}
				case "raven" -> {
					list.add(new Btn(left + 8, by2, bw, 16, Component.translatable("jtechstorage.companion.btn.bag"), !s.flag(3) && !s.flag(5),
							() -> CompanionClient.send(CompanionActionPayload.BAG, 0), Component.translatable("jtechstorage.companion.btn.bag.tip")));
					list.add(new Btn(left + 12 + bw, by2, bw, 16, Component.translatable("jtechstorage.companion.btn.explore"),
							!s.flag(3) && !s.flag(1) && s.timer() == 0, () -> CompanionClient.send(CompanionActionPayload.EXPLORE, 0),
							Component.translatable("jtechstorage.companion.btn.explore.tip")));
				}
				default -> {
				}
			}

			// Spezialangriff (v0.25.3)
			int special = CompanionTalents.chosenSpecial(s.talents());

			if (special >= 0) {
				String id = CompanionTalents.tree(s.species()).get(CompanionTalents.SPECIAL + special).id();
				Component label = s.specialCooldown() > 0
						? Component.translatable("jtechstorage.companion.btn.special_cd", (s.specialCooldown() + 19) / 20)
						: Component.translatable("jtechstorage.talent." + id);
				list.add(new Btn(left + 16 + 2 * bw, by2, bw, 16, label, s.specialCooldown() == 0 && !s.flag(1),
						() -> CompanionClient.send(CompanionActionPayload.SPECIAL, 0),
						Component.translatable("jtechstorage.companion.btn.special.tip", Component.translatable("jtechstorage.talent." + id),
								Component.translatable("jtechstorage.talent." + id + ".desc"))));
			}

			boolean hud = GuiSettings.get().companionHud;
			list.add(new Btn(left + 16 + 2 * bw, by2 + 20, bw, 16, Component.translatable(hud ? "jtechstorage.companion.btn.hud_on" : "jtechstorage.companion.btn.hud_off"),
					true, () -> {
						GuiSettings.get().companionHud = !GuiSettings.get().companionHud;
						GuiSettings.get().save();
					}, Component.translatable("jtechstorage.companion.btn.hud.tip")));
		} else {
			int spent = CompanionTalents.spent(s.talents());
			list.add(new Btn(left + W - 8 - 92, top + H - 22, 92, 16, Component.translatable("jtechstorage.talent.reset"), spent > 0,
					() -> CompanionClient.send(CompanionActionPayload.RESET, 0), Component.translatable("jtechstorage.talent.reset.tip")));
		}

		return list;
	}

	// ------------------------------------------------------------------
	// Zeichnen
	// ------------------------------------------------------------------

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractBackground(graphics, mouseX, mouseY, delta);
		GuiTheme.panel(graphics, left, top, W, H);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		super.extractRenderState(g, mouseX, mouseY, delta);
		CompanionStatusPayload s = s();
		Component title = s.present() && s.loaded()
				? Component.translatable("jtechstorage.companion.menu.title", CompanionClient.name(s), Component.translatable("jtechstorage.companion.stage." + s.stage()))
				: this.title;
		g.text(font, CompanionClient.speciesSymbol(s.species()), left + 9, top + 8, 0xFFE0B0FF, true);
		GuiTheme.label(g, font, title, left + 21, top + 8, W - 30, GuiTheme.text());

		Btn hovered = null;

		for (Btn b : buttons()) {
			boolean active = b.y() == top + 20 && (b.x() - left - 8) / 82 == tab;
			int color = !b.enabled() ? GuiTheme.DISABLED_FACE : active ? GuiTheme.accent()
					: b.over(mouseX, mouseY) ? GuiTheme.accentHover() : b.y() == top + 20 ? GuiTheme.mix(GuiTheme.face(), 0xFF000000, 0.18f) : GuiTheme.accent();
			GuiTheme.labeled(g, font, b.x(), b.y(), b.w(), b.h(), b.label(), color, b.enabled());

			if (b.over(mouseX, mouseY) && b.tooltip() != null) {
				hovered = b;
			}
		}

		if (!s.present()) {
			wrapped(g, Component.translatable("jtechstorage.companion.menu.none"), left + 14, top + 50, W - 28, GuiTheme.text());
			return;
		}

		if (!s.loaded()) {
			wrapped(g, Component.translatable("jtechstorage.companion.menu.unloaded"), left + 14, top + 50, W - 28, GuiTheme.text());
			return;
		}

		if (tab == 0) {
			overview(g, s, mouseX, mouseY);
		} else {
			talents(g, s, mouseX, mouseY);
		}

		if (hovered != null) {
			g.setTooltipForNextFrame(font, font.split(hovered.tooltip(), 200), mouseX, mouseY);
		}
	}

	private void wrapped(GuiGraphicsExtractor g, Component text, int x, int y, int w, int color) {
		for (var line : font.split(text, w)) {
			g.text(font, line, x, y, color, false);
			y += 10;
		}
	}

	private void overview(GuiGraphicsExtractor g, CompanionStatusPayload s, int mouseX, int mouseY) {
		// Porträt: das echte Modell, wenn er in der Nähe ist
		int px = left + 8;
		int py = top + 40;
		int pw = 78;
		int ph = 112;
		GuiTheme.sunken(g, px, py, pw, ph);
		Companion companion = CompanionClient.nearby(minecraft);

		if (companion != null) {
			float size = Math.max(companion.getBbHeight(), companion.getBbWidth() * 1.2f);
			int scale = (int) Math.clamp(62 / Math.max(0.3f, size), 10, 60);
			InventoryScreen.extractEntityInInventoryFollowsMouse(g, px + 1, py + 1, px + pw - 1, py + ph - 1, scale, 0.0625f, mouseX, mouseY, companion);
		} else {
			String symbol = CompanionClient.speciesSymbol(s.species());
			g.text(font, symbol, px + (pw - font.width(symbol)) / 2, py + ph / 2 - 4, CompanionClient.speciesColor(s.species()), false);
			Component far = Component.translatable(s.flag(5) ? "jtechstorage.companion.hud.other_dimension" : "jtechstorage.companion.menu.far");
			int fw = font.width(far);
			g.text(font, GuiTheme.fit(font, far.getString(), pw - 6), px + Math.max(3, (pw - fw) / 2), py + ph - 14, GuiTheme.onColor(GuiTheme.slot()), false);
		}

		int x = left + 94;
		int w = W - 102;
		int y = top + 40;
		g.text(font, Component.translatable("jtechstorage.companion.menu.level", s.level()), x, y, GuiTheme.text(), false);
		ModConfig.Companions config = ModConfig.get().companions;

		if (s.stage() < 3) {
			Component next = Component.translatable("jtechstorage.companion.menu.next_stage", s.stage() + 1, s.stage() == 1 ? config.stage2Level : config.stage3Level);
			g.text(font, next, x + w - font.width(next), y, GuiTheme.subtle(), false);
		}

		CompanionHud.bar(g, x, y + 11, w, 7, CompanionClient.levelProgress(s), 0xFF9A6AE6);
		int from = Companion.xpForLevel(s.level());
		int to = Companion.xpForLevel(s.level() + 1);
		g.text(font, Component.translatable("jtechstorage.companion.menu.xp", s.xp() - from, to - from), x, y + 21, GuiTheme.subtle(), false);
		CompanionHud.bar(g, x, y + 33, w, 7, s.maxHealth() > 0 ? s.health() / s.maxHealth() : 0, s.flag(1) ? 0xFF886060 : 0xFFD04848);
		g.text(font, Component.translatable("jtechstorage.companion.menu.health", fmt(s.health()), fmt(s.maxHealth())), x, y + 43, GuiTheme.subtle(), false);

		int bx = x;
		int by = y + 55;
		GuiTheme.sunken(g, bx, by, w, 57);
		int it = GuiTheme.onColor(GuiTheme.slot());
		List<Component> lines = new ArrayList<>();
		lines.add(Component.translatable("jtechstorage.companion.menu.stats", fmt(s.damage()), fmt(s.armor())));
		lines.add(Component.translatable("jtechstorage.companion.menu.attack", String.format(java.util.Locale.ROOT, "%.1f", s.attackTicks() / 20.0)));
		net.minecraft.network.chat.MutableComponent mode = Component.translatable("jtechstorage.companion.menu.mode",
				Component.translatable(CompanionClient.modeKey(s)), Component.translatable("jtechstorage.companion.stance." + s.stance()));

		if (s.flag(6)) {
			mode.append(Component.translatable(s.species().equals("mantis") ? "jtechstorage.companion.menu.base_mantis" : "jtechstorage.companion.menu.base"));
		}

		lines.add(mode);

		switch (s.species()) {
			case "shroom" -> lines.add(s.special() > 0
					? Component.translatable("jtechstorage.companion.menu.heal_cooldown", (s.special() + 19) / 20)
					: Component.translatable(s.flag(2) ? "jtechstorage.companion.menu.heal_ready_auto" : "jtechstorage.companion.menu.heal_ready_manual"));
			case "raven" -> {
				lines.add(Component.translatable(s.flag(4) ? "jtechstorage.companion.menu.bag" : "jtechstorage.companion.menu.bag_no_terminal",
						s.special(), s.specialMax()));
				lines.add(s.timer() > 0 ? Component.translatable("jtechstorage.companion.menu.exploring", (s.timer() + 1199) / 1200)
						: s.timer() < 0 ? Component.translatable("jtechstorage.companion.menu.explore_cooldown", (-s.timer() + 1199) / 1200)
						: Component.translatable("jtechstorage.companion.menu.explore_ready"));
			}
			case "mantis" -> {
				if (s.stage() == 3) {
					lines.add(Component.translatable("jtechstorage.companion.menu.flight", String.format(java.util.Locale.ROOT, "%.1f", s.specialMax() / 20.0)));
				}
			}
			default -> {
			}
		}

		if (s.treatTicks() > 0) {
			lines.add(Component.translatable("jtechstorage.companion.menu.treat", (s.treatTicks() + 1199) / 1200).withStyle(ChatFormatting.GOLD));
		}

		for (int i = 0; i < Math.min(5, lines.size()); i++) {
			GuiTheme.label(g, font, lines.get(i), bx + 4, by + 4 + i * 10, w - 8, i == 4 || lines.get(i).getStyle().getColor() != null ? 0xFFFFD27A : it);
		}

		Component hint = Component.translatable("jtechstorage.companion.menu.hint");
		GuiTheme.label(g, font, hint, left + 8, top + H - 13, W - 16, GuiTheme.subtle());
	}

	private static String fmt(float value) {
		return value == Math.floor(value) ? String.valueOf((int) value) : String.format(java.util.Locale.ROOT, "%.1f", value);
	}

	// ------------------------------------------------------------------
	// Talentbaum
	// ------------------------------------------------------------------

	private int nodeX(int branch) {
		int cw = (W - 16) / CompanionTalents.BRANCHES;
		return left + 8 + branch * cw + (cw - NODE) / 2;
	}

	private int nodeY(int n) {
		return top + 54 + n * 27;
	}

	private void talents(GuiGraphicsExtractor g, CompanionStatusPayload s, int mouseX, int mouseY) {
		CompanionTalents.Tree tree = CompanionTalents.tree(s.species());
		int[] ranks = s.talents();
		int spent = CompanionTalents.spent(ranks);
		int points = CompanionTalents.points(s.level());
		Component free = Component.translatable("jtechstorage.talent.points", points - spent, points);
		g.text(font, free, left + 8, top + 192, points - spent > 0 ? GuiTheme.good() : GuiTheme.subtle(), false);

		if (points < CompanionTalents.MAX_POINTS) {
			Component next = Component.translatable("jtechstorage.talent.next_point", (points + 1) * CompanionTalents.LEVELS_PER_POINT);
			g.text(font, next, left + W - 8 - font.width(next), top + 192, GuiTheme.subtle(), false);
		}
		int cw = (W - 16) / CompanionTalents.BRANCHES;
		int chosen = CompanionTalents.chosenSpecial(ranks);
		int hover = -1;

		for (int b = 0; b < CompanionTalents.BRANCHES; b++) {
			Component name = Component.translatable("jtechstorage.talent.branch." + tree.branches().get(b));
			int cx = left + 8 + b * cw;
			g.text(font, name, cx + (cw - font.width(name)) / 2, top + 42, GuiTheme.text(), false);

			for (int n = 0; n < CompanionTalents.PER_BRANCH; n++) {
				int index = b * CompanionTalents.PER_BRANCH + n;
				CompanionTalents.Talent talent = tree.get(index);
				int nx = nodeX(b);
				int ny = nodeY(n);

				boolean specialRow = CompanionTalents.isSpecial(index);
				boolean prevDone = index == CompanionTalents.MASTERY ? chosen >= 0 : n > 0 && ranks[index - 1] > 0;

				if (n > 0 && !specialRow) {
					g.fill(nx + NODE / 2 - 1, ny - 5, nx + NODE / 2 + 1, ny, prevDone ? GuiTheme.accent() : 0xFF555560);
				}

				boolean stageLocked = s.stage() < talent.minStage();
				boolean prevLocked = specialRow ? chosen >= 0 && ranks[index] == 0 : n > 0 && !prevDone;
				boolean learnable = CompanionTalents.blocker(s.species(), ranks, index, s.stage(), s.level()) == null;
				boolean over = mouseX >= nx && mouseX < nx + NODE && mouseY >= ny && mouseY < ny + NODE;
				GuiTheme.sunken(g, nx, ny, NODE, NODE);
				int face = stageLocked || prevLocked ? GuiTheme.DISABLED_FACE
						: ranks[index] > 0 ? talent.color() : GuiTheme.mix(talent.color(), 0xFF6B6B70, 0.6f);
				g.fill(nx + 3, ny + 3, nx + NODE - 3, ny + NODE - 3, face);
				g.fill(nx + 3, ny + 3, nx + NODE - 3, ny + 4, GuiTheme.mix(face, 0xFFFFFFFF, 0.35f));
				g.text(font, talent.symbol(), nx + (NODE - font.width(talent.symbol())) / 2 + 1, ny + 7, stageLocked || prevLocked ? 0xFF9A9AA0 : 0xFFFFFFFF, true);

				if (learnable) {
					// lernbar: leuchtender Rahmen
					int glow = 0xFF7CEB96;
					g.fill(nx - 1, ny - 1, nx + NODE + 1, ny, glow);
					g.fill(nx - 1, ny + NODE, nx + NODE + 1, ny + NODE + 1, glow);
					g.fill(nx - 1, ny, nx, ny + NODE, glow);
					g.fill(nx + NODE, ny, nx + NODE + 1, ny + NODE, glow);
				}

				if (over) {
					hover = index;
					g.fill(nx - 1, ny - 1, nx + NODE + 1, ny, 0xFFFFFFFF);
					g.fill(nx - 1, ny + NODE, nx + NODE + 1, ny + NODE + 1, 0xFFFFFFFF);
					g.fill(nx - 1, ny, nx, ny + NODE, 0xFFFFFFFF);
					g.fill(nx + NODE, ny, nx + NODE + 1, ny + NODE, 0xFFFFFFFF);
				}

				String rank = ranks[index] + "/" + talent.max();
				g.text(font, rank, nx + NODE + 3, ny + 8, ranks[index] == talent.max() ? GuiTheme.good() : GuiTheme.subtle(), false);

				if (stageLocked) {
					String st = Component.translatable("jtechstorage.talent.stage_short", talent.minStage()).getString();
					g.text(font, st, nx - 3 - font.width(st), ny + 8, GuiTheme.bad(), false);
				}
			}
		}

		Component cost = Component.translatable("jtechstorage.talent.reset_hint");
		GuiTheme.label(g, font, cost, left + 8, top + H - 17, W - 16 - 100, GuiTheme.subtle());

		if (hover >= 0) {
			g.setComponentTooltipForNextFrame(font, tooltip(s, tree, ranks, hover), mouseX, mouseY);
		}
	}

	private List<Component> tooltip(CompanionStatusPayload s, CompanionTalents.Tree tree, int[] ranks, int index) {
		CompanionTalents.Talent talent = tree.get(index);
		List<Component> lines = new ArrayList<>();
		lines.add(Component.translatable("jtechstorage.talent." + talent.id()).withStyle(ChatFormatting.LIGHT_PURPLE)
				.append(Component.literal("  " + ranks[index] + "/" + talent.max()).withStyle(ChatFormatting.GRAY)));

		for (var line : font.split(Component.translatable("jtechstorage.talent." + talent.id() + ".desc"), 190)) {
			StringBuilder text = new StringBuilder();
			line.accept((pos, style, cp) -> {
				text.appendCodePoint(cp);
				return true;
			});
			lines.add(Component.literal(text.toString()).withStyle(ChatFormatting.WHITE));
		}

		if (CompanionTalents.isSpecial(index)) {
			lines.add(Component.translatable("jtechstorage.talent.special_hint").withStyle(ChatFormatting.GOLD));
		}

		String blocker = CompanionTalents.blocker(s.species(), ranks, index, s.stage(), s.level());

		if (blocker == null) {
			lines.add(Component.translatable("jtechstorage.talent.click").withStyle(ChatFormatting.GREEN));
		} else if (blocker.equals("stage")) {
			lines.add(Component.translatable("jtechstorage.talent.blocked.stage_n", talent.minStage()).withStyle(ChatFormatting.RED));
		} else if (blocker.equals("previous")) {
			lines.add(Component.translatable("jtechstorage.talent.blocked.previous_n",
					Component.translatable("jtechstorage.talent." + tree.get(index - 1).id())).withStyle(ChatFormatting.RED));
		} else {
			lines.add(Component.translatable("jtechstorage.talent.blocked." + blocker).withStyle(blocker.equals("maxed") ? ChatFormatting.GREEN : ChatFormatting.RED));
		}

		return lines;
	}

	// ------------------------------------------------------------------
	// Eingaben
	// ------------------------------------------------------------------

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		for (Btn b : buttons()) {
			if (b.enabled() && b.over(event.x(), event.y())) {
				click();
				b.action().run();
				return true;
			}
		}

		CompanionStatusPayload s = s();

		if (tab == 1 && s.present() && s.loaded()) {
			for (int index = 0; index < CompanionTalents.COUNT; index++) {
				int nx = nodeX(index / CompanionTalents.PER_BRANCH);
				int ny = nodeY(index % CompanionTalents.PER_BRANCH);

				if (event.x() >= nx && event.x() < nx + NODE && event.y() >= ny && event.y() < ny + NODE) {
					if (CompanionTalents.blocker(s.species(), s.talents(), index, s.stage(), s.level()) == null) {
						click();
						CompanionClient.send(CompanionActionPayload.LEARN, index);
					}

					return true;
				}
			}
		}

		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (dev.bartels.jtechstorage.client.JTechStorageClient.COMPANION_MENU_KEY.matches(event)) {
			onClose();
			return true;
		}

		return super.keyPressed(event);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
