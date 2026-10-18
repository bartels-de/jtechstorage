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
import dev.bartels.jtechstorage.entity.StormHerald;

public class StormHeraldRenderer extends MobRenderer<StormHerald, StormHeraldRenderState, StormHeraldModel> {
	private static final Identifier TEXTURE = JTechStorage.id("textures/entity/storm_herald.png");
	private static final RenderType GLOW = RenderTypes.eyes(JTechStorage.id("textures/entity/storm_herald_glow.png"));

	public StormHeraldRenderer(EntityRendererProvider.Context context) {
		super(context, new StormHeraldModel(context.bakeLayer(StormHeraldModel.LAYER)), 0.9f);
		addLayer(new EyesLayer<>(this) {
			@Override
			public RenderType renderType() {
				return GLOW;
			}
		});
	}

	@Override
	public Identifier getTextureLocation(StormHeraldRenderState state) {
		return TEXTURE;
	}

	@Override
	public StormHeraldRenderState createRenderState() {
		return new StormHeraldRenderState();
	}

	@Override
	public void extractRenderState(StormHerald herald, StormHeraldRenderState state, float partialTick) {
		super.extractRenderState(herald, state, partialTick);
		state.anim = herald.getAnim();
		int total = state.anim == StormHerald.ANIM_BOLT ? StormHerald.BOLT_TICKS : StormHerald.SUMMON_TICKS;
		state.progress = state.anim == StormHerald.ANIM_NONE ? 0.0f
				: Mth.clamp(1.0f - (herald.getAnimTicks() - partialTick) / total, 0.0f, 1.0f);
	}

	@Override
	protected void scale(StormHeraldRenderState state, PoseStack poseStack) {
		poseStack.scale(0.6f, 0.6f, 0.6f);   // Modell in doppelter Detaildichte
	}
}
