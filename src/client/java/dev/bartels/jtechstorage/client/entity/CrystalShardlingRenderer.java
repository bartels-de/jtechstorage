package dev.bartels.jtechstorage.client.entity;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

import dev.bartels.jtechstorage.JTechStorage;
import dev.bartels.jtechstorage.entity.CrystalShardling;

public class CrystalShardlingRenderer extends MobRenderer<CrystalShardling, CrystalShardlingRenderState, CrystalShardlingModel> {
	private static final Identifier TEXTURE = JTechStorage.id("textures/entity/crystal_shardling.png");
	private static final RenderType GLOW = RenderTypes.eyes(JTechStorage.id("textures/entity/crystal_shardling_glow.png"));

	public CrystalShardlingRenderer(EntityRendererProvider.Context context) {
		super(context, new CrystalShardlingModel(context.bakeLayer(CrystalShardlingModel.LAYER)), 0.45f);
		addLayer(new EyesLayer<>(this) {
			@Override
			public RenderType renderType() {
				return GLOW;
			}
		});
	}

	@Override
	public Identifier getTextureLocation(CrystalShardlingRenderState state) {
		return TEXTURE;
	}

	@Override
	public CrystalShardlingRenderState createRenderState() {
		return new CrystalShardlingRenderState();
	}

	@Override
	public void extractRenderState(CrystalShardling shardling, CrystalShardlingRenderState state, float partialTick) {
		super.extractRenderState(shardling, state, partialTick);
		int ticks = shardling.getPounceTicks();
		state.pounce = ticks > 0 ? 1.0f - (ticks - partialTick) / CrystalShardling.POUNCE_TICKS : 0.0f;
	}

	@Override
	protected void scale(CrystalShardlingRenderState state, PoseStack poseStack) {
		poseStack.scale(0.5f, 0.5f, 0.5f);   // Modell in doppelter Detaildichte
	}
}
