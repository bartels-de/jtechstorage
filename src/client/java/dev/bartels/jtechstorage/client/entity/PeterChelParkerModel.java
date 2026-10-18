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
 * Peter Chel Parker: Chels Heldenform als Amethyst-Ritter – Tiefenschiefer-Plattenrüstung mit Goldsäumen, Visierhelm mit
 * T-Sehschlitz, Kristallkamm und Helmflossen, Brustplatte mit Kern-Kristall, Umhang mit Hammer-Wappen, Kriegshammer
 * mit Kristallkopf und Rundschild. Nachts im Stillstand döst er auf den Hammer gestützt (Nachtwache).
 * Modell in doppelter Detaildichte (halbe Pixel) – der Renderer skaliert es auf 0.5.
 * Erzeugt von scratchpad/peter2.py (Teile und Posen), Animation von Hand.
 */
public class PeterChelParkerModel extends EntityModel<PeterChelParkerRenderState> implements HelperHatModel {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(JTechStorage.id("peter_chel_parker"), "main");

	private final ModelPart peter;
	private final ModelPart body;
	private final ModelPart head;
	private final ModelPart lids;
	private final ModelPart rFin;
	private final ModelPart lFin;
	private final ModelPart cape;
	private final ModelPart rArm;
	private final ModelPart lArm;
	private final ModelPart rFore;
	private final ModelPart lFore;
	private final ModelPart hammer;
	private final ModelPart rLeg;
	private final ModelPart lLeg;
	private final ModelPart rShin;
	private final ModelPart lShin;

