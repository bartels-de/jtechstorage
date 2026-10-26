package dev.bartels.jtechstorage.client.jei;

import java.util.List;
import java.util.Optional;

import net.minecraft.client.renderer.Rect2i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
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
import net.minecraft.world.item.ItemStack;

/**
 * JEI integration (only loaded if JEI is installed – Fabric entrypoint "jei_mod_plugin").
 *
 * <ul>
 *     <li>"+" in JEI places recipes into the Crafting Terminal – ingredients from network and inventory</li>
 *     <li>R/U/bookmarks also work on the items in the network grid</li>
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

	/** Info pages for items without a recipe (loot, evolutions): the U/R key in JEI shows where they come from. */
	@Override
	public void registerRecipes(mezz.jei.api.registration.IRecipeRegistration registration) {
		info(registration, "amethyst_core");
		info(registration, "wireless_core");
		info(registration, "iron_range_upgrade");
		info(registration, "iron_capacity_upgrade");
		info(registration, "amethyst_obsidian_upgrade_smithing_template");
		info(registration, "peter_chel_parker");
		info(registration, "cheldrogan");
		info(registration, "amethyst_phoenix");
		info(registration, "guide_book");
	}

	private static void info(mezz.jei.api.registration.IRecipeRegistration registration, String id) {
		BuiltInRegistries.ITEM.getOptional(JTechStorage.id(id)).ifPresent(item ->
				registration.addItemStackInfo(new ItemStack(item),
						Component.translatable("jei.jtechstorage.info." + id)));
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

			@Override
			public List<Rect2i> getGuiExtraAreas(StorageTerminalScreen screen) {
				return screen.getExtraAreas();
			}
		});
	}
}
