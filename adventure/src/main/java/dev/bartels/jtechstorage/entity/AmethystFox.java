package dev.bartels.jtechstorage.entity;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Prediction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

import dev.bartels.jtechstorage.ModConfig;
import dev.bartels.jtechstorage.ModSounds;
import dev.bartels.jtechstorage.ModAdvancements;
import dev.bartels.jtechstorage.block.StorageTerminalBlockEntity;
import dev.bartels.jtechstorage.enchant.ModEnchantments;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import java.util.ArrayList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import dev.bartels.jtechstorage.adventure.AdventureContent;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Amethyst Fox – treasure hunter. Roams around its home, sniffs and digs up an enchanted crystal book roughly every
 * three Minecraft days (Double Jump, Sprint, Recall, Gentle Touch, Life Spark – available nowhere else).
 * Glow Berries/Sweet Berries shorten the wait, as does Resonance at the terminal. If it is linked, the book goes
 * into storage, otherwise it drops it in front of itself.
 */
public class AmethystFox extends NetworkHelperMob {
	/** Wait between two finds (game-time ticks, 3 days). */
	public static final long DIG_INTERVAL = Math.round(ModConfig.get().helpers.foxDigIntervalDays * 24000.0);
	public static final int DIG_TICKS = 70;
	private static final int ROAM = 10;
	private static final int MAX_FEEDINGS_PER_DAY = ModConfig.get().helpers.foxMaxFeedingsPerDay;

	private static final EntityDataAccessor<Integer> DATA_DIG = SynchedEntityData.defineId(AmethystFox.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Boolean> DATA_READY = SynchedEntityData.defineId(AmethystFox.class, EntityDataSerializers.BOOLEAN);

	private @Nullable BlockPos home;
	private long nextDig;
	private long feedDay = -1;
	private int feedings;

	public AmethystFox(EntityType<? extends AmethystFox> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return createMobAttributes()
				.add(Attributes.MAX_HEALTH, 20.0)
				.add(Attributes.MOVEMENT_SPEED, 0.28)
				.add(Attributes.FOLLOW_RANGE, 16.0);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DATA_DIG, 0);
		builder.define(DATA_READY, false);
	}

	/** Remaining dig ticks (for the animation). */
	public int getDigTicks() {
		return entityData.get(DATA_DIG);
	}

	/** Currently sensing a treasure (tail crystals light up). */
	public boolean isReady() {
		return entityData.get(DATA_READY);
	}

	@Override
	protected String unlinkedStatus() {
		return "jtechstorage.fox.status.unlinked";
	}

