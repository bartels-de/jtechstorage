package dev.bartels.jtechstorage.display;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.mojang.serialization.Codec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import dev.bartels.jtechstorage.network.StorageNetwork;

/** Lager-Bildschirm (v0.34): zeigt Bestand, Füllstand, Verlauf oder die Top 5 des verknüpften Netzwerks. */
public class StorageDisplayBlockEntity extends NetworkPanelBlockEntity {
	public enum Mode {
		ITEM, FILL, GRAPH, TOP;

		public Mode next() {
			return values()[(ordinal() + 1) % values().length];
		}

		public String id() {
			return name().toLowerCase(Locale.ROOT);
		}
	}

	private ItemStack tracked = ItemStack.EMPTY;
	private Mode mode = Mode.ITEM;
	private long count;
	private int trend;
	private int used;
	private int total;
	private List<Long> history = new ArrayList<>();
	private List<ItemStack> topItems = new ArrayList<>();
	private List<Long> topCounts = new ArrayList<>();
	// Aussehen (v0.36)
	private int frame;
	private int background;
	private int accent;
	private boolean glow = true;
	private boolean scanlines = true;

	public StorageDisplayBlockEntity(BlockPos pos, BlockState state) {
		super(DisplayContent.STORAGE_DISPLAY_BLOCK_ENTITY, pos, state);
	}

	public ItemStack getTracked() {
		return tracked;
	}

	public void setTracked(ItemStack stack) {
		tracked = prototype(stack);
		history.clear();
		count = 0;
		trend = 0;
		setChanged();

		if (level != null) {
			level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
		}
	}

	public Mode getMode() {
		return mode;
	}

	public int getFrame() {
		return frame;
	}

	public int getBackground() {
		return background;
	}

	public int getAccent() {
		return accent;
	}

	public boolean isGlow() {
		return glow;
	}

	public boolean hasScanlines() {
		return scanlines;
	}

	/** Aussehen ändern (Schlüssel aus {@link PanelActions}). */
	public void setLook(int key, int value) {
		switch (key) {
			case PanelActions.FRAME -> frame = Math.floorMod(value, 6);
			case PanelActions.BACKGROUND -> background = Math.floorMod(value, 6);
			case PanelActions.ACCENT -> accent = Math.floorMod(value, 8);
			case PanelActions.GLOW -> glow = value != 0;
			case PanelActions.SCANLINES -> scanlines = value != 0;
			default -> {
				return;
			}
		}

		setChanged();

		if (level != null) {
			level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
		}
	}

	public void setMode(Mode newMode) {
		mode = newMode;
		setChanged();

		if (level != null) {
			level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
		}
	}

	public void cycleMode() {
		mode = mode.next();
		setChanged();

		if (level != null) {
			level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
		}
	}

	public long getCount() {
		return count;
	}

	public int getTrend() {
		return trend;
	}

	public int getUsed() {
		return used;
	}

	public int getTotal() {
		return total;
	}

	public List<Long> getHistory() {
		return history;
	}

	public List<ItemStack> getTopItems() {
		return topItems;
	}

	public List<Long> getTopCounts() {
		return topCounts;
	}

	@Override
	protected boolean refresh(StorageNetwork network, boolean sample) {
		long newCount = tracked.isEmpty() ? 0 : network.count(tracked);
		int newUsed = network.usedSlots();
		int newTotal = network.totalSlots();
		boolean changed = newCount != count || newUsed != used || newTotal != total;
		count = newCount;
		used = newUsed;
		total = newTotal;

		if (sample || history.isEmpty()) {
			history.add(count);

			while (history.size() > HISTORY) {
				history.remove(0);
			}

			changed = true;
		}

		int newTrend = trend(history, count) ;
		changed |= newTrend != trend;
		trend = newTrend;

		if (mode == Mode.TOP || sample) {
			List<StorageNetwork.Entry> top = top(network, 5);
			List<ItemStack> items = new ArrayList<>();
			List<Long> counts = new ArrayList<>();

			for (StorageNetwork.Entry e : top) {
				items.add(e.prototype().copyWithCount(1));
				counts.add(e.total());
			}

			if (!counts.equals(topCounts) || items.size() != topItems.size()) {
				changed = true;
			}

			topItems = items;
			topCounts = counts;
		}

		return changed;
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);

		if (!tracked.isEmpty()) {
			output.store("Tracked", ItemStack.CODEC, tracked);
		}

		output.putString("Mode", mode.id());
		output.putBoolean("Online", online);
		output.putLong("Count", count);
		output.putInt("Trend", trend);
		output.putInt("Used", used);
		output.putInt("Total", total);
		output.store("History", Codec.LONG.listOf(), history);
		output.store("TopItems", ItemStack.CODEC.listOf(), topItems);
		output.store("TopCounts", Codec.LONG.listOf(), topCounts);
		output.putInt("Frame", frame);
		output.putInt("Background", background);
		output.putInt("Accent", accent);
		output.putBoolean("Glow", glow);
		output.putBoolean("Scanlines", scanlines);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		tracked = input.read("Tracked", ItemStack.CODEC).orElse(ItemStack.EMPTY);
		String m = input.getStringOr("Mode", "item");
		mode = Mode.ITEM;

		for (Mode value : Mode.values()) {
			if (value.id().equals(m)) {
				mode = value;
			}
		}

		online = input.getBooleanOr("Online", false);
		count = input.getLongOr("Count", 0);
		trend = input.getIntOr("Trend", 0);
		used = input.getIntOr("Used", 0);
		total = input.getIntOr("Total", 0);
		history = new ArrayList<>(input.read("History", Codec.LONG.listOf()).orElse(List.of()));
		topItems = new ArrayList<>(input.read("TopItems", ItemStack.CODEC.listOf()).orElse(List.of()));
		topCounts = new ArrayList<>(input.read("TopCounts", Codec.LONG.listOf()).orElse(List.of()));
		frame = input.getIntOr("Frame", 0);
		background = input.getIntOr("Background", 0);
		accent = input.getIntOr("Accent", 0);
		glow = input.getBooleanOr("Glow", true);
		scanlines = input.getBooleanOr("Scanlines", true);
	}
}
