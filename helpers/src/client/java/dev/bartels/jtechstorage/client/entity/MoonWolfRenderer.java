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
import dev.bartels.jtechstorage.entity.CrystalWolf;
import dev.bartels.jtechstorage.entity.MoonWolf;

public class MoonWolfRenderer extends MobRenderer<MoonWolf, CrystalWolfRenderState, MoonWolfModel> {
	private static final Identifier TEXTURE = JTechStorage.id("textures/entity/moon_wolf.png");
	private static final Identifier ACCENT = JTechStorage.id("textures/entity/moon_wolf_accent.png");
	private static final RenderType GLOW = RenderTypes.eyes(JTechStorage.id("textures/entity/moon_wolf_glow.png"));
	private static final float SCALE = 0.8f;   // model built at double detail density

	public MoonWolfRenderer(EntityRendererProvider.Context context) {
		super(context, new MoonWolfModel(context.bakeLayer(MoonWolfModel.LAYER)), 0.8f);
		addLayer(new AccentLayer<>(this, ACCENT, state -> state.accent));
		addLayer(new HelperHatLayer<>(this));
		addLayer(new EyesLayer<>(this) {
			@Override
			public RenderType renderType() {
				return GLOW;
			}

			@Override
			public void submit(PoseStack pose, SubmitNodeCollector collector, int light, CrystalWolfRenderState state, float yRot, float xRot) {
				if (!state.hologram) {
					super.submit(pose, collector, light, state, yRot, xRot);
				}
			}
		});
	}

	@Override
	public Identifier getTextureLocation(CrystalWolfRenderState state) {
		return TEXTURE;
	}

	@Override
	public CrystalWolfRenderState createRenderState() {
		return new CrystalWolfRenderState();
	}

	@Override
	public void extractRenderState(MoonWolf wolf, CrystalWolfRenderState state, float partialTick) {
		super.extractRenderState(wolf, state, partialTick);
		state.hologram = wolf.isHologram();
		state.accent = state.hologram ? -1 : wolf.getAccentColor();
		state.resting = wolf.isResting();
		state.exhausted = wolf.isExhausted();
		state.angry = wolf.isAggressive();
		state.sitting = wolf instanceof MoonWolf moon && moon.isWild();
		int bite = wolf.getBiteTicks();
		state.bite = bite > 0 ? 1.0f - (bite - partialTick) / CrystalWolf.BITE_TICKS : 0.0f;
		int howl = wolf.getHowlTicks();
		state.howl = howl > 0 ? 1.0f - (howl - partialTick) / CrystalWolf.HOWL_TICKS : 0.0f;
	}

	@Override
	protected RenderType getRenderType(CrystalWolfRenderState state, boolean bodyVisible, boolean translucent, boolean glowing) {
		return state.hologram ? HologramRendering.renderType(TEXTURE) : super.getRenderType(state, bodyVisible, translucent, glowing);
	}

	@Override
	protected int getModelTint(CrystalWolfRenderState state) {
		return state.hologram ? HologramRendering.tint(state.ageInTicks) : super.getModelTint(state);
	}

	@Override
	protected void scale(CrystalWolfRenderState state, PoseStack poseStack) {
		poseStack.scale(SCALE, SCALE, SCALE);
	}
}
