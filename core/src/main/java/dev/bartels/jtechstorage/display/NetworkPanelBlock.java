package dev.bartels.jtechstorage.display;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import dev.bartels.jtechstorage.block.LinkedPanelBlock;
import dev.bartels.jtechstorage.block.LinkedPanelBlockEntity;
import dev.bartels.jtechstorage.block.StorageTerminalBlockEntity;

/** Flat wall panel linked to a storage terminal (v0.34). Faces in direction {@code FACING}. */
public abstract class NetworkPanelBlock extends LinkedPanelBlock {
	private static final VoxelShape NORTH = Block.box(0, 0, 14, 16, 16, 16);
	private static final VoxelShape SOUTH = Block.box(0, 0, 0, 16, 16, 2);
	private static final VoxelShape WEST = Block.box(14, 0, 0, 16, 16, 16);
	private static final VoxelShape EAST = Block.box(0, 0, 0, 2, 16, 16);

	protected NetworkPanelBlock(Properties properties) {
		super(properties);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		Direction face = context.getClickedFace();
		Direction facing = face.getAxis().isHorizontal() ? face : context.getHorizontalDirection().getOpposite();
		return defaultBlockState().setValue(FACING, facing);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return switch (state.getValue(FACING)) {
			case SOUTH -> SOUTH;
			case WEST -> WEST;
			case EAST -> EAST;
			default -> NORTH;
		};
	}

	@Override
	protected StorageTerminalBlockEntity.Permission linkPermission() {
		return StorageTerminalBlockEntity.Permission.VIEW;
	}

	@Override
	protected void open(ServerPlayer player, LinkedPanelBlockEntity panel, StorageTerminalBlockEntity terminal) {
		player.sendOverlayMessage(Component.translatable("jtechstorage.display.help"));
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		if (level.isClientSide()) {
			return null;
		}

		return (lvl, pos, st, be) -> {
			if (be instanceof NetworkPanelBlockEntity panel) {
				NetworkPanelBlockEntity.serverTick(lvl, pos, st, panel);
			}
		};
	}
}
