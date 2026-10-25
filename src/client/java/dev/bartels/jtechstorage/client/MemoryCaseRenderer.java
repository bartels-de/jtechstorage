package dev.bartels.jtechstorage.client;

import org.jspecify.annotations.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import dev.bartels.jtechstorage.depths.memory.MemoryCaseBlock;
import dev.bartels.jtechstorage.depths.memory.MemoryCaseBlockEntity;
import dev.bartels.jtechstorage.depths.memory.MemoryContent;

/** Erinnerungsvitrine (v0.36): die ausgestellten Kristalle auf drei Böden zu je vier, langsam schwebend. */
public class MemoryCaseRenderer implements BlockEntityRenderer<MemoryCaseBlockEntity, MemoryCaseRenderer.State> {
	private static final float[] SHELF = {2.0f / 16, 7.0f / 16, 12.0f / 16};
	private final ItemModelResolver items;

	public MemoryCaseRenderer(BlockEntityRendererProvider.Context context) {
		items = context.itemModelResolver();
	}

	public static class State extends BlockEntityRenderState {
		final ItemStackRenderState[] crystals = new ItemStackRenderState[MemoryContent.KINDS.length];
		Direction facing = Direction.NORTH;
		float time;

		State() {
			for (int i = 0; i < crystals.length; i++) {
				crystals[i] = new ItemStackRenderState();
			}
		}
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(MemoryCaseBlockEntity memory, State state, float partialTick, Vec3 camera,
			ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(memory, state, crumbling);
		var level = memory.getLevel();
		state.facing = memory.getBlockState().hasProperty(MemoryCaseBlock.FACING) ? memory.getBlockState().getValue(MemoryCaseBlock.FACING) : Direction.NORTH;
		state.time = level == null ? 0.0f : (level.getGameTime() % 240000L) + partialTick;
		int mask = memory.mask();

		for (int i = 0; i < state.crystals.length; i++) {
			state.crystals[i].clear();

			if (level != null && (mask & (1 << i)) != 0) {
				items.updateForTopItem(state.crystals[i], new ItemStack(MemoryContent.CRYSTALS[i]), ItemDisplayContext.FIXED, level, null, i);
			}
		}
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		pose.pushPose();
		pose.translate(0.5f, 0.0f, 0.5f);
		pose.rotateDegrees(Axis.YP, -state.facing.toYRot());

		for (int i = 0; i < state.crystals.length; i++) {
			if (state.crystals[i].isEmpty()) {
				continue;
			}

			int row = i / 4;
			int col = i % 4;
			float bob = (float) Math.sin(state.time * 0.06f + i * 0.9f) * 0.012f;
			pose.pushPose();
			pose.translate(-0.5f + (2.5f + col * 3.67f) / 16.0f, SHELF[row] + 0.11f + bob, 0.0f);
			pose.rotateDegrees(Axis.YP, 180.0f + (float) Math.sin(state.time * 0.03f + i) * 25.0f);
			pose.scale(0.27f, 0.27f, 0.27f);
			state.crystals[i].submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
			pose.popPose();
		}

		pose.popPose();
	}
}
