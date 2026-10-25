package dev.bartels.jtechstorage.depths.entity;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Shearable;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowParentGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import dev.bartels.jtechstorage.depths.DepthsAnimals;
import dev.bartels.jtechstorage.depths.DepthsContent;

/**
 * Geodenschaf (v0.36): Schaf mit Amethystfaser-Wolle und kleinen Kristallhörnern. Scheren gibt Kristallwolle in der
 * Farbe des Schafs; färben wie ein Vanilla-Schaf. Die Wolle wächst nach, wenn es auf Kristallrasen oder Moos grast.
 */
public class GeodeSheep extends Animal implements Shearable {
	private static final EntityDataAccessor<Byte> DATA_WOOL = SynchedEntityData.defineId(GeodeSheep.class, EntityDataSerializers.BYTE);
	public static final int GRAZE_TICKS = 40;
	/** Bit 4 = geschoren, Bits 0–3 = Farbe. */
	private static final int SHEARED_BIT = 16;
	private int grazeTicks;

	public GeodeSheep(EntityType<? extends GeodeSheep> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Animal.createAnimalAttributes().add(Attributes.MAX_HEALTH, 10.0).add(Attributes.MOVEMENT_SPEED, 0.23).add(Attributes.TEMPT_RANGE, 10.0);
	}

