package dev.bartels.jtechstorage.depths.feature;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

import dev.bartels.jtechstorage.depths.DepthsContent;

/**
 * Kristall-Schrein (v0.32): kleiner, gut erhaltener Altar der Lagermeister – Plattform mit Stufen, vier Pfeilern mit
 * Laternen und einer Truhe auf dem Sockel (immer mit einer Notiz und reinem Amethyst).
 */
public record CrystalShrineFeature() implements Feature {
	public static final MapCodec<CrystalShrineFeature> CODEC = MapCodec.unit(new CrystalShrineFeature());

	@Override
	public MapCodec<CrystalShrineFeature> codec() {
		return CODEC;
	}

	@Override
	public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
		BlockPos ground = Ruins.flatGround(level, origin, 4, 2);

		if (ground == null || Gen.space(level, ground, 7) < 6) {
			return false;
		}

		// v0.37: jeder zweite Schrein ist ein überdachter Pavillon (wenn die Höhle hoch genug ist)
		boolean pavilion = random.nextBoolean() && Gen.space(level, ground, 11) >= 10;
		BlockPos base = ground.above();   // Plattform eine Stufe über dem Boden
		BlockState polished = DepthsContent.POLISHED_CRYSTAL_STONE.defaultBlockState();
		BlockState chiseled = DepthsContent.CHISELED_CRYSTAL_STONE.defaultBlockState();

		for (int dx = -4; dx <= 4; dx++) {
			for (int dz = -4; dz <= 4; dz++) {
				BlockPos column = base.offset(dx, 0, dz);
				int ring = Math.max(Math.abs(dx), Math.abs(dz));

				for (int y = 0; y < 6; y++) {
					Gen.set(level, column.above(y), Blocks.AIR.defaultBlockState());
				}

				if (ring == 4) {
					// Stufen ringsum
					Direction facing = Math.abs(dx) == 4 ? (dx > 0 ? Direction.WEST : Direction.EAST) : (dz > 0 ? Direction.NORTH : Direction.SOUTH);
					Ruins.foundation(level, column.below(), DepthsContent.CRYSTAL_STONE_BRICKS.defaultBlockState());
					Gen.set(level, column, Math.abs(dx) == 4 && Math.abs(dz) == 4 ? polished
							: DepthsContent.CRYSTAL_STONE_BRICK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, facing));
				} else {
					Ruins.foundation(level, column, ring <= 1 ? chiseled : polished);
				}
			}
		}

		// Pfeiler mit Laternen (Pavillon: höhere Pfeiler, die das Dach tragen)
		for (int[] c : new int[][] {{-3, -3}, {3, -3}, {-3, 3}, {3, 3}}) {
			BlockPos p = base.offset(c[0], 1, c[1]);
			int height = pavilion ? 4 : 3;

			for (int y = 0; y < height; y++) {
				Gen.set(level, p.above(y), y == 1 ? DepthsContent.GLOW_CRYSTAL.defaultBlockState() : chiseled);
			}

			if (!pavilion) {
				Ruins.lantern(level, p.above(3), false);
			}
		}

		if (pavilion) {
			roof(level, base.above(5), chiseled);
			Ruins.lantern(level, base.above(4), true);

			// halbhohe Brüstung zwischen den Pfeilern, offen zu den Treppen
			BlockState wallSlab = DepthsContent.CRYSTAL_STONE_BRICK_SLAB.defaultBlockState();

			for (int k = -2; k <= 2; k++) {
				if (Math.abs(k) == 2) {
					for (int[] q : new int[][] {{k, -3}, {k, 3}, {-3, k}, {3, k}}) {
						Gen.set(level, base.offset(q[0], 1, q[1]), wallSlab);
					}
				}
			}
		}

		// Sockel mit Truhe
		Gen.set(level, base.above(), chiseled);
		Ruins.chest(level, random, base.above(2), Direction.Plane.HORIZONTAL.getRandomDirection(random), Ruins.SHRINE_LOOT);

		for (Direction d : Direction.Plane.HORIZONTAL) {
			Gen.set(level, base.above().relative(d, 2), Gen.cluster(random, Direction.UP));
		}

		return true;
	}

	/** Gestuftes Dach: Treppenkranz, Stufenring, Spitze mit Leuchtkristall. */
	private static void roof(WorldGenLevel level, BlockPos top, BlockState chiseled) {
		for (int dx = -4; dx <= 4; dx++) {
			for (int dz = -4; dz <= 4; dz++) {
				int ring = Math.max(Math.abs(dx), Math.abs(dz));
				BlockPos p = top.offset(dx, 0, dz);

				if (ring == 4) {
					Direction facing = Math.abs(dx) >= Math.abs(dz) ? (dx > 0 ? Direction.WEST : Direction.EAST) : (dz > 0 ? Direction.NORTH : Direction.SOUTH);
					Gen.set(level, p, DepthsContent.CRYSTAL_STONE_BRICK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, facing));
				} else if (ring == 3) {
					Gen.set(level, p, DepthsContent.CRYSTAL_STONE_BRICKS.defaultBlockState());
				} else if (ring == 2) {
					Gen.set(level, p.above(), DepthsContent.CRYSTAL_STONE_BRICK_SLAB.defaultBlockState());
				} else if (ring == 1) {
					Gen.set(level, p.above(), DepthsContent.POLISHED_CRYSTAL_STONE.defaultBlockState());
				} else {
					Gen.set(level, p.above(), chiseled);
					Gen.set(level, p.above(2), DepthsContent.GLOW_CRYSTAL.defaultBlockState());
				}
			}
		}
	}
}
