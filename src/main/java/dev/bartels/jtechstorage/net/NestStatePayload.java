package dev.bartels.jtechstorage.net;

import java.util.List;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import dev.bartels.jtechstorage.JTechStorage;

/** Server → Client: Zustand eines offenen Phönix-Horsts. */
public record NestStatePayload(int containerId, State state) implements CustomPacketPayload {
	public static final Type<NestStatePayload> TYPE = new Type<>(JTechStorage.id("nest_state"));

	/** Ein mögliches Ziel: Anzeigename, Team-Farbe, Flugzeit in Sekunden. */
	public record Destination(String label, int color, int seconds) {
		public static final StreamCodec<RegistryFriendlyByteBuf, Destination> STREAM_CODEC = StreamCodec.composite(
				ByteBufCodecs.STRING_UTF8, Destination::label,
				ByteBufCodecs.INT, Destination::color,
				ByteBufCodecs.VAR_INT, Destination::seconds,
				Destination::new);
	}

	/**
	 * @param phoenix 0 = kein Phönix, 1 = zu Hause, 2 = unterwegs, 3 = startet gerade
	 */
	public record State(String name, int accept, boolean canSend, boolean canSettings, List<Destination> destinations,
			int selected, boolean daily, int phoenix, int awaySeconds, int incoming) {
		public static final State EMPTY = new State("", 0, false, false, List.of(), -1, false, 0, 0, 0);

		public static final StreamCodec<RegistryFriendlyByteBuf, State> STREAM_CODEC = StreamCodec.composite(
				ByteBufCodecs.STRING_UTF8, State::name,
				ByteBufCodecs.VAR_INT, State::accept,
				ByteBufCodecs.BOOL, State::canSend,
				ByteBufCodecs.BOOL, State::canSettings,
				Destination.STREAM_CODEC.apply(ByteBufCodecs.list()), State::destinations,
				ByteBufCodecs.INT, State::selected,
				ByteBufCodecs.BOOL, State::daily,
				ByteBufCodecs.VAR_INT, State::phoenix,
				ByteBufCodecs.VAR_INT, State::awaySeconds,
				ByteBufCodecs.VAR_INT, State::incoming,
				State::new);
	}

	public static final StreamCodec<RegistryFriendlyByteBuf, NestStatePayload> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, NestStatePayload::containerId,
			State.STREAM_CODEC, NestStatePayload::state,
			NestStatePayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
