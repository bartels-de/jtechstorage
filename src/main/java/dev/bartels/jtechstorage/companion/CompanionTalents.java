package dev.bartels.jtechstorage.companion;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Talentbaum der Begleiter (v0.26): je Art drei Zweige mit je vier Talenten. Alle 5 Level gibt es einen Punkt (bis 20 bei
 * Level 100). Ein Talent braucht mindestens einen Punkt im Talent darüber und teils eine Stufe. Zurücksetzen kostet einen
 * Amethyst-Kern. Texte: {@code jtechstorage.talent.<id>} (Name) und {@code .desc} (Wirkung je Rang).
 */
public final class CompanionTalents {
	public static final int BRANCHES = 3;
	public static final int PER_BRANCH = 4;
	public static final int COUNT = BRANCHES * PER_BRANCH;
	public static final int LEVELS_PER_POINT = 5;
	public static final int MAX_POINTS = 20;

	/** @param symbol kurzes Zeichen für das Symbol im Menü, color Farbe des Symbols */
	public record Talent(String id, int max, int minStage, String symbol, int color) {
	}

	public record Tree(List<String> branches, List<Talent> talents) {
		public Talent get(int index) {
			return talents.get(index);
		}
	}

	private static final Map<String, Tree> TREES = Map.of(
			"mantis", new Tree(List.of("blade", "armor", "hunt"), List.of(
					new Talent("sharp_blades", 3, 1, "⚔", 0xFFB07CF0),
					new Talent("quick_strike", 2, 1, "»", 0xFF7FD06A),
					new Talent("precision", 2, 2, "✦", 0xFFE0B040),
					new Talent("whirl_scythe", 1, 3, "☼", 0xFFC868FF),
					new Talent("chitin", 3, 1, "◆", 0xFF8C8CA8),
					new Talent("crystal_plates", 2, 1, "▣", 0xFFB07CF0),
					new Talent("thorns", 2, 2, "✶", 0xFFC0603A),
					new Talent("unbreakable", 1, 3, "♥", 0xFFE04848),
					new Talent("swift_legs", 2, 1, "»", 0xFF6ACAD0),
					new Talent("hunt_instinct", 2, 1, "◎", 0xFFE0B040),
					new Talent("leap_attack", 1, 2, "↑", 0xFF7FD06A),
					new Talent("crystal_flight", 2, 3, "✧", 0xFFC868FF))),
			"shroom", new Tree(List.of("healing", "protection", "poison"), List.of(
					new Talent("strong_spores", 3, 1, "♥", 0xFFE06890),
					new Talent("fast_bloom", 2, 1, "»", 0xFF7FD06A),
					new Talent("wide_spores", 2, 2, "◎", 0xFFD4B4FF),
					new Talent("lifesaver", 1, 3, "✚", 0xFFFFE070),
					new Talent("tough_stalk", 3, 1, "▮", 0xFFC8A070),
					new Talent("spore_shield", 2, 1, "◆", 0xFFE0C040),
					new Talent("calm_aura", 2, 2, "☁", 0xFF9CC8F0),
					new Talent("purify", 1, 3, "✧", 0xFFFFFFFF),
					new Talent("toxic_spores", 3, 1, "☠", 0xFF7FD06A),
					new Talent("spore_burst", 2, 1, "✶", 0xFFB07CF0),
					new Talent("numbing_spores", 2, 2, "❄", 0xFF8CC8E0),
					new Talent("toxic_cloud", 1, 3, "☣", 0xFF5AC040))),
			"raven", new Tree(List.of("bag", "combat", "flight"), List.of(
					new Talent("keen_eye", 3, 1, "◎", 0xFFE0B040),
					new Talent("eager_courier", 2, 1, "✉", 0xFFC8A070),
					new Talent("swift_delivery", 2, 2, "»", 0xFF6ACAD0),
					new Talent("lucky_find", 1, 3, "✦", 0xFFC868FF),
					new Talent("sharp_beak", 3, 1, "⚔", 0xFFB0B0C0),
					new Talent("dive", 2, 1, "↓", 0xFF7FD06A),
					new Talent("eye_peck", 2, 2, "◉", 0xFFE04848),
					new Talent("shadow_swarm", 1, 3, "☾", 0xFF8D6ACC),
					new Talent("tailwind", 3, 1, "≈", 0xFF9CC8F0),
					new Talent("feather_fall", 2, 1, "❦", 0xFFF0F0F0),
					new Talent("night_eye", 1, 2, "◐", 0xFFE0C040),
					new Talent("sky_lord", 2, 3, "♛", 0xFFC868FF))));

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

	/** Index eines Talents der Art oder -1. */
	public static int index(String species, String id) {
		Map<String, Integer> map = INDEX.get(species);
		Integer index = map == null ? null : map.get(id);
		return index == null ? -1 : index;
	}

	/** Talentpunkte auf diesem Level. */
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

	/** Warum kann man dieses Talent nicht lernen? null = man kann. */
	public static String blocker(String species, int[] ranks, int index, int stage, int level) {
		if (index < 0 || index >= COUNT) {
			return "invalid";
		}

		Talent talent = tree(species).get(index);

		if (ranks[index] >= talent.max()) {
			return "maxed";
		}

		if (stage < talent.minStage()) {
			return "stage";
		}

		if (index % PER_BRANCH > 0 && ranks[index - 1] == 0) {
			return "previous";
		}

		if (spent(ranks) >= points(level)) {
			return "points";
		}

		return null;
	}
}
