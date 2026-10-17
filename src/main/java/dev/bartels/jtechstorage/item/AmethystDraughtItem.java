package dev.bartels.jtechstorage.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import dev.bartels.jtechstorage.ModSounds;
import dev.bartels.jtechstorage.ModAdvancements;
import dev.bartels.jtechstorage.ModContent;
import dev.bartels.jtechstorage.entity.AmethystRobot;

/** Resonanz-Zünder: aktivieren weckt den Amethyst-Roboter ein Stück vor dir. Nur einer gleichzeitig in der Nähe. */
public class AmethystDraughtItem extends Item {
	private static final double SPAWN_DISTANCE = 14.0;

	public AmethystDraughtItem(Properties properties) {
		super(properties);
	}

	private static boolean bossNearby(Level level, Player player) {
		return !level.getEntitiesOfClass(AmethystRobot.class, new AABB(player.blockPosition()).inflate(96),
				dino -> dino.isAlive() && !dino.isHologram()).isEmpty();
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (bossNearby(level, player)) {
			if (!level.isClientSide()) {
				player.sendOverlayMessage(Component.translatable("jtechstorage.draught.already"));
			}

			return InteractionResult.FAIL;
		}

		return super.use(level, player, hand);
	}

	@Override
	public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
		ItemStack result = super.finishUsingItem(stack, level, entity);

		if (level instanceof ServerLevel serverLevel && entity instanceof ServerPlayer player && !bossNearby(level, player)) {
			summon(serverLevel, player);
		}

		return result;
	}

	private static void summon(ServerLevel level, ServerPlayer player) {
		AmethystRobot dino = ModContent.AMETHYST_ROBOT.create(level, EntitySpawnReason.TRIGGERED);

		if (dino == null) {
			return;
		}

		Vec3 look = player.getLookAngle();
		Vec3 flat = new Vec3(look.x, 0, look.z).normalize();
		double x = player.getX() + flat.x * SPAWN_DISTANCE;
		double z = player.getZ() + flat.z * SPAWN_DISTANCE;
		int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) Math.floor(x), (int) Math.floor(z));

		if (Math.abs(y - player.getY()) > 12) {
			x = player.getX();
			z = player.getZ();
			y = (int) player.getY();
		}

		float yaw = player.getYRot() + 180.0f;
		dino.snapTo(x, y, z, yaw, 0.0f);
		dino.setTarget(player);
		level.addFreshEntity(dino);

		LightningBolt bolt = EntityTypes.LIGHTNING_BOLT.create(level, EntitySpawnReason.TRIGGERED);

		if (bolt != null) {
			bolt.snapTo(x, y, z);
			bolt.setVisualOnly(true);
			level.addFreshEntity(bolt);
		}

		dino.playSound(SoundEvents.BEACON_ACTIVATE, 4.0f, 0.5f);
		dino.playSound(ModSounds.ROBOT_AWAKEN, 4.0f, 1.0f);
		player.sendSystemMessage(Component.translatable("jtechstorage.robot.summoned").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD));
		ModAdvancements.grant(player, "summon_amethyst_robot");
	}
}
