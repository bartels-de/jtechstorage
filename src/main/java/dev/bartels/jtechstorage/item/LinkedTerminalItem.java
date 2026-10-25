package dev.bartels.jtechstorage.item;

import java.util.List;

import org.jspecify.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import dev.bartels.jtechstorage.ModAdvancements;
import dev.bartels.jtechstorage.ModContent;
import dev.bartels.jtechstorage.block.StorageTerminalBlockEntity;

/**
 * Gemeinsame Basis für Speicher-Schlüssel und drahtloses Terminal:
 * Shift + Rechtsklick auf ein Terminal koppelt das Item an dieses Terminal.
 */
public abstract class LinkedTerminalItem extends Item {
	protected LinkedTerminalItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		Level level = context.getLevel();
		Player player = context.getPlayer();
		BlockPos pos = context.getClickedPos();

		if (player == null || !player.isSecondaryUseActive()) {
			return InteractionResult.PASS;
		}

		if (level.getBlockEntity(pos) instanceof StorageTerminalBlockEntity terminal) {
			if (!level.isClientSide()) {
				if (terminal.canAccess(player)) {
					bind(context.getItemInHand(), level, pos);

					if (player instanceof ServerPlayer serverPlayer) {
						ModAdvancements.grant(serverPlayer, "key_bound");
					}

					player.sendOverlayMessage(Component.translatable("jtechstorage.message.bound"));
					level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.0f, 1.2f);
				} else {
					player.sendOverlayMessage(Component.translatable("jtechstorage.message.not_owner", terminal.getOwnerName()));
				}
			}

			return InteractionResult.SUCCESS;
		}

		return useOnOtherBlock(context);
	}

	/** Shift + Rechtsklick auf einen anderen Block als ein Terminal. */
	protected InteractionResult useOnOtherBlock(UseOnContext context) {
		return InteractionResult.PASS;
	}

	private static void bind(ItemStack stack, Level level, BlockPos pos) {
		stack.set(ModContent.LINKED_TERMINAL, GlobalPos.of(level.dimension(), pos.immutable()));
		stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
		stack.set(DataComponents.LORE, new ItemLore(List.of(
				Component.translatable("jtechstorage.tooltip.bound", pos.getX(), pos.getY(), pos.getZ())
						.withStyle(ChatFormatting.GRAY)
						.withStyle(style -> style.withItalic(false)))));
	}

	/** Wie {@link #findTerminal}, aber ohne Meldungen (für Werkzeuge, die still ins Lager liefern). */
	public static @Nullable StorageTerminalBlockEntity quietTerminal(ItemStack stack, Level level, Player player) {
		GlobalPos bound = stack.get(ModContent.LINKED_TERMINAL);

		if (bound == null || !bound.dimension().equals(level.dimension()) || !level.isLoaded(bound.pos())
				|| !(level.getBlockEntity(bound.pos()) instanceof StorageTerminalBlockEntity terminal) || !terminal.canAccess(player)) {
			return null;
		}

		double range = terminal.getWirelessRange();
		return terminal.getBlockPos().distToCenterSqr(player.position()) <= range * range ? terminal : null;
	}

	/**
	 * Sucht das gekoppelte Terminal und prüft Dimension, geladenen Chunk und Besitzer.
	 * Gibt bei Problemen eine Meldung an den Spieler aus und liefert {@code null}.
	 */
	protected static @Nullable StorageTerminalBlockEntity findTerminal(ItemStack stack, Level level, Player player) {
		GlobalPos bound = stack.get(ModContent.LINKED_TERMINAL);

		if (bound == null) {
			player.sendOverlayMessage(Component.translatable("jtechstorage.message.not_bound"));
			return null;
		}

		if (!bound.dimension().equals(level.dimension())) {
			player.sendOverlayMessage(Component.translatable("jtechstorage.message.other_dimension"));
			return null;
		}

		if (!level.isLoaded(bound.pos()) || !(level.getBlockEntity(bound.pos()) instanceof StorageTerminalBlockEntity terminal)) {
			player.sendOverlayMessage(Component.translatable("jtechstorage.message.terminal_missing"));
			return null;
		}

		if (!terminal.canAccess(player)) {
			player.sendOverlayMessage(Component.translatable("jtechstorage.message.not_owner", terminal.getOwnerName()));
			return null;
		}

		return terminal;
	}
}
