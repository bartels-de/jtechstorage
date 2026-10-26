package dev.bartels.jtechstorage.net;

import java.util.List;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import dev.bartels.jtechstorage.JTechStorage;

/** Server → Client: state of the team for an open team terminal. */
public record TeamStatePayload(int containerId, State state) implements CustomPacketPayload {
	public static final Type<TeamStatePayload> TYPE = new Type<>(JTechStorage.id("team_state"));

	/** A member: name, role (ordinal), online? */
	public record MemberRow(String name, int role, boolean online) {
		public static final StreamCodec<RegistryFriendlyByteBuf, MemberRow> STREAM_CODEC = StreamCodec.composite(
				ByteBufCodecs.STRING_UTF8, MemberRow::name,
				ByteBufCodecs.VAR_INT, MemberRow::role,
				ByteBufCodecs.BOOL, MemberRow::online,
				MemberRow::new);
	}

	/**
	 * @param myRole     role of the viewer (ordinal)
	 * @param colorIndex index into {@code StorageTerminalBlockEntity.TEAM_COLORS}
	 * @param access     access mode (ordinal)
	 * @param publicRole role for strangers (ordinal)
	 * @param channels   bitmask of the active channels
	 */
	public record State(int myRole, String ownerName, boolean ownerOnline, String teamName, int colorIndex, int color,
			int access, int publicRole, int channels, List<MemberRow> members) {
		public static final State EMPTY = new State(0, "", false, "", 0, 0xFF55FF, 1, 0, 0, List.of());

		public static final StreamCodec<RegistryFriendlyByteBuf, State> STREAM_CODEC = StreamCodec.composite(
				ByteBufCodecs.VAR_INT, State::myRole,
				ByteBufCodecs.STRING_UTF8, State::ownerName,
				ByteBufCodecs.BOOL, State::ownerOnline,
				ByteBufCodecs.STRING_UTF8, State::teamName,
				ByteBufCodecs.VAR_INT, State::colorIndex,
				ByteBufCodecs.INT, State::color,
				ByteBufCodecs.VAR_INT, State::access,
				ByteBufCodecs.VAR_INT, State::publicRole,
				ByteBufCodecs.VAR_INT, State::channels,
				MemberRow.STREAM_CODEC.apply(ByteBufCodecs.list()), State::members,
				State::new);
	}

	public static final StreamCodec<RegistryFriendlyByteBuf, TeamStatePayload> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, TeamStatePayload::containerId,
			State.STREAM_CODEC, TeamStatePayload::state,
			TeamStatePayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
