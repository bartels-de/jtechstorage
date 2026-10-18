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
 * Mondwolf: silberweißer Alpha mit Mähne aus Mondlicht-Strähnen, schwebender Mondsichel-Krone, eisblauen Runen,
 * leuchtenden Ohrspitzen, Pfoten und Schweifspitze und drei kreisenden Mondsplittern.
 * Modell in doppelter Detaildichte (halbe Pixel) – der Renderer skaliert es auf 0.5.
 * Erzeugt von scratchpad/wolves2.py (Teile und Posen), Animation von Hand.
 */
public class MoonWolfModel extends EntityModel<CrystalWolfRenderState> implements HelperHatModel {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(JTechStorage.id("moon_wolf"), "main");

	private final ModelPart wolf;
	private final ModelPart body;
	private final ModelPart neck;
	private final ModelPart head;
	private final ModelPart jaw;
	private final ModelPart lids;
	private final ModelPart rEar;
	private final ModelPart lEar;
	private final ModelPart tail;
	private final ModelPart tail2;
	private final ModelPart tail3;
	private final ModelPart rRuff;
	private final ModelPart lRuff;
	private final ModelPart rfLeg;
	private final ModelPart lfLeg;
	private final ModelPart rhLeg;
	private final ModelPart lhLeg;
	private final ModelPart rfShin;
	private final ModelPart lfShin;
	private final ModelPart rhHock;
	private final ModelPart lhHock;
	private final ModelPart rhFoot;
	private final ModelPart lhFoot;
	private final ModelPart crown;
	private final ModelPart orb0;
	private final ModelPart orb1;
	private final ModelPart orb2;

