package dev.bartels.jtechstorage.net;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import dev.bartels.jtechstorage.JTechStorage;

/** Change a setting on a storage screen or banner (v0.36): keys see {@code PanelActions}. */
public record PanelSettingPayload(BlockPos pos, int key, int value) implements CustomPacketPayload {
	public static final Type<PanelSettingPayload> TYPE = new Type<>(JTechStorage.id("panel_setting"));

	public static final StreamCodec<RegistryFriendlyByteBuf, PanelSettingPayload> STREAM_CODEC = StreamCodec.composite(
			BlockPos.STREAM_CODEC, PanelSettingPayload::pos, ByteBufCodecs.VAR_INT, PanelSettingPayload::key, ByteBufCodecs.VAR_INT,
			PanelSettingPayload::value, PanelSettingPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
