package dev.bartels.jtechstorage.chest;

import java.util.ArrayList;
import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.ContainerOpenersCounter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Amethyst-Kiste (v0.28.0): großer Container mit vervielfachten Stapeln. Gehört über den Block-Tag
 * {@code jtechstorage:network_storage} zum Speichernetz. Stapel über 99 lassen sich nicht mit dem normalen Item-Codec
 * speichern, deshalb eigenes Format (Item mit Anzahl 1 + Anzahl).
 */
public class AmethystChestBlockEntity extends BaseContainerBlockEntity {
	private record Saved(int slot, ItemStack item, int count) {
		static final Codec<Saved> CODEC = RecordCodecBuilder.create(i -> i.group(
				Codec.INT.fieldOf("Slot").forGetter(Saved::slot),
				ItemStack.CODEC.fieldOf("Item").forGetter(Saved::item),
				Codec.INT.fieldOf("Count").forGetter(Saved::count)
		).apply(i, Saved::new));
	}

	private final AmethystChestTier tier;
	private NonNullList<ItemStack> items;
	private final ContainerOpenersCounter openers = new ContainerOpenersCounter() {
		@Override
		protected void onOpen(Level level, BlockPos pos, BlockState state) {
			sound(state, SoundEvents.BARREL_OPEN);
			setOpen(state, true);
		}

		@Override
		protected void onClose(Level level, BlockPos pos, BlockState state) {
			sound(state, SoundEvents.BARREL_CLOSE);
			setOpen(state, false);
		}

		@Override
		protected void openerCountChanged(Level level, BlockPos pos, BlockState state, int previous, int now) {
		}

		@Override
		public boolean isOwnContainer(Player player) {
			return player.containerMenu instanceof AmethystChestMenu menu && menu.container() == AmethystChestBlockEntity.this;
		}
	};

	public AmethystChestBlockEntity(BlockPos pos, BlockState state) {
		super(ChestContent.AMETHYST_CHEST_BLOCK_ENTITY, pos, state);
		tier = state.getBlock() instanceof AmethystChestBlock block ? block.tier() : AmethystChestTier.AMETHYST;
		items = NonNullList.withSize(tier.size(), ItemStack.EMPTY);
	}

	public AmethystChestTier tier() {
		return tier;
	}

	@Override
	public int getContainerSize() {
		return tier.size();
	}

	@Override
	public int getMaxStackSize() {
		return 64 * tier.stackFactor;
	}

	@Override
	public int getMaxStackSize(ItemStack stack) {
		return tier.maxStack(stack);
	}

	/** Legt {@code stack} ab (füllt erst passende Stapel, dann leere Plätze); gibt den Rest zurück. */
	public ItemStack insert(ItemStack stack) {
		if (stack.isEmpty() || !canPlaceItem(0, stack)) {
			return stack;
		}

		int limit = getMaxStackSize(stack);
		boolean changed = false;

		for (int pass = 0; pass < 2 && !stack.isEmpty(); pass++) {
			for (int slot = 0; slot < items.size() && !stack.isEmpty(); slot++) {
				ItemStack existing = items.get(slot);

				if (pass == 0 && !existing.isEmpty() && ItemStack.isSameItemSameComponents(existing, stack) && existing.getCount() < limit) {
					int move = Math.min(stack.getCount(), limit - existing.getCount());
					existing.grow(move);
					stack.shrink(move);
					changed = true;
				} else if (pass == 1 && existing.isEmpty()) {
					items.set(slot, stack.split(Math.min(stack.getCount(), limit)));
					changed = true;
				}
			}
		}

		if (changed) {
			setChanged();
		}

		return stack;
	}

	@Override
	public boolean canPlaceItem(int slot, ItemStack stack) {
		// wie eine Shulkerkiste: keine Kisten mit Inhalt in Kisten
		return stack.getItem().canFitInsideContainerItems();
	}

	// ------------------------------------------------------------------
	// Öffnen (wie ein Fass)
	// ------------------------------------------------------------------

	@Override
	public void startOpen(ContainerUser user) {
		if (!remove && !user.getLivingEntity().isSpectator()) {
			openers.incrementOpeners(user.getLivingEntity(), getLevel(), getBlockPos(), getBlockState(), user.getContainerInteractionRange());
		}
	}

	@Override
	public void stopOpen(ContainerUser user) {
		if (!remove && !user.getLivingEntity().isSpectator()) {
			openers.decrementOpeners(user.getLivingEntity(), getLevel(), getBlockPos(), getBlockState());
		}
	}

