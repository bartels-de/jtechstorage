package dev.bartels.jtechstorage.net;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import dev.bartels.jtechstorage.JTechStorage;

/** Client → Server: change an order in Chel's menu (set from the cursor, change amount, delete). */
public record ChelOrderActionPayload(int containerId, int slot, int action, int amount) implements CustomPacketPayload {
	public static final Type<ChelOrderActionPayload> TYPE = new Type<>(JTechStorage.id("chel_order_action"));
	public static final int SET = 0;
	public static final int ADJUST = 1;
	public static final int CLEAR = 2;

	public static final StreamCodec<RegistryFriendlyByteBuf, ChelOrderActionPayload> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, ChelOrderActionPayload::containerId,
			ByteBufCodecs.VAR_INT, ChelOrderActionPayload::slot,
			ByteBufCodecs.VAR_INT, ChelOrderActionPayload::action,
			ByteBufCodecs.VAR_INT, ChelOrderActionPayload::amount,
			ChelOrderActionPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
