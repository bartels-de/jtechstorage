package dev.bartels.jtechstorage.net;

import java.util.List;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import dev.bartels.jtechstorage.JTechStorage;

/** Client → Server: neue Reihenfolge des Flugplans in der Hand (Indizes der bisherigen Liste, fehlende = entfernt). */
public record FlightPlanEditPayload(boolean offHand, List<Integer> order) implements CustomPacketPayload {
	public static final Type<FlightPlanEditPayload> TYPE = new Type<>(JTechStorage.id("flight_plan_edit"));
	public static final StreamCodec<RegistryFriendlyByteBuf, FlightPlanEditPayload> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.BOOL, FlightPlanEditPayload::offHand,
			ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list(8)), FlightPlanEditPayload::order,
			FlightPlanEditPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
