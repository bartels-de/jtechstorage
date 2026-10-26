package dev.bartels.jtechstorage.network;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Storage statistics of a terminal: once per game day (at the day change, while the terminal is loaded) a
 * snapshot is saved – total items, used and free slots, plus the count per item for "more/less today".
 */
public final class NetworkStats {
	public static final int DAYS = 14;
	public static final int CHANGES = 5;

	/** State at the start of a game day. */
	public record Day(long day, long items, int used, int total) {
		public static final Codec<Day> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.LONG.fieldOf("day").forGetter(Day::day),
				Codec.LONG.fieldOf("items").forGetter(Day::items),
				Codec.INT.fieldOf("used").forGetter(Day::used),
				Codec.INT.fieldOf("total").forGetter(Day::total)
		).apply(instance, Day::new));
		public static final StreamCodec<RegistryFriendlyByteBuf, Day> STREAM_CODEC = StreamCodec.composite(
				ByteBufCodecs.VAR_LONG, Day::day,
				ByteBufCodecs.VAR_LONG, Day::items,
				ByteBufCodecs.VAR_INT, Day::used,
				ByteBufCodecs.VAR_INT, Day::total,
				Day::new);
	}

	/** Change of an item since the start of the day. */
	public record Change(Item item, long delta) {
		public static final StreamCodec<RegistryFriendlyByteBuf, Change> STREAM_CODEC = StreamCodec.composite(
				ByteBufCodecs.registry(Registries.ITEM), Change::item,
				ByteBufCodecs.VAR_LONG, Change::delta,
				Change::new);
	}

	private static final Codec<Map<Item, Long>> COUNTS_CODEC = Codec.unboundedMap(BuiltInRegistries.ITEM.byNameCodec(), Codec.LONG);

	private final List<Day> history = new ArrayList<>();
	private final Map<Item, Long> dayStart = new HashMap<>();
	private long currentDay = Long.MIN_VALUE;

	public long currentDay() {
		return currentDay;
	}

	public List<Day> history() {
		return List.copyOf(history);
	}

	/** New game day? Then take a snapshot. */
	public boolean needsSnapshot(long day) {
		return day != currentDay;
	}

	public void snapshot(long day, StorageNetwork network) {
		Map<Item, Long> counts = counts(network);
		long items = counts.values().stream().mapToLong(Long::longValue).sum();
		currentDay = day;
		dayStart.clear();
		dayStart.putAll(counts);
		history.removeIf(entry -> entry.day() >= day);
		history.add(new Day(day, items, network.usedSlots(), network.totalSlots()));

		while (history.size() > DAYS) {
			history.removeFirst();
		}
	}

	private static Map<Item, Long> counts(StorageNetwork network) {
		Map<Item, Long> counts = new HashMap<>();

		for (StorageNetwork.Entry entry : network.aggregate()) {
			counts.merge(entry.prototype().getItem(), entry.total(), Long::sum);
		}

		return counts;
	}

	/** Biggest gains (positive) and losses (negative) since the start of the day. */
	public List<Change> changes(StorageNetwork network) {
		Map<Item, Long> now = counts(network);
		Map<Item, Long> delta = new HashMap<>();
		now.forEach((item, count) -> delta.put(item, count - dayStart.getOrDefault(item, 0L)));
		dayStart.forEach((item, count) -> delta.putIfAbsent(item, -count));

		List<Change> all = new ArrayList<>();
		delta.forEach((item, value) -> {
			if (value != 0) {
				all.add(new Change(item, value));
			}
		});

		List<Change> result = new ArrayList<>();
		all.stream().filter(c -> c.delta() > 0).sorted(Comparator.comparingLong(Change::delta).reversed()).limit(CHANGES).forEach(result::add);
		all.stream().filter(c -> c.delta() < 0).sorted(Comparator.comparingLong(Change::delta)).limit(CHANGES).forEach(result::add);
		return result;
	}

	/** Total items at the start of the current day (-1 = unknown). */
	public long todayStart() {
		return history.isEmpty() || history.getLast().day() != currentDay ? -1 : history.getLast().items();
	}

	public void save(ValueOutput output) {
		if (currentDay == Long.MIN_VALUE) {
			return;
		}

		output.putLong("StatsDay", currentDay);
		output.store("StatsHistory", Day.CODEC.listOf(), List.copyOf(history));
		output.store("StatsItems", COUNTS_CODEC, Map.copyOf(dayStart));
	}

	public void load(ValueInput input) {
		currentDay = input.getLongOr("StatsDay", Long.MIN_VALUE);
		history.clear();
		history.addAll(input.read("StatsHistory", Day.CODEC.listOf()).orElse(List.of()));
		dayStart.clear();
		dayStart.putAll(input.read("StatsItems", COUNTS_CODEC).orElse(Map.of()));
	}
}
