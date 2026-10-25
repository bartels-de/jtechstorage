package dev.bartels.jtechstorage.depths;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import dev.bartels.jtechstorage.ModAdvancements;

/** Hut der Riesen-Resonanzpilze (v0.33/v0.34): federt; wer aus der Höhe darauf springt, bekommt einen Fortschritt. */
public class ResonanceCapBlock extends Block {
	public ResonanceCapBlock(Properties properties) {
		super(properties);
	}

	@Override
	public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, double fallDistance) {
		if (fallDistance > 3.0 && entity instanceof ServerPlayer player && !player.isSuppressingBounce()) {
			ModAdvancements.grant(player, "bouncy");
		}

		super.fallOn(level, state, pos, entity, fallDistance);
	}
}
