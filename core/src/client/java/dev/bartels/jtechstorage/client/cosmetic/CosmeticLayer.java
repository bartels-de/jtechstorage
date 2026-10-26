package dev.bartels.jtechstorage.client.cosmetic;

import java.util.EnumMap;
import java.util.Map;

import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import org.jspecify.annotations.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import dev.bartels.jtechstorage.JTechStorage;
import dev.bartels.jtechstorage.cosmetic.Cosmetic;
import dev.bartels.jtechstorage.cosmetic.Cosmetics;

/**
 * Renders equipped cosmetics (back, head, shoulder) on players (v0.34). Models use double detail density,
 * glowing parts as a separate layer. The cosmetics come from the player's synced attachment.
 */
public class CosmeticLayer extends RenderLayer<AvatarRenderState, PlayerModel> {

	private record Entry(EntityModel<AvatarRenderState> model, Identifier texture, @Nullable Identifier glow, boolean head) {
	}

	private final Map<Cosmetic, Entry> entries = new EnumMap<>(Cosmetic.class);

	public CosmeticLayer(RenderLayerParent<AvatarRenderState, PlayerModel> parent, EntityRendererProvider.Context context) {
		super(parent);
		put(Cosmetic.CRYSTAL_WINGS, new CrystalWingsModel(context.bakeLayer(CrystalWingsModel.LAYER)), "crystal_wings", false);
		put(Cosmetic.PHOENIX_WINGS, new PhoenixWingsModel(context.bakeLayer(PhoenixWingsModel.LAYER)), "phoenix_wings", false);
		put(Cosmetic.STORAGE_CAPE, new StorageCapeModel(context.bakeLayer(StorageCapeModel.LAYER)), "storage_cape", false);
		put(Cosmetic.PRISM_ORBS, new PrismOrbsModel(context.bakeLayer(PrismOrbsModel.LAYER)), "prism_orbs", false);
		put(Cosmetic.CRYSTAL_HALO, new CrystalHaloModel(context.bakeLayer(CrystalHaloModel.LAYER)), "crystal_halo", true);
		put(Cosmetic.STORAGE_CROWN, new StorageCrownModel(context.bakeLayer(StorageCrownModel.LAYER)), "storage_crown", true);
		put(Cosmetic.SHROOM_HAT, new ShroomHatModel(context.bakeLayer(ShroomHatModel.LAYER)), "shroom_hat", true);
		put(Cosmetic.WANDERER_HAT, new WandererHatModel(context.bakeLayer(WandererHatModel.LAYER)), "wanderer_hat", true);
		put(Cosmetic.SHOULDER_QUOKKA, new ShoulderQuokkaModel(context.bakeLayer(ShoulderQuokkaModel.LAYER)), "shoulder_quokka", false);
		put(Cosmetic.SHOULDER_SPRITE, new ShoulderSpriteModel(context.bakeLayer(ShoulderSpriteModel.LAYER)), "shoulder_sprite", false);
	}

	private void put(Cosmetic cosmetic, EntityModel<AvatarRenderState> model, String name, boolean head) {
		entries.put(cosmetic, new Entry(model, JTechStorage.id("textures/entity/cosmetic/" + name + ".png"),
				JTechStorage.id("textures/entity/cosmetic/" + name + "_glow.png"), head));
	}

	/** All model layers the client has to register. */
	public static void registerLayers() {
		ModelLayerRegistry.registerModelLayer(CrystalWingsModel.LAYER, CrystalWingsModel::createBodyLayer);
		ModelLayerRegistry.registerModelLayer(PhoenixWingsModel.LAYER, PhoenixWingsModel::createBodyLayer);
		ModelLayerRegistry.registerModelLayer(StorageCapeModel.LAYER, StorageCapeModel::createBodyLayer);
		ModelLayerRegistry.registerModelLayer(PrismOrbsModel.LAYER, PrismOrbsModel::createBodyLayer);
		ModelLayerRegistry.registerModelLayer(CrystalHaloModel.LAYER, CrystalHaloModel::createBodyLayer);
		ModelLayerRegistry.registerModelLayer(StorageCrownModel.LAYER, StorageCrownModel::createBodyLayer);
		ModelLayerRegistry.registerModelLayer(ShroomHatModel.LAYER, ShroomHatModel::createBodyLayer);
		ModelLayerRegistry.registerModelLayer(WandererHatModel.LAYER, WandererHatModel::createBodyLayer);
		ModelLayerRegistry.registerModelLayer(ShoulderQuokkaModel.LAYER, ShoulderQuokkaModel::createBodyLayer);
		ModelLayerRegistry.registerModelLayer(ShoulderSpriteModel.LAYER, ShoulderSpriteModel::createBodyLayer);
	}

	@Override
	public void submit(PoseStack pose, SubmitNodeCollector collector, int light, AvatarRenderState state, float yRot, float xRot) {
		if (state.isInvisible || state.isSpectator) {
			return;
		}

		Player player = player(state.id);

		if (player == null) {
			return;
		}

		int order = 3;

		for (Cosmetic.Slot slot : new Cosmetic.Slot[] {Cosmetic.Slot.BACK, Cosmetic.Slot.HEAD, Cosmetic.Slot.SHOULDER}) {
			Cosmetic cosmetic = Cosmetics.equipped(player, slot);
			Entry entry = cosmetic == null ? null : entries.get(cosmetic);

			if (entry == null) {
				continue;
			}

			// No hats over a worn helmet or head item
			if (entry.head && cosmetic != Cosmetic.CRYSTAL_HALO && !state.headEquipment.isEmpty()) {
				continue;
			}

			ModelPart anchor = entry.head ? getParentModel().head : getParentModel().body;
			pose.pushPose();
			getParentModel().root().translateAndRotate(pose);
			anchor.translateAndRotate(pose);
			pose.scale(0.5f, 0.5f, 0.5f);   // model uses double detail density
			renderColoredCutoutModel(entry.model, entry.texture, pose, collector, light, state, 0xFFFFFFFF, order);

			if (entry.glow != null) {
				collector.order(order + 1).submitModel(entry.model, state, pose, RenderTypes.eyes(entry.glow), light,
						OverlayTexture.NO_OVERLAY, state.outlineColor);
			}

			pose.popPose();
			order += 2;
		}
	}

	private static @Nullable Player player(int id) {
		var level = Minecraft.getInstance().level;
		Entity entity = level == null ? null : level.getEntity(id);
		return entity instanceof Player p ? p : null;
	}
}
