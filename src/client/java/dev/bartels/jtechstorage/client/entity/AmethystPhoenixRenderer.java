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
import dev.bartels.jtechstorage.entity.AmethystPhoenix;

public class AmethystPhoenixRenderer extends MobRenderer<AmethystPhoenix, AmethystPhoenixRenderState, AmethystPhoenixModel> {
	private static final Identifier TEXTURE = JTechStorage.id("textures/entity/amethyst_phoenix.png");
	private static final Identifier ACCENT = JTechStorage.id("textures/entity/amethyst_phoenix_accent.png");
	private static final RenderType GLOW = RenderTypes.eyes(JTechStorage.id("textures/entity/amethyst_phoenix_glow.png"));
	private static final float SCALE = 1.25f;

	public AmethystPhoenixRenderer(EntityRendererProvider.Context context) {
		super(context, new AmethystPhoenixModel(context.bakeLayer(AmethystPhoenixModel.LAYER)), 1.3f);
		addLayer(new AccentLayer<>(this, ACCENT, state -> state.accent));
		addLayer(new HelperHatLayer<>(this));
		addLayer(new EyesLayer<>(this) {
			@Override
			public RenderType renderType() {
				return GLOW;
			}

			@Override
			public void submit(PoseStack pose, SubmitNodeCollector collector, int light, AmethystPhoenixRenderState state, float yRot, float xRot) {
				if (!state.hologram) {
					super.submit(pose, collector, light, state, yRot, xRot);
				}
			}
		});
	}

	@Override
	public Identifier getTextureLocation(AmethystPhoenixRenderState state) {
		return TEXTURE;
	}

	@Override
	public AmethystPhoenixRenderState createRenderState() {
		return new AmethystPhoenixRenderState();
	}

	@Override
	public void extractRenderState(AmethystPhoenix phoenix, AmethystPhoenixRenderState state, float partialTick) {
		super.extractRenderState(phoenix, state, partialTick);
		state.hologram = phoenix.isHologram();
		state.accent = state.hologram ? -1 : phoenix.getAccentColor();
		state.sitting = phoenix.isSitting();
		state.flying = phoenix.isFlying() || (!phoenix.onGround() && !phoenix.isSitting());
		state.chests = phoenix.getChests();
		state.speed = 1.0f - 0.12f * state.chests;
	}

	@Override
	protected RenderType getRenderType(AmethystPhoenixRenderState state, boolean bodyVisible, boolean translucent, boolean glowing) {
		return state.hologram ? HologramRendering.renderType(TEXTURE) : super.getRenderType(state, bodyVisible, translucent, glowing);
	}

	@Override
	protected int getModelTint(AmethystPhoenixRenderState state) {
		return state.hologram ? HologramRendering.tint(state.ageInTicks) : super.getModelTint(state);
	}

	@Override
	protected void scale(AmethystPhoenixRenderState state, PoseStack poseStack) {
		float scale = state.hologram ? 0.75f : SCALE;
		poseStack.scale(scale, scale, scale);
	}
}
