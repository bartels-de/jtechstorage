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

/** Hängender Kristallzapfen (v0.30): umgedrehter Kegel aus Leuchtkristall und Amethyst an der Höhlendecke. */
public record HangingCrystalFeature() implements Feature {
	public static final MapCodec<HangingCrystalFeature> CODEC = MapCodec.unit(new HangingCrystalFeature());

	@Override
	public MapCodec<HangingCrystalFeature> codec() {
		return CODEC;
	}

	@Override
	public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
		BlockPos top = Gen.ceiling(level, origin, 32);

		if (top == null) {
			return false;
		}

		int below = 0;

		while (below < 30 && Gen.open(level.getBlockState(top.below(below)))) {
			below++;
		}

		if (below < 8) {
			return false;
		}

		int length = Math.min(below - 4, 5 + random.nextInt(12));
		float radius = 1.2f + random.nextFloat() * 1.8f;
		BlockState glow = DepthsContent.GLOW_CRYSTAL.defaultBlockState();
		BlockState amethyst = Blocks.AMETHYST_BLOCK.defaultBlockState();

		for (int y = 0; y < length; y++) {
			float r = radius * (1 - y / (float) length);
			int ri = Mth.ceil(r);

			for (int dx = -ri; dx <= ri; dx++) {
				for (int dz = -ri; dz <= ri; dz++) {
					if (dx * dx + dz * dz <= r * r + 0.3f) {
						Gen.place(level, top.offset(dx, -y, dz), y > 1 && random.nextFloat() < 0.05f ? glow : amethyst);
					}
				}
			}
		}

		Gen.place(level, top.below(length), Gen.cluster(random, Direction.DOWN));
		return true;
	}
}
