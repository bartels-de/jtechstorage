package dev.bartels.jtechstorage.client.entity;

import com.mojang.blaze3d.vertex.PoseStack;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;

import dev.bartels.jtechstorage.JTechStorage;
import dev.bartels.jtechstorage.backpack.BackpackItem;

/** Shows a backpack worn in the chest slot on the player's back (cloth tinted + trims per tier). */
public class BackpackLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
	private static final Identifier FABRIC = JTechStorage.id("textures/entity/backpack/backpack_fabric.png");

	/** Trim textures per tier, created lazily so no Identifier is allocated per frame. */
	private static final Map<BackpackItem.Tier, Identifier> TRIMS = new EnumMap<>(BackpackItem.Tier.class);

	private final BackpackModel model;

	public BackpackLayer(RenderLayerParent<AvatarRenderState, PlayerModel> parent, BackpackModel model) {
		super(parent);
		this.model = model;
	}

	@Override
	public void submit(PoseStack pose, SubmitNodeCollector collector, int light, AvatarRenderState state, float yRot, float xRot) {
		ItemStack stack = state.chestEquipment;

		if (!(stack.getItem() instanceof BackpackItem backpack) || state.isInvisible) {
			return;
		}

		BackpackItem.Tier tier = backpack.getTier();
		DyedItemColor dyed = stack.get(DataComponents.DYED_COLOR);
		int color = 0xFF000000 | (dyed != null ? dyed.rgb() : tier.defaultColor);
		Identifier trim = TRIMS.computeIfAbsent(tier,
				t -> JTechStorage.id("textures/entity/backpack/backpack_" + t.name().toLowerCase(Locale.ROOT) + ".png"));

		pose.pushPose();
		getParentModel().root().translateAndRotate(pose);
		getParentModel().body.translateAndRotate(pose);
		renderColoredCutoutModel(model, FABRIC, pose, collector, light, state, color, 1);
		renderColoredCutoutModel(model, trim, pose, collector, light, state, 0xFFFFFFFF, 2);
		pose.popPose();
	}
}
