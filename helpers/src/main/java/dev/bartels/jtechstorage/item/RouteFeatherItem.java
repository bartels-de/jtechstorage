package dev.bartels.jtechstorage.item;

import dev.bartels.jtechstorage.helpers.HelpersContent;
import org.jspecify.annotations.Nullable;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;


/** Old route feather (v0.20–v0.24). In the inventory it immediately becomes an empty flight plan. */
public class RouteFeatherItem extends Item {
	public RouteFeatherItem(Properties properties) {
		super(properties);
	}

	@Override
	public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, @Nullable EquipmentSlot slot) {
		if (!(entity instanceof Player player)) {
			return;
		}

		ItemStack plan = new ItemStack(HelpersContent.FLIGHT_PLAN, stack.getCount());
		int index = player.getInventory().findSlotMatchingItem(stack);

		if (index >= 0) {
			player.getInventory().setItem(index, plan);
		} else if (slot != null) {
			player.setItemSlot(slot, plan);
		}
	}
}
