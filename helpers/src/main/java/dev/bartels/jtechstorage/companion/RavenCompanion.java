package dev.bartels.jtechstorage.companion;

import org.jspecify.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import java.util.ArrayList;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

import dev.bartels.jtechstorage.ModContent;
import dev.bartels.jtechstorage.ModSounds;
import dev.bartels.jtechstorage.block.StorageTerminalBlockEntity;
import dev.bartels.jtechstorage.network.StorageNetwork;

/**
 * Night Raven (carrying): collects items around its owner into its bag (tier 1: 9, 2: 18, 3: 27 slots) and
 * flies them to the storage terminal it is linked to (right-click with a paired storage key). When the
 * bag is two-thirds full it departs on its own; clicking again with the key sends it immediately.
 * Shift + right-click: open the bag.
 */
public class RavenCompanion extends Companion {
	/** v0.26.2: sturdier - more health, light armour, 70% of the mantis damage, faster beak strikes. */
	private static final double[] HEALTH = {18, 32, 50};
	private static final double[] ARMOR = {0, 2, 4};
	private static final double[] RADIUS = {3.0, 5.0, 8.0};
	private static final int MAX_SLOTS = 27;

	private static final EntityDataAccessor<Boolean> DATA_AWAY = SynchedEntityData.defineId(
			RavenCompanion.class, EntityDataSerializers.BOOLEAN);
	private final SimpleContainer bag = new SimpleContainer(MAX_SLOTS);
	private @Nullable GlobalPos terminal;
	private int awayTicks;
	private int deliverPhase;
	private int departCooldown;
	/** Exploration trip (v0.25.1): remaining ticks or the cooldown afterwards. */
	private int exploreTicks;
	private int exploreCooldown;
	/** Stays briefly in the air after a fight before returning to the shoulder. */
	private int shoulderDelay;
	public static final int EXPLORE_COOLDOWN = 20 * 60 * 5;

	public RavenCompanion(EntityType<? extends RavenCompanion> type, Level level) {
		super(type, level);
		moveControl = new FlyingMoveControl<>(this, 10, false);
		refreshStage();
	}

