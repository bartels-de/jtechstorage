package dev.bartels.jtechstorage.item;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import dev.bartels.jtechstorage.entity.SortCategory;

import dev.bartels.jtechstorage.ModAdvancements;
import dev.bartels.jtechstorage.ModContent;
import dev.bartels.jtechstorage.network.StorageNetwork;
import dev.bartels.jtechstorage.block.StorageTerminalBlockEntity;

/**
 * Speicher-Schlüssel.
 *
 * <ul>
 *     <li>Shift + Rechtsklick auf ein Terminal: Schlüssel koppeln</li>
 *     <li>Shift + Rechtsklick auf eine Kiste: Kiste zum Netzwerk hinzufügen bzw. entfernen</li>
 *     <li>In der Hand: verknüpfte Kisten werden mit Partikeln markiert</li>
 *     <li>Kiste anschauen: zeigt die Kategorie, die der Amethyst-Wichtel ihr gegeben hat</li>
 * </ul>
 */
public class StorageKeyItem extends LinkedTerminalItem {
	private static final int HIGHLIGHT_INTERVAL = 10;
	private static final double HIGHLIGHT_DISTANCE = 48;

	public StorageKeyItem(Properties properties) {
		super(properties);
	}

	/** Rechtsklick in die Luft: mit Rückruf-Verzauberung zum gekoppelten Terminal teleportieren (1× pro Tag). */
	@Override
	public InteractionResult use(Level level, Player player, net.minecraft.world.InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		GlobalPos bound = stack.get(ModContent.LINKED_TERMINAL);

		if (bound == null || player.isSecondaryUseActive()) {
			return InteractionResult.PASS;
		}

		if (level instanceof ServerLevel && player instanceof ServerPlayer serverPlayer) {
			return dev.bartels.jtechstorage.enchant.ModEnchantments.tryRecall(serverPlayer, stack, bound)
					? InteractionResult.SUCCESS : InteractionResult.PASS;
		}

		return dev.bartels.jtechstorage.enchant.ModEnchantments.level(level, dev.bartels.jtechstorage.enchant.ModEnchantments.RECALL, stack) > 0
				? InteractionResult.SUCCESS : InteractionResult.PASS;
	}

	@Override
	protected InteractionResult useOnOtherBlock(UseOnContext context) {
		Level level = context.getLevel();
		Player player = context.getPlayer();
		BlockPos pos = context.getClickedPos();

		if (player == null || !level.getBlockState(pos).is(ModContent.NETWORK_STORAGE)
				|| !(level.getBlockEntity(pos) instanceof Container)) {
			return InteractionResult.PASS;
		}

		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}

		StorageTerminalBlockEntity terminal = findTerminal(context.getItemInHand(), level, player);

		if (terminal == null) {
			return InteractionResult.FAIL;
		}

		if (!terminal.can(player, StorageTerminalBlockEntity.Permission.LINK)) {
			player.sendOverlayMessage(Component.translatable("jtechstorage.message.no_permission",
					Component.translatable("jtechstorage.role.builder")));
			return InteractionResult.FAIL;
		}

		ServerLevel serverLevel = (ServerLevel) level;
		double x = pos.getX() + 0.5;
		double y = pos.getY() + 0.5;
		double z = pos.getZ() + 0.5;

