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
import dev.bartels.jtechstorage.companion.CompanionBond;
import dev.bartels.jtechstorage.companion.CompanionLooks;
import dev.bartels.jtechstorage.companion.CompanionTalents;
import dev.bartels.jtechstorage.net.CompanionActionPayload;
import dev.bartels.jtechstorage.net.CompanionStatusPayload;

/**
 * Begleiter-Menü (v0.26, Taste K oder Shift + Rechtsklick): Übersicht mit Porträt, Werten und Befehlen sowie der
 * Talentbaum. Alle Werte kommen vom Server ({@link CompanionStatusPayload}), Klicks gehen als {@link CompanionActionPayload} zurück.
 */
public class CompanionScreen extends dev.bartels.jtechstorage.client.gui.ThemedScreen {
	private static final int W = 256;
	private static final int H = 262;
	private static final int NODE = 22;
	private static final String[] TABS = {"jtechstorage.companion.tab.overview", "jtechstorage.companion.tab.talents", "jtechstorage.companion.tab.look"};
	/** Aussehen-Tab (v0.25.4): Spalte rechts neben dem Porträt, Farbbalken für die Leuchtfarbe. */
	private static final int LOOK_X = 104;
	private static final int ROW_Y = 44;
	private static final int ROW_H = 19;
	private static final int HUE_Y = 174;
	private static final int HUE_W = 144;
	private static final int GESTURE_Y = 216;

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

		for (int i = 0; i < 3; i++) {
			int t = i;
			list.add(new Btn(left + 8 + i * 80, top + 20, 76, 14, Component.translatable(TABS[i]), s.present() && s.loaded() || i == 0, () -> tab = t, null));
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
			int by = top + 188;
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

			// Bindung (v0.26.0): streicheln
			list.add(new Btn(left + 8, by2 + 20, bw, 16, Component.translatable("jtechstorage.bond.pet"), !s.flag(1),
					() -> CompanionClient.send(CompanionActionPayload.PET, 0), Component.translatable("jtechstorage.bond.pet.tip")));
			boolean hud = GuiSettings.get().companionHud;
			list.add(new Btn(left + 16 + 2 * bw, by2 + 20, bw, 16, Component.translatable(hud ? "jtechstorage.companion.btn.hud_on" : "jtechstorage.companion.btn.hud_off"),
					true, () -> {
						GuiSettings.get().companionHud = !GuiSettings.get().companionHud;
						GuiSettings.get().save();
					}, Component.translatable("jtechstorage.companion.btn.hud.tip")));
		} else if (tab == 2) {
			lookButtons(list, s);
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
		GuiTheme.window(graphics, left, top, W, H);
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
			boolean active = b.y() == top + 20 && (b.x() - left - 8) / 80 == tab;
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
		} else if (tab == 2) {
			look(g, s, mouseX, mouseY);
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
		portrait(g, s, left + 8, top + 40, 78, 112, mouseX, mouseY);
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
		GuiTheme.sunken(g, bx, by, w, 67);
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
				lines.add(s.decoy() > 0 ? Component.translatable("jtechstorage.companion.menu.decoy_cooldown", (s.decoy() + 1199) / 1200)
						: Component.translatable("jtechstorage.companion.menu.decoy_ready"));
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

		for (int i = 0; i < Math.min(6, lines.size()); i++) {
			GuiTheme.label(g, font, lines.get(i), bx + 4, by + 4 + i * 10, w - 8, lines.get(i).getStyle().getColor() != null ? 0xFFFFD27A : it);
		}

		bondSection(g, s, left + 8, top + 166, W - 16, mouseX, mouseY);
		Component hint = Component.translatable("jtechstorage.companion.menu.hint");
		GuiTheme.label(g, font, hint, left + 8, top + H - 13, W - 16, GuiTheme.subtle());
	}

