package dev.bartels.jtechstorage.logistics;

import org.jspecify.annotations.Nullable;

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

/** Filter der Sortier-Weiche: 3 Reihen à 9 Vorlage-Felder (links, geradeaus, rechts) und der Rest-Schalter. */
public class SorterMenu extends AbstractContainerMenu {
	public static final int WIDTH = 176;
	public static final int FILTER_X = 8;
	public static final int FILTER_Y = 32;
	public static final int ROW_GAP = 28;
	public static final int INVENTORY_Y = 148;
	public static final int HEIGHT = INVENTORY_Y + 58 + 18 + 6;
	private static final int FILTERS = SorterBlockEntity.ROW * 3;

	private final @Nullable SorterBlockEntity sorter;
	private final Container filters;
	private final ContainerData data;

	/** Client. */
	public SorterMenu(int containerId, Inventory inventory) {
		this(containerId, inventory, null, new SimpleContainer(FILTERS), new SimpleContainerData(1));
	}

	/** Server. */
	public SorterMenu(int containerId, Inventory inventory, SorterBlockEntity sorter) {
		this(containerId, inventory, sorter, sorter.getFilters(), sorter.getData());
	}

	private SorterMenu(int containerId, Inventory inventory, @Nullable SorterBlockEntity sorter, Container filters, ContainerData data) {
		super(LogisticsContent.SORTER_MENU, containerId);
		this.sorter = sorter;
		this.filters = filters;
		this.data = data;
		addDataSlots(data);

		for (int row = 0; row < 3; row++) {
			for (int col = 0; col < SorterBlockEntity.ROW; col++) {
				addSlot(new Slot(filters, row * SorterBlockEntity.ROW + col, FILTER_X + col * 18, FILTER_Y + row * ROW_GAP) {
					@Override
					public boolean mayPlace(ItemStack stack) {
						return false;
					}

					@Override
					public boolean mayPickup(Player player) {
						return false;
					}
				});
			}
		}

		for (int row = 0; row < 3; row++) {
			for (int col = 0; col < 9; col++) {
				addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, INVENTORY_Y + row * 18));
			}
		}

		for (int col = 0; col < 9; col++) {
			addSlot(new Slot(inventory, col, 8 + col * 18, INVENTORY_Y + 58));
		}
	}

	public boolean holdsRest() {
		return data.get(0) == 1;
	}

	@Override
	public boolean clickMenuButton(Player player, int id) {
		if (id == 0 && sorter != null) {
			sorter.toggleRest();
			return true;
		}

		return false;
	}

	@Override
	public void clicked(int slotIndex, int button, ContainerInput input, Player player) {
		if (slotIndex >= 0 && slotIndex < FILTERS) {
			if (sorter != null && input == ContainerInput.PICKUP) {
				ItemStack carried = getCarried();
				filters.setItem(slotIndex, carried.isEmpty() ? ItemStack.EMPTY : carried.copyWithCount(1));
			}

			return;
		}

		super.clicked(slotIndex, button, input, player);
	}

	/** Shift-Klick aus dem Inventar: in die erste freie Filterzelle (zuerst geradeaus). */
	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		if (sorter != null && index >= FILTERS) {
			ItemStack stack = slots.get(index).getItem();

			if (!stack.isEmpty()) {
				for (int slot = 0; slot < FILTERS; slot++) {
					if (ItemStack.isSameItem(filters.getItem(slot), stack)) {
						return ItemStack.EMPTY;
					}
				}

				for (int slot = SorterBlockEntity.ROW; slot < FILTERS + SorterBlockEntity.ROW; slot++) {
					int s = slot % FILTERS;

					if (filters.getItem(s).isEmpty()) {
						filters.setItem(s, stack.copyWithCount(1));
						break;
					}
				}
			}
		}

		return ItemStack.EMPTY;
	}

	@Override
	public boolean stillValid(Player player) {
		return sorter == null || !sorter.isRemoved() && sorter.getBlockPos().distToCenterSqr(player.position()) <= 8.0 * 8.0;
	}
}
