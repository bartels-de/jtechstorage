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
 * Kristallhirsch: Reittier mit leuchtendem Kristallgeweih.
 * Modell in doppelter Detaildichte (halbe Pixel) – der Renderer skaliert es auf 0.5.
 * Erzeugt von scratchpad/depthmobs3.py (Teile und Posen), Animation von Hand.
 */
public class CrystalDeerModel extends EntityModel<CrystalDeerRenderState> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(JTechStorage.id("crystal_deer"), "main");

	private final ModelPart deer;
	private final ModelPart body;
	private final ModelPart neck;
	private final ModelPart head;
	private final ModelPart rEar;
	private final ModelPart lEar;
	private final ModelPart tail;
	private final ModelPart saddle;
	private final ModelPart rfLeg;
	private final ModelPart lfLeg;
	private final ModelPart rbLeg;
	private final ModelPart lbLeg;
	private final ModelPart rfLegShin;
	private final ModelPart lfLegShin;
	private final ModelPart rbLegShin;
	private final ModelPart lbLegShin;

	public CrystalDeerModel(ModelPart root) {
		super(root);
		this.deer = root.getChild("deer");
		this.body = root.getChild("deer").getChild("body");
		this.neck = root.getChild("deer").getChild("body").getChild("neck");
		this.head = root.getChild("deer").getChild("body").getChild("neck").getChild("head");
		this.rEar = root.getChild("deer").getChild("body").getChild("neck").getChild("head").getChild("r_ear");
		this.lEar = root.getChild("deer").getChild("body").getChild("neck").getChild("head").getChild("l_ear");
		this.tail = root.getChild("deer").getChild("body").getChild("tail");
		this.saddle = root.getChild("deer").getChild("body").getChild("saddle");
		this.rfLeg = root.getChild("deer").getChild("rf_leg");
		this.lfLeg = root.getChild("deer").getChild("lf_leg");
		this.rbLeg = root.getChild("deer").getChild("rb_leg");
		this.lbLeg = root.getChild("deer").getChild("lb_leg");
		this.rfLegShin = root.getChild("deer").getChild("rf_leg").getChild("rf_leg_shin");
		this.lfLegShin = root.getChild("deer").getChild("lf_leg").getChild("lf_leg_shin");
		this.rbLegShin = root.getChild("deer").getChild("rb_leg").getChild("rb_leg_shin");
		this.lbLegShin = root.getChild("deer").getChild("lb_leg").getChild("lb_leg_shin");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		PartDefinition p1 = root.addOrReplaceChild("deer", CubeListBuilder.create(),
				PartPose.offset(0.0f, 24.0f, 0.0f));
		PartDefinition p2 = p1.addOrReplaceChild("body", CubeListBuilder.create()
				.texOffs(0, 0).addBox(-7.0f, -14.0f, -15.0f, 14.0f, 14.0f, 30.0f, new CubeDeformation(0.0f))
				.texOffs(0, 64).addBox(-6.0f, -13.0f, -17.0f, 12.0f, 12.0f, 6.0f, new CubeDeformation(0.0f))
				.texOffs(0, 44).addBox(-6.5f, -14.5f, 9.0f, 13.0f, 13.0f, 7.0f, new CubeDeformation(0.0f)),
				PartPose.offset(0.0f, -27.0f, 0.0f));
		PartDefinition p3 = p2.addOrReplaceChild("neck", CubeListBuilder.create()
				.texOffs(88, 0).addBox(-3.5f, -16.0f, -3.5f, 7.0f, 16.0f, 7.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, -9.0f, -12.0f, 0.45f, 0.0f, 0.0f));
		PartDefinition p4 = p3.addOrReplaceChild("head", CubeListBuilder.create()
				.texOffs(40, 44).addBox(-4.0f, -7.0f, -9.0f, 8.0f, 8.0f, 11.0f, new CubeDeformation(0.0f))
				.texOffs(0, 82).addBox(-3.0f, -4.5f, -14.5f, 6.0f, 5.0f, 6.0f, new CubeDeformation(0.0f))
				.texOffs(114, 82).addBox(-1.5f, -4.5f, -15.0f, 3.0f, 2.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, -15.0f, 0.0f, -0.45f, 0.0f, 0.0f));
		p4.addOrReplaceChild("r_ear", CubeListBuilder.create()
				.texOffs(94, 82).addBox(-6.0f, -2.0f, 0.0f, 6.0f, 3.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(-3.5f, -6.0f, 0.0f, 0.0f, 0.2f, 0.55f));
		p4.addOrReplaceChild("l_ear", CubeListBuilder.create()
				.texOffs(94, 82).addBox(0.0f, -2.0f, 0.0f, 6.0f, 3.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(3.5f, -6.0f, 0.0f, 0.0f, -0.2f, -0.55f));
		PartDefinition p7 = p4.addOrReplaceChild("r_antler", CubeListBuilder.create()
				.texOffs(108, 64).addBox(-1.0f, -12.0f, -1.0f, 2.0f, 12.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(-2.5f, -7.0f, -1.0f, -0.25f, 0.0f, -0.45f));
		p7.addOrReplaceChild("r_tip", CubeListBuilder.create()
				.texOffs(58, 82).addBox(-1.5f, -5.0f, -1.5f, 3.0f, 5.0f, 3.0f, new CubeDeformation(0.0f)),
				PartPose.offset(0.0f, -12.0f, 0.0f));
		PartDefinition p9 = p7.addOrReplaceChild("r_tine", CubeListBuilder.create()
				.texOffs(50, 82).addBox(-1.0f, -7.0f, -1.0f, 2.0f, 7.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, -5.0f, 0.0f, 0.75f, 0.0f, 0.0f));
		p9.addOrReplaceChild("r_tine_tip", CubeListBuilder.create()
				.texOffs(86, 82).addBox(-1.0f, -4.0f, -1.0f, 2.0f, 4.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offset(0.0f, -7.0f, 0.0f));
		PartDefinition p11 = p7.addOrReplaceChild("r_tine2", CubeListBuilder.create()
				.texOffs(50, 82).addBox(-1.0f, -7.0f, -1.0f, 2.0f, 7.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, -9.0f, 0.0f, -0.2f, 0.0f, -0.85f));
		p11.addOrReplaceChild("r_tine2_tip", CubeListBuilder.create()
				.texOffs(86, 82).addBox(-1.0f, -4.0f, -1.0f, 2.0f, 4.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offset(0.0f, -7.0f, 0.0f));
		PartDefinition p13 = p4.addOrReplaceChild("l_antler", CubeListBuilder.create()
				.texOffs(108, 64).addBox(-1.0f, -12.0f, -1.0f, 2.0f, 12.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(2.5f, -7.0f, -1.0f, -0.25f, 0.0f, 0.45f));
		p13.addOrReplaceChild("l_tip", CubeListBuilder.create()
				.texOffs(58, 82).addBox(-1.5f, -5.0f, -1.5f, 3.0f, 5.0f, 3.0f, new CubeDeformation(0.0f)),
				PartPose.offset(0.0f, -12.0f, 0.0f));
		PartDefinition p15 = p13.addOrReplaceChild("l_tine", CubeListBuilder.create()
				.texOffs(50, 82).addBox(-1.0f, -7.0f, -1.0f, 2.0f, 7.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, -5.0f, 0.0f, 0.75f, 0.0f, 0.0f));
		p15.addOrReplaceChild("l_tine_tip", CubeListBuilder.create()
				.texOffs(86, 82).addBox(-1.0f, -4.0f, -1.0f, 2.0f, 4.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offset(0.0f, -7.0f, 0.0f));
		PartDefinition p17 = p13.addOrReplaceChild("l_tine2", CubeListBuilder.create()
				.texOffs(50, 82).addBox(-1.0f, -7.0f, -1.0f, 2.0f, 7.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, -9.0f, 0.0f, -0.2f, 0.0f, 0.85f));
		p17.addOrReplaceChild("l_tine2_tip", CubeListBuilder.create()
				.texOffs(86, 82).addBox(-1.0f, -4.0f, -1.0f, 2.0f, 4.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offset(0.0f, -7.0f, 0.0f));
		p2.addOrReplaceChild("saddle", CubeListBuilder.create()
				.texOffs(36, 64).addBox(-7.5f, -16.5f, -6.0f, 15.0f, 3.0f, 12.0f, new CubeDeformation(0.0f))
				.texOffs(102, 64).addBox(-8.0f, -14.0f, -1.0f, 1.0f, 13.0f, 2.0f, new CubeDeformation(0.0f))
				.texOffs(102, 64).addBox(7.0f, -14.0f, -1.0f, 1.0f, 13.0f, 2.0f, new CubeDeformation(0.0f))
				.texOffs(108, 82).addBox(-9.0f, -2.0f, -1.5f, 2.0f, 3.0f, 1.0f, new CubeDeformation(0.0f))
				.texOffs(108, 82).addBox(7.0f, -2.0f, -1.5f, 2.0f, 3.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.ZERO);
		p2.addOrReplaceChild("tail", CubeListBuilder.create()
				.texOffs(24, 82).addBox(-2.0f, -1.0f, 0.0f, 4.0f, 6.0f, 3.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, -12.0f, 15.0f, 0.7f, 0.0f, 0.0f));
		p2.addOrReplaceChild("crys0", CubeListBuilder.create()
				.texOffs(38, 82).addBox(-1.5f, -6.0f, -1.5f, 3.0f, 6.0f, 3.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, -14.0f, -5.0f, -0.2f, 0.0f, 0.0f));
		p2.addOrReplaceChild("crys1", CubeListBuilder.create()
				.texOffs(38, 82).addBox(-1.5f, -6.0f, -1.5f, 3.0f, 6.0f, 3.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, -14.0f, 4.0f, 0.25f, 0.0f, 0.0f));
		PartDefinition p23 = p1.addOrReplaceChild("rf_leg", CubeListBuilder.create()
				.texOffs(78, 44).addBox(-2.5f, -1.0f, -2.5f, 5.0f, 14.0f, 5.0f, new CubeDeformation(0.0f)),
				PartPose.offset(-4.5f, -27.0f, -11.0f));
		PartDefinition p24 = p23.addOrReplaceChild("rf_leg_shin", CubeListBuilder.create()
				.texOffs(90, 64).addBox(-1.5f, 0.0f, -1.5f, 3.0f, 12.0f, 3.0f, new CubeDeformation(0.0f)),
				PartPose.offset(0.0f, 13.0f, 0.0f));
		p24.addOrReplaceChild("rf_leg_hoof", CubeListBuilder.create()
				.texOffs(70, 82).addBox(-2.0f, 0.0f, -2.0f, 4.0f, 3.0f, 4.0f, new CubeDeformation(0.0f)),
				PartPose.offset(0.0f, 11.0f, 0.0f));
		PartDefinition p26 = p1.addOrReplaceChild("lf_leg", CubeListBuilder.create()
				.texOffs(78, 44).addBox(-2.5f, -1.0f, -2.5f, 5.0f, 14.0f, 5.0f, new CubeDeformation(0.0f)),
				PartPose.offset(4.5f, -27.0f, -11.0f));
		PartDefinition p27 = p26.addOrReplaceChild("lf_leg_shin", CubeListBuilder.create()
				.texOffs(90, 64).addBox(-1.5f, 0.0f, -1.5f, 3.0f, 12.0f, 3.0f, new CubeDeformation(0.0f)),
				PartPose.offset(0.0f, 13.0f, 0.0f));
		p27.addOrReplaceChild("lf_leg_hoof", CubeListBuilder.create()
				.texOffs(70, 82).addBox(-2.0f, 0.0f, -2.0f, 4.0f, 3.0f, 4.0f, new CubeDeformation(0.0f)),
				PartPose.offset(0.0f, 11.0f, 0.0f));
		PartDefinition p29 = p1.addOrReplaceChild("rb_leg", CubeListBuilder.create()
				.texOffs(78, 44).addBox(-2.5f, -1.0f, -2.5f, 5.0f, 14.0f, 5.0f, new CubeDeformation(0.0f)),
				PartPose.offset(-4.5f, -27.0f, 11.0f));
		PartDefinition p30 = p29.addOrReplaceChild("rb_leg_shin", CubeListBuilder.create()
				.texOffs(90, 64).addBox(-1.5f, 0.0f, -1.5f, 3.0f, 12.0f, 3.0f, new CubeDeformation(0.0f)),
				PartPose.offset(0.0f, 13.0f, 0.0f));
		p30.addOrReplaceChild("rb_leg_hoof", CubeListBuilder.create()
				.texOffs(70, 82).addBox(-2.0f, 0.0f, -2.0f, 4.0f, 3.0f, 4.0f, new CubeDeformation(0.0f)),
				PartPose.offset(0.0f, 11.0f, 0.0f));
		PartDefinition p32 = p1.addOrReplaceChild("lb_leg", CubeListBuilder.create()
				.texOffs(78, 44).addBox(-2.5f, -1.0f, -2.5f, 5.0f, 14.0f, 5.0f, new CubeDeformation(0.0f)),
				PartPose.offset(4.5f, -27.0f, 11.0f));
		PartDefinition p33 = p32.addOrReplaceChild("lb_leg_shin", CubeListBuilder.create()
				.texOffs(90, 64).addBox(-1.5f, 0.0f, -1.5f, 3.0f, 12.0f, 3.0f, new CubeDeformation(0.0f)),
				PartPose.offset(0.0f, 13.0f, 0.0f));
		p33.addOrReplaceChild("lb_leg_hoof", CubeListBuilder.create()
				.texOffs(70, 82).addBox(-2.0f, 0.0f, -2.0f, 4.0f, 3.0f, 4.0f, new CubeDeformation(0.0f)),
				PartPose.offset(0.0f, 11.0f, 0.0f));
		return LayerDefinition.create(mesh, 128, 128);
	}

	@Override
	public void setupAnim(CrystalDeerRenderState state) {
		super.setupAnim(state);
		float pos = state.walkAnimationPos;
		float speed = Math.min(state.walkAnimationSpeed, 1.0f);
		float age = state.ageInTicks;
		float look = Mth.clamp(state.yRot, -40.0f, 40.0f) * Mth.DEG_TO_RAD;
		neck.yRot += look * 0.35f;
		head.yRot += look * 0.5f;
		head.xRot += state.xRot * Mth.DEG_TO_RAD * 0.4f;

		// Kreuzgang: diagonale Beinpaare, die Unterschenkel knicken beim Vorschwingen
		float a = Mth.cos(pos * 0.6662f) * 0.95f * speed;
		rfLeg.xRot += a;
		lbLeg.xRot += a;
		lfLeg.xRot -= a;
		rbLeg.xRot -= a;
		rfLegShin.xRot += Math.max(0.0f, -a) * 0.9f;
		lbLegShin.xRot += Math.max(0.0f, -a) * 0.5f;
		lfLegShin.xRot += Math.max(0.0f, a) * 0.9f;
		rbLegShin.xRot += Math.max(0.0f, a) * 0.5f;
		neck.xRot += Mth.sin(pos * 1.3324f) * 0.05f * speed;
		body.y += Math.abs(Mth.sin(pos * 0.6662f)) * -0.8f * speed;
		tail.xRot += Mth.sin(age * 0.21f) * 0.12f + speed * 0.3f;
		rEar.zRot += Mth.sin(age * 0.09f) * 0.07f;
		lEar.zRot -= Mth.sin(age * 0.11f + 1.3f) * 0.07f;
		saddle.visible = state.saddled;

		if (state.graze > 0.0f) {
			poseGraze(state.graze);
		}

		if (state.rear > 0.0f) {
			poseRear(state.rear);
		}
	}

	private void poseGraze(float k) {
		neck.xRot += k * 1.250f;
		head.xRot += k * 0.350f;
	}

	private void poseRear(float k) {
		body.xRot += k * -0.600f;
		body.y += k * -4.000f;
		rfLeg.xRot += k * -1.100f;
		rfLeg.z += k * -2.000f;
		rfLeg.y += k * -6.000f;
		lfLeg.xRot += k * -0.900f;
		lfLeg.z += k * -2.000f;
		lfLeg.y += k * -6.000f;
		rfLegShin.xRot += k * 1.200f;
		lfLegShin.xRot += k * 1.000f;
		neck.xRot += k * -0.300f;
		tail.xRot += k * 0.400f;
	}


}
