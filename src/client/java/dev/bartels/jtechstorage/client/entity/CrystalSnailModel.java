package dev.bartels.jtechstorage.client.entity;


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
 * Kristallschnecke: weicher grauer Fuß, Haus aus Amethyst mit wachsenden Kristallen, leuchtende Stielaugen.
 * Modell in doppelter Detaildichte (halbe Pixel) – der Renderer skaliert es auf 0.5.
 * Erzeugt von scratchpad/depthmobs.py (Teile und Posen), Animation von Hand.
 */
public class CrystalSnailModel extends EntityModel<CrystalSnailRenderState> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(JTechStorage.id("crystal_snail"), "main");

	private final ModelPart snail;
	private final ModelPart foot;
	private final ModelPart head;
	private final ModelPart face;
	private final ModelPart rStalk;
	private final ModelPart lStalk;
	private final ModelPart shell;
	private final ModelPart crys0;

	public CrystalSnailModel(ModelPart root) {
		super(root);
		this.snail = root.getChild("snail");
		this.foot = root.getChild("snail").getChild("foot");
		this.head = root.getChild("snail").getChild("foot").getChild("head");
		this.face = root.getChild("snail").getChild("foot").getChild("head").getChild("face");
		this.rStalk = root.getChild("snail").getChild("foot").getChild("head").getChild("face").getChild("r_stalk");
		this.lStalk = root.getChild("snail").getChild("foot").getChild("head").getChild("face").getChild("l_stalk");
		this.shell = root.getChild("snail").getChild("foot").getChild("shell");
		this.crys0 = root.getChild("snail").getChild("foot").getChild("shell").getChild("crys0");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		PartDefinition p1 = root.addOrReplaceChild("snail", CubeListBuilder.create(),
				PartPose.offset(0.0f, 24.0f, 0.0f));
		PartDefinition p2 = p1.addOrReplaceChild("foot", CubeListBuilder.create()
				.texOffs(52, 0).addBox(-4.0f, 0.0f, -10.0f, 8.0f, 5.0f, 20.0f, new CubeDeformation(0.0f)),
				PartPose.offset(0.0f, -5.0f, 0.0f));
		PartDefinition p3 = p2.addOrReplaceChild("head", CubeListBuilder.create()
				.texOffs(0, 26).addBox(-3.0f, -7.0f, -2.5f, 6.0f, 7.0f, 5.0f, new CubeDeformation(0.0f)),
				PartPose.offset(0.0f, 0.0f, -8.0f));
		PartDefinition p4 = p3.addOrReplaceChild("face", CubeListBuilder.create()
				.texOffs(38, 26).addBox(-3.0f, -5.0f, -4.0f, 6.0f, 5.0f, 6.0f, new CubeDeformation(0.0f)),
				PartPose.offset(0.0f, -6.0f, 0.0f));
		p4.addOrReplaceChild("r_stalk", CubeListBuilder.create()
				.texOffs(74, 26).addBox(-0.5f, -6.0f, -0.5f, 1.0f, 6.0f, 1.0f, new CubeDeformation(0.0f))
				.texOffs(78, 26).addBox(-1.0f, -8.0f, -1.0f, 2.0f, 2.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(-2.0f, -4.0f, -2.0f, -0.2f, 0.0f, -0.2f));
		p4.addOrReplaceChild("l_stalk", CubeListBuilder.create()
				.texOffs(74, 26).addBox(-0.5f, -6.0f, -0.5f, 1.0f, 6.0f, 1.0f, new CubeDeformation(0.0f))
				.texOffs(78, 26).addBox(-1.0f, -8.0f, -1.0f, 2.0f, 2.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(2.0f, -4.0f, -2.0f, -0.2f, 0.0f, 0.2f));
		PartDefinition p7 = p2.addOrReplaceChild("shell", CubeListBuilder.create()
				.texOffs(0, 0).addBox(-6.0f, -12.0f, -7.0f, 12.0f, 12.0f, 14.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, 0.0f, 2.5f, -0.1f, 0.0f, 0.0f));
		p7.addOrReplaceChild("crys0", CubeListBuilder.create()
				.texOffs(22, 26).addBox(-2.0f, -8.0f, -2.0f, 4.0f, 8.0f, 4.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, -11.0f, 0.0f, 0.15f, 0.0f, 0.0f));
		p7.addOrReplaceChild("crys1", CubeListBuilder.create()
				.texOffs(62, 26).addBox(-1.5f, -6.0f, -1.5f, 3.0f, 6.0f, 3.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(-4.0f, -10.0f, 3.0f, 0.3f, 0.0f, -0.4f));
		p7.addOrReplaceChild("crys2", CubeListBuilder.create()
				.texOffs(62, 26).addBox(-1.5f, -6.0f, -1.5f, 3.0f, 6.0f, 3.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(4.0f, -10.0f, -2.0f, -0.2f, 0.0f, 0.45f));
		p7.addOrReplaceChild("crys3", CubeListBuilder.create()
				.texOffs(62, 26).addBox(-1.5f, -6.0f, -1.5f, 3.0f, 6.0f, 3.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(1.0f, -10.0f, 5.0f, 0.6f, 0.0f, 0.0f));
		return LayerDefinition.create(mesh, 128, 64);
	}

	@Override
	public void setupAnim(CrystalSnailRenderState state) {
		super.setupAnim(state);
		float age = state.ageInTicks;
		float pos = state.walkAnimationPos;
		float speed = Math.min(state.walkAnimationSpeed, 1.0f);
		face.yRot += Mth.clamp(state.yRot, -40.0f, 40.0f) * Mth.DEG_TO_RAD * 0.5f;
		head.z += Mth.sin(pos * 0.8f) * 0.6f * speed;
		shell.yRot += Mth.sin(pos * 0.8f) * 0.04f * speed;
		rStalk.xRot += Mth.sin(age * 0.1f) * 0.15f;
		lStalk.xRot += Mth.sin(age * 0.1f + 1.0f) * 0.15f;
		rStalk.zRot += Mth.sin(age * 0.07f) * 0.1f;
		lStalk.zRot -= Mth.sin(age * 0.07f + 0.5f) * 0.1f;

		if (state.hide > 0.0f) {
			poseHide(state.hide);
		}
	}

	private void poseHide(float k) {
		head.z += k * 6.000f;
		head.y += k * 3.000f;
		head.xRot += k * 0.400f;
		rStalk.xRot += k * 0.800f;
		lStalk.xRot += k * 0.800f;
	}


}
