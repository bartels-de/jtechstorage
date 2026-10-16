package dev.bartels.jtechstorage.client.entity;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;

import dev.bartels.jtechstorage.JTechStorage;
import dev.bartels.jtechstorage.backpack.BackpackItem;

/** Zeigt einen im Brust-Slot getragenen Rucksack auf dem Rücken des Spielers (Stoff getönt + Säume je Stufe). */
public class BackpackLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
	private static final Identifier FABRIC = JTechStorage.id("textures/entity/backpack/backpack_fabric.png");

	private final BackpackModel model;

	public BackpackLayer(RenderLayerParent<AvatarRenderState, PlayerModel> parent, BackpackModel model) {
		super(parent);
		this.model = model;
	}

	@Override
	public void submit(PoseStack pose, SubmitNodeCollector collector, int light, AvatarRenderState state, float yRot, float xRot) {
		ItemStack stack = state.chestEquipment;

		if (!(stack.getItem() instanceof BackpackItem backpack) || state.isInvisible) {
			return;
		}

		BackpackItem.Tier tier = backpack.getTier();
		DyedItemColor dyed = stack.get(DataComponents.DYED_COLOR);
		int color = 0xFF000000 | (dyed != null ? dyed.rgb() : tier.defaultColor);
		Identifier trim = JTechStorage.id("textures/entity/backpack/backpack_" + tier.name().toLowerCase(java.util.Locale.ROOT) + ".png");

		pose.pushPose();
		getParentModel().root().translateAndRotate(pose);
		getParentModel().body.translateAndRotate(pose);
		renderColoredCutoutModel(model, FABRIC, pose, collector, light, state, color, 1);
		renderColoredCutoutModel(model, trim, pose, collector, light, state, 0xFFFFFFFF, 2);
		pose.popPose();
	}
}
