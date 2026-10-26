package dev.bartels.jtechstorage.companion;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Companion talent tree (v0.25.3): four branches per species with five talents each.
 * <ul>
 *     <li>Branches 1-3: normal talents; a talent needs at least one point in the talent above it, the fifth is a
 *     master talent from tier 3.</li>
 *     <li>Branch 4 "Special": three special attacks of which only <b>one</b> can be learned (key, default B), plus
 *     "Mastery" (needs a special attack) and "Instinct" (needs Mastery).</li>
 * </ul>
 * One point every 4 levels (up to 30 at level 120). Resetting costs one amethyst core.
 * Texts: {@code jtechstorage.talent.<id>} (name) and {@code .desc} (effect per rank).
 */
public final class CompanionTalents {
	public static final int BRANCHES = 4;
	public static final int PER_BRANCH = 5;
	public static final int COUNT = BRANCHES * PER_BRANCH;
	public static final int LEVELS_PER_POINT = 4;
	public static final int MAX_POINTS = 30;
	/** Index of the first special attack (branch 4); the three special attacks are at SPECIAL..SPECIAL+2. */
	public static final int SPECIAL = 3 * PER_BRANCH;
	public static final int MASTERY = SPECIAL + 3;
	public static final int INSTINCT = SPECIAL + 4;
	/** Old format (v0.25.0-0.25.2): 3 branches x 4 talents. */
	private static final int OLD_PER_BRANCH = 4;
	private static final int OLD_COUNT = 12;

	/** @param symbol short character for the menu symbol, color colour of the symbol */
	public record Talent(String id, int max, int minStage, String symbol, int color) {
	}

	public record Tree(List<String> branches, List<Talent> talents) {
		public Talent get(int index) {
			return talents.get(index);
		}
	}

	private static final Map<String, Tree> TREES = Map.of(
			"mantis", new Tree(List.of("blade", "armor", "hunt", "special"), List.of(
					new Talent("sharp_blades", 3, 1, "⚔", 0xFFB07CF0),
					new Talent("quick_strike", 2, 1, "»", 0xFF7FD06A),
					new Talent("precision", 2, 2, "✦", 0xFFE0B040),
					new Talent("whirl_scythe", 1, 3, "☼", 0xFFC868FF),
					new Talent("bloodlust", 1, 3, "♨", 0xFFE04848),
					new Talent("chitin", 3, 1, "◆", 0xFF8C8CA8),
					new Talent("crystal_plates", 2, 1, "▣", 0xFFB07CF0),
					new Talent("thorns", 2, 2, "✶", 0xFFC0603A),
					new Talent("unbreakable", 1, 3, "♥", 0xFFE04848),
					new Talent("crystal_skin", 1, 3, "❖", 0xFFD4B4FF),
					new Talent("swift_legs", 2, 1, "»", 0xFF6ACAD0),
					new Talent("hunt_instinct", 2, 1, "◎", 0xFFE0B040),
					new Talent("leap_attack", 1, 2, "↑", 0xFF7FD06A),
					new Talent("crystal_flight", 2, 3, "✧", 0xFFC868FF),
					new Talent("predator", 1, 3, "☠", 0xFFC0603A),
					new Talent("paralyzing_sting", 1, 2, "✹", 0xFF7FD06A),
					new Talent("blade_storm", 1, 2, "✺", 0xFFC868FF),
					new Talent("execution", 1, 2, "⚡", 0xFFE04848),
					new Talent("mastery", 2, 3, "★", 0xFFFFD27A),
					new Talent("instinct", 1, 3, "☯", 0xFFFFFFFF))),
			"shroom", new Tree(List.of("healing", "protection", "poison", "special"), List.of(
					new Talent("strong_spores", 3, 1, "♥", 0xFFE06890),
					new Talent("fast_bloom", 2, 1, "»", 0xFF7FD06A),
					new Talent("wide_spores", 2, 2, "◎", 0xFFD4B4FF),
					new Talent("lifesaver", 1, 3, "✚", 0xFFFFE070),
					new Talent("second_life", 1, 3, "❀", 0xFFFF9AC8),
					new Talent("tough_stalk", 3, 1, "▮", 0xFFC8A070),
					new Talent("spore_shield", 2, 1, "◆", 0xFFE0C040),
					new Talent("calm_aura", 2, 2, "☁", 0xFF9CC8F0),
					new Talent("purify", 1, 3, "✧", 0xFFFFFFFF),
					new Talent("mushroom_skin", 1, 3, "▣", 0xFFB07CF0),
					new Talent("toxic_spores", 3, 1, "☠", 0xFF7FD06A),
					new Talent("spore_burst", 2, 1, "✶", 0xFFB07CF0),
					new Talent("numbing_spores", 2, 2, "❄", 0xFF8CC8E0),
					new Talent("toxic_cloud", 1, 3, "☣", 0xFF5AC040),
					new Talent("spore_field", 1, 3, "⁂", 0xFF9CF08A),
					new Talent("spore_veil", 1, 2, "◌", 0xFFD4B4FF),
					new Talent("life_bloom", 1, 2, "✿", 0xFFFF9AC8),
					new Talent("paralysis_cloud", 1, 2, "☁", 0xFF7FD06A),
					new Talent("mastery", 2, 3, "★", 0xFFFFD27A),
					new Talent("instinct", 1, 3, "☯", 0xFFFFFFFF))),
			"raven", new Tree(List.of("bag", "combat", "flight", "special"), List.of(
					new Talent("keen_eye", 3, 1, "◎", 0xFFE0B040),
					new Talent("eager_courier", 2, 1, "✉", 0xFFC8A070),
					new Talent("swift_delivery", 2, 2, "»", 0xFF6ACAD0),
					new Talent("lucky_find", 1, 3, "✦", 0xFFC868FF),
					new Talent("treasure_hunter", 1, 3, "◈", 0xFFFFD27A),
					new Talent("sharp_beak", 3, 1, "⚔", 0xFFB0B0C0),
					new Talent("dive", 2, 1, "↓", 0xFF7FD06A),
					new Talent("eye_peck", 2, 2, "◉", 0xFFE04848),
					new Talent("shadow_swarm", 1, 3, "☾", 0xFF8D6ACC),
					new Talent("raven_call", 1, 3, "♪", 0xFFC868FF),
					new Talent("tailwind", 3, 1, "≈", 0xFF9CC8F0),
					new Talent("feather_fall", 2, 1, "❦", 0xFFF0F0F0),
					new Talent("night_eye", 1, 2, "◐", 0xFFE0C040),
					new Talent("sky_lord", 2, 3, "♛", 0xFFC868FF),
					new Talent("night_wings", 1, 3, "☽", 0xFF8D6ACC),
					new Talent("raven_swarm", 1, 2, "⁂", 0xFF8D6ACC),
					new Talent("shadow_dive", 1, 2, "↯", 0xFFE04848),
					new Talent("terror_cry", 1, 2, "!", 0xFFE0B040),
					new Talent("mastery", 2, 3, "★", 0xFFFFD27A),
					new Talent("instinct", 1, 3, "☯", 0xFFFFFFFF))));

