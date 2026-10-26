package dev.bartels.jtechstorage.enchant;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

import dev.bartels.jtechstorage.JTechStorage;
import dev.bartels.jtechstorage.block.StorageTerminalBlockEntity;
import dev.bartels.jtechstorage.item.LinkedTerminalItem;
import dev.bartels.jtechstorage.network.StorageNetwork;
import java.util.ArrayList;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.enchantment.Enchantment;
import dev.bartels.jtechstorage.mixin.AbstractArrowAccessor;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.level.gamerules.GameRules;

/**
 * Twelve more crystal enchantments (v0.42, likewise only from the Amethyst Fox). Long Arm is purely data-driven
 * (attribute in the JSON), Nourishing and Soulbound each need a mixin, the rest is here.
 */
public final class MoreEnchantments {
	private static final Identifier CRYSTAL_SHIELD_ID = JTechStorage.id("crystal_shield");
	/** Ticks without damage until the Crystal Shield builds up. */
	private static final int SHIELD_DELAY = 200;

	private static final Map<UUID, Long> LAST_HURT = new HashMap<>();
	private static final Map<UUID, Restock> RESTOCK = new HashMap<>();
	private static final Map<LivingEntity, Bleed> BLEEDING = new WeakHashMap<>();
	private static final Map<LivingEntity, int[]> RESONANCE = new WeakHashMap<>();
	private static final Map<AbstractArrow, Double> WIND = new WeakHashMap<>();
	private static boolean whirling;
	/** Resonance bonus damage as an echo half a second later (before that, invulnerability after the hit blocks it). */
	private static final List<PendingHit> PENDING = new ArrayList<>();

	private record PendingHit(LivingEntity victim, Entity direct, Entity owner, float amount, long due) {
	}

	private record Restock(int slot, ItemStack prototype, int count) {
	}

	private static final class Bleed {
		int ticks;
		int level;
		Entity attacker;
	}

