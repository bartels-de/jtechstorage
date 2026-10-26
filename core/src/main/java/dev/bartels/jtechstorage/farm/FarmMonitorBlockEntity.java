package dev.bartels.jtechstorage.farm;

import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.bartels.jtechstorage.ModContent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import org.jspecify.annotations.Nullable;

import com.mojang.serialization.Codec;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Farm Monitor (v0.27.0): watches the container it is attached to and counts everything newly arriving (items per
 * hour, history of the last hour, most frequent items). Warns its owner when nothing arrives anymore or the container
 * is full, optionally creates a protection zone for companions around the farm and reports to the connected terminal.
 */
public class FarmMonitorBlockEntity extends BlockEntity {
	public static final int MAX_NAME = 24;
	public static final int[] IDLE_MINUTES = {5, 10, 30};
	public static final int[] RADII = {0, 8, 16, 24, 32};
	public static final int TOP = 5;
	public static final int HISTORY = 12;

	public static final int OK = 0;
	public static final int IDLE = 1;
	public static final int FULL = 2;
	public static final int NO_CONTAINER = 3;

	private @Nullable UUID owner;
	private String farmName = "";
	private int idle = 1;
	private int protect;
	private @Nullable GlobalPos terminal;
	private final int[] minutes = new int[60];
	private int minuteIndex;
	private int minuteTicks;
	private int minutesRun;
	private long lastInflow = -1;
	private final Map<Item, Long> totals = new HashMap<>();
	private int status = OK;
	private int warned = OK;
	private int tick;
	/** Game time when saving: after loading, the time the chunk was unloaded is not counted as downtime. */
	private long savedAt = -1;
	private boolean shiftAfterLoad;
	private @Nullable BlockPos watchedPos;
	/** Last state of the container (not saved – measured anew after loading). */
	private @Nullable Map<Item, Integer> previous;
	/** Display of the most frequent items in the menu. */
	private final SimpleContainer topDisplay = new SimpleContainer(TOP);
	private final long[] topCounts = new long[TOP];

	public FarmMonitorBlockEntity(BlockPos pos, BlockState state) {
		super(FarmContent.FARM_MONITOR_BLOCK_ENTITY, pos, state);
	}

	// ------------------------------------------------------------------
	// Values
	// ------------------------------------------------------------------

	public String getFarmName() {
		return farmName;
	}

	public String displayName() {
		return farmName.isBlank() ? Component.translatable("block.jtechstorage.farm_monitor").getString() : farmName;
	}

	public void setFarmName(String name) {
		farmName = clean(name);
		setChanged();
		publish();
	}

	public static String clean(String name) {
		String s = name.replaceAll("[\\p{Cntrl}§]", "").strip();
		return s.length() > MAX_NAME ? s.substring(0, MAX_NAME) : s;
	}

	public void setOwner(UUID owner) {
		this.owner = owner;
		setChanged();
	}

	public int getIdle() {
		return idle;
	}

	public void setIdle(int idle) {
		this.idle = Math.clamp(idle, 0, IDLE_MINUTES.length - 1);
		setChanged();
	}

	public int getProtect() {
		return protect;
	}

	public void setProtect(int protect) {
		this.protect = Math.clamp(protect, 0, RADII.length - 1);
		setChanged();

		if (level != null && !level.isClientSide()) {
			FarmZones.set(level, worldPosition, RADII[this.protect]);
		}
	}

	public @Nullable GlobalPos getTerminal() {
		return terminal;
	}

	public void setTerminal(@Nullable GlobalPos terminal) {
		this.terminal = terminal;
		setChanged();
		publish();
	}

