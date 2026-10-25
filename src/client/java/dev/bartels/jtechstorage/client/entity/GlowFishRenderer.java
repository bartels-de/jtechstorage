package dev.bartels.jtechstorage.client.entity;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

import dev.bartels.jtechstorage.JTechStorage;
import dev.bartels.jtechstorage.depths.entity.GlowFish;

/** Kristalltiefe (v0.31). Modell in doppelter Detaildichte, leuchtende Teile als eigene Schicht. */
public class GlowFishRenderer extends MobRenderer<GlowFish, GlowFishRenderState, GlowFishModel> {
	private static final Identifier TEXTURE = JTechStorage.id("textures/entity/glow_fish.png");
	private static final RenderType GLOW = RenderTypes.eyes(JTechStorage.id("textures/entity/glow_fish_glow.png"));

	public GlowFishRenderer(EntityRendererProvider.Context context) {
		super(context, new GlowFishModel(context.bakeLayer(GlowFishModel.LAYER)), 0.2f);
		addLayer(new EyesLayer<>(this) {
			@Override
			public RenderType renderType() {
				return GLOW;
			}
		});
	}

	@Override
	public Identifier getTextureLocation(GlowFishRenderState state) {
		return TEXTURE;
	}

	@Override
	public GlowFishRenderState createRenderState() {
		return new GlowFishRenderState();
	}

	@Override
	public void extractRenderState(GlowFish mob, GlowFishRenderState state, float partialTick) {
		super.extractRenderState(mob, state, partialTick);
	}

	@Override
	protected void scale(GlowFishRenderState state, PoseStack poseStack) {
		poseStack.scale(0.5f, 0.5f, 0.5f);   // Modell in doppelter Detaildichte
	}

	@Override
	protected void setupRotations(GlowFishRenderState state, PoseStack poseStack, float bodyRot, float scale) {
		super.setupRotations(state, poseStack, bodyRot, scale);
		poseStack.rotateDegrees(com.mojang.math.Axis.YP, 4.3f * Mth.sin(0.6f * state.ageInTicks));

		if (!state.isInWater) {
			poseStack.translate(0.1f, 0.1f, -0.1f);
			poseStack.rotateDegrees(com.mojang.math.Axis.ZP, 90.0f);
		}
	}
}
