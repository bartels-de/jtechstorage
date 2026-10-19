package dev.bartels.jtechstorage.companion;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import dev.bartels.jtechstorage.ModSounds;

/**
 * Amethyst-Pilz (Regeneration): heilt seinen Besitzer mit leuchtenden Sporen.
 * <ul>
 *     <li>Stufe 1: Regeneration I, sobald der Besitzer 2 Herzen verloren hat (alle 30 s).</li>
 *     <li>Stufe 2: zusätzlich 2 Herzen sofort, auch für andere Begleiter und eigene Tiere in der Nähe (alle 20 s).</li>
 *     <li>Stufe 3: 3 Herzen sofort + Regeneration II, entfernt Gift und Verdorren, heilt alle Spieler in der Nähe
 *     (alle 15 s). Notfall-Heilung bei wenig Leben (alle 60 s).</li>
 * </ul>
 */
public class ShroomCompanion extends Companion {
	private static final double[] HEALTH = {20, 30, 45};
	private static final double[] SPEED = {0.26, 0.28, 0.3};
	private static final int[] COOLDOWN = {30 * 20, 20 * 20, 15 * 20};
	private static final DustParticleOptions SPORE = new DustParticleOptions(0xD4B4FF, 1.0f);
	private static final DustParticleOptions SPORE_GREEN = new DustParticleOptions(0x9CF08A, 0.9f);

	private int cooldown;
	private int emergencyCooldown;
	private boolean autoHeal = true;

	public ShroomCompanion(EntityType<? extends ShroomCompanion> type, Level level) {
		super(type, level);
		refreshStage();
	}

	public static AttributeSupplier.Builder createAttributes() {
		return createMobAttributes()
				.add(Attributes.MAX_HEALTH, HEALTH[0])
				.add(Attributes.MOVEMENT_SPEED, SPEED[0])
				.add(Attributes.ATTACK_DAMAGE, MANTIS_DAMAGE[0] * 0.25)
				.add(Attributes.FOLLOW_RANGE, 24.0)
				.add(Attributes.STEP_HEIGHT, 1.0);
	}

	@Override
	public String species() {
		return "shroom";
	}

	@Override
	protected void applyStageAttributes(int stage) {
		setMaxHealthKeepRatio(HEALTH[stage - 1]);
		setBase(this, Attributes.MOVEMENT_SPEED, SPEED[stage - 1]);
	}

	@Override
	protected float stageScale(int stage) {
		return stage == 1 ? 0.7f : stage == 2 ? 1.0f : 1.35f;
	}

	@Override
	protected SoundEvent hurtSound() {
		return ModSounds.SHROOM_HURT;
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return ModSounds.SHROOM_AMBIENT;
	}

	@Override
	public int getAmbientSoundInterval() {
		return 420;
	}

	@Override
	protected int attackInterval() {
		return 40;
	}

	// ------------------------------------------------------------------
	// v0.25.1: Schulter (Stufe 1), Kampf erst ab Stufe 2, Wachstum in der Basis (ab Stufe 2)
	// ------------------------------------------------------------------

	@Override
	protected boolean shoulderStage() {
		return getStage() == 1;
	}

	@Override
	protected boolean canFight() {
		return getStage() >= 2;
	}

	/** Pflanzen der Basis (Cache, alle 30 s neu gesucht). */
	private final List<net.minecraft.core.BlockPos> crops = new ArrayList<>();
	private int cropScan;

	private static boolean isCrop(net.minecraft.world.level.block.state.BlockState state) {
		net.minecraft.world.level.block.Block block = state.getBlock();
		return state.isRandomlyTicking() && (block instanceof net.minecraft.world.level.block.CropBlock
				|| block instanceof net.minecraft.world.level.block.StemBlock || block instanceof net.minecraft.world.level.block.SweetBerryBushBlock
				|| block instanceof net.minecraft.world.level.block.CocoaBlock || block instanceof net.minecraft.world.level.block.NetherWartBlock
				|| block instanceof net.minecraft.world.level.block.SaplingBlock || block instanceof net.minecraft.world.level.block.SugarCaneBlock
				|| block instanceof net.minecraft.world.level.block.CactusBlock || block instanceof net.minecraft.world.level.block.BambooStalkBlock
				|| block instanceof net.minecraft.world.level.block.PitcherCropBlock);
	}

