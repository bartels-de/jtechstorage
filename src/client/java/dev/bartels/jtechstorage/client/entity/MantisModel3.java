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
 * Schattenmantis, Stufe 3 (Begleiter-Redesign v0.25.5).
 * Modell in doppelter Detaildichte (halbe Pixel) – der Renderer skaliert es auf 0.5.
 * Erzeugt von scratchpad/mantis2.py (Teile und Posen), Animation von Hand.
 */
public class MantisModel3 extends EntityModel<CompanionRenderState> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(JTechStorage.id("mantis_3"), "main");

	private final ModelPart mantis;
	private final ModelPart body;
	private final ModelPart neck;
	private final ModelPart head;
	private final ModelPart rAntenna;
	private final ModelPart lAntenna;
	private final ModelPart abdomen;
	private final ModelPart rArm;
	private final ModelPart lArm;
	private final ModelPart rFemur;
	private final ModelPart lFemur;
	private final ModelPart rBlade;
	private final ModelPart lBlade;
	private final ModelPart rWing;
	private final ModelPart lWing;
	private final ModelPart rJaw;
	private final ModelPart lJaw;
	private final ModelPart rmLeg;
	private final ModelPart lmLeg;
	private final ModelPart rhLeg;
	private final ModelPart lhLeg;
	private final ModelPart rmShin;
	private final ModelPart lmShin;
	private final ModelPart rhShin;
	private final ModelPart lhShin;

	public MantisModel3(ModelPart root) {
		super(root);
		this.mantis = root.getChild("mantis");
		this.body = root.getChild("mantis").getChild("body");
		this.neck = root.getChild("mantis").getChild("body").getChild("neck");
		this.head = root.getChild("mantis").getChild("body").getChild("neck").getChild("head");
		this.rAntenna = root.getChild("mantis").getChild("body").getChild("neck").getChild("head").getChild("r_antenna");
		this.lAntenna = root.getChild("mantis").getChild("body").getChild("neck").getChild("head").getChild("l_antenna");
		this.abdomen = root.getChild("mantis").getChild("body").getChild("abdomen");
		this.rArm = root.getChild("mantis").getChild("body").getChild("neck").getChild("r_arm");
		this.lArm = root.getChild("mantis").getChild("body").getChild("neck").getChild("l_arm");
		this.rFemur = root.getChild("mantis").getChild("body").getChild("neck").getChild("r_arm").getChild("r_femur");
		this.lFemur = root.getChild("mantis").getChild("body").getChild("neck").getChild("l_arm").getChild("l_femur");
		this.rBlade = root.getChild("mantis").getChild("body").getChild("neck").getChild("r_arm").getChild("r_femur").getChild("r_blade");
		this.lBlade = root.getChild("mantis").getChild("body").getChild("neck").getChild("l_arm").getChild("l_femur").getChild("l_blade");
		this.rWing = root.getChild("mantis").getChild("body").getChild("r_wing");
		this.lWing = root.getChild("mantis").getChild("body").getChild("l_wing");
		this.rJaw = root.getChild("mantis").getChild("body").getChild("neck").getChild("head").getChild("r_jaw");
		this.lJaw = root.getChild("mantis").getChild("body").getChild("neck").getChild("head").getChild("l_jaw");
		this.rmLeg = root.getChild("mantis").getChild("body").getChild("rm_leg");
		this.lmLeg = root.getChild("mantis").getChild("body").getChild("lm_leg");
		this.rhLeg = root.getChild("mantis").getChild("body").getChild("rh_leg");
		this.lhLeg = root.getChild("mantis").getChild("body").getChild("lh_leg");
		this.rmShin = root.getChild("mantis").getChild("body").getChild("rm_leg").getChild("rm_shin");
		this.lmShin = root.getChild("mantis").getChild("body").getChild("lm_leg").getChild("lm_shin");
		this.rhShin = root.getChild("mantis").getChild("body").getChild("rh_leg").getChild("rh_shin");
		this.lhShin = root.getChild("mantis").getChild("body").getChild("lh_leg").getChild("lh_shin");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		PartDefinition p1 = root.addOrReplaceChild("mantis", CubeListBuilder.create(),
				PartPose.offset(0.0f, 24.0f, 0.0f));
		PartDefinition p2 = p1.addOrReplaceChild("body", CubeListBuilder.create()
				.texOffs(162, 33).addBox(-4.5f, -4.0f, -10.0f, 9.0f, 8.0f, 10.0f, new CubeDeformation(0.0f)),
				PartPose.offset(0.0f, -22.31f, 0.0f));
		PartDefinition p3 = p2.addOrReplaceChild("neck", CubeListBuilder.create()
				.texOffs(0, 33).addBox(-3.0f, -3.0f, -17.0f, 6.0f, 6.0f, 17.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, -3.0f, -9.0f, -1.15f, 0.0f, 0.0f));
		PartDefinition p4 = p3.addOrReplaceChild("head", CubeListBuilder.create()
				.texOffs(36, 56).addBox(-6.5f, -11.0f, -8.0f, 13.0f, 6.0f, 8.0f, new CubeDeformation(0.0f))
				.texOffs(132, 56).addBox(-3.5f, -5.0f, -7.5f, 7.0f, 5.0f, 7.0f, new CubeDeformation(0.0f))
				.texOffs(180, 56).addBox(-10.0f, -12.0f, -7.5f, 5.0f, 5.0f, 6.0f, new CubeDeformation(0.0f))
				.texOffs(180, 56).addBox(5.0f, -12.0f, -7.5f, 5.0f, 5.0f, 6.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, 0.0f, -16.0f, 1.15f, 0.0f, 0.0f));
		p4.addOrReplaceChild("r_antenna", CubeListBuilder.create()
				.texOffs(46, 33).addBox(-0.5f, -0.5f, -21.0f, 1.0f, 1.0f, 21.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(-3.25f, -10.0f, -7.0f, 1.0f, 0.45f, 0.0f));
		p4.addOrReplaceChild("l_antenna", CubeListBuilder.create()
				.texOffs(46, 33).addBox(-0.5f, -0.5f, -21.0f, 1.0f, 1.0f, 21.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(3.25f, -10.0f, -7.0f, 1.0f, -0.45f, 0.0f));
		PartDefinition p7 = p4.addOrReplaceChild("r_horn", CubeListBuilder.create()
				.texOffs(0, 71).addBox(-1.0f, -8.0f, -1.0f, 2.0f, 8.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(-5.0f, -10.5f, -4.0f, -1.35f, 0.0f, -0.22f));
		p7.addOrReplaceChild("r_horn_t", CubeListBuilder.create()
				.texOffs(78, 71).addBox(-0.5f, -6.0f, -0.5f, 1.0f, 6.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, -7.5f, 0.0f, -0.55f, 0.0f, 0.0f));
		PartDefinition p9 = p4.addOrReplaceChild("l_horn", CubeListBuilder.create()
				.texOffs(0, 71).addBox(-1.0f, -8.0f, -1.0f, 2.0f, 8.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(5.0f, -10.5f, -4.0f, -1.35f, 0.0f, 0.22f));
		p9.addOrReplaceChild("l_horn_t", CubeListBuilder.create()
				.texOffs(78, 71).addBox(-0.5f, -6.0f, -0.5f, 1.0f, 6.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, -7.5f, 0.0f, -0.55f, 0.0f, 0.0f));
		p4.addOrReplaceChild("r_jaw", CubeListBuilder.create()
				.texOffs(64, 71).addBox(-1.0f, 0.0f, -1.0f, 2.0f, 5.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(-1.5f, -0.5f, -6.5f, -0.2f, 0.0f, 0.2f));
		p4.addOrReplaceChild("l_jaw", CubeListBuilder.create()
				.texOffs(64, 71).addBox(-1.0f, 0.0f, -1.0f, 2.0f, 5.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(1.5f, -0.5f, -6.5f, -0.2f, 0.0f, -0.2f));
		p4.addOrReplaceChild("crown", CubeListBuilder.create()
				.texOffs(172, 56).addBox(-1.0f, -10.0f, -1.0f, 2.0f, 10.0f, 2.0f, new CubeDeformation(0.0f))
				.texOffs(34, 71).addBox(-4.0f, -7.0f, 0.0f, 2.0f, 7.0f, 2.0f, new CubeDeformation(0.0f))
				.texOffs(34, 71).addBox(2.0f, -7.0f, 0.0f, 2.0f, 7.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, -10.5f, -3.0f, -0.45f, 0.0f, 0.0f));
		PartDefinition p14 = p3.addOrReplaceChild("r_arm", CubeListBuilder.create()
				.texOffs(208, 33).addBox(-2.5f, 0.0f, -2.5f, 5.0f, 11.0f, 5.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(-4.0f, 0.0f, -10.2f, 0.6f, 0.0f, -0.22f));
		PartDefinition p15 = p14.addOrReplaceChild("r_femur", CubeListBuilder.create()
				.texOffs(140, 33).addBox(-3.0f, 0.0f, -2.5f, 6.0f, 16.0f, 5.0f, new CubeDeformation(0.0f))
				.texOffs(72, 71).addBox(-0.5f, 4.0f, -3.5f, 1.0f, 5.0f, 2.0f, new CubeDeformation(0.0f))
				.texOffs(72, 71).addBox(-0.5f, 7.2f, -3.5f, 1.0f, 5.0f, 2.0f, new CubeDeformation(0.0f))
				.texOffs(72, 71).addBox(-0.5f, 10.4f, -3.5f, 1.0f, 5.0f, 2.0f, new CubeDeformation(0.0f))
				.texOffs(72, 71).addBox(-0.5f, 13.6f, -3.5f, 1.0f, 5.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, 10.0f, 0.0f, -2.25f, 0.0f, 0.0f));
		PartDefinition p16 = p15.addOrReplaceChild("r_blade", CubeListBuilder.create()
				.texOffs(222, 0).addBox(-1.5f, 0.0f, -2.5f, 3.0f, 19.0f, 5.0f, new CubeDeformation(0.0f))
				.texOffs(72, 71).addBox(-0.5f, 5.7f, -3.5f, 1.0f, 5.0f, 2.0f, new CubeDeformation(0.0f))
				.texOffs(72, 71).addBox(-0.5f, 11.4f, -3.5f, 1.0f, 5.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, 15.0f, 0.0f, 2.75f, 0.0f, 0.0f));
		p16.addOrReplaceChild("r_scythe", CubeListBuilder.create()
				.texOffs(144, 0).addBox(-1.0f, 0.0f, -2.5f, 2.0f, 24.0f, 5.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, 17.0f, 0.0f, 0.7f, 0.0f, 0.0f));
		PartDefinition p18 = p3.addOrReplaceChild("l_arm", CubeListBuilder.create()
				.texOffs(208, 33).addBox(-2.5f, 0.0f, -2.5f, 5.0f, 11.0f, 5.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(4.0f, 0.0f, -10.2f, 0.6f, 0.0f, 0.22f));
		PartDefinition p19 = p18.addOrReplaceChild("l_femur", CubeListBuilder.create()
				.texOffs(140, 33).addBox(-3.0f, 0.0f, -2.5f, 6.0f, 16.0f, 5.0f, new CubeDeformation(0.0f))
				.texOffs(72, 71).addBox(-0.5f, 4.0f, -3.5f, 1.0f, 5.0f, 2.0f, new CubeDeformation(0.0f))
				.texOffs(72, 71).addBox(-0.5f, 7.2f, -3.5f, 1.0f, 5.0f, 2.0f, new CubeDeformation(0.0f))
				.texOffs(72, 71).addBox(-0.5f, 10.4f, -3.5f, 1.0f, 5.0f, 2.0f, new CubeDeformation(0.0f))
				.texOffs(72, 71).addBox(-0.5f, 13.6f, -3.5f, 1.0f, 5.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, 10.0f, 0.0f, -2.25f, 0.0f, 0.0f));
		PartDefinition p20 = p19.addOrReplaceChild("l_blade", CubeListBuilder.create()
				.texOffs(222, 0).addBox(-1.5f, 0.0f, -2.5f, 3.0f, 19.0f, 5.0f, new CubeDeformation(0.0f))
				.texOffs(72, 71).addBox(-0.5f, 5.7f, -3.5f, 1.0f, 5.0f, 2.0f, new CubeDeformation(0.0f))
				.texOffs(72, 71).addBox(-0.5f, 11.4f, -3.5f, 1.0f, 5.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, 15.0f, 0.0f, 2.75f, 0.0f, 0.0f));
		p20.addOrReplaceChild("l_scythe", CubeListBuilder.create()
				.texOffs(144, 0).addBox(-1.0f, 0.0f, -2.5f, 2.0f, 24.0f, 5.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, 17.0f, 0.0f, 0.7f, 0.0f, 0.0f));
		p3.addOrReplaceChild("shield", CubeListBuilder.create()
				.texOffs(202, 56).addBox(-7.5f, -1.0f, -9.0f, 15.0f, 1.0f, 9.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, -3.2f, -5.95f, 0.12f, 0.0f, 0.0f));
		PartDefinition p23 = p2.addOrReplaceChild("abdomen", CubeListBuilder.create()
				.texOffs(78, 0).addBox(-5.0f, -4.5f, 0.0f, 10.0f, 9.0f, 23.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f, 0.14f, 0.0f, 0.0f));
		PartDefinition p24 = p23.addOrReplaceChild("abd_tip", CubeListBuilder.create()
				.texOffs(78, 56).addBox(-3.5f, -3.0f, 0.0f, 7.0f, 6.0f, 7.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, 0.5f, 22.0f, 0.3f, 0.0f, 0.0f));
		p24.addOrReplaceChild("r_cerc", CubeListBuilder.create()
				.texOffs(8, 71).addBox(-0.5f, -0.5f, 0.0f, 1.0f, 1.0f, 8.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(-1.5f, -1.0f, 6.0f, 0.35f, -0.3f, 0.0f));
		p24.addOrReplaceChild("l_cerc", CubeListBuilder.create()
				.texOffs(8, 71).addBox(-0.5f, -0.5f, 0.0f, 1.0f, 1.0f, 8.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(1.5f, -1.0f, 6.0f, 0.35f, 0.3f, 0.0f));
		p23.addOrReplaceChild("dspike0", CubeListBuilder.create()
				.texOffs(42, 71).addBox(-0.5f, -7.0f, -1.0f, 1.0f, 7.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, -4.0f, 5.75f, 0.75f, 0.0f, 0.0f));
		p23.addOrReplaceChild("dspike1", CubeListBuilder.create()
				.texOffs(42, 71).addBox(-0.5f, -7.0f, -1.0f, 1.0f, 7.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, -4.0f, 11.5f, 0.75f, 0.0f, 0.0f));
		p23.addOrReplaceChild("dspike2", CubeListBuilder.create()
				.texOffs(42, 71).addBox(-0.5f, -7.0f, -1.0f, 1.0f, 7.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, -4.0f, 17.25f, 0.75f, 0.0f, 0.0f));
		PartDefinition p30 = p2.addOrReplaceChild("rm_leg", CubeListBuilder.create()
				.texOffs(228, 33).addBox(-1.5f, 0.0f, -1.5f, 3.0f, 13.0f, 3.0f, new CubeDeformation(0.0f))
				.texOffs(82, 71).addBox(-0.5f, 10.0f, -2.5f, 1.0f, 3.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(-4.5f, 3.0f, -5.5f, -0.6f, 0.0f, 1.15f));
		p30.addOrReplaceChild("rm_shin", CubeListBuilder.create()
				.texOffs(200, 33).addBox(-1.0f, 0.0f, -1.0f, 2.0f, 16.0f, 2.0f, new CubeDeformation(0.0f))
				.texOffs(82, 71).addBox(-0.5f, 7.2f, 1.0f, 1.0f, 3.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, 12.0f, 0.0f, 0.0f, 0.0f, -1.6f));
		PartDefinition p32 = p2.addOrReplaceChild("lm_leg", CubeListBuilder.create()
				.texOffs(228, 33).addBox(-1.5f, 0.0f, -1.5f, 3.0f, 13.0f, 3.0f, new CubeDeformation(0.0f))
				.texOffs(82, 71).addBox(-0.5f, 10.0f, -2.5f, 1.0f, 3.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(4.5f, 3.0f, -5.5f, -0.6f, 0.0f, -1.15f));
		p32.addOrReplaceChild("lm_shin", CubeListBuilder.create()
				.texOffs(200, 33).addBox(-1.0f, 0.0f, -1.0f, 2.0f, 16.0f, 2.0f, new CubeDeformation(0.0f))
				.texOffs(82, 71).addBox(-0.5f, 7.2f, 1.0f, 1.0f, 3.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, 12.0f, 0.0f, 0.0f, 0.0f, 1.6f));
		PartDefinition p34 = p2.addOrReplaceChild("rh_leg", CubeListBuilder.create()
				.texOffs(228, 33).addBox(-1.5f, 0.0f, -1.5f, 3.0f, 13.0f, 3.0f, new CubeDeformation(0.0f))
				.texOffs(82, 71).addBox(-0.5f, 10.0f, -2.5f, 1.0f, 3.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(-4.5f, 3.0f, -1.0f, 0.5f, 0.0f, 1.15f));
		p34.addOrReplaceChild("rh_shin", CubeListBuilder.create()
				.texOffs(200, 33).addBox(-1.0f, 0.0f, -1.0f, 2.0f, 16.0f, 2.0f, new CubeDeformation(0.0f))
				.texOffs(82, 71).addBox(-0.5f, 7.2f, 1.0f, 1.0f, 3.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, 12.0f, 0.0f, 0.0f, 0.0f, -1.6f));
		PartDefinition p36 = p2.addOrReplaceChild("lh_leg", CubeListBuilder.create()
				.texOffs(228, 33).addBox(-1.5f, 0.0f, -1.5f, 3.0f, 13.0f, 3.0f, new CubeDeformation(0.0f))
				.texOffs(82, 71).addBox(-0.5f, 10.0f, -2.5f, 1.0f, 3.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(4.5f, 3.0f, -1.0f, 0.5f, 0.0f, -1.15f));
		p36.addOrReplaceChild("lh_shin", CubeListBuilder.create()
				.texOffs(200, 33).addBox(-1.0f, 0.0f, -1.0f, 2.0f, 16.0f, 2.0f, new CubeDeformation(0.0f))
				.texOffs(82, 71).addBox(-0.5f, 7.2f, 1.0f, 1.0f, 3.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, 12.0f, 0.0f, 0.0f, 0.0f, 1.6f));
		PartDefinition p38 = p2.addOrReplaceChild("r_pauldron", CubeListBuilder.create()
				.texOffs(106, 56).addBox(-6.0f, -6.0f, -3.5f, 6.0f, 6.0f, 7.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(-3.5f, -2.0f, -7.0f, 0.0f, 0.0f, -0.35f));
		p38.addOrReplaceChild("r_pspike", CubeListBuilder.create()
				.texOffs(26, 71).addBox(-1.0f, -7.0f, -1.0f, 2.0f, 7.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(-3.0f, -5.5f, 0.0f, -0.3f, 0.0f, -0.5f));
		PartDefinition p40 = p2.addOrReplaceChild("l_pauldron", CubeListBuilder.create()
				.texOffs(106, 56).addBox(0.0f, -6.0f, -3.5f, 6.0f, 6.0f, 7.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(3.5f, -2.0f, -7.0f, 0.0f, 0.0f, 0.35f));
		p40.addOrReplaceChild("l_pspike", CubeListBuilder.create()
				.texOffs(26, 71).addBox(-1.0f, -7.0f, -1.0f, 2.0f, 7.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(3.0f, -5.5f, 0.0f, -0.3f, 0.0f, 0.5f));
		p2.addOrReplaceChild("back_cr", CubeListBuilder.create()
				.texOffs(160, 56).addBox(-1.5f, -9.0f, -1.5f, 3.0f, 9.0f, 3.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, -4.0f, -2.0f, -0.35f, 0.0f, 0.0f));
		PartDefinition p43 = p2.addOrReplaceChild("r_wing", CubeListBuilder.create()
				.texOffs(48, 71).addBox(-2.0f, -2.0f, -2.0f, 4.0f, 4.0f, 4.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(-2.0f, -5.0f, -2.0f, 0.5f, -0.65f, 0.3f));
		p43.addOrReplaceChild("r_blade0", CubeListBuilder.create()
				.texOffs(0, 0).addBox(-3.5f, -0.5f, 0.0f, 7.0f, 1.0f, 32.0f, new CubeDeformation(0.0f)),
				PartPose.ZERO);
		p43.addOrReplaceChild("r_blade1", CubeListBuilder.create()
				.texOffs(158, 0).addBox(-3.0f, -0.5f, 0.0f, 6.0f, 1.0f, 26.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f, -0.09f, -0.38f, 0.0f));
		p43.addOrReplaceChild("r_blade2", CubeListBuilder.create()
				.texOffs(90, 33).addBox(-2.5f, -0.5f, 0.0f, 5.0f, 1.0f, 20.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f, -0.18f, -0.76f, 0.0f));
		p43.addOrReplaceChild("r_blade3", CubeListBuilder.create()
				.texOffs(0, 56).addBox(-2.0f, -0.5f, 0.0f, 4.0f, 1.0f, 14.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f, -0.27f, -1.14f, 0.0f));
		PartDefinition p48 = p2.addOrReplaceChild("l_wing", CubeListBuilder.create()
				.texOffs(48, 71).addBox(-2.0f, -2.0f, -2.0f, 4.0f, 4.0f, 4.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(2.0f, -5.0f, -2.0f, 0.5f, 0.65f, -0.3f));
		p48.addOrReplaceChild("l_blade0", CubeListBuilder.create()
				.texOffs(0, 0).addBox(-3.5f, -0.5f, 0.0f, 7.0f, 1.0f, 32.0f, new CubeDeformation(0.0f)),
				PartPose.ZERO);
		p48.addOrReplaceChild("l_blade1", CubeListBuilder.create()
				.texOffs(158, 0).addBox(-3.0f, -0.5f, 0.0f, 6.0f, 1.0f, 26.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f, -0.09f, 0.38f, 0.0f));
		p48.addOrReplaceChild("l_blade2", CubeListBuilder.create()
				.texOffs(90, 33).addBox(-2.5f, -0.5f, 0.0f, 5.0f, 1.0f, 20.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f, -0.18f, 0.76f, 0.0f));
		p48.addOrReplaceChild("l_blade3", CubeListBuilder.create()
				.texOffs(0, 56).addBox(-2.0f, -0.5f, 0.0f, 4.0f, 1.0f, 14.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f, -0.27f, 1.14f, 0.0f));
		return LayerDefinition.create(mesh, 256, 128);
	}

	@Override
	public void setupAnim(CompanionRenderState state) {
		super.setupAnim(state);
		float age = state.ageInTicks;
		float pos = state.walkAnimationPos;
		float speed = Math.min(state.walkAnimationSpeed, 1.0f);

		if (state.sitting || state.exhausted) {
			poseSit(1.0f);
		}

		if (state.exhausted) {
			poseTired(1.0f);
		}

		if (state.flying) {
			// Flug mit Reiter: Flügel ausgebreitet und schlagend, Beine angelegt
			poseFly(1.0f);
			rWing.xRot += Mth.sin(age * 1.4f) * 0.35f;
			lWing.xRot += Mth.sin(age * 1.4f) * 0.35f;
		}

		// Blick
		float yaw = Mth.clamp(state.yRot, -50.0f, 50.0f) * Mth.DEG_TO_RAD;
		neck.yRot += yaw * 0.4f;
		head.yRot += yaw * 0.5f;
		head.xRot += state.xRot * Mth.DEG_TO_RAD * 0.4f;

		// Fühler und Fangarme bewegen sich ständig ein wenig, der Hinterleib atmet
		rAntenna.xRot += Mth.sin(age * 0.11f) * 0.12f;
		lAntenna.xRot += Mth.sin(age * 0.11f + 1.3f) * 0.12f;
		rAntenna.yRot += Mth.sin(age * 0.07f) * 0.1f;
		lAntenna.yRot -= Mth.sin(age * 0.07f + 0.7f) * 0.1f;
		rFemur.xRot += Mth.sin(age * 0.06f) * 0.05f;
		lFemur.xRot += Mth.sin(age * 0.06f + 0.4f) * 0.05f;
		abdomen.xRot += Mth.sin(age * 0.05f) * 0.03f;
		head.zRot += Math.max(0.0f, Mth.sin(age * 0.025f) - 0.85f) * 2.0f;   // legt ab und zu den Kopf schief
		float chew = Math.max(0.0f, Mth.sin(age * 0.09f) - 0.7f) * 0.6f;      // Mandibeln zucken
		rJaw.zRot += chew;
		lJaw.zRot -= chew;
		rWing.zRot += Mth.sin(age * 0.5f) * 0.04f;
		lWing.zRot -= Mth.sin(age * 0.5f) * 0.04f;

		// Laufen: dreibeiniger Wechsel (vorn-rechts mit hinten-links)
		float phase = pos * 1.1f;
		float swing = Mth.cos(phase) * 0.55f * speed;
		rmLeg.xRot += swing;
		lhLeg.xRot += swing;
		lmLeg.xRot -= swing;
		rhLeg.xRot -= swing;
		rmShin.zRot -= Math.max(0.0f, Mth.sin(phase)) * 0.3f * speed;
		lhShin.zRot += Math.max(0.0f, Mth.sin(phase)) * 0.3f * speed;
		lmShin.zRot += Math.max(0.0f, -Mth.sin(phase)) * 0.3f * speed;
		rhShin.zRot -= Math.max(0.0f, -Mth.sin(phase)) * 0.3f * speed;
		body.y += -Mth.abs(Mth.cos(phase)) * 0.6f * speed;
		body.zRot += Mth.sin(phase) * 0.04f * speed;

		// Fangschlag: ausholen, zuschnappen
		if (state.attack > 0.0f) {
			float a = state.attack < 0.35f ? state.attack / 0.35f : Math.max(0.0f, 1.0f - (state.attack - 0.35f) / 0.65f);
			poseStrike(a);
		}
	}

	private void poseSit(float k) {
		body.y += k * 2.660f;
		rmLeg.zRot += k * 0.350f;
		rmShin.zRot += k * -0.250f;
		lmLeg.zRot += k * -0.350f;
		lmShin.zRot += k * 0.250f;
		rhLeg.zRot += k * 0.350f;
		rhShin.zRot += k * -0.250f;
		lhLeg.zRot += k * -0.350f;
		lhShin.zRot += k * 0.250f;
		abdomen.xRot += k * -0.100f;
	}

	private void poseTired(float k) {
		neck.xRot += k * 0.450f;
		head.xRot += k * -0.200f;
		rAntenna.xRot += k * 0.500f;
		lAntenna.xRot += k * 0.500f;
		abdomen.xRot += k * -0.150f;
	}

	private void poseStrike(float k) {
		rFemur.xRot += k * -1.000f;
		lFemur.xRot += k * -1.000f;
		rBlade.xRot += k * -1.700f;
		lBlade.xRot += k * -1.700f;
		neck.xRot += k * 0.300f;
		rWing.zRot += k * 0.350f;
		lWing.zRot += k * -0.350f;
		rJaw.zRot += k * 0.450f;
		lJaw.zRot += k * -0.450f;
	}

	private void poseFly(float k) {
		rWing.xRot += k * -0.400f;
		rWing.yRot += k * -0.500f;
		lWing.xRot += k * -0.400f;
		lWing.yRot += k * 0.500f;
		abdomen.xRot += k * -0.150f;
		rmLeg.xRot += k * 0.900f;
		lmLeg.xRot += k * 0.900f;
		rhLeg.xRot += k * 0.900f;
		lhLeg.xRot += k * 0.900f;
	}


}
