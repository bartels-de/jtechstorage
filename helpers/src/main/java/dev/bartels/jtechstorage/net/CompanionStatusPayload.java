package dev.bartels.jtechstorage.net;

import dev.bartels.jtechstorage.companion.CompanionLooks;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import dev.bartels.jtechstorage.JTechStorage;
import dev.bartels.jtechstorage.companion.CompanionTalents;

/**
 * Server → Client (v0.26): state of the player's own companion for HUD and companion menu – even when it is far away or in
 * another dimension.
 *
 * @param present  does the player have a companion?
 * @param loaded   is it currently loaded (otherwise the remaining values are stale or empty)?
 * @param special  mushroom: cooldown (ticks), mantis: flight stamina, raven: used slots
 * @param specialMax mushroom: full cooldown, mantis: maximum flight stamina, raven: slots
 * @param flags    bit 0 stays, 1 exhausted, 2 mushroom heals automatically, 3 raven away, 4 raven has terminal, 5 other dimension,
 *                 6 at base, 7 raven scouting, 8 guard, 9 on shoulder, 10 mushroom flight active
 * @param timer    raven: remaining scouting time (> 0) or cooldown (< 0) in ticks
 * @param specialCooldown remaining special attack cooldown (v0.25.3), specialCooldownMax full cooldown
 * @param look     appearance (v0.25.4, see CompanionLooks), glow glow color (-1 = default), kills defeated monsters
 * @param bond     bond points (v0.26.0), favorite item ID of today's favorite food
 * @param decoy    raven: cooldown of the crow decoy in ticks (0 = ready, -1 = no raven ability) (v0.26.4)
 */
public record CompanionStatusPayload(boolean present, boolean loaded, String species, String name, int stage, int level, int xp,
		float health, float maxHealth, int stance, int flags, int treatTicks, int special, int specialMax, float damage, float armor,
		int attackTicks, int distance, int timer, int specialCooldown, int specialCooldownMax, int[] talents, int look, int glow, int kills, int bond, int decoy, String favorite) implements CustomPacketPayload {
	public static final Type<CompanionStatusPayload> TYPE = new Type<>(JTechStorage.id("companion_status"));
	public static final CompanionStatusPayload NONE = new CompanionStatusPayload(false, false, "", "", 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
			new int[CompanionTalents.COUNT], CompanionLooks.DEFAULT, -1, 0, 0, -1, "");
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
		buf.writeVarInt(timer);
		buf.writeVarInt(specialCooldown);
		buf.writeVarInt(specialCooldownMax);
		buf.writeVarIntArray(talents);
		buf.writeVarInt(look);
		buf.writeInt(glow);
		buf.writeVarInt(kills);
		buf.writeVarInt(bond);
		buf.writeInt(decoy);
		buf.writeUtf(favorite);
	}

	private static CompanionStatusPayload read(RegistryFriendlyByteBuf buf) {
		CompanionStatusPayload p = new CompanionStatusPayload(buf.readBoolean(), buf.readBoolean(), buf.readUtf(), buf.readUtf(), buf.readVarInt(),
				buf.readVarInt(), buf.readVarInt(), buf.readFloat(), buf.readFloat(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(),
				buf.readVarInt(), buf.readVarInt(), buf.readFloat(), buf.readFloat(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarIntArray(),
				buf.readVarInt(), buf.readInt(), buf.readVarInt(), buf.readVarInt(), buf.readInt(), buf.readUtf());
		return p.talents.length == CompanionTalents.COUNT ? p : p.withTalents(new int[CompanionTalents.COUNT]);
	}

	private CompanionStatusPayload withTalents(int[] ranks) {
		return new CompanionStatusPayload(present, loaded, species, name, stage, level, xp, health, maxHealth, stance, flags, treatTicks, special,
				specialMax, damage, armor, attackTicks, distance, timer, specialCooldown, specialCooldownMax, ranks, look, glow, kills, bond, decoy, favorite);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
