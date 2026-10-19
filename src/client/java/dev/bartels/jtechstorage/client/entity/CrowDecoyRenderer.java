package dev.bartels.jtechstorage.client.entity;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.Identifier;

import dev.bartels.jtechstorage.companion.CrowDecoy;

/**
 * Krähen-Abbild (v0.26.4): der Spieler mit seinem eigenen Skin, dunkelviolett eingefärbt und durchscheinend; zum Ende
 * verblasst es.
 */
public class CrowDecoyRenderer extends LivingEntityRenderer<CrowDecoy, CrowDecoyRenderer.State, HumanoidModel<CrowDecoyRenderer.State>> {
	private final HumanoidModel<State> wide;
	private final HumanoidModel<State> slim;

	public static class State extends HumanoidRenderState {
		public Identifier skin = DefaultPlayerSkin.getDefaultTexture();
		public boolean slim;
		public float fade = 1.0f;
	}

	public CrowDecoyRenderer(EntityRendererProvider.Context context) {
		super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER)), 0.4f);
		wide = model;
		slim = new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_SLIM));
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(CrowDecoy decoy, State state, float partialTick) {
		super.extractRenderState(decoy, state, partialTick);

		if (decoy.level().getEntity(decoy.playerId()) instanceof AbstractClientPlayer player) {
			state.skin = player.getSkin().body().texturePath();
			state.slim = player.getSkin().model() == net.minecraft.world.entity.player.PlayerModelType.SLIM;
		} else {
			state.skin = DefaultPlayerSkin.getDefaultTexture();
			state.slim = false;
		}

		float age = decoy.tickCount + partialTick;
		state.fade = Math.clamp((CrowDecoy.LIFE - age) / 25.0f, 0.0f, 1.0f) * Math.clamp(age / 6.0f, 0.0f, 1.0f);
		state.walkAnimationSpeed = 0.0f;
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		model = state.slim ? slim : wide;
		super.submit(state, pose, collector, camera);
	}

	@Override
	public Identifier getTextureLocation(State state) {
		return state.skin;
	}

	@Override
	protected RenderType getRenderType(State state, boolean bodyVisible, boolean translucent, boolean glowing) {
		return RenderTypes.entityTranslucent(getTextureLocation(state));
	}

	/** Dunkelviolett und durchscheinend, verblasst zum Ende. */
	@Override
	protected int getModelTint(State state) {
		int alpha = (int) (200 * state.fade);
		return (alpha << 24) | 0x7A50C0;
	}

	@Override
	protected boolean shouldShowName(CrowDecoy decoy, double distance) {
		return false;
	}

	@Override
	protected void scale(State state, PoseStack pose) {
		pose.scale(0.9375f, 0.9375f, 0.9375f);
	}
}
