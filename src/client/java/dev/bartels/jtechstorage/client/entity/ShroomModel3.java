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
 * Amethyst-Pilz, Stufe 3 (Begleiter v0.25).
 * Modell in doppelter Detaildichte (halbe Pixel) – der Renderer skaliert es auf 0.5.
 * Erzeugt von scratchpad/shroom.py (Teile und Posen), Animation von Hand.
 */
public class ShroomModel3 extends EntityModel<CompanionRenderState> implements CompanionLookModel {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(JTechStorage.id("shroom_3"), "main");

	private final ModelPart shroom;
	private final ModelPart body;
	private final ModelPart cap;
	private final ModelPart rLeg;
	private final ModelPart lLeg;
	private final ModelPart rArm;
	private final ModelPart lArm;
	private final ModelPart ring;
	private final ModelPart mini0;
	private final ModelPart mini1;
	private final ModelPart pack;
	private final ModelPart potion;
	private final ModelPart staff;
	private final ModelPart capWitch;
	private final ModelPart capFlat;
	private final ModelPart strap0;
	private final ModelPart strap1;
	private final ModelPart capeCape;
	private final ModelPart collar;
	private final ModelPart amulet;
	private final ModelPart armorC;
	private final ModelPart rShoulder;
	private final ModelPart lShoulder;

