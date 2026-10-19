package dev.bartels.jtechstorage.client.companion;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import dev.bartels.jtechstorage.companion.Companion;
import dev.bartels.jtechstorage.net.CompanionActionPayload;
import dev.bartels.jtechstorage.net.CompanionStatusPayload;

/** Client-Seite der Begleiter (v0.26): letzter Zustand vom Server, Menü öffnen, Aktionen schicken. */
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
		net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> status = CompanionStatusPayload.NONE);
		Companion.openMenu = companion -> Minecraft.getInstance().setScreenAndShow(new CompanionScreen(0));
		net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry.attachElementAfter(
				net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements.MISC_OVERLAYS, dev.bartels.jtechstorage.JTechStorage.id("companion_hud"),
				CompanionHud::extract);
	}

	public static CompanionStatusPayload status() {
		return status;
	}

	/** Ist der Zustand frisch (nicht älter als 3 Sekunden)? */
	public static boolean fresh() {
		return System.currentTimeMillis() - received < 3000;
	}

	public static void send(int action, int arg) {
		ClientPlayNetworking.send(new CompanionActionPayload(action, arg));
	}

	/** Taste K: Menü öffnen – oder sagen, dass es keinen Begleiter gibt. */
	public static void openFromKey(Minecraft client) {
		if (client.player == null) {
			return;
		}

		client.setScreenAndShow(new CompanionScreen(0));
	}

	/** Der eigene Begleiter, falls er in der Nähe geladen ist (für das Porträt). */
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

	/** Anzeigename: eigener Name oder Name der Art. */
	public static Component name(CompanionStatusPayload s) {
		return s.name().isEmpty() ? Component.translatable("entity.jtechstorage." + s.species()) : Component.literal(s.name());
	}

	/** Fortschritt im aktuellen Level (0..1). */
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

	public static String stanceSymbol(int stance) {
		return stance == Companion.STANCE_AGGRESSIVE ? "⚔" : stance == Companion.STANCE_DEFENSIVE ? "◆" : "☮";
	}

	public static int stanceColor(int stance) {
		return stance == Companion.STANCE_AGGRESSIVE ? 0xFFFF8080 : stance == Companion.STANCE_DEFENSIVE ? 0xFFFFD27A : 0xFF9CE0A0;
	}
}
