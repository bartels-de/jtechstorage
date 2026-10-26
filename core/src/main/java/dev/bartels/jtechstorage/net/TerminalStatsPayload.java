package dev.bartels.jtechstorage.net;

import java.util.List;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import dev.bartels.jtechstorage.JTechStorage;
import dev.bartels.jtechstorage.network.NetworkStats;

/** Server → Client: history (state per game day), items at the start of the day and biggest changes since the start of the day. */
public record TerminalStatsPayload(int containerId, List<NetworkStats.Day> history, long todayStart,
		List<NetworkStats.Change> changes) implements CustomPacketPayload {
	public static final Type<TerminalStatsPayload> TYPE = new Type<>(JTechStorage.id("terminal_stats"));
	public static final StreamCodec<RegistryFriendlyByteBuf, TerminalStatsPayload> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, TerminalStatsPayload::containerId,
			NetworkStats.Day.STREAM_CODEC.apply(ByteBufCodecs.list(NetworkStats.DAYS + 1)), TerminalStatsPayload::history,
			ByteBufCodecs.VAR_LONG, TerminalStatsPayload::todayStart,
			NetworkStats.Change.STREAM_CODEC.apply(ByteBufCodecs.list(NetworkStats.CHANGES * 2)), TerminalStatsPayload::changes,
			TerminalStatsPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
