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
 * Peter Chel Parker: Chels Heldenform – ein Amethyst-Ritter in Tiefenschiefer-Rüstung mit Goldsäumen,
 * Visierhelm mit leuchtenden Augen und Kristallkamm (Chels Spitzohren werden zu Helmflossen), leuchtendem
 * Kern-Kristall in der Brust, Schulterpanzern mit Kristallspitzen, Umhang mit Hammer-Wappen und Kriegshammer.
 */
public class PeterChelParkerModel extends EntityModel<PeterChelParkerRenderState> implements HelperHatModel {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(JTechStorage.id("peter_chel_parker"), "main");

	private final ModelPart head;
	private final ModelPart cape;
	private final ModelPart rightArm;
	private final ModelPart leftArm;
	private final ModelPart rightLeg;
	private final ModelPart leftLeg;
	private final ModelPart rightFin;
	private final ModelPart leftFin;

	public PeterChelParkerModel(ModelPart root) {
		super(root);
		this.head = root.getChild("head");
		this.cape = root.getChild("body").getChild("cape");
		this.rightArm = root.getChild("right_arm");
		this.leftArm = root.getChild("left_arm");
		this.rightLeg = root.getChild("right_leg");
		this.leftLeg = root.getChild("left_leg");
		this.rightFin = head.getChild("right_fin");
		this.leftFin = head.getChild("left_fin");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		PartDefinition p1 = root.addOrReplaceChild("head", CubeListBuilder.create()
				.texOffs(0, 0).addBox(-3.5f, -7.0f, -3.5f, 7.0f, 7.0f, 7.0f, new CubeDeformation(0.0f))
				.texOffs(28, 0).addBox(-1.0f, -11.0f, -2.5f, 2.0f, 5.0f, 3.0f, new CubeDeformation(0.0f)),
				PartPose.offset(0.0f, 2.0f, 0.0f));
		p1.addOrReplaceChild("crest_right", CubeListBuilder.create()
				.texOffs(38, 0).addBox(-0.5f, -3.0f, -1.0f, 1.0f, 3.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(-2.0f, -6.5f, 0.5f, 0.0f, 0.0f, -0.4f));
		p1.addOrReplaceChild("crest_left", CubeListBuilder.create()
				.texOffs(38, 0).mirror().addBox(-0.5f, -3.0f, -1.0f, 1.0f, 3.0f, 2.0f, new CubeDeformation(0.0f)).mirror(false),
				PartPose.offsetAndRotation(2.0f, -6.5f, 0.5f, 0.0f, 0.0f, 0.4f));
		p1.addOrReplaceChild("right_fin", CubeListBuilder.create()
				.texOffs(44, 0).addBox(-1.0f, -2.0f, -1.5f, 1.0f, 4.0f, 3.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(-3.6f, -4.0f, 0.5f, 0.0f, -0.45f, -0.35f));
		p1.addOrReplaceChild("left_fin", CubeListBuilder.create()
				.texOffs(44, 0).mirror().addBox(0.0f, -2.0f, -1.5f, 1.0f, 4.0f, 3.0f, new CubeDeformation(0.0f)).mirror(false),
				PartPose.offsetAndRotation(3.6f, -4.0f, 0.5f, 0.0f, 0.45f, 0.35f));
		PartDefinition p6 = root.addOrReplaceChild("body", CubeListBuilder.create()
				.texOffs(0, 14).addBox(-4.5f, -10.0f, -2.5f, 9.0f, 10.0f, 5.0f, new CubeDeformation(0.0f))
				.texOffs(78, 14).addBox(-5.0f, -2.0f, -3.0f, 10.0f, 2.0f, 6.0f, new CubeDeformation(0.0f))
				.texOffs(52, 0).addBox(-1.5f, -8.0f, -3.3f, 3.0f, 3.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offset(0.0f, 12.0f, 0.0f));
		p6.addOrReplaceChild("cape", CubeListBuilder.create()
				.texOffs(30, 30).addBox(-5.0f, 0.0f, 0.0f, 10.0f, 16.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, -9.8f, 2.6f, 0.08f, 0.0f, 0.0f));
		PartDefinition p8 = root.addOrReplaceChild("right_arm", CubeListBuilder.create()
				.texOffs(28, 14).addBox(-2.5f, -2.5f, -3.0f, 5.0f, 4.0f, 6.0f, new CubeDeformation(0.0f))
				.texOffs(50, 14).addBox(-1.5f, -1.0f, -1.5f, 3.0f, 9.0f, 3.0f, new CubeDeformation(0.0f))
				.texOffs(62, 14).addBox(-2.0f, 6.0f, -2.0f, 4.0f, 4.0f, 4.0f, new CubeDeformation(0.0f)),
				PartPose.offset(-6.0f, 3.0f, 0.0f));
		p8.addOrReplaceChild("right_arm_spike", CubeListBuilder.create()
				.texOffs(60, 0).addBox(-0.5f, -3.0f, -0.5f, 1.0f, 3.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(-1.5f, -2.5f, 0.0f, 0.0f, 0.0f, -0.4f));
		p8.addOrReplaceChild("hammer", CubeListBuilder.create()
				.texOffs(52, 30).addBox(-0.5f, -3.0f, -0.5f, 1.0f, 14.0f, 1.0f, new CubeDeformation(0.0f))
				.texOffs(56, 30).addBox(-3.0f, 9.0f, -2.0f, 6.0f, 4.0f, 4.0f, new CubeDeformation(0.3f))
				.texOffs(76, 30).addBox(-4.0f, 10.0f, -0.5f, 1.0f, 2.0f, 1.0f, new CubeDeformation(0.0f))
				.texOffs(76, 30).mirror().addBox(3.0f, 10.0f, -0.5f, 1.0f, 2.0f, 1.0f, new CubeDeformation(0.0f)).mirror(false)
				.texOffs(92, 30).addBox(-1.0f, -6.0f, -1.0f, 2.0f, 3.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offset(0.0f, 8.5f, -0.5f));
		PartDefinition p11 = root.addOrReplaceChild("left_arm", CubeListBuilder.create()
				.texOffs(28, 14).mirror().addBox(-2.5f, -2.5f, -3.0f, 5.0f, 4.0f, 6.0f, new CubeDeformation(0.0f)).mirror(false)
				.texOffs(50, 14).mirror().addBox(-1.5f, -1.0f, -1.5f, 3.0f, 9.0f, 3.0f, new CubeDeformation(0.0f)).mirror(false)
				.texOffs(62, 14).mirror().addBox(-2.0f, 6.0f, -2.0f, 4.0f, 4.0f, 4.0f, new CubeDeformation(0.0f)).mirror(false),
				PartPose.offset(6.0f, 3.0f, 0.0f));
		p11.addOrReplaceChild("left_arm_spike", CubeListBuilder.create()
				.texOffs(60, 0).addBox(-0.5f, -3.0f, -0.5f, 1.0f, 3.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(1.5f, -2.5f, 0.0f, 0.0f, 0.0f, 0.4f));
		root.addOrReplaceChild("right_leg", CubeListBuilder.create()
				.texOffs(0, 30).addBox(-1.5f, 0.0f, -1.5f, 3.0f, 10.0f, 3.0f, new CubeDeformation(0.0f))
				.texOffs(12, 30).addBox(-2.0f, 9.0f, -2.6f, 4.0f, 3.0f, 5.0f, new CubeDeformation(0.0f))
				.texOffs(80, 30).addBox(-2.0f, 3.0f, -2.2f, 4.0f, 2.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offset(-2.3f, 12.0f, 0.0f));
		root.addOrReplaceChild("left_leg", CubeListBuilder.create()
				.texOffs(0, 30).mirror().addBox(-1.5f, 0.0f, -1.5f, 3.0f, 10.0f, 3.0f, new CubeDeformation(0.0f)).mirror(false)
				.texOffs(12, 30).mirror().addBox(-2.0f, 9.0f, -2.6f, 4.0f, 3.0f, 5.0f, new CubeDeformation(0.0f)).mirror(false)
				.texOffs(80, 30).mirror().addBox(-2.0f, 3.0f, -2.2f, 4.0f, 2.0f, 2.0f, new CubeDeformation(0.0f)).mirror(false),
				PartPose.offset(2.3f, 12.0f, 0.0f));
		return LayerDefinition.create(mesh, 128, 64);
	}

	@Override
	public void setupAnim(PeterChelParkerRenderState state) {
		super.setupAnim(state);
		float age = state.ageInTicks;
		float pos = state.walkAnimationPos;
		float speed = Math.min(state.walkAnimationSpeed, 1.0f);

		head.yRot = state.yRot * Mth.DEG_TO_RAD;
		head.xRot = state.xRot * Mth.DEG_TO_RAD;
		rightFin.yRot += Mth.sin(age * 0.05f) * 0.05f;
		leftFin.yRot -= Mth.sin(age * 0.05f) * 0.05f;

		rightLeg.xRot = Mth.cos(pos * 0.6662f) * 1.1f * speed;
		leftLeg.xRot = Mth.cos(pos * 0.6662f + Mth.PI) * 1.1f * speed;
		leftArm.xRot = Mth.cos(pos * 0.6662f) * 0.8f * speed;
		leftArm.zRot = -0.06f - Mth.sin(age * 0.06f) * 0.03f;
		rightArm.xRot = Mth.cos(pos * 0.6662f + Mth.PI) * 0.3f * speed;
		rightArm.zRot = 0.06f;

		// Umhang weht beim Laufen und leicht im Stand
		cape.xRot = 0.08f + speed * 0.55f + Mth.sin(age * 0.07f) * 0.04f;

		float attack = state.attackProgress;

		if (attack > 0.0f) {
			// ausholen über den Kopf, dann Schlag nach unten
			if (attack < 0.45f) {
				rightArm.xRot = -2.7f * (attack / 0.45f);
			} else {
				float t = (attack - 0.45f) / 0.55f;
				rightArm.xRot = -2.7f + 3.0f * Math.min(1.0f, t * 1.6f);
			}

			rightArm.zRot = 0.15f;
			cape.xRot += 0.25f;
		}
	}

	@Override
	public void translateToHat(PoseStack pose) {
		root().translateAndRotate(pose);
		head.translateAndRotate(pose);
	}

	@Override
	public float hatScale() {
		return 0.875f;
	}
}
