package dev.bartels.jtechstorage.client.cosmetic;

import net.minecraft.client.renderer.entity.state.AvatarRenderState;
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
 * Schulter-Quokka: ein kleines Quokka, das auf der linken Schulter sitzt und sich umsieht.
 * Modell in doppelter Detaildichte (halbe Pixel) – der Renderer skaliert es auf 0.5.
 * Erzeugt von scratchpad/cosmetics.py (Teile und Posen), Animation von Hand.
 */
public class ShoulderQuokkaModel extends EntityModel<AvatarRenderState> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(JTechStorage.id("cosmetic_shoulder_quokka"), "main");

	private final ModelPart quokka;
	private final ModelPart body;
	private final ModelPart head;
	private final ModelPart rEar;
	private final ModelPart lEar;
	private final ModelPart tail;

	public ShoulderQuokkaModel(ModelPart root) {
		super(root);
		this.quokka = root.getChild("quokka");
		this.body = root.getChild("quokka").getChild("body");
		this.head = root.getChild("quokka").getChild("body").getChild("head");
		this.rEar = root.getChild("quokka").getChild("body").getChild("head").getChild("r_ear");
		this.lEar = root.getChild("quokka").getChild("body").getChild("head").getChild("l_ear");
		this.tail = root.getChild("quokka").getChild("body").getChild("tail");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		PartDefinition p1 = root.addOrReplaceChild("quokka", CubeListBuilder.create(),
				PartPose.offsetAndRotation(11.0f, -0.5f, 0.5f, 0.0f, 0.25f, 0.0f));
		PartDefinition p2 = p1.addOrReplaceChild("body", CubeListBuilder.create()
				.texOffs(0, 0).addBox(-3.0f, -6.0f, -3.5f, 6.0f, 6.0f, 7.0f, new CubeDeformation(0.0f))
				.texOffs(20, 13).addBox(-2.0f, -4.5f, -3.8f, 4.0f, 4.0f, 1.0f, new CubeDeformation(0.0f))
				.texOffs(40, 13).addBox(-2.5f, -2.0f, -4.5f, 2.0f, 2.0f, 2.0f, new CubeDeformation(0.0f))
				.texOffs(40, 13).addBox(0.5f, -2.0f, -4.5f, 2.0f, 2.0f, 2.0f, new CubeDeformation(0.0f))
				.texOffs(8, 13).addBox(-3.0f, -1.0f, -3.0f, 2.0f, 1.0f, 4.0f, new CubeDeformation(0.0f))
				.texOffs(8, 13).addBox(1.0f, -1.0f, -3.0f, 2.0f, 1.0f, 4.0f, new CubeDeformation(0.0f))
				.texOffs(0, 13).addBox(-1.0f, -10.0f, -0.5f, 2.0f, 4.0f, 2.0f, new CubeDeformation(0.0f))
				.texOffs(48, 13).addBox(-2.5f, -8.5f, 1.5f, 1.0f, 3.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.ZERO);
		PartDefinition p3 = p2.addOrReplaceChild("head", CubeListBuilder.create()
				.texOffs(26, 0).addBox(-3.0f, -5.0f, -3.0f, 6.0f, 5.0f, 5.0f, new CubeDeformation(0.0f))
				.texOffs(30, 13).addBox(-1.5f, -2.5f, -4.5f, 3.0f, 2.0f, 2.0f, new CubeDeformation(0.0f))
				.texOffs(58, 13).addBox(-0.5f, -2.5f, -5.0f, 1.0f, 1.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offset(0.0f, -5.0f, -2.5f));
		p3.addOrReplaceChild("r_ear", CubeListBuilder.create()
				.texOffs(52, 13).addBox(-1.0f, -2.0f, -0.5f, 2.0f, 2.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(-2.5f, -4.5f, -0.5f, 0.0f, 0.0f, -0.4f));
		p3.addOrReplaceChild("l_ear", CubeListBuilder.create()
				.texOffs(52, 13).addBox(-1.0f, -2.0f, -0.5f, 2.0f, 2.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(2.5f, -4.5f, -0.5f, 0.0f, 0.0f, 0.4f));
		p2.addOrReplaceChild("tail", CubeListBuilder.create()
				.texOffs(48, 0).addBox(-1.0f, -1.0f, 0.0f, 2.0f, 2.0f, 6.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, -1.0f, 3.0f, 0.6f, 0.0f, 0.0f));
		return LayerDefinition.create(mesh, 64, 64);
	}

	@Override
	public void setupAnim(AvatarRenderState state) {
		super.setupAnim(state);
		float age = state.ageInTicks;
		head.yRot += Mth.clamp(state.yRot, -50.0f, 50.0f) * Mth.DEG_TO_RAD * 0.7f + Mth.sin(age * 0.023f) * 0.25f;
		head.xRot += state.xRot * Mth.DEG_TO_RAD * 0.4f;
		body.y += Mth.sin(age * 0.12f) * 0.25f;
		tail.yRot += Mth.sin(age * 0.18f) * 0.35f;

		if (((int) (age / 60.0f) + state.id) % 4 == 0) {
			rEar.zRot -= Math.abs(Mth.sin(age * 0.9f)) * 0.4f;
			lEar.zRot += Math.abs(Mth.sin(age * 0.9f + 0.6f)) * 0.4f;
		}

		if (state.isCrouching) {
			quokka.y += 1.5f;
		}
	}




}
