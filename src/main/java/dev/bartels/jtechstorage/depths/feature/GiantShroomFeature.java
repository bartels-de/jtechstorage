package dev.bartels.jtechstorage.depths.feature;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

import dev.bartels.jtechstorage.depths.DepthsContent;

/** Riesen-Resonanzpilz (v0.33): heller Stiel, gewölbter, federnder Hut; unter dem Hutrand hängen Ranken. */
public record GiantShroomFeature() implements Feature {
	public static final MapCodec<GiantShroomFeature> CODEC = MapCodec.unit(new GiantShroomFeature());

	@Override
	public MapCodec<GiantShroomFeature> codec() {
		return CODEC;
	}

	@Override
	public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
		BlockPos base = Gen.floor(level, origin, 32);

		if (base == null || !level.getFluidState(base).isEmpty()) {
			return false;
		}

		int space = Gen.space(level, base, 24);

		if (space < 7) {
			return false;
		}

		int stem = Math.min(space - 3, 4 + random.nextInt(6));
		int r = stem > 7 ? 4 : 3;
		BlockState stemState = DepthsContent.RESONANCE_STEM.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
		BlockState cap = DepthsContent.RESONANCE_CAP.defaultBlockState();

		for (int y = 0; y < stem; y++) {
			Gen.place(level, base.above(y), stemState);
		}

		BlockPos top = base.above(stem);

		for (int dx = -r; dx <= r; dx++) {
			for (int dz = -r; dz <= r; dz++) {
				int d = dx * dx + dz * dz;

				if (d > r * r + 1) {
					continue;
				}

				Gen.place(level, top.offset(dx, 0, dz), cap);

				if (d <= (r - 2) * (r - 2) + 1) {
					Gen.place(level, top.offset(dx, 1, dz), cap);
				}

				if (d >= (r - 1) * (r - 1)) {
					Gen.place(level, top.offset(dx, -1, dz), cap);

					if (random.nextInt(4) == 0) {
						int len = 1 + random.nextInt(3);

						for (int k = 2; k < 2 + len; k++) {
							Gen.place(level, top.offset(dx, -k, dz), DepthsContent.CRYSTAL_VINES.defaultBlockState());
						}
					}
				}
			}
		}

		return true;
	}
}
