package dev.bartels.jtechstorage.entity;

import dev.bartels.jtechstorage.ModAdvancements;
import dev.bartels.jtechstorage.block.HelperBedBlock;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.util.Prediction;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import dev.bartels.jtechstorage.ModContent;
import dev.bartels.jtechstorage.block.StorageTerminalBlockEntity;

/**
 * Common base for network helpers: owner, link to a terminal via storage key,
 * status message, network name tag, cosmetics (dye colors the crystals, hats, shears remove them),
 * invulnerable and persistent.
 */
public abstract class NetworkHelperMob extends PathfinderMob implements Hologrammable {
	/** Name of the linked network – shown as a prefix in the name tag. */
	private static final EntityDataAccessor<Integer> DATA_TEAM_COLOR = SynchedEntityData.defineId(NetworkHelperMob.class,
			EntityDataSerializers.INT);
	private static final EntityDataAccessor<String> DATA_NETWORK_NAME = SynchedEntityData.defineId(NetworkHelperMob.class,
			EntityDataSerializers.STRING);
	/** Crystal color (RGB) or -1 = original amethyst. */
	private static final EntityDataAccessor<Integer> DATA_ACCENT = SynchedEntityData.defineId(NetworkHelperMob.class,
			EntityDataSerializers.INT);
	/** Off duty: the helper sleeps (at night – or by day for nocturnal ones) while the terminal has no resonance. */
	/** Only for Jbartels' tour: translucent, harmless replica without AI, not saved. */
	private static final EntityDataAccessor<Boolean> DATA_HOLOGRAM = SynchedEntityData.defineId(NetworkHelperMob.class,
			EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Boolean> DATA_RESTING = SynchedEntityData.defineId(NetworkHelperMob.class,
			EntityDataSerializers.BOOLEAN);

	protected @Nullable BlockPos terminalPos;
	protected @Nullable UUID owner;
	private String status;
	private Object[] statusArgs = new Object[0];

	protected NetworkHelperMob(EntityType<? extends NetworkHelperMob> type, Level level) {
		super(type, level);
		setPermanentlyInvulnerable(true);
		setPersistenceRequired();
		setDropChance(EquipmentSlot.HEAD, 0.0f);
		status = unlinkedStatus();
		goalSelector.addGoal(0, new RestGoal());
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DATA_NETWORK_NAME, "");
		builder.define(DATA_TEAM_COLOR, 0xFF55FF);
		builder.define(DATA_ACCENT, -1);
		builder.define(DATA_RESTING, false);
		builder.define(DATA_HOLOGRAM, false);
	}

	// ------------------------------------------------------------------
	// Hologram
	// ------------------------------------------------------------------

	public boolean isHologram() {
		return entityData.get(DATA_HOLOGRAM);
	}

	/** Take over data from the packed item (Griffin, Amethyst Phoenix override this). */
	public void loadFromItem(ItemStack stack) {
	}

	public void makeHologram() {
		entityData.set(DATA_HOLOGRAM, true);
		setNoAi(true);
		setNoGravity(true);
		setSilent(true);
	}

	@Override
	public void tick() {
		if (isHologram() && !level().isClientSide()) {
			// no behavior, just rotate slowly
			tickCount++;
			setYRot(getYRot() + 2.0f);
			yBodyRot = getYRot();
			yHeadRot = getYRot();
			return;
		}

		super.tick();
	}

	@Override
	public boolean shouldBeSaved() {
		return !isHologram() && super.shouldBeSaved();
	}

	@Override
	public boolean isPushable() {
		return !isHologram() && super.isPushable();
	}

	// ------------------------------------------------------------------
	// Sounds
	// ------------------------------------------------------------------

	/** The helper's own ambient sound (null = silent). Silent when off duty and as a hologram. */
	protected net.minecraft.sounds.@Nullable SoundEvent helperAmbientSound() {
		return null;
	}

	/** Quiet sleeping sound while off duty (null = silent). */
	protected net.minecraft.sounds.@Nullable SoundEvent helperSleepSound() {
		return null;
	}

