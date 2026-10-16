package dev.bartels.jtechstorage.net;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;

import dev.bartels.jtechstorage.JTechStorage;

/**
 * Client → Server: Klick auf das Item-Raster des Terminals.
 *
 * <p>{@code prototype} dient nur als Vorlage – der Server entnimmt ausschließlich Items,
 * die wirklich im Netzwerk liegen und exakt gleich sind.
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
		/** Ganzen Stapel auf den Mauszeiger nehmen. */
		TAKE_STACK,
		/** Halben Stapel auf den Mauszeiger nehmen. */
		TAKE_HALF,
		/** Einen Stapel direkt ins Inventar (Shift-Klick). */
		TAKE_TO_INVENTORY,
		/** Item am Mauszeiger komplett einlagern. */
		INSERT_ALL,
		/** Ein einzelnes Item vom Mauszeiger einlagern. */
		INSERT_ONE,
		/** Crafting-Raster ins Netzwerk leeren. */
		CLEAR_GRID
	}
}
