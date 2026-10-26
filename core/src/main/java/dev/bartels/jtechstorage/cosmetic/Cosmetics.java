package dev.bartels.jtechstorage.cosmetic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import com.mojang.serialization.Codec;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import dev.bartels.jtechstorage.JTechStorage;
import dev.bartels.jtechstorage.net.CosmeticEffectPayload;
import dev.bartels.jtechstorage.net.CosmeticUnlocksPayload;

/**
 * Player cosmetics (v0.34): the equipped cosmetic per slot is a Fabric attachment on the player (kept after death,
 * synced to all players so they can see it). Unlocking happens via advancements - only the server
 * checks that.
 */
public final class Cosmetics {
	/** Slot ID -> cosmetic ID. */
	public static final AttachmentType<Map<String, String>> LOADOUT = AttachmentRegistry.<Map<String, String>>builder()
			.persistent(Codec.unboundedMap(Codec.STRING, Codec.STRING))
			.copyOnDeath()
			.initializer(HashMap::new)
			.syncWith(ByteBufCodecs.map(HashMap::new, ByteBufCodecs.STRING_UTF8, ByteBufCodecs.STRING_UTF8), AttachmentSyncPredicate.all())
			.buildAndRegister(JTechStorage.id("cosmetics"));

	public static final int EMOTE_COOLDOWN_TICKS = 200;
	private static final Map<UUID, Long> LAST_EMOTE = new HashMap<>();

	private Cosmetics() {
	}

	public static void init() {
	}

	/** Equipped cosmetic of a slot (client and server). */
	public static @Nullable Cosmetic equipped(Player player, Cosmetic.Slot slot) {
		Map<String, String> loadout = player.getAttached(LOADOUT);
		return loadout == null ? null : Cosmetic.byId(loadout.getOrDefault(slot.id(), ""));
	}

	public static boolean isUnlocked(ServerPlayer player, Cosmetic cosmetic) {
		if (cosmetic.advancement == null) {
			return true;
		}

		AdvancementHolder holder = player.level().getServer().getAdvancements().get(JTechStorage.id(cosmetic.advancement));
		return holder != null && player.getAdvancements().getOrStartProgress(holder).isDone();
	}

	public static List<String> unlocked(ServerPlayer player) {
		List<String> out = new ArrayList<>();

		for (Cosmetic c : Cosmetic.values()) {
			if (isUnlocked(player, c)) {
				out.add(c.id());
			}
		}

		return out;
	}

	public static void sendUnlocks(ServerPlayer player) {
		ServerPlayNetworking.send(player, new CosmeticUnlocksPayload(unlocked(player)));
	}

	/** Equip ({@code cosmeticId} empty = clear the slot). Invalid or locked ones are ignored. */
	public static void equip(ServerPlayer player, String slotId, String cosmeticId) {
		Cosmetic.Slot slot = Cosmetic.Slot.byId(slotId);

		if (slot == null) {
			return;
		}

		Map<String, String> loadout = new HashMap<>(player.getAttachedOrElse(LOADOUT, Map.of()));

		if (cosmeticId.isEmpty()) {
			loadout.remove(slot.id());
		} else {
			Cosmetic cosmetic = Cosmetic.byId(cosmeticId);

			if (cosmetic == null || cosmetic.slot != slot || !isUnlocked(player, cosmetic)) {
				return;
			}

			loadout.put(slot.id(), cosmetic.id());
		}

		player.setAttached(LOADOUT, loadout);
	}

	/** Trigger emote: check cooldown, then send to all players in view range (and oneself). */
	public static void emote(ServerPlayer player) {
		Cosmetic emote = equipped(player, Cosmetic.Slot.EMOTE);

		if (emote == null) {
			emote = Cosmetic.EMOTE_FIREWORK;
		}

		if (!isUnlocked(player, emote)) {
			return;
		}

		long now = player.level().getGameTime();
		Long last = LAST_EMOTE.get(player.getUUID());

		if (last != null && now - last < EMOTE_COOLDOWN_TICKS && now >= last) {
			return;
		}

		LAST_EMOTE.put(player.getUUID(), now);
		CosmeticEffectPayload payload = new CosmeticEffectPayload(player.getId(), emote.id());
		ServerPlayNetworking.send(player, payload);

		for (ServerPlayer other : PlayerLookup.tracking(player)) {
			if (other != player) {
				ServerPlayNetworking.send(other, payload);
			}
		}
	}
}
