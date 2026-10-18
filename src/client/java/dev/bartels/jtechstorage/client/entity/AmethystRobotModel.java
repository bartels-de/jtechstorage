package dev.bartels.jtechstorage.client.entity;

import dev.bartels.jtechstorage.entity.AmethystRobot;
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
 * Amethyst-Roboter: schwerer Kristall-Mech mit Stahlpanzer, Brust-Reaktor hinter Gitter, Kopf mit Augenschlitz und
 * Kristallhörnern, Laserkanone rechts, Kristall-Panzerfaust links, Pylone, Triebwerke, Energiering und Umhang.
 * Ohne Ziel fährt er herunter (Ruhemodus, Augenblende zu).
 * Modell in doppelter Detaildichte (halbe Pixel) – der Renderer skaliert es auf 0.5.
 * Erzeugt von scratchpad/robot2.py (Teile und Posen), Animation von Hand.
 */
public class AmethystRobotModel extends EntityModel<AmethystRobotRenderState> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(JTechStorage.id("amethyst_robot"), "main");

	private final ModelPart body;
	private final ModelPart head;
	private final ModelPart shutter;
	private final ModelPart crest;
	private final ModelPart rightArm;
	private final ModelPart leftArm;
	private final ModelPart cannon;
	private final ModelPart gauntlet;
	private final ModelPart thrusterL;
	private final ModelPart thrusterR;
	private final ModelPart ring;
	private final ModelPart cape;
	private final ModelPart pylonL;
	private final ModelPart pylonR;
	private final ModelPart rightLeg;
	private final ModelPart leftLeg;
	private final ModelPart rightShin;
	private final ModelPart leftShin;

	public AmethystRobotModel(ModelPart root) {
		super(root);
		this.body = root.getChild("body");
		this.head = root.getChild("body").getChild("head");
		this.shutter = root.getChild("body").getChild("head").getChild("shutter");
		this.crest = root.getChild("body").getChild("head").getChild("crest");
		this.rightArm = root.getChild("body").getChild("right_arm");
		this.leftArm = root.getChild("body").getChild("left_arm");
		this.cannon = root.getChild("body").getChild("right_arm").getChild("cannon");
		this.gauntlet = root.getChild("body").getChild("left_arm").getChild("gauntlet");
		this.thrusterL = root.getChild("body").getChild("thruster_l");
		this.thrusterR = root.getChild("body").getChild("thruster_r");
		this.ring = root.getChild("body").getChild("ring");
		this.cape = root.getChild("body").getChild("cape");
		this.pylonL = root.getChild("body").getChild("pylon_l");
		this.pylonR = root.getChild("body").getChild("pylon_r");
		this.rightLeg = root.getChild("right_leg");
		this.leftLeg = root.getChild("left_leg");
		this.rightShin = root.getChild("right_leg").getChild("right_shin");
		this.leftShin = root.getChild("left_leg").getChild("left_shin");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		PartDefinition p1 = root.addOrReplaceChild("body", CubeListBuilder.create()
				.texOffs(88, 0).addBox(-18.0f, -30.0f, -11.0f, 36.0f, 20.0f, 22.0f, new CubeDeformation(0.0f))
				.texOffs(64, 44).addBox(-12.0f, -10.0f, -8.0f, 24.0f, 10.0f, 16.0f, new CubeDeformation(0.0f))
				.texOffs(404, 44).addBox(-14.0f, -4.0f, -9.0f, 28.0f, 4.0f, 18.0f, new CubeDeformation(0.0f))
				.texOffs(68, 74).addBox(-10.0f, -27.0f, -12.0f, 20.0f, 20.0f, 2.0f, new CubeDeformation(0.0f))
				.texOffs(332, 74).addBox(-8.0f, -25.0f, -13.4f, 16.0f, 16.0f, 1.0f, new CubeDeformation(0.0f))
				.texOffs(382, 74).addBox(-7.0f, -24.0f, -12.6f, 14.0f, 14.0f, 1.0f, new CubeDeformation(0.0f))
				.texOffs(176, 44).addBox(-12.0f, -34.0f, -9.0f, 24.0f, 6.0f, 18.0f, new CubeDeformation(0.0f))
				.texOffs(144, 44).addBox(-19.5f, -27.0f, -7.0f, 2.0f, 12.0f, 14.0f, new CubeDeformation(0.0f))
				.texOffs(144, 44).mirror().addBox(17.5f, -27.0f, -7.0f, 2.0f, 12.0f, 14.0f, new CubeDeformation(0.0f)).mirror(false)
				.texOffs(180, 74).addBox(-6.0f, -26.0f, 11.0f, 12.0f, 14.0f, 6.0f, new CubeDeformation(0.0f)),
				PartPose.offset(0.0f, -2.0f, 0.0f));
		PartDefinition p2 = p1.addOrReplaceChild("head", CubeListBuilder.create()
				.texOffs(0, 44).addBox(-8.0f, -14.0f, -9.0f, 16.0f, 14.0f, 16.0f, new CubeDeformation(0.0f))
				.texOffs(196, 96).addBox(-6.0f, -10.0f, -10.0f, 12.0f, 2.0f, 2.0f, new CubeDeformation(0.0f))
				.texOffs(112, 96).addBox(-6.0f, -6.0f, -10.4f, 12.0f, 6.0f, 2.0f, new CubeDeformation(0.0f))
				.texOffs(0, 96).addBox(-10.0f, -8.0f, -5.0f, 2.0f, 6.0f, 6.0f, new CubeDeformation(0.0f))
				.texOffs(0, 96).mirror().addBox(8.0f, -8.0f, -5.0f, 2.0f, 6.0f, 6.0f, new CubeDeformation(0.0f)).mirror(false),
				PartPose.offset(0.0f, -30.0f, -2.0f));
		p2.addOrReplaceChild("shutter", CubeListBuilder.create()
				.texOffs(224, 96).addBox(-6.0f, -10.5f, -10.6f, 12.0f, 3.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.ZERO);
		PartDefinition p4 = p2.addOrReplaceChild("horn_r", CubeListBuilder.create()
				.texOffs(316, 74).addBox(-2.0f, -14.0f, -2.0f, 4.0f, 14.0f, 4.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(-6.4f, -11.0f, 0.0f, -0.35f, 0.0f, -0.45f));
		p4.addOrReplaceChild("horn_r_tip", CubeListBuilder.create()
				.texOffs(80, 96).addBox(-1.0f, -8.0f, -1.0f, 2.0f, 8.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, -13.0f, 0.0f, -0.35f, 0.0f, -0.2f));
		PartDefinition p6 = p2.addOrReplaceChild("horn_l", CubeListBuilder.create()
				.texOffs(316, 74).addBox(-2.0f, -14.0f, -2.0f, 4.0f, 14.0f, 4.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(6.4f, -11.0f, 0.0f, -0.35f, 0.0f, 0.45f));
		p6.addOrReplaceChild("horn_l_tip", CubeListBuilder.create()
				.texOffs(80, 96).addBox(-1.0f, -8.0f, -1.0f, 2.0f, 8.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, -13.0f, 0.0f, -0.35f, 0.0f, 0.2f));
		p2.addOrReplaceChild("crest", CubeListBuilder.create()
				.texOffs(292, 74).addBox(-2.0f, -10.0f, -4.0f, 4.0f, 10.0f, 8.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, -13.0f, 1.0f, -0.3f, 0.0f, 0.0f));
		PartDefinition p9 = p1.addOrReplaceChild("pylon_r", CubeListBuilder.create()
				.texOffs(322, 0).addBox(-3.0f, -28.0f, -3.0f, 6.0f, 28.0f, 6.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(-10.0f, -20.0f, 11.0f, -0.3f, 0.0f, -0.35f));
		p9.addOrReplaceChild("pylon_r_tip", CubeListBuilder.create()
				.texOffs(488, 74).addBox(-2.0f, -10.0f, -2.0f, 4.0f, 10.0f, 4.0f, new CubeDeformation(0.0f)),
				PartPose.offset(0.0f, -28.0f, 0.0f));
		PartDefinition p11 = p1.addOrReplaceChild("pylon_l", CubeListBuilder.create()
				.texOffs(322, 0).addBox(-3.0f, -28.0f, -3.0f, 6.0f, 28.0f, 6.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(10.0f, -20.0f, 11.0f, -0.3f, 0.0f, 0.35f));
		p11.addOrReplaceChild("pylon_l_tip", CubeListBuilder.create()
				.texOffs(488, 74).addBox(-2.0f, -10.0f, -2.0f, 4.0f, 10.0f, 4.0f, new CubeDeformation(0.0f)),
				PartPose.offset(0.0f, -28.0f, 0.0f));
		p1.addOrReplaceChild("ring", CubeListBuilder.create()
				.texOffs(0, 0).addBox(-22.0f, -22.0f, 0.0f, 44.0f, 44.0f, 0.0f, new CubeDeformation(0.0f)),
				PartPose.offset(0.0f, -18.0f, 18.0f));
		p1.addOrReplaceChild("cape", CubeListBuilder.create()
				.texOffs(204, 0).addBox(-14.0f, 0.0f, 0.0f, 28.0f, 36.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, -30.0f, 11.6f, 0.12f, 0.0f, 0.0f));
		p1.addOrReplaceChild("thruster_l", CubeListBuilder.create()
				.texOffs(264, 74).addBox(-4.0f, 0.0f, 0.0f, 8.0f, 12.0f, 6.0f, new CubeDeformation(0.0f))
				.texOffs(40, 96).addBox(-3.0f, 12.0f, 0.0f, 6.0f, 4.0f, 6.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(9.0f, -16.0f, 11.0f, 0.3f, 0.0f, 0.0f));
		p1.addOrReplaceChild("thruster_r", CubeListBuilder.create()
				.texOffs(264, 74).addBox(-4.0f, 0.0f, 0.0f, 8.0f, 12.0f, 6.0f, new CubeDeformation(0.0f))
				.texOffs(40, 96).addBox(-3.0f, 12.0f, 0.0f, 6.0f, 4.0f, 6.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(-9.0f, -16.0f, 11.0f, 0.3f, 0.0f, 0.0f));
		p1.addOrReplaceChild("tasset_r", CubeListBuilder.create()
				.texOffs(16, 96).addBox(-5.0f, 0.0f, -1.0f, 10.0f, 9.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(-7.0f, 0.0f, -9.4f, -0.15f, 0.0f, 0.0f));
		p1.addOrReplaceChild("tasset_l", CubeListBuilder.create()
				.texOffs(16, 96).mirror().addBox(-5.0f, 0.0f, -1.0f, 10.0f, 9.0f, 2.0f, new CubeDeformation(0.0f)).mirror(false),
				PartPose.offsetAndRotation(7.0f, 0.0f, -9.4f, -0.15f, 0.0f, 0.0f));
		PartDefinition p19 = p1.addOrReplaceChild("right_arm", CubeListBuilder.create()
				.texOffs(346, 0).addBox(-9.0f, -8.0f, -10.0f, 18.0f, 12.0f, 20.0f, new CubeDeformation(0.0f))
				.texOffs(364, 44).addBox(-5.0f, 4.0f, -5.0f, 10.0f, 14.0f, 10.0f, new CubeDeformation(0.0f))
				.texOffs(216, 74).addBox(-6.0f, 16.0f, -6.0f, 12.0f, 6.0f, 12.0f, new CubeDeformation(0.0f))
				.texOffs(96, 96).addBox(-7.0f, 8.0f, 4.0f, 2.0f, 8.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(-24.0f, -26.0f, 0.0f, 0.0f, 0.0f, 0.1f));
		p19.addOrReplaceChild("right_spike_a", CubeListBuilder.create()
				.texOffs(366, 74).addBox(-2.0f, -12.0f, -2.0f, 4.0f, 12.0f, 4.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(-5.0f, -8.0f, -3.0f, -0.2f, 0.0f, -0.5f));
		p19.addOrReplaceChild("right_spike_b", CubeListBuilder.create()
				.texOffs(88, 96).addBox(-1.0f, -8.0f, -1.0f, 2.0f, 8.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(-1.0f, -8.0f, 5.0f, 0.35f, 0.0f, -0.3f));
		p19.addOrReplaceChild("cannon", CubeListBuilder.create()
				.texOffs(262, 0).addBox(-7.0f, 0.0f, -8.0f, 14.0f, 18.0f, 16.0f, new CubeDeformation(0.0f))
				.texOffs(112, 74).addBox(-8.0f, 4.0f, -9.0f, 16.0f, 2.0f, 18.0f, new CubeDeformation(0.0f))
				.texOffs(112, 74).addBox(-8.0f, 10.0f, -9.0f, 16.0f, 2.0f, 18.0f, new CubeDeformation(0.0f))
				.texOffs(456, 74).addBox(-4.0f, 18.0f, -4.0f, 8.0f, 6.0f, 8.0f, new CubeDeformation(0.0f))
				.texOffs(148, 96).addBox(-3.0f, 24.0f, -3.0f, 6.0f, 1.0f, 6.0f, new CubeDeformation(0.0f)),
				PartPose.offset(0.0f, 22.0f, 0.0f));
		PartDefinition p23 = p1.addOrReplaceChild("left_arm", CubeListBuilder.create()
				.texOffs(346, 0).mirror().addBox(-9.0f, -8.0f, -10.0f, 18.0f, 12.0f, 20.0f, new CubeDeformation(0.0f)).mirror(false)
				.texOffs(364, 44).mirror().addBox(-5.0f, 4.0f, -5.0f, 10.0f, 14.0f, 10.0f, new CubeDeformation(0.0f)).mirror(false)
				.texOffs(216, 74).mirror().addBox(-6.0f, 16.0f, -6.0f, 12.0f, 6.0f, 12.0f, new CubeDeformation(0.0f)).mirror(false)
				.texOffs(96, 96).addBox(5.0f, 8.0f, 4.0f, 2.0f, 8.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(24.0f, -26.0f, 0.0f, 0.0f, 0.0f, -0.1f));
		p23.addOrReplaceChild("left_spike_a", CubeListBuilder.create()
				.texOffs(366, 74).addBox(-2.0f, -12.0f, -2.0f, 4.0f, 12.0f, 4.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(5.0f, -8.0f, -3.0f, -0.2f, 0.0f, 0.5f));
		p23.addOrReplaceChild("left_spike_b", CubeListBuilder.create()
				.texOffs(88, 96).addBox(-1.0f, -8.0f, -1.0f, 2.0f, 8.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(1.0f, -8.0f, 5.0f, 0.35f, 0.0f, 0.3f));
		p23.addOrReplaceChild("gauntlet", CubeListBuilder.create()
				.texOffs(422, 0).addBox(-8.0f, 0.0f, -8.0f, 16.0f, 16.0f, 16.0f, new CubeDeformation(0.0f))
				.texOffs(64, 96).addBox(-7.0f, 6.0f, -10.0f, 4.0f, 6.0f, 4.0f, new CubeDeformation(0.0f))
				.texOffs(64, 96).addBox(-2.0f, 6.0f, -10.0f, 4.0f, 6.0f, 4.0f, new CubeDeformation(0.0f))
				.texOffs(64, 96).addBox(3.0f, 6.0f, -10.0f, 4.0f, 6.0f, 4.0f, new CubeDeformation(0.0f))
				.texOffs(188, 96).addBox(-6.0f, 2.0f, -11.0f, 2.0f, 4.0f, 2.0f, new CubeDeformation(0.0f))
				.texOffs(188, 96).addBox(-1.0f, 2.0f, -11.0f, 2.0f, 4.0f, 2.0f, new CubeDeformation(0.0f))
				.texOffs(188, 96).addBox(4.0f, 2.0f, -11.0f, 2.0f, 4.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offset(0.0f, 22.0f, 0.0f));
		PartDefinition p27 = root.addOrReplaceChild("right_leg", CubeListBuilder.create()
				.texOffs(316, 44).addBox(-6.0f, 0.0f, -6.0f, 12.0f, 12.0f, 12.0f, new CubeDeformation(0.0f)),
				PartPose.offset(-10.0f, -2.0f, 0.0f));
		PartDefinition p28 = p27.addOrReplaceChild("right_shin", CubeListBuilder.create()
				.texOffs(412, 74).addBox(-7.0f, -2.0f, -8.4f, 14.0f, 6.0f, 8.0f, new CubeDeformation(0.0f))
				.texOffs(260, 44).addBox(-7.0f, 2.0f, -7.0f, 14.0f, 10.0f, 14.0f, new CubeDeformation(0.0f))
				.texOffs(104, 96).addBox(-8.5f, -1.0f, 3.0f, 2.0f, 8.0f, 2.0f, new CubeDeformation(0.0f))
				.texOffs(0, 74).addBox(-8.0f, 10.0f, -12.0f, 16.0f, 4.0f, 18.0f, new CubeDeformation(0.0f))
				.texOffs(172, 96).addBox(-7.5f, 11.0f, -15.0f, 4.0f, 3.0f, 4.0f, new CubeDeformation(0.0f))
				.texOffs(172, 96).addBox(-2.0f, 11.0f, -15.0f, 4.0f, 3.0f, 4.0f, new CubeDeformation(0.0f))
				.texOffs(172, 96).addBox(3.5f, 11.0f, -15.0f, 4.0f, 3.0f, 4.0f, new CubeDeformation(0.0f)),
				PartPose.offset(0.0f, 12.0f, 0.0f));
		p28.addOrReplaceChild("right_leg_spike", CubeListBuilder.create()
				.texOffs(140, 96).addBox(-1.0f, -6.0f, -1.0f, 2.0f, 6.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, 0.0f, -8.0f, -0.6f, 0.0f, 0.0f));
		PartDefinition p30 = root.addOrReplaceChild("left_leg", CubeListBuilder.create()
				.texOffs(316, 44).mirror().addBox(-6.0f, 0.0f, -6.0f, 12.0f, 12.0f, 12.0f, new CubeDeformation(0.0f)).mirror(false),
				PartPose.offset(10.0f, -2.0f, 0.0f));
		PartDefinition p31 = p30.addOrReplaceChild("left_shin", CubeListBuilder.create()
				.texOffs(412, 74).mirror().addBox(-7.0f, -2.0f, -8.4f, 14.0f, 6.0f, 8.0f, new CubeDeformation(0.0f)).mirror(false)
				.texOffs(260, 44).mirror().addBox(-7.0f, 2.0f, -7.0f, 14.0f, 10.0f, 14.0f, new CubeDeformation(0.0f)).mirror(false)
				.texOffs(104, 96).addBox(6.5f, -1.0f, 3.0f, 2.0f, 8.0f, 2.0f, new CubeDeformation(0.0f))
				.texOffs(0, 74).mirror().addBox(-8.0f, 10.0f, -12.0f, 16.0f, 4.0f, 18.0f, new CubeDeformation(0.0f)).mirror(false)
				.texOffs(172, 96).addBox(-7.5f, 11.0f, -15.0f, 4.0f, 3.0f, 4.0f, new CubeDeformation(0.0f))
				.texOffs(172, 96).addBox(-2.0f, 11.0f, -15.0f, 4.0f, 3.0f, 4.0f, new CubeDeformation(0.0f))
				.texOffs(172, 96).addBox(3.5f, 11.0f, -15.0f, 4.0f, 3.0f, 4.0f, new CubeDeformation(0.0f)),
				PartPose.offset(0.0f, 12.0f, 0.0f));
		p31.addOrReplaceChild("left_leg_spike", CubeListBuilder.create()
				.texOffs(140, 96).addBox(-1.0f, -6.0f, -1.0f, 2.0f, 6.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, 0.0f, -8.0f, -0.6f, 0.0f, 0.0f));
		return LayerDefinition.create(mesh, 512, 256);
	}

	@Override
	public void setupAnim(AmethystRobotRenderState state) {
		super.setupAnim(state);
		float pos = state.walkAnimationPos;
		float speed = Math.min(state.walkAnimationSpeed, 1.0f);
		float age = state.ageInTicks;
		float p = state.progress;
		shutter.visible = state.idle;

		// Energiering dreht sich immer (im Ruhemodus langsam), Umhang weht
		float spin = state.idle ? 0.008f : state.enraged ? 0.12f : 0.05f;
		ring.zRot = age * spin;

		if (state.idle) {
			poseDown(1.0f);
			float hum = Mth.sin(age * 0.04f);
			body.y += hum * 0.3f;
			head.xRot += hum * 0.02f;
			cape.xRot += Mth.sin(age * 0.05f) * 0.03f;
			return;
		}

		head.yRot = Mth.clamp(state.yRot, -50.0f, 50.0f) * Mth.DEG_TO_RAD;
		head.xRot = state.xRot * Mth.DEG_TO_RAD * 0.6f;

		// schwerer Gang: Körper wiegt, Arme pendeln gegengleich, Knie federn
		float step = Mth.cos(pos * 0.5f);
		rightLeg.xRot = step * 0.7f * speed;
		leftLeg.xRot = -step * 0.7f * speed;
		rightShin.xRot += Math.max(0.0f, -step) * 0.5f * speed;
		leftShin.xRot += Math.max(0.0f, step) * 0.5f * speed;
		rightArm.xRot = -step * 0.4f * speed;
		leftArm.xRot = step * 0.4f * speed;
		body.zRot = step * 0.04f * speed;
		body.y += Mth.abs(step) * speed * -1.2f;
		rightArm.zRot += Mth.sin(age * 0.05f) * 0.02f;
		leftArm.zRot -= Mth.sin(age * 0.05f) * 0.02f;
		cape.xRot += speed * 0.55f + Mth.sin(age * 0.08f) * 0.06f + Mth.sin(pos * 0.5f) * 0.08f * speed;
		pylonL.xRot += Mth.sin(age * 0.1f) * 0.03f;
		pylonR.xRot += Mth.sin(age * 0.1f + 1.0f) * 0.03f;

		switch (state.anim) {
			case AmethystRobot.ANIM_SLAM -> {
				// Panzerfaust hoch, dann krachend runter
				leftArm.xRot = p < 0.5f ? -2.8f * (p / 0.5f) : -2.8f + 3.2f * Math.min(1.0f, (p - 0.5f) * 3.0f);
				body.xRot = p < 0.5f ? -0.1f : 0.2f;
				cape.xRot += p < 0.5f ? 0.0f : 0.3f;
			}
			case AmethystRobot.ANIM_SWEEP -> {
				body.yRot = Mth.sin(p * Mth.PI * 2.0f) * 0.8f;
				leftArm.zRot = -1.2f;
				rightArm.zRot = 1.2f;
				cape.xRot += 0.4f;
			}
			case AmethystRobot.ANIM_PULSE -> {
				float spread = Mth.sin(Math.min(1.0f, p * 2.0f) * Mth.HALF_PI);
				leftArm.zRot = -1.3f * spread;
				rightArm.zRot = 1.3f * spread;
				body.xRot = -0.25f * spread;
				head.xRot = -0.4f * spread;
				ring.zRot = age * 0.4f;
				pylonL.zRot = 0.35f + 0.3f * spread;
				pylonR.zRot = -0.35f - 0.3f * spread;
			}
			case AmethystRobot.ANIM_STOMP -> {
				float lift = p < 0.6f ? p / 0.6f : 1.0f - (p - 0.6f) / 0.4f;
				rightLeg.xRot = -1.0f * lift;
				rightShin.xRot += 0.8f * lift;
				body.xRot = 0.1f * lift;
			}
			case AmethystRobot.ANIM_DASH -> {
				body.xRot = 0.45f;
				rightArm.xRot = 0.6f;
				leftArm.xRot = 0.6f;
				thrusterL.xRot = 0.9f;
				thrusterR.xRot = 0.9f;
				cape.xRot = 1.2f + Mth.sin(age * 0.6f) * 0.1f;
			}
			case AmethystRobot.ANIM_LASER -> {
				// Kanone nach vorne, Zittern beim Aufladen, Ring dreht hoch
				rightArm.xRot = -Mth.HALF_PI + head.xRot + Mth.sin(age * 2.0f) * 0.02f * (1.0f - p);
				rightArm.yRot = head.yRot * 0.6f;
				leftArm.xRot = -0.3f;
				ring.zRot = age * (0.1f + p * 0.5f);
			}
			default -> {
			}
		}
	}

	private void poseDown(float k) {
		body.y += k * 1.500f;
		body.xRot += k * 0.120f;
		head.xRot += k * 0.550f;
		rightArm.xRot += k * 0.250f;
		rightArm.zRot += k * -0.120f;
		leftArm.xRot += k * 0.250f;
		leftArm.zRot += k * 0.120f;
		cannon.xRot += k * -0.200f;
		gauntlet.xRot += k * -0.250f;
		pylonL.zRot += k * -0.300f;
		pylonL.xRot += k * 0.250f;
		pylonR.zRot += k * 0.300f;
		pylonR.xRot += k * 0.250f;
		rightLeg.xRot += k * -0.500f;
		rightLeg.y += k * 1.500f;
		leftLeg.xRot += k * -0.500f;
		leftLeg.y += k * 1.500f;
		rightShin.xRot += k * 0.500f;
		leftShin.xRot += k * 0.500f;
		thrusterL.xRot += k * -0.200f;
		thrusterR.xRot += k * -0.200f;
		cape.xRot += k * -0.100f;
	}


}