	/** Porträt: das echte Modell, wenn er in der Nähe ist. */
	private void portrait(GuiGraphicsExtractor g, CompanionStatusPayload s, int px, int py, int pw, int ph, int mouseX, int mouseY) {
		GuiTheme.sunken(g, px, py, pw, ph);
		Companion companion = CompanionClient.nearby(minecraft);

		if (companion != null) {
			float size = Math.max(companion.getBbHeight(), companion.getBbWidth() * 1.2f);
			int scale = (int) Math.clamp((ph * 0.55f) / Math.max(0.3f, size), 10, 60);
			InventoryScreen.extractEntityInInventoryFollowsMouse(g, px + 1, py + 1, px + pw - 1, py + ph - 1, scale, 0.0625f, mouseX, mouseY, companion);
		} else {
			String symbol = CompanionClient.speciesSymbol(s.species());
			g.text(font, symbol, px + (pw - font.width(symbol)) / 2, py + ph / 2 - 4, CompanionClient.speciesColor(s.species()), false);
			Component far = Component.translatable(s.flag(5) ? "jtechstorage.companion.hud.other_dimension" : "jtechstorage.companion.menu.far");
			int fw = font.width(far);
			g.text(font, GuiTheme.fit(font, far.getString(), pw - 6), px + Math.max(3, (pw - fw) / 2), py + ph - 14, GuiTheme.onColor(GuiTheme.slot()), false);
		}
	}

	/** Bindung (v0.26.0): Stufe, Fortschritt, Lieblingsessen des Tages. */
	private void bondSection(GuiGraphicsExtractor g, CompanionStatusPayload s, int x, int y, int w, int mouseX, int mouseY) {
		int stage = CompanionBond.stage(s.bond());
		Component name = Component.literal("\u2764 ").withStyle(ChatFormatting.LIGHT_PURPLE)
				.append(Component.translatable("jtechstorage.bond.stage." + CompanionBond.STAGE_KEYS[stage]).withStyle(ChatFormatting.WHITE));
		g.text(font, name, x, y, GuiTheme.text(), false);
		String points = stage >= 4 ? String.valueOf(s.bond()) : s.bond() + "/" + CompanionBond.STAGES[stage + 1];
		g.text(font, points, x + font.width(name) + 6, y, GuiTheme.subtle(), false);
		net.minecraft.world.item.ItemStack favorite = favorite(s);
		Component fav = Component.translatable("jtechstorage.bond.favorite_short");
		int fx = x + w - 18 - font.width(fav);

		if (!favorite.isEmpty()) {
			g.text(font, fav, fx, y, GuiTheme.subtle(), false);
			g.item(favorite, x + w - 16, y - 5);
		}

		CompanionHud.bar(g, x, y + 11, w, 6, CompanionBond.progress(s.bond()), 0xFFE878B0);
		boolean over = mouseX >= x && mouseX < x + w && mouseY >= y - 2 && mouseY < y + 18;

		if (over) {
			List<Component> tip = new ArrayList<>();
			tip.add(Component.translatable("jtechstorage.bond.title").withStyle(ChatFormatting.LIGHT_PURPLE));
			tip.add(Component.translatable("jtechstorage.bond.how.feed", favorite.isEmpty() ? Component.literal("?") : favorite.getHoverName()));
			tip.add(Component.translatable("jtechstorage.bond.how.pet"));
			tip.add(Component.translatable("jtechstorage.bond.how.play"));

			for (int st = 1; st < CompanionBond.STAGES.length; st++) {
				Component reward = st < 4 ? Component.translatable("jtechstorage.gesture." + CompanionBond.gestureKey(s.species(), st))
						: Component.translatable("jtechstorage.bond.soul_reward");
				tip.add(Component.literal((stage >= st ? "\u2714 " : "\u2716 ")).append(Component.translatable("jtechstorage.bond.stage." + CompanionBond.STAGE_KEYS[st]))
						.append(Component.literal(" (" + CompanionBond.STAGES[st] + "): ")).append(reward)
						.withStyle(stage >= st ? ChatFormatting.GREEN : ChatFormatting.GRAY));
			}

			g.setComponentTooltipForNextFrame(font, tip, mouseX, mouseY);
		}
	}

