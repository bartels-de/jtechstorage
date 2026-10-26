package dev.bartels.jtechstorage.client.entity;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

import dev.bartels.jtechstorage.JTechStorage;
import dev.bartels.jtechstorage.depths.entity.SwarmShard;

/** Crystal Depths (v0.31). Model at double detail density, glowing parts as a separate layer. */
public class SwarmShardRenderer extends MobRenderer<SwarmShard, SwarmShardRenderState, SwarmShardModel> {
	private static final Identifier TEXTURE = JTechStorage.id("textures/entity/swarm_shard.png");
	private static final RenderType GLOW = RenderTypes.eyes(JTechStorage.id("textures/entity/swarm_shard_glow.png"));

	public SwarmShardRenderer(EntityRendererProvider.Context context) {
		super(context, new SwarmShardModel(context.bakeLayer(SwarmShardModel.LAYER)), 0.3f);
		addLayer(new EyesLayer<>(this) {
			@Override
			public RenderType renderType() {
				return GLOW;
			}
		});
	}

	@Override
	public Identifier getTextureLocation(SwarmShardRenderState state) {
		return TEXTURE;
	}

	@Override
	public SwarmShardRenderState createRenderState() {
		return new SwarmShardRenderState();
	}

	@Override
	public void extractRenderState(SwarmShard mob, SwarmShardRenderState state, float partialTick) {
		super.extractRenderState(mob, state, partialTick);
		state.dash = mob.getDashTicks() > 0 ? 1.0f - (mob.getDashTicks() - partialTick) / SwarmShard.DASH_TICKS : 0.0f;
	}

	@Override
	protected void scale(SwarmShardRenderState state, PoseStack poseStack) {
		poseStack.scale(0.5f, 0.5f, 0.5f);   // model at double detail density
	}
}
