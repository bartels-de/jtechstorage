package dev.bartels.jtechstorage.block;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;

import dev.bartels.jtechstorage.ModContent;

/**
 * Additional terminal with a screen at the front. Right-click with a bound storage key links it to the
 * network (requires permission {@link #linkPermission()}), right-click without a key opens the interface.
 */
public abstract class LinkedPanelBlock extends HorizontalDirectionalBlock implements EntityBlock {
	protected LinkedPanelBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
	}

	/** Who may link this terminal to the network? */
	protected abstract StorageTerminalBlockEntity.Permission linkPermission();

	/** Opens the interface (server only). */
	protected abstract void open(ServerPlayer player, LinkedPanelBlockEntity panel, StorageTerminalBlockEntity terminal);

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
			InteractionHand hand, BlockHitResult hit) {
		GlobalPos bound = stack.get(ModContent.LINKED_TERMINAL);

		if (!stack.is(ModContent.STORAGE_KEY) || bound == null || player.isSecondaryUseActive()) {
			return InteractionResult.TRY_WITH_EMPTY_HAND;
		}

		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}

		if (!(level.getBlockEntity(pos) instanceof LinkedPanelBlockEntity panel)) {
			return InteractionResult.PASS;
		}

		if (!bound.dimension().equals(level.dimension()) || !level.isLoaded(bound.pos())
				|| !(level.getBlockEntity(bound.pos()) instanceof StorageTerminalBlockEntity terminal)) {
			player.sendOverlayMessage(Component.translatable("jtechstorage.message.terminal_missing"));
			return InteractionResult.FAIL;
		}

		if (!terminal.can(player, linkPermission())) {
			player.sendOverlayMessage(Component.translatable("jtechstorage.message.no_permission",
					linkPermission().minimum.displayName()));
			return InteractionResult.FAIL;
		}

		panel.setLink(bound.pos());
		((ServerLevel) level).sendParticles(ParticleTypes.WITCH, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
				20, 0.3, 0.3, 0.3, 0.02);
		level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.0f, 1.3f);
		player.sendOverlayMessage(Component.translatable("jtechstorage.message.panel_linked", getName(), terminal.teamLabel()));
		return InteractionResult.SUCCESS;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (level.isClientSide() || !(player instanceof ServerPlayer serverPlayer)
				|| !(level.getBlockEntity(pos) instanceof LinkedPanelBlockEntity panel)) {
			return InteractionResult.SUCCESS;
		}

		if (panel.getLink() == null) {
			player.sendOverlayMessage(Component.translatable("jtechstorage.message.panel_unlinked"));
			return InteractionResult.SUCCESS;
		}

		StorageTerminalBlockEntity terminal = panel.terminal();

		if (terminal == null) {
			player.sendOverlayMessage(Component.translatable("jtechstorage.message.terminal_missing"));
			return InteractionResult.SUCCESS;
		}

		open(serverPlayer, panel, terminal);
		return InteractionResult.SUCCESS;
	}

	/** Helper for menus: panel still there and player close enough? */
	public static boolean inReach(@Nullable LinkedPanelBlockEntity panel, Player player) {
		return panel != null && !panel.isRemoved()
				&& panel.getLevel() == player.level()
				&& panel.getBlockPos().distToCenterSqr(player.position()) <= 8.0 * 8.0;
	}
}
