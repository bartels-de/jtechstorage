package dev.bartels.jtechstorage.mail;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import org.jspecify.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import dev.bartels.jtechstorage.ModConfig;
import dev.bartels.jtechstorage.JTechStorage;
import dev.bartels.jtechstorage.block.PhoenixNestBlockEntity;

/**
 * Phoenix post: worldwide address book of all phoenix nests and all packages currently in transit.
 *
 * <p>A package “flies” independently of loaded chunks: it has an arrival time. If the destination nest is loaded then,
 * it is delivered (a messenger phoenix lands there); otherwise the package waits until the nest is loaded again.
 * Rejected packages or packages for vanished nests fly back. Nothing gets lost.
 */
public class PhoenixPost extends SavedData {
	/** Who may send to a nest? */
	public enum Accept {
		ALL, TEAM, NOBODY;

		public String translationKey() {
			return "jtechstorage.nest.accept." + name().toLowerCase(Locale.ROOT);
		}

		public static Accept byId(int id) {
			return values()[Math.floorMod(id, values().length)];
		}
	}

	/** Address book entry. {@code allowed} = owner + team (for “team only”). */
	public record NestInfo(String name, String team, int color, int accept, List<UUID> allowed) {
		public static final Codec<NestInfo> CODEC = RecordCodecBuilder.create(i -> i.group(
				Codec.STRING.fieldOf("name").forGetter(NestInfo::name),
				Codec.STRING.fieldOf("team").forGetter(NestInfo::team),
				Codec.INT.fieldOf("color").forGetter(NestInfo::color),
				Codec.INT.fieldOf("accept").forGetter(NestInfo::accept),
				UUIDUtil.CODEC.listOf().fieldOf("allowed").forGetter(NestInfo::allowed)
		).apply(i, NestInfo::new));

		public boolean accepts(UUID sender) {
			return switch (Accept.byId(accept)) {
				case ALL -> true;
				case TEAM -> allowed.contains(sender);
				case NOBODY -> false;
			};
		}

		/** Display name: “Team · Nest”. */
		public String label() {
			return name.isEmpty() ? team : team + " · " + name;
		}
	}

	private record NestEntry(GlobalPos pos, NestInfo info) {
		static final Codec<NestEntry> CODEC = RecordCodecBuilder.create(i -> i.group(
				GlobalPos.CODEC.fieldOf("pos").forGetter(NestEntry::pos),
				NestInfo.CODEC.fieldOf("info").forGetter(NestEntry::info)
		).apply(i, NestEntry::new));
	}

	/** A package in transit. */
	public record Parcel(UUID id, GlobalPos from, GlobalPos to, UUID sender, String senderName, String fromLabel,
			List<ItemStack> items, long arrival, boolean returning, boolean waiting) {
		public static final Codec<Parcel> CODEC = RecordCodecBuilder.create(i -> i.group(
				UUIDUtil.CODEC.fieldOf("id").forGetter(Parcel::id),
				GlobalPos.CODEC.fieldOf("from").forGetter(Parcel::from),
				GlobalPos.CODEC.fieldOf("to").forGetter(Parcel::to),
				UUIDUtil.CODEC.fieldOf("sender").forGetter(Parcel::sender),
				Codec.STRING.fieldOf("sender_name").forGetter(Parcel::senderName),
				Codec.STRING.fieldOf("from_label").forGetter(Parcel::fromLabel),
				ItemStack.CODEC.listOf().fieldOf("items").forGetter(Parcel::items),
				Codec.LONG.fieldOf("arrival").forGetter(Parcel::arrival),
				Codec.BOOL.fieldOf("returning").forGetter(Parcel::returning),
				Codec.BOOL.fieldOf("waiting").forGetter(Parcel::waiting)
		).apply(i, Parcel::new));

		Parcel waitingNow() {
			return new Parcel(id, from, to, sender, senderName, fromLabel, items, arrival, returning, true);
		}

		Parcel returnTrip(long now) {
			return new Parcel(id, to, from, sender, senderName, fromLabel, items, now + flightTicks(to, from), true, false);
		}
	}

	public static final Codec<PhoenixPost> CODEC = RecordCodecBuilder.create(i -> i.group(
			NestEntry.CODEC.listOf().fieldOf("nests").forGetter(PhoenixPost::nestEntries),
			Parcel.CODEC.listOf().fieldOf("parcels").forGetter(post -> post.parcels)
	).apply(i, PhoenixPost::new));

	public static final SavedDataType<PhoenixPost> TYPE = new SavedDataType<>(JTechStorage.id("phoenix_post"),
			PhoenixPost::new, CODEC, DataFixTypes.SAVED_DATA_COMMAND_STORAGE);

	/** Flight speed: blocks per second. */
	public static final double BLOCKS_PER_SECOND = ModConfig.get().helpers.phoenixMailBlocksPerSecond;
	public static final int MIN_FLIGHT = 5 * 20;
	public static final int MAX_FLIGHT = 4 * 60 * 20;
	public static final int OTHER_DIMENSION_FLIGHT = 60 * 20;

	private final Map<GlobalPos, NestInfo> nests = new LinkedHashMap<>();
	private final List<Parcel> parcels = new ArrayList<>();

	public PhoenixPost() {
	}

	private PhoenixPost(List<NestEntry> nestList, List<Parcel> parcelList) {
		for (NestEntry entry : nestList) {
			nests.put(entry.pos(), entry.info());
		}

		parcels.addAll(parcelList);
	}

