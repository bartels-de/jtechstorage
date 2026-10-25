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
import dev.bartels.jtechstorage.depths.entity.CrystalSnail;

/** Kristalltiefe (v0.31). Modell in doppelter Detaildichte, leuchtende Teile als eigene Schicht. */
public class CrystalSnailRenderer extends MobRenderer<CrystalSnail, CrystalSnailRenderState, CrystalSnailModel> {
	private static final Identifier TEXTURE = JTechStorage.id("textures/entity/crystal_snail.png");
	private static final RenderType GLOW = RenderTypes.eyes(JTechStorage.id("textures/entity/crystal_snail_glow.png"));

	public CrystalSnailRenderer(EntityRendererProvider.Context context) {
		super(context, new CrystalSnailModel(context.bakeLayer(CrystalSnailModel.LAYER)), 0.35f);
		addLayer(new EyesLayer<>(this) {
			@Override
			public RenderType renderType() {
				return GLOW;
			}
		});
	}

	@Override
	public Identifier getTextureLocation(CrystalSnailRenderState state) {
		return TEXTURE;
	}

	@Override
	public CrystalSnailRenderState createRenderState() {
		return new CrystalSnailRenderState();
	}

	@Override
	public void extractRenderState(CrystalSnail mob, CrystalSnailRenderState state, float partialTick) {
		super.extractRenderState(mob, state, partialTick);
		state.hide = Math.min(1.0f, mob.getHideTicks() / 10.0f);
	}

	@Override
	protected void scale(CrystalSnailRenderState state, PoseStack poseStack) {
		poseStack.scale(0.5f, 0.5f, 0.5f);   // Modell in doppelter Detaildichte
	}
}
