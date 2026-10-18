package dev.bartels.jtechstorage.client.entity;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

/** Render-State für Kristallwolf und Mondwolf. */
public class CrystalWolfRenderState extends LivingEntityRenderState {
	public int accent = -1;
	public boolean hologram;
	public boolean resting;
	public boolean exhausted;
	public boolean angry;
	/** Sitzt (wilder Mondwolf auf seiner Anhöhe). */
	public boolean sitting;
	/** Biss 0..1, Heulen 0..1 (0 = inaktiv). */
	public float bite;
	public float howl;
}
