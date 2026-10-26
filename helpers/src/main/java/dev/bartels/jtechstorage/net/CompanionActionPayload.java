package dev.bartels.jtechstorage.net;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import dev.bartels.jtechstorage.JTechStorage;

/**
 * Client → Server: companion keys and the companion menu (v0.26). Actions: 0 = mushroom heals now, 1 = mushroom automatic/key,
 * 2 = follow/stay, 3 = set stance (arg), 4 = call, 5 = learn talent (arg = index), 6 = reset talents,
 * 7 = open raven bag, 8 = send raven scouting (v0.25.1), 9 = special attack (v0.25.3), 10 = appearance
 * (arg = field << 8 | value), 11 = glow color (arg = RGB, -1 = default) (v0.25.4).
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
	/** Special attack (v0.25.3, default key B). */
	public static final int SPECIAL = 9;
	public static final int LOOK = 10;
	public static final int GLOW = 11;
	/** Bond (v0.26.0): petting, play a gesture (arg = 1–3). */
	public static final int PET = 12;
	public static final int GESTURE = 13;

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
