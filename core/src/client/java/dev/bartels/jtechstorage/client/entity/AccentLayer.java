package dev.bartels.jtechstorage.client.entity;

import java.util.function.ToIntFunction;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

/**
 * Recolors the amethyst parts of a helper: a grayscale copy of the crystal pixels is laid over the model,
 * tinted with the dye color.
 */
public class AccentLayer<S extends LivingEntityRenderState, M extends EntityModel<S>> extends RenderLayer<S, M> {
	private final Identifier texture;
	private final ToIntFunction<S> color;

	public AccentLayer(RenderLayerParent<S, M> parent, Identifier texture, ToIntFunction<S> color) {
		super(parent);
		this.texture = texture;
		this.color = color;
	}

	@Override
	public void submit(PoseStack pose, SubmitNodeCollector collector, int light, S state, float yRot, float xRot) {
		int rgb = color.applyAsInt(state);

		if (rgb == -1 || state.isInvisible) {
			return;
		}

		renderColoredCutoutModel(getParentModel(), texture, pose, collector, light, state, 0xFF000000 | rgb, 1);
	}
}
