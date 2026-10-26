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
 * Giant Crystal Spire (v0.30): grows upward from the cave floor – if there is room, it joins the ceiling as an hourglass-
 * shaped column. Amethyst core, glowing crystal veins, buds on the sides.
 */
public record CrystalSpireFeature() implements Feature {
	public static final MapCodec<CrystalSpireFeature> CODEC = MapCodec.unit(new CrystalSpireFeature());

	@Override
	public MapCodec<CrystalSpireFeature> codec() {
		return CODEC;
	}

	@Override
	public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
		BlockPos base = Gen.floor(level, origin, 32);

		if (base == null) {
			return false;
		}

		int space = Gen.space(level, base, 56);

		if (space < 8) {
			return false;
		}

		int wanted = 10 + random.nextInt(28);
		boolean column = space <= wanted + 8;   // reaches the ceiling: continue as a column
		int height = column ? space : Math.min(wanted, space - 4);
		float radius = 1.6f + random.nextFloat() * 1.8f + (height > 24 ? 1.2f : 0);
		BlockState amethyst = Blocks.AMETHYST_BLOCK.defaultBlockState();
		BlockState glow = DepthsContent.GLOW_CRYSTAL.defaultBlockState();
		BlockState budding = Blocks.BUDDING_AMETHYST.defaultBlockState();
		float tiltX = (random.nextFloat() - 0.5f) * 0.25f;
		float tiltZ = (random.nextFloat() - 0.5f) * 0.25f;

		for (int y = 0; y < height; y++) {
			float t = y / (float) Math.max(1, height - 1);
			float r = column ? radius * (0.55f + 0.45f * Math.abs(2 * t - 1)) : radius * (float) Math.pow(1 - t, 0.85);
			int cx = Math.round(tiltX * y);
			int cz = Math.round(tiltZ * y);
			int ri = Mth.ceil(r);

			for (int dx = -ri; dx <= ri; dx++) {
				for (int dz = -ri; dz <= ri; dz++) {
					float d = Mth.sqrt(dx * dx + dz * dz);

					if (d > r + 0.3f) {
						continue;
					}

					BlockPos p = base.offset(dx + cx, y, dz + cz);
					boolean shell = d > r - 1.1f;
					BlockState state = shell && random.nextFloat() < 0.03f ? glow : (!shell && random.nextFloat() < 0.06f ? budding : amethyst);
					Gen.place(level, p, state);
				}
			}
		}

		// tip glows, plus a crystal cluster
		if (!column) {
			BlockPos tip = base.offset(Math.round(tiltX * height), height, Math.round(tiltZ * height));
			Gen.place(level, tip.below(), glow);
			Gen.place(level, tip, Gen.cluster(random, Direction.UP));
		}

		// foot of crystal rock and buds all around
		int foot = Mth.ceil(radius) + 2;

		for (int i = 0; i < 24; i++) {
			int dx = random.nextInt(foot * 2 + 1) - foot;
			int dz = random.nextInt(foot * 2 + 1) - foot;
			BlockPos p = Gen.floor(level, base.offset(dx, 3, dz), 6);

			if (p != null && random.nextBoolean()) {
				Gen.place(level, p, Gen.cluster(random, Direction.UP));
			}
		}

		for (int i = 0; i < height * 2; i++) {
			int y = random.nextInt(Math.max(1, height));
			Direction side = Direction.Plane.HORIZONTAL.getRandomDirection(random);
			BlockPos p = base.offset(Math.round(tiltX * y), y, Math.round(tiltZ * y));

			for (int k = 0; k < 8 && !Gen.open(level.getBlockState(p)); k++) {
				p = p.relative(side);
			}

			if (Gen.open(level.getBlockState(p)) && Gen.solid(level.getBlockState(p.relative(side.getOpposite())))) {
				Gen.place(level, p, Gen.cluster(random, side));
			}
		}

		return true;
	}
}
