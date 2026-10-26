package dev.bartels.jtechstorage.client.entity;

import dev.bartels.jtechstorage.companion.MantisCompanion;
import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;

import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;

import dev.bartels.jtechstorage.JTechStorage;
import dev.bartels.jtechstorage.companion.Companion;
import dev.bartels.jtechstorage.companion.CompanionLooks;
import dev.bartels.jtechstorage.companion.RavenCompanion;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Companion renderer (v0.25): one model per tier with its own texture and glow layer. Before drawing,
 * the model of the current tier is swapped in. Customization (v0.25.4): skin texture, freely tinted glow layer and, per
 * equipment slot, a separate model instance that draws only that slot with the material texture.
 */
public class CompanionRenderer<T extends Companion> extends MobRenderer<T, CompanionRenderState, EntityModel<CompanionRenderState>> {
	private static final float SCALE = 0.5f;     // models built at double detail density
	private final List<EntityModel<CompanionRenderState>> models;
	private final Identifier[][] textures = new Identifier[3][CompanionLooks.SKINS.length];
	/** Equipment: [tier][slot] own model instance, [tier][material 1–5] texture. */
	private final List<List<EntityModel<CompanionRenderState>>> equipModels = new ArrayList<>();
	private final Identifier[][] equipTextures = new Identifier[3][6];
	private final RenderType[] glowTint = new RenderType[3];
	private final RenderType[] glowCore = new RenderType[3];
	private final float shadow;
	/** Extra size per tier (raven larger, big mantis for riding). */
	private final float[] stageScale;

	public CompanionRenderer(EntityRendererProvider.Context context, String species, float shadow, float[] stageScale,
			List<Supplier<EntityModel<CompanionRenderState>>> factories) {
		super(context, factories.get(0).get(), shadow);
		this.models = new ArrayList<>();

		for (var factory : factories) {
			models.add(factory.get());
			List<EntityModel<CompanionRenderState>> slots = new ArrayList<>();

			for (int slot = 0; slot < 3; slot++) {
				EntityModel<CompanionRenderState> m = factory.get();
				((CompanionLookModel) m).equipmentPass(slot);
				slots.add(m);
			}

			equipModels.add(slots);
		}

		this.shadow = shadow;
		this.stageScale = stageScale;

		for (int k = 0; k < 3; k++) {
			int stage = k + 1;
			String base = "textures/entity/companion/" + species + "_" + stage;

			for (int skin = 0; skin < CompanionLooks.SKINS.length; skin++) {
				textures[k][skin] = JTechStorage.id(base + (skin == 0 ? "" : "_" + CompanionLooks.SKINS[skin]) + ".png");
			}

			for (int m = 1; m <= 5; m++) {
				equipTextures[k][m] = JTechStorage.id(base + "_eq_" + CompanionLooks.MATERIALS[m] + ".png");
			}

			glowTint[k] = RenderTypes.eyes(JTechStorage.id(base + "_glow_tint.png"));
			glowCore[k] = RenderTypes.eyes(JTechStorage.id(base + "_glow_core.png"));
			RenderType glow = RenderTypes.eyes(JTechStorage.id("textures/entity/companion/" + species + "_" + stage + "_glow.png"));
			addLayer(new EyesLayer<>(this) {
				@Override
				public RenderType renderType() {
					return glow;
				}

				@Override
				public void submit(PoseStack pose, SubmitNodeCollector collector, int light, CompanionRenderState state, float yRot, float xRot) {
					if (state.stage == stage && !state.isInvisible && state.glow < 0) {
						super.submit(pose, collector, light, state, yRot, xRot);
					}
				}
			});
		}

		addLookLayer();
	}

	private void addLookLayer() {
		addLayer(new RenderLayer<>(this) {
			@Override
			public void submit(PoseStack pose, SubmitNodeCollector collector, int light, CompanionRenderState state, float yRot, float xRot) {
				if (state.isInvisible) {
					return;
				}

				int k = index(state);

				for (int slot = 0; slot < 3; slot++) {
					int material = state.equip[slot];

					if (material >= 1 && material <= 5) {
						renderColoredCutoutModel(equipModels.get(k).get(slot), equipTextures[k][material], pose, collector, light, state, -1, 1);
					}
				}

				if (state.glow >= 0) {
					// freely chosen glow color: grayscale layer tinted, bright core on top
					collector.order(1).submitModel(models.get(k), state, pose, glowTint[k], light, OverlayTexture.NO_OVERLAY, 0xFF000000 | state.glow,
							null, state.outlineColor);
					collector.order(2).submitModel(models.get(k), state, pose, glowCore[k], light, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF, null,
							state.outlineColor);
				}
			}
		});
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
		return textures[index(state)][Math.clamp(state.skin, 0, CompanionLooks.SKINS.length - 1)];
	}

