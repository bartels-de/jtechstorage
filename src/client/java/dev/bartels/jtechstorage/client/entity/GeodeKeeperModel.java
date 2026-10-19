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
 * Geodenhüter (NPC, v0.25): alter Einsiedler in Amethyst-Geoden, der einen Begleiter schenkt.
 * Modell in doppelter Detaildichte (halbe Pixel) – der Renderer skaliert es auf 0.5.
 * Erzeugt von scratchpad/keeper.py (Teile und Posen), Animation von Hand.
 */
public class GeodeKeeperModel extends EntityModel<GeodeKeeperRenderState> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(JTechStorage.id("geode_keeper"), "main");

	private final ModelPart keeper;
	private final ModelPart body;
	private final ModelPart head;
	private final ModelPart beard;
	private final ModelPart rArm;
	private final ModelPart lArm;
	private final ModelPart staff;
	private final ModelPart geoIn;

	public GeodeKeeperModel(ModelPart root) {
		super(root);
		this.keeper = root.getChild("keeper");
		this.body = root.getChild("keeper").getChild("body");
		this.head = root.getChild("keeper").getChild("body").getChild("head");
		this.beard = root.getChild("keeper").getChild("body").getChild("head").getChild("beard");
		this.rArm = root.getChild("keeper").getChild("body").getChild("r_arm");
		this.lArm = root.getChild("keeper").getChild("body").getChild("l_arm");
		this.staff = root.getChild("keeper").getChild("body").getChild("r_arm").getChild("staff");
		this.geoIn = root.getChild("keeper").getChild("body").getChild("r_arm").getChild("staff").getChild("geo_in");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		PartDefinition p1 = root.addOrReplaceChild("keeper", CubeListBuilder.create(),
				PartPose.ZERO);
		PartDefinition p2 = p1.addOrReplaceChild("body", CubeListBuilder.create()
				.texOffs(100, 0).addBox(-6.0f, -14.0f, -4.0f, 12.0f, 14.0f, 8.0f, new CubeDeformation(0.0f))
				.texOffs(0, 48).addBox(-6.5f, -1.0f, -4.5f, 13.0f, 2.0f, 9.0f, new CubeDeformation(0.0f)),
				PartPose.offset(0.0f, 11.5f, 0.0f));
		PartDefinition p3 = p2.addOrReplaceChild("head", CubeListBuilder.create()
				.texOffs(160, 0).addBox(-4.0f, -8.0f, -4.0f, 8.0f, 8.0f, 8.0f, new CubeDeformation(0.0f))
				.texOffs(184, 48).addBox(-1.0f, -4.0f, -5.0f, 2.0f, 2.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offset(0.0f, -14.0f, 0.0f));
		PartDefinition p4 = p3.addOrReplaceChild("hood", CubeListBuilder.create()
				.texOffs(56, 0).addBox(-5.5f, -10.5f, -5.5f, 11.0f, 11.0f, 11.0f, new CubeDeformation(0.0f))
				.texOffs(78, 48).addBox(-2.5f, -3.0f, 3.0f, 5.0f, 5.0f, 4.0f, new CubeDeformation(0.0f)),
				PartPose.ZERO);
		p4.addOrReplaceChild("hood_cr0", CubeListBuilder.create()
				.texOffs(112, 48).addBox(-1.5f, -6.0f, -1.5f, 3.0f, 6.0f, 3.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(3.0f, -10.0f, 2.0f, -0.3f, 0.0f, 0.5f));
		p4.addOrReplaceChild("hood_cr1", CubeListBuilder.create()
				.texOffs(176, 48).addBox(-1.0f, -4.0f, -1.0f, 2.0f, 4.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(-2.0f, -10.0f, 3.0f, -0.5f, 0.0f, -0.4f));
		p3.addOrReplaceChild("beard", CubeListBuilder.create()
				.texOffs(224, 0).addBox(-3.5f, 0.0f, -1.0f, 7.0f, 10.0f, 2.0f, new CubeDeformation(0.0f))
				.texOffs(152, 48).addBox(-2.0f, 10.0f, -1.0f, 4.0f, 5.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, -3.0f, -4.5f, 0.15f, 0.0f, 0.0f));
		PartDefinition p8 = p2.addOrReplaceChild("r_arm", CubeListBuilder.create()
				.texOffs(140, 0).addBox(-2.5f, -1.0f, -2.5f, 5.0f, 13.0f, 5.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(-7.5f, -12.0f, 0.0f, -1.2f, 0.0f, 0.0f));
		p8.addOrReplaceChild("r_hand", CubeListBuilder.create()
				.texOffs(164, 48).addBox(-1.5f, 0.0f, -1.5f, 3.0f, 3.0f, 3.0f, new CubeDeformation(0.0f)),
				PartPose.offset(0.0f, 12.0f, -0.5f));
		PartDefinition p10 = p8.addOrReplaceChild("staff", CubeListBuilder.create()
				.texOffs(0, 0).addBox(-1.0f, -30.0f, -1.0f, 2.0f, 46.0f, 2.0f, new CubeDeformation(0.0f))
				.texOffs(192, 0).addBox(-4.0f, -38.0f, -4.0f, 8.0f, 8.0f, 8.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, 13.0f, -1.5f, 1.2f, 0.0f, 0.0f));
		p10.addOrReplaceChild("geo_in", CubeListBuilder.create()
				.texOffs(138, 48).addBox(-3.0f, -3.0f, -0.5f, 6.0f, 6.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offset(0.0f, -34.0f, -4.2f));
		p10.addOrReplaceChild("geo_cr0", CubeListBuilder.create()
				.texOffs(112, 48).addBox(-1.5f, -6.0f, -1.5f, 3.0f, 6.0f, 3.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, -38.0f, 0.0f, 0.0f, 0.0f, 0.2f));
		p10.addOrReplaceChild("geo_cr1", CubeListBuilder.create()
				.texOffs(176, 48).addBox(-1.0f, -4.0f, -1.0f, 2.0f, 4.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(3.0f, -37.0f, -2.0f, -0.3f, 0.0f, 0.6f));
		PartDefinition p14 = p2.addOrReplaceChild("l_arm", CubeListBuilder.create()
				.texOffs(140, 0).addBox(-2.5f, -1.0f, -2.5f, 5.0f, 13.0f, 5.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(7.5f, -12.0f, 0.0f, 0.1f, 0.0f, -0.12f));
		p14.addOrReplaceChild("l_hand", CubeListBuilder.create()
				.texOffs(164, 48).addBox(-1.5f, 0.0f, -1.5f, 3.0f, 3.0f, 3.0f, new CubeDeformation(0.0f)),
				PartPose.offset(0.0f, 12.0f, -0.5f));
		p2.addOrReplaceChild("r_shoulder_cr", CubeListBuilder.create()
				.texOffs(44, 48).addBox(-1.5f, -8.0f, -1.5f, 3.0f, 8.0f, 3.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(-6.0f, -13.0f, 1.0f, -0.2f, 0.0f, -0.5f));
		p2.addOrReplaceChild("l_shoulder_cr", CubeListBuilder.create()
				.texOffs(112, 48).addBox(-1.5f, -6.0f, -1.5f, 3.0f, 6.0f, 3.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(6.0f, -13.0f, 2.0f, -0.1f, 0.0f, 0.6f));
		p2.addOrReplaceChild("back_cr", CubeListBuilder.create()
				.texOffs(176, 48).addBox(-1.0f, -4.0f, -1.0f, 2.0f, 4.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(2.0f, -8.0f, 4.0f, 0.8f, 0.0f, 0.3f));
		p2.addOrReplaceChild("lantern", CubeListBuilder.create()
				.texOffs(96, 48).addBox(-2.0f, 0.0f, -2.0f, 4.0f, 5.0f, 4.0f, new CubeDeformation(0.0f)),
				PartPose.offset(5.5f, 0.5f, -3.0f));
		p2.addOrReplaceChild("pouch", CubeListBuilder.create()
				.texOffs(124, 48).addBox(-2.0f, 0.0f, -1.5f, 4.0f, 5.0f, 3.0f, new CubeDeformation(0.0f)),
				PartPose.offset(-5.5f, 0.0f, 2.0f));
		p1.addOrReplaceChild("skirt", CubeListBuilder.create()
				.texOffs(8, 0).addBox(-7.0f, 0.0f, -5.0f, 14.0f, 12.0f, 10.0f, new CubeDeformation(0.0f))
				.texOffs(56, 48).addBox(-6.0f, 9.5f, -6.0f, 5.0f, 3.0f, 6.0f, new CubeDeformation(0.0f))
				.texOffs(56, 48).addBox(1.0f, 9.5f, -6.0f, 5.0f, 3.0f, 6.0f, new CubeDeformation(0.0f)),
				PartPose.offset(0.0f, 11.5f, 0.0f));
		return LayerDefinition.create(mesh, 256, 128);
	}

	@Override
	public void setupAnim(GeodeKeeperRenderState state) {
		super.setupAnim(state);
		float age = state.ageInTicks;
		float breathe = Mth.sin(age * 0.05f);
		body.y += breathe * 0.4f;
		head.y -= breathe * 0.15f;
		beard.xRot += Mth.sin(age * 0.07f) * 0.04f;
		float yaw = Mth.clamp(state.yRot, -60.0f, 60.0f) * Mth.DEG_TO_RAD;
		head.yRot += yaw * 0.8f;
		head.xRot += state.xRot * Mth.DEG_TO_RAD * 0.6f;
		staff.zRot += Mth.sin(age * 0.03f) * 0.03f;
		geoIn.visible = true;

		if (state.offer > 0.0f) {
			poseOffer(state.offer);
			lArm.zRot += Mth.sin(age * 0.2f) * 0.05f * state.offer;
		}
	}

	private void poseOffer(float k) {
		lArm.xRot += k * -1.350f;
		lArm.zRot += k * 0.150f;
		head.xRot += k * 0.150f;
	}


}
