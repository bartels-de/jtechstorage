package dev.bartels.jtechstorage.client.entity;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

import dev.bartels.jtechstorage.JTechStorage;
import dev.bartels.jtechstorage.depths.entity.LakeWraith;

/** Crystal Depths (v0.31). Model at double detail density, glowing parts as a separate layer. */
public class LakeWraithRenderer extends MobRenderer<LakeWraith, LakeWraithRenderState, LakeWraithModel> {
	private static final Identifier TEXTURE = JTechStorage.id("textures/entity/lake_wraith.png");
	private static final RenderType GLOW = RenderTypes.eyes(JTechStorage.id("textures/entity/lake_wraith_glow.png"));

	public LakeWraithRenderer(EntityRendererProvider.Context context) {
		super(context, new LakeWraithModel(context.bakeLayer(LakeWraithModel.LAYER)), 0.6f);
		addLayer(new EyesLayer<>(this) {
			@Override
			public RenderType renderType() {
				return GLOW;
			}
		});
	}

	@Override
	public Identifier getTextureLocation(LakeWraithRenderState state) {
		return TEXTURE;
	}

	@Override
	public LakeWraithRenderState createRenderState() {
		return new LakeWraithRenderState();
	}

	@Override
	public void extractRenderState(LakeWraith mob, LakeWraithRenderState state, float partialTick) {
		super.extractRenderState(mob, state, partialTick);
		state.grab = mob.getGrabTicks() > 0 ? 1.0f - (mob.getGrabTicks() - partialTick) / LakeWraith.GRAB_TICKS : 0.0f;
	}

	@Override
	protected void scale(LakeWraithRenderState state, PoseStack poseStack) {
		poseStack.scale(0.5f, 0.5f, 0.5f);   // model at double detail density
	}
}
