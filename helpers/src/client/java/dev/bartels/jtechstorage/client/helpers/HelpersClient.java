package dev.bartels.jtechstorage.client.helpers;

import com.mojang.blaze3d.platform.InputConstants;
import dev.bartels.jtechstorage.chest.AmethystChestTier;
import dev.bartels.jtechstorage.chest.ChestContent;
import dev.bartels.jtechstorage.client.*;
import dev.bartels.jtechstorage.client.chest.AmethystChestScreen;
import dev.bartels.jtechstorage.client.companion.CompanionClient;
import dev.bartels.jtechstorage.client.entity.AmethystPhoenixModel;
import dev.bartels.jtechstorage.client.entity.AmethystPhoenixRenderer;
import dev.bartels.jtechstorage.client.entity.AmethystSpriteModel;
import dev.bartels.jtechstorage.client.entity.AmethystSpriteRenderer;
import dev.bartels.jtechstorage.client.entity.AshleyModel;
import dev.bartels.jtechstorage.client.entity.AshleyRenderer;
import dev.bartels.jtechstorage.client.entity.ChelModel;
import dev.bartels.jtechstorage.client.entity.ChelRenderer;
import dev.bartels.jtechstorage.client.entity.CheldroganModel;
import dev.bartels.jtechstorage.client.entity.CheldroganRenderer;
import dev.bartels.jtechstorage.client.entity.CollectorMothModel;
import dev.bartels.jtechstorage.client.entity.CollectorMothRenderer;
import dev.bartels.jtechstorage.client.entity.CompanionRenderState;
import dev.bartels.jtechstorage.client.entity.CompanionRenderer;
import dev.bartels.jtechstorage.client.entity.CrowDecoyRenderer;
import dev.bartels.jtechstorage.client.entity.CrystalWolfModel;
import dev.bartels.jtechstorage.client.entity.CrystalWolfRenderer;
import dev.bartels.jtechstorage.client.entity.GeodeKeeperModel;
import dev.bartels.jtechstorage.client.entity.GeodeKeeperRenderer;
import dev.bartels.jtechstorage.client.entity.MantisModel1;
import dev.bartels.jtechstorage.client.entity.MantisModel2;
import dev.bartels.jtechstorage.client.entity.MantisModel3;
import dev.bartels.jtechstorage.client.entity.MoonWolfModel;
import dev.bartels.jtechstorage.client.entity.MoonWolfRenderer;
import dev.bartels.jtechstorage.client.entity.PeterChelParkerModel;
import dev.bartels.jtechstorage.client.entity.PeterChelParkerRenderer;
import dev.bartels.jtechstorage.client.entity.PhoenixModel;
import dev.bartels.jtechstorage.client.entity.PhoenixRenderer;
import dev.bartels.jtechstorage.client.entity.RavenModel1;
import dev.bartels.jtechstorage.client.entity.RavenModel2;
import dev.bartels.jtechstorage.client.entity.RavenModel3;
import dev.bartels.jtechstorage.client.entity.ShroomModel1;
import dev.bartels.jtechstorage.client.entity.ShroomModel2;
import dev.bartels.jtechstorage.client.entity.ShroomModel3;
import dev.bartels.jtechstorage.client.entity.SparkBeetleModel;
import dev.bartels.jtechstorage.client.entity.SparkBeetleRenderer;
import dev.bartels.jtechstorage.client.entity.StorageGriffinModel;
import dev.bartels.jtechstorage.client.entity.StorageGriffinRenderer;
import dev.bartels.jtechstorage.client.entity.SwarmRavenRenderer;
import dev.bartels.jtechstorage.companion.CompanionContent;
import dev.bartels.jtechstorage.entity.AmethystPhoenix;
import dev.bartels.jtechstorage.entity.Cheldrogan;
import dev.bartels.jtechstorage.helpers.HelpersContent;
import dev.bartels.jtechstorage.item.FlightPlanItem;
import dev.bartels.jtechstorage.menu.ChelOrderMenu;
import dev.bartels.jtechstorage.menu.NestMenu;
import dev.bartels.jtechstorage.net.ChelOrdersPayload;
import dev.bartels.jtechstorage.net.CompanionActionPayload;
import dev.bartels.jtechstorage.net.DragonActionPayload;
import dev.bartels.jtechstorage.net.NestStatePayload;
import dev.bartels.jtechstorage.net.PhoenixDepartPayload;
import dev.bartels.jtechstorage.wolf.WolfContent;
import java.util.List;
import java.util.function.Supplier;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.client.renderer.entity.NoopRenderer;

