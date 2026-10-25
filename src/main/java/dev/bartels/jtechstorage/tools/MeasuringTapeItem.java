package dev.bartels.jtechstorage.tools;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * Maßband (v0.39): Rechtsklick auf zwei Blöcke zeigt Abmessungen (B × H × T), Anzahl der Blöcke im Quader und die
 * Entfernung; der Quader wird kurz mit Partikeln umrandet. Schleichen + Rechtsklick in die Luft setzt es zurück.
 */
public class MeasuringTapeItem extends Item {
	public MeasuringTapeItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);

		if (player.isSecondaryUseActive() && stack.has(ToolsContent.MEASURE_FROM)) {
			stack.remove(ToolsContent.MEASURE_FROM);

			if (!level.isClientSide()) {
				player.sendOverlayMessage(Component.translatable("jtechstorage.tape.reset").withStyle(ChatFormatting.GRAY));
			}

			return InteractionResult.SUCCESS;
		}

		return InteractionResult.PASS;
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		Player player = context.getPlayer();
		Level level = context.getLevel();
		ItemStack stack = context.getItemInHand();
		BlockPos pos = context.getClickedPos();

		if (player == null) {
			return InteractionResult.PASS;
		}

		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}

		GlobalPos from = stack.get(ToolsContent.MEASURE_FROM);

		if (from == null || !from.dimension().equals(level.dimension())) {
			stack.set(ToolsContent.MEASURE_FROM, GlobalPos.of(level.dimension(), pos.immutable()));
			player.sendOverlayMessage(Component.translatable("jtechstorage.tape.first", pos.getX(), pos.getY(), pos.getZ()).withStyle(ChatFormatting.YELLOW));
			level.playSound(null, pos, SoundEvents.UI_LOOM_SELECT_PATTERN, SoundSource.PLAYERS, 0.8f, 1.4f);
			return InteractionResult.SUCCESS;
		}

		BlockPos a = from.pos();
		int dx = Math.abs(pos.getX() - a.getX()) + 1;
		int dy = Math.abs(pos.getY() - a.getY()) + 1;
		int dz = Math.abs(pos.getZ() - a.getZ()) + 1;
		long volume = (long) dx * dy * dz;
		double dist = Math.sqrt(a.distSqr(pos));
		player.sendSystemMessage(Component.translatable("jtechstorage.tape.result", dx, dy, dz, volume, String.format(java.util.Locale.ROOT, "%.1f", dist))
				.withStyle(ChatFormatting.YELLOW));
		stack.remove(ToolsContent.MEASURE_FROM);
		level.playSound(null, pos, SoundEvents.UI_LOOM_TAKE_RESULT, SoundSource.PLAYERS, 0.8f, 1.2f);

		if (level instanceof ServerLevel server) {
			outline(server, a, pos);
		}

		return InteractionResult.SUCCESS;
	}

	/** Kanten des Quaders mit Partikeln zeichnen (höchstens 48 Punkte je Kante). */
	private static void outline(ServerLevel level, BlockPos a, BlockPos b) {
		double x0 = Math.min(a.getX(), b.getX()), y0 = Math.min(a.getY(), b.getY()), z0 = Math.min(a.getZ(), b.getZ());
		double x1 = Math.max(a.getX(), b.getX()) + 1, y1 = Math.max(a.getY(), b.getY()) + 1, z1 = Math.max(a.getZ(), b.getZ()) + 1;
		DustParticleOptions dust = new DustParticleOptions(0xFFD040, 1.0f);
		double[][] corners = {{x0, y0, z0}, {x1, y0, z0}, {x1, y0, z1}, {x0, y0, z1}, {x0, y1, z0}, {x1, y1, z0}, {x1, y1, z1}, {x0, y1, z1}};
		int[][] edges = {{0, 1}, {1, 2}, {2, 3}, {3, 0}, {4, 5}, {5, 6}, {6, 7}, {7, 4}, {0, 4}, {1, 5}, {2, 6}, {3, 7}};

		for (int[] e : edges) {
			double[] p = corners[e[0]], q = corners[e[1]];
			double len = Math.max(Math.abs(q[0] - p[0]), Math.max(Math.abs(q[1] - p[1]), Math.abs(q[2] - p[2])));
			int n = (int) Math.min(48, Math.max(2, len * 2));

			for (int i = 0; i <= n; i++) {
				double t = i / (double) n;
				level.sendParticles(dust, p[0] + (q[0] - p[0]) * t, p[1] + (q[1] - p[1]) * t, p[2] + (q[2] - p[2]) * t, 1, 0, 0, 0, 0);
			}
		}
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
		GlobalPos from = stack.get(ToolsContent.MEASURE_FROM);

		if (from != null) {
			tooltip.accept(Component.translatable("jtechstorage.tape.first", from.pos().getX(), from.pos().getY(), from.pos().getZ())
					.withStyle(ChatFormatting.YELLOW));
		}
	}
}
