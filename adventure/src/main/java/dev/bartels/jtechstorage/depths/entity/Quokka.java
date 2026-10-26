package dev.bartels.jtechstorage.depths.entity;

import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

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
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.monster.zombie.ZombieVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import dev.bartels.jtechstorage.ModSounds;
import dev.bartels.jtechstorage.depths.DepthsContent;
import dev.bartels.jtechstorage.mixin.ZombieVillagerAccessor;
import net.minecraft.server.level.ServerPlayer;
import dev.bartels.jtechstorage.ModAdvancements;

/**
 * Quokka (Crystal Depths, v0.33): small, always-smiling marsupial with amethyst crystals. Tamed with grass it follows
 * its owner, sits on command and hops to nearby zombie villagers – its crystals then start
 * the cure (like Weakness potion + golden apple, only faster). The owner counts as the curer and gets the
 * discount. Afterwards the quokka needs a while before it can cure again.
 */
public class Quokka extends TamableAnimal {
	private static final EntityDataAccessor<Integer> DATA_HOP = SynchedEntityData.defineId(Quokka.class, EntityDataSerializers.INT);
	public static final int HOP_TICKS = 20;
	private static final int CURE_COOLDOWN = 1200;
	private static final int CURE_RANGE = 16;
	private int cureCooldown;

	public Quokka(EntityType<? extends Quokka> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return TamableAnimal.createAnimalAttributes()
				.add(Attributes.MAX_HEALTH, 14.0)
				.add(Attributes.MOVEMENT_SPEED, 0.28)
				.add(Attributes.TEMPT_RANGE, 10.0)
				.add(Attributes.FOLLOW_RANGE, 20.0);
	}

