package dev.bartels.jtechstorage.net;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import dev.bartels.jtechstorage.JTechStorage;

/** Client → Server: Aktion im Phönix-Horst. */
public record NestActionPayload(int containerId, Action action, String text, int value) implements CustomPacketPayload {
	public static final Type<NestActionPayload> TYPE = new Type<>(JTechStorage.id("nest_action"));

	public enum Action {
		/** Ziel = Eintrag value der zuletzt gesendeten Liste. */
		DESTINATION,
		SEND,
		DAILY,
		RENAME,
		/** Empfangs-Einstellung = value. */
		ACCEPT
	}

	public static final StreamCodec<RegistryFriendlyByteBuf, NestActionPayload> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, NestActionPayload::containerId,
			ByteBufCodecs.idMapper(i -> Action.values()[Math.clamp(i, 0, Action.values().length - 1)], Action::ordinal), NestActionPayload::action,
			ByteBufCodecs.stringUtf8(32), NestActionPayload::text,
			ByteBufCodecs.INT, NestActionPayload::value,
			NestActionPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
