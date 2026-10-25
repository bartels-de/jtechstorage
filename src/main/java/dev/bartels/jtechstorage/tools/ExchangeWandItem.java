package dev.bartels.jtechstorage.tools;

import java.util.List;
import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;

import dev.bartels.jtechstorage.item.LinkedTerminalItem;

/**
 * Austausch-Stab (v0.39): Schleichen + Rechtsklick auf einen Block merkt sich diesen Block. Rechtsklick tauscht dann
 * die zusammenhängende Fläche gleicher Blöcke (3×3 / 5×5 / 7×7, Schleichen + Rechtsklick in die Luft) gegen den
 * gemerkten aus. Die alten Blöcke kommen zurück (ins Lager, wenn gekoppelt), die neuen aus Inventar bzw. Lager.
 */
public class ExchangeWandItem extends LinkedTerminalItem {
	public ExchangeWandItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);

		if (!player.isSecondaryUseActive()) {
			return InteractionResult.PASS;
		}

		int next = BuildingWandItem.radius(stack) % 3 + 1;
		stack.set(ToolsContent.WAND_SIZE, next);

		if (!level.isClientSide()) {
			player.sendOverlayMessage(Component.translatable("jtechstorage.wand.size", 2 * next + 1, 2 * next + 1).withStyle(ChatFormatting.LIGHT_PURPLE));
		}

		return InteractionResult.SUCCESS;
	}

	/** Schleichen + Rechtsklick auf einen Block (kein Terminal): Block merken. */
	@Override
	protected InteractionResult useOnOtherBlock(UseOnContext context) {
		Player player = context.getPlayer();
		BlockState state = context.getLevel().getBlockState(context.getClickedPos());

		if (player == null || state.getBlock().asItem() == Items.AIR || state.hasBlockEntity()) {
			return InteractionResult.FAIL;
		}

		context.getItemInHand().set(ToolsContent.WAND_BLOCK, state);

		if (!context.getLevel().isClientSide()) {
			player.sendOverlayMessage(Component.translatable("jtechstorage.wand.selected", state.getBlock().getName()).withStyle(ChatFormatting.LIGHT_PURPLE));
		}

		return InteractionResult.SUCCESS;
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		Player player = context.getPlayer();

		if (player == null || player.isSecondaryUseActive()) {
			return super.useOn(context);
		}

		Level level = context.getLevel();
		ItemStack wand = context.getItemInHand();
		BlockState chosen = wand.get(ToolsContent.WAND_BLOCK);
		BlockPos clicked = context.getClickedPos();
		BlockState old = level.getBlockState(clicked);

		if (chosen == null) {
			if (!level.isClientSide()) {
				player.sendOverlayMessage(Component.translatable("jtechstorage.wand.nothing_selected").withStyle(ChatFormatting.GRAY));
			}

			return InteractionResult.FAIL;
		}

		if (old.is(chosen.getBlock()) || old.hasBlockEntity() || old.getDestroySpeed(level, clicked) < 0) {
			return InteractionResult.FAIL;
		}

		if (!(level instanceof ServerLevel server)) {
			return InteractionResult.SUCCESS;
		}

		Item item = chosen.getBlock().asItem();
		BuildSupply supply = new BuildSupply(player, wand, level);
		List<BlockPos> area = WandArea.collect(clicked, context.getClickedFace(), BuildingWandItem.radius(wand), p -> {
			BlockState s = level.getBlockState(p);
			return s.is(old.getBlock()) && !s.hasBlockEntity() && chosen.canSurvive(level, p) && level.mayInteract(player, p);
		});
		area.sort((a, b) -> Double.compare(a.distSqr(clicked), b.distSqr(clicked)));
		int got = supply.take(item, area.size());
		ItemStack tool = new ItemStack(Items.NETHERITE_PICKAXE);

		for (int i = 0; i < got; i++) {
			BlockPos p = area.get(i);
			BlockState s = level.getBlockState(p);

			for (ItemStack drop : Block.getDrops(s, server, p, null, player, tool)) {
				supply.give(drop);
			}

			level.setBlock(p, chosen, 3);
		}

		if (got == 0) {
			player.sendOverlayMessage(Component.translatable("jtechstorage.wand.no_blocks", new ItemStack(item).getHoverName()).withStyle(ChatFormatting.RED));
			return InteractionResult.FAIL;
		}

		SoundType sound = chosen.getSoundType();
		level.playSound(null, clicked, sound.getPlaceSound(), SoundSource.BLOCKS, (sound.getVolume() + 1.0f) / 2.0f, sound.getPitch() * 0.8f);
		return InteractionResult.SUCCESS;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
		BlockState chosen = stack.get(ToolsContent.WAND_BLOCK);
		tooltip.accept(chosen == null ? Component.translatable("jtechstorage.wand.nothing_selected").withStyle(ChatFormatting.GRAY)
				: Component.translatable("jtechstorage.wand.selected", chosen.getBlock().getName()).withStyle(ChatFormatting.LIGHT_PURPLE));
		int r = BuildingWandItem.radius(stack);
		tooltip.accept(Component.translatable("jtechstorage.wand.size", 2 * r + 1, 2 * r + 1).withStyle(ChatFormatting.GRAY));
	}
}
