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
import dev.bartels.jtechstorage.companion.GeodeKeeper;

public class GeodeKeeperRenderer extends MobRenderer<GeodeKeeper, GeodeKeeperRenderState, GeodeKeeperModel> {
	private static final Identifier TEXTURE = JTechStorage.id("textures/entity/companion/geode_keeper.png");
	private static final RenderType GLOW = RenderTypes.eyes(JTechStorage.id("textures/entity/companion/geode_keeper_glow.png"));

	public GeodeKeeperRenderer(EntityRendererProvider.Context context) {
		super(context, new GeodeKeeperModel(context.bakeLayer(GeodeKeeperModel.LAYER)), 0.5f);
		addLayer(new EyesLayer<>(this) {
			@Override
			public RenderType renderType() {
				return GLOW;
			}
		});
	}

	@Override
	public Identifier getTextureLocation(GeodeKeeperRenderState state) {
		return TEXTURE;
	}

	@Override
	public GeodeKeeperRenderState createRenderState() {
		return new GeodeKeeperRenderState();
	}

	@Override
	public void extractRenderState(GeodeKeeper keeper, GeodeKeeperRenderState state, float partialTick) {
		super.extractRenderState(keeper, state, partialTick);
		int offer = keeper.getOfferTicks();
		state.offer = offer > 0 ? Mth.clamp(Math.min(60 - offer, offer) / 10.0f, 0.0f, 1.0f) : 0.0f;
	}

	@Override
	protected void scale(GeodeKeeperRenderState state, PoseStack poseStack) {
		poseStack.scale(0.5f, 0.5f, 0.5f);
	}
}
