package dev.bartels.jtechstorage.client.entity;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

public class AshleyRenderState extends LivingEntityRenderState {
	/** Dye color of the crystals, or -1. */
	public int accent = -1;
	/** Hologram (Jbartels' tour). */
	public boolean hologram;
	/** Off duty (sleeping). */
	public boolean resting;
	/** Action animation (0–1): working, alarm or sneezing. */
	public float action;
}