	private static net.minecraft.world.item.ItemStack favorite(CompanionStatusPayload s) {
		net.minecraft.resources.Identifier id = net.minecraft.resources.Identifier.tryParse(s.favorite());
		return id == null ? net.minecraft.world.item.ItemStack.EMPTY
				: new net.minecraft.world.item.ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(id));
	}

	private static String fmt(float value) {
		return value == Math.floor(value) ? String.valueOf((int) value) : String.format(java.util.Locale.ROOT, "%.1f", value);
	}

	// ------------------------------------------------------------------
	// Aussehen (v0.25.4)
	// ------------------------------------------------------------------

	private void lookButtons(List<Btn> list, CompanionStatusPayload s) {
		int x = left + LOOK_X;

		for (int f = 0; f < CompanionLooks.FIELDS; f++) {
			int field = f;
			int y = top + ROW_Y + f * ROW_H;
			boolean usable = f != CompanionLooks.PART2 || s.stage() >= 3;
			Component tip = lookTooltip(s, f);
			list.add(new Btn(x + 58, y, 12, 14, Component.literal("<"), usable, () -> step(s, field, -1), tip));
			list.add(new Btn(x + HUE_W - 12, y, 12, 14, Component.literal(">"), usable, () -> step(s, field, 1), tip));
		}

		boolean glow = CompanionLooks.glowRequirement(s.level()) == null;
		Component glowTip = glow ? Component.translatable("jtechstorage.look.glow.tip")
				: Component.translatable("jtechstorage.look.locked", CompanionLooks.glowRequirement(s.level()));
		int y = top + HUE_Y + 14;
		list.add(new Btn(x, y, 46, 14, Component.translatable("jtechstorage.look.glow.default"), s.glow() >= 0,
				() -> CompanionClient.send(CompanionActionPayload.GLOW, -1), glowTip));
		list.add(new Btn(x + 49, y, 46, 14, Component.translatable("jtechstorage.look.glow.white"), glow && s.glow() != 0xF4F4FF,
				() -> CompanionClient.send(CompanionActionPayload.GLOW, 0xF4F4FF), glowTip));
		boolean soul = CompanionBond.stage(s.bond()) >= 4;
		list.add(new Btn(x + 98, y, 46, 14, Component.translatable("jtechstorage.look.glow.pink"), glow && soul && s.glow() != CompanionBond.SOUL_PINK,
				() -> CompanionClient.send(CompanionActionPayload.GLOW, CompanionBond.SOUL_PINK), soul ? glowTip
						: Component.translatable("jtechstorage.look.locked", Component.translatable("jtechstorage.look.req.bond",
								Component.translatable("jtechstorage.bond.stage.soulmate")))));

		// Gesten (v0.26.0) und Aura
		int stage = CompanionBond.stage(s.bond());
		int gw = (W - 16 - 8) / 3;

		for (int gIndex = 1; gIndex <= 3; gIndex++) {
			int gesture = gIndex;
			String key = "jtechstorage.gesture." + CompanionBond.gestureKey(s.species(), gesture);
			Component tip = stage >= gesture ? Component.translatable("jtechstorage.bond.gesture.tip")
					: Component.translatable("jtechstorage.look.locked", Component.translatable("jtechstorage.look.req.bond",
							Component.translatable("jtechstorage.bond.stage." + CompanionBond.STAGE_KEYS[gesture])));
			list.add(new Btn(left + 8 + (gIndex - 1) * (gw + 4), top + GESTURE_Y, gw, 14, Component.translatable(key), stage >= gesture && !s.flag(1),
					() -> CompanionClient.send(CompanionActionPayload.GESTURE, gesture), tip));
		}

		boolean auraOn = CompanionLooks.get(s.look(), CompanionLooks.AURA) == 0;
		list.add(new Btn(left + W - 8 - 100, top + GESTURE_Y + 20, 100, 14,
				Component.translatable(auraOn ? "jtechstorage.look.aura.on" : "jtechstorage.look.aura.off"), soul,
				() -> CompanionClient.send(CompanionActionPayload.LOOK, CompanionLooks.AURA << 8 | (auraOn ? 1 : 0)),
				soul ? Component.translatable("jtechstorage.look.aura.tip") : Component.translatable("jtechstorage.look.locked",
						Component.translatable("jtechstorage.look.req.bond", Component.translatable("jtechstorage.bond.stage.soulmate")))));
	}

	/** Zum nächsten freigeschalteten Wert eines Felds blättern. */
	private void step(CompanionStatusPayload s, int field, int dir) {
		int n = CompanionLooks.OPTIONS[field];
		int value = CompanionLooks.get(s.look(), field);

		for (int i = 1; i < n; i++) {
			value = Math.floorMod(value + dir, n);

			if (CompanionLooks.requirement(field, value, s.level(), s.stage(), s.kills(), s.bond()) == null) {
				CompanionClient.send(CompanionActionPayload.LOOK, field << 8 | value);
				return;
			}
		}
	}

	private Component valueLabel(CompanionStatusPayload s, int field, int value) {
		if (field >= CompanionLooks.COLLAR && value == CompanionLooks.AUTO) {
			int best = CompanionLooks.bestMaterial(s.level(), s.stage());
			return Component.translatable("jtechstorage.look.material.auto_n", CompanionLooks.valueName(s.species(), field, best));
		}

		return CompanionLooks.valueName(s.species(), field, value);
	}

	private Component lookTooltip(CompanionStatusPayload s, int field) {
		net.minecraft.network.chat.MutableComponent tip = CompanionLooks.fieldName(s.species(), field).copy().withStyle(ChatFormatting.LIGHT_PURPLE);
		int current = CompanionLooks.get(s.look(), field);

		for (int v = 0; v < CompanionLooks.OPTIONS[field]; v++) {
			Component missing = CompanionLooks.requirement(field, v, s.level(), s.stage(), s.kills(), s.bond());
			net.minecraft.network.chat.MutableComponent line = Component.literal("\n" + (v == current ? "\u25B6 " : missing == null ? "\u2714 " : "\u2716 "))
					.append(valueLabel(s, field, v));

			if (missing != null) {
				line.append(Component.literal(" \u2013 ")).append(missing);
			}

			tip.append(line.withStyle(v == current ? ChatFormatting.WHITE : missing == null ? ChatFormatting.GREEN : ChatFormatting.GRAY));
		}

		if (field == CompanionLooks.PART2 && s.stage() < 3) {
			tip.append(Component.literal("\n")).append(Component.translatable("jtechstorage.look.req.stage", 3).withStyle(ChatFormatting.RED));
		}

		if (field >= CompanionLooks.COLLAR) {
			int tier = ModConfig.get().companions.equipment ? CompanionLooks.bestMaterial(s.level(), s.stage()) : 0;
			String key = switch (field) {
				case CompanionLooks.COLLAR -> "jtechstorage.look.bonus.collar";
				case CompanionLooks.ARMOR -> "jtechstorage.look.bonus.armor";
				default -> "jtechstorage.look.bonus.amulet." + s.species();
			};
			int amount = switch (field) {
				case CompanionLooks.COLLAR -> 10 * tier;
				case CompanionLooks.ARMOR -> 2 * tier;
				default -> s.species().equals("mantis") ? 5 * tier : s.species().equals("shroom") ? 10 * tier : tier;
			};
			tip.append(Component.literal("\n\n")).append(Component.translatable(key, amount).withStyle(ChatFormatting.GOLD));
			if (tier > 0) {
				tip.append(Component.literal("\n")).append(Component.translatable("jtechstorage.look.bonus.source",
						CompanionLooks.valueName(s.species(), field, tier)).withStyle(ChatFormatting.GRAY));
			}
		}

		return tip;
	}

	private void look(GuiGraphicsExtractor g, CompanionStatusPayload s, int mouseX, int mouseY) {
		portrait(g, s, left + 8, top + 40, 90, 160, mouseX, mouseY);
		int x = left + LOOK_X;

		for (int f = 0; f < CompanionLooks.FIELDS; f++) {
			int y = top + ROW_Y + f * ROW_H;
			boolean usable = f != CompanionLooks.PART2 || s.stage() >= 3;

			if (f == CompanionLooks.COLLAR) {
				g.fill(x, y - 4, x + HUE_W, y - 3, GuiTheme.mix(GuiTheme.face(), 0xFF000000, 0.25f));
			}

			GuiTheme.label(g, font, CompanionLooks.fieldName(s.species(), f), x, y + 3, 56, usable ? GuiTheme.text() : GuiTheme.subtle());
			int vx = x + 72;
			int vw = HUE_W - 12 - 2 - 72;
			GuiTheme.sunken(g, vx, y, vw, 14);
			Component value = usable ? valueLabel(s, f, CompanionLooks.get(s.look(), f)) : Component.literal("\u2013");
			String text = GuiTheme.fit(font, value.getString(), vw - 4);
			g.text(font, text, vx + (vw - font.width(text)) / 2, y + 3, GuiTheme.onColor(GuiTheme.slot()), false);
		}

		// Leuchtfarbe: Farbbalken (Klick wählt den Farbton), daneben die aktuelle Farbe
		int y = top + HUE_Y;
		Component locked = CompanionLooks.glowRequirement(s.level());
		g.text(font, Component.translatable("jtechstorage.look.glow"), x, y - 11, locked == null ? GuiTheme.text() : GuiTheme.subtle(), false);
		int swatch = s.glow() < 0 ? 0xFFC060FF : 0xFF000000 | s.glow();
		g.fill(x + HUE_W - 22, y - 12, x + HUE_W, y - 3, 0xFF000000);
		g.fill(x + HUE_W - 21, y - 11, x + HUE_W - 1, y - 4, swatch);
		GuiTheme.sunken(g, x, y, HUE_W, 12);

		for (int i = 0; i < HUE_W - 2; i++) {
			int rgb = hue(i / (float) (HUE_W - 3));
			g.fill(x + 1 + i, y + 1, x + 2 + i, y + 11, locked == null ? 0xFF000000 | rgb : GuiTheme.mix(0xFF000000 | rgb, 0xFF505058, 0.75f));
		}

		boolean overHue = mouseX >= x && mouseX < x + HUE_W && mouseY >= y && mouseY < y + 12;

		if (overHue) {
			g.setTooltipForNextFrame(font, font.split(locked == null ? Component.translatable("jtechstorage.look.glow.tip")
					: Component.translatable("jtechstorage.look.locked", locked), 200), mouseX, mouseY);
		}

		g.text(font, Component.translatable("jtechstorage.bond.gestures"), left + 8, top + GESTURE_Y - 10, GuiTheme.text(), false);
		Component kills = Component.translatable("jtechstorage.look.kills", s.kills());
		g.text(font, kills, left + 8, top + GESTURE_Y + 23, GuiTheme.subtle(), false);
	}

	/** Voll gesättigter Farbton 0..1 → RGB. */
	private static int hue(float h) {
		return net.minecraft.util.Mth.hsvToRgb(h, 0.8f, 1.0f) & 0xFFFFFF;
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

		if (tab == 2 && s.present() && s.loaded() && CompanionLooks.glowRequirement(s.level()) == null) {
			int x = left + LOOK_X;
			int y = top + HUE_Y;

			if (event.x() >= x + 1 && event.x() < x + HUE_W - 1 && event.y() >= y && event.y() < y + 12) {
				click();
				CompanionClient.send(CompanionActionPayload.GLOW, hue((float) (event.x() - x - 1) / (HUE_W - 3)));
				return true;
			}
		}

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
