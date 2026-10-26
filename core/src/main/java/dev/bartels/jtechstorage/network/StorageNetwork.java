package dev.bartels.jtechstorage.network;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.AABB;

import dev.bartels.jtechstorage.ModContent;
import dev.bartels.jtechstorage.entity.SortCategory;

/**
 * A snapshot of all containers linked to a terminal.
 *
 * <p>Chests are linked with the storage key. Only links
 * within capacity (order = order of adding), in range and in loaded
 * chunks are considered. For double chests the second half is included automatically.
 *
 * <p><b>Sorting rules:</b> If a frame with an item hangs on a chest, that chest only accepts
 * that item – and with priority over all other chests. Several frames = several allowed items.
 */
public final class StorageNetwork {
	public static final StorageNetwork EMPTY = new StorageNetwork(List.of(), Map.of(), Map.of(), Map.of(), 0, 0, 0);

	private final List<Container> containers;
	/** Chests with a sorting rule → allowed items (identity map). */
	private final Map<Container, List<ItemStack>> filters;
	/** Container → linked position (the same for both halves of double chests). */
	private final Map<Container, BlockPos> keys;
	/** Container → fixed sorting category (assigned by the Amethyst Sprite). */
	private final Map<Container, SortCategory> categories;
	private final int linkCount;
	private final int unavailable;
	private final int overCapacity;

	private StorageNetwork(List<Container> containers, Map<Container, List<ItemStack>> filters,
			Map<Container, BlockPos> keys, Map<Container, SortCategory> categories,
			int linkCount, int unavailable, int overCapacity) {
		this.containers = containers;
		this.filters = filters;
		this.keys = keys;
		this.categories = categories;
		this.linkCount = linkCount;
		this.unavailable = unavailable;
		this.overCapacity = overCapacity;
	}

	/**
	 * Builds the network from the linked positions.
	 *
	 * @param invalid called for positions where (in loaded chunks) there is no chest anymore
	 */
	public static StorageNetwork collect(Level level, BlockPos center, List<BlockPos> links, int range, int capacity,
			Map<BlockPos, SortCategory> assignments, Consumer<BlockPos> invalid) {
		List<Container> containers = new ArrayList<>();
		Map<Container, List<ItemStack>> filters = new IdentityHashMap<>();
		Map<Container, BlockPos> keys = new IdentityHashMap<>();
		Map<Container, SortCategory> categories = new IdentityHashMap<>();
		Set<Container> seen = Collections.newSetFromMap(new IdentityHashMap<>());
		double maxDistSqr = (double) range * range;
		int unavailable = 0;
		int overCapacity = 0;
		int index = 0;

		for (BlockPos pos : links) {
			if (index++ >= capacity) {
				overCapacity++;
				continue;
			}

			if (!level.isLoaded(pos)) {
				unavailable++;
				continue;
			}

			BlockState state = level.getBlockState(pos);

			if (!state.is(ModContent.NETWORK_STORAGE) || !(level.getBlockEntity(pos) instanceof Container container)) {
				invalid.accept(pos);
				continue;
			}

			if (pos.distSqr(center) > maxDistSqr) {
				unavailable++;
				continue;
			}

			BlockPos partner = chestPartner(state, pos);
			List<ItemStack> filter = frameFilter(level, pos, partner);
			SortCategory category = assignments.get(pos);

			if (seen.add(container)) {
				containers.add(container);
				keys.put(container, pos);

				if (!filter.isEmpty()) {
					filters.put(container, filter);
				}

				if (category != null) {
					categories.put(container, category);
				}
			}

			if (partner != null && level.isLoaded(partner) && level.getBlockEntity(partner) instanceof Container other
					&& seen.add(other)) {
				containers.add(other);
				keys.put(other, pos);

				if (!filter.isEmpty()) {
					filters.put(other, filter);
				}

				if (category != null) {
					categories.put(other, category);
				}
			}
		}

		return new StorageNetwork(containers, filters, keys, categories, links.size(), unavailable, overCapacity);
	}

