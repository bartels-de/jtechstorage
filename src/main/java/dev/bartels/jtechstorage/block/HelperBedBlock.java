package dev.bartels.jtechstorage.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Helfer-Zuhause (v0.26): Helferbett (für die kleinen Helfer) und Wolfskorb (für Kristall- und Mondwölfe). Abends laufen
 * die Helfer zum nächsten freien Schlafplatz ihrer Art im Umkreis von 12 Blöcken um das Terminal und schlafen darauf.
 */
public class HelperBedBlock extends Block {
	public static final int KIND_BED = 1;
	public static final int KIND_BASKET = 2;
	public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

	private static final VoxelShape BASKET = Shapes.or(Block.box(1, 0, 1, 15, 3, 15),
			Block.box(1, 0, 1, 15, 6, 2), Block.box(1, 0, 14, 15, 5, 15), Block.box(1, 0, 1, 2, 6, 15), Block.box(14, 0, 1, 15, 6, 15));
	private static final VoxelShape BASKET_COLLISION = Block.box(1, 0, 1, 15, 3, 15);
	private static final VoxelShape BED_NS = Block.box(2, 0, 0, 14, 8, 16);
	private static final VoxelShape BED_EW = Block.box(0, 0, 2, 16, 8, 14);

	private final int kind;

	public HelperBedBlock(int kind, Properties properties) {
		super(properties);
		this.kind = kind;
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
	}

	public int kind() {
		return kind;
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
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		if (kind == KIND_BASKET) {
			return BASKET;
		}

		return state.getValue(FACING).getAxis() == Direction.Axis.Z ? BED_NS : BED_EW;
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return kind == KIND_BASKET ? BASKET_COLLISION : getShape(state, level, pos, context);
	}

	@Override
	protected BlockState rotate(BlockState state, Rotation rotation) {
		return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
	}

	@Override
	protected BlockState mirror(BlockState state, Mirror mirror) {
		return state.rotate(mirror.getRotation(state.getValue(FACING)));
	}
}
