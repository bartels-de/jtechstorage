package dev.bartels.jtechstorage.item;

import dev.bartels.jtechstorage.helpers.HelpersContent;
import net.minecraft.util.Prediction;
import org.jspecify.annotations.Nullable;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;


/**
 * Old crystal paladin item (v0.15–v0.19). In the inventory it immediately becomes a Peter Chel Parker item
 * (data is kept), and the dragon egg is returned.
 */
public class LegacyPaladinItem extends Item {
	public LegacyPaladinItem(Properties properties) {
		super(properties);
	}

	@Override
	public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, @Nullable EquipmentSlot slot) {
		if (!(entity instanceof Player player)) {
			return;
		}

		ItemStack peter = stack.transmuteCopy(HelpersContent.PETER_CHEL_PARKER_ITEM, stack.getCount());
		int index = player.getInventory().findSlotMatchingItem(stack);

		if (index >= 0) {
			player.getInventory().setItem(index, peter);
		} else if (slot != null) {
			player.setItemSlot(slot, peter);
		} else {
			return;
		}

		player.getInventory().placeItemBackInInventory(new ItemStack(Items.DRAGON_EGG, stack.getCount()),
				Prediction.SERVER_ONLY);
	}
}
