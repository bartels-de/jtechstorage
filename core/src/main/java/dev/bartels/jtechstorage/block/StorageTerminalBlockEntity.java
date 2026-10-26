package dev.bartels.jtechstorage.block;

import dev.bartels.jtechstorage.api.JtsHooks;
import dev.bartels.jtechstorage.network.NetworkStats;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.mojang.serialization.Codec;

import net.minecraft.network.chat.TextColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;

import dev.bartels.jtechstorage.ModConfig;
import dev.bartels.jtechstorage.ModContent;
import dev.bartels.jtechstorage.entity.SortAssignments;
import dev.bartels.jtechstorage.entity.SortCategory;
import dev.bartels.jtechstorage.item.UpgradeItem;
import dev.bartels.jtechstorage.menu.StorageTerminalMenu;
import dev.bartels.jtechstorage.network.StorageNetwork;

/**
 * Heart of the network: stores owner, linked chests and upgrades.
 */
public class StorageTerminalBlockEntity extends BlockEntity implements ExtendedMenuProvider<StorageTerminalMenu.OpenData>, SortAssignments {
	public static final int RANGE_SLOT = 0;
	public static final int CAPACITY_SLOT = 1;

	private final SimpleContainer upgrades = new SimpleContainer(2) {
		@Override
		public void setChanged() {
			super.setChanged();
			StorageTerminalBlockEntity.this.setChanged();
		}

		@Override
		public boolean stillValid(Player player) {
			return Container.stillValidBlockEntity(StorageTerminalBlockEntity.this, player);
		}
	};

	private final List<BlockPos> links = new ArrayList<>();
	private @Nullable UUID owner;
	private String ownerName = "";
	/** Team members: UUID -> name + role (order of adding). */
	private final Map<UUID, Member> members = new LinkedHashMap<>();
	/** Who besides the owner may use the network? */
	private AccessMode accessMode = AccessMode.FRIENDS;
	/** Role for strangers when the network is public. */
	private Role publicRole = Role.GUEST;
	/** Team color (helpers' name tags, team channel). */
	private ChatFormatting teamColor = ChatFormatting.LIGHT_PURPLE;
	/** Enabled channel messages. */
	private final EnumSet<Channel> channels = EnumSet.allOf(Channel.class);
	/** Has a warning about nearly full storage already been sent? */
	private boolean warnedFull;
	/** Freely chosen network name - helper mobs carry it as a prefix in their name tag. */
	private String networkName = "";
	public static final int MAX_NAME_LENGTH = 20;
	/** Fixed category per linked chest (assigned by the Amethyst Imp). */
	private final Map<BlockPos, SortCategory> assignments = new LinkedHashMap<>();

	/** A team member. */
	public record Member(String name, Role role) {
	}

	/** Roles in the team - each tier may do everything the one below may. */
	public enum Role {
		/** Sees the contents but may not take or store anything. */
		GUEST(ChatFormatting.GRAY),
		/** Take and store items, craft, wireless terminal. */
		MEMBER(ChatFormatting.GREEN),
		/** + Link chests, link and operate helpers. */
		BUILDER(ChatFormatting.AQUA),
		/** + Upgrades, team settings, manage members up to Builder, trade terminal. */
		MANAGER(ChatFormatting.GOLD),
		/** Owner: everything. */
		OWNER(ChatFormatting.LIGHT_PURPLE);

		public final ChatFormatting color;

		Role(ChatFormatting color) {
			this.color = color;
		}

		public String translationKey() {
			return "jtechstorage.role." + name().toLowerCase(java.util.Locale.ROOT);
		}

		public Component displayName() {
			return Component.translatable(translationKey()).withStyle(color);
		}

		public boolean atLeast(Role other) {
			return ordinal() >= other.ordinal();
		}

		public static Role byId(int id) {
			Role[] values = values();
			return values[Math.clamp(id, 0, values.length - 1)];
		}
	}

