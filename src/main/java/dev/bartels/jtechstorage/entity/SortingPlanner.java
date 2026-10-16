package dev.bartels.jtechstorage.entity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import dev.bartels.jtechstorage.network.StorageNetwork;

/**
 * Plant die nächste Aufgabe des Amethyst-Wichtels.
 *
 * <p>Jede Kiste bekommt eine <b>feste</b> Kategorie (gespeichert im Terminal). So bleibt eine Holz-Kiste eine
 * Holz-Kiste, auch wenn jemand 20 Werkzeuge hineinkippt – die werden dann wieder herausgeholt.
 * <ol>
 *     <li>Kisten mit Rahmen (Sortier-Regel) bekommen genau ihre Items – alles andere wird dort herausgeholt.</li>
 *     <li>Kisten ohne Kategorie, die Items enthalten, bekommen die Kategorie, die die meisten <b>Slots</b> belegt.</li>
 *     <li>Leere Kisten verlieren ihre Kategorie wieder und stehen als neue Kategorie-Kisten bereit.</li>
 *     <li>Items in einer Kiste fremder Kategorie wandern in eine Kiste ihrer Kategorie (oder in eine freie Kiste).</li>
 * </ol>
 * Ist nichts mehr umzuräumen, räumt der Wichtel Kisten <b>innen</b> auf: halbe Stapel zusammenführen und gleiche
 * Items nebeneinander legen (nach Kategorie, dann Item).
 */
public final class SortingPlanner {
	private SortingPlanner() {
	}

	/** Eine Transportaufgabe: Stapel aus einem Container holen und in den Zielcontainer bringen. */
	public record Task(Container source, BlockPos sourcePos, ItemStack prototype, Container target, BlockPos targetPos,
			SortCategory category) {
	}

	/** Eine Kiste, die innen aufgeräumt werden soll. */
	public record TidyTask(BlockPos pos, BlockPos key) {
	}

	/** Ziel für einen Stapel, den der Wichtel bereits trägt. */
	public record Destination(Container container, BlockPos pos) {
	}

	/** Eine Kiste (Doppeltruhe = beide Hälften) mit ihren Containern. */
	private static final class Group {
		final BlockPos key;
		final List<Container> containers = new ArrayList<>();
		final List<BlockPos> positions = new ArrayList<>();
		@Nullable List<ItemStack> filter;
		@Nullable SortCategory category;

		Group(BlockPos key) {
			this.key = key;
		}

		boolean isEmpty() {
			for (Container container : containers) {
				if (!container.isEmpty()) {
					return false;
				}
			}

			return true;
		}

		boolean canHold(ItemStack stack) {
			for (Container container : containers) {
				if (StorageNetwork.canHold(container, stack)) {
					return true;
				}
			}

			return false;
		}

		@Nullable Destination destination(ItemStack stack) {
			for (int i = 0; i < containers.size(); i++) {
				if (StorageNetwork.canHold(containers.get(i), stack)) {
					return new Destination(containers.get(i), positions.get(i));
				}
			}

			return null;
		}

		boolean accepts(ItemStack stack) {
			return filter != null && matchesFilter(filter, stack);
		}
	}

	public static @Nullable Task plan(StorageNetwork network, SortAssignments assignments, int startOffset) {
		List<Group> groups = analyse(network, assignments);

		if (groups.size() < 2) {
			return null;
		}

		for (int n = 0; n < groups.size(); n++) {
			Group source = groups.get(Math.floorMod(startOffset + n, groups.size()));

			for (int c = 0; c < source.containers.size(); c++) {
				Container container = source.containers.get(c);

				for (int slot = 0; slot < container.getContainerSize(); slot++) {
					ItemStack stack = container.getItem(slot);

					if (stack.isEmpty() || isHome(groups, source, stack)) {
						continue;
					}

					Destination target = findTarget(groups, source, stack, assignments);

					if (target != null) {
						return new Task(container, source.positions.get(c), stack.copyWithCount(1), target.container(),
								target.pos(), SortCategory.of(stack));
					}
				}
			}
		}

		return null;
	}

	/** Sucht eine Kiste, deren Inhalt unordentlich ist (halbe Stapel oder durcheinander). */
	public static @Nullable TidyTask planTidy(StorageNetwork network, SortAssignments assignments, int startOffset) {
		List<Group> groups = analyse(network, assignments);

		for (int n = 0; n < groups.size(); n++) {
			Group group = groups.get(Math.floorMod(startOffset + n, groups.size()));

			if (group.isEmpty()) {
				continue;
			}

			List<Container> containers = orderedContainers(group.containers);

			if (containers != null && !isTidy(containers)) {
				return new TidyTask(group.positions.get(0), group.key);
			}
		}

		return null;
	}

