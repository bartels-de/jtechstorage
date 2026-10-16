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
 * Amethyst-Phönix: Feuerfedern, Flammenkamm mit Amethyst-Spitzen, dreiteilige Flügel mit Kristallen, lange
 * Schwanzfedern. Im Flug kräftiger Flügelschlag, sitzend angelegte Flügel.
 */
public class PhoenixModel extends EntityModel<PhoenixRenderState> implements HelperHatModel {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(JTechStorage.id("phoenix"), "main");

	private final ModelPart body;
	private final ModelPart neck;
	private final ModelPart head;
	private final ModelPart rightWing;
	private final ModelPart leftWing;
	private final ModelPart rightWingMid;
	private final ModelPart leftWingMid;
	private final ModelPart rightWingTip;
	private final ModelPart leftWingTip;
	private final ModelPart tail;
	private final ModelPart rightLeg;
	private final ModelPart leftLeg;

	public PhoenixModel(ModelPart root) {
		super(root);
		this.body = root.getChild("body");
		this.neck = body.getChild("neck");
		this.head = neck.getChild("head");
		this.rightWing = body.getChild("right_wing");
		this.leftWing = body.getChild("left_wing");
		this.rightWingMid = rightWing.getChild("right_wing_mid");
		this.leftWingMid = leftWing.getChild("left_wing_mid");
		this.rightWingTip = rightWingMid.getChild("right_wing_tip");
		this.leftWingTip = leftWingMid.getChild("left_wing_tip");
		this.tail = body.getChild("tail");
		this.rightLeg = root.getChild("right_leg");
		this.leftLeg = root.getChild("left_leg");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		PartDefinition p1 = root.addOrReplaceChild("body", CubeListBuilder.create()
				.texOffs(0, 0).addBox(-3.0f, -6.0f, -5.0f, 6.0f, 6.0f, 10.0f, new CubeDeformation(0.0f))
				.texOffs(102, 0).addBox(-2.5f, -5.0f, -7.0f, 5.0f, 4.0f, 3.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, 20.0f, 0.0f, -0.15f, 0.0f, 0.0f));
		PartDefinition p2 = p1.addOrReplaceChild("neck", CubeListBuilder.create()
				.texOffs(0, 16).addBox(-1.5f, -4.0f, -1.5f, 3.0f, 4.0f, 3.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, -4.5f, -4.5f, 0.3f, 0.0f, 0.0f));
		PartDefinition p3 = p2.addOrReplaceChild("head", CubeListBuilder.create()
				.texOffs(84, 0).addBox(-2.0f, -4.0f, -4.0f, 4.0f, 4.0f, 5.0f, new CubeDeformation(0.0f))
				.texOffs(0, 23).addBox(-1.0f, -2.0f, -6.5f, 2.0f, 2.0f, 3.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, -3.0f, -1.0f, 0.15f, 0.0f, 0.0f));
		p3.addOrReplaceChild("beak_tip", CubeListBuilder.create()
				.texOffs(44, 23).addBox(-0.5f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offset(0.0f, -1.0f, -6.5f));
		p3.addOrReplaceChild("crest_a", CubeListBuilder.create()
				.texOffs(80, 16).addBox(-0.5f, -5.0f, -0.5f, 1.0f, 5.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, -3.5f, 0.0f, -0.55f, 0.0f, 0.0f));
		p3.addOrReplaceChild("crest_b", CubeListBuilder.create()
				.texOffs(10, 23).addBox(-0.5f, -4.0f, -0.5f, 1.0f, 4.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(-1.0f, -3.5f, 0.5f, -0.8f, 0.0f, -0.25f));
		p3.addOrReplaceChild("crest_c", CubeListBuilder.create()
				.texOffs(14, 23).addBox(-0.5f, -4.0f, -0.5f, 1.0f, 3.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(1.0f, -3.5f, 0.5f, -0.8f, 0.0f, 0.25f));
		PartDefinition p8 = p1.addOrReplaceChild("right_wing", CubeListBuilder.create()
				.texOffs(12, 16).addBox(-12.0f, -1.0f, -2.0f, 12.0f, 2.0f, 5.0f, new CubeDeformation(0.0f)),
				PartPose.offset(-3.0f, -2.0f, -1.0f));
		PartDefinition p9 = p8.addOrReplaceChild("right_wing_mid", CubeListBuilder.create()
				.texOffs(46, 16).addBox(-11.0f, 0.0f, -1.0f, 11.0f, 1.0f, 6.0f, new CubeDeformation(0.0f)),
				PartPose.offset(0.0f, 0.5f, 2.5f));
		p9.addOrReplaceChild("right_wing_tip", CubeListBuilder.create()
				.texOffs(84, 16).addBox(-8.0f, 0.0f, -1.0f, 8.0f, 1.0f, 5.0f, new CubeDeformation(0.0f))
				.texOffs(34, 23).addBox(-10.0f, -0.5f, 0.0f, 3.0f, 1.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, 0.0f, 5.5f, 0.15f, 0.0f, 0.0f));
		PartDefinition p11 = p1.addOrReplaceChild("left_wing", CubeListBuilder.create()
				.texOffs(12, 16).mirror().addBox(0.0f, -1.0f, -2.0f, 12.0f, 2.0f, 5.0f, new CubeDeformation(0.0f)).mirror(false),
				PartPose.offset(3.0f, -2.0f, -1.0f));
		PartDefinition p12 = p11.addOrReplaceChild("left_wing_mid", CubeListBuilder.create()
				.texOffs(46, 16).mirror().addBox(0.0f, 0.0f, -1.0f, 11.0f, 1.0f, 6.0f, new CubeDeformation(0.0f)).mirror(false),
				PartPose.offset(0.0f, 0.5f, 2.5f));
		p12.addOrReplaceChild("left_wing_tip", CubeListBuilder.create()
				.texOffs(84, 16).mirror().addBox(0.0f, 0.0f, -1.0f, 8.0f, 1.0f, 5.0f, new CubeDeformation(0.0f)).mirror(false)
				.texOffs(34, 23).mirror().addBox(7.0f, -0.5f, 0.0f, 3.0f, 1.0f, 2.0f, new CubeDeformation(0.0f)).mirror(false),
				PartPose.offsetAndRotation(0.0f, 0.0f, 5.5f, 0.15f, 0.0f, 0.0f));
		PartDefinition p14 = p1.addOrReplaceChild("tail", CubeListBuilder.create()
				.texOffs(110, 16).addBox(-2.0f, -1.0f, 0.0f, 4.0f, 2.0f, 4.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, -3.0f, 4.5f, -0.2f, 0.0f, 0.0f));
		p14.addOrReplaceChild("tail_left", CubeListBuilder.create()
				.texOffs(32, 0).addBox(-1.0f, 0.0f, 0.0f, 2.0f, 1.0f, 12.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(1.2f, 0.0f, 3.0f, -0.1f, 0.25f, 0.0f));
		p14.addOrReplaceChild("tail_right", CubeListBuilder.create()
				.texOffs(32, 0).mirror().addBox(-1.0f, 0.0f, 0.0f, 2.0f, 1.0f, 12.0f, new CubeDeformation(0.0f)).mirror(false),
				PartPose.offsetAndRotation(-1.2f, 0.0f, 3.0f, -0.1f, -0.25f, 0.0f));
		p14.addOrReplaceChild("tail_center", CubeListBuilder.create()
				.texOffs(60, 0).addBox(-1.0f, 0.0f, 0.0f, 2.0f, 1.0f, 10.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, -0.6f, 3.0f, -0.25f, 0.0f, 0.0f));
		root.addOrReplaceChild("right_leg", CubeListBuilder.create()
				.texOffs(18, 23).addBox(-0.5f, 0.0f, -0.5f, 1.0f, 3.0f, 1.0f, new CubeDeformation(0.0f))
				.texOffs(22, 23).addBox(-1.5f, 3.0f, -2.0f, 3.0f, 1.0f, 3.0f, new CubeDeformation(0.0f)),
				PartPose.offset(-1.6f, 20.0f, 1.0f));
		root.addOrReplaceChild("left_leg", CubeListBuilder.create()
				.texOffs(18, 23).addBox(-0.5f, 0.0f, -0.5f, 1.0f, 3.0f, 1.0f, new CubeDeformation(0.0f))
				.texOffs(22, 23).addBox(-1.5f, 3.0f, -2.0f, 3.0f, 1.0f, 3.0f, new CubeDeformation(0.0f)),
				PartPose.offset(1.6f, 20.0f, 1.0f));
		return LayerDefinition.create(mesh, 128, 64);
	}

	@Override
	public void setupAnim(PhoenixRenderState state) {
		super.setupAnim(state);
		float age = state.ageInTicks;
		head.yRot = state.yRot * Mth.DEG_TO_RAD;
		head.xRot += state.xRot * Mth.DEG_TO_RAD * 0.5f;

		if (state.flying) {
			float flap = Mth.sin(age * 0.55f);
			rightWing.zRot = 0.15f + flap * 0.65f;
			leftWing.zRot = -0.15f - flap * 0.65f;
			rightWingMid.zRot = flap * 0.3f;
			leftWingMid.zRot = -flap * 0.3f;
			rightWingTip.zRot = flap * 0.2f;
			leftWingTip.zRot = -flap * 0.2f;
			body.xRot = -0.05f + Mth.sin(age * 0.55f + 1.0f) * 0.04f;
			tail.xRot = -0.05f + Mth.sin(age * 0.3f) * 0.08f;
			rightLeg.xRot = 1.2f;
			leftLeg.xRot = 1.2f;
		} else {
			// sitzend: Flügel angelegt, leichtes Kopfnicken
			rightWing.yRot = 1.25f;
			rightWing.zRot = -0.2f;
			leftWing.yRot = -1.25f;
			leftWing.zRot = 0.2f;
			neck.xRot += Mth.sin(age * 0.08f) * 0.06f;
			tail.xRot = -0.35f + Mth.sin(age * 0.05f) * 0.04f;
		}
	}

	@Override
	public void translateToHat(PoseStack pose) {
		root().translateAndRotate(pose);
		body.translateAndRotate(pose);
		neck.translateAndRotate(pose);
		head.translateAndRotate(pose);
		pose.translate(0.0f, -0.05f, -0.1f);
	}

	@Override
	public float hatScale() {
		return 0.5f;
	}
}
