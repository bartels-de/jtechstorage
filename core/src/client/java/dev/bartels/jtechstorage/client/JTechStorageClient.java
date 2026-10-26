package dev.bartels.jtechstorage.client;

import dev.bartels.jtechstorage.JTechStorage;
import dev.bartels.jtechstorage.ModConfig;
import dev.bartels.jtechstorage.backpack.PlacedBackpackBlock;
import dev.bartels.jtechstorage.backpack.PlacedBackpackBlockEntity;
import dev.bartels.jtechstorage.client.cosmetic.CosmeticsClient;
import dev.bartels.jtechstorage.client.display.StorageBannerRenderer;
import dev.bartels.jtechstorage.client.display.StorageBannerScreen;
import dev.bartels.jtechstorage.client.display.StorageDisplayRenderer;
import dev.bartels.jtechstorage.client.display.StorageDisplayScreen;
import dev.bartels.jtechstorage.client.farm.ChunkLoaderScreen;
import dev.bartels.jtechstorage.client.farm.CrystalPusherScreen;
import dev.bartels.jtechstorage.client.farm.FarmMonitorScreen;
import dev.bartels.jtechstorage.client.farm.FarmsScreen;
import dev.bartels.jtechstorage.client.gui.GuiFx;
import dev.bartels.jtechstorage.client.gui.GuiSettings;
import dev.bartels.jtechstorage.client.logistics.SorterScreen;
import dev.bartels.jtechstorage.display.DisplayContent;
import dev.bartels.jtechstorage.display.StorageBannerBlockEntity;
import dev.bartels.jtechstorage.display.StorageDisplayBlockEntity;
import dev.bartels.jtechstorage.farm.FarmContent;
import dev.bartels.jtechstorage.farm.FarmPayloads;
import dev.bartels.jtechstorage.item.HelperSpawnItem;
import dev.bartels.jtechstorage.logistics.LogisticsContent;
import dev.bartels.jtechstorage.net.ConfigSyncPayload;
import dev.bartels.jtechstorage.net.PanelMenuPayload;
import dev.bartels.jtechstorage.net.TerminalStatsPayload;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.BlockColorRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityRenderLayerRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;

