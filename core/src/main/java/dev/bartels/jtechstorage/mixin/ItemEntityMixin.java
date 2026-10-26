package dev.bartels.jtechstorage.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;

import dev.bartels.jtechstorage.backpack.BackpackItem;

/** Auto pickup: items already in a backpack go straight in when picked up. */
@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin {
	@Inject(method = "playerTouch", at = @At("HEAD"))
	private void jtechstorage$backpackPickup(Player player, CallbackInfo ci) {
		BackpackItem.tryAutoPickup((ItemEntity) (Object) this, player);
	}
}
