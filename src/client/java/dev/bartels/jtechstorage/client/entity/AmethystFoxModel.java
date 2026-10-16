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
 * Amethyst-Fuchs (Kristall-Kitsune): drei aufgefächerte Schweife mit leuchtenden Kristallspitzen, Kristallohren,
 * goldenes Stirnband, helle Halskrause, Rückenkristalle und drei kreisende Kristallsplitter.
 */
public class AmethystFoxModel extends EntityModel<AmethystFoxRenderState> implements HelperHatModel {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(JTechStorage.id("amethyst_fox"), "main");

	private final ModelPart body;
	private final ModelPart head;
	private final ModelPart rightEar;
	private final ModelPart leftEar;
	private final ModelPart[] tails = new ModelPart[3];
	private final ModelPart[] orbits = new ModelPart[3];
	private final ModelPart rightFront;
	private final ModelPart leftFront;
	private final ModelPart rightHind;
	private final ModelPart leftHind;

	public AmethystFoxModel(ModelPart root) {
		super(root);
		this.body = root.getChild("body");
		this.head = body.getChild("head");
		this.rightEar = head.getChild("right_ear");
		this.leftEar = head.getChild("left_ear");
		this.tails[0] = body.getChild("tail_center");
		this.tails[1] = body.getChild("tail_left");
		this.tails[2] = body.getChild("tail_right");

		for (int k = 0; k < 3; k++) {
			orbits[k] = body.getChild("orbit_" + k);
		}

		this.rightFront = root.getChild("right_front_leg");
		this.leftFront = root.getChild("left_front_leg");
		this.rightHind = root.getChild("right_hind_leg");
		this.leftHind = root.getChild("left_hind_leg");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		PartDefinition p1 = root.addOrReplaceChild("body", CubeListBuilder.create()
				.texOffs(0, 0).addBox(-3.0f, -3.0f, -6.0f, 6.0f, 6.0f, 11.0f, new CubeDeformation(0.0f))
				.texOffs(80, 0).addBox(-4.0f, -3.5f, -7.5f, 8.0f, 7.0f, 3.0f, new CubeDeformation(0.0f)),
				PartPose.offset(0.0f, 15.0f, 0.0f));
		PartDefinition p2 = p1.addOrReplaceChild("head", CubeListBuilder.create()
				.texOffs(34, 0).addBox(-3.5f, -3.0f, -5.0f, 7.0f, 6.0f, 5.0f, new CubeDeformation(0.0f))
				.texOffs(16, 17).addBox(-1.5f, -0.5f, -8.0f, 3.0f, 3.0f, 3.0f, new CubeDeformation(0.0f))
				.texOffs(68, 17).addBox(-0.5f, -0.8f, -8.4f, 1.0f, 1.0f, 1.0f, new CubeDeformation(0.0f))
				.texOffs(72, 17).addBox(-3.5f, -3.2f, -5.3f, 7.0f, 1.0f, 1.0f, new CubeDeformation(0.0f))
				.texOffs(88, 17).addBox(-0.5f, -3.6f, -5.6f, 1.0f, 1.0f, 1.0f, new CubeDeformation(0.0f))
				.texOffs(36, 17).addBox(-4.2f, -0.5f, -3.5f, 1.0f, 2.0f, 2.0f, new CubeDeformation(0.0f))
				.texOffs(36, 17).mirror().addBox(3.2f, -0.5f, -3.5f, 1.0f, 2.0f, 2.0f, new CubeDeformation(0.0f)).mirror(false),
				PartPose.offset(0.0f, -2.5f, -7.0f));
		PartDefinition p3 = p2.addOrReplaceChild("right_ear", CubeListBuilder.create()
				.texOffs(28, 17).addBox(-1.5f, -4.0f, -0.5f, 3.0f, 4.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(-2.0f, -3.0f, -2.2f, -0.1f, 0.0f, -0.28f));
		p3.addOrReplaceChild("right_ear_tip", CubeListBuilder.create()
				.texOffs(60, 17).addBox(-0.5f, -2.0f, -0.5f, 1.0f, 2.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offset(0.0f, -4.0f, 0.0f));
		PartDefinition p5 = p2.addOrReplaceChild("left_ear", CubeListBuilder.create()
				.texOffs(28, 17).mirror().addBox(-1.5f, -4.0f, -0.5f, 3.0f, 4.0f, 1.0f, new CubeDeformation(0.0f)).mirror(false),
				PartPose.offsetAndRotation(2.0f, -3.0f, -2.2f, -0.1f, 0.0f, 0.28f));
		p5.addOrReplaceChild("left_ear_tip", CubeListBuilder.create()
				.texOffs(60, 17).addBox(-0.5f, -2.0f, -0.5f, 1.0f, 2.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offset(0.0f, -4.0f, 0.0f));
		PartDefinition p7 = p1.addOrReplaceChild("tail_center", CubeListBuilder.create()
				.texOffs(0, 17).addBox(-1.5f, -1.5f, 0.0f, 3.0f, 3.0f, 5.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, -1.5f, 4.5f, 0.85f, 0.0f, 0.0f));
		PartDefinition p8 = p7.addOrReplaceChild("tail_center_mid", CubeListBuilder.create()
				.texOffs(58, 0).addBox(-2.5f, -2.5f, 0.0f, 5.0f, 5.0f, 6.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, 0.0f, 4.5f, -0.3f, 0.0f, 0.0f));
		p8.addOrReplaceChild("tail_center_tip", CubeListBuilder.create()
				.texOffs(102, 0).addBox(-2.0f, -2.0f, 0.0f, 4.0f, 4.0f, 5.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, 0.0f, 5.5f, -0.4f, 0.0f, 0.0f));
		PartDefinition p10 = p1.addOrReplaceChild("tail_left", CubeListBuilder.create()
				.texOffs(0, 17).addBox(-1.5f, -1.5f, 0.0f, 3.0f, 3.0f, 5.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, -1.5f, 4.5f, 0.85f, 0.6f, 0.0f));
		PartDefinition p11 = p10.addOrReplaceChild("tail_left_mid", CubeListBuilder.create()
				.texOffs(58, 0).addBox(-2.5f, -2.5f, 0.0f, 5.0f, 5.0f, 6.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, 0.0f, 4.5f, -0.3f, 0.0f, 0.0f));
		p11.addOrReplaceChild("tail_left_tip", CubeListBuilder.create()
				.texOffs(102, 0).addBox(-2.0f, -2.0f, 0.0f, 4.0f, 4.0f, 5.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, 0.0f, 5.5f, -0.4f, 0.0f, 0.0f));
		PartDefinition p13 = p1.addOrReplaceChild("tail_right", CubeListBuilder.create()
				.texOffs(0, 17).addBox(-1.5f, -1.5f, 0.0f, 3.0f, 3.0f, 5.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, -1.5f, 4.5f, 0.85f, -0.6f, 0.0f));
		PartDefinition p14 = p13.addOrReplaceChild("tail_right_mid", CubeListBuilder.create()
				.texOffs(58, 0).addBox(-2.5f, -2.5f, 0.0f, 5.0f, 5.0f, 6.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, 0.0f, 4.5f, -0.3f, 0.0f, 0.0f));
		p14.addOrReplaceChild("tail_right_tip", CubeListBuilder.create()
				.texOffs(102, 0).addBox(-2.0f, -2.0f, 0.0f, 4.0f, 4.0f, 5.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, 0.0f, 5.5f, -0.4f, 0.0f, 0.0f));
		p1.addOrReplaceChild("mane_spike_0", CubeListBuilder.create()
				.texOffs(56, 17).addBox(-0.5f, -3.0f, -0.5f, 1.0f, 3.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(-2.8f, -3.5f, -5.8f, -0.5f, 0.0f, -0.6f));
		p1.addOrReplaceChild("mane_spike_1", CubeListBuilder.create()
				.texOffs(56, 17).addBox(-0.5f, -3.0f, -0.5f, 1.0f, 3.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(2.8f, -3.5f, -5.8f, -0.5f, 0.0f, 0.6f));
		p1.addOrReplaceChild("back_crystal_0", CubeListBuilder.create()
				.texOffs(52, 17).addBox(-0.5f, -3.0f, -0.5f, 1.0f, 3.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(-0.8f, -3.0f, -1.5f, -0.2f, 0.0f, -0.3f));
		p1.addOrReplaceChild("back_crystal_1", CubeListBuilder.create()
				.texOffs(52, 17).addBox(-0.5f, -3.0f, -0.5f, 1.0f, 3.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.8f, -3.0f, 1.0f, 0.15f, 0.0f, 0.35f));
		p1.addOrReplaceChild("orbit_0", CubeListBuilder.create()
				.texOffs(64, 17).addBox(-0.5f, -1.0f, -0.5f, 1.0f, 2.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offset(6.0f, -6.0f, 0.0f));
		p1.addOrReplaceChild("orbit_1", CubeListBuilder.create()
				.texOffs(64, 17).addBox(-0.5f, -1.0f, -0.5f, 1.0f, 2.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offset(-2.977f, -6.0f, 5.209f));
		p1.addOrReplaceChild("orbit_2", CubeListBuilder.create()
				.texOffs(64, 17).addBox(-0.5f, -1.0f, -0.5f, 1.0f, 2.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offset(-3.046f, -6.0f, -5.17f));
		root.addOrReplaceChild("right_front_leg", CubeListBuilder.create()
				.texOffs(120, 0).addBox(-1.0f, 0.0f, -1.0f, 2.0f, 6.0f, 2.0f, new CubeDeformation(0.0f))
				.texOffs(42, 17).addBox(-1.0f, 5.0f, -2.0f, 2.0f, 1.0f, 3.0f, new CubeDeformation(0.0f)),
				PartPose.offset(-1.8f, 18.0f, -4.0f));
		root.addOrReplaceChild("left_front_leg", CubeListBuilder.create()
				.texOffs(120, 0).mirror().addBox(-1.0f, 0.0f, -1.0f, 2.0f, 6.0f, 2.0f, new CubeDeformation(0.0f)).mirror(false)
				.texOffs(42, 17).mirror().addBox(-1.0f, 5.0f, -2.0f, 2.0f, 1.0f, 3.0f, new CubeDeformation(0.0f)).mirror(false),
				PartPose.offset(1.8f, 18.0f, -4.0f));
		root.addOrReplaceChild("right_hind_leg", CubeListBuilder.create()
				.texOffs(120, 0).addBox(-1.0f, 0.0f, -1.0f, 2.0f, 6.0f, 2.0f, new CubeDeformation(0.0f))
				.texOffs(42, 17).addBox(-1.0f, 5.0f, -2.0f, 2.0f, 1.0f, 3.0f, new CubeDeformation(0.0f)),
				PartPose.offset(-1.8f, 18.0f, 4.0f));
		root.addOrReplaceChild("left_hind_leg", CubeListBuilder.create()
				.texOffs(120, 0).mirror().addBox(-1.0f, 0.0f, -1.0f, 2.0f, 6.0f, 2.0f, new CubeDeformation(0.0f)).mirror(false)
				.texOffs(42, 17).mirror().addBox(-1.0f, 5.0f, -2.0f, 2.0f, 1.0f, 3.0f, new CubeDeformation(0.0f)).mirror(false),
				PartPose.offset(1.8f, 18.0f, 4.0f));
		return LayerDefinition.create(mesh, 128, 64);
	}

	@Override
	public void setupAnim(AmethystFoxRenderState state) {
		super.setupAnim(state);
		float pos = state.walkAnimationPos;
		float speed = Math.min(state.walkAnimationSpeed, 1.0f);
		float age = state.ageInTicks;

		head.yRot = Mth.clamp(state.yRot, -50.0f, 50.0f) * Mth.DEG_TO_RAD;
		head.xRot = state.xRot * Mth.DEG_TO_RAD;

		// Trab
		rightFront.xRot = Mth.cos(pos * 0.9f) * 1.0f * speed;
		leftHind.xRot = Mth.cos(pos * 0.9f) * 1.0f * speed;
		leftFront.xRot = Mth.cos(pos * 0.9f + Mth.PI) * 1.0f * speed;
		rightHind.xRot = Mth.cos(pos * 0.9f + Mth.PI) * 1.0f * speed;

		// Schweife wiegen sich (gegeneinander versetzt), beim Laufen flacher
		for (int k = 0; k < 3; k++) {
			tails[k].yRot += Mth.sin(age * 0.09f + k * 2.1f) * 0.12f;
			tails[k].xRot += Mth.sin(age * 0.07f + k) * 0.06f - speed * 0.35f;
		}

		// kreisende Kristallsplitter
		for (int k = 0; k < 3; k++) {
			float angle = age * 0.06f + k * 2.094f;
			orbits[k].x = Mth.cos(angle) * 6.0f;
			orbits[k].z = Mth.sin(angle) * 6.0f;
			orbits[k].y = -6.0f + Mth.sin(age * 0.1f + k * 1.7f) * 1.0f;
			orbits[k].yRot = -angle;
			orbits[k].visible = !state.resting;
		}

		if (state.ready) {
			// spürt einen Schatz: Ohren zucken, Nase am Boden
			rightEar.zRot += Mth.sin(age * 0.8f) * 0.08f;
			leftEar.zRot -= Mth.sin(age * 0.8f) * 0.08f;
		}

		if (state.digging) {
			body.xRot = 0.35f;
			body.y += 1.0f;
			head.xRot = 0.6f;
			float dig = Mth.sin(age * 1.6f);
			rightFront.xRot = -0.6f + dig * 0.7f;
			leftFront.xRot = -0.6f - dig * 0.7f;
			for (ModelPart tail : tails) {
				tail.xRot += 0.25f;
			}
		}

		boolean legs = !state.resting;
		rightFront.visible = legs;
		leftFront.visible = legs;
		rightHind.visible = legs;
		leftHind.visible = legs;

		if (state.resting) {
			// eingerollt schlafen, Schweife als Decke
			body.y += 5.0f;
			head.xRot = 0.5f;
			head.yRot = 0.6f;
			head.y += 1.0f;
			tails[0].yRot = 1.5f;
			tails[0].xRot = -0.2f;
			tails[1].yRot = 1.9f;
			tails[1].xRot = -0.1f;
			tails[2].yRot = 1.1f;
			tails[2].xRot = -0.3f;
		}
	}

	@Override
	public void translateToHat(PoseStack pose) {
		root().translateAndRotate(pose);
		body.translateAndRotate(pose);
		head.translateAndRotate(pose);
		pose.translate(0.0f, -0.2f, -0.15f);
	}

	@Override
	public float hatScale() {
		return 0.55f;
	}
}
