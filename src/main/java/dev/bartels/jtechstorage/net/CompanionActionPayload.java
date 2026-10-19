package dev.bartels.jtechstorage.net;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import dev.bartels.jtechstorage.JTechStorage;

/**
 * Client → Server: Begleiter-Tasten und das Begleiter-Menü (v0.26). Aktionen: 0 = Pilz heilt jetzt, 1 = Pilz automatisch/Taste,
 * 2 = Folgen/Bleiben, 3 = Haltung setzen (arg), 4 = rufen, 5 = Talent lernen (arg = Index), 6 = Talente zurücksetzen,
 * 7 = Rabentasche öffnen, 8 = Rabe auf Erkundung schicken (v0.25.1), 9 = Spezialangriff (v0.25.3).
 */
public record CompanionActionPayload(int action, int arg) implements CustomPacketPayload {
	public static final int HEAL = 0;
	public static final int HEAL_MODE = 1;
	public static final int STAY = 2;
	public static final int STANCE = 3;
	public static final int CALL = 4;
	public static final int LEARN = 5;
	public static final int RESET = 6;
	public static final int BAG = 7;
	public static final int EXPLORE = 8;
	/** Spezialangriff (v0.25.3, Taste Standard B). */
	public static final int SPECIAL = 9;

	public static final Type<CompanionActionPayload> TYPE = new Type<>(JTechStorage.id("companion_action"));
	public static final StreamCodec<RegistryFriendlyByteBuf, CompanionActionPayload> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, CompanionActionPayload::action,
			ByteBufCodecs.VAR_INT, CompanionActionPayload::arg,
			CompanionActionPayload::new);

	public CompanionActionPayload(int action) {
		this(action, 0);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
