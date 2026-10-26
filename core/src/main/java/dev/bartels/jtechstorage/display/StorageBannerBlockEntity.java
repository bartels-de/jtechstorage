package dev.bartels.jtechstorage.display;

import java.util.ArrayList;
import java.util.List;

import com.mojang.serialization.Codec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import dev.bartels.jtechstorage.network.StorageNetwork;

/** Storage banner (v0.34): ticker with up to 8 items, their stock and trend. */
public class StorageBannerBlockEntity extends NetworkPanelBlockEntity {
	public static final int MAX = 8;
	private List<ItemStack> items = new ArrayList<>();
	private List<Long> counts = new ArrayList<>();
	private List<Integer> trends = new ArrayList<>();
	/** History per entry (every 10 seconds), only on the server for the trend. */
	private final List<List<Long>> histories = new ArrayList<>();
	// Appearance (v0.36)
	private int cloth = DyeColor.PURPLE.getId();
	private int trim;
	private int pattern;
	private int textColor;
	private int wind = 1;

	public StorageBannerBlockEntity(BlockPos pos, BlockState state) {
		super(DisplayContent.STORAGE_BANNER_BLOCK_ENTITY, pos, state);
	}

	public List<ItemStack> getItems() {
		return items;
	}

	public List<Long> getCounts() {
		return counts;
	}

	public List<Integer> getTrends() {
		return trends;
	}

	/** Add an item or - if already present - remove it. @return true = added */
	public boolean toggle(ItemStack stack) {
		for (int i = 0; i < items.size(); i++) {
			if (ItemStack.isSameItemSameComponents(items.get(i), stack)) {
				// remove at the same position from all lists, otherwise the numbers shift to the wrong item
				resize();
				items.remove(i);
				counts.remove(i);
				trends.remove(i);
				histories.remove(i);
				sync();
				return false;
			}
		}

		if (items.size() >= MAX) {
			return false;
		}

		items.add(prototype(stack));
		resize();
		sync();
		return true;
	}

	public int getCloth() {
		return cloth;
	}

	public int getTrim() {
		return trim;
	}

	public int getPattern() {
		return pattern;
	}

	public int getTextColor() {
		return textColor;
	}

	public int getWind() {
		return wind;
	}

	/** Change appearance (key from {@link PanelActions}). */
	public void setLook(int key, int value) {
		switch (key) {
			case PanelActions.CLOTH -> cloth = Math.floorMod(value, 16);
			case PanelActions.TRIM -> trim = Math.floorMod(value, 5);
			case PanelActions.PATTERN -> pattern = Math.floorMod(value, 5);
			case PanelActions.TEXT -> textColor = Math.floorMod(value, 4);
			case PanelActions.WIND -> wind = Math.floorMod(value, 3);
			default -> {
				return;
			}
		}

		sync();
	}

	/** Remove the entry at position {@code index}. */
	public void removeAt(int index) {
		resize();

		if (index < 0 || index >= items.size()) {
			return;
		}

		items.remove(index);
		counts.remove(index);
		trends.remove(index);
		histories.remove(index);
		sync();
	}

	public void clear() {
		items.clear();
		counts.clear();
		trends.clear();
		histories.clear();
		sync();
	}

	private void resize() {
		// each list separately: after loading, stocks and trends come from storage, the histories do not
		int n = items.size();

		while (counts.size() < n) {
			counts.add(0L);
		}

		while (trends.size() < n) {
			trends.add(0);
		}

		while (histories.size() < n) {
			histories.add(new ArrayList<>());
		}

		while (counts.size() > n) {
			counts.remove(counts.size() - 1);
		}

		while (trends.size() > n) {
			trends.remove(trends.size() - 1);
		}

		while (histories.size() > n) {
			histories.remove(histories.size() - 1);
		}
	}

	private void sync() {
		setChanged();

		if (level != null) {
			level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
		}
	}

	@Override
	protected boolean refresh(StorageNetwork network, boolean sample) {
		resize();
		boolean changed = false;

		for (int i = 0; i < items.size(); i++) {
			long c = network.count(items.get(i));
			List<Long> h = histories.get(i);

			if (sample || h.isEmpty()) {
				h.add(c);

				while (h.size() > HISTORY) {
					h.remove(0);
				}
			}

			int t = trend(h, c);
			changed |= counts.get(i) != c || trends.get(i) != t;
			counts.set(i, c);
			trends.set(i, t);
		}

		return changed;
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.store("Items", ItemStack.CODEC.listOf(), items);
		output.store("Counts", Codec.LONG.listOf(), counts);
		output.store("Trends", Codec.INT.listOf(), trends);
		output.putBoolean("Online", online);
		output.putInt("Cloth", cloth);
		output.putInt("Trim", trim);
		output.putInt("Pattern", pattern);
		output.putInt("TextColor", textColor);
		output.putInt("Wind", wind);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		items = new ArrayList<>(input.read("Items", ItemStack.CODEC.listOf()).orElse(List.of()));
		counts = new ArrayList<>(input.read("Counts", Codec.LONG.listOf()).orElse(List.of()));
		trends = new ArrayList<>(input.read("Trends", Codec.INT.listOf()).orElse(List.of()));
		online = input.getBooleanOr("Online", false);
		cloth = input.getIntOr("Cloth", DyeColor.PURPLE.getId());
		trim = input.getIntOr("Trim", 0);
		pattern = input.getIntOr("Pattern", 0);
		textColor = input.getIntOr("TextColor", 0);
		wind = input.getIntOr("Wind", 1);
		resize();
	}
}
