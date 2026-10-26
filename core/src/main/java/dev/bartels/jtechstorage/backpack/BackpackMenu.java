package dev.bartels.jtechstorage.backpack;

import net.minecraft.core.NonNullList;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Backpack menu with the layout of a vanilla chest menu (the client shows the normal chest screen).
 * The backpack in hand is locked, backpacks/shulker boxes may not go in; every change is
 * written to the item immediately.
 */
public class BackpackMenu extends AbstractContainerMenu {
	private final ItemStack backpack;
	private final InteractionHand hand;
	private final SimpleContainer container;
	private final int rows;
	private final int lockedMenuSlot;
	private final int lockedHotbar;

	public BackpackMenu(int containerId, Inventory inventory, ItemStack backpack, InteractionHand hand, BackpackItem.Tier tier) {
		super(menuType(tier.rows), containerId);
		this.backpack = backpack;
		this.hand = hand;
		this.rows = tier.rows;
		this.container = new SimpleContainer(tier.slots()) {
			@Override
			public void setChanged() {
				super.setChanged();
				save();
			}
		};

		NonNullList<ItemStack> items = BackpackItem.readContents(backpack, tier.slots());

		for (int i = 0; i < items.size(); i++) {
			container.setItem(i, items.get(i));
		}

		for (int row = 0; row < rows; row++) {
			for (int col = 0; col < 9; col++) {
				addSlot(new Slot(container, col + row * 9, 8 + col * 18, 18 + row * 18) {
					@Override
					public boolean mayPlace(ItemStack stack) {
						return !BackpackItem.isForbidden(stack);
					}
				});
			}
		}

		int offset = (rows - 4) * 18;
		lockedHotbar = hand == InteractionHand.MAIN_HAND ? inventory.getSelectedSlot() : -1;
		int locked = -1;

		for (int row = 0; row < 3; row++) {
			for (int col = 0; col < 9; col++) {
				addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 103 + row * 18 + offset));
			}
		}

		for (int col = 0; col < 9; col++) {
			if (col == lockedHotbar) {
				locked = slots.size();
				addSlot(new Slot(inventory, col, 8 + col * 18, 161 + offset) {
					@Override
					public boolean mayPickup(Player player) {
						return false;
					}

					@Override
					public boolean mayPlace(ItemStack stack) {
						return false;
					}
				});
			} else {
				addSlot(new Slot(inventory, col, 8 + col * 18, 161 + offset));
			}
		}

		lockedMenuSlot = locked;
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

	public boolean isFor(ItemStack stack) {
		return stack == backpack;
	}

	private void save() {
		BackpackItem.writeContents(backpack, container.getItems());
	}

	@Override
	public void clicked(int slotId, int button, ContainerInput input, Player player) {
		if (slotId == lockedMenuSlot && lockedMenuSlot >= 0) {
			return;
		}

		if (input == ContainerInput.SWAP && (button == lockedHotbar || button == 40 && hand == InteractionHand.OFF_HAND)) {
			return;   // number key/F on the opened backpack
		}

		super.clicked(slotId, button, input, player);
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		ItemStack result = ItemStack.EMPTY;
		Slot slot = slots.get(index);

		if (slot.hasItem() && index != lockedMenuSlot) {
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
		return player.getItemInHand(hand) == backpack && !backpack.isEmpty();
	}

	@Override
	public void removed(Player player) {
		super.removed(player);
		save();
		player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BUNDLE_INSERT,
				SoundSource.PLAYERS, 0.6f, 0.8f);
	}
}