	public static AttributeSupplier.Builder createAttributes() {
		return createMobAttributes()
				.add(Attributes.MAX_HEALTH, HEALTH[0])
				.add(Attributes.MOVEMENT_SPEED, 0.25)
				.add(Attributes.FLYING_SPEED, 0.5)
				.add(Attributes.ATTACK_DAMAGE, MANTIS_DAMAGE[0] * 0.7)
				.add(Attributes.ARMOR, 0.0)
				.add(Attributes.FOLLOW_RANGE, 24.0);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DATA_AWAY, false);
	}

	/** Cannot be clicked while away (it hovers invisibly above the owner). */
	@Override
	public boolean isPickable() {
		return !entityData.get(DATA_AWAY) && super.isPickable();
	}

	@Override
	protected PathNavigation createNavigation(Level level) {
		FlyingPathNavigation navigation = new FlyingPathNavigation(this, level);
		navigation.setCanOpenDoors(false);
		navigation.setCanFloat(true);
		return navigation;
	}

	@Override
	protected boolean canFlyToOwner() {
		return true;
	}

	public boolean isFlying() {
		return !onGround();
	}

	@Override
	public String species() {
		return "raven";
	}

	@Override
	protected void applyStageAttributes(int stage) {
		setMaxHealthKeepRatio(HEALTH[stage - 1]);
		setBase(this, Attributes.FLYING_SPEED, 0.5 + stage * 0.1);
		setBase(this, Attributes.ARMOR, ARMOR[stage - 1]);
	}

	@Override
	protected float stageScale(int stage) {
		return stage == 1 ? 0.9f : stage == 2 ? 1.5f : 2.6f;   // larger than before; tier 3 carries a rider
	}

	@Override
	protected SoundEvent hurtSound() {
		return ModSounds.RAVEN_HURT;
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return isAway() ? null : ModSounds.RAVEN_AMBIENT;
	}

	@Override
	public int getAmbientSoundInterval() {
		return 400;
	}

	@Override
	protected void registerCompanionGoals() {
	}

	@Override
	protected int attackInterval() {
		return 22;
	}

	// ------------------------------------------------------------------
	// Riding at tier 3: permanent flight (look direction, jump key = ascend, looking down = descend)
	// ------------------------------------------------------------------

	@Override
	protected boolean canBeRidden() {
		return getStage() == 3 && !isExhausted() && !isAway();
	}

	@Override
	protected Vec3 seat() {
		return new Vec3(0.0, 1.35, -0.3);
	}

	@Override
	public boolean isFlyingVehicle() {
		return true;
	}

	@Override
	protected Vec3 getRiddenInput(Player player, Vec3 selfInput) {
		float forward = player.zza;
		float strafe = player.xxa * 0.5f;
		float pitch = player.getXRot() * Mth.DEG_TO_RAD;
		double up = forward != 0 ? -Mth.sin(pitch) * forward : 0;

		if (player.isJumping()) {
			up += 0.7;
		}

		double horizontal = forward != 0 ? Mth.cos(pitch) * forward : 0;
		return new Vec3(strafe, up, horizontal);
	}

	@Override
	protected float getRiddenSpeed(Player player) {
		return 0.1f * (1.0f + 0.2f * talent("sky_lord") + 0.07f * talent("tailwind"));
	}

	@Override
	public void travel(Vec3 input) {
		if (getControllingPassenger() != null) {
			travelFlying(input, getSpeed());
		} else {
			super.travel(input);
		}
	}

	@Override
	protected double combatShare() {
		return 0.7;
	}

	@Override
	protected SoundEvent attackSound() {
		return ModSounds.RAVEN_ATTACK;
	}

	// ------------------------------------------------------------------
	// Talents (v0.26): bag, combat, flight
	// ------------------------------------------------------------------

	@Override
	protected double damageBonus() {
		return 0.1 * talent("sharp_beak");
	}

	@Override
	protected double intervalFactor() {
		return 1.0 - 0.1 * talent("dive");
	}

	@Override
	protected double speedBonus() {
		return 0.07 * talent("tailwind");
	}

	@Override
	protected void onTalentHit(ServerLevel level, LivingEntity target) {
		int peck = talent("eye_peck");

		if (peck > 0 && random.nextDouble() < 0.1 * peck) {
			target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0), this);
			target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0), this);
		}

		// Raven Call: sometimes summons a shadow raven
		if (talent("raven_call") > 0 && random.nextDouble() < 0.15 && target.isAlive()) {
			SwarmRaven.summon(level, this, target, (float) getAttributeValue(Attributes.ATTACK_DAMAGE), 8);
		}

		// Shadow Swarm: shadow crows swoop down on the monsters around the target
		if (talent("shadow_swarm") > 0 && random.nextDouble() < 0.2) {
			level.sendParticles(ParticleTypes.LARGE_SMOKE, target.getX(), target.getY() + 1.0, target.getZ(), 20, 1.5, 0.6, 1.5, 0.02);
			level.sendParticles(ParticleTypes.WITCH, target.getX(), target.getY() + 1.0, target.getZ(), 20, 1.5, 0.6, 1.5, 0.05);
			playSound(ModSounds.RAVEN_DEPART, 0.8f, 0.6f);

			for (LivingEntity enemy : level.getEntitiesOfClass(LivingEntity.class, target.getBoundingBox().inflate(4.0),
					e -> e != target && Companion.hostile(e))) {
				enemy.hurtServer(level, damageSources().indirectMagic(this, this), 3.0f);
			}
		}
	}

	/** Feather Fall and Night Eye - aids for the owner. */
	private void ownerTalents(ServerLevel level) {
		if (!isActive() || !(getOwner() instanceof ServerPlayer owner) || owner.level() != level || owner.distanceToSqr(this) > 24 * 24) {
			return;
		}

		int fall = talent("feather_fall");

		if (fall > 0 && tickCount % 5 == 0 && owner.fallDistance > (fall == 1 ? 6.0 : 3.0) && !owner.isFallFlying()
				&& !owner.getAbilities().flying && !owner.hasEffect(MobEffects.SLOW_FALLING)) {
			owner.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 60, 0));
			level.sendParticles(ParticleTypes.WHITE_ASH, owner.getX(), owner.getY() + 1.0, owner.getZ(), 15, 0.4, 0.5, 0.4, 0.02);
		}

		// Night Wings: night vision while it is dark around you
		if (talent("night_wings") > 0 && tickCount % 100 == 0 && level.getMaxLocalRawBrightness(owner.blockPosition()) < 8) {
			owner.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 20 * 15, 0, true, false, true));
		}

		if (talent("night_eye") > 0 && tickCount % 200 == 0) {
			for (LivingEntity enemy : level.getEntitiesOfClass(LivingEntity.class, owner.getBoundingBox().inflate(16.0),
					e -> Companion.hostile(e) && sees(owner, e))) {
				enemy.addEffect(new MobEffectInstance(MobEffects.GLOWING, 120, 0));
			}
		}
	}

	/** Line of sight from the owner or the raven itself; monsters are only marked when one of them sees it. */
	private boolean sees(LivingEntity owner, LivingEntity enemy) {
		return owner.hasLineOfSight(enemy) || hasLineOfSight(enemy);
	}

	/** Slots usable at the current tier. */
	public int capacity() {
		return 9 * getStage();
	}

	public boolean hasTerminal() {
		return terminal != null;
	}

	public int bagUsed() {
		return usedSlots();
	}

	/** Open the bag from the companion menu (nearby, not away). */
	public void openBag(ServerPlayer player) {
		if (isAway() || player.level() != level() || player.distanceToSqr(this) > 8 * 8) {
			player.sendOverlayMessage(Component.translatable("jtechstorage.raven.bag_far", getDisplayName()).withStyle(ChatFormatting.GRAY));
			return;
		}

		int rows = getStage();
		MenuType<ChestMenu> type = rows == 1 ? MenuType.GENERIC_9x1 : rows == 2 ? MenuType.GENERIC_9x2 : MenuType.GENERIC_9x3;
		Container view = new BagView(this, bag, rows * 9);
		player.openMenu(new SimpleMenuProvider((id, inventory, p) -> new ChestMenu(type, id, inventory, view, rows), getDisplayName()));
	}

	public boolean isAway() {
		return level().isClientSide() ? entityData.get(DATA_AWAY) : awayTicks > 0 || deliverPhase > 0 || exploreTicks > 0;
	}

	public boolean isExploring() {
		return exploreTicks > 0;
	}

	/** For menu and HUD: remaining exploration (> 0) or cooldown (< 0). */
	public int exploreTimer() {
		return exploreTicks > 0 ? exploreTicks : -exploreCooldown;
	}

	// ------------------------------------------------------------------
	// Shoulder (tier 1)
	// ------------------------------------------------------------------

	@Override
	protected boolean shoulderStage() {
		return getStage() == 1;
	}

	@Override
	protected boolean wantsShoulder(Player owner) {
		return !isAway() && shoulderDelay == 0 && super.wantsShoulder(owner);
	}

	@Override
	public int shoulderSide() {
		return -1;
	}

	// ------------------------------------------------------------------
	// Exploration trip (v0.25.1): flies off and brings back finds - more valuable per tier
	// ------------------------------------------------------------------

	private record Find(Item item, int min, int max, int weight) {
	}

	private static final java.util.List<java.util.List<Find>> FINDS = java.util.List.of(
			java.util.List.of(
					new Find(Items.RAW_COPPER, 3, 8, 10), new Find(Items.COPPER_INGOT, 2, 6, 8),
					new Find(Items.RAW_IRON, 2, 5, 8), new Find(Items.IRON_INGOT, 1, 4, 6),
					new Find(Items.IRON_NUGGET, 4, 12, 6), new Find(Items.COAL, 4, 10, 8),
					new Find(Items.FLINT, 2, 5, 4), new Find(Items.FEATHER, 2, 6, 4),
					new Find(Items.STRING, 2, 5, 4), new Find(Items.AMETHYST_SHARD, 2, 6, 5),
					new Find(Items.SWEET_BERRIES, 3, 8, 3)),
			java.util.List.of(
					new Find(Items.RAW_GOLD, 2, 5, 8), new Find(Items.GOLD_INGOT, 1, 4, 6),
					new Find(Items.GOLD_NUGGET, 4, 12, 5), new Find(Items.EMERALD, 1, 3, 6),
					new Find(Items.LAPIS_LAZULI, 4, 10, 6), new Find(Items.REDSTONE, 4, 10, 6),
					new Find(Items.GLOW_BERRIES, 2, 6, 3), new Find(Items.NAME_TAG, 1, 1, 1),
					new Find(Items.SADDLE, 1, 1, 1)),
			java.util.List.of(
					new Find(Items.DIAMOND, 1, 2, 6), new Find(Items.EMERALD, 2, 5, 4),
					new Find(Items.OBSIDIAN, 2, 4, 3), new Find(Items.ENDER_PEARL, 1, 3, 4),
					new Find(Items.EXPERIENCE_BOTTLE, 2, 5, 3), new Find(Items.GOLDEN_APPLE, 1, 1, 2),
					new Find(Items.ECHO_SHARD, 1, 1, 1)));

	/** Duration of a trip in ticks (talent Swift Delivery: shorter). */
	public int exploreDuration() {
		return (int) (20 * 60 * 4 * (1.0 - 0.25 * talent("swift_delivery")));
	}

	/** From the menu: send on an exploration trip. */
	public void explore(ServerPlayer player) {
		if (!(level() instanceof ServerLevel level)) {
			return;
		}

		String problem = isExhausted() ? "jtechstorage.raven.explore.exhausted" : isAway() ? "jtechstorage.raven.explore.away"
				: isVehicle() ? "jtechstorage.raven.explore.ridden" : exploreCooldown > 0 ? "jtechstorage.raven.explore.cooldown" : null;

		if (problem != null) {
			player.sendOverlayMessage(Component.translatable(problem, getDisplayName(), (exploreCooldown + 1199) / 1200).withStyle(ChatFormatting.GRAY));
			return;
		}

		exploreTicks = exploreDuration();
		setTarget(null);
		level.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 0.4, getZ(), 8, 0.2, 0.2, 0.2, 0.02);
		level.sendParticles(ParticleTypes.WITCH, getX(), getY() + 0.4, getZ(), 15, 0.3, 0.3, 0.3, 0.05);
		playSound(ModSounds.RAVEN_DEPART, 1.0f, 0.9f);
		setInvisible(true);
		entityData.set(DATA_AWAY, true);
		getNavigation().stop();
		player.sendOverlayMessage(Component.translatable("jtechstorage.raven.explore.start", getDisplayName(), (exploreTicks + 1199) / 1200)
				.withStyle(ChatFormatting.LIGHT_PURPLE));
	}

	private Find roll(int tier, boolean currentOnly) {
		int stage = getStage();
		java.util.List<Find> pool = new ArrayList<>();
		java.util.List<Integer> weights = new ArrayList<>();

		for (int t = currentOnly ? tier : 1; t <= tier; t++) {
			int factor = t == stage ? 4 : t == stage - 1 ? 2 : 1;     // its own tier counts the most

			for (Find find : FINDS.get(t - 1)) {
				pool.add(find);
				weights.add(find.weight() * factor);
			}
		}

		int total = weights.stream().mapToInt(Integer::intValue).sum();
		int pick = random.nextInt(total);

		for (int i = 0; i < pool.size(); i++) {
			pick -= weights.get(i);

			if (pick < 0) {
				return pool.get(i);
			}
		}

		return pool.get(0);
	}

	private void finishExplore(ServerLevel level) {
		int stage = getStage();
		int rolls = 2 + stage + talent("lucky_find");
		java.util.List<ItemStack> found = new ArrayList<>();

		for (int i = 0; i < rolls; i++) {
			// Treasure Hunter: sometimes a find from the next tier, at tier 3 rarely netherite scrap
			if (talent("treasure_hunter") > 0 && stage == 3 && random.nextDouble() < 0.1) {
				found.add(new ItemStack(Items.NETHERITE_SCRAP));
				continue;
			}

			int tier = talent("treasure_hunter") > 0 && stage < 3 && random.nextDouble() < 0.25 ? stage + 1 : stage;
			Find find = roll(tier, i == 0 || tier > stage);      // the first find is always from its own tier
			found.add(new ItemStack(find.item(), find.min() + random.nextInt(find.max() - find.min() + 1)));
		}

		int count = 0;
		LivingEntity owner = getOwner();

		for (ItemStack stack : found) {
			count += stack.getCount();
			ItemStack rest = addToBag(stack);

			if (!rest.isEmpty()) {
				if (owner instanceof ServerPlayer player && player.level() == level) {
					if (!player.getInventory().add(rest)) {
						player.spawnAtLocation(level, rest);
					}
				} else {
					spawnAtLocation(level, rest);
				}
			}
		}

		exploreCooldown = talent("treasure_hunter") > 0 ? EXPLORE_COOLDOWN * 3 / 5 : EXPLORE_COOLDOWN;
		setInvisible(false);
		entityData.set(DATA_AWAY, false);

		if (owner instanceof ServerPlayer player && player.level() == level) {
			Vec3 at = player.position().add(player.getLookAngle().multiply(1, 0, 1).normalize().scale(-1.5)).add(0, 1.2, 0);
			snapTo(at.x, at.y, at.z, getYRot(), getXRot());
			MutableComponent list = Component.empty();

			for (int i = 0; i < found.size(); i++) {
				list.append(Component.literal((i > 0 ? ", " : "") + found.get(i).getCount() + "× ")).append(found.get(i).getHoverName());
			}

			player.sendSystemMessage(Component.translatable("jtechstorage.raven.explore.back", getDisplayName(), list).withStyle(ChatFormatting.LIGHT_PURPLE));
		}

		level.sendParticles(ParticleTypes.WITCH, getX(), getY() + 0.4, getZ(), 15, 0.3, 0.3, 0.3, 0.05);
		playSound(ModSounds.RAVEN_RETURN, 1.0f, 1.1f);
		triggerAction();
	}

	@Override
	public boolean isActive() {
		return super.isActive() && !isAway();
	}

	// ------------------------------------------------------------------
	// Sequence
	// ------------------------------------------------------------------

	@Override
	public void aiStep() {
		super.aiStep();

		if (!(level() instanceof ServerLevel level)) {
			return;
		}

		if (departCooldown > 0) {
			departCooldown--;
		}

		if (exploreCooldown > 0 && exploreTicks == 0) {
			exploreCooldown--;
		}

		tickPassives(level);

		if (getTarget() != null) {
			shoulderDelay = 30;
		} else if (shoulderDelay > 0) {
			shoulderDelay--;
		}

		if (isAway()) {
			if (!isInvisible()) {
				setInvisible(true);     // after loading: while away it stays invisible
			}

			tickAway(level);
			return;
		}

		ownerTalents(level);

		if (tickCount % 10 == 0 && isActive()) {
			collectItems(level);
		}

		// Departs from 2/3 full bag (talent Express Courier: from 1/2 or 1/3)
		int courier = talent("eager_courier");
		double fill = courier == 0 ? 2.0 / 3.0 : courier == 1 ? 0.5 : 1.0 / 3.0;

		if (tickCount % 40 == 0 && terminal != null && isActive() && departCooldown == 0 && usedSlots() >= Math.max(1, Math.ceil(capacity() * fill))) {
			depart(level, null);
		}
	}

	/** Collect nearby items - not the ones the owner has just thrown away. */
	private void collectItems(ServerLevel level) {
		double radius = RADIUS[getStage() - 1] + 1.5 * talent("keen_eye") + equipmentTier();   // amulet: +1 block per material
		LivingEntity owner = getOwner();
		boolean took = false;

		for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, getBoundingBox().inflate(radius),
				e -> e.isAlive() && !e.hasPickUpDelay() && !e.getItem().isEmpty())) {
			if (owner != null && item.getOwner() == owner && item.getAge() < 20 * 30) {
				continue;
			}

			ItemStack rest = addToBag(item.getItem().copy());

			if (rest.getCount() != item.getItem().getCount()) {
				took = true;
				level.sendParticles(ParticleTypes.WITCH, item.getX(), item.getY() + 0.2, item.getZ(), 3, 0.1, 0.1, 0.1, 0.0);

				if (rest.isEmpty()) {
					item.discard();
				} else {
					item.setItem(rest);
				}
			}
		}

		if (took) {
			triggerAction();
			playSound(ModSounds.RAVEN_PICKUP, 0.6f, 0.9f + random.nextFloat() * 0.3f);
		}
	}

	private ItemStack addToBag(ItemStack stack) {
		int capacity = capacity();

		for (int i = 0; i < capacity && !stack.isEmpty(); i++) {
			ItemStack slot = bag.getItem(i);

			if (!slot.isEmpty() && ItemStack.isSameItemSameComponents(slot, stack) && slot.getCount() < slot.getMaxStackSize()) {
				int move = Math.min(stack.getCount(), slot.getMaxStackSize() - slot.getCount());
				slot.grow(move);
				stack.shrink(move);
			}
		}

		for (int i = 0; i < capacity && !stack.isEmpty(); i++) {
			if (bag.getItem(i).isEmpty()) {
				bag.setItem(i, stack.copy());
				stack.setCount(0);
			}
		}

		bag.setChanged();
		return stack;
	}

	private int usedSlots() {
		int used = 0;

		for (int i = 0; i < MAX_SLOTS; i++) {
			if (!bag.getItem(i).isEmpty()) {
				used++;
			}
		}

		return used;
	}

	// ------------------------------------------------------------------
	// Delivery to the terminal
	// ------------------------------------------------------------------

	/** Flies off: invisible, flight time depends on distance (tier 3 flies faster). */
	private void depart(ServerLevel level, @Nullable Player commandedBy) {
		if (terminal == null || usedSlots() == 0) {
			if (commandedBy != null) {
				commandedBy.sendOverlayMessage(Component.translatable(terminal == null ? "jtechstorage.raven.no_terminal" : "jtechstorage.raven.empty"));
			}

			return;
		}

		int seconds;

		if (terminal.dimension().equals(level.dimension())) {
			double distance = Math.sqrt(terminal.pos().distToCenterSqr(position()));
			seconds = (int) Math.clamp(2 + distance / (12.0 + 8.0 * getStage()), 3, 25);
		} else {
			seconds = 12;
		}

		seconds = Math.max(2, (int) Math.round(seconds * (1.0 - 0.25 * talent("swift_delivery"))));
		awayTicks = seconds * 20;
		level.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 0.4, getZ(), 8, 0.2, 0.2, 0.2, 0.02);
		level.sendParticles(ParticleTypes.WITCH, getX(), getY() + 0.4, getZ(), 15, 0.3, 0.3, 0.3, 0.05);
		playSound(ModSounds.RAVEN_DEPART, 1.0f, 1.0f);
		setInvisible(true);
		entityData.set(DATA_AWAY, true);
		getNavigation().stop();

		if (getOwner() instanceof ServerPlayer owner) {
			owner.sendOverlayMessage(Component.translatable("jtechstorage.raven.departs", getDisplayName(), seconds).withStyle(ChatFormatting.LIGHT_PURPLE));
		}
	}

	private void tickAway(ServerLevel level) {
		// stays invisible next to the owner so that it stays loaded
		if (tickCount % 20 == 0 && getOwner() instanceof ServerPlayer owner && owner.level() == level) {
			snapTo(owner.getX(), owner.getY() + 5.0, owner.getZ(), getYRot(), getXRot());
		}

		if (exploreTicks > 0) {
			if (--exploreTicks == 0) {
				finishExplore(level);
			}

			return;
		}

		if (awayTicks > 0) {
			if (--awayTicks == 0) {
				deliverPhase = 20;
				prepareTerminal(level);
			}

			return;
		}

		if (--deliverPhase == 0) {
			deliver(level);
		}
	}

	private @Nullable StorageTerminalBlockEntity terminalEntity(ServerLevel level) {
		if (terminal == null) {
			return null;
		}

		ServerLevel target = level.getServer().getLevel(terminal.dimension());

		if (target == null) {
			return null;
		}

		BlockPos pos = terminal.pos();
		target.getChunk(pos.getX() >> 4, pos.getZ() >> 4);     // loads the terminal chunk
		return target.getBlockEntity(pos) instanceof StorageTerminalBlockEntity t ? t : null;
	}

	private void prepareTerminal(ServerLevel level) {
		StorageTerminalBlockEntity t = terminalEntity(level);

		if (t != null) {
			t.keepNetworkLoaded();    // load chest chunks for the delivery
		}
	}

	private void deliver(ServerLevel level) {
		StorageTerminalBlockEntity t = terminalEntity(level);
		int delivered = 0;
		boolean full = false;

		if (t != null && !(getOwner() instanceof Player owner && t.can(owner, StorageTerminalBlockEntity.Permission.HELPERS))) {
			t = null;    // no (more) access to this terminal
		}

		if (t != null) {
			StorageNetwork network = t.buildNetwork();

			for (int i = 0; i < MAX_SLOTS; i++) {
				ItemStack stack = bag.getItem(i);

				if (stack.isEmpty()) {
					continue;
				}

				int before = stack.getCount();
				ItemStack rest = network.insert(stack.copy());
				delivered += before - rest.getCount();
				bag.setItem(i, rest);
				full |= !rest.isEmpty();
			}
		}

		bag.setChanged();
		setInvisible(false);
		entityData.set(DATA_AWAY, false);
		departCooldown = t == null || full ? 20 * 60 * 5 : 20 * 30;   // wait a long time after a failure

		if (getOwner() instanceof ServerPlayer owner && owner.level() == level) {
			Vec3 at = owner.position().add(owner.getLookAngle().multiply(1, 0, 1).normalize().scale(-1.5)).add(0, 1.2, 0);
			snapTo(at.x, at.y, at.z, getYRot(), getXRot());
		}

		level.sendParticles(ParticleTypes.WITCH, getX(), getY() + 0.4, getZ(), 15, 0.3, 0.3, 0.3, 0.05);
		playSound(ModSounds.RAVEN_RETURN, 1.0f, 1.0f);

 		// Lucky find: sometimes brings back a few amethyst shards from the flight
		if (t != null && talent("lucky_find") > 0 && random.nextDouble() < 0.3 && getOwner() instanceof ServerPlayer finder) {
			ItemStack find = new ItemStack(Items.AMETHYST_SHARD, 1 + random.nextInt(3));

			if (!finder.getInventory().add(find)) {
				finder.spawnAtLocation(level, find);
			}

			finder.sendOverlayMessage(Component.translatable("jtechstorage.raven.lucky_find", getDisplayName()).withStyle(ChatFormatting.LIGHT_PURPLE));
		}

		if (getOwner() instanceof ServerPlayer owner) {
			String key = t == null ? "jtechstorage.raven.terminal_missing" : full ? "jtechstorage.raven.delivered_full" : "jtechstorage.raven.delivered";
			owner.sendOverlayMessage(Component.translatable(key, getDisplayName(), delivered).withStyle(t == null || full ? ChatFormatting.GOLD : ChatFormatting.GREEN));
		}
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		if (isAway() && !source.is(DamageTypes.GENERIC_KILL)) {
			return false;
		}

		if (crowScatter(level, source)) {
			return false;
		}

		return super.hurtServer(level, source, amount);
	}

	// ------------------------------------------------------------------
	// Passive abilities (v0.26.3), always active, grow with the level:
	// Crow Decay (dodges), Scout Crows (mark monsters, warn of creepers), Guardian Crow (intercepts projectiles)
	// ------------------------------------------------------------------

	private int scatterCooldown;
	/** Crow doppelganger (v0.26.4): cooldown in ticks. */
	private int decoyCooldown;

	public int decoyCooldown() {
		return decoyCooldown;
	}

	/** Doppelganger cooldown: 10 minutes, only 6 at level 100. */
	private int decoyInterval() {
		return 20 * 60 * 10 - (int) (20 * 60 * 4 * Math.min(100, getLevel()) / 100.0);
	}

	/**
	 * Crow doppelganger: a lethal hit makes the owner disintegrate into crows. A shadow decoy stays behind and
	 * draws the monsters to itself; the owner reappears 5-8 blocks away with 4 hearts, briefly invisible and with Resistance.
	 */
	public boolean tryDecoy(ServerPlayer owner) {
		if (decoyCooldown > 0 || isAway() || isExhausted() || owner.level() != level() || distanceToSqr(owner) > 32 * 32) {
			return false;
		}

		ServerLevel level = (ServerLevel) level();
		decoyCooldown = decoyInterval();
		crowPuff(level, owner.getX(), owner.getY() + 1.0, owner.getZ());
		level.playSound(null, owner.getX(), owner.getY() + 1, owner.getZ(), ModSounds.RAVEN_DEPART, SoundSource.PLAYERS, 1.2f, 0.8f);
		CrowDecoy.spawn(level, owner);

		for (int k = 0; k < 5; k++) {
			SwarmRaven.scout(level, this, owner, k * (Math.PI * 2 / 5));
		}

		owner.setHealth(8.0f);
		owner.clearFire();
		owner.fallDistance = 0;

		for (int tries = 0; tries < 12; tries++) {
			double angle = random.nextDouble() * Math.PI * 2;
			double dist = 5.0 + random.nextDouble() * 3.0;

			if (owner.randomTeleport(owner.getX() + Math.cos(angle) * dist, owner.getY() + 1, owner.getZ() + Math.sin(angle) * dist, false, state -> true)) {
				break;
			}
		}

		crowPuff(level, owner.getX(), owner.getY() + 1.0, owner.getZ());
		owner.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 60, 0, false, false, true));
		owner.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 60, 1, false, true, true));
		owner.sendSystemMessage(Component.translatable("jtechstorage.raven.decoy", getDisplayName()).withStyle(ChatFormatting.DARK_PURPLE));
		return true;
	}
	private int scoutTimer = 200;
	private int guardCooldown;

	/** Chance to dodge a hit as a swarm of crows: 10% + 0.25% per level, at most 35%. */
	public double scatterChance() {
		return Math.min(0.35, 0.10 + 0.0025 * getLevel());
	}

	/** Chance to intercept a projectile aimed at the owner: 15% + 0.2% per level, at most 35%. */
	public double guardChance() {
		return Math.min(0.35, 0.15 + 0.002 * getLevel());
	}

	/** Scout crow radius: 12 blocks + 1 per 10 levels, at most 24. */
	public double scoutRadius() {
		return Math.min(24.0, 12.0 + getLevel() / 10.0);
	}

	/** Time between two scout flights: 30 s - 1 s per 10 levels, at least 15 s. */
	private int scoutInterval() {
		return 20 * Math.max(15, 30 - getLevel() / 10);
	}

	private void crowPuff(ServerLevel level, double x, double y, double z) {
		level.sendParticles(ParticleTypes.LARGE_SMOKE, x, y, z, 14, 0.4, 0.4, 0.4, 0.03);
		level.sendParticles(ParticleTypes.SQUID_INK, x, y, z, 10, 0.4, 0.4, 0.4, 0.05);
		level.sendParticles(ParticleTypes.WITCH, x, y, z, 6, 0.3, 0.3, 0.3, 0.02);
	}

	/** Crow Decay: disintegrates into crows, dodges the hit and reassembles a few blocks away. */
	private boolean crowScatter(ServerLevel level, DamageSource source) {
		if (scatterCooldown > 0 || isExhausted() || isVehicle() || isOnShoulder() || source.getEntity() == null
				|| source.is(DamageTypes.GENERIC_KILL) || random.nextDouble() >= scatterChance()) {
			return false;
		}

		scatterCooldown = 60;
		crowPuff(level, getX(), getY() + getBbHeight() * 0.5, getZ());
		playSound(ModSounds.RAVEN_AMBIENT, 1.0f, 1.4f);

		for (int tries = 0; tries < 8; tries++) {
			double x = getX() + (random.nextDouble() - 0.5) * 8.0;
			double z = getZ() + (random.nextDouble() - 0.5) * 8.0;

			if (randomTeleport(x, getY() + random.nextInt(3), z, false, state -> true)) {
				break;
			}
		}

		crowPuff(level, getX(), getY() + getBbHeight() * 0.5, getZ());
		return true;
	}

	/** Guardian Crow: a projectile that would hit the owner is sometimes intercepted (from the damage event). */
	public boolean interceptFor(ServerPlayer owner, DamageSource source) {
		if (guardCooldown > 0 || isAway() || isExhausted() || owner.level() != level() || distanceToSqr(owner) > 24 * 24
				|| random.nextDouble() >= guardChance()) {
			return false;
		}

		guardCooldown = 40;
		ServerLevel level = (ServerLevel) level();
		Entity projectile = source.getDirectEntity();
		double x = projectile != null ? projectile.getX() : owner.getX();
		double y = projectile != null ? projectile.getY() : owner.getY() + 1.2;
		double z = projectile != null ? projectile.getZ() : owner.getZ();
		crowPuff(level, x, y, z);
		level.playSound(null, x, y, z, ModSounds.RAVEN_ATTACK, SoundSource.NEUTRAL, 0.9f, 1.3f);

		if (projectile != null && !(projectile instanceof Player)) {
			projectile.discard();
		}

		owner.sendOverlayMessage(Component.translatable("jtechstorage.raven.guarded", getDisplayName()).withStyle(ChatFormatting.DARK_PURPLE));
		return true;
	}

	/** Scout crows: small shadow crows circle the owner, mark monsters and warn of creepers behind them. */
	private void tickPassives(ServerLevel level) {
		if (scatterCooldown > 0) {
			scatterCooldown--;
		}

		if (decoyCooldown > 0) {
			decoyCooldown--;
		}

		if (guardCooldown > 0) {
			guardCooldown--;
		}

		if (--scoutTimer > 0 || isAway() || isExhausted() || !(getOwner() instanceof ServerPlayer owner) || owner.level() != level
				|| distanceToSqr(owner) > 32 * 32) {
			return;
		}

		scoutTimer = scoutInterval();
		double r = scoutRadius();
		int found = 0;

		for (LivingEntity enemy : level.getEntitiesOfClass(LivingEntity.class, owner.getBoundingBox().inflate(r),
				e -> Companion.hostile(e))) {
			// Only mark what can actually be seen (no x-ray through walls); the creeper warning still applies
			if (sees(owner, enemy)) {
				enemy.addEffect(new MobEffectInstance(MobEffects.GLOWING, 120, 0, false, false));
				found++;
			}

			if (enemy instanceof Creeper && enemy.distanceToSqr(owner) < 10 * 10) {
				Vec3 look = owner.getViewVector(1.0f).multiply(1, 0, 1);
				Vec3 to = enemy.position().subtract(owner.position()).multiply(1, 0, 1);

				if (look.dot(to) < 0) {
					owner.sendOverlayMessage(Component.translatable("jtechstorage.raven.creeper_behind", getDisplayName()).withStyle(ChatFormatting.RED));
					level.playSound(null, owner.getX(), owner.getY() + 1, owner.getZ(), ModSounds.RAVEN_AMBIENT, SoundSource.NEUTRAL, 1.2f, 1.6f);
				}
			}
		}

		if (found > 0) {
			for (int k = 0; k < 3; k++) {
				SwarmRaven.scout(level, this, owner, k * (Math.PI * 2 / 3));
			}
		}
	}

	// ------------------------------------------------------------------
	// Interaction
	// ------------------------------------------------------------------

	@Override
	protected @Nullable InteractionResult interactSpecial(Player player, ItemStack held) {
		if (isAway()) {
			return InteractionResult.PASS;
		}

		GlobalPos bound = held.get(ModContent.LINKED_TERMINAL);

		if (held.is(ModContent.STORAGE_KEY) && bound != null) {
			if (level() instanceof ServerLevel level) {
				if (bound.equals(terminal)) {
					depart(level, player);
				} else {
					if (bound.dimension().equals(level.dimension()) && level.isLoaded(bound.pos())
							&& level.getBlockEntity(bound.pos()) instanceof StorageTerminalBlockEntity t
							&& !t.can(player, StorageTerminalBlockEntity.Permission.HELPERS)) {
						player.sendOverlayMessage(Component.translatable("jtechstorage.message.no_permission",
								Component.translatable("jtechstorage.role.builder")));
						return InteractionResult.FAIL;
					}

					terminal = bound;
					level.sendParticles(ParticleTypes.WITCH, getX(), getY() + 0.5, getZ(), 20, 0.3, 0.3, 0.3, 0.02);
					playSound(ModSounds.RAVEN_AMBIENT, 1.0f, 1.3f);
					player.sendOverlayMessage(Component.translatable("jtechstorage.raven.linked", getDisplayName()).withStyle(ChatFormatting.LIGHT_PURPLE));
				}
			}

			return InteractionResult.SUCCESS;
		}

		if (held.isEmpty() && player.isSecondaryUseActive()) {
			if (!level().isClientSide()) {
				int rows = getStage();
				MenuType<ChestMenu> type = rows == 1 ? MenuType.GENERIC_9x1 : rows == 2 ? MenuType.GENERIC_9x2 : MenuType.GENERIC_9x3;
				Container view = new BagView(this, bag, rows * 9);
				player.openMenu(new SimpleMenuProvider((id, inventory, p) -> new ChestMenu(type, id, inventory, view, rows), getDisplayName()));
			}

			return InteractionResult.SUCCESS;
		}

		return null;
	}

	// ------------------------------------------------------------------
	// Saving
	// ------------------------------------------------------------------

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		ContainerHelper.saveAllItems(output.child("RavenBag"), bag.getItems());

		if (terminal != null) {
			output.store("Terminal", GlobalPos.CODEC, terminal);
		}

		output.putInt("Away", awayTicks + deliverPhase);
		output.putInt("Explore", exploreTicks);
		output.putInt("ExploreCooldown", exploreCooldown);
		output.putInt("DecoyCooldown", decoyCooldown);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		ContainerHelper.loadAllItems(input.childOrEmpty("RavenBag"), bag.getItems());
		terminal = input.read("Terminal", GlobalPos.CODEC).orElse(null);
		int away = input.getIntOr("Away", 0);
		exploreTicks = input.getIntOr("Explore", 0);
		exploreCooldown = input.getIntOr("ExploreCooldown", 0);
		decoyCooldown = input.getIntOr("DecoyCooldown", 0);

		if (exploreTicks > 0) {
			entityData.set(DATA_AWAY, true);
		}

		if (away > 0) {
			awayTicks = 0;
			deliverPhase = 1;     // deliver immediately after loading
			entityData.set(DATA_AWAY, true);
		}
	}

	@Override
	public void remove(RemovalReason reason) {
		// on real removal lose nothing: drop the bag
		if (level() instanceof ServerLevel level && (reason == RemovalReason.KILLED || reason == RemovalReason.DISCARDED)) {
			for (ItemStack stack : bag.getItems()) {
				if (!stack.isEmpty()) {
					spawnAtLocation(level, stack.copy());
				}
			}

			bag.clearContent();
		}

		super.remove(reason);
	}

	/** Section of the bag usable at the current tier. */
	private static final class BagView implements Container {
		private final RavenCompanion raven;
		private final SimpleContainer bag;
		private final int size;

		BagView(RavenCompanion raven, SimpleContainer bag, int size) {
			this.raven = raven;
			this.bag = bag;
			this.size = size;
		}

		@Override
		public int getContainerSize() {
			return size;
		}

		@Override
		public boolean isEmpty() {
			for (int i = 0; i < size; i++) {
				if (!bag.getItem(i).isEmpty()) {
					return false;
				}
			}

			return true;
		}

		@Override
		public ItemStack getItem(int slot) {
			return bag.getItem(slot);
		}

		@Override
		public ItemStack removeItem(int slot, int count) {
			return bag.removeItem(slot, count);
		}

		@Override
		public ItemStack removeItemNoUpdate(int slot) {
			return bag.removeItemNoUpdate(slot);
		}

		@Override
		public void setItem(int slot, ItemStack stack) {
			bag.setItem(slot, stack);
		}

		@Override
		public void setChanged() {
			bag.setChanged();
		}

		@Override
		public boolean stillValid(Player player) {
			return raven.isAlive() && !raven.isRemoved() && !raven.isAway() && raven.distanceToSqr(player) < 8 * 8;
		}

		@Override
		public void clearContent() {
			for (int i = 0; i < size; i++) {
				bag.setItem(i, ItemStack.EMPTY);
			}
		}
	}

	// ------------------------------------------------------------------
	// v0.25.3: special attacks
	// ------------------------------------------------------------------

	@Override
	protected int specialBaseCooldown(int which) {
		return switch (which) {
			case 0 -> 20 * 90;       // Raven Swarm
			case 1 -> 20 * 40;       // Shadow Dive
			default -> 20 * 90;      // Terrifying Cry
		};
	}

	@Override
	protected boolean specialNeedsTarget(int which) {
		return which != 2;
	}

	@Override
	protected boolean canUseSpecialNow() {
		return !isAway() && !isVehicle();
	}

	@Override
	protected boolean performSpecial(ServerLevel level, int which, @Nullable LivingEntity target) {
		float power = specialPower();
		float base = (float) getAttributeValue(Attributes.ATTACK_DAMAGE);

		switch (which) {
			case 0 -> {
				// Raven Swarm: five shadow ravens dive onto the target one after another
				if (target == null) {
					return false;
				}

				for (int i = 0; i < 5; i++) {
					SwarmRaven.summon(level, this, target, base * 1.5f * power, 8 + i * 5);
				}

				playSound(ModSounds.RAVEN_DEPART, 1.5f, 0.6f);
				level.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 0.5, getZ(), 15, 0.4, 0.4, 0.4, 0.05);
				setTarget(target);
				return true;
			}
			case 1 -> {
				// Shadow Dive: it dives onto the target itself - triple damage, blindness, knockback
				if (target == null || target.distanceToSqr(this) > 24 * 24) {
					return false;
				}

				Vec3 dir = position().subtract(target.position()).multiply(1, 0, 1);
				Vec3 at = target.position().add(dir.lengthSqr() > 0.01 ? dir.normalize().scale(1.2) : new Vec3(1.2, 0, 0)).add(0, target.getBbHeight(), 0);

				if (distanceToSqr(target) > 3.0 * 3.0 && !hasLineOfSight(target) || !leapTo(level, at) && distanceToSqr(target) > 3.0 * 3.0) {
					return false;
				}

				shoulderDelay = 60;
				level.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 0.5, getZ(), 12, 0.3, 0.3, 0.3, 0.05);
				target.hurtServer(level, damageSources().mobAttack(this), base * 3.0f * power);
				target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 100, 0), this);
				Vec3 push = target.position().subtract(position()).multiply(1, 0, 1).normalize().scale(0.8);
				target.push(push.x, 0.3, push.z);
				level.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY() + target.getBbHeight() * 0.6, target.getZ(), 15, 0.3, 0.3, 0.3, 0.2);
				playSound(ModSounds.RAVEN_ATTACK, 1.3f, 0.6f);
				setTarget(target);
				return true;
			}
			default -> {
				// Terrifying Cry: monsters within 10 blocks back off, become slow, weak and visible
				level.sendParticles(ParticleTypes.SONIC_BOOM, getX(), getY() + 0.6, getZ(), 1, 0, 0, 0, 0);
				playSound(ModSounds.RAVEN_AMBIENT, 2.0f, 0.5f);
				int duration = (int) (160 * power);

				for (LivingEntity enemy : level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(10.0),
						e -> Companion.hostile(e))) {
					enemy.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, duration, 1), this);
					enemy.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, duration, 0), this);
					if (hasLineOfSight(enemy)) {
						enemy.addEffect(new MobEffectInstance(MobEffects.GLOWING, duration, 0), this);
					}
					Vec3 push = enemy.position().subtract(position()).multiply(1, 0, 1).normalize().scale(1.0);
					enemy.push(push.x, 0.35, push.z);

					if (enemy instanceof Mob mob) {
						mob.setTarget(null);
					}
				}

				return true;
			}
		}
	}
}
