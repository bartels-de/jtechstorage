package dev.bartels.jtechstorage.menu;

import java.util.List;
import java.util.Optional;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Prediction;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import dev.bartels.jtechstorage.ModAdvancements;
import dev.bartels.jtechstorage.ModContent;
import dev.bartels.jtechstorage.block.StorageTerminalBlockEntity;
import dev.bartels.jtechstorage.item.UpgradeItem;
import dev.bartels.jtechstorage.net.TerminalActionPayload;
import dev.bartels.jtechstorage.net.TerminalContentsPayload;
import dev.bartels.jtechstorage.network.StorageNetwork;

/**
 * Menü des Speicher- und Crafting-Terminals.
 *
 * <p>Echte Slots gibt es für die zwei Upgrades, das Spielerinventar und (beim Crafting-Terminal)
 * das 3×3-Raster samt Ergebnis. Der Netzwerk-Inhalt wird als Liste per {@link TerminalContentsPayload}
 * an den Client geschickt, Klicks auf das Raster kommen als {@link TerminalActionPayload} zurück.
 */
public class StorageTerminalMenu extends AbstractContainerMenu {
	/** Daten, die beim Öffnen an den Client gehen. */
	public record OpenData(BlockPos pos, boolean crafting) {
		public static final StreamCodec<io.netty.buffer.ByteBuf, OpenData> STREAM_CODEC = StreamCodec.composite(
				BlockPos.STREAM_CODEC, OpenData::pos,
				ByteBufCodecs.BOOL, OpenData::crafting,
				OpenData::new);
	}

	/** Positionen im GUI – vom Screen mitgenutzt. */
	public record Layout(int gridRows, int toolbarY, int craftingY, int inventoryY, int imageHeight) {
		/** Höhe ohne Raster (für die automatische Zeilenzahl). */
		public static int fixedHeight(boolean crafting) {
			return crafting ? 19 + 22 + 71 + 82 : 19 + 34 + 82;
		}

		public static Layout of(boolean crafting, int rows) {
			int toolbarY = 19 + rows * 18;

			if (crafting) {
				int craftingY = toolbarY + 22;
				int inventoryY = craftingY + 71;
				return new Layout(rows, toolbarY, craftingY, inventoryY, inventoryY + 82);
			}

			int inventoryY = toolbarY + 34;
			return new Layout(rows, toolbarY, -1, inventoryY, inventoryY + 82);
		}

		public static Layout of(boolean crafting) {
			return of(crafting, crafting ? 4 : 6);
		}

		public boolean hasCrafting() {
			return craftingY >= 0;
		}
	}

	/** Zeilenzahl auf dem Client (wird von den Oberflächen-Einstellungen gesetzt). */
	public static java.util.function.Function<Boolean, Integer> clientRowProvider = crafting -> crafting ? 4 : 6;

	public static final int GRID_X = 8;
	public static final int GRID_Y = 18;
	public static final int UPGRADE_SLOT_X = 173;
	public static final int CRAFT_GRID_X = 26;
	public static final int CRAFT_RESULT_X = 120;

	public static final int RANGE_SLOT = 0;
	public static final int CAPACITY_SLOT = 1;
	private static final int PLAYER_START = 2;
	private static final int PLAYER_END = PLAYER_START + 36;
	private static final int RESULT_SLOT = PLAYER_END;
	private static final int CRAFT_START = RESULT_SLOT + 1;
	private static final int CRAFT_END = CRAFT_START + 9;
	/** Sichtbare Felder des Netzwerk-Rasters als echte Slots (v0.29.4), höchstens 10 Zeilen. */
	public static final int MAX_VIEW = 90;

	private static final int REFRESH_INTERVAL = 20;
	private static final Identifier RANGE_SLOT_ICON = Identifier.withDefaultNamespace("container/slot/amethyst_shard");
	private static final Identifier CAPACITY_SLOT_ICON = Identifier.withDefaultNamespace("container/slot/ingot");

