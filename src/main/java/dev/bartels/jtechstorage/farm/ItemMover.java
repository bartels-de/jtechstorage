package dev.bartels.jtechstorage.farm;

import java.util.function.Predicate;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.HopperBlockEntity;

/** Item-Transport für Amethyst-Trichter und Kristall-Schieber (v0.27.0): respektiert Seiten (Öfen, Brauerei …) wie ein Trichter. */
final class ItemMover {
	/** Items pro Schritt (alle {@link #INTERVAL} Ticks) – 16-mal so schnell wie ein Trichter. */
	static final int PER_STEP = 8;
	static final int INTERVAL = 4;

	private ItemMover() {
	}

	static @Nullable Container containerAt(Level level, BlockPos pos) {
		return HopperBlockEntity.getContainerAt(level, pos);
	}

	/**
	 * Verschiebt bis zu {@code max} Items von {@code src} nach {@code dst}.
	 *
	 * @param extractFace Seite von src, über die entnommen wird
	 * @param insertFace  Seite von dst, über die eingefügt wird
	 */
	static int move(Container src, Direction extractFace, Container dst, Direction insertFace, int max, Predicate<ItemStack> filter) {
		int moved = 0;
		int size = src.getContainerSize();
		int[] slots = src instanceof WorldlyContainer worldly ? worldly.getSlotsForFace(extractFace) : null;
		int count = slots != null ? slots.length : size;

		for (int k = 0; k < count && moved < max; k++) {
			int slot = slots != null ? slots[k] : k;
			ItemStack stack = src.getItem(slot);

			if (stack.isEmpty() || !filter.test(stack) || !src.canTakeItem(dst, slot, stack)
					|| src instanceof WorldlyContainer worldly && !worldly.canTakeItemThroughFace(slot, stack, extractFace)) {
				continue;
			}

			int n = Math.min(max - moved, stack.getCount());
			ItemStack rest = HopperBlockEntity.addItem(src, dst, stack.copyWithCount(n), insertFace);
			int done = n - rest.getCount();

			if (done > 0) {
				ItemStack left = stack.copy();
				left.shrink(done);
				src.setItem(slot, left.isEmpty() ? ItemStack.EMPTY : left);
				moved += done;
			}
		}

		if (moved > 0) {
			src.setChanged();
			dst.setChanged();
		}

		return moved;
	}
}
