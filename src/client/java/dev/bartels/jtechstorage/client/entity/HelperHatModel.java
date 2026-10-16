package dev.bartels.jtechstorage.client.entity;

import com.mojang.blaze3d.vertex.PoseStack;

/** Modelle, die einen Kosmetik-Hut tragen können. */
public interface HelperHatModel {
	/**
	 * Bewegt die PoseStack an den „virtuellen Hals“ des Kopfes – so, dass ein Hut nach {@link #hatScale()} und den
	 * Vanilla-Kopf-Transformationen genau um den Kopf sitzt.
	 */
	void translateToHat(PoseStack pose);

	/** Kopfgröße relativ zu einem 8-Pixel-Spielerkopf. */
	float hatScale();
}
