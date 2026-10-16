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
import dev.bartels.jtechstorage.entity.AmethystFox;

public class AmethystFoxRenderer extends MobRenderer<AmethystFox, AmethystFoxRenderState, AmethystFoxModel> {
	private static final Identifier TEXTURE = JTechStorage.id("textures/entity/amethyst_fox.png");
	private static final Identifier ACCENT = JTechStorage.id("textures/entity/amethyst_fox_accent.png");
	private static final RenderType GLOW = RenderTypes.eyes(JTechStorage.id("textures/entity/amethyst_fox_glow.png"));

	public AmethystFoxRenderer(EntityRendererProvider.Context context) {
		super(context, new AmethystFoxModel(context.bakeLayer(AmethystFoxModel.LAYER)), 0.4f);
		addLayer(new AccentLayer<>(this, ACCENT, state -> state.accent));
		addLayer(new HelperHatLayer<>(this));
		addLayer(new EyesLayer<>(this) {
			@Override
			public RenderType renderType() {
				return GLOW;
			}

			@Override
			public void submit(PoseStack pose, SubmitNodeCollector collector, int light, AmethystFoxRenderState state, float yRot, float xRot) {
				if (!state.hologram && !state.resting) {
					super.submit(pose, collector, light, state, yRot, xRot);
				}
			}
		});
	}

	@Override
	public Identifier getTextureLocation(AmethystFoxRenderState state) {
		return TEXTURE;
	}

	@Override
	public AmethystFoxRenderState createRenderState() {
		return new AmethystFoxRenderState();
	}

	@Override
	public void extractRenderState(AmethystFox fox, AmethystFoxRenderState state, float partialTick) {
		super.extractRenderState(fox, state, partialTick);
		state.hologram = fox.isHologram();
		state.accent = state.hologram ? -1 : fox.getAccentColor();
		state.digging = fox.getDigTicks() > 0;
		state.resting = fox.isResting();
		state.ready = fox.isReady();
	}

	@Override
	protected RenderType getRenderType(AmethystFoxRenderState state, boolean bodyVisible, boolean translucent, boolean glowing) {
		return state.hologram ? HologramRendering.renderType(TEXTURE) : super.getRenderType(state, bodyVisible, translucent, glowing);
	}

	@Override
	protected int getModelTint(AmethystFoxRenderState state) {
		return state.hologram ? HologramRendering.tint(state.ageInTicks) : super.getModelTint(state);
	}
}