	@Override
	protected net.minecraft.sounds.@Nullable SoundEvent getAmbientSound() {
		if (isHologram()) {
			return null;
		}

		return isResting() ? helperSleepSound() : helperAmbientSound();
	}

	@Override
	public int getAmbientSoundInterval() {
		return 320;
	}

	@Override
	protected float getSoundVolume() {
		return 0.6f;
	}

	// ------------------------------------------------------------------
	// Off duty
	// ------------------------------------------------------------------

	/** May this helper go off duty at all? (Not Peter Chel Parker.) */
	protected boolean canRest() {
		return true;
	}

	/** Nocturnal helpers (Watch Moth) work at night and sleep by day. */
	protected boolean isNocturnal() {
		return false;
	}

	/** Sleeps even when the terminal has resonance (the beetle – otherwise it would keep itself awake). */
	protected boolean restsEvenWhenCharged() {
		return false;
	}

	public boolean isResting() {
		return entityData.get(DATA_RESTING);
	}

	private void updateResting() {
		boolean dark = level().isDarkOutside();
		boolean offShift = isNocturnal() ? !dark : dark;
		StorageTerminalBlockEntity terminal = terminal();
		boolean charged = terminal != null && terminal.isCharged();
		boolean rest = canRest() && offShift && (!charged || restsEvenWhenCharged()) && !isVehicle();

		if (rest != isResting()) {
			entityData.set(DATA_RESTING, rest);

			if (rest) {
				getNavigation().stop();
				playSound(SoundEvents.AMETHYST_BLOCK_CHIME, 0.4f, 0.6f);
			} else {
				playSound(SoundEvents.AMETHYST_BLOCK_CHIME, 0.6f, 1.6f);
			}
		}
	}

	/**
	 * Which sleeping spot suits this helper? (v0.26) 0 = none (sleeps where it is), 1 = helper bed, 2 = wolf basket.
	 * Large or flying helpers override this with 0.
	 */
	protected int bedKind() {
		return HelperBedBlock.KIND_BED;
	}

	/** Bed this helper has reserved for the night (memory only, released on waking up). */
	private @Nullable BlockPos claimedBed;

	/**
	 * Free sleeping spot of the matching kind nearby (around the terminal or the helper), or null. Beds that another helper
	 * has already reserved or that someone is lying in don't count – so everyone finds their own.
	 */
	protected @Nullable BlockPos findBed() {
		int kind = bedKind();

		if (kind == 0) {
			return null;
		}

		BlockPos center = terminalPos != null && terminalPos.distSqr(blockPosition()) < 48 * 48 ? terminalPos : blockPosition();
		Set<BlockPos> taken = takenBeds(center);
		BlockPos best = null;
		double bestDist = Double.MAX_VALUE;

		for (BlockPos pos : BlockPos.betweenClosed(center.offset(-BED_SEARCH, -6, -BED_SEARCH), center.offset(BED_SEARCH, 6, BED_SEARCH))) {
			BlockState state = level().getBlockState(pos);

			if (!(state.getBlock() instanceof HelperBedBlock bed) || bed.kind() != kind) {
				continue;
			}

			double dist = pos.distSqr(blockPosition());

			if (dist < bestDist && !taken.contains(pos)) {
				bestDist = dist;
				best = pos.immutable();
			}
		}

		return best;
	}

	/** Beds in the search area that other helpers have reserved or where someone is currently sleeping. */
	private Set<BlockPos> takenBeds(BlockPos center) {
		Set<BlockPos> taken = new HashSet<>();
		AABB area = new AABB(center).inflate(BED_SEARCH + 32, 16, BED_SEARCH + 32);

		for (NetworkHelperMob other : level().getEntitiesOfClass(NetworkHelperMob.class, area, other -> other != this && other.isAlive())) {
			if (other.claimedBed != null) {
				taken.add(other.claimedBed);
			}

			if (other.isResting()) {
				taken.add(BlockPos.containing(other.getX(), other.getY() + 0.1, other.getZ()));
			}
		}

		return taken;
	}

