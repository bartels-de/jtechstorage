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

/** Resonanzpilze auf dem Moos der Kristallseen (v0.30). */
public record ShroomPatchFeature() implements Feature {
	public static final MapCodec<ShroomPatchFeature> CODEC = MapCodec.unit(new ShroomPatchFeature());

	@Override
	public MapCodec<ShroomPatchFeature> codec() {
		return CODEC;
	}

	@Override
	public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
		BlockPos centre = Gen.floor(level, origin, 24);

		if (centre == null) {
			return false;
		}

		boolean any = false;

		for (int i = 0; i < 18; i++) {
			BlockPos p = Gen.floor(level, centre.offset(random.nextInt(13) - 6, 3, random.nextInt(13) - 6), 7);

			if (p != null && level.getBlockState(p).isAir() && level.getFluidState(p).isEmpty()) {
				Gen.set(level, p, DepthsContent.RESONANCE_SHROOM.defaultBlockState());
				any = true;
			}
		}

		return any;
	}
}