/** Client of the "Helpers &amp; Phoenix" sub-mod (v0.43). */
public class HelpersClient implements ClientModInitializer {
	public static final KeyMapping COMPANION_HEAL_KEY = KeyMappingHelper.registerKeyMapping(
			new KeyMapping("key.jtechstorage.companion_heal", InputConstants.KEY_H, JTechStorageClient.KEY_CATEGORY));
	public static final KeyMapping COMPANION_STAY_KEY = KeyMappingHelper.registerKeyMapping(
			new KeyMapping("key.jtechstorage.companion_stay", InputConstants.KEY_G, JTechStorageClient.KEY_CATEGORY));
	public static final KeyMapping COMPANION_MENU_KEY = KeyMappingHelper.registerKeyMapping(
			new KeyMapping("key.jtechstorage.companion_menu", InputConstants.KEY_K, JTechStorageClient.KEY_CATEGORY));
	public static final KeyMapping COMPANION_SPECIAL_KEY = KeyMappingHelper.registerKeyMapping(
			new KeyMapping("key.jtechstorage.companion_special", InputConstants.KEY_B, JTechStorageClient.KEY_CATEGORY));
	public static final KeyMapping DRAGON_BREATH_KEY = KeyMappingHelper.registerKeyMapping(
			new KeyMapping("key.jtechstorage.dragon_breath", InputConstants.KEY_R, JTechStorageClient.KEY_CATEGORY));
	public static final KeyMapping DRAGON_RAIN_KEY = KeyMappingHelper.registerKeyMapping(
			new KeyMapping("key.jtechstorage.dragon_rain", InputConstants.KEY_V, JTechStorageClient.KEY_CATEGORY));

	private static boolean jumpWasDown;
	private static boolean airJumpArmed;
	private static int airJumps;

