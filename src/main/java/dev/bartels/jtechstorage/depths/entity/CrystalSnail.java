package dev.bartels.jtechstorage.depths.entity;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
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
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import dev.bartels.jtechstorage.depths.DepthsContent;

/**
 * Kristallschnecke (Kristalltiefe, v0.31): friedliche, sehr langsame Schnecke mit einem Haus aus Amethyst. Mit
 * Amethyst-Scherben gefüttert (lässt sich so auch vermehren), wirft sie nach ein paar Minuten Kristalle ab. Wird sie
 * getroffen, zieht sie sich kurz in ihr Haus zurück.
 */
public class CrystalSnail extends Animal {
	private static final EntityDataAccessor<Integer> DATA_HIDE = SynchedEntityData.defineId(CrystalSnail.class, EntityDataSerializers.INT);
	public static final int HIDE_TICKS = 80;
	private static final int SHED_DELAY = 3600;
	private int shedTicks;

	public CrystalSnail(EntityType<? extends CrystalSnail> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Animal.createAnimalAttributes()
				.add(Attributes.MAX_HEALTH, 10.0)
				.add(Attributes.MOVEMENT_SPEED, 0.08)
				.add(Attributes.ARMOR, 6.0)
				.add(Attributes.TEMPT_RANGE, 8.0);
	}

	public static boolean checkSpawn(EntityType<CrystalSnail> type, LevelAccessor level, EntitySpawnReason reason, BlockPos pos, RandomSource random) {
		BlockState below = level.getBlockState(pos.below());
		return below.is(DepthsContent.CRYSTAL_STONE) || below.is(DepthsContent.RESONANCE_MOSS) || below.is(DepthsContent.POLISHED_CRYSTAL_STONE)
				|| below.is(Blocks.AMETHYST_BLOCK) || below.is(Blocks.CALCITE);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DATA_HIDE, 0);
	}

	public int getHideTicks() {
		return entityData.get(DATA_HIDE);
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(2, new BreedGoal(this, 1.0));
		goalSelector.addGoal(3, new TemptGoal(this, 1.2, this::isFood, false));
		goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 1.0) {
			@Override
			public boolean canUse() {
				return getHideTicks() == 0 && super.canUse();
			}
		});
		goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 6.0f));
	}

	@Override
	public boolean isFood(ItemStack stack) {
		return stack.is(Items.AMETHYST_SHARD);
	}

	@Override
	public @Nullable AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
		return DepthsContent.CRYSTAL_SNAIL.create(level, EntitySpawnReason.BREEDING);
	}

	@Override
	public InteractionResult mobInteract(Player player, InteractionHand hand) {
		boolean food = isFood(player.getItemInHand(hand));
		InteractionResult result = super.mobInteract(player, hand);

		if (food && result.consumesAction() && shedTicks == 0 && !level().isClientSide()) {
			shedTicks = SHED_DELAY;
		}

		return result;
	}

	@Override
	public void aiStep() {
		super.aiStep();

		if (level().isClientSide()) {
			if (random.nextInt(20) == 0) {
				level().addParticle(ParticleTypes.END_ROD, getRandomX(0.4), getY() + 0.7, getRandomZ(0.4), 0, 0.01, 0);
			}

			return;
		}

		int hide = getHideTicks();

		if (hide > 0) {
			entityData.set(DATA_HIDE, hide - 1);
			getNavigation().stop();
		}

		if (shedTicks > 0 && --shedTicks == 0 && level() instanceof ServerLevel server) {
			spawnAtLocation(server, new ItemStack(Items.AMETHYST_SHARD, 2 + random.nextInt(3)));

			if (random.nextInt(5) == 0) {
				spawnAtLocation(server, new ItemStack(DepthsContent.GLOW_CRYSTAL));
			}

			playSound(SoundEvents.AMETHYST_BLOCK_RESONATE, 1.0f, 1.2f);
		}
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		boolean hurt = super.hurtServer(level, source, amount);

		if (hurt) {
			entityData.set(DATA_HIDE, HIDE_TICKS);
		}

		return hurt;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.AMETHYST_BLOCK_HIT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.AMETHYST_CLUSTER_BREAK;
	}

	@Override
	protected void playStepSound(BlockPos pos, BlockState state) {
		playSound(SoundEvents.SLIME_SQUISH_SMALL, 0.1f, 1.4f);
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putInt("Shed", shedTicks);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		shedTicks = input.getIntOr("Shed", 0);
	}
}
