package dev.bartels.jtechstorage.client.entity;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

import dev.bartels.jtechstorage.JTechStorage;
import dev.bartels.jtechstorage.depths.entity.VeinCrawler;

/** Crystal Depths (v0.31). Model at double detail density, glowing parts as a separate layer. */
public class VeinCrawlerRenderer extends MobRenderer<VeinCrawler, VeinCrawlerRenderState, VeinCrawlerModel> {
	private static final Identifier TEXTURE = JTechStorage.id("textures/entity/vein_crawler.png");
	private static final RenderType GLOW = RenderTypes.eyes(JTechStorage.id("textures/entity/vein_crawler_glow.png"));

	public VeinCrawlerRenderer(EntityRendererProvider.Context context) {
		super(context, new VeinCrawlerModel(context.bakeLayer(VeinCrawlerModel.LAYER)), 0.7f);
		addLayer(new EyesLayer<>(this) {
			@Override
			public RenderType renderType() {
				return GLOW;
			}
		});
	}

	@Override
	public Identifier getTextureLocation(VeinCrawlerRenderState state) {
		return TEXTURE;
	}

	@Override
	public VeinCrawlerRenderState createRenderState() {
		return new VeinCrawlerRenderState();
	}

	@Override
	public void extractRenderState(VeinCrawler mob, VeinCrawlerRenderState state, float partialTick) {
		super.extractRenderState(mob, state, partialTick);
		state.spit = mob.getSpitTicks() > 0 ? 1.0f - (mob.getSpitTicks() - partialTick) / VeinCrawler.SPIT_TICKS : 0.0f;
	}

	@Override
	protected void scale(VeinCrawlerRenderState state, PoseStack poseStack) {
		poseStack.scale(0.5f, 0.5f, 0.5f);   // model at double detail density
	}
}
