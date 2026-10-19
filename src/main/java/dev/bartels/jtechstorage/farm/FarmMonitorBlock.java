package dev.bartels.jtechstorage.farm;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;

import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;

import dev.bartels.jtechstorage.ModContent;

/**
 * Farm-Monitor (v0.27.0): an die Ausgabe-Truhe einer Farm stellen (Rückseite an die Truhe). Bildschirm grün = läuft,
 * rot = steht still, voll oder kein Behälter. Rechtsklick: Statistik und Einstellungen; Speicher-Schlüssel: mit dem
 * Terminal verbinden.
 */
public class FarmMonitorBlock extends Block implements EntityBlock {
	public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
	public static final BooleanProperty WARN = BooleanProperty.create("warn");

	public FarmMonitorBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(WARN, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, WARN);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	@Override
	protected BlockState rotate(BlockState state, Rotation rotation) {
		return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
	}

	@Override
	protected BlockState mirror(BlockState state, Mirror mirror) {
		return state.rotate(mirror.getRotation(state.getValue(FACING)));
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new FarmMonitorBlockEntity(pos, state);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		if (level.isClientSide() || type != FarmContent.FARM_MONITOR_BLOCK_ENTITY) {
			return null;
		}

		return (BlockEntityTicker<T>) (BlockEntityTicker<FarmMonitorBlockEntity>) FarmMonitorBlockEntity::serverTick;
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
			BlockHitResult hit) {
		if (stack.is(ModContent.STORAGE_KEY) && level.getBlockEntity(pos) instanceof FarmMonitorBlockEntity monitor) {
			return monitor.linkWithKey(player, stack);          // der Schlüssel verbindet den Monitor mit dem Terminal
		}

		return InteractionResult.TRY_WITH_EMPTY_HAND;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (player instanceof ServerPlayer serverPlayer && level.getBlockEntity(pos) instanceof FarmMonitorBlockEntity monitor) {
			open(serverPlayer, monitor);
		}

		return InteractionResult.SUCCESS;
	}

	static void open(ServerPlayer player, FarmMonitorBlockEntity monitor) {
		player.openMenu(new ExtendedMenuProvider<FarmMonitorMenu.OpenData>() {
			@Override
			public FarmMonitorMenu.OpenData getScreenOpeningData(ServerPlayer serverPlayer) {
				return new FarmMonitorMenu.OpenData(monitor.getBlockPos(), monitor.getFarmName());
			}

			@Override
			public Component getDisplayName() {
				return Component.translatable("block.jtechstorage.farm_monitor");
			}

			@Override
			public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
				return new FarmMonitorMenu(containerId, inventory, monitor);
			}
		});
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
		super.setPlacedBy(level, pos, state, placer, stack);

		if (level.isClientSide() || !(level.getBlockEntity(pos) instanceof FarmMonitorBlockEntity monitor)) {
			return;
		}

		if (placer != null) {
			monitor.setOwner(placer.getUUID());
		}

		Component customName = stack.get(DataComponents.CUSTOM_NAME);

		if (customName != null) {
			monitor.setFarmName(customName.getString());
		}
	}
}
