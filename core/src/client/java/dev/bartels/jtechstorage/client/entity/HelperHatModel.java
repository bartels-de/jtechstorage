package dev.bartels.jtechstorage.client.entity;

import com.mojang.blaze3d.vertex.PoseStack;

/** Models that can wear a cosmetic hat. */
public interface HelperHatModel {
	/**
	 * Moves the PoseStack to the head's "virtual neck" – so that a hat, after {@link #hatScale()} and the
	 * vanilla head transforms, sits exactly around the head.
	 */
	void translateToHat(PoseStack pose);

	/** Head size relative to an 8-pixel player head. */
	float hatScale();
}
