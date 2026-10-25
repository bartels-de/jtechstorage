package dev.bartels.jtechstorage.display;

import java.util.ArrayList;
import java.util.List;

import com.mojang.serialization.Codec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import dev.bartels.jtechstorage.network.StorageNetwork;

/** Lager-Banner (v0.34): Laufschrift mit bis zu 8 Gegenständen, ihrem Bestand und Trend. */
public class StorageBannerBlockEntity extends NetworkPanelBlockEntity {
	public static final int MAX = 8;
	private List<ItemStack> items = new ArrayList<>();
	private List<Long> counts = new ArrayList<>();
	private List<Integer> trends = new ArrayList<>();
	/** Verlauf je Eintrag (alle 10 Sekunden), nur auf dem Server für den Trend. */
	private final List<List<Long>> histories = new ArrayList<>();
	// Aussehen (v0.36)
	private int cloth = net.minecraft.world.item.DyeColor.PURPLE.getId();
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

	/** Gegenstand hinzufügen oder – wenn schon vorhanden – entfernen. @return true = hinzugefügt */
	public boolean toggle(ItemStack stack) {
		for (int i = 0; i < items.size(); i++) {
			if (ItemStack.isSameItemSameComponents(items.get(i), stack)) {
				// an derselben Stelle aus allen Listen nehmen, sonst rutschen die Zahlen zum falschen Gegenstand
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

	/** Aussehen ändern (Schlüssel aus {@link PanelActions}). */
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

	/** Eintrag an Stelle {@code index} entfernen. */
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
		// jede Liste einzeln: nach dem Laden kommen Bestände und Trends aus dem Speicher, die Verläufe nicht
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
		cloth = input.getIntOr("Cloth", net.minecraft.world.item.DyeColor.PURPLE.getId());
		trim = input.getIntOr("Trim", 0);
		pattern = input.getIntOr("Pattern", 0);
		textColor = input.getIntOr("TextColor", 0);
		wind = input.getIntOr("Wind", 1);
		resize();
	}
}
