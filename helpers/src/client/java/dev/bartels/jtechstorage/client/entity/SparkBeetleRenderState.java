package dev.bartels.jtechstorage.client.entity;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

public class SparkBeetleRenderState extends LivingEntityRenderState {
	/** Dye color of the crystals, or -1. */
	public int accent = -1;
	/** Hologram (Jbartels' tour). */
	public boolean hologram;
	/** Off duty (sleeping). */
	public boolean resting;
	public boolean charging;
}
