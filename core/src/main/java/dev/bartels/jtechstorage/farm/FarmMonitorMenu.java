package dev.bartels.jtechstorage.farm;

import net.minecraft.network.chat.Component;
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

/**
 * Menu of the Farm Monitor: values (via ContainerData – 15 bits each, large numbers in two parts) and the most frequent items
 * (display slots). Buttons: 0–2 warning time, 10–14 protection zone, 20 reset.
 */
public class FarmMonitorMenu extends AbstractContainerMenu {
	public static final int WIDTH = 210;
	public static final int HEIGHT = 222;
	public static final int TOP_X = 10;
	public static final int TOP_Y = 136;
	public static final int TOP_STEP = 40;

	public static final int BUTTON_IDLE = 0;
	public static final int BUTTON_PROTECT = 10;
	public static final int BUTTON_RESET = 20;

	private static final int D_PER_HOUR = 0;       // 2 values
	private static final int D_STATUS = 2;
	private static final int D_IDLE = 3;
	private static final int D_PROTECT = 4;
	private static final int D_ELAPSED = 5;
	private static final int D_SINCE = 6;
	private static final int D_LINKED = 7;
	private static final int D_HISTORY = 8;        // 12 values
	private static final int D_TOP = D_HISTORY + FarmMonitorBlockEntity.HISTORY;   // 5 × 2 values
	private static final int D_COUNT = D_TOP + FarmMonitorBlockEntity.TOP * 2;

	public record OpenData(BlockPos pos, String name) {
		public static final StreamCodec<RegistryFriendlyByteBuf, OpenData> STREAM_CODEC = StreamCodec.composite(
				BlockPos.STREAM_CODEC, OpenData::pos,
				ByteBufCodecs.stringUtf8(64), OpenData::name,
				OpenData::new);
	}

	private final @Nullable FarmMonitorBlockEntity monitor;
	private final ContainerData data;
	private final BlockPos pos;
	private final String initialName;

	/** Client. */
	public FarmMonitorMenu(int containerId, Inventory inventory, OpenData open) {
		this(containerId, null, new SimpleContainer(FarmMonitorBlockEntity.TOP), new SimpleContainerData(D_COUNT), open.pos(), open.name());
	}

	/** Server. */
	public FarmMonitorMenu(int containerId, Inventory inventory, FarmMonitorBlockEntity monitor) {
		this(containerId, monitor, monitor.getTopDisplay(), new ContainerData() {
			@Override
			public int get(int index) {
				if (index >= D_HISTORY && index < D_TOP) {
					return Math.min(32767, monitor.history(index - D_HISTORY));
				}

				if (index >= D_TOP) {
					long count = Math.min((1L << 30) - 1, monitor.topCount((index - D_TOP) / 2));
					return (index - D_TOP) % 2 == 0 ? (int) (count & 0x7FFF) : (int) (count >> 15);
				}

				return switch (index) {
					case D_PER_HOUR -> monitor.perHour() & 0x7FFF;
					case D_PER_HOUR + 1 -> Math.min(32767, monitor.perHour() >> 15);
					case D_STATUS -> monitor.getStatus();
					case D_IDLE -> monitor.getIdle();
					case D_PROTECT -> monitor.getProtect();
					case D_ELAPSED -> monitor.elapsedMinutes();
					case D_SINCE -> monitor.minutesSinceInflow();
					case D_LINKED -> monitor.getTerminal() != null ? 1 : 0;
					default -> 0;
				};
			}

			@Override
			public void set(int index, int value) {
			}

			@Override
			public int getCount() {
				return D_COUNT;
			}
		}, monitor.getBlockPos(), monitor.getFarmName());
	}

	private FarmMonitorMenu(int containerId, @Nullable FarmMonitorBlockEntity monitor, Container top, ContainerData data, BlockPos pos,
			String initialName) {
		super(FarmContent.FARM_MONITOR_MENU, containerId);
		this.monitor = monitor;
		this.data = data;
		this.pos = pos;
		this.initialName = initialName;
		addDataSlots(data);

		for (int i = 0; i < FarmMonitorBlockEntity.TOP; i++) {
			addSlot(new Slot(top, i, TOP_X + i * TOP_STEP, TOP_Y) {
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

	public BlockPos pos() {
		return pos;
	}

	public String initialName() {
		return initialName;
	}

	public int perHour() {
		return data.get(D_PER_HOUR) | data.get(D_PER_HOUR + 1) << 15;
	}

	public int status() {
		return data.get(D_STATUS);
	}

	public int idle() {
		return data.get(D_IDLE);
	}

	public int protect() {
		return data.get(D_PROTECT);
	}

	public int elapsed() {
		return data.get(D_ELAPSED);
	}

	public int since() {
		return (short) data.get(D_SINCE);
	}

	public boolean linked() {
		return data.get(D_LINKED) != 0;
	}

	public int history(int bucket) {
		return data.get(D_HISTORY + bucket);
	}

	public long topCount(int index) {
		return data.get(D_TOP + index * 2) | (long) data.get(D_TOP + index * 2 + 1) << 15;
	}

	@Override
	public void clicked(int slotIndex, int button, ContainerInput input, Player player) {
		if (slotIndex >= 0 && slotIndex < FarmMonitorBlockEntity.TOP) {
			return;
		}

		super.clicked(slotIndex, button, input, player);
	}

	@Override
	public boolean clickMenuButton(Player player, int id) {
		if (monitor == null) {
			return false;
		}

		if (!monitor.mayEdit(player)) {
			player.sendOverlayMessage(Component.translatable("jtechstorage.farm.not_owner"));
			return false;
		}

		if (id >= BUTTON_IDLE && id < BUTTON_IDLE + FarmMonitorBlockEntity.IDLE_MINUTES.length) {
			monitor.setIdle(id - BUTTON_IDLE);
			return true;
		}

		if (id >= BUTTON_PROTECT && id < BUTTON_PROTECT + FarmMonitorBlockEntity.RADII.length) {
			monitor.setProtect(id - BUTTON_PROTECT);
			return true;
		}

		if (id == BUTTON_RESET) {
			monitor.reset();
			return true;
		}

		return false;
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		return ItemStack.EMPTY;
	}

	@Override
	public boolean stillValid(Player player) {
		return monitor == null || !monitor.isRemoved() && monitor.getBlockPos().distToCenterSqr(player.position()) <= 8.0 * 8.0;
	}
}
