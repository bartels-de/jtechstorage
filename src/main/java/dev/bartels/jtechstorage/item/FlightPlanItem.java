package dev.bartels.jtechstorage.item;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
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

import dev.bartels.jtechstorage.ModContent;
import dev.bartels.jtechstorage.block.PhoenixStopBlockEntity;

/**
 * Flugplan des Amethyst-Phönix: Haltestellen anklicken (bis zu 4, Reihenfolge = Klick-Reihenfolge), Rechtsklick in die
 * Luft öffnet ein Fenster zum Umsortieren und Entfernen, danach den Phönix damit rechtsklicken.
 */
public class FlightPlanItem extends Item {
	public static final int MAX_STOPS = 4;

	/** Eine Haltestelle im Plan (der Name wird beim Anklicken übernommen). */
	public record Stop(GlobalPos pos, String name) {
		public static final Codec<Stop> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				GlobalPos.CODEC.fieldOf("pos").forGetter(Stop::pos),
				Codec.STRING.fieldOf("name").forGetter(Stop::name)
		).apply(instance, Stop::new));
		public static final StreamCodec<RegistryFriendlyByteBuf, Stop> STREAM_CODEC = StreamCodec.composite(
				GlobalPos.STREAM_CODEC, Stop::pos,
				ByteBufCodecs.STRING_UTF8, Stop::name,
				Stop::new);
	}

	/** Client: öffnet das Flugplan-Fenster (vom Client-Initializer gesetzt). */
	public static Consumer<InteractionHand> openScreen = hand -> { };

	public FlightPlanItem(Properties properties) {
		super(properties);
	}

	public static List<Stop> stops(ItemStack stack) {
		return stack.getOrDefault(ModContent.FLIGHT_PLAN_STOPS, List.of());
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		Level level = context.getLevel();
		Player player = context.getPlayer();
		BlockPos pos = context.getClickedPos();

		if (!(level.getBlockEntity(pos) instanceof PhoenixStopBlockEntity stop)) {
			return InteractionResult.PASS;
		}

		if (level.isClientSide() || player == null) {
			return InteractionResult.SUCCESS;
		}

		ItemStack stack = context.getItemInHand();

		if (!stop.hasName()) {
			player.sendOverlayMessage(Component.translatable("jtechstorage.flight_plan.unnamed").withStyle(ChatFormatting.RED));
			return InteractionResult.FAIL;
		}

		List<Stop> stops = new ArrayList<>(stops(stack));
		GlobalPos clicked = GlobalPos.of(level.dimension(), pos.immutable());

		for (int i = 0; i < stops.size(); i++) {
			if (stops.get(i).pos().equals(clicked)) {
				// schon drin: nur den Namen auffrischen
				stops.set(i, new Stop(clicked, stop.getStopName()));
				stack.set(ModContent.FLIGHT_PLAN_STOPS, List.copyOf(stops));
				player.sendOverlayMessage(Component.translatable("jtechstorage.flight_plan.already", stop.getStopName(), i + 1));
				return InteractionResult.SUCCESS;
			}
		}

		if (stops.size() >= MAX_STOPS) {
			player.sendOverlayMessage(Component.translatable("jtechstorage.flight_plan.full", MAX_STOPS).withStyle(ChatFormatting.RED));
			return InteractionResult.FAIL;
		}

		if (!stops.isEmpty() && !stops.getFirst().pos().dimension().equals(clicked.dimension())) {
			player.sendOverlayMessage(Component.translatable("jtechstorage.flight_plan.dimension").withStyle(ChatFormatting.RED));
			return InteractionResult.FAIL;
		}

		stops.add(new Stop(clicked, stop.getStopName()));
		stack.set(ModContent.FLIGHT_PLAN_STOPS, List.copyOf(stops));
		level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0f, 0.9f + 0.15f * stops.size());
		player.sendOverlayMessage(Component.translatable("jtechstorage.flight_plan.added", stop.getStopName(), stops.size(), MAX_STOPS)
				.withStyle(ChatFormatting.LIGHT_PURPLE));
		return InteractionResult.SUCCESS;
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (level.isClientSide()) {
			openScreen.accept(hand);
		}

		return InteractionResult.SUCCESS;
	}

	/** Server: neue Reihenfolge aus dem Fenster übernehmen (Indizes der alten Liste, Lücken = entfernt). */
	public static void reorder(ItemStack stack, List<Integer> order) {
		List<Stop> old = stops(stack);
		List<Stop> result = new ArrayList<>();
		boolean[] used = new boolean[old.size()];

		for (int index : order) {
			if (index >= 0 && index < old.size() && !used[index] && result.size() < MAX_STOPS) {
				used[index] = true;
				result.add(old.get(index));
			}
		}

		if (result.isEmpty()) {
			stack.remove(ModContent.FLIGHT_PLAN_STOPS);
		} else {
			stack.set(ModContent.FLIGHT_PLAN_STOPS, List.copyOf(result));
		}
	}

	@Override
	public boolean isFoil(ItemStack stack) {
		return stops(stack).size() >= 2;
	}

	@Override
	@SuppressWarnings("deprecation")   // in 26.x als veraltet markiert, funktioniert aber weiterhin
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
		List<Stop> stops = stops(stack);

		if (stops.isEmpty()) {
			tooltip.accept(Component.translatable("jtechstorage.flight_plan.empty").withStyle(ChatFormatting.GRAY));
			return;
		}

		for (int i = 0; i < stops.size(); i++) {
			BlockPos pos = stops.get(i).pos().pos();
			tooltip.accept(Component.translatable("jtechstorage.flight_plan.line", i + 1, stops.get(i).name(), pos.getX(), pos.getY(), pos.getZ())
					.withStyle(ChatFormatting.LIGHT_PURPLE));
		}
	}
}
