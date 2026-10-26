package dev.bartels.jtechstorage.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import dev.bartels.jtechstorage.entity.AmethystPhoenix;

/**
 * Signal lantern (v0.25): glows and emits a redstone signal (strength 15) while an Amethyst Phoenix is resting
 * at a stop nearby (8 blocks). Checks every half second.
 */
public class SignalLanternBlock extends Block {
	public static final BooleanProperty LIT = BlockStateProperties.LIT;
	public static final int RADIUS = 8;
	private static final int INTERVAL = 10;
	private static final VoxelShape SHAPE = Shapes.or(Block.box(5, 0, 5, 11, 8, 11), Block.box(6, 8, 6, 10, 10, 10));

	public SignalLanternBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(LIT, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(LIT);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moved) {
		super.onPlace(state, level, pos, old, moved);
		level.scheduleTick(pos, this, 2);
	}

	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		boolean active = phoenixNearby(level, pos);

		if (active != state.getValue(LIT)) {
			level.setBlock(pos, state.setValue(LIT, active), Block.UPDATE_ALL);
			level.playSound(null, pos, active ? SoundEvents.AMETHYST_BLOCK_RESONATE : SoundEvents.AMETHYST_BLOCK_STEP,
					SoundSource.BLOCKS, 0.8f, active ? 1.4f : 0.8f);
		}

		level.scheduleTick(pos, this, INTERVAL);
	}

	/** Is an Amethyst Phoenix resting nearby (not sitting, not flying)? */
	public static boolean phoenixNearby(Level level, BlockPos pos) {
		AABB box = new AABB(pos).inflate(RADIUS);
		return !level.getEntitiesOfClass(AmethystPhoenix.class, box,
				phoenix -> !phoenix.isHologram() && phoenix.getState() == AmethystPhoenix.STATE_RESTING && !phoenix.isSitting()).isEmpty();
	}

	@Override
	protected boolean isSignalSource(BlockState state) {
		return true;
	}

	@Override
	protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
		return state.getValue(LIT) ? 15 : 0;
	}
}
