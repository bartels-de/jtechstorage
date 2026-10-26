package dev.bartels.jtechstorage.depths.memory;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/** A Memory Crystal (v0.36). Collectible – goes into the Memory Case. */
public class MemoryCrystalItem extends Item {
	public final int kind;

	public MemoryCrystalItem(int kind, Properties properties) {
		super(properties);
		this.kind = kind;
	}

	@Override
	public boolean isFoil(ItemStack stack) {
		return true;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
		tooltip.accept(Component.translatable("item.jtechstorage.memory_crystal." + MemoryContent.KINDS[kind] + ".lore")
				.withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
		tooltip.accept(Component.translatable("item.jtechstorage.memory_crystal.number", kind + 1, MemoryContent.KINDS.length)
				.withStyle(ChatFormatting.DARK_PURPLE));
	}
}
