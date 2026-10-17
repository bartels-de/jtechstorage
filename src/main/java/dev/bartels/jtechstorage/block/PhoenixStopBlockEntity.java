package dev.bartels.jtechstorage.block;

import java.util.ArrayList;
import java.util.List;

import com.mojang.serialization.Codec;

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

import dev.bartels.jtechstorage.ModContent;
import dev.bartels.jtechstorage.network.StorageNetwork;

/**
 * Haltestelle des Amethyst-Phönix: Name, Rastdauer und Lieferauftrag. Der Lieferauftrag tauscht beim Landen Items
 * zwischen den Kisten des Phönix und Kisten/Fässern direkt neben der Haltestelle aus.
 */
public class PhoenixStopBlockEntity extends BlockEntity {
	public static final int MAX_NAME = 24;
	public static final int FILTERS = 9;
	public static final int MAX_AMOUNT = 9999;

	/** Rastdauer an dieser Haltestelle. */
	public enum Rest {
		SHORT(600),
		FIVE_MINUTES(6000),
		NIGHT(-1);

		/** Ticks, -1 = bis zum nächsten Sonnenaufgang nach einer ganzen Nacht. */
		public final int ticks;

		Rest(int ticks) {
			this.ticks = ticks;
		}

		public String key() {
			return "jtechstorage.phoenix_stop.rest." + name().toLowerCase(java.util.Locale.ROOT);
		}

		public static Rest byId(int id) {
			Rest[] values = values();
			return values[Math.floorMod(id, values.length)];
		}
	}

	/**
	 * Lieferauftrag. Abladen: Items aus dem Phönix in die Kisten (mit Filtern nur diese). Einladen: Items aus den Kisten
	 * in den Phönix (mit Filtern nur diese). Beides: alles abladen, was nicht auf der Liste steht, dann die Listen-Items
	 * einladen.
	 */
	public enum Mode {
		NONE,
		UNLOAD,
		LOAD,
		BOTH;

		public String key() {
			return "jtechstorage.phoenix_stop.mode." + name().toLowerCase(java.util.Locale.ROOT);
		}

		public static Mode byId(int id) {
			Mode[] values = values();
			return values[Math.floorMod(id, values.length)];
		}
	}

	private String stopName = "";
	private Rest rest = Rest.NIGHT;
	private Mode mode = Mode.NONE;
	/** Filter (Vorlagen, je 1 Stück) – werden nie wirklich eingelagert. */
	private final SimpleContainer filters = new SimpleContainer(FILTERS) {
		@Override
		public void setChanged() {
			super.setChanged();
			PhoenixStopBlockEntity.this.setChanged();
		}
	};
	/** Menge je Filter und Besuch, 0 = alles. */
	private final int[] amounts = new int[FILTERS];
	private long lastVisit = -1;
	private int lastUnloaded;
	private int lastLoaded;

	public PhoenixStopBlockEntity(BlockPos pos, BlockState state) {
		super(ModContent.PHOENIX_STOP_BLOCK_ENTITY, pos, state);
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
	// Lieferauftrag
	// ------------------------------------------------------------------

	/** Ergebnis eines Besuchs: abgeladene und eingeladene Items. */
	public record Exchange(int unloaded, int loaded) {
		public static final Exchange NONE = new Exchange(0, 0);
	}

	/** Kisten, Fässer usw. direkt neben der Haltestelle (oben landet der Phönix). */
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

	/** Index des passenden Filters oder -1. Vergleich nur nach Item (ohne Verzauberungen, Namen usw.). */
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
	 * Führt den Lieferauftrag aus. {@code cargo} sind die Kisten des Phönix, davon die ersten {@code slots} Plätze nutzbar.
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

	/** Legt {@code stack} in die ersten {@code slots} Plätze des Phönix (verändert {@code stack}). */
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
	// Speichern & Sync
	// ------------------------------------------------------------------

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putString("StopName", stopName);
		output.putInt("Rest", rest.ordinal());
		output.putInt("Mode", mode.ordinal());
		output.store("Amounts", Codec.INT.listOf(), java.util.Arrays.stream(amounts).boxed().toList());
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
