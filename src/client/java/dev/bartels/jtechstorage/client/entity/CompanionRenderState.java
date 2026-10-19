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
}