	public static boolean checkSpawn(EntityType<Quokka> type, LevelAccessor level, EntitySpawnReason reason, BlockPos pos, RandomSource random) {
		BlockState below = level.getBlockState(pos.below());
		return below.is(DepthsContent.CRYSTAL_TURF) || below.is(DepthsContent.ROSE_MOSS) || below.is(DepthsContent.RESONANCE_MOSS);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DATA_HOP, 0);
	}

	public int getHopTicks() {
		return entityData.get(DATA_HOP);
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(1, new PanicGoal(this, 1.5) {
			@Override
			public boolean canUse() {
				return !isTame() && super.canUse();
			}
		});
		goalSelector.addGoal(1, new CureGoal());   // before sitting: a quokka that was not told to sit also cures when the owner is offline
		goalSelector.addGoal(2, new SitWhenOrderedToGoal(this));
		goalSelector.addGoal(4, new BreedGoal(this, 1.0));
		goalSelector.addGoal(5, new TemptGoal(this, 1.2, this::isFood, false));
		goalSelector.addGoal(6, new FollowOwnerGoal(this, 1.2, 8.0f, 2.5f));
		goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 1.0));
		goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 6.0f));
		goalSelector.addGoal(9, new RandomLookAroundGoal(this));
	}

	/** Grass tames, cures and lets quokkas breed. */
	@Override
	public boolean isFood(ItemStack stack) {
		return stack.is(Items.SHORT_GRASS) || stack.is(Items.TALL_GRASS) || stack.is(Items.FERN) || stack.is(DepthsContent.CRYSTAL_GRASS.asItem());
	}

	@Override
	public @Nullable AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
		Quokka baby = DepthsContent.QUOKKA.create(level, EntitySpawnReason.BREEDING);

		if (baby != null && getOwnerReference() != null) {
			baby.setOwnerReference(getOwnerReference());
			baby.setTame(true, true);
		}

		return baby;
	}

	@Override
	public InteractionResult mobInteract(Player player, InteractionHand hand) {
		ItemStack held = player.getItemInHand(hand);

		if (!isTame() && isFood(held)) {
			if (!level().isClientSide()) {
				held.consume(1, player);
				playSound(ModSounds.QUOKKA_EAT, 1.0f, 1.0f);

				if (random.nextInt(3) == 0) {
					tame(player);
					setOrderedToSit(true);

					if (player instanceof ServerPlayer server) {
						ModAdvancements.grant(server, "quokka_friend");
					}
					navigation.stop();
					level().broadcastEntityEvent(this, (byte) 7);
				} else {
					level().broadcastEntityEvent(this, (byte) 6);
				}
			}

			return InteractionResult.SUCCESS;
		}

		if (isTame() && isOwnedBy(player)) {
			if (isFood(held) && getHealth() < getMaxHealth()) {
				if (!level().isClientSide()) {
					held.consume(1, player);
					heal(4.0f);
					playSound(ModSounds.QUOKKA_EAT, 1.0f, 1.0f);
				}

				return InteractionResult.SUCCESS;
			}

			InteractionResult result = super.mobInteract(player, hand);

			if (result.consumesAction()) {
				return result;
			}

			if (!level().isClientSide()) {
				setOrderedToSit(!isOrderedToSit());
				jumping = false;
				navigation.stop();
			}

			return InteractionResult.SUCCESS;
		}

		return super.mobInteract(player, hand);
	}

	@Override
	public void aiStep() {
		super.aiStep();

		if (level().isClientSide()) {
			if (isTame() && random.nextInt(60) == 0) {
				level().addParticle(ParticleTypes.END_ROD, getRandomX(0.3), getY() + 0.9, getRandomZ(0.3), 0, 0.01, 0);
			}

			return;
		}

		int hop = getHopTicks();

		if (hop > 0) {
			entityData.set(DATA_HOP, hop - 1);
		}

		if (cureCooldown > 0) {
			cureCooldown--;
		}
	}

	private void cure(ServerLevel level, ZombieVillager target) {
		UUID healer = getOwnerReference() != null ? getOwnerReference().getUUID() : null;
		((ZombieVillagerAccessor) target).jtechstorage$startConverting(healer, 300 + random.nextInt(300));
		entityData.set(DATA_HOP, HOP_TICKS);
		cureCooldown = CURE_COOLDOWN;
		playSound(ModSounds.QUOKKA_CURE, 1.0f, 1.0f);

		if (getOwner() instanceof ServerPlayer owner) {
			ModAdvancements.grant(owner, "quokka_healer");
		}
		level.sendParticles(ParticleTypes.HEART, target.getX(), target.getY() + 2.0, target.getZ(), 5, 0.4, 0.3, 0.4, 0.0);
		level.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 0.8, getZ(), 12, 0.3, 0.3, 0.3, 0.05);
	}

	@Override
	protected @Nullable SoundEvent getAmbientSound() {
		return isInSittingPose() && random.nextInt(3) > 0 ? null : ModSounds.QUOKKA_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return ModSounds.QUOKKA_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return ModSounds.QUOKKA_DEATH;
	}

	@Override
	protected void playStepSound(BlockPos pos, BlockState state) {
		playSound(SoundEvents.RABBIT_JUMP, 0.15f, 1.3f);
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putInt("CureCooldown", cureCooldown);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		cureCooldown = input.getIntOr("CureCooldown", 0);
	}

	/** Hops to the nearest zombie villager and starts curing it. */
	private final class CureGoal extends Goal {
		private @Nullable ZombieVillager target;
		private int timeout;

		CureGoal() {
			setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
		}

		@Override
		public boolean canUse() {
			if (!isTame() || isOrderedToSit() || cureCooldown > 0 || random.nextInt(10) != 0) {
				return false;
			}

			List<ZombieVillager> list = level().getEntitiesOfClass(ZombieVillager.class, getBoundingBox().inflate(CURE_RANGE), z -> z.isAlive() && !z.isConverting());
			target = list.stream().min((a, b) -> Double.compare(distanceToSqr(a), distanceToSqr(b))).orElse(null);
			return target != null;
		}

		@Override
		public boolean canContinueToUse() {
			return target != null && target.isAlive() && !target.isConverting() && timeout > 0 && !isOrderedToSit();
		}

		@Override
		public void start() {
			timeout = 400;
		}

		@Override
		public void stop() {
			target = null;
			navigation.stop();
		}

		@Override
		public void tick() {
			if (target == null) {
				return;
			}

			timeout--;
			getLookControl().setLookAt(target, 30.0f, 30.0f);

			if (distanceToSqr(target) > 6.25) {
				navigation.moveTo(target, 1.3);
			} else if (level() instanceof ServerLevel server) {
				cure(server, target);
				target = null;
			}
		}
	}
}
