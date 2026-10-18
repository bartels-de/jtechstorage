package dev.bartels.jtechstorage.client.entity;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

public class PeterChelParkerRenderState extends LivingEntityRenderState {
	/** Farbstoff-Farbe der Kristalle oder -1. */
	public int accent = -1;
	/** Hologramm (Jbartels' Rundgang). */
	public boolean hologram;
	/** Fortschritt des Hammerschlags 0..1 (0 = kein Angriff). */
	public float attackProgress;
	/** Nachtwache: döst auf den Hammer gestützt. */
	public boolean dozing;
}
