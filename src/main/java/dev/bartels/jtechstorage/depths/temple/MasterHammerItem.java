package dev.bartels.jtechstorage.depths.temple;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;

import dev.bartels.jtechstorage.block.StorageTerminalBlockEntity;
import dev.bartels.jtechstorage.item.LinkedTerminalItem;
import dev.bartels.jtechstorage.network.StorageNetwork;

/**
 * Hammer der Lagermeister (v0.39, Beute des letzten Lagermeisters): eine Spitzhacke über Netherit-Stufe, die 3×3
 * Blöcke auf einmal abbaut (Schleichen = nur einen). Mit Schleichen + Rechtsklick an ein Lager-Terminal gekoppelt,
 * landet alles Abgebaute direkt im Lager, solange man in Funkreichweite ist.
 */
public class MasterHammerItem extends LinkedTerminalItem {
	private static final ThreadLocal<Boolean> BUSY = ThreadLocal.withInitial(() -> false);

	public MasterHammerItem(Properties properties) {
		super(properties);
	}

	static void init() {
		PlayerBlockBreakEvents.BEFORE.register((level, player, pos, state, blockEntity) -> {
			ItemStack tool = player.getMainHandItem();

			if (BUSY.get() || !(tool.getItem() instanceof MasterHammerItem) || !(level instanceof ServerLevel serverLevel)
					|| !(player instanceof ServerPlayer serverPlayer) || player.isCreative()) {
				return true;
			}

			BUSY.set(true);

			try {
				mine(serverLevel, serverPlayer, tool, pos, state);
			} finally {
				BUSY.set(false);
			}

			return false;   // selbst abgebaut
		});
	}

	/** Baut den Block und (ohne Schleichen) die 8 Nachbarn in der Ebene quer zur Blickrichtung ab. */
	private static void mine(ServerLevel level, ServerPlayer player, ItemStack tool, BlockPos center, BlockState centerState) {
		List<BlockPos> targets = new ArrayList<>();
		targets.add(center);

		if (!player.isShiftKeyDown()) {
			Direction face = facing(player, center);
			float centerHardness = centerState.getDestroySpeed(level, center);

			for (int a = -1; a <= 1; a++) {
				for (int b = -1; b <= 1; b++) {
					if (a == 0 && b == 0) {
						continue;
					}

					BlockPos p = switch (face.getAxis()) {
						case X -> center.offset(0, a, b);
						case Y -> center.offset(a, 0, b);
						default -> center.offset(a, b, 0);
					};
					BlockState s = level.getBlockState(p);
					float hardness = s.getDestroySpeed(level, p);

					if (!s.isAir() && hardness >= 0 && hardness <= centerHardness * 3 + 1.5f && s.is(BlockTags.MINEABLE_WITH_PICKAXE)
							&& tool.isCorrectToolForDrops(s)) {
						targets.add(p);
					}
				}
			}
		}

		StorageTerminalBlockEntity terminal = LinkedTerminalItem.quietTerminal(tool, level, player);
		StorageNetwork network = terminal != null ? terminal.buildNetwork() : null;

		for (BlockPos p : targets) {
			if (tool.isEmpty()) {
				break;
			}

			BlockState state = level.getBlockState(p);
			BlockEntity be = level.getBlockEntity(p);
			boolean drops = player.hasCorrectToolForDrops(state);
			List<ItemStack> loot = drops ? Block.getDrops(state, level, p, be, player, tool) : List.of();
			state.getBlock().playerWillDestroy(level, p, state, player);

			if (!level.destroyBlock(p, false, player)) {
				continue;
			}

			if (drops) {
				state.spawnAfterBreak(level, p, tool, true);
			}

			for (ItemStack stack : loot) {
				ItemStack rest = network != null ? network.insert(stack) : stack;

				if (!rest.isEmpty()) {
					Block.popResource(level, p, rest);
				}
			}

			tool.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
		}
	}

	/** Seite des Blocks, auf die der Spieler schaut. */
	private static Direction facing(Player player, BlockPos pos) {
		Vec3 eye = player.getEyePosition();
		Vec3 end = eye.add(player.getViewVector(1.0f).scale(8.0));
		BlockHitResult hit = player.level().clip(new ClipContext(eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));

		if (hit.getType() == HitResult.Type.BLOCK && hit.getBlockPos().equals(pos)) {
			return hit.getDirection();
		}

		return Direction.orderedByNearest(player)[0].getOpposite();
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
		tooltip.accept(Component.translatable("item.jtechstorage.master_hammer.tip.0").withStyle(ChatFormatting.GRAY));
		tooltip.accept(Component.translatable("item.jtechstorage.master_hammer.tip.1").withStyle(ChatFormatting.GRAY));
	}
}