	/** The minimum an action requires. */
	public enum Permission {
		VIEW(Role.GUEST), ITEMS(Role.MEMBER), LINK(Role.BUILDER), HELPERS(Role.BUILDER),
		UPGRADES(Role.MANAGER), SETTINGS(Role.MANAGER), TEAM(Role.MANAGER), TRADE(Role.MANAGER);

		public final Role minimum;

		Permission(Role minimum) {
			this.minimum = minimum;
		}
	}

	/** Team channel: event messages to all members who are online. */
	public enum Channel {
		TEAM, TRADE, GUARD, STORAGE, MAIL;

		public String translationKey() {
			return "jtechstorage.channel." + name().toLowerCase(java.util.Locale.ROOT);
		}
	}

	/** Colors selectable as team color. */
	public static final ChatFormatting[] TEAM_COLORS = {
			ChatFormatting.LIGHT_PURPLE, ChatFormatting.DARK_PURPLE, ChatFormatting.BLUE, ChatFormatting.AQUA,
			ChatFormatting.DARK_AQUA, ChatFormatting.GREEN, ChatFormatting.DARK_GREEN, ChatFormatting.YELLOW,
			ChatFormatting.GOLD, ChatFormatting.RED, ChatFormatting.DARK_RED, ChatFormatting.WHITE, ChatFormatting.GRAY};

	public enum AccessMode {
		/** Owner only. */
		PRIVATE,
		/** Owner + team members (name kept for old worlds). */
		FRIENDS,
		/** Everyone (strangers get the role {@link #getPublicRole()}). */
		PUBLIC;

		public String translationKey() {
			return "jtechstorage.access." + name().toLowerCase(java.util.Locale.ROOT);
		}

		public AccessMode next() {
			return values()[(ordinal() + 1) % values().length];
		}
	}

	public StorageTerminalBlockEntity(BlockPos pos, BlockState state) {
		super(ModContent.STORAGE_TERMINAL_BLOCK_ENTITY, pos, state);
	}

	// ------------------------------------------------------------------
	// Besitzer
	// Owner

	public void setOwner(Player player) {
		owner = player.getUUID();
		ownerName = player.getPlainTextName();
		setChanged();
	}

	/** May this player use the terminal (at least view)? A terminal without an owner is taken over by the first user. */
	public boolean canAccess(Player player) {
		if (owner == null) {
			setOwner(player);
			return true;
		}

		return roleOf(player) != null;
	}

	/** Role of a player in this network or {@code null} (no access). */
	public @Nullable Role roleOf(Player player) {
		return roleOf(player.getUUID());
	}

	public @Nullable Role roleOf(UUID id) {
		if (owner == null || owner.equals(id)) {
			return Role.OWNER;
		}

		Member member = members.get(id);

		return switch (accessMode) {
			case PRIVATE -> null;
			case FRIENDS -> member != null ? member.role() : null;
			case PUBLIC -> member != null && member.role().atLeast(publicRole) ? member.role() : publicRole;
		};
	}

	/** May the player do this? */
	public boolean can(Player player, Permission permission) {
		Role role = roleOf(player);
		return role != null && role.atLeast(permission.minimum);
	}

	public Role getPublicRole() {
		return publicRole;
	}

	public void setPublicRole(Role role) {
		publicRole = role.atLeast(Role.MANAGER) ? Role.BUILDER : role;
		setChanged();
	}

	public ChatFormatting getTeamColor() {
		return teamColor;
	}

	/** Team color as RGB. */
	public int getTeamRgb() {
		return TextColor.fromLegacyFormat(teamColor).getValue();
	}

	public void setTeamColor(ChatFormatting color) {
		teamColor = color;
		setChanged();
	}

	public boolean isChannelEnabled(Channel channel) {
		return channels.contains(channel);
	}

	public void toggleChannel(Channel channel) {
		if (!channels.remove(channel)) {
			channels.add(channel);
		}

		setChanged();
	}

	/** Bitmask of active channels (for the interface). */
	public int channelMask() {
		int mask = 0;

		for (Channel channel : channels) {
			mask |= 1 << channel.ordinal();
		}

		return mask;
	}

