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
 * Nachtrabe, Stufe 3 (Begleiter-Redesign v0.25.5).
 * Modell in doppelter Detaildichte (halbe Pixel) – der Renderer skaliert es auf 0.5.
 * Erzeugt von scratchpad/raven2.py (Teile und Posen), Animation von Hand.
 */
public class RavenModel3 extends EntityModel<CompanionRenderState> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(JTechStorage.id("raven_3"), "main");

	private final ModelPart raven;
	private final ModelPart body;
	private final ModelPart head;
	private final ModelPart beakLo;
	private final ModelPart crest;
	private final ModelPart rWing;
	private final ModelPart lWing;
	private final ModelPart rPrim;
	private final ModelPart lPrim;
	private final ModelPart tail;
	private final ModelPart rLeg;
	private final ModelPart lLeg;

	public RavenModel3(ModelPart root) {
		super(root);
		this.raven = root.getChild("raven");
		this.body = root.getChild("raven").getChild("body");
		this.head = root.getChild("raven").getChild("body").getChild("head");
		this.beakLo = root.getChild("raven").getChild("body").getChild("head").getChild("beak_lo");
		this.crest = root.getChild("raven").getChild("body").getChild("head").getChild("crest");
		this.rWing = root.getChild("raven").getChild("body").getChild("r_wing");
		this.lWing = root.getChild("raven").getChild("body").getChild("l_wing");
		this.rPrim = root.getChild("raven").getChild("body").getChild("r_wing").getChild("r_prim");
		this.lPrim = root.getChild("raven").getChild("body").getChild("l_wing").getChild("l_prim");
		this.tail = root.getChild("raven").getChild("body").getChild("tail");
		this.rLeg = root.getChild("raven").getChild("body").getChild("r_leg");
		this.lLeg = root.getChild("raven").getChild("body").getChild("l_leg");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		PartDefinition p1 = root.addOrReplaceChild("raven", CubeListBuilder.create(),
				PartPose.offset(0.0f, 24.0f, 0.0f));
		PartDefinition p2 = p1.addOrReplaceChild("body", CubeListBuilder.create()
				.texOffs(42, 0).addBox(-6.0f, -6.5f, -8.5f, 12.0f, 13.0f, 17.0f, new CubeDeformation(0.0f))
				.texOffs(0, 33).addBox(-5.5f, -5.0f, -10.5f, 11.0f, 10.0f, 7.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, -13.21f, 0.0f, -0.55f, 0.0f, 0.0f));
		PartDefinition p3 = p2.addOrReplaceChild("head", CubeListBuilder.create()
				.texOffs(218, 0).addBox(-4.5f, -9.0f, -5.5f, 9.0f, 9.0f, 9.0f, new CubeDeformation(0.0f))
				.texOffs(40, 50).addBox(-4.5f, -6.48f, -6.3f, 9.0f, 2.0f, 3.0f, new CubeDeformation(0.0f))
				.texOffs(60, 33).addBox(-1.5f, -5.4f, -14.5f, 3.0f, 3.0f, 9.0f, new CubeDeformation(0.0f))
				.texOffs(28, 50).addBox(-1.0f, -4.9f, -15.0f, 2.0f, 4.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, -6.0f, -7.0f, 0.6f, 0.0f, 0.0f));
		p3.addOrReplaceChild("beak_lo", CubeListBuilder.create()
				.texOffs(180, 33).addBox(-1.0f, 0.0f, -7.0f, 2.0f, 1.0f, 7.0f, new CubeDeformation(0.0f)),
				PartPose.offset(0.0f, -2.4f, -5.5f));
		p3.addOrReplaceChild("mask", CubeListBuilder.create()
				.texOffs(120, 33).addBox(-2.5f, -7.6f, -12.25f, 5.0f, 3.0f, 8.0f, new CubeDeformation(0.0f))
				.texOffs(0, 50).addBox(-5.0f, -9.8f, -6.1f, 10.0f, 2.0f, 4.0f, new CubeDeformation(0.0f)),
				PartPose.ZERO);
		p3.addOrReplaceChild("crest", CubeListBuilder.create()
				.texOffs(146, 33).addBox(-1.0f, -9.0f, -1.0f, 2.0f, 9.0f, 2.0f, new CubeDeformation(0.0f))
				.texOffs(208, 33).addBox(-1.0f, -6.0f, 2.0f, 2.0f, 6.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, -8.0f, 1.0f, -0.55f, 0.0f, 0.0f));
		PartDefinition p7 = p3.addOrReplaceChild("r_horn", CubeListBuilder.create()
				.texOffs(166, 33).addBox(-1.0f, -8.0f, -1.0f, 2.0f, 8.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(-3.5f, -8.0f, 0.5f, -1.35f, 0.0f, -0.55f));
		p7.addOrReplaceChild("r_horn_t", CubeListBuilder.create()
				.texOffs(36, 50).addBox(-0.5f, -5.0f, -0.5f, 1.0f, 5.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, -7.5f, 0.0f, -0.6f, 0.0f, 0.0f));
		PartDefinition p9 = p3.addOrReplaceChild("l_horn", CubeListBuilder.create()
				.texOffs(166, 33).addBox(-1.0f, -8.0f, -1.0f, 2.0f, 8.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(3.5f, -8.0f, 0.5f, -1.35f, 0.0f, 0.55f));
		p9.addOrReplaceChild("l_horn_t", CubeListBuilder.create()
				.texOffs(36, 50).addBox(-0.5f, -5.0f, -0.5f, 1.0f, 5.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, -7.5f, 0.0f, -0.6f, 0.0f, 0.0f));
		p3.addOrReplaceChild("ruff0", CubeListBuilder.create()
				.texOffs(174, 33).addBox(-1.0f, -8.0f, 0.0f, 2.0f, 9.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(-4.0f, -1.0f, 0.0f, 0.9f, 0.0f, -0.8f));
		p3.addOrReplaceChild("ruff1", CubeListBuilder.create()
				.texOffs(174, 33).addBox(-1.0f, -8.0f, 0.0f, 2.0f, 9.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(4.0f, -1.0f, 0.0f, 0.9f, 0.0f, 0.8f));
		p3.addOrReplaceChild("ruff2", CubeListBuilder.create()
				.texOffs(174, 33).addBox(-1.0f, -8.0f, 0.0f, 2.0f, 9.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(-3.0f, -1.0f, 3.0f, 0.9f, 0.4f, -0.6f));
		p3.addOrReplaceChild("ruff3", CubeListBuilder.create()
				.texOffs(174, 33).addBox(-1.0f, -8.0f, 0.0f, 2.0f, 9.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(3.0f, -1.0f, 3.0f, 0.9f, -0.4f, 0.6f));
		p3.addOrReplaceChild("ruff4", CubeListBuilder.create()
				.texOffs(174, 33).addBox(-1.0f, -8.0f, 0.0f, 2.0f, 9.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, -1.0f, 4.0f, 0.9f, 0.0f, 0.0f));
		p3.addOrReplaceChild("ruff5", CubeListBuilder.create()
				.texOffs(174, 33).addBox(-1.0f, -8.0f, 0.0f, 2.0f, 9.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(-1.5f, -1.0f, 3.5f, 0.9f, 0.2f, -0.3f));
		p3.addOrReplaceChild("ruff6", CubeListBuilder.create()
				.texOffs(174, 33).addBox(-1.0f, -8.0f, 0.0f, 2.0f, 9.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(1.5f, -1.0f, 3.5f, 0.9f, -0.2f, 0.3f));
		PartDefinition p18 = p2.addOrReplaceChild("r_wing", CubeListBuilder.create()
				.texOffs(0, 0).addBox(-1.0f, 0.0f, 0.0f, 2.0f, 14.0f, 19.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(-6.5f, -5.5f, -6.5f, 0.22f, 0.0f, -0.05f));
		p18.addOrReplaceChild("r_prim", CubeListBuilder.create()
				.texOffs(100, 0).addBox(-0.5f, 0.0f, 0.0f, 1.0f, 9.0f, 17.0f, new CubeDeformation(0.0f))
				.texOffs(136, 0).addBox(-1.0f, 1.0f, 2.0f, 1.0f, 6.0f, 13.0f, new CubeDeformation(0.0f))
				.texOffs(198, 33).addBox(-0.5f, 0.5f, 16.0f, 1.0f, 4.0f, 4.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, 4.0f, 16.0f, 0.2f, 0.0f, 0.0f));
		PartDefinition p20 = p2.addOrReplaceChild("l_wing", CubeListBuilder.create()
				.texOffs(0, 0).addBox(-1.0f, 0.0f, 0.0f, 2.0f, 14.0f, 19.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(6.5f, -5.5f, -6.5f, 0.22f, 0.0f, 0.05f));
		p20.addOrReplaceChild("l_prim", CubeListBuilder.create()
				.texOffs(100, 0).addBox(-0.5f, 0.0f, 0.0f, 1.0f, 9.0f, 17.0f, new CubeDeformation(0.0f))
				.texOffs(136, 0).addBox(0.0f, 1.0f, 2.0f, 1.0f, 6.0f, 13.0f, new CubeDeformation(0.0f))
				.texOffs(198, 33).addBox(-0.5f, 0.5f, 16.0f, 1.0f, 4.0f, 4.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, 4.0f, 16.0f, 0.2f, 0.0f, 0.0f));
		PartDefinition p22 = p2.addOrReplaceChild("tail", CubeListBuilder.create()
				.texOffs(164, 0).addBox(-5.0f, 0.0f, 0.0f, 10.0f, 1.0f, 17.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, -4.5f, 7.5f, 0.25f, 0.0f, 0.0f));
		p22.addOrReplaceChild("tail_cr0", CubeListBuilder.create()
				.texOffs(244, 33).addBox(-1.0f, -1.0f, 0.0f, 2.0f, 5.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(-4.0f, 0.0f, 16.0f, 1.3f, 0.0f, 0.0f));
		p22.addOrReplaceChild("tail_cr1", CubeListBuilder.create()
				.texOffs(244, 33).addBox(-1.0f, -1.0f, 0.0f, 2.0f, 5.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, 0.0f, 16.0f, 1.3f, 0.0f, 0.0f));
		p22.addOrReplaceChild("tail_cr2", CubeListBuilder.create()
				.texOffs(244, 33).addBox(-1.0f, -1.0f, 0.0f, 2.0f, 5.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(4.0f, 0.0f, 16.0f, 1.3f, 0.0f, 0.0f));
		p2.addOrReplaceChild("r_leg", CubeListBuilder.create()
				.texOffs(154, 33).addBox(-1.5f, 0.0f, -1.5f, 3.0f, 7.0f, 3.0f, new CubeDeformation(0.0f))
				.texOffs(232, 33).addBox(-2.3f, 6.0f, -4.5f, 1.0f, 2.0f, 5.0f, new CubeDeformation(0.0f))
				.texOffs(232, 33).addBox(0.3f, 6.0f, -4.5f, 1.0f, 2.0f, 5.0f, new CubeDeformation(0.0f))
				.texOffs(232, 33).addBox(-0.5f, 6.0f, -4.5f, 1.0f, 2.0f, 5.0f, new CubeDeformation(0.0f))
				.texOffs(232, 33).addBox(-0.5f, 6.0f, 1.0f, 1.0f, 2.0f, 5.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(-3.0f, 5.5f, 1.0f, 0.55f, 0.0f, 0.0f));
		p2.addOrReplaceChild("l_leg", CubeListBuilder.create()
				.texOffs(154, 33).addBox(-1.5f, 0.0f, -1.5f, 3.0f, 7.0f, 3.0f, new CubeDeformation(0.0f))
				.texOffs(232, 33).addBox(-2.3f, 6.0f, -4.5f, 1.0f, 2.0f, 5.0f, new CubeDeformation(0.0f))
				.texOffs(232, 33).addBox(0.3f, 6.0f, -4.5f, 1.0f, 2.0f, 5.0f, new CubeDeformation(0.0f))
				.texOffs(232, 33).addBox(-0.5f, 6.0f, -4.5f, 1.0f, 2.0f, 5.0f, new CubeDeformation(0.0f))
				.texOffs(232, 33).addBox(-0.5f, 6.0f, 1.0f, 1.0f, 2.0f, 5.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(3.0f, 5.5f, 1.0f, 0.55f, 0.0f, 0.0f));
		p2.addOrReplaceChild("bag", CubeListBuilder.create()
				.texOffs(36, 33).addBox(-9.0f, -1.0f, -4.5f, 3.0f, 7.0f, 9.0f, new CubeDeformation(0.0f))
				.texOffs(64, 50).addBox(-7.0f, -7.5f, -0.5f, 13.0f, 1.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offset(0.0f, 0.0f, 1.0f));
		p2.addOrReplaceChild("bag_l", CubeListBuilder.create()
				.texOffs(36, 33).addBox(6.0f, -1.0f, -4.5f, 3.0f, 7.0f, 9.0f, new CubeDeformation(0.0f)),
				PartPose.offset(0.0f, 0.0f, 1.0f));
		p2.addOrReplaceChild("saddle", CubeListBuilder.create()
				.texOffs(84, 33).addBox(-4.5f, -8.0f, -4.5f, 9.0f, 2.0f, 9.0f, new CubeDeformation(0.0f))
				.texOffs(92, 50).addBox(-4.0f, -8.5f, -3.5f, 1.0f, 1.0f, 1.0f, new CubeDeformation(0.0f))
				.texOffs(92, 50).addBox(-4.0f, -8.5f, -0.5f, 1.0f, 1.0f, 1.0f, new CubeDeformation(0.0f))
				.texOffs(92, 50).addBox(-4.0f, -8.5f, 2.5f, 1.0f, 1.0f, 1.0f, new CubeDeformation(0.0f))
				.texOffs(92, 50).addBox(3.0f, -8.5f, -3.5f, 1.0f, 1.0f, 1.0f, new CubeDeformation(0.0f))
				.texOffs(92, 50).addBox(3.0f, -8.5f, -0.5f, 1.0f, 1.0f, 1.0f, new CubeDeformation(0.0f))
				.texOffs(92, 50).addBox(3.0f, -8.5f, 2.5f, 1.0f, 1.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offset(0.0f, 0.0f, 1.0f));
		p2.addOrReplaceChild("pouch", CubeListBuilder.create()
				.texOffs(216, 33).addBox(-3.0f, 0.0f, -2.0f, 6.0f, 5.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offset(0.0f, -1.0f, -11.5f));
		return LayerDefinition.create(mesh, 256, 128);
	}

	@Override
	public void setupAnim(CompanionRenderState state) {
		super.setupAnim(state);
		float age = state.ageInTicks;
		float pos = state.walkAnimationPos;
		float speed = Math.min(state.walkAnimationSpeed, 1.0f);

		if (state.flying) {
			// Flug: Flügel weit, kräftiger Schlag, Beine angezogen
			poseFly(1.0f);
			float flap = Mth.sin(age * 0.9f);
			rWing.zRot += flap * 0.7f;
			lWing.zRot -= flap * 0.7f;
			body.y += flap * 0.8f;

			if (state.attack > 0.0f) {
				posePeck(Mth.sin(state.attack * Mth.PI) * 1.3f);
			}

			return;
		}

		if (state.sitting || state.exhausted) {
			poseSit(1.0f);
		}

		if (state.exhausted) {
			poseTired(1.0f);
		}

		// Kopf: neugieriges Schiefhalten, folgt dem Blick
		float yaw = Mth.clamp(state.yRot, -60.0f, 60.0f) * Mth.DEG_TO_RAD;
		head.yRot += yaw * 0.8f;
		head.xRot += state.xRot * Mth.DEG_TO_RAD * 0.5f;
		head.zRot += Mth.sin(age * 0.045f) * 0.18f;
		beakLo.xRot += Math.max(0.0f, Mth.sin(age * 0.13f) - 0.92f) * 4.0f;   // krächzt ab und zu

		// Hüpfen statt Laufen
		float hop = Mth.abs(Mth.sin(pos * 1.2f));
		body.y -= hop * 2.0f * speed;
		rLeg.xRot += Mth.cos(pos * 1.2f) * 0.5f * speed;
		lLeg.xRot += Mth.cos(pos * 1.2f) * 0.5f * speed;
		tail.xRot += Mth.sin(age * 0.1f) * 0.05f + hop * 0.2f * speed;
		rWing.zRot += hop * 0.15f * speed;
		lWing.zRot -= hop * 0.15f * speed;

		// Picken (Items aufsammeln / abgeben)
		if (state.action > 0.0f) {
			posePeck(Mth.sin(state.action * Mth.PI * 2.0f) * 0.5f + 0.5f);
		}

		// Schnabelhieb
		if (state.attack > 0.0f) {
			posePeck(Mth.sin(state.attack * Mth.PI) * 1.3f);
		}
	}

	private void poseSit(float k) {
		rLeg.xRot += k * 1.300f;
		lLeg.xRot += k * 1.300f;
		body.xRot += k * 0.350f;
		body.y += k * 3.610f;
		tail.xRot += k * -0.100f;
	}

	private void poseTired(float k) {
		head.xRot += k * 0.450f;
		rWing.zRot += k * -0.250f;
		lWing.zRot += k * 0.250f;
	}

	private void poseFly(float k) {
		body.xRot += k * 0.600f;
		head.xRot += k * -0.450f;
		rWing.zRot += k * 1.250f;
		lWing.zRot += k * -1.250f;
		rLeg.xRot += k * 1.100f;
		lLeg.xRot += k * 1.100f;
		tail.xRot += k * -0.250f;
		rPrim.xRot += k * -0.200f;
		lPrim.xRot += k * -0.200f;
	}

	private void posePeck(float k) {
		head.xRot += k * 0.700f;
		body.xRot += k * 0.200f;
	}


}
