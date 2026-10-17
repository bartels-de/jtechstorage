package dev.bartels.jtechstorage.net;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import dev.bartels.jtechstorage.JTechStorage;

/** Server → Client beim Betreten: Konfiguration des Servers (für Tooltips und Handbuch). */
public record ConfigSyncPayload(String json) implements CustomPacketPayload {
	public static final Type<ConfigSyncPayload> TYPE = new Type<>(JTechStorage.id("config_sync"));
	public static final StreamCodec<RegistryFriendlyByteBuf, ConfigSyncPayload> STREAM_CODEC =
			ByteBufCodecs.stringUtf8(32767).map(ConfigSyncPayload::new, ConfigSyncPayload::json).cast();

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