	private final Player player;
	private final Layout layout;
	private final @Nullable StorageTerminalBlockEntity terminal;
	private final boolean wireless;
	private final @Nullable CraftingContainer craftSlots;
	private final @Nullable ResultContainer resultSlots;

	// Server-Zustand
	private @Nullable StorageNetwork network;
	private List<StorageNetwork.Entry> lastSent = List.of();
	private TerminalContentsPayload.@Nullable NetworkInfo lastSentInfo;
	private TerminalContentsPayload.@Nullable AccessInfo lastSentAccess;
	private int ticksSinceRefresh = REFRESH_INTERVAL;
	private boolean forceSync = true;

	// Client-Zustand
	private List<StorageNetwork.Entry> clientEntries = List.of();
	private TerminalContentsPayload.NetworkInfo clientInfo = TerminalContentsPayload.NetworkInfo.EMPTY;
	private TerminalContentsPayload.AccessInfo clientAccess = TerminalContentsPayload.AccessInfo.EMPTY;
	private int clientVersion;

	// Netzwerk-Raster als Vanilla-Slots: Vorlage je Feld (Server: vom Client gemeldet), angezeigte Menge
	private int viewStart;
	private final ItemStack[] viewPrototype = new ItemStack[MAX_VIEW];
	private final int[] viewShown = new int[MAX_VIEW];
	private final ItemStack[] viewDisplay = new ItemStack[MAX_VIEW];
	private final SimpleContainer clientView = new SimpleContainer(MAX_VIEW);

	/** Client-Konstruktor (über {@link net.fabricmc.fabric.api.menu.v1.ExtendedMenuType}). */
	public StorageTerminalMenu(int containerId, Inventory inventory, OpenData data) {
		this(containerId, inventory, data.crafting(), null, false, new SimpleContainer(2), clientRowProvider.apply(data.crafting()));
	}

	/** Server-Konstruktor. */
	public StorageTerminalMenu(int containerId, Inventory inventory, StorageTerminalBlockEntity terminal, boolean wireless) {
		this(containerId, inventory, terminal.isCrafting(), terminal, wireless, terminal.getUpgrades(), terminal.isCrafting() ? 4 : 6);
	}

	private StorageTerminalMenu(int containerId, Inventory inventory, boolean crafting,
			@Nullable StorageTerminalBlockEntity terminal, boolean wireless, Container upgrades, int rows) {
		super(ModContent.STORAGE_TERMINAL_MENU, containerId);
		this.player = inventory.player;
		this.layout = Layout.of(crafting, rows);
		this.terminal = terminal;
		this.wireless = wireless;

		addSlot(new UpgradeSlot(upgrades, RANGE_SLOT, layout.inventoryY(), UpgradeItem.Kind.RANGE, RANGE_SLOT_ICON));
		addSlot(new UpgradeSlot(upgrades, CAPACITY_SLOT, layout.inventoryY() + 18, UpgradeItem.Kind.CAPACITY, CAPACITY_SLOT_ICON));
		addStandardInventorySlots(inventory, GRID_X, layout.inventoryY());

		if (crafting) {
			craftSlots = new TransientCraftingContainer(this, 3, 3);
			resultSlots = new ResultContainer();
			int top = layout.craftingY();
			addSlot(new NetworkResultSlot(player, craftSlots, resultSlots, CRAFT_RESULT_X, top + 18));

			for (int row = 0; row < 3; row++) {
				for (int column = 0; column < 3; column++) {
					addSlot(new Slot(craftSlots, column + row * 3, CRAFT_GRID_X + column * 18, top + row * 18));
				}
			}
		} else {
			craftSlots = null;
			resultSlots = null;
		}

		java.util.Arrays.fill(viewPrototype, ItemStack.EMPTY);
		java.util.Arrays.fill(viewDisplay, ItemStack.EMPTY);
		viewStart = slots.size();

		for (int i = 0; i < MAX_VIEW; i++) {
			addSlot(new NetworkSlot(i, GRID_X + (i % 9) * 18, GRID_Y + (i / 9) * 18));
		}
	}

