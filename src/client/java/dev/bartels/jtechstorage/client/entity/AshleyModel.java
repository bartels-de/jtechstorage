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
 * Ashley: ein zotteliger Rußball mit Ascheflecken, Glutfunken im Fell, verschlafenen Augen, Stummelärmchen,
 * zerzausten Haarbüscheln, einem eingewachsenen Amethyst-Splitter – und zwei Fliegen, die um sie kreisen.
 */
public class AshleyModel extends EntityModel<AshleyRenderState> implements HelperHatModel {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(JTechStorage.id("ashley"), "main");

	private final ModelPart body;
	private final ModelPart flies;
	private final ModelPart rightArm;
	private final ModelPart leftArm;
	private final ModelPart rightFoot;
	private final ModelPart leftFoot;
	private final ModelPart tuftA;
	private final ModelPart tuftB;

	public AshleyModel(ModelPart root) {
		super(root);
		this.body = root.getChild("body");
		this.flies = body.getChild("flies");
		this.rightArm = body.getChild("right_arm");
		this.leftArm = body.getChild("left_arm");
		this.rightFoot = root.getChild("right_foot");
		this.leftFoot = root.getChild("left_foot");
		this.tuftA = body.getChild("tuft_a");
		this.tuftB = body.getChild("tuft_b");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		PartDefinition p1 = root.addOrReplaceChild("body", CubeListBuilder.create()
				.texOffs(0, 0).addBox(-4.0f, -7.0f, -4.0f, 8.0f, 7.0f, 8.0f, new CubeDeformation(0.0f)),
				PartPose.offset(0.0f, 22.0f, 0.0f));
		p1.addOrReplaceChild("tuft_a", CubeListBuilder.create()
				.texOffs(40, 0).addBox(-1.0f, -3.0f, -1.0f, 2.0f, 3.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(-2.0f, -7.0f, -1.0f, 0.3f, 0.0f, -0.4f));
		p1.addOrReplaceChild("tuft_b", CubeListBuilder.create()
				.texOffs(40, 0).addBox(-1.0f, -3.0f, -1.0f, 2.0f, 3.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(1.5f, -7.0f, 0.5f, -0.2f, 0.0f, 0.5f));
		p1.addOrReplaceChild("tuft_c", CubeListBuilder.create()
				.texOffs(40, 0).addBox(-1.0f, -2.0f, -1.0f, 2.0f, 2.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, -7.0f, 2.5f, -0.6f, 0.0f, 0.1f));
		p1.addOrReplaceChild("crystal", CubeListBuilder.create()
				.texOffs(48, 0).addBox(-0.5f, -3.0f, -0.5f, 1.0f, 3.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(2.5f, -6.5f, 2.5f, -0.3f, 0.0f, 0.4f));
		p1.addOrReplaceChild("right_fluff", CubeListBuilder.create()
				.texOffs(0, 15).addBox(-1.0f, -1.5f, -1.5f, 1.0f, 3.0f, 3.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(-4.0f, -3.0f, 0.0f, 0.0f, 0.0f, -0.3f));
		p1.addOrReplaceChild("left_fluff", CubeListBuilder.create()
				.texOffs(0, 15).mirror().addBox(0.0f, -1.5f, -1.5f, 1.0f, 3.0f, 3.0f, new CubeDeformation(0.0f)).mirror(false),
				PartPose.offsetAndRotation(4.0f, -3.0f, 0.0f, 0.0f, 0.0f, 0.3f));
		p1.addOrReplaceChild("right_arm", CubeListBuilder.create()
				.texOffs(52, 0).addBox(-1.0f, 0.0f, -1.0f, 2.0f, 3.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(-4.5f, -3.0f, -1.0f, 0.0f, 0.0f, 0.35f));
		p1.addOrReplaceChild("left_arm", CubeListBuilder.create()
				.texOffs(52, 0).mirror().addBox(-1.0f, 0.0f, -1.0f, 2.0f, 3.0f, 2.0f, new CubeDeformation(0.0f)).mirror(false),
				PartPose.offsetAndRotation(4.5f, -3.0f, -1.0f, 0.0f, 0.0f, -0.35f));
		PartDefinition p10 = p1.addOrReplaceChild("flies", CubeListBuilder.create(),
				PartPose.offset(0.0f, -10.0f, 0.0f));
		p10.addOrReplaceChild("fly_a", CubeListBuilder.create()
				.texOffs(8, 15).addBox(-0.5f, -0.5f, -0.5f, 1.0f, 1.0f, 1.0f, new CubeDeformation(-0.15f)),
				PartPose.offset(3.5f, 0.0f, 0.0f));
		p10.addOrReplaceChild("fly_b", CubeListBuilder.create()
				.texOffs(8, 15).addBox(-0.5f, -0.5f, -0.5f, 1.0f, 1.0f, 1.0f, new CubeDeformation(-0.15f)),
				PartPose.offset(-3.0f, -1.0f, 1.5f));
		root.addOrReplaceChild("right_foot", CubeListBuilder.create()
				.texOffs(32, 0).addBox(-1.0f, 0.0f, -1.5f, 2.0f, 2.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offset(-2.0f, 22.0f, -0.5f));
		root.addOrReplaceChild("left_foot", CubeListBuilder.create()
				.texOffs(32, 0).mirror().addBox(-1.0f, 0.0f, -1.5f, 2.0f, 2.0f, 2.0f, new CubeDeformation(0.0f)).mirror(false),
				PartPose.offset(2.0f, 22.0f, -0.5f));
		return LayerDefinition.create(mesh, 64, 32);
	}

	@Override
	public void setupAnim(AshleyRenderState state) {
		super.setupAnim(state);
		float age = state.ageInTicks;
		float pos = state.walkAnimationPos;
		float speed = Math.min(state.walkAnimationSpeed, 1.0f);

		// watscheln
		body.zRot = Mth.sin(pos * 0.6662f) * 0.18f * speed;
		body.yRot = state.yRot * Mth.DEG_TO_RAD * 0.5f;
		rightFoot.xRot = Mth.cos(pos * 0.6662f) * 1.2f * speed;
		leftFoot.xRot = Mth.cos(pos * 0.6662f + Mth.PI) * 1.2f * speed;
		rightArm.zRot += Mth.sin(age * 0.15f) * 0.1f;
		leftArm.zRot -= Mth.sin(age * 0.15f) * 0.1f;
		tuftA.zRot += Mth.sin(age * 0.11f) * 0.08f;
		tuftB.zRot -= Mth.sin(age * 0.13f) * 0.08f;

		// die Fliegen kreisen immer
		flies.yRot = age * 0.25f;
		flies.y += Mth.sin(age * 0.4f) * 0.5f;

		if (state.resting) {
			body.y += 0.8f;
			body.xRot = 0.15f;
			rightArm.zRot = 0.9f;
			leftArm.zRot = -0.9f;
		}
	}

	@Override
	public void translateToHat(PoseStack pose) {
		root().translateAndRotate(pose);
		body.translateAndRotate(pose);
		pose.translate(0.0f, (-3.5f + 4.0f) / 16.0f, 0.0f);
	}

	@Override
	public float hatScale() {
		return 1.0f;
	}
}
