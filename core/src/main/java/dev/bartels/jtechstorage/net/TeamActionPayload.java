package dev.bartels.jtechstorage.net;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import dev.bartels.jtechstorage.JTechStorage;

/** Client → Server: a change in the team terminal. */
public record TeamActionPayload(int containerId, Action action, String text, int value) implements CustomPacketPayload {
	public static final Type<TeamActionPayload> TYPE = new Type<>(JTechStorage.id("team_action"));

	public enum Action {
		/** Add player (text) to the team. */
		ADD,
		/** Remove member (text). */
		REMOVE,
		/** Set role of member (text) to value. */
		SET_ROLE,
		/** Team name = text. */
		RENAME,
		/** Team color = TEAM_COLORS[value]. */
		COLOR,
		/** Access mode = value. */
		ACCESS,
		/** Role for strangers = value. */
		PUBLIC_ROLE,
		/** Toggle channel value on/off. */
		CHANNEL
	}

	public static final StreamCodec<RegistryFriendlyByteBuf, TeamActionPayload> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, TeamActionPayload::containerId,
			ByteBufCodecs.idMapper(i -> Action.values()[Math.clamp(i, 0, Action.values().length - 1)], Action::ordinal), TeamActionPayload::action,
			ByteBufCodecs.stringUtf8(32), TeamActionPayload::text,
			ByteBufCodecs.VAR_INT, TeamActionPayload::value,
			TeamActionPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