	/** Erster Slot-Index des Netzwerk-Rasters. */
	public int viewStart() {
		return viewStart;
	}

	public boolean isViewSlot(Slot slot) {
		return slot instanceof NetworkSlot;
	}

	// ------------------------------------------------------------------
	// Netzwerk-Raster als Vanilla-Slots (v0.29.4)
	// ------------------------------------------------------------------

	/** Client: zeigt diese Items in den Feldern (Menge = höchstens ein Stapel) – für die Vorhersage der Klicks. */
	public void setClientView(List<ItemStack> displays) {
		for (int i = 0; i < MAX_VIEW; i++) {
			clientView.setItem(i, i < displays.size() ? displays.get(i) : ItemStack.EMPTY);
		}
	}

	/** Server: Vorlagen der sichtbaren Felder vom Client übernehmen. */
	public void setView(List<ItemStack> prototypes) {
		for (int i = 0; i < MAX_VIEW; i++) {
			ItemStack prototype = i < prototypes.size() ? prototypes.get(i) : ItemStack.EMPTY;
			viewPrototype[i] = prototype.isEmpty() ? ItemStack.EMPTY : prototype.copyWithCount(1);
		}

		refreshView(network().aggregate());
		broadcastChanges();
	}

	/** Angezeigte Mengen aus dem echten Netzwerk-Inhalt neu berechnen. */
	private void refreshView(List<StorageNetwork.Entry> entries) {
		java.util.Map<Integer, List<StorageNetwork.Entry>> byHash = new java.util.HashMap<>();

		for (StorageNetwork.Entry entry : entries) {
			byHash.computeIfAbsent(ItemStack.hashItemAndComponents(entry.prototype()), k -> new java.util.ArrayList<>()).add(entry);
		}

		for (int i = 0; i < MAX_VIEW; i++) {
			ItemStack prototype = viewPrototype[i];
			long total = 0;

			if (!prototype.isEmpty()) {
				for (StorageNetwork.Entry entry : byHash.getOrDefault(ItemStack.hashItemAndComponents(prototype), List.of())) {
					if (ItemStack.isSameItemSameComponents(entry.prototype(), prototype)) {
						total = entry.total();
						break;
					}
				}
			}

			setShown(i, (int) Math.min(total, prototype.getMaxStackSize()));
		}
	}

	private void setShown(int i, int count) {
		viewShown[i] = Math.max(0, count);
		// immer ein neues Objekt: Vanilla darf das alte (z. B. auf dem Mauszeiger) behalten
		viewDisplay[i] = count > 0 && !viewPrototype[i].isEmpty() ? viewPrototype[i].copyWithCount(count) : ItemStack.EMPTY;
	}

	@Override
	public void clicked(int slotId, int button, net.minecraft.world.inventory.ContainerInput input, Player player) {
		if (terminal != null) {
			// frische Mengen, damit Vanilla nie mehr verteilt, als wirklich da ist
			refreshView(network().aggregate());
			forceSync = true;
		}

		super.clicked(slotId, button, input, player);
	}

	/** Ein Feld des Netzwerk-Rasters. Server: Entnehmen/Ablegen geht direkt ins Netzwerk. */
	private class NetworkSlot extends Slot {
		private final int view;

		NetworkSlot(int view, int x, int y) {
			super(clientView, view, x, y);
			this.view = view;
		}

		private boolean server() {
			return terminal != null;
		}

		@Override
		public boolean isActive() {
			return server() || view < layout.gridRows() * 9;
		}

		@Override
		public ItemStack getItem() {
			return server() ? viewDisplay[view] : super.getItem();
		}

		@Override
		public boolean hasItem() {
			return !getItem().isEmpty();
		}

		@Override
		public int getMaxStackSize() {
			return Integer.MAX_VALUE / 2;
		}

		@Override
		public int getMaxStackSize(ItemStack stack) {
			return Integer.MAX_VALUE / 2;
		}

