package dev.bartels.jtechstorage.client.entity;

import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;

public class CollectorMothRenderState extends ArmedEntityRenderState {
	/** Dye color of the crystals, or -1. */
	public int accent = -1;
	/** Hologram (Jbartels' tour). */
	public boolean hologram;
	/** Off duty (sleeping). */
	public boolean resting;
	public boolean carrying;
	/** Action animation (0–1): working, alarm or sneezing. */
	public float action;
}
