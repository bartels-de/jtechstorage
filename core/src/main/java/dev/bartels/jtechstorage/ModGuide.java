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

/** Greeting on first entering a world - including the storage guide. */
public final class ModGuide {
	public static final String AUTHOR = "Jbartels";
	private static final String WELCOMED_TAG = "jtechstorage.welcomed";
	/** Players whose tour is about to start (UUID -> remaining ticks). */
	private static final Map<UUID, Integer> PENDING_TOURS = new HashMap<>();

	private ModGuide() {
	}

	public static void init() {
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			ServerPlayer player = handler.getPlayer();

			// Entity tags are saved with the player - the greeting only appears once
			if (!player.addTag(WELCOMED_TAG)) {
				return;
			}

			player.getInventory().placeItemBackInInventory(new ItemStack(ModContent.GUIDE_BOOK), Prediction.SERVER_ONLY);
			player.sendSystemMessage(Component.translatable("jtechstorage.welcome.line1").withStyle(ChatFormatting.LIGHT_PURPLE));
			player.sendSystemMessage(Component.translatable("jtechstorage.welcome.line2", AUTHOR).withStyle(ChatFormatting.GRAY));
			PENDING_TOURS.put(player.getUUID(), 80);
		});

		// The tour starts a few seconds after joining (once the world has loaded)
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