	@Override
	protected String linkAdvancement() {
		return "amethyst_fox";
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(1, new DigGoal());
		goalSelector.addGoal(3, new RoamGoal());
		goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 8.0f));
		goalSelector.addGoal(6, new RandomLookAroundGoal(this));
	}

	// ------------------------------------------------------------------
	// Flow
	// ------------------------------------------------------------------

	@Override
	public void aiStep() {
		super.aiStep();

		if (!(level() instanceof ServerLevel level) || isHologram()) {
			if (level().isClientSide() && !isHologram()) {
				clientEffects();
			}

			return;
		}

		if (home == null) {
			home = blockPosition();
		}

		long now = level.getGameTime();

		if (nextDig == 0) {
			nextDig = now + DIG_INTERVAL;
		}

		int dig = getDigTicks();

		if (dig > 0) {
			entityData.set(DATA_DIG, dig - 1);
			BlockState below = level.getBlockState(blockPosition().below());

			if (dig % 4 == 0 && !below.isAir()) {
				level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, below), getX(), getY() + 0.1, getZ(), 6, 0.2, 0.05, 0.2, 0.08);
			}

			if (dig % 10 == 0) {
				playSound(below.getSoundType().getHitSound(), 0.4f, 1.2f);
			}

			if (dig % 18 == 0) {
				playSound(ModSounds.FOX_DIG, 0.8f, 0.95f + random.nextFloat() * 0.1f);
			}

			if (dig == 1) {
				findBook(level);
			}
		}

		if (tickCount % 20 == 9) {
			entityData.set(DATA_READY, now >= nextDig);
			updateStatus(now);
		}
	}

	private void clientEffects() {
		if (isResting()) {
			return;
		}

		if (random.nextInt(isReady() ? 3 : 12) == 0) {
			// crystal dust from the tail
			float yaw = yBodyRot * Mth.DEG_TO_RAD;
			level().addParticle(isReady() ? ParticleTypes.END_ROD : ParticleTypes.WITCH,
					getX() + Mth.sin(yaw) * 0.6 + (random.nextDouble() - 0.5) * 0.4, getY() + 0.5 + random.nextDouble() * 0.3,
					getZ() - Mth.cos(yaw) * 0.6 + (random.nextDouble() - 0.5) * 0.4, 0.0, 0.01, 0.0);
		}
	}

	private long interval() {
		return (long) (DIG_INTERVAL / Math.max(1.0, speedFactor()));
	}

	private void updateStatus(long now) {
		long left = Math.max(0, nextDig - now);

		if (getDigTicks() > 0) {
			setStatus("jtechstorage.fox.status.digging");
		} else if (left == 0) {
			setStatus("jtechstorage.fox.status.sniffing");
		} else {
			long hours = (left + 999) / 1000;
			setStatus("jtechstorage.fox.status.waiting", hours / 24, hours % 24);
		}
	}

	private boolean canDigHere() {
		BlockState below = level().getBlockState(blockPosition().below());
		return below.is(BlockTags.DIRT) || below.is(BlockTags.SAND) || below.is(Blocks.GRAVEL)
				|| below.is(BlockTags.BASE_STONE_OVERWORLD) || below.is(Blocks.MOSS_BLOCK);
	}

	/** Random crystal enchantment, low levels more common. */
	private ItemStack rollBook(ServerLevel level) {
		List<ResourceKeyHolder> options = new ArrayList<>();

		for (var key : ModEnchantments.ALL) {
			ModEnchantments.holder(level, key).ifPresent(h -> options.add(new ResourceKeyHolder(h)));
		}

		if (options.isEmpty()) {
			return ItemStack.EMPTY;
		}

		Holder<Enchantment> enchantment = options.get(random.nextInt(options.size())).holder();
		int max = enchantment.value().getMaxLevel();
		int roll = random.nextInt(100);
		int lvl = roll < 60 ? 1 : roll < 90 ? 2 : 3;
		return EnchantmentHelper.createBook(new EnchantmentInstance(enchantment, Math.min(max, lvl)));
	}

	private record ResourceKeyHolder(Holder<Enchantment> holder) {
	}

	private void findBook(ServerLevel level) {
		nextDig = level.getGameTime() + interval();
		ItemStack book = rollBook(level);

		if (book.isEmpty()) {
			return;
		}

		Component name = book.getOrDefault(DataComponents.STORED_ENCHANTMENTS,
				ItemEnchantments.EMPTY).entrySet().stream().findFirst()
				.map(e -> Enchantment.getFullname(e.getKey(), e.getIntValue())).orElse(book.getHoverName());
		level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, getX(), getY() + 0.6, getZ(), 25, 0.3, 0.4, 0.3, 0.3);
		level.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 0.6, getZ(), 15, 0.3, 0.4, 0.3, 0.08);
		playSound(ModSounds.FOX_FOUND, 1.2f, 1.0f);

		StorageTerminalBlockEntity terminal = terminal();
		boolean stored = false;

		if (terminal != null) {
			ItemStack rest = terminal.buildNetwork().insert(book.copy());
			stored = rest.isEmpty();
		}

		if (!stored) {
			spawnAtLocation(level, book);
		}

		Component message = Component.translatable(stored ? "jtechstorage.fox.found.stored" : "jtechstorage.fox.found.dropped",
				getDisplayName(), name).withStyle(ChatFormatting.LIGHT_PURPLE);
		ServerPlayer ownerPlayer = owner == null ? null : level.getServer().getPlayerList().getPlayer(owner);

		if (ownerPlayer != null) {
			ownerPlayer.sendSystemMessage(message);
			ModAdvancements.grant(ownerPlayer, "crystal_enchantment");
		}

		for (ServerPlayer player : level.players()) {
			if (player != ownerPlayer && player.distanceToSqr(this) < 16 * 16) {
				player.sendSystemMessage(message);
			}
		}
	}

	// ------------------------------------------------------------------
	// Interaction
	// ------------------------------------------------------------------

	@Override
	protected InteractionResult interactWith(Player player, InteractionHand hand) {
		ItemStack held = player.getItemInHand(hand);

		if (held.is(Items.GLOW_BERRIES) || held.is(Items.SWEET_BERRIES)) {
			long day = level().getGameTime() / 24000L;

			if (day != feedDay) {
				feedDay = day;
				feedings = 0;
			}

			if (feedings >= MAX_FEEDINGS_PER_DAY) {
				player.sendOverlayMessage(Component.translatable("jtechstorage.fox.full"));
				return InteractionResult.FAIL;
			}

			feedings++;
			nextDig -= held.is(Items.GLOW_BERRIES) ? 6000 : 3000;
			held.consume(1, player);
			playSound(SoundEvents.FOX_EAT, 1.0f, 1.0f);

			if (level() instanceof ServerLevel level) {
				level.sendParticles(ParticleTypes.HEART, getX(), getY() + 0.8, getZ(), 3, 0.3, 0.2, 0.3, 0.0);
				updateStatus(level.getGameTime());
			}

			player.sendOverlayMessage(statusText());
			return InteractionResult.SUCCESS;
		}

		if (held.isEmpty() && player.isSecondaryUseActive() && hand == InteractionHand.MAIN_HAND) {
			player.getInventory().placeItemBackInInventory(createPackedItem(AdventureContent.AMETHYST_FOX_ITEM, player), Prediction.SERVER_ONLY);
			playSound(SoundEvents.FOX_SNIFF, 1.0f, 1.2f);
			discard();
			return InteractionResult.SUCCESS;
		}

		return super.interactWith(player, hand);
	}

	@Override
	protected void onLinked(Player player) {
		home = blockPosition();
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		return false;
	}

	@Override
	protected SoundEvent helperSleepSound() {
		return ModSounds.FOX_SLEEP;
	}

	@Override
	protected SoundEvent helperAmbientSound() {
		return ModSounds.FOX_AMBIENT;
	}


	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putLong("NextDig", nextDig);
		output.putLong("FeedDay", feedDay);
		output.putInt("Feedings", feedings);

		if (home != null) {
			output.store("Home", BlockPos.CODEC, home);
		}
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		nextDig = input.getLongOr("NextDig", 0L);
		feedDay = input.getLongOr("FeedDay", -1L);
		feedings = input.getIntOr("Feedings", 0);
		home = input.read("Home", BlockPos.CODEC).orElse(null);
	}

	// ------------------------------------------------------------------
	// Goals
	// ------------------------------------------------------------------

	/** When it is time (during the day), it looks for soft ground nearby and digs. */
	private class DigGoal extends Goal {
		private @Nullable BlockPos spot;
		private int searchTicks;

		DigGoal() {
			setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
		}

		@Override
		public boolean canUse() {
			return !isResting() && getDigTicks() == 0 && level().getGameTime() >= nextDig && nextDig > 0 && onGround();
		}

		@Override
		public boolean canContinueToUse() {
			return getDigTicks() > 0 || (spot != null && searchTicks > 0 && !isResting());
		}

		@Override
		public void start() {
			searchTicks = 200;
			spot = null;

			for (int attempt = 0; attempt < 12 && spot == null; attempt++) {
				BlockPos base = home != null ? home : blockPosition();
				BlockPos candidate = base.offset(random.nextInt(ROAM * 2 + 1) - ROAM, 0, random.nextInt(ROAM * 2 + 1) - ROAM);
				candidate = level().getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, candidate);
				BlockState ground = level().getBlockState(candidate.below());

				if (ground.is(BlockTags.DIRT) || ground.is(BlockTags.SAND) || ground.is(Blocks.GRAVEL)) {
					spot = candidate;
				}
			}

			if (spot == null) {
				spot = blockPosition();
			}

			getNavigation().moveTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, 1.1);
			playSound(SoundEvents.FOX_SNIFF, 1.0f, 1.2f);
		}

		@Override
		public void tick() {
			if (getDigTicks() > 0 || spot == null) {
				getNavigation().stop();
				return;
			}

			searchTicks--;

			if (spot.distToCenterSqr(position()) < 2.0 || getNavigation().isDone() || searchTicks <= 0) {
				if (canDigHere() || searchTicks <= 0) {
					getNavigation().stop();
					entityData.set(DATA_DIG, DIG_TICKS);
					playSound(SoundEvents.FOX_SNIFF, 1.0f, 0.9f);
				} else {
					spot = blockPosition();
				}
			}
		}

		@Override
		public void stop() {
			spot = null;
		}
	}

	/** Leisurely roaming within {@value #ROAM} blocks around its home. */
	private class RoamGoal extends Goal {
		private int cooldown;

		RoamGoal() {
			setFlags(EnumSet.of(Goal.Flag.MOVE));
		}

		@Override
		public boolean canUse() {
			if (isResting() || getDigTicks() > 0 || --cooldown > 0) {
				return false;
			}

			cooldown = 80 + random.nextInt(120);
			return true;
		}

		@Override
		public boolean canContinueToUse() {
			return !getNavigation().isDone() && !isResting() && getDigTicks() == 0;
		}

		@Override
		public void start() {
			BlockPos base = home != null ? home : blockPosition();
			Vec3 target = Vec3.atBottomCenterOf(base.offset(random.nextInt(ROAM * 2 + 1) - ROAM, 0, random.nextInt(ROAM * 2 + 1) - ROAM));
			getNavigation().moveTo(target.x, target.y, target.z, 0.8);
		}
	}

	/** For status displays: who owns the fox. */
	public @Nullable UUID ownerId() {
		return owner;
	}

	/** For tests/manual: remaining time until the next find. */
	public Optional<Long> ticksUntilDig() {
		return nextDig == 0 ? Optional.empty() : Optional.of(Math.max(0, nextDig - level().getGameTime()));
	}
}
