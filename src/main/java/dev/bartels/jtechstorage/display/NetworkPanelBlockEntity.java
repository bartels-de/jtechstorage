package dev.bartels.jtechstorage.display;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import dev.bartels.jtechstorage.block.LinkedPanelBlockEntity;
import dev.bartels.jtechstorage.block.StorageTerminalBlockEntity;
import dev.bartels.jtechstorage.network.StorageNetwork;

/**
 * Gemeinsame Basis für Lager-Bildschirm und Lager-Banner (v0.34): einmal pro Sekunde fragt der Server das verknüpfte
 * Netzwerk ab und schickt die Werte an die Clients, die sie weich animiert anzeigen.
 */
public abstract class NetworkPanelBlockEntity extends LinkedPanelBlockEntity {
	/** Werte im Abstand von 10 Sekunden für Verlauf und Trend (60 = 10 Minuten). */
	public static final int HISTORY = 60;
	protected boolean online;
	/** Name des Netzwerks (vom Terminal), für Überschriften. */
	protected String networkName = "";
	private int ticks;

	protected NetworkPanelBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	public boolean isOnline() {
		return online;
	}

	public String getNetworkName() {
		return networkName;
	}

	public static void serverTick(Level level, BlockPos pos, BlockState state, NetworkPanelBlockEntity panel) {
		if (++panel.ticks % 20 != 0) {
			return;
		}

		StorageTerminalBlockEntity terminal = panel.terminal();
		boolean changed;

		if (terminal == null) {
			changed = panel.online;
			panel.online = false;
		} else {
			changed = !panel.online || !panel.networkName.equals(terminal.getNetworkName());
			panel.online = true;
			panel.networkName = terminal.getNetworkName();
			changed |= panel.refresh(terminal.buildNetwork(), panel.ticks % 200 == 0);
		}

		if (changed) {
			panel.setChanged();
			level.sendBlockUpdated(pos, state, state, 3);
		}
	}

	/** Neue Werte aus dem Netzwerk; {@code sample} = alle 10 Sekunden einen Verlaufspunkt anhängen. */
	protected abstract boolean refresh(StorageNetwork network, boolean sample);

	protected static List<StorageNetwork.Entry> top(StorageNetwork network, int n) {
		List<StorageNetwork.Entry> all = new ArrayList<>(network.aggregate());
		all.sort((a, b) -> Long.compare(b.total(), a.total()));
		return new ArrayList<>(all.subList(0, Math.min(n, all.size())));
	}

	protected static ItemStack prototype(ItemStack stack) {
		return stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1);
	}

	/** Trend pro Minute aus dem Verlauf (Vergleich mit dem Wert vor 60 Sekunden). */
	protected static int trend(List<Long> history, long now) {
		if (history.isEmpty()) {
			return 0;
		}

		long before = history.get(Math.max(0, history.size() - 6));
		return (int) Math.max(Integer.MIN_VALUE, Math.min(Integer.MAX_VALUE, now - before));
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putString("NetworkName", networkName);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		networkName = input.getStringOr("NetworkName", "");
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
