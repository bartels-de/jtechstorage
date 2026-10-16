package dev.bartels.jtechstorage.item;

import org.jspecify.annotations.Nullable;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import dev.bartels.jtechstorage.ModContent;

/**
 * Altes Kristall-Paladin-Item (v0.15–v0.19). Im Inventar wird es sofort zu einem Peter-Chel-Parker-Item
 * (Daten bleiben erhalten), das Drachenei kommt zurück.
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

		ItemStack peter = stack.transmuteCopy(ModContent.PETER_CHEL_PARKER_ITEM, stack.getCount());
		int index = player.getInventory().findSlotMatchingItem(stack);

		if (index >= 0) {
			player.getInventory().setItem(index, peter);
		} else if (slot != null) {
			player.setItemSlot(slot, peter);
		} else {
			return;
		}

		player.getInventory().placeItemBackInInventory(new ItemStack(Items.DRAGON_EGG, stack.getCount()),
				net.minecraft.util.Prediction.SERVER_ONLY);
	}
}
