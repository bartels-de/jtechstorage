package dev.bartels.jtechstorage.net;

import java.util.List;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import dev.bartels.jtechstorage.JTechStorage;
import dev.bartels.jtechstorage.network.StorageNetwork;

/** Server → Client: current contents of the network for an open terminal. */
public record TerminalContentsPayload(int containerId, List<StorageNetwork.Entry> entries, NetworkInfo info, AccessInfo access)
		implements CustomPacketPayload {
	public static final Type<TerminalContentsPayload> TYPE = new Type<>(JTechStorage.id("terminal_contents"));

	public static final StreamCodec<RegistryFriendlyByteBuf, TerminalContentsPayload> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, TerminalContentsPayload::containerId,
			StorageNetwork.Entry.STREAM_CODEC.apply(ByteBufCodecs.list()), TerminalContentsPayload::entries,
			NetworkInfo.STREAM_CODEC, TerminalContentsPayload::info,
			AccessInfo.STREAM_CODEC, TerminalContentsPayload::access,
			TerminalContentsPayload::new);

	/** Maximum packet size (many different items with components can get large). */
	public static final int MAX_SIZE = 16 * 1024 * 1024;

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	/** Metrics for the info display. Resonance in percent (coarse, so it isn't resent every second). */
	public record NetworkInfo(int range, int linked, int capacity, int unavailable, int overCapacity,
			int usedSlots, int totalSlots, int sortRules, int resonancePercent) {
		public static final NetworkInfo EMPTY = new NetworkInfo(0, 0, 0, 0, 0, 0, 0, 0, 0);

		public static final StreamCodec<RegistryFriendlyByteBuf, NetworkInfo> STREAM_CODEC = StreamCodec.composite(
				ByteBufCodecs.VAR_INT, NetworkInfo::range,
				ByteBufCodecs.VAR_INT, NetworkInfo::linked,
				ByteBufCodecs.VAR_INT, NetworkInfo::capacity,
				ByteBufCodecs.VAR_INT, NetworkInfo::unavailable,
				ByteBufCodecs.VAR_INT, NetworkInfo::overCapacity,
				ByteBufCodecs.VAR_INT, NetworkInfo::usedSlots,
				ByteBufCodecs.VAR_INT, NetworkInfo::totalSlots,
				ByteBufCodecs.VAR_INT, NetworkInfo::sortRules,
				ByteBufCodecs.VAR_INT, NetworkInfo::resonancePercent,
				NetworkInfo::new);
	}

	/**
	 * Access info: role of the viewer (ordinal of {@code StorageTerminalBlockEntity.Role}), owner, members,
	 * access mode, team/network name and team color (RGB).
	 */
	public record AccessInfo(int role, String ownerName, List<String> members, int mode, String networkName, int color) {
		public static final AccessInfo EMPTY = new AccessInfo(0, "", List.of(), 1, "", 0xFF55FF);

		public boolean owner() {
			return role == 4;
		}

		public static final StreamCodec<RegistryFriendlyByteBuf, AccessInfo> STREAM_CODEC = StreamCodec.composite(
				ByteBufCodecs.VAR_INT, AccessInfo::role,
				ByteBufCodecs.STRING_UTF8, AccessInfo::ownerName,
				ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), AccessInfo::members,
				ByteBufCodecs.VAR_INT, AccessInfo::mode,
				ByteBufCodecs.STRING_UTF8, AccessInfo::networkName,
				ByteBufCodecs.INT, AccessInfo::color,
				AccessInfo::new);
	}
}
