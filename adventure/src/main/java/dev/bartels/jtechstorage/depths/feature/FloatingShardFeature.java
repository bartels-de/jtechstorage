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
 * Floating Crystal Shard (v0.30): small island of crystal rock in the middle of large caves – pointed at the bottom,
 * glow crystal and amethyst clusters on top, sometimes with a small spire of its own.
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

		// only in really large, open caves
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

		// underside: hanging clusters; top: clusters and possibly a small spire
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
