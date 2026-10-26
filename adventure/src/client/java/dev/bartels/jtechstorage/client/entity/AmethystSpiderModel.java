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
import dev.bartels.jtechstorage.entity.AmethystSpider;

/**
 * Great Amethyst Spider: obsidian carapace with glowing amethyst veins, abdomen with segment bands, crystal druses
 * and hourglass mark, eight legs with spikes, crystal knees and claws, eight glowing eyes, crystal fangs.
 * Model built at double detail density (half pixels) – the renderer scales it to 0.85. Without a target it lurks crouched.
 */
public class AmethystSpiderModel extends EntityModel<AmethystSpiderRenderState> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(JTechStorage.id("amethyst_spider"), "main");

	private final ModelPart body;
	private final ModelPart head;
	private final ModelPart abdomen;
	private final ModelPart fangLeft;
	private final ModelPart fangRight;
	private final ModelPart lids;
	/** [0..3] left (front → back), [4..7] right. */
	private final ModelPart[] legs = new ModelPart[8];
	private final ModelPart[] lowers = new ModelPart[8];

	public AmethystSpiderModel(ModelPart root) {
		super(root);
		this.body = root.getChild("body");
		this.head = body.getChild("head");
		this.abdomen = body.getChild("abdomen");
		this.fangLeft = head.getChild("fang_l");
		this.fangRight = head.getChild("fang_r");
		this.lids = head.getChild("lids");

		for (int k = 0; k < 4; k++) {
			legs[k] = body.getChild("left_leg" + (k + 1));
			legs[k + 4] = body.getChild("right_leg" + (k + 1));
			lowers[k] = legs[k].getChild("left_leg" + (k + 1) + "_low");
			lowers[k + 4] = legs[k + 4].getChild("right_leg" + (k + 1) + "_low");
		}
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		PartDefinition p1 = root.addOrReplaceChild("body", CubeListBuilder.create()
				.texOffs(152, 0).addBox(-12.0f, -8.0f, -16.0f, 24.0f, 14.0f, 20.0f, new CubeDeformation(0.0f))
				.texOffs(424, 0).addBox(-3.0f, -11.0f, -14.0f, 6.0f, 4.0f, 16.0f, new CubeDeformation(0.0f)),
				PartPose.offset(0.0f, -1.0f, 0.0f));
		PartDefinition p2 = p1.addOrReplaceChild("head", CubeListBuilder.create()
				.texOffs(344, 0).addBox(-8.0f, -7.0f, -12.0f, 16.0f, 14.0f, 12.0f, new CubeDeformation(0.0f))
				.texOffs(354, 66).addBox(-8.0f, -8.0f, -12.6f, 16.0f, 2.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offset(0.0f, -2.0f, -16.0f));
		p2.addOrReplaceChild("lids", CubeListBuilder.create()
				.texOffs(390, 66).addBox(-7.0f, -5.5f, -12.6f, 14.0f, 3.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.ZERO);
		p2.addOrReplaceChild("fang_r", CubeListBuilder.create()
				.texOffs(108, 66).addBox(-2.5f, 0.0f, -2.5f, 5.0f, 6.0f, 5.0f, new CubeDeformation(0.0f))
				.texOffs(176, 66).addBox(-1.5f, 5.0f, -2.0f, 3.0f, 7.0f, 3.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(-4.0f, 5.0f, -10.0f, -0.35f, 0.0f, 0.15f));
		p2.addOrReplaceChild("fang_l", CubeListBuilder.create()
				.texOffs(108, 66).mirror().addBox(-2.5f, 0.0f, -2.5f, 5.0f, 6.0f, 5.0f, new CubeDeformation(0.0f)).mirror(false)
				.texOffs(176, 66).addBox(-1.5f, 5.0f, -2.0f, 3.0f, 7.0f, 3.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(4.0f, 5.0f, -10.0f, -0.35f, 0.0f, -0.15f));
		p2.addOrReplaceChild("palp_r", CubeListBuilder.create()
				.texOffs(468, 0).addBox(-2.0f, -2.0f, -12.0f, 4.0f, 4.0f, 12.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(-7.0f, 4.0f, -8.0f, 0.3f, 0.25f, 0.0f));
		p2.addOrReplaceChild("palp_l", CubeListBuilder.create()
				.texOffs(468, 0).mirror().addBox(-2.0f, -2.0f, -12.0f, 4.0f, 4.0f, 12.0f, new CubeDeformation(0.0f)).mirror(false),
				PartPose.offsetAndRotation(7.0f, 4.0f, -8.0f, 0.3f, -0.25f, 0.0f));
		PartDefinition p8 = p1.addOrReplaceChild("abdomen", CubeListBuilder.create()
				.texOffs(0, 0).addBox(-18.0f, -24.0f, 0.0f, 36.0f, 26.0f, 40.0f, new CubeDeformation(0.0f))
				.texOffs(272, 66).addBox(-19.0f, -24.5f, 12.0f, 38.0f, 3.0f, 3.0f, new CubeDeformation(0.1f))
				.texOffs(272, 66).addBox(-19.0f, -24.5f, 26.0f, 38.0f, 3.0f, 3.0f, new CubeDeformation(0.1f))
				.texOffs(240, 0).addBox(-4.0f, -28.0f, 6.0f, 8.0f, 6.0f, 28.0f, new CubeDeformation(0.0f))
				.texOffs(128, 66).addBox(-6.0f, -14.0f, 39.0f, 12.0f, 6.0f, 4.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, 2.0f, 2.0f, -0.22f, 0.0f, 0.0f));
		p8.addOrReplaceChild("crystal_a", CubeListBuilder.create()
				.texOffs(312, 0).addBox(-4.0f, -24.0f, -4.0f, 8.0f, 24.0f, 8.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(-5.0f, -25.0f, 18.0f, -0.25f, 0.0f, -0.3f));
		p8.addOrReplaceChild("crystal_b", CubeListBuilder.create()
				.texOffs(400, 0).addBox(-3.0f, -16.0f, -3.0f, 6.0f, 16.0f, 6.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(8.0f, -25.0f, 12.0f, -0.1f, 0.0f, 0.45f));
		p8.addOrReplaceChild("crystal_c", CubeListBuilder.create()
				.texOffs(0, 66).addBox(-2.0f, -10.0f, -2.0f, 4.0f, 10.0f, 4.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(1.0f, -25.0f, 28.0f, -0.6f, 0.0f, 0.1f));
		p8.addOrReplaceChild("crystal_d", CubeListBuilder.create()
				.texOffs(400, 0).addBox(-3.0f, -16.0f, -3.0f, 6.0f, 16.0f, 6.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(-11.0f, -23.0f, 29.0f, -0.45f, 0.0f, -0.6f));
		p8.addOrReplaceChild("crystal_e", CubeListBuilder.create()
				.texOffs(160, 66).addBox(-2.0f, -6.0f, -2.0f, 4.0f, 6.0f, 4.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(12.0f, -23.0f, 26.0f, -0.4f, 0.0f, 0.7f));
		p8.addOrReplaceChild("crystal_f", CubeListBuilder.create()
				.texOffs(0, 66).addBox(-2.0f, -10.0f, -2.0f, 4.0f, 10.0f, 4.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(15.0f, -20.0f, 6.0f, 0.1f, 0.0f, 0.9f));
		p8.addOrReplaceChild("crystal_g", CubeListBuilder.create()
				.texOffs(160, 66).addBox(-2.0f, -6.0f, -2.0f, 4.0f, 6.0f, 4.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(-15.0f, -20.0f, 8.0f, 0.1f, 0.0f, -0.9f));
		PartDefinition p16 = p1.addOrReplaceChild("left_leg1", CubeListBuilder.create()
				.texOffs(16, 66).addBox(0.0f, -3.0f, -3.0f, 28.0f, 6.0f, 6.0f, new CubeDeformation(0.0f))
				.texOffs(84, 66).addBox(25.0f, -3.0f, -3.0f, 6.0f, 6.0f, 6.0f, new CubeDeformation(0.0f))
				.texOffs(264, 66).addBox(8.0f, -7.0f, -1.0f, 2.0f, 5.0f, 2.0f, new CubeDeformation(0.0f))
				.texOffs(264, 66).addBox(17.0f, -7.0f, -1.0f, 2.0f, 5.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(11.0f, 0.0f, -14.0f, 0.0f, 0.75f, -0.62f));
		p16.addOrReplaceChild("left_leg1_low", CubeListBuilder.create()
				.texOffs(188, 66).addBox(0.0f, -2.0f, -2.0f, 34.0f, 4.0f, 4.0f, new CubeDeformation(0.0f))
				.texOffs(420, 66).addBox(32.0f, -1.0f, -1.0f, 8.0f, 2.0f, 2.0f, new CubeDeformation(0.0f))
				.texOffs(264, 66).addBox(12.0f, -5.0f, -1.0f, 2.0f, 5.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(28.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.9f));
		PartDefinition p18 = p1.addOrReplaceChild("left_leg2", CubeListBuilder.create()
				.texOffs(16, 66).addBox(0.0f, -3.0f, -3.0f, 28.0f, 6.0f, 6.0f, new CubeDeformation(0.0f))
				.texOffs(84, 66).addBox(25.0f, -3.0f, -3.0f, 6.0f, 6.0f, 6.0f, new CubeDeformation(0.0f))
				.texOffs(264, 66).addBox(8.0f, -7.0f, -1.0f, 2.0f, 5.0f, 2.0f, new CubeDeformation(0.0f))
				.texOffs(264, 66).addBox(17.0f, -7.0f, -1.0f, 2.0f, 5.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(11.0f, 0.0f, -9.0f, 0.0f, 0.28f, -0.62f));
		p18.addOrReplaceChild("left_leg2_low", CubeListBuilder.create()
				.texOffs(188, 66).addBox(0.0f, -2.0f, -2.0f, 34.0f, 4.0f, 4.0f, new CubeDeformation(0.0f))
				.texOffs(420, 66).addBox(32.0f, -1.0f, -1.0f, 8.0f, 2.0f, 2.0f, new CubeDeformation(0.0f))
				.texOffs(264, 66).addBox(12.0f, -5.0f, -1.0f, 2.0f, 5.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(28.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.9f));
		PartDefinition p20 = p1.addOrReplaceChild("left_leg3", CubeListBuilder.create()
				.texOffs(16, 66).addBox(0.0f, -3.0f, -3.0f, 28.0f, 6.0f, 6.0f, new CubeDeformation(0.0f))
				.texOffs(84, 66).addBox(25.0f, -3.0f, -3.0f, 6.0f, 6.0f, 6.0f, new CubeDeformation(0.0f))
				.texOffs(264, 66).addBox(8.0f, -7.0f, -1.0f, 2.0f, 5.0f, 2.0f, new CubeDeformation(0.0f))
				.texOffs(264, 66).addBox(17.0f, -7.0f, -1.0f, 2.0f, 5.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(11.0f, 0.0f, -4.0f, 0.0f, -0.22f, -0.62f));
		p20.addOrReplaceChild("left_leg3_low", CubeListBuilder.create()
				.texOffs(188, 66).addBox(0.0f, -2.0f, -2.0f, 34.0f, 4.0f, 4.0f, new CubeDeformation(0.0f))
				.texOffs(420, 66).addBox(32.0f, -1.0f, -1.0f, 8.0f, 2.0f, 2.0f, new CubeDeformation(0.0f))
				.texOffs(264, 66).addBox(12.0f, -5.0f, -1.0f, 2.0f, 5.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(28.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.9f));
		PartDefinition p22 = p1.addOrReplaceChild("left_leg4", CubeListBuilder.create()
				.texOffs(16, 66).addBox(0.0f, -3.0f, -3.0f, 28.0f, 6.0f, 6.0f, new CubeDeformation(0.0f))
				.texOffs(84, 66).addBox(25.0f, -3.0f, -3.0f, 6.0f, 6.0f, 6.0f, new CubeDeformation(0.0f))
				.texOffs(264, 66).addBox(8.0f, -7.0f, -1.0f, 2.0f, 5.0f, 2.0f, new CubeDeformation(0.0f))
				.texOffs(264, 66).addBox(17.0f, -7.0f, -1.0f, 2.0f, 5.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(11.0f, 0.0f, 1.0f, 0.0f, -0.7f, -0.62f));
		p22.addOrReplaceChild("left_leg4_low", CubeListBuilder.create()
				.texOffs(188, 66).addBox(0.0f, -2.0f, -2.0f, 34.0f, 4.0f, 4.0f, new CubeDeformation(0.0f))
				.texOffs(420, 66).addBox(32.0f, -1.0f, -1.0f, 8.0f, 2.0f, 2.0f, new CubeDeformation(0.0f))
				.texOffs(264, 66).addBox(12.0f, -5.0f, -1.0f, 2.0f, 5.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(28.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.9f));
		PartDefinition p24 = p1.addOrReplaceChild("right_leg1", CubeListBuilder.create()
				.texOffs(16, 66).mirror().addBox(-28.0f, -3.0f, -3.0f, 28.0f, 6.0f, 6.0f, new CubeDeformation(0.0f)).mirror(false)
				.texOffs(84, 66).mirror().addBox(-31.0f, -3.0f, -3.0f, 6.0f, 6.0f, 6.0f, new CubeDeformation(0.0f)).mirror(false)
				.texOffs(264, 66).addBox(-10.0f, -7.0f, -1.0f, 2.0f, 5.0f, 2.0f, new CubeDeformation(0.0f))
				.texOffs(264, 66).addBox(-19.0f, -7.0f, -1.0f, 2.0f, 5.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(-11.0f, 0.0f, -14.0f, 0.0f, -0.75f, 0.62f));
		p24.addOrReplaceChild("right_leg1_low", CubeListBuilder.create()
				.texOffs(188, 66).mirror().addBox(-34.0f, -2.0f, -2.0f, 34.0f, 4.0f, 4.0f, new CubeDeformation(0.0f)).mirror(false)
				.texOffs(420, 66).mirror().addBox(-40.0f, -1.0f, -1.0f, 8.0f, 2.0f, 2.0f, new CubeDeformation(0.0f)).mirror(false)
				.texOffs(264, 66).addBox(-14.0f, -5.0f, -1.0f, 2.0f, 5.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(-28.0f, 0.0f, 0.0f, 0.0f, 0.0f, -1.9f));
		PartDefinition p26 = p1.addOrReplaceChild("right_leg2", CubeListBuilder.create()
				.texOffs(16, 66).mirror().addBox(-28.0f, -3.0f, -3.0f, 28.0f, 6.0f, 6.0f, new CubeDeformation(0.0f)).mirror(false)
				.texOffs(84, 66).mirror().addBox(-31.0f, -3.0f, -3.0f, 6.0f, 6.0f, 6.0f, new CubeDeformation(0.0f)).mirror(false)
				.texOffs(264, 66).addBox(-10.0f, -7.0f, -1.0f, 2.0f, 5.0f, 2.0f, new CubeDeformation(0.0f))
				.texOffs(264, 66).addBox(-19.0f, -7.0f, -1.0f, 2.0f, 5.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(-11.0f, 0.0f, -9.0f, 0.0f, -0.28f, 0.62f));
		p26.addOrReplaceChild("right_leg2_low", CubeListBuilder.create()
				.texOffs(188, 66).mirror().addBox(-34.0f, -2.0f, -2.0f, 34.0f, 4.0f, 4.0f, new CubeDeformation(0.0f)).mirror(false)
				.texOffs(420, 66).mirror().addBox(-40.0f, -1.0f, -1.0f, 8.0f, 2.0f, 2.0f, new CubeDeformation(0.0f)).mirror(false)
				.texOffs(264, 66).addBox(-14.0f, -5.0f, -1.0f, 2.0f, 5.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(-28.0f, 0.0f, 0.0f, 0.0f, 0.0f, -1.9f));
		PartDefinition p28 = p1.addOrReplaceChild("right_leg3", CubeListBuilder.create()
				.texOffs(16, 66).mirror().addBox(-28.0f, -3.0f, -3.0f, 28.0f, 6.0f, 6.0f, new CubeDeformation(0.0f)).mirror(false)
				.texOffs(84, 66).mirror().addBox(-31.0f, -3.0f, -3.0f, 6.0f, 6.0f, 6.0f, new CubeDeformation(0.0f)).mirror(false)
				.texOffs(264, 66).addBox(-10.0f, -7.0f, -1.0f, 2.0f, 5.0f, 2.0f, new CubeDeformation(0.0f))
				.texOffs(264, 66).addBox(-19.0f, -7.0f, -1.0f, 2.0f, 5.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(-11.0f, 0.0f, -4.0f, 0.0f, 0.22f, 0.62f));
		p28.addOrReplaceChild("right_leg3_low", CubeListBuilder.create()
				.texOffs(188, 66).mirror().addBox(-34.0f, -2.0f, -2.0f, 34.0f, 4.0f, 4.0f, new CubeDeformation(0.0f)).mirror(false)
				.texOffs(420, 66).mirror().addBox(-40.0f, -1.0f, -1.0f, 8.0f, 2.0f, 2.0f, new CubeDeformation(0.0f)).mirror(false)
				.texOffs(264, 66).addBox(-14.0f, -5.0f, -1.0f, 2.0f, 5.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(-28.0f, 0.0f, 0.0f, 0.0f, 0.0f, -1.9f));
		PartDefinition p30 = p1.addOrReplaceChild("right_leg4", CubeListBuilder.create()
				.texOffs(16, 66).mirror().addBox(-28.0f, -3.0f, -3.0f, 28.0f, 6.0f, 6.0f, new CubeDeformation(0.0f)).mirror(false)
				.texOffs(84, 66).mirror().addBox(-31.0f, -3.0f, -3.0f, 6.0f, 6.0f, 6.0f, new CubeDeformation(0.0f)).mirror(false)
				.texOffs(264, 66).addBox(-10.0f, -7.0f, -1.0f, 2.0f, 5.0f, 2.0f, new CubeDeformation(0.0f))
				.texOffs(264, 66).addBox(-19.0f, -7.0f, -1.0f, 2.0f, 5.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(-11.0f, 0.0f, 1.0f, 0.0f, 0.7f, 0.62f));
		p30.addOrReplaceChild("right_leg4_low", CubeListBuilder.create()
				.texOffs(188, 66).mirror().addBox(-34.0f, -2.0f, -2.0f, 34.0f, 4.0f, 4.0f, new CubeDeformation(0.0f)).mirror(false)
				.texOffs(420, 66).mirror().addBox(-40.0f, -1.0f, -1.0f, 8.0f, 2.0f, 2.0f, new CubeDeformation(0.0f)).mirror(false)
				.texOffs(264, 66).addBox(-14.0f, -5.0f, -1.0f, 2.0f, 5.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(-28.0f, 0.0f, 0.0f, 0.0f, 0.0f, -1.9f));
		return LayerDefinition.create(mesh, 512, 256);
	}

	/** Raises a leg (positive = up), independent of the side. */
	private void lift(int index, float amount) {
		legs[index].zRot += index < 4 ? -amount : amount;
	}

	/** Swings a leg forward (positive) or backward. */
	private void swing(int index, float amount) {
		legs[index].yRot += index < 4 ? amount : -amount;
	}

	@Override
	public void setupAnim(AmethystSpiderRenderState state) {
		super.setupAnim(state);
		float pos = state.walkAnimationPos;
		float speed = Math.min(state.walkAnimationSpeed, 1.0f);
		float age = state.ageInTicks;
		float p = state.progress;

		lids.visible = state.lurking;

		if (state.lurking) {
			// Lurking: crouched, legs drawn in, eyes half closed, only the abdomen breathes
			body.y += 7.0f + Mth.sin(age * 0.05f) * 0.4f;
			head.xRot += 0.12f;
			abdomen.xRot += 0.12f + Mth.sin(age * 0.05f) * 0.03f;
			for (int i = 0; i < 8; i++) {
				lift(i, 0.23f);
				lowers[i].zRot += i < 4 ? 0.04f : -0.04f;
			}
			if (Mth.sin(age * 0.13f) > 0.97f) {
				fangLeft.zRot -= 0.2f;   // occasional twitch
				fangRight.zRot += 0.2f;
			}
			return;
		}

		head.yRot = Mth.clamp(state.yRot, -40.0f, 40.0f) * Mth.DEG_TO_RAD;
		head.xRot = state.xRot * Mth.DEG_TO_RAD * 0.5f;

		// gait: two groups of four alternating (L1 R2 L3 R4 / R1 L2 R3 L4)
		for (int i = 0; i < 8; i++) {
			int k = i % 4;
			boolean groupA = (k % 2 == 0) == (i < 4);
			float phase = pos * 0.6f + (groupA ? 0.0f : Mth.PI);
			swing(i, Mth.cos(phase) * 0.32f * speed);
			lift(i, Math.max(0.0f, Mth.sin(phase)) * 0.3f * speed);
		}

		// breathing, fangs twitch, crystal abdomen bobs
		abdomen.xRot += Mth.sin(age * 0.07f) * 0.035f + speed * 0.05f * Mth.sin(pos * 1.2f);
		body.y += Mth.sin(age * 0.07f) * 0.5f;
		float twitch = Mth.sin(age * 0.25f) > 0.8f ? 0.15f : 0.0f;
		fangLeft.zRot -= twitch;
		fangRight.zRot += twitch;

		if (state.enraged) {
			abdomen.yRot = Mth.sin(age * 0.5f) * 0.03f;
		}

		if (state.leaping) {
			// in the air: front legs far forward/up, hind legs stretched
			lift(0, 0.7f);
			lift(4, 0.7f);
			swing(0, 0.3f);
			swing(4, 0.3f);
			lift(1, 0.4f);
			lift(5, 0.4f);
			swing(3, -0.3f);
			swing(7, -0.3f);
			body.xRot = -0.15f;
			fangLeft.zRot -= 0.4f;
			fangRight.zRot += 0.4f;
			return;
		}

		switch (state.anim) {
			case AmethystSpider.ANIM_BITE -> {
				float strike = p < 0.6f ? p / 0.6f : 1.0f - (p - 0.6f) / 0.4f;
				body.xRot = 0.15f * strike;
				body.z -= 6.0f * strike;
				head.xRot += 0.35f * strike;
				fangLeft.zRot -= 0.5f * (1.0f - strike);
				fangRight.zRot += 0.5f * (1.0f - strike);
				fangLeft.xRot -= 0.5f * strike;
				fangRight.xRot -= 0.5f * strike;
			}
			case AmethystSpider.ANIM_LEAP -> {
				// crouch before the leap
				float crouch = Mth.sin(p * Mth.HALF_PI);
				body.y += 7.0f * crouch;
				for (int i = 0; i < 8; i++) {
					lift(i, 0.25f * crouch);
					lowers[i].zRot += (i < 4 ? 0.2f : -0.2f) * crouch;
				}
				body.xRot = 0.1f * crouch;
			}
			case AmethystSpider.ANIM_WEB -> {
				float rise = Mth.sin(Math.min(1.0f, p * 1.6f) * Mth.HALF_PI);
				body.xRot = -0.25f * rise;
				head.xRot -= 0.3f * rise;
				fangLeft.zRot -= 0.6f * rise;
				fangRight.zRot += 0.6f * rise;
				abdomen.xRot += 0.3f * rise;
			}
			case AmethystSpider.ANIM_SPIKES -> {
				// front legs up – and crashing into the ground
				float up = p < 0.5f ? Mth.sin(p * 2.0f * Mth.HALF_PI) : Math.max(-0.3f, 1.0f - (p - 0.5f) * 5.0f);
				lift(0, 1.1f * up);
				lift(4, 1.1f * up);
				lift(1, 0.6f * up);
				lift(5, 0.6f * up);
				body.xRot = -0.3f * Math.max(0.0f, up);
				body.y -= 3.0f * Math.max(0.0f, up);
			}
			case AmethystSpider.ANIM_SHRIEK -> {
				float spread = Mth.sin(Math.min(1.0f, p * 2.5f) * Mth.HALF_PI);
				body.xRot = -0.35f * spread;
				head.xRot -= 0.5f * spread;
				fangLeft.zRot -= 0.8f * spread;
				fangRight.zRot += 0.8f * spread;
				lift(0, 0.9f * spread);
				lift(4, 0.9f * spread);
				swing(0, 0.25f * spread);
				swing(4, 0.25f * spread);
				abdomen.xRot += 0.3f * spread;
				body.zRot = Mth.sin(age * 1.5f) * 0.02f * spread;
			}
			case AmethystSpider.ANIM_BROOD -> {
				float pulse = 1.0f + Mth.sin(p * Mth.PI * 4.0f) * 0.07f;
				abdomen.xScale = pulse;
				abdomen.yScale = pulse;
				abdomen.zScale = pulse;
				abdomen.xRot += 0.35f * Mth.sin(p * Mth.PI);
			}
			default -> {
			}
		}
	}
}
