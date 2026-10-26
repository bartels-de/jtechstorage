package dev.bartels.jtechstorage.logistics;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Crystal Conveyor (v0.40): a flat belt that carries items lying on it, animals and players in its direction
 * (away from the placer) while keeping them centered. At the end it puts items into a chest, hopper or
 * sorting switch in front. Sneaking players stay put; a redstone signal stops the belt.
 */
public class ConveyorBlock extends Block {
	public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
	private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 2, 16);
	private static final VoxelShape INSIDE = Block.box(0, 0, 0, 16, 5, 16);
	private final double speed;

	public ConveyorBlock(double speed, Properties properties) {
		super(properties);
		this.speed = speed;
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection());
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
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	protected VoxelShape getEntityInsideCollisionShape(BlockState state, BlockGetter level, BlockPos pos, Entity entity) {
		return INSIDE;
	}

	@Override
	protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effects, boolean intersects) {
		if (entity instanceof Player player && (player.isShiftKeyDown() || player.getAbilities().flying) || level.hasNeighborSignal(pos)
				|| entity.getY() > pos.getY() + 0.3) {
			return;
		}

		Direction facing = state.getValue(FACING);
		Vec3 center = Vec3.atBottomCenterOf(pos);
		// pull toward the center across the direction of travel
		double side = facing.getAxis() == Direction.Axis.X ? center.z - entity.getZ() : center.x - entity.getX();
		double pull = Math.max(-0.08, Math.min(0.08, side * 0.2));
		Vec3 motion = entity.getDeltaMovement();
		double vx = facing.getStepX() * speed + (facing.getAxis() == Direction.Axis.Z ? pull : 0.0);
		double vz = facing.getStepZ() * speed + (facing.getAxis() == Direction.Axis.X ? pull : 0.0);

		if (entity instanceof Player) {
			// players can still walk themselves: add belt speed to their own movement
			entity.setDeltaMovement(motion.x * 0.6 + vx * 0.6, motion.y, motion.z * 0.6 + vz * 0.6);
		} else {
			entity.setDeltaMovement(vx, motion.y, vz);
		}

		if (!level.isClientSide() && entity instanceof ItemEntity item && item.isAlive()) {
			double ahead = (entity.getX() - center.x) * facing.getStepX() + (entity.getZ() - center.z) * facing.getStepZ();

			if (ahead > 0.3) {
				Container container = HopperBlockEntity.getContainerAt(level, pos.relative(facing));

				if (container != null) {
					HopperBlockEntity.addItem(container, item);
				}
			}
		}
	}
}
