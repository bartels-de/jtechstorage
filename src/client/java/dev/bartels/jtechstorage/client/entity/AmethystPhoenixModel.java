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
 * Amethyst-Phönix v2 („majestätisch“): gewölbte Brust, S-förmiger Hals, Kristallkrone über goldenem Diadem,
 * leuchtender Heiligenschein, Schwingen aus drei Federreihen plus vier Schwungfedern mit Flammenspitzen,
 * Schleppe aus fünf Schwanzfedern, goldene Beinschienen, zwei Sättel, bis zu vier Kisten.
 */
public class AmethystPhoenixModel extends EntityModel<AmethystPhoenixRenderState> implements HelperHatModel {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(JTechStorage.id("amethyst_phoenix"), "main");

	private final ModelPart body;
	private final ModelPart neck;
	private final ModelPart neck2;
	private final ModelPart head;
	private final ModelPart halo;
	private final ModelPart crest;
	private final ModelPart rightWing;
	private final ModelPart leftWing;
	private final ModelPart rightCov;
	private final ModelPart leftCov;
	private final ModelPart rightSec;
	private final ModelPart leftSec;
	private final ModelPart[] rightPrimaries = new ModelPart[4];
	private final ModelPart[] leftPrimaries = new ModelPart[4];
	private final ModelPart tail;
	private final ModelPart[] tailFeathers = new ModelPart[5];
	private final ModelPart rightLeg;
	private final ModelPart leftLeg;
	private final ModelPart[] chests = new ModelPart[4];

