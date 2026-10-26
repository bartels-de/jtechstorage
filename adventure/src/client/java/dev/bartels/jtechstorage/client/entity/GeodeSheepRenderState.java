package dev.bartels.jtechstorage.client.entity;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

public class GeodeSheepRenderState extends LivingEntityRenderState {
	/** sheared. */
	public boolean sheared;
	/** Wool color (ARGB). */
	public int woolColor;
	/** Grazing 0..1. */
	public float graze;
}