	/** Räumt die Kiste an dieser Position auf (Doppeltruhe = beide Hälften). Gibt die Anzahl bewegter Slots zurück. */
	public static int tidy(StorageNetwork network, BlockPos pos) {
		List<Container> group = new ArrayList<>();
		BlockPos key = null;

		for (Container container : network.containers()) {
			if (container instanceof BlockEntity blockEntity && blockEntity.getBlockPos().equals(pos)) {
				key = network.keyOf(container);
			}
		}

		if (key == null) {
			return 0;
		}

		for (Container container : network.containers()) {
			if (key.equals(network.keyOf(container))) {
				group.add(container);
			}
		}

		List<Container> containers = orderedContainers(group);

		if (containers == null) {
			return 0;
		}

		List<ItemStack> layout = layout(containers);
		int changed = 0;
		int index = 0;

		for (Container container : containers) {
			for (int slot = 0; slot < container.getContainerSize(); slot++, index++) {
				ItemStack wanted = layout.get(index);

				if (!ItemStack.matches(container.getItem(slot), wanted)) {
					container.setItem(slot, wanted);
					changed++;
				}
			}

			if (changed > 0) {
				container.setChanged();
			}
		}

		return changed;
	}

	/**
	 * Container in der Reihenfolge, wie das Truhen-GUI sie zeigt. {@code null}, wenn die Kiste nicht aufgeräumt
	 * werden soll (z. B. Öfen, Trichter – nur Kisten mit vollen 9er-Reihen).
	 */
	private static @Nullable List<Container> orderedContainers(List<Container> containers) {
		for (Container container : containers) {
			if (container.getContainerSize() % 9 != 0 || container instanceof BlockEntity blockEntity && blockEntity.isRemoved()) {
				return null;
			}
		}

		if (containers.size() == 2 && containers.get(0) instanceof BlockEntity first) {
			Level level = first.getLevel();
			BlockState state = first.getBlockState();

			if (level != null && state.getBlock() instanceof ChestBlock chestBlock) {
				Container combined = ChestBlock.getContainer(chestBlock, state, level, first.getBlockPos(), true);

				if (combined != null && combined.getContainerSize() == containers.get(0).getContainerSize() * 2) {
					return List.of(combined);
				}
			}
		}

		return containers;
	}

	private static boolean isTidy(List<Container> containers) {
		List<ItemStack> layout = layout(containers);
		int index = 0;

		for (Container container : containers) {
			for (int slot = 0; slot < container.getContainerSize(); slot++, index++) {
				if (!ItemStack.matches(container.getItem(slot), layout.get(index))) {
					return false;
				}
			}
		}

		return true;
	}

	/** Gewünschte Belegung: Stapel zusammengeführt, sortiert nach Kategorie → Item → Name → Menge. */
	private static List<ItemStack> layout(List<Container> containers) {
		List<ItemStack> merged = new ArrayList<>();
		int total = 0;

		for (Container container : containers) {
			total += container.getContainerSize();

			for (int slot = 0; slot < container.getContainerSize(); slot++) {
				ItemStack stack = container.getItem(slot);

				if (stack.isEmpty()) {
					continue;
				}

				ItemStack rest = stack.copy();

				for (ItemStack existing : merged) {
					if (rest.isEmpty()) {
						break;
					}

					if (ItemStack.isSameItemSameComponents(existing, rest) && existing.getCount() < existing.getMaxStackSize()) {
						int move = Math.min(rest.getCount(), existing.getMaxStackSize() - existing.getCount());
						existing.grow(move);
						rest.shrink(move);
					}
				}

				if (!rest.isEmpty()) {
					merged.add(rest);
				}
			}
		}

		merged.sort(Comparator.<ItemStack>comparingInt(stack -> SortCategory.of(stack).ordinal())
				.thenComparing(stack -> BuiltInRegistries.ITEM.getKey(stack.getItem()).toString())
				.thenComparing(stack -> stack.getHoverName().getString())
				.thenComparing(Comparator.<ItemStack>comparingInt(ItemStack::getCount).reversed()));

		List<ItemStack> layout = new ArrayList<>(total);

		for (int i = 0; i < total; i++) {
			layout.add(i < merged.size() ? merged.get(i) : ItemStack.EMPTY);
		}

		return layout;
	}

