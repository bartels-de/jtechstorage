package dev.bartels.jtechstorage.item;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import dev.bartels.jtechstorage.ModContent;

/**
 * Route-Feder: erster Rechtsklick auf einen Block = Haltestelle A, zweiter = Haltestelle B (der Amethyst-Phönix landet
 * oben auf dem Block). Ist die Route vollständig, beginnt ein neuer Klick wieder bei A. Shift + Rechtsklick in die Luft
 * löscht die Route. Danach den Amethyst-Phönix mit der Feder rechtsklicken.
 */
public class RouteFeatherItem extends Item {
	public RouteFeatherItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		Level level = context.getLevel();
		ItemStack stack = context.getItemInHand();
		Player player = context.getPlayer();

		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}

		GlobalPos clicked = GlobalPos.of(level.dimension(), context.getClickedPos().immutable());
		GlobalPos a = stack.get(ModContent.ROUTE_A);
		GlobalPos b = stack.get(ModContent.ROUTE_B);
		String message;

		if (a == null || b != null) {
			stack.set(ModContent.ROUTE_A, clicked);
			stack.remove(ModContent.ROUTE_B);
			message = "jtechstorage.route_feather.set_a";
		} else {
			if (!a.dimension().equals(clicked.dimension())) {
				if (player != null) {
					player.sendOverlayMessage(Component.translatable("jtechstorage.route_feather.dimension").withStyle(ChatFormatting.RED));
				}

				return InteractionResult.FAIL;
			}

			stack.set(ModContent.ROUTE_B, clicked);
			message = "jtechstorage.route_feather.set_b";
		}

		BlockPos pos = clicked.pos();
		level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0f, message.endsWith("a") ? 1.0f : 1.4f);

		if (player != null) {
			player.sendOverlayMessage(Component.translatable(message, pos.getX(), pos.getY(), pos.getZ()).withStyle(ChatFormatting.LIGHT_PURPLE));
		}

		return InteractionResult.SUCCESS;
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);

		if (player.isSecondaryUseActive() && (stack.has(ModContent.ROUTE_A) || stack.has(ModContent.ROUTE_B))) {
			if (!level.isClientSide()) {
				stack.remove(ModContent.ROUTE_A);
				stack.remove(ModContent.ROUTE_B);
				player.sendOverlayMessage(Component.translatable("jtechstorage.route_feather.cleared"));
			}

			return InteractionResult.SUCCESS;
		}

		return InteractionResult.PASS;
	}

	@Override
	public boolean isFoil(ItemStack stack) {
		return stack.has(ModContent.ROUTE_A) && stack.has(ModContent.ROUTE_B);
	}

	@Override
	@SuppressWarnings("deprecation")   // in 26.x als veraltet markiert, funktioniert aber weiterhin
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
		line(tooltip, "A", stack.get(ModContent.ROUTE_A));
		line(tooltip, "B", stack.get(ModContent.ROUTE_B));
		tooltip.accept(Component.translatable("jtechstorage.route_feather.hint").withStyle(ChatFormatting.GRAY));
	}

	private static void line(Consumer<Component> tooltip, String letter, GlobalPos pos) {
		if (pos == null) {
			tooltip.accept(Component.translatable("jtechstorage.route_feather.unset", letter).withStyle(ChatFormatting.GRAY));
		} else {
			tooltip.accept(Component.translatable("jtechstorage.route_feather.line", letter, pos.pos().getX(), pos.pos().getY(), pos.pos().getZ())
					.withStyle(ChatFormatting.LIGHT_PURPLE));
		}
	}
}