	/** Name the team goes by: network name or "Team of ...". */
	public Component teamLabel() {
		return networkName.isEmpty()
				? Component.translatable("jtechstorage.team.default_name", ownerName)
				: Component.literal(networkName);
	}

	/**
	 * Sends a message to all team members (incl. owner) who are currently online - if the channel is active.
	 */
	public void broadcast(Channel channel, Component message) {
		if (!channels.contains(channel) || !(level instanceof ServerLevel serverLevel)) {
			return;
		}

		Component line = Component.empty()
				.append(Component.literal("[").withStyle(ChatFormatting.DARK_GRAY))
				.append(teamLabel().copy().withStyle(teamColor))
				.append(Component.literal("] ").withStyle(ChatFormatting.DARK_GRAY))
				.append(message.copy().withStyle(ChatFormatting.GRAY));
		var playerList = serverLevel.getServer().getPlayerList();
		Set<UUID> sent = new HashSet<>();

		if (owner != null && playerList.getPlayer(owner) instanceof ServerPlayer player && sent.add(owner)) {
			player.sendSystemMessage(line);
		}

		for (UUID id : members.keySet()) {
			if (playerList.getPlayer(id) instanceof ServerPlayer player && sent.add(id)) {
				player.sendSystemMessage(line);
			}
		}
	}

	/** Is any team member online? */
	private boolean anyMemberOnline(ServerLevel level) {
		var playerList = level.getServer().getPlayerList();

		if (owner != null && playerList.getPlayer(owner) != null) {
			return true;
		}

		for (UUID id : members.keySet()) {
			if (playerList.getPlayer(id) != null) {
				return true;
			}
		}

		return false;
	}

	public AccessMode getAccessMode() {
		return accessMode;
	}

	public void setAccessMode(AccessMode mode) {
		accessMode = mode;
		setChanged();
	}

	public String getNetworkName() {
		return networkName;
	}

	public void setNetworkName(String name) {
		String cleaned = name.replaceAll("[\\p{Cntrl}§]", "").trim();
		networkName = cleaned.length() > MAX_NAME_LENGTH ? cleaned.substring(0, MAX_NAME_LENGTH) : cleaned;
		setChanged();
	}

	// ------------------------------------------------------------------
	// Sort assignments (SortAssignments)
	// ------------------------------------------------------------------

	@Override
	public @Nullable SortCategory get(BlockPos key) {
		return assignments.get(key);
	}

	@Override
	public void assign(BlockPos key, SortCategory category) {
		if (assignments.put(key.immutable(), category) != category) {
			setChanged();
		}
	}

	@Override
	public void release(BlockPos key) {
		if (assignments.remove(key) != null) {
			setChanged();
		}
	}

	@Override
	public Map<BlockPos, SortCategory> asMap() {
		return assignments;
	}

	public boolean isOwner(Player player) {
		return owner != null && owner.equals(player.getUUID());
	}

	/** Names of the members (in order of adding). */
	public List<String> getMemberNames() {
		return members.values().stream().map(Member::name).toList();
	}

	/** All members (UUID -> name + role), unmodifiable. */
	public Map<UUID, Member> getMembers() {
		return java.util.Collections.unmodifiableMap(members);
	}

	public @Nullable UUID getOwnerId() {
		return owner;
	}

	public enum MemberResult {
		ADDED, ALREADY, IS_OWNER, NOT_FOUND, NOT_ALLOWED
	}

	public MemberResult addMember(Player player, Role role) {
		if (isOwner(player)) {
			return MemberResult.IS_OWNER;
		}

		if (members.containsKey(player.getUUID())) {
			return MemberResult.ALREADY;
		}

		members.put(player.getUUID(), new Member(player.getPlainTextName(), role.atLeast(Role.OWNER) ? Role.MANAGER : role));
		setChanged();
		return MemberResult.ADDED;
	}

