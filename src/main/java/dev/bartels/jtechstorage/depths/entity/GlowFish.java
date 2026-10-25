package dev.bartels.jtechstorage.depths.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.fish.AbstractSchoolingFish;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;

/** Leuchtfisch (Kristalltiefe, v0.31): kleine leuchtende Schwarmfische der Kristallseen. */
public class GlowFish extends AbstractSchoolingFish {
	public GlowFish(EntityType<? extends GlowFish> type, Level level) {
		super(type, level);
	}

	public static boolean checkSpawn(EntityType<GlowFish> type, LevelAccessor level, EntitySpawnReason reason, BlockPos pos, RandomSource random) {
		return level.getFluidState(pos).is(FluidTags.WATER) && level.getFluidState(pos.below()).is(FluidTags.WATER);
	}

	@Override
	protected InteractionResult mobInteract(Player player, InteractionHand hand) {
		if (player.getItemInHand(hand).is(Items.WATER_BUCKET)) {
			return InteractionResult.PASS;   // lässt sich nicht einfangen
		}

		return super.mobInteract(player, hand);
	}

	@Override
	public ItemStack getBucketItemStack() {
		return new ItemStack(Items.WATER_BUCKET);
	}

	@Override
	public int getMaxSchoolSize() {
		return 8;
	}

	@Override
	protected SoundEvent getFlopSound() {
		return SoundEvents.COD_FLOP;
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.COD_AMBIENT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.COD_DEATH;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.COD_HURT;
	}
}
