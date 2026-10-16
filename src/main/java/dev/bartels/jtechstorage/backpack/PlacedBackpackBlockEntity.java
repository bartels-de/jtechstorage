package dev.bartels.jtechstorage.backpack;

import java.util.List;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import dev.bartels.jtechstorage.ModContent;

/**
 * Ein abgestellter Rucksack. Merkt sich das Rucksack-Item (Stufe, Farbe, Name) und hält den Inhalt als echten
 * Container – Trichter und der Amethyst-Wichtel können ihn leeren. Beim Abbauen fällt der Rucksack samt Inhalt heraus.
 */
public class PlacedBackpackBlockEntity extends BlockEntity implements Container {
	private ItemStack backpack = ItemStack.EMPTY;
	private NonNullList<ItemStack> items = NonNullList.withSize(9, ItemStack.EMPTY);

	public PlacedBackpackBlockEntity(BlockPos pos, BlockState state) {
		super(ModContent.PLACED_BACKPACK_BLOCK_ENTITY, pos, state);
	}

	/** Beim Abstellen: Rucksack-Item übernehmen, Inhalt auspacken. */
	public void setBackpack(ItemStack stack) {
		backpack = stack.copyWithCount(1);
		int size = backpack.getItem() instanceof BackpackItem item ? item.getTier().slots() : 9;
		items = BackpackItem.readContents(backpack, size);
		backpack.remove(net.minecraft.core.component.DataComponents.CONTAINER);
		setChanged();
		sync();
	}

	/** Das Rucksack-Item (ohne Inhalt) – für Anzeige und Farbe. */
	public ItemStack getBackpack() {
		return backpack;
	}

	public BackpackItem.@Nullable Tier tier() {
		return backpack.getItem() instanceof BackpackItem item ? item.getTier() : null;
	}

	/** Rucksack samt Inhalt als Item (zum Fallenlassen beim Abbauen). */
	public ItemStack toItem() {
		if (backpack.isEmpty()) {
			return ItemStack.EMPTY;
		}

		ItemStack result = backpack.copy();
		BackpackItem.writeContents(result, items);
		return result;
	}

	private void sync() {
		if (level != null && !level.isClientSide()) {
			level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
		}
	}

	@Override
	public void preRemoveSideEffects(BlockPos pos, BlockState state) {
		super.preRemoveSideEffects(pos, state);

		if (level != null && !level.isClientSide()) {
			ItemStack drop = toItem();

			if (!drop.isEmpty()) {
				Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.3, pos.getZ() + 0.5, drop);
			}

			backpack = ItemStack.EMPTY;
			items.clear();
		}
	}

	// ------------------------------------------------------------------
	// Container
	// ------------------------------------------------------------------

	@Override
	public int getContainerSize() {
		return items.size();
	}

	@Override
	public boolean isEmpty() {
		return items.stream().allMatch(ItemStack::isEmpty);
	}

	@Override
	public ItemStack getItem(int slot) {
		return items.get(slot);
	}

	@Override
	public ItemStack removeItem(int slot, int amount) {
		ItemStack result = ContainerHelper.removeItem(items, slot, amount);

		if (!result.isEmpty()) {
			setChanged();
		}

		return result;
	}

	@Override
	public ItemStack removeItemNoUpdate(int slot) {
		return ContainerHelper.takeItem(items, slot);
	}

	@Override
	public void setItem(int slot, ItemStack stack) {
		items.set(slot, stack);
		stack.limitSize(getMaxStackSize(stack));
		setChanged();
	}

	@Override
	public boolean canPlaceItem(int slot, ItemStack stack) {
		return !BackpackItem.isForbidden(stack);
	}

	@Override
	public boolean stillValid(Player player) {
		return Container.stillValidBlockEntity(this, player);
	}

	@Override
	public void clearContent() {
		items.clear();
	}

	public List<ItemStack> getItems() {
		return items;
	}

	// ------------------------------------------------------------------
	// Speichern & Synchronisieren
	// ------------------------------------------------------------------

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);

		if (!backpack.isEmpty()) {
			output.store("Backpack", ItemStack.CODEC, backpack);
		}

		output.putInt("Size", items.size());
		ContainerHelper.saveAllItems(output, items);
	}

	/** Farbe des Stoffs (Färbung oder Standardfarbe der Stufe) – für die Block-Tönung. */
	public int fabricColor() {
		BackpackItem.Tier tier = tier();
		net.minecraft.world.item.component.DyedItemColor dyed = backpack.get(net.minecraft.core.component.DataComponents.DYED_COLOR);
		return dyed != null ? dyed.rgb() : tier != null ? tier.defaultColor : 0x8B5A2B;
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		backpack = input.read("Backpack", ItemStack.CODEC).orElse(ItemStack.EMPTY);

		if (level != null && level.isClientSide()) {
			level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 8);   // neu einfärben
		}
		items = NonNullList.withSize(Math.max(1, input.getIntOr("Size", 9)), ItemStack.EMPTY);
		ContainerHelper.loadAllItems(input, items);
	}

	/** Für den Client nur das Rucksack-Item (Farbe/Stufe), nicht den Inhalt. */
	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		CompoundTag tag = new CompoundTag();

		if (!backpack.isEmpty()) {
			tag.store("Backpack", ItemStack.CODEC, registries.createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE), backpack);
		}

		return tag;
	}

	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}
}
