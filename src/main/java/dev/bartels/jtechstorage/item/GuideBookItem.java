package dev.bartels.jtechstorage.item;

import java.util.function.Consumer;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Die Bücher der Mod – eines pro Thema. Jedes öffnet (nur auf dem Client) das passende Buch-Fenster.
 * Shift + Rechtsklick mit dem Speicher-Handbuch startet Jbartels' Rundgang neu.
 */
public class GuideBookItem extends Item {
	public enum Kind {
		/** Speicher-Handbuch: Grundlagen und Bedienung. */
		HANDBOOK,
		/** Rezeptbuch: alle Rezepte mit Kapiteln. */
		RECIPES,
		/** Bestiarium: alle Helfer und Bosse mit 3D-Modell. */
		BESTIARY,
		/** Gegenstands-Lexikon: jedes Item mit Beschreibung und Bedienung. */
		LEXICON,
		/** Kristall-Chronik: Bosse, End, Amethyst-Obsidian, Verzauberungen, Entwicklungen. */
		CHRONICLE,
		/** Chels Witzbuch: gesammelte Witze. */
		JOKES
	}

	/** Wird vom Client-Initializer gesetzt – so bleibt diese Klasse frei von Client-Code. */
	public static Consumer<ItemStack> openScreen = stack -> { };

	private final Kind kind;

	public GuideBookItem(Kind kind, Properties properties) {
		super(properties);
		this.kind = kind;
	}

	public Kind kind() {
		return kind;
	}

	@Override
	@SuppressWarnings("deprecation")   // in 26.x als veraltet markiert, funktioniert aber weiterhin
	public void appendHoverText(ItemStack stack, TooltipContext context, net.minecraft.world.item.component.TooltipDisplay display,
			Consumer<net.minecraft.network.chat.Component> tooltip, net.minecraft.world.item.TooltipFlag flag) {
		if (kind == Kind.JOKES) {
			int heard = stack.getOrDefault(dev.bartels.jtechstorage.ModContent.JOKES_HEARD, java.util.List.of()).size();
			tooltip.accept(net.minecraft.network.chat.Component.translatable("jtechstorage.joke_book.count", heard,
					dev.bartels.jtechstorage.entity.Chel.JOKES).withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE));
		}
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (kind == Kind.HANDBOOK && player.isSecondaryUseActive()) {
			// Shift + Rechtsklick: Jbartels' Rundgang noch einmal starten
			if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
				dev.bartels.jtechstorage.entity.JbartelsHologram.start(serverPlayer);
			}

			return InteractionResult.SUCCESS;
		}

		if (level.isClientSide()) {
			openScreen.accept(player.getItemInHand(hand));
		}

		return InteractionResult.SUCCESS;
	}
}
