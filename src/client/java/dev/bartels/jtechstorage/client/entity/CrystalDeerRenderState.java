package dev.bartels.jtechstorage.client.entity;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

public class CrystalDeerRenderState extends LivingEntityRenderState {
	/** Sattel liegt. */
	public boolean saddled;
	/** Grasen 0..1. */
	public float graze;
	/** Aufbäumen/Sprung 0..1. */
	public float rear;
}
