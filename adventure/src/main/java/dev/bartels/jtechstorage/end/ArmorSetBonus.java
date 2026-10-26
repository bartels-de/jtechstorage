package dev.bartels.jtechstorage.end;

import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.Blocks;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import dev.bartels.jtechstorage.JTechStorage;
import net.minecraft.world.damagesource.DamageTypes;

/**
 * Set bonus of the full Amethyst Obsidian armor:
 * <ul>
 * <li>Stat bonus: +4 health (2 hearts), fire resistance, Haste I – as long as all four pieces are worn.</li>
 * <li>Resonance Thorns: whoever hits the wearer in melee takes crystal damage back and is slowed.</li>
 * </ul>
 */
public final class ArmorSetBonus {
	private static final Identifier HEALTH_ID = JTechStorage.id("amethyst_obsidian_set_health");
	private static final AttributeModifier HEALTH_BONUS = new AttributeModifier(HEALTH_ID, 4.0, AttributeModifier.Operation.ADD_VALUE);
	/** Reflected damage of the Resonance Thorns (+ 25 % of the damage taken). */
	private static final float THORNS_DAMAGE = 3.0f;
	private static final int EFFECT_TICKS = 260;

	private ArmorSetBonus() {
	}

	public static boolean hasFullSet(LivingEntity entity) {
		return entity.getItemBySlot(EquipmentSlot.HEAD).is(EndContent.AMETHYST_OBSIDIAN_HELMET)
				&& entity.getItemBySlot(EquipmentSlot.CHEST).is(EndContent.AMETHYST_OBSIDIAN_CHESTPLATE)
				&& entity.getItemBySlot(EquipmentSlot.LEGS).is(EndContent.AMETHYST_OBSIDIAN_LEGGINGS)
				&& entity.getItemBySlot(EquipmentSlot.FEET).is(EndContent.AMETHYST_OBSIDIAN_BOOTS);
	}

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(ArmorSetBonus::tick);
		ServerLivingEntityEvents.AFTER_DAMAGE.register(ArmorSetBonus::afterDamage);
	}

	private static void tick(MinecraftServer server) {
		if (server.getTickCount() % 10 != 0) {
			return;
		}

		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			boolean full = hasFullSet(player);
			AttributeInstance health = player.getAttribute(Attributes.MAX_HEALTH);

			if (health != null) {
				if (full && !health.hasModifier(HEALTH_ID)) {
					health.addTransientModifier(HEALTH_BONUS);
				} else if (!full && health.hasModifier(HEALTH_ID)) {
					health.removeModifier(HEALTH_ID);

					if (player.getHealth() > player.getMaxHealth()) {
						player.setHealth(player.getMaxHealth());
					}
				}
			}

			if (full) {
				refresh(player, new MobEffectInstance(MobEffects.FIRE_RESISTANCE, EFFECT_TICKS, 0, true, false, true));
				refresh(player, new MobEffectInstance(MobEffects.HASTE, EFFECT_TICKS, 0, true, false, true));
			}
		}
	}

	/** Only refresh the effect when it is about to expire (no flickering, stronger effects stay untouched). */
	private static void refresh(ServerPlayer player, MobEffectInstance effect) {
		MobEffectInstance current = player.getEffect(effect.getEffect());

		if (current == null || (current.getAmplifier() <= effect.getAmplifier() && current.getDuration() < EFFECT_TICKS - 40)) {
			player.addEffect(effect);
		}
	}

	private static void afterDamage(LivingEntity entity, DamageSource source, float baseDamage, float damage, boolean blocked) {
		if (blocked || damage <= 0 || !(entity.level() instanceof ServerLevel level) || !hasFullSet(entity)) {
			return;
		}

		// real melee only: direct source = attacker, and no thorns chain
		if (!(source.getDirectEntity() instanceof LivingEntity attacker) || source.getEntity() != attacker || attacker == entity
				|| source.is(DamageTypes.THORNS)) {
			return;
		}

		float reflected = THORNS_DAMAGE + damage * 0.25f;
		attacker.hurtServer(level, entity.damageSources().thorns(entity), reflected);
		attacker.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, 1));
		level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.AMETHYST_CLUSTER.defaultBlockState()),
				attacker.getX(), attacker.getY() + attacker.getBbHeight() * 0.6, attacker.getZ(), 12, 0.3, 0.3, 0.3, 0.12);
		level.sendParticles(ParticleTypes.END_ROD, entity.getX(), entity.getY() + 1.0, entity.getZ(), 6, 0.4, 0.5, 0.4, 0.04);
		level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0f, 1.3f);
	}
}
