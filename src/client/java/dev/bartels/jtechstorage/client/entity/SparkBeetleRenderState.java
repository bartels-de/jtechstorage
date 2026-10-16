package dev.bartels.jtechstorage.client.entity;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

public class SparkBeetleRenderState extends LivingEntityRenderState {
	/** Farbstoff-Farbe der Kristalle oder -1. */
	public int accent = -1;
	/** Hologramm (Jbartels' Rundgang). */
	public boolean hologram;
	/** Feierabend (schläft). */
	public boolean resting;
	public boolean charging;
}
