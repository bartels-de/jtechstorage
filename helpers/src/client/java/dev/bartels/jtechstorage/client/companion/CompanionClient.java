package dev.bartels.jtechstorage.client.companion;

import dev.bartels.jtechstorage.JTechStorage;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import dev.bartels.jtechstorage.companion.Companion;
import dev.bartels.jtechstorage.net.CompanionActionPayload;
import dev.bartels.jtechstorage.net.CompanionStatusPayload;

/** Client side of the companions (v0.26): last state from the server, open the menu, send actions. */
public final class CompanionClient {
	private static CompanionStatusPayload status = CompanionStatusPayload.NONE;
	private static long received;

	private CompanionClient() {
	}

	public static void init() {
		ClientPlayNetworking.registerGlobalReceiver(CompanionStatusPayload.TYPE, (payload, context) -> {
			status = payload;
			received = System.currentTimeMillis();
		});
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> status = CompanionStatusPayload.NONE);
		Companion.openMenu = companion -> Minecraft.getInstance().setScreenAndShow(new CompanionScreen(0));
		HudElementRegistry.attachElementAfter(
				VanillaHudElements.MISC_OVERLAYS, JTechStorage.id("companion_hud"),
				CompanionHud::extract);
	}

	public static CompanionStatusPayload status() {
		return status;
	}

	/** Is the state fresh (no older than 3 seconds)? */
	public static boolean fresh() {
		return System.currentTimeMillis() - received < 3000;
	}

	public static void send(int action, int arg) {
		ClientPlayNetworking.send(new CompanionActionPayload(action, arg));
	}

	/** Key K: open the menu, or say that there is no companion. */
	public static void openFromKey(Minecraft client) {
		if (client.player == null) {
			return;
		}

		client.setScreenAndShow(new CompanionScreen(0));
	}

	/** The player's own companion, if loaded nearby (for the portrait). */
	public static Companion nearby(Minecraft client) {
		if (client.level == null || client.player == null || !status.loaded()) {
			return null;
		}

		for (Entity entity : client.level.entitiesForRendering()) {
			if (entity instanceof Companion companion && companion.isOwnedBy(client.player) && companion.species().equals(status.species())) {
				return companion;
			}
		}

		return null;
	}

	/** Display name: custom name or the species name. */
	public static Component name(CompanionStatusPayload s) {
		return s.name().isEmpty() ? Component.translatable("entity.jtechstorage." + s.species()) : Component.literal(s.name());
	}

	/** Progress within the current level (0..1). */
	public static float levelProgress(CompanionStatusPayload s) {
		int from = Companion.xpForLevel(s.level());
		int to = Companion.xpForLevel(s.level() + 1);
		return to > from ? Math.clamp((s.xp() - from) / (float) (to - from), 0.0f, 1.0f) : 0.0f;
	}

	public static int speciesColor(String species) {
		return switch (species) {
			case "mantis" -> 0xFF7A3EC0;
			case "shroom" -> 0xFFC06AD8;
			case "raven" -> 0xFF3A3A78;
			default -> 0xFF707080;
		};
	}

	public static String speciesSymbol(String species) {
		return switch (species) {
			case "mantis" -> "⚔";
			case "shroom" -> "✿";
			case "raven" -> "☾";
			default -> "?";
		};
	}

	/** State as a language key (v0.25.1 with explore, guard and shoulder). */
	public static String modeKey(CompanionStatusPayload s) {
		return s.flag(1) ? "jtechstorage.companion.hud.exhausted" : s.flag(7) ? "jtechstorage.companion.hud.exploring"
				: s.flag(3) ? "jtechstorage.companion.hud.away" : s.flag(8) ? "jtechstorage.companion.hud.guard"
				: s.flag(0) ? "jtechstorage.companion.hud.stay" : s.flag(9) ? "jtechstorage.companion.hud.shoulder" : "jtechstorage.companion.hud.follow";
	}

	public static String modeSymbol(CompanionStatusPayload s) {
		return s.flag(1) ? "✖" : s.flag(7) ? "⌖" : s.flag(3) ? "✉" : s.flag(8) ? "⚔" : s.flag(0) ? "■" : s.flag(9) ? "♦" : "»";
	}

	public static int modeColor(CompanionStatusPayload s) {
		return s.flag(1) ? 0xFFFF8080 : s.flag(7) || s.flag(3) ? 0xFFC8B0FF : s.flag(8) ? 0xFFFF9C6A : s.flag(0) ? 0xFFFFD27A : 0xFFB8F0B8;
	}

	public static String stanceSymbol(int stance) {
		return stance == Companion.STANCE_AGGRESSIVE ? "⚔" : stance == Companion.STANCE_DEFENSIVE ? "◆" : "☮";
	}

	public static int stanceColor(int stance) {
		return stance == Companion.STANCE_AGGRESSIVE ? 0xFFFF8080 : stance == Companion.STANCE_DEFENSIVE ? 0xFFFFD27A : 0xFF9CE0A0;
	}
}
