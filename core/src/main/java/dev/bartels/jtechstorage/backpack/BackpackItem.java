package dev.bartels.jtechstorage.backpack;

import java.util.ArrayList;
import java.util.List;

import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Backpack in five sizes (leather 9 · iron 18 · gold 27 · diamond 36 · netherite 54 slots).
 * Right-click opens it. It can be worn in the chest slot (then it is visible on the back),
 * can be dyed, and items that are already inside are automatically picked into the backpack.
 * The contents live in the item ({@link DataComponents#CONTAINER}) and are kept when upgrading.
 */
public class BackpackItem extends Item {
	public enum Tier implements StringRepresentable {
		LEATHER(1, 0x8B5A2B),
		IRON(2, 0x6B6F7A),
		GOLD(3, 0x8E3B2E),
		DIAMOND(4, 0x2E6F78),
		NETHERITE(6, 0x3A2F3F);

		public final int rows;
		public final int defaultColor;

		Tier(int rows, int defaultColor) {
			this.rows = rows;
			this.defaultColor = defaultColor;
		}

		public int slots() {
			return rows * 9;
		}

		@Override
		public String getSerializedName() {
			return name().toLowerCase(java.util.Locale.ROOT);
		}
	}

	private final Tier tier;

	public BackpackItem(Tier tier, Properties properties) {
		super(properties);
		this.tier = tier;
	}

	public Tier getTier() {
		return tier;
	}

	public static boolean isBackpack(ItemStack stack) {
		return stack.getItem() instanceof BackpackItem;
	}

	/** Items that may not go into a backpack (backpacks and shulker boxes - no container-in-container tricks). */
	public static boolean isForbidden(ItemStack stack) {
		return isBackpack(stack) || stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof ShulkerBoxBlock;
	}

	/** Shift + right-click on a block: place the backpack. */
	@Override
	public InteractionResult useOn(UseOnContext context) {
		Player player = context.getPlayer();

		if (player == null || !player.isSecondaryUseActive()) {
			return InteractionResult.PASS;
		}

		BlockPos pos = context.getClickedPos();
		BlockState clicked = context.getLevel().getBlockState(pos);

		if (!clicked.canBeReplaced()) {
			pos = pos.relative(context.getClickedFace());
		}

		return PlacedBackpackBlock.place(context.getLevel(), pos, player.getDirection().getOpposite(), player, context.getItemInHand());
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);

		if (player instanceof ServerPlayer serverPlayer) {
			serverPlayer.openMenu(new SimpleMenuProvider((id, inventory, p) -> new BackpackMenu(id, inventory, stack, hand, tier),
					stack.getHoverName()));
			level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BUNDLE_DROP_CONTENTS,
					SoundSource.PLAYERS, 0.8f, 0.9f + level.getRandom().nextFloat() * 0.2f);
			player.awardStat(Stats.ITEM_USED.get(this));
		}

		return InteractionResult.SUCCESS;
	}

	@Override
	@SuppressWarnings("deprecation")   // deprecated in 26.x, but still works
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
			Consumer<Component> tooltip, TooltipFlag flag) {
		int used = 0;

		for (ItemStack item : readContents(stack, tier.slots())) {
			if (!item.isEmpty()) {
				used++;
			}
		}

		tooltip.accept(Component.translatable("jtechstorage.backpack.slots", used + "/" + tier.slots())
				.withStyle(net.minecraft.ChatFormatting.GOLD));
		tooltip.accept(Component.translatable("jtechstorage.backpack.hint")
				.withStyle(net.minecraft.ChatFormatting.GRAY));
	}

	// ------------------------------------------------------------------
	// Contents
	// ------------------------------------------------------------------

	public static NonNullList<ItemStack> readContents(ItemStack backpack, int size) {
		NonNullList<ItemStack> items = NonNullList.withSize(size, ItemStack.EMPTY);
		ItemContainerContents contents = backpack.get(DataComponents.CONTAINER);

		if (contents != null) {
			contents.copyInto(items);
		}

		return items;
	}

	public static void writeContents(ItemStack backpack, List<ItemStack> items) {
		backpack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(items));
	}

	// ------------------------------------------------------------------
	// Auto-pickup (called from ItemEntityMixin, server only)
	// ------------------------------------------------------------------

	public static void tryAutoPickup(ItemEntity entity, Player player) {
		if (entity.level().isClientSide() || entity.hasPickUpDelay() || !entity.isAlive()) {
			return;
		}

		ItemStack picked = entity.getItem();

		if (picked.isEmpty() || isForbidden(picked)) {
			return;
		}

		int before = picked.getCount();

		for (ItemStack backpack : backpacksOf(player)) {
			if (picked.isEmpty()) {
				break;
			}

			if (player.containerMenu instanceof BackpackMenu menu && menu.isFor(backpack)) {
				continue;   // currently open - do not interfere
			}

			absorbInto(backpack, picked);
		}

		int absorbed = before - picked.getCount();

		if (absorbed > 0) {
			entity.setItem(picked);
			player.take(entity, absorbed);
			player.awardStat(Stats.ITEM_PICKED_UP.get(picked.getItem()), absorbed);
			entity.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BUNDLE_INSERT,
					SoundSource.PLAYERS, 0.4f, 1.0f + entity.level().getRandom().nextFloat() * 0.3f);

			if (picked.isEmpty()) {
				entity.discard();
			}
		}
	}

	private static List<ItemStack> backpacksOf(Player player) {
		List<ItemStack> result = new ArrayList<>();
		ItemStack worn = player.getItemBySlot(EquipmentSlot.CHEST);

		if (isBackpack(worn)) {
			result.add(worn);
		}

		Inventory inventory = player.getInventory();

		for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
			ItemStack stack = inventory.getItem(slot);

			if (isBackpack(stack) && stack != worn) {
				result.add(stack);
			}
		}

		return result;
	}

	/** Only if the backpack already contains this item: first top up stacks, then free slots. */
	private static void absorbInto(ItemStack backpack, ItemStack picked) {
		int size = ((BackpackItem) backpack.getItem()).tier.slots();
		NonNullList<ItemStack> items = readContents(backpack, size);
		boolean contains = false;

		for (ItemStack item : items) {
			if (ItemStack.isSameItemSameComponents(item, picked)) {
				contains = true;
				break;
			}
		}

		if (!contains) {
			return;
		}

		for (ItemStack item : items) {
			if (picked.isEmpty()) {
				break;
			}

			if (ItemStack.isSameItemSameComponents(item, picked) && item.getCount() < item.getMaxStackSize()) {
				int move = Math.min(picked.getCount(), item.getMaxStackSize() - item.getCount());
				item.grow(move);
				picked.shrink(move);
			}
		}

		for (int i = 0; i < items.size() && !picked.isEmpty(); i++) {
			if (items.get(i).isEmpty()) {
				items.set(i, picked.split(Math.min(picked.getCount(), picked.getMaxStackSize())));
			}
		}

		writeContents(backpack, items);
	}
}
