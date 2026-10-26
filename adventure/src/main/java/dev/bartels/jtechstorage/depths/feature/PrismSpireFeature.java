package dev.bartels.jtechstorage.depths.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

/**
 * Prism Spire (v0.33): slender, slightly tilted crystal needle in one palette color, with a crystal tuft on top.
 * With {@code hanging} it grows from the ceiling downward (icicles in the Azure Frost).
 */
public record PrismSpireFeature(String palette, boolean hanging) implements Feature {
	public static final MapCodec<PrismSpireFeature> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.STRING.fieldOf("palette").forGetter(PrismSpireFeature::palette),
			Codec.BOOL.optionalFieldOf("hanging", false).forGetter(PrismSpireFeature::hanging)).apply(i, PrismSpireFeature::new));

	@Override
	public MapCodec<PrismSpireFeature> codec() {
		return CODEC;
	}

	@Override
	public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
		BlockPos base = hanging ? Gen.ceiling(level, origin, 40) : Gen.floor(level, origin, 40);

		if (base == null) {
			return false;
		}

		int dir = hanging ? -1 : 1;
		int space = 0;

		while (space < 40 && Gen.open(level.getBlockState(base.above(space * dir)))) {
			space++;
		}

		if (space < 6) {
			return false;
		}

		int height = Math.min(space - 3, 5 + random.nextInt(hanging ? 9 : 14));
		BlockState crystal = Palette.crystal(palette, random).defaultBlockState();
		float leanX = (random.nextFloat() - 0.5f) * 0.3f;
		float leanZ = (random.nextFloat() - 0.5f) * 0.3f;
		float radius = height > 12 ? 1.6f : 1.1f;

		for (int y = 0; y < height; y++) {
			float r = radius * (1 - y / (float) height) + 0.2f;
			int ri = Mth.ceil(r);
			int cx = Math.round(leanX * y);
			int cz = Math.round(leanZ * y);

			for (int dx = -ri; dx <= ri; dx++) {
				for (int dz = -ri; dz <= ri; dz++) {
					if (dx * dx + dz * dz <= r * r) {
						Gen.place(level, base.offset(dx + cx, y * dir, dz + cz), crystal);
					}
				}
			}
		}

		BlockPos tip = base.offset(Math.round(leanX * height), height * dir, Math.round(leanZ * height));
		Gen.place(level, tip, Palette.cluster(palette, random, hanging ? Direction.DOWN : Direction.UP));

		// tuft at the foot
		for (int i = 0; i < 6; i++) {
			BlockPos p = base.offset(random.nextInt(5) - 2, 0, random.nextInt(5) - 2);

			if (Gen.open(level.getBlockState(p)) && level.getFluidState(p).isEmpty() && Gen.solid(level.getBlockState(p.relative(hanging ? Direction.UP : Direction.DOWN)))) {
				Gen.set(level, p, Palette.cluster(palette, random, hanging ? Direction.DOWN : Direction.UP));
			}
		}

		return true;
	}
}