	public static boolean checkSpawn(EntityType<GeodeSheep> type, LevelAccessor level, EntitySpawnReason reason, BlockPos pos, RandomSource random) {
		BlockState below = level.getBlockState(pos.below());
		return below.is(DepthsContent.CRYSTAL_TURF) || below.is(DepthsContent.ROSE_MOSS) || below.is(DepthsContent.RESONANCE_MOSS);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DATA_WOOL, (byte) DyeColor.PURPLE.getId());
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(1, new PanicGoal(this, 1.25));
		goalSelector.addGoal(2, new BreedGoal(this, 1.0));
		goalSelector.addGoal(3, new TemptGoal(this, 1.1, this::isFood, false));
		goalSelector.addGoal(4, new FollowParentGoal(this, 1.1));
		goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 1.0));
		goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 6.0f));
		goalSelector.addGoal(8, new RandomLookAroundGoal(this));
	}

	public DyeColor getColor() {
		return DyeColor.byId(entityData.get(DATA_WOOL) & 15);
	}

	public void setColor(DyeColor color) {
		byte b = entityData.get(DATA_WOOL);
		entityData.set(DATA_WOOL, (byte) ((b & SHEARED_BIT) | (color.getId() & 15)));
	}

	public boolean isSheared() {
		return (entityData.get(DATA_WOOL) & SHEARED_BIT) != 0;
	}

	public void setSheared(boolean sheared) {
		byte b = entityData.get(DATA_WOOL);
		entityData.set(DATA_WOOL, (byte) (sheared ? b | SHEARED_BIT : b & ~SHEARED_BIT));
	}

	public float getGrazeAmount(float partialTick) {
		if (grazeTicks <= 0) {
			return 0.0f;
		}

		float t = (grazeTicks - partialTick) / GRAZE_TICKS;
		return Math.min(1.0f, Math.min(t, 1.0f - t) * 4.0f);
	}

	@Override
	public boolean isFood(ItemStack stack) {
		return stack.is(Items.GLOW_BERRIES) || stack.is(Items.WHEAT) || stack.is(DepthsContent.CRYSTAL_GRASS.asItem());
	}

	@Override
	public InteractionResult mobInteract(Player player, InteractionHand hand) {
		ItemStack held = player.getItemInHand(hand);

		if (held.is(Items.SHEARS)) {
			if (level() instanceof ServerLevel server && readyForShearing()) {
				shear(server, SoundSource.PLAYERS, held);
				gameEvent(GameEvent.SHEAR, player);
				held.hurtAndBreak(1, player, hand.asEquipmentSlot());
				return InteractionResult.SUCCESS_SERVER;
			}

			return readyForShearing() ? InteractionResult.CONSUME : InteractionResult.PASS;
		}

		if (held.getItem() instanceof DyeItem dye && !isSheared()) {
			DyeColor color = held.get(net.minecraft.core.component.DataComponents.DYE);

			if (color != null && color != getColor()) {
				if (!level().isClientSide()) {
					setColor(color);
					held.consume(1, player);
					playSound(SoundEvents.DYE_USE, 1.0f, 1.0f);
				}

				return InteractionResult.SUCCESS;
			}
		}

		return super.mobInteract(player, hand);
	}

	@Override
	public void shear(ServerLevel level, SoundSource source, ItemStack tool) {
		level.playSound(null, this, SoundEvents.SHEEP_SHEAR, source, 1.0f, 1.2f);
		level.playSound(null, this, SoundEvents.AMETHYST_BLOCK_CHIME, source, 0.8f, 1.4f);
		setSheared(true);
		int count = 1 + random.nextInt(3);

		for (int i = 0; i < count; i++) {
			var item = spawnAtLocation(level, new ItemStack(DepthsAnimals.crystalWool(getColor())), 1.0f);

			if (item != null) {
				item.setDeltaMovement(item.getDeltaMovement().add((random.nextFloat() - random.nextFloat()) * 0.1f, random.nextFloat() * 0.05f,
						(random.nextFloat() - random.nextFloat()) * 0.1f));
			}
		}
	}

	@Override
	public boolean readyForShearing() {
		return isAlive() && !isSheared() && !isBaby();
	}

	@Override
	public void aiStep() {
		super.aiStep();

		if (grazeTicks > 0) {
			grazeTicks--;
		}

		if (level().isClientSide()) {
			return;
		}

		// grasen: auf Kristallrasen/Moos wächst die Wolle nach
		if (isSheared() && onGround() && random.nextInt(isBaby() ? 300 : 900) == 0) {
			BlockState below = level().getBlockState(blockPosition().below());

			if (below.is(DepthsContent.CRYSTAL_TURF) || below.is(DepthsContent.RESONANCE_MOSS) || below.is(DepthsContent.ROSE_MOSS)
					|| below.is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK)) {
				setSheared(false);
				grazeTicks = GRAZE_TICKS;
				level().broadcastEntityEvent(this, (byte) 10);
				gameEvent(GameEvent.EAT);
				if (isBaby()) {
					ageUp(60);
				}
			}
		}
	}

	@Override
	public void handleEntityEvent(byte id) {
		if (id == 10) {
			grazeTicks = GRAZE_TICKS;
		} else {
			super.handleEntityEvent(id);
		}
	}

	@Override
	public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason,
			@Nullable SpawnGroupData data) {
		int r = level.getRandom().nextInt(100);
		setColor(r < 60 ? DyeColor.PURPLE : r < 75 ? DyeColor.MAGENTA : r < 90 ? DyeColor.LIGHT_BLUE : DyeColor.PINK);
		return super.finalizeSpawn(level, difficulty, reason, data);
	}

	@Override
	public @Nullable AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
		GeodeSheep baby = DepthsAnimals.GEODE_SHEEP.create(level, EntitySpawnReason.BREEDING);

		if (baby != null) {
			DyeColor color = random.nextBoolean() || !(partner instanceof GeodeSheep other) ? getColor() : other.getColor();
			baby.setColor(color);
		}

		return baby;
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putBoolean("Sheared", isSheared());
		output.putInt("Color", getColor().getId());
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		setColor(DyeColor.byId(input.getIntOr("Color", DyeColor.PURPLE.getId())));
		setSheared(input.getBooleanOr("Sheared", false));
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.SHEEP_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.SHEEP_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.SHEEP_DEATH;
	}

	@Override
	public float getVoicePitch() {
		return super.getVoicePitch() * 1.15f;
	}

	@Override
	protected void playStepSound(BlockPos pos, BlockState state) {
		playSound(SoundEvents.SHEEP_STEP, 0.15f, 1.0f);
	}
}
