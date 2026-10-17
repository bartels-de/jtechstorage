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
		GuideBookItem.openScreen = () -> Minecraft.getInstance().setScreenAndShow(new GuideBookScreen());

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

		ClientPlayNetworking.registerGlobalReceiver(TerminalContentsPayload.TYPE, (payload, context) -> {
			if (context.player().containerMenu instanceof StorageTerminalMenu menu
					&& menu.containerId == payload.containerId()) {
				menu.setClientContents(payload.entries(), payload.info(), payload.access());
			}
		});
	}
}
