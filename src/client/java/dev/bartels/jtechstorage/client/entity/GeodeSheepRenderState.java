package dev.bartels.jtechstorage.client.entity;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

public class GeodeSheepRenderState extends LivingEntityRenderState {
	/** geschoren. */
	public boolean sheared;
	/** Wollfarbe (ARGB). */
	public int woolColor;
	/** Grasen 0..1. */
	public float graze;
}