		@Override
		public boolean mayPlace(ItemStack stack) {
			return hasPermission(StorageTerminalBlockEntity.Permission.ITEMS);
		}

		@Override
		public boolean mayPickup(Player player) {
			return hasPermission(StorageTerminalBlockEntity.Permission.ITEMS);
		}

		@Override
		public ItemStack remove(int amount) {
			if (!server()) {
				return super.remove(amount);
			}

			ItemStack prototype = viewPrototype[view];
			int take = Math.min(amount, viewShown[view]);

			if (prototype.isEmpty() || take <= 0) {
				return ItemStack.EMPTY;
			}

			ItemStack taken = network().extract(prototype, take);
			setShown(view, viewShown[view] - taken.getCount());
			forceSync = true;
			return taken;
		}

		@Override
		public void set(ItemStack stack) {
			if (!server()) {
				super.set(stack);
				return;
			}

			StorageNetwork net = network();
			ItemStack prototype = viewPrototype[view];
			int had = viewShown[view];

			if (!stack.isEmpty() && !prototype.isEmpty() && ItemStack.isSameItemSameComponents(stack, prototype)) {
				int diff = stack.getCount() - had;

				if (diff > 0) {
					giveBack(net.insert(prototype.copyWithCount(diff)));
				} else if (diff < 0) {
					net.extract(prototype, -diff);
				}
			} else {
				// was bisher angezeigt wurde, hat Vanilla weitergegeben (z. B. beim Tauschen auf den Mauszeiger)
				if (had > 0 && !prototype.isEmpty()) {
					net.extract(prototype, had);
				}

				if (!stack.isEmpty()) {
					giveBack(net.insert(stack.copy()));
					viewPrototype[view] = stack.copyWithCount(1);
				}
			}

			ItemStack now = viewPrototype[view];
			setShown(view, now.isEmpty() ? 0 : (int) Math.min(net.count(now), now.getMaxStackSize()));
			forceSync = true;
		}

		private void giveBack(ItemStack rest) {
			if (!rest.isEmpty() && player instanceof ServerPlayer serverPlayer) {
				giveToPlayer(serverPlayer, rest); // Netzwerk voll
			}
		}

		@Override
		public void setChanged() {
			if (!server()) {
				super.setChanged();
			}
		}
	}

	public Layout getLayout() {
		return layout;
	}

	// ------------------------------------------------------------------
	// Server: Netzwerk & Synchronisierung
	// ------------------------------------------------------------------

	private StorageNetwork network() {
		if (network == null) {
			rescan();
		}

		return network != null ? network : StorageNetwork.EMPTY;
	}

	private void rescan() {
		if (terminal != null) {
			network = terminal.buildNetwork();
		}
	}

	/** Wird jeden Tick aufgerufen, solange das Menü offen ist. */
	@Override
	public void broadcastChanges() {
		if (terminal != null && player instanceof ServerPlayer serverPlayer
				&& (forceSync || ++ticksSinceRefresh >= REFRESH_INTERVAL)) {
			syncContents(serverPlayer);
		}

		super.broadcastChanges();
	}

	private void syncContents(ServerPlayer serverPlayer) {
		ticksSinceRefresh = 0;
		forceSync = false;

		if (wireless && terminal != null && terminal.hasEnderUpgrade()) {
			terminal.keepNetworkLoaded();
		}

		rescan();

		if (terminal == null || network == null) {
			return;
		}

		List<StorageNetwork.Entry> entries = network.aggregate();
		refreshView(entries);
		TerminalContentsPayload.NetworkInfo info = new TerminalContentsPayload.NetworkInfo(
				terminal.getRange(), network.linkCount(), terminal.getCapacity(), network.unavailable(),
				network.overCapacity(), network.usedSlots(), network.totalSlots(), network.sortRuleCount(),
				(terminal.getResonance() * 100 + StorageTerminalBlockEntity.MAX_RESONANCE - 1) / StorageTerminalBlockEntity.MAX_RESONANCE);

		StorageTerminalBlockEntity.Role role = terminal.roleOf(serverPlayer);
		terminal.refreshName(serverPlayer);
		TerminalContentsPayload.AccessInfo access = new TerminalContentsPayload.AccessInfo(
				role != null ? role.ordinal() : 0, terminal.getOwnerName(), terminal.getMemberNames(),
				terminal.getAccessMode().ordinal(), terminal.getNetworkName(), terminal.getTeamRgb());

		if (!info.equals(lastSentInfo) || !access.equals(lastSentAccess) || !StorageNetwork.Entry.sameContents(entries, lastSent)) {
			lastSent = entries;
			lastSentInfo = info;
			lastSentAccess = access;
			ServerPlayNetworking.send(serverPlayer, new TerminalContentsPayload(containerId, entries, info, access));
		}
	}