	public PeterChelParkerModel(ModelPart root) {
		super(root);
		this.peter = root.getChild("peter");
		this.body = root.getChild("peter").getChild("body");
		this.head = root.getChild("peter").getChild("body").getChild("head");
		this.lids = root.getChild("peter").getChild("body").getChild("head").getChild("lids");
		this.rFin = root.getChild("peter").getChild("body").getChild("head").getChild("r_fin");
		this.lFin = root.getChild("peter").getChild("body").getChild("head").getChild("l_fin");
		this.cape = root.getChild("peter").getChild("body").getChild("cape");
		this.rArm = root.getChild("peter").getChild("body").getChild("r_arm");
		this.lArm = root.getChild("peter").getChild("body").getChild("l_arm");
		this.rFore = root.getChild("peter").getChild("body").getChild("r_arm").getChild("r_fore");
		this.lFore = root.getChild("peter").getChild("body").getChild("l_arm").getChild("l_fore");
		this.hammer = root.getChild("peter").getChild("body").getChild("r_arm").getChild("r_fore").getChild("hammer");
		this.rLeg = root.getChild("peter").getChild("body").getChild("r_leg");
		this.lLeg = root.getChild("peter").getChild("body").getChild("l_leg");
		this.rShin = root.getChild("peter").getChild("body").getChild("r_leg").getChild("r_shin");
		this.lShin = root.getChild("peter").getChild("body").getChild("l_leg").getChild("l_shin");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		PartDefinition p1 = root.addOrReplaceChild("peter", CubeListBuilder.create(),
				PartPose.offset(0.0f, 24.0f, 0.0f));
		PartDefinition p2 = p1.addOrReplaceChild("body", CubeListBuilder.create()
				.texOffs(38, 0).addBox(-8.0f, -20.0f, -5.0f, 16.0f, 20.0f, 10.0f, new CubeDeformation(0.0f))
				.texOffs(162, 55).addBox(-7.0f, -19.0f, -6.6f, 14.0f, 9.0f, 2.0f, new CubeDeformation(0.0f))
				.texOffs(238, 55).addBox(-3.0f, -17.0f, -7.8f, 6.0f, 6.0f, 2.0f, new CubeDeformation(0.0f))
				.texOffs(120, 55).addBox(-6.0f, -22.5f, -4.5f, 12.0f, 3.0f, 9.0f, new CubeDeformation(0.0f))
				.texOffs(184, 37).addBox(-9.0f, -3.0f, -6.0f, 18.0f, 3.0f, 12.0f, new CubeDeformation(0.0f))
				.texOffs(64, 69).addBox(-2.0f, -3.5f, -6.8f, 4.0f, 4.0f, 1.0f, new CubeDeformation(0.0f))
				.texOffs(84, 69).addBox(-7.5f, -21.0f, 4.6f, 3.0f, 3.0f, 1.0f, new CubeDeformation(0.0f))
				.texOffs(84, 69).addBox(4.5f, -21.0f, 4.6f, 3.0f, 3.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offset(0.0f, -24.0f, 0.0f));
		PartDefinition p3 = p2.addOrReplaceChild("head", CubeListBuilder.create()
				.texOffs(126, 0).addBox(-7.0f, -13.0f, -7.0f, 14.0f, 13.0f, 14.0f, new CubeDeformation(0.0f))
				.texOffs(42, 37).addBox(-7.5f, -10.5f, -7.5f, 15.0f, 2.0f, 15.0f, new CubeDeformation(0.0f))
				.texOffs(212, 55).addBox(-6.0f, -9.0f, -8.2f, 12.0f, 7.0f, 1.0f, new CubeDeformation(0.0f))
				.texOffs(58, 69).addBox(-1.0f, -12.0f, -8.6f, 2.0f, 6.0f, 1.0f, new CubeDeformation(0.0f))
				.texOffs(22, 37).addBox(-2.0f, -25.0f, -3.0f, 4.0f, 12.0f, 6.0f, new CubeDeformation(0.0f)),
				PartPose.offset(0.0f, -22.0f, 0.0f));
		p3.addOrReplaceChild("lids", CubeListBuilder.create()
				.texOffs(92, 69).addBox(-5.0f, -8.0f, -7.6f, 10.0f, 2.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.ZERO);
		p3.addOrReplaceChild("crest_r", CubeListBuilder.create()
				.texOffs(194, 55).addBox(-1.0f, -7.0f, -1.5f, 2.0f, 7.0f, 3.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(-3.5f, -12.5f, 0.0f, 0.0f, 0.0f, -0.45f));
		p3.addOrReplaceChild("crest_l", CubeListBuilder.create()
				.texOffs(194, 55).addBox(-1.0f, -7.0f, -1.5f, 2.0f, 7.0f, 3.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(3.5f, -12.5f, 0.0f, 0.0f, 0.0f, 0.45f));
		p3.addOrReplaceChild("r_fin", CubeListBuilder.create()
				.texOffs(166, 37).addBox(-2.0f, -4.5f, -3.5f, 2.0f, 9.0f, 7.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(-7.2f, -6.0f, 1.0f, 0.0f, -0.45f, -0.35f));
		p3.addOrReplaceChild("l_fin", CubeListBuilder.create()
				.texOffs(166, 37).mirror().addBox(0.0f, -4.5f, -3.5f, 2.0f, 9.0f, 7.0f, new CubeDeformation(0.0f)).mirror(false),
				PartPose.offsetAndRotation(7.2f, -6.0f, 1.0f, 0.0f, 0.45f, 0.35f));
		p2.addOrReplaceChild("cape", CubeListBuilder.create()
				.texOffs(0, 0).addBox(-9.0f, 0.0f, 0.0f, 18.0f, 36.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, -20.0f, 5.4f, 0.08f, 0.0f, 0.0f));
		PartDefinition p10 = p2.addOrReplaceChild("r_arm", CubeListBuilder.create()
				.texOffs(182, 0).addBox(-6.0f, -4.0f, -6.0f, 11.0f, 7.0f, 12.0f, new CubeDeformation(0.0f))
				.texOffs(142, 37).addBox(-3.0f, -1.0f, -3.0f, 6.0f, 10.0f, 6.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(-11.5f, -18.0f, 0.0f, 0.0f, 0.0f, 0.06f));
		p10.addOrReplaceChild("r_spike", CubeListBuilder.create()
				.texOffs(204, 55).addBox(-1.0f, -7.0f, -1.0f, 2.0f, 7.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(-3.0f, -3.5f, 0.0f, 0.0f, 0.0f, -0.4f));
		PartDefinition p12 = p10.addOrReplaceChild("r_fore", CubeListBuilder.create()
				.texOffs(68, 55).addBox(-3.5f, 0.0f, -3.5f, 7.0f, 7.0f, 7.0f, new CubeDeformation(0.0f))
				.texOffs(36, 55).addBox(-4.0f, 6.0f, -4.0f, 8.0f, 6.0f, 8.0f, new CubeDeformation(0.0f)),
				PartPose.offset(0.0f, 9.0f, 0.0f));
		p12.addOrReplaceChild("hammer", CubeListBuilder.create()
				.texOffs(90, 0).addBox(-1.0f, -10.0f, -1.0f, 2.0f, 28.0f, 2.0f, new CubeDeformation(0.0f))
				.texOffs(46, 69).addBox(-1.5f, -14.0f, -1.5f, 3.0f, 4.0f, 3.0f, new CubeDeformation(0.0f))
				.texOffs(102, 37).addBox(-6.0f, 17.0f, -4.0f, 12.0f, 8.0f, 8.0f, new CubeDeformation(0.0f))
				.texOffs(0, 37).addBox(-7.0f, 16.5f, -4.5f, 2.0f, 9.0f, 9.0f, new CubeDeformation(0.0f))
				.texOffs(0, 37).mirror().addBox(5.0f, 16.5f, -4.5f, 2.0f, 9.0f, 9.0f, new CubeDeformation(0.0f)).mirror(false)
				.texOffs(74, 69).addBox(-9.5f, 20.0f, -1.0f, 3.0f, 2.0f, 2.0f, new CubeDeformation(0.0f))
				.texOffs(74, 69).mirror().addBox(6.5f, 20.0f, -1.0f, 3.0f, 2.0f, 2.0f, new CubeDeformation(0.0f)).mirror(false),
				PartPose.offset(0.0f, 9.0f, -0.5f));
		PartDefinition p14 = p2.addOrReplaceChild("l_arm", CubeListBuilder.create()
				.texOffs(182, 0).mirror().addBox(-5.0f, -4.0f, -6.0f, 11.0f, 7.0f, 12.0f, new CubeDeformation(0.0f)).mirror(false)
				.texOffs(142, 37).mirror().addBox(-3.0f, -1.0f, -3.0f, 6.0f, 10.0f, 6.0f, new CubeDeformation(0.0f)).mirror(false),
				PartPose.offsetAndRotation(11.5f, -18.0f, 0.0f, 0.0f, 0.0f, -0.06f));
		p14.addOrReplaceChild("l_spike", CubeListBuilder.create()
				.texOffs(204, 55).addBox(-1.0f, -7.0f, -1.0f, 2.0f, 7.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(3.0f, -3.5f, 0.0f, 0.0f, 0.0f, 0.4f));
		PartDefinition p16 = p14.addOrReplaceChild("l_fore", CubeListBuilder.create()
				.texOffs(68, 55).mirror().addBox(-3.5f, 0.0f, -3.5f, 7.0f, 7.0f, 7.0f, new CubeDeformation(0.0f)).mirror(false)
				.texOffs(36, 55).mirror().addBox(-4.0f, 6.0f, -4.0f, 8.0f, 6.0f, 8.0f, new CubeDeformation(0.0f)).mirror(false),
				PartPose.offset(0.0f, 9.0f, 0.0f));
		p16.addOrReplaceChild("shield", CubeListBuilder.create()
				.texOffs(98, 0).addBox(4.0f, -3.0f, -6.0f, 2.0f, 16.0f, 12.0f, new CubeDeformation(0.0f))
				.texOffs(0, 69).addBox(6.0f, 3.0f, -2.0f, 1.0f, 4.0f, 4.0f, new CubeDeformation(0.0f)),
				PartPose.ZERO);
		PartDefinition p18 = p2.addOrReplaceChild("r_leg", CubeListBuilder.create()
				.texOffs(228, 0).addBox(-3.0f, 0.0f, -3.0f, 6.0f, 12.0f, 6.0f, new CubeDeformation(0.0f))
				.texOffs(30, 69).addBox(-3.5f, -0.5f, -4.2f, 7.0f, 6.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offset(-4.2f, 0.0f, 0.0f));
		p18.addOrReplaceChild("r_shin", CubeListBuilder.create()
				.texOffs(96, 55).addBox(-3.0f, 0.0f, -3.0f, 6.0f, 8.0f, 6.0f, new CubeDeformation(0.0f))
				.texOffs(10, 69).addBox(-3.5f, -2.5f, -4.4f, 7.0f, 4.0f, 3.0f, new CubeDeformation(0.0f))
				.texOffs(0, 55).addBox(-4.0f, 8.0f, -6.0f, 8.0f, 4.0f, 10.0f, new CubeDeformation(0.0f)),
				PartPose.offset(0.0f, 12.0f, 0.0f));
		PartDefinition p20 = p2.addOrReplaceChild("l_leg", CubeListBuilder.create()
				.texOffs(228, 0).mirror().addBox(-3.0f, 0.0f, -3.0f, 6.0f, 12.0f, 6.0f, new CubeDeformation(0.0f)).mirror(false)
				.texOffs(30, 69).mirror().addBox(-3.5f, -0.5f, -4.2f, 7.0f, 6.0f, 1.0f, new CubeDeformation(0.0f)).mirror(false),
				PartPose.offset(4.2f, 0.0f, 0.0f));
		p20.addOrReplaceChild("l_shin", CubeListBuilder.create()
				.texOffs(96, 55).mirror().addBox(-3.0f, 0.0f, -3.0f, 6.0f, 8.0f, 6.0f, new CubeDeformation(0.0f)).mirror(false)
				.texOffs(10, 69).mirror().addBox(-3.5f, -2.5f, -4.4f, 7.0f, 4.0f, 3.0f, new CubeDeformation(0.0f)).mirror(false)
				.texOffs(0, 55).mirror().addBox(-4.0f, 8.0f, -6.0f, 8.0f, 4.0f, 10.0f, new CubeDeformation(0.0f)).mirror(false),
				PartPose.offset(0.0f, 12.0f, 0.0f));
		return LayerDefinition.create(mesh, 256, 128);
	}

	@Override
	public void setupAnim(PeterChelParkerRenderState state) {
		super.setupAnim(state);
		float age = state.ageInTicks;
		lids.visible = state.dozing;

		if (state.dozing) {
			poseGuard(1.0f);
			float breath = Mth.sin(age * 0.05f);
			head.xRot += breath * 0.05f;
			body.y += breath * 0.3f;
			cape.xRot += Mth.sin(age * 0.04f) * 0.03f;
			return;
		}

		float pos = state.walkAnimationPos;
		float speed = Math.min(state.walkAnimationSpeed, 1.0f);
		head.yRot = state.yRot * Mth.DEG_TO_RAD;
		head.xRot = state.xRot * Mth.DEG_TO_RAD;
		rFin.yRot += Mth.sin(age * 0.05f) * 0.05f;
		lFin.yRot -= Mth.sin(age * 0.05f) * 0.05f;

		float swing = Mth.cos(pos * 0.6662f);
		rLeg.xRot += swing * 1.0f * speed;
		lLeg.xRot -= swing * 1.0f * speed;
		rShin.xRot += Math.max(0.0f, -swing) * 0.6f * speed;
		lShin.xRot += Math.max(0.0f, swing) * 0.6f * speed;
		lArm.xRot += swing * 0.7f * speed;
		lArm.zRot -= Mth.sin(age * 0.06f) * 0.03f;
		rArm.xRot -= swing * 0.3f * speed;
		body.y += -Mth.abs(swing) * 0.8f * speed;
		// Umhang weht beim Laufen und leicht im Stand
		cape.xRot += speed * 0.55f + Mth.sin(age * 0.07f) * 0.04f;

		float attack = state.attackProgress;

		if (attack > 0.0f) {
			// ausholen über den Kopf, dann Schlag nach unten
			float up = attack < 0.45f ? attack / 0.45f : 1.0f - Math.min(1.0f, (attack - 0.45f) / 0.55f * 1.6f);
			poseSmash(up);
			rArm.xRot += attack < 0.45f ? 0.0f : 0.3f * Math.min(1.0f, (attack - 0.45f) / 0.3f);
		}
	}

	private void poseSmash(float k) {
		rArm.xRot += k * -2.700f;
		rArm.zRot += k * 0.150f;
		rFore.xRot += k * -0.300f;
		cape.xRot += k * 0.250f;
		body.yRot += k * 0.100f;
	}

	private void poseGuard(float k) {
		body.xRot += k * 0.060f;
		head.xRot += k * 0.500f;
		head.zRot += k * 0.080f;
		cape.xRot += k * -0.040f;
		rArm.xRot += k * -0.400f;
		rArm.zRot += k * -0.420f;
		rFore.xRot += k * -0.450f;
		hammer.xRot += k * 0.900f;
		hammer.zRot += k * 0.420f;
		lArm.xRot += k * -0.450f;
		lArm.zRot += k * 0.500f;
		lFore.xRot += k * -0.550f;
		rFin.zRot += k * 0.250f;
		lFin.zRot += k * -0.250f;
	}

	@Override
	public void translateToHat(PoseStack pose) {
		root().translateAndRotate(pose);
		peter.translateAndRotate(pose);
		body.translateAndRotate(pose);
		head.translateAndRotate(pose);
	}

	@Override
	public float hatScale() {
		return 1.75f;
	}
}
