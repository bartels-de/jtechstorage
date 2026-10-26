package dev.bartels.jtechstorage.client.entity;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

public class LastMasterRenderState extends LivingEntityRenderState {
	/** Spell 0..1. */
	public float cast;
	/** Summoning 0..1. */
	public float summon;
	/** Shockwave 0..1. */
	public float slam;
	/** The seal protects him. */
	public boolean shielded;
}
