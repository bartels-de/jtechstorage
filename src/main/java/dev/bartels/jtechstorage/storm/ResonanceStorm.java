package dev.bartels.jtechstorage.storm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.block.state.BlockState;

import dev.bartels.jtechstorage.ModAdvancements;
import dev.bartels.jtechstorage.ModSounds;
import dev.bartels.jtechstorage.entity.CrystalShardling;
import dev.bartels.jtechstorage.entity.StormHerald;

/**
 * Resonanz-Sturm (v0.25): Kristall-Splitterlinge greifen in drei Wellen ein Storage Terminal an, in der letzten Welle
 * mit dem Sturm-Herold. Solange der Sturm tobt, ist das Netzwerk gestört (Helfer halb so schnell, halbe Reichweite,
 * Resonanz wird abgesaugt). Der Tod des Herolds beendet den Sturm. Läuft nur im Speicher (endet bei Server-Neustart).
 */
public final class ResonanceStorm {
	/** Radius um das Sturm-Zentrum, in dem Terminals gestört sind und Spieler die Bossleiste sehen. */
	public static final int RADIUS = 96;
	public static final int WAVES = 3;
	private static final int FIRST_DELAY = 120;
	private static final int WAVE_DELAY = 160;
	private static final int WAVE_TIMEOUT = 20 * 150;
	private static final Map<ResourceKey<Level>, List<ResonanceStorm>> ACTIVE = new HashMap<>();

	private final ServerLevel level;
	private final BlockPos center;
	private final ServerBossEvent bar = new ServerBossEvent(UUID.randomUUID(),
			Component.translatable("jtechstorage.storm.bar.gathering").withStyle(ChatFormatting.LIGHT_PURPLE),
			BossEvent.BossBarColor.PINK, BossEvent.BossBarOverlay.NOTCHED_6);
	private final List<Mob> mobs = new ArrayList<>();
	private @Nullable StormHerald herald;
	/** 0 = zieht auf, 1..3 = Welle. */
	private int wave;
	private int waveTotal;
	private int countdown = FIRST_DELAY;
	private int waveAge;
	private boolean over;

	private ResonanceStorm(ServerLevel level, BlockPos center) {
		this.level = level;
		this.center = center.immutable();
	}

	// ------------------------------------------------------------------
	// Verwaltung
	// ------------------------------------------------------------------

	public static @Nullable ResonanceStorm start(ServerLevel level, BlockPos center) {
		if (at(level, center) != null) {
			return null;
		}

		ResonanceStorm storm = new ResonanceStorm(level, center);
		ACTIVE.computeIfAbsent(level.dimension(), key -> new ArrayList<>()).add(storm);
		level.getServer().setWeatherParameters(0, 20 * 60 * 10, true, true);
		level.playSound(null, center, ModSounds.STORM_START, SoundSource.HOSTILE, 6.0f, 1.0f);
		broadcast(level, center, Component.translatable("jtechstorage.storm.start").withStyle(ChatFormatting.LIGHT_PURPLE));
		return storm;
	}

	/** Beendet alle Stürme in der Nähe (ohne Belohnung). @return Anzahl beendeter Stürme. */
	public static int stop(ServerLevel level, BlockPos near) {
		int count = 0;

		for (ResonanceStorm storm : List.copyOf(ACTIVE.getOrDefault(level.dimension(), List.of()))) {
			if (storm.center.distSqr(near) <= (double) RADIUS * RADIUS * 4) {
				storm.finish(false);
				count++;
			}
		}

		return count;
	}

	/** Sturm, der diese Position stört (oder null). */
	public static @Nullable ResonanceStorm at(Level level, BlockPos pos) {
		List<ResonanceStorm> storms = ACTIVE.get(level.dimension());

		if (storms == null) {
			return null;
		}

		for (ResonanceStorm storm : storms) {
			if (!storm.over && storm.center.distSqr(pos) <= (double) RADIUS * RADIUS) {
				return storm;
			}
		}

		return null;
	}

	/** Ist das Netzwerk an dieser Position durch einen Sturm gestört? (nur Server) */
	public static boolean isDisturbed(@Nullable Level level, BlockPos pos) {
		return level != null && !level.isClientSide() && at(level, pos) != null;
	}

