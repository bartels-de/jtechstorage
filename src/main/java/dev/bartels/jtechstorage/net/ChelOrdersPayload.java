package dev.bartels.jtechstorage.net;

import java.util.List;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;

import dev.bartels.jtechstorage.JTechStorage;

/** Server → Client: Chels Aufträge (8 Felder) mit aktuellem Vorrat und seinem Status. */
public record ChelOrdersPayload(int containerId, List<Entry> entries, Component status) implements CustomPacketPayload {
	public static final Type<ChelOrdersPayload> TYPE = new Type<>(JTechStorage.id("chel_orders"));

	public record Entry(ItemStack prototype, int target, long have) {
		public static final Entry EMPTY = new Entry(ItemStack.EMPTY, 0, 0);

		public static final StreamCodec<RegistryFriendlyByteBuf, Entry> STREAM_CODEC = StreamCodec.composite(
				ItemStack.OPTIONAL_STREAM_CODEC, Entry::prototype,
				ByteBufCodecs.VAR_INT, Entry::target,
				ByteBufCodecs.VAR_LONG, Entry::have,
				Entry::new);

		public boolean sameAs(Entry other) {
			return target == other.target && have == other.have && ItemStack.matches(prototype, other.prototype);
		}
	}

	public static final StreamCodec<RegistryFriendlyByteBuf, ChelOrdersPayload> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, ChelOrdersPayload::containerId,
			Entry.STREAM_CODEC.apply(ByteBufCodecs.list()), ChelOrdersPayload::entries,
			ComponentSerialization.STREAM_CODEC, ChelOrdersPayload::status,
			ChelOrdersPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
