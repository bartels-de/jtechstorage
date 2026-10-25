package dev.bartels.jtechstorage.net;

import java.util.List;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import dev.bartels.jtechstorage.JTechStorage;

/** Server → Client (v0.34): freigeschaltete Kosmetik des Spielers. */
public record CosmeticUnlocksPayload(List<String> unlocked) implements CustomPacketPayload {
	public static final Type<CosmeticUnlocksPayload> TYPE = new Type<>(JTechStorage.id("cosmetic_unlocks"));

	public static final StreamCodec<RegistryFriendlyByteBuf, CosmeticUnlocksPayload> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), CosmeticUnlocksPayload::unlocked,
			CosmeticUnlocksPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
