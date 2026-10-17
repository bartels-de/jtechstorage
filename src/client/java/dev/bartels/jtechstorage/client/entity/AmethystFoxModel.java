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
 * Amethyst-Fuchs (Kristall-Kitsune): Amethyst-Fell, Halskrause, Rückenkristalle, Goldstirnband mit Juwel, Ohren mit
 * Kristallspitzen, drei buschige Schweife mit leuchtenden Spitzen und drei kreisende Kristallsplitter.
 * Modell in doppelter Detaildichte (halbe Pixel) – der Renderer skaliert es auf 0.5.
 * Erzeugt von scratchpad/fox2.py (Teile und Posen), Animation von Hand.
 */
public class AmethystFoxModel extends EntityModel<AmethystFoxRenderState> implements HelperHatModel {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(JTechStorage.id("amethyst_fox"), "main");

	private final ModelPart fox;
	private final ModelPart body;
	private final ModelPart head;
	private final ModelPart lids;
	private final ModelPart rEar;
	private final ModelPart lEar;
	private final ModelPart rfLeg;
	private final ModelPart lfLeg;
	private final ModelPart rhLeg;
	private final ModelPart lhLeg;
	private final ModelPart rfShin;
	private final ModelPart lfShin;
	private final ModelPart rhShin;
	private final ModelPart lhShin;
	private final ModelPart tail0;
	private final ModelPart tail1;
	private final ModelPart tail2;
	private final ModelPart tail0b;
	private final ModelPart tail1b;
	private final ModelPart tail2b;
	private final ModelPart tail0c;
	private final ModelPart tail1c;
	private final ModelPart tail2c;
	private final ModelPart orb0;
	private final ModelPart orb1;
	private final ModelPart orb2;