	/** Items in frames hanging on this chest (or its second half). */
	public static List<ItemStack> frameFilter(Level level, BlockPos pos, @Nullable BlockPos partner) {
		AABB area = new AABB(pos).inflate(1.0);

		if (partner != null) {
			area = area.minmax(new AABB(partner).inflate(1.0));
		}

		List<ItemStack> items = new ArrayList<>();

		for (ItemFrame frame : level.getEntitiesOfClass(ItemFrame.class, area, frame -> !frame.getItem().isEmpty())) {
			BlockPos support = frame.getPos().relative(frame.getDirection().getOpposite());

			if (support.equals(pos) || support.equals(partner)) {
				items.add(frame.getItem().copyWithCount(1));
			}
		}

		return items;
	}

	/** All reachable containers (in link order). */
	public List<Container> containers() {
		return containers;
	}

	/** Linked position of a container (double chest: shared position). */
	public @Nullable BlockPos keyOf(Container container) {
		return keys.get(container);
	}

	/** Fixed sorting category of a container or {@code null}. */
	public @Nullable SortCategory categoryOf(Container container) {
		return categories.get(container);
	}

	/** Sorting rule (frame items) of a container or {@code null}. */
	public @Nullable List<ItemStack> filterOf(Container container) {
		return filters.get(container);
	}

	/** May {@code stack} go into this container at all (shulker rule, canPlaceItem)? */
	public static boolean canHold(Container container, ItemStack stack) {
		if (!accepts(container, stack)) {
			return false;
		}

		for (int slot = 0; slot < container.getContainerSize(); slot++) {
			ItemStack existing = container.getItem(slot);

			if (existing.isEmpty() ? container.canPlaceItem(slot, stack)
					: ItemStack.isSameItemSameComponents(existing, stack) && existing.getCount() < container.getMaxStackSize(existing)) {
				return true;
			}
		}

		return false;
	}

	/** Puts {@code stack} into exactly this one container (modifies {@code stack}). */
	public static void insertInto(Container container, ItemStack stack) {
		insertInto(List.of(container), stack);
	}

	/** Number of chests with a sorting rule. */
	public int sortRuleCount() {
		return filters.size();
	}

	/** Second half of a double chest or {@code null}. */
	public static @Nullable BlockPos chestPartner(BlockState state, BlockPos pos) {
		if (state.getBlock() instanceof ChestBlock && state.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
			return pos.relative(ChestBlock.getConnectedDirection(state));
		}

		return null;
	}

	/** Number of linked chests (including unreachable ones). */
	public int linkCount() {
		return linkCount;
	}

	/** Linked chests that are currently not loaded or out of range. */
	public int unavailable() {
		return unavailable;
	}

	/** Linked chests beyond capacity (e.g. after removing an upgrade). */
	public int overCapacity() {
		return overCapacity;
	}

	public int containerCount() {
		return containers.size();
	}

	public int totalSlots() {
		int total = 0;

		for (Container container : containers) {
			total += container.getContainerSize();
		}

		return total;
	}

	public int usedSlots() {
		int used = 0;

		for (Container container : containers) {
			for (int slot = 0; slot < container.getContainerSize(); slot++) {
				if (!container.getItem(slot).isEmpty()) {
					used++;
				}
			}
		}

		return used;
	}

	/** Extracts up to {@code amount} items of the same kind – components (durability, names …) ignored. */
	public ItemStack extractAnyVariant(ItemStack prototype, int amount) {
		for (Container container : containers) {
			for (int slot = 0; slot < container.getContainerSize(); slot++) {
				ItemStack stack = container.getItem(slot);

				if (!stack.isEmpty() && ItemStack.isSameItem(stack, prototype)) {
					return extract(stack.copyWithCount(1), amount);
				}
			}
		}

		return ItemStack.EMPTY;
	}

