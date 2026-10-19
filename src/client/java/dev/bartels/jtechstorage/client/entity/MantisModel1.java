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
 * Schattenmantis, Stufe 1 (Begleiter-Redesign v0.25.5).
 * Modell in doppelter Detaildichte (halbe Pixel) – der Renderer skaliert es auf 0.5.
 * Erzeugt von scratchpad/mantis2.py (Teile und Posen), Animation von Hand.
 */
public class MantisModel1 extends EntityModel<CompanionRenderState> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(JTechStorage.id("mantis_1"), "main");

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

	public MantisModel1(ModelPart root) {
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
				.texOffs(88, 0).addBox(-2.5f, -2.5f, -6.0f, 5.0f, 5.0f, 6.0f, new CubeDeformation(0.0f)),
				PartPose.offset(0.0f, -12.96f, 0.0f));
		PartDefinition p3 = p2.addOrReplaceChild("neck", CubeListBuilder.create()
				.texOffs(146, 0).addBox(-1.5f, -1.5f, -7.0f, 3.0f, 3.0f, 7.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, -1.5f, -5.0f, -1.15f, 0.0f, 0.0f));
		PartDefinition p4 = p3.addOrReplaceChild("head", CubeListBuilder.create()
				.texOffs(114, 0).addBox(-5.0f, -8.0f, -6.0f, 10.0f, 4.0f, 6.0f, new CubeDeformation(0.0f))
				.texOffs(178, 0).addBox(-2.5f, -4.0f, -5.5f, 5.0f, 4.0f, 5.0f, new CubeDeformation(0.0f))
				.texOffs(198, 0).addBox(-7.5f, -9.0f, -5.5f, 4.0f, 4.0f, 4.0f, new CubeDeformation(0.0f))
				.texOffs(198, 0).addBox(3.5f, -9.0f, -5.5f, 4.0f, 4.0f, 4.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, 0.0f, -6.0f, 1.15f, 0.0f, 0.0f));
		p4.addOrReplaceChild("r_antenna", CubeListBuilder.create()
				.texOffs(50, 0).addBox(-0.5f, -0.5f, -12.0f, 1.0f, 1.0f, 12.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(-2.5f, -7.0f, -5.0f, 1.0f, 0.45f, 0.0f));
		p4.addOrReplaceChild("l_antenna", CubeListBuilder.create()
				.texOffs(50, 0).addBox(-0.5f, -0.5f, -12.0f, 1.0f, 1.0f, 12.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(2.5f, -7.0f, -5.0f, 1.0f, -0.45f, 0.0f));
		PartDefinition p7 = p4.addOrReplaceChild("r_horn", CubeListBuilder.create()
				.texOffs(214, 0).addBox(-1.0f, -6.0f, -1.0f, 2.0f, 6.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(-3.5f, -7.5f, -3.0f, -1.35f, 0.0f, -0.22f));
		p7.addOrReplaceChild("r_horn_t", CubeListBuilder.create()
				.texOffs(14, 19).addBox(-0.5f, -4.0f, -0.5f, 1.0f, 4.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, -5.5f, 0.0f, -0.55f, 0.0f, 0.0f));
		PartDefinition p9 = p4.addOrReplaceChild("l_horn", CubeListBuilder.create()
				.texOffs(214, 0).addBox(-1.0f, -6.0f, -1.0f, 2.0f, 6.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(3.5f, -7.5f, -3.0f, -1.35f, 0.0f, 0.22f));
		p9.addOrReplaceChild("l_horn_t", CubeListBuilder.create()
				.texOffs(14, 19).addBox(-0.5f, -4.0f, -0.5f, 1.0f, 4.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, -5.5f, 0.0f, -0.55f, 0.0f, 0.0f));
		p4.addOrReplaceChild("r_jaw", CubeListBuilder.create()
				.texOffs(0, 19).addBox(-1.0f, 0.0f, -1.0f, 2.0f, 4.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(-1.5f, -0.5f, -4.5f, -0.2f, 0.0f, 0.2f));
		p4.addOrReplaceChild("l_jaw", CubeListBuilder.create()
				.texOffs(0, 19).addBox(-1.0f, 0.0f, -1.0f, 2.0f, 4.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(1.5f, -0.5f, -4.5f, -0.2f, 0.0f, -0.2f));
		PartDefinition p13 = p3.addOrReplaceChild("r_arm", CubeListBuilder.create()
				.texOffs(166, 0).addBox(-1.5f, 0.0f, -1.5f, 3.0f, 7.0f, 3.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(-2.5f, 0.0f, -4.2f, 0.6f, 0.0f, -0.22f));
		PartDefinition p14 = p13.addOrReplaceChild("r_femur", CubeListBuilder.create()
				.texOffs(76, 0).addBox(-1.5f, 0.0f, -1.5f, 3.0f, 10.0f, 3.0f, new CubeDeformation(0.0f))
				.texOffs(8, 19).addBox(-0.5f, 2.5f, -2.5f, 1.0f, 3.0f, 2.0f, new CubeDeformation(0.0f))
				.texOffs(8, 19).addBox(-0.5f, 4.5f, -2.5f, 1.0f, 3.0f, 2.0f, new CubeDeformation(0.0f))
				.texOffs(8, 19).addBox(-0.5f, 6.5f, -2.5f, 1.0f, 3.0f, 2.0f, new CubeDeformation(0.0f))
				.texOffs(8, 19).addBox(-0.5f, 8.5f, -2.5f, 1.0f, 3.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, 6.0f, 0.0f, -2.25f, 0.0f, 0.0f));
		p14.addOrReplaceChild("r_blade", CubeListBuilder.create()
				.texOffs(40, 0).addBox(-1.0f, 0.0f, -1.5f, 2.0f, 12.0f, 3.0f, new CubeDeformation(0.0f))
				.texOffs(8, 19).addBox(-0.5f, 3.6f, -2.5f, 1.0f, 3.0f, 2.0f, new CubeDeformation(0.0f))
				.texOffs(8, 19).addBox(-0.5f, 7.2f, -2.5f, 1.0f, 3.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, 9.0f, 0.0f, 2.75f, 0.0f, 0.0f));
		PartDefinition p16 = p3.addOrReplaceChild("l_arm", CubeListBuilder.create()
				.texOffs(166, 0).addBox(-1.5f, 0.0f, -1.5f, 3.0f, 7.0f, 3.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(2.5f, 0.0f, -4.2f, 0.6f, 0.0f, 0.22f));
		PartDefinition p17 = p16.addOrReplaceChild("l_femur", CubeListBuilder.create()
				.texOffs(76, 0).addBox(-1.5f, 0.0f, -1.5f, 3.0f, 10.0f, 3.0f, new CubeDeformation(0.0f))
				.texOffs(8, 19).addBox(-0.5f, 2.5f, -2.5f, 1.0f, 3.0f, 2.0f, new CubeDeformation(0.0f))
				.texOffs(8, 19).addBox(-0.5f, 4.5f, -2.5f, 1.0f, 3.0f, 2.0f, new CubeDeformation(0.0f))
				.texOffs(8, 19).addBox(-0.5f, 6.5f, -2.5f, 1.0f, 3.0f, 2.0f, new CubeDeformation(0.0f))
				.texOffs(8, 19).addBox(-0.5f, 8.5f, -2.5f, 1.0f, 3.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, 6.0f, 0.0f, -2.25f, 0.0f, 0.0f));
		p17.addOrReplaceChild("l_blade", CubeListBuilder.create()
				.texOffs(40, 0).addBox(-1.0f, 0.0f, -1.5f, 2.0f, 12.0f, 3.0f, new CubeDeformation(0.0f))
				.texOffs(8, 19).addBox(-0.5f, 3.6f, -2.5f, 1.0f, 3.0f, 2.0f, new CubeDeformation(0.0f))
				.texOffs(8, 19).addBox(-0.5f, 7.2f, -2.5f, 1.0f, 3.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, 9.0f, 0.0f, 2.75f, 0.0f, 0.0f));
		PartDefinition p19 = p2.addOrReplaceChild("abdomen", CubeListBuilder.create()
				.texOffs(0, 0).addBox(-3.0f, -2.5f, 0.0f, 6.0f, 5.0f, 14.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f, 0.14f, 0.0f, 0.0f));
		PartDefinition p20 = p19.addOrReplaceChild("abd_tip", CubeListBuilder.create()
				.texOffs(226, 0).addBox(-2.0f, -1.5f, 0.0f, 4.0f, 3.0f, 4.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, 0.5f, 13.0f, 0.3f, 0.0f, 0.0f));
		p20.addOrReplaceChild("r_cerc", CubeListBuilder.create()
				.texOffs(242, 0).addBox(-0.5f, -0.5f, 0.0f, 1.0f, 1.0f, 5.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(-1.5f, -1.0f, 3.0f, 0.35f, -0.3f, 0.0f));
		p20.addOrReplaceChild("l_cerc", CubeListBuilder.create()
				.texOffs(242, 0).addBox(-0.5f, -0.5f, 0.0f, 1.0f, 1.0f, 5.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(1.5f, -1.0f, 3.0f, 0.35f, 0.3f, 0.0f));
		PartDefinition p23 = p2.addOrReplaceChild("rm_leg", CubeListBuilder.create()
				.texOffs(222, 0).addBox(-0.5f, 0.0f, -0.5f, 1.0f, 7.0f, 1.0f, new CubeDeformation(0.0f))
				.texOffs(18, 19).addBox(-0.5f, 4.0f, -1.5f, 1.0f, 2.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(-2.5f, 1.5f, -3.3f, -0.6f, 0.0f, 1.15f));
		p23.addOrReplaceChild("rm_shin", CubeListBuilder.create()
				.texOffs(110, 0).addBox(-0.5f, 0.0f, -0.5f, 1.0f, 10.0f, 1.0f, new CubeDeformation(0.0f))
				.texOffs(18, 19).addBox(-0.5f, 4.5f, 0.5f, 1.0f, 2.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, 6.0f, 0.0f, 0.0f, 0.0f, -1.6f));
		PartDefinition p25 = p2.addOrReplaceChild("lm_leg", CubeListBuilder.create()
				.texOffs(222, 0).addBox(-0.5f, 0.0f, -0.5f, 1.0f, 7.0f, 1.0f, new CubeDeformation(0.0f))
				.texOffs(18, 19).addBox(-0.5f, 4.0f, -1.5f, 1.0f, 2.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(2.5f, 1.5f, -3.3f, -0.6f, 0.0f, -1.15f));
		p25.addOrReplaceChild("lm_shin", CubeListBuilder.create()
				.texOffs(110, 0).addBox(-0.5f, 0.0f, -0.5f, 1.0f, 10.0f, 1.0f, new CubeDeformation(0.0f))
				.texOffs(18, 19).addBox(-0.5f, 4.5f, 0.5f, 1.0f, 2.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, 6.0f, 0.0f, 0.0f, 0.0f, 1.6f));
		PartDefinition p27 = p2.addOrReplaceChild("rh_leg", CubeListBuilder.create()
				.texOffs(222, 0).addBox(-0.5f, 0.0f, -0.5f, 1.0f, 7.0f, 1.0f, new CubeDeformation(0.0f))
				.texOffs(18, 19).addBox(-0.5f, 4.0f, -1.5f, 1.0f, 2.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(-2.5f, 1.5f, -1.0f, 0.5f, 0.0f, 1.15f));
		p27.addOrReplaceChild("rh_shin", CubeListBuilder.create()
				.texOffs(110, 0).addBox(-0.5f, 0.0f, -0.5f, 1.0f, 10.0f, 1.0f, new CubeDeformation(0.0f))
				.texOffs(18, 19).addBox(-0.5f, 4.5f, 0.5f, 1.0f, 2.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, 6.0f, 0.0f, 0.0f, 0.0f, -1.6f));
		PartDefinition p29 = p2.addOrReplaceChild("lh_leg", CubeListBuilder.create()
				.texOffs(222, 0).addBox(-0.5f, 0.0f, -0.5f, 1.0f, 7.0f, 1.0f, new CubeDeformation(0.0f))
				.texOffs(18, 19).addBox(-0.5f, 4.0f, -1.5f, 1.0f, 2.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(2.5f, 1.5f, -1.0f, 0.5f, 0.0f, -1.15f));
		p29.addOrReplaceChild("lh_shin", CubeListBuilder.create()
				.texOffs(110, 0).addBox(-0.5f, 0.0f, -0.5f, 1.0f, 10.0f, 1.0f, new CubeDeformation(0.0f))
				.texOffs(18, 19).addBox(-0.5f, 4.5f, 0.5f, 1.0f, 2.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, 6.0f, 0.0f, 0.0f, 0.0f, 1.6f));
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
	}

	private void poseSit(float k) {
		body.y += k * 1.490f;
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
		rJaw.zRot += k * 0.450f;
		lJaw.zRot += k * -0.450f;
	}

	private void poseFly(float k) {
		abdomen.xRot += k * -0.150f;
		rmLeg.xRot += k * 0.900f;
		lmLeg.xRot += k * 0.900f;
		rhLeg.xRot += k * 0.900f;
		lhLeg.xRot += k * 0.900f;
	}


}