	private static final int BED_SEARCH = 16;

	/** Height of the lying surface above the block's bottom edge (helper bed 8 px, wolf basket 3 px). */
	private static double bedHeight(int kind) {
		return kind == HelperBedBlock.KIND_BASKET ? 3.0 / 16.0 : 8.0 / 16.0;
	}

	/**
	 * Blocks all movement goals while off duty – first walks to its sleeping spot (bed/basket).
	 * If the path fails (own flying/hovering control, obstacles), it jumps straight in after a while.
	 */
	private class RestGoal extends Goal {
		private @Nullable BlockPos bed;
		private int walkTicks;
		private int retry;
		private boolean settled;

		RestGoal() {
			setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
		}

		@Override
		public boolean canUse() {
			return isResting();
		}

		@Override
		public boolean requiresUpdateEveryTick() {
			return true;
		}

		@Override
		public void start() {
			getNavigation().stop();
			bed = null;
			claimedBed = null;
			settled = false;
			retry = 0;
		}

		@Override
		public void tick() {
			if (isHologram() || isPassenger()) {
				return;
			}

			if (bed == null || !(level().getBlockState(bed).getBlock() instanceof HelperBedBlock)) {
				bed = null;
				claimedBed = null;
				settled = false;
				getNavigation().stop();

				if (--retry <= 0) {
					// no free bed: lies down on the floor where it is – checks now and then whether one becomes free
					retry = 200;
					bed = findBed();
					claimedBed = bed;
					walkTicks = 20 * 15;
				}
				return;
			}

			double tx = bed.getX() + 0.5;
			double ty = bed.getY() + bedHeight(bedKind());
			double tz = bed.getZ() + 0.5;
			double dx = getX() - tx;
			double dz = getZ() - tz;
			double horizontal = dx * dx + dz * dz;

			if (settled) {
				getNavigation().stop();

				if (horizontal > 0.25 || Math.abs(getY() - ty) > 0.6) {
					settled = false;   // pushed away – go there again
					walkTicks = 20 * 10;
				}
				return;
			}

			if (horizontal < 0.35 && Math.abs(getY() - ty) < 0.8) {
				settle(tx, ty, tz);
				return;
			}

			if (--walkTicks <= 0) {
				// path fails: hop straight into the bed (only from close by)
				if (horizontal < 32 * 32 && level() instanceof ServerLevel server) {
					server.sendParticles(ParticleTypes.WITCH, getX(), getY() + 0.4, getZ(), 10, 0.3, 0.3, 0.3, 0.02);
					settle(tx, ty, tz);
					server.sendParticles(ParticleTypes.WITCH, tx, ty + 0.4, tz, 10, 0.3, 0.3, 0.3, 0.02);
				} else {
					bed = null;
					claimedBed = null;
				}
				return;
			}

			if (tickCount % 10 == 0 || getNavigation().isDone()) {
				boolean path = getNavigation().moveTo(tx, bed.getY(), tz, 1.0);

				if (!path) {
					getMoveControl().setWantedPosition(tx, ty, tz, 1.0);
				}
			}

			// last steps: navigation often stops just short – steer directly
			if (horizontal < 2.5 * 2.5) {
				getMoveControl().setWantedPosition(tx, ty, tz, 0.8);
			}
		}

		private void settle(double x, double y, double z) {
			getNavigation().stop();
			snapTo(x, y, z, getYRot(), getXRot());
			setDeltaMovement(Vec3.ZERO);
			settled = true;
		}

		@Override
		public void stop() {
			bed = null;
			claimedBed = null;
			settled = false;
		}
	}

	// ------------------------------------------------------------------
	// Cosmetics
	// ------------------------------------------------------------------

	public int getAccentColor() {
		return entityData.get(DATA_ACCENT);
	}

	public void setAccentColor(int color) {
		entityData.set(DATA_ACCENT, color);
	}

