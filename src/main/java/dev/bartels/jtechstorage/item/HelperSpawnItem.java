package dev.bartels.jtechstorage.item;

import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;

import dev.bartels.jtechstorage.entity.NetworkHelperMob;
import dev.bartels.jtechstorage.entity.StorageGriffin;

/** Setzt einen Netzwerk-Helfer (z. B. Sammel-Falter) auf den angeklickten Block. */
public class HelperSpawnItem extends Item {
	private final Supplier<EntityType<? extends NetworkHelperMob>> type;

	public HelperSpawnItem(Supplier<EntityType<? extends NetworkHelperMob>> type, Properties properties) {
		super(properties);
		this.type = type;
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		if (!(context.getLevel() instanceof ServerLevel level)) {
			return InteractionResult.SUCCESS;
		}

		ItemStack stack = context.getItemInHand();
		Player player = context.getPlayer();
		BlockPos pos = context.getClickedPos().relative(context.getClickedFace());
		NetworkHelperMob mob = type.get().spawn(level, stack, player, pos, EntitySpawnReason.SPAWN_ITEM_USE, true, false);

		if (mob == null) {
			return InteractionResult.FAIL;
		}

		if (player != null) {
			mob.setOwner(player);
		}

		if (stack.has(DataComponents.CUSTOM_NAME)) {
			mob.setCustomName(stack.get(DataComponents.CUSTOM_NAME));
		}

		mob.applyItemCosmetics(stack);

		if (mob instanceof StorageGriffin griffin) {
			griffin.loadFromItem(stack);
		}

		mob.playSound(SoundEvents.AMETHYST_BLOCK_RESONATE, 1.0f, 1.3f);
		stack.consume(1, player);
		return InteractionResult.SUCCESS;
	}
}
