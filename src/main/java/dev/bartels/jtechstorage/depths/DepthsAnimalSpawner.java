package dev.bartels.jtechstorage.depths;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

/**
 * Friedliche Tiere in der Kristalltiefe (v0.36.1). Vanilla setzt Tiere fast nur beim Erzeugen eines Chunks und sucht in
 * Dimensionen mit Decke dabei nur die oberste Höhle ab – dort liegt kaum Kristallrasen, also kamen fast keine Tiere.
 * Dieser Spawner hält um jeden Spieler in der Tiefe eine kleine Tierwelt am Leben: alle 10 Sekunden, solange im Umkreis
 * von 64 Blöcken weniger als {@link #TARGET} friedliche Tiere leben, sucht er 24–56 Blöcke entfernt eine Stelle auf dem
 * Höhlenboden und setzt dort eine Gruppe aus der Spawnliste des Bioms ab – mit den normalen Spawnregeln der Tiere.
 */
public final class DepthsAnimalSpawner {
	private static final int INTERVAL = 200;
	private static final int TARGET = 8;
	private static final int ATTEMPTS = 8;

	private DepthsAnimalSpawner() {
	}

	public static void init() {
		ServerTickEvents.END_LEVEL_TICK.register(DepthsAnimalSpawner::tick);
	}

	private static void tick(ServerLevel level) {
		if (!level.dimension().equals(DepthsContent.DEPTHS) || level.getGameTime() % INTERVAL != 0
				|| !level.getGameRules().get(net.minecraft.world.level.gamerules.GameRules.SPAWN_MOBS)) {
			return;
		}

		for (Entity player : centers(level)) {
			if (player.isSpectator()) {
				continue;
			}

			AABB area = player.getBoundingBox().inflate(64.0, 32.0, 64.0);
			int animals = level.getEntitiesOfClass(Mob.class, area, mob -> mob.getType().getCategory() == MobCategory.CREATURE).size();

			if (animals >= TARGET) {
				continue;
			}

			RandomSource random = level.getRandom();

			for (int i = 0; i < ATTEMPTS; i++) {
				BlockPos spot = floorSpot(level, player.blockPosition(), random);

				if (spot != null && spawnGroup(level, spot, random) > 0) {
					break;
				}
			}
		}
	}

	private static List<Entity> centers(ServerLevel level) {
		return new ArrayList<>(level.players());
	}

	/** Eine Gruppe aus der Biom-Spawnliste rund um {@code spot}; gibt die Zahl der gesetzten Tiere zurück. */
	private static int spawnGroup(ServerLevel level, BlockPos spot, RandomSource random) {
		var list = level.getChunkSource().getGenerator().getMobsAt(level, level.structureManager(), MobCategory.CREATURE, spot);
		MobSpawnSettings.SpawnerData data = list.getRandom(random).orElse(null);

		if (data == null) {
			return 0;
		}

		EntityType<?> type = data.type();
		int wanted = Math.max(1, data.count().sample(random));
		int placed = 0;

		for (int tries = 0; tries < wanted * 4 && placed < wanted; tries++) {
			BlockPos pos = tries == 0 ? spot : nearbyFloor(level, spot.offset(random.nextInt(9) - 4, 0, random.nextInt(9) - 4));

			if (pos == null || !SpawnPlacements.isSpawnPositionOk(type, level, pos)
					|| !SpawnPlacements.checkSpawnRules(type, level, EntitySpawnReason.NATURAL, pos, random)
					|| !level.noCollision(type.getSpawnAABB(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5))) {
				continue;
			}

			Entity entity = type.create(level, EntitySpawnReason.NATURAL);

			if (!(entity instanceof Mob mob)) {
				continue;
			}

			mob.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, random.nextFloat() * 360.0f, 0.0f);
			mob.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), EntitySpawnReason.NATURAL, null);
			level.addFreshEntityWithPassengers(mob);
			placed++;
		}

		return placed;
	}

	/** Freie Stelle auf festem Boden, 24–56 Blöcke von {@code center}, in geladenen Chunks. */
	private static @Nullable BlockPos floorSpot(ServerLevel level, BlockPos center, RandomSource random) {
		double angle = random.nextDouble() * Math.PI * 2.0;
		int distance = 24 + random.nextInt(33);
		int x = center.getX() + (int) Math.round(Math.cos(angle) * distance);
		int z = center.getZ() + (int) Math.round(Math.sin(angle) * distance);
		int top = Math.min(level.getMaxY() - 2, center.getY() + 16);
		int bottom = Math.max(level.getMinY() + 1, center.getY() - 24);

		if (!level.hasChunkAt(new BlockPos(x, top, z))) {
			return null;
		}

		return scanDown(level, x, z, top, bottom);
	}

	/** Boden in der Nähe von {@code pos} (3 Blöcke hoch/runter). */
	private static @Nullable BlockPos nearbyFloor(ServerLevel level, BlockPos pos) {
		if (!level.hasChunkAt(pos)) {
			return null;
		}

		return scanDown(level, pos.getX(), pos.getZ(), pos.getY() + 3, pos.getY() - 3);
	}

	/** Von oben nach unten: erste Stelle mit zwei Blöcken Luft über einem festen Block. */
	private static @Nullable BlockPos scanDown(ServerLevel level, int x, int z, int top, int bottom) {
		BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos(x, top, z);
		boolean room = false;

		for (int y = top; y >= bottom; y--) {
			p.setY(y);
			BlockState state = level.getBlockState(p);

			if (state.isAir()) {
				room = room || level.getBlockState(p.above()).isAir();
			} else if (room && state.getFluidState().isEmpty() && state.isRedstoneConductor(level, p)) {
				return p.above().immutable();
			} else {
				room = false;
			}
		}

		return null;
	}
}
