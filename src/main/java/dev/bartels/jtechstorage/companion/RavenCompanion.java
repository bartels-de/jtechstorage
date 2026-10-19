package dev.bartels.jtechstorage.companion;

import org.jspecify.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import java.util.ArrayList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

import dev.bartels.jtechstorage.ModContent;
import dev.bartels.jtechstorage.ModSounds;
import dev.bartels.jtechstorage.block.StorageTerminalBlockEntity;
import dev.bartels.jtechstorage.network.StorageNetwork;

/**
 * Nachtrabe (Tragen): sammelt Items rund um seinen Besitzer in seine Tasche (Stufe 1: 9, 2: 18, 3: 27 Plätze) und
 * fliegt sie zum Speicher-Terminal, mit dem er verbunden ist (Rechtsklick mit gekoppeltem Speicher-Schlüssel). Ist die
 * Tasche zu zwei Dritteln voll, fliegt er von selbst los; erneuter Klick mit dem Schlüssel schickt ihn sofort.
 * Shift + Rechtsklick: Tasche öffnen.
 */
public class RavenCompanion extends Companion {
	private static final double[] HEALTH = {16, 24, 36};
	private static final double[] RADIUS = {3.0, 5.0, 8.0};
	private static final int MAX_SLOTS = 27;

	private static final net.minecraft.network.syncher.EntityDataAccessor<Boolean> DATA_AWAY = net.minecraft.network.syncher.SynchedEntityData.defineId(
			RavenCompanion.class, net.minecraft.network.syncher.EntityDataSerializers.BOOLEAN);
	private final SimpleContainer bag = new SimpleContainer(MAX_SLOTS);
	private @Nullable GlobalPos terminal;
	private int awayTicks;
	private int deliverPhase;
	private int departCooldown;
	/** Erkundungstour (v0.25.1): verbleibende Ticks bzw. Abklingzeit danach. */
	private int exploreTicks;
	private int exploreCooldown;
	/** Nach einem Kampf noch kurz in der Luft, bevor er auf die Schulter zurückkehrt. */
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
				.add(Attributes.ATTACK_DAMAGE, MANTIS_DAMAGE[0] * 0.5)
				.add(Attributes.FOLLOW_RANGE, 24.0);
	}

	@Override
	protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DATA_AWAY, false);
	}

	/** Unterwegs nicht anklickbar (er schwebt unsichtbar über dem Besitzer). */
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
	}

	@Override
	protected float stageScale(int stage) {
		return stage == 1 ? 0.9f : stage == 2 ? 1.5f : 2.6f;   // größer als zuvor; Stufe 3 trägt einen Reiter
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
		return 32;
	}

	// ------------------------------------------------------------------
	// Reiten auf Stufe 3: dauerhaft fliegen (Blickrichtung, Sprungtaste = steigen, nach unten schauen = sinken)
	// ------------------------------------------------------------------

	@Override
	protected boolean canBeRidden() {
		return getStage() == 3 && !isExhausted() && !isAway();
	}

	@Override
	protected net.minecraft.world.phys.Vec3 seat() {
		return new net.minecraft.world.phys.Vec3(0.0, 1.35, -0.3);
	}

	@Override
	public boolean isFlyingVehicle() {
		return true;
	}

	@Override
	protected net.minecraft.world.phys.Vec3 getRiddenInput(Player player, net.minecraft.world.phys.Vec3 selfInput) {
		float forward = player.zza;
		float strafe = player.xxa * 0.5f;
		float pitch = player.getXRot() * net.minecraft.util.Mth.DEG_TO_RAD;
		double up = forward != 0 ? -net.minecraft.util.Mth.sin(pitch) * forward : 0;

		if (player.isJumping()) {
			up += 0.7;
		}

		double horizontal = forward != 0 ? net.minecraft.util.Mth.cos(pitch) * forward : 0;
		return new net.minecraft.world.phys.Vec3(strafe, up, horizontal);
	}

	@Override
	protected float getRiddenSpeed(Player player) {
		return 0.1f * (1.0f + 0.2f * talent("sky_lord") + 0.07f * talent("tailwind"));
	}

	@Override
	public void travel(net.minecraft.world.phys.Vec3 input) {
		if (getControllingPassenger() != null) {
			travelFlying(input, getSpeed());
		} else {
			super.travel(input);
		}
	}

	@Override
	protected double combatShare() {
		return 0.5;
	}

	@Override
	protected SoundEvent attackSound() {
		return ModSounds.RAVEN_ATTACK;
	}

	// ------------------------------------------------------------------
	// Talente (v0.26): Tasche, Kampf, Flug
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
			target.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.BLINDNESS, 60, 0), this);
			target.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.WEAKNESS, 60, 0), this);
		}

		// Rabenruf: ruft manchmal einen Schattenraben dazu
		if (talent("raven_call") > 0 && random.nextDouble() < 0.15 && target.isAlive()) {
			SwarmRaven.summon(level, this, target, (float) getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE), 8);
		}

		// Schattenschwarm: Schattenkrähen fallen über die Monster rund um das Ziel her
		if (talent("shadow_swarm") > 0 && random.nextDouble() < 0.2) {
			level.sendParticles(ParticleTypes.LARGE_SMOKE, target.getX(), target.getY() + 1.0, target.getZ(), 20, 1.5, 0.6, 1.5, 0.02);
			level.sendParticles(ParticleTypes.WITCH, target.getX(), target.getY() + 1.0, target.getZ(), 20, 1.5, 0.6, 1.5, 0.05);
			playSound(ModSounds.RAVEN_DEPART, 0.8f, 0.6f);

			for (LivingEntity enemy : level.getEntitiesOfClass(LivingEntity.class, target.getBoundingBox().inflate(4.0),
					e -> e != target && e instanceof net.minecraft.world.entity.monster.Enemy && e.isAlive())) {
				enemy.hurtServer(level, damageSources().indirectMagic(this, this), 3.0f);
			}
		}
	}

	/** Federfall und Nachtauge – Hilfen für den Besitzer. */
	private void ownerTalents(ServerLevel level) {
		if (!isActive() || !(getOwner() instanceof ServerPlayer owner) || owner.level() != level || owner.distanceToSqr(this) > 24 * 24) {
			return;
		}

		int fall = talent("feather_fall");

		if (fall > 0 && tickCount % 5 == 0 && owner.fallDistance > (fall == 1 ? 6.0 : 3.0) && !owner.isFallFlying()
				&& !owner.getAbilities().flying && !owner.hasEffect(net.minecraft.world.effect.MobEffects.SLOW_FALLING)) {
			owner.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.SLOW_FALLING, 60, 0));
			level.sendParticles(ParticleTypes.WHITE_ASH, owner.getX(), owner.getY() + 1.0, owner.getZ(), 15, 0.4, 0.5, 0.4, 0.02);
		}

		// Nachtschwingen: Nachtsicht, solange es um dich dunkel ist
		if (talent("night_wings") > 0 && tickCount % 100 == 0 && level.getMaxLocalRawBrightness(owner.blockPosition()) < 8) {
			owner.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.NIGHT_VISION, 20 * 15, 0, true, false, true));
		}

		if (talent("night_eye") > 0 && tickCount % 200 == 0) {
			for (LivingEntity enemy : level.getEntitiesOfClass(LivingEntity.class, owner.getBoundingBox().inflate(16.0),
					e -> e instanceof net.minecraft.world.entity.monster.Enemy && e.isAlive())) {
				enemy.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.GLOWING, 120, 0));
			}
		}
	}

	/** Plätze, die auf der aktuellen Stufe nutzbar sind. */
	public int capacity() {
		return 9 * getStage();
	}

	public boolean hasTerminal() {
		return terminal != null;
	}

	public int bagUsed() {
		return usedSlots();
	}

	/** Tasche aus dem Begleiter-Menü öffnen (in der Nähe, nicht unterwegs). */
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

	/** Für Menü und HUD: verbleibende Erkundung (> 0) oder Abklingzeit (< 0). */
	public int exploreTimer() {
		return exploreTicks > 0 ? exploreTicks : -exploreCooldown;
	}

	// ------------------------------------------------------------------
	// Schulter (Stufe 1)
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
	// Erkundungstour (v0.25.1): fliegt los und bringt Fundstücke mit – je Stufe wertvollere
	// ------------------------------------------------------------------

	private record Find(net.minecraft.world.item.Item item, int min, int max, int weight) {
	}

	private static final java.util.List<java.util.List<Find>> FINDS = java.util.List.of(
			java.util.List.of(
					new Find(net.minecraft.world.item.Items.RAW_COPPER, 3, 8, 10), new Find(net.minecraft.world.item.Items.COPPER_INGOT, 2, 6, 8),
					new Find(net.minecraft.world.item.Items.RAW_IRON, 2, 5, 8), new Find(net.minecraft.world.item.Items.IRON_INGOT, 1, 4, 6),
					new Find(net.minecraft.world.item.Items.IRON_NUGGET, 4, 12, 6), new Find(net.minecraft.world.item.Items.COAL, 4, 10, 8),
					new Find(net.minecraft.world.item.Items.FLINT, 2, 5, 4), new Find(net.minecraft.world.item.Items.FEATHER, 2, 6, 4),
					new Find(net.minecraft.world.item.Items.STRING, 2, 5, 4), new Find(net.minecraft.world.item.Items.AMETHYST_SHARD, 2, 6, 5),
					new Find(net.minecraft.world.item.Items.SWEET_BERRIES, 3, 8, 3)),
			java.util.List.of(
					new Find(net.minecraft.world.item.Items.RAW_GOLD, 2, 5, 8), new Find(net.minecraft.world.item.Items.GOLD_INGOT, 1, 4, 6),
					new Find(net.minecraft.world.item.Items.GOLD_NUGGET, 4, 12, 5), new Find(net.minecraft.world.item.Items.EMERALD, 1, 3, 6),
					new Find(net.minecraft.world.item.Items.LAPIS_LAZULI, 4, 10, 6), new Find(net.minecraft.world.item.Items.REDSTONE, 4, 10, 6),
					new Find(net.minecraft.world.item.Items.GLOW_BERRIES, 2, 6, 3), new Find(net.minecraft.world.item.Items.NAME_TAG, 1, 1, 1),
					new Find(net.minecraft.world.item.Items.SADDLE, 1, 1, 1)),
			java.util.List.of(
					new Find(net.minecraft.world.item.Items.DIAMOND, 1, 2, 6), new Find(net.minecraft.world.item.Items.EMERALD, 2, 5, 4),
					new Find(net.minecraft.world.item.Items.OBSIDIAN, 2, 4, 3), new Find(net.minecraft.world.item.Items.ENDER_PEARL, 1, 3, 4),
					new Find(net.minecraft.world.item.Items.EXPERIENCE_BOTTLE, 2, 5, 3), new Find(net.minecraft.world.item.Items.GOLDEN_APPLE, 1, 1, 2),
					new Find(net.minecraft.world.item.Items.ECHO_SHARD, 1, 1, 1)));

	/** Dauer einer Tour in Ticks (Talent Schnelle Lieferung: kürzer). */
	public int exploreDuration() {
		return (int) (20 * 60 * 4 * (1.0 - 0.25 * talent("swift_delivery")));
	}

	/** Aus dem Menü: auf Erkundung schicken. */
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
			int factor = t == stage ? 4 : t == stage - 1 ? 2 : 1;     // die eigene Stufe zählt am meisten

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
			// Schatzsucher: manchmal ein Fund aus der nächsten Stufe, auf Stufe 3 selten Netheritschrott
			if (talent("treasure_hunter") > 0 && stage == 3 && random.nextDouble() < 0.1) {
				found.add(new ItemStack(net.minecraft.world.item.Items.NETHERITE_SCRAP));
				continue;
			}

			int tier = talent("treasure_hunter") > 0 && stage < 3 && random.nextDouble() < 0.25 ? stage + 1 : stage;
			Find find = roll(tier, i == 0 || tier > stage);      // der erste Fund ist immer aus der eigenen Stufe
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
	// Ablauf
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

		if (getTarget() != null) {
			shoulderDelay = 30;
		} else if (shoulderDelay > 0) {
			shoulderDelay--;
		}

		if (isAway()) {
			if (!isInvisible()) {
				setInvisible(true);     // nach dem Laden: unterwegs bleibt er unsichtbar
			}

			tickAway(level);
			return;
		}

		ownerTalents(level);

		if (tickCount % 10 == 0 && isActive()) {
			collectItems(level);
		}

		// Abflug ab 2/3 voller Tasche (Talent Eilbote: ab 1/2 bzw. 1/3)
		int courier = talent("eager_courier");
		double fill = courier == 0 ? 2.0 / 3.0 : courier == 1 ? 0.5 : 1.0 / 3.0;

		if (tickCount % 40 == 0 && terminal != null && isActive() && departCooldown == 0 && usedSlots() >= Math.max(1, Math.ceil(capacity() * fill))) {
			depart(level, null);
		}
	}

	/** Items in der Nähe einsammeln – nicht die, die der Besitzer gerade selbst weggeworfen hat. */
	private void collectItems(ServerLevel level) {
		double radius = RADIUS[getStage() - 1] + 1.5 * talent("keen_eye") + equipmentTier();   // Amulett: +1 Block je Material
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
	// Lieferung zum Terminal
	// ------------------------------------------------------------------

	/** Fliegt los: unsichtbar, Flugzeit je nach Entfernung (Stufe 3 fliegt schneller). */
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
		// bleibt unsichtbar beim Besitzer, damit er geladen bleibt
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
		target.getChunk(pos.getX() >> 4, pos.getZ() >> 4);     // lädt den Terminal-Chunk
		return target.getBlockEntity(pos) instanceof StorageTerminalBlockEntity t ? t : null;
	}

	private void prepareTerminal(ServerLevel level) {
		StorageTerminalBlockEntity t = terminalEntity(level);

		if (t != null) {
			t.keepNetworkLoaded();    // Kisten-Chunks für die Ablieferung laden
		}
	}

	private void deliver(ServerLevel level) {
		StorageTerminalBlockEntity t = terminalEntity(level);
		int delivered = 0;
		boolean full = false;

		if (t != null && !(getOwner() instanceof Player owner && t.can(owner, StorageTerminalBlockEntity.Permission.HELPERS))) {
			t = null;    // kein Zugriff (mehr) auf dieses Terminal
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
		departCooldown = t == null || full ? 20 * 60 * 5 : 20 * 30;   // nach einem Fehlschlag lange warten

		if (getOwner() instanceof ServerPlayer owner && owner.level() == level) {
			Vec3 at = owner.position().add(owner.getLookAngle().multiply(1, 0, 1).normalize().scale(-1.5)).add(0, 1.2, 0);
			snapTo(at.x, at.y, at.z, getYRot(), getXRot());
		}

		level.sendParticles(ParticleTypes.WITCH, getX(), getY() + 0.4, getZ(), 15, 0.3, 0.3, 0.3, 0.05);
		playSound(ModSounds.RAVEN_RETURN, 1.0f, 1.0f);

 		// Glücksfund: bringt manchmal ein paar Amethyst-Splitter vom Flug mit
		if (t != null && talent("lucky_find") > 0 && random.nextDouble() < 0.3 && getOwner() instanceof ServerPlayer finder) {
			ItemStack find = new ItemStack(net.minecraft.world.item.Items.AMETHYST_SHARD, 1 + random.nextInt(3));

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
		if (isAway() && !source.is(net.minecraft.world.damagesource.DamageTypes.GENERIC_KILL)) {
			return false;
		}

		return super.hurtServer(level, source, amount);
	}

	// ------------------------------------------------------------------
	// Interaktion
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
	// Speichern
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
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		ContainerHelper.loadAllItems(input.childOrEmpty("RavenBag"), bag.getItems());
		terminal = input.read("Terminal", GlobalPos.CODEC).orElse(null);
		int away = input.getIntOr("Away", 0);
		exploreTicks = input.getIntOr("Explore", 0);
		exploreCooldown = input.getIntOr("ExploreCooldown", 0);

		if (exploreTicks > 0) {
			entityData.set(DATA_AWAY, true);
		}

		if (away > 0) {
			awayTicks = 0;
			deliverPhase = 1;     // nach dem Laden sofort abliefern
			entityData.set(DATA_AWAY, true);
		}
	}

	@Override
	public void remove(RemovalReason reason) {
		// beim echten Entfernen nichts verlieren: Tasche fallen lassen
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

	/** Ausschnitt der Tasche, der auf der aktuellen Stufe nutzbar ist. */
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
	// v0.25.3: Spezialangriffe
	// ------------------------------------------------------------------

	@Override
	protected int specialBaseCooldown(int which) {
		return switch (which) {
			case 0 -> 20 * 90;       // Rabenschwarm
			case 1 -> 20 * 40;       // Schattensturz
			default -> 20 * 90;      // Schreckensruf
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
		float base = (float) getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);

		switch (which) {
			case 0 -> {
				// Rabenschwarm: fünf Schattenraben stürzen sich nacheinander auf das Ziel
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
				// Schattensturz: er selbst stürzt sich auf das Ziel – dreifacher Schaden, Blindheit, Rückstoß
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
				target.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.BLINDNESS, 100, 0), this);
				Vec3 push = target.position().subtract(position()).multiply(1, 0, 1).normalize().scale(0.8);
				target.push(push.x, 0.3, push.z);
				level.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY() + target.getBbHeight() * 0.6, target.getZ(), 15, 0.3, 0.3, 0.3, 0.2);
				playSound(ModSounds.RAVEN_ATTACK, 1.3f, 0.6f);
				setTarget(target);
				return true;
			}
			default -> {
				// Schreckensruf: Monster in 10 Blöcken weichen zurück, werden langsam, schwach und sichtbar
				level.sendParticles(ParticleTypes.SONIC_BOOM, getX(), getY() + 0.6, getZ(), 1, 0, 0, 0, 0);
				playSound(ModSounds.RAVEN_AMBIENT, 2.0f, 0.5f);
				int duration = (int) (160 * power);

				for (LivingEntity enemy : level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(10.0),
						e -> e instanceof net.minecraft.world.entity.monster.Enemy && e.isAlive())) {
					enemy.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.SLOWNESS, duration, 1), this);
					enemy.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.WEAKNESS, duration, 0), this);
					enemy.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.GLOWING, duration, 0), this);
					Vec3 push = enemy.position().subtract(position()).multiply(1, 0, 1).normalize().scale(1.0);
					enemy.push(push.x, 0.35, push.z);

					if (enemy instanceof net.minecraft.world.entity.Mob mob) {
						mob.setTarget(null);
					}
				}

				return true;
			}
		}
	}
}
