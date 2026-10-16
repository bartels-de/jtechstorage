package dev.bartels.jtechstorage.net;

import io.netty.buffer.ByteBuf;

import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import dev.bartels.jtechstorage.JTechStorage;

/** Client → Server: Spieler hat (mit Doppelsprung-Stiefeln) in der Luft erneut gesprungen. */
public record DoubleJumpPayload() implements CustomPacketPayload {
	public static final DoubleJumpPayload INSTANCE = new DoubleJumpPayload();
	public static final Type<DoubleJumpPayload> TYPE = new Type<>(JTechStorage.id("double_jump"));
	public static final StreamCodec<ByteBuf, DoubleJumpPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