	public AmethystFoxModel(ModelPart root) {
		super(root);
		this.fox = root.getChild("fox");
		this.body = root.getChild("fox").getChild("body");
		this.head = root.getChild("fox").getChild("body").getChild("head");
		this.lids = root.getChild("fox").getChild("body").getChild("head").getChild("lids");
		this.rEar = root.getChild("fox").getChild("body").getChild("head").getChild("r_ear");
		this.lEar = root.getChild("fox").getChild("body").getChild("head").getChild("l_ear");
		this.rfLeg = root.getChild("fox").getChild("body").getChild("rf_leg");
		this.lfLeg = root.getChild("fox").getChild("body").getChild("lf_leg");
		this.rhLeg = root.getChild("fox").getChild("body").getChild("rh_leg");
		this.lhLeg = root.getChild("fox").getChild("body").getChild("lh_leg");
		this.rfShin = root.getChild("fox").getChild("body").getChild("rf_leg").getChild("rf_shin");
		this.lfShin = root.getChild("fox").getChild("body").getChild("lf_leg").getChild("lf_shin");
		this.rhShin = root.getChild("fox").getChild("body").getChild("rh_leg").getChild("rh_shin");
		this.lhShin = root.getChild("fox").getChild("body").getChild("lh_leg").getChild("lh_shin");
		this.tail0 = root.getChild("fox").getChild("body").getChild("tail0");
		this.tail1 = root.getChild("fox").getChild("body").getChild("tail1");
		this.tail2 = root.getChild("fox").getChild("body").getChild("tail2");
		this.tail0b = root.getChild("fox").getChild("body").getChild("tail0").getChild("tail0b");
		this.tail1b = root.getChild("fox").getChild("body").getChild("tail1").getChild("tail1b");
		this.tail2b = root.getChild("fox").getChild("body").getChild("tail2").getChild("tail2b");
		this.tail0c = root.getChild("fox").getChild("body").getChild("tail0").getChild("tail0b").getChild("tail0c");
		this.tail1c = root.getChild("fox").getChild("body").getChild("tail1").getChild("tail1b").getChild("tail1c");
		this.tail2c = root.getChild("fox").getChild("body").getChild("tail2").getChild("tail2b").getChild("tail2c");
		this.orb0 = root.getChild("fox").getChild("orb0");
		this.orb1 = root.getChild("fox").getChild("orb1");
		this.orb2 = root.getChild("fox").getChild("orb2");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		PartDefinition p1 = root.addOrReplaceChild("fox", CubeListBuilder.create(),
				PartPose.offset(0.0f, 24.0f, 0.0f));
		PartDefinition p2 = p1.addOrReplaceChild("body", CubeListBuilder.create()
				.texOffs(0, 0).addBox(-4.5f, -4.0f, -8.0f, 9.0f, 8.0f, 16.0f, new CubeDeformation(0.0f))
				.texOffs(84, 0).addBox(-5.0f, -4.5f, -10.5f, 10.0f, 9.0f, 5.0f, new CubeDeformation(0.2f))
				.texOffs(56, 24).addBox(-1.0f, -7.5f, -4.0f, 2.0f, 4.0f, 2.0f, new CubeDeformation(0.0f))
				.texOffs(56, 24).addBox(-1.0f, -7.0f, 0.0f, 2.0f, 4.0f, 2.0f, new CubeDeformation(0.0f))
				.texOffs(56, 24).addBox(-1.0f, -6.5f, 4.0f, 2.0f, 4.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offset(0.0f, -13.0f, 0.0f));
		PartDefinition p3 = p2.addOrReplaceChild("head", CubeListBuilder.create()
				.texOffs(50, 0).addBox(-4.5f, -6.0f, -7.0f, 9.0f, 8.0f, 8.0f, new CubeDeformation(0.0f))
				.texOffs(214, 0).addBox(-2.5f, -1.8f, -11.6f, 5.0f, 4.0f, 5.0f, new CubeDeformation(0.0f))
				.texOffs(102, 24).addBox(-1.0f, -2.0f, -12.1f, 2.0f, 1.0f, 1.0f, new CubeDeformation(0.0f))
				.texOffs(14, 24).addBox(-6.0f, -3.5f, -4.0f, 2.0f, 4.0f, 3.0f, new CubeDeformation(0.0f))
				.texOffs(14, 24).mirror().addBox(4.0f, -3.5f, -4.0f, 2.0f, 4.0f, 3.0f, new CubeDeformation(0.0f)).mirror(false)
				.texOffs(180, 0).addBox(-4.5f, -6.2f, -7.0f, 9.0f, 1.0f, 8.0f, new CubeDeformation(0.15f))
				.texOffs(96, 24).addBox(-1.0f, -6.6f, -7.5f, 2.0f, 2.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offset(0.0f, -3.5f, -9.0f));
		p3.addOrReplaceChild("lids", CubeListBuilder.create()
				.texOffs(78, 24).addBox(-4.0f, -5.6f, -7.3f, 8.0f, 2.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.ZERO);
		p3.addOrReplaceChild("r_ear", CubeListBuilder.create()
				.texOffs(24, 24).addBox(-1.5f, -5.0f, -1.0f, 3.0f, 5.0f, 2.0f, new CubeDeformation(0.0f))
				.texOffs(72, 24).addBox(-1.0f, -7.6f, -0.5f, 2.0f, 3.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(-2.8f, -5.6f, -3.0f, -0.15f, 0.0f, -0.3f));
		p3.addOrReplaceChild("l_ear", CubeListBuilder.create()
				.texOffs(24, 24).mirror().addBox(-1.5f, -5.0f, -1.0f, 3.0f, 5.0f, 2.0f, new CubeDeformation(0.0f)).mirror(false)
				.texOffs(72, 24).addBox(-1.0f, -7.6f, -0.5f, 2.0f, 3.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(2.8f, -5.6f, -3.0f, -0.15f, 0.0f, 0.3f));
		PartDefinition p7 = p2.addOrReplaceChild("rf_leg", CubeListBuilder.create()
				.texOffs(234, 0).addBox(-1.5f, -1.0f, -1.5f, 3.0f, 6.0f, 3.0f, new CubeDeformation(0.0f)),
				PartPose.offset(-2.8f, 2.5f, -6.0f));
		p7.addOrReplaceChild("rf_shin", CubeListBuilder.create()
				.texOffs(34, 24).addBox(-1.0f, 0.0f, -1.0f, 2.0f, 5.0f, 2.0f, new CubeDeformation(0.0f))
				.texOffs(42, 24).addBox(-1.5f, 4.2f, -2.5f, 3.0f, 2.0f, 4.0f, new CubeDeformation(0.0f)),
				PartPose.offset(0.0f, 5.0f, 0.0f));
		PartDefinition p9 = p2.addOrReplaceChild("lf_leg", CubeListBuilder.create()
				.texOffs(234, 0).addBox(-1.5f, -1.0f, -1.5f, 3.0f, 6.0f, 3.0f, new CubeDeformation(0.0f)),
				PartPose.offset(2.8f, 2.5f, -6.0f));
		p9.addOrReplaceChild("lf_shin", CubeListBuilder.create()
				.texOffs(34, 24).addBox(-1.0f, 0.0f, -1.0f, 2.0f, 5.0f, 2.0f, new CubeDeformation(0.0f))
				.texOffs(42, 24).addBox(-1.5f, 4.2f, -2.5f, 3.0f, 2.0f, 4.0f, new CubeDeformation(0.0f)),
				PartPose.offset(0.0f, 5.0f, 0.0f));
		PartDefinition p11 = p2.addOrReplaceChild("rh_leg", CubeListBuilder.create()
				.texOffs(234, 0).addBox(-1.5f, -1.0f, -1.5f, 3.0f, 6.0f, 3.0f, new CubeDeformation(0.0f)),
				PartPose.offset(-2.8f, 2.5f, 6.0f));
		p11.addOrReplaceChild("rh_shin", CubeListBuilder.create()
				.texOffs(34, 24).addBox(-1.0f, 0.0f, -1.0f, 2.0f, 5.0f, 2.0f, new CubeDeformation(0.0f))
				.texOffs(42, 24).addBox(-1.5f, 4.2f, -2.5f, 3.0f, 2.0f, 4.0f, new CubeDeformation(0.0f)),
				PartPose.offset(0.0f, 5.0f, 0.0f));
		PartDefinition p13 = p2.addOrReplaceChild("lh_leg", CubeListBuilder.create()
				.texOffs(234, 0).addBox(-1.5f, -1.0f, -1.5f, 3.0f, 6.0f, 3.0f, new CubeDeformation(0.0f)),
				PartPose.offset(2.8f, 2.5f, 6.0f));
		p13.addOrReplaceChild("lh_shin", CubeListBuilder.create()
				.texOffs(34, 24).addBox(-1.0f, 0.0f, -1.0f, 2.0f, 5.0f, 2.0f, new CubeDeformation(0.0f))
				.texOffs(42, 24).addBox(-1.5f, 4.2f, -2.5f, 3.0f, 2.0f, 4.0f, new CubeDeformation(0.0f)),
				PartPose.offset(0.0f, 5.0f, 0.0f));
		PartDefinition p15 = p2.addOrReplaceChild("tail0", CubeListBuilder.create()
				.texOffs(138, 0).addBox(-2.0f, -2.0f, 0.0f, 4.0f, 4.0f, 7.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, -2.0f, 7.5f, -0.75f, -0.5f, 0.0f));
		PartDefinition p16 = p15.addOrReplaceChild("tail0b", CubeListBuilder.create()
				.texOffs(114, 0).addBox(-2.5f, -2.5f, 0.0f, 5.0f, 5.0f, 7.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, 0.0f, 6.5f, 0.25f, 0.0f, 0.0f));
		p16.addOrReplaceChild("tail0c", CubeListBuilder.create()
				.texOffs(160, 0).addBox(-2.0f, -2.0f, 0.0f, 4.0f, 4.0f, 6.0f, new CubeDeformation(0.0f))
				.texOffs(0, 24).addBox(-1.5f, -1.5f, 5.5f, 3.0f, 3.0f, 4.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, 0.0f, 6.5f, 0.3f, 0.0f, 0.0f));
		PartDefinition p18 = p2.addOrReplaceChild("tail1", CubeListBuilder.create()
				.texOffs(138, 0).addBox(-2.0f, -2.0f, 0.0f, 4.0f, 4.0f, 7.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, -2.0f, 7.5f, -0.75f, 0.0f, 0.0f));
		PartDefinition p19 = p18.addOrReplaceChild("tail1b", CubeListBuilder.create()
				.texOffs(114, 0).addBox(-2.5f, -2.5f, 0.0f, 5.0f, 5.0f, 7.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, 0.0f, 6.5f, 0.25f, 0.0f, 0.0f));
		p19.addOrReplaceChild("tail1c", CubeListBuilder.create()
				.texOffs(160, 0).addBox(-2.0f, -2.0f, 0.0f, 4.0f, 4.0f, 6.0f, new CubeDeformation(0.0f))
				.texOffs(0, 24).addBox(-1.5f, -1.5f, 5.5f, 3.0f, 3.0f, 4.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, 0.0f, 6.5f, 0.3f, 0.0f, 0.0f));
		PartDefinition p21 = p2.addOrReplaceChild("tail2", CubeListBuilder.create()
				.texOffs(138, 0).addBox(-2.0f, -2.0f, 0.0f, 4.0f, 4.0f, 7.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, -2.0f, 7.5f, -0.75f, 0.5f, 0.0f));
		PartDefinition p22 = p21.addOrReplaceChild("tail2b", CubeListBuilder.create()
				.texOffs(114, 0).addBox(-2.5f, -2.5f, 0.0f, 5.0f, 5.0f, 7.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, 0.0f, 6.5f, 0.25f, 0.0f, 0.0f));
		p22.addOrReplaceChild("tail2c", CubeListBuilder.create()
				.texOffs(160, 0).addBox(-2.0f, -2.0f, 0.0f, 4.0f, 4.0f, 6.0f, new CubeDeformation(0.0f))
				.texOffs(0, 24).addBox(-1.5f, -1.5f, 5.5f, 3.0f, 3.0f, 4.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, 0.0f, 6.5f, 0.3f, 0.0f, 0.0f));
		p1.addOrReplaceChild("orb0", CubeListBuilder.create()
				.texOffs(64, 24).addBox(-1.0f, -2.0f, -1.0f, 2.0f, 4.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, -10.0f, 0.0f, 0.0f, 0.0f, 0.3f));
		p1.addOrReplaceChild("orb1", CubeListBuilder.create()
				.texOffs(64, 24).addBox(-1.0f, -2.0f, -1.0f, 2.0f, 4.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, -10.0f, 0.0f, 0.0f, 0.0f, 0.3f));
		p1.addOrReplaceChild("orb2", CubeListBuilder.create()
				.texOffs(64, 24).addBox(-1.0f, -2.0f, -1.0f, 2.0f, 4.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, -10.0f, 0.0f, 0.0f, 0.0f, 0.3f));
		return LayerDefinition.create(mesh, 256, 128);
	}

	@Override
	public void setupAnim(AmethystFoxRenderState state) {
		super.setupAnim(state);
		float age = state.ageInTicks;
		lids.visible = state.resting;
		ModelPart[] orbs = {orb0, orb1, orb2};

		for (int k = 0; k < 3; k++) {
			float angle = age * 0.06f + k * 2.094f;
			orbs[k].x = Mth.cos(angle) * 12.0f;
			orbs[k].z = Mth.sin(angle) * 12.0f;
			orbs[k].y = -16.0f + Mth.sin(age * 0.1f + k * 1.7f) * 2.0f;
			orbs[k].yRot = -angle;
			orbs[k].visible = !state.resting;
		}

		if (state.resting) {
			poseSleep(1.0f);
			float breath = Mth.sin(age * 0.06f);
			body.y += breath * 0.3f;
			tail1.yRot += breath * 0.04f;
			return;
		}

		float pos = state.walkAnimationPos;
		float speed = Math.min(state.walkAnimationSpeed, 1.0f);
		head.yRot += Mth.clamp(state.yRot, -50.0f, 50.0f) * Mth.DEG_TO_RAD;
		head.xRot += state.xRot * Mth.DEG_TO_RAD;

		// Trab
		float a = Mth.cos(pos * 0.9f) * 1.0f * speed;
		rfLeg.xRot += a;
		lhLeg.xRot += a;
		lfLeg.xRot -= a;
		rhLeg.xRot -= a;
		rfShin.xRot += Math.max(0.0f, -a) * 0.5f;
		lfShin.xRot += Math.max(0.0f, a) * 0.5f;

		// Schweife wiegen sich gegeneinander versetzt, beim Laufen flacher
		ModelPart[] tails = {tail0, tail1, tail2};
		ModelPart[] mids = {tail0b, tail1b, tail2b};
		ModelPart[] tips = {tail0c, tail1c, tail2c};

		for (int k = 0; k < 3; k++) {
			tails[k].yRot += Mth.sin(age * 0.09f + k * 2.1f) * 0.12f;
			tails[k].xRot += Mth.sin(age * 0.07f + k) * 0.06f + speed * 0.4f;
			mids[k].yRot += Mth.sin(age * 0.09f + k * 2.1f - 0.6f) * 0.12f;
			tips[k].yRot += Mth.sin(age * 0.09f + k * 2.1f - 1.2f) * 0.15f;
		}

		if (state.ready) {
			// spürt einen Schatz: Ohren zucken
			rEar.zRot += Mth.sin(age * 0.8f) * 0.08f;
			lEar.zRot -= Mth.sin(age * 0.8f) * 0.08f;
		}

		if (state.digging) {
			poseDig(1.0f);
			float dig = Mth.sin(age * 1.6f);
			rfLeg.xRot += dig * 0.7f;
			lfLeg.xRot -= dig * 0.7f;
		}
	}

	private void poseDig(float k) {
		body.xRot += k * 0.350f;
		body.y += k * 1.500f;
		head.xRot += k * 0.550f;
		rfLeg.xRot += k * -0.700f;
		lfLeg.xRot += k * -0.300f;
		rfShin.xRot += k * -0.500f;
		lfShin.xRot += k * -0.200f;
		tail0.xRot += k * 0.300f;
		tail1.xRot += k * 0.300f;
		tail2.xRot += k * 0.300f;
	}

	private void poseSleep(float k) {
		body.y += k * 6.700f;
		rfLeg.xRot += k * -1.500f;
		lfLeg.xRot += k * -1.500f;
		rfShin.xRot += k * 0.000f;
		lfShin.xRot += k * 0.000f;
		rhLeg.xRot += k * -1.400f;
		lhLeg.xRot += k * -1.400f;
		rhShin.xRot += k * 2.900f;
		lhShin.xRot += k * 2.900f;
		head.yRot += k * 0.900f;
		head.xRot += k * 0.450f;
		head.y += k * 3.500f;
		head.x += k * -1.500f;
		rEar.xRot += k * 0.600f;
		lEar.xRot += k * 0.600f;
		tail0.xRot += k * 0.950f;
		tail0.yRot += k * -1.600f;
		tail0b.yRot += k * -0.600f;
		tail0c.yRot += k * -0.600f;
		tail1.xRot += k * 0.850f;
		tail1.yRot += k * -1.250f;
		tail1b.yRot += k * -0.500f;
		tail1c.yRot += k * -0.500f;
		tail2.xRot += k * 0.750f;
		tail2.yRot += k * -0.950f;
		tail2b.yRot += k * -0.450f;
		tail2c.yRot += k * -0.450f;
	}

	@Override
	public void translateToHat(PoseStack pose) {
		root().translateAndRotate(pose);
		fox.translateAndRotate(pose);
		body.translateAndRotate(pose);
		head.translateAndRotate(pose);
		pose.translate(0.0f, 2.0f / 16.0f, -3.0f / 16.0f);
	}

	@Override
	public float hatScale() {
		return 1.1f;
	}
}