	/** Kategorie der Kiste an dieser Position (für die Anzeige mit dem Schlüssel). */
	public static @Nullable SortCategory categoryAt(SortAssignments assignments, BlockPos key) {
		return assignments.get(key);
	}

	/** Neues Ziel für einen getragenen Stapel. */
	public static @Nullable Destination destinationFor(StorageNetwork network, SortAssignments assignments, ItemStack stack) {
		return findTarget(analyse(network, assignments), null, stack, assignments);
	}

	/** Liegt der Stapel schon richtig? */
	private static boolean isHome(List<Group> groups, Group source, ItemStack stack) {
		if (source.filter != null) {
			return source.accepts(stack);
		}

		// Gibt es eine Rahmen-Kiste mit Platz für genau dieses Item, gehört es dorthin
		for (Group group : groups) {
			if (group != source && group.accepts(stack) && group.canHold(stack)) {
				return false;
			}
		}

		return source.category == SortCategory.of(stack);
	}

	private static @Nullable Destination findTarget(List<Group> groups, @Nullable Group source, ItemStack stack,
			SortAssignments assignments) {
		SortCategory category = SortCategory.of(stack);

		// 1. Rahmen-Kiste für genau dieses Item
		for (Group group : groups) {
			if (group != source && group.accepts(stack)) {
				Destination destination = group.destination(stack);

				if (destination != null) {
					return destination;
				}
			}
		}

		// 2. Kiste mit derselben Kategorie
		for (Group group : groups) {
			if (group != source && group.filter == null && group.category == category) {
				Destination destination = group.destination(stack);

				if (destination != null) {
					return destination;
				}
			}
		}

		// 3. Freie (leere, unzugeordnete) Kiste wird zur neuen Kategorie-Kiste
		for (Group group : groups) {
			if (group != source && group.filter == null && group.category == null && group.isEmpty()) {
				Destination destination = group.destination(stack);

				if (destination != null) {
					group.category = category;
					assignments.assign(group.key, category);
					return destination;
				}
			}
		}

		return null;
	}

	private static boolean matchesFilter(List<ItemStack> filter, ItemStack stack) {
		for (ItemStack item : filter) {
			if (ItemStack.isSameItem(item, stack)) {
				return true;
			}
		}

		return false;
	}

	/** Gruppiert Container nach Kiste und pflegt die Kategorie-Zuordnung. */
	private static List<Group> analyse(StorageNetwork network, SortAssignments assignments) {
		Map<BlockPos, Group> groups = new LinkedHashMap<>();

		for (Container container : network.containers()) {
			BlockPos key = network.keyOf(container);

			if (key == null || !(container instanceof BlockEntity blockEntity)) {
				continue;
			}

			Group group = groups.computeIfAbsent(key, Group::new);
			group.containers.add(container);
			group.positions.add(blockEntity.getBlockPos());
			group.filter = network.filterOf(container);
		}

		for (Group group : groups.values()) {
			group.category = assignments.get(group.key);

			if (group.filter != null) {
				// Rahmen-Kisten brauchen keine Kategorie
				if (group.category != null) {
					assignments.release(group.key);
					group.category = null;
				}

				continue;
			}

			if (group.isEmpty()) {
				if (group.category != null) {
					assignments.release(group.key);
					group.category = null;
				}

				continue;
			}

			if (group.category == null) {
				group.category = dominantBySlots(group);
				assignments.assign(group.key, group.category);
			}
		}

		return new ArrayList<>(groups.values());
	}

	/** Kategorie, die die meisten Slots belegt (Werkzeuge zählen so genauso viel wie Stapel). */
	private static SortCategory dominantBySlots(Group group) {
		Map<SortCategory, Integer> slots = new EnumMap<>(SortCategory.class);

		for (Container container : group.containers) {
			for (int slot = 0; slot < container.getContainerSize(); slot++) {
				ItemStack stack = container.getItem(slot);

				if (!stack.isEmpty()) {
					slots.merge(SortCategory.of(stack), 1, Integer::sum);
				}
			}
		}

		SortCategory best = SortCategory.OTHER;
		int bestCount = 0;

		for (Map.Entry<SortCategory, Integer> entry : slots.entrySet()) {
			if (entry.getValue() > bestCount) {
				bestCount = entry.getValue();
				best = entry.getKey();
			}
		}

		return best;
	}
}
