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

	/** Infoseiten für Items ohne Rezept (Beute, Entwicklungen): Taste U/R in JEI zeigt, woher sie kommen. */
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
		net.minecraft.core.registries.BuiltInRegistries.ITEM.getOptional(JTechStorage.id(id)).ifPresent(item ->
				registration.addItemStackInfo(new net.minecraft.world.item.ItemStack(item),
						net.minecraft.network.chat.Component.translatable("jei.jtechstorage.info." + id)));
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
			public java.util.List<Rect2i> getGuiExtraAreas(StorageTerminalScreen screen) {
				return screen.getExtraAreas();
			}
		});
	}
}
