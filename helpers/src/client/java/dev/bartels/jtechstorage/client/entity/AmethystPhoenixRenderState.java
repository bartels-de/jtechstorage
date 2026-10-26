package dev.bartels.jtechstorage.client.entity;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

public class AmethystPhoenixRenderState extends LivingEntityRenderState {
	public int accent = -1;
	public boolean hologram;
	public boolean flying;
	public boolean sitting;
	public int chests;
	/** Speed factor 0..1; when heavily loaded he flaps his wings more slowly. */
	public float speed = 1.0f;
	/** Sleeps on the ground at night. */
	public boolean sleeping;
}
