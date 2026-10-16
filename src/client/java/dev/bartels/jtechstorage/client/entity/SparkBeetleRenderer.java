package dev.bartels.jtechstorage.client.entity;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;

import dev.bartels.jtechstorage.JTechStorage;
import dev.bartels.jtechstorage.entity.SparkBeetle;

public class SparkBeetleRenderer extends MobRenderer<SparkBeetle, SparkBeetleRenderState, SparkBeetleModel> {
	private static final Identifier TEXTURE = JTechStorage.id("textures/entity/spark_beetle.png");
	private static final Identifier ACCENT = JTechStorage.id("textures/entity/spark_beetle_accent.png");
	private static final RenderType GLOW = RenderTypes.eyes(JTechStorage.id("textures/entity/spark_beetle_glow.png"));

	public SparkBeetleRenderer(EntityRendererProvider.Context context) {
		super(context, new SparkBeetleModel(context.bakeLayer(SparkBeetleModel.LAYER)), 0.35f);
		addLayer(new AccentLayer<>(this, ACCENT, state -> state.accent));
		addLayer(new HelperHatLayer<>(this));
		addLayer(new EyesLayer<>(this) {
			@Override
			public RenderType renderType() {
				return GLOW;
			}

			@Override
			public void submit(PoseStack pose, SubmitNodeCollector collector, int light, SparkBeetleRenderState state, float yRot, float xRot) {
				if (!state.resting && !state.hologram) {
					super.submit(pose, collector, light, state, yRot, xRot);
				}
			}
		});
	}

	@Override
	public Identifier getTextureLocation(SparkBeetleRenderState state) {
		return TEXTURE;
	}

	@Override
	protected RenderType getRenderType(SparkBeetleRenderState state, boolean bodyVisible, boolean translucent, boolean glowing) {
		return state.hologram ? HologramRendering.renderType(getTextureLocation(state)) : super.getRenderType(state, bodyVisible, translucent, glowing);
	}

	@Override
	protected int getModelTint(SparkBeetleRenderState state) {
		return state.hologram ? HologramRendering.tint(state.ageInTicks) : super.getModelTint(state);
	}

	@Override
	public SparkBeetleRenderState createRenderState() {
		return new SparkBeetleRenderState();
	}

	@Override
	public void extractRenderState(SparkBeetle beetle, SparkBeetleRenderState state, float partialTick) {
		super.extractRenderState(beetle, state, partialTick);
		state.accent = beetle.getAccentColor();
		state.hologram = beetle.isHologram();

		if (state.hologram) {
			state.accent = -1;
		}
		state.resting = beetle.isResting();
		state.charging = beetle.isCharging();
	}

	@Override
	protected int getBlockLightLevel(SparkBeetle beetle, BlockPos pos) {
		return Math.max(10, super.getBlockLightLevel(beetle, pos));
	}
}
