package dev.bartels.jtechstorage.depths.feature;

import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import dev.bartels.jtechstorage.depths.DepthsContent;

/** Farbpaletten der Kristall-Gebilde (v0.33): prism (bunt gemischt), azure, rose, citrine, amethyst. */
final class Palette {
	private Palette() {
	}

	static Block crystal(String palette, RandomSource random) {
		return switch (pick(palette, random)) {
			case "azure" -> DepthsContent.AZURE_CRYSTAL;
			case "rose" -> DepthsContent.ROSE_CRYSTAL;
			case "citrine" -> DepthsContent.CITRINE_CRYSTAL;
			default -> Blocks.AMETHYST_BLOCK;
		};
	}

	static BlockState cluster(String palette, RandomSource random, Direction facing) {
		Block block = switch (pick(palette, random)) {
			case "azure" -> DepthsContent.AZURE_CLUSTER;
			case "rose" -> DepthsContent.ROSE_CLUSTER;
			case "citrine" -> DepthsContent.CITRINE_CLUSTER;
			default -> null;
		};
		return block == null ? Gen.cluster(random, facing) : block.defaultBlockState().setValue(AmethystClusterBlock.FACING, facing);
	}

	/** Für „prism“ eine der drei Farben, sonst die Palette selbst. */
	static String pick(String palette, RandomSource random) {
		if (palette.equals("prism")) {
			return switch (random.nextInt(3)) {
				case 0 -> "azure";
				case 1 -> "rose";
				default -> "citrine";
			};
		}

		return palette;
	}
}