	/** Look up a member by name (case-insensitive). */
	public @Nullable UUID findMember(String name) {
		for (Map.Entry<UUID, Member> entry : members.entrySet()) {
			if (entry.getValue().name().equalsIgnoreCase(name)) {
				return entry.getKey();
			}
		}

		return null;
	}

	public boolean removeMember(UUID id) {
		boolean removed = members.remove(id) != null;

		if (removed) {
			setChanged();
		}

		return removed;
	}

	public void setMemberRole(UUID id, Role role) {
		Member member = members.get(id);

		if (member != null && role != Role.OWNER) {
			members.put(id, new Member(member.name(), role));
			setChanged();
		}
	}

	/** Updates the stored name when a member has renamed. */
	public void refreshName(Player player) {
		Member member = members.get(player.getUUID());

		if (member != null && !member.name().equals(player.getPlainTextName())) {
			members.put(player.getUUID(), new Member(player.getPlainTextName(), member.role()));
			setChanged();
		}
	}

	public String getOwnerName() {
		return ownerName;
	}

	// ------------------------------------------------------------------
	// Upgrades & metrics
	// ------------------------------------------------------------------

	public SimpleContainer getUpgrades() {
		return upgrades;
	}

	public int getRange() {
		int range = UpgradeItem.valueOf(upgrades.getItem(RANGE_SLOT), UpgradeItem.Kind.RANGE, ModContent.BASE_RANGE);
		range = isCharged() ? range + RESONANCE_RANGE_BONUS : range;
		// Resonance storm: the network is disturbed, range halved
		return isDisturbed() ? Math.max(4, range / 2) : range;
	}

	/** Is a resonance storm currently disturbing this network? (only known on the server) */
	public boolean isDisturbed() {
		return JtsHooks.stormDisturbed.test(level, worldPosition);
	}

	// ------------------------------------------------------------------
	// Resonance ("power" as a bonus - the network also works without it)
	// ------------------------------------------------------------------

	public static final int MAX_RESONANCE = 1000;
	public static final int RESONANCE_RANGE_BONUS = ModConfig.get().network.resonanceRangeBonus;
	public static final float RESONANCE_HELPER_SPEED = (float) ModConfig.get().network.resonanceHelperSpeed;

	private int resonance;

	public int getResonance() {
		return resonance;
	}

	/** Charged with resonance? Then the bonuses apply (range, wireless, helper speed). */
	public boolean isCharged() {
		return resonance > 0;
	}

	/** Speed factor for this network's helper mobs. */
	public float helperSpeedFactor() {
		float factor = isCharged() ? RESONANCE_HELPER_SPEED : 1.0f;
		return isDisturbed() ? factor * 0.5f : factor;   // resonance storm: helpers work at half speed
	}

	/** Adds resonance (e.g. from the Lightning Beetle). @return true if the storage is now full. */
	public boolean addResonance(int amount) {
		int before = resonance;
		resonance = Math.min(MAX_RESONANCE, resonance + amount);

		if (resonance != before) {
			setChanged();
		}

		return resonance >= MAX_RESONANCE;
	}

	/** Storage statistics (snapshot per game day). */
	private final NetworkStats stats = new NetworkStats();

	public NetworkStats getStats() {
		return stats;
	}

