package dev.bartels.jtechstorage.depths.entity;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
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
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

import dev.bartels.jtechstorage.JTechStorage;
import dev.bartels.jtechstorage.depths.DepthsAnimals;
import dev.bartels.jtechstorage.depths.DepthsContent;

/**
 * Crystal Crab (v0.36): lives on the shores of the Crystal Lakes, scuttles sideways and occasionally digs up treasure from
 * sand, gravel or grass. Tamed with fish it follows you, sits on command and digs more often – and then
 * also finds the better stuff.
 */
public class CrystalCrab extends TamableAnimal {
	private static final EntityDataAccessor<Integer> DATA_DIG = SynchedEntityData.defineId(CrystalCrab.class, EntityDataSerializers.INT);
	public static final int DIG_TICKS = 50;
	public static final ResourceKey<LootTable> DIG_LOOT = ResourceKey.create(Registries.LOOT_TABLE, JTechStorage.id("gameplay/crystal_crab_dig"));
	private int digCooldown = 600;

	public CrystalCrab(EntityType<? extends CrystalCrab> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return TamableAnimal.createAnimalAttributes()
				.add(Attributes.MAX_HEALTH, 12.0)
				.add(Attributes.MOVEMENT_SPEED, 0.25)
				.add(Attributes.ARMOR, 4.0)
				.add(Attributes.TEMPT_RANGE, 10.0);
	}

	public static boolean checkSpawn(EntityType<CrystalCrab> type, LevelAccessor level, EntitySpawnReason reason, BlockPos pos, RandomSource random) {
		BlockState below = level.getBlockState(pos.below());
		return diggable(below) || below.is(DepthsContent.CRYSTAL_STONE);
	}

	static boolean diggable(BlockState state) {
		return state.is(Blocks.SAND) || state.is(Blocks.GRAVEL) || state.is(Blocks.DIRT) || state.is(Blocks.CLAY)
				|| state.is(DepthsContent.CITRINE_SAND) || state.is(DepthsContent.CRYSTAL_TURF) || state.is(DepthsContent.RESONANCE_MOSS)
				|| state.is(DepthsContent.ROSE_MOSS);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DATA_DIG, 0);
	}

	public int getDigTicks() {
		return entityData.get(DATA_DIG);
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(1, new PanicGoal(this, 1.4) {
			@Override
			public boolean canUse() {
				return !isTame() && super.canUse();
			}
		});
		goalSelector.addGoal(2, new SitWhenOrderedToGoal(this));
		goalSelector.addGoal(4, new BreedGoal(this, 1.0));
		goalSelector.addGoal(5, new TemptGoal(this, 1.2, this::isFood, false));
		goalSelector.addGoal(6, new FollowOwnerGoal(this, 1.2, 8.0f, 2.5f));
		goalSelector.addGoal(7, new RandomStrollGoal(this, 1.0));
		goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 6.0f));
		goalSelector.addGoal(9, new RandomLookAroundGoal(this));
	}

	@Override
	public boolean canBreatheUnderwater() {
		return true;
	}

	@Override
	public boolean isFood(ItemStack stack) {
		return stack.is(Items.COD) || stack.is(Items.SALMON) || stack.is(Items.TROPICAL_FISH);
	}

	@Override
	public InteractionResult mobInteract(Player player, InteractionHand hand) {
		ItemStack held = player.getItemInHand(hand);

		if (!isTame() && isFood(held)) {
			if (!level().isClientSide()) {
				held.consume(1, player);
				playSound(SoundEvents.TURTLE_EGG_CRACK, 0.6f, 1.6f);

				if (random.nextInt(3) == 0) {
					tame(player);
					setOrderedToSit(true);
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
		int dig = getDigTicks();

		if (level() instanceof ServerLevel server) {
			if (dig > 0) {
				navigation.stop();
				entityData.set(DATA_DIG, dig - 1);
				BlockState below = server.getBlockState(blockPosition().below());

				if (dig % 6 == 0) {
					server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, below), getX(), getY() + 0.1, getZ(), 6, 0.25, 0.05, 0.25, 0.05);
					playSound(below.getSoundType().getHitSound(), 0.5f, 1.2f);
				}

				if (dig == 1) {
					finishDig(server);
				}
			} else if (--digCooldown <= 0 && onGround() && !isOrderedToSit() && diggable(server.getBlockState(blockPosition().below()))) {
				entityData.set(DATA_DIG, DIG_TICKS);
				digCooldown = isTame() ? 1200 + random.nextInt(1200) : 2400 + random.nextInt(2400);
			}
		}
	}

	private void finishDig(ServerLevel level) {
		// wild crabs only find something now and then
		if (!isTame() && random.nextBoolean()) {
			return;
		}

		LootTable table = level.getServer().reloadableRegistries().getLootTable(DIG_LOOT);
		LootParams params = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, position())
				.withParameter(LootContextParams.THIS_ENTITY, this).create(LootContextParamSets.GIFT);

		for (ItemStack stack : table.getRandomItems(params)) {
			spawnAtLocation(level, stack, 0.3f);
		}

		playSound(SoundEvents.AMETHYST_BLOCK_CHIME, 0.8f, 1.5f);
		level.sendParticles(ParticleTypes.HAPPY_VILLAGER, getX(), getY() + 0.4, getZ(), 4, 0.3, 0.2, 0.3, 0.0);
	}

	@Override
	public @Nullable AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
		CrystalCrab baby = DepthsAnimals.CRYSTAL_CRAB.create(level, EntitySpawnReason.BREEDING);

		if (baby != null && getOwnerReference() != null) {
			baby.setOwnerReference(getOwnerReference());
			baby.setTame(true, true);
		}

		return baby;
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putInt("DigCooldown", digCooldown);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		digCooldown = input.getIntOr("DigCooldown", 600);
	}

	@Override
	protected @Nullable SoundEvent getAmbientSound() {
		return random.nextInt(3) == 0 ? SoundEvents.AMETHYST_BLOCK_HIT : null;
	}

	@Override
	public float getVoicePitch() {
		return 1.8f + random.nextFloat() * 0.2f;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.TURTLE_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.TURTLE_DEATH;
	}

	@Override
	protected void playStepSound(BlockPos pos, BlockState state) {
		playSound(SoundEvents.TURTLE_SHAMBLE_BABY, 0.3f, 1.4f);
	}
}
