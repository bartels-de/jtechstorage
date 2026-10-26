package dev.bartels.jtechstorage.client.entity;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

public class AmethystRobotRenderState extends LivingEntityRenderState {
	public int anim;
	/** Progress of the current attack animation 0..1. */
	public float progress;
	public boolean enraged;
	public boolean hologram;
	/** Idle mode: no target, powered down. */
	public boolean idle;
}
