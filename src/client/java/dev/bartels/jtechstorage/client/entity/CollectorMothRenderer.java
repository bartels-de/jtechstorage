package dev.bartels.jtechstorage.client.entity;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;

import dev.bartels.jtechstorage.JTechStorage;
import dev.bartels.jtechstorage.entity.CollectorMoth;

public class CollectorMothRenderer extends MobRenderer<CollectorMoth, CollectorMothRenderState, CollectorMothModel> {
	private static final Identifier TEXTURE = JTechStorage.id("textures/entity/collector_moth.png");
	private static final Identifier ACCENT = JTechStorage.id("textures/entity/collector_moth_accent.png");
	private static final RenderType GLOW = RenderTypes.eyes(JTechStorage.id("textures/entity/collector_moth_glow.png"));

	public CollectorMothRenderer(EntityRendererProvider.Context context) {
		super(context, new CollectorMothModel(context.bakeLayer(CollectorMothModel.LAYER)), 0.2f);
		addLayer(new AccentLayer<>(this, ACCENT, state -> state.accent));
		addLayer(new HelperHatLayer<>(this));
		addLayer(new ItemInHandLayer<>(this));
		addLayer(new EyesLayer<>(this) {
			@Override
			public RenderType renderType() {
				return GLOW;
			}

			@Override
			public void submit(PoseStack pose, SubmitNodeCollector collector, int light, CollectorMothRenderState state, float yRot, float xRot) {
				if (!state.resting && !state.hologram) {
					super.submit(pose, collector, light, state, yRot, xRot);
				}
			}
		});
	}

	@Override
	public Identifier getTextureLocation(CollectorMothRenderState state) {
		return TEXTURE;
	}

	@Override
	protected RenderType getRenderType(CollectorMothRenderState state, boolean bodyVisible, boolean translucent, boolean glowing) {
		return state.hologram ? HologramRendering.renderType(getTextureLocation(state)) : super.getRenderType(state, bodyVisible, translucent, glowing);
	}

	@Override
	protected int getModelTint(CollectorMothRenderState state) {
		return state.hologram ? HologramRendering.tint(state.ageInTicks) : super.getModelTint(state);
	}

	@Override
	public CollectorMothRenderState createRenderState() {
		return new CollectorMothRenderState();
	}

	@Override
	public void extractRenderState(CollectorMoth moth, CollectorMothRenderState state, float partialTick) {
		super.extractRenderState(moth, state, partialTick);
		state.accent = moth.getAccentColor();
		state.hologram = moth.isHologram();

		if (state.hologram) {
			state.accent = -1;
		}
		state.resting = moth.isResting();
		state.action = moth.getActionAmount(partialTick);
		ArmedEntityRenderState.extractArmedEntityRenderState(moth, state, itemModelResolver, partialTick);
		state.carrying = !moth.getMainHandItem().isEmpty();
	}

	@Override
	protected int getBlockLightLevel(CollectorMoth moth, BlockPos pos) {
		return Math.max(8, super.getBlockLightLevel(moth, pos));
	}

	/** Modell ist in doppelter Detaildichte gebaut. */
	@Override
	protected void scale(CollectorMothRenderState state, PoseStack poseStack) {
		poseStack.scale(0.5f, 0.5f, 0.5f);
	}
}
