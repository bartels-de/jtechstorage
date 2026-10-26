package dev.bartels.jtechstorage.client.cosmetic;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityRenderLayerRegistrationCallback;

import dev.bartels.jtechstorage.ModSounds;
import dev.bartels.jtechstorage.client.JTechStorageClient;
import dev.bartels.jtechstorage.cosmetic.Cosmetic;
import dev.bartels.jtechstorage.cosmetic.Cosmetics;
import dev.bartels.jtechstorage.net.CosmeticEffectPayload;
import dev.bartels.jtechstorage.net.CosmeticRequestPayload;
import dev.bartels.jtechstorage.net.CosmeticUnlocksPayload;

/** Client side of player cosmetics (v0.34): keys, trails and auras, emote effects, menu. */
public final class CosmeticsClient {
	public static final KeyMapping MENU_KEY = KeyMappingHelper.registerKeyMapping(
			new KeyMapping("key.jtechstorage.cosmetics", InputConstants.KEY_J, JTechStorageClient.KEY_CATEGORY));
	public static final KeyMapping EMOTE_KEY = KeyMappingHelper.registerKeyMapping(
			new KeyMapping("key.jtechstorage.emote", InputConstants.KEY_N, JTechStorageClient.KEY_CATEGORY));

	static final int AZURE = 0x6CCCF0;
	static final int ROSE = 0xF08CC0;
	static final int CITRINE = 0xF0C444;
	static final int LAVENDER = 0xBB92F6;

	/** Unlocked cosmetics of the local player (from the server). */
	static final Set<String> UNLOCKED = new HashSet<>();
	private static final List<Effect> EFFECTS = new ArrayList<>();
	private static long lastEmoteRequest;

	private CosmeticsClient() {
	}

	/** Running emote effect that spawns particles over several ticks. */
	private static final class Effect {
		final int entityId;
		final Cosmetic emote;
		int age;

		Effect(int entityId, Cosmetic emote) {
			this.entityId = entityId;
			this.emote = emote;
		}
	}

