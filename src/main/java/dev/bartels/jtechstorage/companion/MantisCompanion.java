package dev.bartels.jtechstorage.companion;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.Level;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import dev.bartels.jtechstorage.ModSounds;

/**
 * Schattenmantis (Angriff): kämpft an der Seite ihres Besitzers – greift an, was er angreift oder was ihn angreift, und
 * Monster in seiner Nähe. Stufe 2: mehr Schaden und Rückstoß, Stufe 3: Sensenhieb trifft auch Monster rund um das Ziel.
 */
public class MantisCompanion extends Companion {
	private static final double[] HEALTH = {20, 40, 70};
	private static final double[] SPEED = {0.32, 0.34, 0.36};
	private static final double[] ARMOR = {0, 4, 8};

	public MantisCompanion(EntityType<? extends MantisCompanion> type, Level level) {
		super(type, level);
		refreshStage();
	}

	public static AttributeSupplier.Builder createAttributes() {
		return createMobAttributes()
				.add(Attributes.MAX_HEALTH, HEALTH[0])
				.add(Attributes.MOVEMENT_SPEED, SPEED[0])
				.add(Attributes.ATTACK_DAMAGE, MANTIS_DAMAGE[0])
				.add(Attributes.ATTACK_KNOCKBACK, 0.0)
				.add(Attributes.ARMOR, ARMOR[0])
				.add(Attributes.FOLLOW_RANGE, 24.0)
				.add(Attributes.STEP_HEIGHT, 1.0);
	}

	@Override
	public String species() {
		return "mantis";
	}

	@Override
	protected void applyStageAttributes(int stage) {
		int k = stage - 1;
		setMaxHealthKeepRatio(HEALTH[k]);
		setBase(this, Attributes.MOVEMENT_SPEED, SPEED[k]);
		setBase(this, Attributes.ARMOR, ARMOR[k]);
		setBase(this, Attributes.ATTACK_KNOCKBACK, k * 0.6);
	}

	@Override
	protected float stageScale(int stage) {
		return stage == 1 ? 0.6f : stage == 2 ? 1.0f : 1.5f;
	}

	@Override
	protected SoundEvent hurtSound() {
		return ModSounds.MANTIS_HURT;
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return isOrderedToSit() ? null : ModSounds.MANTIS_AMBIENT;
	}

	@Override
	public int getAmbientSoundInterval() {
		return 360;
	}

	@Override
	protected void registerCompanionGoals() {
	}

	@Override
	protected int attackInterval() {
		return 26;
	}

	// ------------------------------------------------------------------
	// Talente (v0.26): Klinge, Panzer, Jagd
	// ------------------------------------------------------------------

	private int unbreakableCooldown;
	private int leapCooldown;

	@Override
	protected double damageBonus() {
		return 0.08 * talent("sharp_blades");
	}

	@Override
	protected double intervalFactor() {
		return 1.0 - 0.08 * talent("quick_strike");
	}

	@Override
	protected double critChance() {
		return 0.1 * talent("precision");
	}

	@Override
	protected double armorBonus() {
		return 2.0 * talent("chitin");
	}

	@Override
	protected double healthBonus() {
		return 0.1 * talent("crystal_plates");
	}

	@Override
	protected double thornsFraction() {
		return 0.15 * talent("thorns");
	}

	@Override
	protected double speedBonus() {
		return 0.07 * talent("swift_legs");
	}

	@Override
	protected double huntRadius() {
		return 16.0 + 4.0 * talent("hunt_instinct");
	}

