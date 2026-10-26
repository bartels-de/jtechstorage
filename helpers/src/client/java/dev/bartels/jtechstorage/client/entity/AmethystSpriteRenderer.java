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
import dev.bartels.jtechstorage.entity.AmethystSprite;

public class AmethystSpriteRenderer extends MobRenderer<AmethystSprite, AmethystSpriteRenderState, AmethystSpriteModel> {
	private static final Identifier TEXTURE = JTechStorage.id("textures/entity/amethyst_sprite.png");
	private static final Identifier ACCENT = JTechStorage.id("textures/entity/amethyst_sprite_accent.png");
	private static final RenderType GLOW = RenderTypes.eyes(JTechStorage.id("textures/entity/amethyst_sprite_glow.png"));

	public AmethystSpriteRenderer(EntityRendererProvider.Context context) {
		super(context, new AmethystSpriteModel(context.bakeLayer(AmethystSpriteModel.LAYER)), 0.3f);
		addLayer(new AccentLayer<>(this, ACCENT, state -> state.accent));
		addLayer(new HelperHatLayer<>(this));
		addLayer(new ItemInHandLayer<>(this));
		addLayer(new EyesLayer<>(this) {
			@Override
			public RenderType renderType() {
				return GLOW;
			}

			@Override
			public void submit(PoseStack pose, SubmitNodeCollector collector, int light, AmethystSpriteRenderState state, float yRot, float xRot) {
				if (!state.resting && !state.hologram) {
					super.submit(pose, collector, light, state, yRot, xRot);
				}
			}
		});
	}

	@Override
	public Identifier getTextureLocation(AmethystSpriteRenderState state) {
		return TEXTURE;
	}

	@Override
	protected RenderType getRenderType(AmethystSpriteRenderState state, boolean bodyVisible, boolean translucent, boolean glowing) {
		return state.hologram ? HologramRendering.renderType(getTextureLocation(state)) : super.getRenderType(state, bodyVisible, translucent, glowing);
	}

	@Override
	protected int getModelTint(AmethystSpriteRenderState state) {
		return state.hologram ? HologramRendering.tint(state.ageInTicks) : super.getModelTint(state);
	}

	@Override
	public AmethystSpriteRenderState createRenderState() {
		return new AmethystSpriteRenderState();
	}

	@Override
	public void extractRenderState(AmethystSprite sprite, AmethystSpriteRenderState state, float partialTick) {
		super.extractRenderState(sprite, state, partialTick);
		state.accent = sprite.getAccentColor();
		state.hologram = sprite.isHologram();

		if (state.hologram) {
			state.accent = -1;
		}
		state.resting = sprite.isResting();
		state.action = sprite.getActionAmount(partialTick);
		ArmedEntityRenderState.extractArmedEntityRenderState(sprite, state, itemModelResolver, partialTick);
		state.carrying = !sprite.getMainHandItem().isEmpty();
	}

	/** Glows like amethyst; always at least half bright. */
	@Override
	protected int getBlockLightLevel(AmethystSprite sprite, BlockPos pos) {
		return Math.max(10, super.getBlockLightLevel(sprite, pos));
	}

	/** Model is built at double detail density. */
	@Override
	protected void scale(AmethystSpriteRenderState state, PoseStack poseStack) {
		poseStack.scale(0.5f, 0.5f, 0.5f);
	}
}
