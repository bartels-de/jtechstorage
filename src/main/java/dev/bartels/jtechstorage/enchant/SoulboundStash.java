package dev.bartels.jtechstorage.enchant;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.UUIDUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Prediction;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import dev.bartels.jtechstorage.JTechStorage;

/** Seelenband (v0.42): Gegenstände, die ein Spieler beim Tod behält, bis er wieder da ist (übersteht auch Neustarts). */
public class SoulboundStash extends SavedData {
	private record Entry(UUID player, List<ItemStack> items) {
		static final Codec<Entry> CODEC = RecordCodecBuilder.create(i -> i.group(
				UUIDUtil.CODEC.fieldOf("player").forGetter(Entry::player),
				ItemStack.CODEC.listOf().fieldOf("items").forGetter(Entry::items)
		).apply(i, Entry::new));
	}

	public static final Codec<SoulboundStash> CODEC = Entry.CODEC.listOf().xmap(SoulboundStash::new,
			stash -> stash.items.entrySet().stream().map(e -> new Entry(e.getKey(), e.getValue())).toList());

	public static final SavedDataType<SoulboundStash> TYPE = new SavedDataType<>(JTechStorage.id("soulbound"), SoulboundStash::new, CODEC,
			DataFixTypes.SAVED_DATA_COMMAND_STORAGE);

	private final Map<UUID, List<ItemStack>> items = new HashMap<>();

	public SoulboundStash() {
	}

	private SoulboundStash(List<Entry> entries) {
		for (Entry entry : entries) {
			items.put(entry.player(), new ArrayList<>(entry.items()));
		}
	}

	public static SoulboundStash get(MinecraftServer server) {
		return server.getDataStorage().computeIfAbsent(TYPE);
	}

	public void put(UUID player, List<ItemStack> kept) {
		items.computeIfAbsent(player, p -> new ArrayList<>()).addAll(kept);
		setDirty();
	}

	/** Gibt aufbewahrte Gegenstände zurück (volles Inventar: fallen vor die Füße). */
	public static void giveBack(ServerPlayer player) {
		SoulboundStash stash = get(player.level().getServer());
		List<ItemStack> kept = stash.items.remove(player.getUUID());

		if (kept == null) {
			return;
		}

		stash.setDirty();

		for (ItemStack stack : kept) {
			player.getInventory().placeItemBackInInventory(stack, Prediction.SERVER_ONLY);
		}
	}
}
