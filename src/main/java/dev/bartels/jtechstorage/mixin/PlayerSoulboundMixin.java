package dev.bartels.jtechstorage.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import dev.bartels.jtechstorage.enchant.MoreEnchantments;

/** v0.42: Seelenband (Gegenstände vor dem Fallenlassen beiseitelegen) und Nährend (weniger Erschöpfung). */
@Mixin(Player.class)
public abstract class PlayerSoulboundMixin {
	@Inject(method = "dropEquipment", at = @At("HEAD"))
	private void jtechstorage$keepSoulbound(ServerLevel level, CallbackInfo ci) {
		if ((Object) this instanceof ServerPlayer player) {
			MoreEnchantments.keepSoulbound(player);
		}
	}

	@ModifyVariable(method = "causeFoodExhaustion", at = @At("HEAD"), argsOnly = true)
	private float jtechstorage$nourish(float exhaustion) {
		return MoreEnchantments.nourish((Player) (Object) this, exhaustion);
	}
}
