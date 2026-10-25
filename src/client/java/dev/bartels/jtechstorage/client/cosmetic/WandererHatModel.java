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
 * Wanderhut: breitkrempiger Lederhut mit Kristallband und Amethyst-Schnalle.
 * Modell in doppelter Detaildichte (halbe Pixel) – der Renderer skaliert es auf 0.5.
 * Erzeugt von scratchpad/cosmetics.py (Teile und Posen), Animation von Hand.
 */
public class WandererHatModel extends EntityModel<AvatarRenderState> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(JTechStorage.id("cosmetic_wanderer_hat"), "main");

	private final ModelPart hat;

	public WandererHatModel(ModelPart root) {
		super(root);
		this.hat = root.getChild("hat");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		PartDefinition p1 = root.addOrReplaceChild("hat", CubeListBuilder.create()
				.texOffs(0, 21).addBox(-13.0f, 0.0f, -9.0f, 26.0f, 1.0f, 18.0f, new CubeDeformation(0.0f))
				.texOffs(0, 0).addBox(-8.0f, -6.0f, -7.5f, 16.0f, 6.0f, 15.0f, new CubeDeformation(0.0f))
				.texOffs(66, 40).addBox(-7.0f, -9.0f, -6.0f, 14.0f, 3.0f, 12.0f, new CubeDeformation(0.0f))
				.texOffs(38, 58).addBox(-2.0f, -9.5f, -4.0f, 4.0f, 1.0f, 8.0f, new CubeDeformation(0.0f))
				.texOffs(0, 40).addBox(-8.5f, -2.0f, -8.0f, 17.0f, 2.0f, 16.0f, new CubeDeformation(0.0f))
				.texOffs(62, 58).addBox(-1.5f, -2.5f, -8.6f, 3.0f, 3.0f, 1.0f, new CubeDeformation(0.0f))
				.texOffs(70, 58).addBox(-0.5f, -1.5f, -9.0f, 1.0f, 1.0f, 1.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(0.0f, -15.5f, 0.0f, -0.08f, 0.0f, 0.0f));
		p1.addOrReplaceChild("brim_r", CubeListBuilder.create()
				.texOffs(0, 58).addBox(-5.0f, 0.0f, -7.0f, 5.0f, 1.0f, 14.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(-13.0f, 0.0f, 0.0f, 0.0f, 0.0f, -0.35f));
		p1.addOrReplaceChild("brim_l", CubeListBuilder.create()
				.texOffs(0, 58).addBox(0.0f, 0.0f, -7.0f, 5.0f, 1.0f, 14.0f, new CubeDeformation(0.0f)),
				PartPose.offsetAndRotation(13.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.35f));
		return LayerDefinition.create(mesh, 128, 128);
	}

	@Override
	public void setupAnim(AvatarRenderState state) {
		super.setupAnim(state);

	}




}
