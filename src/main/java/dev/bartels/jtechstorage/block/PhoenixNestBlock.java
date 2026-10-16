package dev.bartels.jtechstorage.block;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;

import dev.bartels.jtechstorage.ModContent;
import dev.bartels.jtechstorage.menu.NestMenu;

/** Phönix-Horst: Nest aus Zweigen und Amethyst – Post-Adresse eines Netzwerks. */
public class PhoenixNestBlock extends LinkedPanelBlock {
	private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 7, 16);

	public PhoenixNestBlock(Properties properties) {
		super(properties);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new PhoenixNestBlockEntity(pos, state);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		if (level.isClientSide() || type != ModContent.PHOENIX_NEST_BLOCK_ENTITY) {
			return null;
		}

		return (BlockEntityTicker<T>) (BlockEntityTicker<PhoenixNestBlockEntity>) PhoenixNestBlockEntity::serverTick;
	}

	@Override
	protected StorageTerminalBlockEntity.Permission linkPermission() {
		return StorageTerminalBlockEntity.Permission.SETTINGS;
	}

	@Override
	protected void open(ServerPlayer player, LinkedPanelBlockEntity panel, StorageTerminalBlockEntity terminal) {
		if (!(panel instanceof PhoenixNestBlockEntity nest)) {
			return;
		}

		if (!terminal.canAccess(player)) {
			player.sendOverlayMessage(Component.translatable("jtechstorage.message.not_owner", terminal.getOwnerName()));
			return;
		}

		nest.register();
		player.openMenu(new ExtendedMenuProvider<NestMenu.OpenData>() {
			@Override
			public NestMenu.OpenData getScreenOpeningData(ServerPlayer serverPlayer) {
				return new NestMenu.OpenData(panel.getBlockPos());
			}

			@Override
			public Component getDisplayName() {
				return getName();
			}

			@Override
			public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player menuPlayer) {
				return new NestMenu(containerId, inventory, nest);
			}
		});
	}
}
