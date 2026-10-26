package dev.bartels.jtechstorage.net;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import dev.bartels.jtechstorage.JTechStorage;

/** Client → Server: request the open terminal's statistics. */
public record TerminalStatsRequestPayload(int containerId) implements CustomPacketPayload {
	public static final Type<TerminalStatsRequestPayload> TYPE = new Type<>(JTechStorage.id("terminal_stats_request"));
	public static final StreamCodec<RegistryFriendlyByteBuf, TerminalStatsRequestPayload> STREAM_CODEC =
			ByteBufCodecs.VAR_INT.map(TerminalStatsRequestPayload::new, TerminalStatsRequestPayload::containerId).cast();

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
