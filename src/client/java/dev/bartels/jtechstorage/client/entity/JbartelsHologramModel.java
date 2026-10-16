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
 * Jbartels als Hologramm: Amethyst-Hoodie mit Kapuze und Kristall-Emblem, Holo-Brille, Mantelschoß,
 * Holo-Tablet in der linken Hand, Projektionsring unter den Füßen.
 */
public class JbartelsHologramModel extends EntityModel<JbartelsHologramRenderState> implements ArmedModel<JbartelsHologramRenderState> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(JTechStorage.id("jbartels_hologram"), "main");

	private final ModelPart head;
	private final ModelPart rightArm;
	private final ModelPart leftArm;
	private final ModelPart base;

	public JbartelsHologramModel(ModelPart root) {
		super(root);
		this.head = root.getChild("head");
		this.rightArm = root.getChild("right_arm");
		this.leftArm = root.getChild("left_arm");
		this.base = root.getChild("base");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		PartDefinition p1 = root.addOrReplaceChild("head", CubeListBuilder.create()
				.texOffs(0, 0).addBox(-4.0f, -8.0f, -4.0f, 8.0f, 8.0f, 8.0f, new CubeDeformation(0.0f))
				.texOffs(32, 0).addBox(-4.0f, -8.0f, -4.0f, 8.0f, 8.0f, 8.0f, new CubeDeformation(0.5f)),
				PartPose.ZERO);
		p1.addOrReplaceChild("goggles", CubeListBuilder.create()
				.texOffs(18, 32).addBox(-4.5f, -5.5f, -4.7f, 9.0f, 2.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.ZERO);
		PartDefinition p3 = root.addOrReplaceChild("body", CubeListBuilder.create()
				.texOffs(16, 16).addBox(-4.0f, 0.0f, -2.0f, 8.0f, 12.0f, 4.0f, new CubeDeformation(0.0f)),
				PartPose.ZERO);
		p3.addOrReplaceChild("coat", CubeListBuilder.create()
				.texOffs(0, 32).addBox(-4.0f, 0.0f, 0.0f, 8.0f, 10.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, 9.0f, 1.6f, 0.1f, 0.0f, 0.0f));
		root.addOrReplaceChild("right_arm", CubeListBuilder.create()
				.texOffs(40, 16).addBox(-3.0f, -2.0f, -2.0f, 4.0f, 12.0f, 4.0f, new CubeDeformation(0.0f)),
				PartPose.offset(-5.0f, 2.0f, 0.0f));
		PartDefinition p6 = root.addOrReplaceChild("left_arm", CubeListBuilder.create()
				.texOffs(40, 16).mirror().addBox(-1.0f, -2.0f, -2.0f, 4.0f, 12.0f, 4.0f, new CubeDeformation(0.0f)).mirror(false),
				PartPose.offset(5.0f, 2.0f, 0.0f));
		p6.addOrReplaceChild("tablet", CubeListBuilder.create()
				.texOffs(38, 32).addBox(-2.5f, -2.0f, -0.5f, 5.0f, 4.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(1.0f, 9.0f, -2.5f, -1.1f, 0.0f, 0.0f));
		root.addOrReplaceChild("right_leg", CubeListBuilder.create()
				.texOffs(0, 16).addBox(-2.0f, 0.0f, -2.0f, 4.0f, 12.0f, 4.0f, new CubeDeformation(0.0f)),
				PartPose.offset(-1.9f, 12.0f, 0.0f));
		root.addOrReplaceChild("left_leg", CubeListBuilder.create()
				.texOffs(0, 16).mirror().addBox(-2.0f, 0.0f, -2.0f, 4.0f, 12.0f, 4.0f, new CubeDeformation(0.0f)).mirror(false),
				PartPose.offset(1.9f, 12.0f, 0.0f));
		root.addOrReplaceChild("base", CubeListBuilder.create()
				.texOffs(0, 44).addBox(-7.0f, 0.0f, -7.0f, 14.0f, 0.0f, 14.0f, new CubeDeformation(0.0f)),
				PartPose.offset(0.0f, 23.9f, 0.0f));
		return LayerDefinition.create(mesh, 64, 64);
	}

	@Override
	public void setupAnim(JbartelsHologramRenderState state) {
		super.setupAnim(state);
		float age = state.ageInTicks;
		head.yRot = state.yRot * Mth.DEG_TO_RAD;
		head.xRot = state.xRot * Mth.DEG_TO_RAD + Mth.sin(age * 0.25f) * 0.04f;
		// erklärt gestikulierend
		rightArm.xRot = -0.6f + Mth.sin(age * 0.18f) * 0.35f;
		rightArm.zRot = 0.15f + Mth.sin(age * 0.11f) * 0.1f;
		leftArm.xRot = -0.35f;
		leftArm.zRot = -0.1f;
		base.yRot = age * 0.04f;
		root().y += Mth.sin(age * 0.08f) * 0.6f - 0.6f;
	}

	@Override
	public void translateToHand(JbartelsHologramRenderState state, HumanoidArm arm, PoseStack pose) {
		root().translateAndRotate(pose);
		rightArm.translateAndRotate(pose);
		pose.translate(-0.0625f, 0.0f, 0.0f);
	}
}
