package dev.bartels.jtechstorage.farm;

import org.jspecify.annotations.Nullable;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import dev.bartels.jtechstorage.ModConfig;

/** Chunk Loader: Treibstoff-Platz, Status, Restlaufzeit, Anzahl/Limit, Chunk; Knopf 0 = Chunk-Grenzen zeigen. */
public class ChunkLoaderMenu extends AbstractContainerMenu {
	public static final int WIDTH = 176;
	public static final int FUEL_X = 12;
	public static final int FUEL_Y = 22;
	public static final int INVENTORY_Y = 100;
	public static final int HEIGHT = INVENTORY_Y + 58 + 18 + 6;
	public static final int BUTTON_BOUNDS = 0;

	private static final int D_STATUS = 0;
	private static final int D_MINUTES = 1;    // 2 Werte
	private static final int D_CX = 3;         // 2 Werte
	private static final int D_CZ = 5;         // 2 Werte
	private static final int D_COUNT = 7;
	private static final int D_LIMIT = 8;
	private static final int D_SIZE = 9;

	private final @Nullable ChunkLoaderBlockEntity loader;
	private final ContainerData data;

	/** Client. */
	public ChunkLoaderMenu(int containerId, Inventory inventory) {
		this(containerId, inventory, null, new SimpleContainer(1), new SimpleContainerData(D_SIZE));
	}

	/** Server. */
	public ChunkLoaderMenu(int containerId, Inventory inventory, ChunkLoaderBlockEntity loader) {
		this(containerId, inventory, loader, loader, new ContainerData() {
			@Override
			public int get(int index) {
				int cx = loader.getBlockPos().getX() >> 4;
				int cz = loader.getBlockPos().getZ() >> 4;
				int minutes = Math.min(1 << 29, loader.minutesLeft());
				return switch (index) {
					case D_STATUS -> loader.getStatus();
					case D_MINUTES -> minutes & 0x7FFF;
					case D_MINUTES + 1 -> minutes >> 15;
					case D_CX -> cx & 0x7FFF;
					case D_CX + 1 -> cx >> 15;
					case D_CZ -> cz & 0x7FFF;
					case D_CZ + 1 -> cz >> 15;
					case D_COUNT -> loader.ownerCount();
					case D_LIMIT -> ModConfig.get().farm.chunkLoadersPerPlayer;
					default -> 0;
				};
			}

			@Override
			public void set(int index, int value) {
			}

			@Override
			public int getCount() {
				return D_SIZE;
			}
		});
	}

	private ChunkLoaderMenu(int containerId, Inventory inventory, @Nullable ChunkLoaderBlockEntity loader, Container fuel, ContainerData data) {
		super(FarmContent.CHUNK_LOADER_MENU, containerId);
		this.loader = loader;
		this.data = data;
		addDataSlots(data);
		addSlot(new Slot(fuel, 0, FUEL_X, FUEL_Y) {
			@Override
			public boolean mayPlace(ItemStack stack) {
				return ChunkLoaderBlockEntity.shardValue(stack) > 0;
			}
		});

		for (int row = 0; row < 3; row++) {
			for (int col = 0; col < 9; col++) {
				addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, INVENTORY_Y + row * 18));
			}
		}

		for (int col = 0; col < 9; col++) {
			addSlot(new Slot(inventory, col, 8 + col * 18, INVENTORY_Y + 58));
		}
	}

	public int status() {
		return data.get(D_STATUS);
	}

	public int minutesLeft() {
		return data.get(D_MINUTES) | data.get(D_MINUTES + 1) << 15;
	}

	public int chunkX() {
		return (short) data.get(D_CX + 1) << 15 | data.get(D_CX);
	}

	public int chunkZ() {
		return (short) data.get(D_CZ + 1) << 15 | data.get(D_CZ);
	}

	public int count() {
		return data.get(D_COUNT);
	}

	public int limit() {
		return data.get(D_LIMIT);
	}

	@Override
	public boolean clickMenuButton(Player player, int id) {
		if (id == BUTTON_BOUNDS && loader != null && player instanceof ServerPlayer serverPlayer) {
			loader.showBounds(serverPlayer);
			return true;
		}

		return false;
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		Slot slot = slots.get(index);

		if (!slot.hasItem()) {
			return ItemStack.EMPTY;
		}

		ItemStack stack = slot.getItem();
		ItemStack copy = stack.copy();

		if (index == 0) {
			if (!moveItemStackTo(stack, 1, slots.size(), true)) {
				return ItemStack.EMPTY;
			}
		} else if (ChunkLoaderBlockEntity.shardValue(stack) > 0) {
			if (!moveItemStackTo(stack, 0, 1, false)) {
				return ItemStack.EMPTY;
			}
		} else {
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
		return loader == null || !loader.isRemoved() && loader.getBlockPos().distToCenterSqr(player.position()) <= 8.0 * 8.0;
	}
}
