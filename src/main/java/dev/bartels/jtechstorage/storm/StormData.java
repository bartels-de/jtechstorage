package dev.bartels.jtechstorage.storm;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.UUIDUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import dev.bartels.jtechstorage.JTechStorage;

/** Natürliche Resonanz-Stürme (v0.27): je Spieler der Amethyst-Stand und die Spielzeit beim letzten Sturm. */
public class StormData extends SavedData {
	public record Entry(UUID player, int amethyst, long time) {
		static final Codec<Entry> CODEC = RecordCodecBuilder.create(i -> i.group(
				UUIDUtil.CODEC.fieldOf("player").forGetter(Entry::player),
				Codec.INT.fieldOf("amethyst").forGetter(Entry::amethyst),
				Codec.LONG.fieldOf("time").forGetter(Entry::time)
		).apply(i, Entry::new));
	}

	public static final Codec<StormData> CODEC = RecordCodecBuilder.create(i -> i.group(
			Entry.CODEC.listOf().fieldOf("players").forGetter(data -> List.copyOf(data.byPlayer.values()))
	).apply(i, StormData::new));

	public static final SavedDataType<StormData> TYPE = new SavedDataType<>(JTechStorage.id("storms"), StormData::new, CODEC,
			DataFixTypes.SAVED_DATA_COMMAND_STORAGE);

	private final Map<UUID, Entry> byPlayer = new HashMap<>();

	public StormData() {
	}

	private StormData(List<Entry> entries) {
		for (Entry entry : entries) {
			byPlayer.put(entry.player(), entry);
		}
	}

	public static StormData get(MinecraftServer server) {
		return server.getDataStorage().computeIfAbsent(TYPE);
	}

	/** Letzter Sturm des Spielers (Amethyst 0, Zeit lange her, wenn es noch keinen gab). */
	public Entry of(UUID player) {
		return byPlayer.getOrDefault(player, new Entry(player, 0, Long.MIN_VALUE / 2));
	}

	public void record(UUID player, int amethyst, long time) {
		byPlayer.put(player, new Entry(player, amethyst, time));
		setDirty();
	}
}
