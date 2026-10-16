package dev.bartels.jtechstorage.end;

import java.util.Optional;

import org.jspecify.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.LodestoneTracker;
import net.minecraft.world.level.Level;

/**
 * Hort-Kompass: Rechtsklick im End sucht den nächsten Kristallhort; die Nadel zeigt danach dorthin (wie ein
 * Leitstein-Kompass). Hält man ihn in der Hand, steht die Entfernung in der Aktionsleiste.
 */
public class LairCompassItem extends Item {
	/** Suchradius in Chunks. */
	private static final int SEARCH_RADIUS = 100;

	public LairCompassItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);

		if (!(level instanceof ServerLevel serverLevel)) {
			return InteractionResult.SUCCESS;
		}

		if (level.dimension() != Level.END) {
			player.sendOverlayMessage(Component.translatable("jtechstorage.compass.not_end").withStyle(ChatFormatting.RED));
			return InteractionResult.FAIL;
		}

		player.getCooldowns().addCooldown(stack, 60);
		BlockPos found = serverLevel.findNearestMapStructure(EndContent.CRYSTAL_LAIR, player.blockPosition(), SEARCH_RADIUS, false);

		if (found == null) {
			stack.remove(DataComponents.LODESTONE_TRACKER);
			player.sendOverlayMessage(Component.translatable("jtechstorage.compass.none").withStyle(ChatFormatting.RED));
			return InteractionResult.FAIL;
		}

		stack.set(DataComponents.LODESTONE_TRACKER, new LodestoneTracker(Optional.of(GlobalPos.of(level.dimension(), found)), false));
		level.playSound(null, player.blockPosition(), SoundEvents.LODESTONE_COMPASS_LOCK, SoundSource.PLAYERS, 1.0f, 0.8f);
		level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0f, 1.2f);
		player.sendOverlayMessage(Component.translatable("jtechstorage.compass.found", distance(player, found))
				.withStyle(ChatFormatting.LIGHT_PURPLE));
		return InteractionResult.SUCCESS;
	}

	private static int distance(Entity entity, BlockPos target) {
		double dx = target.getX() + 0.5 - entity.getX();
		double dz = target.getZ() + 0.5 - entity.getZ();
		return (int) Math.round(Math.sqrt(dx * dx + dz * dz));
	}

	private static @Nullable BlockPos target(ItemStack stack, Level level) {
		LodestoneTracker tracker = stack.get(DataComponents.LODESTONE_TRACKER);

		if (tracker == null || tracker.target().isEmpty() || tracker.target().get().dimension() != level.dimension()) {
			return null;
		}

		return tracker.target().get().pos();
	}

	@Override
	public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, @Nullable EquipmentSlot slot) {
		super.inventoryTick(stack, level, entity, slot);

		if (!(entity instanceof ServerPlayer player) || entity.tickCount % 20 != 0
				|| (slot != EquipmentSlot.MAINHAND && slot != EquipmentSlot.OFFHAND)) {
			return;
		}

		BlockPos target = target(stack, level);

		if (target != null) {
			int distance = distance(player, target);
			player.sendOverlayMessage(Component.translatable(distance < 24 ? "jtechstorage.compass.here" : "jtechstorage.compass.distance", distance)
					.withStyle(ChatFormatting.LIGHT_PURPLE));
		}
	}

	@Override
	public boolean isFoil(ItemStack stack) {
		return stack.has(DataComponents.LODESTONE_TRACKER);
	}
}
