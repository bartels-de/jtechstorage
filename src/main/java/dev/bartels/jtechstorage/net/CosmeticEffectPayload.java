package dev.bartels.jtechstorage.net;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import dev.bartels.jtechstorage.JTechStorage;

/** Spieler-Kosmetik (v0.34). */
public record CosmeticEffectPayload(int entityId, String cosmetic) implements CustomPacketPayload {
	public static final Type<CosmeticEffectPayload> TYPE = new Type<>(JTechStorage.id("cosmetic_effect"));

	public static final StreamCodec<RegistryFriendlyByteBuf, CosmeticEffectPayload> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, CosmeticEffectPayload::entityId, ByteBufCodecs.STRING_UTF8, CosmeticEffectPayload::cosmetic,
			CosmeticEffectPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
