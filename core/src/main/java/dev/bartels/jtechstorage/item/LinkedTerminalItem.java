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
 * Common base for the storage key and the wireless terminal:
 * Shift + right-click on a terminal links the item to that terminal.
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

	/** Shift + right-click on a block other than a terminal. */
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

	/** Like {@link #findTerminal}, but without messages (for tools that quietly deliver to storage). */
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
	 * Finds the linked terminal and checks dimension, loaded chunk and owner.
	 * On problems, sends a message to the player and returns {@code null}.
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
