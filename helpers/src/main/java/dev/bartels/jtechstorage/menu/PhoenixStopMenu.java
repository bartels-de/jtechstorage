package dev.bartels.jtechstorage.menu;

import dev.bartels.jtechstorage.helpers.HelpersContent;
import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import dev.bartels.jtechstorage.block.PhoenixStopBlockEntity;

/**
 * Menu of a phoenix stop: name, rest duration, delivery order (mode + 9 filters with amount per visit).
 * Filters are templates: clicking with an item sets the filter, without an item clears it, right-click resets the
 * amount to “all”.
 */
public class PhoenixStopMenu extends AbstractContainerMenu {
	public static final int WIDTH = 240;
	public static final int FILTER_X = (WIDTH - 9 * 18) / 2 + 1;
	public static final int FILTER_Y = 107;
	public static final int INVENTORY_X = (WIDTH - 9 * 18) / 2 + 1;
	public static final int INVENTORY_Y = 161;
	public static final int HEIGHT = INVENTORY_Y + 58 + 18 + 7;

	/** Buttons: 0–2 rest duration, 10–13 mode, 100 + filter × 10 + step = change amount. */
	public static final int BUTTON_REST = 0;
	public static final int BUTTON_MODE = 10;
	public static final int BUTTON_AMOUNT = 100;
	/** Amount steps: +1, −1, +16, −16, +64, −64, reset. */
	private static final int[] STEPS = {1, -1, 16, -16, 64, -64, 0};

	private static final int DATA_REST = PhoenixStopBlockEntity.FILTERS;
	private static final int DATA_MODE = DATA_REST + 1;
	private static final int DATA_UNLOADED = DATA_REST + 2;
	private static final int DATA_LOADED = DATA_REST + 3;
	private static final int DATA_MINUTES = DATA_REST + 4;
	private static final int DATA_COUNT = DATA_REST + 5;

	public record OpenData(BlockPos pos, String name) {
		public static final StreamCodec<RegistryFriendlyByteBuf, OpenData> STREAM_CODEC = StreamCodec.composite(
				BlockPos.STREAM_CODEC, OpenData::pos,
				ByteBufCodecs.stringUtf8(64), OpenData::name,
				OpenData::new);
	}

	private final @Nullable PhoenixStopBlockEntity stop;
	private final ContainerData data;
	private final BlockPos pos;
	private final String initialName;

	/** Client. */
	public PhoenixStopMenu(int containerId, Inventory inventory, OpenData openData) {
		this(containerId, inventory, null, new SimpleContainer(PhoenixStopBlockEntity.FILTERS), new SimpleContainerData(DATA_COUNT),
				openData.pos(), openData.name());
	}

	/** Server. */
	public PhoenixStopMenu(int containerId, Inventory inventory, PhoenixStopBlockEntity stop) {
		this(containerId, inventory, stop, stop.getFilters(), new ContainerData() {
			@Override
			public int get(int index) {
				if (index < PhoenixStopBlockEntity.FILTERS) {
					return stop.getAmount(index);
				}

				return switch (index - PhoenixStopBlockEntity.FILTERS) {
					case 0 -> stop.getRest().ordinal();
					case 1 -> stop.getMode().ordinal();
					case 2 -> Math.min(30000, stop.getLastUnloaded());
					case 3 -> Math.min(30000, stop.getLastLoaded());
					case 4 -> stop.getLastVisit() < 0 || stop.getLevel() == null ? -1
							: (int) Math.min(30000, (stop.getLevel().getGameTime() - stop.getLastVisit()) / 1200);
					default -> 0;
				};
			}

			@Override
			public void set(int index, int value) {
			}

			@Override
			public int getCount() {
				return DATA_COUNT;
			}
		}, stop.getBlockPos(), stop.getStopName());
	}

	private PhoenixStopMenu(int containerId, Inventory inventory, @Nullable PhoenixStopBlockEntity stop, Container filters,
			ContainerData data, BlockPos pos, String initialName) {
		super(HelpersContent.PHOENIX_STOP_MENU, containerId);
		this.stop = stop;
		this.data = data;
		this.pos = pos;
		this.initialName = initialName;
		addDataSlots(data);

		for (int slot = 0; slot < PhoenixStopBlockEntity.FILTERS; slot++) {
			addSlot(new FilterSlot(filters, slot, FILTER_X + slot * 18, FILTER_Y));
		}

		for (int row = 0; row < 3; row++) {
			for (int col = 0; col < 9; col++) {
				addSlot(new Slot(inventory, col + row * 9 + 9, INVENTORY_X + col * 18, INVENTORY_Y + row * 18));
			}
		}

		for (int col = 0; col < 9; col++) {
			addSlot(new Slot(inventory, col, INVENTORY_X + col * 18, INVENTORY_Y + 58));
		}
	}

