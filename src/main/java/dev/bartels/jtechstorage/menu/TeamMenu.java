package dev.bartels.jtechstorage.menu;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import dev.bartels.jtechstorage.ModAdvancements;
import dev.bartels.jtechstorage.ModContent;
import dev.bartels.jtechstorage.block.LinkedPanelBlock;
import dev.bartels.jtechstorage.block.LinkedPanelBlockEntity;
import dev.bartels.jtechstorage.block.StorageTerminalBlockEntity;
import dev.bartels.jtechstorage.block.StorageTerminalBlockEntity.Channel;
import dev.bartels.jtechstorage.block.StorageTerminalBlockEntity.Permission;
import dev.bartels.jtechstorage.block.StorageTerminalBlockEntity.Role;
import dev.bartels.jtechstorage.net.TeamActionPayload;
import dev.bartels.jtechstorage.net.TeamStatePayload;

/**
 * Team-Terminal: Mitglieder und Rollen, Team-Name und -Farbe, Zugriff und Team-Kanal.
 *
 * <p>Regeln: Verwalter dürfen Mitglieder bis zur Rolle Baumeister hinzufügen, befördern und entfernen sowie Name,
 * Farbe und Kanal ändern. Nur der Besitzer ernennt Verwalter und stellt den Zugriffsmodus um.
 */
public class TeamMenu extends AbstractContainerMenu {
	public record OpenData(BlockPos pos) {
		public static final StreamCodec<io.netty.buffer.ByteBuf, OpenData> STREAM_CODEC =
				BlockPos.STREAM_CODEC.map(OpenData::new, OpenData::pos);
	}

	private static final int REFRESH_INTERVAL = 10;

	private final Player player;
	private final @Nullable LinkedPanelBlockEntity panel;
	private TeamStatePayload.@Nullable State lastSent;
	private int ticks = REFRESH_INTERVAL;

	private TeamStatePayload.State clientState = TeamStatePayload.State.EMPTY;
	private int clientVersion;

	public TeamMenu(int containerId, Inventory inventory, OpenData data) {
		this(containerId, inventory, (LinkedPanelBlockEntity) null);
	}

	public TeamMenu(int containerId, Inventory inventory, @Nullable LinkedPanelBlockEntity panel) {
		super(ModContent.TEAM_MENU, containerId);
		this.player = inventory.player;
		this.panel = panel;
	}

	private @Nullable StorageTerminalBlockEntity terminal() {
		return panel != null ? panel.terminal() : null;
	}

	// ------------------------------------------------------------------
	// Server
	// ------------------------------------------------------------------

	@Override
	public void broadcastChanges() {
		super.broadcastChanges();

		if (panel != null && player instanceof ServerPlayer serverPlayer && ++ticks >= REFRESH_INTERVAL) {
			ticks = 0;
			sync(serverPlayer, false);
		}
	}

	private void sync(ServerPlayer serverPlayer, boolean force) {
		StorageTerminalBlockEntity terminal = terminal();

		if (terminal == null) {
			return;
		}

		var playerList = serverPlayer.level().getServer().getPlayerList();
		List<TeamStatePayload.MemberRow> rows = new ArrayList<>();

		for (Map.Entry<UUID, StorageTerminalBlockEntity.Member> entry : terminal.getMembers().entrySet()) {
			rows.add(new TeamStatePayload.MemberRow(entry.getValue().name(), entry.getValue().role().ordinal(),
					playerList.getPlayer(entry.getKey()) != null));
		}

		Role role = terminal.roleOf(serverPlayer);
		int colorIndex = 0;

		for (int i = 0; i < StorageTerminalBlockEntity.TEAM_COLORS.length; i++) {
			if (StorageTerminalBlockEntity.TEAM_COLORS[i] == terminal.getTeamColor()) {
				colorIndex = i;
			}
		}

		UUID ownerId = terminal.getOwnerId();
		TeamStatePayload.State state = new TeamStatePayload.State(role != null ? role.ordinal() : 0, terminal.getOwnerName(),
				ownerId != null && playerList.getPlayer(ownerId) != null, terminal.getNetworkName(), colorIndex,
				terminal.getTeamRgb(), terminal.getAccessMode().ordinal(), terminal.getPublicRole().ordinal(),
				terminal.channelMask(), rows);

		if (force || !state.equals(lastSent)) {
			lastSent = state;
			ServerPlayNetworking.send(serverPlayer, new TeamStatePayload(containerId, state));
		}
	}

