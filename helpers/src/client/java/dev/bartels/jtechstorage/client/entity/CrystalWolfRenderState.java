package dev.bartels.jtechstorage.client.entity;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

/** Render state for Crystal Wolf and Moon Wolf. */
public class CrystalWolfRenderState extends LivingEntityRenderState {
	public int accent = -1;
	public boolean hologram;
	public boolean resting;
	public boolean exhausted;
	public boolean angry;
	/** Sitting (wild Moon Wolf on its hilltop). */
	public boolean sitting;
	/** Bite 0..1, howl 0..1 (0 = inactive). */
	public float bite;
	public float howl;
}