	private List<NestEntry> nestEntries() {
		return nests.entrySet().stream().map(e -> new NestEntry(e.getKey(), e.getValue())).toList();
	}

	public static PhoenixPost get(MinecraftServer server) {
		return server.getDataStorage().computeIfAbsent(TYPE);
	}

	/** Flight time in ticks between two nests. */
	public static int flightTicks(GlobalPos from, GlobalPos to) {
		if (!from.dimension().equals(to.dimension())) {
			return OTHER_DIMENSION_FLIGHT;
		}

		double distance = Math.sqrt(from.pos().distSqr(to.pos()));
		return (int) Math.clamp(distance / BLOCKS_PER_SECOND * 20.0, MIN_FLIGHT, MAX_FLIGHT);
	}

	// ------------------------------------------------------------------
	// Address book
	// ------------------------------------------------------------------

	public void putNest(GlobalPos pos, NestInfo info) {
		if (!info.equals(nests.get(pos))) {
			nests.put(pos, info);
			setDirty();
		}
	}

	public void removeNest(GlobalPos pos) {
		if (nests.remove(pos) != null) {
			setDirty();
		}
	}

	public @Nullable NestInfo nest(GlobalPos pos) {
		return nests.get(pos);
	}

	/** All nests {@code sender} may send to (excluding their own). */
	public List<Map.Entry<GlobalPos, NestInfo>> destinationsFor(UUID sender, GlobalPos self) {
		List<Map.Entry<GlobalPos, NestInfo>> result = new ArrayList<>();

		for (Map.Entry<GlobalPos, NestInfo> entry : nests.entrySet()) {
			if (!entry.getKey().equals(self) && entry.getValue().accepts(sender)) {
				result.add(Map.entry(entry.getKey(), entry.getValue()));
			}
		}

		result.sort(Comparator.comparing(e -> e.getValue().label().toLowerCase(Locale.ROOT)));
		return result;
	}

	// ------------------------------------------------------------------
	// Packages
	// ------------------------------------------------------------------

	/** Sends a package off. @return flight time in ticks */
	public int send(MinecraftServer server, GlobalPos from, GlobalPos to, UUID sender, String senderName, String fromLabel,
			List<ItemStack> items) {
		int flight = flightTicks(from, to);
		parcels.add(new Parcel(UUID.randomUUID(), from, to, sender, senderName, fromLabel,
				List.copyOf(items), server.overworld().getGameTime() + flight, false, false));
		setDirty();
		return flight;
	}

	/** Packages currently on their way to this nest or waiting there. */
	public long incomingTo(GlobalPos pos) {
		return parcels.stream().filter(p -> p.to().equals(pos)).count();
	}

	/** Every second: deliver due packages. */
	public void tick(MinecraftServer server) {
		if (parcels.isEmpty()) {
			return;
		}

		long now = server.overworld().getGameTime();
		List<Parcel> snapshot = List.copyOf(parcels);

		for (Parcel parcel : snapshot) {
			if (parcel.arrival() > now) {
				continue;
			}

			NestInfo target = nests.get(parcel.to());

			if (target == null) {
				// destination nest no longer exists
				if (!parcel.returning()) {
					replace(parcel, parcel.returnTrip(now));
					notify(server, parcel.sender(), Component.translatable("jtechstorage.mail.nest_gone").withStyle(ChatFormatting.GOLD));
				}

				continue;   // return flight to a vanished nest: wait until it stands again
			}

			ServerLevel level = server.getLevel(parcel.to().dimension());

			if (level == null || !level.isLoaded(parcel.to().pos())
					|| !(level.getBlockEntity(parcel.to().pos()) instanceof PhoenixNestBlockEntity nest)) {
				if (!parcel.waiting()) {
					replace(parcel, parcel.waitingNow());
				}

				continue;
			}

			if (!parcel.returning() && !target.accepts(parcel.sender())) {
				replace(parcel, parcel.returnTrip(now));
				notify(server, parcel.sender(), Component.translatable("jtechstorage.mail.rejected", target.label())
						.withStyle(ChatFormatting.GOLD));
				continue;
			}

			parcels.remove(parcel);
			setDirty();
			nest.receive(parcel);

			if (!parcel.returning()) {
				notify(server, parcel.sender(), Component.translatable("jtechstorage.mail.delivered", target.label(),
						summary(parcel.items())).withStyle(ChatFormatting.GOLD));
			}
		}
	}

	private void replace(Parcel old, Parcel updated) {
		int index = parcels.indexOf(old);

		if (index >= 0) {
			parcels.set(index, updated);
			setDirty();
		}
	}

	private static void notify(MinecraftServer server, UUID player, Component message) {
		ServerPlayer online = server.getPlayerList().getPlayer(player);

		if (online != null) {
			online.sendSystemMessage(Component.literal("🔥 ").withStyle(ChatFormatting.GOLD).append(message));
		}
	}

	/** “64× bread, 32× iron ingot, …” */
	public static MutableComponent summary(List<ItemStack> items) {
		MutableComponent text = Component.empty();
		int shown = 0;

		for (ItemStack stack : items) {
			if (shown == 3) {
				text.append(Component.literal(", …"));
				break;
			}

			if (shown > 0) {
				text.append(Component.literal(", "));
			}

			text.append(Component.literal(stack.getCount() + "× ")).append(stack.getHoverName());
			shown++;
		}

		return text;
	}
}
