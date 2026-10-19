package dev.bartels.jtechstorage.client.entity;

import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;

import dev.bartels.jtechstorage.JTechStorage;
import dev.bartels.jtechstorage.companion.Companion;
import dev.bartels.jtechstorage.companion.RavenCompanion;

/**
 * Begleiter-Renderer (v0.25): je Stufe ein eigenes Modell mit eigener Textur und Leucht-Ebene. Vor dem Zeichnen wird
 * das Modell der aktuellen Stufe eingesetzt.
 */
public class CompanionRenderer<T extends Companion> extends MobRenderer<T, CompanionRenderState, EntityModel<CompanionRenderState>> {
	private static final float SCALE = 0.5f;     // Modelle in doppelter Detaildichte
	private final List<EntityModel<CompanionRenderState>> models;
	private final Identifier[] textures = new Identifier[3];
	private final float shadow;
	/** Zusätzliche Größe je Stufe (Rabe größer, große Mantis zum Reiten). */
	private final float[] stageScale;

	public CompanionRenderer(EntityRendererProvider.Context context, String species, float shadow, float[] stageScale,
			List<EntityModel<CompanionRenderState>> models) {
		super(context, models.get(0), shadow);
		this.models = models;
		this.shadow = shadow;
		this.stageScale = stageScale;

		for (int k = 0; k < 3; k++) {
			textures[k] = JTechStorage.id("textures/entity/companion/" + species + "_" + (k + 1) + ".png");
			int stage = k + 1;
			RenderType glow = RenderTypes.eyes(JTechStorage.id("textures/entity/companion/" + species + "_" + stage + "_glow.png"));
			addLayer(new EyesLayer<>(this) {
				@Override
				public RenderType renderType() {
					return glow;
				}

				@Override
				public void submit(PoseStack pose, SubmitNodeCollector collector, int light, CompanionRenderState state, float yRot, float xRot) {
					if (state.stage == stage && !state.isInvisible) {
						super.submit(pose, collector, light, state, yRot, xRot);
					}
				}
			});
		}
	}

	private static int index(CompanionRenderState state) {
		return Math.clamp(state.stage, 1, 3) - 1;
	}

	@Override
	public void submit(CompanionRenderState state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		model = models.get(index(state));
		super.submit(state, pose, collector, camera);
	}

	@Override
	public Identifier getTextureLocation(CompanionRenderState state) {
		return textures[index(state)];
	}

	@Override
	public CompanionRenderState createRenderState() {
		return new CompanionRenderState();
	}

	@Override
	public void extractRenderState(T companion, CompanionRenderState state, float partialTick) {
		super.extractRenderState(companion, state, partialTick);
		state.stage = companion.getStage();
		state.sitting = companion.isInSittingPose();
		state.exhausted = companion.isExhausted();
		state.flying = (companion instanceof RavenCompanion raven && raven.isFlying() && !state.sitting)
				|| (companion instanceof dev.bartels.jtechstorage.companion.MantisCompanion && companion.isVehicle() && !companion.onGround());
		int attack = companion.getAttackTicks();
		state.attack = attack > 0 ? 1.0f - (attack - partialTick) / Companion.ATTACK_TICKS : 0.0f;
		int action = companion.getActionTicks();
		state.action = action > 0 ? 1.0f - (action - partialTick) / Companion.ACTION_TICKS : 0.0f;
	}

	@Override
	protected float getShadowRadius(CompanionRenderState state) {
		return shadow * (state.stage == 1 ? 0.6f : state.stage == 2 ? 0.85f : 1.1f) * stageScale[index(state)];
	}

	@Override
	protected void scale(CompanionRenderState state, PoseStack poseStack) {
		float scale = SCALE * stageScale[index(state)];
		poseStack.scale(scale, scale, scale);
	}
}
