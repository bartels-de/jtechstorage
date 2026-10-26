package dev.bartels.jtechstorage.depths.feature;

import java.util.ArrayList;
import java.util.List;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

import dev.bartels.jtechstorage.JTechStorage;

/**
 * Vein chasm (v0.30): below the water line, water turns into lava, so a glowing stream runs along the bottom of the
 * chasms while the crystal lakes stay turquoise. Runs once per chunk.
 *
 * <p>World generation does not trigger fluid interactions, so lava bordering water of a neighbouring biome is turned
 * into obsidian here, matching what vanilla would do in game.
 */
public record ChasmLavaFeature() implements Feature {
	public static final MapCodec<ChasmLavaFeature> CODEC = MapCodec.unit(new ChasmLavaFeature());

	private static final ResourceKey<Biome> VEIN_CHASM = ResourceKey.create(Registries.BIOME, JTechStorage.id("vein_chasm"));

	/** Sides on which foreign water can touch the lava (below is always rock or more lava). */
	private static final Direction[] WATER_SIDES = {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST, Direction.UP};

	@Override
	public MapCodec<ChasmLavaFeature> codec() {
		return CODEC;
	}

	@Override
	public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
		int baseX = origin.getX() & ~15;
		int baseZ = origin.getZ() & ~15;
		int sea = generator.getSeaLevel();
		int minY = level.getMinY() + 4;
		BlockState lava = Blocks.LAVA.defaultBlockState();
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		List<BlockPos> placed = new ArrayList<>();

		for (int x = 0; x < 16; x++) {
			for (int z = 0; z < 16; z++) {
				pos.set(baseX + x, sea, baseZ + z);

				if (!isChasm(level, pos)) {
					continue;
				}

				for (int y = sea; y > minY; y--) {
					pos.setY(y);

					if (level.getBlockState(pos).is(Blocks.WATER)) {
						Gen.set(level, pos, lava);
						placed.add(pos.immutable());
					}
				}
			}
		}

		sealWaterBorder(level, placed, sea);
		return !placed.isEmpty();
	}

	/** Turns lava that touches water of a non-chasm column into obsidian. */
	private static void sealWaterBorder(WorldGenLevel level, List<BlockPos> lavaBlocks, int sea) {
		BlockState obsidian = Blocks.OBSIDIAN.defaultBlockState();
		BlockPos.MutableBlockPos neighbour = new BlockPos.MutableBlockPos();

		for (BlockPos pos : lavaBlocks) {
			for (Direction side : WATER_SIDES) {
				neighbour.setWithOffset(pos, side);

				if (level.getFluidState(neighbour).is(FluidTags.WATER) && (side == Direction.UP || !isChasm(level, neighbour.atY(sea)))) {
					Gen.set(level, pos, obsidian);
					break;
				}
			}
		}
	}

	/** Water in another chasm column turns into lava there as well, so only columns outside the chasm count as foreign. */
	private static boolean isChasm(WorldGenLevel level, BlockPos pos) {
		return level.getBiome(pos).is(VEIN_CHASM);
	}
}
