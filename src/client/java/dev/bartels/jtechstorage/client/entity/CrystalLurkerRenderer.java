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
import dev.bartels.jtechstorage.depths.entity.CrystalLurker;

/** Kristalltiefe (v0.31). Modell in doppelter Detaildichte, leuchtende Teile als eigene Schicht. */
public class CrystalLurkerRenderer extends MobRenderer<CrystalLurker, CrystalLurkerRenderState, CrystalLurkerModel> {
	private static final Identifier TEXTURE = JTechStorage.id("textures/entity/crystal_lurker.png");
	private static final RenderType GLOW = RenderTypes.eyes(JTechStorage.id("textures/entity/crystal_lurker_glow.png"));

	public CrystalLurkerRenderer(EntityRendererProvider.Context context) {
		super(context, new CrystalLurkerModel(context.bakeLayer(CrystalLurkerModel.LAYER)), 1.0f);
		addLayer(new EyesLayer<>(this) {
			@Override
			public RenderType renderType() {
				return GLOW;
			}
		});
	}

	@Override
	public Identifier getTextureLocation(CrystalLurkerRenderState state) {
		return TEXTURE;
	}

	@Override
	public CrystalLurkerRenderState createRenderState() {
		return new CrystalLurkerRenderState();
	}

	@Override
	public void extractRenderState(CrystalLurker mob, CrystalLurkerRenderState state, float partialTick) {
		super.extractRenderState(mob, state, partialTick);
		state.dormant = net.minecraft.util.Mth.lerp(partialTick, mob.prevDormantAmount, mob.dormantAmount);
		state.slam = mob.getSlamTicks() > 0 ? 1.0f - (mob.getSlamTicks() - partialTick) / dev.bartels.jtechstorage.depths.entity.CrystalLurker.SLAM_TICKS : 0.0f;
	}

	@Override
	protected void scale(CrystalLurkerRenderState state, PoseStack poseStack) {
		poseStack.scale(0.5f, 0.5f, 0.5f);   // Modell in doppelter Detaildichte
	}
}
