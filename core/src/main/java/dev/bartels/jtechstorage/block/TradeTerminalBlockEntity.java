package dev.bartels.jtechstorage.block;

import java.util.List;

import com.mojang.serialization.Codec;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Prediction;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import dev.bartels.jtechstorage.ModAdvancements;
import dev.bartels.jtechstorage.ModContent;
import dev.bartels.jtechstorage.network.StorageNetwork;

/**
 * Trade terminal: up to {@value #OFFERS} "goods for price" offers. The goods come straight from the linked
 * network, payment is stored there - even if the owner is offline (as long as the base is loaded).
 */
public class TradeTerminalBlockEntity extends LinkedPanelBlockEntity {
	public static final int OFFERS = 6;
	/** Limit for the stock display (transferred as a short). */
	public static final int MAX_SHOWN_STOCK = 30000;
	public static final int STOCK_NONE = -1;
	public static final int STOCK_UNREACHABLE = -2;

	private final SimpleContainer offers = new SimpleContainer(OFFERS * 2) {
		@Override
		public void setChanged() {
			super.setChanged();
			TradeTerminalBlockEntity.this.setChanged();
		}
	};
	private final int[] sold = new int[OFFERS];

	public TradeTerminalBlockEntity(BlockPos pos, BlockState state) {
		super(ModContent.TRADE_TERMINAL_BLOCK_ENTITY, pos, state);
	}

	/** Slot 2i = goods, 2i+1 = price. */
	public SimpleContainer getOffers() {
		return offers;
	}

	public ItemStack goods(int index) {
		return offers.getItem(index * 2);
	}

	public ItemStack price(int index) {
		return offers.getItem(index * 2 + 1);
	}

	public int sold(int index) {
		return sold[index];
	}

	/** How many more times can the offer be bought? (or {@link #STOCK_NONE} / {@link #STOCK_UNREACHABLE}) */
	public int[] stocks() {
		int[] result = new int[OFFERS];
		StorageTerminalBlockEntity terminal = terminal();
		StorageNetwork network = terminal != null ? terminal.buildNetwork() : null;

		for (int i = 0; i < OFFERS; i++) {
			ItemStack goods = goods(i);

			if (goods.isEmpty() || price(i).isEmpty()) {
				result[i] = STOCK_NONE;
			} else if (network == null) {
				result[i] = STOCK_UNREACHABLE;
			} else {
				result[i] = (int) Math.min(MAX_SHOWN_STOCK, network.count(goods) / goods.getCount());
			}
		}

		return result;
	}

	/** Buys an offer up to {@code times} times. */
	public void buy(ServerPlayer buyer, int index, int times) {
		if (index < 0 || index >= OFFERS) {
			return;
		}

		ItemStack goods = goods(index).copy();
		ItemStack price = price(index).copy();

		if (goods.isEmpty() || price.isEmpty()) {
			return;
		}

		StorageTerminalBlockEntity terminal = terminal();

		if (terminal == null) {
			buyer.sendOverlayMessage(Component.translatable("jtechstorage.trade.unreachable"));
			return;
		}

		StorageNetwork network = terminal.buildNetwork();
		int done = 0;
		String problem = null;

		for (int t = 0; t < times; t++) {
			if (network.count(goods) < goods.getCount()) {
				problem = "jtechstorage.trade.out_of_stock";
				break;
			}

			if (countInInventory(buyer.getInventory(), price) < price.getCount()) {
				problem = "jtechstorage.trade.no_payment";
				break;
			}

			if (network.spaceFor(price, price.getCount()) < price.getCount()) {
				problem = "jtechstorage.trade.shop_full";
				break;
			}

			ItemStack got = network.extract(goods, goods.getCount());

			if (got.getCount() < goods.getCount()) {
				network.insert(got);
				problem = "jtechstorage.trade.out_of_stock";
				break;
			}

			removeFromInventory(buyer.getInventory(), price, price.getCount());
			ItemStack rest = network.insert(price.copy());

			if (!rest.isEmpty()) {
				buyer.getInventory().placeItemBackInInventory(rest, Prediction.SERVER_ONLY);
			}

			buyer.getInventory().placeItemBackInInventory(got, Prediction.SERVER_ONLY);
			done++;
		}

		if (done > 0) {
			sold[index] += done;
			setChanged();
			buyer.level().playSound(null, getBlockPos(), SoundEvents.VILLAGER_YES, SoundSource.BLOCKS, 0.6f, 1.3f);
			buyer.level().playSound(null, getBlockPos(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.8f, 1.5f);
			Component deal = Component.translatable("jtechstorage.trade.deal", done, goods.getCount(), goods.getHoverName(),
					price.getCount(), price.getHoverName());
			buyer.sendOverlayMessage(Component.translatable("jtechstorage.trade.bought", deal));
			terminal.broadcast(StorageTerminalBlockEntity.Channel.TRADE,
					Component.translatable("jtechstorage.channel.msg.trade", buyer.getPlainTextName(), deal));
			ModAdvancements.grant(buyer, "trade");
		} else if (problem != null) {
			buyer.sendOverlayMessage(Component.translatable(problem));
			buyer.level().playSound(null, getBlockPos(), SoundEvents.VILLAGER_NO, SoundSource.BLOCKS, 0.6f, 1.3f);
		}
	}

	private static int countInInventory(Inventory inventory, ItemStack prototype) {
		int total = 0;

		for (int slot = 0; slot < Inventory.INVENTORY_SIZE; slot++) {
			ItemStack stack = inventory.getItem(slot);

			if (ItemStack.isSameItemSameComponents(stack, prototype)) {
				total += stack.getCount();
			}
		}

		return total;
	}

	private static void removeFromInventory(Inventory inventory, ItemStack prototype, int amount) {
		for (int slot = 0; slot < Inventory.INVENTORY_SIZE && amount > 0; slot++) {
			ItemStack stack = inventory.getItem(slot);

			if (ItemStack.isSameItemSameComponents(stack, prototype)) {
				int taken = Math.min(amount, stack.getCount());
				stack.shrink(taken);
				amount -= taken;
			}
		}

		inventory.setChanged();
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);

		for (int i = 0; i < offers.getContainerSize(); i++) {
			ItemStack stack = offers.getItem(i);

			if (!stack.isEmpty()) {
				output.store("Offer" + i, ItemStack.CODEC, stack);
			}
		}

		output.store("Sold", Codec.INT.listOf(), java.util.Arrays.stream(sold).boxed().toList());
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);

		for (int i = 0; i < offers.getContainerSize(); i++) {
			offers.setItem(i, input.read("Offer" + i, ItemStack.CODEC).orElse(ItemStack.EMPTY));
		}

		List<Integer> soldList = input.read("Sold", Codec.INT.listOf()).orElse(List.of());

		for (int i = 0; i < OFFERS; i++) {
			sold[i] = i < soldList.size() ? soldList.get(i) : 0;
		}
	}
}
