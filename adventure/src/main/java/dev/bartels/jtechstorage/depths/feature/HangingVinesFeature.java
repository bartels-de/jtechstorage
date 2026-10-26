package dev.bartels.jtechstorage.depths.feature;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

import dev.bartels.jtechstorage.depths.DepthsContent;

/** Tuft of crystal vines on the cave ceiling (v0.32). */
public record HangingVinesFeature() implements Feature {
	public static final MapCodec<HangingVinesFeature> CODEC = MapCodec.unit(new HangingVinesFeature());

	@Override
	public MapCodec<HangingVinesFeature> codec() {
		return CODEC;
	}

	@Override
	public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
		BlockPos centre = Gen.ceiling(level, origin, 32);

		if (centre == null) {
			return false;
		}

		boolean any = false;

		for (int i = 0; i < 14; i++) {
			BlockPos top = Gen.ceiling(level, centre.offset(random.nextInt(9) - 4, -3, random.nextInt(9) - 4), 8);

			if (top == null) {
				continue;
			}

			int length = 2 + random.nextInt(7);

			for (int k = 0; k < length && level.getBlockState(top.below(k)).isAir(); k++) {
				Gen.set(level, top.below(k), DepthsContent.CRYSTAL_VINES.defaultBlockState());
				any = true;
			}
		}

		return any;
	}
}