	private MoreEnchantments() {
	}

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(MoreEnchantments::tick);
		ServerLivingEntityEvents.ALLOW_DAMAGE.register(MoreEnchantments::allowDamage);
		ServerLivingEntityEvents.AFTER_DAMAGE.register(MoreEnchantments::afterDamage);
		ServerLivingEntityEvents.AFTER_DEATH.register(MoreEnchantments::afterDeath);
		ServerEntityEvents.ENTITY_LOAD.register(MoreEnchantments::onLoad);
		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> SoulboundStash.giveBack(newPlayer));
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> server.execute(() -> {
			if (handler.player.isAlive()) {
				SoulboundStash.giveBack(handler.player);
			}
		}));
	}

	private static int lvl(LivingEntity entity, ResourceKey<Enchantment> key,
			EquipmentSlot slot) {
		return ModEnchantments.level(entity, key, slot);
	}

	// ------------------------------------------------------------------
	// Tick: Crystal Shield, Resupply, Crystal Poison, Wind Arrow
	// ------------------------------------------------------------------

	private static void tick(MinecraftServer server) {
		long now = server.overworld().getGameTime();

		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			crystalShield(player, now);
			restock(player);
		}

		bleedTick();
		windTick();
		pendingTick();
	}

	/** Crystal Shield: after 10 s without damage, 2/4/6 absorption hearts build up (1 health point per second). */
	private static void crystalShield(ServerPlayer player, long now) {
		int level = lvl(player, ModEnchantments.CRYSTAL_SHIELD, EquipmentSlot.CHEST);
		AttributeInstance max = player.getAttribute(Attributes.MAX_ABSORPTION);

		if (max == null) {
			return;
		}

		if (level <= 0) {
			if (max.hasModifier(CRYSTAL_SHIELD_ID)) {
				max.removeModifier(CRYSTAL_SHIELD_ID);
				player.setAbsorptionAmount(Math.min(player.getAbsorptionAmount(), player.getMaxAbsorption()));
			}

			return;
		}

		double cap = 4.0 * level;
		AttributeModifier current = max.getModifier(CRYSTAL_SHIELD_ID);

		if (current == null || current.amount() != cap) {
			max.removeModifier(CRYSTAL_SHIELD_ID);
			max.addTransientModifier(new AttributeModifier(CRYSTAL_SHIELD_ID, cap, AttributeModifier.Operation.ADD_VALUE));
		}

		if (player.tickCount % 20 != 0 || now - LAST_HURT.getOrDefault(player.getUUID(), 0L) < SHIELD_DELAY
				|| player.getAbsorptionAmount() >= cap) {
			return;
		}

		player.setAbsorptionAmount((float) Math.min(cap, player.getAbsorptionAmount() + 1.0f));
		player.level().sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.AMETHYST_BLOCK.defaultBlockState()),
				player.getX(), player.getY() + 1.1, player.getZ(), 4, 0.35, 0.4, 0.35, 0.02);

		if (player.getAbsorptionAmount() >= cap) {
			player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 0.7f, 1.5f);
		}
	}

	/** Resupply: when the stack in hand runs out, it is refilled from the inventory or from storage. */
	private static void restock(ServerPlayer player) {
		Inventory inventory = player.getInventory();
		int slot = inventory.getSelectedSlot();
		ItemStack held = inventory.getSelectedItem();
		Restock last = RESTOCK.get(player.getUUID());

		if (!held.isEmpty()) {
			RESTOCK.put(player.getUUID(), new Restock(slot, held.copyWithCount(1), held.getCount()));
			return;
		}

		RESTOCK.remove(player.getUUID());

		if (last == null || last.slot() != slot || last.count() > 2 || last.prototype().getMaxStackSize() <= 1 || player.isCreative()
				|| player.containerMenu != player.inventoryMenu || player.isSpectator()) {
			return;
		}

		ItemStack key = restockKey(player);

		if (key.isEmpty()) {
			return;
		}

		ItemStack wanted = last.prototype();

		// first from your own inventory
		for (int i = 0; i < Inventory.INVENTORY_SIZE; i++) {
			ItemStack other = inventory.getItem(i);

			if (i != slot && ItemStack.isSameItemSameComponents(other, wanted)) {
				inventory.setItem(slot, other);
				inventory.setItem(i, ItemStack.EMPTY);
				restocked(player);
				return;
			}
		}

		// then from storage (linked key within radio range)
		StorageTerminalBlockEntity terminal = LinkedTerminalItem.quietTerminal(key, player.level(), player);

		if (terminal == null) {
			return;
		}

		StorageNetwork network = terminal.buildNetwork();
		ItemStack got = network.extract(wanted.copyWithCount(1), wanted.getMaxStackSize());

		if (!got.isEmpty()) {
			inventory.setItem(slot, got);
			restocked(player);
		}
	}

	private static void restocked(ServerPlayer player) {
		player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.5f, 1.6f);
		RESTOCK.put(player.getUUID(), new Restock(player.getInventory().getSelectedSlot(), player.getInventory().getSelectedItem().copyWithCount(1),
				player.getInventory().getSelectedItem().getCount()));
	}

	private static ItemStack restockKey(Player player) {
		Inventory inventory = player.getInventory();

		for (int i = 0; i < inventory.getContainerSize(); i++) {
			ItemStack stack = inventory.getItem(i);

			if (!stack.isEmpty() && ModEnchantments.level(player.level(), ModEnchantments.RESTOCK, stack) > 0) {
				return stack;
			}
		}

		return ItemStack.EMPTY;
	}

	/** Crystal Poison: 3 s of bleeding, 1/2/3 damage per second. */
	private static void bleedTick() {
		Iterator<Map.Entry<LivingEntity, Bleed>> it = BLEEDING.entrySet().iterator();

		while (it.hasNext()) {
			Map.Entry<LivingEntity, Bleed> entry = it.next();
			LivingEntity victim = entry.getKey();
			Bleed bleed = entry.getValue();

			if (!victim.isAlive() || !(victim.level() instanceof ServerLevel level) || --bleed.ticks < 0) {
				it.remove();
				continue;
			}

			if (bleed.ticks % 20 == 0) {
				int invulnerable = victim.getInvulnerableTime();
				victim.setInvulnerableTime(0);
				victim.hurtServer(level, level.damageSources().indirectMagic(bleed.attacker, bleed.attacker), bleed.level);
				victim.setInvulnerableTime(Math.max(victim.getInvulnerableTime(), invulnerable));
				level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.AMETHYST_CLUSTER.defaultBlockState()),
						victim.getX(), victim.getY() + victim.getBbHeight() * 0.6, victim.getZ(), 6, 0.25, 0.3, 0.25, 0.05);
			}
		}
	}

	private static void pendingTick() {
		if (PENDING.isEmpty()) {
			return;
		}

		List<PendingHit> hits = List.copyOf(PENDING);
		PENDING.clear();

		for (PendingHit hit : hits) {
			LivingEntity victim = hit.victim();

			if (victim.level().getGameTime() < hit.due()) {
				PENDING.add(hit);
				continue;
			}

			if (victim.isAlive() && victim.level() instanceof ServerLevel level) {
				victim.setInvulnerableTime(0);
				victim.hurtServer(level, level.damageSources().indirectMagic(hit.owner(), hit.owner()), hit.amount());
				level.sendParticles(ParticleTypes.END_ROD, victim.getX(), victim.getY() + victim.getBbHeight() * 0.6, victim.getZ(), 16, 0.3, 0.3, 0.3, 0.12);
				level.playSound(null, victim.getX(), victim.getY(), victim.getZ(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.2f, 0.7f);
			}
		}
	}

	/** Wind Arrow: arrows drop less (30/50/70 % less gravity). */
	private static void windTick() {
		Iterator<Map.Entry<AbstractArrow, Double>> it = WIND.entrySet().iterator();

		while (it.hasNext()) {
			Map.Entry<AbstractArrow, Double> entry = it.next();
			AbstractArrow arrow = entry.getKey();

			if (arrow.isRemoved() || arrow.tickCount > 200 || ((AbstractArrowAccessor) arrow).jtechstorage$inGround()) {
				it.remove();
				continue;
			}

			arrow.setDeltaMovement(arrow.getDeltaMovement().add(0.0, 0.05 * entry.getValue(), 0.0));

			if (arrow.tickCount % 2 == 0 && arrow.level() instanceof ServerLevel level) {
				level.sendParticles(ParticleTypes.CLOUD, arrow.getX(), arrow.getY(), arrow.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
			}
		}
	}

	// ------------------------------------------------------------------
	// Arrows on launch
	// ------------------------------------------------------------------

	private static void onLoad(Entity entity, ServerLevel level) {
		if (!(entity instanceof AbstractArrow arrow) || arrow.tickCount > 0 || !(arrow.getOwner() instanceof Player)
				|| ((AbstractArrowAccessor) arrow).jtechstorage$inGround()) {
			return;
		}

		ItemStack weapon = arrow.getWeaponItem();
		int wind = weapon == null ? 0 : ModEnchantments.level(level, ModEnchantments.WIND_ARROW, weapon);

		if (wind > 0 && !WIND.containsKey(arrow)) {
			// faster, but same damage (arrow damage grows with speed)
			double factor = 1.0 + 0.15 * wind;
			arrow.setDeltaMovement(arrow.getDeltaMovement().scale(factor));
			arrow.setBaseDamage(2.0 / factor);
			WIND.put(arrow, 0.1 + 0.2 * wind);
		}
	}

	// ------------------------------------------------------------------
	// Damage
	// ------------------------------------------------------------------

	/** Arrow Ward: 20/40/60 % chance that projectiles bounce off. */
	private static boolean allowDamage(LivingEntity victim, DamageSource source, float amount) {
		if (!(victim instanceof Player player) || !source.is(DamageTypeTags.IS_PROJECTILE) || !(victim.level() instanceof ServerLevel level)) {
			return true;
		}

		int ward = lvl(player, ModEnchantments.ARROW_WARD, EquipmentSlot.HEAD);

		if (ward <= 0 || level.getRandom().nextFloat() >= 0.2f * ward) {
			return true;
		}

		if (source.getDirectEntity() instanceof Projectile projectile) {
			projectile.setDeltaMovement(projectile.getDeltaMovement().scale(-0.25));
		}

		level.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getEyeY(), player.getZ(), 8, 0.3, 0.3, 0.3, 0.05);
		level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.AMETHYST_BLOCK_HIT, SoundSource.PLAYERS, 1.0f, 1.4f);
		return false;
	}

	private static void afterDamage(LivingEntity victim, DamageSource source, float baseDamage, float damage, boolean blocked) {
		if (victim instanceof Player hurt && damage > 0) {
			LAST_HURT.put(hurt.getUUID(), hurt.level().getServer().overworld().getGameTime());
		}

		if (blocked || damage <= 0 || !(victim.level() instanceof ServerLevel level)) {
			return;
		}

		// arrows from the player's bow/crossbow
		if (source.getDirectEntity() instanceof AbstractArrow arrow && source.getEntity() instanceof Player shooter && arrow.getWeaponItem() != null) {
			ItemStack weapon = arrow.getWeaponItem();
			int binding = ModEnchantments.level(level, ModEnchantments.BINDING_ARROW, weapon);

			if (binding > 0) {
				victim.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 20 + 20 * binding, 2), shooter);
				level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.AMETHYST_CLUSTER.defaultBlockState()),
						victim.getX(), victim.getY() + 0.2, victim.getZ(), 14, 0.4, 0.1, 0.4, 0.02);
			}

			int resonance = ModEnchantments.level(level, ModEnchantments.RESONANCE_ARROW, weapon);

			if (resonance > 0) {
				int[] state = RESONANCE.computeIfAbsent(victim, v -> new int[2]);
				int now = (int) level.getGameTime();

				if (now - state[1] > 200) {
					state[0] = 0;
				}

				state[1] = now;

				if (++state[0] >= 3) {
					state[0] = 0;
					PENDING.add(new PendingHit(victim, arrow, shooter, damage * (0.25f + 0.25f * resonance), level.getGameTime() + 10));
				} else {
					level.playSound(null, victim.getX(), victim.getY(), victim.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.8f,
							1.0f + 0.3f * state[0]);
				}
			}

			return;
		}

		// melee
		if (!source.is(DamageTypes.PLAYER_ATTACK) || !(source.getEntity() instanceof Player player) || source.getDirectEntity() != player
				|| victim == player) {
			return;
		}

		int venom = lvl(player, ModEnchantments.CRYSTAL_VENOM, EquipmentSlot.MAINHAND);

		if (venom > 0) {
			Bleed bleed = BLEEDING.computeIfAbsent(victim, v -> new Bleed());
			bleed.ticks = 60;
			bleed.level = venom;
			bleed.attacker = player;
		}

		int whirl = lvl(player, ModEnchantments.WHIRLWIND, EquipmentSlot.MAINHAND);

		// fully charged hit (damage ≥ 85 % of the attack value) while sneaking
		if (whirl > 0 && !whirling && player.isShiftKeyDown() && damage >= player.getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.85) {
			whirling = true;

			try {
				double radius = 1.5 + whirl;
				float share = damage * (0.25f + 0.25f * whirl);

				for (LivingEntity other : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(radius, 1.0, radius),
						e -> e != player && e != victim && e instanceof Enemy && e.isAlive())) {
					other.hurtServer(level, level.damageSources().playerAttack(player), share);
					Vec3 push = other.position().subtract(player.position()).normalize().scale(0.4);
					other.push(push.x, 0.15, push.z);
				}
			} finally {
				whirling = false;
			}

			for (int i = 0; i < 16; i++) {
				double a = i * Math.PI / 8.0;
				level.sendParticles(ParticleTypes.SWEEP_ATTACK, player.getX() + Math.cos(a) * 2.0, player.getY() + 0.9, player.getZ() + Math.sin(a) * 2.0,
						1, 0.0, 0.0, 0.0, 0.0);
			}

			level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0f, 0.8f);
		}
	}

	// ------------------------------------------------------------------
	// Beheading
	// ------------------------------------------------------------------

	private static void afterDeath(LivingEntity victim, DamageSource source) {
		BLEEDING.remove(victim);

		if (!(source.getEntity() instanceof Player player) || source.getDirectEntity() != player || !(victim.level() instanceof ServerLevel level)) {
			return;
		}

		int beheading = lvl(player, ModEnchantments.BEHEADING, EquipmentSlot.MAINHAND);

		if (beheading <= 0 || level.getRandom().nextFloat() >= 0.05f * beheading) {
			return;
		}

		ItemStack head = head(victim);

		if (!head.isEmpty()) {
			victim.spawnAtLocation(level, head);
			level.sendParticles(ParticleTypes.END_ROD, victim.getX(), victim.getEyeY(), victim.getZ(), 10, 0.2, 0.2, 0.2, 0.05);
		}
	}

	private static ItemStack head(LivingEntity victim) {
		EntityType<?> type = victim.getType();

		if (victim instanceof Player player) {
			ItemStack head = new ItemStack(Items.PLAYER_HEAD);
			head.set(DataComponents.PROFILE, ResolvableProfile.createResolved(player.getGameProfile()));
			return head;
		}

		Map<EntityType<?>, Item> heads = Map.of(EntityTypes.ZOMBIE, Items.ZOMBIE_HEAD, EntityTypes.SKELETON, Items.SKELETON_SKULL,
				EntityTypes.CREEPER, Items.CREEPER_HEAD, EntityTypes.WITHER_SKELETON, Items.WITHER_SKELETON_SKULL, EntityTypes.PIGLIN, Items.PIGLIN_HEAD);
		Item item = heads.get(type);
		return item == null ? ItemStack.EMPTY : new ItemStack(item);
	}

	// ------------------------------------------------------------------
	// Soulbound (called by the mixin in Player#dropEquipment)
	// ------------------------------------------------------------------

	/** Takes all items with Soulbound out of the inventory, lowers the level by one and sets them aside for the respawn. */
	public static void keepSoulbound(ServerPlayer player) {
		if (player.level().getGameRules().get(GameRules.KEEP_INVENTORY)) {
			return;
		}

		var holder = ModEnchantments.holder(player.level(), ModEnchantments.SOULBOUND).orElse(null);

		if (holder == null) {
			return;
		}

		Inventory inventory = player.getInventory();
		List<ItemStack> kept = new ArrayList<>();

		for (int i = 0; i < inventory.getContainerSize(); i++) {
			ItemStack stack = inventory.getItem(i);
			int level = stack.isEmpty() ? 0 : EnchantmentHelper.getItemEnchantmentLevel(holder, stack);

			if (level > 0) {
				ItemStack copy = stack.copy();
				EnchantmentHelper.updateEnchantments(copy, mutable -> mutable.set(holder, level - 1));
				kept.add(copy);
				inventory.setItem(i, ItemStack.EMPTY);
			}
		}

		if (!kept.isEmpty()) {
			SoulboundStash.get(player.level().getServer()).put(player.getUUID(), kept);
		}
	}

	/** For Nourishing (from the mixin in Player#causeFoodExhaustion). */
	public static float nourish(Player player, float exhaustion) {
		if (player.level().isClientSide()) {
			return exhaustion;
		}

		int level = lvl(player, ModEnchantments.NOURISHING, EquipmentSlot.HEAD);
		return level > 0 ? exhaustion * (1.0f - 0.2f * level) : exhaustion;
	}
}
