package dev.bartels.jtechstorage.block;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;

import dev.bartels.jtechstorage.menu.TradeMenu;

/**
 * Trade terminal: a little shop for the network. Anyone may buy; managers and the owner of the linked network
 * may edit offers.
 */
public class TradeTerminalBlock extends LinkedPanelBlock {
	public TradeTerminalBlock(Properties properties) {
		super(properties);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new TradeTerminalBlockEntity(pos, state);
	}

	@Override
	protected StorageTerminalBlockEntity.Permission linkPermission() {
		return StorageTerminalBlockEntity.Permission.TRADE;
	}

	@Override
	protected void open(ServerPlayer player, LinkedPanelBlockEntity panel, StorageTerminalBlockEntity terminal) {
		if (!(panel instanceof TradeTerminalBlockEntity trade)) {
			return;
		}

		boolean editable = terminal.can(player, StorageTerminalBlockEntity.Permission.TRADE);
		String shop = terminal.teamLabel().getString();
		int color = terminal.getTeamRgb();

		player.openMenu(new ExtendedMenuProvider<TradeMenu.OpenData>() {
			@Override
			public TradeMenu.OpenData getScreenOpeningData(ServerPlayer serverPlayer) {
				return new TradeMenu.OpenData(panel.getBlockPos(), editable, shop, color);
			}

			@Override
			public Component getDisplayName() {
				return getName();
			}

			@Override
			public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player menuPlayer) {
				return new TradeMenu(containerId, inventory, trade, editable);
			}
		});
	}
}
