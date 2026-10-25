package dev.bartels.jtechstorage.depths.feature;

import java.util.HashMap;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

import dev.bartels.jtechstorage.JTechStorage;
import dev.bartels.jtechstorage.depths.DepthsContent;

/**
 * Weiche Biom-Übergänge (v0.37): läuft einmal pro Chunk als letzter Schritt. Für jede Säule sucht er das nächste andere
 * Biom im Umkreis von {@link #RANGE} Blöcken. Je näher die Grenze, desto öfter bekommt die Säule Boden, Decke und
 * Pflanzen des Nachbarbioms – so verlaufen Moos, Rasen, Sand und Frost über 16–24 Blöcke ineinander statt an einer Kante
 * zu wechseln.
 */
public record BiomeBlendFeature() implements Feature {
	public static final MapCodec<BiomeBlendFeature> CODEC = MapCodec.unit(new BiomeBlendFeature());
	private static final int RANGE = 24;
	private static final int Q = RANGE / 4 + 1;          // Rand des Biom-Rasters in Vierteln (4 Blöcke)

	/** Boden-, Decken- und Pflanzenblock je Biom (null = keiner). */
	private record Look(@Nullable Block floor, @Nullable Block ceiling, @Nullable Block plant) {
	}

	private static @Nullable Map<String, Look> looks;

	private static Map<String, Look> looks() {
		if (looks == null) {
			Map<String, Look> m = new HashMap<>();
			m.put("geode_halls", new Look(DepthsContent.POLISHED_CRYSTAL_STONE, null, null));
			m.put("crystal_lakes", new Look(DepthsContent.RESONANCE_MOSS, null, DepthsContent.CRYSTAL_SPROUT));
			m.put("vein_chasm", new Look(DepthsContent.VEIN_STONE, DepthsContent.VEIN_STONE, null));
			m.put("resonance_grotto", new Look(DepthsContent.RESONANCE_MOSS, DepthsContent.RESONANCE_MOSS, DepthsContent.RESONANCE_SHROOM));
			m.put("prism_forest", new Look(DepthsContent.CRYSTAL_TURF, null, DepthsContent.CRYSTAL_GRASS));
			m.put("rose_gardens", new Look(DepthsContent.ROSE_MOSS, DepthsContent.ROSE_MOSS, DepthsContent.ROSE_BLOOM));
			m.put("azure_frost", new Look(DepthsContent.FROST_CRYSTAL, DepthsContent.FROST_CRYSTAL, null));
			m.put("citrine_dunes", new Look(DepthsContent.CITRINE_SANDSTONE, DepthsContent.CITRINE_SANDSTONE, null));
			m.put("hollow_expanse", new Look(DepthsContent.CRYSTAL_TURF, null, DepthsContent.CRYSTAL_GRASS));
			m.put("crystal_plains", new Look(DepthsContent.CRYSTAL_TURF, null, DepthsContent.CRYSTAL_GRASS));
			looks = m;
		}

		return looks;
	}

	@Override
	public MapCodec<BiomeBlendFeature> codec() {
		return CODEC;
	}

	private static String key(Holder<Biome> biome) {
		return biome.unwrapKey().map(k -> k.identifier()).filter(id -> id.getNamespace().equals(JTechStorage.MOD_ID))
				.map(Identifier::getPath).orElse("");
	}

	@Override
	public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
		int x0 = (origin.getX() >> 4) << 4;
		int z0 = (origin.getZ() >> 4) << 4;
		BiomeResolver resolver = generator.getBiomeSource().createUncachedResolver(level.getLevel().getChunkSource().randomState());

		// Biom-Raster in Vierteln rund um den Chunk (die Biome hängen in der Tiefe nicht von der Höhe ab)
		int n = 4 + 2 * Q;
		String[][] grid = new String[n][n];

		for (int i = 0; i < n; i++) {
			for (int k = 0; k < n; k++) {
				grid[i][k] = key(resolver.getNoiseBiome((x0 >> 2) - Q + i, 24, (z0 >> 2) - Q + k));
			}
		}

		Map<String, Look> looks = looks();
		BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
		boolean changed = false;