	public ShroomModel3(ModelPart root) {
		super(root);
		this.shroom = root.getChild("shroom");
		this.body = root.getChild("shroom").getChild("body");
		this.cap = root.getChild("shroom").getChild("body").getChild("cap");
		this.rLeg = root.getChild("shroom").getChild("body").getChild("r_leg");
		this.lLeg = root.getChild("shroom").getChild("body").getChild("l_leg");
		this.rArm = root.getChild("shroom").getChild("body").getChild("r_arm");
		this.lArm = root.getChild("shroom").getChild("body").getChild("l_arm");
		this.ring = root.getChild("shroom").getChild("body").getChild("ring");
		this.mini0 = root.getChild("shroom").getChild("body").getChild("mini0");
		this.mini1 = root.getChild("shroom").getChild("body").getChild("mini1");
		this.pack = root.getChild("shroom").getChild("body").getChild("pack");
		this.potion = root.getChild("shroom").getChild("body").getChild("pack").getChild("potion");
		this.staff = root.getChild("shroom").getChild("body").getChild("r_arm").getChild("staff");
		this.capWitch = root.getChild("shroom").getChild("body").getChild("cap_witch");
		this.capFlat = root.getChild("shroom").getChild("body").getChild("cap_flat");
		this.strap0 = root.getChild("shroom").getChild("body").getChild("strap0");
		this.strap1 = root.getChild("shroom").getChild("body").getChild("strap1");
		this.capeCape = root.getChild("shroom").getChild("body").getChild("cape_cape");
		this.collar = root.getChild("shroom").getChild("body").getChild("collar");
		this.amulet = root.getChild("shroom").getChild("body").getChild("amulet");
		this.armorC = root.getChild("shroom").getChild("body").getChild("armor_c");
		this.rShoulder = root.getChild("shroom").getChild("body").getChild("r_shoulder");
		this.lShoulder = root.getChild("shroom").getChild("body").getChild("l_shoulder");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		PartDefinition p1 = root.addOrReplaceChild("shroom", CubeListBuilder.create(),
				PartPose.offset(0.0f, 24.0f, 0.0f));
		PartDefinition p2 = p1.addOrReplaceChild("body", CubeListBuilder.create()
				.texOffs(112, 0).addBox(-6.0f, -17.0f, -6.0f, 12.0f, 17.0f, 12.0f, new CubeDeformation(0.0f)),
				PartPose.offset(0.0f, -8.5f, 0.0f));
		PartDefinition p3 = p2.addOrReplaceChild("cap", CubeListBuilder.create()
				.texOffs(0, 0).addBox(-14.0f, -7.0f, -14.0f, 28.0f, 7.0f, 28.0f, new CubeDeformation(0.0f))
				.texOffs(160, 0).addBox(-10.0f, -11.0f, -10.0f, 20.0f, 4.0f, 20.0f, new CubeDeformation(0.0f))
				.texOffs(108, 35).addBox(-5.0f, -14.0f, -5.0f, 10.0f, 3.0f, 10.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, -15.0f, 0.0f, 0.0f, 0.0f, 0.12f));
		p3.addOrReplaceChild("crystal0", CubeListBuilder.create()
				.texOffs(184, 35).addBox(-1.5f, -9.0f, -1.5f, 3.0f, 9.0f, 3.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(-6.0f, -10.0f, -4.0f, -0.2f, 0.0f, -0.35f));
		p3.addOrReplaceChild("crystal1", CubeListBuilder.create()
				.texOffs(80, 53).addBox(-1.5f, -6.0f, -1.5f, 3.0f, 6.0f, 3.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(7.0f, -10.0f, -2.0f, 0.1f, 0.0f, 0.45f));
		p3.addOrReplaceChild("crystal2", CubeListBuilder.create()
				.texOffs(184, 53).addBox(-1.0f, -4.0f, -1.0f, 2.0f, 4.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(2.0f, -10.0f, 8.0f, 0.5f, 0.0f, 0.1f));
		p3.addOrReplaceChild("crystal3", CubeListBuilder.create()
				.texOffs(80, 53).addBox(-1.5f, -6.0f, -1.5f, 3.0f, 6.0f, 3.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(-8.0f, -10.0f, 7.0f, 0.4f, 0.0f, -0.5f));
		p3.addOrReplaceChild("crystal4", CubeListBuilder.create()
				.texOffs(184, 53).addBox(-1.0f, -4.0f, -1.0f, 2.0f, 4.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(11.0f, -6.0f, 6.0f, 0.2f, 0.0f, 0.7f));
		p3.addOrReplaceChild("crystal5", CubeListBuilder.create()
				.texOffs(184, 35).addBox(-1.5f, -9.0f, -1.5f, 3.0f, 9.0f, 3.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(1.0f, -10.0f, -1.0f, -0.05f, 0.0f, 0.05f));
		p2.addOrReplaceChild("r_leg", CubeListBuilder.create()
				.texOffs(234, 35).addBox(-2.5f, 0.0f, -2.5f, 5.0f, 6.0f, 5.0f, new CubeDeformation(0.0f))
				.texOffs(20, 53).addBox(-3.0f, 6.0f, -5.0f, 6.0f, 2.0f, 8.0f, new CubeDeformation(0.0f))
				.texOffs(172, 53).addBox(-3.5f, 6.5f, -1.0f, 2.0f, 2.0f, 4.0f, new CubeDeformation(0.0f))
				.texOffs(172, 53).addBox(1.5f, 6.5f, -1.0f, 2.0f, 2.0f, 4.0f, new CubeDeformation(0.0f)),
				PartPose.offset(-3.5f, 0.0f, 0.0f));
		p2.addOrReplaceChild("l_leg", CubeListBuilder.create()
				.texOffs(234, 35).addBox(-2.5f, 0.0f, -2.5f, 5.0f, 6.0f, 5.0f, new CubeDeformation(0.0f))
				.texOffs(20, 53).addBox(-3.0f, 6.0f, -5.0f, 6.0f, 2.0f, 8.0f, new CubeDeformation(0.0f))
				.texOffs(172, 53).addBox(-3.5f, 6.5f, -1.0f, 2.0f, 2.0f, 4.0f, new CubeDeformation(0.0f))
				.texOffs(172, 53).addBox(1.5f, 6.5f, -1.0f, 2.0f, 2.0f, 4.0f, new CubeDeformation(0.0f)),
				PartPose.offset(3.5f, 0.0f, 0.0f));
		p2.addOrReplaceChild("ring", CubeListBuilder.create()
				.texOffs(34, 35).addBox(-7.5f, 0.0f, -7.5f, 15.0f, 2.0f, 15.0f, new CubeDeformation(0.0f)),
				PartPose.offset(0.0f, -4.59f, 0.0f));
		PartDefinition p13 = p2.addOrReplaceChild("r_arm", CubeListBuilder.create()
				.texOffs(148, 35).addBox(-3.0f, 0.0f, -1.5f, 3.0f, 10.0f, 3.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(-6.0f, -9.35f, 0.0f, -0.9f, 0.0f, 0.25f));
		p13.addOrReplaceChild("staff", CubeListBuilder.create()
				.texOffs(94, 35).addBox(-1.0f, -5.0f, -1.0f, 2.0f, 14.0f, 2.0f, new CubeDeformation(0.0f))
				.texOffs(160, 35).addBox(-3.0f, -11.0f, -3.0f, 6.0f, 6.0f, 6.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(-1.5f, 9.0f, -1.0f, 0.9f, 0.0f, 0.0f));
		p2.addOrReplaceChild("l_arm", CubeListBuilder.create()
				.texOffs(148, 35).addBox(0.0f, 0.0f, -1.5f, 3.0f, 10.0f, 3.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(6.0f, -9.35f, 0.0f, 0.1f, 0.0f, -0.35f));
		p2.addOrReplaceChild("mini0", CubeListBuilder.create()
				.texOffs(134, 53).addBox(-1.0f, -5.0f, -1.0f, 2.0f, 5.0f, 2.0f, new CubeDeformation(0.0f))
				.texOffs(92, 53).addBox(-3.0f, -7.0f, -3.0f, 6.0f, 2.0f, 6.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(5.0f, -4.25f, 5.0f, 0.3f, 0.0f, 0.6f));
		p2.addOrReplaceChild("mini1", CubeListBuilder.create()
				.texOffs(134, 53).addBox(-1.0f, -5.0f, -1.0f, 2.0f, 5.0f, 2.0f, new CubeDeformation(0.0f))
				.texOffs(92, 53).addBox(-3.0f, -7.0f, -3.0f, 6.0f, 2.0f, 6.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(-5.0f, -2.55f, 4.0f, 0.4f, 0.0f, -0.7f));
		PartDefinition p18 = p2.addOrReplaceChild("pack", CubeListBuilder.create()
				.texOffs(0, 35).addBox(-5.5f, 0.0f, 0.0f, 11.0f, 12.0f, 6.0f, new CubeDeformation(0.0f))
				.texOffs(196, 35).addBox(-6.0f, -0.5f, -0.5f, 12.0f, 4.0f, 7.0f, new CubeDeformation(0.0f))
				.texOffs(116, 53).addBox(-3.5f, 5.4f, 5.5f, 7.0f, 5.0f, 2.0f, new CubeDeformation(0.0f))
				.texOffs(142, 53).addBox(-6.0f, 11.0f, 1.5f, 12.0f, 3.0f, 3.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, -13.6f, 5.5f, 0.06f, 0.0f, 0.0f));
		p18.addOrReplaceChild("potion", CubeListBuilder.create()
				.texOffs(0, 53).addBox(-2.5f, -6.0f, -2.5f, 5.0f, 6.0f, 5.0f, new CubeDeformation(0.0f))
				.texOffs(204, 53).addBox(-1.0f, -8.0f, -1.0f, 2.0f, 2.0f, 2.0f, new CubeDeformation(0.0f))
				.texOffs(192, 53).addBox(-1.5f, -9.5f, -1.5f, 3.0f, 2.0f, 3.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(2.5f, 1.5f, 4.5f, -0.95f, 0.0f, 0.25f));
		p2.addOrReplaceChild("strap0", CubeListBuilder.create()
				.texOffs(102, 35).addBox(0.0f, 0.0f, 0.0f, 2.0f, 13.0f, 1.0f, new CubeDeformation(0.0f))
				.texOffs(212, 53).addBox(0.0f, 6.0f, -0.5f, 2.0f, 2.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offset(-6.4f, -13.6f, -6.6f));
		p2.addOrReplaceChild("strap1", CubeListBuilder.create()
				.texOffs(102, 35).addBox(0.0f, 0.0f, 0.0f, 2.0f, 13.0f, 1.0f, new CubeDeformation(0.0f))
				.texOffs(212, 53).addBox(0.0f, 6.0f, -0.5f, 2.0f, 2.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offset(4.4f, -13.6f, -6.6f));
		PartDefinition p22 = p2.addOrReplaceChild("cap_witch", CubeListBuilder.create()
				.texOffs(0, 128).addBox(-12.0f, -5.0f, -12.0f, 24.0f, 5.0f, 24.0f, new CubeDeformation(0.0f))
				.texOffs(96, 128).addBox(-8.0f, -11.0f, -8.0f, 16.0f, 6.0f, 16.0f, new CubeDeformation(0.0f))
				.texOffs(160, 128).addBox(-5.0f, -16.0f, -5.0f, 10.0f, 5.0f, 10.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, -15.0f, 0.0f, 0.0f, 0.0f, 0.12f));
		p22.addOrReplaceChild("tip_witch", CubeListBuilder.create()
				.texOffs(200, 128).addBox(-3.0f, -9.0f, -3.0f, 6.0f, 9.0f, 6.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, -15.5f, 1.0f, -0.7f, 0.0f, 0.0f));
		p22.addOrReplaceChild("crystal0_witch", CubeListBuilder.create()
				.texOffs(224, 128).addBox(-1.5f, -9.0f, -1.5f, 3.0f, 9.0f, 3.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(-6.0f, -10.0f, -4.0f, -0.2f, 0.0f, -0.35f));
		p22.addOrReplaceChild("crystal1_witch", CubeListBuilder.create()
				.texOffs(236, 128).addBox(-1.5f, -6.0f, -1.5f, 3.0f, 6.0f, 3.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(7.0f, -4.0f, -2.0f, 0.1f, 0.0f, 0.45f));
		p22.addOrReplaceChild("crystal2_witch", CubeListBuilder.create()
				.texOffs(248, 128).addBox(-1.0f, -4.0f, -1.0f, 2.0f, 4.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(2.0f, -4.0f, 8.0f, 0.5f, 0.0f, 0.1f));
		PartDefinition p27 = p2.addOrReplaceChild("cap_flat", CubeListBuilder.create()
				.texOffs(0, 157).addBox(-17.0f, -4.0f, -17.0f, 34.0f, 4.0f, 34.0f, new CubeDeformation(0.0f))
				.texOffs(136, 157).addBox(-9.0f, -6.0f, -9.0f, 18.0f, 2.0f, 18.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, -15.0f, 0.0f, 0.0f, 0.0f, 0.12f));
		p27.addOrReplaceChild("crystal0_flat", CubeListBuilder.create()
				.texOffs(208, 157).addBox(-1.5f, -9.0f, -1.5f, 3.0f, 9.0f, 3.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(-6.0f, -5.0f, -4.0f, -0.2f, 0.0f, -0.35f));
		p27.addOrReplaceChild("crystal1_flat", CubeListBuilder.create()
				.texOffs(220, 157).addBox(-1.5f, -6.0f, -1.5f, 3.0f, 6.0f, 3.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(7.0f, -5.0f, -2.0f, 0.1f, 0.0f, 0.45f));
		p27.addOrReplaceChild("crystal2_flat", CubeListBuilder.create()
				.texOffs(232, 157).addBox(-1.0f, -4.0f, -1.0f, 2.0f, 4.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(2.0f, -3.0f, 8.0f, 0.5f, 0.0f, 0.1f));
		p27.addOrReplaceChild("crystal3_flat", CubeListBuilder.create()
				.texOffs(220, 157).addBox(-1.5f, -6.0f, -1.5f, 3.0f, 6.0f, 3.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(-8.0f, -3.0f, 7.0f, 0.4f, 0.0f, -0.5f));
		p27.addOrReplaceChild("crystal4_flat", CubeListBuilder.create()
				.texOffs(232, 157).addBox(-1.0f, -4.0f, -1.0f, 2.0f, 4.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(11.0f, -3.0f, 6.0f, 0.2f, 0.0f, 0.7f));
		p27.addOrReplaceChild("crystal5_flat", CubeListBuilder.create()
				.texOffs(208, 157).addBox(-1.5f, -9.0f, -1.5f, 3.0f, 9.0f, 3.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(1.0f, -5.0f, -1.0f, -0.05f, 0.0f, 0.05f));
		p2.addOrReplaceChild("cape_cape", CubeListBuilder.create()
				.texOffs(0, 195).addBox(-8.0f, 0.0f, 0.0f, 16.0f, 14.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, -9.54f, 6.5f, 0.12f, 0.0f, 0.0f));
		p2.addOrReplaceChild("collar", CubeListBuilder.create()
				.texOffs(34, 195).addBox(-7.0f, -14.5f, -7.0f, 14.0f, 2.0f, 14.0f, new CubeDeformation(0.0f)),
				PartPose.ZERO);
		p2.addOrReplaceChild("amulet", CubeListBuilder.create()
				.texOffs(90, 195).addBox(-2.0f, 0.0f, 0.0f, 4.0f, 5.0f, 1.0f, new CubeDeformation(0.0f))
				.texOffs(100, 195).addBox(-1.0f, 1.0f, -0.6f, 2.0f, 3.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, -12.5f, -7.8f, 0.1f, 0.0f, 0.0f));
		p2.addOrReplaceChild("armor_c", CubeListBuilder.create()
				.texOffs(106, 195).addBox(-5.0f, -11.56f, -7.0f, 10.0f, 7.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.ZERO);
		p2.addOrReplaceChild("r_shoulder", CubeListBuilder.create()
				.texOffs(128, 195).addBox(-4.0f, 0.0f, -4.0f, 4.0f, 3.0f, 8.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(-5.5f, -10.54f, 0.0f, 0.0f, 0.0f, 0.35f));
		p2.addOrReplaceChild("l_shoulder", CubeListBuilder.create()
				.texOffs(152, 195).addBox(0.0f, 0.0f, -4.0f, 4.0f, 3.0f, 8.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(5.5f, -10.54f, 0.0f, 0.0f, 0.0f, -0.35f));
		return LayerDefinition.create(mesh, 256, 256);
	}

	@Override
	public void setupAnim(CompanionRenderState state) {
		super.setupAnim(state);
		// Aussehen (v0.25.4): gewählte Körperteil-Varianten; Ausrüstung nur im eigenen Durchgang
		cap.visible = state.part1 == 0;
		capWitch.visible = state.part1 == 1;
		capFlat.visible = state.part1 == 2;
		pack.visible = state.part2 == 0;
		strap0.visible = state.part2 == 0;
		strap1.visible = state.part2 == 0;
		capeCape.visible = state.part2 == 1;
		collar.visible = pass == 0;
		amulet.visible = pass == 1;
		armorC.visible = pass == 2;
		rShoulder.visible = pass == 2;
		lShoulder.visible = pass == 2;

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
				body.yRot += Mth.sin(p * Mth.TWO_PI) * 0.9f * k;          // dreht sich im Tanz
			}
		}

		// Watscheln: kurze Schritte, der Stiel schaukelt, der Hut wippt nach
		float swing = Mth.cos(pos * 1.0f) * 0.8f * speed;
		rLeg.xRot += swing;
		lLeg.xRot -= swing;
		body.zRot += Mth.sin(pos * 0.5f) * 0.12f * speed;
		body.y += -Mth.abs(Mth.sin(pos * 1.0f)) * 1.0f * speed;
		cap.zRot -= Mth.sin(pos * 0.5f - 0.6f) * 0.08f * speed;
		rArm.xRot -= swing * 0.6f;
		lArm.xRot += swing * 0.6f;
		rArm.zRot += Mth.sin(age * 0.07f) * 0.05f;
		lArm.zRot -= Mth.sin(age * 0.07f) * 0.05f;
		pack.xRot += Math.abs(Mth.sin(pos * 0.6f)) * 0.08f * speed;          // Rucksack wippt beim Laufen
		potion.zRot += Mth.sin(age * 0.11f) * 0.06f + Mth.sin(pos * 0.6f) * 0.1f * speed;

		// Ruhig atmen: der Hut hebt und senkt sich
		cap.y += Mth.sin(age * 0.08f) * 0.35f;
		body.xRot += Mth.sin(age * 0.05f) * 0.02f;
		float yaw = Mth.clamp(state.yRot, -40.0f, 40.0f) * Mth.DEG_TO_RAD;
		body.yRot += yaw * 0.3f;
		cap.yRot += yaw * 0.2f;

		// Kopfnuss: mit dem Hut voran
		if (state.attack > 0.0f) {
			float a = Mth.sin(state.attack * Mth.PI);
			body.xRot += 0.45f * a;
			cap.xRot += 0.25f * a;
			body.z -= 1.5f * a;
		}

		// Heilen: Arme hoch, hüpft vor Freude, der Hut leuchtet (Partikel)
		if (state.action > 0.0f) {
			float h = Mth.sin(state.action * Mth.PI);
			poseHeal(h);
			body.y -= Mth.abs(Mth.sin(state.action * Mth.PI * 3.0f)) * 2.0f * h;
			cap.xRot -= 0.1f * h;
		}
		// Körperteil-Varianten bewegen sich wie das Original
		CompanionLookModel.follow(cap, capWitch);
		CompanionLookModel.follow(cap, capFlat);
	}

	private void poseSit(float k) {
		rLeg.xRot += k * -1.450f;
		lLeg.xRot += k * -1.450f;
		body.y += k * 4.500f;
	}

	private void poseTired(float k) {
		cap.xRot += k * 0.300f;
		cap.zRot += k * 0.150f;
		rArm.zRot += k * 0.400f;
		lArm.zRot += k * -0.400f;
	}

	private void poseHeal(float k) {
		rArm.xRot += k * -2.400f;
		rArm.zRot += k * 0.300f;
		lArm.xRot += k * -2.400f;
		lArm.zRot += k * -0.300f;
		cap.xRot += k * -0.150f;
	}

	private void poseJoy(float k) {
		body.y += k * -6.000f;
		rArm.xRot += k * -1.600f;
		rArm.zRot += k * 0.500f;
		lArm.xRot += k * -2.400f;
		lArm.zRot += k * -0.500f;
		rLeg.xRot += k * 0.500f;
		lLeg.xRot += k * -0.500f;
		cap.y += k * -1.500f;
	}

	private void poseNuzzle(float k) {
		body.zRot += k * 0.220f;
		body.xRot += k * 0.150f;
		cap.zRot += k * 0.180f;
		rArm.xRot += k * -1.300f;
		rArm.zRot += k * -0.600f;
		lArm.xRot += k * -1.300f;
		lArm.zRot += k * 0.600f;
	}

	private void poseGesture(float k) {
		body.yRot += k * 0.700f;
		body.zRot += k * -0.150f;
		rArm.zRot += k * 1.400f;
		lArm.zRot += k * -1.400f;
		lArm.xRot += k * -0.400f;
		rLeg.xRot += k * -0.900f;
		cap.zRot += k * 0.200f;
	}

	/** Ausrüstungs-Durchgang: -1 = Grundmodell (Ausrüstung unsichtbar), 0–2 = nur dieser Platz wird gezeichnet. */
	private int pass = -1;

	@Override
	public void equipmentPass(int slot) {
		pass = slot;
		java.util.List<java.util.List<ModelPart>> slots = java.util.List.of(java.util.List.of(collar), java.util.List.of(amulet), java.util.List.of(armorC, rShoulder, lShoulder));

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
