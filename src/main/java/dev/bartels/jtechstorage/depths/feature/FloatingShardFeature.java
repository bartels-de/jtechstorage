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

/**
 * Schwebender Kristallsplitter (v0.30): kleine Insel aus Kristallgestein mitten in großen Höhlen – unten spitz,
 * oben Leuchtkristall und Amethyst-Haufen, manchmal mit einer eigenen kleinen Säule.
 */
public record FloatingShardFeature() implements Feature {
	public static final MapCodec<FloatingShardFeature> CODEC = MapCodec.unit(new FloatingShardFeature());

	@Override
	public MapCodec<FloatingShardFeature> codec() {
		return CODEC;
	}

	@Override
	public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
		int r = 3 + random.nextInt(3);

		// nur in wirklich großen, offenen Höhlen
		for (BlockPos check : new BlockPos[] {origin, origin.offset(r + 3, 0, 0), origin.offset(-r - 3, 0, 0), origin.offset(0, 0, r + 3),
				origin.offset(0, 0, -r - 3), origin.above(8), origin.below(r * 2 + 6)}) {
			if (!level.getBlockState(check).isAir()) {
				return false;
			}
		}

		BlockState stone = DepthsContent.CRYSTAL_STONE.defaultBlockState();
		BlockState glow = DepthsContent.GLOW_CRYSTAL.defaultBlockState();
		BlockState amethyst = Blocks.AMETHYST_BLOCK.defaultBlockState();
		int depth = r * 2;

		for (int y = 0; y <= depth; y++) {
			float rr = r * (1 - y / (float) (depth + 1)) + (y == 0 ? 0.4f : 0);
			int ri = Mth.ceil(rr);

			for (int dx = -ri; dx <= ri; dx++) {
				for (int dz = -ri; dz <= ri; dz++) {
					float wobble = (Mth.sin(dx * 1.3f + dz * 0.7f + y) * 0.5f);

					if (dx * dx + dz * dz <= (rr + wobble) * (rr + wobble)) {
						BlockState state = y == 0 ? (random.nextFloat() < 0.05f ? glow : stone)
								: (random.nextFloat() < 0.08f ? amethyst : stone);
						Gen.place(level, origin.offset(dx, -y, dz), state);
					}
				}
			}
		}

		// Unterseite: hängende Haufen; Oberseite: Haufen und ggf. eine kleine Säule
		for (int i = 0; i < r * 6; i++) {
			int dx = random.nextInt(r * 2 + 1) - r;
			int dz = random.nextInt(r * 2 + 1) - r;
			BlockPos up = origin.offset(dx, 1, dz);

			if (Gen.solid(level.getBlockState(up.below())) && random.nextInt(3) == 0) {
				Gen.place(level, up, Gen.cluster(random, Direction.UP));
			}

			BlockPos down = origin.offset(dx, -depth - 2, dz);

			for (int k = 0; k < depth + 2 && !Gen.solid(level.getBlockState(down.above())); k++) {
				down = down.above();
			}

			if (Gen.solid(level.getBlockState(down.above())) && random.nextInt(3) == 0) {
				Gen.place(level, down, Gen.cluster(random, Direction.DOWN));
			}
		}

		if (random.nextInt(3) == 0) {
			int h = 3 + random.nextInt(5);

			for (int y = 1; y <= h; y++) {
				Gen.place(level, origin.above(y), y == h ? glow : amethyst);
			}

			Gen.place(level, origin.above(h + 1), Gen.cluster(random, Direction.UP));
		}

		return true;
	}
}
