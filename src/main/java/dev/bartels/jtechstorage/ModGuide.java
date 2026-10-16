package dev.bartels.jtechstorage;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Prediction;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

import dev.bartels.jtechstorage.entity.JbartelsHologram;

/** Begrüßung beim ersten Betreten einer Welt – inklusive Speicher-Handbuch. */
public final class ModGuide {
	public static final String AUTHOR = "Jbartels";
	private static final String WELCOMED_TAG = "jtechstorage.welcomed";
	/** Spieler, für die der Rundgang gleich startet (UUID → verbleibende Ticks). */
	private static final Map<UUID, Integer> PENDING_TOURS = new HashMap<>();

	private ModGuide() {
	}

	public static void init() {
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			ServerPlayer player = handler.getPlayer();

			// Entity-Tags werden mit dem Spieler gespeichert – die Begrüßung kommt nur einmal
			if (!player.addTag(WELCOMED_TAG)) {
				return;
			}

			player.getInventory().placeItemBackInInventory(new ItemStack(ModContent.GUIDE_BOOK), Prediction.SERVER_ONLY);
			player.sendSystemMessage(Component.translatable("jtechstorage.welcome.line1").withStyle(ChatFormatting.LIGHT_PURPLE));
			player.sendSystemMessage(Component.translatable("jtechstorage.welcome.line2", AUTHOR).withStyle(ChatFormatting.GRAY));
			PENDING_TOURS.put(player.getUUID(), 80);
		});

		// Der Rundgang startet ein paar Sekunden nach dem Betreten (wenn die Welt geladen ist)
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (PENDING_TOURS.isEmpty()) {
				return;
			}

			PENDING_TOURS.replaceAll((uuid, ticks) -> ticks - 1);
			PENDING_TOURS.entrySet().removeIf(entry -> {
				if (entry.getValue() > 0) {
					return false;
				}

				ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());

				if (player != null && player.isAlive()) {
					JbartelsHologram.start(player);
				}

				return true;
			});
		});
	}
}
