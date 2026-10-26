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
import dev.bartels.jtechstorage.entity.Chel;

public class ChelRenderer extends MobRenderer<Chel, ChelRenderState, ChelModel> {
	private static final Identifier TEXTURE = JTechStorage.id("textures/entity/chel.png");
	private static final Identifier ACCENT = JTechStorage.id("textures/entity/chel_accent.png");
	private static final RenderType GLOW = RenderTypes.eyes(JTechStorage.id("textures/entity/chel_glow.png"));

	public ChelRenderer(EntityRendererProvider.Context context) {
		super(context, new ChelModel(context.bakeLayer(ChelModel.LAYER)), 0.4f);
		addLayer(new AccentLayer<>(this, ACCENT, state -> state.accent));
		addLayer(new HelperHatLayer<>(this));
		addLayer(new EyesLayer<>(this) {
			@Override
			public RenderType renderType() {
				return GLOW;
			}

			@Override
			public void submit(PoseStack pose, SubmitNodeCollector collector, int light, ChelRenderState state, float yRot, float xRot) {
				if (!state.resting && !state.hologram) {
					super.submit(pose, collector, light, state, yRot, xRot);
				}
			}
		});
	}

	@Override
	public Identifier getTextureLocation(ChelRenderState state) {
		return TEXTURE;
	}

	@Override
	protected RenderType getRenderType(ChelRenderState state, boolean bodyVisible, boolean translucent, boolean glowing) {
		return state.hologram ? HologramRendering.renderType(getTextureLocation(state)) : super.getRenderType(state, bodyVisible, translucent, glowing);
	}

	@Override
	protected int getModelTint(ChelRenderState state) {
		return state.hologram ? HologramRendering.tint(state.ageInTicks) : super.getModelTint(state);
	}

	@Override
	public ChelRenderState createRenderState() {
		return new ChelRenderState();
	}

	@Override
	public void extractRenderState(Chel chel, ChelRenderState state, float partialTick) {
		super.extractRenderState(chel, state, partialTick);
		state.accent = chel.getAccentColor();
		state.hologram = chel.isHologram();

		if (state.hologram) {
			state.accent = -1;
		}
		state.resting = chel.isResting();
		state.crafting = chel.getCraftingTicks() > 0;
	}

	/** Model is built at double detail density. */
	@Override
	protected void scale(ChelRenderState state, PoseStack poseStack) {
		poseStack.scale(0.5f, 0.5f, 0.5f);
	}
}
