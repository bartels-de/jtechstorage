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
 * Chel: kleiner Tiefenschiefer-Kobold mit riesigem Kopf, schiefem Chaos-Blick, Spitzohren, Kristall-Strubbelhaaren,
 * einer Mini-Werkbank als Hut, Latzschürze und Amethyst-Hammer.
 */
public class ChelModel extends EntityModel<ChelRenderState> implements HelperHatModel {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(JTechStorage.id("chel"), "main");

	private final ModelPart head;
	private final ModelPart hat;
	private final ModelPart rightEar;
	private final ModelPart leftEar;
	private final ModelPart rightArm;
	private final ModelPart leftArm;
	private final ModelPart rightLeg;
	private final ModelPart leftLeg;

	public ChelModel(ModelPart root) {
		super(root);
		this.head = root.getChild("head");
		this.hat = head.getChild("hat");
		this.rightEar = head.getChild("right_ear");
		this.leftEar = head.getChild("left_ear");
		this.rightArm = root.getChild("right_arm");
		this.leftArm = root.getChild("left_arm");
		this.rightLeg = root.getChild("right_leg");
		this.leftLeg = root.getChild("left_leg");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		PartDefinition p1 = root.addOrReplaceChild("head", CubeListBuilder.create()
				.texOffs(0, 0).addBox(-4.0f, -7.0f, -3.5f, 8.0f, 7.0f, 7.0f, new CubeDeformation(0.0f))
				.texOffs(56, 8).addBox(-1.0f, -3.5f, -4.5f, 2.0f, 2.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offset(0.0f, 15.0f, 0.0f));
		p1.addOrReplaceChild("hat", CubeListBuilder.create()
				.texOffs(32, 0).addBox(-3.0f, -4.0f, -3.0f, 6.0f, 4.0f, 6.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.5f, -7.0f, 0.0f, 0.0f, 0.2f, 0.18f));
		p1.addOrReplaceChild("right_ear", CubeListBuilder.create()
				.texOffs(0, 26).addBox(-1.0f, -1.5f, -1.0f, 1.0f, 3.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(-4.0f, -4.0f, 0.0f, 0.0f, 0.0f, -0.7f));
		p1.addOrReplaceChild("left_ear", CubeListBuilder.create()
				.texOffs(0, 26).mirror().addBox(0.0f, -1.5f, -1.0f, 1.0f, 3.0f, 2.0f, new CubeDeformation(0.0f)).mirror(false),
				PartPose.offsetAndRotation(4.0f, -4.0f, 0.0f, 0.0f, 0.0f, 0.7f));
		p1.addOrReplaceChild("hair", CubeListBuilder.create()
				.texOffs(56, 0).addBox(-0.5f, -3.0f, -0.5f, 1.0f, 3.0f, 1.0f, new CubeDeformation(0.0f))
				.texOffs(60, 0).addBox(-0.5f, -2.0f, -0.5f, 1.0f, 2.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(-3.0f, -6.5f, 2.5f, -0.4f, 0.0f, -0.5f));
		p1.addOrReplaceChild("hair2", CubeListBuilder.create()
				.texOffs(60, 0).addBox(-0.5f, -2.0f, -0.5f, 1.0f, 2.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(3.2f, -6.5f, 2.8f, -0.3f, 0.0f, 0.6f));
		root.addOrReplaceChild("body", CubeListBuilder.create()
				.texOffs(0, 16).addBox(-3.0f, -6.0f, -2.0f, 6.0f, 6.0f, 4.0f, new CubeDeformation(0.0f)),
				PartPose.offset(0.0f, 21.0f, 0.0f));
		PartDefinition p8 = root.addOrReplaceChild("right_arm", CubeListBuilder.create()
				.texOffs(24, 16).addBox(-1.0f, -0.5f, -1.0f, 2.0f, 5.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(-4.0f, 15.5f, 0.0f, 0.0f, 0.0f, 0.15f));
		p8.addOrReplaceChild("hammer", CubeListBuilder.create()
				.texOffs(40, 16).addBox(-0.5f, 0.0f, -0.5f, 1.0f, 4.0f, 1.0f, new CubeDeformation(0.0f))
				.texOffs(44, 16).addBox(-1.5f, 3.0f, -1.0f, 3.0f, 2.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, 4.0f, 0.0f, -1.2f, 0.0f, 0.0f));
		root.addOrReplaceChild("left_arm", CubeListBuilder.create()
				.texOffs(24, 16).mirror().addBox(-1.0f, -0.5f, -1.0f, 2.0f, 5.0f, 2.0f, new CubeDeformation(0.0f)).mirror(false),
				PartPose.offsetAndRotation(4.0f, 15.5f, 0.0f, 0.0f, 0.0f, -0.15f));
		root.addOrReplaceChild("right_leg", CubeListBuilder.create()
				.texOffs(32, 16).addBox(-1.0f, 0.0f, -1.0f, 2.0f, 3.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offset(-1.5f, 21.0f, 0.0f));
		root.addOrReplaceChild("left_leg", CubeListBuilder.create()
				.texOffs(32, 16).mirror().addBox(-1.0f, 0.0f, -1.0f, 2.0f, 3.0f, 2.0f, new CubeDeformation(0.0f)).mirror(false),
				PartPose.offset(1.5f, 21.0f, 0.0f));
		return LayerDefinition.create(mesh, 64, 64);
	}

	@Override
	public void setupAnim(ChelRenderState state) {
		super.setupAnim(state);
		float age = state.ageInTicks;
		float pos = state.walkAnimationPos;
		float speed = Math.min(state.walkAnimationSpeed, 1.0f);

		head.yRot = state.yRot * Mth.DEG_TO_RAD;
		head.xRot = state.xRot * Mth.DEG_TO_RAD;
		// Chaos: Kopf wackelt, Hut tanzt, Ohren zucken
		head.zRot = Mth.sin(age * 0.15f) * 0.12f;
		hat.visible = state.headItem.isEmpty();
		hat.zRot += Mth.sin(age * 0.21f) * 0.1f;
		hat.y -= Math.max(0.0f, Mth.sin(age * 0.3f)) * 0.6f;
		rightEar.zRot += Mth.sin(age * 0.5f) * 0.12f;
		leftEar.zRot -= Mth.sin(age * 0.45f + 1.0f) * 0.12f;

		rightLeg.xRot = Mth.cos(pos * 0.6662f) * 1.3f * speed;
		leftLeg.xRot = Mth.cos(pos * 0.6662f + Mth.PI) * 1.3f * speed;
		leftArm.xRot = Mth.cos(pos * 0.6662f) * 1.0f * speed;
		leftArm.zRot += Mth.sin(age * 0.12f) * 0.08f;

		if (state.crafting) {
			// hämmern!
			rightArm.xRot = -0.9f + Mth.sin(age * 1.1f) * 0.7f;
			rightArm.yRot = 0.15f;
			head.xRot += 0.35f;
			rightEar.zRot += Mth.sin(age * 1.1f) * 0.2f;
			leftEar.zRot -= Mth.sin(age * 1.1f) * 0.2f;
		} else {
			rightArm.xRot = Mth.cos(pos * 0.6662f + Mth.PI) * 1.0f * speed + Mth.sin(age * 0.1f) * 0.06f;
		}

		if (state.resting) {
			head.xRot = 0.6f;
			head.zRot = 0.25f;
		}
	}

	@Override
	public void translateToHat(PoseStack pose) {
		root().translateAndRotate(pose);
		head.translateAndRotate(pose);
		pose.translate(0.0f, 0.5f / 16.0f, 0.0f);
	}

	@Override
	public float hatScale() {
		return 1.0f;
	}
}
