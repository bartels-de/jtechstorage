package dev.bartels.jtechstorage.client.entity;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

import dev.bartels.jtechstorage.JTechStorage;
import dev.bartels.jtechstorage.entity.AmethystSpider;

public class AmethystSpiderRenderer extends MobRenderer<AmethystSpider, AmethystSpiderRenderState, AmethystSpiderModel> {
	private static final Identifier TEXTURE = JTechStorage.id("textures/entity/amethyst_spider.png");
	private static final RenderType GLOW = RenderTypes.eyes(JTechStorage.id("textures/entity/amethyst_spider_glow.png"));
	private static final float SCALE = 1.7f;

	public AmethystSpiderRenderer(EntityRendererProvider.Context context) {
		super(context, new AmethystSpiderModel(context.bakeLayer(AmethystSpiderModel.LAYER)), 2.4f);
		addLayer(new EyesLayer<>(this) {
			@Override
			public RenderType renderType() {
				return GLOW;
			}
		});
	}

	@Override
	public Identifier getTextureLocation(AmethystSpiderRenderState state) {
		return TEXTURE;
	}

	@Override
	public AmethystSpiderRenderState createRenderState() {
		return new AmethystSpiderRenderState();
	}

	private static int duration(int anim) {
		return switch (anim) {
			case AmethystSpider.ANIM_BITE -> 10;
			case AmethystSpider.ANIM_LEAP -> 12;
			case AmethystSpider.ANIM_WEB -> 16;
			case AmethystSpider.ANIM_SPIKES -> 20;
			case AmethystSpider.ANIM_SHRIEK -> 30;
			case AmethystSpider.ANIM_BROOD -> 25;
			default -> 1;
		};
	}

	@Override
	public void extractRenderState(AmethystSpider spider, AmethystSpiderRenderState state, float partialTick) {
		super.extractRenderState(spider, state, partialTick);
		state.anim = spider.getAnim();
		int total = duration(state.anim);
		state.progress = state.anim == 0 ? 0.0f : Math.clamp(1.0f - (spider.getAnimTicks() - partialTick) / total, 0.0f, 1.0f);
		state.enraged = spider.isEnraged();
		state.leaping = spider.isLeaping();
	}

	@Override
	protected int getModelTint(AmethystSpiderRenderState state) {
		return state.enraged ? 0xFFFFC4E8 : super.getModelTint(state);
	}

	@Override
	protected float getFlipDegrees() {
		return 180.0f;
	}

	@Override
	protected void scale(AmethystSpiderRenderState state, PoseStack poseStack) {
		poseStack.scale(SCALE, SCALE, SCALE);
	}
}
