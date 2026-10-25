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

/** Streut Amethyst-Knospen und -Haufen an Boden, Wände und Decke rund um den Ursprung (v0.30). */
public record CrystalScatterFeature() implements Feature {
	public static final MapCodec<CrystalScatterFeature> CODEC = MapCodec.unit(new CrystalScatterFeature());
	private static final Direction[] DIRS = Direction.values();

	@Override
	public MapCodec<CrystalScatterFeature> codec() {
		return CODEC;
	}

	@Override
	public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
		boolean any = false;

		for (int i = 0; i < 64; i++) {
			BlockPos p = origin.offset(random.nextInt(15) - 7, random.nextInt(9) - 4, random.nextInt(15) - 7);

			if (!level.getBlockState(p).isAir()) {
				continue;
			}

			Direction against = DIRS[random.nextInt(DIRS.length)];

			if (Gen.solid(level.getBlockState(p.relative(against)))) {
				if (random.nextInt(40) == 0 && against == Direction.DOWN && level.getBlockState(p.below()).is(DepthsContent.CRYSTAL_STONE)) {
					Gen.set(level, p.relative(against), DepthsContent.GLOW_CRYSTAL.defaultBlockState());
				}

				Gen.set(level, p, Gen.cluster(random, against.getOpposite()));
				any = true;
			}
		}

		return any;
	}
}