		switch (terminal.toggleLink(pos)) {
			case ADDED -> {
				serverLevel.sendParticles(ParticleTypes.WITCH, x, y + 0.3, z, 20, 0.35, 0.35, 0.35, 0.02);
				level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.0f, 1.5f);
				player.sendOverlayMessage(Component.translatable("jtechstorage.message.linked",
						terminal.getLinks().size(), terminal.getCapacity()));

				if (player instanceof ServerPlayer serverPlayer) {
					int count = terminal.getLinks().size();
					ModAdvancements.grant(serverPlayer, "first_link");

					if (count >= 64) {
						ModAdvancements.grant(serverPlayer, "linked_64");
					}

					if (count >= 128) {
						ModAdvancements.grant(serverPlayer, "linked_128");
					}

					BlockPos partner = StorageNetwork.chestPartner(level.getBlockState(pos), pos);

					if (!StorageNetwork.frameFilter(level, pos, partner).isEmpty()) {
						ModAdvancements.grant(serverPlayer, "sort_rule");
					}
				}
			}
			case REMOVED -> {
				serverLevel.sendParticles(ParticleTypes.SMOKE, x, y + 0.3, z, 15, 0.3, 0.3, 0.3, 0.01);
				level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_HIT, SoundSource.BLOCKS, 1.0f, 0.7f);
				player.sendOverlayMessage(Component.translatable("jtechstorage.message.unlinked",
						terminal.getLinks().size(), terminal.getCapacity()));
			}
			case FULL -> player.sendOverlayMessage(Component.translatable("jtechstorage.message.full", terminal.getCapacity()));
			case OUT_OF_RANGE -> player.sendOverlayMessage(Component.translatable("jtechstorage.message.out_of_range", terminal.getRange()));
		}

		return InteractionResult.SUCCESS;
	}

	/** Solange der Schlüssel in der Hand ist, werden verknüpfte Kisten (nur für diesen Spieler) markiert. */
	@Override
	public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, @Nullable EquipmentSlot slot) {
		if (slot != EquipmentSlot.MAINHAND && slot != EquipmentSlot.OFFHAND) {
			return;
		}

		if (!(entity instanceof ServerPlayer player) || level.getGameTime() % HIGHLIGHT_INTERVAL != 0) {
			return;
		}

		GlobalPos bound = stack.get(ModContent.LINKED_TERMINAL);

		if (bound == null || !bound.dimension().equals(level.dimension()) || !level.isLoaded(bound.pos())
				|| !(level.getBlockEntity(bound.pos()) instanceof StorageTerminalBlockEntity terminal)) {
			return;
		}

		double maxDistSqr = HIGHLIGHT_DISTANCE * HIGHLIGHT_DISTANCE;
		BlockPos terminalPos = bound.pos();

		if (terminalPos.distToCenterSqr(player.position()) < maxDistSqr) {
			level.sendParticles(player, ParticleTypes.END_ROD, true, true,
					terminalPos.getX() + 0.5, terminalPos.getY() + 1.2, terminalPos.getZ() + 0.5, 1, 0, 0, 0, 0);
		}

		for (BlockPos pos : terminal.getLinks()) {
			if (pos.distToCenterSqr(player.position()) < maxDistSqr) {
				level.sendParticles(player, ParticleTypes.WITCH, true, true,
						pos.getX() + 0.5, pos.getY() + 1.05, pos.getZ() + 0.5, 2, 0.2, 0.05, 0.2, 0);
			}
		}

		showCategory(player, level, terminal);
	}

	/** Zeigt beim Anschauen einer verknüpften Kiste deren Wichtel-Kategorie bzw. Sortier-Regel. */
	private static void showCategory(ServerPlayer player, ServerLevel level, StorageTerminalBlockEntity terminal) {
		HitResult hit = player.pick(6.0, 0.0f, false);

		if (!(hit instanceof BlockHitResult blockHit) || hit.getType() != HitResult.Type.BLOCK) {
			return;
		}

		BlockPos pos = blockHit.getBlockPos();
		BlockPos partner = StorageNetwork.chestPartner(level.getBlockState(pos), pos);
		BlockPos linked = terminal.getLinks().contains(pos) ? pos
				: partner != null && terminal.getLinks().contains(partner) ? partner : null;

		if (linked == null) {
			return;
		}

		if (!StorageNetwork.frameFilter(level, pos, partner).isEmpty()) {
			player.sendOverlayMessage(Component.translatable("jtechstorage.key.category.rule"));
			return;
		}

		SortCategory category = terminal.get(linked);

		if (category == null && partner != null) {
			category = terminal.get(linked.equals(pos) ? partner : pos);
		}

		player.sendOverlayMessage(category == null
				? Component.translatable("jtechstorage.key.category.none")
				: Component.translatable("jtechstorage.key.category", Component.translatable(category.translationKey())));
	}
}
