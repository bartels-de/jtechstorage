package dev.bartels.jtechstorage.net;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import dev.bartels.jtechstorage.JTechStorage;

/** Player cosmetics (v0.34). */
public record CosmeticEquipPayload(String slot, String cosmetic) implements CustomPacketPayload {
	public static final Type<CosmeticEquipPayload> TYPE = new Type<>(JTechStorage.id("cosmetic_equip"));

	public static final StreamCodec<RegistryFriendlyByteBuf, CosmeticEquipPayload> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.STRING_UTF8, CosmeticEquipPayload::slot, ByteBufCodecs.STRING_UTF8, CosmeticEquipPayload::cosmetic,
			CosmeticEquipPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
