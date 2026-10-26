package dev.bartels.jtechstorage.depths.feature;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

import dev.bartels.jtechstorage.depths.DepthsContent;

/** Glowing Vein (v0.30): winds through the crystal rock and replaces it with vein stone. */
public record VeinFeature() implements Feature {
	public static final MapCodec<VeinFeature> CODEC = MapCodec.unit(new VeinFeature());

	@Override
	public MapCodec<VeinFeature> codec() {
		return CODEC;
	}

	@Override
	public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
		BlockState vein = DepthsContent.VEIN_STONE.defaultBlockState();
		double x = origin.getX();
		double y = origin.getY();
		double z = origin.getZ();
		double yaw = random.nextDouble() * Math.PI * 2;
		double pitch = (random.nextDouble() - 0.5) * 0.8;
		int steps = 20 + random.nextInt(25);
		boolean any = false;

		for (int i = 0; i < steps; i++) {
			yaw += (random.nextDouble() - 0.5) * 0.6;
			pitch = Mth.clamp(pitch + (random.nextDouble() - 0.5) * 0.3, -0.9, 0.9);
			x += Math.cos(yaw) * Math.cos(pitch);
			y += Math.sin(pitch);
			z += Math.sin(yaw) * Math.cos(pitch);

			if (Math.abs(x - origin.getX()) > 14 || Math.abs(z - origin.getZ()) > 14) {
				break;
			}

			int thick = random.nextInt(4) == 0 ? 1 : 0;

			for (int dx = -thick; dx <= thick; dx++) {
				for (int dy = -thick; dy <= thick; dy++) {
					for (int dz = -thick; dz <= thick; dz++) {
						BlockPos p = BlockPos.containing(x + dx, y + dy, z + dz);

						if (level.getBlockState(p).is(DepthsContent.CRYSTAL_STONE)) {
							Gen.set(level, p, vein);
							any = true;
						}
					}
				}
			}
		}

		return any;
	}
}
