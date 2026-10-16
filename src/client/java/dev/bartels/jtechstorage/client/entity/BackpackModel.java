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
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;

import dev.bartels.jtechstorage.JTechStorage;

/** 3D-Rucksack auf dem Rücken (Koordinaten relativ zum Oberkörper). */
public class BackpackModel extends EntityModel<HumanoidRenderState> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(JTechStorage.id("backpack"), "main");

	public BackpackModel(ModelPart root) {
		super(root);
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		root.addOrReplaceChild("backpack", CubeListBuilder.create()
				.texOffs(0, 0).addBox(-4.0f, 1.0f, 2.0f, 8.0f, 10.0f, 4.0f, new CubeDeformation(0.0f))
				.texOffs(24, 0).addBox(-4.0f, 0.5f, 2.0f, 8.0f, 3.0f, 4.0f, new CubeDeformation(0.3f))
				.texOffs(48, 0).addBox(-3.0f, 2.0f, 5.9f, 6.0f, 4.0f, 1.0f, new CubeDeformation(0.0f))
				.texOffs(48, 6).addBox(-1.0f, 4.6f, 6.5f, 2.0f, 2.0f, 1.0f, new CubeDeformation(0.0f))
				.texOffs(0, 14).addBox(-3.0f, 6.6f, 6.0f, 6.0f, 4.0f, 1.0f, new CubeDeformation(0.0f))
				.texOffs(14, 14).addBox(-5.0f, 5.5f, 3.0f, 1.0f, 4.0f, 2.0f, new CubeDeformation(0.0f))
				.texOffs(14, 14).mirror().addBox(4.0f, 5.5f, 3.0f, 1.0f, 4.0f, 2.0f, new CubeDeformation(0.0f)).mirror(false)
				.texOffs(20, 14).addBox(-3.5f, 0.0f, -2.6f, 1.0f, 9.0f, 1.0f, new CubeDeformation(-0.2f))
				.texOffs(20, 14).addBox(2.5f, 0.0f, -2.6f, 1.0f, 9.0f, 1.0f, new CubeDeformation(-0.2f))
				.texOffs(24, 14).addBox(-3.5f, -0.6f, -2.6f, 1.0f, 1.0f, 5.0f, new CubeDeformation(-0.2f))
				.texOffs(24, 14).addBox(2.5f, -0.6f, -2.6f, 1.0f, 1.0f, 5.0f, new CubeDeformation(-0.2f))
				.texOffs(36, 14).addBox(-2.0f, -0.8f, 3.5f, 4.0f, 1.0f, 1.0f, new CubeDeformation(-0.1f))
				.texOffs(0, 24).addBox(-4.5f, 10.4f, 2.6f, 9.0f, 3.0f, 3.0f, new CubeDeformation(0.0f))
				.texOffs(0, 32).addBox(-3.0f, 10.4f, 2.6f, 1.0f, 3.0f, 3.0f, new CubeDeformation(0.15f))
				.texOffs(0, 32).addBox(2.0f, 10.4f, 2.6f, 1.0f, 3.0f, 3.0f, new CubeDeformation(0.15f)),
				PartPose.ZERO);
		return LayerDefinition.create(mesh, 64, 64);
	}

}
