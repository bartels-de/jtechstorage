package dev.bartels.jtechstorage.block;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.mojang.serialization.Codec;

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
 * Herz des Netzwerks: speichert Besitzer, verknüpfte Kisten und Upgrades.
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
	/** Team-Mitglieder: UUID → Name + Rolle (Reihenfolge des Hinzufügens). */
	private final Map<UUID, Member> members = new LinkedHashMap<>();
	/** Wer außer dem Besitzer darf das Netzwerk nutzen? */
	private AccessMode accessMode = AccessMode.FRIENDS;
	/** Rolle für Fremde, wenn das Netzwerk öffentlich ist. */
	private Role publicRole = Role.GUEST;
	/** Team-Farbe (Namensschilder der Helfer, Team-Kanal). */
	private ChatFormatting teamColor = ChatFormatting.LIGHT_PURPLE;
	/** Aktivierte Kanal-Meldungen. */
	private final EnumSet<Channel> channels = EnumSet.allOf(Channel.class);
	/** Wurde schon vor einem fast vollen Lager gewarnt? */
	private boolean warnedFull;
	/** Frei wählbarer Netzwerk-Name – Helfer-Mobs tragen ihn als Präfix im Namensschild. */
	private String networkName = "";
	public static final int MAX_NAME_LENGTH = 20;
	/** Feste Kategorie je verknüpfter Kiste (vom Amethyst-Wichtel vergeben). */
	private final Map<BlockPos, SortCategory> assignments = new LinkedHashMap<>();

	/** Ein Team-Mitglied. */
	public record Member(String name, Role role) {
	}

	/** Rollen im Team – jede Stufe darf alles, was die darunter darf. */
	public enum Role {
		/** Sieht den Inhalt, darf aber nichts nehmen oder einlagern. */
		GUEST(ChatFormatting.GRAY),
		/** Items nehmen und einlagern, craften, Funk-Terminal. */
		MEMBER(ChatFormatting.GREEN),
		/** + Kisten verknüpfen, Helfer verknüpfen und bedienen. */
		BUILDER(ChatFormatting.AQUA),
		/** + Upgrades, Team-Einstellungen, Mitglieder bis Baumeister verwalten, Handels-Terminal. */
		MANAGER(ChatFormatting.GOLD),
		/** Besitzer: alles. */
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

	/** Was eine Aktion mindestens verlangt. */
	public enum Permission {
		VIEW(Role.GUEST), ITEMS(Role.MEMBER), LINK(Role.BUILDER), HELPERS(Role.BUILDER),
		UPGRADES(Role.MANAGER), SETTINGS(Role.MANAGER), TEAM(Role.MANAGER), TRADE(Role.MANAGER);

		public final Role minimum;

		Permission(Role minimum) {
			this.minimum = minimum;
		}
	}

	/** Team-Kanal: Ereignis-Meldungen an alle Mitglieder, die online sind. */
	public enum Channel {
		TEAM, TRADE, GUARD, STORAGE, MAIL;

		public String translationKey() {
			return "jtechstorage.channel." + name().toLowerCase(java.util.Locale.ROOT);
		}
	}

	/** Farben, die man als Team-Farbe wählen kann. */
	public static final ChatFormatting[] TEAM_COLORS = {
			ChatFormatting.LIGHT_PURPLE, ChatFormatting.DARK_PURPLE, ChatFormatting.BLUE, ChatFormatting.AQUA,
			ChatFormatting.DARK_AQUA, ChatFormatting.GREEN, ChatFormatting.DARK_GREEN, ChatFormatting.YELLOW,
			ChatFormatting.GOLD, ChatFormatting.RED, ChatFormatting.DARK_RED, ChatFormatting.WHITE, ChatFormatting.GRAY};

	public enum AccessMode {
		/** Nur der Besitzer. */
		PRIVATE,
		/** Besitzer + Team-Mitglieder (Name bleibt für alte Welten erhalten). */
		FRIENDS,
		/** Jeder (Fremde bekommen die Rolle {@link #getPublicRole()}). */
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
	// ------------------------------------------------------------------

	public void setOwner(Player player) {
		owner = player.getUUID();
		ownerName = player.getPlainTextName();
		setChanged();
	}

	/** Darf dieser Spieler das Terminal benutzen (mindestens ansehen)? Ein Terminal ohne Besitzer wird vom ersten Nutzer übernommen. */
	public boolean canAccess(Player player) {
		if (owner == null) {
			setOwner(player);
			return true;
		}

		return roleOf(player) != null;
	}

	/** Rolle eines Spielers in diesem Netzwerk oder {@code null} (kein Zugang). */
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

	/** Darf der Spieler das? */
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

	/** Team-Farbe als RGB. */
	public int getTeamRgb() {
		return net.minecraft.network.chat.TextColor.fromLegacyFormat(teamColor).getValue();
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

	/** Bitmaske der aktiven Kanäle (für die Oberfläche). */
	public int channelMask() {
		int mask = 0;

		for (Channel channel : channels) {
			mask |= 1 << channel.ordinal();
		}

		return mask;
	}

	/** Name, unter dem das Team auftritt: Netzwerk-Name oder „Team von …“. */
	public Component teamLabel() {
		return networkName.isEmpty()
				? Component.translatable("jtechstorage.team.default_name", ownerName)
				: Component.literal(networkName);
	}

	/**
	 * Schickt eine Meldung an alle Team-Mitglieder (inkl. Besitzer), die gerade online sind – sofern der Kanal aktiv ist.
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

	/** Ist irgendein Team-Mitglied online? */
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
	// Sortier-Zuordnung (SortAssignments)
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

	/** Namen der Mitglieder (in der Reihenfolge des Hinzufügens). */
	public List<String> getMemberNames() {
		return members.values().stream().map(Member::name).toList();
	}

	/** Alle Mitglieder (UUID → Name + Rolle), unveränderlich. */
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

	/** Mitglied per Name suchen (Groß-/Kleinschreibung egal). */
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

	/** Aktualisiert den gespeicherten Namen, wenn ein Mitglied sich umbenannt hat. */
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
	// Upgrades & Kennzahlen
	// ------------------------------------------------------------------

	public SimpleContainer getUpgrades() {
		return upgrades;
	}

	public int getRange() {
		int range = UpgradeItem.valueOf(upgrades.getItem(RANGE_SLOT), UpgradeItem.Kind.RANGE, ModContent.BASE_RANGE);
		return isCharged() ? range + RESONANCE_RANGE_BONUS : range;
	}

	// ------------------------------------------------------------------
	// Resonanz („Strom“ als Bonus – das Netzwerk läuft auch ohne)
	// ------------------------------------------------------------------

	public static final int MAX_RESONANCE = 1000;
	public static final int RESONANCE_RANGE_BONUS = ModConfig.get().network.resonanceRangeBonus;
	public static final float RESONANCE_HELPER_SPEED = (float) ModConfig.get().network.resonanceHelperSpeed;

	private int resonance;

	public int getResonance() {
		return resonance;
	}

	/** Mit Resonanz geladen? Dann gelten die Boni (Reichweite, Funk, Helfer-Tempo). */
	public boolean isCharged() {
		return resonance > 0;
	}

	/** Tempo-Faktor für Helfer-Mobs dieses Netzwerks. */
	public float helperSpeedFactor() {
		return isCharged() ? RESONANCE_HELPER_SPEED : 1.0f;
	}

	/** Fügt Resonanz hinzu (z. B. vom Blitz-Käfer). @return true, wenn der Speicher jetzt voll ist. */
	public boolean addResonance(int amount) {
		int before = resonance;
		resonance = Math.min(MAX_RESONANCE, resonance + amount);

		if (resonance != before) {
			setChanged();
		}

		return resonance >= MAX_RESONANCE;
	}

	/** Wird jeden Tick auf dem Server aufgerufen: Resonanz entlädt sich langsam (1 pro Sekunde). */
	public static void serverTick(net.minecraft.world.level.Level level, BlockPos pos, BlockState state,
			StorageTerminalBlockEntity terminal) {
		if (terminal.resonance > 0 && level.getGameTime() % 20 == 0) {
			terminal.resonance--;
			terminal.setChanged();
		}

		// Team-Kanal „Lager“: Warnung, wenn das Lager fast voll ist (alle 30 Sekunden geprüft)
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

	/** Ender-Upgrade eingelegt? Dann funktioniert das Funk-Terminal grenzenlos und über Dimensionen hinweg. */
	public boolean hasEnderUpgrade() {
		return UpgradeItem.isEnder(upgrades.getItem(RANGE_SLOT));
	}

	/**
	 * Hält Terminal- und Kisten-Chunks für ca. 15 Sekunden geladen (wird vom offenen Funk-Menü
	 * regelmäßig erneuert, läuft danach von selbst ab).
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
	// Verknüpfte Kisten
	// ------------------------------------------------------------------

	public List<BlockPos> getLinks() {
		return List.copyOf(links);
	}

	public enum LinkResult {
		ADDED, REMOVED, FULL, OUT_OF_RANGE
	}

	/** Fügt eine Kiste hinzu oder entfernt sie (Doppeltruhen zählen als eine Kiste). */
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

	/** Aktuelles Netzwerk; entfernt dabei Verknüpfungen zu abgebauten Kisten. */
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
	// Speichern
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
				// unbekannte Rolle – Mitglied
			}

			members.put(memberIds.get(i), new Member(i < memberNames.size() ? memberNames.get(i) : "?", role == Role.OWNER ? Role.MANAGER : role));
		}

		// Alte Welten (bis v0.14): Freunde werden zu Baumeistern
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
			channels.add(Channel.MAIL);   // neuer Kanal (v0.16) ist bei alten Welten an
		}

		resonance = Math.clamp(input.getIntOr("Resonance", 0), 0, MAX_RESONANCE);
		networkName = input.getStringOr("NetworkName", "");

		try {
			accessMode = AccessMode.valueOf(input.getStringOr("AccessMode", AccessMode.FRIENDS.name()));
		} catch (IllegalArgumentException e) {
			accessMode = AccessMode.FRIENDS;
		}

		if (oldWorld && accessMode == AccessMode.PUBLIC) {
			publicRole = Role.BUILDER;   // früher durfte bei „öffentlich“ jeder fast alles
		}

		assignments.clear();
		List<BlockPos> sortKeys = input.read("SortKeys", BlockPos.CODEC.listOf()).orElse(List.of());
		List<String> sortCategories = input.read("SortCategories", Codec.STRING.listOf()).orElse(List.of());

		for (int i = 0; i < sortKeys.size() && i < sortCategories.size(); i++) {
			try {
				assignments.put(sortKeys.get(i), SortCategory.valueOf(sortCategories.get(i)));
			} catch (IllegalArgumentException ignored) {
				// unbekannte Kategorie aus einer anderen Version – überspringen
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
	// Menü
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

	/** Menü-Anbieter für das drahtlose Terminal (größere Reichweite). */
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
}
