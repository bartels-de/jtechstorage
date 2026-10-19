package dev.bartels.jtechstorage.client.entity;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

/** Render-State der Begleiter (Mantis, Pilz, Rabe). */
public class CompanionRenderState extends LivingEntityRenderState {
	/** Stufe 1–3: bestimmt Modell und Textur. */
	public int stage = 1;
	public boolean sitting;
	public boolean exhausted;
	public boolean flying;
	/** Angriff 0..1 und Aktion (Heilen, Picken) 0..1, 0 = inaktiv. */
	public float attack;
	public float action;
	/** Sitzt auf der Schulter (v0.25.1): Versatz von der eigenen Position zur Schulter des Besitzers. */
	public boolean shoulder;
	public net.minecraft.world.phys.Vec3 shoulderOffset = net.minecraft.world.phys.Vec3.ZERO;
	/** Aussehen (v0.25.4): Skin, Körperteil-Varianten, Ausrüstungs-Material je Platz (0 = keins) und Leuchtfarbe (-1 = Standard). */
	public int skin;
	public int part1;
	public int part2;
	public final int[] equip = new int[3];
	public int glow = -1;
	/** Bindungs-Geste (v0.26.0): 0 keine, 1 Freudensprung, 2 Kuscheln, 3 eigene Geste; Fortschritt 0..1. */
	public int gesture;
	public float gestureProgress;
}
