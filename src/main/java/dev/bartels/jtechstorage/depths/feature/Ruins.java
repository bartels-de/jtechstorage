package dev.bartels.jtechstorage.depths.feature;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootTable;

import dev.bartels.jtechstorage.JTechStorage;
import dev.bartels.jtechstorage.depths.BrokenTerminalBlock;
import dev.bartels.jtechstorage.depths.DepthsContent;

/** Bausteine für die Bauwerke der Lagermeister (v0.32). */
final class Ruins {
	static final ResourceKey<LootTable> RUIN_LOOT = ResourceKey.create(Registries.LOOT_TABLE, JTechStorage.id("chests/storage_ruin"));
	static final ResourceKey<LootTable> SHRINE_LOOT = ResourceKey.create(Registries.LOOT_TABLE, JTechStorage.id("chests/crystal_shrine"));

	private Ruins() {
	}

	/** Liegt der Boden im Umkreis ungefähr auf einer Höhe? Liefert die Bodenhöhe oder null. */
	static BlockPos flatGround(WorldGenLevel level, BlockPos origin, int half, int tolerance) {
		BlockPos base = Gen.floor(level, origin, 40);

		if (base == null) {
			return null;
		}

		for (int[] c : new int[][] {{-half, -half}, {half, -half}, {-half, half}, {half, half}}) {
			BlockPos corner = Gen.floor(level, base.offset(c[0], tolerance, c[1]), tolerance * 2 + 2);

			if (corner == null || Math.abs(corner.getY() - base.getY()) > tolerance) {
				return null;
			}
		}

		return base;
	}

	/** Fundament von {@code y - 1} abwärts bis zum Boden auffüllen. */
	static void foundation(WorldGenLevel level, BlockPos top, BlockState state) {
		for (int i = 0; i < 6; i++) {
			BlockPos p = top.below(i);

			if (Gen.solid(level.getBlockState(p)) && i > 0) {
				break;
			}

			Gen.set(level, p, state);
		}
	}

	static BlockState brick(RandomSource random) {
		int r = random.nextInt(10);
		return r < 3 ? DepthsContent.CRACKED_CRYSTAL_STONE_BRICKS.defaultBlockState()
				: r == 3 ? DepthsContent.CRYSTAL_STONE.defaultBlockState() : DepthsContent.CRYSTAL_STONE_BRICKS.defaultBlockState();
	}

	static void chest(WorldGenLevel level, RandomSource random, BlockPos pos, Direction facing, ResourceKey<LootTable> loot) {
		Gen.set(level, pos, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, facing));
		RandomizableContainer.setBlockEntityLootTable(level, random, pos, loot);
	}

	static void terminal(WorldGenLevel level, BlockPos pos, Direction facing) {
		Gen.set(level, pos, DepthsContent.BROKEN_TERMINAL.defaultBlockState().setValue(BrokenTerminalBlock.FACING, facing));
	}

	static void lantern(WorldGenLevel level, BlockPos pos, boolean hanging) {
		Gen.set(level, pos, DepthsContent.CRYSTAL_LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, hanging));
	}
}