	/** Position des Terminals (nur Server; v0.27.0 für die Farmen-Liste). */
	public net.minecraft.core.@Nullable GlobalPos terminalPos() {
		return terminal == null || terminal.getLevel() == null ? null : net.minecraft.core.GlobalPos.of(terminal.getLevel().dimension(), terminal.getBlockPos());
	}

	/** Statistik an den Client schicken (nur Server). */
	public void sendStats(ServerPlayer player) {
		if (terminal == null) {
			return;
		}

		StorageNetwork current = terminal.buildNetwork();
		dev.bartels.jtechstorage.network.NetworkStats stats = terminal.getStats();
		long day = Math.floorDiv(player.level().getDefaultClockTime(), 24000L);

		if (stats.needsSnapshot(day)) {
			stats.snapshot(day, current);
			terminal.setChanged();
		}

		ServerPlayNetworking.send(player, new dev.bartels.jtechstorage.net.TerminalStatsPayload(containerId, stats.history(),
				stats.todayStart(), stats.changes(current)));
	}

	/** Verarbeitet einen Klick auf das Item-Raster oder einen Knopf (nur Server). */
	public void handleAction(ServerPlayer player, TerminalActionPayload.Action action, ItemStack prototype) {
		if (!hasPermission(StorageTerminalBlockEntity.Permission.ITEMS)) {
			player.sendOverlayMessage(Component.translatable("jtechstorage.message.no_permission",
					Component.translatable("jtechstorage.role.member")));
			return;
		}

		StorageNetwork network = network();
		ItemStack carried = getCarried();

		switch (action) {
			case TAKE_STACK, TAKE_HALF -> {
				if (!carried.isEmpty() || prototype.isEmpty()) {
					return;
				}

				int amount = prototype.getMaxStackSize();

				if (action == TerminalActionPayload.Action.TAKE_HALF) {
					long available = network.count(prototype);
					amount = (int) Math.max(1, (Math.min(available, amount) + 1) / 2);
				}

				setCarried(network.extract(prototype, amount));
			}
			case TAKE_TO_INVENTORY -> {
				if (!prototype.isEmpty()) {
					giveToPlayer(player, network.extract(prototype, prototype.getMaxStackSize()));
				}
			}
			case INSERT_ALL -> {
				if (!carried.isEmpty()) {
					setCarried(network.insert(carried));
				}
			}
			case INSERT_ONE -> {
				if (!carried.isEmpty()) {
					ItemStack rest = network.insert(carried.split(1));
					carried.grow(rest.getCount());
					setCarried(carried);
				}
			}
			case CLEAR_GRID -> clearCraftingGrid(network);
		}

		forceSync = true;
		broadcastChanges();
	}

	/** Rezept aus JEI ins Raster legen: Zutaten erst aus dem Netzwerk, dann aus dem Inventar. */
	public void handleCraftingTransfer(ServerPlayer player, List<List<ItemStack>> slots) {
		if (craftSlots == null || !hasPermission(StorageTerminalBlockEntity.Permission.ITEMS)) {
			return;
		}

		StorageNetwork network = network();
		clearCraftingGrid(network);

		for (int i = 0; i < craftSlots.getContainerSize(); i++) {
			ItemStack leftover = craftSlots.getItem(i);

			if (!leftover.isEmpty()) {
				craftSlots.setItem(i, ItemStack.EMPTY);
				giveToPlayer(player, leftover);
			}
		}

		for (int i = 0; i < Math.min(slots.size(), craftSlots.getContainerSize()); i++) {
			ItemStack found = findIngredient(player, network, slots.get(i));

			if (!found.isEmpty()) {
				craftSlots.setItem(i, found);
			}
		}

		forceSync = true;
		broadcastChanges();
	}

