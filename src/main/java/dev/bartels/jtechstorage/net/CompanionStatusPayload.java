package dev.bartels.jtechstorage.net;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import dev.bartels.jtechstorage.JTechStorage;
import dev.bartels.jtechstorage.companion.CompanionTalents;

/**
 * Server → Client (v0.26): Zustand des eigenen Begleiters für HUD und Begleiter-Menü – auch, wenn er weit weg oder in
 * einer anderen Dimension ist.
 *
 * @param present  hat der Spieler einen Begleiter?
 * @param loaded   ist er gerade geladen (sonst sind die übrigen Werte veraltet bzw. leer)?
 * @param special  Pilz: Abklingzeit (Ticks), Mantis: Flugausdauer, Rabe: belegte Plätze
 * @param specialMax Pilz: volle Abklingzeit, Mantis: maximale Flugausdauer, Rabe: Plätze
 * @param flags    Bit 0 bleibt, 1 erschöpft, 2 Pilz heilt automatisch, 3 Rabe unterwegs, 4 Rabe hat Terminal, 5 andere Dimension
 */
public record CompanionStatusPayload(boolean present, boolean loaded, String species, String name, int stage, int level, int xp,
		float health, float maxHealth, int stance, int flags, int treatTicks, int special, int specialMax, float damage, float armor,
		int attackTicks, int distance, int[] talents) implements CustomPacketPayload {
	public static final Type<CompanionStatusPayload> TYPE = new Type<>(JTechStorage.id("companion_status"));
	public static final CompanionStatusPayload NONE = new CompanionStatusPayload(false, false, "", "", 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
			new int[CompanionTalents.COUNT]);
	public static final StreamCodec<RegistryFriendlyByteBuf, CompanionStatusPayload> STREAM_CODEC = StreamCodec.ofMember(CompanionStatusPayload::write,
			CompanionStatusPayload::read);

	public boolean flag(int bit) {
		return (flags & (1 << bit)) != 0;
	}

	private void write(RegistryFriendlyByteBuf buf) {
		buf.writeBoolean(present);
		buf.writeBoolean(loaded);
		buf.writeUtf(species);
		buf.writeUtf(name);
		buf.writeVarInt(stage);
		buf.writeVarInt(level);
		buf.writeVarInt(xp);
		buf.writeFloat(health);
		buf.writeFloat(maxHealth);
		buf.writeVarInt(stance);
		buf.writeVarInt(flags);
		buf.writeVarInt(treatTicks);
		buf.writeVarInt(special);
		buf.writeVarInt(specialMax);
		buf.writeFloat(damage);
		buf.writeFloat(armor);
		buf.writeVarInt(attackTicks);
		buf.writeVarInt(distance);
		buf.writeVarIntArray(talents);
	}

	private static CompanionStatusPayload read(RegistryFriendlyByteBuf buf) {
		CompanionStatusPayload p = new CompanionStatusPayload(buf.readBoolean(), buf.readBoolean(), buf.readUtf(), buf.readUtf(), buf.readVarInt(),
				buf.readVarInt(), buf.readVarInt(), buf.readFloat(), buf.readFloat(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(),
				buf.readVarInt(), buf.readVarInt(), buf.readFloat(), buf.readFloat(), buf.readVarInt(), buf.readVarInt(), buf.readVarIntArray());
		return p.talents.length == CompanionTalents.COUNT ? p : p.withTalents(new int[CompanionTalents.COUNT]);
	}

	private CompanionStatusPayload withTalents(int[] ranks) {
		return new CompanionStatusPayload(present, loaded, species, name, stage, level, xp, health, maxHealth, stance, flags, treatTicks, special,
				specialMax, damage, armor, attackTicks, distance, ranks);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
