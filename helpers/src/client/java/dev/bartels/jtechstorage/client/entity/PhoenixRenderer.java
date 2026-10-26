package dev.bartels.jtechstorage.client.entity;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;

import dev.bartels.jtechstorage.JTechStorage;
import dev.bartels.jtechstorage.entity.Phoenix;

public class PhoenixRenderer extends MobRenderer<Phoenix, PhoenixRenderState, PhoenixModel> {
	private static final Identifier TEXTURE = JTechStorage.id("textures/entity/phoenix.png");
	private static final Identifier ACCENT = JTechStorage.id("textures/entity/phoenix_accent.png");
	private static final RenderType GLOW = RenderTypes.eyes(JTechStorage.id("textures/entity/phoenix_glow.png"));
	private static final float SCALE = 0.625f;   // model built at double detail density

	public PhoenixRenderer(EntityRendererProvider.Context context) {
		super(context, new PhoenixModel(context.bakeLayer(PhoenixModel.LAYER)), 0.4f);
		addLayer(new AccentLayer<>(this, ACCENT, state -> state.accent));
		addLayer(new HelperHatLayer<>(this));
		addLayer(new EyesLayer<>(this) {
			@Override
			public RenderType renderType() {
				return GLOW;
			}

			@Override
			public void submit(PoseStack pose, SubmitNodeCollector collector, int light, PhoenixRenderState state, float yRot, float xRot) {
				if (!state.hologram) {
					super.submit(pose, collector, light, state, yRot, xRot);
				}
			}
		});
	}

	@Override
	public Identifier getTextureLocation(PhoenixRenderState state) {
		return TEXTURE;
	}

	@Override
	protected RenderType getRenderType(PhoenixRenderState state, boolean bodyVisible, boolean translucent, boolean glowing) {
		return state.hologram ? HologramRendering.renderType(getTextureLocation(state)) : super.getRenderType(state, bodyVisible, translucent, glowing);
	}

	@Override
	protected int getModelTint(PhoenixRenderState state) {
		return state.hologram ? HologramRendering.tint(state.ageInTicks) : super.getModelTint(state);
	}

	@Override
	public PhoenixRenderState createRenderState() {
		return new PhoenixRenderState();
	}

	@Override
	public void extractRenderState(Phoenix hero, PhoenixRenderState state, float partialTick) {
		super.extractRenderState(hero, state, partialTick);
		state.accent = hero.getAccentColor();
		state.hologram = hero.isHologram();

		if (state.hologram) {
			state.accent = -1;
		}
		state.flying = !state.hologram && (!hero.onGround() || hero.isMessenger() || hero.isDeparting());
		state.away = hero.isAway();
		long time = Math.floorMod(hero.level().getDefaultClockTime(), 24000L);
		state.resting = !state.hologram && !state.flying && hero.onGround() && time >= 13000L && time <= 23000L;
	}

	@Override
	public void submit(PhoenixRenderState state, PoseStack poseStack,
			SubmitNodeCollector collector, CameraRenderState camera) {
		if (!state.away) {
			super.submit(state, poseStack, collector, camera);
		}
	}

	@Override
	protected void scale(PhoenixRenderState state, PoseStack poseStack) {
		poseStack.scale(SCALE, SCALE, SCALE);
	}
}
