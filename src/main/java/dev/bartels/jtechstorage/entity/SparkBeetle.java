package dev.bartels.jtechstorage.entity;

import java.util.EnumSet;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import dev.bartels.jtechstorage.ModAdvancements;
import dev.bartels.jtechstorage.ModContent;
import dev.bartels.jtechstorage.block.StorageTerminalBlockEntity;

/**
 * Blitz-Käfer: krabbelt um sein Terminal herum und lädt es mit Resonanz auf („Strom als Bonus“).
 * Bei Regen doppelt, bei Gewitter fünfmal so schnell. Füttern mit Redstone/Glowstone gibt einen Schub.
 *
 * <ul>
 *     <li>Rechtsklick (leere Hand): Ladestand anzeigen</li>
 *     <li>Rechtsklick mit Redstone, Glowstone-Staub oder Redstone-Block: füttern</li>
 *     <li>Shift + Rechtsklick (leere Hand): einpacken</li>
 * </ul>
 */
public class SparkBeetle extends NetworkHelperMob {
	/** Der Käfer muss so nah am Terminal sein, um es aufzuladen. */
	public static final int CHARGE_DISTANCE = 8;
	private static final int BASE_CHARGE = 2;
	private static final EntityDataAccessor<Boolean> DATA_CHARGING = SynchedEntityData.defineId(SparkBeetle.class,
			EntityDataSerializers.BOOLEAN);

	public SparkBeetle(EntityType<? extends SparkBeetle> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return createMobAttributes().add(Attributes.MAX_HEALTH, 10.0).add(Attributes.MOVEMENT_SPEED, 0.15);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DATA_CHARGING, false);
	}

	/** Lädt der Käfer gerade auf? (für die Flügeldecken-Animation) */
	public boolean isCharging() {
		return entityData.get(DATA_CHARGING);
	}

	/** Der Käfer macht immer nachts Feierabend – die Resonanz muss tagsüber gesammelt werden. */
	@Override
	protected boolean restsEvenWhenCharged() {
		return true;
	}

	@Override
	protected String unlinkedStatus() {
		return "jtechstorage.beetle.status.unlinked";
	}

	@Override
	protected String linkAdvancement() {
		return "spark_beetle";
	}

	@Override
	protected void onLinked(Player player) {
		setStatus("jtechstorage.beetle.status.linked");
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(1, new StayNearTerminalGoal());
		goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.6));
		goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 6.0f));
		goalSelector.addGoal(7, new RandomLookAroundGoal(this));
	}

	@Override
	protected InteractionResult interactWith(Player player, InteractionHand hand) {
		ItemStack held = player.getItemInHand(hand);
		int boost = held.is(Items.REDSTONE) ? 40 : held.is(Items.GLOWSTONE_DUST) ? 80 : held.is(Items.REDSTONE_BLOCK) ? 400 : 0;

		if (boost > 0) {
			StorageTerminalBlockEntity terminal = terminal();

			if (terminal == null) {
				player.sendOverlayMessage(net.minecraft.network.chat.Component.translatable("jtechstorage.helper.status.no_terminal"));
				return InteractionResult.FAIL;
			}

			boolean full = terminal.addResonance(boost);
			held.consume(1, player);
			sparks(12);
			playSound(SoundEvents.AMETHYST_BLOCK_RESONATE, 1.0f, 1.8f);
			showCharge(player, terminal);

			if (full && player instanceof ServerPlayer serverPlayer) {
				ModAdvancements.grant(serverPlayer, "full_resonance");
			}

			return InteractionResult.SUCCESS;
		}

		if (held.isEmpty() && player.isSecondaryUseActive()) {
			ItemStack item = createPackedItem(ModContent.SPARK_BEETLE_ITEM, player);

			player.getInventory().placeItemBackInInventory(item, Prediction.SERVER_ONLY);
			playSound(SoundEvents.AMETHYST_CLUSTER_BREAK, 1.0f, 1.6f);
			discard();
			return InteractionResult.SUCCESS;
		}

		if (held.isEmpty() && terminal() != null) {
			showCharge(player, terminal());
			return InteractionResult.SUCCESS;
		}

		return super.interactWith(player, hand);
	}

	private static void showCharge(Player player, StorageTerminalBlockEntity terminal) {
		player.sendOverlayMessage(net.minecraft.network.chat.Component.translatable("jtechstorage.beetle.charge",
				terminal.getResonance(), StorageTerminalBlockEntity.MAX_RESONANCE));
	}

	private void sparks(int count) {
		if (level() instanceof ServerLevel serverLevel) {
			serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, getX(), getY() + 0.3, getZ(), count, 0.25, 0.2, 0.25, 0.05);
		}
	}

	@Override
	public void aiStep() {
		super.aiStep();

		if (!(level() instanceof ServerLevel serverLevel) || tickCount % 20 != 0) {
			return;
		}

		if (isResting()) {
			entityData.set(DATA_CHARGING, false);
			return;   // Feierabend: Resonanz muss tagsüber gesammelt werden
		}

		StorageTerminalBlockEntity terminal = terminal();
		entityData.set(DATA_CHARGING, false);

		if (terminal == null) {
			if (terminalPos != null) {
				setStatus("jtechstorage.helper.status.no_terminal");
			}

			return;
		}

		if (distanceToSqr(Vec3.atCenterOf(terminal.getBlockPos())) > CHARGE_DISTANCE * CHARGE_DISTANCE) {
			setStatus("jtechstorage.beetle.status.too_far", CHARGE_DISTANCE);
			return;
		}

		int amount = serverLevel.isThundering() ? BASE_CHARGE * 5 : serverLevel.isRaining() ? BASE_CHARGE * 2 : BASE_CHARGE;
		boolean full = terminal.addResonance(amount);
		entityData.set(DATA_CHARGING, true);
		setStatus("jtechstorage.beetle.status.charging", amount);

		if (random.nextInt(4) == 0) {
			sparks(3);
		}

		if (full && owner != null && serverLevel.getPlayerByUUID(owner) instanceof ServerPlayer serverPlayer
				&& serverPlayer.distanceToSqr(this) < 32 * 32) {
			ModAdvancements.grant(serverPlayer, "full_resonance");
		}
	}

	/** Läuft zurück zum Terminal, wenn er sich zu weit entfernt hat. */
	private class StayNearTerminalGoal extends Goal {
		StayNearTerminalGoal() {
			setFlags(EnumSet.of(Goal.Flag.MOVE));
		}

		@Override
		public boolean canUse() {
			return terminalPos != null && distanceToSqr(Vec3.atCenterOf(terminalPos)) > 5 * 5;
		}

		@Override
		public boolean canContinueToUse() {
			return terminalPos != null && distanceToSqr(Vec3.atCenterOf(terminalPos)) > 2 * 2 && !getNavigation().isDone();
		}

		@Override
		public void start() {
			BlockPos pos = terminalPos;

			if (pos != null) {
				getNavigation().moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 1.0);
			}
		}
	}
}
