package dev.bartels.jtechstorage.client.entity;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;

/**
 * Companion model with customization (v0.25.4): contains all body part variants and the equipment. The variants are shown and hidden in
 * {@code setupAnim} via the render state; the equipment is drawn by a separate model instance per
 * slot (collar, amulet, armor) using the texture of the chosen material.
 */
public interface CompanionLookModel {
	/** This instance now draws only the equipment of slot {@code slot} (0 collar, 1 amulet, 2 armor). */
	void equipmentPass(int slot);

	/** Copies the motion (deviation from the base pose) of a part onto its variant. */
	static void follow(ModelPart src, ModelPart dst) {
		PartPose a = src.getInitialPose();
		PartPose b = dst.getInitialPose();
		dst.x = b.x() + src.x - a.x();
		dst.y = b.y() + src.y - a.y();
		dst.z = b.z() + src.z - a.z();
		dst.xRot = b.xRot() + src.xRot - a.xRot();
		dst.yRot = b.yRot() + src.yRot - a.yRot();
		dst.zRot = b.zRot() + src.zRot - a.zRot();
	}
}
