package dev.bartels.jtechstorage.depths.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

/** Farbige Kristallbüschel an Boden, Wänden und Decke (v0.33). */
public record ColorClusterFeature(String palette, int tries) implements Feature {
	public static final MapCodec<ColorClusterFeature> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.STRING.fieldOf("palette").forGetter(ColorClusterFeature::palette),
			Codec.INT.optionalFieldOf("tries", 48).forGetter(ColorClusterFeature::tries)).apply(i, ColorClusterFeature::new));

	private static final Direction[] DIRS = Direction.values();

	@Override
	public MapCodec<ColorClusterFeature> codec() {
		return CODEC;
	}

	@Override
	public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
		boolean any = false;

		for (int i = 0; i < tries; i++) {
			BlockPos p = origin.offset(random.nextInt(15) - 7, random.nextInt(11) - 5, random.nextInt(15) - 7);

			if (!level.getBlockState(p).isAir()) {
				continue;
			}

			Direction against = DIRS[random.nextInt(DIRS.length)];

			if (Gen.solid(level.getBlockState(p.relative(against)))) {
				Gen.set(level, p, Palette.cluster(palette, random, against.getOpposite()));
				any = true;
			}
		}

		return any;
	}
}
