package dev.bartels.jtechstorage.depths.memory;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Memory Crystal in the world (v0.36): a floating shard above a small pedestal. {@link #KIND} selects the
 * biome (0..8, same order as {@link MemoryContent#KINDS}); when mined it drops the matching crystal.
 */
public class MemoryCrystalBlock extends Block {
	public static final IntegerProperty KIND = IntegerProperty.create("kind", 0, MemoryContent.WORLD_KINDS - 1);
	private static final VoxelShape SHAPE = Shapes.or(Block.box(2, 0, 2, 14, 3, 14), Block.box(5, 3, 5, 11, 15, 11));

	public MemoryCrystalBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(KIND, 0));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(KIND);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		if (random.nextInt(3) == 0) {
			double a = random.nextDouble() * Math.PI * 2;
			level.addParticle(ParticleTypes.END_ROD, pos.getX() + 0.5 + Math.cos(a) * 0.45, pos.getY() + 0.3 + random.nextDouble() * 0.6,
					pos.getZ() + 0.5 + Math.sin(a) * 0.45, 0, 0.015, 0);
		}
	}
}
