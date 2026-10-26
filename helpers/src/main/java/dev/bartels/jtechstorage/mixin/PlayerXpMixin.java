package dev.bartels.jtechstorage.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import dev.bartels.jtechstorage.companion.CompanionContent;

/** Companion (v0.25): collected experience is shared with the companion (default: half). */
@Mixin(Player.class)
public abstract class PlayerXpMixin {
	@ModifyVariable(method = "giveExperiencePoints", at = @At("HEAD"), argsOnly = true)
	private int jtechstorage$shareWithCompanion(int amount) {
		if (amount > 0 && (Object) this instanceof ServerPlayer player) {
			return CompanionContent.shareXp(player, amount);
		}

		return amount;
	}
}