	public static void init() {
		CosmeticLayer.registerLayers();
		LivingEntityRenderLayerRegistrationCallback.EVENT.register((type, renderer, helper, context) -> {
			if (renderer instanceof AvatarRenderer<?> avatar) {
				helper.register(new CosmeticLayer(avatar, context));
			}
		});
		ClientPlayNetworking.registerGlobalReceiver(CosmeticUnlocksPayload.TYPE, (payload, context) -> {
			UNLOCKED.clear();
			UNLOCKED.addAll(payload.unlocked());
		});
		ClientPlayNetworking.registerGlobalReceiver(CosmeticEffectPayload.TYPE, (payload, context) -> {
			Cosmetic emote = Cosmetic.byId(payload.cosmetic());

			if (emote != null) {
				EFFECTS.add(new Effect(payload.entityId(), emote));
			}
		});
		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
			UNLOCKED.clear();
			EFFECTS.clear();
			ClientPlayNetworking.send(new CosmeticRequestPayload(CosmeticRequestPayload.UNLOCKS));
		});
		ClientTickEvents.END_CLIENT_TICK.register(CosmeticsClient::tick);
	}

	private static void tick(Minecraft client) {
		while (MENU_KEY.consumeClick()) {
			if (client.player != null && client.gui.screen() == null) {
				client.setScreenAndShow(new CosmeticsScreen());
			}
		}

		while (EMOTE_KEY.consumeClick()) {
			if (client.player != null && client.level != null && client.level.getGameTime() - lastEmoteRequest > 10) {
				lastEmoteRequest = client.level.getGameTime();
				ClientPlayNetworking.send(new CosmeticRequestPayload(CosmeticRequestPayload.EMOTE));
			}
		}

		ClientLevel level = client.level;

		if (level == null || client.isPaused()) {
			return;
		}

		for (Player player : level.players()) {
			if (player.isInvisible() || player.isSpectator() || client.player != null && player.distanceToSqr(client.player) > 64 * 64) {
				continue;
			}

			trail(level, player, Cosmetics.equipped(player, Cosmetic.Slot.TRAIL));
			aura(level, player, Cosmetics.equipped(player, Cosmetic.Slot.AURA));
		}

		for (Iterator<Effect> it = EFFECTS.iterator(); it.hasNext();) {
			Effect effect = it.next();
			Entity entity = level.getEntity(effect.entityId);

			if (entity == null || effect.age > 60) {
				it.remove();
				continue;
			}

			emote(level, entity, effect);
			effect.age++;
		}
	}

	// ------------------------------------------------------------------ Trails (while walking)

	private static void trail(ClientLevel level, Player player, Cosmetic trail) {
		if (trail == null || !player.onGround()) {
			return;
		}

		Vec3 move = player.getDeltaMovement();

		if (move.horizontalDistanceSqr() < 0.0004 || player.tickCount % 2 != 0) {
			return;
		}

		RandomSource r = level.getRandom();
		double x = player.getX() - move.x * 2 + (r.nextDouble() - 0.5) * 0.4;
		double z = player.getZ() - move.z * 2 + (r.nextDouble() - 0.5) * 0.4;
		double y = player.getY() + 0.05;

		switch (trail) {
			case TRAIL_SPARKLE -> {
				level.addParticle(dust(LAVENDER, 0.7f), x, y + 0.05, z, 0, 0.01, 0);

				if (r.nextInt(3) == 0) {
					level.addParticle(ParticleTypes.END_ROD, x, y + 0.1, z, 0, 0.015, 0);
				}
			}
			case TRAIL_PETALS -> level.addParticle(ParticleTypes.CHERRY_LEAVES, x, y + 0.6, z, 0, 0, 0);
			case TRAIL_SNOW -> level.addParticle(ParticleTypes.SNOWFLAKE, x, y + 0.1, z, (r.nextDouble() - 0.5) * 0.02, 0.02, (r.nextDouble() - 0.5) * 0.02);
			case TRAIL_EMBERS -> {
				level.addParticle(ParticleTypes.SMALL_FLAME, x, y + 0.05, z, 0, 0.01, 0);

				if (r.nextInt(2) == 0) {
					level.addParticle(dust(0xF6901E, 0.6f), x, y + 0.1, z, 0, 0.03, 0);
				}
			}
			case TRAIL_PRISM -> {
				int color = switch ((player.tickCount / 2) % 3) {
					case 0 -> AZURE;
					case 1 -> ROSE;
					default -> CITRINE;
				};
				level.addParticle(dust(color, 0.9f), x, y + 0.08, z, 0, 0.01, 0);
			}
			default -> {
			}
		}
	}

	// ------------------------------------------------------------------ Auras (always on, calm)

	private static final int[] MEMORY_COLORS = {0x9468E6, 0x2EB4B8, 0xB8307E, 0x22957F, 0x8474D0, 0xC84482, 0x4EA0D4, 0xD0901E,
			0x7676A8, 0xDCAE52, 0x86C08C, 0x80E0FF};

	private static void aura(ClientLevel level, Player player, Cosmetic aura) {
		if (aura == null) {
			return;
		}

		RandomSource r = level.getRandom();

		switch (aura) {
			case AURA_CRYSTAL -> {
				if (player.tickCount % 3 == 0) {
					float a = player.tickCount * 0.21f;
					double radius = 0.6;
					double h = (player.tickCount % 40) / 40.0 * 2.0;
					level.addParticle(dust(LAVENDER, 0.6f), player.getX() + Mth.cos(a) * radius, player.getY() + h, player.getZ() + Mth.sin(a) * radius, 0, 0.005, 0);
					level.addParticle(dust(0xDCC4FF, 0.5f), player.getX() - Mth.cos(a) * radius, player.getY() + 2.0 - h, player.getZ() - Mth.sin(a) * radius, 0, 0.005, 0);
				}

				if (r.nextInt(25) == 0) {
					level.addParticle(ParticleTypes.END_ROD, player.getRandomX(0.8), player.getY() + 0.2, player.getRandomZ(0.8), 0, 0.03, 0);
				}
			}
			case AURA_FIREFLIES -> {
				if (r.nextInt(12) == 0) {
					level.addParticle(ParticleTypes.FIREFLY, player.getRandomX(1.6), player.getY() + 0.4 + r.nextDouble() * 1.6, player.getRandomZ(1.6), 0, 0, 0);
				}
			}
			case AURA_MEMORY -> {
				// twelve memory colors slowly orbit at chest height
				if (player.tickCount % 4 == 0) {
					int k = (player.tickCount / 4) % MEMORY_COLORS.length;
					float a = player.tickCount * 0.09f + k * (Mth.TWO_PI / MEMORY_COLORS.length);
					level.addParticle(dust(MEMORY_COLORS[k], 0.7f), player.getX() + Mth.cos(a) * 0.75, player.getY() + 1.1 + Mth.sin(a * 2.0f) * 0.15,
							player.getZ() + Mth.sin(a) * 0.75, 0, 0, 0);
				}

				if (r.nextInt(40) == 0) {
					level.addParticle(ParticleTypes.END_ROD, player.getRandomX(0.9), player.getY() + 1.8, player.getRandomZ(0.9), 0, 0.01, 0);
				}
			}
			default -> {
			}
		}
	}

	// ------------------------------------------------------------------ Emotes

	private static void emote(ClientLevel level, Entity entity, Effect effect) {
		RandomSource r = level.getRandom();
		double x = entity.getX();
		double y = entity.getY();
		double z = entity.getZ();
		int age = effect.age;

		switch (effect.emote) {
			case EMOTE_FIREWORK -> {
				if (age == 0) {
					level.playLocalSound(x, y, z, SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.PLAYERS, 0.8f, 1.2f, false);
				}

				if (age < 12) {
					level.addParticle(dust(LAVENDER, 1.0f), x, y + 2.2 + age * 0.15, z, 0, 0, 0);
				}

				if (age == 12) {
					level.playLocalSound(x, y, z, SoundEvents.FIREWORK_ROCKET_TWINKLE, SoundSource.PLAYERS, 0.9f, 1.0f, false);
					int[] colors = {AZURE, ROSE, CITRINE, LAVENDER};

					for (int i = 0; i < 70; i++) {
						double a = r.nextDouble() * Math.PI * 2;
						double b = (r.nextDouble() - 0.5) * Math.PI;
						double s = 0.18 + r.nextDouble() * 0.05;
						level.addParticle(dust(colors[i % 4], 1.2f), x, y + 4.0, z, Math.cos(a) * Math.cos(b) * s, Math.sin(b) * s, Math.sin(a) * Math.cos(b) * s);
					}

					for (int i = 0; i < 12; i++) {
						level.addParticle(ParticleTypes.FIREWORK, x, y + 4.0, z, (r.nextDouble() - 0.5) * 0.3, (r.nextDouble() - 0.5) * 0.3, (r.nextDouble() - 0.5) * 0.3);
					}
				}

				if (age >= 14 && age <= 24 && age % 3 == 0) {
					level.playLocalSound(x, y + 4, z, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.8f, 0.8f + (age - 14) * 0.05f, false);
				}
			}
			case EMOTE_WAVE -> {
				if (age == 0) {
					level.playLocalSound(x, y, z, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0f, 1.0f, false);
				}

				if (age < 24 && age % 2 == 0) {
					double radius = 0.4 + age * 0.22;
					int count = 16 + age;

					for (int i = 0; i < count; i++) {
						double a = i * Math.PI * 2 / count;
						level.addParticle(dust(age % 4 == 0 ? LAVENDER : 0xDCC4FF, 0.9f), x + Math.cos(a) * radius, y + 0.15, z + Math.sin(a) * radius, 0, 0.01, 0);
					}
				}
			}
			case EMOTE_QUOKKA -> {
				if (age == 0) {
					level.playLocalSound(x, y, z, ModSounds.QUOKKA_AMBIENT, SoundSource.PLAYERS, 1.0f, 1.1f, false);
				}

				if (age < 30 && age % 3 == 0) {
					level.addParticle(ParticleTypes.HEART, entity.getRandomX(0.8), y + 2.0 + r.nextDouble() * 0.4, entity.getRandomZ(0.8), 0, 0.05, 0);
				}

				if (age == 10) {
					level.playLocalSound(x, y, z, ModSounds.QUOKKA_CURE, SoundSource.PLAYERS, 0.6f, 1.2f, false);
				}
			}
			case EMOTE_RAIN -> {
				if (age == 0) {
					level.playLocalSound(x, y, z, SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.PLAYERS, 0.8f, 1.3f, false);
				}

				ParticleOptions shard = new BlockParticleOption(ParticleTypes.FALLING_DUST, Blocks.AMETHYST_BLOCK.defaultBlockState());

				for (int i = 0; i < 3; i++) {
					level.addParticle(shard, x + (r.nextDouble() - 0.5) * 4, y + 3.5, z + (r.nextDouble() - 0.5) * 4, 0, 0, 0);
				}

				if (age % 6 == 0) {
					level.addParticle(ParticleTypes.END_ROD, entity.getRandomX(2.0), y + 3.0, entity.getRandomZ(2.0), 0, -0.05, 0);
					level.playLocalSound(x, y, z, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.4f, 1.4f + r.nextFloat() * 0.4f, false);
				}
			}
			default -> {
			}
		}
	}

	static DustParticleOptions dust(int rgb, float scale) {
		return new DustParticleOptions(rgb, scale);
	}

	static void requestUnlocks() {
		ClientPlayNetworking.send(new CosmeticRequestPayload(CosmeticRequestPayload.UNLOCKS));
	}
}