	/** Unbeugsam: einmal alle 5 Minuten bleibt sie statt erschöpft mit halbem Leben stehen. */
	@Override
	protected boolean resistExhaustion(ServerLevel level) {
		if (talent("unbreakable") == 0 || unbreakableCooldown > 0) {
			return false;
		}

		unbreakableCooldown = 20 * 60 * 5;
		setHealth(getMaxHealth() * 0.5f);
		addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.RESISTANCE, 100, 1));
		level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, getX(), getY() + getBbHeight() * 0.5, getZ(), 30, 0.4, 0.6, 0.4, 0.3);
		playSound(ModSounds.COMPANION_EVOLVE, 0.8f, 1.6f);
		return true;
	}

	@Override
	public void aiStep() {
		super.aiStep();

		if (!(level() instanceof ServerLevel)) {
			return;
		}

		if (unbreakableCooldown > 0) {
			unbreakableCooldown--;
		}

		if (leapCooldown > 0) {
			leapCooldown--;
		}

		// Sprungangriff: springt Ziele aus 4–10 Blöcken Entfernung an
		LivingEntity target = getTarget();

		if (leapCooldown == 0 && target != null && talent("leap_attack") > 0 && onGround() && !isVehicle() && isActive()) {
			double distance = distanceTo(target);

			if (distance > 4.0 && distance < 10.0 && hasLineOfSight(target)) {
				Vec3 dir = target.position().subtract(position()).normalize();
				setDeltaMovement(dir.x * Math.min(1.4, distance * 0.16), 0.45, dir.z * Math.min(1.4, distance * 0.16));
				needsSync = true;
				leapCooldown = 20 * 4;
				triggerAttack();
				playSound(ModSounds.MANTIS_ATTACK, 0.8f, 0.7f);
			}
		}
	}

	/** Flugausdauer beim Reiten (Talent Kristallflug: +50 % je Rang). */
	public int maxFlight() {
		return (int) (MAX_FLIGHT * (1.0 + 0.5 * talent("crystal_flight")));
	}

	// ------------------------------------------------------------------
	// Reiten auf Stufe 3: laufen, mit der Sprungtaste kurz fliegen (Ausdauer ~4 s, lädt am Boden nach)
	// ------------------------------------------------------------------

	public static final int MAX_FLIGHT = 80;
	private int flightEnergy = MAX_FLIGHT;
	private boolean riddenFlight;

	@Override
	protected boolean canBeRidden() {
		return getStage() == 3 && !isExhausted();
	}

	@Override
	protected Vec3 seat() {
		return new Vec3(0.0, 1.12, -0.35);
	}

	/** Für die Animation: fliegt sie gerade mit Reiter? */
	public boolean isRiddenFlying() {
		return isVehicle() && !onGround();
	}

	@Override
	protected Vec3 getRiddenInput(Player player, Vec3 selfInput) {
		float forward = player.zza;
		float strafe = player.xxa * 0.5f;
		riddenFlight = player.isJumping() && flightEnergy > 0;

		if (onGround() && !riddenFlight) {
			flightEnergy = Math.min(maxFlight(), flightEnergy + 2);
		}

		if (!riddenFlight) {
			return new Vec3(strafe, 0, forward > 0 ? forward : forward * 0.35f);
		}

		flightEnergy--;
		float pitch = player.getXRot() * Mth.DEG_TO_RAD;
		double up = (forward != 0 ? -Mth.sin(pitch) * forward : 0) + 0.6;
		double horizontal = forward != 0 ? Mth.cos(pitch) * forward : 0;
		return new Vec3(strafe, up, horizontal);
	}

	@Override
	protected float getRiddenSpeed(Player player) {
		return riddenFlight ? 0.085f : (float) getAttributeValue(Attributes.MOVEMENT_SPEED) * 0.85f;
	}

	@Override
	public void travel(Vec3 input) {
		if (getControllingPassenger() != null && riddenFlight) {
			travelFlying(input, getSpeed());
			return;
		}

		super.travel(input);

		// nach dem Flug gleitet sie mit den Kristallflügeln sanft zu Boden
		if (getControllingPassenger() != null && !onGround() && getDeltaMovement().y < -0.16) {
			setDeltaMovement(getDeltaMovement().x, -0.16, getDeltaMovement().z);
		}
	}

	public int getFlightEnergy() {
		return Math.min(flightEnergy, maxFlight());
	}

	@Override
	protected double combatShare() {
		return 1.0;
	}

	@Override
	protected SoundEvent attackSound() {
		return ModSounds.MANTIS_ATTACK;
	}

	@Override
	public boolean doHurtTarget(ServerLevel level, Entity target) {
		boolean hit = super.doHurtTarget(level, target);

		if (hit && getStage() == 3) {
			// Sensenhieb: trifft auch Monster rund um das Ziel (Talent Wirbelsense: voller Schaden, größerer Kreis)
			boolean whirl = talent("whirl_scythe") > 0;
			float damage = (float) getAttributeValue(Attributes.ATTACK_DAMAGE) * (whirl ? 1.0f : 0.5f);
			level.sendParticles(ParticleTypes.SWEEP_ATTACK, target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(), 1, 0, 0, 0, 0);

			for (LivingEntity other : level.getEntitiesOfClass(LivingEntity.class, target.getBoundingBox().inflate(whirl ? 3.0 : 2.0),
					e -> e != target && e instanceof Enemy && e.isAlive())) {
				other.hurtServer(level, damageSources().mobAttack(this), damage);
				Vec3 push = other.position().subtract(position()).normalize().scale(0.5);
				other.push(push.x, 0.2, push.z);
			}
		}

		return hit;
	}
}
