package dev.bartels.jtechstorage.client.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.model.ArmedModel;
import net.minecraft.world.entity.HumanoidArm;
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
 * Wächter-Falter: samtiger Nachtfalter mit Pelzkragen, Facettenaugen, gefiederten Fühlern, Kristall-Laterne am
 * Hinterleib und Augenflecken auf den Vorderflügeln. Schläft tagsüber gelandet mit hochgeklappten Flügeln.
 * Modell in doppelter Detaildichte (halbe Pixel) – der Renderer skaliert es auf 0.5.
 * Erzeugt von scratchpad/moth2.py (Teile und Posen), Animation von Hand.
 */
public class CollectorMothModel extends EntityModel<CollectorMothRenderState> implements ArmedModel<CollectorMothRenderState>, HelperHatModel {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(JTechStorage.id("collector_moth"), "main");

	private final ModelPart moth;
	private final ModelPart body;
	private final ModelPart head;
	private final ModelPart lids;
	private final ModelPart rAnt;
	private final ModelPart lAnt;
	private final ModelPart rWing;
	private final ModelPart lWing;
	private final ModelPart rWingLo;
	private final ModelPart lWingLo;
	private final ModelPart abdomen;
	private final ModelPart abd2;
	private final ModelPart abd3;
	private final ModelPart rLeg0;
	private final ModelPart rLeg1;
	private final ModelPart rLeg2;
	private final ModelPart lLeg0;
	private final ModelPart lLeg1;
	private final ModelPart lLeg2;
	private final ModelPart tongue;

