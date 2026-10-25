package dev.bartels.jtechstorage.client.entity;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

public class LastMasterRenderState extends LivingEntityRenderState {
	/** Zauber 0..1. */
	public float cast;
	/** Beschwörung 0..1. */
	public float summon;
	/** Schockwelle 0..1. */
	public float slam;
	/** Siegel schützt ihn. */
	public boolean shielded;
}