	/** Vom Herold beschworene Splitterlinge zählen zur aktuellen Welle. */
	public static void adopt(ServerLevel level, Mob mob) {
		ResonanceStorm storm = at(level, mob.blockPosition());

		if (storm != null) {
			storm.mobs.add(mob);
			storm.waveTotal++;
		}
	}

	public static void tickLevel(ServerLevel level) {
		List<ResonanceStorm> storms = ACTIVE.get(level.dimension());

		if (storms == null || storms.isEmpty()) {
			return;
		}

		for (ResonanceStorm storm : List.copyOf(storms)) {
			storm.tick();
		}

		storms.removeIf(storm -> storm.over);
	}

	public static void clearAll(MinecraftServer server) {
		for (List<ResonanceStorm> storms : ACTIVE.values()) {
			for (ResonanceStorm storm : storms) {
				storm.bar.removeAllPlayers();
			}
		}
		ACTIVE.clear();
	}

	// ------------------------------------------------------------------
	// Ablauf
	// ------------------------------------------------------------------

	private void tick() {
		if (over) {
			return;
		}

		long time = level.getGameTime();

		if (time % 20 == 0) {
			updatePlayers();
			mobs.removeIf(mob -> !mob.isAlive() || mob.isRemoved());
		}

		if (time % 4 == 0) {
			ambience(level.getRandom());
		}

		if (wave == WAVES && countdown == 0) {
			if (herald != null && (!herald.isAlive() || herald.isRemoved())) {
				finish(herald.getHealth() <= 0.0f);
				return;
			}

			if (herald == null && mobs.isEmpty()) {
				finish(true);   // kein Platz für den Herold gefunden: die Welle zählt trotzdem
				return;
			}
		}

		if (countdown > 0) {
			countdown--;
			bar.setProgress(1.0f - countdown / (float) (wave == 0 ? FIRST_DELAY : WAVE_DELAY));

			if (countdown == 0) {
				spawnWave(wave + 1);
			}
			return;
		}

		waveAge++;
		bar.setProgress(waveTotal == 0 ? 0.0f : mobs.size() / (float) waveTotal);

		if (wave < WAVES && (mobs.isEmpty() || waveAge > WAVE_TIMEOUT)) {
			countdown = WAVE_DELAY;
			bar.setName(Component.translatable("jtechstorage.storm.bar.next", wave + 1, WAVES).withStyle(ChatFormatting.LIGHT_PURPLE));
			bar.setColor(BossEvent.BossBarColor.PINK);
		}
	}

	private void spawnWave(int number) {
		wave = number;
		waveAge = 0;
		int players = Math.max(1, nearbyPlayers().size());
		int count = Math.min(18, 4 + number * 2 + (players - 1) * 2);
		waveTotal = 0;

		for (int i = 0; i < count; i++) {
			spawn(StormContent.CRYSTAL_SHARDLING.create(level, EntitySpawnReason.EVENT));
		}

		if (number == WAVES) {
			StormHerald boss = StormContent.STORM_HERALD.create(level, EntitySpawnReason.EVENT);

			if (spawn(boss)) {
				herald = boss;
				boss.setStormCenter(center);
			}

			bar.setName(Component.translatable("jtechstorage.storm.bar.herald").withStyle(ChatFormatting.RED));
			bar.setColor(BossEvent.BossBarColor.RED);
			broadcast(level, center, Component.translatable("jtechstorage.storm.herald").withStyle(ChatFormatting.RED));
		} else {
			bar.setName(Component.translatable("jtechstorage.storm.bar.wave", number, WAVES).withStyle(ChatFormatting.LIGHT_PURPLE));
			bar.setColor(BossEvent.BossBarColor.PURPLE);
		}

		level.playSound(null, center, ModSounds.STORM_WAVE, SoundSource.HOSTILE, 5.0f, 1.0f);
	}

