package dev.bartels.jtechstorage.client.entity;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

public class CrystalDeerRenderState extends LivingEntityRenderState {
	/** Saddle is on. */
	public boolean saddled;
	/** Grazing 0..1. */
	public float graze;
	/** Rearing/jump 0..1. */
	public float rear;
}
