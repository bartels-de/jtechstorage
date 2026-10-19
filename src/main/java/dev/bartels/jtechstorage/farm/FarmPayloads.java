package dev.bartels.jtechstorage.farm;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import dev.bartels.jtechstorage.JTechStorage;

/** Netzwerk-Pakete der Farm-Zusätze (v0.27.0). */
public final class FarmPayloads {
	private FarmPayloads() {
	}

	/** Client → Server: Farm-Monitor umbenennen. */
	public record Rename(BlockPos pos, String name) implements CustomPacketPayload {
		public static final Type<Rename> TYPE = new Type<>(JTechStorage.id("farm_monitor_rename"));
		public static final StreamCodec<RegistryFriendlyByteBuf, Rename> STREAM_CODEC = StreamCodec.composite(
				BlockPos.STREAM_CODEC, Rename::pos,
				ByteBufCodecs.stringUtf8(64), Rename::name,
				Rename::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	/** Client → Server: Farmen-Liste des offenen Terminals anfordern. */
	public record Request(int containerId) implements CustomPacketPayload {
		public static final Type<Request> TYPE = new Type<>(JTechStorage.id("terminal_farms_request"));
		public static final StreamCodec<RegistryFriendlyByteBuf, Request> STREAM_CODEC =
				ByteBufCodecs.VAR_INT.map(Request::new, Request::containerId).cast();

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	/** Eine Farm in der Liste. */
	public record Farm(String name, String dimension, BlockPos pos, int perHour, int status, String topItem, int minutesAgo) {
		public static final StreamCodec<RegistryFriendlyByteBuf, Farm> STREAM_CODEC = new StreamCodec<>() {
			@Override
			public Farm decode(RegistryFriendlyByteBuf buf) {
				return new Farm(buf.readUtf(64), buf.readUtf(), BlockPos.STREAM_CODEC.decode(buf), buf.readVarInt(), buf.readVarInt(), buf.readUtf(),
						buf.readVarInt());
			}

			@Override
			public void encode(RegistryFriendlyByteBuf buf, Farm farm) {
				buf.writeUtf(farm.name(), 64);
				buf.writeUtf(farm.dimension());
				BlockPos.STREAM_CODEC.encode(buf, farm.pos());
				buf.writeVarInt(farm.perHour());
				buf.writeVarInt(farm.status());
				buf.writeUtf(farm.topItem());
				buf.writeVarInt(farm.minutesAgo());
			}
		};
	}

	/** Server → Client: die Farmen des Terminals. */
	public record Farms(int containerId, List<Farm> farms) implements CustomPacketPayload {
		public static final Type<Farms> TYPE = new Type<>(JTechStorage.id("terminal_farms"));
		public static final StreamCodec<RegistryFriendlyByteBuf, Farms> STREAM_CODEC = StreamCodec.composite(
				ByteBufCodecs.VAR_INT, Farms::containerId,
				Farm.STREAM_CODEC.apply(ByteBufCodecs.list(256)), Farms::farms,
				Farms::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}
}