	/** Lässt die Pflanzen der Basis schneller wachsen: zusätzliche Zufalls-Ticks (Stufe 2 ≈ 3×, Stufe 3 ≈ 4,5× so schnell). */
	private void growCrops(ServerLevel level) {
		net.minecraft.core.BlockPos terminal = baseTerminal();

		if (terminal == null || getStage() < 2 || isExhausted()) {
			crops.clear();
			return;
		}

		if (--cropScan <= 0 || crops.isEmpty() && cropScan % 5 == 0) {
			cropScan = 30;
			crops.clear();
			int cx = terminal.getX() >> 4;
			int cz = terminal.getZ() >> 4;
			net.minecraft.core.BlockPos.MutableBlockPos pos = new net.minecraft.core.BlockPos.MutableBlockPos();

			for (int x = (cx - 1) << 4; x < (cx + 2) << 4; x++) {
				for (int z = (cz - 1) << 4; z < (cz + 2) << 4; z++) {
					for (int y = terminal.getY() - 12; y <= terminal.getY() + 12; y++) {
						pos.set(x, y, z);

						if (level.isLoaded(pos) && isCrop(level.getBlockState(pos))) {
							crops.add(pos.immutable());
						}
					}
				}
			}
		}

		double chance = getStage() == 2 ? 0.03 : 0.05;

		for (net.minecraft.core.BlockPos pos : crops) {
			if (random.nextDouble() < chance) {
				net.minecraft.world.level.block.state.BlockState state = level.getBlockState(pos);

				if (isCrop(state)) {
					state.randomTick(level, pos, random);

					if (random.nextInt(4) == 0) {
						level.sendParticles(SPORE_GREEN, pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 0.5, 3, 0.25, 0.2, 0.25, 0.0);
					}
				}
			}
		}
	}

	@Override
	protected double combatShare() {
		return 0.25;
	}

	@Override
	protected SoundEvent attackSound() {
		return ModSounds.SHROOM_ATTACK;
	}

	// ------------------------------------------------------------------
	// Talente (v0.26): Heilung, Schutz, Gift
	// ------------------------------------------------------------------

	@Override
	protected double healthBonus() {
		return 0.1 * talent("tough_stalk");
	}

	@Override
	protected double damageBonus() {
		return 0.15 * talent("spore_burst");
	}

	private int cooldownTicks(int stage) {
		return (int) (COOLDOWN[stage - 1] * (1.0 - 0.12 * talent("fast_bloom")));
	}

	private double healRange() {
		return 12.0 + 3.0 * talent("wide_spores");
	}

