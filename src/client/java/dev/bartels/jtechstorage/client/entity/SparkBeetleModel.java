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
 * Blitz-Käfer im Amethyst-Stil: Tiefenschiefer-Körper, gewölbte Kristall-Flügeldecken mit leuchtender Naht,
 * goldenes Blitzableiter-Horn mit Kristallspitze, sechs geknickte Beine. Beim Aufladen flattern die Flügeldecken.
 */
public class SparkBeetleModel extends EntityModel<SparkBeetleRenderState> implements HelperHatModel {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(JTechStorage.id("spark_beetle"), "main");

	private final ModelPart body;
	private final ModelPart head;
	private final ModelPart rightAntenna;
	private final ModelPart leftAntenna;
	private final ModelPart rightElytron;
	private final ModelPart leftElytron;
	private final ModelPart[] rightLegs = new ModelPart[3];
	private final ModelPart[] leftLegs = new ModelPart[3];

	public SparkBeetleModel(ModelPart root) {
		super(root);
		this.body = root.getChild("body");
		this.head = body.getChild("head");
		this.rightAntenna = head.getChild("right_antenna");
		this.leftAntenna = head.getChild("left_antenna");
		this.rightElytron = body.getChild("right_elytron");
		this.leftElytron = body.getChild("left_elytron");

		for (int i = 0; i < 3; i++) {
			rightLegs[i] = root.getChild("right_leg_" + i);
			leftLegs[i] = root.getChild("left_leg_" + i);
		}
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		PartDefinition p1 = root.addOrReplaceChild("body", CubeListBuilder.create()
				.texOffs(0, 0).addBox(-3.0f, -2.0f, -4.0f, 6.0f, 3.0f, 8.0f, new CubeDeformation(0.0f)),
				PartPose.offset(0.0f, 21.0f, 0.0f));
		PartDefinition p2 = p1.addOrReplaceChild("head", CubeListBuilder.create()
				.texOffs(28, 0).addBox(-2.0f, -1.5f, -3.0f, 4.0f, 3.0f, 3.0f, new CubeDeformation(0.0f))
				.texOffs(42, 0).addBox(-2.0f, 0.5f, -4.0f, 1.0f, 1.0f, 2.0f, new CubeDeformation(0.0f))
				.texOffs(42, 0).mirror().addBox(1.0f, 0.5f, -4.0f, 1.0f, 1.0f, 2.0f, new CubeDeformation(0.0f)).mirror(false),
				PartPose.offset(0.0f, -0.5f, -4.0f));
		p2.addOrReplaceChild("horn", CubeListBuilder.create()
				.texOffs(48, 0).addBox(-0.5f, -3.0f, -0.5f, 1.0f, 3.0f, 1.0f, new CubeDeformation(0.0f))
				.texOffs(52, 0).addBox(-1.0f, -5.0f, -1.0f, 2.0f, 2.0f, 2.0f, new CubeDeformation(-0.2f)),
				PartPose.offsetAndRotation(0.0f, -1.5f, -1.5f, 0.25f, 0.0f, 0.0f));
		p2.addOrReplaceChild("right_antenna", CubeListBuilder.create()
				.texOffs(42, 4).addBox(-0.5f, -3.0f, -0.5f, 1.0f, 3.0f, 1.0f, new CubeDeformation(-0.25f)),
				PartPose.offsetAndRotation(-1.5f, -1.5f, -2.5f, -0.6f, 0.0f, -0.5f));
		p2.addOrReplaceChild("left_antenna", CubeListBuilder.create()
				.texOffs(42, 4).mirror().addBox(-0.5f, -3.0f, -0.5f, 1.0f, 3.0f, 1.0f, new CubeDeformation(-0.25f)).mirror(false),
				PartPose.offsetAndRotation(1.5f, -1.5f, -2.5f, -0.6f, 0.0f, 0.5f));
		PartDefinition p6 = p1.addOrReplaceChild("right_elytron", CubeListBuilder.create()
				.texOffs(0, 11).addBox(-4.0f, -2.0f, 0.0f, 4.0f, 2.0f, 9.0f, new CubeDeformation(0.0f))
				.texOffs(38, 11).addBox(-3.5f, -3.0f, 1.0f, 3.0f, 1.0f, 7.0f, new CubeDeformation(0.0f)),
				PartPose.offset(0.0f, -1.5f, -4.5f));
		p6.addOrReplaceChild("right_elytron_crystal", CubeListBuilder.create()
				.texOffs(26, 11).addBox(-1.0f, -3.0f, -1.0f, 2.0f, 3.0f, 2.0f, new CubeDeformation(0.0f))
				.texOffs(34, 11).addBox(-2.0f, -2.0f, 1.5f, 1.0f, 2.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(-2.0f, -2.5f, 6.0f, 0.35f, 0.0f, -0.3f));
		PartDefinition p8 = p1.addOrReplaceChild("left_elytron", CubeListBuilder.create()
				.texOffs(0, 11).mirror().addBox(0.0f, -2.0f, 0.0f, 4.0f, 2.0f, 9.0f, new CubeDeformation(0.0f)).mirror(false)
				.texOffs(38, 11).mirror().addBox(0.5f, -3.0f, 1.0f, 3.0f, 1.0f, 7.0f, new CubeDeformation(0.0f)).mirror(false),
				PartPose.offset(0.0f, -1.5f, -4.5f));
		p8.addOrReplaceChild("left_elytron_crystal", CubeListBuilder.create()
				.texOffs(26, 11).mirror().addBox(-1.0f, -3.0f, -1.0f, 2.0f, 3.0f, 2.0f, new CubeDeformation(0.0f)).mirror(false)
				.texOffs(34, 11).addBox(1.0f, -2.0f, 1.5f, 1.0f, 2.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(2.0f, -2.5f, 6.0f, 0.35f, 0.0f, 0.3f));
		root.addOrReplaceChild("right_leg_0", CubeListBuilder.create()
				.texOffs(0, 22).addBox(-2.0f, -0.5f, -0.5f, 2.0f, 1.0f, 1.0f, new CubeDeformation(0.0f))
				.texOffs(6, 22).addBox(-2.5f, 0.0f, -0.5f, 1.0f, 3.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offset(-2.5f, 20.5f, -2.5f));
		root.addOrReplaceChild("left_leg_0", CubeListBuilder.create()
				.texOffs(0, 22).mirror().addBox(0.0f, -0.5f, -0.5f, 2.0f, 1.0f, 1.0f, new CubeDeformation(0.0f)).mirror(false)
				.texOffs(6, 22).mirror().addBox(1.5f, 0.0f, -0.5f, 1.0f, 3.0f, 1.0f, new CubeDeformation(0.0f)).mirror(false),
				PartPose.offset(2.5f, 20.5f, -2.5f));
		root.addOrReplaceChild("right_leg_1", CubeListBuilder.create()
				.texOffs(0, 22).addBox(-2.0f, -0.5f, -0.5f, 2.0f, 1.0f, 1.0f, new CubeDeformation(0.0f))
				.texOffs(6, 22).addBox(-2.5f, 0.0f, -0.5f, 1.0f, 3.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offset(-2.5f, 20.5f, 0.0f));
		root.addOrReplaceChild("left_leg_1", CubeListBuilder.create()
				.texOffs(0, 22).mirror().addBox(0.0f, -0.5f, -0.5f, 2.0f, 1.0f, 1.0f, new CubeDeformation(0.0f)).mirror(false)
				.texOffs(6, 22).mirror().addBox(1.5f, 0.0f, -0.5f, 1.0f, 3.0f, 1.0f, new CubeDeformation(0.0f)).mirror(false),
				PartPose.offset(2.5f, 20.5f, 0.0f));
		root.addOrReplaceChild("right_leg_2", CubeListBuilder.create()
				.texOffs(0, 22).addBox(-2.0f, -0.5f, -0.5f, 2.0f, 1.0f, 1.0f, new CubeDeformation(0.0f))
				.texOffs(6, 22).addBox(-2.5f, 0.0f, -0.5f, 1.0f, 3.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offset(-2.5f, 20.5f, 2.5f));
		root.addOrReplaceChild("left_leg_2", CubeListBuilder.create()
				.texOffs(0, 22).mirror().addBox(0.0f, -0.5f, -0.5f, 2.0f, 1.0f, 1.0f, new CubeDeformation(0.0f)).mirror(false)
				.texOffs(6, 22).mirror().addBox(1.5f, 0.0f, -0.5f, 1.0f, 3.0f, 1.0f, new CubeDeformation(0.0f)).mirror(false),
				PartPose.offset(2.5f, 20.5f, 2.5f));
		return LayerDefinition.create(mesh, 64, 32);
	}

	@Override
	public void setupAnim(SparkBeetleRenderState state) {
		super.setupAnim(state);
		float age = state.ageInTicks;
		head.yRot = Mth.clamp(state.yRot, -40.0f, 40.0f) * Mth.DEG_TO_RAD * 0.6f;
		rightAntenna.zRot += Mth.sin(age * 0.3f) * 0.15f;
		leftAntenna.zRot -= Mth.sin(age * 0.3f + 1.0f) * 0.15f;
		body.y += Mth.sin(age * 0.2f) * 0.15f;

		if (state.charging) {
			// Flügeldecken öffnen sich leicht und vibrieren
			float buzz = Mth.sin(age * 2.2f) * 0.06f;
			rightElytron.yRot = 0.18f + buzz;
			leftElytron.yRot = -0.18f - buzz;
			rightElytron.zRot = -0.25f;
			leftElytron.zRot = 0.25f;
		}

		float pos = state.walkAnimationPos * 1.6f;
		float speed = Math.min(state.walkAnimationSpeed, 1.0f);

		for (int i = 0; i < 3; i++) {
			float phase = (i % 2 == 0) ? 0.0f : Mth.PI;
			rightLegs[i].yRot = Mth.cos(pos + phase) * 0.6f * speed;
			leftLegs[i].yRot = Mth.cos(pos + phase + Mth.PI) * 0.6f * speed;
			rightLegs[i].zRot = Math.max(0.0f, Mth.sin(pos + phase)) * 0.4f * speed;
			leftLegs[i].zRot = -Math.max(0.0f, Mth.sin(pos + phase + Mth.PI)) * 0.4f * speed;
		}

		if (state.resting) {
			head.xRot = 0.35f;
		}
	}

	@Override
	public void translateToHat(PoseStack pose) {
		root().translateAndRotate(pose);
		body.translateAndRotate(pose);
		head.translateAndRotate(pose);
		pose.translate(0.0f, 2.0f / 16.0f, -1.5f / 16.0f);
	}

	@Override
	public float hatScale() {
		return 0.5f;
	}
}
