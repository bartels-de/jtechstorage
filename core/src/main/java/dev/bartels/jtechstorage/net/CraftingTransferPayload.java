package dev.bartels.jtechstorage.net;

import java.util.List;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;

import dev.bartels.jtechstorage.JTechStorage;

/**
 * Client → Server: transfer a recipe from JEI into the crafting grid.
 * For each of the 9 fields a list of allowed ingredients (alternatives), empty = field stays empty.
 */
public record CraftingTransferPayload(int containerId, List<List<ItemStack>> slots) implements CustomPacketPayload {
	public static final int MAX_ALTERNATIVES = 32;

	public static final Type<CraftingTransferPayload> TYPE = new Type<>(JTechStorage.id("crafting_transfer"));

	public static final StreamCodec<RegistryFriendlyByteBuf, CraftingTransferPayload> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, CraftingTransferPayload::containerId,
			ItemStack.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_ALTERNATIVES)).apply(ByteBufCodecs.list(9)),
			CraftingTransferPayload::slots,
			CraftingTransferPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
