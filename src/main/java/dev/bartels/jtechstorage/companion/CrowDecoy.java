package dev.bartels.jtechstorage.companion;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import dev.bartels.jtechstorage.ModSounds;

/**
 * Krähen-Abbild (v0.26.4): das Schatten-Abbild des Spielers, das beim Krähen-Doppelgänger des Nachtraben zurückbleibt.
 * Steht 6 Sekunden, zieht die Monster in der Nähe auf sich und zerfällt dann in Rauch und Krähen. Nimmt keinen Schaden.
 */
public class CrowDecoy extends Mob {
	public static final int LIFE = 120;
	/** Entity-ID des Spielers (für Skin und Blickrichtung im Client). */
	private static final EntityDataAccessor<Integer> DATA_PLAYER = SynchedEntityData.defineId(CrowDecoy.class, EntityDataSerializers.INT);

	public CrowDecoy(EntityType<? extends CrowDecoy> type, Level level) {
		super(type, level);
		setNoAi(true);
		setPersistenceRequired();
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 20.0).add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DATA_PLAYER, -1);
	}

	public int playerId() {
		return entityData.get(DATA_PLAYER);
	}

	/** Lässt an der Stelle des Spielers ein Abbild zurück. */
	public static void spawn(ServerLevel level, ServerPlayer player) {
		CrowDecoy decoy = CompanionContent.CROW_DECOY.create(level, net.minecraft.world.entity.EntitySpawnReason.EVENT);

		if (decoy == null) {
			return;
		}

		decoy.entityData.set(DATA_PLAYER, player.getId());
		decoy.snapTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
		decoy.yBodyRot = player.yBodyRot;
		decoy.yHeadRot = player.getYHeadRot();
		level.addFreshEntity(decoy);

		// Monster in der Nähe greifen ab jetzt das Abbild an
		for (Mob mob : level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(16.0), m -> m.getTarget() == player)) {
			mob.setTarget(decoy);
			mob.getBrain().setMemory(net.minecraft.world.entity.ai.memory.MemoryModuleType.ATTACK_TARGET, decoy);
		}
	}

	@Override
	public void tick() {
		super.tick();

		if (level().isClientSide()) {
			if (random.nextInt(3) == 0) {
				level().addParticle(ParticleTypes.WITCH, getRandomX(0.6), getY() + random.nextDouble() * 1.8, getRandomZ(0.6), 0.0, 0.02, 0.0);
			}

			return;
		}

		if (!(level() instanceof ServerLevel level)) {
			return;
		}

		if (tickCount >= LIFE || !(level.getEntity(playerId()) instanceof ServerPlayer player) || !player.isAlive()) {
			level.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 1.0, getZ(), 30, 0.4, 0.8, 0.4, 0.04);
			level.sendParticles(ParticleTypes.SQUID_INK, getX(), getY() + 1.0, getZ(), 20, 0.4, 0.8, 0.4, 0.06);
			playSound(ModSounds.RAVEN_DEPART, 1.0f, 1.2f);
			discard();
			return;
		}

		// wer das Abbild verloren hat, sucht es wieder (statt des Spielers)
		if (tickCount % 10 == 0) {
			for (Mob mob : level.getEntitiesOfClass(Mob.class, getBoundingBox().inflate(16.0), m -> m.getTarget() == player)) {
				mob.setTarget(this);
			}
		}
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		if (source.is(net.minecraft.world.damagesource.DamageTypes.GENERIC_KILL)) {
			return super.hurtServer(level, source, amount);
		}

		level.sendParticles(ParticleTypes.SMOKE, getX(), getY() + 1.0, getZ(), 8, 0.3, 0.5, 0.3, 0.02);
		playSound(ModSounds.RAVEN_HURT, 0.5f, 1.4f);
		return true;       // „getroffen“ – aber ohne Schaden
	}

	@Override
	public boolean isPushable() {
		return false;
	}

	@Override
	protected void doPush(Entity entity) {
	}

	@Override
	public boolean shouldBeSaved() {
		return false;
	}

	@Override
	public boolean removeWhenFarAway(double distance) {
		return false;
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
	}
}
