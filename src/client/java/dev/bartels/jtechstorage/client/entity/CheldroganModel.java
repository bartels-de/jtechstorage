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
 * Cheldrogan v2 – Peters Drachenform: aufrecht stehender Amethyst-Drache. Digitigrad-Beine mit Goldknieschützern und
 * Kristallkrallen, tonnenförmige Brust mit V-Goldpanzer und strahlendem Kern, dreiteiliger S-Hals, langer Drachenkopf
 * (Brauenwülste, Wangenfächer, dreigliedrige Hörner, Kristallkrone), Fledermaus-Schwingen mit drei Fingern, fünfgliedriger
 * Schwanz mit Kristallklingen und Kristall-Morgenstern. Geometrie und Posen werden aus einem Modell-Skript erzeugt.
 * Grundpose = Flügel ausgebreitet; am Boden werden sie angelegt ({@link #poseFold}), im Flug kippt der Körper waagerecht.
 */
public class CheldroganModel extends EntityModel<CheldroganRenderState> implements HelperHatModel {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(JTechStorage.id("cheldrogan"), "main");

	private final ModelPart body;
	private final ModelPart torso;
	private final ModelPart neck1;
	private final ModelPart neck2;
	private final ModelPart neck3;
	private final ModelPart head;
	private final ModelPart jaw;
	private final ModelPart lArm;
	private final ModelPart rArm;
	private final ModelPart lForearm;
	private final ModelPart rForearm;
	private final ModelPart lWingRoot;
	private final ModelPart rWingRoot;
	private final ModelPart lWing;
	private final ModelPart rWing;
	private final ModelPart lWfore;
	private final ModelPart rWfore;
	private final ModelPart lFinger1;
	private final ModelPart rFinger1;
	private final ModelPart lFinger2;
	private final ModelPart rFinger2;
	private final ModelPart lFinger3;
	private final ModelPart rFinger3;
	private final ModelPart t1;
	private final ModelPart t2;
	private final ModelPart t3;
	private final ModelPart t4;
	private final ModelPart t5;
	private final ModelPart lLeg;
	private final ModelPart rLeg;
	private final ModelPart lShin;
	private final ModelPart rShin;
	private final ModelPart lMeta;
	private final ModelPart rMeta;
	private final ModelPart lFoot;
	private final ModelPart rFoot;

	public CheldroganModel(ModelPart root) {
		super(root);
		this.body = root.getChild("body");
		this.torso = root.getChild("body").getChild("torso");
		this.neck1 = root.getChild("body").getChild("torso").getChild("neck1");
		this.neck2 = root.getChild("body").getChild("torso").getChild("neck1").getChild("neck2");
		this.neck3 = root.getChild("body").getChild("torso").getChild("neck1").getChild("neck2").getChild("neck3");
		this.head = root.getChild("body").getChild("torso").getChild("neck1").getChild("neck2").getChild("neck3").getChild("head");
		this.jaw = root.getChild("body").getChild("torso").getChild("neck1").getChild("neck2").getChild("neck3").getChild("head").getChild("jaw");
		this.lArm = root.getChild("body").getChild("torso").getChild("l_arm");
		this.rArm = root.getChild("body").getChild("torso").getChild("r_arm");
		this.lForearm = root.getChild("body").getChild("torso").getChild("l_arm").getChild("l_forearm");
		this.rForearm = root.getChild("body").getChild("torso").getChild("r_arm").getChild("r_forearm");
		this.lWingRoot = root.getChild("body").getChild("torso").getChild("l_wing_root");
		this.rWingRoot = root.getChild("body").getChild("torso").getChild("r_wing_root");
		this.lWing = root.getChild("body").getChild("torso").getChild("l_wing_root").getChild("l_wing");
		this.rWing = root.getChild("body").getChild("torso").getChild("r_wing_root").getChild("r_wing");
		this.lWfore = root.getChild("body").getChild("torso").getChild("l_wing_root").getChild("l_wing").getChild("l_wfore");
		this.rWfore = root.getChild("body").getChild("torso").getChild("r_wing_root").getChild("r_wing").getChild("r_wfore");
		this.lFinger1 = root.getChild("body").getChild("torso").getChild("l_wing_root").getChild("l_wing").getChild("l_wfore").getChild("l_finger1");
		this.rFinger1 = root.getChild("body").getChild("torso").getChild("r_wing_root").getChild("r_wing").getChild("r_wfore").getChild("r_finger1");
		this.lFinger2 = root.getChild("body").getChild("torso").getChild("l_wing_root").getChild("l_wing").getChild("l_wfore").getChild("l_finger2");
		this.rFinger2 = root.getChild("body").getChild("torso").getChild("r_wing_root").getChild("r_wing").getChild("r_wfore").getChild("r_finger2");
		this.lFinger3 = root.getChild("body").getChild("torso").getChild("l_wing_root").getChild("l_wing").getChild("l_wfore").getChild("l_finger3");
		this.rFinger3 = root.getChild("body").getChild("torso").getChild("r_wing_root").getChild("r_wing").getChild("r_wfore").getChild("r_finger3");
		this.t1 = root.getChild("body").getChild("t1");
		this.t2 = root.getChild("body").getChild("t1").getChild("t2");
		this.t3 = root.getChild("body").getChild("t1").getChild("t2").getChild("t3");
		this.t4 = root.getChild("body").getChild("t1").getChild("t2").getChild("t3").getChild("t4");
		this.t5 = root.getChild("body").getChild("t1").getChild("t2").getChild("t3").getChild("t4").getChild("t5");
		this.lLeg = root.getChild("l_leg");
		this.rLeg = root.getChild("r_leg");
		this.lShin = root.getChild("l_leg").getChild("l_shin");
		this.rShin = root.getChild("r_leg").getChild("r_shin");
		this.lMeta = root.getChild("l_leg").getChild("l_shin").getChild("l_meta");
		this.rMeta = root.getChild("r_leg").getChild("r_shin").getChild("r_meta");
		this.lFoot = root.getChild("l_leg").getChild("l_shin").getChild("l_meta").getChild("l_foot");
		this.rFoot = root.getChild("r_leg").getChild("r_shin").getChild("r_meta").getChild("r_foot");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		PartDefinition p1 = root.addOrReplaceChild("body", CubeListBuilder.create()
				.texOffs(74, 0).addBox(-5.0f, -3.0f, -4.5f, 10.0f, 7.0f, 9.0f, new CubeDeformation(0.0f)),
				PartPose.offset(0.0f, 2.5f, 2.0f));
		PartDefinition p2 = p1.addOrReplaceChild("torso", CubeListBuilder.create()
				.texOffs(112, 0).addBox(-5.0f, -6.0f, -4.5f, 10.0f, 6.0f, 9.0f, new CubeDeformation(0.0f))
				.texOffs(0, 0).addBox(-6.5f, -15.0f, -5.5f, 13.0f, 9.0f, 11.0f, new CubeDeformation(0.0f))
				.texOffs(0, 20).addBox(-3.5f, -13.5f, -6.2f, 7.0f, 13.0f, 1.0f, new CubeDeformation(0.0f))
				.texOffs(98, 44).addBox(-5.5f, -14.5f, -6.6f, 11.0f, 6.0f, 1.0f, new CubeDeformation(0.0f))
				.texOffs(130, 52).addBox(-1.5f, -9.0f, -6.6f, 3.0f, 3.0f, 1.0f, new CubeDeformation(0.0f))
				.texOffs(172, 44).addBox(-2.5f, -13.5f, -7.2f, 5.0f, 5.0f, 1.0f, new CubeDeformation(0.0f))
				.texOffs(138, 52).addBox(-1.5f, -12.5f, -7.7f, 3.0f, 3.0f, 1.0f, new CubeDeformation(0.0f))
				.texOffs(164, 34).addBox(-4.0f, -16.5f, -1.5f, 8.0f, 2.0f, 7.0f, new CubeDeformation(0.0f))
				.texOffs(12, 52).addBox(-1.0f, -18.5f, -2.0f, 2.0f, 3.0f, 2.0f, new CubeDeformation(0.0f))
				.texOffs(112, 52).addBox(-4.0f, -18.0f, 5.0f, 8.0f, 3.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, -2.0f, -0.5f, -0.12f, 0.0f, 0.0f));
		PartDefinition p3 = p2.addOrReplaceChild("neck1", CubeListBuilder.create()
				.texOffs(16, 20).addBox(-3.0f, -8.0f, -3.0f, 6.0f, 8.0f, 6.0f, new CubeDeformation(0.0f))
				.texOffs(228, 34).addBox(-1.5f, -7.5f, -3.6f, 4.0f, 7.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, -14.0f, -3.0f, -0.62f, 0.0f, 0.0f));
		PartDefinition p4 = p3.addOrReplaceChild("neck2", CubeListBuilder.create()
				.texOffs(98, 20).addBox(-2.5f, -8.0f, -2.5f, 5.0f, 8.0f, 5.0f, new CubeDeformation(0.0f))
				.texOffs(228, 34).addBox(-1.5f, -7.5f, -3.1f, 4.0f, 7.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, -7.5f, 0.0f, 0.32f, 0.0f, 0.0f));
		PartDefinition p5 = p4.addOrReplaceChild("neck3", CubeListBuilder.create()
				.texOffs(240, 20).addBox(-2.0f, -7.0f, -2.0f, 4.0f, 7.0f, 4.0f, new CubeDeformation(0.0f))
				.texOffs(228, 34).addBox(-1.5f, -7.0f, -2.6f, 4.0f, 7.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, -7.0f, 0.0f, 0.42f, 0.0f, 0.0f));
		PartDefinition p6 = p5.addOrReplaceChild("head", CubeListBuilder.create()
				.texOffs(40, 20).addBox(-4.0f, -5.0f, -4.0f, 8.0f, 6.0f, 7.0f, new CubeDeformation(0.0f))
				.texOffs(214, 20).addBox(-3.0f, -3.2f, -10.5f, 6.0f, 4.0f, 7.0f, new CubeDeformation(0.0f))
				.texOffs(184, 44).addBox(-2.5f, -2.8f, -13.0f, 5.0f, 3.0f, 3.0f, new CubeDeformation(0.0f))
				.texOffs(170, 52).addBox(-0.5f, -4.6f, -12.2f, 1.0f, 2.0f, 1.0f, new CubeDeformation(0.0f))
				.texOffs(174, 52).addBox(-2.6f, 0.8f, -12.3f, 1.0f, 2.0f, 1.0f, new CubeDeformation(0.0f))
				.texOffs(174, 52).mirror().addBox(1.6f, 0.8f, -12.3f, 1.0f, 2.0f, 1.0f, new CubeDeformation(0.0f)).mirror(false),
				PartPose.offsetAndRotation(0.0f, -6.0f, 0.0f, 0.22f, 0.0f, 0.0f));
		p6.addOrReplaceChild("jaw", CubeListBuilder.create()
				.texOffs(134, 20).addBox(-2.5f, 0.0f, -9.5f, 5.0f, 2.0f, 10.0f, new CubeDeformation(0.0f))
				.texOffs(146, 52).addBox(-0.5f, 1.5f, -9.2f, 1.0f, 2.0f, 2.0f, new CubeDeformation(0.0f))
				.texOffs(174, 52).addBox(-2.2f, -1.6f, -9.1f, 1.0f, 2.0f, 1.0f, new CubeDeformation(0.0f))
				.texOffs(174, 52).mirror().addBox(1.2f, -1.6f, -9.1f, 1.0f, 2.0f, 1.0f, new CubeDeformation(0.0f)).mirror(false),
				PartPose.offsetAndRotation(0.0f, 0.8f, -2.8f, 0.06f, 0.0f, 0.0f));
		PartDefinition p8 = p6.addOrReplaceChild("l_horn", CubeListBuilder.create()
				.texOffs(34, 44).mirror().addBox(-1.0f, -1.0f, 0.0f, 2.0f, 2.0f, 6.0f, new CubeDeformation(0.0f)).mirror(false),
				PartPose.offsetAndRotation(2.6f, -4.5f, 0.5f, 0.65f, 0.32f, 0.1f));
		PartDefinition p9 = p8.addOrReplaceChild("l_horn2", CubeListBuilder.create()
				.texOffs(50, 44).mirror().addBox(-1.0f, -1.0f, 0.0f, 2.0f, 2.0f, 6.0f, new CubeDeformation(-0.15f)).mirror(false),
				PartPose.offsetAndRotation(0.0f, 0.0f, 5.6f, -0.42f, 0.12f, 0.0f));
		p9.addOrReplaceChild("l_horn3", CubeListBuilder.create()
				.texOffs(218, 44).addBox(-0.5f, -0.5f, 0.0f, 1.0f, 1.0f, 5.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, 0.0f, 5.4f, -0.5f, 0.0f, 0.0f));
		PartDefinition p11 = p6.addOrReplaceChild("r_horn", CubeListBuilder.create()
				.texOffs(34, 44).addBox(-1.0f, -1.0f, 0.0f, 2.0f, 2.0f, 6.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(-2.6f, -4.5f, 0.5f, 0.65f, -0.32f, -0.1f));
		PartDefinition p12 = p11.addOrReplaceChild("r_horn2", CubeListBuilder.create()
				.texOffs(50, 44).addBox(-1.0f, -1.0f, 0.0f, 2.0f, 2.0f, 6.0f, new CubeDeformation(-0.15f)),
				PartPose.offsetAndRotation(0.0f, 0.0f, 5.6f, -0.42f, -0.12f, 0.0f));
		p12.addOrReplaceChild("r_horn3", CubeListBuilder.create()
				.texOffs(218, 44).addBox(-0.5f, -0.5f, 0.0f, 1.0f, 1.0f, 5.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, 0.0f, 5.4f, -0.5f, 0.0f, 0.0f));
		p6.addOrReplaceChild("l_horn_s", CubeListBuilder.create()
				.texOffs(38, 52).addBox(-0.5f, -0.5f, 0.0f, 1.0f, 1.0f, 4.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(3.6f, -2.2f, 1.2f, 0.35f, 0.55f, 0.0f));
		p6.addOrReplaceChild("r_horn_s", CubeListBuilder.create()
				.texOffs(38, 52).addBox(-0.5f, -0.5f, 0.0f, 1.0f, 1.0f, 4.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(-3.6f, -2.2f, 1.2f, 0.35f, -0.55f, 0.0f));
		p6.addOrReplaceChild("l_brow", CubeListBuilder.create()
				.texOffs(0, 44).mirror().addBox(-1.5f, -1.0f, -5.5f, 3.0f, 2.0f, 6.0f, new CubeDeformation(0.0f)).mirror(false),
				PartPose.offsetAndRotation(2.4f, -4.6f, 1.0f, 0.12f, -0.18f, 0.18f));
		p6.addOrReplaceChild("r_brow", CubeListBuilder.create()
				.texOffs(0, 44).addBox(-1.5f, -1.0f, -5.5f, 3.0f, 2.0f, 6.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(-2.4f, -4.6f, 1.0f, 0.12f, 0.18f, -0.18f));
		p6.addOrReplaceChild("l_frill0", CubeListBuilder.create()
				.texOffs(122, 44).mirror().addBox(-0.5f, -4.0f, -1.0f, 1.0f, 4.0f, 3.0f, new CubeDeformation(0.0f)).mirror(false),
				PartPose.offsetAndRotation(3.9f, -1.0f, 1.5f, -0.9f, 0.0f, 0.5f));
		p6.addOrReplaceChild("l_frill1", CubeListBuilder.create()
				.texOffs(122, 44).mirror().addBox(-0.5f, -4.0f, -1.0f, 1.0f, 4.0f, 3.0f, new CubeDeformation(0.0f)).mirror(false),
				PartPose.offsetAndRotation(3.9f, 0.5f, 2.5f, -0.9f, 0.0f, 0.65f));
		p6.addOrReplaceChild("r_frill0", CubeListBuilder.create()
				.texOffs(122, 44).addBox(-0.5f, -4.0f, -1.0f, 1.0f, 4.0f, 3.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(-3.9f, -1.0f, 1.5f, -0.9f, 0.0f, -0.5f));
		p6.addOrReplaceChild("r_frill1", CubeListBuilder.create()
				.texOffs(122, 44).addBox(-0.5f, -4.0f, -1.0f, 1.0f, 4.0f, 3.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(-3.9f, 0.5f, 2.5f, -0.9f, 0.0f, -0.65f));
		p6.addOrReplaceChild("crown", CubeListBuilder.create()
				.texOffs(66, 44).addBox(-1.0f, -6.0f, -1.0f, 2.0f, 6.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, -4.6f, 0.8f, -0.42f, 0.0f, 0.0f));
		p6.addOrReplaceChild("l_crown", CubeListBuilder.create()
				.texOffs(48, 52).addBox(-0.5f, -4.0f, -0.5f, 1.0f, 4.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(1.6f, -4.7f, 1.8f, -0.55f, 0.0f, 0.32f));
		p6.addOrReplaceChild("r_crown", CubeListBuilder.create()
				.texOffs(48, 52).addBox(-0.5f, -4.0f, -0.5f, 1.0f, 4.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(-1.6f, -4.7f, 1.8f, -0.55f, 0.0f, -0.32f));
		p5.addOrReplaceChild("n3_spine0", CubeListBuilder.create()
				.texOffs(32, 52).addBox(-0.5f, -3.0f, -1.0f, 1.0f, 3.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, -2.0f, 2.0f, -0.6f, 0.0f, 0.0f));
		p5.addOrReplaceChild("n3_spine1", CubeListBuilder.create()
				.texOffs(32, 52).addBox(-0.5f, -3.0f, -1.0f, 1.0f, 3.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, -5.0f, 2.0f, -0.6f, 0.0f, 0.0f));
		p4.addOrReplaceChild("n2_spine0", CubeListBuilder.create()
				.texOffs(32, 52).addBox(-0.5f, -3.0f, -1.0f, 1.0f, 3.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, -2.0f, 2.5f, -0.6f, 0.0f, 0.0f));
		p4.addOrReplaceChild("n2_spine1", CubeListBuilder.create()
				.texOffs(32, 52).addBox(-0.5f, -3.0f, -1.0f, 1.0f, 3.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, -5.5f, 2.5f, -0.6f, 0.0f, 0.0f));
		p3.addOrReplaceChild("n1_spine0", CubeListBuilder.create()
				.texOffs(90, 44).addBox(-0.5f, -5.0f, -1.5f, 1.0f, 5.0f, 3.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, -2.5f, 3.0f, -0.6f, 0.0f, 0.0f));
		p3.addOrReplaceChild("n1_spine1", CubeListBuilder.create()
				.texOffs(90, 44).addBox(-0.5f, -5.0f, -1.5f, 1.0f, 5.0f, 3.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, -6.0f, 3.0f, -0.6f, 0.0f, 0.0f));
		PartDefinition p31 = p2.addOrReplaceChild("l_arm", CubeListBuilder.create()
				.texOffs(122, 34).mirror().addBox(-1.5f, -1.0f, -1.5f, 3.0f, 7.0f, 3.0f, new CubeDeformation(0.0f)).mirror(false),
				PartPose.offsetAndRotation(6.6f, -12.5f, -3.5f, -0.5f, 0.0f, -0.22f));
		PartDefinition p32 = p31.addOrReplaceChild("l_forearm", CubeListBuilder.create()
				.texOffs(194, 34).mirror().addBox(-1.5f, 0.0f, -1.5f, 3.0f, 6.0f, 3.0f, new CubeDeformation(0.0f)).mirror(false)
				.texOffs(200, 44).mirror().addBox(-1.5f, 1.5f, -1.5f, 3.0f, 3.0f, 3.0f, new CubeDeformation(0.35f)).mirror(false),
				PartPose.offsetAndRotation(0.0f, 5.8f, 0.0f, -1.0f, 0.0f, 0.0f));
		PartDefinition p33 = p32.addOrReplaceChild("l_hand", CubeListBuilder.create()
				.texOffs(0, 52).mirror().addBox(-1.5f, 0.0f, -1.5f, 3.0f, 2.0f, 3.0f, new CubeDeformation(0.0f)).mirror(false),
				PartPose.offsetAndRotation(0.0f, 5.6f, 0.0f, -0.15f, 0.0f, 0.0f));
		p33.addOrReplaceChild("l_hclaw0", CubeListBuilder.create()
				.texOffs(152, 52).addBox(-0.5f, 0.0f, -0.5f, 1.0f, 3.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(-1.0f, 1.6f, -1.0f, -0.5f, 0.0f, 0.0f));
		p33.addOrReplaceChild("l_hclaw1", CubeListBuilder.create()
				.texOffs(152, 52).addBox(-0.5f, 0.0f, -0.5f, 1.0f, 3.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, 1.6f, -1.0f, -0.5f, 0.0f, 0.0f));
		p33.addOrReplaceChild("l_hclaw2", CubeListBuilder.create()
				.texOffs(152, 52).addBox(-0.5f, 0.0f, -0.5f, 1.0f, 3.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(1.0f, 1.6f, -1.0f, -0.5f, 0.0f, 0.0f));
		PartDefinition p37 = p2.addOrReplaceChild("r_arm", CubeListBuilder.create()
				.texOffs(122, 34).addBox(-1.5f, -1.0f, -1.5f, 3.0f, 7.0f, 3.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(-6.6f, -12.5f, -3.5f, -0.5f, 0.0f, 0.22f));
		PartDefinition p38 = p37.addOrReplaceChild("r_forearm", CubeListBuilder.create()
				.texOffs(194, 34).addBox(-1.5f, 0.0f, -1.5f, 3.0f, 6.0f, 3.0f, new CubeDeformation(0.0f))
				.texOffs(200, 44).addBox(-1.5f, 1.5f, -1.5f, 3.0f, 3.0f, 3.0f, new CubeDeformation(0.35f)),
				PartPose.offsetAndRotation(0.0f, 5.8f, 0.0f, -1.0f, 0.0f, 0.0f));
		PartDefinition p39 = p38.addOrReplaceChild("r_hand", CubeListBuilder.create()
				.texOffs(0, 52).addBox(-1.5f, 0.0f, -1.5f, 3.0f, 2.0f, 3.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, 5.6f, 0.0f, -0.15f, 0.0f, 0.0f));
		p39.addOrReplaceChild("r_hclaw0", CubeListBuilder.create()
				.texOffs(152, 52).addBox(-0.5f, 0.0f, -0.5f, 1.0f, 3.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(-1.0f, 1.6f, -1.0f, -0.5f, 0.0f, 0.0f));
		p39.addOrReplaceChild("r_hclaw1", CubeListBuilder.create()
				.texOffs(152, 52).addBox(-0.5f, 0.0f, -0.5f, 1.0f, 3.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, 1.6f, -1.0f, -0.5f, 0.0f, 0.0f));
		p39.addOrReplaceChild("r_hclaw2", CubeListBuilder.create()
				.texOffs(152, 52).addBox(-0.5f, 0.0f, -0.5f, 1.0f, 3.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(1.0f, 1.6f, -1.0f, -0.5f, 0.0f, 0.0f));
		PartDefinition p43 = p2.addOrReplaceChild("l_wing_root", CubeListBuilder.create(),
				PartPose.offset(4.0f, -14.0f, 3.5f));
		PartDefinition p44 = p43.addOrReplaceChild("l_wing", CubeListBuilder.create()
				.texOffs(146, 44).mirror().addBox(0.0f, -1.5f, -1.5f, 10.0f, 3.0f, 3.0f, new CubeDeformation(0.0f)).mirror(false)
				.texOffs(150, 0).mirror().addBox(0.0f, 0.0f, 0.0f, 10.0f, 0.0f, 15.0f, new CubeDeformation(0.0f)).mirror(false),
				PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f, 0.0f, -0.2f, -0.45f));
		PartDefinition p45 = p44.addOrReplaceChild("l_wfore", CubeListBuilder.create()
				.texOffs(52, 52).mirror().addBox(0.0f, -1.0f, -1.0f, 12.0f, 2.0f, 2.0f, new CubeDeformation(0.0f)).mirror(false)
				.texOffs(200, 0).mirror().addBox(0.0f, 0.0f, 0.0f, 12.0f, 0.0f, 14.0f, new CubeDeformation(0.0f)).mirror(false),
				PartPose.offsetAndRotation(9.5f, 0.0f, 0.0f, 0.0f, 0.55f, 0.15f));
		p45.addOrReplaceChild("l_finger1", CubeListBuilder.create()
				.texOffs(178, 52).mirror().addBox(0.0f, -0.5f, -0.5f, 15.0f, 1.0f, 1.0f, new CubeDeformation(0.0f)).mirror(false)
				.texOffs(0, 34).mirror().addBox(0.0f, 0.04f, 0.0f, 15.0f, 0.0f, 10.0f, new CubeDeformation(0.0f)).mirror(false),
				PartPose.offset(11.5f, 0.0f, 0.0f));
		p45.addOrReplaceChild("l_finger2", CubeListBuilder.create()
				.texOffs(210, 52).mirror().addBox(0.0f, -0.5f, -0.5f, 14.0f, 1.0f, 1.0f, new CubeDeformation(0.0f)).mirror(false)
				.texOffs(50, 34).mirror().addBox(0.0f, 0.08f, 0.0f, 14.0f, 0.0f, 10.0f, new CubeDeformation(0.0f)).mirror(false),
				PartPose.offsetAndRotation(11.5f, 0.0f, 0.0f, 0.0f, -0.75f, 0.0f));
		p45.addOrReplaceChild("l_finger3", CubeListBuilder.create()
				.texOffs(0, 57).mirror().addBox(0.0f, -0.5f, -0.5f, 12.0f, 1.0f, 1.0f, new CubeDeformation(0.0f)).mirror(false)
				.texOffs(80, 52).mirror().addBox(0.0f, 0.12f, 0.0f, 12.0f, 0.0f, 4.0f, new CubeDeformation(0.0f)).mirror(false),
				PartPose.offsetAndRotation(11.5f, 0.0f, 0.0f, 0.0f, -1.45f, 0.0f));
		p45.addOrReplaceChild("l_thumb", CubeListBuilder.create()
				.texOffs(156, 52).addBox(-0.5f, -0.5f, -3.0f, 1.0f, 1.0f, 3.0f, new CubeDeformation(0.0f)),
				PartPose.offset(11.5f, -0.6f, 0.0f));
		PartDefinition p50 = p2.addOrReplaceChild("r_wing_root", CubeListBuilder.create(),
				PartPose.offset(-4.0f, -14.0f, 3.5f));
		PartDefinition p51 = p50.addOrReplaceChild("r_wing", CubeListBuilder.create()
				.texOffs(146, 44).addBox(-10.0f, -1.5f, -1.5f, 10.0f, 3.0f, 3.0f, new CubeDeformation(0.0f))
				.texOffs(150, 0).addBox(-10.0f, 0.0f, 0.0f, 10.0f, 0.0f, 15.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f, 0.0f, 0.2f, 0.45f));
		PartDefinition p52 = p51.addOrReplaceChild("r_wfore", CubeListBuilder.create()
				.texOffs(52, 52).addBox(-12.0f, -1.0f, -1.0f, 12.0f, 2.0f, 2.0f, new CubeDeformation(0.0f))
				.texOffs(200, 0).addBox(-12.0f, 0.0f, 0.0f, 12.0f, 0.0f, 14.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(-9.5f, 0.0f, 0.0f, 0.0f, -0.55f, -0.15f));
		p52.addOrReplaceChild("r_finger1", CubeListBuilder.create()
				.texOffs(178, 52).addBox(-15.0f, -0.5f, -0.5f, 15.0f, 1.0f, 1.0f, new CubeDeformation(0.0f))
				.texOffs(0, 34).addBox(-15.0f, 0.04f, 0.0f, 15.0f, 0.0f, 10.0f, new CubeDeformation(0.0f)),
				PartPose.offset(-11.5f, 0.0f, 0.0f));
		p52.addOrReplaceChild("r_finger2", CubeListBuilder.create()
				.texOffs(210, 52).addBox(-14.0f, -0.5f, -0.5f, 14.0f, 1.0f, 1.0f, new CubeDeformation(0.0f))
				.texOffs(50, 34).addBox(-14.0f, 0.08f, 0.0f, 14.0f, 0.0f, 10.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(-11.5f, 0.0f, 0.0f, 0.0f, 0.75f, 0.0f));
		p52.addOrReplaceChild("r_finger3", CubeListBuilder.create()
				.texOffs(0, 57).addBox(-12.0f, -0.5f, -0.5f, 12.0f, 1.0f, 1.0f, new CubeDeformation(0.0f))
				.texOffs(80, 52).addBox(-12.0f, 0.12f, 0.0f, 12.0f, 0.0f, 4.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(-11.5f, 0.0f, 0.0f, 0.0f, 1.45f, 0.0f));
		p52.addOrReplaceChild("r_thumb", CubeListBuilder.create()
				.texOffs(156, 52).addBox(-0.5f, -0.5f, -3.0f, 1.0f, 1.0f, 3.0f, new CubeDeformation(0.0f)),
				PartPose.offset(-11.5f, -0.6f, 0.0f));
		PartDefinition p57 = p2.addOrReplaceChild("l_pauldron", CubeListBuilder.create()
				.texOffs(98, 34).mirror().addBox(-2.5f, -1.5f, -3.5f, 5.0f, 3.0f, 7.0f, new CubeDeformation(0.0f)).mirror(false),
				PartPose.offsetAndRotation(5.8f, -14.5f, -0.5f, 0.0f, 0.0f, 0.3f));
		p57.addOrReplaceChild("l_pspike0", CubeListBuilder.create()
				.texOffs(212, 44).addBox(-0.5f, -4.0f, -1.0f, 1.0f, 4.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.5f, -1.5f, -1.5f, -0.25f, 0.0f, 0.3f));
		p57.addOrReplaceChild("l_pspike1", CubeListBuilder.create()
				.texOffs(212, 44).addBox(-0.5f, -4.0f, -1.0f, 1.0f, 4.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(1.0f, -1.5f, 1.5f, 0.15f, 0.0f, 0.45f));
		PartDefinition p60 = p2.addOrReplaceChild("r_pauldron", CubeListBuilder.create()
				.texOffs(98, 34).addBox(-2.5f, -1.5f, -3.5f, 5.0f, 3.0f, 7.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(-5.8f, -14.5f, -0.5f, 0.0f, 0.0f, -0.3f));
		p60.addOrReplaceChild("r_pspike0", CubeListBuilder.create()
				.texOffs(212, 44).addBox(-0.5f, -4.0f, -1.0f, 1.0f, 4.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(-0.5f, -1.5f, -1.5f, -0.25f, 0.0f, -0.3f));
		p60.addOrReplaceChild("r_pspike1", CubeListBuilder.create()
				.texOffs(212, 44).addBox(-0.5f, -4.0f, -1.0f, 1.0f, 4.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(-1.0f, -1.5f, 1.5f, 0.15f, 0.0f, -0.45f));
		p2.addOrReplaceChild("back_spine0", CubeListBuilder.create()
				.texOffs(154, 34).addBox(-1.0f, -7.0f, -1.5f, 2.0f, 7.0f, 3.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, -13.0f, 6.0f, -0.55f, 0.0f, 0.0f));
		p2.addOrReplaceChild("back_spine1", CubeListBuilder.create()
				.texOffs(154, 34).addBox(-1.0f, -7.0f, -1.5f, 2.0f, 7.0f, 3.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, -9.5f, 6.0f, -0.75f, 0.0f, 0.0f));
		p2.addOrReplaceChild("back_spine2", CubeListBuilder.create()
				.texOffs(90, 44).addBox(-0.5f, -5.0f, -1.5f, 1.0f, 5.0f, 3.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, -5.5f, 5.0f, -0.9f, 0.0f, 0.0f));
		p2.addOrReplaceChild("back_spine3", CubeListBuilder.create()
				.texOffs(32, 52).addBox(-0.5f, -3.0f, -1.0f, 1.0f, 3.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, -2.0f, 4.6f, -1.0f, 0.0f, 0.0f));
		PartDefinition p67 = p1.addOrReplaceChild("t1", CubeListBuilder.create()
				.texOffs(70, 20).addBox(-3.0f, -2.5f, 0.0f, 6.0f, 5.0f, 8.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, -1.0f, 4.0f, 0.62f, 0.0f, 0.0f));
		PartDefinition p68 = p67.addOrReplaceChild("t2", CubeListBuilder.create()
				.texOffs(164, 20).addBox(-2.5f, -2.0f, 0.0f, 5.0f, 4.0f, 8.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, 0.0f, 7.5f, -0.25f, 0.0f, 0.0f));
		PartDefinition p69 = p68.addOrReplaceChild("t3", CubeListBuilder.create()
				.texOffs(190, 20).addBox(-2.0f, -2.0f, 0.0f, 4.0f, 4.0f, 8.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, 0.0f, 7.5f, -0.2f, 0.0f, 0.0f));
		PartDefinition p70 = p69.addOrReplaceChild("t4", CubeListBuilder.create()
				.texOffs(134, 34).addBox(-1.5f, -1.5f, 0.0f, 3.0f, 3.0f, 7.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, 0.0f, 7.5f, -0.15f, 0.0f, 0.0f));
		PartDefinition p71 = p70.addOrReplaceChild("t5", CubeListBuilder.create()
				.texOffs(74, 44).addBox(-1.0f, -1.0f, 0.0f, 2.0f, 2.0f, 6.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, 0.0f, 6.5f, -0.12f, 0.0f, 0.0f));
		PartDefinition p72 = p71.addOrReplaceChild("mace", CubeListBuilder.create()
				.texOffs(18, 44).addBox(-1.5f, -1.5f, 0.0f, 3.0f, 3.0f, 5.0f, new CubeDeformation(0.0f))
				.texOffs(244, 44).addBox(-2.0f, -2.0f, -0.5f, 4.0f, 4.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offset(0.0f, 0.0f, 5.5f));
		p72.addOrReplaceChild("mace_spike0", CubeListBuilder.create()
				.texOffs(140, 44).addBox(-0.5f, -5.0f, -1.0f, 1.0f, 5.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offset(0.0f, 0.0f, 2.5f));
		p72.addOrReplaceChild("mace_spike1", CubeListBuilder.create()
				.texOffs(140, 44).addBox(-0.5f, -5.0f, -1.0f, 1.0f, 5.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, 0.0f, 2.5f, 0.0f, 0.0f, 1.57f));
		p72.addOrReplaceChild("mace_spike2", CubeListBuilder.create()
				.texOffs(140, 44).addBox(-0.5f, -5.0f, -1.0f, 1.0f, 5.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, 0.0f, 2.5f, 0.0f, 0.0f, 3.14f));
		p72.addOrReplaceChild("mace_spike3", CubeListBuilder.create()
				.texOffs(140, 44).addBox(-0.5f, -5.0f, -1.0f, 1.0f, 5.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, 0.0f, 2.5f, 0.0f, 0.0f, -1.57f));
		p72.addOrReplaceChild("mace_spike4", CubeListBuilder.create()
				.texOffs(140, 44).addBox(-0.5f, -5.0f, -1.0f, 1.0f, 5.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, 0.0f, 2.5f, 0.0f, 0.0f, 0.78f));
		p72.addOrReplaceChild("mace_spike5", CubeListBuilder.create()
				.texOffs(140, 44).addBox(-0.5f, -5.0f, -1.0f, 1.0f, 5.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, 0.0f, 2.5f, 0.0f, 0.0f, -0.78f));
		p72.addOrReplaceChild("mace_spike6", CubeListBuilder.create()
				.texOffs(140, 44).addBox(-0.5f, -5.0f, -1.0f, 1.0f, 5.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, 0.0f, 2.5f, 0.0f, 0.0f, 2.36f));
		p72.addOrReplaceChild("mace_spike7", CubeListBuilder.create()
				.texOffs(140, 44).addBox(-0.5f, -5.0f, -1.0f, 1.0f, 5.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, 0.0f, 2.5f, 0.0f, 0.0f, -2.36f));
		p70.addOrReplaceChild("t4_bl", CubeListBuilder.create()
				.texOffs(130, 44).mirror().addBox(-0.5f, -3.0f, -2.0f, 1.0f, 3.0f, 4.0f, new CubeDeformation(0.0f)).mirror(false),
				PartPose.offsetAndRotation(1.4f, 0.0f, 3.5f, 0.0f, 0.0f, 1.2f));
		p70.addOrReplaceChild("t4_br", CubeListBuilder.create()
				.texOffs(130, 44).addBox(-0.5f, -3.0f, -2.0f, 1.0f, 3.0f, 4.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(-1.4f, 0.0f, 3.5f, 0.0f, 0.0f, -1.2f));
		p70.addOrReplaceChild("t4_spine0", CubeListBuilder.create()
				.texOffs(32, 52).addBox(-0.5f, -3.0f, -1.0f, 1.0f, 3.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, -1.5f, 3.5f, -0.8f, 0.0f, 0.0f));
		p69.addOrReplaceChild("t3_bl", CubeListBuilder.create()
				.texOffs(130, 44).mirror().addBox(-0.5f, -3.0f, -2.0f, 1.0f, 3.0f, 4.0f, new CubeDeformation(0.0f)).mirror(false),
				PartPose.offsetAndRotation(1.9f, 0.0f, 4.0f, 0.0f, 0.0f, 1.2f));
		p69.addOrReplaceChild("t3_br", CubeListBuilder.create()
				.texOffs(130, 44).addBox(-0.5f, -3.0f, -2.0f, 1.0f, 3.0f, 4.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(-1.9f, 0.0f, 4.0f, 0.0f, 0.0f, -1.2f));
		p69.addOrReplaceChild("t3_spine0", CubeListBuilder.create()
				.texOffs(32, 52).addBox(-0.5f, -3.0f, -1.0f, 1.0f, 3.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, -2.0f, 4.0f, -0.8f, 0.0f, 0.0f));
		p68.addOrReplaceChild("t2_spine0", CubeListBuilder.create()
				.texOffs(90, 44).addBox(-0.5f, -5.0f, -1.5f, 1.0f, 5.0f, 3.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, -2.0f, 2.5f, -0.8f, 0.0f, 0.0f));
		p68.addOrReplaceChild("t2_spine1", CubeListBuilder.create()
				.texOffs(32, 52).addBox(-0.5f, -3.0f, -1.0f, 1.0f, 3.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, -2.0f, 6.0f, -0.8f, 0.0f, 0.0f));
		p67.addOrReplaceChild("t1_spine0", CubeListBuilder.create()
				.texOffs(90, 44).addBox(-0.5f, -5.0f, -1.5f, 1.0f, 5.0f, 3.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, -2.5f, 2.5f, -0.8f, 0.0f, 0.0f));
		p67.addOrReplaceChild("t1_spine1", CubeListBuilder.create()
				.texOffs(90, 44).addBox(-0.5f, -5.0f, -1.5f, 1.0f, 5.0f, 3.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, -2.5f, 6.0f, -0.8f, 0.0f, 0.0f));
		PartDefinition p91 = root.addOrReplaceChild("l_leg", CubeListBuilder.create()
				.texOffs(48, 0).mirror().addBox(-3.0f, -2.0f, -3.5f, 6.0f, 10.0f, 7.0f, new CubeDeformation(0.0f)).mirror(false)
				.texOffs(230, 44).mirror().addBox(-2.5f, 6.5f, -4.3f, 5.0f, 3.0f, 2.0f, new CubeDeformation(0.0f)).mirror(false),
				PartPose.offsetAndRotation(4.8f, 2.5f, 2.5f, -0.55f, 0.0f, 0.0f));
		PartDefinition p92 = p91.addOrReplaceChild("l_shin", CubeListBuilder.create()
				.texOffs(118, 20).mirror().addBox(-2.0f, 0.0f, -2.0f, 4.0f, 9.0f, 4.0f, new CubeDeformation(0.0f)).mirror(false),
				PartPose.offsetAndRotation(0.0f, 7.8f, -0.5f, 1.25f, 0.0f, 0.0f));
		p92.addOrReplaceChild("l_spur", CubeListBuilder.create()
				.texOffs(28, 52).addBox(-0.5f, -4.0f, -0.5f, 1.0f, 4.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, 2.5f, 2.0f, -0.9f, 0.0f, 0.0f));
		PartDefinition p94 = p92.addOrReplaceChild("l_meta", CubeListBuilder.create()
				.texOffs(238, 34).mirror().addBox(-1.5f, 0.0f, -1.5f, 3.0f, 5.0f, 3.0f, new CubeDeformation(0.0f)).mirror(false),
				PartPose.offsetAndRotation(0.0f, 8.4f, 0.0f, -0.95f, 0.0f, 0.0f));
		PartDefinition p95 = p94.addOrReplaceChild("l_foot", CubeListBuilder.create()
				.texOffs(206, 34).mirror().addBox(-2.5f, 0.0f, -4.5f, 5.0f, 2.0f, 6.0f, new CubeDeformation(0.0f)).mirror(false)
				.texOffs(164, 52).addBox(-0.5f, 1.0f, 1.2f, 1.0f, 1.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, 4.6f, 0.0f, 0.25f, 0.0f, 0.0f));
		p95.addOrReplaceChild("l_talon0", CubeListBuilder.create()
				.texOffs(20, 52).addBox(-0.5f, 0.0f, -3.0f, 1.0f, 2.0f, 3.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(-1.7f, 1.0f, -4.2f, 0.35f, 0.0f, 0.0f));
		p95.addOrReplaceChild("l_talon1", CubeListBuilder.create()
				.texOffs(20, 52).addBox(-0.5f, 0.0f, -3.0f, 1.0f, 2.0f, 3.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, 1.0f, -4.2f, 0.35f, 0.0f, 0.0f));
		p95.addOrReplaceChild("l_talon2", CubeListBuilder.create()
				.texOffs(20, 52).addBox(-0.5f, 0.0f, -3.0f, 1.0f, 2.0f, 3.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(1.7f, 1.0f, -4.2f, 0.35f, 0.0f, 0.0f));
		PartDefinition p99 = root.addOrReplaceChild("r_leg", CubeListBuilder.create()
				.texOffs(48, 0).addBox(-3.0f, -2.0f, -3.5f, 6.0f, 10.0f, 7.0f, new CubeDeformation(0.0f))
				.texOffs(230, 44).addBox(-2.5f, 6.5f, -4.3f, 5.0f, 3.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(-4.8f, 2.5f, 2.5f, -0.55f, 0.0f, 0.0f));
		PartDefinition p100 = p99.addOrReplaceChild("r_shin", CubeListBuilder.create()
				.texOffs(118, 20).addBox(-2.0f, 0.0f, -2.0f, 4.0f, 9.0f, 4.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, 7.8f, -0.5f, 1.25f, 0.0f, 0.0f));
		p100.addOrReplaceChild("r_spur", CubeListBuilder.create()
				.texOffs(28, 52).addBox(-0.5f, -4.0f, -0.5f, 1.0f, 4.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, 2.5f, 2.0f, -0.9f, 0.0f, 0.0f));
		PartDefinition p102 = p100.addOrReplaceChild("r_meta", CubeListBuilder.create()
				.texOffs(238, 34).addBox(-1.5f, 0.0f, -1.5f, 3.0f, 5.0f, 3.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, 8.4f, 0.0f, -0.95f, 0.0f, 0.0f));
		PartDefinition p103 = p102.addOrReplaceChild("r_foot", CubeListBuilder.create()
				.texOffs(206, 34).addBox(-2.5f, 0.0f, -4.5f, 5.0f, 2.0f, 6.0f, new CubeDeformation(0.0f))
				.texOffs(164, 52).addBox(-0.5f, 1.0f, 1.2f, 1.0f, 1.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, 4.6f, 0.0f, 0.25f, 0.0f, 0.0f));
		p103.addOrReplaceChild("r_talon0", CubeListBuilder.create()
				.texOffs(20, 52).addBox(-0.5f, 0.0f, -3.0f, 1.0f, 2.0f, 3.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(-1.7f, 1.0f, -4.2f, 0.35f, 0.0f, 0.0f));
		p103.addOrReplaceChild("r_talon1", CubeListBuilder.create()
				.texOffs(20, 52).addBox(-0.5f, 0.0f, -3.0f, 1.0f, 2.0f, 3.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, 1.0f, -4.2f, 0.35f, 0.0f, 0.0f));
		p103.addOrReplaceChild("r_talon2", CubeListBuilder.create()
				.texOffs(20, 52).addBox(-0.5f, 0.0f, -3.0f, 1.0f, 2.0f, 3.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(1.7f, 1.0f, -4.2f, 0.35f, 0.0f, 0.0f));
		return LayerDefinition.create(mesh, 256, 256);
	}

	private void poseFold(float k) {
		lWing.xRot += k * 2.940f;
		lWing.yRot += k * -0.210f;
		lWing.zRot += k * 0.030f;
		rWing.xRot += k * 2.940f;
		rWing.yRot += k * 0.210f;
		rWing.zRot += k * -0.030f;
		lWfore.yRot += k * 2.150f;
		lWfore.zRot += k * 0.800f;
		rWfore.yRot += k * -2.150f;
		rWfore.zRot += k * -0.800f;
		lFinger1.yRot += k * -0.180f;
		rFinger1.yRot += k * 0.180f;
		lFinger2.yRot += k * 0.330f;
		rFinger2.yRot += k * -0.330f;
		lFinger3.yRot += k * 0.890f;
		rFinger3.yRot += k * -0.890f;
	}

	private void poseFly(float k) {
		body.xRot += k * 0.950f;
		lWingRoot.xRot += k * -0.950f;
		rWingRoot.xRot += k * -0.950f;
		lLeg.xRot += k * 0.950f;
		rLeg.xRot += k * 0.950f;
		lShin.xRot += k * 0.300f;
		rShin.xRot += k * 0.300f;
		neck1.xRot += k * 0.450f;
		head.xRot += k * 0.150f;
		t1.xRot += k * -0.500f;
		lArm.xRot += k * 0.600f;
		rArm.xRot += k * 0.600f;
	}

	private void poseFlapUp(float k) {
		lWing.zRot += k * -0.550f;
		rWing.zRot += k * 0.550f;
		lWfore.zRot += k * -0.250f;
		rWfore.zRot += k * 0.250f;
	}

	private void poseFlapDown(float k) {
		lWing.zRot += k * 0.450f;
		rWing.zRot += k * -0.450f;
		lWfore.zRot += k * 0.300f;
		rWfore.zRot += k * -0.300f;
	}

	private void poseBreath(float k) {
		jaw.xRot += k * 0.750f;
		neck1.xRot += k * 0.300f;
		head.xRot += k * 0.150f;
		torso.xRot += k * 0.100f;
	}

	private void poseClaw(float k) {
		rArm.xRot += k * -1.600f;
		rForearm.xRot += k * 0.600f;
		lArm.xRot += k * -0.600f;
		torso.yRot += k * -0.300f;
		head.xRot += k * 0.250f;
		jaw.xRot += k * 0.400f;
		t1.yRot += k * 0.700f;
		t2.yRot += k * 0.500f;
	}

	@Override
	public void setupAnim(CheldroganRenderState state) {
		super.setupAnim(state);
		float age = state.ageInTicks;
		float pos = state.walkAnimationPos;
		float speed = Math.min(state.walkAnimationSpeed, 1.0f);
		float fly = state.flyAmount;
		float ground = 1.0f - fly;

		// Flügel anlegen (Boden) bzw. Flughaltung, dazu Flügelschlag
		poseFold(ground);
		poseFly(fly);
		float flap = Mth.sin(age * 0.3f);

		if (flap > 0.0f) {
			poseFlapUp(flap * fly);
		} else {
			poseFlapDown(-flap * fly);
		}

		float lag = Mth.sin(age * 0.3f - 0.8f) * 0.18f * fly;
		lFinger1.zRot -= lag;
		lFinger2.zRot -= lag;
		lFinger3.zRot -= lag;
		rFinger1.zRot += lag;
		rFinger2.zRot += lag;
		rFinger3.zRot += lag;
		body.y += flap * 0.8f * fly;

		// Blick: verteilt auf Hals und Kopf
		float yaw = Mth.clamp(state.yRot, -60.0f, 60.0f) * Mth.DEG_TO_RAD;
		float pitch = Mth.clamp(state.xRot, -40.0f, 40.0f) * Mth.DEG_TO_RAD;
		neck1.yRot += yaw * 0.25f;
		neck2.yRot += yaw * 0.3f;
		head.yRot += yaw * 0.45f;
		head.xRot += pitch * 0.65f;

		// Atmen (Brust hebt sich, Flügel zittern leicht)
		float breathe = Mth.sin(age * 0.06f);
		torso.xRot += breathe * 0.015f;
		neck1.xRot += breathe * 0.02f;
		head.xRot -= breathe * 0.025f;
		lWing.zRot += breathe * 0.02f * ground;
		rWing.zRot -= breathe * 0.02f * ground;

		// Gehen (zweibeinig, digitigrad)
		float step = Mth.cos(pos * 0.5f);
		float walk = speed * ground;
		lLeg.xRot += step * 0.55f * walk;
		rLeg.xRot -= step * 0.55f * walk;
		lShin.xRot += Math.max(0.0f, -step) * 0.45f * walk;
		rShin.xRot += Math.max(0.0f, step) * 0.45f * walk;
		lMeta.xRot -= Math.max(0.0f, -step) * 0.25f * walk;
		rMeta.xRot -= Math.max(0.0f, step) * 0.25f * walk;
		body.y += Math.abs(Mth.sin(pos * 0.5f)) * 0.9f * walk;
		body.zRot += step * 0.04f * walk;
		lArm.xRot -= step * 0.2f * speed;
		rArm.xRot += step * 0.2f * speed;

		// Schwanz: Gegengewicht, Spitze schwingt nach
		float sway = Mth.sin(age * 0.04f) * 0.1f + Mth.sin(pos * 0.5f) * 0.18f * walk;
		t1.yRot += sway * 0.8f;
		t2.yRot += sway;
		t3.yRot += sway * 1.1f;
		t4.yRot += sway * 1.2f;
		t5.yRot += sway * 1.3f;
		t3.xRot += Mth.sin(age * 0.05f + 1.0f) * 0.04f;

		// Klauenhieb mit Schwanzpeitsche
		if (state.attackProgress > 0.0f) {
			poseClaw(Mth.sin(state.attackProgress * Mth.PI));
		}

		// Kristall-Odem: Kopf nach vorn, Maul weit offen
		if (state.breathProgress > 0.0f) {
			float b = state.breathProgress;
			poseBreath(Math.min(1.0f, b * 5.0f) * Math.min(1.0f, (1.0f - b) * 5.0f));
		}
	}

	@Override
	public float hatScale() {
		return 1.0f;
	}

	@Override
	public void translateToHat(PoseStack pose) {
		root().translateAndRotate(pose);
		body.translateAndRotate(pose);
		torso.translateAndRotate(pose);
		neck1.translateAndRotate(pose);
		neck2.translateAndRotate(pose);
		neck3.translateAndRotate(pose);
		head.translateAndRotate(pose);
		pose.translate(0.0f, -0.05f, 0.0f);
	}
}
