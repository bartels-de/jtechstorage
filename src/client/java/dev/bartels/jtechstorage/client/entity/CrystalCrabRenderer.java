package dev.bartels.jtechstorage.client.entity;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

import dev.bartels.jtechstorage.JTechStorage;
import dev.bartels.jtechstorage.depths.entity.CrystalCrab;

/** Kristalltiefe (v0.36). Modell in doppelter Detaildichte, leuchtende Teile als eigene Schicht. */
public class CrystalCrabRenderer extends MobRenderer<CrystalCrab, CrystalCrabRenderState, CrystalCrabModel> {
	private static final Identifier TEXTURE = JTechStorage.id("textures/entity/crystal_crab.png");
	private static final RenderType GLOW = RenderTypes.eyes(JTechStorage.id("textures/entity/crystal_crab_glow.png"));

	public CrystalCrabRenderer(EntityRendererProvider.Context context) {
		super(context, new CrystalCrabModel(context.bakeLayer(CrystalCrabModel.LAYER)), 0.45f);
		addLayer(new EyesLayer<>(this) {
			@Override
			public RenderType renderType() {
				return GLOW;
			}
		});
	}

	@Override
	public Identifier getTextureLocation(CrystalCrabRenderState state) {
		return TEXTURE;
	}

	@Override
	public CrystalCrabRenderState createRenderState() {
		return new CrystalCrabRenderState();
	}

	@Override
	public void extractRenderState(CrystalCrab mob, CrystalCrabRenderState state, float partialTick) {
		super.extractRenderState(mob, state, partialTick);
		state.sitting = mob.isInSittingPose();
		state.dig = mob.getDigTicks() > 0 ? 1.0f - (mob.getDigTicks() - partialTick) / dev.bartels.jtechstorage.depths.entity.CrystalCrab.DIG_TICKS : 0.0f;
	}

	@Override
	protected void scale(CrystalCrabRenderState state, PoseStack poseStack) {
		float s = 0.5f * state.ageScale;   // Modell in doppelter Detaildichte, Junge halb so groß
		poseStack.scale(s, s, s);
	}
}
