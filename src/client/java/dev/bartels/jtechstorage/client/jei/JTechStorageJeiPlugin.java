package dev.bartels.jtechstorage.client.jei;

import java.util.Optional;

import net.minecraft.client.renderer.Rect2i;
import net.minecraft.resources.Identifier;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.gui.builder.IClickableIngredientFactory;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import mezz.jei.api.runtime.IClickableIngredient;

import dev.bartels.jtechstorage.JTechStorage;
import dev.bartels.jtechstorage.client.StorageTerminalScreen;

/**
 * JEI-Anbindung (wird nur geladen, wenn JEI installiert ist – Fabric-Entrypoint "jei_mod_plugin").
 *
 * <ul>
 *     <li>„+“ in JEI legt Rezepte ins Crafting-Terminal – Zutaten aus Netzwerk und Inventar</li>
 *     <li>R/U/Lesezeichen funktionieren auch auf den Items im Netzwerk-Raster</li>
 * </ul>
 */
public class JTechStorageJeiPlugin implements IModPlugin {
	@Override
	public Identifier getPluginUid() {
		return JTechStorage.id("jei");
	}

	@Override
	public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {
		registration.addRecipeTransferHandler(new CraftingTerminalTransferHandler(registration.getTransferHelper()), RecipeTypes.CRAFTING);
	}

	@Override
	public void registerGuiHandlers(IGuiHandlerRegistration registration) {
		registration.addGuiContainerHandler(StorageTerminalScreen.class, new IGuiContainerHandler<StorageTerminalScreen>() {
			@Override
			public Optional<? extends IClickableIngredient<?>> getClickableIngredientUnderMouse(
					IClickableIngredientFactory builder, StorageTerminalScreen screen, double mouseX, double mouseY) {
				StorageTerminalScreen.HoveredIngredient hovered = screen.getIngredientUnderMouse(mouseX, mouseY);

				if (hovered == null) {
					return Optional.empty();
				}

				return builder.createBuilder(hovered.stack()).buildWithArea(new Rect2i(hovered.x(), hovered.y(), 16, 16));
			}
		});
	}
}