	@Override
	public void onInitializeClient() {
		ModelLayerRegistry.registerModelLayer(MantisModel1.LAYER, MantisModel1::createBodyLayer);
		ModelLayerRegistry.registerModelLayer(MantisModel2.LAYER, MantisModel2::createBodyLayer);
		ModelLayerRegistry.registerModelLayer(MantisModel3.LAYER, MantisModel3::createBodyLayer);
		ModelLayerRegistry.registerModelLayer(ShroomModel1.LAYER, ShroomModel1::createBodyLayer);
		ModelLayerRegistry.registerModelLayer(ShroomModel2.LAYER, ShroomModel2::createBodyLayer);
		ModelLayerRegistry.registerModelLayer(ShroomModel3.LAYER, ShroomModel3::createBodyLayer);
		ModelLayerRegistry.registerModelLayer(RavenModel1.LAYER, RavenModel1::createBodyLayer);
		ModelLayerRegistry.registerModelLayer(RavenModel3.LAYER, RavenModel3::createBodyLayer);
		MenuScreens.register(HelpersContent.CHEL_ORDER_MENU, ChelOrderScreen::new);
		MenuScreens.register(HelpersContent.NEST_MENU, NestScreen::new);
		ModelLayerRegistry.registerModelLayer(PhoenixModel.LAYER, PhoenixModel::createBodyLayer);
		EntityRenderers.register(HelpersContent.PHOENIX, PhoenixRenderer::new);
		ModelLayerRegistry.registerModelLayer(AmethystPhoenixModel.LAYER,
				AmethystPhoenixModel::createBodyLayer);
		EntityRenderers.register(HelpersContent.AMETHYST_PHOENIX, AmethystPhoenixRenderer::new);
		MenuScreens.register(HelpersContent.AMETHYST_PHOENIX_MENU, AmethystPhoenixScreen::new);
		MenuScreens.register(HelpersContent.PHOENIX_STOP_MENU, PhoenixStopScreen::new);
		for (AmethystChestTier tier : AmethystChestTier.values()) {
			MenuScreens.register(ChestContent.menuType(tier), AmethystChestScreen::new);
		}
		// Wolves (v0.26)
		ModelLayerRegistry.registerModelLayer(CrystalWolfModel.LAYER,
				CrystalWolfModel::createBodyLayer);
		EntityRenderers.register(WolfContent.CRYSTAL_WOLF,
				CrystalWolfRenderer::new);
		ModelLayerRegistry.registerModelLayer(MoonWolfModel.LAYER,
				MoonWolfModel::createBodyLayer);
		EntityRenderers.register(WolfContent.MOON_WOLF,
				MoonWolfRenderer::new);
		ModelLayerRegistry.registerModelLayer(RavenModel2.LAYER, RavenModel2::createBodyLayer);
		ModelLayerRegistry.registerModelLayer(GeodeKeeperModel.LAYER, GeodeKeeperModel::createBodyLayer);
		EntityRenderers.register(CompanionContent.MANTIS, context -> new CompanionRenderer<>(context, "mantis", 0.6f, new float[] {1.0f, 1.0f, 1.2f},
				List.<Supplier<EntityModel<CompanionRenderState>>>of(() -> new MantisModel1(context.bakeLayer(MantisModel1.LAYER)), () -> new MantisModel2(context.bakeLayer(MantisModel2.LAYER)), () -> new MantisModel3(context.bakeLayer(MantisModel3.LAYER)))));
		EntityRenderers.register(CompanionContent.SHROOM, context -> new CompanionRenderer<>(context, "shroom", 0.5f, new float[] {1.0f, 1.0f, 1.0f},
				List.<Supplier<EntityModel<CompanionRenderState>>>of(() -> new ShroomModel1(context.bakeLayer(ShroomModel1.LAYER)), () -> new ShroomModel2(context.bakeLayer(ShroomModel2.LAYER)), () -> new ShroomModel3(context.bakeLayer(ShroomModel3.LAYER)))));
		EntityRenderers.register(CompanionContent.RAVEN, context -> new CompanionRenderer<>(context, "raven", 0.4f, new float[] {1.3f, 1.5f, 2.0f},
				List.<Supplier<EntityModel<CompanionRenderState>>>of(() -> new RavenModel1(context.bakeLayer(RavenModel1.LAYER)), () -> new RavenModel2(context.bakeLayer(RavenModel2.LAYER)), () -> new RavenModel3(context.bakeLayer(RavenModel3.LAYER)))));
		EntityRenderers.register(CompanionContent.SWARM_RAVEN, SwarmRavenRenderer::new);
		EntityRenderers.register(CompanionContent.CROW_DECOY, CrowDecoyRenderer::new);
		EntityRenderers.register(CompanionContent.GEODE_KEEPER, GeodeKeeperRenderer::new);
		ModelLayerRegistry.registerModelLayer(CheldroganModel.LAYER,
				CheldroganModel::createBodyLayer);
		EntityRenderers.register(HelpersContent.CHELDROGAN, CheldroganRenderer::new);
		CompanionClient.init();
		// Companions (v0.25.2): key H = mushroom heals now, Shift + H = toggle automatic/hotkey
		net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (COMPANION_STAY_KEY.consumeClick()) {
				if (client.player != null) {
					ClientPlayNetworking.send(new CompanionActionPayload(2));
				}
			}

			while (COMPANION_SPECIAL_KEY.consumeClick()) {
				if (client.player != null) {
					ClientPlayNetworking.send(new CompanionActionPayload(CompanionActionPayload.SPECIAL));
				}
			}

			while (COMPANION_MENU_KEY.consumeClick()) {
				if (client.player != null && client.gui.screen() == null) {
					CompanionClient.openFromKey(client);
				}
			}

			while (COMPANION_HEAL_KEY.consumeClick()) {
				if (client.player != null) {
					ClientPlayNetworking.send(new CompanionActionPayload(client.player.isShiftKeyDown() ? 1 : 0));
				}
			}
		});
		// Riding Cheldrogan: left click = tail strike, key (default R) = crystal breath, key (default V) = shard rain.
		// START_CLIENT_TICK runs before vanilla key handling; the clicks are consumed here.
		net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.START_CLIENT_TICK.register(client -> {
			if (client.player == null || !(client.player.getVehicle() instanceof Cheldrogan dragon)
					|| dragon.getControllingPassenger() != client.player) {
				return;
			}

			while (client.options.keyAttack.consumeClick()) {
				ClientPlayNetworking.send(new DragonActionPayload(Cheldrogan.ACTION_CLAW));
			}

			while (DRAGON_BREATH_KEY.consumeClick()) {
				ClientPlayNetworking.send(new DragonActionPayload(Cheldrogan.ACTION_BREATH));
			}

			while (DRAGON_RAIN_KEY.consumeClick()) {
				ClientPlayNetworking.send(new DragonActionPayload(Cheldrogan.ACTION_RAIN));
			}
		});
		// Jump key on the front seat of the Amethyst Phoenix: take off early
		net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(client -> {
			boolean down = client.player != null && client.options.keyJump.isDown()
					&& client.player.getVehicle() instanceof AmethystPhoenix phoenix
					&& phoenix.getFirstPassenger() == client.player;

			if (down && !jumpWasDown) {
				ClientPlayNetworking.send(new PhoenixDepartPayload(client.player.getVehicle().getId()));
			}

			jumpWasDown = down;
		});
		ModelLayerRegistry.registerModelLayer(AmethystSpriteModel.LAYER, AmethystSpriteModel::createBodyLayer);
		EntityRenderers.register(HelpersContent.AMETHYST_SPRITE, AmethystSpriteRenderer::new);
		ModelLayerRegistry.registerModelLayer(CollectorMothModel.LAYER, CollectorMothModel::createBodyLayer);
		EntityRenderers.register(HelpersContent.COLLECTOR_MOTH, CollectorMothRenderer::new);
		ModelLayerRegistry.registerModelLayer(StorageGriffinModel.LAYER, StorageGriffinModel::createBodyLayer);
		EntityRenderers.register(HelpersContent.STORAGE_GRIFFIN, StorageGriffinRenderer::new);
		ModelLayerRegistry.registerModelLayer(SparkBeetleModel.LAYER, SparkBeetleModel::createBodyLayer);
		EntityRenderers.register(HelpersContent.SPARK_BEETLE, SparkBeetleRenderer::new);
		ModelLayerRegistry.registerModelLayer(ChelModel.LAYER, ChelModel::createBodyLayer);
		EntityRenderers.register(HelpersContent.CHEL, ChelRenderer::new);
		ModelLayerRegistry.registerModelLayer(PeterChelParkerModel.LAYER, PeterChelParkerModel::createBodyLayer);
		EntityRenderers.register(HelpersContent.PETER_CHEL_PARKER, PeterChelParkerRenderer::new);
		EntityRenderers.register(HelpersContent.CRYSTAL_PALADIN, NoopRenderer::new);   // old paladins immediately become Peter
		ModelLayerRegistry.registerModelLayer(AshleyModel.LAYER, AshleyModel::createBodyLayer);
		EntityRenderers.register(HelpersContent.ASHLEY, AshleyRenderer::new);
		FlightPlanItem.openScreen = hand -> {
			Minecraft client = Minecraft.getInstance();

			if (client.player != null) {
				client.setScreenAndShow(new FlightPlanScreen(hand, client.player.getItemInHand(hand)));
			}
		};
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
	}
}
