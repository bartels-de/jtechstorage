package dev.bartels.jtechstorage.client.entity;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

import dev.bartels.jtechstorage.JTechStorage;
import dev.bartels.jtechstorage.entity.AmethystRobot;

public class AmethystRobotRenderer extends MobRenderer<AmethystRobot, AmethystRobotRenderState, AmethystRobotModel> {
	private static final Identifier TEXTURE = JTechStorage.id("textures/entity/amethyst_robot.png");
	private static final RenderType GLOW = RenderTypes.eyes(JTechStorage.id("textures/entity/amethyst_robot_glow.png"));
	private static final float SCALE = 1.1f;   // Modell in doppelter Detaildichte
	private static final float HOLOGRAM_SCALE = 0.225f;

	public AmethystRobotRenderer(EntityRendererProvider.Context context) {
		super(context, new AmethystRobotModel(context.bakeLayer(AmethystRobotModel.LAYER)), 2.0f);
		addLayer(new EyesLayer<>(this) {
			@Override
			public RenderType renderType() {
				return GLOW;
			}

			@Override
			public void submit(PoseStack pose, SubmitNodeCollector collector, int light, AmethystRobotRenderState state, float yRot, float xRot) {
				if (!state.hologram) {
					super.submit(pose, collector, light, state, yRot, xRot);
				}
			}
		});
	}

	@Override
	public Identifier getTextureLocation(AmethystRobotRenderState state) {
		return TEXTURE;
	}

	@Override
	public AmethystRobotRenderState createRenderState() {
		return new AmethystRobotRenderState();
	}

	private static int duration(int anim) {
		return switch (anim) {
			case AmethystRobot.ANIM_SLAM -> 14;
			case AmethystRobot.ANIM_SWEEP -> 15;
			case AmethystRobot.ANIM_PULSE -> 30;
			case AmethystRobot.ANIM_STOMP -> 20;
			case AmethystRobot.ANIM_DASH -> 18;
			case AmethystRobot.ANIM_LASER -> AmethystRobot.LASER_CHARGE;
			default -> 1;
		};
	}

	@Override
	public void extractRenderState(AmethystRobot dino, AmethystRobotRenderState state, float partialTick) {
		super.extractRenderState(dino, state, partialTick);
		state.anim = dino.getAnim();
		int total = duration(state.anim);
		state.progress = state.anim == 0 ? 0.0f : Math.clamp(1.0f - (dino.getAnimTicks() - partialTick) / total, 0.0f, 1.0f);
		state.enraged = dino.isEnraged();
		state.hologram = dino.isHologram();
		state.idle = !state.hologram && state.anim == 0 && !dino.isAggressive() && state.walkAnimationSpeed < 0.02f;
	}

	@Override
	protected RenderType getRenderType(AmethystRobotRenderState state, boolean bodyVisible, boolean translucent, boolean glowing) {
		return state.hologram ? HologramRendering.renderType(TEXTURE) : super.getRenderType(state, bodyVisible, translucent, glowing);
	}

	@Override
	protected int getModelTint(AmethystRobotRenderState state) {
		if (state.hologram) {
			return HologramRendering.tint(state.ageInTicks);
		}

		return state.enraged ? 0xFFFFB8C8 : super.getModelTint(state);
	}

	@Override
	protected void scale(AmethystRobotRenderState state, PoseStack poseStack) {
		float scale = state.hologram ? HOLOGRAM_SCALE : SCALE;
		poseStack.scale(scale, scale, scale);
	}
}