	private static ItemStack findIngredient(ServerPlayer player, StorageNetwork network, List<ItemStack> options) {
		// 1. exakt aus dem Netzwerk, 2. exakt aus dem Inventar, 3. gleiche Sorte (Komponenten egal)
		for (ItemStack option : options) {
			ItemStack found = network.extract(option, 1);

			if (!found.isEmpty()) {
				return found;
			}
		}

		Inventory inventory = player.getInventory();

		for (ItemStack option : options) {
			for (int slot = 0; slot < Inventory.INVENTORY_SIZE; slot++) {
				ItemStack stack = inventory.getItem(slot);

				if (!stack.isEmpty() && ItemStack.isSameItemSameComponents(stack, option)) {
					inventory.setChanged();
					return stack.split(1);
				}
			}
		}

		for (ItemStack option : options) {
			ItemStack found = network.extractAnyVariant(option, 1);

			if (!found.isEmpty()) {
				return found;
			}
		}

		return ItemStack.EMPTY;
	}

	/**
	 * Hat der Betrachter mindestens diese Berechtigung? Server: aktuelle Rolle im Team; Client: zuletzt übertragene Rolle.
	 */
	public boolean hasPermission(StorageTerminalBlockEntity.Permission permission) {
		if (terminal != null) {
			return terminal.can(player, permission);
		}

		return StorageTerminalBlockEntity.Role.byId(clientAccess.role()).atLeast(permission.minimum);
	}

	private void giveToPlayer(ServerPlayer player, ItemStack stack) {
		if (stack.isEmpty()) {
			return;
		}

		player.getInventory().add(stack);

		if (!stack.isEmpty()) {
			// Inventar voll: zurück ins Netzwerk, notfalls fallen lassen
			ItemStack rest = network().insert(stack);

			if (!rest.isEmpty()) {
				player.drop(rest, false, Prediction.SERVER_ONLY);
			}
		}
	}

	// ------------------------------------------------------------------
	// Crafting
	// ------------------------------------------------------------------

	@Override
	public void slotsChanged(Container container) {
		super.slotsChanged(container);

		if (container == craftSlots && player instanceof ServerPlayer serverPlayer) {
			updateCraftingResult(serverPlayer);
		}
	}

	private void updateCraftingResult(ServerPlayer serverPlayer) {
		if (craftSlots == null || resultSlots == null) {
			return;
		}

		ServerLevel level = serverPlayer.level();
		CraftingInput input = craftSlots.asCraftInput();
		ItemStack result = ItemStack.EMPTY;
		Optional<RecipeHolder<CraftingRecipe>> recipe = level.getServer().getRecipeManager()
				.getRecipeFor(RecipeType.CRAFTING, input, level);

		if (recipe.isPresent() && resultSlots.setRecipeUsed(serverPlayer, recipe.get())) {
			ItemStack assembled = recipe.get().value().assemble(input);

			if (assembled.isItemEnabled(level.enabledFeatures())) {
				result = assembled;
			}
		}

		resultSlots.setItem(0, result);
		setRemoteSlot(RESULT_SLOT, result);
		serverPlayer.connection.send(new ClientboundContainerSetSlotPacket(containerId, incrementStateId(), RESULT_SLOT, result));
	}

	private void clearCraftingGrid(StorageNetwork network) {
		if (craftSlots == null) {
			return;
		}

		for (int i = 0; i < craftSlots.getContainerSize(); i++) {
			ItemStack stack = craftSlots.getItem(i);

			if (!stack.isEmpty()) {
				craftSlots.setItem(i, network.insert(stack.copy()));
			}
		}
	}

