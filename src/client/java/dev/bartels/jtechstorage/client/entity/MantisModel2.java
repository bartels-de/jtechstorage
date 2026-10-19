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
 * Schattenmantis, Stufe 2 (Begleiter-Redesign v0.25.5).
 * Modell in doppelter Detaildichte (halbe Pixel) – der Renderer skaliert es auf 0.5.
 * Erzeugt von scratchpad/mantis2.py (Teile und Posen), Animation von Hand.
 */
public class MantisModel2 extends EntityModel<CompanionRenderState> implements CompanionLookModel {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(JTechStorage.id("mantis_2"), "main");

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
	private final ModelPart rHorn;
	private final ModelPart lHorn;
	private final ModelPart rHornCrown;
	private final ModelPart lHornCrown;
	private final ModelPart rHornSickle;
	private final ModelPart lHornSickle;
	private final ModelPart collar;
	private final ModelPart amulet;
	private final ModelPart armorT;
	private final ModelPart armorA;

	public MantisModel2(ModelPart root) {
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
		this.rWing = root.getChild("mantis").getChild("body").getChild("abdomen").getChild("r_wing");
		this.lWing = root.getChild("mantis").getChild("body").getChild("abdomen").getChild("l_wing");
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
		this.rHorn = root.getChild("mantis").getChild("body").getChild("neck").getChild("head").getChild("r_horn");
		this.lHorn = root.getChild("mantis").getChild("body").getChild("neck").getChild("head").getChild("l_horn");
		this.rHornCrown = root.getChild("mantis").getChild("body").getChild("neck").getChild("head").getChild("r_horn_crown");
		this.lHornCrown = root.getChild("mantis").getChild("body").getChild("neck").getChild("head").getChild("l_horn_crown");
		this.rHornSickle = root.getChild("mantis").getChild("body").getChild("neck").getChild("head").getChild("r_horn_sickle");
		this.lHornSickle = root.getChild("mantis").getChild("body").getChild("neck").getChild("head").getChild("l_horn_sickle");
		this.collar = root.getChild("mantis").getChild("body").getChild("neck").getChild("collar");
		this.amulet = root.getChild("mantis").getChild("body").getChild("neck").getChild("amulet");
		this.armorT = root.getChild("mantis").getChild("body").getChild("armor_t");
		this.armorA = root.getChild("mantis").getChild("body").getChild("abdomen").getChild("armor_a");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		PartDefinition p1 = root.addOrReplaceChild("mantis", CubeListBuilder.create(),
				PartPose.offset(0.0f, 24.0f, 0.0f));
		PartDefinition p2 = p1.addOrReplaceChild("body", CubeListBuilder.create()
				.texOffs(0, 28).addBox(-4.0f, -3.5f, -9.0f, 8.0f, 7.0f, 9.0f, new CubeDeformation(0.0f)),
				PartPose.offset(0.0f, -19.29f, 0.0f));
		PartDefinition p3 = p2.addOrReplaceChild("neck", CubeListBuilder.create()
				.texOffs(128, 0).addBox(-2.5f, -2.5f, -15.0f, 5.0f, 5.0f, 15.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, -2.5f, -8.0f, -1.15f, 0.0f, 0.0f));
		PartDefinition p4 = p3.addOrReplaceChild("head", CubeListBuilder.create()
				.texOffs(66, 28).addBox(-5.5f, -9.0f, -7.0f, 11.0f, 5.0f, 7.0f, new CubeDeformation(0.0f))
				.texOffs(126, 28).addBox(-3.0f, -4.0f, -6.5f, 6.0f, 4.0f, 6.0f, new CubeDeformation(0.0f))
				.texOffs(192, 28).addBox(-8.0f, -10.0f, -6.5f, 4.0f, 4.0f, 5.0f, new CubeDeformation(0.0f))
				.texOffs(192, 28).addBox(4.0f, -10.0f, -6.5f, 4.0f, 4.0f, 5.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, 0.0f, -14.0f, 1.15f, 0.0f, 0.0f));
		p4.addOrReplaceChild("r_antenna", CubeListBuilder.create()
				.texOffs(168, 0).addBox(-0.5f, -0.5f, -18.0f, 1.0f, 1.0f, 18.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(-2.75f, -8.0f, -6.0f, 1.0f, 0.45f, 0.0f));
		p4.addOrReplaceChild("l_antenna", CubeListBuilder.create()
				.texOffs(168, 0).addBox(-0.5f, -0.5f, -18.0f, 1.0f, 1.0f, 18.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(2.75f, -8.0f, -6.0f, 1.0f, -0.45f, 0.0f));
		PartDefinition p7 = p4.addOrReplaceChild("r_horn", CubeListBuilder.create()
				.texOffs(210, 28).addBox(-1.0f, -7.0f, -1.0f, 2.0f, 7.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(-4.0f, -8.5f, -3.5f, -1.35f, 0.0f, -0.22f));
		p7.addOrReplaceChild("r_horn_t", CubeListBuilder.create()
				.texOffs(0, 44).addBox(-0.5f, -5.0f, -0.5f, 1.0f, 5.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, -6.5f, 0.0f, -0.55f, 0.0f, 0.0f));
		PartDefinition p9 = p4.addOrReplaceChild("l_horn", CubeListBuilder.create()
				.texOffs(210, 28).addBox(-1.0f, -7.0f, -1.0f, 2.0f, 7.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(4.0f, -8.5f, -3.5f, -1.35f, 0.0f, 0.22f));
		p9.addOrReplaceChild("l_horn_t", CubeListBuilder.create()
				.texOffs(0, 44).addBox(-0.5f, -5.0f, -0.5f, 1.0f, 5.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, -6.5f, 0.0f, -0.55f, 0.0f, 0.0f));
		p4.addOrReplaceChild("r_jaw", CubeListBuilder.create()
				.texOffs(240, 28).addBox(-1.0f, 0.0f, -1.0f, 2.0f, 4.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(-1.5f, -0.5f, -5.5f, -0.2f, 0.0f, 0.2f));
		p4.addOrReplaceChild("l_jaw", CubeListBuilder.create()
				.texOffs(240, 28).addBox(-1.0f, 0.0f, -1.0f, 2.0f, 4.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(1.5f, -0.5f, -5.5f, -0.2f, 0.0f, -0.2f));
		PartDefinition p13 = p4.addOrReplaceChild("r_horn_crown", CubeListBuilder.create()
				.texOffs(0, 128).addBox(-1.0f, -9.0f, -1.0f, 2.0f, 9.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(-4.0f, -8.5f, -3.5f, -0.3f, 0.0f, -0.32f));
		p13.addOrReplaceChild("r_horn_t_crown", CubeListBuilder.create()
				.texOffs(8, 128).addBox(-0.5f, -4.0f, -0.5f, 1.0f, 4.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, -8.5f, 0.0f, -0.05f, 0.0f, 0.1f));
		p13.addOrReplaceChild("r_horn_s_crown", CubeListBuilder.create()
				.texOffs(12, 128).addBox(-0.5f, -3.0f, -0.5f, 1.0f, 3.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, -4.5f, 0.0f, 0.0f, 0.0f, -0.9f));
		PartDefinition p16 = p4.addOrReplaceChild("l_horn_crown", CubeListBuilder.create()
				.texOffs(16, 128).addBox(-1.0f, -9.0f, -1.0f, 2.0f, 9.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(4.0f, -8.5f, -3.5f, -0.3f, 0.0f, 0.32f));
		p16.addOrReplaceChild("l_horn_t_crown", CubeListBuilder.create()
				.texOffs(24, 128).addBox(-0.5f, -4.0f, -0.5f, 1.0f, 4.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, -8.5f, 0.0f, -0.05f, 0.0f, -0.1f));
		p16.addOrReplaceChild("l_horn_s_crown", CubeListBuilder.create()
				.texOffs(28, 128).addBox(-0.5f, -3.0f, -0.5f, 1.0f, 3.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, -4.5f, 0.0f, 0.0f, 0.0f, 0.9f));
		PartDefinition p19 = p4.addOrReplaceChild("r_horn_sickle", CubeListBuilder.create()
				.texOffs(32, 128).addBox(-1.0f, -9.0f, -1.0f, 2.0f, 9.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(-4.0f, -8.5f, -3.5f, 0.25f, 0.0f, -0.55f));
		p19.addOrReplaceChild("r_horn_t_sickle", CubeListBuilder.create()
				.texOffs(40, 128).addBox(-0.5f, -7.0f, -0.5f, 1.0f, 7.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, -8.5f, 0.0f, 0.75f, 0.0f, 0.15f));
		PartDefinition p21 = p4.addOrReplaceChild("l_horn_sickle", CubeListBuilder.create()
				.texOffs(44, 128).addBox(-1.0f, -9.0f, -1.0f, 2.0f, 9.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(4.0f, -8.5f, -3.5f, 0.25f, 0.0f, 0.55f));
		p21.addOrReplaceChild("l_horn_t_sickle", CubeListBuilder.create()
				.texOffs(52, 128).addBox(-0.5f, -7.0f, -0.5f, 1.0f, 7.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, -8.5f, 0.0f, 0.75f, 0.0f, -0.15f));
		PartDefinition p23 = p3.addOrReplaceChild("r_arm", CubeListBuilder.create()
				.texOffs(42, 28).addBox(-2.0f, 0.0f, -2.0f, 4.0f, 10.0f, 4.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(-3.5f, 0.0f, -9.0f, 0.6f, 0.0f, -0.22f));
		PartDefinition p24 = p23.addOrReplaceChild("r_femur", CubeListBuilder.create()
				.texOffs(206, 0).addBox(-2.5f, 0.0f, -2.0f, 5.0f, 14.0f, 4.0f, new CubeDeformation(0.0f))
				.texOffs(248, 28).addBox(-0.5f, 3.5f, -3.0f, 1.0f, 4.0f, 2.0f, new CubeDeformation(0.0f))
				.texOffs(248, 28).addBox(-0.5f, 6.3f, -3.0f, 1.0f, 4.0f, 2.0f, new CubeDeformation(0.0f))
				.texOffs(248, 28).addBox(-0.5f, 9.1f, -3.0f, 1.0f, 4.0f, 2.0f, new CubeDeformation(0.0f))
				.texOffs(248, 28).addBox(-0.5f, 11.9f, -3.0f, 1.0f, 4.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, 9.0f, 0.0f, -2.25f, 0.0f, 0.0f));
		p24.addOrReplaceChild("r_blade", CubeListBuilder.create()
				.texOffs(114, 0).addBox(-1.5f, 0.0f, -2.0f, 3.0f, 17.0f, 4.0f, new CubeDeformation(0.0f))
				.texOffs(248, 28).addBox(-0.5f, 5.1f, -3.0f, 1.0f, 4.0f, 2.0f, new CubeDeformation(0.0f))
				.texOffs(248, 28).addBox(-0.5f, 10.2f, -3.0f, 1.0f, 4.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, 13.0f, 0.0f, 2.75f, 0.0f, 0.0f));
		PartDefinition p26 = p3.addOrReplaceChild("l_arm", CubeListBuilder.create()
				.texOffs(42, 28).addBox(-2.0f, 0.0f, -2.0f, 4.0f, 10.0f, 4.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(3.5f, 0.0f, -9.0f, 0.6f, 0.0f, 0.22f));
		PartDefinition p27 = p26.addOrReplaceChild("l_femur", CubeListBuilder.create()
				.texOffs(206, 0).addBox(-2.5f, 0.0f, -2.0f, 5.0f, 14.0f, 4.0f, new CubeDeformation(0.0f))
				.texOffs(248, 28).addBox(-0.5f, 3.5f, -3.0f, 1.0f, 4.0f, 2.0f, new CubeDeformation(0.0f))
				.texOffs(248, 28).addBox(-0.5f, 6.3f, -3.0f, 1.0f, 4.0f, 2.0f, new CubeDeformation(0.0f))
				.texOffs(248, 28).addBox(-0.5f, 9.1f, -3.0f, 1.0f, 4.0f, 2.0f, new CubeDeformation(0.0f))
				.texOffs(248, 28).addBox(-0.5f, 11.9f, -3.0f, 1.0f, 4.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, 9.0f, 0.0f, -2.25f, 0.0f, 0.0f));
		p27.addOrReplaceChild("l_blade", CubeListBuilder.create()
				.texOffs(114, 0).addBox(-1.5f, 0.0f, -2.0f, 3.0f, 17.0f, 4.0f, new CubeDeformation(0.0f))
				.texOffs(248, 28).addBox(-0.5f, 5.1f, -3.0f, 1.0f, 4.0f, 2.0f, new CubeDeformation(0.0f))
				.texOffs(248, 28).addBox(-0.5f, 10.2f, -3.0f, 1.0f, 4.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, 13.0f, 0.0f, 2.75f, 0.0f, 0.0f));
		p3.addOrReplaceChild("shield", CubeListBuilder.create()
				.texOffs(150, 28).addBox(-6.5f, -1.0f, -8.0f, 13.0f, 1.0f, 8.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, -2.7f, -5.25f, 0.12f, 0.0f, 0.0f));
		p3.addOrReplaceChild("collar", CubeListBuilder.create()
				.texOffs(56, 128).addBox(-4.0f, -4.0f, -4.0f, 8.0f, 8.0f, 3.0f, new CubeDeformation(0.0f)),
				PartPose.ZERO);
		p3.addOrReplaceChild("amulet", CubeListBuilder.create()
				.texOffs(78, 128).addBox(-2.0f, 0.0f, -0.5f, 4.0f, 5.0f, 1.0f, new CubeDeformation(0.0f))
				.texOffs(88, 128).addBox(-1.0f, 1.0f, -1.2f, 2.0f, 3.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, 4.0f, -2.5f, 1.0f, 0.0f, 0.0f));
		PartDefinition p32 = p2.addOrReplaceChild("abdomen", CubeListBuilder.create()
				.texOffs(0, 0).addBox(-4.5f, -4.0f, 0.0f, 9.0f, 8.0f, 20.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f, 0.14f, 0.0f, 0.0f));
		PartDefinition p33 = p32.addOrReplaceChild("abd_tip", CubeListBuilder.create()
				.texOffs(102, 28).addBox(-3.0f, -2.5f, 0.0f, 6.0f, 5.0f, 6.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, 0.5f, 19.0f, 0.3f, 0.0f, 0.0f));
		p33.addOrReplaceChild("r_cerc", CubeListBuilder.create()
				.texOffs(218, 28).addBox(-0.5f, -0.5f, 0.0f, 1.0f, 1.0f, 7.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(-1.5f, -1.0f, 5.0f, 0.35f, -0.3f, 0.0f));
		p33.addOrReplaceChild("l_cerc", CubeListBuilder.create()
				.texOffs(218, 28).addBox(-0.5f, -0.5f, 0.0f, 1.0f, 1.0f, 7.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(1.5f, -1.0f, 5.0f, 0.35f, 0.3f, 0.0f));
		p32.addOrReplaceChild("dspike0", CubeListBuilder.create()
				.texOffs(234, 28).addBox(-0.5f, -6.0f, -1.0f, 1.0f, 6.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, -3.5f, 5.0f, 0.75f, 0.0f, 0.0f));
		p32.addOrReplaceChild("dspike1", CubeListBuilder.create()
				.texOffs(234, 28).addBox(-0.5f, -6.0f, -1.0f, 1.0f, 6.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, -3.5f, 10.0f, 0.75f, 0.0f, 0.0f));
		p32.addOrReplaceChild("dspike2", CubeListBuilder.create()
				.texOffs(234, 28).addBox(-0.5f, -6.0f, -1.0f, 1.0f, 6.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, -3.5f, 15.0f, 0.75f, 0.0f, 0.0f));
		p32.addOrReplaceChild("r_wing", CubeListBuilder.create()
				.texOffs(58, 0).addBox(-8.0f, -1.0f, 0.0f, 8.0f, 1.0f, 20.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, -4.6f, -1.0f, -0.04f, 0.1f, -0.14f));
		p32.addOrReplaceChild("l_wing", CubeListBuilder.create()
				.texOffs(58, 0).addBox(0.0f, -1.0f, 0.0f, 8.0f, 1.0f, 20.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, -4.6f, -1.0f, -0.04f, -0.1f, 0.14f));
		p32.addOrReplaceChild("armor_a", CubeListBuilder.create()
				.texOffs(134, 128).addBox(-5.5f, -5.0f, 0.5f, 11.0f, 5.0f, 10.0f, new CubeDeformation(0.0f)),
				PartPose.ZERO);
		PartDefinition p42 = p2.addOrReplaceChild("rm_leg", CubeListBuilder.create()
				.texOffs(58, 28).addBox(-1.0f, 0.0f, -1.0f, 2.0f, 11.0f, 2.0f, new CubeDeformation(0.0f))
				.texOffs(4, 44).addBox(-0.5f, 8.0f, -2.0f, 1.0f, 3.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(-4.0f, 2.5f, -4.95f, -0.6f, 0.0f, 1.15f));
		p42.addOrReplaceChild("rm_shin", CubeListBuilder.create()
				.texOffs(34, 28).addBox(-1.0f, 0.0f, -1.0f, 2.0f, 14.0f, 2.0f, new CubeDeformation(0.0f))
				.texOffs(4, 44).addBox(-0.5f, 6.3f, 1.0f, 1.0f, 3.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, 10.0f, 0.0f, 0.0f, 0.0f, -1.6f));
		PartDefinition p44 = p2.addOrReplaceChild("lm_leg", CubeListBuilder.create()
				.texOffs(58, 28).addBox(-1.0f, 0.0f, -1.0f, 2.0f, 11.0f, 2.0f, new CubeDeformation(0.0f))
				.texOffs(4, 44).addBox(-0.5f, 8.0f, -2.0f, 1.0f, 3.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(4.0f, 2.5f, -4.95f, -0.6f, 0.0f, -1.15f));
		p44.addOrReplaceChild("lm_shin", CubeListBuilder.create()
				.texOffs(34, 28).addBox(-1.0f, 0.0f, -1.0f, 2.0f, 14.0f, 2.0f, new CubeDeformation(0.0f))
				.texOffs(4, 44).addBox(-0.5f, 6.3f, 1.0f, 1.0f, 3.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, 10.0f, 0.0f, 0.0f, 0.0f, 1.6f));
		PartDefinition p46 = p2.addOrReplaceChild("rh_leg", CubeListBuilder.create()
				.texOffs(58, 28).addBox(-1.0f, 0.0f, -1.0f, 2.0f, 11.0f, 2.0f, new CubeDeformation(0.0f))
				.texOffs(4, 44).addBox(-0.5f, 8.0f, -2.0f, 1.0f, 3.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(-4.0f, 2.5f, -1.0f, 0.5f, 0.0f, 1.15f));
		p46.addOrReplaceChild("rh_shin", CubeListBuilder.create()
				.texOffs(34, 28).addBox(-1.0f, 0.0f, -1.0f, 2.0f, 14.0f, 2.0f, new CubeDeformation(0.0f))
				.texOffs(4, 44).addBox(-0.5f, 6.3f, 1.0f, 1.0f, 3.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, 10.0f, 0.0f, 0.0f, 0.0f, -1.6f));
		PartDefinition p48 = p2.addOrReplaceChild("lh_leg", CubeListBuilder.create()
				.texOffs(58, 28).addBox(-1.0f, 0.0f, -1.0f, 2.0f, 11.0f, 2.0f, new CubeDeformation(0.0f))
				.texOffs(4, 44).addBox(-0.5f, 8.0f, -2.0f, 1.0f, 3.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(4.0f, 2.5f, -1.0f, 0.5f, 0.0f, -1.15f));
		p48.addOrReplaceChild("lh_shin", CubeListBuilder.create()
				.texOffs(34, 28).addBox(-1.0f, 0.0f, -1.0f, 2.0f, 14.0f, 2.0f, new CubeDeformation(0.0f))
				.texOffs(4, 44).addBox(-0.5f, 6.3f, 1.0f, 1.0f, 3.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, 10.0f, 0.0f, 0.0f, 0.0f, 1.6f));
		p2.addOrReplaceChild("armor_t", CubeListBuilder.create()
				.texOffs(94, 128).addBox(-5.0f, -4.5f, -9.5f, 10.0f, 5.0f, 10.0f, new CubeDeformation(0.0f)),
				PartPose.ZERO);
		return LayerDefinition.create(mesh, 256, 256);
	}

	@Override
	public void setupAnim(CompanionRenderState state) {
		super.setupAnim(state);
		// Aussehen (v0.25.4): gewählte Körperteil-Varianten; Ausrüstung nur im eigenen Durchgang
		rHorn.visible = state.part1 == 0;
		lHorn.visible = state.part1 == 0;
		rHornCrown.visible = state.part1 == 1;
		lHornCrown.visible = state.part1 == 1;
		rHornSickle.visible = state.part1 == 2;
		lHornSickle.visible = state.part1 == 2;
		collar.visible = pass == 0;
		amulet.visible = pass == 1;
		armorT.visible = pass == 2;
		armorA.visible = pass == 2;

		float age = state.ageInTicks;
		float pos = state.walkAnimationPos;
		float speed = Math.min(state.walkAnimationSpeed, 1.0f);

		if (state.sitting || state.exhausted) {
			poseSit(1.0f);
		}

		if (state.exhausted) {
			poseTired(1.0f);
		}

		// Bindungs-Gesten (v0.26.0): Freudensprung (zwei Hüpfer), Kuscheln, eigene Geste
		if (state.gesture > 0) {
			float p = state.gestureProgress;
			float k = Mth.sin(p * Mth.PI);

			if (state.gesture == 1) {
				poseJoy(Math.abs(Mth.sin(p * Mth.TWO_PI)));
			} else if (state.gesture == 2) {
				poseNuzzle(Math.min(1.0f, k * 1.6f));
			} else {
				poseGesture(Math.min(1.0f, k * 1.6f));
			}
		}

		if (state.flying) {
			// Flug mit Reiter: Flügel ausgebreitet und schlagend, Beine angelegt
			poseFly(1.0f);
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
		// Körperteil-Varianten bewegen sich wie das Original
		CompanionLookModel.follow(rHorn, rHornCrown);
		CompanionLookModel.follow(lHorn, lHornCrown);
		CompanionLookModel.follow(rHorn, rHornSickle);
		CompanionLookModel.follow(lHorn, lHornSickle);
	}

	private void poseSit(float k) {
		body.y += k * 2.620f;
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

	private void poseJoy(float k) {
		body.y += k * -7.000f;
		rFemur.xRot += k * -0.900f;
		lFemur.xRot += k * -0.900f;
		rAntenna.xRot += k * -0.500f;
		lAntenna.xRot += k * -0.500f;
		rmLeg.zRot += k * 0.500f;
		lmLeg.zRot += k * -0.500f;
		rhLeg.zRot += k * 0.500f;
		lhLeg.zRot += k * -0.500f;
		abdomen.xRot += k * 0.250f;
	}

	private void poseNuzzle(float k) {
		neck.xRot += k * 0.250f;
		head.zRot += k * 0.500f;
		head.xRot += k * 0.100f;
		body.zRot += k * 0.120f;
		rAntenna.xRot += k * 0.600f;
		lAntenna.xRot += k * 0.600f;
	}

	private void poseGesture(float k) {
		rFemur.xRot += k * -1.500f;
		rFemur.yRot += k * 0.550f;
		lFemur.xRot += k * -1.500f;
		lFemur.yRot += k * -0.550f;
		rBlade.xRot += k * -0.600f;
		lBlade.xRot += k * -0.600f;
		neck.xRot += k * -0.200f;
		rWing.zRot += k * 0.400f;
		lWing.zRot += k * -0.400f;
	}

	/** Ausrüstungs-Durchgang: -1 = Grundmodell (Ausrüstung unsichtbar), 0–2 = nur dieser Platz wird gezeichnet. */
	private int pass = -1;

	@Override
	public void equipmentPass(int slot) {
		pass = slot;
		java.util.List<java.util.List<ModelPart>> slots = java.util.List.of(java.util.List.of(collar), java.util.List.of(amulet), java.util.List.of(armorT, armorA));

		for (ModelPart part : root().getAllParts()) {
			part.skipDraw = true;
		}

		for (ModelPart part : slots.get(slot)) {
			for (ModelPart child : part.getAllParts()) {
				child.skipDraw = false;
			}
		}
	}
}