	/** Connect to the terminal of the storage key or (same terminal) disconnect. */
	public InteractionResult linkWithKey(Player player, ItemStack key) {
		if (level == null || level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}

		GlobalPos bound = key.get(ModContent.LINKED_TERMINAL);

		if (!mayEdit(player)) {
			player.sendOverlayMessage(Component.translatable("jtechstorage.farm.not_owner"));
			return InteractionResult.FAIL;
		}

		if (bound == null) {
			player.sendOverlayMessage(Component.translatable("jtechstorage.farm.link_unbound"));
			return InteractionResult.FAIL;
		}

		boolean unlink = bound.equals(terminal);
		setTerminal(unlink ? null : bound);
		level.playSound(null, worldPosition, unlink ? SoundEvents.AMETHYST_BLOCK_HIT : SoundEvents.AMETHYST_BLOCK_CHIME,
				SoundSource.BLOCKS, 1.0f, unlink ? 0.7f : 1.5f);
		player.sendOverlayMessage(Component.translatable(unlink ? "jtechstorage.farm.unlinked" : "jtechstorage.farm.linked", displayName()));
		return InteractionResult.SUCCESS;
	}

	public int getStatus() {
		return status;
	}

	public SimpleContainer getTopDisplay() {
		return topDisplay;
	}

	public long topCount(int index) {
		return topCounts[index];
	}

	/** Measurement runtime in minutes (at most 60 count towards the average). */
	public int elapsedMinutes() {
		return minutesRun;
	}

	/** Minutes since the last new item, -1 = never. */
	public int minutesSinceInflow() {
		return lastInflow < 0 || level == null ? -1 : (int) Math.min(30000, (level.getGameTime() - lastInflow) / 1200);
	}

	/** Items per hour (from the last hour, extrapolated for shorter runtimes). */
	public int perHour() {
		long sum = 0;

		for (int m : minutes) {
			sum += m;
		}

		float elapsed = minutesRun >= 60 ? 59.0f + minuteTicks / 1200.0f : minutesRun + minuteTicks / 1200.0f;
		return (int) Math.min(Integer.MAX_VALUE, Math.round(sum * 60.0 / Math.max(1.0f, elapsed)));
	}

	/** History: 12 sections of 5 minutes, oldest first. */
	public int history(int bucket) {
		int sum = 0;

		for (int k = 0; k < 5; k++) {
			int age = (HISTORY - 1 - bucket) * 5 + k;           // minutes back
			sum += minutes[Math.floorMod(minuteIndex - age, 60)];
		}

		return sum;
	}

	public void reset() {
		Arrays.fill(minutes, 0);
		minuteIndex = 0;
		minuteTicks = 0;
		minutesRun = 0;
		lastInflow = -1;
		totals.clear();
		previous = null;
		updateTop();
		setChanged();
		publish();
	}

	// ------------------------------------------------------------------
	// Measuring
	// ------------------------------------------------------------------

	/** The observed container: behind the monitor, otherwise below, above or to the side – blocks only, no minecarts. */
	private @Nullable Container watched(Level level, BlockState state) {
		Direction facing = state.getValue(FarmMonitorBlock.FACING);
		Direction[] order = {facing.getOpposite(), Direction.DOWN, Direction.UP, facing.getClockWise(), facing.getCounterClockWise(), facing};

		for (Direction dir : order) {
			BlockPos at = worldPosition.relative(dir);

			if (!(level.getBlockEntity(at) instanceof Container) || level.getBlockEntity(at) instanceof HopperBlockEntity
					|| level.getBlockEntity(at) instanceof AmethystHopperBlockEntity) {
				continue;
			}

			Container c = HopperBlockEntity.getContainerAt(level, at);

			if (c != null) {
				if (!at.equals(watchedPos)) {
					watchedPos = at.immutable();
					previous = null;            // different container: measure anew, don't count everything as "new"
				}

				return c;
			}
		}

		watchedPos = null;
		return null;
	}

	/** May the player configure the monitor (owner; anyone if there is no owner; operators always)? */
	public boolean mayEdit(Player player) {
		return owner == null || owner.equals(player.getUUID())
				|| player instanceof ServerPlayer sp && sp.level().getServer().getPlayerList().isOp(sp.nameAndId());
	}

