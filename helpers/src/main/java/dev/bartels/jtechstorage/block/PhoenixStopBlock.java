package dev.bartels.jtechstorage.block;

import dev.bartels.jtechstorage.helpers.HelpersContent;
import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;

import dev.bartels.jtechstorage.menu.PhoenixStopMenu;

/**
 * Phoenix stop: a landing spot for the Amethyst Phoenix. Right-click opens a window for name, rest duration
 * and delivery order (chests directly next to it).
 * Clicked with the flight plan, it is added to the route.
 */
public class PhoenixStopBlock extends Block implements EntityBlock {
	private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 16, 16);

	public PhoenixStopBlock(Properties properties) {
		super(properties);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new PhoenixStopBlockEntity(pos, state);
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
			InteractionHand hand, BlockHitResult hit) {
		// The flight plan handles the click itself (add stop)
		if (stack.is(HelpersContent.FLIGHT_PLAN)) {
			return InteractionResult.PASS;
		}

		return InteractionResult.TRY_WITH_EMPTY_HAND;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (player instanceof ServerPlayer serverPlayer && level.getBlockEntity(pos) instanceof PhoenixStopBlockEntity stop) {
			open(serverPlayer, stop);
		}

		return InteractionResult.SUCCESS;
	}

	private static void open(ServerPlayer player, PhoenixStopBlockEntity stop) {
		player.openMenu(new ExtendedMenuProvider<PhoenixStopMenu.OpenData>() {
			@Override
			public PhoenixStopMenu.OpenData getScreenOpeningData(ServerPlayer serverPlayer) {
				return new PhoenixStopMenu.OpenData(stop.getBlockPos(), stop.getStopName());
			}

			@Override
			public Component getDisplayName() {
				return Component.translatable("block.jtechstorage.phoenix_stop");
			}

			@Override
			public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
				return new PhoenixStopMenu(containerId, inventory, stop);
			}
		});
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
		super.setPlacedBy(level, pos, state, placer, stack);
		Component customName = stack.get(DataComponents.CUSTOM_NAME);

		if (level.isClientSide() || !(level.getBlockEntity(pos) instanceof PhoenixStopBlockEntity stop)) {
			return;
		}

		// Renamed in an anvil? Then the stop gets that name – otherwise ask for a name straight away.
		if (customName != null) {
			stop.apply(customName.getString(), stop.getRest());
		} else if (placer instanceof ServerPlayer serverPlayer) {
			open(serverPlayer, stop);
		}
	}
}