	public MoonWolfModel(ModelPart root) {
		super(root);
		this.wolf = root.getChild("wolf");
		this.body = root.getChild("wolf").getChild("body");
		this.neck = root.getChild("wolf").getChild("body").getChild("neck");
		this.head = root.getChild("wolf").getChild("body").getChild("neck").getChild("head");
		this.jaw = root.getChild("wolf").getChild("body").getChild("neck").getChild("head").getChild("jaw");
		this.lids = root.getChild("wolf").getChild("body").getChild("neck").getChild("head").getChild("lids");
		this.rEar = root.getChild("wolf").getChild("body").getChild("neck").getChild("head").getChild("r_ear");
		this.lEar = root.getChild("wolf").getChild("body").getChild("neck").getChild("head").getChild("l_ear");
		this.tail = root.getChild("wolf").getChild("body").getChild("tail");
		this.tail2 = root.getChild("wolf").getChild("body").getChild("tail").getChild("tail2");
		this.tail3 = root.getChild("wolf").getChild("body").getChild("tail").getChild("tail2").getChild("tail3");
		this.rRuff = root.getChild("wolf").getChild("body").getChild("r_ruff");
		this.lRuff = root.getChild("wolf").getChild("body").getChild("l_ruff");
		this.rfLeg = root.getChild("wolf").getChild("body").getChild("rf_leg");
		this.lfLeg = root.getChild("wolf").getChild("body").getChild("lf_leg");
		this.rhLeg = root.getChild("wolf").getChild("body").getChild("rh_leg");
		this.lhLeg = root.getChild("wolf").getChild("body").getChild("lh_leg");
		this.rfShin = root.getChild("wolf").getChild("body").getChild("rf_leg").getChild("rf_shin");
		this.lfShin = root.getChild("wolf").getChild("body").getChild("lf_leg").getChild("lf_shin");
		this.rhHock = root.getChild("wolf").getChild("body").getChild("rh_leg").getChild("rh_hock");
		this.lhHock = root.getChild("wolf").getChild("body").getChild("lh_leg").getChild("lh_hock");
		this.rhFoot = root.getChild("wolf").getChild("body").getChild("rh_leg").getChild("rh_hock").getChild("rh_foot");
		this.lhFoot = root.getChild("wolf").getChild("body").getChild("lh_leg").getChild("lh_hock").getChild("lh_foot");
		this.crown = root.getChild("wolf").getChild("body").getChild("neck").getChild("head").getChild("crown");
		this.orb0 = root.getChild("wolf").getChild("orb0");
		this.orb1 = root.getChild("wolf").getChild("orb1");
		this.orb2 = root.getChild("wolf").getChild("orb2");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		PartDefinition p1 = root.addOrReplaceChild("wolf", CubeListBuilder.create(),
				PartPose.offset(0.0f, 24.0f, 0.0f));
		PartDefinition p2 = p1.addOrReplaceChild("body", CubeListBuilder.create()
				.texOffs(0, 0).addBox(-6.0f, -6.0f, -11.0f, 12.0f, 12.0f, 12.0f, new CubeDeformation(0.0f))
				.texOffs(48, 0).addBox(-5.0f, -5.0f, 0.0f, 10.0f, 10.0f, 11.0f, new CubeDeformation(0.0f))
				.texOffs(164, 0).addBox(-7.0f, -6.5f, -13.0f, 14.0f, 11.0f, 5.0f, new CubeDeformation(0.0f))
				.texOffs(184, 24).addBox(-4.0f, -8.0f, -10.0f, 8.0f, 4.0f, 6.0f, new CubeDeformation(0.0f)),
				PartPose.offset(0.0f, -17.5f, 0.0f));
		PartDefinition p3 = p2.addOrReplaceChild("neck", CubeListBuilder.create()
				.texOffs(128, 0).addBox(-4.0f, -8.0f, -4.0f, 8.0f, 9.0f, 8.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, -2.0f, -10.0f, -0.45f, 0.0f, 0.0f));
		PartDefinition p4 = p3.addOrReplaceChild("head", CubeListBuilder.create()
				.texOffs(90, 0).addBox(-5.0f, -6.0f, -7.0f, 10.0f, 9.0f, 9.0f, new CubeDeformation(0.0f))
				.texOffs(112, 38).addBox(-5.5f, -6.6f, -7.6f, 11.0f, 2.0f, 3.0f, new CubeDeformation(0.0f))
				.texOffs(104, 24).addBox(-2.5f, -3.0f, -14.0f, 5.0f, 4.0f, 8.0f, new CubeDeformation(0.0f))
				.texOffs(22, 38).addBox(-2.0f, -4.0f, -13.0f, 4.0f, 2.0f, 6.0f, new CubeDeformation(0.0f))
				.texOffs(166, 38).addBox(-1.0f, -3.4f, -14.6f, 2.0f, 2.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, -7.0f, -1.0f, 0.45f, 0.0f, 0.0f));
		p4.addOrReplaceChild("lids", CubeListBuilder.create()
				.texOffs(146, 38).addBox(-4.5f, -5.0f, -7.4f, 9.0f, 2.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.ZERO);
		p4.addOrReplaceChild("jaw", CubeListBuilder.create()
				.texOffs(226, 24).addBox(-2.0f, 0.0f, -7.0f, 4.0f, 2.0f, 7.0f, new CubeDeformation(0.0f))
				.texOffs(178, 38).addBox(-1.8f, -1.6f, -6.8f, 1.0f, 2.0f, 1.0f, new CubeDeformation(0.0f))
				.texOffs(178, 38).addBox(0.8f, -1.6f, -6.8f, 1.0f, 2.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offset(0.0f, 1.6f, -6.5f));
		PartDefinition p7 = p4.addOrReplaceChild("r_ear", CubeListBuilder.create()
				.texOffs(12, 38).addBox(-1.5f, -7.0f, -1.0f, 3.0f, 7.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(-3.0f, -4.5f, -2.0f, -0.1f, 0.0f, -0.18f));
		p7.addOrReplaceChild("r_ear_tip", CubeListBuilder.create()
				.texOffs(140, 38).addBox(-1.0f, -3.0f, -0.5f, 2.0f, 3.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offset(0.0f, -6.5f, 0.0f));
		PartDefinition p9 = p4.addOrReplaceChild("l_ear", CubeListBuilder.create()
				.texOffs(12, 38).addBox(-1.5f, -7.0f, -1.0f, 3.0f, 7.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(3.0f, -4.5f, -2.0f, -0.1f, 0.0f, 0.18f));
		p9.addOrReplaceChild("l_ear_tip", CubeListBuilder.create()
				.texOffs(140, 38).addBox(-1.0f, -3.0f, -0.5f, 2.0f, 3.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offset(0.0f, -6.5f, 0.0f));
		p4.addOrReplaceChild("r_cheek", CubeListBuilder.create()
				.texOffs(212, 24).addBox(-1.0f, -2.0f, -2.5f, 2.0f, 5.0f, 5.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(-5.2f, -1.0f, -3.0f, 0.0f, 0.35f, 0.3f));
		p4.addOrReplaceChild("l_cheek", CubeListBuilder.create()
				.texOffs(212, 24).addBox(-1.0f, -2.0f, -2.5f, 2.0f, 5.0f, 5.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(5.2f, -1.0f, -3.0f, 0.0f, -0.35f, -0.3f));
		p4.addOrReplaceChild("crown", CubeListBuilder.create()
				.texOffs(58, 24).addBox(-6.0f, -6.0f, 0.0f, 12.0f, 12.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offset(0.0f, -17.0f, -1.0f));
		PartDefinition p14 = p2.addOrReplaceChild("tail", CubeListBuilder.create()
				.texOffs(32, 24).addBox(-2.5f, -2.5f, 0.0f, 5.0f, 5.0f, 8.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, -3.0f, 10.5f, -0.95f, 0.0f, 0.0f));
		PartDefinition p15 = p14.addOrReplaceChild("tail2", CubeListBuilder.create()
				.texOffs(222, 0).addBox(-3.0f, -3.0f, 0.0f, 6.0f, 6.0f, 8.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, 0.0f, 7.0f, 0.3f, 0.0f, 0.0f));
		p15.addOrReplaceChild("tail3", CubeListBuilder.create()
				.texOffs(134, 24).addBox(-2.0f, -2.0f, 0.0f, 4.0f, 4.0f, 7.0f, new CubeDeformation(0.0f))
				.texOffs(92, 38).addBox(-1.5f, -2.0f, 6.0f, 3.0f, 4.0f, 3.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, 0.0f, 7.0f, 0.35f, 0.0f, 0.0f));
		p2.addOrReplaceChild("r_ruff", CubeListBuilder.create()
				.texOffs(202, 0).addBox(-1.5f, -4.5f, -3.5f, 3.0f, 9.0f, 7.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(-6.5f, -1.0f, -8.0f, 0.0f, 0.2f, 0.25f));
		p2.addOrReplaceChild("l_ruff", CubeListBuilder.create()
				.texOffs(202, 0).addBox(-1.5f, -4.5f, -3.5f, 3.0f, 9.0f, 7.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(6.5f, -1.0f, -8.0f, 0.0f, -0.2f, -0.25f));
		PartDefinition p19 = p2.addOrReplaceChild("rf_leg", CubeListBuilder.create()
				.texOffs(156, 24).addBox(-2.0f, 0.0f, -2.0f, 4.0f, 7.0f, 4.0f, new CubeDeformation(0.0f)),
				PartPose.offset(-3.8f, 4.0f, -6.5f));
		p19.addOrReplaceChild("rf_shin", CubeListBuilder.create()
				.texOffs(0, 38).addBox(-1.5f, 0.0f, -1.5f, 3.0f, 6.0f, 3.0f, new CubeDeformation(0.0f))
				.texOffs(62, 38).addBox(-2.0f, 5.0f, -3.5f, 4.0f, 2.0f, 5.0f, new CubeDeformation(0.0f))
				.texOffs(172, 38).addBox(-1.8f, 6.0f, -5.0f, 1.0f, 1.0f, 2.0f, new CubeDeformation(0.0f))
				.texOffs(172, 38).addBox(-0.5f, 6.0f, -5.0f, 1.0f, 1.0f, 2.0f, new CubeDeformation(0.0f))
				.texOffs(172, 38).addBox(0.8f, 6.0f, -5.0f, 1.0f, 1.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offset(0.0f, 6.5f, 0.0f));
		PartDefinition p21 = p2.addOrReplaceChild("lf_leg", CubeListBuilder.create()
				.texOffs(156, 24).addBox(-2.0f, 0.0f, -2.0f, 4.0f, 7.0f, 4.0f, new CubeDeformation(0.0f)),
				PartPose.offset(3.8f, 4.0f, -6.5f));
		p21.addOrReplaceChild("lf_shin", CubeListBuilder.create()
				.texOffs(0, 38).addBox(-1.5f, 0.0f, -1.5f, 3.0f, 6.0f, 3.0f, new CubeDeformation(0.0f))
				.texOffs(62, 38).addBox(-2.0f, 5.0f, -3.5f, 4.0f, 2.0f, 5.0f, new CubeDeformation(0.0f))
				.texOffs(172, 38).addBox(-1.8f, 6.0f, -5.0f, 1.0f, 1.0f, 2.0f, new CubeDeformation(0.0f))
				.texOffs(172, 38).addBox(-0.5f, 6.0f, -5.0f, 1.0f, 1.0f, 2.0f, new CubeDeformation(0.0f))
				.texOffs(172, 38).addBox(0.8f, 6.0f, -5.0f, 1.0f, 1.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offset(0.0f, 6.5f, 0.0f));
		PartDefinition p23 = p2.addOrReplaceChild("rh_leg", CubeListBuilder.create()
				.texOffs(84, 24).addBox(-2.0f, -1.0f, -3.0f, 4.0f, 7.0f, 6.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(-3.2f, 3.8f, 7.0f, 0.35f, 0.0f, 0.0f));
		PartDefinition p24 = p23.addOrReplaceChild("rh_hock", CubeListBuilder.create()
				.texOffs(42, 38).addBox(-1.5f, 0.0f, -1.5f, 3.0f, 5.0f, 3.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, 5.5f, 1.5f, -0.8f, 0.0f, 0.0f));
		p24.addOrReplaceChild("rh_foot", CubeListBuilder.create()
				.texOffs(80, 38).addBox(-1.5f, 0.0f, -1.5f, 3.0f, 4.0f, 3.0f, new CubeDeformation(0.0f))
				.texOffs(62, 38).addBox(-2.0f, 3.0f, -3.5f, 4.0f, 2.0f, 5.0f, new CubeDeformation(0.0f))
				.texOffs(172, 38).addBox(-1.8f, 4.0f, -5.0f, 1.0f, 1.0f, 2.0f, new CubeDeformation(0.0f))
				.texOffs(172, 38).addBox(-0.5f, 4.0f, -5.0f, 1.0f, 1.0f, 2.0f, new CubeDeformation(0.0f))
				.texOffs(172, 38).addBox(0.8f, 4.0f, -5.0f, 1.0f, 1.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, 4.5f, 0.0f, 0.45f, 0.0f, 0.0f));
		PartDefinition p26 = p2.addOrReplaceChild("lh_leg", CubeListBuilder.create()
				.texOffs(84, 24).addBox(-2.0f, -1.0f, -3.0f, 4.0f, 7.0f, 6.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(3.2f, 3.8f, 7.0f, 0.35f, 0.0f, 0.0f));
		PartDefinition p27 = p26.addOrReplaceChild("lh_hock", CubeListBuilder.create()
				.texOffs(42, 38).addBox(-1.5f, 0.0f, -1.5f, 3.0f, 5.0f, 3.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, 5.5f, 1.5f, -0.8f, 0.0f, 0.0f));
		p27.addOrReplaceChild("lh_foot", CubeListBuilder.create()
				.texOffs(80, 38).addBox(-1.5f, 0.0f, -1.5f, 3.0f, 4.0f, 3.0f, new CubeDeformation(0.0f))
				.texOffs(62, 38).addBox(-2.0f, 3.0f, -3.5f, 4.0f, 2.0f, 5.0f, new CubeDeformation(0.0f))
				.texOffs(172, 38).addBox(-1.8f, 4.0f, -5.0f, 1.0f, 1.0f, 2.0f, new CubeDeformation(0.0f))
				.texOffs(172, 38).addBox(-0.5f, 4.0f, -5.0f, 1.0f, 1.0f, 2.0f, new CubeDeformation(0.0f))
				.texOffs(172, 38).addBox(0.8f, 4.0f, -5.0f, 1.0f, 1.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, 4.5f, 0.0f, 0.45f, 0.0f, 0.0f));
		p2.addOrReplaceChild("strand0", CubeListBuilder.create()
				.texOffs(160, 0).addBox(-0.5f, -16.0f, -0.5f, 1.0f, 16.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, -5.5f, -11.0f, -1.0f, 0.0f, 0.0f));
		p2.addOrReplaceChild("strand1", CubeListBuilder.create()
				.texOffs(160, 0).addBox(-0.5f, -16.0f, -0.5f, 1.0f, 16.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(-3.0f, -5.5f, -10.0f, -1.1f, 0.0f, -0.35f));
		p2.addOrReplaceChild("strand2", CubeListBuilder.create()
				.texOffs(160, 0).addBox(-0.5f, -16.0f, -0.5f, 1.0f, 16.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(3.0f, -5.5f, -10.0f, -1.1f, 0.0f, 0.35f));
		p2.addOrReplaceChild("strand3", CubeListBuilder.create()
				.texOffs(160, 0).addBox(-0.5f, -16.0f, -0.5f, 1.0f, 16.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(-1.5f, -5.5f, -7.0f, -1.25f, 0.0f, -0.15f));
		p2.addOrReplaceChild("strand4", CubeListBuilder.create()
				.texOffs(160, 0).addBox(-0.5f, -16.0f, -0.5f, 1.0f, 16.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(1.5f, -5.5f, -7.0f, -1.25f, 0.0f, 0.15f));
		p2.addOrReplaceChild("strand5", CubeListBuilder.create()
				.texOffs(130, 24).addBox(-0.5f, -11.0f, -0.5f, 1.0f, 11.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, -5.5f, -3.0f, -1.35f, 0.0f, 0.0f));
		p2.addOrReplaceChild("strand6", CubeListBuilder.create()
				.texOffs(130, 24).addBox(-0.5f, -11.0f, -0.5f, 1.0f, 11.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(-1.0f, -4.6f, 1.0f, -1.4f, 0.0f, -0.1f));
		p2.addOrReplaceChild("strand7", CubeListBuilder.create()
				.texOffs(130, 24).addBox(-0.5f, -11.0f, -0.5f, 1.0f, 11.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(1.0f, -4.6f, 1.0f, -1.4f, 0.0f, 0.1f));
		p2.addOrReplaceChild("strand8", CubeListBuilder.create()
				.texOffs(130, 24).addBox(-0.5f, -11.0f, -0.5f, 1.0f, 11.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, -4.6f, 5.0f, -1.45f, 0.0f, 0.0f));
		p1.addOrReplaceChild("orb0", CubeListBuilder.create()
				.texOffs(104, 38).addBox(-1.0f, -2.0f, -1.0f, 2.0f, 4.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, -30.0f, 0.0f, 0.3f, 0.0f, 0.4f));
		p1.addOrReplaceChild("orb1", CubeListBuilder.create()
				.texOffs(104, 38).addBox(-1.0f, -2.0f, -1.0f, 2.0f, 4.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, -30.0f, 0.0f, 0.3f, 0.0f, 0.4f));
		p1.addOrReplaceChild("orb2", CubeListBuilder.create()
				.texOffs(104, 38).addBox(-1.0f, -2.0f, -1.0f, 2.0f, 4.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, -30.0f, 0.0f, 0.3f, 0.0f, 0.4f));
		return LayerDefinition.create(mesh, 256, 128);
	}

	@Override
	public void setupAnim(CrystalWolfRenderState state) {
		super.setupAnim(state);
		float age = state.ageInTicks;
		float pos = state.walkAnimationPos;
		float speed = Math.min(state.walkAnimationSpeed, 1.0f);
		lids.visible = state.resting;

		crown.yRot = age * 0.03f;
		crown.y += Mth.sin(age * 0.06f) * 0.8f;
		ModelPart[] orbs = {orb0, orb1, orb2};

		for (int k = 0; k < 3; k++) {
			float angle = age * 0.05f + k * 2.094f;
			orbs[k].x = Mth.cos(angle) * 16.0f;
			orbs[k].z = Mth.sin(angle) * 16.0f;
			orbs[k].y = -34.0f + Mth.sin(age * 0.09f + k * 1.7f) * 3.0f;
			orbs[k].yRot = -angle;
			orbs[k].visible = !state.resting;
		}

		if (state.resting) {
			poseSleep(1.0f);
			float breath = Mth.sin(age * 0.05f);
			body.y += breath * 0.4f;
			tail3.yRot += Mth.sin(age * 0.03f) * 0.08f;
			rEar.zRot -= Math.max(0.0f, Mth.sin(age * 0.3f) - 0.9f) * 2.0f;   // Ohr zuckt im Schlaf
			return;
		}

		float yaw = Mth.clamp(state.yRot, -50.0f, 50.0f) * Mth.DEG_TO_RAD;
		neck.yRot += yaw * 0.4f;
		head.yRot += yaw * 0.5f;
		head.xRot += state.xRot * Mth.DEG_TO_RAD * 0.5f;

		// Trab: diagonale Beinpaare (rechts vorn + links hinten), Knie und Sprunggelenke beugen sich beim Vorschwingen
		float phase = pos * 0.8f;
		float swing = Mth.cos(phase) * 0.6f * speed;
		float lift = Mth.sin(phase);
		rfLeg.xRot += swing;
		lhLeg.xRot += swing * 0.8f;
		lfLeg.xRot -= swing;
		rhLeg.xRot -= swing * 0.8f;
		rfShin.xRot += Math.max(0.0f, lift) * 0.9f * speed;
		lfShin.xRot += Math.max(0.0f, -lift) * 0.9f * speed;
		lhHock.xRot -= Math.max(0.0f, lift) * 0.55f * speed;
		rhHock.xRot -= Math.max(0.0f, -lift) * 0.55f * speed;
		lhFoot.xRot += Math.max(0.0f, lift) * 0.4f * speed;
		rhFoot.xRot += Math.max(0.0f, -lift) * 0.4f * speed;
		body.y += -Mth.abs(Mth.cos(phase)) * 0.7f * speed;
		body.xRot += Mth.sin(phase * 2.0f) * 0.03f * speed;
		neck.xRot -= Mth.sin(phase * 2.0f) * 0.04f * speed;   // Kopf bleibt ruhig
		neck.xRot += speed * 0.15f;                           // im Lauf gestreckter Hals

		// Schweif: wedeln im Stand, gestreckt im Lauf
		tail.yRot += Mth.sin(age * 0.18f) * 0.22f * (1.0f - speed);
		tail2.yRot += Mth.sin(age * 0.18f - 0.6f) * 0.15f * (1.0f - speed);
		tail.xRot += speed * 0.45f;
		rEar.xRot += Math.max(0.0f, Mth.sin(age * 0.23f) - 0.95f) * 4.0f;
		lEar.xRot += Math.max(0.0f, Mth.sin(age * 0.19f + 1.0f) - 0.95f) * 4.0f;
		float breathe = Mth.sin(age * 0.07f);
		body.y += breathe * 0.25f;

		if (state.exhausted) {
			neck.xRot += 0.5f;
			tail.xRot -= 0.35f;
			rEar.xRot += 0.6f;
			lEar.xRot += 0.6f;
		}

		if (state.angry && state.howl <= 0.0f) {
			poseGrowl(0.75f);
			jaw.xRot += Math.max(0.0f, Mth.sin(age * 0.6f)) * 0.08f;   // Knurren: Lefzen zittern
		}

		if (state.bite > 0.0f) {
			float b = Mth.sin(state.bite * Mth.PI);
			jaw.xRot += 0.6f * b;
			neck.xRot += 0.35f * b;
			body.z -= 1.5f * b;
		}

		float h = state.howl > 0.0f ? Mth.sin(Math.min(1.0f, state.howl * 1.3f) * Mth.HALF_PI) * Math.min(1.0f, (1.0f - state.howl) * 6.0f) : 0.0f;

		if (state.sitting) {
			poseSit(1.0f - h);   // Heulen enthält die Sitzhaltung schon
		}

		if (h > 0.0f) {
			poseHowl(h);
		}
	}

	private void poseSit(float k) {
		body.xRot += k * -0.450f;
		body.y += k * 3.200f;
		neck.xRot += k * 0.200f;
		head.xRot += k * 0.200f;
		rhLeg.xRot += k * -1.900f;
		lhLeg.xRot += k * -1.900f;
		rhHock.xRot += k * 2.200f;
		lhHock.xRot += k * 2.200f;
		rhFoot.xRot += k * -0.400f;
		lhFoot.xRot += k * -0.400f;
		rfLeg.xRot += k * 0.450f;
		lfLeg.xRot += k * 0.450f;
		tail.xRot += k * 0.700f;
	}

	private void poseHowl(float k) {
		body.xRot += k * -0.450f;
		body.y += k * 3.200f;
		neck.xRot += k * -0.350f;
		head.xRot += k * -0.550f;
		rhLeg.xRot += k * -1.900f;
		lhLeg.xRot += k * -1.900f;
		rhHock.xRot += k * 2.200f;
		lhHock.xRot += k * 2.200f;
		rhFoot.xRot += k * -0.400f;
		lhFoot.xRot += k * -0.400f;
		rfLeg.xRot += k * 0.450f;
		lfLeg.xRot += k * 0.450f;
		tail.xRot += k * 0.700f;
		jaw.xRot += k * 0.500f;
		rEar.xRot += k * 0.300f;
		lEar.xRot += k * 0.300f;
	}

	private void poseGrowl(float k) {
		body.xRot += k * 0.150f;
		body.y += k * -0.600f;
		neck.xRot += k * 0.550f;
		head.xRot += k * -0.350f;
		jaw.xRot += k * 0.300f;
		rEar.xRot += k * 0.500f;
		lEar.xRot += k * 0.500f;
		rfLeg.xRot += k * -0.500f;
		lfLeg.xRot += k * -0.350f;
		rfShin.xRot += k * 0.300f;
		lfShin.xRot += k * 0.200f;
		rhLeg.xRot += k * 0.100f;
		lhLeg.xRot += k * 0.100f;
		tail.xRot += k * 0.200f;
		rRuff.zRot += k * 0.200f;
		lRuff.zRot += k * -0.200f;
	}

	private void poseSleep(float k) {
		body.y += k * 11.050f;
		neck.xRot += k * 0.950f;
		neck.yRot += k * 0.350f;
		head.xRot += k * -0.300f;
		rEar.xRot += k * 0.700f;
		lEar.xRot += k * 0.700f;
		rfLeg.xRot += k * -1.500f;
		lfLeg.xRot += k * -1.500f;
		rfShin.xRot += k * 0.000f;
		lfShin.xRot += k * 0.000f;
		rhLeg.xRot += k * -2.400f;
		lhLeg.xRot += k * -2.400f;
		rhHock.xRot += k * 1.800f;
		lhHock.xRot += k * 1.800f;
		rhFoot.xRot += k * -1.600f;
		lhFoot.xRot += k * -1.600f;
		tail.xRot += k * 1.000f;
		tail.yRot += k * -1.100f;
		tail2.yRot += k * -0.700f;
		tail3.yRot += k * -0.600f;
	}

	@Override
	public void translateToHat(PoseStack pose) {
		root().translateAndRotate(pose);
		wolf.translateAndRotate(pose);
		body.translateAndRotate(pose);
		neck.translateAndRotate(pose);
		head.translateAndRotate(pose);
		pose.translate(0.0f, 0.0f, -2.5f / 16.0f);
	}

	@Override
	public float hatScale() {
		return 1.25f;
	}
}
