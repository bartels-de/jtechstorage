package dev.bartels.jtechstorage.net;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import dev.bartels.jtechstorage.JTechStorage;

/** Client → Server: eine Änderung im Team-Terminal. */
public record TeamActionPayload(int containerId, Action action, String text, int value) implements CustomPacketPayload {
	public static final Type<TeamActionPayload> TYPE = new Type<>(JTechStorage.id("team_action"));

	public enum Action {
		/** Spieler (text) ins Team holen. */
		ADD,
		/** Mitglied (text) entfernen. */
		REMOVE,
		/** Rolle von Mitglied (text) auf value setzen. */
		SET_ROLE,
		/** Team-Name = text. */
		RENAME,
		/** Team-Farbe = TEAM_COLORS[value]. */
		COLOR,
		/** Zugriffsmodus = value. */
		ACCESS,
		/** Rolle für Fremde = value. */
		PUBLIC_ROLE,
		/** Kanal value an/aus. */
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
