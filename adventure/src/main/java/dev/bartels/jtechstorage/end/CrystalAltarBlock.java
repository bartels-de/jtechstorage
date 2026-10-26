package dev.bartels.jtechstorage.end;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Crystal Altar in the heart of the Crystal Lair. When a player enters the lair for the first time, the Great
 * Amethyst Spider awakens by itself. After that, you summon it again with a Crystal Cocoon (right-click). Unbreakable.
 */
public class CrystalAltarBlock extends Block implements EntityBlock {
	private static final VoxelShape SHAPE = Shapes.or(
			Block.box(1, 0, 1, 15, 4, 15),
			Block.box(3, 4, 3, 13, 10, 13),
			Block.box(2, 10, 2, 14, 13, 14));

	public CrystalAltarBlock(Properties properties) {
		super(properties);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new CrystalAltarBlockEntity(pos, state);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		if (level.isClientSide() || type != EndContent.CRYSTAL_ALTAR_BLOCK_ENTITY) {
			return null;
		}

		return (BlockEntityTicker<T>) (BlockEntityTicker<CrystalAltarBlockEntity>) CrystalAltarBlockEntity::serverTick;
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
			InteractionHand hand, BlockHitResult hit) {
		if (!stack.is(EndContent.CRYSTAL_COCOON)) {
			return InteractionResult.TRY_WITH_EMPTY_HAND;
		}

		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}

		if (!(level.getBlockEntity(pos) instanceof CrystalAltarBlockEntity altar) || !(player instanceof ServerPlayer serverPlayer)) {
			return InteractionResult.PASS;
		}

		if (altar.spiderNearby()) {
			player.sendOverlayMessage(Component.translatable("jtechstorage.altar.busy"));
			return InteractionResult.FAIL;
		}

		stack.consume(1, player);
		altar.summon((ServerLevel) level, serverPlayer);
		return InteractionResult.SUCCESS;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!level.isClientSide()) {
			player.sendOverlayMessage(Component.translatable("jtechstorage.altar.hint"));
		}

		return InteractionResult.SUCCESS;
	}

	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		if (random.nextInt(2) == 0) {
			level.addParticle(ParticleTypes.END_ROD, pos.getX() + 0.2 + random.nextDouble() * 0.6, pos.getY() + 0.9,
					pos.getZ() + 0.2 + random.nextDouble() * 0.6, 0.0, 0.03, 0.0);
		}

		if (random.nextInt(4) == 0) {
			level.addParticle(ParticleTypes.PORTAL, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5,
					(random.nextDouble() - 0.5) * 1.5, -0.3, (random.nextDouble() - 0.5) * 1.5);
		}
	}
}
