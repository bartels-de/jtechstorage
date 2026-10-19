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

	/** In der eigenen Basis kämpft sie um die Hälfte stärker (v0.25.1). */
	@Override
	protected double baseDamageBonus() {
		return 0.5;
	}

	/** „Bleiben“ in der Basis = Wache: verteidigt die Basis auch ohne dich. */
	@Override
	protected boolean canGuardBase() {
		return true;
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
					e -> e != target && Companion.hostile(e))) {
				other.hurtServer(level, damageSources().mobAttack(this), damage);
				Vec3 push = other.position().subtract(position()).normalize().scale(0.5);
				other.push(push.x, 0.2, push.z);
			}
		}

		return hit;
	}

	// ------------------------------------------------------------------
	// v0.25.3: Meister-Talente und Spezialangriffe
	// ------------------------------------------------------------------

	private int bloodlustTicks;
	private int bloodlustStacks;
	private int stormTicks;

	@Override
	protected double intervalFactor() {
		double factor = 1.0 - 0.08 * talent("quick_strike");
		return bloodlustTicks > 0 ? factor * (1.0 - 0.15 * bloodlustStacks) : factor;
	}

	/** Blutrausch: jeder Sieg macht sie 5 s lang schneller (bis 3-fach). */
	/** Amulett (v0.25.4): +5 % Schaden je Material. */
	@Override
	protected double amuletDamage(int tier) {
		return 0.05 * tier;
	}

	@Override
	protected void onKill(ServerLevel level, LivingEntity target) {
		if (talent("bloodlust") > 0) {
			bloodlustStacks = Math.min(3, bloodlustStacks + 1);
			bloodlustTicks = 100;
			level.sendParticles(ParticleTypes.ANGRY_VILLAGER, getX(), getY() + getBbHeight(), getZ(), 2, 0.2, 0.1, 0.2, 0.0);
		}
	}

	/** Raubtier: +30 % Schaden gegen Ziele unter halbem Leben. */
	@Override
	protected double bonusDamageAgainst(LivingEntity target) {
		return talent("predator") > 0 && target.getHealth() < target.getMaxHealth() * 0.5f ? 0.3 : 0.0;
	}

	/** Kristallhaut: 20 % Chance, einen Treffer ganz abzuwehren. */
	@Override
	protected boolean blockDamage(ServerLevel level, net.minecraft.world.damagesource.DamageSource source) {
		return talent("crystal_skin") > 0 && random.nextFloat() < 0.2f;
	}

	@Override
	protected int specialBaseCooldown(int which) {
		return switch (which) {
			case 0 -> 20 * 60;       // Lähmender Stich
			case 1 -> 20 * 45;       // Klingensturm
			default -> 20 * 40;      // Hinrichtung
		};
	}

	@Override
	protected boolean specialNeedsTarget(int which) {
		return which != 1;
	}

	@Override
	protected boolean canUseSpecialNow() {
		return !isVehicle();
	}

	/** Springt zum Ziel (für Stich und Hinrichtung); false = kein Platz neben dem Ziel und zu weit weg. */
	private boolean lungeAt(ServerLevel level, LivingEntity target) {
		if (distanceToSqr(target) > 3.5 * 3.5) {
			Vec3 dir = position().subtract(target.position()).multiply(1, 0, 1);
			Vec3 at = target.position().add(dir.lengthSqr() > 0.01 ? dir.normalize().scale(1.4) : new Vec3(1.4, 0, 0));
			level.sendParticles(ParticleTypes.PORTAL, getX(), getY() + 0.5, getZ(), 20, 0.3, 0.4, 0.3, 0.2);

			if (!leapTo(level, new Vec3(at.x, target.getY(), at.z))) {
				return false;
			}
		}

		lookAt(target, 360.0f, 360.0f);
		setTarget(target);
		return true;
	}

	@Override
	protected boolean performSpecial(ServerLevel level, int which, net.minecraft.world.entity.@org.jspecify.annotations.Nullable LivingEntity target) {
		float power = specialPower();
		float base = (float) getAttributeValue(Attributes.ATTACK_DAMAGE);

		switch (which) {
			case 0 -> {
				// Lähmender Stich: 5 s betäubt, 30 s Gift
				if (target == null || target.distanceToSqr(this) > 20 * 20) {
					return false;
				}

				if (!lungeAt(level, target)) {
					return false;
				}

				target.hurtServer(level, damageSources().mobAttack(this), base * 1.5f * power);
				Companion.stun(level, target, (int) (100 * power));
				target.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.POISON, 20 * 30, power > 1.2f ? 1 : 0), this);
				level.sendParticles(ParticleTypes.ITEM_SLIME, target.getX(), target.getY() + target.getBbHeight() * 0.6, target.getZ(), 20, 0.3, 0.3, 0.3, 0.1);
				playSound(ModSounds.MANTIS_ATTACK, 1.2f, 0.6f);
				return true;
			}
			case 1 -> {
				// Klingensturm: drei Wirbel, jeder trifft alles in 4 Blöcken
				stormTicks = 30;
				playSound(ModSounds.MANTIS_ATTACK, 1.2f, 1.4f);
				return true;
			}
			default -> {
				// Hinrichtung: Sprung + gewaltiger Hieb, gegen geschwächte Ziele doppelt
				if (target == null || target.distanceToSqr(this) > 20 * 20) {
					return false;
				}

				if (!lungeAt(level, target)) {
					return false;
				}

				float damage = base * 2.5f * power * (target.getHealth() < target.getMaxHealth() * 0.35f ? 2.0f : 1.0f);
				target.hurtServer(level, damageSources().mobAttack(this), damage);
				level.sendParticles(ParticleTypes.SWEEP_ATTACK, target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(), 3, 0.3, 0.2, 0.3, 0);
				level.sendParticles(ParticleTypes.ENCHANTED_HIT, target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(), 25, 0.3, 0.4, 0.3, 0.2);
				playSound(ModSounds.MANTIS_ATTACK, 1.3f, 0.5f);
				return true;
			}
		}
	}

	@Override
	protected void tickSpecialEffects(ServerLevel level) {
		if (bloodlustTicks > 0 && --bloodlustTicks == 0) {
			bloodlustStacks = 0;
		}

		if (stormTicks > 0 && isExhausted()) {
			stormTicks = 0;
		}

		if (stormTicks > 0) {
			stormTicks--;
			setYRot(getYRot() + 36.0f);
			yBodyRot = getYRot();

			if (stormTicks % 10 == 0) {
				float damage = (float) getAttributeValue(Attributes.ATTACK_DAMAGE) * specialPower();
				triggerAttack();
				level.sendParticles(ParticleTypes.SWEEP_ATTACK, getX(), getY() + 0.8, getZ(), 6, 1.6, 0.2, 1.6, 0.0);

				for (LivingEntity other : level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(4.0),
						e -> Companion.hostile(e))) {
					other.hurtServer(level, damageSources().mobAttack(this), damage);
					Vec3 push = other.position().subtract(position()).normalize().scale(0.6);
					other.push(push.x, 0.25, push.z);
				}

				playSound(ModSounds.MANTIS_ATTACK, 0.9f, 1.2f + random.nextFloat() * 0.4f);
			}
		}
	}

	@Override
	protected void addAdditionalSaveData(net.minecraft.world.level.storage.ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putInt("Unbreakable", unbreakableCooldown);
	}

	@Override
	protected void readAdditionalSaveData(net.minecraft.world.level.storage.ValueInput input) {
		super.readAdditionalSaveData(input);
		unbreakableCooldown = input.getIntOr("Unbreakable", 0);
	}
}