		for (int dx = 0; dx < 16; dx++) {
			for (int dz = 0; dz < 16; dz++) {
				int qi = Q + (dx >> 2);
				int qk = Q + (dz >> 2);
				String own = grid[qi][qk];
				String other = null;
				int best = Integer.MAX_VALUE;

				for (int i = 0; i < n; i++) {
					for (int k = 0; k < n; k++) {
						String b = grid[i][k];

						if (b.equals(own) || !looks.containsKey(b)) {
							continue;
						}

						int ddx = (i - Q) * 4 + 2 - dx;
						int ddz = (k - Q) * 4 + 2 - dz;
						int d2 = ddx * ddx + ddz * ddz;

						if (d2 < best) {
							best = d2;
							other = b;
						}
					}
				}

				if (other == null) {
					continue;
				}

				double d = Math.sqrt(best);

				if (d > RANGE) {
					continue;
				}

				// je näher an der Grenze, desto wahrscheinlicher (an der Grenze ~55 %, bei RANGE 0 %)
				double chance = 0.55 * (1.0 - d / RANGE);
				double roll = roll(level.getSeed(), x0 + dx, z0 + dz);

				if (roll < chance) {
					changed |= blendColumn(level, p, x0 + dx, z0 + dz, looks.get(other), random);
				}
			}
		}

		return changed;
	}

	/** Gleichmäßig verteilter Zufallswert 0..1 je Säule (gut gemischt, damit keine Streifen entstehen). */
	private static double roll(long seed, int x, int z) {
		long h = seed ^ (x * 0x9E3779B97F4A7C15L) ^ (z * 0xC2B2AE3D27D4EB4FL);
		h ^= h >>> 33;
		h *= 0xFF51AFD7ED558CCDL;
		h ^= h >>> 33;
		h *= 0xC4CEB9FE1A85EC53L;
		h ^= h >>> 33;
		return (h >>> 11) * 0x1.0p-53;
	}

	/** Setzt in einer Säule Boden, Decke und (manchmal) Pflanzen des Nachbarbioms. */
	private static boolean blendColumn(WorldGenLevel level, BlockPos.MutableBlockPos p, int x, int z, Look look, RandomSource random) {
		boolean changed = false;
		int top = level.getMaxY() - 2;
		int bottom = level.getMinY() + 2;

		for (int y = top; y > bottom; y--) {
			p.set(x, y, z);
			BlockState state = level.getBlockState(p);

			if (!replaceable(state)) {
				continue;
			}

			BlockState above = level.getBlockState(p.above());
			BlockState below = level.getBlockState(p.below());

			// Boden: fester Untergrund-Block mit Luft oder Pflanze darüber
			if (look.floor() != null && (above.isAir() || plant(above)) && Gen.solid(below)) {
				level.setBlock(p, look.floor().defaultBlockState(), 2);
				changed = true;

				if (plant(above)) {
					level.setBlock(p.above(), Blocks.AIR.defaultBlockState(), 2);
				}

				if (look.plant() != null && above.isAir() && random.nextInt(4) == 0 && level.getBlockState(p.above(2)).isAir()) {
					level.setBlock(p.above(), look.plant().defaultBlockState(), 2);
				}
			} else if (look.ceiling() != null && below.isAir() && Gen.solid(above)) {
				level.setBlock(p, look.ceiling().defaultBlockState(), 2);
				changed = true;
			}
		}

		return changed;
	}

	/** Oberflächenblöcke, die das Übergangsfeld austauschen darf (Gestein und die Bodenblöcke der Biome). */
	private static boolean replaceable(BlockState state) {
		return state.is(DepthsContent.CRYSTAL_STONE) || state.is(DepthsContent.POLISHED_CRYSTAL_STONE) || state.is(DepthsContent.RESONANCE_MOSS)
				|| state.is(DepthsContent.VEIN_STONE) || state.is(DepthsContent.CRYSTAL_TURF) || state.is(DepthsContent.ROSE_MOSS)
				|| state.is(DepthsContent.FROST_CRYSTAL) || state.is(DepthsContent.CITRINE_SANDSTONE) || state.is(DepthsContent.CITRINE_SAND);
	}

	private static boolean plant(BlockState state) {
		return state.is(DepthsContent.CRYSTAL_GRASS) || state.is(DepthsContent.CRYSTAL_SPROUT) || state.is(DepthsContent.RESONANCE_SHROOM)
				|| state.is(DepthsContent.ROSE_BLOOM);
	}
}
