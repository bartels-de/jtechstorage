package dev.bartels.jtechstorage.block;

import dev.bartels.jtechstorage.helpers.HelpersContent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.mojang.serialization.Codec;

import java.util.Locale;
import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.ItemStackWithSlot;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import dev.bartels.jtechstorage.network.StorageNetwork;

/**
 * Stop of the Amethyst Phoenix: name, rest duration and delivery order. On landing, the delivery order swaps items
 * between the phoenix's chests and chests/barrels directly next to the stop.
 */
public class PhoenixStopBlockEntity extends BlockEntity {
	public static final int MAX_NAME = 24;
	public static final int FILTERS = 9;
	public static final int MAX_AMOUNT = 9999;

	/** Rest duration at this stop. */
	public enum Rest {
		SHORT(600),
		FIVE_MINUTES(6000),
		NIGHT(-1);

		/** Ticks, -1 = until the next sunrise after a full night. */
		public final int ticks;

		Rest(int ticks) {
			this.ticks = ticks;
		}

		public String key() {
			return "jtechstorage.phoenix_stop.rest." + name().toLowerCase(Locale.ROOT);
		}

		public static Rest byId(int id) {
			Rest[] values = values();
			return values[Math.floorMod(id, values.length)];
		}
	}

	/**
	 * Delivery order. Unload: items from the phoenix into the chests (only the filtered ones if filters are set). Load: items from the chests
	 * into the phoenix (only the filtered ones if filters are set). Both: unload everything that is not on the list, then
	 * load the list items.
	 */
	public enum Mode {
		NONE,
		UNLOAD,
		LOAD,
		BOTH;

		public String key() {
			return "jtechstorage.phoenix_stop.mode." + name().toLowerCase(Locale.ROOT);
		}

		public static Mode byId(int id) {
			Mode[] values = values();
			return values[Math.floorMod(id, values.length)];
		}
	}

	private String stopName = "";
	private Rest rest = Rest.NIGHT;
	private Mode mode = Mode.NONE;
	/** Filters (templates, 1 item each) – never actually stored. */
	private final SimpleContainer filters = new SimpleContainer(FILTERS) {
		@Override
		public void setChanged() {
			super.setChanged();
			PhoenixStopBlockEntity.this.setChanged();
		}
	};
	/** Amount per filter and visit, 0 = all. */
	private final int[] amounts = new int[FILTERS];
	private long lastVisit = -1;
	private int lastUnloaded;
	private int lastLoaded;

	public PhoenixStopBlockEntity(BlockPos pos, BlockState state) {
		super(HelpersContent.PHOENIX_STOP_BLOCK_ENTITY, pos, state);
	}

	public String getStopName() {
		return stopName;
	}

	public boolean hasName() {
		return !stopName.isBlank();
	}

	public Rest getRest() {
		return rest;
	}

	public Mode getMode() {
		return mode;
	}

	public SimpleContainer getFilters() {
		return filters;
	}

	public int getAmount(int slot) {
		return amounts[slot];
	}

	public long getLastVisit() {
		return lastVisit;
	}

	public int getLastUnloaded() {
		return lastUnloaded;
	}

	public int getLastLoaded() {
		return lastLoaded;
	}

	public void apply(String name, Rest rest) {
		this.stopName = clean(name);
		this.rest = rest;
		changed();
	}

	public void setRest(Rest rest) {
		this.rest = rest;
		changed();
	}

	public void setMode(Mode mode) {
		this.mode = mode;
		changed();
	}

	public void setAmount(int slot, int amount) {
		amounts[slot] = Math.clamp(amount, 0, MAX_AMOUNT);
		changed();
	}

	private void changed() {
		setChanged();

		if (level != null && !level.isClientSide()) {
			level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
		}
	}

	public static String clean(String name) {
		String trimmed = name.strip().replaceAll("[\\p{Cntrl}§]", "");
		return trimmed.length() > MAX_NAME ? trimmed.substring(0, MAX_NAME) : trimmed;
	}

	// ------------------------------------------------------------------
	// Delivery order
	// ------------------------------------------------------------------

	/** Result of a visit: items unloaded and loaded. */
	public record Exchange(int unloaded, int loaded) {
		public static final Exchange NONE = new Exchange(0, 0);
	}

	/** Chests, barrels etc. directly next to the stop (the phoenix lands on top). */
	private List<Container> neighbours(ServerLevel level) {
		List<Container> result = new ArrayList<>();

		for (Direction direction : Direction.values()) {
			if (direction == Direction.UP) {
				continue;
			}

			Container container = HopperBlockEntity.getContainerAt(level, worldPosition.relative(direction));

			if (container != null && !result.contains(container)) {
				result.add(container);
			}
		}

		return result;
	}

	private boolean hasFilters() {
		return !filters.isEmpty();
	}

	/** Index of the matching filter or -1. Compares by item only (no enchantments, names etc.). */
	private int ruleFor(ItemStack stack) {
		for (int slot = 0; slot < FILTERS; slot++) {
			ItemStack filter = filters.getItem(slot);

			if (!filter.isEmpty() && ItemStack.isSameItem(filter, stack)) {
				return slot;
			}
		}

		return -1;
	}

	private int[] budgets() {
		int[] budget = new int[FILTERS];

		for (int slot = 0; slot < FILTERS; slot++) {
			budget[slot] = amounts[slot] > 0 ? amounts[slot] : Integer.MAX_VALUE;
		}

		return budget;
	}

