package dev.bartels.jtechstorage.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import dev.bartels.jtechstorage.ModAdvancements;
import dev.bartels.jtechstorage.ModContent;
import dev.bartels.jtechstorage.block.StorageTerminalBlockEntity;

/**
 * Drahtloses Terminal: Shift + Rechtsklick auf ein Terminal koppelt es,
 * danach öffnet ein Rechtsklick das Netzwerk aus der Ferne (Reichweite = Terminal-Reichweite × 4).
 * Mit Ender-Upgrade im Terminal: unbegrenzte Reichweite, auch aus anderen Dimensionen.
 */
public class WirelessTerminalItem extends LinkedTerminalItem {
	public WirelessTerminalItem(Properties properties) {
		super(properties);
	}

	@Override
	@SuppressWarnings("deprecation")
	public void appendHoverText(net.minecraft.world.item.ItemStack stack, TooltipContext context,
			net.minecraft.world.item.component.TooltipDisplay display, java.util.function.Consumer<Component> tooltip,
			net.minecraft.world.item.TooltipFlag flag) {
		tooltip.accept(stack.has(ModContent.WIRELESS_CORE_INSTALLED)
				? Component.translatable("jtechstorage.wireless.core_installed").withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE)
				: Component.translatable("jtechstorage.wireless.core_missing").withStyle(net.minecraft.ChatFormatting.RED));
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (level.isClientSide() || !(player instanceof ServerPlayer serverPlayer)) {
			return InteractionResult.SUCCESS;
		}

		if (!player.getItemInHand(hand).has(ModContent.WIRELESS_CORE_INSTALLED)) {
			player.sendOverlayMessage(Component.translatable("jtechstorage.message.no_wireless_core"));
			return InteractionResult.FAIL;
		}

		GlobalPos bound = player.getItemInHand(hand).get(ModContent.LINKED_TERMINAL);

		if (bound == null) {
			player.sendOverlayMessage(Component.translatable("jtechstorage.message.not_bound"));
			return InteractionResult.FAIL;
		}

		ServerLevel terminalLevel = serverPlayer.level().getServer().getLevel(bound.dimension());
		BlockPos pos = bound.pos();

		if (terminalLevel == null) {
			player.sendOverlayMessage(Component.translatable("jtechstorage.message.terminal_missing"));
			return InteractionResult.FAIL;
		}

		boolean sameDimension = terminalLevel == level;
		boolean wasLoaded = terminalLevel.isLoaded(pos);

		if (!wasLoaded) {
			// Chunk kurz laden, um nachzusehen, ob dort ein Terminal (mit Ender-Upgrade) steht
			terminalLevel.getChunk(pos.getX() >> 4, pos.getZ() >> 4);
		}

		if (!(terminalLevel.getBlockEntity(pos) instanceof StorageTerminalBlockEntity terminal)) {
			player.sendOverlayMessage(Component.translatable("jtechstorage.message.terminal_missing"));
			return InteractionResult.FAIL;
		}

		if (!terminal.canAccess(player)) {
			player.sendOverlayMessage(Component.translatable("jtechstorage.message.not_owner", terminal.getOwnerName()));
			return InteractionResult.FAIL;
		}

		if (terminal.hasEnderUpgrade()) {
			terminal.keepNetworkLoaded();

			if (!sameDimension) {
				ModAdvancements.grant(serverPlayer, "beyond_dimensions");
			}
		} else if (!sameDimension) {
			player.sendOverlayMessage(Component.translatable("jtechstorage.message.other_dimension"));
			return InteractionResult.FAIL;
		} else if (!wasLoaded) {
			player.sendOverlayMessage(Component.translatable("jtechstorage.message.terminal_unloaded"));
			return InteractionResult.FAIL;
		} else {
			int range = terminal.getWirelessRange();

			if (pos.distToCenterSqr(player.position()) > (double) range * range) {
				player.sendOverlayMessage(Component.translatable("jtechstorage.message.wireless_out_of_range", range));
				return InteractionResult.FAIL;
			}
		}

		player.openMenu(terminal.wirelessProvider());
		return InteractionResult.SUCCESS;
	}
}
