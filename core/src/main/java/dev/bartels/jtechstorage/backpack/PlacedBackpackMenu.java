package dev.bartels.jtechstorage.backpack;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Menu of a placed backpack (chest layout, backpacks/shulker boxes may not go in). */
public class PlacedBackpackMenu extends AbstractContainerMenu {
	private final PlacedBackpackBlockEntity backpack;
	private final int rows;

	public PlacedBackpackMenu(int containerId, Inventory inventory, PlacedBackpackBlockEntity backpack, int rows) {
		super(menuType(rows), containerId);
		this.backpack = backpack;
		this.rows = rows;

		for (int row = 0; row < rows; row++) {
			for (int col = 0; col < 9; col++) {
				addSlot(new Slot(backpack, col + row * 9, 8 + col * 18, 18 + row * 18) {
					@Override
					public boolean mayPlace(ItemStack stack) {
						return !BackpackItem.isForbidden(stack);
					}
				});
			}
		}

		int offset = (rows - 4) * 18;

		for (int row = 0; row < 3; row++) {
			for (int col = 0; col < 9; col++) {
				addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 103 + row * 18 + offset));
			}
		}

		for (int col = 0; col < 9; col++) {
			addSlot(new Slot(inventory, col, 8 + col * 18, 161 + offset));
		}
	}

	private static MenuType<?> menuType(int rows) {
		return switch (rows) {
			case 1 -> MenuType.GENERIC_9x1;
			case 2 -> MenuType.GENERIC_9x2;
			case 3 -> MenuType.GENERIC_9x3;
			case 4 -> MenuType.GENERIC_9x4;
			case 5 -> MenuType.GENERIC_9x5;
			default -> MenuType.GENERIC_9x6;
		};
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		ItemStack result = ItemStack.EMPTY;
		Slot slot = slots.get(index);

		if (slot.hasItem()) {
			ItemStack stack = slot.getItem();
			result = stack.copy();
			int containerSlots = rows * 9;

			if (index < containerSlots) {
				if (!moveItemStackTo(stack, containerSlots, slots.size(), true)) {
					return ItemStack.EMPTY;
				}
			} else if (BackpackItem.isForbidden(stack) || !moveItemStackTo(stack, 0, containerSlots, false)) {
				return ItemStack.EMPTY;
			}

			if (stack.isEmpty()) {
				slot.setByPlayer(ItemStack.EMPTY);
			} else {
				slot.setChanged();
			}
		}

		return result;
	}

	@Override
	public boolean stillValid(Player player) {
		return backpack.stillValid(player) && !backpack.isRemoved();
	}

	@Override
	public void removed(Player player) {
		super.removed(player);
		player.level().playSound(null, backpack.getBlockPos(), SoundEvents.BUNDLE_INSERT, SoundSource.BLOCKS, 0.6f, 0.8f);
	}
}
