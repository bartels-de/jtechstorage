package dev.bartels.jtechstorage.companion;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

import dev.bartels.jtechstorage.ModSounds;

/**
 * Schattenrabe (v0.25.3): kurzlebiger Rabe aus dem Rabenschwarm des Nachtraben. Kreist kurz über dem Ziel, stürzt sich
 * darauf, trifft einmal und löst sich in Rauch auf. Kein Speichern, keine KI, nicht verwundbar.
 */
public class SwarmRaven extends Mob {
	private @Nullable LivingEntity target;
	private @Nullable Companion summoner;
	private float damage;
	private int delay;
	private int age;
	private boolean struck;
	private double angle;
	private int away;
	/** Späherkrähe (v0.26.3): kreist nur um den Besitzer und löst sich auf. */
	private boolean scout;

	public SwarmRaven(EntityType<? extends SwarmRaven> type, Level level) {
		super(type, level);
		noPhysics = true;
		setNoGravity(true);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 1.0);
	}

	/** Schickt einen Schattenraben los: erscheint über dem Ziel und stürzt nach {@code delay} Ticks herab. */
	public static void summon(ServerLevel level, Companion summoner, LivingEntity target, float damage, int delay) {
		SwarmRaven raven = CompanionContent.SWARM_RAVEN.create(level, net.minecraft.world.entity.EntitySpawnReason.EVENT);

		if (raven == null) {
			return;
		}

		raven.summoner = summoner;
		raven.target = target;
		raven.damage = damage;
		raven.delay = delay;
		raven.angle = level.getRandom().nextDouble() * Math.PI * 2;
		Vec3 start = target.position().add(Math.cos(raven.angle) * 4.0, 6.0 + level.getRandom().nextDouble() * 2.0, Math.sin(raven.angle) * 4.0);
		raven.snapTo(start.x, start.y, start.z, 0.0f, 0.0f);
		level.addFreshEntity(raven);
		level.sendParticles(ParticleTypes.LARGE_SMOKE, start.x, start.y, start.z, 8, 0.2, 0.2, 0.2, 0.02);
	}

	/** Späherkrähe: kreist 3 Sekunden um den Besitzer und verschwindet in Rauch. */
	public static void scout(ServerLevel level, Companion summoner, LivingEntity owner, double angle) {
		SwarmRaven raven = CompanionContent.SWARM_RAVEN.create(level, net.minecraft.world.entity.EntitySpawnReason.EVENT);

		if (raven == null) {
			return;
		}

		raven.summoner = summoner;
		raven.target = owner;
		raven.scout = true;
		raven.angle = angle;
		Vec3 start = summoner.position().add(0, 1.0, 0);
		raven.snapTo(start.x, start.y, start.z, 0.0f, 0.0f);
		level.addFreshEntity(raven);
		level.sendParticles(ParticleTypes.SMOKE, start.x, start.y, start.z, 6, 0.2, 0.2, 0.2, 0.02);
	}

	@Override
	public void tick() {
		super.tick();

		if (!(level() instanceof ServerLevel level)) {
			return;
		}

		age++;

		if (scout) {
			if (target == null || !target.isAlive() || age > 70) {
				vanish(level);
				return;
			}

			angle += 0.18;
			double radius = 3.0 + Math.sin(age * 0.1) * 0.6;
			Vec3 circle = target.position().add(Math.cos(angle) * radius, 3.0 + Math.sin(age * 0.2) * 0.4, Math.sin(angle) * radius);
			move(position().add(circle.subtract(position()).scale(0.3)));
			return;
		}

		if (target == null || summoner == null || age > 100) {
			vanish(level);
			return;
		}

		Vec3 pos = position();

		if (struck || !target.isAlive()) {
			// nach dem Treffer: hoch und weg
			move(pos.add(Math.cos(angle) * 0.5, 0.5, Math.sin(angle) * 0.5));

			if (++away > 14) {
				vanish(level);
			}

			return;
		}

		if (age < delay) {
			// kreist über dem Ziel
			angle += 0.25;
			Vec3 circle = target.position().add(Math.cos(angle) * 4.0, 6.0, Math.sin(angle) * 4.0);
			move(pos.add(circle.subtract(pos).scale(0.35)));
			return;
		}

		Vec3 aim = target.position().add(0, target.getBbHeight() * 0.6, 0);
		Vec3 dir = aim.subtract(pos);
		double dist = dir.length();

		if (dist < 1.0) {
			strike(level);
			return;
		}

		move(pos.add(dir.normalize().scale(Math.min(dist, 1.1))));

		if (age % 2 == 0) {
			level.sendParticles(ParticleTypes.SMOKE, getX(), getY() + 0.2, getZ(), 2, 0.05, 0.05, 0.05, 0.0);
		}
	}

	private void move(Vec3 to) {
		Vec3 delta = to.subtract(position());
		setDeltaMovement(delta);
		setPos(to.x, to.y, to.z);

		if (delta.horizontalDistanceSqr() > 1.0E-4) {
			float yaw = (float) (Math.atan2(delta.z, delta.x) * 180.0 / Math.PI) - 90.0f;
			setYRot(yaw);
			yBodyRot = yaw;
			yHeadRot = yaw;
		}
	}

	private void strike(ServerLevel level) {
		struck = true;

		if (target != null && summoner != null) {
			DamageSource source = summoner.damageSources().mobAttack(summoner);
			target.hurtServer(level, source, damage);
			Vec3 push = target.position().subtract(position()).multiply(1, 0, 1).normalize().scale(0.25);
			target.push(push.x, 0.15, push.z);
			level.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY() + target.getBbHeight() * 0.6, target.getZ(), 6, 0.2, 0.2, 0.2, 0.1);
			level.sendParticles(ParticleTypes.WITCH, target.getX(), target.getY() + target.getBbHeight() * 0.6, target.getZ(), 6, 0.2, 0.2, 0.2, 0.05);
			playSound(ModSounds.RAVEN_ATTACK, 0.7f, 1.2f + random.nextFloat() * 0.3f);
		}
	}

	private void vanish(ServerLevel level) {
		level.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 0.2, getZ(), 6, 0.15, 0.15, 0.15, 0.02);
		discard();
	}

	/** Die Bewegung kommt allein aus {@link #tick()} (vom Server gesetzt) – keine eigene Physik. */
	@Override
	public void travel(Vec3 input) {
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		return false;
	}

	@Override
	public boolean isPickable() {
		return false;
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
	protected void addAdditionalSaveData(ValueOutput output) {
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
	}
}
