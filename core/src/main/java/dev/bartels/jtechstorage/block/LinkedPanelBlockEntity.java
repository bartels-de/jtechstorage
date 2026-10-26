package dev.bartels.jtechstorage.block;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Base for additional terminals (team terminal, trade terminal) that are linked to a storage terminal
 * (right-click with a bound key). All network data stays in the storage terminal.
 */
public abstract class LinkedPanelBlockEntity extends BlockEntity {
	private @Nullable BlockPos link;

	protected LinkedPanelBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	public @Nullable BlockPos getLink() {
		return link;
	}

	public void setLink(BlockPos pos) {
		link = pos.immutable();
		setChanged();
	}

	/** The linked storage terminal, if loaded and present. */
	public @Nullable StorageTerminalBlockEntity terminal() {
		if (link == null || level == null || !level.isLoaded(link)) {
			return null;
		}

		return level.getBlockEntity(link) instanceof StorageTerminalBlockEntity terminal ? terminal : null;
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);

		if (link != null) {
			output.store("Link", BlockPos.CODEC, link);
		}
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		link = input.read("Link", BlockPos.CODEC).orElse(null);
	}
}
