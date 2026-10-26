package dev.bartels.jtechstorage.client.entity;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

import dev.bartels.jtechstorage.JTechStorage;
import dev.bartels.jtechstorage.depths.entity.CrystalDeer;

/** Crystal Depths (v0.36). Model at double detail density, glowing parts as a separate layer. */
public class CrystalDeerRenderer extends MobRenderer<CrystalDeer, CrystalDeerRenderState, CrystalDeerModel> {
	private static final Identifier TEXTURE = JTechStorage.id("textures/entity/crystal_deer.png");
	private static final RenderType GLOW = RenderTypes.eyes(JTechStorage.id("textures/entity/crystal_deer_glow.png"));

	public CrystalDeerRenderer(EntityRendererProvider.Context context) {
		super(context, new CrystalDeerModel(context.bakeLayer(CrystalDeerModel.LAYER)), 0.8f);
		addLayer(new EyesLayer<>(this) {
			@Override
			public RenderType renderType() {
				return GLOW;
			}
		});
	}

	@Override
	public Identifier getTextureLocation(CrystalDeerRenderState state) {
		return TEXTURE;
	}

	@Override
	public CrystalDeerRenderState createRenderState() {
		return new CrystalDeerRenderState();
	}

	@Override
	public void extractRenderState(CrystalDeer mob, CrystalDeerRenderState state, float partialTick) {
		super.extractRenderState(mob, state, partialTick);
		state.saddled = mob.isSaddled();
		state.graze = mob.getEatAnim(partialTick);
		state.rear = mob.getStandAnim(partialTick);
	}

	@Override
	protected void scale(CrystalDeerRenderState state, PoseStack poseStack) {
		float s = 0.5f * state.ageScale;   // model at double detail density, young ones half the size
		poseStack.scale(s, s, s);
	}
}