	/**
	 * How many items of this kind still fit? (Frame chests for other items don't count.)
	 * Stops as soon as {@code limit} is reached.
	 */
	public long spaceFor(ItemStack stack, long limit) {
		long space = 0;

		for (Container container : containers) {
			List<ItemStack> filter = filters.get(container);

			if (filter != null && filter.stream().noneMatch(item -> ItemStack.isSameItem(item, stack)) || !accepts(container, stack)) {
				continue;
			}

			for (int slot = 0; slot < container.getContainerSize(); slot++) {
				ItemStack existing = container.getItem(slot);

				if (existing.isEmpty()) {
					if (container.canPlaceItem(slot, stack)) {
						space += container.getMaxStackSize(stack);
					}
				} else if (ItemStack.isSameItemSameComponents(existing, stack)) {
					space += Math.max(0, container.getMaxStackSize(existing) - existing.getCount());
				}

				if (space >= limit) {
					return space;
				}
			}
		}

		return space;
	}

	/** Total amount of an item in the network. */
	public long count(ItemStack prototype) {
		long total = 0;

		for (Container container : containers) {
			for (int slot = 0; slot < container.getContainerSize(); slot++) {
				ItemStack stack = container.getItem(slot);

				if (!stack.isEmpty() && ItemStack.isSameItemSameComponents(stack, prototype)) {
					total += stack.getCount();
				}
			}
		}

		return total;
	}

	/**
	 * Combines all items in the network by item + components.
	 */
	public List<Entry> aggregate() {
		Map<Key, long[]> totals = new LinkedHashMap<>();

		for (Container container : containers) {
			for (int slot = 0; slot < container.getContainerSize(); slot++) {
				ItemStack stack = container.getItem(slot);

				if (stack.isEmpty()) {
					continue;
				}

				totals.computeIfAbsent(new Key(stack.copyWithCount(1)), k -> new long[1])[0] += stack.getCount();
			}
		}

		List<Entry> entries = new ArrayList<>(totals.size());
		totals.forEach((key, total) -> entries.add(new Entry(key.stack(), total[0])));
		return entries;
	}

	/**
	 * Extracts up to {@code amount} items matching {@code prototype}.
	 *
	 * @return the extracted items (may be empty or smaller than requested)
	 */
	public ItemStack extract(ItemStack prototype, int amount) {
		ItemStack result = ItemStack.EMPTY;
		int remaining = Math.min(amount, prototype.getMaxStackSize());

		for (Container container : containers) {
			boolean changed = false;

			for (int slot = 0; slot < container.getContainerSize() && remaining > 0; slot++) {
				ItemStack stack = container.getItem(slot);

				if (stack.isEmpty() || !ItemStack.isSameItemSameComponents(stack, prototype)) {
					continue;
				}

				ItemStack taken = container.removeItem(slot, Math.min(remaining, stack.getCount()));

				if (taken.isEmpty()) {
					continue;
				}

				remaining -= taken.getCount();
				changed = true;

				if (result.isEmpty()) {
					result = taken;
				} else {
					result.grow(taken.getCount());
				}
			}

			if (changed) {
				container.setChanged();
			}

			if (remaining <= 0) {
				break;
			}
		}

		return result;
	}

