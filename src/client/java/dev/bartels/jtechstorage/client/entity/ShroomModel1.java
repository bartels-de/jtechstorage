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
 * Amethyst-Pilz, Stufe 1 (Begleiter v0.25).
 * Modell in doppelter Detaildichte (halbe Pixel) – der Renderer skaliert es auf 0.5.
 * Erzeugt von scratchpad/shroom.py (Teile und Posen), Animation von Hand.
 */
public class ShroomModel1 extends EntityModel<CompanionRenderState> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(JTechStorage.id("shroom_1"), "main");

	private final ModelPart shroom;
	private final ModelPart body;
	private final ModelPart cap;
	private final ModelPart rLeg;
	private final ModelPart lLeg;

	public ShroomModel1(ModelPart root) {
		super(root);
		this.shroom = root.getChild("shroom");
		this.body = root.getChild("shroom").getChild("body");
		this.cap = root.getChild("shroom").getChild("body").getChild("cap");
		this.rLeg = root.getChild("shroom").getChild("body").getChild("r_leg");
		this.lLeg = root.getChild("shroom").getChild("body").getChild("l_leg");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		PartDefinition p1 = root.addOrReplaceChild("shroom", CubeListBuilder.create(),
				PartPose.offset(0.0f, 24.0f, 0.0f));
		PartDefinition p2 = p1.addOrReplaceChild("body", CubeListBuilder.create()
				.texOffs(64, 0).addBox(-4.0f, -9.0f, -4.0f, 8.0f, 9.0f, 8.0f, new CubeDeformation(0.0f)),
				PartPose.offset(0.0f, -6.0f, 0.0f));
		p2.addOrReplaceChild("cap", CubeListBuilder.create()
				.texOffs(0, 0).addBox(-8.0f, -5.0f, -8.0f, 16.0f, 5.0f, 16.0f, new CubeDeformation(0.0f))
				.texOffs(0, 21).addBox(-5.0f, -8.0f, -5.0f, 10.0f, 3.0f, 10.0f, new CubeDeformation(0.0f)),
				PartPose.offset(0.0f, -7.0f, 0.0f));
		p2.addOrReplaceChild("r_leg", CubeListBuilder.create()
				.texOffs(58, 21).addBox(-1.5f, 0.0f, -1.5f, 3.0f, 4.0f, 3.0f, new CubeDeformation(0.0f))
				.texOffs(40, 21).addBox(-2.0f, 4.0f, -3.0f, 4.0f, 2.0f, 5.0f, new CubeDeformation(0.0f)),
				PartPose.offset(-2.5f, 0.0f, 0.0f));
		p2.addOrReplaceChild("l_leg", CubeListBuilder.create()
				.texOffs(58, 21).addBox(-1.5f, 0.0f, -1.5f, 3.0f, 4.0f, 3.0f, new CubeDeformation(0.0f))
				.texOffs(40, 21).addBox(-2.0f, 4.0f, -3.0f, 4.0f, 2.0f, 5.0f, new CubeDeformation(0.0f)),
				PartPose.offset(2.5f, 0.0f, 0.0f));
		return LayerDefinition.create(mesh, 128, 64);
	}

	@Override
	public void setupAnim(CompanionRenderState state) {
		super.setupAnim(state);
		float age = state.ageInTicks;
		float pos = state.walkAnimationPos;
		float speed = Math.min(state.walkAnimationSpeed, 1.0f);

		if (state.sitting || state.exhausted) {
			poseSit(1.0f);
		}

		if (state.exhausted) {
			poseTired(1.0f);
		}

		// Watscheln: kurze Schritte, der Stiel schaukelt, der Hut wippt nach
		float swing = Mth.cos(pos * 1.0f) * 0.8f * speed;
		rLeg.xRot += swing;
		lLeg.xRot -= swing;
		body.zRot += Mth.sin(pos * 0.5f) * 0.12f * speed;
		body.y += -Mth.abs(Mth.sin(pos * 1.0f)) * 1.0f * speed;
		cap.zRot -= Mth.sin(pos * 0.5f - 0.6f) * 0.08f * speed;

		// Ruhig atmen: der Hut hebt und senkt sich
		cap.y += Mth.sin(age * 0.08f) * 0.35f;
		body.xRot += Mth.sin(age * 0.05f) * 0.02f;
		float yaw = Mth.clamp(state.yRot, -40.0f, 40.0f) * Mth.DEG_TO_RAD;
		body.yRot += yaw * 0.3f;
		cap.yRot += yaw * 0.2f;

		// Kopfnuss: mit dem Hut voran
		if (state.attack > 0.0f) {
			float a = Mth.sin(state.attack * Mth.PI);
			body.xRot += 0.45f * a;
			cap.xRot += 0.25f * a;
			body.z -= 1.5f * a;
		}

		// Heilen: Arme hoch, hüpft vor Freude, der Hut leuchtet (Partikel)
		if (state.action > 0.0f) {
			float h = Mth.sin(state.action * Mth.PI);
			body.y -= Mth.abs(Mth.sin(state.action * Mth.PI * 3.0f)) * 2.0f * h;
			cap.xRot -= 0.1f * h;
		}
	}

	private void poseSit(float k) {
		rLeg.xRot += k * -1.450f;
		lLeg.xRot += k * -1.450f;
		body.y += k * 3.290f;
	}

	private void poseTired(float k) {
		cap.xRot += k * 0.300f;
		cap.zRot += k * 0.150f;
	}

	private void poseHeal(float k) {
		cap.xRot += k * -0.150f;
	}


}