	public static boolean isCosmeticItem(ItemStack stack) {
		return stack.has(DataComponents.DYE) || stack.is(ModContent.HELPER_HATS) || stack.is(Items.SHEARS);
	}

	/** Dye, hat or shears – returns {@code null} if the item is not a cosmetic. */
	protected @Nullable InteractionResult tryCosmetics(Player player, ItemStack held) {
		DyeColor dye = held.get(DataComponents.DYE);

		if (dye != null) {
			setAccentColor(dye.getTextureDiffuseColor() & 0xFFFFFF);
			held.consume(1, player);
			playSound(SoundEvents.DYE_USE, 1.0f, 1.0f);
			((ServerLevel) level()).sendParticles(ParticleTypes.HAPPY_VILLAGER, getX(), getY() + getBbHeight(), getZ(),
					8, 0.3, 0.2, 0.3, 0.0);
			player.sendOverlayMessage(Component.translatable("jtechstorage.cosmetic.dyed", getDisplayName(),
					Component.translatable("color.minecraft." + dye.getSerializedName())));
			return InteractionResult.SUCCESS;
		}

		if (held.is(ModContent.HELPER_HATS)) {
			returnHat(player);
			setItemSlot(EquipmentSlot.HEAD, held.copyWithCount(1));
			held.consume(1, player);
			playSound(SoundEvents.ARMOR_EQUIP_LEATHER.value(), 1.0f, 1.2f);
			player.sendOverlayMessage(Component.translatable("jtechstorage.cosmetic.hat", getDisplayName()));
			return InteractionResult.SUCCESS;
		}

		if (held.is(Items.SHEARS)) {
			if (getItemBySlot(EquipmentSlot.HEAD).isEmpty() && getAccentColor() == -1) {
				return InteractionResult.PASS;
			}

			returnHat(player);
			setAccentColor(-1);
			playSound(SoundEvents.SHEEP_SHEAR, 1.0f, 1.3f);
			player.sendOverlayMessage(Component.translatable("jtechstorage.cosmetic.reset", getDisplayName()));
			return InteractionResult.SUCCESS;
		}

		return null;
	}

	/** Hat back to the player. */
	protected void returnHat(Player player) {
		ItemStack hat = getItemBySlot(EquipmentSlot.HEAD);

		if (!hat.isEmpty()) {
			player.getInventory().placeItemBackInInventory(hat.copy(), Prediction.SERVER_ONLY);
			setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
		}
	}

	/** Spawn item when packing up: name and color are kept, the hat goes back to the player. */
	protected ItemStack createPackedItem(Item item, Player player) {
		returnHat(player);
		ItemStack stack = new ItemStack(item);

		if (hasCustomName()) {
			stack.set(DataComponents.CUSTOM_NAME, getCustomName());
		}

		if (getAccentColor() != -1) {
			stack.set(DataComponents.DYED_COLOR, new DyedItemColor(getAccentColor()));
		}

		return stack;
	}

	/** When placing from the item: take over color. */
	public void applyItemCosmetics(ItemStack stack) {
		DyedItemColor color = stack.get(DataComponents.DYED_COLOR);

		if (color != null) {
			setAccentColor(color.rgb());
		}
	}

	public String getNetworkName() {
		return entityData.get(DATA_NETWORK_NAME);
	}

	/** Takes over the network name from the terminal (server only). */
	protected void syncNetworkName() {
		StorageTerminalBlockEntity terminal = terminal();

		if (terminal != null) {
			entityData.set(DATA_NETWORK_NAME, terminal.getNetworkName());
			entityData.set(DATA_TEAM_COLOR, terminal.getTeamRgb());
		} else if (terminalPos == null) {
			entityData.set(DATA_NETWORK_NAME, "");
		}
	}

	@Override
	public void aiStep() {
		super.aiStep();

		if (!level().isClientSide() && tickCount % 40 == 5) {
			syncNetworkName();
		}

		if (!level().isClientSide() && tickCount % 20 == 7) {
			updateResting();
		}
	}

