package dev.bartels.jtechstorage.companion;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.UUIDUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import dev.bartels.jtechstorage.JTechStorage;

/**
 * Begleiter-Register (v0.25): welcher Spieler welchen Begleiter hat (je Spieler höchstens einer) und welche Geoden schon
 * besucht wurden (jede Geode bekommt genau eine Chance auf einen Geodenhüter).
 */
public class CompanionData extends SavedData {
	public record Entry(UUID owner, UUID companion, String species) {
		static final Codec<Entry> CODEC = RecordCodecBuilder.create(i -> i.group(
				UUIDUtil.CODEC.fieldOf("owner").forGetter(Entry::owner),
				UUIDUtil.CODEC.fieldOf("companion").forGetter(Entry::companion),
				Codec.STRING.fieldOf("species").forGetter(Entry::species)
		).apply(i, Entry::new));
	}

	public static final Codec<CompanionData> CODEC = RecordCodecBuilder.create(i -> i.group(
			Entry.CODEC.listOf().fieldOf("companions").forGetter(data -> List.copyOf(data.byOwner.values())),
			Codec.LONG.listOf().fieldOf("geodes").forGetter(data -> List.copyOf(data.geodes))
	).apply(i, CompanionData::new));

	public static final SavedDataType<CompanionData> TYPE = new SavedDataType<>(JTechStorage.id("companions"),
			CompanionData::new, CODEC, DataFixTypes.SAVED_DATA_COMMAND_STORAGE);

	private final Map<UUID, Entry> byOwner = new HashMap<>();
	private final Set<Long> geodes = new HashSet<>();

	public CompanionData() {
	}

	private CompanionData(List<Entry> entries, List<Long> geodeList) {
		for (Entry entry : entries) {
			byOwner.put(entry.owner(), entry);
		}

		geodes.addAll(geodeList);
	}

	public static CompanionData get(MinecraftServer server) {
		return server.getDataStorage().computeIfAbsent(TYPE);
	}

	public @Nullable Entry of(UUID owner) {
		return byOwner.get(owner);
	}

	public boolean hasCompanion(UUID owner) {
		return byOwner.containsKey(owner);
	}

	public void set(UUID owner, UUID companion, String species) {
		byOwner.put(owner, new Entry(owner, companion, species));
		setDirty();
	}

	/** Nach einem Dimensionswechsel kann sich die Entity-ID ändern. */
	public void updateCompanion(UUID owner, UUID companion) {
		Entry entry = byOwner.get(owner);

		if (entry != null && !entry.companion().equals(companion)) {
			byOwner.put(owner, new Entry(owner, companion, entry.species()));
			setDirty();
		}
	}

	public void forgetCompanion(UUID companion) {
		List<UUID> owners = new ArrayList<>();

		for (Entry entry : byOwner.values()) {
			if (entry.companion().equals(companion)) {
				owners.add(entry.owner());
			}
		}

		if (!owners.isEmpty()) {
			owners.forEach(byOwner::remove);
			setDirty();
		}
	}

	public void forgetOwner(UUID owner) {
		if (byOwner.remove(owner) != null) {
			setDirty();
		}
	}

	/** Markiert eine Geode als besucht; true = sie war noch neu. */
	public boolean visitGeode(long key) {
		if (geodes.add(key)) {
			setDirty();
			return true;
		}

		return false;
	}

	public boolean isGeodeVisited(long key) {
		return geodes.contains(key);
	}
}