	/** Called every tick on the server: resonance slowly discharges (1 per second). */
	public static void serverTick(Level level, BlockPos pos, BlockState state,
			StorageTerminalBlockEntity terminal) {
		// statistics: a snapshot on each new game day (checked every 10 seconds)
		if (level.getGameTime() % 200 == 11) {
			long day = Math.floorDiv(level.getDefaultClockTime(), 24000L);

			if (terminal.stats.needsSnapshot(day)) {
				terminal.stats.snapshot(day, terminal.buildNetwork());
				terminal.setChanged();
			}
		}

		// resonance discharges (1 per second) - during a storm the crystal beings drain 5 per second
		if (terminal.resonance > 0 && level.getGameTime() % 20 == 0) {
			terminal.resonance = Math.max(0, terminal.resonance - (terminal.isDisturbed() ? 5 : 1));
			terminal.setChanged();
		}

		// team channel "storage": warning when storage is nearly full (checked every 30 seconds)
		if (level.getGameTime() % 600 == 17 && level instanceof ServerLevel serverLevel
				&& terminal.isChannelEnabled(Channel.STORAGE) && terminal.anyMemberOnline(serverLevel)) {
			StorageNetwork network = terminal.buildNetwork();
			int total = network.totalSlots();

			if (total > 0) {
				int percent = network.usedSlots() * 100 / total;

				if (percent >= 90 && !terminal.warnedFull) {
					terminal.warnedFull = true;
					terminal.broadcast(Channel.STORAGE, Component.translatable(percent >= 100
							? "jtechstorage.channel.msg.storage_full" : "jtechstorage.channel.msg.storage_almost_full", percent));
				} else if (percent < 80) {
					terminal.warnedFull = false;
				}
			}
		}
	}

	public int getCapacity() {
		return UpgradeItem.valueOf(upgrades.getItem(CAPACITY_SLOT), UpgradeItem.Kind.CAPACITY, ModContent.BASE_CAPACITY);
	}

	/** Ender upgrade inserted? Then the wireless terminal works unlimited and across dimensions. */
	public boolean hasEnderUpgrade() {
		return UpgradeItem.isEnder(upgrades.getItem(RANGE_SLOT));
	}

	/**
	 * Keeps terminal and chest chunks loaded for about 15 seconds (renewed regularly by the open
	 * wireless menu, expires on its own afterwards).
	 */
	public void keepNetworkLoaded() {
		if (!(level instanceof ServerLevel serverLevel)) {
			return;
		}

		Set<ChunkPos> chunks = new HashSet<>();
		chunks.add(ChunkPos.containing(worldPosition));

		for (BlockPos link : links) {
			chunks.add(ChunkPos.containing(link));
		}

		for (ChunkPos chunk : chunks) {
			serverLevel.getChunkSource().addTicketWithRadius(TicketType.PORTAL, chunk, 1);
		}
	}

	public int getWirelessRange() {
		int range = getRange() * ModContent.WIRELESS_RANGE_FACTOR;
		return isCharged() ? range * 2 : range;
	}

	public boolean isCrafting() {
		return getBlockState().getBlock() instanceof StorageTerminalBlock block && block.isCrafting();
	}

	// ------------------------------------------------------------------
	// Linked chests
	// ------------------------------------------------------------------

	public List<BlockPos> getLinks() {
		return List.copyOf(links);
	}

	public enum LinkResult {
		ADDED, REMOVED, FULL, OUT_OF_RANGE
	}

	/** Adds or removes a chest (double chests count as one chest). */
	public LinkResult toggleLink(BlockPos pos) {
		BlockPos partner = level == null ? null : StorageNetwork.chestPartner(level.getBlockState(pos), pos);

		if (links.remove(pos) || (partner != null && links.remove(partner))) {
			assignments.remove(pos);

			if (partner != null) {
				assignments.remove(partner);
			}

			setChanged();
			return LinkResult.REMOVED;
		}

		if (links.size() >= getCapacity()) {
			return LinkResult.FULL;
		}

		double range = getRange();

		if (pos.distSqr(worldPosition) > range * range) {
			return LinkResult.OUT_OF_RANGE;
		}

		links.add(pos.immutable());
		setChanged();
		return LinkResult.ADDED;
	}

	/** Current network; removes links to broken chests along the way. */
	public StorageNetwork buildNetwork() {
		if (level == null) {
			return StorageNetwork.EMPTY;
		}

		List<BlockPos> invalid = new ArrayList<>();
		StorageNetwork network = StorageNetwork.collect(level, worldPosition, links, getRange(), getCapacity(),
				assignments, invalid::add);

		if (!invalid.isEmpty()) {
			links.removeAll(invalid);
			invalid.forEach(assignments::remove);
			setChanged();
		}

		return network;
	}

