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
import dev.bartels.jtechstorage.depths.temple.LastMaster;

/** The Last Storage Master (v0.39). Model at double detail density, rendered somewhat larger; eyes, runes and crystals glow. */
public class LastMasterRenderer extends MobRenderer<LastMaster, LastMasterRenderState, LastMasterModel> {
	private static final Identifier TEXTURE = JTechStorage.id("textures/entity/last_master.png");
	private static final RenderType GLOW = RenderTypes.eyes(JTechStorage.id("textures/entity/last_master_glow.png"));

	public LastMasterRenderer(EntityRendererProvider.Context context) {
		super(context, new LastMasterModel(context.bakeLayer(LastMasterModel.LAYER)), 0.9f);
		addLayer(new EyesLayer<>(this) {
			@Override
			public RenderType renderType() {
				return GLOW;
			}
		});
	}

	@Override
	public Identifier getTextureLocation(LastMasterRenderState state) {
		return TEXTURE;
	}

	@Override
	public LastMasterRenderState createRenderState() {
		return new LastMasterRenderState();
	}

	private static float anim(LastMaster mob, int anim, int total, float partialTick) {
		if (mob.getAnim() != anim || mob.getAnimTicks() <= 0) {
			return 0.0f;
		}

		float t = 1.0f - (mob.getAnimTicks() - partialTick) / total;
		return Mth.sin(Mth.clamp(t, 0.0f, 1.0f) * Mth.PI);
	}

	@Override
	public void extractRenderState(LastMaster mob, LastMasterRenderState state, float partialTick) {
		super.extractRenderState(mob, state, partialTick);
		state.cast = anim(mob, LastMaster.ANIM_CAST, LastMaster.CAST_TICKS, partialTick);
		state.summon = anim(mob, LastMaster.ANIM_SUMMON, LastMaster.SUMMON_TICKS, partialTick);
		state.slam = anim(mob, LastMaster.ANIM_SLAM, LastMaster.SLAM_TICKS, partialTick);
		state.shielded = mob.isShielded();
	}

	@Override
	protected void scale(LastMasterRenderState state, PoseStack poseStack) {
		poseStack.scale(0.6f, 0.6f, 0.6f);   // model at double detail density, scaled up 1.2×
	}
}
