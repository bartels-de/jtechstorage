package dev.bartels.jtechstorage.client.entity;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

public class AmethystPhoenixRenderState extends LivingEntityRenderState {
	public int accent = -1;
	public boolean hologram;
	public boolean flying;
	public boolean sitting;
	public int chests;
	/** Tempo-Faktor 0..1 – schwer beladen schlägt er langsamer mit den Flügeln. */
	public float speed = 1.0f;
}
