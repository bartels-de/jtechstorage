package dev.bartels.jtechstorage.mixin.client;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.TooltipRenderUtil;
import net.minecraft.resources.Identifier;

import dev.bartels.jtechstorage.client.gui.GuiTheme;

/** Oberfläche (v0.29): Tooltips in Mod-Fenstern bekommen den Rahmen des gewählten Designs (eigene Item-Stile haben Vorrang). */
@Mixin(TooltipRenderUtil.class)
public abstract class TooltipStyleMixin {
	@ModifyVariable(method = "extractTooltipBackground", at = @At("HEAD"), argsOnly = true)
	private static @Nullable Identifier jtechstorage$themedTooltip(@Nullable Identifier style) {
		if (style != null) {
			return style;
		}

		Screen screen = Minecraft.getInstance().gui.screen();

		if (screen != null && screen.getClass().getName().startsWith("dev.bartels.jtechstorage.")) {
			return GuiTheme.tooltipStyle();
		}

		return null;
	}
}
