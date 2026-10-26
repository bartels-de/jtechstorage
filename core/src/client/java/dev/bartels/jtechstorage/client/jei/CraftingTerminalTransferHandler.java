package dev.bartels.jtechstorage.client.jei;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.jspecify.annotations.Nullable;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.transfer.IRecipeTransferContext;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import mezz.jei.api.recipe.transfer.RecipeTransferResult;
import mezz.jei.api.recipe.types.IRecipeType;

import dev.bartels.jtechstorage.ModContent;
import dev.bartels.jtechstorage.menu.StorageTerminalMenu;
import dev.bartels.jtechstorage.net.CraftingTransferPayload;
import dev.bartels.jtechstorage.network.StorageNetwork;

/** Transfers JEI crafting recipes into the Crafting Terminal. The server does the actual work. */
public class CraftingTerminalTransferHandler implements IRecipeTransferHandler<StorageTerminalMenu, RecipeHolder<CraftingRecipe>> {
	private final IRecipeTransferHandlerHelper helper;

	public CraftingTerminalTransferHandler(IRecipeTransferHandlerHelper helper) {
		this.helper = helper;
	}

	@Override
	public Class<? extends StorageTerminalMenu> getContainerClass() {
		return StorageTerminalMenu.class;
	}

	@Override
	public Optional<MenuType<StorageTerminalMenu>> getMenuType() {
		return Optional.of(ModContent.STORAGE_TERMINAL_MENU);
	}

	@Override
	public IRecipeType<RecipeHolder<CraftingRecipe>> getRecipeType() {
		return RecipeTypes.CRAFTING;
	}

	@Override
	public @Nullable IRecipeTransferError transferRecipe(IRecipeTransferContext<RecipeHolder<CraftingRecipe>, StorageTerminalMenu> context,
			boolean doTransfer) {
		IRecipeTransferError error = transfer(context.getContainer(), context.getRecipeSlots(), context.getPlayer(), doTransfer);

		if (doTransfer && error == null) {
			context.completeRecipeTransfer(RecipeTransferResult.SUCCESS);
		}

		return error;
	}

	@Override
	@SuppressWarnings("removal")
	public @Nullable IRecipeTransferError transferRecipe(StorageTerminalMenu menu, RecipeHolder<CraftingRecipe> recipe,
			IRecipeSlotsView recipeSlots, Player player, boolean maxTransfer, boolean doTransfer) {
		return transfer(menu, recipeSlots, player, doTransfer);
	}

	private @Nullable IRecipeTransferError transfer(StorageTerminalMenu menu, IRecipeSlotsView recipeSlots, Player player, boolean doTransfer) {
		if (!menu.getLayout().hasCrafting()) {
			return helper.createUserErrorWithTooltip(Component.translatable("jtechstorage.jei.not_crafting"));
		}

		List<IRecipeSlotView> inputs = recipeSlots.getSlotViews(RecipeIngredientRole.INPUT);
		List<List<ItemStack>> options = new ArrayList<>();

		for (int i = 0; i < 9; i++) {
			if (i < inputs.size()) {
				options.add(inputs.get(i).getItemStacks()
						.filter(stack -> !stack.isEmpty())
						.limit(CraftingTransferPayload.MAX_ALTERNATIVES)
						.map(stack -> stack.copyWithCount(1))
						.toList());
			} else {
				options.add(List.of());
			}
		}

		// check whether everything is available (network + inventory) – rough, the server has the final say
		List<Available> available = new ArrayList<>();

		for (StorageNetwork.Entry entry : menu.getClientEntries()) {
			available.add(new Available(entry.prototype(), entry.total()));
		}

		Inventory inventory = player.getInventory();

		for (int slot = 0; slot < Inventory.INVENTORY_SIZE; slot++) {
			ItemStack stack = inventory.getItem(slot);

			if (!stack.isEmpty()) {
				available.add(new Available(stack, stack.getCount()));
			}
		}

		List<IRecipeSlotView> missing = new ArrayList<>();

		for (int i = 0; i < options.size(); i++) {
			List<ItemStack> slotOptions = options.get(i);

			if (!slotOptions.isEmpty() && !take(available, slotOptions) && i < inputs.size()) {
				missing.add(inputs.get(i));
			}
		}

		if (!missing.isEmpty()) {
			return helper.createUserErrorForMissingSlots(Component.translatable("jtechstorage.jei.missing"), missing);
		}

		if (doTransfer) {
			ClientPlayNetworking.send(new CraftingTransferPayload(menu.containerId, options));
		}

		return null;
	}

	private static boolean take(List<Available> available, List<ItemStack> options) {
		for (ItemStack option : options) {
			for (Available entry : available) {
				if (entry.count > 0 && ItemStack.isSameItem(entry.stack, option)) {
					entry.count--;
					return true;
				}
			}
		}

		return false;
	}

	private static final class Available {
		private final ItemStack stack;
		private long count;

		Available(ItemStack stack, long count) {
			this.stack = stack;
			this.count = count;
		}
	}
}
