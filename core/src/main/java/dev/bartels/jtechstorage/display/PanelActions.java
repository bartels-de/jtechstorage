package dev.bartels.jtechstorage.display;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import dev.bartels.jtechstorage.block.StorageTerminalBlockEntity;
import dev.bartels.jtechstorage.net.PanelItemPayload;
import dev.bartels.jtechstorage.net.PanelMenuPayload;
import dev.bartels.jtechstorage.net.PanelSettingPayload;
import dev.bartels.jtechstorage.network.StorageNetwork;

/** Menu of storage screen and storage banner (v0.36): open, settings, choose items. */
public final class PanelActions {
	// Screen
	public static final int MODE = 0;
	public static final int FRAME = 1;
	public static final int BACKGROUND = 2;
	public static final int ACCENT = 3;
	public static final int GLOW = 4;
	public static final int SCANLINES = 5;
	public static final int DISPLAY_CLEAR = 6;
	public static final int DISPLAY_FROM_HAND = 7;
	// Banner
	public static final int CLOTH = 10;
	public static final int TRIM = 11;
	public static final int PATTERN = 12;
	public static final int TEXT = 13;
	public static final int WIND = 14;
	public static final int BANNER_REMOVE = 15;
	public static final int BANNER_CLEAR = 16;
	/** At most this many network entries go to the menu. */
	private static final int MAX_ITEMS = 324;

	private PanelActions() {
	}

	public static void init() {
		PayloadTypeRegistry.serverboundPlay().register(PanelSettingPayload.TYPE, PanelSettingPayload.STREAM_CODEC);
		PayloadTypeRegistry.serverboundPlay().register(PanelItemPayload.TYPE, PanelItemPayload.STREAM_CODEC);
		PayloadTypeRegistry.clientboundPlay().register(PanelMenuPayload.TYPE, PanelMenuPayload.STREAM_CODEC);
		ServerPlayNetworking.registerGlobalReceiver(PanelSettingPayload.TYPE, (payload, context) -> setting(context.player(), payload));
		ServerPlayNetworking.registerGlobalReceiver(PanelItemPayload.TYPE, (payload, context) -> item(context.player(), payload));
	}

	/** Sends the player the menu (with the items of the linked network). */
	public static void open(ServerPlayer player, NetworkPanelBlockEntity panel) {
		List<ItemStack> items = new ArrayList<>();
		List<Long> counts = new ArrayList<>();
		StorageTerminalBlockEntity terminal = panel.terminal();

		if (terminal != null) {
			List<StorageNetwork.Entry> all = new ArrayList<>(terminal.buildNetwork().aggregate());
			all.sort((a, b) -> Long.compare(b.total(), a.total()));

			for (StorageNetwork.Entry e : all) {
				if (items.size() >= MAX_ITEMS) {
					break;
				}

				items.add(e.prototype().copyWithCount(1));
				counts.add(e.total());
			}
		}

		ServerPlayNetworking.send(player, new PanelMenuPayload(panel.getBlockPos(), items, counts));
	}

	private static NetworkPanelBlockEntity panel(ServerPlayer player, BlockPos pos) {
		if (!player.level().isLoaded(pos) || pos.distToCenterSqr(player.position()) > 10.0 * 10.0) {
			return null;
		}

		BlockPos target = player.level().getBlockEntity(pos) instanceof StorageDisplayBlockEntity ? DisplayShape.of(player.level(), pos).origin() : pos;
		return player.level().getBlockEntity(target) instanceof NetworkPanelBlockEntity panel ? panel : null;
	}

	private static void setting(ServerPlayer player, PanelSettingPayload p) {
		NetworkPanelBlockEntity panel = panel(player, p.pos());

		if (panel instanceof StorageDisplayBlockEntity display) {
			switch (p.key()) {
				case MODE -> display.setMode(StorageDisplayBlockEntity.Mode.values()[Math.floorMod(p.value(), StorageDisplayBlockEntity.Mode.values().length)]);
				case FRAME, BACKGROUND, ACCENT, GLOW, SCANLINES -> display.setLook(p.key(), p.value());
				case DISPLAY_CLEAR -> display.setTracked(ItemStack.EMPTY);
				case DISPLAY_FROM_HAND -> display.setTracked(player.getMainHandItem());
				default -> {
				}
			}
		} else if (panel instanceof StorageBannerBlockEntity banner) {
			switch (p.key()) {
				case CLOTH, TRIM, PATTERN, TEXT, WIND -> banner.setLook(p.key(), p.value());
				case BANNER_REMOVE -> banner.removeAt(p.value());
				case BANNER_CLEAR -> banner.clear();
				default -> {
				}
			}
		} else {
			return;
		}

		player.level().playSound(null, panel.getBlockPos(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.BLOCKS, 0.25f, 1.6f);
	}

	private static void item(ServerPlayer player, PanelItemPayload p) {
		NetworkPanelBlockEntity panel = panel(player, p.pos());

		if (p.stack().isEmpty()) {
			return;
		}

		if (panel instanceof StorageDisplayBlockEntity display) {
			display.setTracked(p.stack());
		} else if (panel instanceof StorageBannerBlockEntity banner) {
			banner.toggle(p.stack());
		} else {
			return;
		}

		player.level().playSound(null, panel.getBlockPos(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.5f, 1.5f);
	}
}
