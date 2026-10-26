package dev.bartels.jtechstorage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.mojang.serialization.Codec;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.world.InteractionResult;

import dev.bartels.jtechstorage.entity.JbartelsHologram;

/**
 * Jbartels at major milestones (v0.35): when an advancement from {@link #MILESTONES} is newly reached, his
 * hologram briefly appears next to the player - once per milestone per player, one after another. Players who have
 * been playing for a while get no flood the first time: whatever was already reached at introduction counts as seen.
 * <p>Also: protects the prototypes in the workshop (tag {@value #PROTOTYPE_TAG}) - no interaction, no attacks.
 */
public final class JbartelsMilestones {
	/** Advancement IDs in order (the speech bubble is "jtechstorage.milestone.&lt;id&gt;"). */
	public static final List<String> MILESTONES = List.of("first_link", "sprite", "amethyst_robot", "crystal_depths", "find_workshop",
			"storm_survived", "amethyst_spider", "max_upgrade", "depths_lore");
	public static final String PROTOTYPE_TAG = "jts_prototype";
	/** Pause between two appearances (ticks), in case several milestones fall in quick succession. */
	private static final int GAP_TICKS = JbartelsHologram.MILESTONE_TICKS + 60;

	/** Milestones already shown to the player (null = never checked). */
	public static final AttachmentType<List<String>> SEEN = AttachmentRegistry.<List<String>>builder()
			.persistent(Codec.STRING.listOf())
			.copyOnDeath()
			.buildAndRegister(JTechStorage.id("milestones_seen"));

	private static final Map<UUID, Long> LAST_SHOWN = new HashMap<>();

	private JbartelsMilestones() {
	}

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % 20 == 0) {
				check(server);
			}
		});

		UseEntityCallback.EVENT.register((player, level, hand, entity, hit) ->
				entity.entityTags().contains(PROTOTYPE_TAG) && !player.isCreative() ? InteractionResult.FAIL : InteractionResult.PASS);
		AttackEntityCallback.EVENT.register((player, level, hand, entity, hit) ->
				entity.entityTags().contains(PROTOTYPE_TAG) && !player.isCreative() ? InteractionResult.FAIL : InteractionResult.PASS);
	}

	private static boolean done(MinecraftServer server, ServerPlayer player, String id) {
		AdvancementHolder holder = server.getAdvancements().get(JTechStorage.id(id));
		return holder != null && player.getAdvancements().getOrStartProgress(holder).isDone();
	}

	private static void check(MinecraftServer server) {
		boolean enabled = ModConfig.get().helpers.milestoneHologram;
		long now = server.getTickCount();

		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			List<String> seen = player.getAttached(SEEN);

			if (seen == null) {
				// first time: quietly remember what was already reached as seen
				List<String> initial = new ArrayList<>();

				for (String id : MILESTONES) {
					if (done(server, player, id)) {
						initial.add(id);
					}
				}

				player.setAttached(SEEN, initial);
				continue;
			}

			if (!enabled || !player.isAlive() || player.isSpectator() || now - LAST_SHOWN.getOrDefault(player.getUUID(), -100000L) < GAP_TICKS) {
				continue;
			}

			for (String id : MILESTONES) {
				if (seen.contains(id) || !done(server, player, id)) {
					continue;
				}

				// don't burst in on the tour or another appearance
				boolean busy = !player.level().getEntitiesOfClass(JbartelsHologram.class, player.getBoundingBox().inflate(64),
						h -> h.isFor(player)).isEmpty();

				if (!busy) {
					List<String> updated = new ArrayList<>(seen);
					updated.add(id);
					player.setAttached(SEEN, updated);
					LAST_SHOWN.put(player.getUUID(), now);
					JbartelsHologram.milestone(player, id);
				}

				break;
			}
		}
	}
}
