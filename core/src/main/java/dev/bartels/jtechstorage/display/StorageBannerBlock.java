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
 * Storage banner (v0.34): hangs on the wall, the cloth extends two blocks down and flutters. Items in hand
 * add them to the ticker (or remove them), sneaking + empty hand clears the list.
 */
public class StorageBannerBlock extends NetworkPanelBlock {
	public StorageBannerBlock(Properties properties) {
		super(properties);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new StorageBannerBlockEntity(pos, state);
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
			InteractionHand hand, BlockHitResult hit) {
		if (stack.is(ModContent.STORAGE_KEY)) {
			return super.useItemOn(stack, state, level, pos, player, hand, hit);
		}

		if (stack.isEmpty()) {
			return InteractionResult.TRY_WITH_EMPTY_HAND;
		}

		if (!level.isClientSide() && level.getBlockEntity(pos) instanceof StorageBannerBlockEntity banner) {
			boolean full = banner.getItems().size() >= StorageBannerBlockEntity.MAX;
			boolean added = banner.toggle(stack);
			level.playSound(null, pos, SoundEvents.WOOL_PLACE, SoundSource.BLOCKS, 0.8f, added ? 1.3f : 0.9f);
			player.sendOverlayMessage(Component.translatable(added ? "jtechstorage.banner.added" : full ? "jtechstorage.banner.full" : "jtechstorage.banner.removed",
					stack.getHoverName()));
		}

		return InteractionResult.SUCCESS;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (level.isClientSide() || !(level.getBlockEntity(pos) instanceof StorageBannerBlockEntity banner)) {
			return InteractionResult.SUCCESS;
		}

		if (player.isSecondaryUseActive()) {
			banner.clear();
			player.sendOverlayMessage(Component.translatable("jtechstorage.banner.cleared"));
		} else if (player instanceof ServerPlayer server) {
			if (banner.getLink() == null) {
				player.sendOverlayMessage(Component.translatable("jtechstorage.message.panel_unlinked"));
			}

			PanelActions.open(server, banner);
		}

		return InteractionResult.SUCCESS;
	}
}