	@Override
	protected void onTalentHit(ServerLevel level, LivingEntity target) {
		int toxic = talent("toxic_spores");
		int numb = talent("numbing_spores");

		if (toxic > 0) {
			target.addEffect(new MobEffectInstance(MobEffects.POISON, 40 * toxic, 0), this);
		}

		if (numb > 0) {
			target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40 * numb, 0), this);
		}

		if (toxic > 0 || numb > 0) {
			level.sendParticles(SPORE_GREEN, target.getX(), target.getY() + target.getBbHeight() * 0.6, target.getZ(), 8, 0.3, 0.3, 0.3, 0.02);
		}
	}

	@Override
	public void sendStatus(Player player) {
		super.sendStatus(player);
		player.sendSystemMessage(Component.translatable(autoHeal ? "jtechstorage.shroom.status_auto" : "jtechstorage.shroom.status_manual",
				cooldown > 0 ? String.format(java.util.Locale.ROOT, "%.0f", Math.ceil(cooldown / 20.0)) : "0").withStyle(ChatFormatting.GRAY));
	}

	@Override
	protected void registerCompanionGoals() {
		// heilt im aiStep – keine eigenen Bewegungsziele
	}

	@Override
	public void aiStep() {
		super.aiStep();

		if (!(level() instanceof ServerLevel level)) {
			if (tickCount % 8 == 0 && !isExhausted()) {
				// leuchtende Sporen steigen vom Hut auf
				level().addParticle(SPORE, getX() + (random.nextDouble() - 0.5) * getBbWidth(), getY() + getBbHeight(),
						getZ() + (random.nextDouble() - 0.5) * getBbWidth(), 0, 0.02, 0);
			}

			return;
		}

		int ticks = veilTicks > 0 ? 2 : 1;      // Sporenschleier: Abklingzeiten laufen doppelt so schnell
		cooldown = Math.max(0, cooldown - ticks);
		emergencyCooldown = Math.max(0, emergencyCooldown - ticks);

		if (secondLifeCooldown > 0) {
			secondLifeCooldown--;
		}

		if (tickCount % 20 == 0) {
			growCrops(level);
		}

		capstones(level);

		double range = healRange();

		if (tickCount % 10 != 0 || isExhausted() || !(getOwner() instanceof Player owner) || owner.level() != level()
				|| owner.distanceToSqr(this) > range * range || !owner.isAlive()) {
			return;
		}

		int stage = getStage();
		boolean lifesaver = talent("lifesaver") > 0;

		// Notfall-Heilung (Stufe 3) – mit dem Talent Lebensretter auch im Tasten-Modus und doppelt so oft
		if (stage == 3 && (autoHeal || lifesaver) && emergencyCooldown == 0 && owner.getHealth() < owner.getMaxHealth() * 0.3f) {
			emergencyCooldown = (lifesaver ? 30 : 60) * 20;
			healTarget(level, owner, 8.0f, stage);
			level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, owner.getX(), owner.getY() + 1.0, owner.getZ(), 20, 0.4, 0.6, 0.4, 0.2);
			return;
		}

		if (autoHeal && cooldown == 0) {
			heal(level, owner, false);
		}
	}

	/** Wer braucht Heilung? Manuell reicht schon ein halbes Herz. */
	private List<LivingEntity> patients(ServerLevel level, Player owner, boolean manual) {
		int stage = getStage();
		List<LivingEntity> patients = new ArrayList<>();
		float missing = manual ? 0.5f : stage == 1 ? 4.0f : 2.0f;

		if (owner.getHealth() <= owner.getMaxHealth() - missing || stage == 3 && hasBadEffect(owner)) {
			patients.add(owner);
		}

		if (stage >= 2) {
			for (LivingEntity other : level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(8.0 + 3.0 * talent("wide_spores")),
					e -> e != owner && e.isAlive() && e.getHealth() < e.getMaxHealth() - (manual ? 0.5f : 2.0f) && isFriend(e, owner, stage))) {
				patients.add(other);
			}
		}

		return patients;
	}

	/** Heilt alle Patienten; true = geheilt (Abklingzeit läuft). */
	private boolean heal(ServerLevel level, Player owner, boolean manual) {
		List<LivingEntity> patients = patients(level, owner, manual);

		if (patients.isEmpty()) {
			return false;
		}

		int stage = getStage();
		cooldown = cooldownTicks(stage);
		triggerAction();
		playSound(ModSounds.SHROOM_HEAL, 1.0f, 0.9f + random.nextFloat() * 0.2f);
		level.sendParticles(SPORE, getX(), getY() + getBbHeight(), getZ(), 25, 0.5, 0.4, 0.5, 0.02);

		for (LivingEntity patient : patients) {
			healTarget(level, patient, stage == 1 ? (manual ? 2.0f : 0.0f) : stage == 2 ? 4.0f : 6.0f, stage);
		}

		// Giftwolke: Monster rundherum bekommen die giftige Seite der Sporen ab
		if (talent("toxic_cloud") > 0) {
			for (LivingEntity enemy : level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(5.0),
					e -> e instanceof net.minecraft.world.entity.monster.Enemy && e.isAlive())) {
				enemy.addEffect(new MobEffectInstance(MobEffects.POISON, 80, 1), this);
				level.sendParticles(SPORE_GREEN, enemy.getX(), enemy.getY() + enemy.getBbHeight() * 0.5, enemy.getZ(), 10, 0.3, 0.4, 0.3, 0.02);
			}
		}

		return true;
	}

	/** Hotkey (Standard H): jetzt heilen – mit Abklingzeit. */
	public void manualHeal(ServerPlayer player) {
		if (!(level() instanceof ServerLevel level)) {
			return;
		}

		if (isExhausted()) {
			player.sendOverlayMessage(Component.translatable("jtechstorage.shroom.exhausted", getDisplayName()).withStyle(ChatFormatting.RED));
			return;
		}

		if (player.level() != level || player.distanceToSqr(this) > healRange() * healRange()) {
			player.sendOverlayMessage(Component.translatable("jtechstorage.shroom.too_far", getDisplayName()).withStyle(ChatFormatting.GOLD));
			return;
		}

		if (cooldown > 0) {
			player.sendOverlayMessage(Component.translatable("jtechstorage.shroom.cooldown",
					String.format(java.util.Locale.ROOT, "%.1f", cooldown / 20.0)).withStyle(ChatFormatting.GRAY));
			return;
		}

		if (!heal(level, player, true)) {
			player.sendOverlayMessage(Component.translatable("jtechstorage.shroom.nobody").withStyle(ChatFormatting.GRAY));
		}
	}

	/** Shift + Hotkey: automatisch heilen oder nur per Hotkey. */
	public void toggleAutoHeal(ServerPlayer player) {
		autoHeal = !autoHeal;
		playSound(ModSounds.COMPANION_COMMAND, 0.8f, autoHeal ? 1.2f : 0.8f);
		player.sendOverlayMessage(Component.translatable(autoHeal ? "jtechstorage.shroom.mode_auto" : "jtechstorage.shroom.mode_manual", getDisplayName())
				.withStyle(ChatFormatting.LIGHT_PURPLE));
	}

	public boolean isAutoHeal() {
		return autoHeal;
	}

	public int healCooldown() {
		return cooldown;
	}

	/** Volle Abklingzeit auf der aktuellen Stufe (für die Anzeige). */
	public int healCooldownMax() {
		return cooldownTicks(getStage());
	}

	private static boolean hasBadEffect(LivingEntity entity) {
		return entity.hasEffect(MobEffects.POISON) || entity.hasEffect(MobEffects.WITHER);
	}

	private boolean isFriend(LivingEntity entity, Player owner, int stage) {
		if (entity instanceof Companion) {
			return true;
		}

		if (entity instanceof TamableAnimal pet && pet.isOwnedBy(owner)) {
			return true;
		}

		return stage == 3 && entity instanceof Player;
	}

	private void healTarget(ServerLevel level, LivingEntity target, float instant, int stage) {
		if (instant > 0) {
			target.heal(instant * (1.0f + 0.15f * talent("strong_spores")));
		}

		target.addEffect(new MobEffectInstance(MobEffects.REGENERATION, stage == 1 ? 120 : stage == 2 ? 160 : 120, stage == 3 ? 1 : 0));

		if (stage == 3) {
			target.removeEffect(MobEffects.POISON);
			target.removeEffect(MobEffects.WITHER);
		}

		int shield = talent("spore_shield");

		if (shield > 0) {
			target.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 200, shield - 1));
		}

		int calm = talent("calm_aura");

		if (calm > 0) {
			target.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 100 * calm, 0));
		}

		if (talent("purify") > 0) {
			// Reinigung: alle schädlichen Effekte weg
			for (var effect : List.copyOf(target.getActiveEffects())) {
				if (effect.getEffect().value().getCategory() == net.minecraft.world.effect.MobEffectCategory.HARMFUL) {
					target.removeEffect(effect.getEffect());
				}
			}
		}

		level.sendParticles(ParticleTypes.HEART, target.getX(), target.getY() + target.getBbHeight() + 0.2, target.getZ(), 3, 0.3, 0.2, 0.3, 0.0);
		level.sendParticles(SPORE_GREEN, target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(), 15, 0.4, 0.5, 0.4, 0.02);
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putInt("HealCooldown", cooldown);
		output.putBoolean("AutoHeal", autoHeal);
		output.putInt("SecondLife", secondLifeCooldown);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		cooldown = input.getIntOr("HealCooldown", 0);
		autoHeal = input.getBooleanOr("AutoHeal", true);
		secondLifeCooldown = input.getIntOr("SecondLife", 0);
	}

	// ------------------------------------------------------------------
	// v0.25.3: Meister-Talente und Spezialangriffe
	// ------------------------------------------------------------------

	private int veilTicks;
	private int bloomTicks;
	private net.minecraft.world.phys.Vec3 bloomCenter = net.minecraft.world.phys.Vec3.ZERO;
	private int secondLifeCooldown;

	private void capstones(ServerLevel level) {
		if (isExhausted() || !(getOwner() instanceof net.minecraft.server.level.ServerPlayer owner) || owner.level() != level) {
			return;
		}

		// Pilzhaut: Resistenz I für dich, solange er in der Nähe ist
		if (talent("mushroom_skin") > 0 && tickCount % 40 == 0 && owner.distanceToSqr(this) < 12 * 12) {
			owner.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 60, 0, true, false, true));
		}

		// Sporenfeld: Monster rund um ihn werden alle 5 s vergiftet
		if (talent("spore_field") > 0 && tickCount % 100 == 0 && getStage() >= 2) {
			for (LivingEntity enemy : level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(4.0),
					e -> e instanceof net.minecraft.world.entity.monster.Enemy && e.isAlive())) {
				enemy.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 0), this);
				level.sendParticles(SPORE_GREEN, enemy.getX(), enemy.getY() + enemy.getBbHeight() * 0.5, enemy.getZ(), 6, 0.3, 0.3, 0.3, 0.01);
			}
		}
	}

	/** Zweites Leben: rettet dich einmal alle 10 Minuten vor dem Tod. */
	public boolean trySecondLife(net.minecraft.server.level.ServerPlayer owner) {
		if (talent("second_life") == 0 || secondLifeCooldown > 0 || isExhausted() || owner.level() != level() || owner.distanceToSqr(this) > 32 * 32
				|| !(level() instanceof ServerLevel level)) {
			return false;
		}

		secondLifeCooldown = 20 * 60 * 10;
		owner.setHealth(8.0f);
		owner.removeAllEffects();
		owner.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 20 * 10, 1));
		owner.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 20 * 10, 1));
		level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, owner.getX(), owner.getY() + 1.0, owner.getZ(), 40, 0.5, 0.8, 0.5, 0.3);
		level.sendParticles(SPORE, owner.getX(), owner.getY() + 1.0, owner.getZ(), 30, 0.6, 0.8, 0.6, 0.05);
		playSound(ModSounds.SHROOM_HEAL, 1.5f, 0.7f);
		owner.sendSystemMessage(Component.translatable("jtechstorage.shroom.second_life", getDisplayName()).withStyle(ChatFormatting.LIGHT_PURPLE));
		return true;
	}

	@Override
	protected int specialBaseCooldown(int which) {
		return switch (which) {
			case 0 -> 20 * 180;      // Sporenschleier
			case 1 -> 20 * 120;      // Lebensblüte
			default -> 20 * 90;      // Lähmsporen-Wolke
		};
	}

	@Override
	protected boolean specialNeedsTarget(int which) {
		return false;
	}

	@Override
	protected boolean performSpecial(ServerLevel level, int which, net.minecraft.world.entity.@org.jspecify.annotations.Nullable LivingEntity target) {
		float power = specialPower();
		triggerAction();

		switch (which) {
			case 0 -> {
				// Sporenschleier: 60 s unsichtbar (du und er), Abklingzeiten halbiert und laufen doppelt so schnell
				veilTicks = 20 * 60;
				cooldown /= 2;
				emergencyCooldown /= 2;
				addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, veilTicks, 0, false, false));

				if (getOwner() instanceof Player owner) {
					owner.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, veilTicks, 0, false, true));
					level.sendParticles(SPORE, owner.getX(), owner.getY() + 1.0, owner.getZ(), 40, 0.6, 0.9, 0.6, 0.03);

					for (net.minecraft.world.entity.Mob mob : level.getEntitiesOfClass(net.minecraft.world.entity.Mob.class, owner.getBoundingBox().inflate(24.0),
							m -> m.getTarget() == owner || m.getTarget() == this)) {
						mob.setTarget(null);     // verlieren dich im Sporennebel aus den Augen
					}
				}

				playSound(ModSounds.SHROOM_HEAL, 1.2f, 0.6f);
				return true;
			}
			case 1 -> {
				// Lebensblüte: 10 s Heilkreis
				bloomTicks = 200;
				bloomCenter = position();
				playSound(ModSounds.SHROOM_HEAL, 1.2f, 1.3f);
				return true;
			}
			default -> {
				// Lähmsporen-Wolke um das Ziel (oder um ihn)
				net.minecraft.world.phys.Vec3 at = target != null ? target.position() : position();
				level.sendParticles(SPORE_GREEN, at.x, at.y + 1.0, at.z, 80, 2.5, 1.0, 2.5, 0.02);
				level.sendParticles(ParticleTypes.SNEEZE, at.x, at.y + 0.5, at.z, 30, 2.0, 0.6, 2.0, 0.01);

				for (LivingEntity enemy : level.getEntitiesOfClass(LivingEntity.class, new net.minecraft.world.phys.AABB(at, at).inflate(5.0),
						e -> e instanceof net.minecraft.world.entity.monster.Enemy && e.isAlive())) {
					enemy.hurtServer(level, damageSources().mobAttack(this), 4.0f * power);
					enemy.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 160, 2), this);
					enemy.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 160, 1), this);
					enemy.addEffect(new MobEffectInstance(MobEffects.POISON, 160, 0), this);
				}

				playSound(ModSounds.SHROOM_ATTACK, 1.2f, 0.7f);
				return true;
			}
		}
	}

	@Override
	protected void tickSpecialEffects(ServerLevel level) {
		if (veilTicks > 0) {
			veilTicks--;
		}

		if (bloomTicks > 0) {
			bloomTicks--;

			if (bloomTicks % 5 == 0) {
				level.sendParticles(SPORE_GREEN, bloomCenter.x, bloomCenter.y + 0.3, bloomCenter.z, 10, 3.0, 0.2, 3.0, 0.0);
				level.sendParticles(SPORE, bloomCenter.x, bloomCenter.y + 1.0, bloomCenter.z, 6, 2.5, 0.8, 2.5, 0.01);
			}

			if (bloomTicks % 20 == 0 && getOwner() instanceof Player owner) {
				for (LivingEntity friend : level.getEntitiesOfClass(LivingEntity.class,
						new net.minecraft.world.phys.AABB(bloomCenter, bloomCenter).inflate(6.0, 3.0, 6.0),
						e -> e.isAlive() && (e == owner || e instanceof Player || e instanceof Companion
								|| e instanceof net.minecraft.world.entity.TamableAnimal pet && pet.isOwnedBy(owner)))) {
					friend.heal(2.0f * specialPower());
					friend.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 0));
					level.sendParticles(ParticleTypes.HEART, friend.getX(), friend.getY() + friend.getBbHeight() + 0.2, friend.getZ(), 1, 0.2, 0.1, 0.2, 0.0);
				}
			}
		}
	}

	/** Für die Anzeige: läuft gerade der Sporenschleier? */
	public boolean isVeiled() {
		return veilTicks > 0;
	}
}
