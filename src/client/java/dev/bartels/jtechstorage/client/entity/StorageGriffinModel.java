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
 * Lager-Greif im Amethyst-Stil: Adler-Vorderteil mit Amethyst-Gefieder, Kristallkrone und goldenem Schnabel,
 * Tiefenschiefer-Löwenkörper mit leuchtenden Amethyst-Adern, zweiteilige Flügel, Sattel und Truhe.
 */
public class StorageGriffinModel extends EntityModel<StorageGriffinRenderState> implements HelperHatModel {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(JTechStorage.id("storage_griffin"), "main");

	private final ModelPart neck;
	private final ModelPart head;
	private final ModelPart tail;
	private final ModelPart tailTip;
	private final ModelPart rightWing;
	private final ModelPart rightWingTip;
	private final ModelPart leftWing;
	private final ModelPart leftWingTip;
	private final ModelPart rightFrontLeg;
	private final ModelPart leftFrontLeg;
	private final ModelPart rightHindLeg;
	private final ModelPart leftHindLeg;

	public StorageGriffinModel(ModelPart root) {
		super(root);
		ModelPart body = root.getChild("body");
		this.neck = root.getChild("neck");
		this.head = neck.getChild("head");
		this.tail = body.getChild("tail");
		this.tailTip = tail.getChild("tail_tip");
		this.rightWing = body.getChild("right_wing");
		this.rightWingTip = rightWing.getChild("right_wing_tip");
		this.leftWing = body.getChild("left_wing");
		this.leftWingTip = leftWing.getChild("left_wing_tip");
		this.rightFrontLeg = root.getChild("right_front_leg");
		this.leftFrontLeg = root.getChild("left_front_leg");
		this.rightHindLeg = root.getChild("right_hind_leg");
		this.leftHindLeg = root.getChild("left_hind_leg");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		PartDefinition p1 = root.addOrReplaceChild("body", CubeListBuilder.create()
				.texOffs(0, 0).addBox(-4.0f, 0.0f, -6.0f, 8.0f, 7.0f, 12.0f, new CubeDeformation(0.0f))
				.texOffs(40, 0).addBox(-4.5f, -0.5f, -7.5f, 9.0f, 6.0f, 3.0f, new CubeDeformation(0.0f))
				.texOffs(64, 0).addBox(-4.5f, -0.6f, -4.0f, 9.0f, 1.0f, 5.0f, new CubeDeformation(0.0f))
				.texOffs(92, 0).addBox(-3.0f, -4.5f, 1.5f, 6.0f, 4.0f, 5.0f, new CubeDeformation(0.0f)),
				PartPose.offset(0.0f, 11.0f, 0.0f));
		p1.addOrReplaceChild("right_crystal", CubeListBuilder.create()
				.texOffs(64, 22).addBox(-1.0f, -3.0f, -1.0f, 2.0f, 3.0f, 2.0f, new CubeDeformation(0.0f))
				.texOffs(72, 22).addBox(-0.5f, -2.0f, 1.0f, 1.0f, 2.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(-3.5f, 0.5f, -5.5f, 0.0f, 0.0f, -0.5f));
		p1.addOrReplaceChild("left_crystal", CubeListBuilder.create()
				.texOffs(64, 22).mirror().addBox(-1.0f, -3.0f, -1.0f, 2.0f, 3.0f, 2.0f, new CubeDeformation(0.0f)).mirror(false)
				.texOffs(72, 22).addBox(-0.5f, -2.0f, 1.0f, 1.0f, 2.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(3.5f, 0.5f, -5.5f, 0.0f, 0.0f, 0.5f));
		PartDefinition p4 = p1.addOrReplaceChild("tail", CubeListBuilder.create()
				.texOffs(32, 22).addBox(-1.0f, -1.0f, 0.0f, 2.0f, 2.0f, 7.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, 1.5f, 6.0f, 0.5f, 0.0f, 0.0f));
		p4.addOrReplaceChild("tail_tip", CubeListBuilder.create()
				.texOffs(50, 22).addBox(-1.5f, -1.5f, 0.0f, 3.0f, 3.0f, 2.0f, new CubeDeformation(0.0f))
				.texOffs(60, 22).addBox(-0.5f, -3.5f, 0.5f, 1.0f, 3.0f, 1.0f, new CubeDeformation(0.0f))
				.texOffs(60, 22).addBox(-0.5f, -0.5f, 1.5f, 1.0f, 1.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offset(0.0f, 0.0f, 6.5f));
		PartDefinition p6 = p1.addOrReplaceChild("right_wing", CubeListBuilder.create()
				.texOffs(0, 32).addBox(-9.0f, 0.0f, 0.0f, 9.0f, 0.0f, 10.0f, new CubeDeformation(0.0f)),
				PartPose.offset(-4.0f, 0.5f, -4.5f));
		p6.addOrReplaceChild("right_wing_tip", CubeListBuilder.create()
				.texOffs(0, 44).addBox(-9.0f, 0.0f, 0.0f, 9.0f, 0.0f, 11.0f, new CubeDeformation(0.0f)),
				PartPose.offset(-9.0f, 0.0f, 0.0f));
		PartDefinition p8 = p1.addOrReplaceChild("left_wing", CubeListBuilder.create()
				.texOffs(0, 32).mirror().addBox(0.0f, 0.0f, 0.0f, 9.0f, 0.0f, 10.0f, new CubeDeformation(0.0f)).mirror(false),
				PartPose.offset(4.0f, 0.5f, -4.5f));
		p8.addOrReplaceChild("left_wing_tip", CubeListBuilder.create()
				.texOffs(0, 44).mirror().addBox(0.0f, 0.0f, 0.0f, 9.0f, 0.0f, 11.0f, new CubeDeformation(0.0f)).mirror(false),
				PartPose.offset(9.0f, 0.0f, 0.0f));
		PartDefinition p10 = root.addOrReplaceChild("neck", CubeListBuilder.create()
				.texOffs(40, 10).addBox(-2.5f, -6.0f, -3.0f, 5.0f, 6.0f, 5.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, 12.0f, -6.0f, 0.35f, 0.0f, 0.0f));
		p10.addOrReplaceChild("head", CubeListBuilder.create()
				.texOffs(60, 10).addBox(-3.0f, -5.0f, -4.5f, 6.0f, 5.0f, 6.0f, new CubeDeformation(0.0f))
				.texOffs(84, 10).addBox(-1.0f, -3.0f, -7.5f, 2.0f, 2.0f, 3.0f, new CubeDeformation(0.0f))
				.texOffs(94, 10).addBox(-1.0f, -1.0f, -7.5f, 2.0f, 1.0f, 1.0f, new CubeDeformation(0.0f))
				.texOffs(100, 10).addBox(-0.5f, -8.0f, -1.5f, 1.0f, 3.0f, 1.0f, new CubeDeformation(0.0f))
				.texOffs(104, 10).addBox(-2.0f, -7.0f, -0.5f, 1.0f, 2.0f, 1.0f, new CubeDeformation(0.0f))
				.texOffs(104, 10).addBox(1.0f, -7.0f, 0.0f, 1.0f, 2.0f, 1.0f, new CubeDeformation(0.0f))
				.texOffs(108, 10).addBox(-3.5f, -3.0f, 0.0f, 1.0f, 2.0f, 2.0f, new CubeDeformation(0.0f))
				.texOffs(108, 10).mirror().addBox(2.5f, -3.0f, 0.0f, 1.0f, 2.0f, 2.0f, new CubeDeformation(0.0f)).mirror(false),
				PartPose.offsetAndRotation(0.0f, -6.0f, -0.5f, -0.35f, 0.0f, 0.0f));
		root.addOrReplaceChild("right_front_leg", CubeListBuilder.create()
				.texOffs(0, 20).addBox(-1.0f, 0.0f, -1.0f, 2.0f, 6.0f, 2.0f, new CubeDeformation(0.0f))
				.texOffs(8, 20).addBox(-1.5f, 5.0f, -2.2f, 3.0f, 1.0f, 3.0f, new CubeDeformation(0.0f)),
				PartPose.offset(-2.5f, 18.0f, -4.0f));
		root.addOrReplaceChild("left_front_leg", CubeListBuilder.create()
				.texOffs(0, 20).mirror().addBox(-1.0f, 0.0f, -1.0f, 2.0f, 6.0f, 2.0f, new CubeDeformation(0.0f)).mirror(false)
				.texOffs(8, 20).mirror().addBox(-1.5f, 5.0f, -2.2f, 3.0f, 1.0f, 3.0f, new CubeDeformation(0.0f)).mirror(false),
				PartPose.offset(2.5f, 18.0f, -4.0f));
		root.addOrReplaceChild("right_hind_leg", CubeListBuilder.create()
				.texOffs(20, 20).addBox(-1.5f, 0.0f, -1.5f, 3.0f, 7.0f, 3.0f, new CubeDeformation(0.0f)),
				PartPose.offset(-2.5f, 17.0f, 4.0f));
		root.addOrReplaceChild("left_hind_leg", CubeListBuilder.create()
				.texOffs(20, 20).mirror().addBox(-1.5f, 0.0f, -1.5f, 3.0f, 7.0f, 3.0f, new CubeDeformation(0.0f)).mirror(false),
				PartPose.offset(2.5f, 17.0f, 4.0f));
		return LayerDefinition.create(mesh, 128, 64);
	}

	@Override
	public void setupAnim(StorageGriffinRenderState state) {
		super.setupAnim(state);
		float age = state.ageInTicks;
		float headYaw = state.ridden ? 0.0f : Mth.clamp(state.yRot, -50.0f, 50.0f) * Mth.DEG_TO_RAD;
		neck.yRot = headYaw * 0.4f;
		head.yRot = headYaw * 0.6f;
		head.xRot = -0.35f + state.xRot * Mth.DEG_TO_RAD * 0.6f;
		tail.yRot = Mth.sin(age * 0.09f) * 0.25f;
		tailTip.yRot = Mth.sin(age * 0.09f - 0.8f) * 0.3f;

		if (state.flying) {
			float flap = Mth.sin(age * 0.35f) * 0.75f + 0.1f;
			float tipFlap = Mth.sin(age * 0.35f - 0.7f) * 0.45f;
			rightWing.yRot = 0.0f;
			leftWing.yRot = 0.0f;
			rightWing.zRot = flap;
			leftWing.zRot = -flap;
			rightWingTip.yRot = 0.0f;
			leftWingTip.yRot = 0.0f;
			rightWingTip.zRot = tipFlap;
			leftWingTip.zRot = -tipFlap;
			rightFrontLeg.xRot = leftFrontLeg.xRot = -1.1f;
			rightHindLeg.xRot = leftHindLeg.xRot = 1.2f;
			tail.xRot = 0.15f;
			neck.xRot = 0.6f;
			head.xRot -= 0.25f;
		} else {
			float breathe = Mth.sin(age * 0.07f) * 0.04f;
			rightWing.yRot = 1.4f;
			leftWing.yRot = -1.4f;
			rightWing.zRot = 0.35f + breathe;
			leftWing.zRot = -0.35f - breathe;
			rightWingTip.yRot = 0.2f;
			leftWingTip.yRot = -0.2f;
			rightWingTip.zRot = 0.1f;
			leftWingTip.zRot = -0.1f;
			float pos = state.walkAnimationPos;
			float speed = Math.min(state.walkAnimationSpeed, 1.0f);
			rightFrontLeg.xRot = leftHindLeg.xRot = Mth.cos(pos * 0.6662f) * 1.1f * speed;
			leftFrontLeg.xRot = rightHindLeg.xRot = Mth.cos(pos * 0.6662f + Mth.PI) * 1.1f * speed;
			tail.xRot = 0.5f + Mth.cos(pos * 0.6662f) * 0.2f * speed;
			neck.xRot = 0.35f;
		}

		if (state.resting) {
			neck.xRot = 0.9f;
			head.xRot = 0.4f;
		}
	}

	@Override
	public void translateToHat(PoseStack pose) {
		root().translateAndRotate(pose);
		neck.translateAndRotate(pose);
		head.translateAndRotate(pose);
		pose.translate(0.0f, 0.5f / 16.0f, -1.5f / 16.0f);
	}

	@Override
	public float hatScale() {
		return 0.75f;
	}
}
