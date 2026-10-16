package dev.bartels.jtechstorage.client.entity;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.ArmedModel;
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
import net.minecraft.world.entity.HumanoidArm;

import dev.bartels.jtechstorage.JTechStorage;

/**
 * Sammel-Falter: pelziger Tiefenschiefer-Körper mit hellem Pelzkragen, leuchtende Facettenaugen, gefiederte
 * Amethyst-Fühler, große Vorderflügel mit leuchtenden Augenflecken und kleine Hinterflügel.
 */
public class CollectorMothModel extends EntityModel<CollectorMothRenderState> implements ArmedModel<CollectorMothRenderState>, HelperHatModel {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(JTechStorage.id("collector_moth"), "main");

	private final ModelPart moth;
	private final ModelPart body;
	private final ModelPart rightWing;
	private final ModelPart leftWing;
	private final ModelPart rightHindWing;
	private final ModelPart leftHindWing;
	private final ModelPart rightAntenna;
	private final ModelPart leftAntenna;

	public CollectorMothModel(ModelPart root) {
		super(root);
		this.moth = root.getChild("moth");
		this.body = moth.getChild("body");
		this.rightWing = body.getChild("right_wing");
		this.leftWing = body.getChild("left_wing");
		this.rightHindWing = body.getChild("right_hind_wing");
		this.leftHindWing = body.getChild("left_hind_wing");
		ModelPart head = body.getChild("head");
		this.rightAntenna = head.getChild("right_antenna");
		this.leftAntenna = head.getChild("left_antenna");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		PartDefinition p1 = root.addOrReplaceChild("moth", CubeListBuilder.create(),
				PartPose.offset(0.0f, 20.0f, 0.0f));
		PartDefinition p2 = p1.addOrReplaceChild("body", CubeListBuilder.create()
				.texOffs(0, 0).addBox(-1.5f, -1.5f, -3.0f, 3.0f, 3.0f, 6.0f, new CubeDeformation(0.0f))
				.texOffs(18, 0).addBox(-1.0f, -1.0f, 3.0f, 2.0f, 2.0f, 4.0f, new CubeDeformation(0.0f))
				.texOffs(30, 0).addBox(-2.0f, -2.0f, -3.5f, 4.0f, 4.0f, 2.0f, new CubeDeformation(0.1f)),
				PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f, 0.1f, 0.0f, 0.0f));
		PartDefinition p3 = p2.addOrReplaceChild("head", CubeListBuilder.create()
				.texOffs(0, 9).addBox(-1.5f, -1.5f, -2.0f, 3.0f, 3.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offset(0.0f, 0.0f, -3.5f));
		p3.addOrReplaceChild("right_antenna", CubeListBuilder.create()
				.texOffs(10, 9).addBox(-3.0f, -4.0f, 0.0f, 3.0f, 4.0f, 0.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(-0.7f, -1.2f, -1.5f, -0.35f, 0.2f, -0.3f));
		p3.addOrReplaceChild("left_antenna", CubeListBuilder.create()
				.texOffs(10, 9).mirror().addBox(0.0f, -4.0f, 0.0f, 3.0f, 4.0f, 0.0f, new CubeDeformation(0.0f)).mirror(false),
				PartPose.offsetAndRotation(0.7f, -1.2f, -1.5f, -0.35f, -0.2f, 0.3f));
		p2.addOrReplaceChild("right_wing", CubeListBuilder.create()
				.texOffs(0, 14).addBox(-9.0f, 0.0f, -3.0f, 9.0f, 0.0f, 8.0f, new CubeDeformation(0.0f)),
				PartPose.offset(-1.5f, -1.5f, -1.0f));
		p2.addOrReplaceChild("left_wing", CubeListBuilder.create()
				.texOffs(0, 14).mirror().addBox(0.0f, 0.0f, -3.0f, 9.0f, 0.0f, 8.0f, new CubeDeformation(0.0f)).mirror(false),
				PartPose.offset(1.5f, -1.5f, -1.0f));
		p2.addOrReplaceChild("right_hind_wing", CubeListBuilder.create()
				.texOffs(0, 22).addBox(-6.0f, 0.0f, 0.0f, 6.0f, 0.0f, 5.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(-1.2f, -1.0f, 2.0f, 0.0f, -0.35f, 0.0f));
		p2.addOrReplaceChild("left_hind_wing", CubeListBuilder.create()
				.texOffs(0, 22).mirror().addBox(0.0f, 0.0f, 0.0f, 6.0f, 0.0f, 5.0f, new CubeDeformation(0.0f)).mirror(false),
				PartPose.offsetAndRotation(1.2f, -1.0f, 2.0f, 0.0f, 0.35f, 0.0f));
		return LayerDefinition.create(mesh, 64, 32);
	}

	@Override
	public void setupAnim(CollectorMothRenderState state) {
		super.setupAnim(state);
		float age = state.ageInTicks;
		float flap = Mth.sin(age * 0.9f) * 0.65f + 0.15f;
		float hindFlap = Mth.sin(age * 0.9f - 0.5f) * 0.45f + 0.1f;

		moth.y += Mth.sin(age * 0.15f) * 1.0f;
		body.xRot = state.carrying ? 0.3f : 0.1f;
		rightWing.zRot = flap;
		leftWing.zRot = -flap;
		rightHindWing.zRot = hindFlap;
		leftHindWing.zRot = -hindFlap;
		rightAntenna.xRot += Mth.sin(age * 0.2f) * 0.1f;
		leftAntenna.xRot += Mth.sin(age * 0.2f + 1.0f) * 0.1f;

		if (state.resting) {
			// Flügel zusammengefaltet über dem Rücken
			rightWing.zRot = -1.3f;
			leftWing.zRot = 1.3f;
			rightHindWing.zRot = -1.1f;
			leftHindWing.zRot = 1.1f;
			body.xRot = 0.35f;
		}
	}

	@Override
	public void translateToHand(CollectorMothRenderState state, HumanoidArm arm, PoseStack pose) {
		root().translateAndRotate(pose);
		moth.translateAndRotate(pose);
		body.translateAndRotate(pose);
		pose.translate(0.0f, 0.12f, -0.02f);
		pose.scale(0.6f, 0.6f, 0.6f);
	}

	@Override
	public void translateToHat(PoseStack pose) {
		root().translateAndRotate(pose);
		moth.translateAndRotate(pose);
		body.translateAndRotate(pose);
		body.getChild("head").translateAndRotate(pose);
		pose.translate(0.0f, 1.6f / 16.0f, -1.0f / 16.0f);
	}

	@Override
	public float hatScale() {
		return 0.4f;
	}
}
