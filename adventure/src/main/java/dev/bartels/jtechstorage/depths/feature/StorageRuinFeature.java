package dev.bartels.jtechstorage.depths.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

import dev.bartels.jtechstorage.depths.DepthsContent;

/**
 * Abandoned storage ruin (v0.32, expanded v0.37). Three building layouts, each furnished:
 * <ul>
 * <li><b>Hall</b> (9×9): barrel shelves along the sides, terminal, workbench, carpet remnants.</li>
 * <li><b>Two rooms</b> (13×9): front room with shelves, behind it an archive with bookshelves and a lectern.</li>
 * <li><b>Hall with tower</b> (9×9 + tower 5×5): half-collapsed tower with a ladder, a chest and a lantern at the top.</li>
 * </ul>
 * Walls crumble depending on decay; cobwebs, crystals and rubble lie around. {@code flooded}: sunken hall at the
 * bottom of the Crystal Lakes (always the simple hall).
 */
public record StorageRuinFeature(boolean flooded) implements Feature {
	public static final MapCodec<StorageRuinFeature> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.BOOL.optionalFieldOf("flooded", false).forGetter(StorageRuinFeature::flooded)
	).apply(i, StorageRuinFeature::new));

	private enum Layout { HALL, TWO_ROOMS, TOWER }

	@Override
	public MapCodec<StorageRuinFeature> codec() {
		return CODEC;
	}

	@Override
	public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
		Layout layout = flooded ? Layout.HALL : Layout.values()[random.nextInt(3)];
		Direction door = Direction.Plane.HORIZONTAL.getRandomDirection(random);
		Direction back = door.getOpposite();
		// extent: half 4 across the door, 4 or 8 along (toward the back wall) for two rooms
		int halfW = 4;
		int depth = layout == Layout.TWO_ROOMS ? 12 : 8;
		int checkHalf = layout == Layout.TWO_ROOMS ? 6 : 4;
		BlockPos center = origin;
		BlockPos base = flooded ? waterFloor(level, origin) : Ruins.flatGround(level, center, checkHalf, 3);

		if (base == null || (!flooded && Gen.space(level, base, 8) < 7)) {
			return false;
		}

		// origin of the hall: middle of the door wall
		BlockPos front = base.relative(door, depth / 2);
		Frame f = new Frame(front, back, back.getClockWise());
		BlockState fill = flooded ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState();
		float decay = 0.25f + random.nextFloat() * 0.4f;

		shell(level, random, f, halfW, depth, fill, decay, true);

		if (layout == Layout.TWO_ROOMS) {
			innerWall(level, random, f, halfW, 6, fill);
		}

		roof(level, random, f, halfW, depth, decay);

		if (layout == Layout.TOWER) {
			tower(level, random, f.at(halfW - 1, depth - 1, 0), f, fill);
		}

		furnish(level, random, f, halfW, depth, layout, door);
		rubble(level, random, f, halfW, depth);
		return true;
	}

	/** Local grid: a = across (to the right as seen from the door), b = depth (0 = door wall), y = height. */
	private record Frame(BlockPos front, Direction in, Direction right) {
		BlockPos at(int a, int b, int y) {
			return front.relative(right, a).relative(in, b).above(y);
		}
	}

	/** Floor, walls, pillars and door; the interior is cleared out. */
	private void shell(WorldGenLevel level, RandomSource random, Frame f, int halfW, int depth, BlockState fill, float decay, boolean doorway) {
		BlockState floor = DepthsContent.POLISHED_CRYSTAL_STONE.defaultBlockState();
		BlockState pillar = DepthsContent.CHISELED_CRYSTAL_STONE.defaultBlockState();

		for (int a = -halfW; a <= halfW; a++) {
			for (int b = 0; b <= depth; b++) {
				BlockPos column = f.at(a, b, 0);
				Ruins.foundation(level, column.below(), random.nextInt(5) == 0 ? DepthsContent.CRACKED_CRYSTAL_STONE_BRICKS.defaultBlockState() : floor);
				boolean edge = Math.abs(a) == halfW || b == 0 || b == depth;
				boolean corner = Math.abs(a) == halfW && (b == 0 || b == depth);
				boolean midPillar = Math.abs(a) == halfW && b % 4 == 0;

				for (int y = 0; y < 6; y++) {
					if (!edge) {
						Gen.set(level, column.above(y), fill);
					}
				}

				if (!edge) {
					continue;
				}

				int height = corner || midPillar ? 5 : Mth.clamp(Math.round(5 - decay * 6 * (0.5f + random.nextFloat())), 0, 5);
				boolean door = doorway && b == 0 && Math.abs(a) <= 1;

				for (int y = 0; y < height; y++) {
					BlockPos p = column.above(y);

					if (door && y < 3) {
						Gen.set(level, p, fill);
						continue;
					}

					BlockState state = corner || midPillar ? pillar : (y == 2 && !door && random.nextInt(3) == 0
							? DepthsContent.CRYSTAL_GLASS.defaultBlockState() : Ruins.brick(random));
					Gen.set(level, p, state);
				}

				if (corner && !flooded) {
					Ruins.lantern(level, column.above(5), false);
				}
			}
		}

		// door lintel
		if (doorway) {
			for (int a = -1; a <= 1; a++) {
				Gen.set(level, f.at(a, 0, 3), DepthsContent.CRYSTAL_STONE_BRICK_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP));
			}
		}
	}

	/** Partition wall with a passage at depth {@code b}. */
	private void innerWall(WorldGenLevel level, RandomSource random, Frame f, int halfW, int b, BlockState fill) {
		for (int a = -halfW + 1; a < halfW; a++) {
			for (int y = 0; y < 4; y++) {
				BlockPos p = f.at(a, b, y);

				if (Math.abs(a) <= 0 && y < 3) {
					Gen.set(level, p, fill);
				} else if (random.nextInt(8) != 0 || y < 2) {
					Gen.set(level, p, Ruins.brick(random));
				}
			}
		}
	}

	/** Remnants of roof beams along the whole length. */
	private void roof(WorldGenLevel level, RandomSource random, Frame f, int halfW, int depth, float decay) {
		BlockState slab = DepthsContent.CRYSTAL_STONE_BRICK_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP);

		for (int b = 1; b < depth; b++) {
			if (random.nextFloat() > decay) {
				Gen.set(level, f.at(0, b, 4), slab);
			}
		}

		for (int b = 2; b < depth; b += 4) {
			for (int a = -halfW + 1; a < halfW; a++) {
				if (random.nextFloat() > decay + 0.15f) {
					Gen.set(level, f.at(a, b, 4), slab);
				}
			}
		}
	}

	/** Corner tower 5×5, up to 10 high, ladder inside, mezzanine floor with chest and lantern. */
	private void tower(WorldGenLevel level, RandomSource random, BlockPos corner, Frame f, BlockState fill) {
		int top = 8 + random.nextInt(3);
		BlockPos c = corner;   // inner corner of the hall; the tower grows outward

		for (int a = 0; a < 5; a++) {
			for (int b = 0; b < 5; b++) {
				BlockPos column = c.relative(f.right(), a - 1).relative(f.in(), b - 1);
				boolean edge = a == 0 || a == 4 || b == 0 || b == 4;
				Ruins.foundation(level, column.below(), DepthsContent.POLISHED_CRYSTAL_STONE.defaultBlockState());

				for (int y = 0; y <= top; y++) {
					BlockPos p = column.above(y);
					boolean broken = y > top - 3 && random.nextInt(3) == 0;

					if (edge && !broken) {
						boolean cornerPost = (a == 0 || a == 4) && (b == 0 || b == 4);
						Gen.set(level, p, cornerPost ? DepthsContent.CHISELED_CRYSTAL_STONE.defaultBlockState()
								: (y % 4 == 2 && random.nextInt(2) == 0 ? DepthsContent.CRYSTAL_GLASS.defaultBlockState() : Ruins.brick(random)));
					} else if (!edge) {
						Gen.set(level, p, y == 5 ? DepthsContent.CRYSTAL_STONE_BRICK_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP) : fill);
					}
				}
			}
		}

		// passage to the hall and ladder on the outer wall
		BlockPos inside = c.relative(f.right(), 1).relative(f.in(), 1);
		Direction ladderFacing = f.in().getOpposite();

		for (int y = 0; y < 3; y++) {
			Gen.set(level, c.relative(f.right(), 1).above(y), fill);
		}

		for (int y = 0; y <= 5; y++) {
			Gen.set(level, inside.relative(f.in(), 1).above(y),
					Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, ladderFacing));
		}

		Ruins.chest(level, random, inside.relative(f.right(), 1).above(6), f.right().getOpposite(), Ruins.RUIN_LOOT);
		Ruins.lantern(level, inside.above(6), false);
	}

	/** Furnishing per layout. */
	private void furnish(WorldGenLevel level, RandomSource random, Frame f, int halfW, int depth, Layout layout, Direction door) {
		Direction toDoor = f.in().getOpposite();
		int backB = layout == Layout.TWO_ROOMS ? 5 : depth - 1;

		// terminal on the back wall of the (front) room, barrel shelves left and right
		Ruins.terminal(level, f.at(0, backB, 0), toDoor);
		shelf(level, random, f, -halfW + 1, 2, backB, f.right());
		shelf(level, random, f, halfW - 1, 2, backB, f.right().getOpposite());
		Ruins.chest(level, random, f.at(halfW - 1, 1, 0), f.right().getOpposite(), Ruins.RUIN_LOOT);

		if (random.nextBoolean()) {
			Ruins.chest(level, random, f.at(-halfW + 1, 1, 0), f.right(), Ruins.RUIN_LOOT);
		}

		if (!flooded) {
			Ruins.lantern(level, f.at(0, depth / 2, 3), true);

			// carpet remnants in the middle
			for (int a = -1; a <= 1; a++) {
				for (int b = 2; b < backB - 1; b++) {
					if (random.nextInt(3) != 0 && level.getBlockState(f.at(a, b, 0)).isAir()) {
						Gen.set(level, f.at(a, b, 0), Blocks.CARPET.purple().defaultBlockState());
					}
				}
			}
		}

		// workbench in front of the terminal
		BlockState stair = DepthsContent.CRYSTAL_STONE_BRICK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, toDoor)
				.setValue(BlockStateProperties.WATERLOGGED, flooded);
		Gen.set(level, f.at(-1, backB, 0), stair);
		Gen.set(level, f.at(1, backB, 0), stair);

		if (layout == Layout.TWO_ROOMS) {
			// archive: bookshelves on the walls, lectern, chest with notes
			for (int b = 7; b < depth; b++) {
				for (int y = 0; y < 3; y++) {
					if (random.nextInt(5) != 0) {
						Gen.set(level, f.at(-halfW + 1, b, y), y == 2 && random.nextInt(3) == 0 ? Blocks.CHISELED_BOOKSHELF.defaultBlockState()
								.setValue(BlockStateProperties.HORIZONTAL_FACING, f.right()) : Blocks.BOOKSHELF.defaultBlockState());
					}

					if (random.nextInt(5) != 0) {
						Gen.set(level, f.at(halfW - 1, b, y), Blocks.BOOKSHELF.defaultBlockState());
					}
				}
			}

			Gen.set(level, f.at(0, depth - 2, 0), Blocks.LECTERN.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, toDoor));
			Ruins.chest(level, random, f.at(1, depth - 1, 0), toDoor, Ruins.SHRINE_LOOT);
			Ruins.lantern(level, f.at(0, 9, 3), true);
		}

		// cobwebs in upper corners
		if (!flooded) {
			for (int i = 0; i < 4; i++) {
				BlockPos p = f.at(random.nextBoolean() ? -halfW + 1 : halfW - 1, 1 + random.nextInt(depth - 1), 3);

				if (level.getBlockState(p).isAir()) {
					Gen.set(level, p, Blocks.COBWEB.defaultBlockState());
				}
			}
		}
	}

	/** Barrel shelf along a side wall from depth b0 to b1 (2 high, with gaps). */
	private void shelf(WorldGenLevel level, RandomSource random, Frame f, int a, int b0, int b1, Direction facing) {
		for (int b = b0; b < b1; b++) {
			for (int y = 0; y < 2; y++) {
				if (random.nextInt(4) != 0) {
					Gen.set(level, f.at(a, b, y), Blocks.BARREL.defaultBlockState().setValue(BarrelBlock.FACING, facing));
				}
			}
		}
	}

	/** Rubble and crystals in the interior. */
	private void rubble(WorldGenLevel level, RandomSource random, Frame f, int halfW, int depth) {
		for (int i = 0; i < 10; i++) {
			BlockPos p = f.at(random.nextInt(halfW * 2 - 1) - halfW + 1, 1 + random.nextInt(depth - 1), 0);
			BlockState here = level.getBlockState(p);

			if (here.isAir() || here.is(Blocks.WATER)) {
				BlockState rubble = random.nextInt(3) == 0 ? Gen.cluster(random, Direction.UP)
						: DepthsContent.CRYSTAL_STONE_BRICK_SLAB.defaultBlockState();

				if (flooded && rubble.hasProperty(BlockStateProperties.WATERLOGGED)) {
					rubble = rubble.setValue(BlockStateProperties.WATERLOGGED, true);
				}

				Gen.set(level, p, rubble);
			}
		}
	}

	/** For sunken halls: floor under water, at least 6 blocks deep. */
	private static BlockPos waterFloor(WorldGenLevel level, BlockPos origin) {
		BlockPos.MutableBlockPos p = origin.mutable();

		if (!level.getBlockState(p).is(Blocks.WATER)) {
			return null;
		}

		while (p.getY() > level.getMinY() + 2 && level.getBlockState(p.below()).is(Blocks.WATER)) {
			p.move(Direction.DOWN);
		}

		BlockPos floor = p.immutable();
		int up = 0;

		while (up < 12 && level.getBlockState(floor.above(up)).is(Blocks.WATER)) {
			up++;
		}

		return up >= 6 && Gen.solid(level.getBlockState(floor.below())) ? floor : null;
	}
}
