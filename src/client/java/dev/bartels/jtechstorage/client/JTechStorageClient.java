package dev.bartels.jtechstorage.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityRenderLayerRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;

import dev.bartels.jtechstorage.ModContent;
import dev.bartels.jtechstorage.client.entity.AmethystSpiderModel;
import dev.bartels.jtechstorage.client.entity.AmethystSpiderRenderer;
import dev.bartels.jtechstorage.client.entity.AmethystSpriteModel;
import dev.bartels.jtechstorage.client.entity.AmethystSpriteRenderer;
import dev.bartels.jtechstorage.client.entity.ChelModel;
import dev.bartels.jtechstorage.client.entity.ChelRenderer;
import dev.bartels.jtechstorage.client.entity.AshleyModel;
import dev.bartels.jtechstorage.client.entity.AshleyRenderer;
import dev.bartels.jtechstorage.client.entity.BackpackLayer;
import dev.bartels.jtechstorage.client.entity.BackpackModel;
import dev.bartels.jtechstorage.client.entity.CollectorMothModel;
import dev.bartels.jtechstorage.client.entity.JbartelsHologramModel;
import dev.bartels.jtechstorage.client.entity.JbartelsHologramRenderer;
import dev.bartels.jtechstorage.client.entity.AmethystRobotModel;
import dev.bartels.jtechstorage.client.entity.AmethystRobotRenderer;
import dev.bartels.jtechstorage.client.entity.PeterChelParkerModel;
import dev.bartels.jtechstorage.client.entity.PeterChelParkerRenderer;
import dev.bartels.jtechstorage.client.entity.CollectorMothRenderer;
import dev.bartels.jtechstorage.client.entity.SparkBeetleModel;
import dev.bartels.jtechstorage.client.entity.SparkBeetleRenderer;
import dev.bartels.jtechstorage.client.entity.StorageGriffinModel;
import dev.bartels.jtechstorage.client.entity.StorageGriffinRenderer;
import dev.bartels.jtechstorage.item.GuideBookItem;
import dev.bartels.jtechstorage.menu.ChelOrderMenu;
import dev.bartels.jtechstorage.menu.StorageTerminalMenu;
import dev.bartels.jtechstorage.net.ChelOrdersPayload;
import dev.bartels.jtechstorage.net.TerminalContentsPayload;
import dev.bartels.jtechstorage.menu.TeamMenu;
import dev.bartels.jtechstorage.menu.NestMenu;
import dev.bartels.jtechstorage.net.NestStatePayload;
import dev.bartels.jtechstorage.client.entity.PhoenixModel;
import dev.bartels.jtechstorage.client.entity.PhoenixRenderer;
import dev.bartels.jtechstorage.net.TeamStatePayload;

public class JTechStorageClient implements ClientModInitializer {
	public static final net.minecraft.client.KeyMapping.Category KEY_CATEGORY =
			net.minecraft.client.KeyMapping.Category.register(dev.bartels.jtechstorage.JTechStorage.id("main"));
	public static final net.minecraft.client.KeyMapping COMPANION_HEAL_KEY = net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper.registerKeyMapping(
			new net.minecraft.client.KeyMapping("key.jtechstorage.companion_heal", com.mojang.blaze3d.platform.InputConstants.KEY_H, KEY_CATEGORY));
	public static final net.minecraft.client.KeyMapping COMPANION_STAY_KEY = net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper.registerKeyMapping(
			new net.minecraft.client.KeyMapping("key.jtechstorage.companion_stay", com.mojang.blaze3d.platform.InputConstants.KEY_G, KEY_CATEGORY));
	public static final net.minecraft.client.KeyMapping COMPANION_MENU_KEY = net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper.registerKeyMapping(
			new net.minecraft.client.KeyMapping("key.jtechstorage.companion_menu", com.mojang.blaze3d.platform.InputConstants.KEY_K, KEY_CATEGORY));
	public static final net.minecraft.client.KeyMapping COMPANION_SPECIAL_KEY = net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper.registerKeyMapping(
			new net.minecraft.client.KeyMapping("key.jtechstorage.companion_special", com.mojang.blaze3d.platform.InputConstants.KEY_B, KEY_CATEGORY));
	public static final net.minecraft.client.KeyMapping DRAGON_BREATH_KEY = net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper.registerKeyMapping(
			new net.minecraft.client.KeyMapping("key.jtechstorage.dragon_breath", com.mojang.blaze3d.platform.InputConstants.KEY_R, KEY_CATEGORY));
	public static final net.minecraft.client.KeyMapping DRAGON_RAIN_KEY = net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper.registerKeyMapping(
			new net.minecraft.client.KeyMapping("key.jtechstorage.dragon_rain", com.mojang.blaze3d.platform.InputConstants.KEY_V, KEY_CATEGORY));

