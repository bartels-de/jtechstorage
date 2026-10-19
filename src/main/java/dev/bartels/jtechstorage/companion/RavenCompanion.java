package dev.bartels.jtechstorage.companion;

import org.jspecify.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
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
		return level().isClientSide() ? entityData.get(DATA_AWAY) : awayTicks > 0 || deliverPhase > 0;
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

		if (isAway()) {
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
		double radius = RADIUS[getStage() - 1] + 1.5 * talent("keen_eye");
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
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		ContainerHelper.loadAllItems(input.childOrEmpty("RavenBag"), bag.getItems());
		terminal = input.read("Terminal", GlobalPos.CODEC).orElse(null);
		int away = input.getIntOr("Away", 0);

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
}
