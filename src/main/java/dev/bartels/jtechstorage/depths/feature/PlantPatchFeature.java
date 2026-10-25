package dev.bartels.jtechstorage.depths.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

/** Pflanzenfeld (v0.33): setzt {@code plant} auf freie Bodenstellen aus {@code ground}. */
public record PlantPatchFeature(Block plant, Block ground, int tries) implements Feature {
	public static final MapCodec<PlantPatchFeature> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			BuiltInRegistries.BLOCK.byNameCodec().fieldOf("plant").forGetter(PlantPatchFeature::plant),
			BuiltInRegistries.BLOCK.byNameCodec().fieldOf("ground").forGetter(PlantPatchFeature::ground),
			Codec.INT.optionalFieldOf("tries", 48).forGetter(PlantPatchFeature::tries)).apply(i, PlantPatchFeature::new));

	@Override
	public MapCodec<PlantPatchFeature> codec() {
		return CODEC;
	}

	@Override
	public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
		BlockPos centre = Gen.floor(level, origin, 24);

		if (centre == null) {
			return false;
		}

		boolean any = false;

		for (int i = 0; i < tries; i++) {
			BlockPos p = Gen.floor(level, centre.offset(random.nextInt(15) - 7, 3, random.nextInt(15) - 7), 7);

			if (p != null && level.getBlockState(p).isAir() && level.getBlockState(p.below()).is(ground)) {
				Gen.set(level, p, plant.defaultBlockState());
				any = true;
			}
		}

		return any;
	}
}
