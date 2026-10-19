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
 * Nachtrabe, Stufe 1 (Begleiter-Redesign v0.25.5).
 * Modell in doppelter Detaildichte (halbe Pixel) – der Renderer skaliert es auf 0.5.
 * Erzeugt von scratchpad/raven2.py (Teile und Posen), Animation von Hand.
 */
public class RavenModel1 extends EntityModel<CompanionRenderState> implements CompanionLookModel {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(JTechStorage.id("raven_1"), "main");

	private final ModelPart raven;
	private final ModelPart body;
	private final ModelPart head;
	private final ModelPart tuft;
	private final ModelPart rWing;
	private final ModelPart lWing;
	private final ModelPart tail;
	private final ModelPart rLeg;
	private final ModelPart lLeg;
	private final ModelPart tailLong;
	private final ModelPart tailForked;
	private final ModelPart collar;
	private final ModelPart amulet;
	private final ModelPart armorC;
	private final ModelPart rArmor;
	private final ModelPart lArmor;

	public RavenModel1(ModelPart root) {
		super(root);
		this.raven = root.getChild("raven");
		this.body = root.getChild("raven").getChild("body");
		this.head = root.getChild("raven").getChild("body").getChild("head");
		this.tuft = root.getChild("raven").getChild("body").getChild("head").getChild("tuft");
		this.rWing = root.getChild("raven").getChild("body").getChild("r_wing");
		this.lWing = root.getChild("raven").getChild("body").getChild("l_wing");
		this.tail = root.getChild("raven").getChild("body").getChild("tail");
		this.rLeg = root.getChild("raven").getChild("body").getChild("r_leg");
		this.lLeg = root.getChild("raven").getChild("body").getChild("l_leg");
		this.tailLong = root.getChild("raven").getChild("body").getChild("tail_long");
		this.tailForked = root.getChild("raven").getChild("body").getChild("tail_forked");
		this.collar = root.getChild("raven").getChild("body").getChild("collar");
		this.amulet = root.getChild("raven").getChild("body").getChild("amulet");
		this.armorC = root.getChild("raven").getChild("body").getChild("armor_c");
		this.rArmor = root.getChild("raven").getChild("body").getChild("r_wing").getChild("r_armor");
		this.lArmor = root.getChild("raven").getChild("body").getChild("l_wing").getChild("l_armor");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		PartDefinition p1 = root.addOrReplaceChild("raven", CubeListBuilder.create(),
				PartPose.offset(0.0f, 24.0f, 0.0f));
		PartDefinition p2 = p1.addOrReplaceChild("body", CubeListBuilder.create()
				.texOffs(0, 0).addBox(-4.5f, -4.5f, -5.0f, 9.0f, 9.0f, 10.0f, new CubeDeformation(0.0f)),
				PartPose.offset(0.0f, -7.5f, 0.0f));
		PartDefinition p3 = p2.addOrReplaceChild("head", CubeListBuilder.create()
				.texOffs(38, 0).addBox(-4.0f, -8.0f, -5.0f, 8.0f, 8.0f, 8.0f, new CubeDeformation(0.0f))
				.texOffs(136, 0).addBox(-4.0f, -5.76f, -5.8f, 8.0f, 2.0f, 2.0f, new CubeDeformation(0.0f))
				.texOffs(108, 0).addBox(-1.5f, -4.8f, -9.0f, 3.0f, 2.0f, 4.0f, new CubeDeformation(0.0f))
				.texOffs(164, 0).addBox(-1.0f, -4.3f, -9.5f, 2.0f, 2.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offset(0.0f, -3.5f, -4.0f));
		p3.addOrReplaceChild("tuft", CubeListBuilder.create()
				.texOffs(130, 0).addBox(-1.0f, -4.0f, -0.5f, 2.0f, 4.0f, 1.0f, new CubeDeformation(0.0f))
				.texOffs(130, 0).addBox(-2.5f, -3.0f, 1.0f, 2.0f, 4.0f, 1.0f, new CubeDeformation(0.0f))
				.texOffs(130, 0).addBox(0.5f, -3.0f, 1.0f, 2.0f, 4.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, -8.0f, -1.0f, -0.4f, 0.0f, 0.0f));
		PartDefinition p5 = p2.addOrReplaceChild("r_wing", CubeListBuilder.create()
				.texOffs(70, 0).addBox(-1.0f, 0.0f, 0.0f, 2.0f, 6.0f, 7.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(-5.0f, -3.5f, -3.0f, 0.22f, 0.0f, -0.05f));
		p5.addOrReplaceChild("r_armor", CubeListBuilder.create()
				.texOffs(148, 128).addBox(-2.0f, -0.5f, -0.5f, 4.0f, 3.0f, 4.0f, new CubeDeformation(0.0f)),
				PartPose.ZERO);
		PartDefinition p7 = p2.addOrReplaceChild("l_wing", CubeListBuilder.create()
				.texOffs(70, 0).addBox(-1.0f, 0.0f, 0.0f, 2.0f, 6.0f, 7.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(5.0f, -3.5f, -3.0f, 0.22f, 0.0f, 0.05f));
		p7.addOrReplaceChild("l_armor", CubeListBuilder.create()
				.texOffs(164, 128).addBox(-2.0f, -0.5f, -0.5f, 4.0f, 3.0f, 4.0f, new CubeDeformation(0.0f)),
				PartPose.ZERO);
		p2.addOrReplaceChild("tail", CubeListBuilder.create()
				.texOffs(88, 0).addBox(-2.5f, 0.0f, 0.0f, 5.0f, 1.0f, 5.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, -2.5f, 4.0f, 0.5f, 0.0f, 0.0f));
		p2.addOrReplaceChild("r_leg", CubeListBuilder.create()
				.texOffs(122, 0).addBox(-1.0f, 0.0f, -1.0f, 2.0f, 4.0f, 2.0f, new CubeDeformation(0.0f))
				.texOffs(156, 0).addBox(-1.8f, 3.0f, -2.5f, 1.0f, 1.0f, 3.0f, new CubeDeformation(0.0f))
				.texOffs(156, 0).addBox(-0.2f, 3.0f, -2.5f, 1.0f, 1.0f, 3.0f, new CubeDeformation(0.0f))
				.texOffs(156, 0).addBox(-0.5f, 3.0f, -2.5f, 1.0f, 1.0f, 3.0f, new CubeDeformation(0.0f))
				.texOffs(156, 0).addBox(-0.5f, 3.0f, 0.5f, 1.0f, 1.0f, 3.0f, new CubeDeformation(0.0f)),
				PartPose.offset(-2.25f, 3.5f, 1.0f));
		p2.addOrReplaceChild("l_leg", CubeListBuilder.create()
				.texOffs(122, 0).addBox(-1.0f, 0.0f, -1.0f, 2.0f, 4.0f, 2.0f, new CubeDeformation(0.0f))
				.texOffs(156, 0).addBox(-1.8f, 3.0f, -2.5f, 1.0f, 1.0f, 3.0f, new CubeDeformation(0.0f))
				.texOffs(156, 0).addBox(-0.2f, 3.0f, -2.5f, 1.0f, 1.0f, 3.0f, new CubeDeformation(0.0f))
				.texOffs(156, 0).addBox(-0.5f, 3.0f, -2.5f, 1.0f, 1.0f, 3.0f, new CubeDeformation(0.0f))
				.texOffs(156, 0).addBox(-0.5f, 3.0f, 0.5f, 1.0f, 1.0f, 3.0f, new CubeDeformation(0.0f)),
				PartPose.offset(2.25f, 3.5f, 1.0f));
		p2.addOrReplaceChild("fluff0", CubeListBuilder.create()
				.texOffs(130, 0).addBox(-1.0f, -4.0f, 0.0f, 2.0f, 4.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(-4.5f, -3.0f, 0.0f, 0.3f, 0.0f, -0.5f));
		p2.addOrReplaceChild("fluff1", CubeListBuilder.create()
				.texOffs(130, 0).addBox(-1.0f, -4.0f, 0.0f, 2.0f, 4.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(4.5f, -3.0f, 1.0f, 0.3f, 0.0f, 0.5f));
		p2.addOrReplaceChild("fluff2", CubeListBuilder.create()
				.texOffs(130, 0).addBox(-1.0f, -4.0f, 0.0f, 2.0f, 4.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, -4.5f, 3.0f, 0.3f, 0.0f, 0.0f));
		PartDefinition p15 = p2.addOrReplaceChild("tail_long", CubeListBuilder.create()
				.texOffs(0, 128).addBox(-1.5f, 0.0f, 0.0f, 3.0f, 1.0f, 13.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, -2.5f, 4.0f, 0.5f, 0.0f, 0.0f));
		p15.addOrReplaceChild("streamer0_long", CubeListBuilder.create()
				.texOffs(32, 128).addBox(-0.5f, 0.0f, 0.0f, 1.0f, 1.0f, 11.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(-1.5f, 0.5f, 11.0f, 0.12f, -0.09f, 0.0f));
		p15.addOrReplaceChild("streamer1_long", CubeListBuilder.create()
				.texOffs(32, 128).addBox(-0.5f, 0.0f, 0.0f, 1.0f, 1.0f, 11.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(1.5f, 0.5f, 11.0f, 0.12f, 0.09f, 0.0f));
		PartDefinition p18 = p2.addOrReplaceChild("tail_forked", CubeListBuilder.create(),
				PartPose.offsetAndRotation(0.0f, -2.5f, 4.0f, 0.5f, 0.0f, 0.0f));
		p18.addOrReplaceChild("tail_fork_r_forked", CubeListBuilder.create()
				.texOffs(56, 128).addBox(-3.0f, 0.0f, 0.0f, 3.0f, 1.0f, 9.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(-1.0f, 0.0f, 0.0f, 0.0f, 0.55f, 0.0f));
		p18.addOrReplaceChild("tail_fork_l_forked", CubeListBuilder.create()
				.texOffs(56, 128).addBox(0.0f, 0.0f, 0.0f, 3.0f, 1.0f, 9.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(1.0f, 0.0f, 0.0f, 0.0f, -0.55f, 0.0f));
		p2.addOrReplaceChild("collar", CubeListBuilder.create()
				.texOffs(80, 128).addBox(-5.0f, -5.5f, -6.5f, 10.0f, 3.0f, 6.0f, new CubeDeformation(0.0f)),
				PartPose.ZERO);
		p2.addOrReplaceChild("amulet", CubeListBuilder.create()
				.texOffs(112, 128).addBox(-2.0f, 0.0f, 0.0f, 4.0f, 5.0f, 1.0f, new CubeDeformation(0.0f))
				.texOffs(122, 128).addBox(-1.0f, 1.0f, -0.6f, 2.0f, 3.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, -1.5f, -8.4f, 0.4f, 0.0f, 0.0f));
		p2.addOrReplaceChild("armor_c", CubeListBuilder.create()
				.texOffs(128, 128).addBox(-4.5f, -3.0f, -7.6f, 9.0f, 8.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.ZERO);
		return LayerDefinition.create(mesh, 256, 256);
	}

	@Override
	public void setupAnim(CompanionRenderState state) {
		super.setupAnim(state);
		// Aussehen (v0.25.4): gewählte Körperteil-Varianten; Ausrüstung nur im eigenen Durchgang
		tail.visible = state.part1 == 0;
		tailLong.visible = state.part1 == 1;
		tailForked.visible = state.part1 == 2;
		collar.visible = pass == 0;
		amulet.visible = pass == 1;
		armorC.visible = pass == 2;
		rArmor.visible = pass == 2;
		lArmor.visible = pass == 2;

		float age = state.ageInTicks;
		float pos = state.walkAnimationPos;
		float speed = Math.min(state.walkAnimationSpeed, 1.0f);

		if (state.flying) {
			// Flug: Flügel weit, kräftiger Schlag, Beine angezogen
			poseFly(1.0f);
			float flap = Mth.sin(age * 0.9f);
			rWing.zRot += flap * 0.6f;
			lWing.zRot -= flap * 0.6f;
			body.y += flap * 0.8f;

			if (state.attack > 0.0f) {
				posePeck(Mth.sin(state.attack * Mth.PI) * 1.3f);
			}

			// Körperteil-Varianten bewegen sich wie das Original
			CompanionLookModel.follow(tail, tailLong);
			CompanionLookModel.follow(tail, tailForked);

			return;
		}

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
				head.zRot += Mth.sin(p * Mth.PI * 4.0f) * 0.15f * k;
			}
		}

		// Kopf: neugieriges Schiefhalten, folgt dem Blick
		float yaw = Mth.clamp(state.yRot, -60.0f, 60.0f) * Mth.DEG_TO_RAD;
		head.yRot += yaw * 0.8f;
		head.xRot += state.xRot * Mth.DEG_TO_RAD * 0.5f;
		head.zRot += Mth.sin(age * 0.045f) * 0.18f;
		tuft.xRot += Mth.sin(age * 0.2f) * 0.1f;

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
		// Körperteil-Varianten bewegen sich wie das Original
		CompanionLookModel.follow(tail, tailLong);
		CompanionLookModel.follow(tail, tailForked);
	}

	private void poseSit(float k) {
		rLeg.xRot += k * 1.300f;
		lLeg.xRot += k * 1.300f;
		body.xRot += k * 0.000f;
		body.y += k * 0.520f;
		tail.xRot += k * -0.100f;
	}

	private void poseTired(float k) {
		head.xRot += k * 0.450f;
		rWing.zRot += k * -0.250f;
		lWing.zRot += k * 0.250f;
	}

	private void poseFly(float k) {
		body.xRot += k * 0.150f;
		head.xRot += k * -0.250f;
		rWing.xRot += k * 1.350f;
		rWing.zRot += k * -1.520f;
		lWing.xRot += k * 1.350f;
		lWing.zRot += k * 1.520f;
		rLeg.xRot += k * 1.100f;
		lLeg.xRot += k * 1.100f;
		tail.xRot += k * -0.250f;
	}

	private void posePeck(float k) {
		head.xRot += k * 0.700f;
		body.xRot += k * 0.200f;
	}

	private void poseJoy(float k) {
		body.y += k * -5.000f;
		rWing.xRot += k * 1.350f;
		rWing.zRot += k * -0.900f;
		lWing.xRot += k * 1.350f;
		lWing.zRot += k * 0.900f;
		head.xRot += k * -0.300f;
		rLeg.xRot += k * 0.400f;
		lLeg.xRot += k * 0.400f;
	}

	private void poseNuzzle(float k) {
		head.xRot += k * 0.450f;
		head.zRot += k * 0.500f;
		body.zRot += k * 0.150f;
		body.xRot += k * 0.150f;
		tail.xRot += k * -0.200f;
	}

	private void poseGesture(float k) {
		body.xRot += k * 0.650f;
		head.xRot += k * 0.350f;
		rWing.xRot += k * 1.350f;
		rWing.zRot += k * -1.150f;
		lWing.xRot += k * 1.350f;
		lWing.zRot += k * 1.150f;
		tail.xRot += k * -0.600f;
	}

	/** Ausrüstungs-Durchgang: -1 = Grundmodell (Ausrüstung unsichtbar), 0–2 = nur dieser Platz wird gezeichnet. */
	private int pass = -1;

	@Override
	public void equipmentPass(int slot) {
		pass = slot;
		java.util.List<java.util.List<ModelPart>> slots = java.util.List.of(java.util.List.of(collar), java.util.List.of(amulet), java.util.List.of(armorC, rArmor, lArmor));

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
