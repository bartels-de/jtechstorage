package dev.bartels.jtechstorage.chest;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Window of the amethyst chests (v0.28.0). Slots hold several stacks; at most one normal stack goes onto the
 * cursor or into the hotbar.
 */
public class AmethystChestMenu extends AbstractContainerMenu {
	public static final int TOP = 18;

	private final AmethystChestTier tier;
	private final Container container;
	private final int size;

	/** Client (type per tier). */
	public AmethystChestMenu(AmethystChestTier tier, int containerId, Inventory inventory) {
		this(tier, containerId, inventory, new BigContainer(tier));
	}

	/** Server. */
	public AmethystChestMenu(int containerId, Inventory inventory, AmethystChestBlockEntity chest) {
		this(chest.tier(), containerId, inventory, chest);
	}

	private AmethystChestMenu(AmethystChestTier tier, int containerId, Inventory inventory, Container container) {
		super(ChestContent.menuType(tier), containerId);
		this.tier = tier;
		this.container = container;
		this.size = tier.size();
		checkContainerSize(container, size);
		container.startOpen(inventory.player);

		for (int row = 0; row < tier.rows; row++) {
			for (int col = 0; col < tier.columns; col++) {
				addSlot(new BigSlot(container, col + row * tier.columns, 8 + col * 18, TOP + row * 18));
			}
		}

		int left = inventoryLeft(tier);
		int top = inventoryTop(tier);

		for (int row = 0; row < 3; row++) {
			for (int col = 0; col < 9; col++) {
				addSlot(new Slot(inventory, col + row * 9 + 9, left + col * 18, top + row * 18));
			}
		}

		for (int col = 0; col < 9; col++) {
			addSlot(new Slot(inventory, col, left + col * 18, top + 58));
		}
	}

	public static int width(AmethystChestTier tier) {
		return 16 + tier.columns * 18;
	}

	public static int height(AmethystChestTier tier) {
		return inventoryTop(tier) + 58 + 18 + 7;
	}

	public static int inventoryLeft(AmethystChestTier tier) {
		return (width(tier) - 176) / 2 + 8;
	}

	public static int inventoryTop(AmethystChestTier tier) {
		return TOP + tier.rows * 18 + 14;
	}

	public AmethystChestTier tier() {
		return tier;
	}

	public Container container() {
		return container;
	}

	@Override
	public void clicked(int slotId, int button, ContainerInput input, Player player) {
		// hotbar key on an overfull slot: only put one normal stack into the (empty) hotbar
		if (input == ContainerInput.SWAP && slotId >= 0 && slotId < size) {
			Slot slot = slots.get(slotId);
			ItemStack stack = slot.getItem();

			if (stack.getCount() > stack.getMaxStackSize()) {
				Inventory inventory = player.getInventory();

				if (button >= 0 && button < 9 && inventory.getItem(button).isEmpty()) {
					inventory.setItem(button, slot.remove(stack.getMaxStackSize()));
					slot.setChanged();
				}

				return;
			}
		}

		// clicking with another item on an overfull slot would swap the whole slot onto the cursor
		if (input == ContainerInput.PICKUP && slotId >= 0 && slotId < size) {
			ItemStack stack = slots.get(slotId).getItem();
			ItemStack carried = getCarried();

			if (!carried.isEmpty() && stack.getCount() > stack.getMaxStackSize() && !ItemStack.isSameItemSameComponents(stack, carried)) {
				return;
			}
		}

		super.clicked(slotId, button, input, player);
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		Slot slot = slots.get(index);

		if (!slot.hasItem()) {
			return ItemStack.EMPTY;
		}

		ItemStack stack = slot.getItem();
		ItemStack copy = stack.copy();

		if (index < size) {
			if (!moveItemStackTo(stack, size, slots.size(), true)) {
				return ItemStack.EMPTY;
			}
		} else if (!moveItemStackTo(stack, 0, size, false)) {
			return ItemStack.EMPTY;
		}

		if (stack.isEmpty()) {
			slot.setByPlayer(ItemStack.EMPTY);
		} else {
			slot.setChanged();
		}

		return copy;
	}

	@Override
	public boolean stillValid(Player player) {
		return container.stillValid(player);
	}

	@Override
	public void removed(Player player) {
		super.removed(player);
		container.stopOpen(player);
	}

	/** Slot with large stacks; taking out yields at most one normal stack. */
	private static class BigSlot extends Slot {
		BigSlot(Container container, int slot, int x, int y) {
			super(container, slot, x, y);
		}

		@Override
		public int getMaxStackSize() {
			return container.getMaxStackSize();
		}

		@Override
		public int getMaxStackSize(ItemStack stack) {
			return container.getMaxStackSize(stack);
		}

		@Override
		public boolean mayPlace(ItemStack stack) {
			return container.canPlaceItem(getContainerSlot(), stack);
		}

		@Override
		public ItemStack remove(int amount) {
			return super.remove(Math.min(amount, Math.max(1, getItem().getMaxStackSize())));
		}
	}

	/** Client counterpart with the same stack limits. */
	private static class BigContainer extends SimpleContainer {
		private final AmethystChestTier tier;

		BigContainer(AmethystChestTier tier) {
			super(tier.size());
			this.tier = tier;
		}

		@Override
		public int getMaxStackSize() {
			return 64 * tier.stackFactor;
		}

		@Override
		public int getMaxStackSize(ItemStack stack) {
			return tier.maxStack(stack);
		}

		@Override
		public boolean canPlaceItem(int slot, ItemStack stack) {
			return stack.getItem().canFitInsideContainerItems();
		}
	}

	static MenuType<AmethystChestMenu> type(AmethystChestTier tier) {
		return new MenuType<>((id, inventory) -> new AmethystChestMenu(tier, id, inventory), FeatureFlags.VANILLA_SET);
	}
}
