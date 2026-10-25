package dev.bartels.jtechstorage.depths.temple;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import dev.bartels.jtechstorage.ModContent;

/**
 * Siegel der Lagermeister (v0.39): steht in der Mitte des Tempels. Rechtsklick mit einem Amethyst-Kern weckt den
 * letzten Lagermeister (der Kern wird verbraucht). Solange er lebt, lässt sich kein zweiter rufen.
 */
public class MasterSealBlock extends Block {
	public MasterSealBlock(Properties properties) {
		super(properties);
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
			BlockHitResult hit) {
		if (!stack.is(ModContent.AMETHYST_CORE)) {
			if (!level.isClientSide()) {
				player.sendOverlayMessage(Component.translatable("block.jtechstorage.master_seal.hint").withStyle(ChatFormatting.LIGHT_PURPLE));
			}

			return InteractionResult.TRY_WITH_EMPTY_HAND;
		}

		if (!(level instanceof ServerLevel server)) {
			return InteractionResult.SUCCESS;
		}

		if (!server.getEntitiesOfClass(LastMaster.class, new net.minecraft.world.phys.AABB(pos).inflate(64)).isEmpty()) {
			player.sendOverlayMessage(Component.translatable("block.jtechstorage.master_seal.awake").withStyle(ChatFormatting.RED));
			return InteractionResult.FAIL;
		}

		LastMaster master = TempleContent.LAST_MASTER.create(server, EntitySpawnReason.TRIGGERED);

		if (master == null) {
			return InteractionResult.FAIL;
		}

		stack.consume(1, player);
		master.snapTo(pos.getX() + 0.5, pos.getY() + 2.5, pos.getZ() + 0.5, player.getYRot() + 180.0f, 0.0f);
		master.onSummoned(pos);
		master.setTarget(player);
		server.addFreshEntity(master);
		server.sendParticles(ParticleTypes.SOUL, pos.getX() + 0.5, pos.getY() + 2.0, pos.getZ() + 0.5, 80, 0.8, 1.5, 0.8, 0.05);
		server.sendParticles(ParticleTypes.REVERSE_PORTAL, pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5, 120, 1.0, 1.0, 1.0, 0.3);
		server.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.HOSTILE, 3.0f, 0.4f);
		return InteractionResult.SUCCESS;
	}

	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		if (random.nextInt(2) == 0) {
			double a = random.nextDouble() * Math.PI * 2;
			level.addParticle(ParticleTypes.SOUL_FIRE_FLAME, pos.getX() + 0.5 + Math.cos(a) * 0.6, pos.getY() + 1.05, pos.getZ() + 0.5 + Math.sin(a) * 0.6,
					0, 0.03, 0);
		}
	}
}
