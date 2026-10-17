package dev.bartels.jtechstorage.client.entity;

import com.mojang.blaze3d.vertex.PoseStack;

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
 * Ashley: flauschiger Ascheball aus Fell-Büscheln mit Glutflecken, großen Augen, Amethyst-Haarspange und aufsteigenden
 * Rauchwölkchen. Watschelt, niest und schläft zusammengesackt.
 * Modell in doppelter Detaildichte (halbe Pixel) – der Renderer skaliert es auf 0.5.
 * Erzeugt von scratchpad/ashley2.py (Teile und Posen), Animation von Hand.
 */
public class AshleyModel extends EntityModel<AshleyRenderState> implements HelperHatModel {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(JTechStorage.id("ashley"), "main");

	private final ModelPart ashley;
	private final ModelPart body;
	private final ModelPart lids;
	private final ModelPart rArm;
	private final ModelPart lArm;
	private final ModelPart rFoot;
	private final ModelPart lFoot;
	private final ModelPart bow;
	private final ModelPart smoke;
	private final ModelPart puff1;
	private final ModelPart puff2;
	private final ModelPart puff3;

	public AshleyModel(ModelPart root) {
		super(root);
		this.ashley = root.getChild("ashley");
		this.body = root.getChild("ashley").getChild("body");
		this.lids = root.getChild("ashley").getChild("body").getChild("lids");
		this.rArm = root.getChild("ashley").getChild("body").getChild("r_arm");
		this.lArm = root.getChild("ashley").getChild("body").getChild("l_arm");
		this.rFoot = root.getChild("r_foot");
		this.lFoot = root.getChild("l_foot");
		this.bow = root.getChild("ashley").getChild("body").getChild("bow");
		this.smoke = root.getChild("ashley").getChild("body").getChild("smoke");
		this.puff1 = root.getChild("ashley").getChild("body").getChild("smoke").getChild("puff1");
		this.puff2 = root.getChild("ashley").getChild("body").getChild("smoke").getChild("puff1").getChild("puff2");
		this.puff3 = root.getChild("ashley").getChild("body").getChild("smoke").getChild("puff1").getChild("puff2").getChild("puff3");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		PartDefinition p1 = root.addOrReplaceChild("ashley", CubeListBuilder.create(),
				PartPose.offset(0.0f, 24.0f, 0.0f));
		PartDefinition p2 = p1.addOrReplaceChild("body", CubeListBuilder.create()
				.texOffs(0, 0).addBox(-7.0f, -12.0f, -6.5f, 14.0f, 12.0f, 13.0f, new CubeDeformation(0.0f))
				.texOffs(78, 0).addBox(-4.5f, -14.5f, -4.5f, 9.0f, 4.0f, 9.0f, new CubeDeformation(0.3f))
				.texOffs(54, 0).addBox(-9.0f, -10.0f, -4.5f, 3.0f, 8.0f, 9.0f, new CubeDeformation(0.0f))
				.texOffs(54, 0).mirror().addBox(6.0f, -10.0f, -4.5f, 3.0f, 8.0f, 9.0f, new CubeDeformation(0.0f)).mirror(false)
				.texOffs(114, 0).addBox(-5.0f, -11.0f, 5.5f, 10.0f, 7.0f, 3.0f, new CubeDeformation(0.0f))
				.texOffs(198, 0).addBox(-5.0f, -2.4f, -7.4f, 10.0f, 3.0f, 2.0f, new CubeDeformation(0.0f))
				.texOffs(12, 25).addBox(-6.4f, -5.2f, -6.9f, 2.0f, 2.0f, 1.0f, new CubeDeformation(0.0f))
				.texOffs(12, 25).mirror().addBox(4.4f, -5.2f, -6.9f, 2.0f, 2.0f, 1.0f, new CubeDeformation(0.0f)).mirror(false),
				PartPose.offset(0.0f, -3.0f, 0.0f));
		p2.addOrReplaceChild("lids", CubeListBuilder.create()
				.texOffs(222, 0).addBox(-5.0f, -9.2f, -7.1f, 10.0f, 4.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.ZERO);
		p2.addOrReplaceChild("r_arm", CubeListBuilder.create()
				.texOffs(174, 0).addBox(-1.5f, -0.5f, -1.5f, 3.0f, 5.0f, 3.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(-8.4f, -5.0f, -1.0f, 0.0f, 0.0f, 0.5f));
		p2.addOrReplaceChild("l_arm", CubeListBuilder.create()
				.texOffs(174, 0).mirror().addBox(-1.5f, -0.5f, -1.5f, 3.0f, 5.0f, 3.0f, new CubeDeformation(0.0f)).mirror(false),
				PartPose.offsetAndRotation(8.4f, -5.0f, -1.0f, 0.0f, 0.0f, -0.5f));
		p2.addOrReplaceChild("bow", CubeListBuilder.create()
				.texOffs(244, 0).addBox(-1.5f, -1.5f, -1.0f, 3.0f, 3.0f, 2.0f, new CubeDeformation(0.0f))
				.texOffs(18, 25).addBox(-3.4f, -1.0f, -0.5f, 2.0f, 2.0f, 1.0f, new CubeDeformation(0.0f))
				.texOffs(18, 25).addBox(1.4f, -1.0f, -0.5f, 2.0f, 2.0f, 1.0f, new CubeDeformation(0.0f))
				.texOffs(8, 25).addBox(-0.5f, 1.0f, -0.5f, 1.0f, 3.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(4.6f, -13.2f, -3.2f, 0.0f, 0.0f, 0.35f));
		PartDefinition p7 = p2.addOrReplaceChild("smoke", CubeListBuilder.create(),
				PartPose.offset(-1.5f, -15.0f, 1.0f));
		PartDefinition p8 = p7.addOrReplaceChild("puff1", CubeListBuilder.create()
				.texOffs(158, 0).addBox(-2.0f, -4.0f, -2.0f, 4.0f, 4.0f, 4.0f, new CubeDeformation(0.0f)),
				PartPose.ZERO);
		PartDefinition p9 = p8.addOrReplaceChild("puff2", CubeListBuilder.create()
				.texOffs(186, 0).addBox(-1.5f, -3.0f, -1.5f, 3.0f, 3.0f, 3.0f, new CubeDeformation(0.0f)),
				PartPose.offset(1.2f, -4.6f, 0.4f));
		p9.addOrReplaceChild("puff3", CubeListBuilder.create()
				.texOffs(0, 25).addBox(-1.0f, -2.0f, -1.0f, 2.0f, 2.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offset(-0.8f, -3.6f, -0.2f));
		root.addOrReplaceChild("r_foot", CubeListBuilder.create()
				.texOffs(140, 0).addBox(-2.0f, 0.0f, -3.0f, 4.0f, 3.0f, 5.0f, new CubeDeformation(0.0f)),
				PartPose.offset(-3.6f, 21.0f, 0.0f));
		root.addOrReplaceChild("l_foot", CubeListBuilder.create()
				.texOffs(140, 0).mirror().addBox(-2.0f, 0.0f, -3.0f, 4.0f, 3.0f, 5.0f, new CubeDeformation(0.0f)).mirror(false),
				PartPose.offset(3.6f, 21.0f, 0.0f));
		return LayerDefinition.create(mesh, 256, 128);
	}

	@Override
	public void setupAnim(AshleyRenderState state) {
		super.setupAnim(state);
		float age = state.ageInTicks;
		lids.visible = state.resting;

		// Rauch steigt und wabert
		puff1.y += Mth.sin(age * 0.08f) * 0.6f;
		puff2.x += Mth.sin(age * 0.11f) * 0.5f;
		puff3.x += Mth.sin(age * 0.13f + 1.0f) * 0.7f;
		puff3.y += Mth.sin(age * 0.09f) * 0.5f;
		smoke.yRot += age * 0.02f;

		if (state.resting) {
			poseSleep(1.0f);
			float breath = Mth.sin(age * 0.06f);
			body.yScale = 0.9f + breath * 0.03f;
			body.xScale = 1.06f - breath * 0.02f;
			body.zScale = 1.06f - breath * 0.02f;
			return;
		}

		float t = state.walkAnimationPos * 1.2f;
		float amp = Math.min(1.0f, state.walkAnimationSpeed * 1.5f);
		poseWalk(Mth.sin(t) * amp);
		body.y += -Mth.abs(Mth.cos(t)) * 0.8f * amp;
		float breath = Mth.sin(age * 0.09f);
		body.yScale = 1.0f + breath * 0.02f;
		rArm.zRot += Mth.sin(age * 0.07f) * 0.08f;
		lArm.zRot -= Mth.sin(age * 0.07f) * 0.08f;
		bow.zRot += Mth.sin(age * 0.05f) * 0.05f;

		if (state.action > 0.0f) {
			poseSneeze(state.action);
		}
	}

	private void poseWalk(float k) {
		body.zRot += k * 0.120f;
		rFoot.xRot += k * -0.500f;
		lFoot.xRot += k * 0.500f;
		rArm.xRot += k * 0.500f;
		lArm.xRot += k * -0.500f;
	}

	private void poseSleep(float k) {
		body.y += k * 1.500f;
		body.xRot += k * 0.120f;
		rArm.zRot += k * -0.350f;
		rArm.xRot += k * -0.400f;
		lArm.zRot += k * 0.350f;
		lArm.xRot += k * -0.400f;
		smoke.y += k * 3.000f;
		puff2.x += k * -0.600f;
		bow.zRot += k * 0.250f;
	}

	private void poseSneeze(float k) {
		body.xRot += k * 0.450f;
		body.y += k * 0.800f;
		rArm.xRot += k * -1.200f;
		lArm.xRot += k * -1.200f;
	}

	@Override
	public void translateToHat(PoseStack pose) {
		root().translateAndRotate(pose);
		ashley.translateAndRotate(pose);
		body.translateAndRotate(pose);
	}

	@Override
	public float hatScale() {
		return 1.6f;
	}
}
