package dev.bartels.jtechstorage.mixin;

import java.util.UUID;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.world.entity.monster.zombie.ZombieVillager;

/** Access to the curing of zombie villagers – the quokka (v0.33) starts it. */
@Mixin(ZombieVillager.class)
public interface ZombieVillagerAccessor {
	@Invoker("startConverting")
	void jtechstorage$startConverting(UUID healer, int ticks);
}
