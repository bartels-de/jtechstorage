package dev.bartels.jtechstorage.depths.feature;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

import dev.bartels.jtechstorage.depths.DepthsContent;

/** Lavafall (v0.30): eine Lavaquelle in der Schluchtwand, die nach unten fließt. */
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

		// Quelle in der Wand: oben, unten und hinten muss Stein sein, vorne Luft
		if (!Gen.solid(wall) || !Gen.solid(level.getBlockState(p.above())) || !Gen.solid(level.getBlockState(p.relative(dir)))
				|| !Gen.solid(level.getBlockState(p.below())) || !level.getBlockState(p.relative(dir.getOpposite())).isAir()) {
			return false;
		}

		Gen.set(level, p, Blocks.LAVA.defaultBlockState());
		level.scheduleTick(p, net.minecraft.world.level.material.Fluids.LAVA, 0);
		return true;
	}
}
