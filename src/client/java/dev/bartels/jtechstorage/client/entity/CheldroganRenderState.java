package dev.bartels.jtechstorage.client.entity;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

public class CheldroganRenderState extends LivingEntityRenderState {
	/** Farbstoff-Farbe der Kristalle oder -1. */
	public int accent = -1;
	public boolean hologram;
	/** Schwanzhieb 0..1 (0 = kein Angriff). */
	public float sweepProgress;
	/** Kristall-Odem 0..1 (0 = kein Odem). */
	public float breathProgress;
	/** Splitter-Regen 0..1 (0 = inaktiv). */
	public float rainProgress;
	/** 0 = schwebt in Bodennähe … 1 = fliegt mit Reiter. */
	public float flyAmount;
	/** Nacht: am Boden eingerollt. */
	public boolean resting;
}
