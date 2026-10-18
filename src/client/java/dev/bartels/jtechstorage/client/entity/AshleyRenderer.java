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
import dev.bartels.jtechstorage.entity.Ashley;

public class AshleyRenderer extends MobRenderer<Ashley, AshleyRenderState, AshleyModel> {
	private static final Identifier TEXTURE = JTechStorage.id("textures/entity/ashley.png");
	private static final Identifier ACCENT = JTechStorage.id("textures/entity/ashley_accent.png");
	private static final RenderType GLOW = RenderTypes.eyes(JTechStorage.id("textures/entity/ashley_glow.png"));

	public AshleyRenderer(EntityRendererProvider.Context context) {
		super(context, new AshleyModel(context.bakeLayer(AshleyModel.LAYER)), 0.45f);
		addLayer(new AccentLayer<>(this, ACCENT, state -> state.accent));
		addLayer(new HelperHatLayer<>(this));
		addLayer(new EyesLayer<>(this) {
			@Override
			public RenderType renderType() {
				return GLOW;
			}

			@Override
			public void submit(PoseStack pose, SubmitNodeCollector collector, int light, AshleyRenderState state, float yRot, float xRot) {
				if (!state.resting && !state.hologram) {
					super.submit(pose, collector, light, state, yRot, xRot);
				}
			}
		});
	}

	@Override
	public Identifier getTextureLocation(AshleyRenderState state) {
		return TEXTURE;
	}

	@Override
	protected RenderType getRenderType(AshleyRenderState state, boolean bodyVisible, boolean translucent, boolean glowing) {
		return state.hologram ? HologramRendering.renderType(getTextureLocation(state)) : super.getRenderType(state, bodyVisible, translucent, glowing);
	}

	@Override
	protected int getModelTint(AshleyRenderState state) {
		return state.hologram ? HologramRendering.tint(state.ageInTicks) : super.getModelTint(state);
	}

	@Override
	public AshleyRenderState createRenderState() {
		return new AshleyRenderState();
	}

	@Override
	public void extractRenderState(Ashley ashley, AshleyRenderState state, float partialTick) {
		super.extractRenderState(ashley, state, partialTick);
		state.accent = ashley.getAccentColor();
		state.hologram = ashley.isHologram();

		if (state.hologram) {
			state.accent = -1;
		}
		state.resting = ashley.isResting();
		state.action = ashley.getActionAmount(partialTick);
	}

	/** Modell ist in doppelter Detaildichte gebaut. */
	@Override
	protected void scale(AshleyRenderState state, PoseStack poseStack) {
		poseStack.scale(0.5f, 0.5f, 0.5f);
	}
}
