package dev.bartels.jtechstorage.client.entity;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

import dev.bartels.jtechstorage.JTechStorage;
import dev.bartels.jtechstorage.depths.entity.Quokka;

/** Crystal Depths (v0.31). Model at double detail density, glowing parts as a separate layer. */
public class QuokkaRenderer extends MobRenderer<Quokka, QuokkaRenderState, QuokkaModel> {
	private static final Identifier TEXTURE = JTechStorage.id("textures/entity/quokka.png");
	private static final RenderType GLOW = RenderTypes.eyes(JTechStorage.id("textures/entity/quokka_glow.png"));

	public QuokkaRenderer(EntityRendererProvider.Context context) {
		super(context, new QuokkaModel(context.bakeLayer(QuokkaModel.LAYER)), 0.35f);
		addLayer(new EyesLayer<>(this) {
			@Override
			public RenderType renderType() {
				return GLOW;
			}
		});
	}

	@Override
	public Identifier getTextureLocation(QuokkaRenderState state) {
		return TEXTURE;
	}

	@Override
	public QuokkaRenderState createRenderState() {
		return new QuokkaRenderState();
	}

	@Override
	public void extractRenderState(Quokka mob, QuokkaRenderState state, float partialTick) {
		super.extractRenderState(mob, state, partialTick);
		state.sitting = mob.isInSittingPose();
		state.hop = mob.getHopTicks() > 0 ? 1.0f - (mob.getHopTicks() - partialTick) / Quokka.HOP_TICKS : 0.0f;
	}

	@Override
	protected void scale(QuokkaRenderState state, PoseStack poseStack) {
		poseStack.scale(0.5f, 0.5f, 0.5f);   // model at double detail density
	}
}
