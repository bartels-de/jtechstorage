package dev.bartels.jtechstorage.client.entity;

import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

import dev.bartels.jtechstorage.JTechStorage;
import dev.bartels.jtechstorage.entity.JbartelsHologram;

/** Jbartels hologram: translucent and flickering, with a multi-line speech bubble above the head. */
public class JbartelsHologramRenderer extends MobRenderer<JbartelsHologram, JbartelsHologramRenderState, JbartelsHologramModel> {
	private static final Identifier TEXTURE = JTechStorage.id("textures/entity/jbartels_hologram.png");
	private static final int BUBBLE_WIDTH = 190;

	private final Font font;

	public JbartelsHologramRenderer(EntityRendererProvider.Context context) {
		super(context, new JbartelsHologramModel(context.bakeLayer(JbartelsHologramModel.LAYER)), 0.0f);
		this.font = context.getFont();
		addLayer(new ItemInHandLayer<>(this));
	}

	@Override
	public Identifier getTextureLocation(JbartelsHologramRenderState state) {
		return TEXTURE;
	}

	@Override
	public JbartelsHologramRenderState createRenderState() {
		return new JbartelsHologramRenderState();
	}

	@Override
	public void extractRenderState(JbartelsHologram hologram, JbartelsHologramRenderState state, float partialTick) {
		super.extractRenderState(hologram, state, partialTick);
		ArmedEntityRenderState.extractArmedEntityRenderState(hologram, state, itemModelResolver, partialTick);
		state.standby = hologram.level().getNearestPlayer(hologram, 8.0) == null;
		state.bubble.clear();
		boolean milestone = hologram.isMilestone();
		String text = Component.translatable(milestone ? "jtechstorage.milestone." + hologram.getMilestone()
				: "jtechstorage.tutorial.step." + hologram.getStep()).getString();
		StringBuilder line = new StringBuilder();

		for (String word : text.split(" ")) {
			String candidate = line.isEmpty() ? word : line + " " + word;

			if (font.width(candidate) > BUBBLE_WIDTH && !line.isEmpty()) {
				state.bubble.add(Component.literal(line.toString()));
				line = new StringBuilder(word);
			} else {
				line = new StringBuilder(candidate);
			}
		}

		if (!line.isEmpty()) {
			state.bubble.add(Component.literal(line.toString()));
		}

		if (!milestone) {
			state.bubble.add(Component.translatable("jtechstorage.tutorial.hint", hologram.getStep() + 1, JbartelsHologram.STEP_COUNT)
					.withStyle(ChatFormatting.GRAY));
		}
	}

	@Override
	public void submit(JbartelsHologramRenderState state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		super.submit(state, pose, collector, camera);
		List<Component> lines = state.bubble;
		int count = lines.size();

		for (int i = 0; i < count; i++) {
			double y = 2.55 + (count - 1 - i) * 0.27;
			collector.submitNameTag(pose, new Vec3(0, y, 0), 0, lines.get(i), true, state.lightCoords, camera);
		}
	}

	@Override
	protected RenderType getRenderType(JbartelsHologramRenderState state, boolean bodyVisible, boolean translucent, boolean glowing) {
		return HologramRendering.renderType(TEXTURE);
	}

	@Override
	protected int getModelTint(JbartelsHologramRenderState state) {
		return HologramRendering.tint(state.ageInTicks) | 0x00FFFFFF;
	}

	@Override
	protected void scale(JbartelsHologramRenderState state, PoseStack poseStack) {
		poseStack.scale(0.5f, 0.5f, 0.5f);   // model uses double detail density
	}
}