	public static void serverTick(Level level, BlockPos pos, BlockState state, FarmMonitorBlockEntity monitor) {
		if (++monitor.tick % 20 != 0 || !(level instanceof ServerLevel server)) {
			return;
		}

		FarmZones.set(level, pos, RADII[monitor.protect]);

		if (monitor.shiftAfterLoad) {
			monitor.shiftAfterLoad = false;

			if (monitor.lastInflow >= 0 && monitor.savedAt >= 0 && level.getGameTime() > monitor.savedAt) {
				monitor.lastInflow += level.getGameTime() - monitor.savedAt;
			}
		}

		Container container = monitor.watched(level, state);
		boolean full = false;
		int added = 0;

		if (container != null) {
			Map<Item, Integer> now = new HashMap<>();
			full = true;

			for (int slot = 0; slot < container.getContainerSize(); slot++) {
				ItemStack stack = container.getItem(slot);

				if (stack.isEmpty()) {
					full = false;
					continue;
				}

				now.merge(stack.getItem(), stack.getCount(), Integer::sum);

				if (stack.getCount() < container.getMaxStackSize(stack)) {
					full = false;
				}
			}

			if (monitor.previous != null) {
				for (Map.Entry<Item, Integer> e : now.entrySet()) {
					int diff = e.getValue() - monitor.previous.getOrDefault(e.getKey(), 0);

					if (diff > 0) {
						added += diff;
						monitor.totals.merge(e.getKey(), (long) diff, Long::sum);
					}
				}
			}

			monitor.previous = now;
		} else {
			monitor.previous = null;
		}

		long time = level.getGameTime();

		if (added > 0) {
			monitor.lastInflow = time;
			monitor.minutes[monitor.minuteIndex] = (int) Math.min(Integer.MAX_VALUE, (long) monitor.minutes[monitor.minuteIndex] + added);
			monitor.updateTop();
		}

		monitor.minuteTicks += 20;

		if (monitor.minuteTicks >= 1200) {
			monitor.minuteTicks = 0;
			monitor.minuteIndex = (monitor.minuteIndex + 1) % 60;
			monitor.minutes[monitor.minuteIndex] = 0;
			monitor.minutesRun = Math.min(60, monitor.minutesRun + 1);
			monitor.setChanged();
		}

		int idleTicks = IDLE_MINUTES[monitor.idle] * 1200;
		long since = monitor.lastInflow < 0 ? (long) monitor.minutesRun * 1200 + monitor.minuteTicks : time - monitor.lastInflow;
		int status = container == null ? NO_CONTAINER : full ? FULL : since > idleTicks ? IDLE : OK;

		if (status != monitor.status) {
			monitor.status = status;
			monitor.publish();
		}

		boolean warn = status != OK;

		if (state.getValue(FarmMonitorBlock.WARN) != warn) {
			level.setBlock(pos, state.setValue(FarmMonitorBlock.WARN, warn), 3);
		}

		if (status == OK) {
			monitor.warned = OK;
		} else if (status != monitor.warned && monitor.owner != null
				&& server.getServer().getPlayerList().getPlayer(monitor.owner) instanceof ServerPlayer player) {
			monitor.warned = status;           // only remember as warned if the message actually arrived
			player.sendSystemMessage(Component.translatable("jtechstorage.farm.warn." + status, monitor.displayName(),
					pos.getX(), pos.getY(), pos.getZ()).withStyle(ChatFormatting.GOLD));
		}

		if (monitor.tick % 1200 == 0) {
			monitor.publish();
		}
	}

	private void updateTop() {
		List<Map.Entry<Item, Long>> list = new ArrayList<>(totals.entrySet());
		list.sort((a, b) -> Long.compare(b.getValue(), a.getValue()));

		for (int i = 0; i < TOP; i++) {
			if (i < list.size()) {
				Item item = list.get(i).getKey();

				if (!topDisplay.getItem(i).is(item)) {
					topDisplay.setItem(i, new ItemStack(item));
				}

				topCounts[i] = list.get(i).getValue();
			} else {
				if (!topDisplay.getItem(i).isEmpty()) {
					topDisplay.setItem(i, ItemStack.EMPTY);
				}

				topCounts[i] = 0;
			}
		}
	}

