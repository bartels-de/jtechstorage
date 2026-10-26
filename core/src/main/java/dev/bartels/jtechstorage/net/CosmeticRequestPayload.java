package dev.bartels.jtechstorage.net;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import dev.bartels.jtechstorage.JTechStorage;

/** Client → Server (v0.34): 0 = request unlocks, 1 = trigger emote. */
public record CosmeticRequestPayload(int action) implements CustomPacketPayload {
	public static final int UNLOCKS = 0;
	public static final int EMOTE = 1;
	public static final Type<CosmeticRequestPayload> TYPE = new Type<>(JTechStorage.id("cosmetic_request"));

	public static final StreamCodec<RegistryFriendlyByteBuf, CosmeticRequestPayload> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, CosmeticRequestPayload::action,
			CosmeticRequestPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
