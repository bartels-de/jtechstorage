package dev.bartels.jtechstorage.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import dev.bartels.jtechstorage.ModContent;

/**
 * Der Kristall-Paladin wurde in v0.20 entfernt. Diese Klasse existiert nur noch, damit Paladine aus alten Welten
 * nicht verloren gehen: Beim ersten Tick verwandelt sich jeder Paladin zurück in Peter Chel Parker
 * (Besitzer, Netzwerk, Zuhause, Farbe und Hut bleiben) und das Drachenei fällt heraus.
 */
public class CrystalPaladin extends PeterChelParker {
	public CrystalPaladin(EntityType<? extends PeterChelParker> type, Level level) {
		super(type, level);
	}

	@Override
	public void tick() {
		if (level() instanceof ServerLevel level && !isRemoved()) {
			revert(level);
			discard();
			return;
		}

		super.tick();
	}

	@Override
	protected void revert(ServerLevel level) {
		PeterChelParker peter = ModContent.PETER_CHEL_PARKER.create(level, EntitySpawnReason.CONVERSION);

		if (peter != null) {
			peter.snapTo(getX(), getY(), getZ(), getYRot(), 0.0f);
			peter.owner = owner;
			peter.terminalPos = terminalPos;
			peter.home = home;
			peter.setAccentColor(getAccentColor());
			ItemStack hat = getItemBySlot(EquipmentSlot.HEAD);

			if (!hat.isEmpty()) {
				peter.setItemSlot(EquipmentSlot.HEAD, hat.copy());
			}

			level.addFreshEntity(peter);
		}

		spawnAtLocation(level, new ItemStack(Items.DRAGON_EGG));
	}
}