	@Override
	public CompanionRenderState createRenderState() {
		return new CompanionRenderState();
	}

	/** On the player's shoulder, from the player's own view (first person): don't draw, it would block the view. */
	@Override
	public boolean shouldRender(T companion, Frustum frustum, double x, double y, double z, float partialTick) {
		Minecraft mc = Minecraft.getInstance();

		if (companion.isOnShoulder() && companion.getOwner() == mc.getCameraEntity() && mc.options.getCameraType().isFirstPerson()) {
			return false;
		}

		return companion.isOnShoulder() || super.shouldRender(companion, frustum, x, y, z, partialTick);
	}

	@Override
	public Vec3 getRenderOffset(CompanionRenderState state) {
		return state.shoulder ? state.shoulderOffset : super.getRenderOffset(state);
	}

	@Override
	public void extractRenderState(T companion, CompanionRenderState state, float partialTick) {
		super.extractRenderState(companion, state, partialTick);
		state.stage = companion.getStage();
		state.shoulder = false;
		int look = companion.getLook();
		int level = companion.getLevel();
		state.skin = CompanionLooks.get(look, CompanionLooks.SKIN);
		state.part1 = CompanionLooks.get(look, CompanionLooks.PART1);
		state.part2 = CompanionLooks.get(look, CompanionLooks.PART2);

		for (int slot = 0; slot < 3; slot++) {
			state.equip[slot] = CompanionLooks.shownMaterial(CompanionLooks.get(look, CompanionLooks.COLLAR + slot), level, state.stage);
		}

		state.glow = companion.getGlowColor();
		state.gesture = companion.gestureId(partialTick);
		state.gestureProgress = companion.gestureProgress(partialTick);

		if (companion.isOnShoulder() && companion.getOwner() instanceof LivingEntity owner) {
			// exactly on the owner's (interpolated) shoulder, independent of the companion's network lag
			float bodyYaw = Mth.rotLerp(partialTick, owner.yBodyRotO, owner.yBodyRot);
			Vec3 target = Companion.shoulderPos(owner, owner.getPosition(partialTick), bodyYaw, companion.shoulderSide());
			state.shoulder = true;
			state.shoulderOffset = target.subtract(state.x, state.y, state.z);
			state.bodyRot = bodyYaw;
			state.yRot = Mth.wrapDegrees(Mth.rotLerp(partialTick, owner.yHeadRotO, owner.yHeadRot) - bodyYaw) * 0.5f;
			state.shadowRadius = 0.0f;
			state.walkAnimationPos = 0.0f;       // sits still: no walk animation while the owner walks (v0.25.5)
			state.walkAnimationSpeed = 0.0f;
		}

		state.sitting = companion.isInSittingPose() || state.shoulder;
		state.exhausted = companion.isExhausted();
		state.flying = (companion instanceof RavenCompanion raven && raven.isFlying() && !state.sitting && !state.shoulder)
				|| (companion instanceof MantisCompanion && companion.isVehicle() && !companion.onGround());
		int attack = companion.getAttackTicks();
		state.attack = attack > 0 ? 1.0f - (attack - partialTick) / Companion.ATTACK_TICKS : 0.0f;
		int action = companion.getActionTicks();
		state.action = action > 0 ? 1.0f - (action - partialTick) / Companion.ACTION_TICKS : 0.0f;
	}

	@Override
	protected float getShadowRadius(CompanionRenderState state) {
		if (state.shoulder) {
			return 0.0f;
		}

		return shadow * (state.stage == 1 ? 0.6f : state.stage == 2 ? 0.85f : 1.1f) * stageScale[index(state)];
	}

	@Override
	protected void scale(CompanionRenderState state, PoseStack poseStack) {
		float scale = SCALE * stageScale[index(state)] * (state.shoulder ? 0.7f : 1.0f);   // slightly smaller on the shoulder
		poseStack.scale(scale, scale, scale);
	}
}
