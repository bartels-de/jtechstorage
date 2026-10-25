package dev.bartels.jtechstorage.client.cosmetic;

import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

import dev.bartels.jtechstorage.JTechStorage;

/**
 * Prismen-Orbs: drei Farbkristalle (Azur, Rosen, Citrin), die hinter dem Rücken kreisen.
 * Modell in doppelter Detaildichte (halbe Pixel) – der Renderer skaliert es auf 0.5.
 * Erzeugt von scratchpad/cosmetics.py (Teile und Posen), Animation von Hand.
 */
public class PrismOrbsModel extends EntityModel<AvatarRenderState> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(JTechStorage.id("cosmetic_prism_orbs"), "main");

	private final ModelPart orbs;
	private final ModelPart orbA;
	private final ModelPart orbB;
	private final ModelPart orbC;

	public PrismOrbsModel(ModelPart root) {
		super(root);
		this.orbs = root.getChild("orbs");
		this.orbA = root.getChild("orbs").getChild("orb_a");
		this.orbB = root.getChild("orbs").getChild("orb_b");
		this.orbC = root.getChild("orbs").getChild("orb_c");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		PartDefinition p1 = root.addOrReplaceChild("orbs", CubeListBuilder.create(),
				PartPose.offset(0.0f, 8.0f, 0.0f));
		p1.addOrReplaceChild("orb_a", CubeListBuilder.create()
				.texOffs(0, 0).addBox(-2.0f, -4.0f, -2.0f, 4.0f, 8.0f, 4.0f, new CubeDeformation(0.0f))
				.texOffs(48, 0).addBox(-1.0f, -7.0f, -1.0f, 2.0f, 3.0f, 2.0f, new CubeDeformation(0.0f))
				.texOffs(48, 0).addBox(-1.0f, 4.0f, -1.0f, 2.0f, 3.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offset(-12.0f, -2.0f, 12.0f));
		p1.addOrReplaceChild("orb_b", CubeListBuilder.create()
				.texOffs(16, 0).addBox(-2.0f, -4.0f, -2.0f, 4.0f, 8.0f, 4.0f, new CubeDeformation(0.0f))
				.texOffs(56, 0).addBox(-1.0f, -7.0f, -1.0f, 2.0f, 3.0f, 2.0f, new CubeDeformation(0.0f))
				.texOffs(56, 0).addBox(-1.0f, 4.0f, -1.0f, 2.0f, 3.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offset(0.0f, -8.0f, 12.0f));
		p1.addOrReplaceChild("orb_c", CubeListBuilder.create()
				.texOffs(32, 0).addBox(-2.0f, -4.0f, -2.0f, 4.0f, 8.0f, 4.0f, new CubeDeformation(0.0f))
				.texOffs(0, 12).addBox(-1.0f, -7.0f, -1.0f, 2.0f, 3.0f, 2.0f, new CubeDeformation(0.0f))
				.texOffs(0, 12).addBox(-1.0f, 4.0f, -1.0f, 2.0f, 3.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offset(12.0f, -2.0f, 12.0f));
		return LayerDefinition.create(mesh, 64, 64);
	}

	@Override
	public void setupAnim(AvatarRenderState state) {
		super.setupAnim(state);
		float age = state.ageInTicks;
		ModelPart[] all = {orbA, orbB, orbC};

		for (int k = 0; k < 3; k++) {
			float angle = age * 0.04f + k * 2.0944f;
			all[k].x = Mth.sin(angle) * 13.0f;
			all[k].z = 12.0f + Mth.cos(angle) * 4.0f;
			all[k].y = -3.0f + Mth.sin(age * 0.09f + k * 1.7f) * 2.0f;
			all[k].yRot = age * 0.08f + k;
			all[k].zRot = Mth.sin(age * 0.05f + k) * 0.15f;
		}
	}




}
