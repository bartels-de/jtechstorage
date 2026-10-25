package dev.bartels.jtechstorage.farm;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.DirectionalPlaceContext;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Block-Setzer (v0.38): sieht aus und verhält sich wie ein Spender (9 Slots, Redstone-Puls, Ausrichtung in alle sechs
 * Richtungen), setzt aber den nächsten Block aus seinem Inventar direkt vor sich in die Welt – auch Setzlinge, Saat,
 * Pilze, Zuckerrohr und Co., sofern sie dort stehen können. Ist der Platz belegt oder passt nichts, klickt er nur.
 */
public class BlockPlacerBlock extends DispenserBlock {
	public BlockPlacerBlock(Properties properties) {
		super(properties);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new BlockPlacerBlockEntity(pos, state);
	}

	@Override
	protected void dispenseFrom(ServerLevel level, BlockState state, BlockPos pos) {
		if (!(level.getBlockEntity(pos) instanceof BlockPlacerBlockEntity placer)) {
			return;
		}

		Direction facing = state.getValue(FACING);
		BlockPos target = pos.relative(facing);

		if (level.getBlockState(target).canBeReplaced()) {
			for (int slot = 0; slot < placer.getContainerSize(); slot++) {
				ItemStack stack = placer.getItem(slot);

				if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem blockItem)) {
					continue;
				}

				InteractionResult result = blockItem.place(new DirectionalPlaceContext(level, target, facing, stack, facing.getOpposite()));

				if (result.consumesAction()) {
					placer.setChanged();
					level.levelEvent(1000, pos, 0);   // Klick wie beim Spender
					return;
				}
			}
		}

		level.levelEvent(1001, pos, 0);   // nichts zu setzen
	}
}
