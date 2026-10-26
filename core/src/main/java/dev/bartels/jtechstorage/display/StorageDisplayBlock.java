package dev.bartels.jtechstorage.display;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import dev.bartels.jtechstorage.ModContent;

/**
 * Storage screen (v0.34). Linked with the storage key, an item determines what is shown, sneaking +
 * empty hand switches the display. Screens placed side by side become one large one (up to 4×3).
 */
public class StorageDisplayBlock extends NetworkPanelBlock {
	public StorageDisplayBlock(Properties properties) {
		super(properties);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new StorageDisplayBlockEntity(pos, state);
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
			InteractionHand hand, BlockHitResult hit) {
		// A combined screen is always configured via the top-left block
		BlockPos origin = DisplayShape.of(level, pos).origin();

		if (stack.is(ModContent.STORAGE_KEY)) {
			return super.useItemOn(stack, level.getBlockState(origin), level, origin, player, hand, hit);
		}

		if (stack.isEmpty()) {
			return InteractionResult.TRY_WITH_EMPTY_HAND;
		}

		if (!level.isClientSide() && level.getBlockEntity(origin) instanceof StorageDisplayBlockEntity display) {
			display.setTracked(stack);
			level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.8f, 1.5f);
			player.sendOverlayMessage(Component.translatable("jtechstorage.display.tracked", stack.getHoverName()));
		}

		return InteractionResult.SUCCESS;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (level.isClientSide() || !(level.getBlockEntity(DisplayShape.of(level, pos).origin()) instanceof StorageDisplayBlockEntity display)) {
			return InteractionResult.SUCCESS;
		}

		if (player.isSecondaryUseActive()) {
			display.cycleMode();
			level.playSound(null, pos, SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.BLOCKS, 0.4f, 1.6f);
			player.sendOverlayMessage(Component.translatable("jtechstorage.display.mode",
					Component.translatable("jtechstorage.display.mode." + display.getMode().id())));
		} else if (player instanceof ServerPlayer server) {
			// v0.36: menu (display + appearance); without a link only a hint
			if (display.getLink() == null) {
				player.sendOverlayMessage(Component.translatable("jtechstorage.message.panel_unlinked"));
			}

			PanelActions.open(server, display);
		}

		return InteractionResult.SUCCESS;
	}
}
