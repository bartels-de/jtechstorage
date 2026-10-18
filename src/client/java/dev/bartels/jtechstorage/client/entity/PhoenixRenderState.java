package dev.bartels.jtechstorage.client.entity;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

public class PhoenixRenderState extends LivingEntityRenderState {
	public int accent = -1;
	public boolean hologram;
	/** Fliegt (oder ist Bote / startet gerade). */
	public boolean flying;
	/** Ist gerade unterwegs (unsichtbar). */
	public boolean away;
	/** Nachts sitzend: schläft (nur Optik, der Phönix arbeitet trotzdem). */
	public boolean resting;
}
