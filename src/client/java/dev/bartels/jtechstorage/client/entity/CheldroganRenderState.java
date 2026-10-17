package dev.bartels.jtechstorage.client.entity;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

public class CheldroganRenderState extends LivingEntityRenderState {
	/** Farbstoff-Farbe der Kristalle oder -1. */
	public int accent = -1;
	public boolean hologram;
	/** Klauenhieb 0..1 (0 = kein Angriff). */
	public float attackProgress;
	/** Kristall-Odem 0..1 (0 = kein Odem). */
	public float breathProgress;
	/** 0 = am Boden (Flügel angelegt) … 1 = fliegt (Flügel ausgebreitet). */
	public float flyAmount;
}
