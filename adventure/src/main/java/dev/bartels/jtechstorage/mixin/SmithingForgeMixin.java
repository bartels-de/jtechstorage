package dev.bartels.jtechstorage.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.item.ItemStack;

import dev.bartels.jtechstorage.forge.ForgeContent;
import dev.bartels.jtechstorage.forge.ForgeUpgrade;

/**
 * Crystal Forge (v0.40): when upgrading (e.g. Netherite → Amethyst Obsidian), the smithing table copies all
 * components – including the durability and attributes of the old item computed by the forge. Therefore
 * they are recomputed for the result from the values of the new item.
 */
@Mixin(SmithingMenu.class)
public abstract class SmithingForgeMixin {
	@Inject(method = "createResult", at = @At("TAIL"))
	private void jtechstorage$refreshForged(CallbackInfo ci) {
		ItemStack result = ((AbstractContainerMenu) (Object) this).getSlot(SmithingMenu.RESULT_SLOT).getItem();

		if (!result.isEmpty() && result.has(ForgeContent.UPGRADES)) {
			ForgeUpgrade.refresh(result);
		}
	}
}
