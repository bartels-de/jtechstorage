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
 * Aderschlucht (v0.30): unterhalb der Wasserlinie wird aus Wasser Lava – so fließt unten in den Schluchten ein
 * glühender Strom, während die Kristallseen türkis bleiben. Einmal pro Chunk.
 */
public record ChasmLavaFeature() implements Feature {
	public static final MapCodec<ChasmLavaFeature> CODEC = MapCodec.unit(new ChasmLavaFeature());

	@Override
	public MapCodec<ChasmLavaFeature> codec() {
		return CODEC;
	}

	@Override
	public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
		int baseX = origin.getX() & ~15;
		int baseZ = origin.getZ() & ~15;
		int sea = generator.getSeaLevel();
		BlockState lava = Blocks.LAVA.defaultBlockState();
		BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
		boolean any = false;

		for (int x = 0; x < 16; x++) {
			for (int z = 0; z < 16; z++) {
				p.set(baseX + x, sea, baseZ + z);

				if (!level.getBiome(p).is(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.BIOME,
						dev.bartels.jtechstorage.JTechStorage.id("vein_chasm")))) {
					continue;
				}

				for (int y = sea; y > level.getMinY() + 4; y--) {
					p.setY(y);
					BlockState state = level.getBlockState(p);

					if (state.is(Blocks.WATER)) {
						Gen.set(level, p, lava);
						any = true;
					}
				}
			}
		}

		return any;
	}
}
