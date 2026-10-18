package dev.bartels.jtechstorage.entity;

import java.util.EnumSet;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Prediction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

import dev.bartels.jtechstorage.ModSounds;
import dev.bartels.jtechstorage.ModContent;

/**
 * Ashley – ein erfundenes, ziemlich verdrecktes Asche-Wesen (ein wandelnder Rußball mit Glutfunken im Fell).
 * Sie zieht eine Aschespur hinter sich her, niest Staubwolken und hat sich noch nie gewaschen.
 * Peter Chel Parker heilt sich, indem er sie tritt – dabei fliegen heilende Glutfunken aus ihrem Fell.
 * Sie ist unverwundbar (und ziemlich beleidigt).
 *
 * <ul>
 *     <li>Rechtsklick (leere Hand): Status + Spruch · Shift + Rechtsklick: einpacken</li>
 * </ul>
 */
public class Ashley extends NetworkHelperMob {
	public static final int STAY_DISTANCE = 8;
	private static final int IDLE_LINES = 8;
	private static final int KICK_LINES = 8;

	private @Nullable BlockPos home;
	private int kicks;
	private int sayCooldown = 900;

	public Ashley(EntityType<? extends Ashley> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return createMobAttributes().add(Attributes.MAX_HEALTH, 20.0).add(Attributes.MOVEMENT_SPEED, 0.18);
	}

	@Override
	protected String unlinkedStatus() {
		return "jtechstorage.ashley.status.idle";
	}

	@Override
	protected String linkAdvancement() {
		return "ashley";
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(1, new StayNearHomeGoal());
		goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.6));
		goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 6.0f));
		goalSelector.addGoal(7, new RandomLookAroundGoal(this));
	}

	/** Peter tritt zu – Glutfunken fliegen, Ashley fliegt ein Stück weg und beschwert sich. */
	public void onKicked(PeterChelParker peter) {
		kicks++;
		setStatus("jtechstorage.ashley.status.kicked", kicks);
		Vec3 away = position().subtract(peter.position()).normalize().scale(0.45);
		push(away.x, 0.3, away.z);
		playSound(ModSounds.ASHLEY_KICKED, 1.0f, 0.95f + random.nextFloat() * 0.1f);
		playSound(SoundEvents.PANDA_SNEEZE, 0.6f, 1.3f);
		playAction();

		if (level() instanceof ServerLevel level) {
			level.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 0.5, getZ(), 12, 0.3, 0.3, 0.3, 0.02);
			level.sendParticles(ParticleTypes.ASH, getX(), getY() + 0.6, getZ(), 30, 0.4, 0.4, 0.4, 0.05);
			level.sendParticles(ParticleTypes.FLAME, getX(), getY() + 0.6, getZ(), 6, 0.3, 0.3, 0.3, 0.02);

			if (random.nextInt(2) == 0) {
				sayToNearby(level, Component.translatable("jtechstorage.ashley.kicked." + random.nextInt(KICK_LINES)));
			}
		}
	}

	@Override
	public void aiStep() {
		super.aiStep();

		if (!(level() instanceof ServerLevel level)) {
			return;
		}

		if (home == null) {
			home = blockPosition();
		}

		// Aschespur
		if (tickCount % 8 == 0) {
			level.sendParticles(ParticleTypes.ASH, getX(), getY() + 0.1, getZ(), 3, 0.25, 0.05, 0.25, 0.0);
		}

		if (isResting()) {
			return;
		}

		// Staubwolken-Nieser
		if (random.nextInt(900) == 0) {
			level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, getX(), getY() + 0.6, getZ(), 4, 0.2, 0.1, 0.2, 0.02);
			level.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 0.6, getZ(), 10, 0.3, 0.2, 0.3, 0.05);
			playSound(ModSounds.ASHLEY_SNEEZE, 1.0f, 0.95f + random.nextFloat() * 0.1f);
			playAction();
		}

		if (--sayCooldown <= 0) {
			sayCooldown = 1500 + random.nextInt(2000);
			sayToNearby(level, Component.translatable("jtechstorage.ashley.say." + random.nextInt(IDLE_LINES)));
		}
	}

	private void sayToNearby(ServerLevel level, Component text) {
		for (ServerPlayer player : level.players()) {
			if (player.distanceToSqr(this) < 12 * 12) {
				player.sendOverlayMessage(Component.translatable("jtechstorage.chel.says", getDisplayName(), text));
			}
		}
	}

	@Override
	protected InteractionResult interactWith(Player player, InteractionHand hand) {
		if (player.getItemInHand(hand).isEmpty() && player.isSecondaryUseActive()) {
			player.getInventory().placeItemBackInInventory(createPackedItem(ModContent.ASHLEY_ITEM, player), Prediction.SERVER_ONLY);
			playSound(ModSounds.ASHLEY_SNEEZE, 1.0f, 1.1f);
			discard();
			return InteractionResult.SUCCESS;
		}

		if (player.getItemInHand(hand).isEmpty()) {
			player.sendOverlayMessage(statusText());
			player.sendSystemMessage(Component.translatable("jtechstorage.chel.joke_format", getDisplayName(),
					Component.translatable("jtechstorage.ashley.say." + random.nextInt(IDLE_LINES))));
			return InteractionResult.SUCCESS;
		}

		return InteractionResult.PASS;
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putInt("Kicks", kicks);

		if (home != null) {
			output.store("Home", BlockPos.CODEC, home);
		}
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		kicks = input.getIntOr("Kicks", 0);
		home = input.read("Home", BlockPos.CODEC).orElse(null);
	}

	/** Bleibt in der Nähe ihres Platzes (damit Peter sie findet). */
	private class StayNearHomeGoal extends Goal {
		StayNearHomeGoal() {
			setFlags(EnumSet.of(Goal.Flag.MOVE));
		}

		@Override
		public boolean canUse() {
			return home != null && distanceToSqr(Vec3.atCenterOf(home)) > STAY_DISTANCE * STAY_DISTANCE;
		}

		@Override
		public boolean canContinueToUse() {
			return home != null && distanceToSqr(Vec3.atCenterOf(home)) > 3 * 3 && !getNavigation().isDone();
		}

		@Override
		public void start() {
			BlockPos pos = home;

			if (pos != null) {
				getNavigation().moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 1.0);
			}
		}
	}

	@Override
	protected net.minecraft.sounds.SoundEvent helperSleepSound() {
		return dev.bartels.jtechstorage.ModSounds.ASHLEY_SLEEP;
	}

	@Override
	protected void playStepSound(net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
		playSound(dev.bartels.jtechstorage.ModSounds.ASHLEY_STEP, 0.35f, 0.9f + random.nextFloat() * 0.2f);
	}

	@Override
	protected net.minecraft.sounds.SoundEvent helperAmbientSound() {
		return ModSounds.ASHLEY_AMBIENT;
	}
}
