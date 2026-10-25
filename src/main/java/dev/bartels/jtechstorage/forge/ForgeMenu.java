package dev.bartels.jtechstorage.forge;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Menü der Kristall-Schmiede: links oben die Ausrüstung, darunter das Material; rechts je ein Knopf pro Verbesserung.
 * Ein Klick verbraucht das Material und hebt die Stufe um eins.
 */
public class ForgeMenu extends AbstractContainerMenu {
	public static final int WIDTH = 176;
	public static final int ITEM_X = 17;
	public static final int ITEM_Y = 26;
	public static final int MATERIAL_X = 17;
	public static final int MATERIAL_Y = 62;
	public static final int BUTTON_X = 44;
	public static final int BUTTON_Y = 18;
	public static final int BUTTON_W = 124;
	public static final int BUTTON_H = 16;
	public static final int BUTTON_GAP = 18;
	public static final int INVENTORY_Y = 100;
	public static final int HEIGHT = INVENTORY_Y + 58 + 18 + 6;

	private final Container slots = new SimpleContainer(2) {
		@Override
		public void setChanged() {
			super.setChanged();
			ForgeMenu.this.slotsChanged(this);
		}
	};
	private final ContainerLevelAccess access;

	/** Client. */
	public ForgeMenu(int containerId, Inventory inventory) {
		this(containerId, inventory, ContainerLevelAccess.NULL);
	}

	public ForgeMenu(int containerId, Inventory inventory, ContainerLevelAccess access) {
		super(ForgeContent.MENU, containerId);
		this.access = access;
		addSlot(new Slot(slots, 0, ITEM_X, ITEM_Y) {
			@Override
			public boolean mayPlace(ItemStack stack) {
				return stack.is(ForgeContent.FORGEABLE);
			}

			@Override
			public int getMaxStackSize() {
				return 1;
			}
		});
		addSlot(new Slot(slots, 1, MATERIAL_X, MATERIAL_Y));

		for (int row = 0; row < 3; row++) {
			for (int col = 0; col < 9; col++) {
				addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, INVENTORY_Y + row * 18));
			}
		}

		for (int col = 0; col < 9; col++) {
			addSlot(new Slot(inventory, col, 8 + col * 18, INVENTORY_Y + 58));
		}
	}

	public ItemStack equipment() {
		return getSlot(0).getItem();
	}

	public ItemStack material() {
		return getSlot(1).getItem();
	}

	/** Kann die Verbesserung jetzt angewendet werden? */
	public boolean canApply(ForgeUpgrade upgrade) {
		ItemStack item = equipment();

		if (item.isEmpty() || !upgrade.appliesTo(item)) {
			return false;
		}

		int level = ForgeUpgrade.level(item, upgrade);

		if (level >= upgrade.maxLevel) {
			return false;
		}

		ItemStack cost = upgrade.cost(level + 1);
		ItemStack mat = material();
		return ItemStack.isSameItem(mat, cost) && mat.getCount() >= cost.getCount();
	}

	@Override
	public boolean clickMenuButton(Player player, int id) {
		if (id < 0 || id >= ForgeUpgrade.values().length) {
			return false;
		}

		ForgeUpgrade upgrade = ForgeUpgrade.values()[id];

		if (!canApply(upgrade)) {
			return false;
		}

		ItemStack item = equipment();
		int level = ForgeUpgrade.level(item, upgrade);
		material().shrink(upgrade.cost(level + 1).getCount());
		ForgeUpgrade.set(item, upgrade, level + 1);
		slots.setChanged();
		access.execute((level2, pos) -> level2.playSound(null, pos, SoundEvents.SMITHING_TABLE_USE, SoundSource.BLOCKS, 1.0f, 1.1f));
		broadcastChanges();
		return true;
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		Slot slot = slots(index);

		if (slot == null || !slot.hasItem()) {
			return ItemStack.EMPTY;
		}

		ItemStack stack = slot.getItem();
		ItemStack copy = stack.copy();

		if (index < 2) {
			if (!moveItemStackTo(stack, 2, 38, true)) {
				return ItemStack.EMPTY;
			}
		} else if (stack.is(ForgeContent.FORGEABLE) && !getSlot(0).hasItem()) {
			if (!moveItemStackTo(stack, 0, 1, false)) {
				return ItemStack.EMPTY;
			}
		} else if (!moveItemStackTo(stack, 1, 2, false)) {
			return ItemStack.EMPTY;
		}

		if (stack.isEmpty()) {
			slot.setByPlayer(ItemStack.EMPTY);
		} else {
			slot.setChanged();
		}

		return copy;
	}

	private Slot slots(int index) {
		return index >= 0 && index < this.slots.getContainerSize() + 36 ? getSlot(index) : null;
	}

	@Override
	public void removed(Player player) {
		super.removed(player);
		access.execute((level, pos) -> clearContainer(player, slots));
	}

	@Override
	public boolean stillValid(Player player) {
		return stillValid(access, player, ForgeContent.FORGE);
	}
}
