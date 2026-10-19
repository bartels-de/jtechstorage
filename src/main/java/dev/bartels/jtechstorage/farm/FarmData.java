package dev.bartels.jtechstorage.farm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.GlobalPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import dev.bartels.jtechstorage.JTechStorage;

/**
 * Übersicht aller Farm-Monitore (v0.27.0) für die „Farmen“-Liste im Terminal – auch, wenn die Farm gerade nicht geladen
 * ist (dann mit dem zuletzt bekannten Stand).
 */
public class FarmData extends SavedData {
	public record Entry(GlobalPos monitor, String name, Optional<GlobalPos> terminal, int perHour, int status, String topItem, long updated) {
		static final Codec<Entry> CODEC = RecordCodecBuilder.create(i -> i.group(
				GlobalPos.CODEC.fieldOf("monitor").forGetter(Entry::monitor),
				Codec.STRING.fieldOf("name").forGetter(Entry::name),
				GlobalPos.CODEC.optionalFieldOf("terminal").forGetter(Entry::terminal),
				Codec.INT.fieldOf("per_hour").forGetter(Entry::perHour),
				Codec.INT.fieldOf("status").forGetter(Entry::status),
				Codec.STRING.fieldOf("top").forGetter(Entry::topItem),
				Codec.LONG.fieldOf("updated").forGetter(Entry::updated)
		).apply(i, Entry::new));
	}

	public static final Codec<FarmData> CODEC = Entry.CODEC.listOf().fieldOf("farms").xmap(FarmData::new,
			data -> List.copyOf(data.farms.values())).codec();
	public static final SavedDataType<FarmData> TYPE = new SavedDataType<>(JTechStorage.id("farms"), FarmData::new, CODEC,
			DataFixTypes.SAVED_DATA_COMMAND_STORAGE);

	private final Map<GlobalPos, Entry> farms = new HashMap<>();

	public FarmData() {
	}

	private FarmData(List<Entry> entries) {
		for (Entry entry : entries) {
			farms.put(entry.monitor(), entry);
		}
	}

	public static FarmData get(MinecraftServer server) {
		return server.getDataStorage().computeIfAbsent(TYPE);
	}

	public void put(Entry entry) {
		Entry old = farms.put(entry.monitor(), entry);

		if (!entry.equals(old)) {
			setDirty();
		}
	}

	public void remove(GlobalPos monitor) {
		if (farms.remove(monitor) != null) {
			setDirty();
		}
	}

	/** Alle Farmen, die mit diesem Terminal verbunden sind (nach Name sortiert). */
	public List<Entry> forTerminal(GlobalPos terminal) {
		List<Entry> list = new ArrayList<>();

		for (Entry entry : farms.values()) {
			if (entry.terminal().isPresent() && entry.terminal().get().equals(terminal)) {
				list.add(entry);
			}
		}

		list.sort((a, b) -> a.name().compareToIgnoreCase(b.name()));
		return list;
	}
}
