package dev.bartels.jtechstorage.depths.feature;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

import dev.bartels.jtechstorage.JTechStorage;

/** Azurfrost (v0.33): die Oberfläche der Seen friert zu Eis, Pfützen werden zu Frostkristall. Einmal pro Chunk. */
public record FrozenLakeFeature() implements Feature {
	public static final MapCodec<FrozenLakeFeature> CODEC = MapCodec.unit(new FrozenLakeFeature());
	private static final ResourceKey<Biome> FROST = ResourceKey.create(Registries.BIOME, JTechStorage.id("azure_frost"));

	@Override
	public MapCodec<FrozenLakeFeature> codec() {
		return CODEC;
	}

	@Override
	public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
		int baseX = origin.getX() & ~15;
		int baseZ = origin.getZ() & ~15;
		int sea = generator.getSeaLevel();
		BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
		boolean any = false;

		for (int x = 0; x < 16; x++) {
			for (int z = 0; z < 16; z++) {
				p.set(baseX + x, sea - 1, baseZ + z);

				if (!level.getBiome(p).is(FROST)) {
					continue;
				}

				for (int y = sea - 1; y > sea - 5; y--) {
					p.setY(y);

					if (level.getBlockState(p).is(Blocks.WATER) && level.getBlockState(p.above()).isAir()) {
						Gen.set(level, p, random.nextInt(12) == 0 ? Blocks.PACKED_ICE.defaultBlockState() : Blocks.ICE.defaultBlockState());
						any = true;
						break;
					}
				}
			}
		}

		return any;
	}
}
