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
 * Sturm-Herold: schwebende Gestalt aus verderbtem Amethyst – zerfetzte Sturmrobe mit glühendem Saum, Sturmkern,
 * Kapuze mit leerem Gesicht und glühenden Augen, zerbrochene Kristallkrone, Stimmgabel-Stab, kreisende Splitter.
 * Modell in doppelter Detaildichte (halbe Pixel) – der Renderer skaliert es auf 0.5.
 * Erzeugt von scratchpad/herald.py (Teile und Posen), Animation von Hand.
 */
public class StormHeraldModel extends EntityModel<StormHeraldRenderState> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(JTechStorage.id("storm_herald"), "main");

	private final ModelPart herald;
	private final ModelPart body;
	private final ModelPart head;
	private final ModelPart crown;
	private final ModelPart rArm;
	private final ModelPart lArm;
	private final ModelPart rFore;
	private final ModelPart lFore;
	private final ModelPart staff;
	private final ModelPart skirt;
	private final ModelPart hem;
	private final ModelPart tatterL;
	private final ModelPart tatterR;
	private final ModelPart shard0;
	private final ModelPart shard1;
	private final ModelPart shard2;
	private final ModelPart shard3;

	public StormHeraldModel(ModelPart root) {
		super(root);
		this.herald = root.getChild("herald");
		this.body = root.getChild("herald").getChild("body");
		this.head = root.getChild("herald").getChild("body").getChild("head");
		this.crown = root.getChild("herald").getChild("body").getChild("head").getChild("crown");
		this.rArm = root.getChild("herald").getChild("body").getChild("r_arm");
		this.lArm = root.getChild("herald").getChild("body").getChild("l_arm");
		this.rFore = root.getChild("herald").getChild("body").getChild("r_arm").getChild("r_fore");
		this.lFore = root.getChild("herald").getChild("body").getChild("l_arm").getChild("l_fore");
		this.staff = root.getChild("herald").getChild("body").getChild("r_arm").getChild("r_fore").getChild("staff");
		this.skirt = root.getChild("herald").getChild("body").getChild("skirt_root").getChild("skirt");
		this.hem = root.getChild("herald").getChild("body").getChild("skirt_root").getChild("skirt").getChild("hem");
		this.tatterL = root.getChild("herald").getChild("body").getChild("tatter_l");
		this.tatterR = root.getChild("herald").getChild("body").getChild("tatter_r");
		this.shard0 = root.getChild("herald").getChild("shard0");
		this.shard1 = root.getChild("herald").getChild("shard1");
		this.shard2 = root.getChild("herald").getChild("shard2");
		this.shard3 = root.getChild("herald").getChild("shard3");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		PartDefinition p1 = root.addOrReplaceChild("herald", CubeListBuilder.create(),
				PartPose.offset(0.0f, 24.0f, 0.0f));
		PartDefinition p2 = p1.addOrReplaceChild("body", CubeListBuilder.create()
				.texOffs(60, 48).addBox(-8.0f, -14.0f, -5.0f, 16.0f, 14.0f, 10.0f, new CubeDeformation(0.0f))
				.texOffs(98, 74).addBox(-3.0f, -11.0f, -6.6f, 6.0f, 6.0f, 2.0f, new CubeDeformation(0.0f))
				.texOffs(142, 48).addBox(-9.0f, -16.0f, -6.0f, 18.0f, 4.0f, 12.0f, new CubeDeformation(0.0f))
				.texOffs(0, 48).addBox(-9.0f, -2.0f, -6.0f, 18.0f, 14.0f, 12.0f, new CubeDeformation(0.0f)),
				PartPose.offset(0.0f, -44.0f, 0.0f));
		PartDefinition p3 = p2.addOrReplaceChild("head", CubeListBuilder.create()
				.texOffs(170, 0).addBox(-7.0f, -14.0f, -7.0f, 14.0f, 14.0f, 14.0f, new CubeDeformation(0.0f))
				.texOffs(226, 48).addBox(-4.0f, -17.0f, 0.0f, 8.0f, 6.0f, 6.0f, new CubeDeformation(0.0f))
				.texOffs(76, 74).addBox(-5.0f, -11.0f, -7.2f, 10.0f, 8.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offset(0.0f, -16.0f, -1.0f));
		PartDefinition p4 = p3.addOrReplaceChild("crown", CubeListBuilder.create(),
				PartPose.ZERO);
		p4.addOrReplaceChild("crown0", CubeListBuilder.create()
				.texOffs(36, 74).addBox(-1.0f, -9.0f, -1.0f, 2.0f, 9.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, -14.0f, -5.0f, -0.2f, 0.0f, 0.0f));
		p4.addOrReplaceChild("crown1", CubeListBuilder.create()
				.texOffs(114, 74).addBox(-1.0f, -6.0f, -1.0f, 2.0f, 6.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(-4.0f, -14.0f, -3.0f, -0.15f, 0.0f, -0.35f));
		p4.addOrReplaceChild("crown2", CubeListBuilder.create()
				.texOffs(36, 74).addBox(-1.0f, -9.0f, -1.0f, 2.0f, 9.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(4.0f, -14.0f, -3.0f, -0.15f, 0.0f, 0.35f));
		p4.addOrReplaceChild("crown3", CubeListBuilder.create()
				.texOffs(114, 74).addBox(-1.0f, -6.0f, -1.0f, 2.0f, 6.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(-5.5f, -14.0f, 1.0f, 0.1f, 0.0f, -0.6f));
		p4.addOrReplaceChild("crown4", CubeListBuilder.create()
				.texOffs(36, 74).addBox(-1.0f, -9.0f, -1.0f, 2.0f, 9.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(5.5f, -14.0f, 1.0f, 0.1f, 0.0f, 0.6f));
		PartDefinition p10 = p2.addOrReplaceChild("r_arm", CubeListBuilder.create()
				.texOffs(126, 48).addBox(-2.0f, -1.0f, -2.0f, 4.0f, 14.0f, 4.0f, new CubeDeformation(0.0f))
				.texOffs(0, 74).addBox(-3.0f, -5.0f, -3.0f, 6.0f, 5.0f, 6.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(-11.0f, -13.0f, 0.0f, 0.0f, 0.0f, 0.32f));
		p10.addOrReplaceChild("r_spike", CubeListBuilder.create()
				.texOffs(24, 74).addBox(-1.5f, -8.0f, -1.5f, 3.0f, 8.0f, 3.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(-1.5f, -4.0f, 0.0f, -0.2f, 0.0f, -0.35f));
		PartDefinition p12 = p10.addOrReplaceChild("r_fore", CubeListBuilder.create()
				.texOffs(202, 48).addBox(-2.0f, 0.0f, -2.0f, 4.0f, 12.0f, 4.0f, new CubeDeformation(0.0f))
				.texOffs(122, 74).addBox(-2.0f, 11.0f, -2.0f, 1.0f, 6.0f, 1.0f, new CubeDeformation(0.0f))
				.texOffs(122, 74).addBox(0.5f, 11.0f, -2.0f, 1.0f, 6.0f, 1.0f, new CubeDeformation(0.0f))
				.texOffs(122, 74).addBox(-0.5f, 11.0f, 1.0f, 1.0f, 6.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, 12.0f, 0.0f, -0.3f, 0.0f, 0.0f));
		p12.addOrReplaceChild("staff", CubeListBuilder.create()
				.texOffs(0, 0).addBox(-1.0f, -40.0f, -1.0f, 2.0f, 46.0f, 2.0f, new CubeDeformation(0.0f))
				.texOffs(218, 48).addBox(-4.0f, -52.0f, -1.0f, 2.0f, 12.0f, 2.0f, new CubeDeformation(0.0f))
				.texOffs(218, 48).addBox(2.0f, -52.0f, -1.0f, 2.0f, 12.0f, 2.0f, new CubeDeformation(0.0f))
				.texOffs(126, 74).addBox(-4.0f, -42.0f, -1.0f, 8.0f, 2.0f, 2.0f, new CubeDeformation(0.0f))
				.texOffs(44, 74).addBox(-2.5f, -49.5f, -2.5f, 5.0f, 5.0f, 5.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, 11.0f, -1.0f, 0.3f, 0.0f, 0.0f));
		PartDefinition p14 = p2.addOrReplaceChild("l_arm", CubeListBuilder.create()
				.texOffs(126, 48).addBox(-2.0f, -1.0f, -2.0f, 4.0f, 14.0f, 4.0f, new CubeDeformation(0.0f))
				.texOffs(0, 74).addBox(-3.0f, -5.0f, -3.0f, 6.0f, 5.0f, 6.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(11.0f, -13.0f, 0.0f, 0.0f, 0.0f, -0.15f));
		p14.addOrReplaceChild("l_spike", CubeListBuilder.create()
				.texOffs(24, 74).addBox(-1.5f, -8.0f, -1.5f, 3.0f, 8.0f, 3.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(1.5f, -4.0f, 0.0f, -0.2f, 0.0f, 0.35f));
		p14.addOrReplaceChild("l_fore", CubeListBuilder.create()
				.texOffs(202, 48).addBox(-2.0f, 0.0f, -2.0f, 4.0f, 12.0f, 4.0f, new CubeDeformation(0.0f))
				.texOffs(122, 74).addBox(-2.0f, 11.0f, -2.0f, 1.0f, 6.0f, 1.0f, new CubeDeformation(0.0f))
				.texOffs(122, 74).addBox(0.5f, 11.0f, -2.0f, 1.0f, 6.0f, 1.0f, new CubeDeformation(0.0f))
				.texOffs(122, 74).addBox(-0.5f, 11.0f, 1.0f, 1.0f, 6.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, 12.0f, 0.0f, -0.3f, 0.0f, 0.0f));
		PartDefinition p17 = p2.addOrReplaceChild("skirt_root", CubeListBuilder.create(),
				PartPose.offset(0.0f, 11.0f, 0.0f));
		PartDefinition p18 = p17.addOrReplaceChild("skirt", CubeListBuilder.create()
				.texOffs(96, 0).addBox(-11.0f, 0.0f, -7.5f, 22.0f, 14.0f, 15.0f, new CubeDeformation(0.0f)),
				PartPose.ZERO);
		p18.addOrReplaceChild("hem", CubeListBuilder.create()
				.texOffs(8, 0).addBox(-13.0f, 0.0f, -9.0f, 26.0f, 14.0f, 18.0f, new CubeDeformation(0.0f)),
				PartPose.offset(0.0f, 13.0f, 0.0f));
		p2.addOrReplaceChild("tatter_l", CubeListBuilder.create()
				.texOffs(112, 48).addBox(-3.0f, 0.0f, 0.0f, 6.0f, 22.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(5.0f, -14.0f, 5.2f, 0.15f, 0.0f, 0.0f));
		p2.addOrReplaceChild("tatter_r", CubeListBuilder.create()
				.texOffs(112, 48).addBox(-3.0f, 0.0f, 0.0f, 6.0f, 22.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(-5.0f, -14.0f, 5.2f, 0.18f, 0.0f, 0.0f));
		p1.addOrReplaceChild("shard0", CubeListBuilder.create()
				.texOffs(64, 74).addBox(-1.5f, -3.5f, -1.5f, 3.0f, 7.0f, 3.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, -40.0f, 0.0f, 0.3f, 0.0f, 0.4f));
		p1.addOrReplaceChild("shard1", CubeListBuilder.create()
				.texOffs(64, 74).addBox(-1.5f, -3.5f, -1.5f, 3.0f, 7.0f, 3.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, -40.0f, 0.0f, 0.3f, 0.0f, 0.4f));
		p1.addOrReplaceChild("shard2", CubeListBuilder.create()
				.texOffs(64, 74).addBox(-1.5f, -3.5f, -1.5f, 3.0f, 7.0f, 3.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, -40.0f, 0.0f, 0.3f, 0.0f, 0.4f));
		p1.addOrReplaceChild("shard3", CubeListBuilder.create()
				.texOffs(64, 74).addBox(-1.5f, -3.5f, -1.5f, 3.0f, 7.0f, 3.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, -40.0f, 0.0f, 0.3f, 0.0f, 0.4f));
		return LayerDefinition.create(mesh, 256, 128);
	}

	@Override
	public void setupAnim(StormHeraldRenderState state) {
		super.setupAnim(state);
		float age = state.ageInTicks;
		float p = state.progress;
		head.yRot += Mth.clamp(state.yRot, -50.0f, 50.0f) * Mth.DEG_TO_RAD;
		head.xRot += state.xRot * Mth.DEG_TO_RAD * 0.5f;

		// Schweben: Wogen, Robe und Fetzen wehen nach
		float speed = Math.min(state.walkAnimationSpeed, 1.0f);
		herald.y += Mth.sin(age * 0.08f) * 1.6f;
		body.xRot += speed * 0.2f;
		skirt.xRot += Mth.sin(age * 0.08f - 0.6f) * 0.05f + speed * 0.25f;
		hem.xRot += Mth.sin(age * 0.08f - 1.2f) * 0.06f + speed * 0.15f;
		skirt.zRot += Mth.sin(age * 0.05f) * 0.03f;
		tatterL.xRot += 0.15f + Mth.sin(age * 0.11f) * 0.12f + speed * 0.4f;
		tatterR.xRot += 0.15f + Mth.sin(age * 0.11f + 1.3f) * 0.12f + speed * 0.4f;
		rArm.zRot += Mth.sin(age * 0.06f) * 0.04f;
		lArm.zRot -= Mth.sin(age * 0.06f) * 0.04f;
		lArm.xRot += Mth.sin(age * 0.05f) * 0.06f;
		crown.yRot += Mth.sin(age * 0.04f) * 0.05f;

		// kreisende Splitter
		ModelPart[] shards = {shard0, shard1, shard2, shard3};
		float spin = state.anim == dev.bartels.jtechstorage.entity.StormHerald.ANIM_SUMMON ? 0.18f : 0.05f;

		for (int k = 0; k < 4; k++) {
			float angle = age * spin + k * Mth.HALF_PI;
			shards[k].x = Mth.cos(angle) * 18.0f;
			shards[k].z = Mth.sin(angle) * 18.0f;
			shards[k].y += Mth.sin(age * 0.1f + k * 1.7f) * 3.0f + (k % 2) * 4.0f;
			shards[k].yRot = -angle;
		}

		switch (state.anim) {
			case dev.bartels.jtechstorage.entity.StormHerald.ANIM_BOLT -> {
				float up = p < 0.5f ? Mth.sin(p * 2.0f * Mth.HALF_PI) : 1.0f - (p - 0.5f) * 1.6f;
				poseBolt(Math.max(0.0f, up));
			}
			case dev.bartels.jtechstorage.entity.StormHerald.ANIM_SUMMON -> poseSummon(Mth.sin(Math.min(1.0f, p * 1.4f) * Mth.PI * 0.5f)
					* Math.min(1.0f, (1.0f - p) * 4.0f));
			default -> {
			}
		}
	}

	private void poseBolt(float k) {
		rArm.xRot += k * -2.600f;
		rArm.zRot += k * 0.200f;
		rFore.xRot += k * 0.200f;
		staff.xRot += k * -0.600f;
		head.xRot += k * -0.250f;
		body.xRot += k * -0.100f;
		lArm.xRot += k * -0.400f;
		lArm.zRot += k * -0.500f;
	}

	private void poseSummon(float k) {
		rArm.zRot += k * 1.300f;
		rArm.xRot += k * -0.300f;
		lArm.zRot += k * -1.500f;
		lArm.xRot += k * -0.300f;
		rFore.xRot += k * 0.200f;
		lFore.xRot += k * 0.200f;
		head.xRot += k * -0.350f;
		body.xRot += k * -0.120f;
		skirt.xRot += k * 0.100f;
	}


}
