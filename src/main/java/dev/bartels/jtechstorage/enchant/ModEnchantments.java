package dev.bartels.jtechstorage.enchant;

import java.util.List;
import java.util.Optional;

import org.jspecify.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;

import dev.bartels.jtechstorage.JTechStorage;

/**
 * Kristall-Verzauberungen (nur vom Amethyst-Fuchs – nicht am Verzauberungstisch, nicht beim Handel, nicht in Truhen):
 * Doppelsprung (Stiefel), Sprint (Beinschutz), Rückruf (Speicher-Schlüssel), Behutsamkeit (Spitzhacke),
 * Lebensfunke (Schwert/Axt). Die Daten liegen in {@code data/jtechstorage/enchantment/}, das Verhalten steht hier.
 */
public final class ModEnchantments {
	public static final ResourceKey<Enchantment> DOUBLE_JUMP = key("double_jump");
	public static final ResourceKey<Enchantment> SPRINT = key("sprint");
	public static final ResourceKey<Enchantment> RECALL = key("recall");
	public static final ResourceKey<Enchantment> CAREFUL_TOUCH = key("careful_touch");
	public static final ResourceKey<Enchantment> LIFE_SPARK = key("life_spark");
	public static final List<ResourceKey<Enchantment>> ALL = List.of(DOUBLE_JUMP, SPRINT, RECALL, CAREFUL_TOUCH, LIFE_SPARK);

