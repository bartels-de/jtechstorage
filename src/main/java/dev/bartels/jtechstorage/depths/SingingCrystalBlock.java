package dev.bartels.jtechstorage.depths;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AmethystBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Singender Farbkristall (v0.33): klingt beim Betreten und bei Treffern – jede Farbe in ihrer eigenen Tonhöhe
 * (Azur hoch, Rosen mittel, Citrin tief). So lässt sich im Prismenwald eine Melodie laufen.
 */
public class SingingCrystalBlock extends AmethystBlock {
	private final float pitch;

	public SingingCrystalBlock(float pitch, Properties properties) {
		super(properties);
		this.pitch = pitch;
	}

	@Override
	public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
		if (!level.isClientSide() && !entity.isSteppingCarefully() && level.getRandom().nextInt(6) == 0) {
			level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.7f, pitch * (0.95f + level.getRandom().nextFloat() * 0.1f));
		}

		super.stepOn(level, pos, state, entity);
	}

	@Override
	protected void onProjectileHit(Level level, BlockState state, BlockHitResult hit, Projectile projectile) {
		if (!level.isClientSide()) {
			BlockPos pos = hit.getBlockPos();
			level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_HIT, SoundSource.BLOCKS, 1.0f, pitch);
			level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.0f, pitch);
		}
	}
}
