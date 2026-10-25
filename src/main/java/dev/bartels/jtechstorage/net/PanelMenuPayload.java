package dev.bartels.jtechstorage.net;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;

import dev.bartels.jtechstorage.JTechStorage;

/** Menü von Bildschirm/Banner öffnen (v0.36) – mit den Gegenständen des verknüpften Netzwerks (meiste zuerst). */
public record PanelMenuPayload(BlockPos pos, List<ItemStack> items, List<Long> counts) implements CustomPacketPayload {
	public static final Type<PanelMenuPayload> TYPE = new Type<>(JTechStorage.id("panel_menu"));

	public static final StreamCodec<RegistryFriendlyByteBuf, PanelMenuPayload> STREAM_CODEC = StreamCodec.composite(
			BlockPos.STREAM_CODEC, PanelMenuPayload::pos,
			ByteBufCodecs.collection(ArrayList::new, ItemStack.STREAM_CODEC), PanelMenuPayload::items,
			ByteBufCodecs.collection(ArrayList::new, ByteBufCodecs.VAR_LONG), PanelMenuPayload::counts,
			PanelMenuPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