	public BlockPos pos() {
		return pos;
	}

	public String initialName() {
		return initialName;
	}

	public int amount(int slot) {
		return data.get(slot);
	}

	public PhoenixStopBlockEntity.Rest rest() {
		return PhoenixStopBlockEntity.Rest.byId(data.get(DATA_REST));
	}

	public PhoenixStopBlockEntity.Mode mode() {
		return PhoenixStopBlockEntity.Mode.byId(data.get(DATA_MODE));
	}

	public int lastUnloaded() {
		return data.get(DATA_UNLOADED);
	}

	public int lastLoaded() {
		return data.get(DATA_LOADED);
	}

	/** Minutes since the last visit, -1 = never. */
	public int minutesSinceVisit() {
		return data.get(DATA_MINUTES);
	}

	/** Template field: never accepts real items. */
	private static class FilterSlot extends Slot {
		FilterSlot(Container container, int slot, int x, int y) {
			super(container, slot, x, y);
		}

		@Override
		public boolean mayPlace(ItemStack stack) {
			return false;
		}

		@Override
		public boolean mayPickup(Player player) {
			return false;
		}
	}

	@Override
	public void clicked(int slotIndex, int button, ContainerInput input, Player player) {
		if (slotIndex >= 0 && slotIndex < PhoenixStopBlockEntity.FILTERS) {
			if (stop != null && input == ContainerInput.PICKUP) {
				if (button == 1) {
					stop.setAmount(slotIndex, 0);
				} else {
					ItemStack carried = getCarried();
					stop.getFilters().setItem(slotIndex, carried.isEmpty() ? ItemStack.EMPTY : carried.copyWithCount(1));
				}
			}

			return;
		}

		super.clicked(slotIndex, button, input, player);
	}

	@Override
	public boolean clickMenuButton(Player player, int id) {
		if (stop == null) {
			return false;
		}

		if (id >= BUTTON_REST && id < BUTTON_REST + PhoenixStopBlockEntity.Rest.values().length) {
			stop.setRest(PhoenixStopBlockEntity.Rest.byId(id - BUTTON_REST));
			return true;
		}

		if (id >= BUTTON_MODE && id < BUTTON_MODE + PhoenixStopBlockEntity.Mode.values().length) {
			stop.setMode(PhoenixStopBlockEntity.Mode.byId(id - BUTTON_MODE));
			return true;
		}

		if (id >= BUTTON_AMOUNT && id < BUTTON_AMOUNT + PhoenixStopBlockEntity.FILTERS * 10) {
			int slot = (id - BUTTON_AMOUNT) / 10;
			int step = (id - BUTTON_AMOUNT) % 10;

			if (step >= STEPS.length) {
				return false;
			}

			int value = STEPS[step] == 0 ? 0 : stop.getAmount(slot) + STEPS[step];
			stop.setAmount(slot, value);
			return true;
		}

		return false;
	}

	/** Button ID for an amount change (for the step index see {@link #STEPS}). */
	public static int amountButton(int slot, int stepIndex) {
		return BUTTON_AMOUNT + slot * 10 + stepIndex;
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		// Shift-click in inventory: take the item as a filter into the first free field
		if (stop != null && index >= PhoenixStopBlockEntity.FILTERS) {
			ItemStack stack = slots.get(index).getItem();

			if (!stack.isEmpty()) {
				for (int slot = 0; slot < PhoenixStopBlockEntity.FILTERS; slot++) {
					if (ItemStack.isSameItem(stop.getFilters().getItem(slot), stack)) {
						return ItemStack.EMPTY;
					}
				}

				for (int slot = 0; slot < PhoenixStopBlockEntity.FILTERS; slot++) {
					if (stop.getFilters().getItem(slot).isEmpty()) {
						stop.getFilters().setItem(slot, stack.copyWithCount(1));
						break;
					}
				}
			}
		}

		return ItemStack.EMPTY;
	}

	@Override
	public boolean stillValid(Player player) {
		return stop == null || !stop.isRemoved() && stop.getBlockPos().distToCenterSqr(player.position()) <= 8.0 * 8.0;
	}
}