	/** Ergebnis-Slot, der nach dem Craften leer gewordene Rasterfelder aus dem Netzwerk nachfüllt. */
	private class NetworkResultSlot extends ResultSlot {
		NetworkResultSlot(Player player, CraftingContainer craftSlots, Container resultSlots, int x, int y) {
			super(player, craftSlots, resultSlots, 0, x, y);
		}

		@Override
		public void onTake(Player player, ItemStack stack) {
			if (craftSlots == null) {
				super.onTake(player, stack);
				return;
			}

			ItemStack[] before = new ItemStack[craftSlots.getContainerSize()];

			for (int i = 0; i < before.length; i++) {
				before[i] = craftSlots.getItem(i).copyWithCount(1);
			}

			super.onTake(player, stack);

			if (terminal == null || !hasPermission(StorageTerminalBlockEntity.Permission.ITEMS)) {
				return;
			}

			for (int i = 0; i < before.length; i++) {
				if (!before[i].isEmpty() && craftSlots.getItem(i).isEmpty()) {
					ItemStack refill = network().extract(before[i], 1);

					if (!refill.isEmpty()) {
						craftSlots.setItem(i, refill);
					}
				}
			}

			forceSync = true;
		}
	}

	// ------------------------------------------------------------------
	// Shift-Klick & Schließen
	// ------------------------------------------------------------------

	@Override
	public ItemStack quickMoveStack(Player player, int slotIndex) {
		Slot slot = getSlot(slotIndex);

		if (!slot.hasItem()) {
			return ItemStack.EMPTY;
		}

		if (slot instanceof NetworkSlot) {
			if (terminal != null && slot.mayPickup(player)) {
				// wie bei einer Kiste: nur dieser eine Stapel
				ItemStack moving = slot.remove(slot.getItem().getCount());
				moveItemStackTo(moving, PLAYER_START, PLAYER_END, true);

				if (!moving.isEmpty()) {
					ItemStack rest = network().insert(moving);

					if (!rest.isEmpty() && player instanceof ServerPlayer serverPlayer) {
						giveToPlayer(serverPlayer, rest);
					}
				}
			}

			return ItemStack.EMPTY;
		}

		ItemStack stack = slot.getItem();

		// Crafting-Ergebnis: so oft wie möglich craften (wird von Vanilla wiederholt aufgerufen)
		if (slotIndex == RESULT_SLOT && craftSlots != null) {
			ItemStack original = stack.copy();
			stack.getItem().onCraftedBy(stack, player);

			if (!moveItemStackTo(stack, PLAYER_START, PLAYER_END, true)) {
				return ItemStack.EMPTY;
			}

			slot.onQuickCraft(stack, original);

			if (stack.isEmpty()) {
				slot.setByPlayer(ItemStack.EMPTY);
			} else {
				slot.setChanged();
			}

			if (stack.getCount() == original.getCount()) {
				return ItemStack.EMPTY;
			}

			slot.onTake(player, stack);
			return original;
		}

		// Upgrades zurück ins Inventar
		if (slotIndex == RANGE_SLOT || slotIndex == CAPACITY_SLOT) {
			if (!hasPermission(StorageTerminalBlockEntity.Permission.UPGRADES)) {
				return ItemStack.EMPTY;
			}

			if (moveItemStackTo(stack, PLAYER_START, PLAYER_END, true)) {
				slot.setChanged();
			}

			return ItemStack.EMPTY;
		}

		// Upgrades aus dem Inventar in den passenden Slot
		if (slotIndex >= PLAYER_START && slotIndex < PLAYER_END && stack.getItem() instanceof UpgradeItem upgrade
				&& hasPermission(StorageTerminalBlockEntity.Permission.UPGRADES)) {
			int target = upgrade.kind() == UpgradeItem.Kind.RANGE ? RANGE_SLOT : CAPACITY_SLOT;

			if (!getSlot(target).hasItem() && moveItemStackTo(stack, target, target + 1, false)) {
				slot.setChanged();
				return ItemStack.EMPTY;
			}
		}

		// Alles andere (Inventar oder Crafting-Raster): ins Netzwerk einlagern – nur auf dem Server
		if (terminal != null && hasPermission(StorageTerminalBlockEntity.Permission.ITEMS)) {
			ItemStack rest = network().insert(stack.copy());

			if (!rest.isEmpty() && slotIndex >= CRAFT_START && slotIndex < CRAFT_END) {
				// Netzwerk voll: Rasterinhalt wenigstens ins Inventar
				moveItemStackTo(rest, PLAYER_START, PLAYER_END, false);
			}

			slot.set(rest);
			slot.setChanged();
			forceSync = true;
		}

		return ItemStack.EMPTY;
	}

