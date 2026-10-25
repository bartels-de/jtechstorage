package dev.bartels.jtechstorage.tools;

import java.util.List;
import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
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
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;

import dev.bartels.jtechstorage.item.LinkedTerminalItem;

/**
 * Bau-Stab (v0.39): Rechtsklick auf eine Blockseite setzt vor die ganze zusammenhängende Fläche aus diesem Block eine
 * weitere Lage (3×3, 5×5 oder 7×7). Schleichen + Rechtsklick in die Luft wechselt die Größe. Die Blöcke kommen aus
 * dem Inventar – und, an ein Terminal gekoppelt (Schleichen + Rechtsklick aufs Terminal), aus dem Lager.
 */
public class BuildingWandItem extends LinkedTerminalItem {
	public BuildingWandItem(Properties properties) {
		super(properties);
	}

	static int radius(ItemStack stack) {
		return stack.getOrDefault(ToolsContent.WAND_SIZE, 1);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);

		if (!player.isSecondaryUseActive()) {
			return InteractionResult.PASS;
		}

		int next = radius(stack) % 3 + 1;
		stack.set(ToolsContent.WAND_SIZE, next);

		if (!level.isClientSide()) {
			player.sendOverlayMessage(Component.translatable("jtechstorage.wand.size", 2 * next + 1, 2 * next + 1).withStyle(ChatFormatting.LIGHT_PURPLE));
		}

		return InteractionResult.SUCCESS;
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		Player player = context.getPlayer();

		if (player == null || player.isSecondaryUseActive()) {
			return super.useOn(context);   // Terminal koppeln
		}

		Level level = context.getLevel();
		BlockPos clicked = context.getClickedPos();
		Direction face = context.getClickedFace();
		BlockState state = level.getBlockState(clicked);
		Item item = state.getBlock().asItem();

		if (item == Items.AIR || state.hasBlockEntity()) {
			return InteractionResult.FAIL;
		}

		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}

		ItemStack wand = context.getItemInHand();
		BuildSupply supply = new BuildSupply(player, wand, level);
		List<BlockPos> area = WandArea.collect(clicked, face, radius(wand), p -> {
			BlockPos target = p.relative(face);
			return level.getBlockState(p).is(state.getBlock()) && level.getBlockState(target).canBeReplaced()
					&& state.canSurvive(level, target) && level.isUnobstructed(state, target, CollisionContext.empty())
					&& level.mayInteract(player, target);
		});
		area.sort((a, b) -> Double.compare(a.distSqr(clicked), b.distSqr(clicked)));
		int got = supply.take(item, area.size());

		for (int i = 0; i < got; i++) {
			level.setBlock(area.get(i).relative(face), state, 3);
		}

		if (got == 0) {
			player.sendOverlayMessage(Component.translatable("jtechstorage.wand.no_blocks", new ItemStack(item).getHoverName()).withStyle(ChatFormatting.RED));
			return InteractionResult.FAIL;
		}

		SoundType sound = state.getSoundType();
		level.playSound(null, clicked.relative(face), sound.getPlaceSound(), SoundSource.BLOCKS, (sound.getVolume() + 1.0f) / 2.0f, sound.getPitch() * 0.8f);
		return InteractionResult.SUCCESS;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
		int r = radius(stack);
		tooltip.accept(Component.translatable("jtechstorage.wand.size", 2 * r + 1, 2 * r + 1).withStyle(ChatFormatting.LIGHT_PURPLE));
	}
}
