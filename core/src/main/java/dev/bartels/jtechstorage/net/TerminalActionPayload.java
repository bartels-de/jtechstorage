package dev.bartels.jtechstorage.net;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;

import dev.bartels.jtechstorage.JTechStorage;

/**
 * Client → Server: click on the terminal's item grid.
 *
 * <p>{@code prototype} serves only as a template – the server only takes items
 * that actually exist in the network and are exactly equal.
 */
public record TerminalActionPayload(int containerId, Action action, ItemStack prototype) implements CustomPacketPayload {
	public static final Type<TerminalActionPayload> TYPE = new Type<>(JTechStorage.id("terminal_action"));

	public static final StreamCodec<RegistryFriendlyByteBuf, TerminalActionPayload> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, TerminalActionPayload::containerId,
			ByteBufCodecs.idMapper(i -> Action.values()[Math.floorMod(i, Action.values().length)], Action::ordinal),
			TerminalActionPayload::action,
			ItemStack.OPTIONAL_STREAM_CODEC, TerminalActionPayload::prototype,
			TerminalActionPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public enum Action {
		/** Take the whole stack onto the cursor. */
		TAKE_STACK,
		/** Take half a stack onto the cursor. */
		TAKE_HALF,
		/** One stack directly into the inventory (Shift-click). */
		TAKE_TO_INVENTORY,
		/** Store the cursor item completely. */
		INSERT_ALL,
		/** Store a single item from the cursor. */
		INSERT_ONE,
		/** Empty the crafting grid into the network. */
		CLEAR_GRID
	}
}
