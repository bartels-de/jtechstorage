package dev.bartels.jtechstorage.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

import dev.bartels.jtechstorage.ModContent;

/** Team-Terminal: merkt sich nur, mit welchem Netzwerk es verknüpft ist. */
public class TeamTerminalBlockEntity extends LinkedPanelBlockEntity {
	public TeamTerminalBlockEntity(BlockPos pos, BlockState state) {
		super(ModContent.TEAM_TERMINAL_BLOCK_ENTITY, pos, state);
	}
}
