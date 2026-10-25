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

/**
 * Der letzte Lagermeister: schwebender Geist in zerschlissener Robe mit Schlüsselstab und kreisenden Siegeln.
 * Modell in doppelter Detaildichte (halbe Pixel) – der Renderer skaliert es auf 0.5.
 * Erzeugt von scratchpad/lastmaster.py (Teile und Posen), Animation von Hand.
 */
public class LastMasterModel extends EntityModel<LastMasterRenderState> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(JTechStorage.id("last_master"), "main");

	private final ModelPart master;
	private final ModelPart body;
	private final ModelPart robe;
	private final ModelPart wisp;
	private final ModelPart wisp2;
	private final ModelPart head;
	private final ModelPart rArm;
	private final ModelPart lArm;
	private final ModelPart rSleeve;
	private final ModelPart lSleeve;
	private final ModelPart staff;
	private final ModelPart cape;
	private final ModelPart seals;
	private final ModelPart seal0;
	private final ModelPart seal1;
	private final ModelPart seal2;
	private final ModelPart book;

	public LastMasterModel(ModelPart root) {
		super(root);
		this.master = root.getChild("master");
		this.body = root.getChild("master").getChild("body");
		this.robe = root.getChild("master").getChild("robe");
		this.wisp = root.getChild("master").getChild("robe").getChild("wisp");
		this.wisp2 = root.getChild("master").getChild("robe").getChild("wisp").getChild("wisp2");
		this.head = root.getChild("master").getChild("body").getChild("head");
		this.rArm = root.getChild("master").getChild("body").getChild("r_arm");
		this.lArm = root.getChild("master").getChild("body").getChild("l_arm");
		this.rSleeve = root.getChild("master").getChild("body").getChild("r_arm").getChild("r_sleeve");
		this.lSleeve = root.getChild("master").getChild("body").getChild("l_arm").getChild("l_sleeve");
		this.staff = root.getChild("master").getChild("body").getChild("r_arm").getChild("r_sleeve").getChild("r_hand").getChild("staff");
		this.cape = root.getChild("master").getChild("body").getChild("cape");
		this.seals = root.getChild("master").getChild("seals");
		this.seal0 = root.getChild("master").getChild("seals").getChild("seal0");
		this.seal1 = root.getChild("master").getChild("seals").getChild("seal1");
		this.seal2 = root.getChild("master").getChild("seals").getChild("seal2");
		this.book = root.getChild("master").getChild("body").getChild("l_arm").getChild("l_sleeve").getChild("l_hand").getChild("book");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		PartDefinition p1 = root.addOrReplaceChild("master", CubeListBuilder.create(),
				PartPose.offset(0.0f, 6.0f, 0.0f));
		PartDefinition p2 = p1.addOrReplaceChild("body", CubeListBuilder.create()
				.texOffs(46, 0).addBox(-8.0f, -18.0f, -5.0f, 16.0f, 18.0f, 10.0f, new CubeDeformation(0.0f))
				.texOffs(0, 66).addBox(-9.0f, -18.0f, -5.5f, 18.0f, 6.0f, 11.0f, new CubeDeformation(0.0f))
				.texOffs(94, 66).addBox(-9.5f, -3.0f, -6.0f, 19.0f, 3.0f, 12.0f, new CubeDeformation(0.0f))
				.texOffs(114, 83).addBox(4.0f, -2.0f, -7.5f, 5.0f, 5.0f, 3.0f, new CubeDeformation(0.0f))
				.texOffs(114, 83).addBox(-9.0f, -2.0f, -7.5f, 5.0f, 5.0f, 3.0f, new CubeDeformation(0.0f)),
				PartPose.offset(0.0f, -28.0f, 0.0f));
		PartDefinition p3 = p2.addOrReplaceChild("head", CubeListBuilder.create()
				.texOffs(68, 42).addBox(-5.0f, -10.0f, -5.0f, 10.0f, 10.0f, 10.0f, new CubeDeformation(0.0f))
				.texOffs(174, 0).addBox(-6.5f, -12.0f, -7.0f, 13.0f, 13.0f, 13.0f, new CubeDeformation(0.0f))
				.texOffs(0, 83).addBox(-5.5f, -9.0f, 5.0f, 11.0f, 6.0f, 5.0f, new CubeDeformation(0.0f)),
				PartPose.offset(0.0f, -18.0f, 0.0f));
		p3.addOrReplaceChild("crown0", CubeListBuilder.create()
				.texOffs(220, 66).addBox(-1.5f, -9.0f, -1.5f, 3.0f, 9.0f, 3.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, -12.0f, -2.0f, -0.15f, 0.0f, 0.0f));
		p3.addOrReplaceChild("crown1", CubeListBuilder.create()
				.texOffs(142, 83).addBox(-1.0f, -6.0f, -1.0f, 2.0f, 6.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(-4.0f, -11.0f, -2.0f, -0.1f, 0.0f, -0.35f));
		p3.addOrReplaceChild("crown2", CubeListBuilder.create()
				.texOffs(142, 83).addBox(-1.0f, -6.0f, -1.0f, 2.0f, 6.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(4.0f, -11.0f, -2.0f, -0.1f, 0.0f, 0.35f));
		p3.addOrReplaceChild("crown3", CubeListBuilder.create()
				.texOffs(142, 83).addBox(-1.0f, -6.0f, -1.0f, 2.0f, 6.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(-6.0f, -9.0f, 1.0f, 0.2f, 0.0f, -0.6f));
		p3.addOrReplaceChild("crown4", CubeListBuilder.create()
				.texOffs(142, 83).addBox(-1.0f, -6.0f, -1.0f, 2.0f, 6.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(6.0f, -9.0f, 1.0f, 0.2f, 0.0f, 0.6f));
		PartDefinition p9 = p2.addOrReplaceChild("r_arm", CubeListBuilder.create()
				.texOffs(188, 42).addBox(-3.0f, -2.0f, -3.0f, 6.0f, 12.0f, 6.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(-11.0f, -16.0f, 0.0f, 0.0f, 0.0f, 0.12f));
		p9.addOrReplaceChild("r_shoulder", CubeListBuilder.create()
				.texOffs(58, 66).addBox(-4.0f, -4.0f, -5.0f, 8.0f, 6.0f, 10.0f, new CubeDeformation(0.0f))
				.texOffs(86, 83).addBox(-1.5f, -10.0f, -1.5f, 3.0f, 7.0f, 3.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(-1.0f, -1.0f, 0.0f, 0.0f, 0.0f, -0.15f));
		PartDefinition p11 = p9.addOrReplaceChild("r_sleeve", CubeListBuilder.create()
				.texOffs(108, 42).addBox(-4.0f, 0.0f, -4.0f, 8.0f, 12.0f, 8.0f, new CubeDeformation(0.0f))
				.texOffs(184, 66).addBox(-4.5f, 10.0f, -4.5f, 9.0f, 3.0f, 9.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, 9.0f, 0.0f, -0.35f, 0.0f, 0.0f));
		PartDefinition p12 = p11.addOrReplaceChild("r_hand", CubeListBuilder.create()
				.texOffs(98, 83).addBox(-2.0f, 0.0f, -2.0f, 4.0f, 5.0f, 4.0f, new CubeDeformation(0.0f)),
				PartPose.offset(0.0f, 12.0f, 0.0f));
		PartDefinition p13 = p12.addOrReplaceChild("staff", CubeListBuilder.create()
				.texOffs(0, 0).addBox(-1.0f, -30.0f, -1.0f, 2.0f, 40.0f, 2.0f, new CubeDeformation(0.0f))
				.texOffs(158, 83).addBox(-2.0f, 8.0f, -2.0f, 4.0f, 3.0f, 4.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, 3.0f, -3.5f, 0.35f, 0.0f, 0.0f));
		p13.addOrReplaceChild("staff_head", CubeListBuilder.create()
				.texOffs(48, 83).addBox(-4.0f, -8.0f, -1.0f, 8.0f, 8.0f, 2.0f, new CubeDeformation(0.0f))
				.texOffs(150, 83).addBox(-1.0f, 0.0f, -1.0f, 2.0f, 6.0f, 2.0f, new CubeDeformation(0.0f))
				.texOffs(188, 83).addBox(0.0f, 2.0f, -1.0f, 4.0f, 2.0f, 2.0f, new CubeDeformation(0.0f))
				.texOffs(32, 83).addBox(-2.0f, -15.0f, -2.0f, 4.0f, 7.0f, 4.0f, new CubeDeformation(0.0f)),
				PartPose.offset(0.0f, -30.0f, 0.0f));
		PartDefinition p15 = p2.addOrReplaceChild("l_arm", CubeListBuilder.create()
				.texOffs(188, 42).addBox(-3.0f, -2.0f, -3.0f, 6.0f, 12.0f, 6.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(11.0f, -16.0f, 0.0f, 0.0f, 0.0f, -0.12f));
		p15.addOrReplaceChild("l_shoulder", CubeListBuilder.create()
				.texOffs(58, 66).addBox(-4.0f, -4.0f, -5.0f, 8.0f, 6.0f, 10.0f, new CubeDeformation(0.0f))
				.texOffs(86, 83).addBox(-1.5f, -10.0f, -1.5f, 3.0f, 7.0f, 3.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(1.0f, -1.0f, 0.0f, 0.0f, 0.0f, 0.15f));
		PartDefinition p17 = p15.addOrReplaceChild("l_sleeve", CubeListBuilder.create()
				.texOffs(108, 42).addBox(-4.0f, 0.0f, -4.0f, 8.0f, 12.0f, 8.0f, new CubeDeformation(0.0f))
				.texOffs(184, 66).addBox(-4.5f, 10.0f, -4.5f, 9.0f, 3.0f, 9.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, 9.0f, 0.0f, -0.35f, 0.0f, 0.0f));
		PartDefinition p18 = p17.addOrReplaceChild("l_hand", CubeListBuilder.create()
				.texOffs(98, 83).addBox(-2.0f, 0.0f, -2.0f, 4.0f, 5.0f, 4.0f, new CubeDeformation(0.0f)),
				PartPose.offset(0.0f, 12.0f, 0.0f));
		p18.addOrReplaceChild("book", CubeListBuilder.create()
				.texOffs(68, 83).addBox(-3.0f, 0.0f, -1.5f, 6.0f, 7.0f, 3.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, 3.0f, -2.0f, -0.4f, 0.0f, 0.0f));
		p2.addOrReplaceChild("cape", CubeListBuilder.create()
				.texOffs(8, 0).addBox(-9.0f, 0.0f, 0.0f, 18.0f, 34.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, -17.0f, 5.5f, 0.12f, 0.0f, 0.0f));
		PartDefinition p21 = p1.addOrReplaceChild("robe", CubeListBuilder.create()
				.texOffs(0, 42).addBox(-10.0f, 0.0f, -7.0f, 20.0f, 10.0f, 14.0f, new CubeDeformation(0.0f))
				.texOffs(98, 0).addBox(-11.0f, 10.0f, -8.0f, 22.0f, 10.0f, 16.0f, new CubeDeformation(0.0f)),
				PartPose.offset(0.0f, -28.0f, 0.0f));
		PartDefinition p22 = p21.addOrReplaceChild("wisp", CubeListBuilder.create()
				.texOffs(140, 42).addBox(-7.0f, 0.0f, -5.0f, 14.0f, 8.0f, 10.0f, new CubeDeformation(0.0f)),
				PartPose.offset(0.0f, 20.0f, 0.0f));
		p22.addOrReplaceChild("wisp2", CubeListBuilder.create()
				.texOffs(156, 66).addBox(-4.0f, 0.0f, -3.0f, 8.0f, 7.0f, 6.0f, new CubeDeformation(0.0f)),
				PartPose.offset(0.0f, 8.0f, 0.0f));
		PartDefinition p24 = p1.addOrReplaceChild("seals", CubeListBuilder.create(),
				PartPose.offset(0.0f, -34.0f, 0.0f));
		p24.addOrReplaceChild("seal0", CubeListBuilder.create()
				.texOffs(174, 83).addBox(-2.5f, -2.5f, -1.0f, 5.0f, 5.0f, 2.0f, new CubeDeformation(0.0f))
				.texOffs(130, 83).addBox(-1.5f, -7.5f, -1.5f, 3.0f, 5.0f, 3.0f, new CubeDeformation(0.0f)),
				PartPose.offset(0.0f, 0.0f, -20.0f));
		p24.addOrReplaceChild("seal1", CubeListBuilder.create()
				.texOffs(174, 83).addBox(-2.5f, -2.5f, -1.0f, 5.0f, 5.0f, 2.0f, new CubeDeformation(0.0f))
				.texOffs(130, 83).addBox(-1.5f, -7.5f, -1.5f, 3.0f, 5.0f, 3.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(17.0f, -4.0f, 10.0f, 0.0f, 2.1f, 0.0f));
		p24.addOrReplaceChild("seal2", CubeListBuilder.create()
				.texOffs(174, 83).addBox(-2.5f, -2.5f, -1.0f, 5.0f, 5.0f, 2.0f, new CubeDeformation(0.0f))
				.texOffs(130, 83).addBox(-1.5f, -7.5f, -1.5f, 3.0f, 5.0f, 3.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(-17.0f, 4.0f, 10.0f, 0.0f, -2.1f, 0.0f));
		return LayerDefinition.create(mesh, 256, 256);
	}

	@Override
	public void setupAnim(LastMasterRenderState state) {
		super.setupAnim(state);
		float age = state.ageInTicks;
		head.yRot += Mth.clamp(state.yRot, -50.0f, 50.0f) * Mth.DEG_TO_RAD;
		head.xRot += state.xRot * Mth.DEG_TO_RAD * 0.5f;
		// schwebt auf und ab, Robe und Geisterschweif wehen nach
		master.y += Mth.sin(age * 0.07f) * 1.6f;
		robe.xRot += Mth.sin(age * 0.05f) * 0.04f + state.walkAnimationSpeed * 0.25f;
		wisp.xRot += Mth.sin(age * 0.09f + 0.6f) * 0.12f + state.walkAnimationSpeed * 0.3f;
		wisp2.xRot += Mth.sin(age * 0.11f + 1.2f) * 0.18f;
		wisp.zRot += Mth.sin(age * 0.06f) * 0.06f;
		cape.xRot += Mth.sin(age * 0.08f) * 0.06f + state.walkAnimationSpeed * 0.35f;
		rArm.xRot += Mth.sin(age * 0.06f) * 0.06f;
		lArm.xRot -= Mth.sin(age * 0.06f) * 0.06f;
		book.zRot += Mth.sin(age * 0.1f) * 0.05f;
		// Siegel kreisen um ihn und drehen sich selbst
		seals.yRot = age * (state.shielded ? 0.12f : 0.04f);
		seal0.yRot += age * 0.1f;
		seal1.yRot += age * 0.1f;
		seal2.yRot += age * 0.1f;
		float lift = state.shielded ? 1.0f : 0.0f;
		seal0.y -= lift * 6.0f;
		seal1.y += lift * 4.0f;
		seal2.y -= lift * 2.0f;

		if (state.cast > 0.0f) {
			poseCast(state.cast);
		}

		if (state.summon > 0.0f) {
			poseSummon(state.summon);
		}

		if (state.slam > 0.0f) {
			poseSlam(state.slam);
		}
	}

	private void poseCast(float k) {
		rArm.xRot += k * -2.200f;
		rArm.zRot += k * 0.250f;
		rSleeve.xRot += k * 0.300f;
		head.xRot += k * -0.250f;
		staff.xRot += k * 0.400f;
	}

	private void poseSummon(float k) {
		rArm.xRot += k * -2.700f;
		rArm.zRot += k * 0.500f;
		lArm.xRot += k * -2.700f;
		lArm.zRot += k * -0.500f;
		rSleeve.xRot += k * 0.100f;
		lSleeve.xRot += k * 0.100f;
		head.xRot += k * -0.500f;
		body.xRot += k * -0.150f;
	}

	private void poseSlam(float k) {
		rArm.xRot += k * -0.600f;
		rSleeve.xRot += k * -0.400f;
		body.xRot += k * 0.350f;
		head.xRot += k * 0.200f;
		staff.xRot += k * 1.200f;
	}


}