	/**
	 * Runs the delivery order. {@code cargo} are the phoenix's chests, of which the first {@code slots} slots are usable.
	 */
	public Exchange exchange(ServerLevel level, Container cargo, int slots) {
		if (mode == Mode.NONE || slots <= 0) {
			return Exchange.NONE;
		}

		List<Container> chests = neighbours(level);

		if (chests.isEmpty()) {
			return Exchange.NONE;
		}

		boolean filtered = hasFilters();
		int unloaded = 0;
		int loaded = 0;

		if (mode == Mode.UNLOAD || mode == Mode.BOTH) {
			int[] budget = budgets();

			for (int slot = 0; slot < slots; slot++) {
				ItemStack stack = cargo.getItem(slot);

				if (stack.isEmpty()) {
					continue;
				}

				int rule = ruleFor(stack);
				boolean unload = mode == Mode.UNLOAD ? !filtered || rule >= 0 : rule < 0;

				if (!unload) {
					continue;
				}

				int limit = mode == Mode.UNLOAD && rule >= 0 ? Math.min(budget[rule], stack.getCount()) : stack.getCount();

				if (limit <= 0) {
					continue;
				}

				ItemStack part = stack.copyWithCount(limit);

				for (Container chest : chests) {
					if (part.isEmpty()) {
						break;
					}

					StorageNetwork.insertInto(chest, part);
					chest.setChanged();
				}

				int moved = limit - part.getCount();

				if (moved > 0) {
					stack.shrink(moved);
					cargo.setItem(slot, stack.isEmpty() ? ItemStack.EMPTY : stack);
					unloaded += moved;

					if (mode == Mode.UNLOAD && rule >= 0) {
						budget[rule] -= moved;
					}
				}
			}
		}

		if ((mode == Mode.LOAD || mode == Mode.BOTH) && (filtered || mode == Mode.LOAD)) {
			int[] budget = budgets();

			for (Container chest : chests) {
				for (int slot = 0; slot < chest.getContainerSize(); slot++) {
					ItemStack stack = chest.getItem(slot);

					if (stack.isEmpty()) {
						continue;
					}

					int rule = ruleFor(stack);

					if (filtered && rule < 0) {
						continue;
					}

					int limit = rule >= 0 ? Math.min(budget[rule], stack.getCount()) : stack.getCount();

					if (limit <= 0) {
						continue;
					}

					ItemStack part = stack.copyWithCount(limit);
					insertCargo(cargo, slots, part);
					int moved = limit - part.getCount();

					if (moved > 0) {
						stack.shrink(moved);
						chest.setItem(slot, stack.isEmpty() ? ItemStack.EMPTY : stack);
						chest.setChanged();
						loaded += moved;

						if (rule >= 0) {
							budget[rule] -= moved;
						}
					}
				}
			}
		}

		cargo.setChanged();
		lastVisit = level.getGameTime();
		lastUnloaded = unloaded;
		lastLoaded = loaded;
		changed();
		return new Exchange(unloaded, loaded);
	}

	/** Puts {@code stack} into the first {@code slots} slots of the phoenix (modifies {@code stack}). */
	private static void insertCargo(Container cargo, int slots, ItemStack stack) {
		for (int slot = 0; slot < slots && !stack.isEmpty(); slot++) {
			ItemStack existing = cargo.getItem(slot);

			if (!existing.isEmpty() && ItemStack.isSameItemSameComponents(existing, stack)) {
				int move = Math.min(stack.getCount(), existing.getMaxStackSize() - existing.getCount());

				if (move > 0) {
					existing.grow(move);
					stack.shrink(move);
				}
			}
		}

		for (int slot = 0; slot < slots && !stack.isEmpty(); slot++) {
			if (cargo.getItem(slot).isEmpty()) {
				int move = Math.min(stack.getCount(), stack.getMaxStackSize());
				cargo.setItem(slot, stack.split(move));
			}
		}
	}

	// ------------------------------------------------------------------
	// Saving & sync
	// ------------------------------------------------------------------

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putString("StopName", stopName);
		output.putInt("Rest", rest.ordinal());
		output.putInt("Mode", mode.ordinal());
		output.store("Amounts", Codec.INT.listOf(), Arrays.stream(amounts).boxed().toList());
		output.putLong("LastVisit", lastVisit);
		output.putInt("LastUnloaded", lastUnloaded);
		output.putInt("LastLoaded", lastLoaded);
		ValueOutput.TypedOutputList<ItemStackWithSlot> items = output.list("Filters", ItemStackWithSlot.CODEC);

		for (int slot = 0; slot < FILTERS; slot++) {
			ItemStack stack = filters.getItem(slot);

			if (!stack.isEmpty()) {
				items.add(new ItemStackWithSlot(slot, stack));
			}
		}
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		stopName = clean(input.getStringOr("StopName", ""));
		rest = Rest.byId(input.getIntOr("Rest", Rest.NIGHT.ordinal()));
		mode = Mode.byId(input.getIntOr("Mode", Mode.NONE.ordinal()));
		List<Integer> stored = input.read("Amounts", Codec.INT.listOf()).orElse(List.of());

		for (int slot = 0; slot < FILTERS; slot++) {
			amounts[slot] = slot < stored.size() ? Math.clamp(stored.get(slot), 0, MAX_AMOUNT) : 0;
		}

		lastVisit = input.getLongOr("LastVisit", -1L);
		lastUnloaded = input.getIntOr("LastUnloaded", 0);
		lastLoaded = input.getIntOr("LastLoaded", 0);
		filters.clearContent();

		for (ItemStackWithSlot item : input.listOrEmpty("Filters", ItemStackWithSlot.CODEC)) {
			if (item.isValidInContainer(FILTERS)) {
				filters.setItem(item.slot(), item.stack());
			}
		}
	}

	@Override
	public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		return saveCustomOnly(registries);
	}
}
