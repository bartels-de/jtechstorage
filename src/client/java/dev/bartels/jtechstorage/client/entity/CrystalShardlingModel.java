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
 * Kristall-Splitterling: kauernder Krabbler aus verderbtem Amethyst mit glühenden Rissen, Rückensplittern, vier
 * glühenden Augen, Splitterzähnen, spitzen Kristallbeinen und Splitterschwanz.
 * Modell in doppelter Detaildichte (halbe Pixel) – der Renderer skaliert es auf 0.5.
 * Erzeugt von scratchpad/shardling.py (Teile und Posen), Animation von Hand.
 */
public class CrystalShardlingModel extends EntityModel<CrystalShardlingRenderState> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(JTechStorage.id("crystal_shardling"), "main");

	private final ModelPart shardling;
	private final ModelPart body;
	private final ModelPart head;
	private final ModelPart jaw;
	private final ModelPart rfLeg;
	private final ModelPart lfLeg;
	private final ModelPart rhLeg;
	private final ModelPart lhLeg;
	private final ModelPart rfFoot;
	private final ModelPart lfFoot;
	private final ModelPart rhFoot;
	private final ModelPart lhFoot;
	private final ModelPart tail;
	private final ModelPart tailTip;

	public CrystalShardlingModel(ModelPart root) {
		super(root);
		this.shardling = root.getChild("shardling");
		this.body = root.getChild("shardling").getChild("body");
		this.head = root.getChild("shardling").getChild("body").getChild("head");
		this.jaw = root.getChild("shardling").getChild("body").getChild("head").getChild("jaw");
		this.rfLeg = root.getChild("shardling").getChild("body").getChild("rf_leg");
		this.lfLeg = root.getChild("shardling").getChild("body").getChild("lf_leg");
		this.rhLeg = root.getChild("shardling").getChild("body").getChild("rh_leg");
		this.lhLeg = root.getChild("shardling").getChild("body").getChild("lh_leg");
		this.rfFoot = root.getChild("shardling").getChild("body").getChild("rf_leg").getChild("rf_foot");
		this.lfFoot = root.getChild("shardling").getChild("body").getChild("lf_leg").getChild("lf_foot");
		this.rhFoot = root.getChild("shardling").getChild("body").getChild("rh_leg").getChild("rh_foot");
		this.lhFoot = root.getChild("shardling").getChild("body").getChild("lh_leg").getChild("lh_foot");
		this.tail = root.getChild("shardling").getChild("body").getChild("tail");
		this.tailTip = root.getChild("shardling").getChild("body").getChild("tail").getChild("tail_tip");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		PartDefinition p1 = root.addOrReplaceChild("shardling", CubeListBuilder.create(),
				PartPose.offset(0.0f, 24.0f, 0.0f));
		PartDefinition p2 = p1.addOrReplaceChild("body", CubeListBuilder.create()
				.texOffs(0, 0).addBox(-5.0f, -5.0f, -7.0f, 10.0f, 8.0f, 14.0f, new CubeDeformation(0.0f)),
				PartPose.offset(0.0f, -12.7f, 0.0f));
		PartDefinition p3 = p2.addOrReplaceChild("head", CubeListBuilder.create()
				.texOffs(48, 0).addBox(-5.0f, -5.0f, -7.0f, 10.0f, 7.0f, 8.0f, new CubeDeformation(0.0f))
				.texOffs(68, 22).addBox(-4.5f, -8.0f, -4.0f, 2.0f, 4.0f, 2.0f, new CubeDeformation(0.0f))
				.texOffs(68, 22).addBox(2.5f, -8.0f, -4.0f, 2.0f, 4.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offset(0.0f, -3.0f, -7.0f));
		p3.addOrReplaceChild("jaw", CubeListBuilder.create()
				.texOffs(0, 22).addBox(-4.0f, 0.0f, -6.0f, 8.0f, 3.0f, 6.0f, new CubeDeformation(0.0f))
				.texOffs(76, 22).addBox(-3.0f, -2.0f, -6.0f, 1.0f, 2.0f, 1.0f, new CubeDeformation(0.0f))
				.texOffs(76, 22).addBox(-1.0f, -2.0f, -6.2f, 1.0f, 2.0f, 1.0f, new CubeDeformation(0.0f))
				.texOffs(76, 22).addBox(1.0f, -2.0f, -6.2f, 1.0f, 2.0f, 1.0f, new CubeDeformation(0.0f))
				.texOffs(76, 22).addBox(2.0f, -2.0f, -6.0f, 1.0f, 2.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offset(0.0f, 1.0f, -1.0f));
		p2.addOrReplaceChild("spike0", CubeListBuilder.create()
				.texOffs(84, 0).addBox(-1.5f, -9.0f, -1.5f, 3.0f, 9.0f, 3.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, -4.5f, -2.0f, -0.35f, 0.0f, 0.1f));
		p2.addOrReplaceChild("spike1", CubeListBuilder.create()
				.texOffs(28, 22).addBox(-1.0f, -7.0f, -1.0f, 2.0f, 7.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(-3.0f, -4.0f, 1.0f, -0.2f, 0.0f, -0.5f));
		p2.addOrReplaceChild("spike2", CubeListBuilder.create()
				.texOffs(28, 22).addBox(-1.0f, -7.0f, -1.0f, 2.0f, 7.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(3.0f, -4.0f, 0.0f, -0.3f, 0.0f, 0.55f));
		p2.addOrReplaceChild("spike3", CubeListBuilder.create()
				.texOffs(60, 22).addBox(-1.0f, -5.0f, -1.0f, 2.0f, 5.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, -4.0f, 4.0f, 0.3f, 0.0f, -0.2f));
		p2.addOrReplaceChild("spike4", CubeListBuilder.create()
				.texOffs(60, 22).addBox(-1.0f, -5.0f, -1.0f, 2.0f, 5.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(-3.5f, -3.5f, -4.0f, -0.5f, 0.0f, -0.7f));
		PartDefinition p10 = p2.addOrReplaceChild("rf_leg", CubeListBuilder.create()
				.texOffs(36, 22).addBox(-1.0f, 0.0f, -1.0f, 2.0f, 7.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(-4.0f, 1.0f, -5.0f, 0.0f, 0.0f, 0.9f));
		p10.addOrReplaceChild("rf_foot", CubeListBuilder.create()
				.texOffs(116, 0).addBox(-1.0f, 0.0f, -1.0f, 2.0f, 8.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, 6.5f, 0.0f, 0.0f, 0.0f, -1.35f));
		PartDefinition p12 = p2.addOrReplaceChild("lf_leg", CubeListBuilder.create()
				.texOffs(36, 22).addBox(-1.0f, 0.0f, -1.0f, 2.0f, 7.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(4.0f, 1.0f, -5.0f, 0.0f, 0.0f, -0.9f));
		p12.addOrReplaceChild("lf_foot", CubeListBuilder.create()
				.texOffs(116, 0).addBox(-1.0f, 0.0f, -1.0f, 2.0f, 8.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, 6.5f, 0.0f, 0.0f, 0.0f, 1.35f));
		PartDefinition p14 = p2.addOrReplaceChild("rh_leg", CubeListBuilder.create()
				.texOffs(36, 22).addBox(-1.0f, 0.0f, -1.0f, 2.0f, 7.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(-4.0f, 1.0f, 5.0f, 0.0f, 0.0f, 0.9f));
		p14.addOrReplaceChild("rh_foot", CubeListBuilder.create()
				.texOffs(116, 0).addBox(-1.0f, 0.0f, -1.0f, 2.0f, 8.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, 6.5f, 0.0f, 0.0f, 0.0f, -1.35f));
		PartDefinition p16 = p2.addOrReplaceChild("lh_leg", CubeListBuilder.create()
				.texOffs(36, 22).addBox(-1.0f, 0.0f, -1.0f, 2.0f, 7.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(4.0f, 1.0f, 5.0f, 0.0f, 0.0f, -0.9f));
		p16.addOrReplaceChild("lh_foot", CubeListBuilder.create()
				.texOffs(116, 0).addBox(-1.0f, 0.0f, -1.0f, 2.0f, 8.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, 6.5f, 0.0f, 0.0f, 0.0f, 1.35f));
		PartDefinition p18 = p2.addOrReplaceChild("tail", CubeListBuilder.create()
				.texOffs(96, 0).addBox(-2.0f, -2.0f, 0.0f, 4.0f, 4.0f, 6.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, -1.0f, 6.5f, 0.35f, 0.0f, 0.0f));
		p18.addOrReplaceChild("tail_tip", CubeListBuilder.create()
				.texOffs(44, 22).addBox(-1.0f, -1.0f, 0.0f, 2.0f, 2.0f, 6.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, 0.0f, 5.5f, -0.3f, 0.0f, 0.0f));
		return LayerDefinition.create(mesh, 128, 64);
	}

	@Override
	public void setupAnim(CrystalShardlingRenderState state) {
		super.setupAnim(state);
		float age = state.ageInTicks;
		float pos = state.walkAnimationPos;
		float speed = Math.min(state.walkAnimationSpeed, 1.0f);
		head.yRot += Mth.clamp(state.yRot, -40.0f, 40.0f) * Mth.DEG_TO_RAD;
		head.xRot += state.xRot * Mth.DEG_TO_RAD * 0.5f;

		// hastiges Krabbeln: diagonale Beinpaare im Wechsel
		float a = Mth.cos(pos * 1.4f) * 0.55f * speed;
		float lift = Mth.sin(pos * 1.4f) * 0.35f * speed;
		rfLeg.yRot += a;
		lhLeg.yRot += a;
		lfLeg.yRot += a;
		rhLeg.yRot += a;
		rfLeg.zRot += Math.max(0.0f, lift);
		lhLeg.zRot -= Math.max(0.0f, lift);
		lfLeg.zRot -= Math.max(0.0f, -lift);
		rhLeg.zRot += Math.max(0.0f, -lift);
		body.y += Mth.abs(Mth.sin(pos * 1.4f)) * -0.8f * speed;

		// Kiefer klappert, Schwanz zuckt
		jaw.xRot += Math.max(0.0f, Mth.sin(age * 0.5f)) * 0.15f;
		tail.yRot += Mth.sin(age * 0.3f) * 0.25f;
		tailTip.yRot += Mth.sin(age * 0.3f - 0.8f) * 0.3f;
		body.y += Mth.sin(age * 0.2f) * 0.3f;

		if (state.pounce > 0.0f) {
			posePounce(Mth.sin(state.pounce * Mth.PI));
		}
	}

	private void posePounce(float k) {
		body.xRot += k * -0.300f;
		body.y += k * -2.000f;
		head.xRot += k * -0.250f;
		jaw.xRot += k * 0.700f;
		rfLeg.xRot += k * -0.800f;
		lfLeg.xRot += k * -0.800f;
		rhLeg.xRot += k * 0.600f;
		lhLeg.xRot += k * 0.600f;
		tail.xRot += k * -0.300f;
	}


}
