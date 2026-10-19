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
 * Cheldrogan v3 – Peters Drachenform: ein schwebender Kristall-Schlangendrache. Neun Körpersegmente mit Amethyst-Schuppen,
 * goldenen Bauchplatten und Kristalldornen, leuchtende Mähne aus Lichtsträhnen, Kopf mit Kristallgeweih, Barteln und
 * Kinnbart, vier kurze Beine (im Schweben angelegt), Kristall-Flamme als Schwanzspitze und die Resonanz-Perle vor ihm.
 * Grundpose = Gleiten; Schlängeln, Kristall-Odem, Splitter-Regen, Schwanzhieb (Wirbel) und Ruhe (eingerollt) von Hand.
 * Modell in doppelter Detaildichte (halbe Pixel) – der Renderer skaliert es auf 0.5.
 * Erzeugt von scratchpad/cheldrogan3.py (Teile und Posen), Animation von Hand.
 */
public class CheldroganModel extends EntityModel<CheldroganRenderState> implements HelperHatModel {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(JTechStorage.id("cheldrogan"), "main");

	private final ModelPart dragon;
	private final ModelPart seg0;
	private final ModelPart seg1;
	private final ModelPart seg2;
	private final ModelPart seg3;
	private final ModelPart seg4;
	private final ModelPart seg5;
	private final ModelPart seg6;
	private final ModelPart seg7;
	private final ModelPart seg8;
	private final ModelPart neck0;
	private final ModelPart neck1;
	private final ModelPart head;
	private final ModelPart jaw;
	private final ModelPart lids;
	private final ModelPart beard;
	private final ModelPart rWhisker;
	private final ModelPart lWhisker;
	private final ModelPart rFin;
	private final ModelPart lFin;
	private final ModelPart flame;
	private final ModelPart pearl;
	private final ModelPart rfLeg;
	private final ModelPart lfLeg;
	private final ModelPart rhLeg;
	private final ModelPart lhLeg;
	private final ModelPart rfShin;
	private final ModelPart lfShin;
	private final ModelPart rhShin;
	private final ModelPart lhShin;

