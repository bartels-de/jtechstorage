package dev.bartels.jtechstorage.client.entity;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

public class PhoenixRenderState extends LivingEntityRenderState {
	public int accent = -1;
	public boolean hologram;
	/** Flying (or is a messenger / just taking off). */
	public boolean flying;
	/** Currently away on a trip (invisible). */
	public boolean away;
	/** Sitting at night: sleeping (visual only, the phoenix still works). */
	public boolean resting;
}
