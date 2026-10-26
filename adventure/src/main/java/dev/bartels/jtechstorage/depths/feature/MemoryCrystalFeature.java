package dev.bartels.jtechstorage.depths.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

import dev.bartels.jtechstorage.depths.DepthsContent;
import dev.bartels.jtechstorage.depths.memory.MemoryContent;
import dev.bartels.jtechstorage.depths.memory.MemoryCrystalBlock;

/**
 * Memory Crystal (v0.36): a single glowing crystal on a chiseled stone in the middle of the
 * cave floor, surrounded by a few crystal buds. {@code kind} = biome (index into {@link MemoryContent#KINDS}).
 */
public record MemoryCrystalFeature(int kind) implements Feature {
	public static final MapCodec<MemoryCrystalFeature> CODEC = Codec.intRange(0, MemoryContent.WORLD_KINDS - 1).fieldOf("kind")
			.xmap(MemoryCrystalFeature::new, MemoryCrystalFeature::kind);

	@Override
	public MapCodec<MemoryCrystalFeature> codec() {
		return CODEC;
	}

	@Override
	public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
		BlockPos pos = Gen.floor(level, origin, 32);

		if (pos == null || Gen.space(level, pos, 4) < 3 || !level.getFluidState(pos).isEmpty()) {
			return false;
		}

		Gen.set(level, pos.below(), DepthsContent.CHISELED_CRYSTAL_STONE.defaultBlockState());
		Gen.set(level, pos, MemoryContent.MEMORY_CRYSTAL.defaultBlockState().setValue(MemoryCrystalBlock.KIND, kind));

		for (Direction side : Direction.Plane.HORIZONTAL) {
			if (random.nextInt(3) == 0) {
				continue;
			}

			BlockPos bud = pos.relative(side);

			if (Gen.open(level.getBlockState(bud)) && level.getFluidState(bud).isEmpty() && Gen.solid(level.getBlockState(bud.below()))) {
				Gen.set(level, bud, Gen.cluster(random, Direction.UP));
			}
		}

		return true;
	}
}