import dev.bartels.jtechstorage.ModContent;
import dev.bartels.jtechstorage.client.entity.BackpackLayer;
import dev.bartels.jtechstorage.client.entity.BackpackModel;
import dev.bartels.jtechstorage.client.entity.JbartelsHologramModel;
import dev.bartels.jtechstorage.client.entity.JbartelsHologramRenderer;
import dev.bartels.jtechstorage.item.GuideBookItem;
import dev.bartels.jtechstorage.menu.StorageTerminalMenu;
import dev.bartels.jtechstorage.net.TerminalContentsPayload;
import dev.bartels.jtechstorage.menu.TeamMenu;
import dev.bartels.jtechstorage.net.TeamStatePayload;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public class JTechStorageClient implements ClientModInitializer {
	public static final KeyMapping.Category KEY_CATEGORY =
			KeyMapping.Category.register(JTechStorage.id("main"));

	private static boolean jumpWasDown;
	private static boolean airJumpArmed;
	private static int airJumps;

	@Override
	public void onInitializeClient() {
		GuiFx.register();
		MenuScreens.register(ModContent.STORAGE_TERMINAL_MENU, StorageTerminalScreen::new);
		StorageTerminalMenu.clientRowProvider = crafting -> {
			int rows = GuiSettings.get().rows.rows;
			int min = crafting ? 2 : 3;

			if (rows == 0) {
				int height = Minecraft.getInstance().getWindow().getGuiScaledHeight();
				rows = (height - 8 - StorageTerminalMenu.Layout.fixedHeight(crafting)) / 18;
				return Math.clamp(rows, min, 14);
			}

			return Math.max(min, crafting ? rows - 2 : rows);
		};
		MenuScreens.register(ModContent.TEAM_MENU, TeamScreen::new);
		MenuScreens.register(ModContent.TRADE_MENU, TradeScreen::new);
		MenuScreens.register(FarmContent.CRYSTAL_PUSHER_MENU, CrystalPusherScreen::new);
		MenuScreens.register(LogisticsContent.SORTER_MENU, SorterScreen::new);
		MenuScreens.register(FarmContent.FARM_MONITOR_MENU, FarmMonitorScreen::new);
		MenuScreens.register(FarmContent.CHUNK_LOADER_MENU, ChunkLoaderScreen::new);
		// v0.36: menus of Storage Display and Storage Banner
		ClientPlayNetworking.registerGlobalReceiver(PanelMenuPayload.TYPE, (payload, context) -> {
			var client = context.client();

			if (client.level == null) {
				return;
			}

			var be = client.level.getBlockEntity(payload.pos());

			if (be instanceof StorageDisplayBlockEntity) {
				client.setScreenAndShow(new StorageDisplayScreen(payload.pos(), payload.items(), payload.counts()));
			} else if (be instanceof StorageBannerBlockEntity) {
				client.setScreenAndShow(new StorageBannerScreen(payload.pos(), payload.items(), payload.counts()));
			}
		});
		// Companions (v0.25)
		// Tooltips: short description, controls while Shift is held (keys tooltip.jtechstorage.<id>.N / .controls.N)
		ItemTooltipCallback.EVENT.register((stack, context, flag, lines) -> addModTooltip(stack, lines));
		EntityRenderers.register(ModContent.CRYSTAL_SHARD, ThrownItemRenderer::new);
		ModelLayerRegistry.registerModelLayer(JbartelsHologramModel.LAYER, JbartelsHologramModel::createBodyLayer);
		EntityRenderers.register(ModContent.JBARTELS_HOLOGRAM, JbartelsHologramRenderer::new);

		// Backpack on the back
		ModelLayerRegistry.registerModelLayer(BackpackModel.LAYER, BackpackModel::createBodyLayer);
		// Placed backpacks: tint the cloth in the backpack's color
		BlockColorRegistry.register(List.of(
				new BlockTintSource() {
					@Override
					public int color(BlockState state) {
						return 0xFF000000 | state.getValue(PlacedBackpackBlock.TIER).defaultColor;
					}

					@Override
					public int colorInWorld(BlockState state,
							BlockAndTintGetter level, BlockPos pos) {
						return level.getBlockEntity(pos) instanceof PlacedBackpackBlockEntity backpack
								? 0xFF000000 | backpack.fabricColor() : color(state);
					}
				}), ModContent.PLACED_BACKPACK);
		LivingEntityRenderLayerRegistrationCallback.EVENT.register((type, renderer, helper, context) -> {
			if (renderer instanceof AvatarRenderer<?> avatar) {
				helper.register(new BackpackLayer(avatar, new BackpackModel(context.bakeLayer(BackpackModel.LAYER))));
			}
		});
		CosmeticsClient.init();
		BlockEntityRendererRegistry.register(DisplayContent.STORAGE_DISPLAY_BLOCK_ENTITY,
				StorageDisplayRenderer::new);
		BlockEntityRendererRegistry.register(DisplayContent.STORAGE_BANNER_BLOCK_ENTITY,
				StorageBannerRenderer::new);
		GuideBookItem.openScreen = stack -> Minecraft.getInstance().setScreenAndShow(new GuideBookScreen(stack));

		ClientPlayNetworking.registerGlobalReceiver(ConfigSyncPayload.TYPE,
				(payload, context) -> ModConfig.applyRemote(payload.json()));
		ClientPlayConnectionEvents.DISCONNECT.register(
				(handler, client) -> ModConfig.restoreLocal());

		ClientPlayNetworking.registerGlobalReceiver(TeamStatePayload.TYPE, (payload, context) -> {
			if (context.player().containerMenu instanceof TeamMenu menu && menu.containerId == payload.containerId()) {
				menu.setClientState(payload.state());
			}
		});

		ClientPlayNetworking.registerGlobalReceiver(FarmPayloads.Farms.TYPE, (payload, context) -> {
			if (Minecraft.getInstance().gui.screen() instanceof FarmsScreen farms && farms.containerId() == payload.containerId()) {
				farms.setFarms(payload.farms());
			}
		});
		ClientPlayNetworking.registerGlobalReceiver(TerminalStatsPayload.TYPE, (payload, context) -> {
			if (Minecraft.getInstance().gui.screen() instanceof TerminalStatsScreen stats && stats.containerId() == payload.containerId()) {
				stats.setStats(payload);
			}
		});
		ClientPlayNetworking.registerGlobalReceiver(TerminalContentsPayload.TYPE, (payload, context) -> {
			if (context.player().containerMenu instanceof StorageTerminalMenu menu
					&& menu.containerId == payload.containerId()) {
				menu.setClientContents(payload.entries(), payload.info(), payload.access());
			}
		});
	}

	private static void addModTooltip(ItemStack stack, List<Component> lines) {
		Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());

		if (!JTechStorage.MOD_ID.equals(id.getNamespace())) {
			return;
		}

		String path = id.getPath().endsWith("_backpack") ? "backpack" : id.getPath();
		String base = "tooltip.jtechstorage." + path;

		if (!Language.getInstance().has(base + ".0")) {
			return;
		}

		List<Component> add = new ArrayList<>();

		for (int i = 0; Language.getInstance().has(base + "." + i); i++) {
			add.add(Component.translatable(base + "." + i).withStyle(ChatFormatting.GRAY));
		}

		boolean helper = stack.getItem() instanceof HelperSpawnItem;
		boolean controls = Language.getInstance().has(base + ".controls.0");

		if (controls || helper) {
			if (Minecraft.getInstance().hasShiftDown()) {
				for (int i = 0; Language.getInstance().has(base + ".controls." + i); i++) {
					add.add(Component.literal("• ").append(Component.translatable(base + ".controls." + i))
							.withStyle(ChatFormatting.LIGHT_PURPLE));
				}

				if (helper) {
					add.add(Component.literal("• ").append(Component.translatable("tooltip.jtechstorage.pack"))
							.withStyle(ChatFormatting.LIGHT_PURPLE));
				}
			} else {
				add.add(Component.translatable("tooltip.jtechstorage.hold_shift")
						.withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));
			}
		}

		lines.addAll(Math.min(1, lines.size()), add);
	}
}