	private static boolean jumpWasDown;
	private static boolean airJumpArmed;
	private static int airJumps;

	@Override
	public void onInitializeClient() {
		MenuScreens.register(ModContent.STORAGE_TERMINAL_MENU, StorageTerminalScreen::new);
		StorageTerminalMenu.clientRowProvider = crafting -> {
			int rows = dev.bartels.jtechstorage.client.gui.GuiSettings.get().rows.rows;
			int min = crafting ? 2 : 3;

			if (rows == 0) {
				int height = Minecraft.getInstance().getWindow().getGuiScaledHeight();
				rows = (height - 8 - StorageTerminalMenu.Layout.fixedHeight(crafting)) / 18;
				return Math.clamp(rows, min, 14);
			}

			return Math.max(min, crafting ? rows - 2 : rows);
		};
		MenuScreens.register(ModContent.CHEL_ORDER_MENU, ChelOrderScreen::new);
		MenuScreens.register(ModContent.TEAM_MENU, TeamScreen::new);
		MenuScreens.register(ModContent.TRADE_MENU, TradeScreen::new);
		MenuScreens.register(ModContent.NEST_MENU, NestScreen::new);
		ModelLayerRegistry.registerModelLayer(PhoenixModel.LAYER, PhoenixModel::createBodyLayer);
		EntityRenderers.register(ModContent.PHOENIX, PhoenixRenderer::new);
		ModelLayerRegistry.registerModelLayer(dev.bartels.jtechstorage.client.entity.AmethystFoxModel.LAYER,
				dev.bartels.jtechstorage.client.entity.AmethystFoxModel::createBodyLayer);
		EntityRenderers.register(ModContent.AMETHYST_FOX, dev.bartels.jtechstorage.client.entity.AmethystFoxRenderer::new);
		ModelLayerRegistry.registerModelLayer(dev.bartels.jtechstorage.client.entity.AmethystPhoenixModel.LAYER,
				dev.bartels.jtechstorage.client.entity.AmethystPhoenixModel::createBodyLayer);
		EntityRenderers.register(ModContent.AMETHYST_PHOENIX, dev.bartels.jtechstorage.client.entity.AmethystPhoenixRenderer::new);
		MenuScreens.register(ModContent.AMETHYST_PHOENIX_MENU, AmethystPhoenixScreen::new);
		MenuScreens.register(ModContent.PHOENIX_STOP_MENU, PhoenixStopScreen::new);
		MenuScreens.register(dev.bartels.jtechstorage.farm.FarmContent.CRYSTAL_PUSHER_MENU, dev.bartels.jtechstorage.client.farm.CrystalPusherScreen::new);
		MenuScreens.register(dev.bartels.jtechstorage.farm.FarmContent.FARM_MONITOR_MENU, dev.bartels.jtechstorage.client.farm.FarmMonitorScreen::new);
		MenuScreens.register(dev.bartels.jtechstorage.farm.FarmContent.CHUNK_LOADER_MENU, dev.bartels.jtechstorage.client.farm.ChunkLoaderScreen::new);
		for (dev.bartels.jtechstorage.chest.AmethystChestTier tier : dev.bartels.jtechstorage.chest.AmethystChestTier.values()) {
			MenuScreens.register(dev.bartels.jtechstorage.chest.ChestContent.menuType(tier), dev.bartels.jtechstorage.client.chest.AmethystChestScreen::new);
		}
		// Wölfe (v0.26)
		ModelLayerRegistry.registerModelLayer(dev.bartels.jtechstorage.client.entity.CrystalWolfModel.LAYER,
				dev.bartels.jtechstorage.client.entity.CrystalWolfModel::createBodyLayer);
		EntityRenderers.register(dev.bartels.jtechstorage.wolf.WolfContent.CRYSTAL_WOLF,
				dev.bartels.jtechstorage.client.entity.CrystalWolfRenderer::new);
		ModelLayerRegistry.registerModelLayer(dev.bartels.jtechstorage.client.entity.MoonWolfModel.LAYER,
				dev.bartels.jtechstorage.client.entity.MoonWolfModel::createBodyLayer);
		EntityRenderers.register(dev.bartels.jtechstorage.wolf.WolfContent.MOON_WOLF,
				dev.bartels.jtechstorage.client.entity.MoonWolfRenderer::new);
		// Resonanz-Sturm (v0.25)
		ModelLayerRegistry.registerModelLayer(dev.bartels.jtechstorage.client.entity.CrystalShardlingModel.LAYER,
				dev.bartels.jtechstorage.client.entity.CrystalShardlingModel::createBodyLayer);
		EntityRenderers.register(dev.bartels.jtechstorage.storm.StormContent.CRYSTAL_SHARDLING,
				dev.bartels.jtechstorage.client.entity.CrystalShardlingRenderer::new);
		ModelLayerRegistry.registerModelLayer(dev.bartels.jtechstorage.client.entity.StormHeraldModel.LAYER,
				dev.bartels.jtechstorage.client.entity.StormHeraldModel::createBodyLayer);
		EntityRenderers.register(dev.bartels.jtechstorage.storm.StormContent.STORM_HERALD,
				dev.bartels.jtechstorage.client.entity.StormHeraldRenderer::new);
		// Begleiter (v0.25)
		ModelLayerRegistry.registerModelLayer(dev.bartels.jtechstorage.client.entity.MantisModel1.LAYER, dev.bartels.jtechstorage.client.entity.MantisModel1::createBodyLayer);
		ModelLayerRegistry.registerModelLayer(dev.bartels.jtechstorage.client.entity.MantisModel2.LAYER, dev.bartels.jtechstorage.client.entity.MantisModel2::createBodyLayer);
		ModelLayerRegistry.registerModelLayer(dev.bartels.jtechstorage.client.entity.MantisModel3.LAYER, dev.bartels.jtechstorage.client.entity.MantisModel3::createBodyLayer);
		ModelLayerRegistry.registerModelLayer(dev.bartels.jtechstorage.client.entity.ShroomModel1.LAYER, dev.bartels.jtechstorage.client.entity.ShroomModel1::createBodyLayer);
		ModelLayerRegistry.registerModelLayer(dev.bartels.jtechstorage.client.entity.ShroomModel2.LAYER, dev.bartels.jtechstorage.client.entity.ShroomModel2::createBodyLayer);
		ModelLayerRegistry.registerModelLayer(dev.bartels.jtechstorage.client.entity.ShroomModel3.LAYER, dev.bartels.jtechstorage.client.entity.ShroomModel3::createBodyLayer);
		ModelLayerRegistry.registerModelLayer(dev.bartels.jtechstorage.client.entity.RavenModel1.LAYER, dev.bartels.jtechstorage.client.entity.RavenModel1::createBodyLayer);
		ModelLayerRegistry.registerModelLayer(dev.bartels.jtechstorage.client.entity.RavenModel2.LAYER, dev.bartels.jtechstorage.client.entity.RavenModel2::createBodyLayer);
		ModelLayerRegistry.registerModelLayer(dev.bartels.jtechstorage.client.entity.RavenModel3.LAYER, dev.bartels.jtechstorage.client.entity.RavenModel3::createBodyLayer);
		ModelLayerRegistry.registerModelLayer(dev.bartels.jtechstorage.client.entity.GeodeKeeperModel.LAYER, dev.bartels.jtechstorage.client.entity.GeodeKeeperModel::createBodyLayer);
		EntityRenderers.register(dev.bartels.jtechstorage.companion.CompanionContent.MANTIS, context -> new dev.bartels.jtechstorage.client.entity.CompanionRenderer<>(context, "mantis", 0.6f, new float[] {1.0f, 1.0f, 1.2f},
				java.util.List.<java.util.function.Supplier<net.minecraft.client.model.EntityModel<dev.bartels.jtechstorage.client.entity.CompanionRenderState>>>of(() -> new dev.bartels.jtechstorage.client.entity.MantisModel1(context.bakeLayer(dev.bartels.jtechstorage.client.entity.MantisModel1.LAYER)), () -> new dev.bartels.jtechstorage.client.entity.MantisModel2(context.bakeLayer(dev.bartels.jtechstorage.client.entity.MantisModel2.LAYER)), () -> new dev.bartels.jtechstorage.client.entity.MantisModel3(context.bakeLayer(dev.bartels.jtechstorage.client.entity.MantisModel3.LAYER)))));
		EntityRenderers.register(dev.bartels.jtechstorage.companion.CompanionContent.SHROOM, context -> new dev.bartels.jtechstorage.client.entity.CompanionRenderer<>(context, "shroom", 0.5f, new float[] {1.0f, 1.0f, 1.0f},
				java.util.List.<java.util.function.Supplier<net.minecraft.client.model.EntityModel<dev.bartels.jtechstorage.client.entity.CompanionRenderState>>>of(() -> new dev.bartels.jtechstorage.client.entity.ShroomModel1(context.bakeLayer(dev.bartels.jtechstorage.client.entity.ShroomModel1.LAYER)), () -> new dev.bartels.jtechstorage.client.entity.ShroomModel2(context.bakeLayer(dev.bartels.jtechstorage.client.entity.ShroomModel2.LAYER)), () -> new dev.bartels.jtechstorage.client.entity.ShroomModel3(context.bakeLayer(dev.bartels.jtechstorage.client.entity.ShroomModel3.LAYER)))));
		EntityRenderers.register(dev.bartels.jtechstorage.companion.CompanionContent.RAVEN, context -> new dev.bartels.jtechstorage.client.entity.CompanionRenderer<>(context, "raven", 0.4f, new float[] {1.3f, 1.5f, 2.0f},
				java.util.List.<java.util.function.Supplier<net.minecraft.client.model.EntityModel<dev.bartels.jtechstorage.client.entity.CompanionRenderState>>>of(() -> new dev.bartels.jtechstorage.client.entity.RavenModel1(context.bakeLayer(dev.bartels.jtechstorage.client.entity.RavenModel1.LAYER)), () -> new dev.bartels.jtechstorage.client.entity.RavenModel2(context.bakeLayer(dev.bartels.jtechstorage.client.entity.RavenModel2.LAYER)), () -> new dev.bartels.jtechstorage.client.entity.RavenModel3(context.bakeLayer(dev.bartels.jtechstorage.client.entity.RavenModel3.LAYER)))));
		EntityRenderers.register(dev.bartels.jtechstorage.companion.CompanionContent.SWARM_RAVEN, dev.bartels.jtechstorage.client.entity.SwarmRavenRenderer::new);
		EntityRenderers.register(dev.bartels.jtechstorage.companion.CompanionContent.CROW_DECOY, dev.bartels.jtechstorage.client.entity.CrowDecoyRenderer::new);
		EntityRenderers.register(dev.bartels.jtechstorage.companion.CompanionContent.GEODE_KEEPER, dev.bartels.jtechstorage.client.entity.GeodeKeeperRenderer::new);
		ModelLayerRegistry.registerModelLayer(dev.bartels.jtechstorage.client.entity.CheldroganModel.LAYER,
				dev.bartels.jtechstorage.client.entity.CheldroganModel::createBodyLayer);
		EntityRenderers.register(ModContent.CHELDROGAN, dev.bartels.jtechstorage.client.entity.CheldroganRenderer::new);
		dev.bartels.jtechstorage.client.companion.CompanionClient.init();
		// Begleiter (v0.25.2): Taste H = Pilz heilt jetzt, Shift + H = automatisch/Hotkey umschalten
		net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (COMPANION_STAY_KEY.consumeClick()) {
				if (client.player != null) {
					ClientPlayNetworking.send(new dev.bartels.jtechstorage.net.CompanionActionPayload(2));
				}
			}

			while (COMPANION_SPECIAL_KEY.consumeClick()) {
				if (client.player != null) {
					ClientPlayNetworking.send(new dev.bartels.jtechstorage.net.CompanionActionPayload(dev.bartels.jtechstorage.net.CompanionActionPayload.SPECIAL));
				}
			}

			while (COMPANION_MENU_KEY.consumeClick()) {
				if (client.player != null && client.gui.screen() == null) {
					dev.bartels.jtechstorage.client.companion.CompanionClient.openFromKey(client);
				}
			}

			while (COMPANION_HEAL_KEY.consumeClick()) {
				if (client.player != null) {
					ClientPlayNetworking.send(new dev.bartels.jtechstorage.net.CompanionActionPayload(client.player.isShiftKeyDown() ? 1 : 0));
				}
			}
		});
		// Cheldrogan reiten: Linksklick = Schwanzhieb, Taste (Standard R) = Kristall-Odem, Taste (Standard V) = Splitter-Regen.
		// START_CLIENT_TICK läuft vor der Vanilla-Tastenverarbeitung – die Klicks werden hier verbraucht.
		net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.START_CLIENT_TICK.register(client -> {
			if (client.player == null || !(client.player.getVehicle() instanceof dev.bartels.jtechstorage.entity.Cheldrogan dragon)
					|| dragon.getControllingPassenger() != client.player) {
				return;
			}

			while (client.options.keyAttack.consumeClick()) {
				ClientPlayNetworking.send(new dev.bartels.jtechstorage.net.DragonActionPayload(dev.bartels.jtechstorage.entity.Cheldrogan.ACTION_CLAW));
			}

			while (DRAGON_BREATH_KEY.consumeClick()) {
				ClientPlayNetworking.send(new dev.bartels.jtechstorage.net.DragonActionPayload(dev.bartels.jtechstorage.entity.Cheldrogan.ACTION_BREATH));
			}

			while (DRAGON_RAIN_KEY.consumeClick()) {
				ClientPlayNetworking.send(new dev.bartels.jtechstorage.net.DragonActionPayload(dev.bartels.jtechstorage.entity.Cheldrogan.ACTION_RAIN));
			}
		});
		// Tooltips: kurze Beschreibung, mit gedrückter Shift-Taste die Bedienung (Schlüssel tooltip.jtechstorage.<id>.N / .controls.N)
		net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback.EVENT.register((stack, context, flag, lines) -> addModTooltip(stack, lines));
		// Doppelsprung-Stiefel: in der Luft Sprungtaste erneut drücken (Bewegung des eigenen Spielers rechnet der Client)
		net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(client -> {
			net.minecraft.client.player.LocalPlayer player = client.player;

			if (player == null) {
				return;
			}

			boolean down = client.options.keyJump.isDown();

			if (player.onGround() || player.isInWater() || player.onClimbable() || player.getAbilities().flying || player.isPassenger()
					|| player.isFallFlying()) {
				airJumps = 0;
				airJumpArmed = false;
				return;
			}

			if (!down) {
				airJumpArmed = true;
			} else if (airJumpArmed) {
				airJumpArmed = false;
				int level = dev.bartels.jtechstorage.enchant.ModEnchantments.level(player,
						dev.bartels.jtechstorage.enchant.ModEnchantments.DOUBLE_JUMP, net.minecraft.world.entity.EquipmentSlot.FEET);

				if (airJumps < level) {
					airJumps++;
					net.minecraft.world.phys.Vec3 motion = player.getDeltaMovement();
					player.setDeltaMovement(motion.x * 1.15, 0.58, motion.z * 1.15);
					player.resetFallDistance();
					ClientPlayNetworking.send(dev.bartels.jtechstorage.net.DoubleJumpPayload.INSTANCE);
				}
			}
		});
		// Sprungtaste auf dem vorderen Sitz des Amethyst-Phönix: früher abfliegen
		net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(client -> {
			boolean down = client.player != null && client.options.keyJump.isDown()
					&& client.player.getVehicle() instanceof dev.bartels.jtechstorage.entity.AmethystPhoenix phoenix
					&& phoenix.getFirstPassenger() == client.player;

			if (down && !jumpWasDown) {
				ClientPlayNetworking.send(new dev.bartels.jtechstorage.net.PhoenixDepartPayload(client.player.getVehicle().getId()));
			}

			jumpWasDown = down;
		});
		ModelLayerRegistry.registerModelLayer(AmethystSpriteModel.LAYER, AmethystSpriteModel::createBodyLayer);
		EntityRenderers.register(ModContent.AMETHYST_SPRITE, AmethystSpriteRenderer::new);
		ModelLayerRegistry.registerModelLayer(CollectorMothModel.LAYER, CollectorMothModel::createBodyLayer);
		EntityRenderers.register(ModContent.COLLECTOR_MOTH, CollectorMothRenderer::new);
		ModelLayerRegistry.registerModelLayer(StorageGriffinModel.LAYER, StorageGriffinModel::createBodyLayer);
		EntityRenderers.register(ModContent.STORAGE_GRIFFIN, StorageGriffinRenderer::new);
		ModelLayerRegistry.registerModelLayer(SparkBeetleModel.LAYER, SparkBeetleModel::createBodyLayer);
		EntityRenderers.register(ModContent.SPARK_BEETLE, SparkBeetleRenderer::new);
		ModelLayerRegistry.registerModelLayer(ChelModel.LAYER, ChelModel::createBodyLayer);
		EntityRenderers.register(ModContent.CHEL, ChelRenderer::new);
		ModelLayerRegistry.registerModelLayer(PeterChelParkerModel.LAYER, PeterChelParkerModel::createBodyLayer);
		EntityRenderers.register(ModContent.PETER_CHEL_PARKER, PeterChelParkerRenderer::new);
		EntityRenderers.register(ModContent.CRYSTAL_PALADIN, net.minecraft.client.renderer.entity.NoopRenderer::new);   // alte Paladine werden sofort zu Peter
		ModelLayerRegistry.registerModelLayer(AshleyModel.LAYER, AshleyModel::createBodyLayer);
		EntityRenderers.register(ModContent.ASHLEY, AshleyRenderer::new);
		EntityRenderers.register(ModContent.CRYSTAL_SHARD, ThrownItemRenderer::new);
		ModelLayerRegistry.registerModelLayer(AmethystRobotModel.LAYER, AmethystRobotModel::createBodyLayer);
		EntityRenderers.register(ModContent.AMETHYST_ROBOT, AmethystRobotRenderer::new);
		ModelLayerRegistry.registerModelLayer(AmethystSpiderModel.LAYER, AmethystSpiderModel::createBodyLayer);
		EntityRenderers.register(dev.bartels.jtechstorage.end.EndContent.AMETHYST_SPIDER, AmethystSpiderRenderer::new);
		EntityRenderers.register(dev.bartels.jtechstorage.end.EndContent.WEB_SHOT, ThrownItemRenderer::new);
		ModelLayerRegistry.registerModelLayer(JbartelsHologramModel.LAYER, JbartelsHologramModel::createBodyLayer);
		EntityRenderers.register(ModContent.JBARTELS_HOLOGRAM, JbartelsHologramRenderer::new);

		// Rucksack auf dem Rücken
		ModelLayerRegistry.registerModelLayer(BackpackModel.LAYER, BackpackModel::createBodyLayer);
		// Abgestellte Rucksäcke: Stoff in der Farbe des Rucksacks tönen
		net.fabricmc.fabric.api.client.rendering.v1.BlockColorRegistry.register(java.util.List.of(
				new net.minecraft.client.color.block.BlockTintSource() {
					@Override
					public int color(net.minecraft.world.level.block.state.BlockState state) {
						return 0xFF000000 | state.getValue(dev.bartels.jtechstorage.backpack.PlacedBackpackBlock.TIER).defaultColor;
					}

					@Override
					public int colorInWorld(net.minecraft.world.level.block.state.BlockState state,
							net.minecraft.client.renderer.block.BlockAndTintGetter level, net.minecraft.core.BlockPos pos) {
						return level.getBlockEntity(pos) instanceof dev.bartels.jtechstorage.backpack.PlacedBackpackBlockEntity backpack
								? 0xFF000000 | backpack.fabricColor() : color(state);
					}
				}), ModContent.PLACED_BACKPACK);
		LivingEntityRenderLayerRegistrationCallback.EVENT.register((type, renderer, helper, context) -> {
			if (renderer instanceof AvatarRenderer<?> avatar) {
				helper.register(new BackpackLayer(avatar, new BackpackModel(context.bakeLayer(BackpackModel.LAYER))));
			}
		});
		GuideBookItem.openScreen = stack -> Minecraft.getInstance().setScreenAndShow(new GuideBookScreen(stack));
		dev.bartels.jtechstorage.item.FlightPlanItem.openScreen = hand -> {
			Minecraft client = Minecraft.getInstance();

			if (client.player != null) {
				client.setScreenAndShow(new FlightPlanScreen(hand, client.player.getItemInHand(hand)));
			}
		};

		ClientPlayNetworking.registerGlobalReceiver(dev.bartels.jtechstorage.net.ConfigSyncPayload.TYPE,
				(payload, context) -> dev.bartels.jtechstorage.ModConfig.applyRemote(payload.json()));
		net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.DISCONNECT.register(
				(handler, client) -> dev.bartels.jtechstorage.ModConfig.restoreLocal());
		ClientPlayNetworking.registerGlobalReceiver(ChelOrdersPayload.TYPE, (payload, context) -> {
			if (context.player().containerMenu instanceof ChelOrderMenu menu && menu.containerId == payload.containerId()) {
				menu.setClientData(payload.entries(), payload.status());
			}
		});

		ClientPlayNetworking.registerGlobalReceiver(NestStatePayload.TYPE, (payload, context) -> {
			if (context.player().containerMenu instanceof NestMenu menu && menu.containerId == payload.containerId()) {
				menu.setClientState(payload.state());
			}
		});

		ClientPlayNetworking.registerGlobalReceiver(TeamStatePayload.TYPE, (payload, context) -> {
			if (context.player().containerMenu instanceof TeamMenu menu && menu.containerId == payload.containerId()) {
				menu.setClientState(payload.state());
			}
		});

		ClientPlayNetworking.registerGlobalReceiver(dev.bartels.jtechstorage.farm.FarmPayloads.Farms.TYPE, (payload, context) -> {
			if (Minecraft.getInstance().gui.screen() instanceof dev.bartels.jtechstorage.client.farm.FarmsScreen farms && farms.containerId() == payload.containerId()) {
				farms.setFarms(payload.farms());
			}
		});
		ClientPlayNetworking.registerGlobalReceiver(dev.bartels.jtechstorage.net.TerminalStatsPayload.TYPE, (payload, context) -> {
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

	private static void addModTooltip(net.minecraft.world.item.ItemStack stack, java.util.List<net.minecraft.network.chat.Component> lines) {
		net.minecraft.resources.Identifier id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem());

		if (!dev.bartels.jtechstorage.JTechStorage.MOD_ID.equals(id.getNamespace())) {
			return;
		}

		String path = id.getPath().endsWith("_backpack") ? "backpack" : id.getPath();
		String base = "tooltip.jtechstorage." + path;

		if (!net.minecraft.locale.Language.getInstance().has(base + ".0")) {
			return;
		}

		java.util.List<net.minecraft.network.chat.Component> add = new java.util.ArrayList<>();

		for (int i = 0; net.minecraft.locale.Language.getInstance().has(base + "." + i); i++) {
			add.add(net.minecraft.network.chat.Component.translatable(base + "." + i).withStyle(net.minecraft.ChatFormatting.GRAY));
		}

		boolean helper = stack.getItem() instanceof dev.bartels.jtechstorage.item.HelperSpawnItem;
		boolean controls = net.minecraft.locale.Language.getInstance().has(base + ".controls.0");

		if (controls || helper) {
			if (net.minecraft.client.Minecraft.getInstance().hasShiftDown()) {
				for (int i = 0; net.minecraft.locale.Language.getInstance().has(base + ".controls." + i); i++) {
					add.add(net.minecraft.network.chat.Component.literal("• ").append(net.minecraft.network.chat.Component.translatable(base + ".controls." + i))
							.withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE));
				}

				if (helper) {
					add.add(net.minecraft.network.chat.Component.literal("• ").append(net.minecraft.network.chat.Component.translatable("tooltip.jtechstorage.pack"))
							.withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE));
				}
			} else {
				add.add(net.minecraft.network.chat.Component.translatable("tooltip.jtechstorage.hold_shift")
						.withStyle(net.minecraft.ChatFormatting.DARK_PURPLE, net.minecraft.ChatFormatting.ITALIC));
			}
		}

		lines.addAll(Math.min(1, lines.size()), add);
	}
}