	/** Name tag: "Network · mob name" (mob name = custom name or type name). */
	@Override
	public Component getDisplayName() {
		Component base = super.getDisplayName();
		String network = getNetworkName();

		if (network.isEmpty()) {
			return base;
		}

		return Component.empty()
				.append(Component.literal(network).withStyle(style -> style.withColor(entityData.get(DATA_TEAM_COLOR))))
				.append(Component.literal(" · ").withStyle(ChatFormatting.DARK_GRAY))
				.append(base);
	}

	/** The name is always shown above the mob. */
	@Override
	public boolean shouldShowName() {
		return true;
	}

	/** Status message while no network is linked. */
	protected abstract String unlinkedStatus();

	/** Name of the advancement for linking. */
	protected abstract String linkAdvancement();

	/** Called after linking. */
	protected void onLinked(Player player) {
	}

	/** Right-click without a key – default: show status. */
	protected InteractionResult interactWith(Player player, InteractionHand hand) {
		if (player.getItemInHand(hand).isEmpty()) {
			player.sendOverlayMessage(statusText());
			return InteractionResult.SUCCESS;
		}

		return InteractionResult.PASS;
	}

	public void setOwner(Player player) {
		owner = player.getUUID();
	}

	public @Nullable UUID getOwnerId() {
		return owner;
	}

	protected boolean mayInteract(Player player) {
		if (owner == null || owner.equals(player.getUUID())) {
			return true;
		}

		return terminal() instanceof StorageTerminalBlockEntity terminal
				&& terminal.can(player, StorageTerminalBlockEntity.Permission.HELPERS);
	}

	/** Message in the team channel of the linked network (if linked and loaded). */
	protected void teamBroadcast(StorageTerminalBlockEntity.Channel channel, Component message) {
		StorageTerminalBlockEntity terminal = terminal();

		if (terminal != null) {
			terminal.broadcast(channel, message);
		}
	}

	protected @Nullable StorageTerminalBlockEntity terminal() {
		if (terminalPos == null || !level().isLoaded(terminalPos)) {
			return null;
		}

		return level().getBlockEntity(terminalPos) instanceof StorageTerminalBlockEntity terminal ? terminal : null;
	}

	/** Resonance bonus of the terminal: helpers work faster. */
	protected double speedFactor() {
		StorageTerminalBlockEntity terminal = terminal();
		return terminal != null ? terminal.helperSpeedFactor() : 1.0;
	}

	/** Current status message (e.g. for menus). */
	public Component statusText() {
		if (isResting()) {
			return Component.translatable(isNocturnal() ? "jtechstorage.helper.status.resting_day" : "jtechstorage.helper.status.resting");
		}

		return Component.translatable(status, statusArgs);
	}

	protected void setStatus(String key, Object... args) {
		status = key;
		statusArgs = args;
	}

