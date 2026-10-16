package dev.bartels.jtechstorage.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/** Speicher-Handbuch: öffnet (nur auf dem Client) das Buch mit Begrüßung, Anleitung und allen Rezepten. */
public class GuideBookItem extends Item {
	/** Wird vom Client-Initializer gesetzt – so bleibt diese Klasse frei von Client-Code. */
	public static Runnable openScreen = () -> { };

	public GuideBookItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (player.isSecondaryUseActive()) {
			// Shift + Rechtsklick: Jbartels' Rundgang noch einmal starten
			if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
				dev.bartels.jtechstorage.entity.JbartelsHologram.start(serverPlayer);
			}

			return InteractionResult.SUCCESS;
		}

		if (level.isClientSide()) {
			openScreen.run();
		}

		return InteractionResult.SUCCESS;
	}
}