	public void handleAction(ServerPlayer player, TeamActionPayload.Action action, String text, int value) {
		StorageTerminalBlockEntity terminal = terminal();

		if (terminal == null) {
			return;
		}

		Role myRole = terminal.roleOf(player);
		boolean owner = myRole == Role.OWNER;

		switch (action) {
			case ADD -> {
				if (!terminal.can(player, Permission.TEAM)) {
					return;
				}

				ServerPlayer target = player.level().getServer().getPlayerList().getPlayerByName(text.trim());

				if (target == null) {
					player.sendOverlayMessage(Component.translatable("jtechstorage.message.friend_offline", text.trim()));
					return;
				}

				switch (terminal.addMember(target, Role.MEMBER)) {
					case ADDED -> {
						player.sendOverlayMessage(Component.translatable("jtechstorage.message.friend_added", target.getPlainTextName()));
						target.sendSystemMessage(Component.translatable("jtechstorage.message.team_added_you",
								player.getPlainTextName(), terminal.teamLabel()));
						terminal.broadcast(Channel.TEAM, Component.translatable("jtechstorage.channel.msg.joined",
								target.getPlainTextName(), player.getPlainTextName()));
						ModAdvancements.grant(player, "friend");
					}
					case ALREADY -> player.sendOverlayMessage(Component.translatable("jtechstorage.message.friend_already", target.getPlainTextName()));
					default -> { }
				}
			}
			case REMOVE -> {
				UUID id = terminal.findMember(text);
				StorageTerminalBlockEntity.Member member = id != null ? terminal.getMembers().get(id) : null;

				if (member == null || !mayManage(myRole, member.role())) {
					return;
				}

				terminal.removeMember(id);
				terminal.broadcast(Channel.TEAM, Component.translatable("jtechstorage.channel.msg.removed",
						member.name(), player.getPlainTextName()));
				player.sendOverlayMessage(Component.translatable("jtechstorage.message.friend_removed", member.name()));
			}
			case SET_ROLE -> {
				UUID id = terminal.findMember(text);
				StorageTerminalBlockEntity.Member member = id != null ? terminal.getMembers().get(id) : null;
				Role newRole = Role.byId(value);

				if (member == null || newRole == Role.OWNER || !mayManage(myRole, member.role()) || !mayManage(myRole, newRole)
						|| member.role() == newRole) {
					return;
				}

				terminal.setMemberRole(id, newRole);
				terminal.broadcast(Channel.TEAM, Component.translatable("jtechstorage.channel.msg.role",
						member.name(), newRole.displayName(), player.getPlainTextName()));
			}
			case RENAME -> {
				if (terminal.can(player, Permission.SETTINGS)) {
					terminal.setNetworkName(text);
					String result = terminal.getNetworkName();
					player.sendOverlayMessage(result.isEmpty()
							? Component.translatable("jtechstorage.message.name_cleared")
							: Component.translatable("jtechstorage.message.name_set", result));
				}
			}
			case COLOR -> {
				if (terminal.can(player, Permission.SETTINGS)) {
					ChatFormattingHolder.set(terminal, value);
				}
			}
			case ACCESS -> {
				if (owner) {
					StorageTerminalBlockEntity.AccessMode[] modes = StorageTerminalBlockEntity.AccessMode.values();
					StorageTerminalBlockEntity.AccessMode mode = modes[Math.floorMod(value, modes.length)];
					terminal.setAccessMode(mode);
					player.sendOverlayMessage(Component.translatable("jtechstorage.message.access_mode",
							Component.translatable(mode.translationKey())));
				}
			}
			case PUBLIC_ROLE -> {
				if (owner) {
					terminal.setPublicRole(Role.byId(Math.floorMod(value, Role.MANAGER.ordinal())));
				}
			}
			case CHANNEL -> {
				if (terminal.can(player, Permission.SETTINGS) && value >= 0 && value < Channel.values().length) {
					terminal.toggleChannel(Channel.values()[value]);
				}
			}
		}

		player.level().playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.4f, 1.6f);
		sync(player, true);
	}

	/** Darf {@code me} ein Mitglied mit Rolle {@code other} verwalten bzw. diese Rolle vergeben? */
	public static boolean mayManage(@Nullable Role me, Role other) {
		if (me == Role.OWNER) {
			return other != Role.OWNER;
		}

		return me == Role.MANAGER && other.ordinal() < Role.MANAGER.ordinal();
	}

	/** Kleine Hilfe, damit die Farbwahl gegen ungültige Indizes abgesichert ist. */
	private static final class ChatFormattingHolder {
		static void set(StorageTerminalBlockEntity terminal, int index) {
			var colors = StorageTerminalBlockEntity.TEAM_COLORS;
			terminal.setTeamColor(colors[Math.floorMod(index, colors.length)]);
		}
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		return ItemStack.EMPTY;
	}

	@Override
	public boolean stillValid(Player player) {
		if (panel == null) {
			return true;
		}

		StorageTerminalBlockEntity terminal = terminal();
		return LinkedPanelBlock.inReach(panel, player) && terminal != null && terminal.canAccess(player);
	}

	// ------------------------------------------------------------------
	// Client
	// ------------------------------------------------------------------

	public void setClientState(TeamStatePayload.State state) {
		clientState = state;
		clientVersion++;
	}

	public TeamStatePayload.State getClientState() {
		return clientState;
	}

	public int getClientVersion() {
		return clientVersion;
	}
}
