package dev.bartels.jtechstorage.block;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Basis für Zusatz-Terminals (Team-Terminal, Handels-Terminal), die mit einem Speicher-Terminal verknüpft werden
 * (Rechtsklick mit einem gekoppelten Schlüssel). Alle Netzwerk-Daten bleiben im Speicher-Terminal.
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

	/** Das verknüpfte Speicher-Terminal, falls geladen und vorhanden. */
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
