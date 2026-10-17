package dev.bartels.jtechstorage.menu;

import java.util.List;

import org.jspecify.annotations.Nullable;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import dev.bartels.jtechstorage.ModContent;
import dev.bartels.jtechstorage.entity.AmethystPhoenix;

/**
 * Menü des Amethyst-Phönix: vier Kisten (je 27 Plätze, 2 × 2 angeordnet – nicht angelegte Kisten sind gesperrt),
 * Status, Route, Knöpfe für Sitz-Modus, Abflug, Kiste abnehmen und Einpacken.
 */
public class AmethystPhoenixMenu extends AbstractContainerMenu {
	public static final int WIDTH = 344;
	public static final int HEADER = 70;
	public static final int CHEST_W = 9 * 18;
	public static final int CHEST_H = 3 * 18 + 12;
	public static final int CHEST_X0 = 8;
	public static final int CHEST_X1 = WIDTH - 8 - CHEST_W;
	public static final int CHEST_Y0 = HEADER + 2;
	public static final int CHEST_Y1 = CHEST_Y0 + CHEST_H;
	public static final int INVENTORY_X = (WIDTH - CHEST_W) / 2;
	public static final int INVENTORY_Y = CHEST_Y1 + CHEST_H + 12;
	public static final int HEIGHT = INVENTORY_Y + 3 * 18 + 4 + 18 + 8;

	public static final int BUTTON_SIT = 0;
	public static final int BUTTON_DEPART = 1;
	public static final int BUTTON_REMOVE_CHEST = 2;
	public static final int BUTTON_PACK = 3;

	private static final int CARGO = AmethystPhoenix.CHEST_SLOTS * AmethystPhoenix.MAX_CHESTS;

	/** Haltestellen-Namen in Flugreihenfolge und Index des aktuellen Ziels. */
	public record OpenData(int entityId, List<String> stops, int target) {
		public static final StreamCodec<RegistryFriendlyByteBuf, OpenData> STREAM_CODEC = StreamCodec.composite(
				ByteBufCodecs.VAR_INT, OpenData::entityId,
				ByteBufCodecs.stringUtf8(64).apply(ByteBufCodecs.list(8)), OpenData::stops,
				ByteBufCodecs.VAR_INT, OpenData::target,
				OpenData::new);
	}

	private final @Nullable AmethystPhoenix phoenix;
	private final ContainerData data;
	private final @Nullable OpenData openData;

	/** Client. */
	public AmethystPhoenixMenu(int containerId, Inventory inventory, OpenData openData) {
		this(containerId, inventory, null, new SimpleContainer(CARGO), new SimpleContainerData(6), openData);
	}

	/** Server. */
	public AmethystPhoenixMenu(int containerId, Inventory inventory, AmethystPhoenix phoenix) {
		this(containerId, inventory, phoenix, phoenix.getCargo(), new ContainerData() {
			@Override
			public int get(int index) {
				return switch (index) {
					case 0 -> phoenix.getChests();
					case 1 -> phoenix.isSitting() ? 1 : 0;
					case 2 -> phoenix.getState();
					case 3 -> phoenix.getSpeedPercent();
					case 4 -> Math.min(30000, phoenix.getMinutesUntilDeparture() + 1);
					case 5 -> phoenix.getFillPercent();
					default -> 0;
				};
			}

			@Override
			public void set(int index, int value) {
			}

			@Override
			public int getCount() {
				return 6;
			}
		}, null);
	}

	private AmethystPhoenixMenu(int containerId, Inventory inventory, @Nullable AmethystPhoenix phoenix, Container cargo, ContainerData data,
			@Nullable OpenData openData) {
		super(ModContent.AMETHYST_PHOENIX_MENU, containerId);
		this.phoenix = phoenix;
		this.data = data;
		this.openData = openData;
		addDataSlots(data);

		for (int chest = 0; chest < AmethystPhoenix.MAX_CHESTS; chest++) {
			int x0 = chest % 2 == 0 ? CHEST_X0 : CHEST_X1;
			int y0 = chest < 2 ? CHEST_Y0 : CHEST_Y1;

			for (int row = 0; row < 3; row++) {
				for (int col = 0; col < 9; col++) {
					addSlot(new CargoSlot(cargo, chest * AmethystPhoenix.CHEST_SLOTS + row * 9 + col, x0 + col * 18 + 1, y0 + 10 + row * 18 + 1, chest));
				}
			}
		}

		for (int row = 0; row < 3; row++) {
			for (int col = 0; col < 9; col++) {
				addSlot(new Slot(inventory, col + row * 9 + 9, INVENTORY_X + col * 18 + 1, INVENTORY_Y + row * 18 + 1));
			}
		}

		for (int col = 0; col < 9; col++) {
			addSlot(new Slot(inventory, col, INVENTORY_X + col * 18 + 1, INVENTORY_Y + 58 + 1));
		}
	}

	public int chests() {
		return data.get(0);
	}

	public boolean sitting() {
		return data.get(1) == 1;
	}

	public int state() {
		return data.get(2);
	}

	public int speedPercent() {
		return data.get(3);
	}

	/** Minuten bis Abflug, -1 = nicht am Rasten. */
	public int minutesLeft() {
		return data.get(4) - 1;
	}

	public int fillPercent() {
		return data.get(5);
	}

	public List<String> stops() {
		return openData == null ? List.of() : openData.stops();
	}

	public int target() {
		return openData == null ? 0 : openData.target();
	}

	private class CargoSlot extends Slot {
		private final int chest;

		CargoSlot(Container container, int slot, int x, int y, int chest) {
			super(container, slot, x, y);
			this.chest = chest;
		}

		@Override
		public boolean isActive() {
			return chest < chests();
		}

		@Override
		public boolean mayPlace(ItemStack stack) {
			return isActive();
		}

		@Override
		public boolean mayPickup(Player player) {
			return isActive();
		}
	}

	@Override
	public boolean clickMenuButton(Player player, int id) {
		if (phoenix == null) {
			return false;
		}

		switch (id) {
			case BUTTON_SIT -> phoenix.setSitting(!phoenix.isSitting());
			case BUTTON_DEPART -> {
				if (!phoenix.departNow()) {
					player.sendOverlayMessage(net.minecraft.network.chat.Component.translatable("jtechstorage.amethyst_phoenix.cannot_depart"));
				}
			}
			case BUTTON_REMOVE_CHEST -> phoenix.removeChest(player);
			case BUTTON_PACK -> {
				if (phoenix.pack(player) && player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
					serverPlayer.closeContainer();
				}
			}
			default -> {
				return false;
			}
		}

		return true;
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		Slot slot = slots.get(index);

		if (!slot.hasItem()) {
			return ItemStack.EMPTY;
		}

		ItemStack stack = slot.getItem();
		ItemStack copy = stack.copy();
		int cargoEnd = Math.min(CARGO, chests() * AmethystPhoenix.CHEST_SLOTS);

		if (index < CARGO) {
			if (!moveItemStackTo(stack, CARGO, slots.size(), true)) {
				return ItemStack.EMPTY;
			}
		} else if (cargoEnd == 0 || !moveItemStackTo(stack, 0, cargoEnd, false)) {
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
		return phoenix == null || phoenix.isAlive() && !phoenix.isRemoved() && player.distanceToSqr(phoenix) < 10 * 10;
	}
}
