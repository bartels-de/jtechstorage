package dev.bartels.jtechstorage.depths.memory;

import org.jspecify.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
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
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;

import dev.bartels.jtechstorage.JTechStorage;
import net.minecraft.util.Prediction;

/**
 * Memory Case (v0.36): right-click with a Memory Crystal to display it, right-click with an empty hand shows
 * the progress ("7/12"), sneaking + empty hand takes the most recently inserted one back out. When the case is full, you get
 * the advancement "All Memories".
 */
public class MemoryCaseBlock extends Block implements EntityBlock {
	public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

	public MemoryCaseBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
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
	protected BlockState rotate(BlockState state, Rotation rotation) {
		return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
	}

	@Override
	protected BlockState mirror(BlockState state, Mirror mirror) {
		return state.rotate(mirror.getRotation(state.getValue(FACING)));
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new MemoryCaseBlockEntity(pos, state);
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
			InteractionHand hand, BlockHitResult hit) {
		int kind = MemoryContent.kindOf(stack.getItem());

		if (kind < 0 || !(level.getBlockEntity(pos) instanceof MemoryCaseBlockEntity memory)) {
			return InteractionResult.TRY_WITH_EMPTY_HAND;
		}

		if (memory.has(kind)) {
			if (!level.isClientSide()) {
				player.sendOverlayMessage(Component.translatable("block.jtechstorage.memory_case.already").withStyle(ChatFormatting.GRAY));
			}

			return InteractionResult.SUCCESS;
		}

		if (!level.isClientSide()) {
			memory.add(kind);
			stack.consume(1, player);
			level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 1.0f, 0.8f + kind * 0.06f);
			status(player, memory);
		}

		return InteractionResult.SUCCESS;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!(level.getBlockEntity(pos) instanceof MemoryCaseBlockEntity memory)) {
			return InteractionResult.PASS;
		}

		if (!level.isClientSide()) {
			if (player.isSecondaryUseActive()) {
				ItemStack taken = memory.takeLast();

				if (!taken.isEmpty()) {
					player.getInventory().placeItemBackInInventory(taken, Prediction.SERVER_ONLY);
					level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.BLOCKS, 0.8f, 1.2f);
				}
			}

			status(player, memory);
		}

		return InteractionResult.SUCCESS;
	}

	private static void status(Player player, MemoryCaseBlockEntity memory) {
		int count = memory.count();
		int all = MemoryContent.KINDS.length;
		player.sendOverlayMessage(Component.translatable("block.jtechstorage.memory_case.status", count, all)
				.withStyle(count == all ? ChatFormatting.GOLD : ChatFormatting.LIGHT_PURPLE));

		if (count == all && player instanceof ServerPlayer serverPlayer) {
			AdvancementHolder holder = serverPlayer.level().getServer().getAdvancements().get(JTechStorage.id("all_memories"));

			if (holder != null) {
				serverPlayer.getAdvancements().award(holder, "case");
			}
		}
	}
}
