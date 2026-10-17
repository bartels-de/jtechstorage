package dev.bartels.jtechstorage.end;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

import dev.bartels.jtechstorage.ModSounds;
import dev.bartels.jtechstorage.ModAdvancements;
import dev.bartels.jtechstorage.entity.AmethystSpider;

/** Merkt sich, ob die Spinne dieses Horts schon erwacht ist, und ruft sie herbei. */
public class CrystalAltarBlockEntity extends BlockEntity {
	/** So nah muss ein Spieler kommen, damit die Spinne beim ersten Besuch erwacht. */
	private static final double AWAKEN_RANGE = 20.0;
	private static final double BUSY_RANGE = 64.0;

	private boolean awakened;

	public CrystalAltarBlockEntity(BlockPos pos, BlockState state) {
		super(EndContent.CRYSTAL_ALTAR_BLOCK_ENTITY, pos, state);
	}

	public static void serverTick(Level level, BlockPos pos, BlockState state, CrystalAltarBlockEntity altar) {
		if (altar.awakened || level.getGameTime() % 20 != 7 || !(level instanceof ServerLevel serverLevel)) {
			return;
		}

		for (ServerPlayer player : serverLevel.players()) {
			if (!player.isSpectator() && !player.isCreative()
					&& player.distanceToSqr(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5) < AWAKEN_RANGE * AWAKEN_RANGE) {
				altar.summon(serverLevel, player);
				return;
			}
		}
	}

	public boolean spiderNearby() {
		return level != null && !level.getEntitiesOfClass(AmethystSpider.class, new AABB(worldPosition).inflate(BUSY_RANGE),
				AmethystSpider::isAlive).isEmpty();
	}

	/** Die Spinne seilt sich über dem Altar ab. */
	public void summon(ServerLevel level, ServerPlayer player) {
		awakened = true;
		setChanged();
		AmethystSpider spider = EndContent.AMETHYST_SPIDER.create(level, EntitySpawnReason.TRIGGERED);

		if (spider == null) {
			return;
		}

		double x = worldPosition.getX() + 0.5;
		double y = worldPosition.getY() + 1.0;
		double z = worldPosition.getZ() + 0.5;
		spider.snapTo(x, y + 8.0, z, player.getYRot() + 180.0f, 0.0f);
		spider.setHome(worldPosition);
		spider.setTarget(player);
		level.addFreshEntity(spider);

		level.playSound(null, worldPosition, SoundEvents.END_PORTAL_SPAWN, SoundSource.HOSTILE, 1.5f, 1.4f);
		level.playSound(null, worldPosition, ModSounds.SPIDER_AWAKEN, SoundSource.HOSTILE, 4.0f, 1.0f);
		level.sendParticles(ParticleTypes.REVERSE_PORTAL, x, y + 2, z, 120, 2.0, 3.0, 2.0, 0.2);
		level.sendParticles(ParticleTypes.END_ROD, x, y + 4, z, 40, 1.0, 4.0, 1.0, 0.05);

		for (ServerPlayer nearby : level.players()) {
			if (nearby.distanceToSqr(x, y, z) < 48 * 48) {
				nearby.sendSystemMessage(Component.translatable("jtechstorage.spider.summoned").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD));
				ModAdvancements.grant(nearby, "summon_amethyst_spider");
			}
		}
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putBoolean("Awakened", awakened);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		awakened = input.getBooleanOr("Awakened", false);
	}
}
