package dev.bartels.jtechstorage.client.entity;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

public class AmethystSpiderRenderState extends LivingEntityRenderState {
	public int anim;
	/** Fortschritt der aktuellen Angriffs-Animation 0..1. */
	public float progress;
	public boolean enraged;
	public boolean leaping;
	public boolean hologram;
}
