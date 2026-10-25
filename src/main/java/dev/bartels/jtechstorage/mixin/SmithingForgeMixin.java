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
 * Kristall-Schmiede (v0.40): Der Schmiedetisch kopiert beim Aufwerten (z. B. Netherit → Amethyst-Obsidian) alle
 * Komponenten – auch die von der Schmiede berechnete Haltbarkeit und die Attribute des alten Gegenstands. Deshalb werden
 * sie für das Ergebnis aus den Werten des neuen Gegenstands neu berechnet.
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
