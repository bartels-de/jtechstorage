package dev.bartels.jtechstorage.farm;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.GlobalPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import dev.bartels.jtechstorage.JTechStorage;

/** Active chunk loaders (v0.27.1): for the per-player limit, and so a chunk is only freed once no loader in it is active. */
public class ChunkLoaderData extends SavedData {
	private record Entry(GlobalPos pos, UUID owner) {
		static final Codec<Entry> CODEC = RecordCodecBuilder.create(i -> i.group(
				GlobalPos.CODEC.fieldOf("pos").forGetter(Entry::pos),
				UUIDUtil.CODEC.fieldOf("owner").forGetter(Entry::owner)
		).apply(i, Entry::new));
	}

	public static final Codec<ChunkLoaderData> CODEC = Entry.CODEC.listOf().fieldOf("loaders").xmap(ChunkLoaderData::new,
			data -> data.active.entrySet().stream().map(e -> new Entry(e.getKey(), e.getValue())).toList()).codec();
	public static final SavedDataType<ChunkLoaderData> TYPE = new SavedDataType<>(JTechStorage.id("chunk_loaders"), ChunkLoaderData::new, CODEC,
			DataFixTypes.SAVED_DATA_COMMAND_STORAGE);

	private final Map<GlobalPos, UUID> active = new HashMap<>();

	public ChunkLoaderData() {
	}

	private ChunkLoaderData(List<Entry> entries) {
		for (Entry entry : entries) {
			active.put(entry.pos(), entry.owner());
		}
	}

	public static ChunkLoaderData get(MinecraftServer server) {
		return server.getDataStorage().computeIfAbsent(TYPE);
	}

	public boolean isActive(GlobalPos pos) {
		return active.containsKey(pos);
	}

	public void setActive(GlobalPos pos, UUID owner, boolean on) {
		boolean changed = on ? !owner.equals(active.put(pos, owner)) : active.remove(pos) != null;

		if (changed) {
			setDirty();
		}
	}

	public int count(UUID owner) {
		int n = 0;

		for (UUID u : active.values()) {
			if (u.equals(owner)) {
				n++;
			}
		}

		return n;
	}

	/** Is another loader still active in the same chunk (same dimension)? */
	public boolean otherInChunk(GlobalPos pos) {
		int cx = pos.pos().getX() >> 4;
		int cz = pos.pos().getZ() >> 4;

		for (GlobalPos other : active.keySet()) {
			if (!other.equals(pos) && other.dimension().equals(pos.dimension()) && other.pos().getX() >> 4 == cx && other.pos().getZ() >> 4 == cz) {
				return true;
			}
		}

		return false;
	}
}
