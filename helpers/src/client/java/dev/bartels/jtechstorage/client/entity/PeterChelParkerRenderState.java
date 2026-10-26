package dev.bartels.jtechstorage.client.entity;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

public class PeterChelParkerRenderState extends LivingEntityRenderState {
	/** Dye color of the crystals, or -1. */
	public int accent = -1;
	/** Hologram (Jbartels' tour). */
	public boolean hologram;
	/** Hammer strike progress 0..1 (0 = no attack). */
	public float attackProgress;
	/** Night watch: dozes leaning on the hammer. */
	public boolean dozing;
}
