package dev.bartels.jtechstorage.client.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;

/** Renders the item in the head slot as a hat (cosmetic for network helpers). */
public class HelperHatLayer<S extends LivingEntityRenderState, M extends EntityModel<S> & HelperHatModel> extends RenderLayer<S, M> {
	public HelperHatLayer(RenderLayerParent<S, M> parent) {
		super(parent);
	}

	@Override
	public void submit(PoseStack pose, SubmitNodeCollector collector, int light, S state, float yRot, float xRot) {
		if (state.headItem.isEmpty() || state.isInvisible) {
			return;
		}

		pose.pushPose();
		getParentModel().translateToHat(pose);
		float scale = getParentModel().hatScale();
		pose.scale(scale, scale, scale);
		pose.translate(0.0f, -0.25f, 0.0f);
		pose.rotate(Axis.YP, Mth.PI);
		pose.scale(0.625f, -0.625f, -0.625f);
		state.headItem.submit(pose, collector, light, OverlayTexture.NO_OVERLAY, state.outlineColor);
		pose.popPose();
	}
}
