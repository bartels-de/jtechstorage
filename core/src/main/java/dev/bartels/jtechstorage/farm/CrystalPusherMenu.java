package dev.bartels.jtechstorage.farm;

import org.jspecify.annotations.Nullable;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Filter of the Crystal Pusher: 9 template slots (click with item sets, without item clears, Shift-click copies). */
public class CrystalPusherMenu extends AbstractContainerMenu {
	public static final int WIDTH = 176;
	public static final int FILTER_X = 8;
	public static final int FILTER_Y = 34;
	public static final int INVENTORY_Y = 84;
	public static final int HEIGHT = INVENTORY_Y + 58 + 18 + 6;

	private final @Nullable CrystalPusherBlockEntity pusher;
	private final Container filters;

	/** Client. */
	public CrystalPusherMenu(int containerId, Inventory inventory) {
		this(containerId, inventory, null, new SimpleContainer(CrystalPusherBlockEntity.FILTERS));
	}

	/** Server. */
	public CrystalPusherMenu(int containerId, Inventory inventory, CrystalPusherBlockEntity pusher) {
		this(containerId, inventory, pusher, pusher.getFilters());
	}

	private CrystalPusherMenu(int containerId, Inventory inventory, @Nullable CrystalPusherBlockEntity pusher, Container filters) {
		super(FarmContent.CRYSTAL_PUSHER_MENU, containerId);
		this.pusher = pusher;
		this.filters = filters;

		for (int slot = 0; slot < CrystalPusherBlockEntity.FILTERS; slot++) {
			addSlot(new Slot(filters, slot, FILTER_X + slot * 18, FILTER_Y) {
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

		for (int row = 0; row < 3; row++) {
			for (int col = 0; col < 9; col++) {
				addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, INVENTORY_Y + row * 18));
			}
		}

		for (int col = 0; col < 9; col++) {
			addSlot(new Slot(inventory, col, 8 + col * 18, INVENTORY_Y + 58));
		}
	}

	@Override
	public void clicked(int slotIndex, int button, ContainerInput input, Player player) {
		if (slotIndex >= 0 && slotIndex < CrystalPusherBlockEntity.FILTERS) {
			if (pusher != null && input == ContainerInput.PICKUP) {
				ItemStack carried = getCarried();
				filters.setItem(slotIndex, carried.isEmpty() ? ItemStack.EMPTY : carried.copyWithCount(1));
			}

			return;
		}

		super.clicked(slotIndex, button, input, player);
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		if (pusher != null && index >= CrystalPusherBlockEntity.FILTERS) {
			ItemStack stack = slots.get(index).getItem();

			if (!stack.isEmpty()) {
				for (int slot = 0; slot < CrystalPusherBlockEntity.FILTERS; slot++) {
					if (ItemStack.isSameItem(filters.getItem(slot), stack)) {
						return ItemStack.EMPTY;
					}
				}

				for (int slot = 0; slot < CrystalPusherBlockEntity.FILTERS; slot++) {
					if (filters.getItem(slot).isEmpty()) {
						filters.setItem(slot, stack.copyWithCount(1));
						break;
					}
				}
			}
		}

		return ItemStack.EMPTY;
	}

	@Override
	public boolean stillValid(Player player) {
		return pusher == null || !pusher.isRemoved() && pusher.getBlockPos().distToCenterSqr(player.position()) <= 8.0 * 8.0;
	}
}