	@Override
	protected InteractionResult mobInteract(Player player, InteractionHand hand) {
		ItemStack held = player.getItemInHand(hand);

		if (isHologram()) {
			return InteractionResult.PASS;
		}

		if (level().isClientSide()) {
			return held.isEmpty() || held.is(ModContent.STORAGE_KEY) || isCosmeticItem(held)
					? InteractionResult.SUCCESS : super.mobInteract(player, hand);
		}

		if (!mayInteract(player)) {
			player.sendOverlayMessage(Component.translatable("jtechstorage.helper.not_yours"));
			return InteractionResult.FAIL;
		}

		InteractionResult cosmetic = tryCosmetics(player, held);

		if (cosmetic != null && cosmetic != InteractionResult.PASS) {
			return cosmetic;
		}

		GlobalPos bound = held.get(ModContent.LINKED_TERMINAL);

		if (held.is(ModContent.STORAGE_KEY) && bound != null) {
			if (!bound.dimension().equals(level().dimension()) || !level().isLoaded(bound.pos())
					|| !(level().getBlockEntity(bound.pos()) instanceof StorageTerminalBlockEntity terminal)) {
				player.sendOverlayMessage(Component.translatable("jtechstorage.message.terminal_missing"));
				return InteractionResult.FAIL;
			}

			if (!terminal.can(player, StorageTerminalBlockEntity.Permission.HELPERS)) {
				player.sendOverlayMessage(Component.translatable("jtechstorage.message.no_permission",
						Component.translatable("jtechstorage.role.builder")));
				return InteractionResult.FAIL;
			}

			terminalPos = bound.pos().immutable();

			if (owner == null) {
				setOwner(player);
			}

			onLinked(player);
			syncNetworkName();
			((ServerLevel) level()).sendParticles(ParticleTypes.WITCH, getX(), getY() + 0.5, getZ(), 20, 0.3, 0.3, 0.3, 0.02);
			playSound(SoundEvents.AMETHYST_BLOCK_CHIME, 1.0f, 1.4f);
			player.sendOverlayMessage(Component.translatable("jtechstorage.helper.linked", getDisplayName()));

			if (player instanceof ServerPlayer serverPlayer) {
				ModAdvancements.grant(serverPlayer, linkAdvancement());
			}

			return InteractionResult.SUCCESS;
		}

		// Uniform for all helpers: Shift + right-click with an empty hand = pack up – twice, for safety
		if (hand == InteractionHand.MAIN_HAND && held.isEmpty() && player.isSecondaryUseActive() && !confirmPackUp(player)) {
			return InteractionResult.SUCCESS;
		}

		InteractionResult result = interactWith(player, hand);
		return result == InteractionResult.PASS ? super.mobInteract(player, hand) : result;
	}

	// ------------------------------------------------------------------
	// Action animation (working, alarm, sneezing …) – sent to clients via entity event
	// ------------------------------------------------------------------

	/** Entity event ID for the action animation. */
	public static final byte EVENT_ACTION = 70;
	public static final int ACTION_TICKS = 16;
	private int actionStart = -1000;

	/** Starts the action animation on all clients (server only). */
	protected void playAction() {
		level().broadcastEntityEvent(this, EVENT_ACTION);
	}

	@Override
	public void handleEntityEvent(byte id) {
		if (id == EVENT_ACTION) {
			actionStart = tickCount;
			return;
		}

		super.handleEntityEvent(id);
	}

	/** 0 → 1 → 0 during the action (for renderers). */
	public float getActionAmount(float partialTick) {
		float t = (tickCount - actionStart + partialTick) / ACTION_TICKS;
		return t < 0.0f || t > 1.0f ? 0.0f : Mth.sin(t * Mth.PI);
	}

	private @Nullable UUID packRequester;
	private long packRequestTime;

	/** First Shift + right-click: hint; second within 3 seconds: actually pack up. */
	protected boolean confirmPackUp(Player player) {
		long now = level().getGameTime();

		if (player.getUUID().equals(packRequester) && now - packRequestTime <= 60) {
			packRequester = null;
			return true;
		}

		packRequester = player.getUUID();
		packRequestTime = now;
		player.sendOverlayMessage(Component.translatable("jtechstorage.helper.pack_confirm", getDisplayName()).withStyle(ChatFormatting.YELLOW));
		return false;
	}

	@Override
	protected void checkFallDamage(double distance, boolean onGround, BlockState state, BlockPos pos) {
		// Network helpers take no fall damage
	}

	@Override
	public boolean removeWhenFarAway(double distance) {
		return false;
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);

		if (terminalPos != null) {
			output.store("Terminal", BlockPos.CODEC, terminalPos);
		}

		if (owner != null) {
			output.store("Owner", UUIDUtil.CODEC, owner);
		}

		output.putInt("AccentColor", getAccentColor());
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		terminalPos = input.read("Terminal", BlockPos.CODEC).orElse(null);
		owner = input.read("Owner", UUIDUtil.CODEC).orElse(null);
		setAccentColor(input.getIntOr("AccentColor", -1));
	}
}
