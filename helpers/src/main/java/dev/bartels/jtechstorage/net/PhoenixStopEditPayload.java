package dev.bartels.jtechstorage.net;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import dev.bartels.jtechstorage.JTechStorage;

/** Client → Server: save name and rest duration of a phoenix stop. */
public record PhoenixStopEditPayload(BlockPos pos, String name, int rest) implements CustomPacketPayload {
	public static final Type<PhoenixStopEditPayload> TYPE = new Type<>(JTechStorage.id("phoenix_stop_edit"));
	public static final StreamCodec<RegistryFriendlyByteBuf, PhoenixStopEditPayload> STREAM_CODEC = StreamCodec.composite(
			BlockPos.STREAM_CODEC, PhoenixStopEditPayload::pos,
			ByteBufCodecs.stringUtf8(64), PhoenixStopEditPayload::name,
			ByteBufCodecs.VAR_INT, PhoenixStopEditPayload::rest,
			PhoenixStopEditPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
