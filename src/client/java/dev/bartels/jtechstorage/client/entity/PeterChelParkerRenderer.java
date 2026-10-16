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
import dev.bartels.jtechstorage.entity.PeterChelParker;

public class PeterChelParkerRenderer extends MobRenderer<PeterChelParker, PeterChelParkerRenderState, PeterChelParkerModel> {
	private static final Identifier TEXTURE = JTechStorage.id("textures/entity/peter_chel_parker.png");
	private static final Identifier ACCENT = JTechStorage.id("textures/entity/peter_chel_parker_accent.png");
	private static final RenderType GLOW = RenderTypes.eyes(JTechStorage.id("textures/entity/peter_chel_parker_glow.png"));
	private static final float SCALE = 1.15f;

	public PeterChelParkerRenderer(EntityRendererProvider.Context context) {
		super(context, new PeterChelParkerModel(context.bakeLayer(PeterChelParkerModel.LAYER)), 0.7f);
		addLayer(new AccentLayer<>(this, ACCENT, state -> state.accent));
		addLayer(new HelperHatLayer<>(this));
		addLayer(new EyesLayer<>(this) {
			@Override
			public RenderType renderType() {
				return GLOW;
			}

			@Override
			public void submit(PoseStack pose, SubmitNodeCollector collector, int light, PeterChelParkerRenderState state, float yRot, float xRot) {
				if (!state.hologram) {
					super.submit(pose, collector, light, state, yRot, xRot);
				}
			}
		});
	}

	@Override
	public Identifier getTextureLocation(PeterChelParkerRenderState state) {
		return TEXTURE;
	}

	@Override
	protected RenderType getRenderType(PeterChelParkerRenderState state, boolean bodyVisible, boolean translucent, boolean glowing) {
		return state.hologram ? HologramRendering.renderType(getTextureLocation(state)) : super.getRenderType(state, bodyVisible, translucent, glowing);
	}

	@Override
	protected int getModelTint(PeterChelParkerRenderState state) {
		return state.hologram ? HologramRendering.tint(state.ageInTicks) : super.getModelTint(state);
	}

	@Override
	public PeterChelParkerRenderState createRenderState() {
		return new PeterChelParkerRenderState();
	}

	@Override
	public void extractRenderState(PeterChelParker hero, PeterChelParkerRenderState state, float partialTick) {
		super.extractRenderState(hero, state, partialTick);
		state.accent = hero.getAccentColor();
		state.hologram = hero.isHologram();

		if (state.hologram) {
			state.accent = -1;
		}
		int ticks = hero.getAttackTicks();
		state.attackProgress = ticks > 0 ? 1.0f - (ticks - partialTick) / PeterChelParker.ATTACK_ANIMATION : 0.0f;
	}

	@Override
	protected void scale(PeterChelParkerRenderState state, PoseStack poseStack) {
		poseStack.scale(SCALE, SCALE, SCALE);
	}
}
