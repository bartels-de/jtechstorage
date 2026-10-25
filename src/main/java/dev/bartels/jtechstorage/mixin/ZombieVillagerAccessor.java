package dev.bartels.jtechstorage.mixin;

import java.util.UUID;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.world.entity.monster.zombie.ZombieVillager;

/** Zugriff auf die Heilung von Zombie-Dorfbewohnern – das Quokka (v0.33) startet sie. */
@Mixin(ZombieVillager.class)
public interface ZombieVillagerAccessor {
	@Invoker("startConverting")
	void jtechstorage$startConverting(UUID healer, int ticks);
}