	private static final Map<String, Map<String, Integer>> INDEX = new HashMap<>();

	static {
		TREES.forEach((species, tree) -> {
			Map<String, Integer> map = new HashMap<>();

			for (int i = 0; i < tree.talents().size(); i++) {
				map.put(tree.talents().get(i).id(), i);
			}

			INDEX.put(species, map);
		});
	}

	private CompanionTalents() {
	}

	public static Tree tree(String species) {
		return TREES.getOrDefault(species, TREES.get("mantis"));
	}

	/** Index of a talent of the species, or -1. */
	public static int index(String species, String id) {
		Map<String, Integer> map = INDEX.get(species);
		Integer index = map == null ? null : map.get(id);
		return index == null ? -1 : index;
	}

	public static boolean isSpecial(int index) {
		return index >= SPECIAL && index < SPECIAL + 3;
	}

	/** Learned special attack (0-2) or -1. */
	public static int chosenSpecial(int[] ranks) {
		for (int i = 0; i < 3; i++) {
			if (ranks.length > SPECIAL + i && ranks[SPECIAL + i] > 0) {
				return i;
			}
		}

		return -1;
	}

	/** Talent points at this level. */
	public static int points(int level) {
		return Math.min(MAX_POINTS, level / LEVELS_PER_POINT);
	}

	public static int spent(int[] ranks) {
		int sum = 0;

		for (int rank : ranks) {
			sum += rank;
		}

		return sum;
	}

	/** Convert stored ranks to the current format (old saves: 3 x 4 talents). */
	public static int[] migrate(String species, int[] saved) {
		int[] ranks = new int[COUNT];

		if (saved.length == OLD_COUNT) {
			for (int i = 0; i < OLD_COUNT; i++) {
				ranks[(i / OLD_PER_BRANCH) * PER_BRANCH + i % OLD_PER_BRANCH] = saved[i];
			}
		} else {
			System.arraycopy(saved, 0, ranks, 0, Math.min(saved.length, COUNT));
		}

		Tree tree = tree(species);

		for (int i = 0; i < COUNT; i++) {
			ranks[i] = Math.clamp(ranks[i], 0, tree.get(i).max());
		}

		return ranks;
	}

	/** Why can this talent not be learned? null = it can. */
	public static String blocker(String species, int[] ranks, int index, int stage, int level) {
		if (index < 0 || index >= COUNT || ranks.length != COUNT) {
			return "invalid";
		}

		Talent talent = tree(species).get(index);

		if (ranks[index] >= talent.max()) {
			return "maxed";
		}

		if (stage < talent.minStage()) {
			return "stage";
		}

		if (isSpecial(index)) {
			if (chosenSpecial(ranks) >= 0) {
				return "special_taken";       // only one special attack
			}
		} else if (index == MASTERY) {
			if (chosenSpecial(ranks) < 0) {
				return "needs_special";
			}
		} else if (index % PER_BRANCH > 0 && ranks[index - 1] == 0) {
			return "previous";
		}

		if (spent(ranks) >= points(level)) {
			return "points";
		}

		return null;
	}
}
