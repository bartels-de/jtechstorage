package dev.bartels.jtechstorage.net;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;

import dev.bartels.jtechstorage.JTechStorage;

/** Choose an item from the network (v0.36): screen shows it, banner picks it up or drops it. */
public record PanelItemPayload(BlockPos pos, ItemStack stack) implements CustomPacketPayload {
	public static final Type<PanelItemPayload> TYPE = new Type<>(JTechStorage.id("panel_item"));

	public static final StreamCodec<RegistryFriendlyByteBuf, PanelItemPayload> STREAM_CODEC = StreamCodec.composite(
			BlockPos.STREAM_CODEC, PanelItemPayload::pos, ItemStack.OPTIONAL_STREAM_CODEC, PanelItemPayload::stack, PanelItemPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
