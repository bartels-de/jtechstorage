package dev.bartels.jtechstorage.farm;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Inventar des Block-Setzers (v0.38): 9 Slots wie beim Spender, eigener Name. */
public class BlockPlacerBlockEntity extends DispenserBlockEntity {
	public BlockPlacerBlockEntity(BlockPos pos, BlockState state) {
		super(FarmContent.BLOCK_PLACER_BLOCK_ENTITY, pos, state);
	}

	@Override
	protected Component getDefaultName() {
		return Component.translatable("container.jtechstorage.block_placer");
	}
}
