package dev.bartels.jtechstorage.farm;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Amethyst-Trichter (v0.27.0): verhält sich wie ein Trichter (Ausgabe nach unten oder zur Seite, Redstone sperrt ihn,
 * fünf Plätze, Komparator), bewegt aber 8 Items alle 4 Ticks und saugt Items in einem 3×3-Bereich über sich ein.
 */
public class AmethystHopperBlock extends HopperBlock {
	public AmethystHopperBlock(Properties properties) {
		super(properties);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new AmethystHopperBlockEntity(pos, state);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		if (level.isClientSide() || type != FarmContent.AMETHYST_HOPPER_BLOCK_ENTITY) {
			return null;
		}

		return (BlockEntityTicker<T>) (BlockEntityTicker<AmethystHopperBlockEntity>) AmethystHopperBlockEntity::serverTick;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!level.isClientSide() && level.getBlockEntity(pos) instanceof AmethystHopperBlockEntity hopper) {
			player.openMenu(hopper);
		}

		return InteractionResult.SUCCESS;
	}

	@Override
	protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effects, boolean intersects) {
		// das Einsaugen übernimmt der Block selbst (größerer Bereich), siehe AmethystHopperBlockEntity
	}
}