	public CollectorMothModel(ModelPart root) {
		super(root);
		this.moth = root.getChild("moth");
		this.body = root.getChild("moth").getChild("body");
		this.head = root.getChild("moth").getChild("body").getChild("head");
		this.lids = root.getChild("moth").getChild("body").getChild("head").getChild("lids");
		this.rAnt = root.getChild("moth").getChild("body").getChild("head").getChild("r_ant");
		this.lAnt = root.getChild("moth").getChild("body").getChild("head").getChild("l_ant");
		this.rWing = root.getChild("moth").getChild("body").getChild("r_wing");
		this.lWing = root.getChild("moth").getChild("body").getChild("l_wing");
		this.rWingLo = root.getChild("moth").getChild("body").getChild("r_wing_lo");
		this.lWingLo = root.getChild("moth").getChild("body").getChild("l_wing_lo");
		this.abdomen = root.getChild("moth").getChild("body").getChild("abdomen");
		this.abd2 = root.getChild("moth").getChild("body").getChild("abdomen").getChild("abd2");
		this.abd3 = root.getChild("moth").getChild("body").getChild("abdomen").getChild("abd2").getChild("abd3");
		this.rLeg0 = root.getChild("moth").getChild("body").getChild("r_leg0");
		this.rLeg1 = root.getChild("moth").getChild("body").getChild("r_leg1");
		this.rLeg2 = root.getChild("moth").getChild("body").getChild("r_leg2");
		this.lLeg0 = root.getChild("moth").getChild("body").getChild("l_leg0");
		this.lLeg1 = root.getChild("moth").getChild("body").getChild("l_leg1");
		this.lLeg2 = root.getChild("moth").getChild("body").getChild("l_leg2");
		this.tongue = root.getChild("moth").getChild("body").getChild("head").getChild("tongue");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		PartDefinition p1 = root.addOrReplaceChild("moth", CubeListBuilder.create(),
				PartPose.offset(0.0f, 13.0f, 0.0f));
		PartDefinition p2 = p1.addOrReplaceChild("body", CubeListBuilder.create()
				.texOffs(0, 0).addBox(-4.0f, -3.5f, -4.0f, 8.0f, 7.0f, 8.0f, new CubeDeformation(0.0f))
				.texOffs(26, 15).addBox(-5.0f, -4.4f, -5.0f, 10.0f, 4.0f, 4.0f, new CubeDeformation(0.2f)),
				PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f, 0.1f, 0.0f, 0.0f));
		PartDefinition p3 = p2.addOrReplaceChild("head", CubeListBuilder.create()
				.texOffs(176, 0).addBox(-3.0f, -3.0f, -5.0f, 6.0f, 5.0f, 5.0f, new CubeDeformation(0.0f))
				.texOffs(70, 15).addBox(-4.4f, -3.2f, -4.6f, 3.0f, 3.0f, 3.0f, new CubeDeformation(0.0f))
				.texOffs(70, 15).addBox(1.4f, -3.2f, -4.6f, 3.0f, 3.0f, 3.0f, new CubeDeformation(0.0f)),
				PartPose.offset(0.0f, -0.5f, -3.6f));
		p3.addOrReplaceChild("lids", CubeListBuilder.create()
				.texOffs(118, 15).addBox(-4.6f, -3.4f, -4.9f, 3.0f, 3.0f, 1.0f, new CubeDeformation(0.0f))
				.texOffs(118, 15).addBox(1.6f, -3.4f, -4.9f, 3.0f, 3.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.ZERO);
		p3.addOrReplaceChild("r_ant", CubeListBuilder.create()
				.texOffs(220, 0).addBox(-0.5f, -0.5f, -9.0f, 1.0f, 1.0f, 9.0f, new CubeDeformation(0.0f))
				.texOffs(0, 15).addBox(-2.0f, 0.0f, -9.0f, 4.0f, 0.0f, 9.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(-1.4f, -3.0f, -4.2f, -0.75f, -0.35f, 0.0f));
		p3.addOrReplaceChild("l_ant", CubeListBuilder.create()
				.texOffs(220, 0).addBox(-0.5f, -0.5f, -9.0f, 1.0f, 1.0f, 9.0f, new CubeDeformation(0.0f))
				.texOffs(0, 15).mirror().addBox(-2.0f, 0.0f, -9.0f, 4.0f, 0.0f, 9.0f, new CubeDeformation(0.0f)).mirror(false),
				PartPose.offsetAndRotation(1.4f, -3.0f, -4.2f, -0.75f, 0.35f, 0.0f));
		p3.addOrReplaceChild("tongue", CubeListBuilder.create()
				.texOffs(126, 15).addBox(-0.5f, 0.0f, -0.5f, 1.0f, 3.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, 1.6f, -4.4f, 0.4f, 0.0f, 0.0f));
		p2.addOrReplaceChild("r_wing", CubeListBuilder.create()
				.texOffs(32, 0).addBox(-20.0f, 0.0f, -5.0f, 20.0f, 0.0f, 14.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(-3.6f, -3.2f, -1.5f, 0.0f, -0.1f, 0.0f));
		p2.addOrReplaceChild("l_wing", CubeListBuilder.create()
				.texOffs(32, 0).mirror().addBox(0.0f, 0.0f, -5.0f, 20.0f, 0.0f, 14.0f, new CubeDeformation(0.0f)).mirror(false),
				PartPose.offsetAndRotation(3.6f, -3.2f, -1.5f, 0.0f, 0.1f, 0.0f));
		p2.addOrReplaceChild("r_wing_lo", CubeListBuilder.create()
				.texOffs(126, 0).addBox(-14.0f, 0.0f, -2.0f, 14.0f, 0.0f, 11.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(-3.4f, -2.6f, 2.5f, 0.0f, 0.3f, 0.0f));
		p2.addOrReplaceChild("l_wing_lo", CubeListBuilder.create()
				.texOffs(126, 0).mirror().addBox(0.0f, 0.0f, -2.0f, 14.0f, 0.0f, 11.0f, new CubeDeformation(0.0f)).mirror(false),
				PartPose.offsetAndRotation(3.4f, -2.6f, 2.5f, 0.0f, -0.3f, 0.0f));
		PartDefinition p12 = p2.addOrReplaceChild("r_leg0", CubeListBuilder.create()
				.texOffs(110, 15).addBox(-0.5f, 0.0f, -0.5f, 1.0f, 4.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(-3.2f, 2.5f, -2.5f, 0.0f, 0.0f, 0.9f));
		p12.addOrReplaceChild("r_shin0", CubeListBuilder.create()
				.texOffs(114, 15).addBox(-0.5f, 0.0f, -0.5f, 1.0f, 4.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, 3.6f, 0.0f, 0.0f, 0.0f, -1.3f));
		PartDefinition p14 = p2.addOrReplaceChild("r_leg1", CubeListBuilder.create()
				.texOffs(110, 15).addBox(-0.5f, 0.0f, -0.5f, 1.0f, 4.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(-3.2f, 2.5f, 0.0f, 0.0f, 0.0f, 0.9f));
		p14.addOrReplaceChild("r_shin1", CubeListBuilder.create()
				.texOffs(114, 15).addBox(-0.5f, 0.0f, -0.5f, 1.0f, 4.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, 3.6f, 0.0f, 0.0f, 0.0f, -1.3f));
		PartDefinition p16 = p2.addOrReplaceChild("r_leg2", CubeListBuilder.create()
				.texOffs(110, 15).addBox(-0.5f, 0.0f, -0.5f, 1.0f, 4.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(-3.2f, 2.5f, 2.5f, 0.0f, 0.0f, 0.9f));
		p16.addOrReplaceChild("r_shin2", CubeListBuilder.create()
				.texOffs(114, 15).addBox(-0.5f, 0.0f, -0.5f, 1.0f, 4.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, 3.6f, 0.0f, 0.0f, 0.0f, -1.3f));
		PartDefinition p18 = p2.addOrReplaceChild("l_leg0", CubeListBuilder.create()
				.texOffs(110, 15).addBox(-0.5f, 0.0f, -0.5f, 1.0f, 4.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(3.2f, 2.5f, -2.5f, 0.0f, 0.0f, -0.9f));
		p18.addOrReplaceChild("l_shin0", CubeListBuilder.create()
				.texOffs(114, 15).addBox(-0.5f, 0.0f, -0.5f, 1.0f, 4.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, 3.6f, 0.0f, 0.0f, 0.0f, 1.3f));
		PartDefinition p20 = p2.addOrReplaceChild("l_leg1", CubeListBuilder.create()
				.texOffs(110, 15).addBox(-0.5f, 0.0f, -0.5f, 1.0f, 4.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(3.2f, 2.5f, 0.0f, 0.0f, 0.0f, -0.9f));
		p20.addOrReplaceChild("l_shin1", CubeListBuilder.create()
				.texOffs(114, 15).addBox(-0.5f, 0.0f, -0.5f, 1.0f, 4.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, 3.6f, 0.0f, 0.0f, 0.0f, 1.3f));
		PartDefinition p22 = p2.addOrReplaceChild("l_leg2", CubeListBuilder.create()
				.texOffs(110, 15).addBox(-0.5f, 0.0f, -0.5f, 1.0f, 4.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(3.2f, 2.5f, 2.5f, 0.0f, 0.0f, -0.9f));
		p22.addOrReplaceChild("l_shin2", CubeListBuilder.create()
				.texOffs(114, 15).addBox(-0.5f, 0.0f, -0.5f, 1.0f, 4.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, 3.6f, 0.0f, 0.0f, 0.0f, 1.3f));
		PartDefinition p24 = p2.addOrReplaceChild("abdomen", CubeListBuilder.create()
				.texOffs(100, 0).addBox(-3.5f, -3.0f, 0.0f, 7.0f, 6.0f, 6.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, 0.5f, 3.6f, -0.3f, 0.0f, 0.0f));
		PartDefinition p25 = p24.addOrReplaceChild("abd2", CubeListBuilder.create()
				.texOffs(198, 0).addBox(-3.0f, -2.5f, 0.0f, 6.0f, 5.0f, 5.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, 0.2f, 5.6f, -0.15f, 0.0f, 0.0f));
		PartDefinition p26 = p25.addOrReplaceChild("abd3", CubeListBuilder.create()
				.texOffs(54, 15).addBox(-2.0f, -2.0f, 0.0f, 4.0f, 4.0f, 4.0f, new CubeDeformation(0.0f))
				.texOffs(94, 15).addBox(-2.0f, -2.6f, 3.4f, 4.0f, 1.0f, 4.0f, new CubeDeformation(0.1f)),
				PartPose.offsetAndRotation(0.0f, 0.2f, 4.6f, -0.15f, 0.0f, 0.0f));
		p26.addOrReplaceChild("lantern", CubeListBuilder.create()
				.texOffs(82, 15).addBox(-1.5f, -1.5f, 0.0f, 3.0f, 3.0f, 3.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, 0.0f, 4.2f, 0.785f, 0.785f, 0.0f));
		return LayerDefinition.create(mesh, 256, 128);
	}

	@Override
	public void setupAnim(CollectorMothRenderState state) {
		super.setupAnim(state);
		float age = state.ageInTicks;
		lids.visible = state.resting;

		if (state.resting) {
			poseSleep(1.0f);
			float breath = Mth.sin(age * 0.06f);
			abdomen.xRot += breath * 0.05f;
			rWing.zRot += breath * 0.02f;
			lWing.zRot -= breath * 0.02f;
			rAnt.yRot += Mth.sin(age * 0.04f) * 0.05f;
			lAnt.yRot -= Mth.sin(age * 0.04f) * 0.05f;
			return;
		}

		// Flattern: Mischung aus Flügel-oben- und Flügel-unten-Pose
		float flap = (Mth.sin(age * 0.9f) + 1.0f) * 0.5f;
		poseFlapUp(flap);
		poseFlapDown(1.0f - flap);
		moth.y += Mth.sin(age * 0.15f) * 1.8f + (flap - 0.5f) * 0.8f;
		head.yRot += state.yRot * Mth.DEG_TO_RAD * 0.6f;
		head.xRot += state.xRot * Mth.DEG_TO_RAD * 0.5f;
		rAnt.xRot += Mth.sin(age * 0.2f) * 0.1f;
		lAnt.xRot += Mth.sin(age * 0.2f + 1.0f) * 0.1f;
		abdomen.xRot += Mth.sin(age * 0.12f) * 0.06f;
		abd3.xRot += Mth.sin(age * 0.12f - 0.6f) * 0.08f;
		float dangle = Mth.sin(age * 0.2f) * 0.06f;
		rLeg0.zRot += dangle;
		rLeg1.zRot += dangle;
		rLeg2.zRot += dangle;
		lLeg0.zRot -= dangle;
		lLeg1.zRot -= dangle;
		lLeg2.zRot -= dangle;

		if (state.carrying) {
			poseCarry(1.0f);
		}

		if (state.action > 0.0f) {
			poseAlarm(state.action);
		}
	}

	private void poseFlapUp(float k) {
		rWing.zRot += k * 0.700f;
		lWing.zRot += k * -0.700f;
		rWingLo.zRot += k * 0.500f;
		lWingLo.zRot += k * -0.500f;
	}

	private void poseFlapDown(float k) {
		rWing.zRot += k * -0.450f;
		lWing.zRot += k * 0.450f;
		rWingLo.zRot += k * -0.300f;
		lWingLo.zRot += k * 0.300f;
	}

	private void poseCarry(float k) {
		body.xRot += k * 0.250f;
		rLeg0.xRot += k * -0.800f;
		lLeg0.xRot += k * -0.800f;
		rLeg1.xRot += k * -0.500f;
		lLeg1.xRot += k * -0.500f;
	}

	private void poseAlarm(float k) {
		rAnt.xRot += k * -0.500f;
		rAnt.yRot += k * -0.300f;
		lAnt.xRot += k * -0.500f;
		lAnt.yRot += k * 0.300f;
		abdomen.xRot += k * -0.300f;
	}

	private void poseSleep(float k) {
		moth.y += k * 2.100f;
		body.xRot += k * -0.100f;
		rWing.zRot += k * 0.420f;
		rWing.yRot += k * 1.050f;
		lWing.zRot += k * -0.420f;
		lWing.yRot += k * -1.050f;
		rWingLo.zRot += k * 0.300f;
		rWingLo.yRot += k * 0.950f;
		lWingLo.zRot += k * -0.300f;
		lWingLo.yRot += k * -0.950f;
		rAnt.xRot += k * 1.000f;
		rAnt.yRot += k * 0.500f;
		lAnt.xRot += k * 1.000f;
		lAnt.yRot += k * -0.500f;
		abdomen.xRot += k * 0.300f;
		abd2.xRot += k * 0.150f;
		abd3.xRot += k * 0.150f;
		head.xRot += k * 0.250f;
		tongue.xRot += k * -0.400f;
		rLeg0.zRot += k * -0.300f;
		rLeg1.zRot += k * -0.300f;
		rLeg2.zRot += k * -0.300f;
		lLeg0.zRot += k * 0.300f;
		lLeg1.zRot += k * 0.300f;
		lLeg2.zRot += k * 0.300f;
	}

	@Override
	public void translateToHand(CollectorMothRenderState state, HumanoidArm arm, PoseStack pose) {
		root().translateAndRotate(pose);
		moth.translateAndRotate(pose);
		body.translateAndRotate(pose);
		pose.translate(0.0f, 0.35f, -0.05f);
		pose.scale(1.2f, 1.2f, 1.2f);
	}

	@Override
	public void translateToHat(PoseStack pose) {
		root().translateAndRotate(pose);
		moth.translateAndRotate(pose);
		body.translateAndRotate(pose);
		head.translateAndRotate(pose);
		pose.translate(0.0f, 2.0f / 16.0f, -2.5f / 16.0f);
	}

	@Override
	public float hatScale() {
		return 0.75f;
	}
}