	@Override
	public List<ContainerUser> getEntitiesWithContainerOpen() {
		return openers.getEntitiesWithContainerOpen(getLevel(), getBlockPos());
	}

	public void recheckOpen() {
		if (!remove) {
			openers.recheckOpeners(getLevel(), getBlockPos(), getBlockState());
		}
	}

	private void setOpen(BlockState state, boolean open) {
		if (level != null && state.hasProperty(AmethystChestBlock.OPEN)) {
			level.setBlock(getBlockPos(), state.setValue(AmethystChestBlock.OPEN, open), 3);
		}
	}

	private void sound(BlockState state, SoundEvent sound) {
		if (level != null) {
			level.playSound(null, getBlockPos(), sound, SoundSource.BLOCKS, 0.5f, level.getRandom().nextFloat() * 0.1f + 0.9f);
			level.playSound(null, getBlockPos(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.6f, 0.8f + level.getRandom().nextFloat() * 0.4f);
		}
	}

	// ------------------------------------------------------------------
	// Abbauen: Inhalt in normalen Stapeln fallen lassen
	// ------------------------------------------------------------------

	@Override
	public void preRemoveSideEffects(BlockPos pos, BlockState state) {
		if (level != null && !level.isClientSide()) {
			for (ItemStack stack : takeAll()) {
				Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack);
			}
		}

		super.preRemoveSideEffects(pos, state);
	}

	/** Nimmt den ganzen Inhalt heraus, aufgeteilt in normale Stapel. */
	public List<ItemStack> takeAll() {
		List<ItemStack> out = new ArrayList<>();

		for (int slot = 0; slot < items.size(); slot++) {
			ItemStack stack = items.get(slot);

			while (!stack.isEmpty()) {
				out.add(stack.split(Math.max(1, stack.getMaxStackSize())));
			}

			items.set(slot, ItemStack.EMPTY);
		}

		setChanged();
		return out;
	}

	/** Inhalt unverändert (für das Aufwerten an Ort und Stelle). */
	List<ItemStack> copyContents() {
		List<ItemStack> out = new ArrayList<>();

		for (ItemStack stack : items) {
			out.add(stack.copy());
		}

		return out;
	}

	void clearForUpgrade() {
		items = NonNullList.withSize(tier.size(), ItemStack.EMPTY);
	}

	void fill(List<ItemStack> contents) {
		for (int slot = 0; slot < contents.size() && slot < items.size(); slot++) {
			items.set(slot, contents.get(slot));
		}

		setChanged();
	}

	// ------------------------------------------------------------------
	// Container-Grundlagen
	// ------------------------------------------------------------------

	@Override
	protected Component getDefaultName() {
		return Component.translatable("block.jtechstorage." + tier.id);
	}

	@Override
	protected NonNullList<ItemStack> getItems() {
		return items;
	}

	@Override
	protected void setItems(NonNullList<ItemStack> items) {
		NonNullList<ItemStack> sized = NonNullList.withSize(tier.size(), ItemStack.EMPTY);

		for (int slot = 0; slot < items.size() && slot < sized.size(); slot++) {
			sized.set(slot, items.get(slot));
		}

		this.items = sized;
	}

	@Override
	protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
		return new AmethystChestMenu(containerId, inventory, this);
	}

	@Override
	protected void collectImplicitComponents(DataComponentMap.Builder builder) {
		// kein CONTAINER-Inhalt am Item (große Stapel); nur der Name
		if (getCustomName() != null) {
			builder.set(DataComponents.CUSTOM_NAME, getCustomName());
		}
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		List<Saved> saved = new ArrayList<>();

		for (int slot = 0; slot < items.size(); slot++) {
			ItemStack stack = items.get(slot);

			if (!stack.isEmpty()) {
				saved.add(new Saved(slot, stack.copyWithCount(1), stack.getCount()));
			}
		}

		output.store("BigItems", Saved.CODEC.listOf(), saved);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		items = NonNullList.withSize(tier.size(), ItemStack.EMPTY);

		for (Saved saved : input.read("BigItems", Saved.CODEC.listOf()).orElse(List.of())) {
			if (saved.slot() >= 0 && saved.slot() < items.size() && saved.count() > 0) {
				items.set(saved.slot(), saved.item().copyWithCount(saved.count()));
			}
		}
	}
}
