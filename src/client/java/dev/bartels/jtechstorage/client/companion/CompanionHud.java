package dev.bartels.jtechstorage.client.companion;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import dev.bartels.jtechstorage.client.gui.GuiSettings;
import dev.bartels.jtechstorage.companion.MantisCompanion;
import dev.bartels.jtechstorage.net.CompanionStatusPayload;

/**
 * Begleiter-HUD (v0.26) oben links: Name, Level, Leben, Erfahrung, Folgen/Bleiben, Haltung, Leckerli und je Art die
 * Abklingzeit (Pilz), Flugausdauer beim Reiten (Mantis) oder Taschenfüllung (Rabe). Im Begleiter-Menü abschaltbar.
 */
public final class CompanionHud {
	private static final int W = 124;

	private CompanionHud() {
	}

	static void bar(GuiGraphicsExtractor g, int x, int y, int w, int h, float frac, int color) {
		g.fill(x, y, x + w, y + h, 0xFF000000);
		g.fill(x + 1, y + 1, x + w - 1, y + h - 1, 0xFF2A2A30);
		int fill = (int) ((w - 2) * Math.clamp(frac, 0.0f, 1.0f));

		if (fill > 0) {
			g.fill(x + 1, y + 1, x + 1 + fill, y + h - 1, color);
			g.fill(x + 1, y + 1, x + 1 + fill, y + 2, dev.bartels.jtechstorage.client.gui.GuiTheme.mix(color, 0xFFFFFFFF, 0.35f));
		}
	}

	public static void extract(GuiGraphicsExtractor g, DeltaTracker delta) {
		Minecraft mc = Minecraft.getInstance();
		CompanionStatusPayload s = CompanionClient.status();

		if (!GuiSettings.get().companionHud || mc.player == null || !s.present() || !CompanionClient.fresh() || mc.getDebugOverlay().showDebugScreen()) {
			return;
		}

		Font font = mc.font;
		int x = 4;
		int y = 4;

		if (!s.loaded()) {
			Component text = Component.translatable("jtechstorage.companion.hud.unloaded");
			g.fill(x, y, x + font.width(text) + 8, y + 13, 0xA0101018);
			g.text(font, text, x + 4, y + 3, 0xFFB8B8C6, true);
			return;
		}

		boolean riding = mc.player.getVehicle() instanceof MantisCompanion;
		int h = 39;
		g.fill(x, y, x + W, y + h, 0xA0101018);

		// Symbol der Art
		int color = CompanionClient.speciesColor(s.species());
		g.fill(x + 2, y + 2, x + 22, y + 22, 0xFF000000);
		g.fill(x + 3, y + 3, x + 21, y + 21, color);
		g.fill(x + 3, y + 3, x + 21, y + 4, dev.bartels.jtechstorage.client.gui.GuiTheme.mix(color, 0xFFFFFFFF, 0.35f));
		String symbol = CompanionClient.speciesSymbol(s.species());
		g.text(font, symbol, x + 12 - font.width(symbol) / 2, y + 8, 0xFFFFFFFF, true);
		g.text(font, String.valueOf(s.stage()), x + 17, y + 14, 0xFFFFE0A0, true);

		String level = "Lv " + s.level();
		int nameW = W - 28 - font.width(level) - 4;
		g.text(font, dev.bartels.jtechstorage.client.gui.GuiTheme.fit(font, CompanionClient.name(s).getString(), nameW), x + 26, y + 3, 0xFFFFFFFF, true);
		g.text(font, level, x + W - 3 - font.width(level), y + 3, 0xFFE0C8FF, true);
		bar(g, x + 26, y + 13, W - 29, 5, s.maxHealth() > 0 ? s.health() / s.maxHealth() : 0, s.flag(1) ? 0xFF886060 : 0xFFD04848);
		bar(g, x + 26, y + 19, W - 29, 4, CompanionClient.levelProgress(s), 0xFF9A6AE6);

		// Zeile unten: Modus, Haltung, Leckerli, Besonderes
		int lx = x + 4;
		int ly = y + 28;
		String mode;
		int modeColor;

		if (s.flag(1)) {
			mode = "✖";
			modeColor = 0xFFFF8080;
		} else if (s.flag(3)) {
			mode = "✉";
			modeColor = 0xFFC8B0FF;
		} else if (s.flag(0)) {
			mode = "■";
			modeColor = 0xFFFFD27A;
		} else {
			mode = "»";
			modeColor = 0xFFB8F0B8;
		}

		Component modeText = Component.translatable(s.flag(1) ? "jtechstorage.companion.hud.exhausted" : s.flag(3) ? "jtechstorage.companion.hud.away"
				: s.flag(0) ? "jtechstorage.companion.hud.stay" : "jtechstorage.companion.hud.follow");
		String modeLine = mode + " " + modeText.getString();
		g.text(font, modeLine, lx, ly, modeColor, true);
		lx += font.width(modeLine) + 5;
		String stance = CompanionClient.stanceSymbol(s.stance());
		g.text(font, stance, lx, ly, CompanionClient.stanceColor(s.stance()), true);
		lx += font.width(stance) + 5;

		if (s.treatTicks() > 0) {
			String treat = "✦" + (s.treatTicks() + 1199) / 1200 + "m";
			g.text(font, treat, lx, ly, 0xFFFFD27A, true);
			lx += font.width(treat) + 5;
		}

		int barX = Math.max(lx, x + W - 34);
		int barW = x + W - 3 - barX;

		if (barW >= 12) {
			switch (s.species()) {
				case "shroom" -> {
					if (s.special() > 0 && s.specialMax() > 0) {
						bar(g, barX, ly + 2, barW, 4, 1.0f - s.special() / (float) s.specialMax(), 0xFF9CF08A);
					} else {
						g.text(font, "✚", barX + barW - font.width("✚"), ly, s.flag(2) ? 0xFF9CF08A : 0xFFD4B4FF, true);
					}
				}
				case "mantis" -> {
					if (riding) {
						bar(g, barX, ly + 2, barW, 4, s.special() / (float) Math.max(1, s.specialMax()), 0xFF8DD0F0);
					}
				}
				case "raven" -> bar(g, barX, ly + 2, barW, 4, s.special() / (float) Math.max(1, s.specialMax()), 0xFFC8A070);
				default -> {
				}
			}
		}

		// weit weg / andere Dimension
		if (s.flag(5) || s.distance() > 40) {
			Component far = s.flag(5) ? Component.translatable("jtechstorage.companion.hud.other_dimension")
					: Component.translatable("jtechstorage.companion.hud.distance", s.distance());
			g.text(font, far, x + 2, y + h + 2, 0xFFB8B8C6, true);
		}
	}
}
