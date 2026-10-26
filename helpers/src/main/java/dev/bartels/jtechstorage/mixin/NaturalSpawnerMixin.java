package dev.bartels.jtechstorage.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.NaturalSpawner;

import dev.bartels.jtechstorage.entity.MothWards;

/** Watch moth: no natural monster spawns inside the watch area of an awake moth ({@link MothWards}). */
@Mixin(NaturalSpawner.class)
public abstract class NaturalSpawnerMixin {
	@Inject(method = "isValidPositionForMob", at = @At("HEAD"), cancellable = true)
	private static void jtechstorage$mothWard(ServerLevel level, Mob mob, double distance, CallbackInfoReturnable<Boolean> cir) {
		if (mob instanceof Enemy && MothWards.protects(level, mob.blockPosition())) {
			cir.setReturnValue(false);
		}
	}
}
