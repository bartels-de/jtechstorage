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
import dev.bartels.jtechstorage.entity.StorageGriffin;

public class StorageGriffinRenderer extends MobRenderer<StorageGriffin, StorageGriffinRenderState, StorageGriffinModel> {
	private static final Identifier TEXTURE = JTechStorage.id("textures/entity/storage_griffin.png");
	private static final Identifier ACCENT = JTechStorage.id("textures/entity/storage_griffin_accent.png");
	private static final RenderType GLOW = RenderTypes.eyes(JTechStorage.id("textures/entity/storage_griffin_glow.png"));
	private static final float SCALE = 0.65f;   // model built at double detail density

	public StorageGriffinRenderer(EntityRendererProvider.Context context) {
		super(context, new StorageGriffinModel(context.bakeLayer(StorageGriffinModel.LAYER)), 0.8f);
		addLayer(new AccentLayer<>(this, ACCENT, state -> state.accent));
		addLayer(new HelperHatLayer<>(this));
		addLayer(new EyesLayer<>(this) {
			@Override
			public RenderType renderType() {
				return GLOW;
			}

			@Override
			public void submit(PoseStack pose, SubmitNodeCollector collector, int light, StorageGriffinRenderState state, float yRot, float xRot) {
				if (!state.resting && !state.hologram) {
					super.submit(pose, collector, light, state, yRot, xRot);
				}
			}
		});
	}

	@Override
	public Identifier getTextureLocation(StorageGriffinRenderState state) {
		return TEXTURE;
	}

	@Override
	protected RenderType getRenderType(StorageGriffinRenderState state, boolean bodyVisible, boolean translucent, boolean glowing) {
		return state.hologram ? HologramRendering.renderType(getTextureLocation(state)) : super.getRenderType(state, bodyVisible, translucent, glowing);
	}

	@Override
	protected int getModelTint(StorageGriffinRenderState state) {
		return state.hologram ? HologramRendering.tint(state.ageInTicks) : super.getModelTint(state);
	}

	@Override
	public StorageGriffinRenderState createRenderState() {
		return new StorageGriffinRenderState();
	}

	@Override
	public void extractRenderState(StorageGriffin griffin, StorageGriffinRenderState state, float partialTick) {
		super.extractRenderState(griffin, state, partialTick);
		state.accent = griffin.getAccentColor();
		state.hologram = griffin.isHologram();

		if (state.hologram) {
			state.accent = -1;
		}
		state.resting = griffin.isResting();
		state.flying = griffin.isFlying();
		state.ridden = griffin.isVehicle();
	}

	@Override
	protected void scale(StorageGriffinRenderState state, PoseStack poseStack) {
		poseStack.scale(SCALE, SCALE, SCALE);
	}
}