	/** State for the "Farms" list in the terminal. */
	void publish() {
		if (!(level instanceof ServerLevel server)) {
			return;
		}

		String top = topDisplay.getItem(0).isEmpty() ? "" : BuiltInRegistries.ITEM.getKey(topDisplay.getItem(0).getItem()).toString();
		FarmData.get(server.getServer()).put(new FarmData.Entry(GlobalPos.of(level.dimension(), worldPosition), displayName(),
				Optional.ofNullable(terminal), perHour(), status, top, level.getGameTime()));
	}

	@Override
	public void setRemoved() {
		super.setRemoved();

		if (level != null && !level.isClientSide()) {
			FarmZones.remove(level, worldPosition);
		}
	}

	@Override
	public void preRemoveSideEffects(BlockPos pos, BlockState state) {
		super.preRemoveSideEffects(pos, state);

		if (level instanceof ServerLevel server) {
			FarmZones.remove(level, pos);
			FarmData.get(server.getServer()).remove(GlobalPos.of(level.dimension(), pos));
		}
	}

	// ------------------------------------------------------------------
	// Saving
	// ------------------------------------------------------------------

	private record Total(String item, long count) {
		static final Codec<Total> CODEC = RecordCodecBuilder.create(i -> i.group(
				Codec.STRING.fieldOf("item").forGetter(Total::item),
				Codec.LONG.fieldOf("count").forGetter(Total::count)
		).apply(i, Total::new));
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);

		if (owner != null) {
			output.store("Owner", UUIDUtil.CODEC, owner);
		}

		output.putString("FarmName", farmName);
		output.putInt("Idle", idle);
		output.putInt("Protect", protect);

		if (terminal != null) {
			output.store("Terminal", GlobalPos.CODEC, terminal);
		}

		output.putIntArray("Minutes", minutes.clone());
		output.putInt("MinuteIndex", minuteIndex);
		output.putInt("MinuteTicks", minuteTicks);
		output.putInt("MinutesRun", minutesRun);
		output.putLong("LastInflow", lastInflow);
		output.putLong("SavedAt", level != null ? level.getGameTime() : -1L);
		output.putInt("Warned", warned);
		List<Total> list = new ArrayList<>();
		totals.forEach((item, count) -> list.add(new Total(BuiltInRegistries.ITEM.getKey(item).toString(), count)));
		output.store("Totals", Total.CODEC.listOf(), list);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		owner = input.read("Owner", UUIDUtil.CODEC).orElse(null);
		farmName = clean(input.getStringOr("FarmName", ""));
		idle = Math.clamp(input.getIntOr("Idle", 1), 0, IDLE_MINUTES.length - 1);
		protect = Math.clamp(input.getIntOr("Protect", 0), 0, RADII.length - 1);
		terminal = input.read("Terminal", GlobalPos.CODEC).orElse(null);
		int[] stored = input.getIntArray("Minutes").orElse(new int[0]);
		System.arraycopy(stored, 0, minutes, 0, Math.min(stored.length, minutes.length));
		minuteIndex = Math.floorMod(input.getIntOr("MinuteIndex", 0), 60);
		minuteTicks = input.getIntOr("MinuteTicks", 0);
		minutesRun = input.getIntOr("MinutesRun", 0);
		lastInflow = input.getLongOr("LastInflow", -1L);
		savedAt = input.getLongOr("SavedAt", -1L);
		warned = input.getIntOr("Warned", OK);
		shiftAfterLoad = true;
		totals.clear();

		for (Total total : input.read("Totals", Total.CODEC.listOf()).orElse(List.of())) {
			Identifier id = Identifier.tryParse(total.item());

			if (id != null) {
				BuiltInRegistries.ITEM.getOptional(id).ifPresent(item -> totals.put(item, total.count()));
			}
		}

		updateTop();
	}
}
