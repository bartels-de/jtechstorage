package dev.bartels.jtechstorage.depths;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

import dev.bartels.jtechstorage.JTechStorage;

/**
 * Notiz der Lagermeister (v0.32): eine von {@link #COUNT} Seiten der Geschichte der Kristalltiefe, zu finden in Ruinen
 * und Schreinen. Rechtsklick liest sie; jede gelesene Notiz zählt zum Fortschritt „Die ganze Geschichte“.
 */
public class AncientNoteItem extends Item {
	public static final int COUNT = 10;
	/** Client: Lese-Fenster öffnen (Nummer 1..COUNT). */
	public static Consumer<Integer> openScreen = note -> { };

	public AncientNoteItem(Properties properties) {
		super(properties);
	}

	public static int note(ItemStack stack) {
		Integer note = stack.get(DepthsContent.NOTE);
		return note == null ? 1 : Math.max(1, Math.min(COUNT, note));
	}

	@Override
	public Component getName(ItemStack stack) {
		return Component.translatable("item.jtechstorage.ancient_note.named", Component.translatable("jtechstorage.note." + note(stack) + ".title"));
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
		tooltip.accept(Component.translatable("item.jtechstorage.ancient_note.page", note(stack), COUNT).withStyle(ChatFormatting.GRAY));
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		int note = note(player.getItemInHand(hand));

		if (level.isClientSide()) {
			openScreen.accept(note);
		} else if (player instanceof ServerPlayer serverPlayer) {
			AdvancementHolder lore = serverPlayer.level().getServer().getAdvancements().get(JTechStorage.id("depths_lore"));

			if (lore != null) {
				serverPlayer.getAdvancements().award(lore, "note_" + note);
			}

			level.playSound(null, player.blockPosition(), SoundEvents.BOOK_PAGE_TURN, net.minecraft.sounds.SoundSource.PLAYERS, 1.0f, 1.0f);
		}

		return InteractionResult.SUCCESS;
	}
}
