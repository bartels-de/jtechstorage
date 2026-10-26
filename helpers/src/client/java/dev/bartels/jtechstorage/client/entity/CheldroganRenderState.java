package dev.bartels.jtechstorage.client.entity;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

public class CheldroganRenderState extends LivingEntityRenderState {
	/** Dye color of the crystals, or -1. */
	public int accent = -1;
	public boolean hologram;
	/** Tail strike 0..1 (0 = no attack). */
	public float sweepProgress;
	/** Crystal breath 0..1 (0 = no breath). */
	public float breathProgress;
	/** Shard rain 0..1 (0 = inactive). */
	public float rainProgress;
	/** 0 = hovers near the ground ... 1 = flies with rider. */
	public float flyAmount;
	/** Night: curled up on the ground. */
	public boolean resting;
}