	public AmethystPhoenixModel(ModelPart root) {
		super(root);
		this.body = root.getChild("body");
		this.neck = body.getChild("neck");
		this.neck2 = neck.getChild("neck2");
		this.head = neck2.getChild("head");
		this.halo = head.getChild("halo");
		this.crest = head.getChild("crest_c");
		this.rightWing = body.getChild("right_wing");
		this.leftWing = body.getChild("left_wing");
		this.rightCov = rightWing.getChild("right_wing_cov");
		this.leftCov = leftWing.getChild("left_wing_cov");
		this.rightSec = rightCov.getChild("right_wing_sec");
		this.leftSec = leftCov.getChild("left_wing_sec");

		for (int k = 0; k < 4; k++) {
			rightPrimaries[k] = rightWing.getChild("right_wing_primary" + k);
			leftPrimaries[k] = leftWing.getChild("left_wing_primary" + k);
		}

		this.tail = body.getChild("tail");
		String[] feathers = {"tail_c", "tail_l1", "tail_r1", "tail_l2", "tail_r2"};

		for (int k = 0; k < feathers.length; k++) {
			tailFeathers[k] = tail.getChild(feathers[k]);
		}

		this.rightLeg = root.getChild("right_leg");
		this.leftLeg = root.getChild("left_leg");

		for (int i = 0; i < 4; i++) {
			chests[i] = body.getChild("chest_" + (i + 1));
		}
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		PartDefinition p1 = root.addOrReplaceChild("body", CubeListBuilder.create()
				.texOffs(0, 0).addBox(-6.0f, -9.0f, -9.0f, 12.0f, 9.0f, 18.0f, new CubeDeformation(0.0f))
				.texOffs(36, 27).addBox(-5.5f, -10.0f, -13.0f, 11.0f, 10.0f, 5.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, 14.0f, 0.0f, -0.12f, 0.0f, 0.0f));
		PartDefinition p2 = p1.addOrReplaceChild("neck", CubeListBuilder.create()
				.texOffs(68, 27).addBox(-3.0f, -8.0f, -3.0f, 6.0f, 8.0f, 6.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, -8.0f, -11.0f, 0.55f, 0.0f, 0.0f));
		PartDefinition p3 = p2.addOrReplaceChild("neck2", CubeListBuilder.create()
				.texOffs(122, 27).addBox(-2.5f, -7.0f, -2.5f, 5.0f, 7.0f, 5.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, -7.5f, 0.0f, -0.8f, 0.0f, 0.0f));
		PartDefinition p4 = p3.addOrReplaceChild("head", CubeListBuilder.create()
				.texOffs(92, 27).addBox(-3.5f, -6.0f, -6.0f, 7.0f, 6.0f, 8.0f, new CubeDeformation(0.0f))
				.texOffs(160, 45).addBox(-1.5f, -3.5f, -10.0f, 3.0f, 3.0f, 4.0f, new CubeDeformation(0.0f))
				.texOffs(228, 45).addBox(-1.0f, -1.0f, -8.5f, 2.0f, 1.0f, 3.0f, new CubeDeformation(0.0f))
				.texOffs(166, 27).addBox(-4.0f, -6.6f, -6.5f, 8.0f, 1.0f, 9.0f, new CubeDeformation(0.0f))
				.texOffs(0, 55).addBox(-1.0f, -8.0f, -6.9f, 2.0f, 2.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, -6.5f, -0.5f, 0.3f, 0.0f, 0.0f));
		p4.addOrReplaceChild("crest_c", CubeListBuilder.create()
				.texOffs(142, 27).addBox(-1.0f, -11.0f, -0.5f, 2.0f, 11.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, -5.5f, 1.0f, -0.85f, 0.0f, 0.0f));
		p4.addOrReplaceChild("crest_l1", CubeListBuilder.create()
				.texOffs(200, 27).addBox(-0.5f, -9.0f, -0.5f, 1.0f, 9.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(1.8f, -5.5f, 1.2f, -0.95f, 0.0f, 0.35f));
		p4.addOrReplaceChild("crest_r1", CubeListBuilder.create()
				.texOffs(200, 27).addBox(-0.5f, -9.0f, -0.5f, 1.0f, 9.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(-1.8f, -5.5f, 1.2f, -0.95f, 0.0f, -0.35f));
		p4.addOrReplaceChild("crest_l2", CubeListBuilder.create()
				.texOffs(128, 45).addBox(-0.5f, -7.0f, -0.5f, 1.0f, 7.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(3.0f, -4.8f, 1.6f, -1.15f, 0.0f, 0.75f));
		p4.addOrReplaceChild("crest_r2", CubeListBuilder.create()
				.texOffs(128, 45).addBox(-0.5f, -7.0f, -0.5f, 1.0f, 7.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(-3.0f, -4.8f, 1.6f, -1.15f, 0.0f, -0.75f));
		p4.addOrReplaceChild("halo", CubeListBuilder.create()
				.texOffs(0, 27).addBox(-9.0f, -9.0f, 0.0f, 18.0f, 18.0f, 0.0f, new CubeDeformation(0.0f)),
				PartPose.offset(0.0f, -3.0f, 5.0f));
		PartDefinition p11 = p1.addOrReplaceChild("right_wing", CubeListBuilder.create()
				.texOffs(204, 27).addBox(-17.0f, -1.5f, -3.0f, 17.0f, 3.0f, 7.0f, new CubeDeformation(0.0f))
				.texOffs(216, 45).addBox(-19.0f, -2.0f, -3.0f, 3.0f, 3.0f, 3.0f, new CubeDeformation(0.0f)),
				PartPose.offset(-6.0f, -7.0f, -4.0f));
		PartDefinition p12 = p11.addOrReplaceChild("right_wing_cov", CubeListBuilder.create()
				.texOffs(0, 45).addBox(-17.0f, 0.0f, 0.0f, 17.0f, 2.0f, 8.0f, new CubeDeformation(0.0f)),
				PartPose.offset(0.0f, 0.5f, 3.5f));
		p12.addOrReplaceChild("right_wing_sec", CubeListBuilder.create()
				.texOffs(50, 45).addBox(-16.0f, 0.0f, 0.0f, 16.0f, 1.0f, 9.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, 0.4f, 7.0f, 0.08f, 0.0f, 0.0f));
		p11.addOrReplaceChild("right_wing_primary0", CubeListBuilder.create()
				.texOffs(168, 0).addBox(-2.0f, 0.0f, 0.0f, 4.0f, 1.0f, 18.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(-11.0f, 0.4f, 1.5f, 0.0f, -0.12f, 0.0f));
		p11.addOrReplaceChild("right_wing_primary1", CubeListBuilder.create()
				.texOffs(168, 0).addBox(-2.0f, 0.0f, 0.0f, 4.0f, 1.0f, 18.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(-13.0f, 0.52f, 1.5f, 0.04f, -0.3f, 0.0f));
		p11.addOrReplaceChild("right_wing_primary2", CubeListBuilder.create()
				.texOffs(168, 0).addBox(-2.0f, 0.0f, 0.0f, 4.0f, 1.0f, 18.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(-15.0f, 0.64f, 1.5f, 0.08f, -0.48f, 0.0f));
		p11.addOrReplaceChild("right_wing_primary3", CubeListBuilder.create()
				.texOffs(168, 0).addBox(-2.0f, 0.0f, 0.0f, 4.0f, 1.0f, 18.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(-17.0f, 0.76f, 1.5f, 0.12f, -0.66f, 0.0f));
		PartDefinition p18 = p1.addOrReplaceChild("left_wing", CubeListBuilder.create()
				.texOffs(204, 27).mirror().addBox(0.0f, -1.5f, -3.0f, 17.0f, 3.0f, 7.0f, new CubeDeformation(0.0f)).mirror(false)
				.texOffs(216, 45).mirror().addBox(16.0f, -2.0f, -3.0f, 3.0f, 3.0f, 3.0f, new CubeDeformation(0.0f)).mirror(false),
				PartPose.offset(6.0f, -7.0f, -4.0f));
		PartDefinition p19 = p18.addOrReplaceChild("left_wing_cov", CubeListBuilder.create()
				.texOffs(0, 45).mirror().addBox(0.0f, 0.0f, 0.0f, 17.0f, 2.0f, 8.0f, new CubeDeformation(0.0f)).mirror(false),
				PartPose.offset(0.0f, 0.5f, 3.5f));
		p19.addOrReplaceChild("left_wing_sec", CubeListBuilder.create()
				.texOffs(50, 45).mirror().addBox(0.0f, 0.0f, 0.0f, 16.0f, 1.0f, 9.0f, new CubeDeformation(0.0f)).mirror(false),
				PartPose.offsetAndRotation(0.0f, 0.4f, 7.0f, 0.08f, 0.0f, 0.0f));
		p18.addOrReplaceChild("left_wing_primary0", CubeListBuilder.create()
				.texOffs(168, 0).mirror().addBox(-2.0f, 0.0f, 0.0f, 4.0f, 1.0f, 18.0f, new CubeDeformation(0.0f)).mirror(false),
				PartPose.offsetAndRotation(11.0f, 0.4f, 1.5f, 0.0f, 0.12f, 0.0f));
		p18.addOrReplaceChild("left_wing_primary1", CubeListBuilder.create()
				.texOffs(168, 0).mirror().addBox(-2.0f, 0.0f, 0.0f, 4.0f, 1.0f, 18.0f, new CubeDeformation(0.0f)).mirror(false),
				PartPose.offsetAndRotation(13.0f, 0.52f, 1.5f, 0.04f, 0.3f, 0.0f));
		p18.addOrReplaceChild("left_wing_primary2", CubeListBuilder.create()
				.texOffs(168, 0).mirror().addBox(-2.0f, 0.0f, 0.0f, 4.0f, 1.0f, 18.0f, new CubeDeformation(0.0f)).mirror(false),
				PartPose.offsetAndRotation(15.0f, 0.64f, 1.5f, 0.08f, 0.48f, 0.0f));
		p18.addOrReplaceChild("left_wing_primary3", CubeListBuilder.create()
				.texOffs(168, 0).mirror().addBox(-2.0f, 0.0f, 0.0f, 4.0f, 1.0f, 18.0f, new CubeDeformation(0.0f)).mirror(false),
				PartPose.offsetAndRotation(17.0f, 0.76f, 1.5f, 0.12f, 0.66f, 0.0f));
		PartDefinition p25 = p1.addOrReplaceChild("tail", CubeListBuilder.create()
				.texOffs(100, 45).addBox(-4.0f, -1.5f, 0.0f, 8.0f, 3.0f, 6.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, -5.5f, 8.0f, -0.28f, 0.0f, 0.0f));
		p25.addOrReplaceChild("tail_c", CubeListBuilder.create()
				.texOffs(60, 0).addBox(-1.5f, 0.0f, 0.0f, 3.0f, 1.0f, 26.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, -0.6f, 4.5f, -0.05f, 0.0f, 0.0f));
		p25.addOrReplaceChild("tail_l1", CubeListBuilder.create()
				.texOffs(118, 0).addBox(-1.5f, 0.0f, 0.0f, 3.0f, 1.0f, 22.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(1.8f, -0.2f, 4.5f, 0.0f, 0.3f, 0.0f));
		p25.addOrReplaceChild("tail_r1", CubeListBuilder.create()
				.texOffs(118, 0).mirror().addBox(-1.5f, 0.0f, 0.0f, 3.0f, 1.0f, 22.0f, new CubeDeformation(0.0f)).mirror(false),
				PartPose.offsetAndRotation(-1.8f, -0.2f, 4.5f, 0.0f, -0.3f, 0.0f));
		p25.addOrReplaceChild("tail_l2", CubeListBuilder.create()
				.texOffs(212, 0).addBox(-1.5f, 0.0f, 0.0f, 3.0f, 1.0f, 18.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(3.2f, 0.2f, 4.0f, 0.06f, 0.62f, 0.0f));
		p25.addOrReplaceChild("tail_r2", CubeListBuilder.create()
				.texOffs(212, 0).mirror().addBox(-1.5f, 0.0f, 0.0f, 3.0f, 1.0f, 18.0f, new CubeDeformation(0.0f)).mirror(false),
				PartPose.offsetAndRotation(-3.2f, 0.2f, 4.0f, 0.06f, -0.62f, 0.0f));
		p1.addOrReplaceChild("saddles", CubeListBuilder.create()
				.texOffs(132, 45).addBox(-4.0f, -11.0f, -6.0f, 8.0f, 2.0f, 6.0f, new CubeDeformation(0.0f))
				.texOffs(238, 45).addBox(-4.0f, -13.0f, -1.0f, 8.0f, 3.0f, 1.0f, new CubeDeformation(0.0f))
				.texOffs(132, 45).addBox(-4.0f, -11.0f, 2.0f, 8.0f, 2.0f, 6.0f, new CubeDeformation(0.0f))
				.texOffs(238, 45).addBox(-4.0f, -13.0f, 7.0f, 8.0f, 3.0f, 1.0f, new CubeDeformation(0.0f))
				.texOffs(6, 55).addBox(-6.0f, -9.5f, -3.5f, 12.0f, 1.0f, 1.0f, new CubeDeformation(0.0f))
				.texOffs(6, 55).addBox(-6.0f, -9.5f, 4.5f, 12.0f, 1.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.ZERO);
		p1.addOrReplaceChild("chest_1", CubeListBuilder.create()
				.texOffs(148, 27).mirror().addBox(0.0f, 0.0f, -3.0f, 3.0f, 6.0f, 6.0f, new CubeDeformation(0.0f)).mirror(false),
				PartPose.offset(6.0f, -7.5f, -3.5f));
		p1.addOrReplaceChild("chest_2", CubeListBuilder.create()
				.texOffs(148, 27).addBox(-3.0f, 0.0f, -3.0f, 3.0f, 6.0f, 6.0f, new CubeDeformation(0.0f)),
				PartPose.offset(-6.0f, -7.5f, -3.5f));
		p1.addOrReplaceChild("chest_3", CubeListBuilder.create()
				.texOffs(148, 27).mirror().addBox(0.0f, 0.0f, -3.0f, 3.0f, 6.0f, 6.0f, new CubeDeformation(0.0f)).mirror(false),
				PartPose.offset(6.0f, -7.5f, 4.5f));
		p1.addOrReplaceChild("chest_4", CubeListBuilder.create()
				.texOffs(148, 27).addBox(-3.0f, 0.0f, -3.0f, 3.0f, 6.0f, 6.0f, new CubeDeformation(0.0f)),
				PartPose.offset(-6.0f, -7.5f, 4.5f));
		root.addOrReplaceChild("right_leg", CubeListBuilder.create()
				.texOffs(174, 45).addBox(-1.5f, 0.0f, -1.5f, 3.0f, 4.0f, 3.0f, new CubeDeformation(0.0f))
				.texOffs(186, 45).addBox(-1.0f, 4.0f, -1.0f, 2.0f, 5.0f, 2.0f, new CubeDeformation(0.0f))
				.texOffs(194, 45).addBox(-2.5f, 9.0f, -4.0f, 5.0f, 1.0f, 6.0f, new CubeDeformation(0.0f)),
				PartPose.offset(-3.5f, 14.0f, 1.0f));
		root.addOrReplaceChild("left_leg", CubeListBuilder.create()
				.texOffs(174, 45).mirror().addBox(-1.5f, 0.0f, -1.5f, 3.0f, 4.0f, 3.0f, new CubeDeformation(0.0f)).mirror(false)
				.texOffs(186, 45).mirror().addBox(-1.0f, 4.0f, -1.0f, 2.0f, 5.0f, 2.0f, new CubeDeformation(0.0f)).mirror(false)
				.texOffs(194, 45).mirror().addBox(-2.5f, 9.0f, -4.0f, 5.0f, 1.0f, 6.0f, new CubeDeformation(0.0f)).mirror(false),
				PartPose.offset(3.5f, 14.0f, 1.0f));
		return LayerDefinition.create(mesh, 256, 128);
	}

	@Override
	public void setupAnim(AmethystPhoenixRenderState state) {
		super.setupAnim(state);
		float age = state.ageInTicks;

		for (int i = 0; i < 4; i++) {
			chests[i].visible = i < state.chests;
		}

		// Heiligenschein dreht sich langsam, Krone wiegt sich
		halo.zRot = age * 0.02f;
		crest.xRot += Mth.sin(age * 0.05f) * 0.06f;
		head.yRot = Mth.clamp(state.yRot, -45.0f, 45.0f) * Mth.DEG_TO_RAD;
		head.xRot += state.xRot * Mth.DEG_TO_RAD * 0.3f;

		if (state.flying) {
			// langsame, mächtige Flügelschläge – mit Ladung noch etwas langsamer
			float beat = Mth.sin(age * (0.11f + 0.08f * state.speed));
			rightWing.zRot = beat * 0.5f + 0.08f;
			leftWing.zRot = -beat * 0.5f - 0.08f;
			rightCov.zRot = beat * 0.18f;
			leftCov.zRot = -beat * 0.18f;
			rightSec.zRot = beat * 0.1f;
			leftSec.zRot = -beat * 0.1f;

			for (int k = 0; k < 4; k++) {
				float lag = Mth.sin(age * (0.11f + 0.08f * state.speed) - 0.4f - k * 0.15f);
				rightPrimaries[k].zRot = lag * 0.12f;
				leftPrimaries[k].zRot = -lag * 0.12f;
			}

			body.y += -beat * 0.9f;
			neck.xRot -= 0.25f;
			neck2.xRot += 0.2f;
			rightLeg.xRot = 1.35f;
			leftLeg.xRot = 1.35f;
			tail.xRot += 0.12f + Mth.sin(age * 0.08f) * 0.05f;

			for (int k = 1; k < 5; k++) {
				tailFeathers[k].yRot *= 1.25f;   // Schleppe im Flug weit aufgefächert
			}
		} else {
			// Flügel angelegt, Schwungfedern zusammengelegt
			rightWing.yRot = 1.25f;
			rightWing.zRot = -0.3f;
			leftWing.yRot = -1.25f;
			leftWing.zRot = 0.3f;

			for (int k = 0; k < 4; k++) {
				rightPrimaries[k].yRot = -0.04f * k;
				leftPrimaries[k].yRot = 0.04f * k;
			}

			// ruhiges, stolzes Atmen
			float breath = Mth.sin(age * 0.05f);
			body.y += breath * 0.2f;
			neck.xRot += breath * 0.02f;

			for (int k = 1; k < 5; k++) {
				tailFeathers[k].yRot *= 0.55f;   // Schleppe zusammengelegt
			}

			tail.yRot = Mth.sin(age * 0.035f) * 0.06f;

			if (state.sitting) {
				body.y += 4.0f;
				rightLeg.visible = false;
				leftLeg.visible = false;
			} else {
				rightLeg.visible = true;
				leftLeg.visible = true;
			}
		}
	}

	@Override
	public void translateToHat(PoseStack pose) {
		root().translateAndRotate(pose);
		body.translateAndRotate(pose);
		neck.translateAndRotate(pose);
		neck2.translateAndRotate(pose);
		head.translateAndRotate(pose);
		pose.translate(0.0f, -0.42f, -0.12f);
	}

	@Override
	public float hatScale() {
		return 0.7f;
	}
}
