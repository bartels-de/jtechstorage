package dev.bartels.jtechstorage.client.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

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
 * Amethyst-Wichtel: Tiefenschiefer-Köpfchen mit großen leuchtenden Augen und Kristallkrone, kleine Robe mit
 * leuchtendem Kern-Kristall, Kristallhände und zwei schwebende Kristallsplitter-Flügel; unten ein Kristall-Zipfel.
 */
public class AmethystSpriteModel extends EntityModel<AmethystSpriteRenderState> implements ArmedModel<AmethystSpriteRenderState>, HelperHatModel {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(JTechStorage.id("amethyst_sprite"), "main");

	private final ModelPart sprite;
	private final ModelPart head;
	private final ModelPart body;
	private final ModelPart rightArm;
	private final ModelPart leftArm;
	private final ModelPart rightWing;
	private final ModelPart leftWing;
	private final ModelPart crownRight;
	private final ModelPart crownLeft;

	public AmethystSpriteModel(ModelPart root) {
		super(root);
		this.sprite = root.getChild("sprite");
		this.head = sprite.getChild("head");
		this.body = sprite.getChild("body");
		this.rightArm = body.getChild("right_arm");
		this.leftArm = body.getChild("left_arm");
		this.rightWing = body.getChild("right_wing");
		this.leftWing = body.getChild("left_wing");
		this.crownRight = head.getChild("crown_right");
		this.crownLeft = head.getChild("crown_left");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		PartDefinition p1 = root.addOrReplaceChild("sprite", CubeListBuilder.create(),
				PartPose.offset(0.0f, 23.5f, 0.0f));
		PartDefinition p2 = p1.addOrReplaceChild("head", CubeListBuilder.create()
				.texOffs(0, 0).addBox(-3.0f, -6.0f, -3.0f, 6.0f, 6.0f, 6.0f, new CubeDeformation(0.0f))
				.texOffs(24, 0).addBox(-1.0f, -10.0f, -1.0f, 2.0f, 4.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offset(0.0f, -8.0f, 0.0f));
		p2.addOrReplaceChild("crown_right", CubeListBuilder.create()
				.texOffs(32, 0).addBox(-0.5f, -3.0f, -0.5f, 1.0f, 3.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(-2.0f, -5.5f, 0.5f, 0.0f, 0.0f, -0.45f));
		p2.addOrReplaceChild("crown_left", CubeListBuilder.create()
				.texOffs(32, 0).mirror().addBox(-0.5f, -3.0f, -0.5f, 1.0f, 3.0f, 1.0f, new CubeDeformation(0.0f)).mirror(false),
				PartPose.offsetAndRotation(2.0f, -5.5f, -0.5f, 0.0f, 0.0f, 0.45f));
		p2.addOrReplaceChild("crown_back", CubeListBuilder.create()
				.texOffs(32, 0).addBox(-0.5f, -2.5f, -0.5f, 1.0f, 3.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.5f, -5.5f, 2.0f, -0.5f, 0.0f, 0.0f));
		PartDefinition p6 = p1.addOrReplaceChild("body", CubeListBuilder.create()
				.texOffs(0, 12).addBox(-2.0f, 0.0f, -1.5f, 4.0f, 4.0f, 3.0f, new CubeDeformation(0.0f))
				.texOffs(14, 12).addBox(-1.5f, 4.0f, -1.0f, 3.0f, 2.0f, 2.0f, new CubeDeformation(0.0f))
				.texOffs(24, 10).addBox(-0.5f, 6.0f, -0.5f, 1.0f, 3.0f, 1.0f, new CubeDeformation(0.0f))
				.texOffs(28, 10).addBox(-1.0f, 1.0f, -2.0f, 2.0f, 2.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, -8.0f, 0.0f, 0.1f, 0.0f, 0.0f));
		p6.addOrReplaceChild("right_arm", CubeListBuilder.create()
				.texOffs(40, 0).addBox(-0.5f, -0.5f, -0.5f, 1.0f, 4.0f, 1.0f, new CubeDeformation(0.0f))
				.texOffs(44, 0).addBox(-1.0f, 3.0f, -1.0f, 2.0f, 1.0f, 2.0f, new CubeDeformation(-0.2f)),
				PartPose.offsetAndRotation(-2.6f, 0.5f, 0.0f, 0.0f, 0.0f, 0.3f));
		p6.addOrReplaceChild("left_arm", CubeListBuilder.create()
				.texOffs(40, 0).mirror().addBox(-0.5f, -0.5f, -0.5f, 1.0f, 4.0f, 1.0f, new CubeDeformation(0.0f)).mirror(false)
				.texOffs(44, 0).mirror().addBox(-1.0f, 3.0f, -1.0f, 2.0f, 1.0f, 2.0f, new CubeDeformation(-0.2f)).mirror(false),
				PartPose.offsetAndRotation(2.6f, 0.5f, 0.0f, 0.0f, 0.0f, -0.3f));
		p6.addOrReplaceChild("right_wing", CubeListBuilder.create()
				.texOffs(0, 20).addBox(-6.0f, -4.0f, 0.0f, 6.0f, 6.0f, 0.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(-1.0f, 1.0f, 1.6f, 0.0f, 0.55f, 0.0f));
		p6.addOrReplaceChild("left_wing", CubeListBuilder.create()
				.texOffs(0, 20).mirror().addBox(0.0f, -4.0f, 0.0f, 6.0f, 6.0f, 0.0f, new CubeDeformation(0.0f)).mirror(false),
				PartPose.offsetAndRotation(1.0f, 1.0f, 1.6f, 0.0f, -0.55f, 0.0f));
		return LayerDefinition.create(mesh, 64, 32);
	}

	@Override
	public void setupAnim(AmethystSpriteRenderState state) {
		super.setupAnim(state);
		float age = state.ageInTicks;

		// Schweben und Wippen
		sprite.y += Mth.sin(age * 0.12f) * 0.8f;
		head.yRot = state.yRot * Mth.DEG_TO_RAD;
		head.xRot = state.xRot * Mth.DEG_TO_RAD;
		head.zRot = Mth.sin(age * 0.07f) * 0.05f;
		body.xRot = 0.1f + state.walkAnimationSpeed * 0.4f;
		crownRight.zRot += Mth.sin(age * 0.1f) * 0.05f;
		crownLeft.zRot -= Mth.sin(age * 0.1f) * 0.05f;

		// Kristallflügel schwirren
		float buzz = Mth.sin(age * 0.9f) * 0.25f;
		rightWing.yRot = 0.55f + buzz;
		leftWing.yRot = -0.55f - buzz;
		rightWing.zRot = Mth.sin(age * 0.2f) * 0.1f;
		leftWing.zRot = -Mth.sin(age * 0.2f) * 0.1f;

		if (state.carrying) {
			rightArm.xRot = -1.1f;
			leftArm.xRot = -1.1f;
			rightArm.yRot = 0.2f;
			leftArm.yRot = -0.2f;
			rightArm.zRot = 0.0f;
			leftArm.zRot = 0.0f;
		} else {
			rightArm.xRot = Mth.sin(age * 0.1f) * 0.15f;
			leftArm.xRot = -Mth.sin(age * 0.1f) * 0.15f;
		}

		if (state.resting) {
			head.xRot = 0.55f;
			rightWing.yRot = 1.1f;
			leftWing.yRot = -1.1f;
		}
	}

	@Override
	public void translateToHand(AmethystSpriteRenderState state, HumanoidArm arm, PoseStack pose) {
		root().translateAndRotate(pose);
		sprite.translateAndRotate(pose);
		body.translateAndRotate(pose);
		pose.translate(0.0f, 0.0625f, -0.05f);
		pose.rotate(Axis.XP, rightArm.xRot);
		pose.translate(0.0f, 0.2f, -0.05f);
		pose.scale(0.7f, 0.7f, 0.7f);
	}

	@Override
	public void translateToHat(PoseStack pose) {
		root().translateAndRotate(pose);
		sprite.translateAndRotate(pose);
		head.translateAndRotate(pose);
		pose.translate(0.0f, 0.0f, 0.0f);
	}

	@Override
	public float hatScale() {
		return 0.75f;
	}
}
