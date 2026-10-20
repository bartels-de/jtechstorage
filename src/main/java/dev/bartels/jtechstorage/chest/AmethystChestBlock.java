package dev.bartels.jtechstorage.chest;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Prediction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Amethyst-Kiste (v0.28.0): Vorderseite zeigt zum Spieler und öffnet sich wie ein Fass. Rechtsklick mit einer höheren
 * Kiste wertet sie an Ort und Stelle auf (Inhalt bleibt, die alte Kiste bekommt man zurück).
 */
public class AmethystChestBlock extends Block implements EntityBlock {
	public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
	public static final BooleanProperty OPEN = BlockStateProperties.OPEN;

	private final AmethystChestTier tier;

	public AmethystChestBlock(AmethystChestTier tier, Properties properties) {
		super(properties);
		this.tier = tier;
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(OPEN, false));
	}

	public AmethystChestTier tier() {
		return tier;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, OPEN);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	@Override
	protected BlockState rotate(BlockState state, Rotation rotation) {
		return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
	}

	@Override
	protected BlockState mirror(BlockState state, Mirror mirror) {
		return state.rotate(mirror.getRotation(state.getValue(FACING)));
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new AmethystChestBlockEntity(pos, state);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (level instanceof ServerLevel server && level.getBlockEntity(pos) instanceof AmethystChestBlockEntity chest) {
			player.openMenu(chest);
			net.minecraft.world.entity.monster.piglin.PiglinAi.angerNearbyPiglins(server, player, true);
		}

		return InteractionResult.SUCCESS;
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
			BlockHitResult hit) {
		if (!(stack.getItem() instanceof BlockItem item && item.getBlock() instanceof AmethystChestBlock target && target.tier.ordinal() > tier.ordinal())) {
			return super.useItemOn(stack, state, level, pos, player, hand, hit);
		}

		if (level instanceof ServerLevel server && level.getBlockEntity(pos) instanceof AmethystChestBlockEntity chest) {
			// Aufwerten: Inhalt + Name übernehmen, alte Kiste zurückgeben
			List<ItemStack> contents = chest.copyContents();
			net.minecraft.network.chat.Component name = chest.getCustomName();
			chest.clearForUpgrade();
			level.setBlock(pos, target.defaultBlockState().setValue(FACING, state.getValue(FACING)), 3);

			if (level.getBlockEntity(pos) instanceof AmethystChestBlockEntity upgraded) {
				if (name != null) {
					// vor fill(): applyImplicitComponents setzt sonst den (leeren) CONTAINER-Inhalt
					ItemStack named = new ItemStack(target);
					named.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, name);
					upgraded.applyComponentsFromItemStack(named);
				}

				upgraded.fill(contents);
			}

			if (!player.getAbilities().instabuild) {
				stack.shrink(1);
				player.getInventory().placeItemBackInInventory(new ItemStack(this), Prediction.SERVER_ONLY);
			}

			level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 1.0f, 1.0f);
			server.sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 20, 0.4, 0.4, 0.4, 0.03);
		}

		return InteractionResult.SUCCESS;
	}

	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (level.getBlockEntity(pos) instanceof AmethystChestBlockEntity chest) {
			chest.recheckOpen();
		}
	}

	@Override
	protected boolean hasAnalogOutputSignal(BlockState state) {
		return true;
	}

	@Override
	protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
		return AbstractContainerMenu.getRedstoneSignalFromBlockEntity(level.getBlockEntity(pos));
	}
}
