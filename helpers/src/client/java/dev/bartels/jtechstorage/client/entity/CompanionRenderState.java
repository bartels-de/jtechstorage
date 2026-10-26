package dev.bartels.jtechstorage.client.entity;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.phys.Vec3;

/** Render state of the companions (mantis, mushroom, raven). */
public class CompanionRenderState extends LivingEntityRenderState {
	/** Tier 1–3: determines model and texture. */
	public int stage = 1;
	public boolean sitting;
	public boolean exhausted;
	public boolean flying;
	/** Attack 0..1 and action (heal, peck) 0..1, 0 = inactive. */
	public float attack;
	public float action;
	/** Sits on the shoulder (v0.25.1): offset from own position to the owner's shoulder. */
	public boolean shoulder;
	public Vec3 shoulderOffset = Vec3.ZERO;
	/** Appearance (v0.25.4): skin, body part variants, equipment material per slot (0 = none) and glow color (-1 = default). */
	public int skin;
	public int part1;
	public int part2;
	public final int[] equip = new int[3];
	public int glow = -1;
	/** Bond gesture (v0.26.0): 0 none, 1 joy jump, 2 cuddle, 3 custom gesture; progress 0..1. */
	public int gesture;
	public float gestureProgress;
}
