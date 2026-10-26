package dev.bartels.jtechstorage.companion;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import dev.bartels.jtechstorage.ModSounds;

/**
 * Crow decoy (v0.26.4): the shadow image of the player left behind by the Night Raven's crow doppelganger.
 * Stands for 6 seconds, draws nearby monsters to itself and then dissolves into smoke and crows. Takes no damage.
 */
public class CrowDecoy extends Mob {
	public static final int LIFE = 120;
	/** Entity ID of the player (for skin and facing direction on the client). */
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

	/** Leaves a decoy at the player's position. */
	public static void spawn(ServerLevel level, ServerPlayer player) {
		CrowDecoy decoy = CompanionContent.CROW_DECOY.create(level, EntitySpawnReason.EVENT);

		if (decoy == null) {
			return;
		}

		decoy.entityData.set(DATA_PLAYER, player.getId());
		decoy.snapTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
		decoy.yBodyRot = player.yBodyRot;
		decoy.yHeadRot = player.getYHeadRot();
		level.addFreshEntity(decoy);

		// nearby monsters attack the decoy from now on
		for (Mob mob : level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(16.0), m -> m.getTarget() == player)) {
			mob.setTarget(decoy);
			mob.getBrain().setMemory(MemoryModuleType.ATTACK_TARGET, decoy);
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

		// whoever lost the decoy looks for it again (instead of the player)
		if (tickCount % 10 == 0) {
			for (Mob mob : level.getEntitiesOfClass(Mob.class, getBoundingBox().inflate(16.0), m -> m.getTarget() == player)) {
				mob.setTarget(this);
			}
		}
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		if (source.is(DamageTypes.GENERIC_KILL)) {
			return super.hurtServer(level, source, amount);
		}

		level.sendParticles(ParticleTypes.SMOKE, getX(), getY() + 1.0, getZ(), 8, 0.3, 0.5, 0.3, 0.02);
		playSound(ModSounds.RAVEN_HURT, 0.5f, 1.4f);
		return true;       // "hit" - but without damage
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
