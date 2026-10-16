package dev.bartels.jtechstorage.backpack;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.util.RandomSource;

/**
 * Abgestellter Rucksack (Shift + Rechtsklick mit dem Rucksack auf den Boden). Normales Blockmodell je Stufe (Stoff in der Rucksack-Farbe getönt),
 * Rechtsklick öffnet ihn, Abbauen gibt ihn mitsamt Inhalt zurück.
 */
public class PlacedBackpackBlock extends HorizontalDirectionalBlock implements EntityBlock {
	private static final VoxelShape SHAPE = Block.box(3, 0, 3, 13, 14, 13);
	public static final net.minecraft.world.level.block.state.properties.EnumProperty<BackpackItem.Tier> TIER =
			net.minecraft.world.level.block.state.properties.EnumProperty.create("tier", BackpackItem.Tier.class);

	public PlacedBackpackBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(TIER, BackpackItem.Tier.LEATHER));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, TIER);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new PlacedBackpackBlockEntity(pos, state);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer
				&& level.getBlockEntity(pos) instanceof PlacedBackpackBlockEntity backpack && backpack.tier() != null) {
			int rows = backpack.tier().rows;
			serverPlayer.openMenu(new SimpleMenuProvider((id, inventory, p) -> new PlacedBackpackMenu(id, inventory, backpack, rows),
					backpack.getBackpack().getHoverName()));
			level.playSound(null, pos, SoundEvents.BUNDLE_DROP_CONTENTS, SoundSource.BLOCKS, 0.8f, 1.0f);
		}

		return InteractionResult.SUCCESS;
	}

	@Override
	protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		return level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP);
	}

	@Override
	protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
			BlockPos neighborPos, BlockState neighborState, RandomSource random) {
		return direction == Direction.DOWN && !canSurvive(state, level, pos) ? Blocks.AIR.defaultBlockState()
				: super.updateShape(state, level, ticks, pos, direction, neighborPos, neighborState, random);
	}

	/** Shift + Rechtsklick mit einem Rucksack: abstellen. */
	public static InteractionResult place(Level level, BlockPos pos, Direction facing, Player player, ItemStack stack) {
		if (!(stack.getItem() instanceof BackpackItem item)) {
			return InteractionResult.PASS;
		}

		BlockState state = dev.bartels.jtechstorage.ModContent.PLACED_BACKPACK.defaultBlockState().setValue(FACING, facing)
				.setValue(TIER, item.getTier());

		if (!level.getBlockState(pos).canBeReplaced() || !state.canSurvive(level, pos)) {
			return InteractionResult.FAIL;
		}

		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}

		level.setBlock(pos, state, 3);

		if (level.getBlockEntity(pos) instanceof PlacedBackpackBlockEntity backpack) {
			backpack.setBackpack(stack);
		}

		stack.shrink(1);
		level.playSound(null, pos, SoundEvents.ARMOR_EQUIP_LEATHER.value(), SoundSource.BLOCKS, 1.0f, 0.9f);
		return InteractionResult.SUCCESS;
	}
}
