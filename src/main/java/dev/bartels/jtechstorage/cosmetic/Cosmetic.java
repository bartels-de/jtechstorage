package dev.bartels.jtechstorage.cosmetic;

import java.util.Locale;

import org.jspecify.annotations.Nullable;

/**
 * Spieler-Kosmetik (v0.34). Jede Kosmetik gehört zu einem Platz und wird durch einen Fortschritt der Mod
 * freigeschaltet ({@code null} = von Anfang an frei).
 */
public enum Cosmetic {
	CRYSTAL_WINGS(Slot.BACK, "crystal_depths"),
	PHOENIX_WINGS(Slot.BACK, "amethyst_phoenix"),
	STORAGE_CAPE(Slot.BACK, "depths_lore"),
	PRISM_ORBS(Slot.BACK, "pure_amethyst"),
	CRYSTAL_HALO(Slot.HEAD, "amethyst_robot"),
	STORAGE_CROWN(Slot.HEAD, "max_upgrade"),
	SHROOM_HAT(Slot.HEAD, "bouncy"),
	WANDERER_HAT(Slot.HEAD, "beyond_dimensions"),
	SHOULDER_QUOKKA(Slot.SHOULDER, "quokka_friend"),
	SHOULDER_SPRITE(Slot.SHOULDER, "sprite"),
	TRAIL_SPARKLE(Slot.TRAIL, "first_link"),
	TRAIL_PETALS(Slot.TRAIL, "friend"),
	TRAIL_SNOW(Slot.TRAIL, "moon_wolf"),
	TRAIL_EMBERS(Slot.TRAIL, "storm_survived"),
	TRAIL_PRISM(Slot.TRAIL, "full_resonance"),
	AURA_CRYSTAL(Slot.AURA, "crystal_enchantment"),
	AURA_FIREFLIES(Slot.AURA, "helper_home"),
	AURA_MEMORY(Slot.AURA, "all_memories"),
	EMOTE_FIREWORK(Slot.EMOTE, null),
	EMOTE_WAVE(Slot.EMOTE, "signal_lantern"),
	EMOTE_QUOKKA(Slot.EMOTE, "quokka_healer"),
	EMOTE_RAIN(Slot.EMOTE, "find_crystal_lair");

	public enum Slot {
		BACK, HEAD, SHOULDER, TRAIL, AURA, EMOTE;

		public String id() {
			return name().toLowerCase(Locale.ROOT);
		}

		public static @Nullable Slot byId(String id) {
			for (Slot slot : values()) {
				if (slot.id().equals(id)) {
					return slot;
				}
			}

			return null;
		}
	}

	public final Slot slot;
	/** Fortschritt (Pfad unter jtechstorage:), der die Kosmetik freischaltet; {@code null} = frei. */
	public final @Nullable String advancement;

	Cosmetic(Slot slot, @Nullable String advancement) {
		this.slot = slot;
		this.advancement = advancement;
	}

	public String id() {
		return name().toLowerCase(Locale.ROOT);
	}

	public String translationKey() {
		return "cosmetic.jtechstorage." + id();
	}

	public static @Nullable Cosmetic byId(String id) {
		for (Cosmetic c : values()) {
			if (c.id().equals(id)) {
				return c;
			}
		}

		return null;
	}
}