	@Override
	public void removed(Player player) {
		super.removed(player);

		if (craftSlots == null || terminal == null) {
			return; // Client: der Server kümmert sich um den Rasterinhalt
		}

		// Rasterinhalt zurück ins Netzwerk, Rest ins Inventar
		for (int i = 0; i < craftSlots.getContainerSize(); i++) {
			ItemStack stack = craftSlots.removeItemNoUpdate(i);

			if (!stack.isEmpty()) {
				craftSlots.setItem(i, network().insert(stack));
			}
		}

		clearContainer(player, craftSlots);
	}

	@Override
	public boolean canTakeItemForPickAll(ItemStack carried, Slot slot) {
		return slot.container != resultSlots && super.canTakeItemForPickAll(carried, slot);
	}

	@Override
	public boolean stillValid(Player player) {
		if (terminal == null) {
			return true;
		}

		if (terminal.isRemoved() || !terminal.canAccess(player)) {
			return false;
		}

		if (wireless && terminal.hasEnderUpgrade()) {
			return true; // grenzenlos, auch aus anderen Dimensionen
		}

		if (terminal.getLevel() != player.level()) {
			return false;
		}

		if (wireless) {
			double range = terminal.getWirelessRange();
			return terminal.getBlockPos().distToCenterSqr(player.position()) <= range * range;
		}

		return Container.stillValidBlockEntity(terminal, player);
	}

	// ------------------------------------------------------------------
	// Client
	// ------------------------------------------------------------------

	/** Vom Netzwerk-Paket aufgerufen (nur Client). */
	public void setClientContents(List<StorageNetwork.Entry> entries, TerminalContentsPayload.NetworkInfo info,
			TerminalContentsPayload.AccessInfo access) {
		this.clientEntries = List.copyOf(entries);
		this.clientInfo = info;
		this.clientAccess = access;
		this.clientVersion++;
	}

	public List<StorageNetwork.Entry> getClientEntries() {
		return clientEntries;
	}

	public TerminalContentsPayload.NetworkInfo getClientInfo() {
		return clientInfo;
	}

	public TerminalContentsPayload.AccessInfo getClientAccess() {
		return clientAccess;
	}

	/** Steigt bei jedem neuen Inhalt – der Screen filtert dann neu. */
	public int getClientVersion() {
		return clientVersion;
	}

	// ------------------------------------------------------------------

	private class UpgradeSlot extends Slot {
		private final UpgradeItem.Kind kind;
		private final Identifier icon;

		UpgradeSlot(Container container, int index, int y, UpgradeItem.Kind kind, Identifier icon) {
			super(container, index, UPGRADE_SLOT_X, y);
			this.kind = kind;
			this.icon = icon;
		}

		@Override
		public boolean mayPlace(ItemStack stack) {
			return UpgradeItem.is(stack, kind) && hasPermission(StorageTerminalBlockEntity.Permission.UPGRADES);
		}

		@Override
		public boolean mayPickup(Player player) {
			return hasPermission(StorageTerminalBlockEntity.Permission.UPGRADES);
		}

		@Override
		public int getMaxStackSize() {
			return 1;
		}

		@Override
		public Identifier getNoItemIcon() {
			return icon;
		}
	}
}
