package dev.bartels.jtechstorage.depths.memory;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import com.mojang.serialization.Codec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Contents of the Memory Case (v0.36): which of the twelve crystals are displayed, in insertion order. */
public class MemoryCaseBlockEntity extends BlockEntity {
	private final List<Integer> order = new ArrayList<>();

	public MemoryCaseBlockEntity(BlockPos pos, BlockState state) {
		super(MemoryContent.MEMORY_CASE_BLOCK_ENTITY, pos, state);
	}

	public boolean has(int kind) {
		return order.contains(kind);
	}

	public int count() {
		return order.size();
	}

	/** Bitmask of the displayed crystals (bit = index into {@link MemoryContent#KINDS}). */
	public int mask() {
		int mask = 0;

		for (int kind : order) {
			mask |= 1 << kind;
		}

		return mask;
	}

	void add(int kind) {
		if (kind >= 0 && kind < MemoryContent.KINDS.length && !has(kind)) {
			order.add(kind);
			sync();
		}
	}

	ItemStack takeLast() {
		if (order.isEmpty()) {
			return ItemStack.EMPTY;
		}

		int kind = order.remove(order.size() - 1);
		sync();
		return new ItemStack(MemoryContent.CRYSTALS[kind]);
	}

	private void sync() {
		setChanged();

		if (level != null) {
			level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
		}
	}

	@Override
	public void preRemoveSideEffects(BlockPos pos, BlockState state) {
		super.preRemoveSideEffects(pos, state);

		if (level != null && !level.isClientSide()) {
			for (int kind : order) {
				Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, new ItemStack(MemoryContent.CRYSTALS[kind]));
			}

			order.clear();
		}
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.store("Memories", Codec.INT.listOf(), List.copyOf(order));
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		order.clear();

		for (int kind : input.read("Memories", Codec.INT.listOf()).orElse(List.of())) {
			if (kind >= 0 && kind < MemoryContent.KINDS.length && !order.contains(kind)) {
				order.add(kind);
			}
		}
	}

	@Override
	public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		return saveCustomOnly(registries);
	}
}
