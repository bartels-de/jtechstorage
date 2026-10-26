package dev.bartels.jtechstorage.entity;

import dev.bartels.jtechstorage.helpers.HelpersContent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;


/**
 * The crystal paladin was removed in v0.20. This class only exists so paladins from old worlds
 * are not lost: on the first tick every paladin turns back into Peter Chel Parker
 * (owner, network, home, color and hat are kept) and the dragon egg drops out.
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
		PeterChelParker peter = HelpersContent.PETER_CHEL_PARKER.create(level, EntitySpawnReason.CONVERSION);

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
