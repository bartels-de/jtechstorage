package dev.bartels.jtechstorage.client.entity;

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
import dev.bartels.jtechstorage.entity.AmethystRobot;

/**
 * Amethyst-Roboter v2: Hörnerkrone, goldgefasster Reaktorkern, Kristall-Schulterdornen, rotierender Energiering,
 * große Kristall-Pylone, zerfetzter Umhang, Laserkanone mit Glühspulen (rechts), Kristall-Panzerfaust (links),
 * Kniedornen und Goldkanten.
 */
public class AmethystRobotModel extends EntityModel<AmethystRobotRenderState> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(JTechStorage.id("amethyst_robot"), "main");

	private final ModelPart body;
	private final ModelPart head;
	private final ModelPart rightArm;
	private final ModelPart leftArm;
	private final ModelPart rightLeg;
	private final ModelPart leftLeg;
	private final ModelPart thrusterLeft;
	private final ModelPart thrusterRight;
	private final ModelPart ring;
	private final ModelPart cape;
	private final ModelPart pylonLeft;
	private final ModelPart pylonRight;

	public AmethystRobotModel(ModelPart root) {
		super(root);
		this.body = root.getChild("body");
		this.head = body.getChild("head");
		this.rightArm = body.getChild("right_arm");
		this.leftArm = body.getChild("left_arm");
		this.thrusterLeft = body.getChild("thruster_l");
		this.thrusterRight = body.getChild("thruster_r");
		this.ring = body.getChild("ring");
		this.cape = body.getChild("cape");
		this.pylonLeft = body.getChild("pylon_l");
		this.pylonRight = body.getChild("pylon_r");
		this.rightLeg = root.getChild("right_leg");
		this.leftLeg = root.getChild("left_leg");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		PartDefinition p1 = root.addOrReplaceChild("body", CubeListBuilder.create()
				.texOffs(44, 0).addBox(-9.0f, -15.0f, -5.5f, 18.0f, 10.0f, 11.0f, new CubeDeformation(0.0f))
				.texOffs(64, 40).addBox(-6.0f, -5.0f, -4.0f, 12.0f, 5.0f, 8.0f, new CubeDeformation(0.0f))
				.texOffs(36, 68).addBox(-7.0f, -2.0f, -4.5f, 14.0f, 2.0f, 9.0f, new CubeDeformation(0.0f))
				.texOffs(82, 68).addBox(-5.0f, -13.5f, -6.0f, 10.0f, 10.0f, 1.0f, new CubeDeformation(0.0f))
				.texOffs(34, 80).addBox(-4.0f, -12.5f, -6.6f, 8.0f, 8.0f, 1.0f, new CubeDeformation(0.0f))
				.texOffs(0, 56).addBox(-6.0f, -17.0f, -4.5f, 12.0f, 3.0f, 9.0f, new CubeDeformation(0.0f)),
				PartPose.offset(0.0f, 11.0f, 0.0f));
		PartDefinition p2 = p1.addOrReplaceChild("head", CubeListBuilder.create()
				.texOffs(32, 40).addBox(-4.0f, -7.0f, -4.5f, 8.0f, 7.0f, 8.0f, new CubeDeformation(0.0f))
				.texOffs(66, 90).addBox(-3.0f, -3.0f, -5.0f, 6.0f, 3.0f, 1.0f, new CubeDeformation(0.0f))
				.texOffs(84, 90).addBox(-3.0f, -5.0f, -5.0f, 6.0f, 1.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offset(0.0f, -15.0f, -1.0f));
		PartDefinition p3 = p2.addOrReplaceChild("horn_r", CubeListBuilder.create()
				.texOffs(52, 80).addBox(-1.0f, -7.0f, -1.0f, 2.0f, 7.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(-3.2f, -5.5f, 0.0f, -0.35f, 0.0f, -0.45f));
		p3.addOrReplaceChild("horn_r_tip", CubeListBuilder.create()
				.texOffs(38, 90).addBox(-0.5f, -4.0f, -0.5f, 1.0f, 4.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, -6.5f, 0.0f, -0.35f, 0.0f, -0.2f));
		PartDefinition p5 = p2.addOrReplaceChild("horn_l", CubeListBuilder.create()
				.texOffs(52, 80).mirror().addBox(-1.0f, -7.0f, -1.0f, 2.0f, 7.0f, 2.0f, new CubeDeformation(0.0f)).mirror(false),
				PartPose.offsetAndRotation(3.2f, -5.5f, 0.0f, -0.35f, 0.0f, 0.45f));
		p5.addOrReplaceChild("horn_l_tip", CubeListBuilder.create()
				.texOffs(38, 90).addBox(-0.5f, -4.0f, -0.5f, 1.0f, 4.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, -6.5f, 0.0f, -0.35f, 0.0f, 0.2f));
		p2.addOrReplaceChild("crest", CubeListBuilder.create()
				.texOffs(60, 80).addBox(-1.0f, -5.0f, -2.0f, 2.0f, 5.0f, 4.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, -6.5f, 0.5f, -0.3f, 0.0f, 0.0f));
		PartDefinition p8 = p1.addOrReplaceChild("pylon_r", CubeListBuilder.create()
				.texOffs(28, 22).addBox(-1.5f, -14.0f, -1.5f, 3.0f, 14.0f, 3.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(-5.0f, -10.0f, 5.5f, -0.3f, 0.0f, -0.35f));
		p8.addOrReplaceChild("pylon_r_tip", CubeListBuilder.create()
				.texOffs(118, 80).addBox(-1.0f, -5.0f, -1.0f, 2.0f, 5.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offset(0.0f, -14.0f, 0.0f));
		PartDefinition p10 = p1.addOrReplaceChild("pylon_l", CubeListBuilder.create()
				.texOffs(28, 22).addBox(-1.5f, -14.0f, -1.5f, 3.0f, 14.0f, 3.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(5.0f, -10.0f, 5.5f, -0.3f, 0.0f, 0.35f));
		p10.addOrReplaceChild("pylon_l_tip", CubeListBuilder.create()
				.texOffs(118, 80).addBox(-1.0f, -5.0f, -1.0f, 2.0f, 5.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offset(0.0f, -14.0f, 0.0f));
		p1.addOrReplaceChild("ring", CubeListBuilder.create()
				.texOffs(0, 0).addBox(-11.0f, -11.0f, 0.0f, 22.0f, 22.0f, 0.0f, new CubeDeformation(0.0f)),
				PartPose.offset(0.0f, -9.0f, 7.5f));
		p1.addOrReplaceChild("cape", CubeListBuilder.create()
				.texOffs(0, 22).addBox(-7.0f, 0.0f, 0.0f, 14.0f, 18.0f, 0.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, -15.0f, 5.8f, 0.12f, 0.0f, 0.0f));
		p1.addOrReplaceChild("thruster_l", CubeListBuilder.create()
				.texOffs(72, 80).addBox(-2.0f, 0.0f, 0.0f, 4.0f, 6.0f, 3.0f, new CubeDeformation(0.0f))
				.texOffs(42, 90).addBox(-1.5f, 6.0f, 0.0f, 3.0f, 2.0f, 3.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(4.5f, -8.0f, 5.5f, 0.3f, 0.0f, 0.0f));
		p1.addOrReplaceChild("thruster_r", CubeListBuilder.create()
				.texOffs(72, 80).mirror().addBox(-2.0f, 0.0f, 0.0f, 4.0f, 6.0f, 3.0f, new CubeDeformation(0.0f)).mirror(false)
				.texOffs(42, 90).mirror().addBox(-1.5f, 6.0f, 0.0f, 3.0f, 2.0f, 3.0f, new CubeDeformation(0.0f)).mirror(false),
				PartPose.offsetAndRotation(-4.5f, -8.0f, 5.5f, 0.3f, 0.0f, 0.0f));
		PartDefinition p16 = p1.addOrReplaceChild("right_arm", CubeListBuilder.create()
				.texOffs(70, 22).addBox(-4.5f, -4.0f, -5.0f, 9.0f, 6.0f, 10.0f, new CubeDeformation(0.0f))
				.texOffs(42, 56).addBox(-2.5f, 2.0f, -2.5f, 5.0f, 7.0f, 5.0f, new CubeDeformation(0.0f))
				.texOffs(86, 80).addBox(-3.0f, 8.0f, -3.0f, 6.0f, 3.0f, 6.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(-12.0f, -13.0f, 0.0f, 0.0f, 0.0f, 0.1f));
		p16.addOrReplaceChild("right_spike_a", CubeListBuilder.create()
				.texOffs(110, 80).addBox(-1.0f, -6.0f, -1.0f, 2.0f, 6.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(-2.5f, -4.0f, -1.5f, -0.2f, 0.0f, -0.5f));
		p16.addOrReplaceChild("right_spike_b", CubeListBuilder.create()
				.texOffs(54, 90).addBox(-0.5f, -4.0f, -0.5f, 1.0f, 4.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(-0.5f, -4.0f, 2.5f, 0.35f, 0.0f, -0.3f));
		p16.addOrReplaceChild("cannon", CubeListBuilder.create()
				.texOffs(40, 22).addBox(-3.5f, 0.0f, -4.0f, 7.0f, 9.0f, 8.0f, new CubeDeformation(0.0f))
				.texOffs(0, 80).addBox(-4.0f, 2.0f, -4.5f, 8.0f, 1.0f, 9.0f, new CubeDeformation(0.0f))
				.texOffs(0, 80).addBox(-4.0f, 5.0f, -4.5f, 8.0f, 1.0f, 9.0f, new CubeDeformation(0.0f))
				.texOffs(0, 90).addBox(-2.0f, 9.0f, -2.0f, 4.0f, 3.0f, 4.0f, new CubeDeformation(0.0f)),
				PartPose.offset(0.0f, 11.0f, 0.0f));
		PartDefinition p20 = p1.addOrReplaceChild("left_arm", CubeListBuilder.create()
				.texOffs(70, 22).mirror().addBox(-4.5f, -4.0f, -5.0f, 9.0f, 6.0f, 10.0f, new CubeDeformation(0.0f)).mirror(false)
				.texOffs(42, 56).mirror().addBox(-2.5f, 2.0f, -2.5f, 5.0f, 7.0f, 5.0f, new CubeDeformation(0.0f)).mirror(false)
				.texOffs(86, 80).mirror().addBox(-3.0f, 8.0f, -3.0f, 6.0f, 3.0f, 6.0f, new CubeDeformation(0.0f)).mirror(false),
				PartPose.offsetAndRotation(12.0f, -13.0f, 0.0f, 0.0f, 0.0f, -0.1f));
		p20.addOrReplaceChild("left_spike_a", CubeListBuilder.create()
				.texOffs(110, 80).addBox(-1.0f, -6.0f, -1.0f, 2.0f, 6.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(2.5f, -4.0f, -1.5f, -0.2f, 0.0f, 0.5f));
		p20.addOrReplaceChild("left_spike_b", CubeListBuilder.create()
				.texOffs(54, 90).addBox(-0.5f, -4.0f, -0.5f, 1.0f, 4.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.5f, -4.0f, 2.5f, 0.35f, 0.0f, 0.3f));
		p20.addOrReplaceChild("gauntlet", CubeListBuilder.create()
				.texOffs(0, 40).addBox(-4.0f, 0.0f, -4.0f, 8.0f, 8.0f, 8.0f, new CubeDeformation(0.0f))
				.texOffs(58, 90).addBox(-3.5f, 3.0f, -5.0f, 2.0f, 3.0f, 2.0f, new CubeDeformation(0.0f))
				.texOffs(58, 90).addBox(-1.0f, 3.0f, -5.0f, 2.0f, 3.0f, 2.0f, new CubeDeformation(0.0f))
				.texOffs(58, 90).addBox(1.5f, 3.0f, -5.0f, 2.0f, 3.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offset(0.0f, 11.0f, 0.0f));
		PartDefinition p24 = root.addOrReplaceChild("right_leg", CubeListBuilder.create()
				.texOffs(62, 56).addBox(-3.0f, 0.0f, -3.0f, 6.0f, 6.0f, 6.0f, new CubeDeformation(0.0f))
				.texOffs(16, 90).addBox(-3.5f, 5.0f, -4.2f, 7.0f, 3.0f, 4.0f, new CubeDeformation(0.0f))
				.texOffs(86, 56).addBox(-3.5f, 7.0f, -3.5f, 7.0f, 5.0f, 7.0f, new CubeDeformation(0.0f))
				.texOffs(0, 68).addBox(-4.0f, 11.0f, -6.0f, 8.0f, 2.0f, 10.0f, new CubeDeformation(0.0f)),
				PartPose.offset(-5.0f, 11.0f, 0.0f));
		p24.addOrReplaceChild("right_leg_spike", CubeListBuilder.create()
				.texOffs(80, 90).addBox(-0.5f, -3.0f, -0.5f, 1.0f, 3.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, 5.5f, -4.0f, -0.6f, 0.0f, 0.0f));
		PartDefinition p26 = root.addOrReplaceChild("left_leg", CubeListBuilder.create()
				.texOffs(62, 56).mirror().addBox(-3.0f, 0.0f, -3.0f, 6.0f, 6.0f, 6.0f, new CubeDeformation(0.0f)).mirror(false)
				.texOffs(16, 90).mirror().addBox(-3.5f, 5.0f, -4.2f, 7.0f, 3.0f, 4.0f, new CubeDeformation(0.0f)).mirror(false)
				.texOffs(86, 56).mirror().addBox(-3.5f, 7.0f, -3.5f, 7.0f, 5.0f, 7.0f, new CubeDeformation(0.0f)).mirror(false)
				.texOffs(0, 68).mirror().addBox(-4.0f, 11.0f, -6.0f, 8.0f, 2.0f, 10.0f, new CubeDeformation(0.0f)).mirror(false),
				PartPose.offset(5.0f, 11.0f, 0.0f));
		p26.addOrReplaceChild("left_leg_spike", CubeListBuilder.create()
				.texOffs(80, 90).addBox(-0.5f, -3.0f, -0.5f, 1.0f, 3.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, 5.5f, -4.0f, -0.6f, 0.0f, 0.0f));
		return LayerDefinition.create(mesh, 128, 128);
	}

	@Override
	public void setupAnim(AmethystRobotRenderState state) {
		super.setupAnim(state);
		float pos = state.walkAnimationPos;
		float speed = Math.min(state.walkAnimationSpeed, 1.0f);
		float age = state.ageInTicks;
		float p = state.progress;

		head.yRot = Mth.clamp(state.yRot, -50.0f, 50.0f) * Mth.DEG_TO_RAD;
		head.xRot = state.xRot * Mth.DEG_TO_RAD * 0.6f;

		// schwerer Gang: Körper wiegt, Arme pendeln gegengleich
		rightLeg.xRot = Mth.cos(pos * 0.5f) * 0.7f * speed;
		leftLeg.xRot = Mth.cos(pos * 0.5f + Mth.PI) * 0.7f * speed;
		rightArm.xRot = Mth.cos(pos * 0.5f + Mth.PI) * 0.4f * speed;
		leftArm.xRot = Mth.cos(pos * 0.5f) * 0.4f * speed;
		body.zRot = Mth.cos(pos * 0.5f) * 0.04f * speed;
		body.y += Mth.abs(Mth.cos(pos * 0.5f)) * speed * -0.6f;
		rightArm.zRot += Mth.sin(age * 0.05f) * 0.02f;
		leftArm.zRot -= Mth.sin(age * 0.05f) * 0.02f;

		// Energiering dreht sich immer, Umhang weht, Pylone pulsieren leicht
		float spin = state.enraged ? 0.12f : 0.05f;
		ring.zRot = age * spin;
		cape.xRot = 0.12f + speed * 0.55f + Mth.sin(age * 0.08f) * 0.06f + Mth.sin(pos * 0.5f) * 0.08f * speed;
		pylonLeft.xRot += Mth.sin(age * 0.1f) * 0.03f;
		pylonRight.xRot += Mth.sin(age * 0.1f + 1.0f) * 0.03f;

		switch (state.anim) {
			case AmethystRobot.ANIM_SLAM -> {
				// Panzerfaust hoch, dann krachend runter
				leftArm.xRot = p < 0.5f ? -2.8f * (p / 0.5f) : -2.8f + 3.2f * Math.min(1.0f, (p - 0.5f) * 3.0f);
				body.xRot = p < 0.5f ? -0.1f : 0.2f;
				cape.xRot += p < 0.5f ? 0.0f : 0.3f;
			}
			case AmethystRobot.ANIM_SWEEP -> {
				body.yRot = Mth.sin(p * Mth.PI * 2.0f) * 0.8f;
				leftArm.zRot = -1.2f;
				rightArm.zRot = 1.2f;
				cape.xRot += 0.4f;
			}
			case AmethystRobot.ANIM_PULSE -> {
				float spread = Mth.sin(Math.min(1.0f, p * 2.0f) * Mth.HALF_PI);
				leftArm.zRot = -1.3f * spread;
				rightArm.zRot = 1.3f * spread;
				body.xRot = -0.25f * spread;
				head.xRot = -0.4f * spread;
				ring.zRot = age * 0.4f;
				pylonLeft.zRot = 0.35f + 0.3f * spread;
				pylonRight.zRot = -0.35f - 0.3f * spread;
			}
			case AmethystRobot.ANIM_STOMP -> {
				float lift = p < 0.6f ? p / 0.6f : 1.0f - (p - 0.6f) / 0.4f;
				rightLeg.xRot = -1.0f * lift;
				body.xRot = 0.1f * lift;
			}
			case AmethystRobot.ANIM_DASH -> {
				body.xRot = 0.45f;
				rightArm.xRot = 0.6f;
				leftArm.xRot = 0.6f;
				thrusterLeft.xRot = 0.9f;
				thrusterRight.xRot = 0.9f;
				cape.xRot = 1.2f + Mth.sin(age * 0.6f) * 0.1f;
			}
			case AmethystRobot.ANIM_LASER -> {
				// Kanone nach vorne, Zittern beim Aufladen, Ring dreht hoch
				rightArm.xRot = -Mth.HALF_PI + head.xRot + Mth.sin(age * 2.0f) * 0.02f * (1.0f - p);
				rightArm.yRot = head.yRot * 0.6f;
				leftArm.xRot = -0.3f;
				ring.zRot = age * (0.1f + p * 0.5f);
			}
			default -> {
			}
		}
	}
}
