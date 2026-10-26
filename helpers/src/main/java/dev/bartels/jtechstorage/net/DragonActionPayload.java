package dev.bartels.jtechstorage.net;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import dev.bartels.jtechstorage.JTechStorage;

/** Client → Server: Cheldrogan rider – 0 = tail sweep (left click), 1 = crystal breath (R key), 2 = shard rain (V key). */
public record DragonActionPayload(int action) implements CustomPacketPayload {
	public static final Type<DragonActionPayload> TYPE = new Type<>(JTechStorage.id("dragon_action"));
	public static final StreamCodec<RegistryFriendlyByteBuf, DragonActionPayload> STREAM_CODEC =
			ByteBufCodecs.VAR_INT.map(DragonActionPayload::new, DragonActionPayload::action).cast();

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