	/**
	 * Stores {@code stack} into the network. First matching stacks are topped up,
	 * then empty slots are filled.
	 *
	 * @return what could not be stored (the passed stack is modified)
	 */
	public ItemStack insert(ItemStack stack) {
		if (stack.isEmpty()) {
			return ItemStack.EMPTY;
		}

		// 1. frame chests for this item, 2. chests of its category, 3. chests without a category,
		// 4. as a last resort chests of other categories. Frame chests for other items stay off limits.
		SortCategory category = categories.isEmpty() ? null : SortCategory.of(stack);
		List<Container> framed = new ArrayList<>();
		List<Container> sameCategory = new ArrayList<>();
		List<Container> unassigned = new ArrayList<>();
		List<Container> otherCategory = new ArrayList<>();

		for (Container container : containers) {
			List<ItemStack> filter = filters.get(container);

			if (filter != null) {
				if (filter.stream().anyMatch(item -> ItemStack.isSameItem(item, stack))) {
					framed.add(container);
				}
			} else {
				SortCategory assigned = categories.get(container);

				if (assigned == null) {
					unassigned.add(container);
				} else if (assigned == category) {
					sameCategory.add(container);
				} else {
					otherCategory.add(container);
				}
			}
		}

		insertInto(framed, stack);
		insertInto(sameCategory, stack);
		insertInto(unassigned, stack);
		insertInto(otherCategory, stack);
		return stack.isEmpty() ? ItemStack.EMPTY : stack;
	}

	/** First tops up matching stacks, then empty slots. Modifies {@code stack}. */
	private static void insertInto(List<Container> targets, ItemStack stack) {
		// 1. Top up existing stacks
		for (Container container : targets) {
			if (stack.isEmpty()) {
				return;
			}

			if (!accepts(container, stack)) {
				continue;
			}

			boolean changed = false;

			for (int slot = 0; slot < container.getContainerSize() && !stack.isEmpty(); slot++) {
				ItemStack existing = container.getItem(slot);

				if (existing.isEmpty() || !ItemStack.isSameItemSameComponents(existing, stack)) {
					continue;
				}

				int space = container.getMaxStackSize(existing) - existing.getCount();

				if (space > 0) {
					int moved = Math.min(space, stack.getCount());
					existing.grow(moved);
					stack.shrink(moved);
					changed = true;
				}
			}

			if (changed) {
				container.setChanged();
			}
		}

		// 2. Fill empty slots
		for (Container container : targets) {
			if (stack.isEmpty()) {
				return;
			}

			if (!accepts(container, stack)) {
				continue;
			}

			boolean changed = false;

			for (int slot = 0; slot < container.getContainerSize() && !stack.isEmpty(); slot++) {
				if (!container.getItem(slot).isEmpty() || !container.canPlaceItem(slot, stack)) {
					continue;
				}

				int moved = Math.min(container.getMaxStackSize(stack), stack.getCount());
				container.setItem(slot, stack.split(moved));
				changed = true;
			}

			if (changed) {
				container.setChanged();
			}
		}
	}

	private static boolean accepts(Container container, ItemStack stack) {
		// No shulker boxes (or similar) in shulker boxes – as in vanilla
		return !(container instanceof ShulkerBoxBlockEntity) || stack.getItem().canFitInsideContainerItems();
	}

	/** A combined entry: a prototype (count 1) and the total amount. */
	public record Entry(ItemStack prototype, long total) {
		public static final StreamCodec<RegistryFriendlyByteBuf, Entry> STREAM_CODEC = StreamCodec.composite(
				ItemStack.STREAM_CODEC, Entry::prototype,
				ByteBufCodecs.VAR_LONG, Entry::total,
				Entry::new);

		/** Compares contents (not identity) of two lists. */
		public static boolean sameContents(List<Entry> a, List<Entry> b) {
			if (a.size() != b.size()) {
				return false;
			}

			for (int i = 0; i < a.size(); i++) {
				Entry x = a.get(i);
				Entry y = b.get(i);

				if (x.total != y.total || !ItemStack.isSameItemSameComponents(x.prototype, y.prototype)) {
					return false;
				}
			}

			return true;
		}
	}

	/** Map key that compares items including components. */
	private record Key(ItemStack stack) {
		@Override
		public boolean equals(Object o) {
			return o instanceof Key other && ItemStack.isSameItemSameComponents(stack, other.stack);
		}

		@Override
		public int hashCode() {
			return ItemStack.hashItemAndComponents(stack);
		}
	}
}