	/** Mob-Typ eines mit Behutsamkeit abgebauten Spawners. */
	public static final DataComponentType<Identifier> SPAWNER_MOB = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,
			JTechStorage.id("spawner_mob"),
			DataComponentType.<Identifier>builder().persistent(Identifier.CODEC).networkSynchronized(Identifier.STREAM_CODEC).build());

	/** Tag (Spielzeit / 24000), an dem der Rückruf zuletzt benutzt wurde. */
	public static final DataComponentType<Long> RECALL_DAY = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,
			JTechStorage.id("recall_day"),
			DataComponentType.<Long>builder().persistent(com.mojang.serialization.Codec.LONG)
					.networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.VAR_LONG).build());

	private static final Identifier SPRINT_ID = JTechStorage.id("sprint_enchantment");

	private ModEnchantments() {
	}

	private static ResourceKey<Enchantment> key(String name) {
		return ResourceKey.create(Registries.ENCHANTMENT, JTechStorage.id(name));
	}

	public static Optional<Holder.Reference<Enchantment>> holder(Level level, ResourceKey<Enchantment> key) {
		return level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(key);
	}

	public static int level(Level level, ResourceKey<Enchantment> key, ItemStack stack) {
		if (stack.isEmpty()) {
			return 0;
		}

		return holder(level, key).map(h -> EnchantmentHelper.getItemEnchantmentLevel(h, stack)).orElse(0);
	}

	public static int level(LivingEntity entity, ResourceKey<Enchantment> key, EquipmentSlot slot) {
		return level(entity.level(), key, entity.getItemBySlot(slot));
	}

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(ModEnchantments::tick);
		ServerLivingEntityEvents.AFTER_DAMAGE.register(ModEnchantments::lifeSpark);
		PlayerBlockBreakEvents.AFTER.register(ModEnchantments::carefulTouch);
		UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
			ItemStack stack = player.getItemInHand(hand);

			if (!level.isClientSide() && stack.is(Items.SPAWNER) && stack.has(SPAWNER_MOB) && level instanceof ServerLevel serverLevel) {
				Identifier mob = stack.get(SPAWNER_MOB);
				BlockPos clicked = hit.getBlockPos();
				BlockPos beside = clicked.relative(hit.getDirection());
				// erst nach dem Platzieren (gleicher Tick, später in der Warteschlange) den Mob eintragen
				serverLevel.getServer().execute(() -> {
					applySpawnerMob(serverLevel, beside, mob);
					applySpawnerMob(serverLevel, clicked, mob);
				});
			}

			return InteractionResult.PASS;
		});
	}

	// ------------------------------------------------------------------
	// Sprint: schneller beim Sprinten (+12 % je Stufe), mit Kristallspur
	// ------------------------------------------------------------------

	private static void tick(MinecraftServer server) {
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			int sprint = level(player, SPRINT, EquipmentSlot.LEGS);
			AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);

			if (speed == null) {
				continue;
			}

			boolean active = sprint > 0 && player.isSprinting() && !player.isPassenger();

			if (active) {
				AttributeModifier modifier = new AttributeModifier(SPRINT_ID, 0.12 * sprint, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
				AttributeModifier current = speed.getModifier(SPRINT_ID);

				if (current == null || current.amount() != modifier.amount()) {
					speed.removeModifier(SPRINT_ID);
					speed.addTransientModifier(modifier);
				}

				if (player.tickCount % 3 == 0 && player.onGround()) {
					player.level().sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 0.1, player.getZ(), 1, 0.15, 0.0, 0.15, 0.0);
				}
			} else if (speed.hasModifier(SPRINT_ID)) {
				speed.removeModifier(SPRINT_ID);
			}
		}
	}

	// ------------------------------------------------------------------
	// Lebensfunke: Nahkampftreffer heilen (10 % des Schadens je Stufe, höchstens 2 Herzen pro Treffer)
	// ------------------------------------------------------------------

	private static void lifeSpark(LivingEntity victim, DamageSource source, float baseDamage, float damage, boolean blocked) {
		if (blocked || damage <= 0 || !(source.getEntity() instanceof Player player) || source.getDirectEntity() != player
				|| !(player.level() instanceof ServerLevel level) || victim == player) {
			return;
		}

		int spark = level(player, LIFE_SPARK, EquipmentSlot.MAINHAND);

		if (spark <= 0 || player.getHealth() >= player.getMaxHealth()) {
			return;
		}

		float heal = Math.min(4.0f, damage * 0.1f * spark);
		player.heal(heal);
		level.sendParticles(ParticleTypes.HEART, player.getX(), player.getY() + 1.9, player.getZ(), 1, 0.2, 0.1, 0.2, 0.0);
		level.sendParticles(ParticleTypes.WITCH, victim.getX(), victim.getY() + victim.getBbHeight() * 0.6, victim.getZ(), 6, 0.2, 0.2, 0.2, 0.05);
	}

	// ------------------------------------------------------------------
	// Behutsamkeit: Monster-Spawner (mit Mob) und knospender Amethyst lassen sich abbauen
	// ------------------------------------------------------------------

	private static void carefulTouch(Level level, Player player, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity) {
		if (!(level instanceof ServerLevel serverLevel) || player.isCreative()
				|| level(player, CAREFUL_TOUCH, EquipmentSlot.MAINHAND) <= 0) {
			return;
		}

		ItemStack drop = ItemStack.EMPTY;

		if (state.is(Blocks.BUDDING_AMETHYST)) {
			drop = new ItemStack(Items.BUDDING_AMETHYST);
		} else if (state.is(Blocks.SPAWNER)) {
			drop = new ItemStack(Items.SPAWNER);

			if (blockEntity instanceof SpawnerBlockEntity spawner) {
				Entity display = spawner.getSpawner().getOrCreateDisplayEntity(level, pos);

				if (display != null) {
					Identifier mob = BuiltInRegistries.ENTITY_TYPE.getKey(display.getType());
					drop.set(SPAWNER_MOB, mob);
					drop.set(DataComponents.LORE, new ItemLore(List.of(Component.translatable("jtechstorage.careful_touch.spawner",
							display.getType().getDescription()).withStyle(style -> style.withColor(ChatFormatting.LIGHT_PURPLE).withItalic(false)))));
				}
			}
		}

		if (drop.isEmpty()) {
			return;
		}

		net.minecraft.world.level.block.Block.popResource(serverLevel, pos, drop);
		serverLevel.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.AMETHYST_BLOCK.defaultBlockState()),
				pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 20, 0.3, 0.3, 0.3, 0.1);
		serverLevel.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.0f, 1.2f);
	}

	private static void applySpawnerMob(ServerLevel level, BlockPos pos, Identifier mob) {
		if (level.getBlockEntity(pos) instanceof SpawnerBlockEntity spawner) {
			Optional<EntityType<?>> type = BuiltInRegistries.ENTITY_TYPE.getOptional(mob);

			if (type.isPresent() && spawner.getSpawner().getOrCreateDisplayEntity(level, pos) == null) {
				spawner.setEntityId(type.get(), level.getRandom());
				spawner.setChanged();
				level.sendBlockUpdated(pos, spawner.getBlockState(), spawner.getBlockState(), 3);
			}
		}
	}

	// ------------------------------------------------------------------
	// Doppelsprung (Server-Seite): Fallhöhe zurücksetzen, Effekte
	// ------------------------------------------------------------------

	public static void handleDoubleJump(ServerPlayer player) {
		if (player.onGround() || level(player, DOUBLE_JUMP, EquipmentSlot.FEET) <= 0 || player.isPassenger()) {
			return;
		}

		player.resetFallDistance();
		ServerLevel level = player.level();
		level.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY(), player.getZ(), 10, 0.3, 0.05, 0.3, 0.03);
		level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.AMETHYST_BLOCK.defaultBlockState()),
				player.getX(), player.getY(), player.getZ(), 8, 0.3, 0.05, 0.3, 0.05);
		level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 0.6f, 1.6f);
	}

	// ------------------------------------------------------------------
	// Rückruf: einmal pro Minecraft-Tag zum gekoppelten Terminal teleportieren
	// ------------------------------------------------------------------

	/** @return true, wenn der Rückruf ausgeführt (oder mit Meldung abgelehnt) wurde. */
	public static boolean tryRecall(ServerPlayer player, ItemStack key, net.minecraft.core.GlobalPos terminal) {
		if (level(player.level(), RECALL, key) <= 0) {
			return false;
		}

		long day = player.level().getGameTime() / 24000L;
		Long last = key.get(RECALL_DAY);

		if (last != null && last >= day && !player.isCreative()) {
			long ticksLeft = (day + 1) * 24000L - player.level().getGameTime();
			player.sendOverlayMessage(Component.translatable("jtechstorage.recall.cooldown", (ticksLeft + 1199) / 1200).withStyle(ChatFormatting.RED));
			return true;
		}

		ServerLevel target = player.level().getServer().getLevel(terminal.dimension());

		if (target == null) {
			player.sendOverlayMessage(Component.translatable("jtechstorage.message.terminal_missing"));
			return true;
		}

		BlockPos pos = terminal.pos();
		ServerLevel from = player.level();
		from.sendParticles(ParticleTypes.REVERSE_PORTAL, player.getX(), player.getY() + 1, player.getZ(), 60, 0.4, 0.8, 0.4, 0.1);
		from.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0f, 1.3f);
		// auf den Block vor dem Terminal (oben drauf, falls dort nichts frei ist)
		BlockPos landing = pos.above();

		for (net.minecraft.core.Direction direction : net.minecraft.core.Direction.Plane.HORIZONTAL) {
			BlockPos candidate = pos.relative(direction);

			if (target.getBlockState(candidate).isAir() && target.getBlockState(candidate.above()).isAir()) {
				landing = candidate;
				break;
			}
		}

		player.teleportTo(target, landing.getX() + 0.5, landing.getY(), landing.getZ() + 0.5, java.util.Set.of(), player.getYRot(), player.getXRot(), true);
		player.resetFallDistance();
		key.set(RECALL_DAY, day);
		target.sendParticles(ParticleTypes.END_ROD, landing.getX() + 0.5, landing.getY() + 1, landing.getZ() + 0.5, 40, 0.4, 0.8, 0.4, 0.05);
		target.playSound(null, landing, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0f, 1.0f);
		player.sendOverlayMessage(Component.translatable("jtechstorage.recall.done").withStyle(ChatFormatting.LIGHT_PURPLE));
		return true;
	}
}
