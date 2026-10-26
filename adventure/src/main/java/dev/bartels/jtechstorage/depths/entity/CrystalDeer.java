package dev.bartels.jtechstorage.depths.entity;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;

import dev.bartels.jtechstorage.depths.DepthsAnimals;
import dev.bartels.jtechstorage.depths.DepthsContent;

/**
 * Crystal Deer (v0.36): large mount with glowing crystal antlers from the Prism Forest. Tamed with Glow Berries
 * (or the classic way, by mounting repeatedly), carries a saddle and jumps very high. The crystal hooves sound like
 * amethyst. Riding, jumping and saddle come from the vanilla horse logic.
 */
public class CrystalDeer extends AbstractHorse {
	public CrystalDeer(EntityType<? extends CrystalDeer> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return createBaseHorseAttributes()
				.add(Attributes.MAX_HEALTH, 30.0)
				.add(Attributes.MOVEMENT_SPEED, 0.27)
				.add(Attributes.JUMP_STRENGTH, 1.05)
				.add(Attributes.STEP_HEIGHT, 1.5)
				.add(Attributes.SAFE_FALL_DISTANCE, 7.0);
	}

	public static boolean checkSpawn(EntityType<CrystalDeer> type, LevelAccessor level, EntitySpawnReason reason, BlockPos pos, RandomSource random) {
		BlockState below = level.getBlockState(pos.below());
		return below.is(DepthsContent.CRYSTAL_TURF) || below.is(DepthsContent.ROSE_MOSS) || below.is(DepthsContent.CRYSTAL_STONE);
	}

	/** Fixed stats instead of random horse stats: every deer jumps equally high. */
	@Override
	protected void randomizeAttributes(RandomSource random) {
	}

	@Override
	public boolean isFood(ItemStack stack) {
		return stack.is(Items.GLOW_BERRIES);
	}

	@Override
	public InteractionResult mobInteract(Player player, InteractionHand hand) {
		ItemStack held = player.getItemInHand(hand);

		// tame with Glow Berries (every third berry on average) instead of only by mounting
		if (!isTamed() && !isBaby() && held.is(Items.GLOW_BERRIES) && !isVehicle()) {
			if (!level().isClientSide()) {
				held.consume(1, player);
				playSound(SoundEvents.HORSE_EAT, 1.0f, 1.25f);

				if (random.nextInt(3) == 0) {
					tameWithName(player);
					level().broadcastEntityEvent(this, (byte) 7);
				} else {
					level().broadcastEntityEvent(this, (byte) 6);
				}
			}

			return InteractionResult.SUCCESS;
		}

		return super.mobInteract(player, hand);
	}

	@Override
	protected boolean handleEating(Player player, ItemStack stack) {
		if (!stack.is(Items.GLOW_BERRIES)) {
			return super.handleEating(player, stack);
		}

		boolean used = false;

		if (getHealth() < getMaxHealth()) {
			heal(4.0f);
			used = true;
		}

		if (isBaby()) {
			ageUp(30);
			used = true;
		} else if (isTamed() && getAge() == 0 && canFallInLove()) {
			setInLove(player);
			used = true;
		}

		if (used) {
			eating();
		}

		return used;
	}

	private void eating() {
		if (!isSilent()) {
			playSound(SoundEvents.HORSE_EAT, 1.0f, 1.25f);
		}
	}

	@Override
	public @Nullable AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
		CrystalDeer baby = DepthsAnimals.CRYSTAL_DEER.create(level, EntitySpawnReason.BREEDING);

		if (baby != null && partner instanceof AbstractHorse other) {
			setOffspringAttributes(other, baby);
		}

		return baby;
	}

	@Override
	public void aiStep() {
		super.aiStep();

		// antlers sparkle now and then
		if (level().isClientSide() && random.nextInt(40) == 0) {
			level().addParticle(ParticleTypes.END_ROD, getRandomX(0.6), getY() + getBbHeight() + 0.2, getRandomZ(0.6), 0, 0.01, 0);
		}
	}

	// ------------------------------------------------------------------ Sounds: horse, slightly higher; crystal hooves

	@Override
	public float getVoicePitch() {
		return super.getVoicePitch() * 1.25f;
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.HORSE_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.HORSE_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.HORSE_DEATH;
	}

	@Override
	protected @Nullable SoundEvent getEatingSound() {
		return SoundEvents.HORSE_EAT;
	}

	@Override
	protected SoundEvent getAngrySound() {
		return SoundEvents.HORSE_ANGRY;
	}

	@Override
	protected void playStepSound(BlockPos pos, BlockState state) {
		playSound(SoundEvents.AMETHYST_BLOCK_STEP, 0.35f, 0.9f + random.nextFloat() * 0.3f);
	}

	@Override
	protected void playJumpSound() {
		playSound(SoundEvents.AMETHYST_BLOCK_CHIME, 0.6f, 1.2f);
	}
}
