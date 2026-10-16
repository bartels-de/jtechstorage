package dev.bartels.jtechstorage.client.entity;

import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/** Gemeinsame Hologramm-Darstellung: durchscheinend, selbstleuchtend, lila getönt und flimmernd. */
public final class HologramRendering {
	private HologramRendering() {
	}

	public static RenderType renderType(Identifier texture) {
		return RenderTypes.entityTranslucentEmissive(texture);
	}

	/** ARGB-Tönung mit flackerndem Alpha (Hologramm-Effekt). */
	public static int tint(float age) {
		float flicker = 0.55f + 0.12f * Mth.sin(age * 0.35f) + (Mth.sin(age * 2.7f) > 0.97f ? -0.25f : 0.0f);
		int alpha = Mth.clamp((int) (flicker * 255), 40, 230);
		return (alpha << 24) | 0xB89CFF;
	}
}
