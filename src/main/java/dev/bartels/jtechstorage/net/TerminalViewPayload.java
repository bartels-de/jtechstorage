package dev.bartels.jtechstorage.net;

import java.util.List;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;

import dev.bartels.jtechstorage.JTechStorage;

/**
 * Client → Server (v0.29.4): welche Items gerade in den sichtbaren Feldern des Terminal-Rasters stehen (Suche,
 * Sortierung und Scrollen passieren auf dem Client). Damit sind die Felder echte Vanilla-Slots – Klicks, Tasten,
 * Ziehen und Doppelklick laufen über das normale Inventar-Verhalten.
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
