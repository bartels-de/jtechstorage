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

import dev.bartels.jtechstorage.menu.TeamMenu;

/**
 * Team terminal: manage members, roles, team name, team color, access and team channel of a network.
 * Anyone with access can view the list; managers and the owner may change it.
 */
public class TeamTerminalBlock extends LinkedPanelBlock {
	public TeamTerminalBlock(Properties properties) {
		super(properties);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new TeamTerminalBlockEntity(pos, state);
	}

	@Override
	protected StorageTerminalBlockEntity.Permission linkPermission() {
		return StorageTerminalBlockEntity.Permission.SETTINGS;
	}

	@Override
	protected void open(ServerPlayer player, LinkedPanelBlockEntity panel, StorageTerminalBlockEntity terminal) {
		if (!terminal.canAccess(player)) {
			player.sendOverlayMessage(Component.translatable("jtechstorage.message.not_owner", terminal.getOwnerName()));
			return;
		}

		player.openMenu(new ExtendedMenuProvider<TeamMenu.OpenData>() {
			@Override
			public TeamMenu.OpenData getScreenOpeningData(ServerPlayer serverPlayer) {
				return new TeamMenu.OpenData(panel.getBlockPos());
			}

			@Override
			public Component getDisplayName() {
				return getName();
			}

			@Override
			public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player menuPlayer) {
				return new TeamMenu(containerId, inventory, panel);
			}
		});
	}
}
