package dev.bartels.jtechstorage.client.entity;

import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;

public class AmethystSpriteRenderState extends ArmedEntityRenderState {
	/** Farbstoff-Farbe der Kristalle oder -1. */
	public int accent = -1;
	/** Hologramm (Jbartels' Rundgang). */
	public boolean hologram;
	/** Feierabend (schläft). */
	public boolean resting;
	/** Trägt der Wichtel gerade etwas? Dann streckt er die Arme nach vorn. */
	public boolean carrying;
	/** Aktions-Animation (0–1): Arbeiten, Alarm oder Niesen. */
	public float action;
}
