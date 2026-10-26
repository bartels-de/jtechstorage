package dev.bartels.jtechstorage.client.adventure;

import dev.bartels.jtechstorage.client.*;
import dev.bartels.jtechstorage.client.entity.AmethystRobotModel;
import dev.bartels.jtechstorage.client.entity.AmethystRobotRenderer;
import dev.bartels.jtechstorage.client.entity.AmethystSpiderModel;
import dev.bartels.jtechstorage.client.entity.AmethystSpiderRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;

/** Client of the Adventure sub-mod (v0.43). */
public class AdventureClient implements ClientModInitializer {

	private static boolean jumpWasDown;
	private static boolean airJumpArmed;
	private static int airJumps;

	@Override
	public void onInitializeClient() {
		ModelLayerRegistry.registerModelLayer(dev.bartels.jtechstorage.client.entity.AmethystFoxModel.LAYER,
				dev.bartels.jtechstorage.client.entity.AmethystFoxModel::createBodyLayer);
		EntityRenderers.register(dev.bartels.jtechstorage.adventure.AdventureContent.AMETHYST_FOX, dev.bartels.jtechstorage.client.entity.AmethystFoxRenderer::new);
		MenuScreens.register(dev.bartels.jtechstorage.forge.ForgeContent.MENU, dev.bartels.jtechstorage.client.forge.ForgeScreen::new);
		// Resonance Storm (v0.25)
		ModelLayerRegistry.registerModelLayer(dev.bartels.jtechstorage.client.entity.CrystalShardlingModel.LAYER,
				dev.bartels.jtechstorage.client.entity.CrystalShardlingModel::createBodyLayer);
		ModelLayerRegistry.registerModelLayer(dev.bartels.jtechstorage.client.entity.CrystalLurkerModel.LAYER, dev.bartels.jtechstorage.client.entity.CrystalLurkerModel::createBodyLayer);
		EntityRenderers.register(dev.bartels.jtechstorage.depths.DepthsContent.CRYSTAL_LURKER, dev.bartels.jtechstorage.client.entity.CrystalLurkerRenderer::new);
		ModelLayerRegistry.registerModelLayer(dev.bartels.jtechstorage.client.entity.SwarmShardModel.LAYER, dev.bartels.jtechstorage.client.entity.SwarmShardModel::createBodyLayer);
		EntityRenderers.register(dev.bartels.jtechstorage.depths.DepthsContent.SWARM_SHARD, dev.bartels.jtechstorage.client.entity.SwarmShardRenderer::new);
		ModelLayerRegistry.registerModelLayer(dev.bartels.jtechstorage.client.entity.LakeWraithModel.LAYER, dev.bartels.jtechstorage.client.entity.LakeWraithModel::createBodyLayer);
		EntityRenderers.register(dev.bartels.jtechstorage.depths.DepthsContent.LAKE_WRAITH, dev.bartels.jtechstorage.client.entity.LakeWraithRenderer::new);
		ModelLayerRegistry.registerModelLayer(dev.bartels.jtechstorage.client.entity.VeinCrawlerModel.LAYER, dev.bartels.jtechstorage.client.entity.VeinCrawlerModel::createBodyLayer);
		EntityRenderers.register(dev.bartels.jtechstorage.depths.DepthsContent.VEIN_CRAWLER, dev.bartels.jtechstorage.client.entity.VeinCrawlerRenderer::new);
		ModelLayerRegistry.registerModelLayer(dev.bartels.jtechstorage.client.entity.CrystalSnailModel.LAYER, dev.bartels.jtechstorage.client.entity.CrystalSnailModel::createBodyLayer);
		EntityRenderers.register(dev.bartels.jtechstorage.depths.DepthsContent.CRYSTAL_SNAIL, dev.bartels.jtechstorage.client.entity.CrystalSnailRenderer::new);
		ModelLayerRegistry.registerModelLayer(dev.bartels.jtechstorage.client.entity.GlowFishModel.LAYER, dev.bartels.jtechstorage.client.entity.GlowFishModel::createBodyLayer);
		EntityRenderers.register(dev.bartels.jtechstorage.depths.DepthsContent.GLOW_FISH, dev.bartels.jtechstorage.client.entity.GlowFishRenderer::new);
		ModelLayerRegistry.registerModelLayer(dev.bartels.jtechstorage.client.entity.QuokkaModel.LAYER, dev.bartels.jtechstorage.client.entity.QuokkaModel::createBodyLayer);
		EntityRenderers.register(dev.bartels.jtechstorage.depths.DepthsContent.QUOKKA, dev.bartels.jtechstorage.client.entity.QuokkaRenderer::new);
		// v0.36: new animals of the Crystal Depths
		ModelLayerRegistry.registerModelLayer(dev.bartels.jtechstorage.client.entity.CrystalDeerModel.LAYER, dev.bartels.jtechstorage.client.entity.CrystalDeerModel::createBodyLayer);
		ModelLayerRegistry.registerModelLayer(dev.bartels.jtechstorage.client.entity.GeodeSheepModel.LAYER, dev.bartels.jtechstorage.client.entity.GeodeSheepModel::createBodyLayer);
		ModelLayerRegistry.registerModelLayer(dev.bartels.jtechstorage.client.entity.GeodeSheepWoolModel.LAYER, dev.bartels.jtechstorage.client.entity.GeodeSheepWoolModel::createBodyLayer);
		ModelLayerRegistry.registerModelLayer(dev.bartels.jtechstorage.client.entity.CrystalCrabModel.LAYER, dev.bartels.jtechstorage.client.entity.CrystalCrabModel::createBodyLayer);
		ModelLayerRegistry.registerModelLayer(dev.bartels.jtechstorage.client.entity.LastMasterModel.LAYER, dev.bartels.jtechstorage.client.entity.LastMasterModel::createBodyLayer);
		EntityRenderers.register(dev.bartels.jtechstorage.depths.DepthsAnimals.CRYSTAL_DEER, dev.bartels.jtechstorage.client.entity.CrystalDeerRenderer::new);
		EntityRenderers.register(dev.bartels.jtechstorage.depths.DepthsAnimals.GEODE_SHEEP, dev.bartels.jtechstorage.client.entity.GeodeSheepRenderer::new);
		EntityRenderers.register(dev.bartels.jtechstorage.depths.DepthsAnimals.CRYSTAL_CRAB, dev.bartels.jtechstorage.client.entity.CrystalCrabRenderer::new);
		EntityRenderers.register(dev.bartels.jtechstorage.depths.temple.TempleContent.LAST_MASTER, dev.bartels.jtechstorage.client.entity.LastMasterRenderer::new);
		EntityRenderers.register(dev.bartels.jtechstorage.storm.StormContent.CRYSTAL_SHARDLING,
				dev.bartels.jtechstorage.client.entity.CrystalShardlingRenderer::new);
		ModelLayerRegistry.registerModelLayer(dev.bartels.jtechstorage.client.entity.StormHeraldModel.LAYER,
				dev.bartels.jtechstorage.client.entity.StormHeraldModel::createBodyLayer);
		EntityRenderers.register(dev.bartels.jtechstorage.storm.StormContent.STORM_HERALD,
				dev.bartels.jtechstorage.client.entity.StormHeraldRenderer::new);
		// Double Jump boots: press the jump key again in mid-air (the client computes the local player's movement)
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
		ModelLayerRegistry.registerModelLayer(AmethystRobotModel.LAYER, AmethystRobotModel::createBodyLayer);
		EntityRenderers.register(dev.bartels.jtechstorage.adventure.AdventureContent.AMETHYST_ROBOT, AmethystRobotRenderer::new);
		ModelLayerRegistry.registerModelLayer(AmethystSpiderModel.LAYER, AmethystSpiderModel::createBodyLayer);
		EntityRenderers.register(dev.bartels.jtechstorage.end.EndContent.AMETHYST_SPIDER, AmethystSpiderRenderer::new);
		EntityRenderers.register(dev.bartels.jtechstorage.end.EndContent.WEB_SHOT, ThrownItemRenderer::new);
		net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry.register(dev.bartels.jtechstorage.depths.memory.MemoryContent.MEMORY_CASE_BLOCK_ENTITY,
				MemoryCaseRenderer::new);
		dev.bartels.jtechstorage.depths.AncientNoteItem.openScreen = n -> Minecraft.getInstance().setScreenAndShow(new dev.bartels.jtechstorage.client.gui.NoteScreen(n));
		// Crystal Forge upgrades in the tooltip
		net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback.EVENT.register((stack, context, flag, lines) -> {
		java.util.Map<String, Integer> forged = stack.get(dev.bartels.jtechstorage.forge.ForgeContent.UPGRADES);

			if (forged != null) {
				for (dev.bartels.jtechstorage.forge.ForgeUpgrade up : dev.bartels.jtechstorage.forge.ForgeUpgrade.values()) {
					int lvl = forged.getOrDefault(up.id(), 0);

					if (lvl > 0) {
						lines.add(Math.min(1, lines.size()), net.minecraft.network.chat.Component.translatable("jtechstorage.forge.upgrade." + up.id())
								.append(up.maxLevel > 1 ? " " + "I".repeat(lvl) : "").withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE));
					}
				}
			}

		});
	}
}
