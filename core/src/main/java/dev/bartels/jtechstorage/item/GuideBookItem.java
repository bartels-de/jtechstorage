package dev.bartels.jtechstorage.item;

import dev.bartels.jtechstorage.ModContent;
import dev.bartels.jtechstorage.entity.JbartelsHologram;
import java.util.List;
import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

/**
 * The mod's books – one per topic. Each opens (client only) the matching book screen.
 * Shift + right-click with the Storage Handbook restarts Jbartels' tour.
 */
public class GuideBookItem extends Item {
	/** Number of Chel's jokes (v0.43 moved here from Chel so the core doesn't need to know Chel). */
	public static final int JOKE_COUNT = 75;

	public enum Kind {
		/** Storage Handbook: basics and usage. */
		HANDBOOK,
		/** Recipe book: all recipes with chapters. */
		RECIPES,
		/** Bestiary: all helpers and bosses with 3D model. */
		BESTIARY,
		/** Item encyclopedia: every item with description and usage. */
		LEXICON,
		/** Crystal Chronicle: bosses, End, amethyst obsidian, enchantments, developments. */
		CHRONICLE,
		/** Chel's joke book: collected jokes. */
		JOKES
	}

	/** Set by the client initializer – keeps this class free of client code. */
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
	@SuppressWarnings("deprecation")   // marked deprecated in 26.x, but still works
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
			Consumer<Component> tooltip, TooltipFlag flag) {
		if (kind == Kind.JOKES) {
			int heard = stack.getOrDefault(ModContent.JOKES_HEARD, List.of()).size();
			tooltip.accept(Component.translatable("jtechstorage.joke_book.count", heard,
					JOKE_COUNT).withStyle(ChatFormatting.LIGHT_PURPLE));
		}
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (kind == Kind.HANDBOOK && player.isSecondaryUseActive()) {
			// Shift + right-click: restart Jbartels' tour
			if (player instanceof ServerPlayer serverPlayer) {
				JbartelsHologram.start(serverPlayer);
			}

			return InteractionResult.SUCCESS;
		}

		if (level.isClientSide()) {
			openScreen.accept(player.getItemInHand(hand));
		}

		return InteractionResult.SUCCESS;
	}
}
