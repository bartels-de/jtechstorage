package dev.bartels.jtechstorage.client.entity;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

import dev.bartels.jtechstorage.JTechStorage;
import dev.bartels.jtechstorage.depths.entity.GeodeSheep;

/** Kristalltiefe (v0.36). Modell in doppelter Detaildichte, leuchtende Teile als eigene Schicht. */
public class GeodeSheepRenderer extends MobRenderer<GeodeSheep, GeodeSheepRenderState, GeodeSheepModel> {
	private static final Identifier TEXTURE = JTechStorage.id("textures/entity/geode_sheep.png");
	private static final RenderType GLOW = RenderTypes.eyes(JTechStorage.id("textures/entity/geode_sheep_glow.png"));

	public GeodeSheepRenderer(EntityRendererProvider.Context context) {
		super(context, new GeodeSheepModel(context.bakeLayer(GeodeSheepModel.LAYER)), 0.6f);
		addLayer(new EyesLayer<>(this) {
			@Override
			public RenderType renderType() {
				return GLOW;
			}
		});
		addLayer(new WoolLayer(this, new GeodeSheepWoolModel(context.bakeLayer(GeodeSheepWoolModel.LAYER))));
	}

	@Override
	public Identifier getTextureLocation(GeodeSheepRenderState state) {
		return TEXTURE;
	}

	@Override
	public GeodeSheepRenderState createRenderState() {
		return new GeodeSheepRenderState();
	}

	@Override
	public void extractRenderState(GeodeSheep mob, GeodeSheepRenderState state, float partialTick) {
		super.extractRenderState(mob, state, partialTick);
		state.sheared = mob.isSheared();
		state.woolColor = 0xFF000000 | mob.getColor().getTextureDiffuseColor();
		state.graze = mob.getGrazeAmount(partialTick);
	}

	@Override
	protected void scale(GeodeSheepRenderState state, PoseStack poseStack) {
		float s = 0.5f * state.ageScale;   // Modell in doppelter Detaildichte, Junge halb so groß
		poseStack.scale(s, s, s);
	}

	/** Wolle in der Schaffarbe (nicht, wenn geschoren). */
	private static final class WoolLayer extends RenderLayer<GeodeSheepRenderState, GeodeSheepModel> {
		private static final Identifier WOOL = JTechStorage.id("textures/entity/geode_sheep_wool.png");
		private final GeodeSheepWoolModel model;

		WoolLayer(GeodeSheepRenderer parent, GeodeSheepWoolModel model) {
			super(parent);
			this.model = model;
		}

		@Override
		public void submit(PoseStack pose, SubmitNodeCollector collector, int light, GeodeSheepRenderState state, float yRot, float xRot) {
			if (state.sheared || state.isInvisible) {
				return;
			}

			coloredCutoutModelCopyLayerRender(model, WOOL, pose, collector, light, state, state.woolColor, 1);
		}
	}
}