	public CheldroganModel(ModelPart root) {
		super(root);
		this.dragon = root.getChild("dragon");
		this.seg0 = root.getChild("dragon").getChild("seg0");
		this.seg1 = root.getChild("dragon").getChild("seg0").getChild("seg1");
		this.seg2 = root.getChild("dragon").getChild("seg0").getChild("seg1").getChild("seg2");
		this.seg3 = root.getChild("dragon").getChild("seg0").getChild("seg1").getChild("seg2").getChild("seg3");
		this.seg4 = root.getChild("dragon").getChild("seg0").getChild("seg1").getChild("seg2").getChild("seg3").getChild("seg4");
		this.seg5 = root.getChild("dragon").getChild("seg0").getChild("seg1").getChild("seg2").getChild("seg3").getChild("seg4").getChild("seg5");
		this.seg6 = root.getChild("dragon").getChild("seg0").getChild("seg1").getChild("seg2").getChild("seg3").getChild("seg4").getChild("seg5").getChild("seg6");
		this.seg7 = root.getChild("dragon").getChild("seg0").getChild("seg1").getChild("seg2").getChild("seg3").getChild("seg4").getChild("seg5").getChild("seg6").getChild("seg7");
		this.seg8 = root.getChild("dragon").getChild("seg0").getChild("seg1").getChild("seg2").getChild("seg3").getChild("seg4").getChild("seg5").getChild("seg6").getChild("seg7").getChild("seg8");
		this.neck0 = root.getChild("dragon").getChild("seg0").getChild("neck0");
		this.neck1 = root.getChild("dragon").getChild("seg0").getChild("neck0").getChild("neck1");
		this.head = root.getChild("dragon").getChild("seg0").getChild("neck0").getChild("neck1").getChild("head");
		this.jaw = root.getChild("dragon").getChild("seg0").getChild("neck0").getChild("neck1").getChild("head").getChild("jaw");
		this.lids = root.getChild("dragon").getChild("seg0").getChild("neck0").getChild("neck1").getChild("head").getChild("lids");
		this.beard = root.getChild("dragon").getChild("seg0").getChild("neck0").getChild("neck1").getChild("head").getChild("jaw").getChild("beard");
		this.rWhisker = root.getChild("dragon").getChild("seg0").getChild("neck0").getChild("neck1").getChild("head").getChild("r_whisker");
		this.lWhisker = root.getChild("dragon").getChild("seg0").getChild("neck0").getChild("neck1").getChild("head").getChild("l_whisker");
		this.rFin = root.getChild("dragon").getChild("seg0").getChild("neck0").getChild("neck1").getChild("head").getChild("r_fin");
		this.lFin = root.getChild("dragon").getChild("seg0").getChild("neck0").getChild("neck1").getChild("head").getChild("l_fin");
		this.flame = root.getChild("dragon").getChild("seg0").getChild("seg1").getChild("seg2").getChild("seg3").getChild("seg4").getChild("seg5").getChild("seg6").getChild("seg7").getChild("seg8").getChild("flame");
		this.pearl = root.getChild("dragon").getChild("pearl");
		this.rfLeg = root.getChild("dragon").getChild("seg0").getChild("seg1").getChild("rf_leg");
		this.lfLeg = root.getChild("dragon").getChild("seg0").getChild("seg1").getChild("lf_leg");
		this.rhLeg = root.getChild("dragon").getChild("seg0").getChild("seg1").getChild("seg2").getChild("seg3").getChild("seg4").getChild("seg5").getChild("rh_leg");
		this.lhLeg = root.getChild("dragon").getChild("seg0").getChild("seg1").getChild("seg2").getChild("seg3").getChild("seg4").getChild("seg5").getChild("lh_leg");
		this.rfShin = root.getChild("dragon").getChild("seg0").getChild("seg1").getChild("rf_leg").getChild("rf_shin");
		this.lfShin = root.getChild("dragon").getChild("seg0").getChild("seg1").getChild("lf_leg").getChild("lf_shin");
		this.rhShin = root.getChild("dragon").getChild("seg0").getChild("seg1").getChild("seg2").getChild("seg3").getChild("seg4").getChild("seg5").getChild("rh_leg").getChild("rh_shin");
		this.lhShin = root.getChild("dragon").getChild("seg0").getChild("seg1").getChild("seg2").getChild("seg3").getChild("seg4").getChild("seg5").getChild("lh_leg").getChild("lh_shin");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		PartDefinition p1 = root.addOrReplaceChild("dragon", CubeListBuilder.create(),
				PartPose.offset(0.0f, 24.0f, 0.0f));
		PartDefinition p2 = p1.addOrReplaceChild("seg0", CubeListBuilder.create()
				.texOffs(0, 0).addBox(-6.0f, -6.0f, -7.0f, 12.0f, 12.0f, 14.0f, new CubeDeformation(0.0f)),
				PartPose.offset(0.0f, -40.0f, 0.0f));
		PartDefinition p3 = p2.addOrReplaceChild("neck0", CubeListBuilder.create()
				.texOffs(394, 0).addBox(-4.5f, -4.5f, -10.0f, 9.0f, 9.0f, 10.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, -1.0f, -6.5f, -0.45f, 0.0f, 0.0f));
		PartDefinition p4 = p3.addOrReplaceChild("neck1", CubeListBuilder.create()
				.texOffs(0, 26).addBox(-4.0f, -4.0f, -10.0f, 8.0f, 8.0f, 10.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, 0.0f, -9.5f, -0.35f, 0.0f, 0.0f));
		PartDefinition p5 = p4.addOrReplaceChild("head", CubeListBuilder.create()
				.texOffs(154, 0).addBox(-7.0f, -12.0f, -12.0f, 14.0f, 12.0f, 12.0f, new CubeDeformation(0.0f))
				.texOffs(252, 0).addBox(-5.0f, -8.0f, -25.0f, 10.0f, 8.0f, 14.0f, new CubeDeformation(0.0f))
				.texOffs(186, 26).addBox(-3.0f, -9.6f, -24.0f, 6.0f, 2.0f, 12.0f, new CubeDeformation(0.0f))
				.texOffs(440, 26).addBox(-2.0f, -8.4f, -25.6f, 4.0f, 2.0f, 1.0f, new CubeDeformation(0.0f))
				.texOffs(368, 26).addBox(-7.5f, -12.6f, -12.6f, 15.0f, 2.0f, 3.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, 0.0f, -9.5f, 0.8f, 0.0f, 0.0f));
		p5.addOrReplaceChild("lids", CubeListBuilder.create()
				.texOffs(414, 26).addBox(-6.0f, -9.6f, -12.4f, 12.0f, 2.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.ZERO);
		PartDefinition p7 = p5.addOrReplaceChild("jaw", CubeListBuilder.create()
				.texOffs(54, 26).addBox(-4.5f, 0.0f, -13.0f, 9.0f, 3.0f, 13.0f, new CubeDeformation(0.0f))
				.texOffs(410, 26).addBox(-4.0f, -2.6f, -12.6f, 1.0f, 3.0f, 1.0f, new CubeDeformation(0.0f))
				.texOffs(410, 26).addBox(3.0f, -2.6f, -12.6f, 1.0f, 3.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offset(0.0f, -1.0f, -12.0f));
		p7.addOrReplaceChild("beard", CubeListBuilder.create()
				.texOffs(326, 26).addBox(-0.5f, 0.0f, -0.5f, 1.0f, 8.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, 3.0f, -9.0f, 0.3f, 0.0f, 0.0f));
		PartDefinition p9 = p5.addOrReplaceChild("r_antler", CubeListBuilder.create()
				.texOffs(342, 0).addBox(-1.5f, -18.0f, -1.5f, 3.0f, 18.0f, 3.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(-4.5f, -11.5f, -4.0f, 0.75f, 0.0f, -0.3f));
		p9.addOrReplaceChild("r_tine0", CubeListBuilder.create()
				.texOffs(306, 26).addBox(-1.0f, -8.0f, -1.0f, 2.0f, 8.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, -6.0f, 0.0f, -0.2f, 0.0f, -0.8f));
		p9.addOrReplaceChild("r_tine1", CubeListBuilder.create()
				.texOffs(352, 26).addBox(-1.0f, -5.0f, -1.0f, 2.0f, 5.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, -12.0f, 0.0f, 0.5f, 0.0f, 0.55f));
		p9.addOrReplaceChild("r_tine2", CubeListBuilder.create()
				.texOffs(352, 26).addBox(-1.0f, -5.0f, -1.0f, 2.0f, 5.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, -16.0f, 0.0f, -0.6f, 0.0f, -0.45f));
		PartDefinition p13 = p5.addOrReplaceChild("l_antler", CubeListBuilder.create()
				.texOffs(342, 0).addBox(-1.5f, -18.0f, -1.5f, 3.0f, 18.0f, 3.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(4.5f, -11.5f, -4.0f, 0.75f, 0.0f, 0.3f));
		p13.addOrReplaceChild("l_tine0", CubeListBuilder.create()
				.texOffs(306, 26).addBox(-1.0f, -8.0f, -1.0f, 2.0f, 8.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, -6.0f, 0.0f, -0.2f, 0.0f, 0.8f));
		p13.addOrReplaceChild("l_tine1", CubeListBuilder.create()
				.texOffs(352, 26).addBox(-1.0f, -5.0f, -1.0f, 2.0f, 5.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, -12.0f, 0.0f, 0.5f, 0.0f, -0.55f));
		p13.addOrReplaceChild("l_tine2", CubeListBuilder.create()
				.texOffs(352, 26).addBox(-1.0f, -5.0f, -1.0f, 2.0f, 5.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, -16.0f, 0.0f, -0.6f, 0.0f, 0.45f));
		p5.addOrReplaceChild("r_whisker", CubeListBuilder.create()
				.texOffs(432, 0).addBox(-0.5f, -0.5f, 0.0f, 1.0f, 1.0f, 18.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(-5.0f, -4.0f, -22.0f, 0.35f, -2.4f, 0.0f));
		p5.addOrReplaceChild("l_whisker", CubeListBuilder.create()
				.texOffs(432, 0).addBox(-0.5f, -0.5f, 0.0f, 1.0f, 1.0f, 18.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(5.0f, -4.0f, -22.0f, 0.35f, 2.4f, 0.0f));
		p5.addOrReplaceChild("r_fin", CubeListBuilder.create()
				.texOffs(36, 26).addBox(0.0f, -4.0f, 0.0f, 0.0f, 8.0f, 9.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(-7.0f, -6.0f, -3.0f, 0.0f, -0.5f, 0.0f));
		p5.addOrReplaceChild("l_fin", CubeListBuilder.create()
				.texOffs(36, 26).addBox(0.0f, -4.0f, 0.0f, 0.0f, 8.0f, 9.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(7.0f, -6.0f, -3.0f, 0.0f, 0.5f, 0.0f));
		p3.addOrReplaceChild("nstrand0", CubeListBuilder.create()
				.texOffs(182, 26).addBox(-0.5f, -14.0f, -0.5f, 1.0f, 14.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(-2.0f, -4.5f, -8.0f, -1.1f, 0.0f, -0.3f));
		p3.addOrReplaceChild("nstrand1", CubeListBuilder.create()
				.texOffs(182, 26).addBox(-0.5f, -14.0f, -0.5f, 1.0f, 14.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(2.0f, -4.5f, -8.0f, -1.1f, 0.0f, 0.3f));
		p3.addOrReplaceChild("nstrand2", CubeListBuilder.create()
				.texOffs(182, 26).addBox(-0.5f, -14.0f, -0.5f, 1.0f, 14.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, -4.5f, -4.0f, -1.1f, 0.0f, 0.0f));
		PartDefinition p24 = p2.addOrReplaceChild("seg1", CubeListBuilder.create()
				.texOffs(52, 0).addBox(-6.0f, -6.0f, 0.0f, 12.0f, 12.0f, 14.0f, new CubeDeformation(0.0f)),
				PartPose.offset(0.0f, 0.0f, 6.5f));
		PartDefinition p25 = p24.addOrReplaceChild("seg2", CubeListBuilder.create()
				.texOffs(104, 0).addBox(-5.5f, -5.5f, 0.0f, 11.0f, 11.0f, 14.0f, new CubeDeformation(0.0f)),
				PartPose.offset(0.0f, 0.0f, 13.5f));
		PartDefinition p26 = p25.addOrReplaceChild("seg3", CubeListBuilder.create()
				.texOffs(206, 0).addBox(-5.0f, -5.0f, 0.0f, 10.0f, 10.0f, 13.0f, new CubeDeformation(0.0f)),
				PartPose.offset(0.0f, 0.0f, 13.5f));
		PartDefinition p27 = p26.addOrReplaceChild("seg4", CubeListBuilder.create()
				.texOffs(300, 0).addBox(-4.5f, -4.5f, 0.0f, 9.0f, 9.0f, 12.0f, new CubeDeformation(0.0f)),
				PartPose.offset(0.0f, 0.0f, 12.5f));
		PartDefinition p28 = p27.addOrReplaceChild("seg5", CubeListBuilder.create()
				.texOffs(354, 0).addBox(-4.0f, -4.0f, 0.0f, 8.0f, 8.0f, 12.0f, new CubeDeformation(0.0f)),
				PartPose.offset(0.0f, 0.0f, 11.5f));
		PartDefinition p29 = p28.addOrReplaceChild("seg6", CubeListBuilder.create()
				.texOffs(470, 0).addBox(-3.5f, -3.5f, 0.0f, 7.0f, 7.0f, 11.0f, new CubeDeformation(0.0f)),
				PartPose.offset(0.0f, 0.0f, 11.5f));
		PartDefinition p30 = p29.addOrReplaceChild("seg7", CubeListBuilder.create()
				.texOffs(98, 26).addBox(-3.0f, -3.0f, 0.0f, 6.0f, 6.0f, 10.0f, new CubeDeformation(0.0f)),
				PartPose.offset(0.0f, 0.0f, 10.5f));
		PartDefinition p31 = p30.addOrReplaceChild("seg8", CubeListBuilder.create()
				.texOffs(222, 26).addBox(-2.5f, -2.5f, 0.0f, 5.0f, 5.0f, 9.0f, new CubeDeformation(0.0f)),
				PartPose.offset(0.0f, 0.0f, 9.5f));
		p31.addOrReplaceChild("flame", CubeListBuilder.create()
				.texOffs(162, 26).addBox(-2.5f, -5.0f, 0.0f, 5.0f, 10.0f, 5.0f, new CubeDeformation(0.0f))
				.texOffs(278, 26).addBox(-1.5f, -10.0f, 2.0f, 3.0f, 8.0f, 3.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, 0.0f, 8.0f, -0.3f, 0.0f, 0.0f));
		p31.addOrReplaceChild("spine8", CubeListBuilder.create()
				.texOffs(360, 26).addBox(-1.0f, -5.0f, -1.0f, 2.0f, 5.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, -2.0f, 4.5f, -0.45f, 0.0f, 0.0f));
		p30.addOrReplaceChild("spine7", CubeListBuilder.create()
				.texOffs(360, 26).addBox(-1.0f, -5.0f, -1.0f, 2.0f, 5.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, -2.5f, 5.0f, -0.45f, 0.0f, 0.0f));
		p29.addOrReplaceChild("spine6", CubeListBuilder.create()
				.texOffs(360, 26).addBox(-1.0f, -5.0f, -1.0f, 2.0f, 5.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, -3.0f, 5.5f, -0.45f, 0.0f, 0.0f));
		p28.addOrReplaceChild("spine5", CubeListBuilder.create()
				.texOffs(360, 26).addBox(-1.0f, -5.0f, -1.0f, 2.0f, 5.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, -3.5f, 6.0f, -0.45f, 0.0f, 0.0f));
		PartDefinition p37 = p28.addOrReplaceChild("rh_leg", CubeListBuilder.create()
				.texOffs(262, 26).addBox(-2.0f, 0.0f, -2.0f, 4.0f, 7.0f, 4.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(-3.0f, 4.0f, 4.0f, 0.3f, 0.0f, 0.0f));
		p37.addOrReplaceChild("rh_shin", CubeListBuilder.create()
				.texOffs(314, 26).addBox(-1.5f, 0.0f, -1.5f, 3.0f, 6.0f, 3.0f, new CubeDeformation(0.0f))
				.texOffs(330, 26).addBox(-2.5f, 5.0f, -4.0f, 5.0f, 2.0f, 6.0f, new CubeDeformation(0.0f))
				.texOffs(404, 26).addBox(-2.2f, 6.0f, -5.0f, 1.0f, 2.0f, 2.0f, new CubeDeformation(0.0f))
				.texOffs(404, 26).addBox(-0.5f, 6.0f, -5.0f, 1.0f, 2.0f, 2.0f, new CubeDeformation(0.0f))
				.texOffs(404, 26).addBox(1.2f, 6.0f, -5.0f, 1.0f, 2.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, 6.5f, 0.0f, -0.6f, 0.0f, 0.0f));
		PartDefinition p39 = p28.addOrReplaceChild("lh_leg", CubeListBuilder.create()
				.texOffs(262, 26).addBox(-2.0f, 0.0f, -2.0f, 4.0f, 7.0f, 4.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(3.0f, 4.0f, 4.0f, 0.3f, 0.0f, 0.0f));
		p39.addOrReplaceChild("lh_shin", CubeListBuilder.create()
				.texOffs(314, 26).addBox(-1.5f, 0.0f, -1.5f, 3.0f, 6.0f, 3.0f, new CubeDeformation(0.0f))
				.texOffs(330, 26).addBox(-2.5f, 5.0f, -4.0f, 5.0f, 2.0f, 6.0f, new CubeDeformation(0.0f))
				.texOffs(404, 26).addBox(-2.2f, 6.0f, -5.0f, 1.0f, 2.0f, 2.0f, new CubeDeformation(0.0f))
				.texOffs(404, 26).addBox(-0.5f, 6.0f, -5.0f, 1.0f, 2.0f, 2.0f, new CubeDeformation(0.0f))
				.texOffs(404, 26).addBox(1.2f, 6.0f, -5.0f, 1.0f, 2.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, 6.5f, 0.0f, -0.6f, 0.0f, 0.0f));
		p27.addOrReplaceChild("spine4", CubeListBuilder.create()
				.texOffs(294, 26).addBox(-1.5f, -7.0f, -1.5f, 3.0f, 7.0f, 3.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, -4.0f, 6.0f, -0.45f, 0.0f, 0.0f));
		p27.addOrReplaceChild("strand4_0", CubeListBuilder.create()
				.texOffs(290, 26).addBox(-0.5f, -10.0f, -0.5f, 1.0f, 10.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(-2.25f, -4.5f, 2.0f, -1.2f, 0.0f, -0.35f));
		p27.addOrReplaceChild("strand4_1", CubeListBuilder.create()
				.texOffs(290, 26).addBox(-0.5f, -10.0f, -0.5f, 1.0f, 10.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(2.25f, -4.5f, 2.0f, -1.2f, 0.0f, 0.35f));
		p26.addOrReplaceChild("spine3", CubeListBuilder.create()
				.texOffs(294, 26).addBox(-1.5f, -7.0f, -1.5f, 3.0f, 7.0f, 3.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, -4.5f, 6.5f, -0.45f, 0.0f, 0.0f));
		p26.addOrReplaceChild("strand3_0", CubeListBuilder.create()
				.texOffs(290, 26).addBox(-0.5f, -10.0f, -0.5f, 1.0f, 10.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(-2.5f, -5.0f, 2.0f, -1.2f, 0.0f, -0.35f));
		p26.addOrReplaceChild("strand3_1", CubeListBuilder.create()
				.texOffs(290, 26).addBox(-0.5f, -10.0f, -0.5f, 1.0f, 10.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(2.5f, -5.0f, 2.0f, -1.2f, 0.0f, 0.35f));
		p25.addOrReplaceChild("spine2", CubeListBuilder.create()
				.texOffs(294, 26).addBox(-1.5f, -7.0f, -1.5f, 3.0f, 7.0f, 3.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, -5.0f, 7.0f, -0.45f, 0.0f, 0.0f));
		p25.addOrReplaceChild("strand2_0", CubeListBuilder.create()
				.texOffs(182, 26).addBox(-0.5f, -14.0f, -0.5f, 1.0f, 14.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(-2.75f, -5.5f, 2.0f, -1.2f, 0.0f, -0.35f));
		p25.addOrReplaceChild("strand2_1", CubeListBuilder.create()
				.texOffs(182, 26).addBox(-0.5f, -14.0f, -0.5f, 1.0f, 14.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(2.75f, -5.5f, 2.0f, -1.2f, 0.0f, 0.35f));
		p24.addOrReplaceChild("spine1", CubeListBuilder.create()
				.texOffs(250, 26).addBox(-1.5f, -9.0f, -1.5f, 3.0f, 9.0f, 3.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, -5.5f, 7.0f, -0.45f, 0.0f, 0.0f));
		PartDefinition p51 = p24.addOrReplaceChild("rf_leg", CubeListBuilder.create()
				.texOffs(262, 26).addBox(-2.0f, 0.0f, -2.0f, 4.0f, 7.0f, 4.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(-5.0f, 4.0f, 4.0f, 0.3f, 0.0f, 0.0f));
		p51.addOrReplaceChild("rf_shin", CubeListBuilder.create()
				.texOffs(314, 26).addBox(-1.5f, 0.0f, -1.5f, 3.0f, 6.0f, 3.0f, new CubeDeformation(0.0f))
				.texOffs(330, 26).addBox(-2.5f, 5.0f, -4.0f, 5.0f, 2.0f, 6.0f, new CubeDeformation(0.0f))
				.texOffs(404, 26).addBox(-2.2f, 6.0f, -5.0f, 1.0f, 2.0f, 2.0f, new CubeDeformation(0.0f))
				.texOffs(404, 26).addBox(-0.5f, 6.0f, -5.0f, 1.0f, 2.0f, 2.0f, new CubeDeformation(0.0f))
				.texOffs(404, 26).addBox(1.2f, 6.0f, -5.0f, 1.0f, 2.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, 6.5f, 0.0f, -0.6f, 0.0f, 0.0f));
		PartDefinition p53 = p24.addOrReplaceChild("lf_leg", CubeListBuilder.create()
				.texOffs(262, 26).addBox(-2.0f, 0.0f, -2.0f, 4.0f, 7.0f, 4.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(5.0f, 4.0f, 4.0f, 0.3f, 0.0f, 0.0f));
		p53.addOrReplaceChild("lf_shin", CubeListBuilder.create()
				.texOffs(314, 26).addBox(-1.5f, 0.0f, -1.5f, 3.0f, 6.0f, 3.0f, new CubeDeformation(0.0f))
				.texOffs(330, 26).addBox(-2.5f, 5.0f, -4.0f, 5.0f, 2.0f, 6.0f, new CubeDeformation(0.0f))
				.texOffs(404, 26).addBox(-2.2f, 6.0f, -5.0f, 1.0f, 2.0f, 2.0f, new CubeDeformation(0.0f))
				.texOffs(404, 26).addBox(-0.5f, 6.0f, -5.0f, 1.0f, 2.0f, 2.0f, new CubeDeformation(0.0f))
				.texOffs(404, 26).addBox(1.2f, 6.0f, -5.0f, 1.0f, 2.0f, 2.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, 6.5f, 0.0f, -0.6f, 0.0f, 0.0f));
		p24.addOrReplaceChild("strand1_0", CubeListBuilder.create()
				.texOffs(182, 26).addBox(-0.5f, -14.0f, -0.5f, 1.0f, 14.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(-3.0f, -6.0f, 2.0f, -1.2f, 0.0f, -0.35f));
		p24.addOrReplaceChild("strand1_1", CubeListBuilder.create()
				.texOffs(182, 26).addBox(-0.5f, -14.0f, -0.5f, 1.0f, 14.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(3.0f, -6.0f, 2.0f, -1.2f, 0.0f, 0.35f));
		p24.addOrReplaceChild("strand1_2", CubeListBuilder.create()
				.texOffs(182, 26).addBox(-0.5f, -14.0f, -0.5f, 1.0f, 14.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(-1.5f, -6.0f, 8.0f, -1.2f, 0.0f, -0.15f));
		p24.addOrReplaceChild("strand1_3", CubeListBuilder.create()
				.texOffs(182, 26).addBox(-0.5f, -14.0f, -0.5f, 1.0f, 14.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(1.5f, -6.0f, 8.0f, -1.2f, 0.0f, 0.15f));
		p2.addOrReplaceChild("spine0", CubeListBuilder.create()
				.texOffs(250, 26).addBox(-1.5f, -9.0f, -1.5f, 3.0f, 9.0f, 3.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, -5.5f, 7.0f, -0.45f, 0.0f, 0.0f));
		p2.addOrReplaceChild("strand0_0", CubeListBuilder.create()
				.texOffs(182, 26).addBox(-0.5f, -14.0f, -0.5f, 1.0f, 14.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(-3.0f, -6.0f, 2.0f, -1.2f, 0.0f, -0.35f));
		p2.addOrReplaceChild("strand0_1", CubeListBuilder.create()
				.texOffs(182, 26).addBox(-0.5f, -14.0f, -0.5f, 1.0f, 14.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(3.0f, -6.0f, 2.0f, -1.2f, 0.0f, 0.35f));
		p2.addOrReplaceChild("strand0_2", CubeListBuilder.create()
				.texOffs(182, 26).addBox(-0.5f, -14.0f, -0.5f, 1.0f, 14.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(-1.5f, -6.0f, 8.0f, -1.2f, 0.0f, -0.15f));
		p2.addOrReplaceChild("strand0_3", CubeListBuilder.create()
				.texOffs(182, 26).addBox(-0.5f, -14.0f, -0.5f, 1.0f, 14.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(1.5f, -6.0f, 8.0f, -1.2f, 0.0f, 0.15f));
		p1.addOrReplaceChild("pearl", CubeListBuilder.create()
				.texOffs(130, 26).addBox(-4.0f, -4.0f, -4.0f, 8.0f, 8.0f, 8.0f, new CubeDeformation(0.0f)),
				PartPose.offset(0.0f, -48.0f, -52.0f));
		return LayerDefinition.create(mesh, 512, 256);
	}

	@Override
	public void setupAnim(CheldroganRenderState state) {
		super.setupAnim(state);
		float age = state.ageInTicks;
		float speed = Math.min(state.walkAnimationSpeed, 1.0f);
		float fly = state.flyAmount;
		lids.visible = state.resting;
		ModelPart[] segs = {seg1, seg2, seg3, seg4, seg5, seg6, seg7, seg8};

		// Resonanz-Perle: schwebt und dreht sich immer
		pearl.yRot = age * 0.06f;
		pearl.xRot = age * 0.04f;
		pearl.y += Mth.sin(age * 0.1f) * 2.0f;

		if (state.resting) {
			// Nacht: am Boden eingerollt, atmet tief, die Schwanzflamme flackert
			poseRest(1.0f);
			float breathe = Mth.sin(age * 0.04f);
			seg0.y += breathe * 0.3f;
			neck0.xRot += breathe * 0.03f;
			seg8.yRot += Mth.sin(age * 0.03f) * 0.1f;
			flame.yRot += Mth.sin(age * 0.2f) * 0.15f;
			rWhisker.xRot += breathe * 0.1f;
			lWhisker.xRot += breathe * 0.1f;
			return;
		}

		// Schwebt immer – Beine angelegt, sanftes Auf und Ab
		poseTuck(1.0f);
		dragon.y += Mth.sin(age * 0.08f) * 1.5f;
		float paddle = Mth.sin(age * 0.15f) * 0.15f;
		rfLeg.xRot += paddle;
		lfLeg.xRot -= paddle;
		rhLeg.xRot -= paddle;
		lhLeg.xRot += paddle;

		// Schlängeln: eine Welle läuft vom Hals zur Schwanzspitze, schneller und weiter in Bewegung und im Flug
		// Schlängeln nur in Bewegung – im Stand ruht der Körper, nur ein ganz leichtes Atmen läuft hindurch
		float move = Math.max(speed, fly);
		float t = age * 0.04f + state.walkAnimationPos * 0.45f;
		float amp = 0.015f + 0.2f * move;

		for (int k = 0; k < segs.length; k++) {
			segs[k].yRot += amp * Mth.sin(t - (k + 1) * 0.7f);
			segs[k].xRot += (0.015f + 0.06f * move) * Mth.sin(t * 0.8f - (k + 1) * 0.6f);
		}

		seg0.yRot -= amp * 0.4f * Mth.sin(t);
		neck0.yRot += amp * 0.3f * Mth.sin(t + 0.7f);

		// Blick über Hals und Kopf
		float yaw = Mth.clamp(state.yRot, -60.0f, 60.0f) * Mth.DEG_TO_RAD;
		float pitch = Mth.clamp(state.xRot, -40.0f, 40.0f) * Mth.DEG_TO_RAD;
		neck0.yRot += yaw * 0.3f;
		neck1.yRot += yaw * 0.3f;
		head.yRot += yaw * 0.4f;
		head.xRot += pitch * 0.6f;

		// Barteln, Flossen, Bart und Flamme wehen
		rWhisker.yRot += Mth.sin(age * 0.09f) * 0.15f;
		lWhisker.yRot -= Mth.sin(age * 0.09f + 0.5f) * 0.15f;
		rWhisker.xRot += Mth.sin(age * 0.07f) * 0.12f + move * 0.25f;
		lWhisker.xRot += Mth.sin(age * 0.07f + 1.0f) * 0.12f + move * 0.25f;
		beard.xRot += Mth.sin(age * 0.11f) * 0.1f + move * 0.3f;
		rFin.yRot -= Mth.sin(age * 0.2f) * 0.12f;
		lFin.yRot += Mth.sin(age * 0.2f) * 0.12f;
		flame.yRot += Mth.sin(age * 0.12f) * (0.04f + 0.15f * move);   // Flamme flackert leicht, schwingt nur in Bewegung
		flame.xRot += Mth.sin(age * 0.1f) * 0.03f;

		// Kristall-Odem: Hals senken, Maul weit auf, Kiefer bebt
		if (state.breathProgress > 0.0f) {
			float b = Math.min(1.0f, state.breathProgress * 6.0f) * Math.min(1.0f, (1.0f - state.breathProgress) * 8.0f);
			poseBreath(b);
			jaw.xRot += Mth.sin(age * 1.7f) * 0.05f * b;
		}

		// Splitter-Regen: bäumt sich auf und brüllt in den Himmel
		if (state.rainProgress > 0.0f) {
			float r = Math.min(1.0f, state.rainProgress * 5.0f) * Math.min(1.0f, (1.0f - state.rainProgress) * 5.0f);
			poseRain(r);
			jaw.xRot += Mth.sin(age * 1.3f) * 0.06f * r;
		}

		// Schwanzhieb: krümmt sich und wirbelt einmal um die eigene Achse – der Schweif fegt im Kreis
		if (state.sweepProgress > 0.0f) {
			float p = state.sweepProgress;
			float curl = Mth.sin(p * Mth.PI);
			poseSweep(curl);
			float spin = p < 0.2f ? 0.0f : (p - 0.2f) / 0.8f;
			dragon.yRot += (1.0f - (1.0f - spin) * (1.0f - spin)) * Mth.TWO_PI;
		}
	}

	private void poseTuck(float k) {
		rfLeg.xRot += k * 1.100f;
		lfLeg.xRot += k * 1.100f;
		rhLeg.xRot += k * 1.100f;
		lhLeg.xRot += k * 1.100f;
		rfShin.xRot += k * 0.800f;
		lfShin.xRot += k * 0.800f;
		rhShin.xRot += k * 0.800f;
		lhShin.xRot += k * 0.800f;
	}

	private void poseBreath(float k) {
		neck0.xRot += k * 0.300f;
		neck1.xRot += k * 0.200f;
		head.xRot += k * -0.250f;
		jaw.xRot += k * 0.750f;
		rFin.yRot += k * -0.400f;
		lFin.yRot += k * 0.400f;
	}

	private void poseSweep(float k) {
		seg1.yRot += k * 0.420f;
		seg2.yRot += k * 0.420f;
		seg3.yRot += k * 0.420f;
		seg4.yRot += k * 0.420f;
		seg5.yRot += k * 0.420f;
		seg6.yRot += k * 0.420f;
		seg7.yRot += k * 0.420f;
		seg8.yRot += k * 0.420f;
		seg0.yRot += k * -0.500f;
		neck0.yRot += k * -0.400f;
		flame.yRot += k * 0.500f;
	}

	private void poseRain(float k) {
		seg0.xRot += k * -0.750f;
		seg1.xRot += k * 0.350f;
		seg2.xRot += k * 0.250f;
		seg3.xRot += k * 0.150f;
		neck0.xRot += k * -0.200f;
		head.xRot += k * -0.600f;
		jaw.xRot += k * 0.550f;
		rfLeg.xRot += k * -0.900f;
		rfLeg.zRot += k * -0.400f;
		lfLeg.xRot += k * -0.900f;
		lfLeg.zRot += k * 0.400f;
	}

	private void poseRest(float k) {
		seg0.y += k * 28.500f;
		seg1.yRot += k * 0.750f;
		seg2.yRot += k * 0.750f;
		seg3.yRot += k * 0.700f;
		seg4.yRot += k * 0.700f;
		seg5.yRot += k * 0.650f;
		seg6.yRot += k * 0.650f;
		seg7.yRot += k * 0.600f;
		seg8.yRot += k * 0.550f;
		neck0.xRot += k * 0.400f;
		neck0.yRot += k * -0.500f;
		neck1.xRot += k * 0.300f;
		neck1.yRot += k * -0.400f;
		head.xRot += k * -0.600f;
		pearl.y += k * 30.000f;
		pearl.z += k * 16.000f;
		pearl.x += k * -10.000f;
		rfLeg.xRot += k * -1.200f;
		lfLeg.xRot += k * -1.200f;
		rhLeg.xRot += k * -1.200f;
		lhLeg.xRot += k * -1.200f;
	}

	@Override
	public void translateToHat(PoseStack pose) {
		root().translateAndRotate(pose);
		dragon.translateAndRotate(pose);
		seg0.translateAndRotate(pose);
		neck0.translateAndRotate(pose);
		neck1.translateAndRotate(pose);
		head.translateAndRotate(pose);
		pose.translate(0.0f, 0.0f, -6.0f / 16.0f);
	}

	@Override
	public float hatScale() {
		return 1.6f;
	}
}
