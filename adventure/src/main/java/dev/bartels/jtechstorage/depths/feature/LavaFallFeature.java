package dev.bartels.jtechstorage.depths.feature;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.material.Fluids;


/** Lava Fall (v0.30): a lava source in the gorge wall that flows downward. */
public record LavaFallFeature() implements Feature {
	public static final MapCodec<LavaFallFeature> CODEC = MapCodec.unit(new LavaFallFeature());

	@Override
	public MapCodec<LavaFallFeature> codec() {
		return CODEC;
	}

	@Override
	public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
		if (!level.getBlockState(origin).isAir()) {
			return false;
		}

		Direction dir = Direction.Plane.HORIZONTAL.getRandomDirection(random);
		BlockPos p = origin;

		for (int i = 0; i < 10 && level.getBlockState(p).isAir(); i++) {
			p = p.relative(dir);
		}

		BlockState wall = level.getBlockState(p);

		// source in the wall: stone must be above, below and behind, air in front
		if (!Gen.solid(wall) || !Gen.solid(level.getBlockState(p.above())) || !Gen.solid(level.getBlockState(p.relative(dir)))
				|| !Gen.solid(level.getBlockState(p.below())) || !level.getBlockState(p.relative(dir.getOpposite())).isAir()) {
			return false;
		}

		Gen.set(level, p, Blocks.LAVA.defaultBlockState());
		level.scheduleTick(p, Fluids.LAVA, 0);
		return true;
	}
}
