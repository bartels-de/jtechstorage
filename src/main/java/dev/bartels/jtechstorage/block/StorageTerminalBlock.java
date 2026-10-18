package dev.bartels.jtechstorage.block;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;

import dev.bartels.jtechstorage.ModContent;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Speicher-Terminal bzw. Crafting-Terminal. Speichert selbst nichts, sondern zeigt alle
 * mit dem Schlüssel verknüpften Kisten an. Die Vorderseite (Bildschirm) zeigt zum Spieler.
 */
public class StorageTerminalBlock extends HorizontalDirectionalBlock implements EntityBlock {
	private final boolean crafting;

	public StorageTerminalBlock(Properties properties, boolean crafting) {
		super(properties);
		this.crafting = crafting;
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
	}

	public boolean isCrafting() {
		return crafting;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new StorageTerminalBlockEntity(pos, state);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		if (level.isClientSide() || type != ModContent.STORAGE_TERMINAL_BLOCK_ENTITY) {
			return null;
		}

		return (BlockEntityTicker<T>) (BlockEntityTicker<StorageTerminalBlockEntity>) StorageTerminalBlockEntity::serverTick;
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
		super.setPlacedBy(level, pos, state, placer, stack);

		if (!level.isClientSide() && placer instanceof Player player
				&& level.getBlockEntity(pos) instanceof StorageTerminalBlockEntity terminal) {
			terminal.setOwner(player);
		}
	}

	@Override
	public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!level.isClientSide() && level.getBlockEntity(pos) instanceof StorageTerminalBlockEntity terminal) {
			if (terminal.canAccess(player)) {
				player.openMenu(terminal);

				if (terminal.isDisturbed()) {
					player.sendOverlayMessage(net.minecraft.network.chat.Component.translatable("jtechstorage.terminal.storm")
							.withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE));
				}
			} else {
				player.sendOverlayMessage(Component.translatable("jtechstorage.message.not_owner", terminal.getOwnerName()));
			}
		}

		return InteractionResult.SUCCESS;
	}
}