	private boolean spawn(@Nullable Mob mob) {
		if (mob == null) {
			return false;
		}

		RandomSource random = level.getRandom();

		for (int attempt = 0; attempt < 24; attempt++) {
			double angle = random.nextDouble() * Math.PI * 2;
			double radius = 24 + random.nextDouble() * 16;
			int x = center.getX() + (int) Math.round(Math.cos(angle) * radius);
			int z = center.getZ() + (int) Math.round(Math.sin(angle) * radius);

			if (!level.isLoaded(new BlockPos(x, center.getY(), z))) {
				continue;
			}

			int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
			BlockPos pos = new BlockPos(x, y, z);
			BlockState ground = level.getBlockState(pos.below());

			if (!ground.getFluidState().isEmpty() || !ground.isFaceSturdy(level, pos.below(), net.minecraft.core.Direction.UP)
					|| Math.abs(y - center.getY()) > 24) {
				continue;
			}

			mob.snapTo(x + 0.5, y, z + 0.5, random.nextFloat() * 360.0f, 0.0f);

			if (mob instanceof CrystalShardling shardling) {
				shardling.setStormCenter(center);
			}

			mob.setPersistenceRequired();
			level.addFreshEntity(mob);
			mobs.add(mob);
			waveTotal++;
			level.sendParticles(ParticleTypes.REVERSE_PORTAL, x + 0.5, y + 0.5, z + 0.5, 30, 0.4, 0.6, 0.4, 0.08);
			level.sendParticles(new DustParticleOptions(CrystalShardling.COLOR, 1.5f), x + 0.5, y + 0.5, z + 0.5, 12, 0.4, 0.6, 0.4, 0.0);
			return true;
		}

		mob.discard();
		return false;
	}

	private void finish(boolean victory) {
		if (over) {
			return;
		}

		over = true;
		bar.removeAllPlayers();
		level.getServer().setWeatherParameters(20 * 60 * 5, 0, false, false);

		for (Mob mob : mobs) {
			if (mob.isAlive()) {
				CrystalShardling.shatter(level, mob);
				mob.discard();
			}
		}

		if (victory) {
			level.playSound(null, center, ModSounds.STORM_END, SoundSource.HOSTILE, 5.0f, 1.0f);
			broadcast(level, center, Component.translatable("jtechstorage.storm.victory").withStyle(ChatFormatting.GREEN));

			for (ServerPlayer player : nearbyPlayers()) {
				ModAdvancements.grant(player, "storm_survived");
			}
		} else {
			broadcast(level, center, Component.translatable("jtechstorage.storm.stopped").withStyle(ChatFormatting.GRAY));
		}
	}

	private List<ServerPlayer> nearbyPlayers() {
		List<ServerPlayer> out = new ArrayList<>();

		for (ServerPlayer player : level.players()) {
			if (player.blockPosition().distSqr(center) <= (double) RADIUS * RADIUS) {
				out.add(player);
			}
		}

		return out;
	}

	private void updatePlayers() {
		List<ServerPlayer> near = nearbyPlayers();

		for (ServerPlayer player : List.copyOf(bar.getPlayers())) {
			if (!near.contains(player)) {
				bar.removePlayer(player);
			}
		}

		for (ServerPlayer player : near) {
			bar.addPlayer(player);
		}
	}

	private void ambience(RandomSource random) {
		for (int i = 0; i < 6; i++) {
			double x = center.getX() + 0.5 + (random.nextDouble() - 0.5) * 48;
			double z = center.getZ() + 0.5 + (random.nextDouble() - 0.5) * 48;
			double y = center.getY() + 2 + random.nextDouble() * 12;
			level.sendParticles(new DustParticleOptions(random.nextBoolean() ? CrystalShardling.COLOR : 0x9A60E6, 1.6f), x, y, z, 1, 0.2, 0.2, 0.2, 0.0);
		}

		level.sendParticles(ParticleTypes.REVERSE_PORTAL, center.getX() + 0.5, center.getY() + 1.2, center.getZ() + 0.5, 3, 0.4, 0.4, 0.4, 0.05);
	}

	private static void broadcast(ServerLevel level, BlockPos center, Component message) {
		for (ServerPlayer player : level.players()) {
			if (player.blockPosition().distSqr(center) <= (double) RADIUS * RADIUS * 4) {
				player.sendSystemMessage(message);
			}
		}
	}

	public int wave() {
		return wave;
	}

	public BlockPos center() {
		return center;
	}
}
