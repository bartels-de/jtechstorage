package dev.bartels.jtechstorage.net;

import java.util.List;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;

import dev.bartels.jtechstorage.JTechStorage;

/**
 * Client → Server (v0.29.4): which items are currently in the visible fields of the terminal grid (search,
 * sorting and scrolling happen on the client). This makes the fields real vanilla slots – clicks, keys,
 * dragging and double-click all use normal inventory behavior.
 */
public record TerminalViewPayload(int containerId, List<ItemStack> prototypes) implements CustomPacketPayload {
	public static final Type<TerminalViewPayload> TYPE = new Type<>(JTechStorage.id("terminal_view"));

	public static final StreamCodec<RegistryFriendlyByteBuf, TerminalViewPayload> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, TerminalViewPayload::containerId,
			ItemStack.OPTIONAL_LIST_STREAM_CODEC, TerminalViewPayload::prototypes,
			TerminalViewPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
