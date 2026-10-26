package dev.bartels.jtechstorage.depths.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

import dev.bartels.jtechstorage.depths.DepthsContent;

/**
 * Crystal Stalactite (v0.33): large cone of crystal rock hanging from the ceiling; the tip is made of crystal of the
 * palette. Often a matching stalagmite grows beneath it from the floor.
 */
public record StalactiteFeature(String palette) implements Feature {
	public static final MapCodec<StalactiteFeature> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.STRING.fieldOf("palette").forGetter(StalactiteFeature::palette)).apply(i, StalactiteFeature::new));

	@Override
	public MapCodec<StalactiteFeature> codec() {
		return CODEC;
	}

	@Override
	public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
		BlockPos top = Gen.ceiling(level, origin, 48);

		if (top == null) {
			return false;
		}

		int below = 0;

		while (below < 80 && Gen.open(level.getBlockState(top.below(below)))) {
			below++;
		}

		if (below < 12) {
			return false;
		}

		int length = Math.min(below / 2, 6 + random.nextInt(14));
		float radius = 1.5f + length * 0.12f + random.nextFloat();
		BlockState tip = Palette.crystal(palette, random).defaultBlockState();
		cone(level, random, top.above(), length, radius, -1, tip);

		if (random.nextInt(3) > 0) {
			BlockPos floor = top.below(below - 1);
			int up = Math.min(below - length - 4, 3 + random.nextInt(9));

			if (up > 2) {
				cone(level, random, floor.below(), up, radius * 0.9f, 1, tip);
			}
		}

		return true;
	}

	private void cone(WorldGenLevel level, RandomSource random, BlockPos root, int length, float radius, int dir, BlockState tip) {
		BlockState stone = DepthsContent.CRYSTAL_STONE.defaultBlockState();

		for (int i = 0; i <= length; i++) {
			float t = i / (float) length;
			float r = radius * (float) Math.pow(1 - t, 1.4) + 0.3f;
			int ri = Mth.ceil(r);

			for (int dx = -ri; dx <= ri; dx++) {
				for (int dz = -ri; dz <= ri; dz++) {
					if (dx * dx + dz * dz <= r * r) {
						Gen.place(level, root.offset(dx, i * dir, dz), t > 0.72f ? tip : stone);
					}
				}
			}
		}

		Gen.place(level, root.offset(0, (length + 1) * dir, 0), Palette.cluster(palette, random,
				dir < 0 ? Direction.DOWN : Direction.UP));
	}
}