	// ------------------------------------------------------------------
	// Saving
	// ------------------------------------------------------------------

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		storeItem(output, "Upgrade", upgrades.getItem(RANGE_SLOT));
		storeItem(output, "CapacityUpgrade", upgrades.getItem(CAPACITY_SLOT));
		output.store("Links", BlockPos.CODEC.listOf(), links);

		if (owner != null) {
			output.store("Owner", UUIDUtil.CODEC, owner);
			output.putString("OwnerName", ownerName);
		}

		if (!members.isEmpty()) {
			output.store("MemberIds", UUIDUtil.CODEC.listOf(), List.copyOf(members.keySet()));
			output.store("MemberNames", Codec.STRING.listOf(), members.values().stream().map(Member::name).toList());
			output.store("MemberRoles", Codec.STRING.listOf(), members.values().stream().map(m -> m.role().name()).toList());
		}

		output.putString("AccessMode", accessMode.name());
		output.putString("PublicRole", publicRole.name());
		output.putString("TeamColor", teamColor.name());
		output.putInt("Channels", channelMask());
		output.putInt("ChannelVersion", 2);
		output.putString("NetworkName", networkName);
		output.putInt("Resonance", resonance);
		stats.save(output);

		if (!assignments.isEmpty()) {
			output.store("SortKeys", BlockPos.CODEC.listOf(), List.copyOf(assignments.keySet()));
			output.store("SortCategories", Codec.STRING.listOf(), assignments.values().stream().map(Enum::name).toList());
		}
	}

	private static void storeItem(ValueOutput output, String key, ItemStack stack) {
		if (!stack.isEmpty()) {
			output.store(key, ItemStack.CODEC, stack);
		}
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		upgrades.setItem(RANGE_SLOT, input.read("Upgrade", ItemStack.CODEC).orElse(ItemStack.EMPTY));
		upgrades.setItem(CAPACITY_SLOT, input.read("CapacityUpgrade", ItemStack.CODEC).orElse(ItemStack.EMPTY));
		links.clear();
		links.addAll(input.read("Links", BlockPos.CODEC.listOf()).orElse(List.of()));
		stats.load(input);
		owner = input.read("Owner", UUIDUtil.CODEC).orElse(null);
		ownerName = input.getStringOr("OwnerName", "");

		members.clear();
		List<UUID> memberIds = input.read("MemberIds", UUIDUtil.CODEC.listOf()).orElse(List.of());
		List<String> memberNames = input.read("MemberNames", Codec.STRING.listOf()).orElse(List.of());
		List<String> memberRoles = input.read("MemberRoles", Codec.STRING.listOf()).orElse(List.of());

		for (int i = 0; i < memberIds.size(); i++) {
			Role role = Role.MEMBER;

			try {
				role = Role.valueOf(i < memberRoles.size() ? memberRoles.get(i) : "MEMBER");
			} catch (IllegalArgumentException ignored) {
				// unknown role - member
			}

			members.put(memberIds.get(i), new Member(i < memberNames.size() ? memberNames.get(i) : "?", role == Role.OWNER ? Role.MANAGER : role));
		}

		// Old worlds (up to v0.14): friends become builders
		List<UUID> friendIds = input.read("FriendIds", UUIDUtil.CODEC.listOf()).orElse(List.of());
		List<String> friendNames = input.read("FriendNames", Codec.STRING.listOf()).orElse(List.of());

		for (int i = 0; i < friendIds.size(); i++) {
			members.putIfAbsent(friendIds.get(i), new Member(i < friendNames.size() ? friendNames.get(i) : "?", Role.BUILDER));
		}

		boolean oldWorld = input.getStringOr("PublicRole", "").isEmpty();

		try {
			publicRole = Role.valueOf(input.getStringOr("PublicRole", Role.GUEST.name()));
		} catch (IllegalArgumentException e) {
			publicRole = Role.GUEST;
		}

		teamColor = ChatFormatting.LIGHT_PURPLE;

		for (ChatFormatting color : TEAM_COLORS) {
			if (color.name().equals(input.getStringOr("TeamColor", ""))) {
				teamColor = color;
			}
		}
		int mask = input.getIntOr("Channels", -1);
		channels.clear();

		for (Channel channel : Channel.values()) {
			if ((mask & (1 << channel.ordinal())) != 0) {
				channels.add(channel);
			}
		}

		if (input.getIntOr("ChannelVersion", 1) < 2) {
			channels.add(Channel.MAIL);   // new channel (v0.16) is on for old worlds
		}

		resonance = Math.clamp(input.getIntOr("Resonance", 0), 0, MAX_RESONANCE);
		networkName = input.getStringOr("NetworkName", "");

		try {
			accessMode = AccessMode.valueOf(input.getStringOr("AccessMode", AccessMode.FRIENDS.name()));
		} catch (IllegalArgumentException e) {
			accessMode = AccessMode.FRIENDS;
		}

		if (oldWorld && accessMode == AccessMode.PUBLIC) {
			publicRole = Role.BUILDER;   // previously with "public" everyone could do almost everything
		}

		assignments.clear();
		List<BlockPos> sortKeys = input.read("SortKeys", BlockPos.CODEC.listOf()).orElse(List.of());
		List<String> sortCategories = input.read("SortCategories", Codec.STRING.listOf()).orElse(List.of());

		for (int i = 0; i < sortKeys.size() && i < sortCategories.size(); i++) {
			try {
				assignments.put(sortKeys.get(i), SortCategory.valueOf(sortCategories.get(i)));
			} catch (IllegalArgumentException ignored) {
				// unknown category from another version - skip
			}
		}
	}

	@Override
	public void preRemoveSideEffects(BlockPos pos, BlockState state) {
		super.preRemoveSideEffects(pos, state);

		if (level != null) {
			Containers.dropContents(level, pos, upgrades);
		}
	}

	// ------------------------------------------------------------------
	// Menu
	// ------------------------------------------------------------------

	@Override
	public Component getDisplayName() {
		return getBlockState().getBlock().getName();
	}

	@Override
	public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
		return new StorageTerminalMenu(containerId, inventory, this, false);
	}

	@Override
	public StorageTerminalMenu.OpenData getScreenOpeningData(ServerPlayer player) {
		return new StorageTerminalMenu.OpenData(worldPosition, isCrafting());
	}

	/** Menu provider for the wireless terminal (larger range). */
	public ExtendedMenuProvider<StorageTerminalMenu.OpenData> wirelessProvider() {
		StorageTerminalBlockEntity terminal = this;

		return new ExtendedMenuProvider<>() {
			@Override
			public StorageTerminalMenu.OpenData getScreenOpeningData(ServerPlayer player) {
				return terminal.getScreenOpeningData(player);
			}

			@Override
			public Component getDisplayName() {
				return terminal.getDisplayName();
			}

			@Override
			public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
				return new StorageTerminalMenu(containerId, inventory, terminal, true);
			}
		};
	}

	/** Nearest storage terminal in loaded chunks nearby (v0.43 moved here from StormContent). */
	public static @Nullable BlockPos nearestTerminal(ServerLevel level, BlockPos from, int search) {
		BlockPos best = null;
		double bestDist = (double) search * search;
		ChunkPos center = ChunkPos.containing(from);
		int r = search / 16 + 1;

		for (int cx = center.x() - r; cx <= center.x() + r; cx++) {
			for (int cz = center.z() - r; cz <= center.z() + r; cz++) {
				if (!(level.getChunkSource().getChunkNow(cx, cz) instanceof LevelChunk chunk)) {
					continue;
				}

				for (BlockEntity entity : chunk.getBlockEntities().values()) {
					if (entity instanceof StorageTerminalBlockEntity) {
						double dist = entity.getBlockPos().distToCenterSqr(Vec3.atCenterOf(from));

						if (dist < bestDist) {
							bestDist = dist;
							best = entity.getBlockPos();
						}
					}
				}
			}
		}

		return best;
	}
}
