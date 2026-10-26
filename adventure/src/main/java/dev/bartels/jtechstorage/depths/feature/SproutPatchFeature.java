package dev.bartels.jtechstorage.depths.feature;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

import dev.bartels.jtechstorage.depths.DepthsContent;

/** Patch of crystal sprouts on moss or rock (v0.30). */
public record SproutPatchFeature() implements Feature {
	public static final MapCodec<SproutPatchFeature> CODEC = MapCodec.unit(new SproutPatchFeature());

	@Override
	public MapCodec<SproutPatchFeature> codec() {
		return CODEC;
	}

	@Override
	public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
		BlockPos centre = Gen.floor(level, origin, 24);

		if (centre == null) {
			return false;
		}

		boolean any = false;

		for (int i = 0; i < 40; i++) {
			BlockPos p = Gen.floor(level, centre.offset(random.nextInt(13) - 6, 3, random.nextInt(13) - 6), 7);

			if (p != null && level.getBlockState(p).isAir() && level.getFluidState(p).isEmpty()) {
				Gen.set(level, p, DepthsContent.CRYSTAL_SPROUT.defaultBlockState());
				any = true;
			}
		}

		return any;
	}
}
