package dev.bartels.jtechstorage.client.entity;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

import dev.bartels.jtechstorage.JTechStorage;
import dev.bartels.jtechstorage.companion.SwarmRaven;

/** Schattenrabe aus dem Rabenschwarm (v0.25.3): das Nachtraben-Modell der Stufe 2 im Flug, etwas kleiner. */
public class SwarmRavenRenderer extends MobRenderer<SwarmRaven, CompanionRenderState, EntityModel<CompanionRenderState>> {
	private static final Identifier TEXTURE = JTechStorage.id("textures/entity/companion/raven_2.png");
	private static final RenderType GLOW = RenderTypes.eyes(JTechStorage.id("textures/entity/companion/raven_2_glow.png"));

	public SwarmRavenRenderer(EntityRendererProvider.Context context) {
		super(context, new RavenModel2(context.bakeLayer(RavenModel2.LAYER)), 0.0f);
		addLayer(new EyesLayer<>(this) {
			@Override
			public RenderType renderType() {
				return GLOW;
			}
		});
	}

	@Override
	public Identifier getTextureLocation(CompanionRenderState state) {
		return TEXTURE;
	}

	@Override
	public CompanionRenderState createRenderState() {
		return new CompanionRenderState();
	}

	@Override
	public void extractRenderState(SwarmRaven raven, CompanionRenderState state, float partialTick) {
		super.extractRenderState(raven, state, partialTick);
		state.stage = 2;
		state.flying = true;
	}

	@Override
	protected void scale(CompanionRenderState state, PoseStack poseStack) {
		poseStack.scale(0.5f, 0.5f, 0.5f);
	}
}
