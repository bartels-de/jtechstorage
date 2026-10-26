package dev.bartels.jtechstorage.net;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import dev.bartels.jtechstorage.JTechStorage;

/** Client → Server: player in the front seat presses the jump key → Amethyst Phoenix departs early. */
public record PhoenixDepartPayload(int entityId) implements CustomPacketPayload {
	public static final Type<PhoenixDepartPayload> TYPE = new Type<>(JTechStorage.id("phoenix_depart"));
	public static final StreamCodec<RegistryFriendlyByteBuf, PhoenixDepartPayload> STREAM_CODEC =
			ByteBufCodecs.VAR_INT.map(PhoenixDepartPayload::new, PhoenixDepartPayload::entityId).cast();

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
