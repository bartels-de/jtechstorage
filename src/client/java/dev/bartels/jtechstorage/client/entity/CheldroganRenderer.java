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
import dev.bartels.jtechstorage.entity.Cheldrogan;

public class CheldroganRenderer extends MobRenderer<Cheldrogan, CheldroganRenderState, CheldroganModel> {
	private static final Identifier TEXTURE = JTechStorage.id("textures/entity/cheldrogan.png");
	private static final Identifier ACCENT = JTechStorage.id("textures/entity/cheldrogan_accent.png");
	private static final RenderType GLOW = RenderTypes.eyes(JTechStorage.id("textures/entity/cheldrogan_glow.png"));
	public static final float SCALE = 0.6f;   // Modell in doppelter Detaildichte
	private static final float HOLOGRAM_SCALE = 0.25f;

	public CheldroganRenderer(EntityRendererProvider.Context context) {
		super(context, new CheldroganModel(context.bakeLayer(CheldroganModel.LAYER)), 1.1f);
		addLayer(new AccentLayer<>(this, ACCENT, state -> state.accent));
		addLayer(new HelperHatLayer<>(this));
		addLayer(new EyesLayer<>(this) {
			@Override
			public RenderType renderType() {
				return GLOW;
			}

			@Override
			public void submit(PoseStack pose, SubmitNodeCollector collector, int light, CheldroganRenderState state, float yRot, float xRot) {
				if (!state.hologram) {
					super.submit(pose, collector, light, state, yRot, xRot);
				}
			}
		});
	}

	@Override
	public Identifier getTextureLocation(CheldroganRenderState state) {
		return TEXTURE;
	}

	@Override
	protected RenderType getRenderType(CheldroganRenderState state, boolean bodyVisible, boolean translucent, boolean glowing) {
		return state.hologram ? HologramRendering.renderType(getTextureLocation(state)) : super.getRenderType(state, bodyVisible, translucent, glowing);
	}

	@Override
	protected int getModelTint(CheldroganRenderState state) {
		return state.hologram ? HologramRendering.tint(state.ageInTicks) : super.getModelTint(state);
	}

	@Override
	public CheldroganRenderState createRenderState() {
		return new CheldroganRenderState();
	}

	@Override
	public void extractRenderState(Cheldrogan dragon, CheldroganRenderState state, float partialTick) {
		super.extractRenderState(dragon, state, partialTick);
		state.hologram = dragon.isHologram();
		state.accent = state.hologram ? -1 : dragon.getAccentColor();
		int sweep = dragon.getSweepTicks();
		state.sweepProgress = sweep > 0 ? 1.0f - (sweep - partialTick) / Cheldrogan.SWEEP_TICKS : 0.0f;
		int breath = dragon.getBreathTicks();
		state.breathProgress = breath > 0 ? 1.0f - (breath - partialTick) / Cheldrogan.BREATH_TICKS : 0.0f;
		int rain = dragon.getRainTicks();
		state.rainProgress = rain > 0 ? 1.0f - (rain - partialTick) / Cheldrogan.RAIN_TICKS : 0.0f;
		state.flyAmount = dragon.getFlyAmount(partialTick);
		long time = Math.floorMod(dragon.level().getDefaultClockTime(), 24000L);
		state.resting = !state.hologram && sweep == 0 && breath == 0 && rain == 0 && state.flyAmount <= 0.0f && !dragon.isAggressive()
				&& state.walkAnimationSpeed < 0.02f && time >= 13000L && time <= 23000L && !dragon.isVehicle();
	}

	@Override
	protected void scale(CheldroganRenderState state, PoseStack poseStack) {
		float scale = state.hologram ? HOLOGRAM_SCALE : SCALE;
		poseStack.scale(scale, scale, scale);
	}
}
