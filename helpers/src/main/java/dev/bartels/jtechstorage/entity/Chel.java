package dev.bartels.jtechstorage.entity;

import dev.bartels.jtechstorage.helpers.HelpersContent;
import dev.bartels.jtechstorage.item.GuideBookItem;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.jspecify.annotations.Nullable;

import com.mojang.serialization.Codec;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Prediction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;

import dev.bartels.jtechstorage.ModConfig;
import dev.bartels.jtechstorage.ModSounds;
import dev.bartels.jtechstorage.ModAdvancements;
import dev.bartels.jtechstorage.menu.ChelOrderMenu;
import dev.bartels.jtechstorage.ModContent;
import dev.bartels.jtechstorage.block.StorageTerminalBlockEntity;
import dev.bartels.jtechstorage.network.StorageNetwork;

/**
 * Chel – the chaotic auto-crafter. You teach him how much of what to keep in stock in the network;
 * if something is missing, he crafts it with ingredients from the network (normal crafting table recipes).
 * Meanwhile he hops around, spins in circles and utters wisdom unprompted.
 *
 * <ul>
 *     <li>Right-click (empty hand): order menu (8 fields: item + target amount)</li>
 *     <li>Right-click with diamond: Chel tells a joke</li>
 *     <li>Right-click with nether star: transforms into Peter Chel Parker, protector of the base</li>
 *     <li>Shift + right-click (empty hand): pack up</li>
 * </ul>
 */
@SuppressWarnings("deprecation") // Ingredient#items() – the only way to get an example item
public class Chel extends NetworkHelperMob {
	public static final int MAX_ORDERS = 8;
	public static final int STAY_DISTANCE = 6;
	private static final int CRAFT_INTERVAL = (int) Math.round(ModConfig.get().helpers.chelCraftIntervalSeconds * 20.0);
	private static final int MAX_CRAFTS_PER_ROUND = ModConfig.get().helpers.chelMaxCraftsPerRound;
	private static final int SAY_LINES = 16;
	public static final int JOKES = GuideBookItem.JOKE_COUNT;
	public static final int MAX_TARGET = 999;
	private static final EntityDataAccessor<Integer> DATA_CRAFTING = SynchedEntityData.defineId(Chel.class,
			EntityDataSerializers.INT);

	/** Orders (fixed fields 0..7 in the order menu): item + target amount in the network. */
	private final @Nullable Item[] orderItems = new Item[MAX_ORDERS];
	private final int[] orderCounts = new int[MAX_ORDERS];
	private int lastJoke = -1;
	/** Found recipes per item (not saved, searched again when needed). */
	private final Map<Item, List<ResourceKey<Recipe<?>>>> recipeCache = new HashMap<>();
	private int craftCooldown = CRAFT_INTERVAL;
	private int sayCooldown = 600;
	private int spinTicks;
	private int orderIndex;
	private boolean craftedOnce;

	public Chel(EntityType<? extends Chel> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return createMobAttributes().add(Attributes.MAX_HEALTH, 20.0).add(Attributes.MOVEMENT_SPEED, 0.2);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DATA_CRAFTING, 0);
	}

	/** Remaining ticks of the hammer animation (for the renderer). */
	public int getCraftingTicks() {
		return entityData.get(DATA_CRAFTING);
	}

	@Override
	protected String unlinkedStatus() {
		return "jtechstorage.chel.status.unlinked";
	}

	@Override
	protected String linkAdvancement() {
		return "chel";
	}

	@Override
	protected void onLinked(Player player) {
		setStatus("jtechstorage.chel.status.linked");
		say(player, Component.translatable("jtechstorage.chel.hello"));
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(1, new StayNearTerminalGoal());
		goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
		goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0f));
		goalSelector.addGoal(7, new RandomLookAroundGoal(this));
	}

	// ------------------------------------------------------------------
	// Interaction
	// ------------------------------------------------------------------

	@Override
	protected InteractionResult mobInteract(Player player, InteractionHand hand) {
		if (isHologram()) {
			return InteractionResult.PASS;
		}

		if (level().isClientSide()) {
			// Every item is a possible order – do not accidentally eat or place it
			return InteractionResult.SUCCESS;
		}

		return super.mobInteract(player, hand);
	}

	@Override
	protected InteractionResult interactWith(Player player, InteractionHand hand) {
		ItemStack held = player.getItemInHand(hand);

		if (held.isEmpty() && player.isSecondaryUseActive()) {
			ItemStack item = createPackedItem(HelpersContent.CHEL_ITEM, player);
			player.getInventory().placeItemBackInInventory(item, Prediction.SERVER_ONLY);
			playSound(SoundEvents.AMETHYST_CLUSTER_BREAK, 1.0f, 1.3f);
			say(player, Component.translatable("jtechstorage.chel.bye"));
			discard();
			return InteractionResult.SUCCESS;
		}

		if (held.isEmpty()) {
			if (player instanceof ServerPlayer serverPlayer) {
				serverPlayer.openMenu(new ChelMenuProvider());
			}

			return InteractionResult.SUCCESS;
		}

		if (held.is(Items.DIAMOND) && level() instanceof ServerLevel serverLevel) {
			tellJoke(serverLevel, player);
			held.consume(1, player);
			return InteractionResult.SUCCESS;
		}

		if (held.is(Items.NETHER_STAR) && level() instanceof ServerLevel serverLevel) {
			held.consume(1, player);
			transform(serverLevel, player);
			return InteractionResult.SUCCESS;
		}

		if (held.is(HelpersContent.CHEL_ITEM)) {
			return InteractionResult.PASS;
		}

		say(player, Component.translatable("jtechstorage.chel.use_menu"));
		return InteractionResult.SUCCESS;
	}

	// ------------------------------------------------------------------
	// Jokes & transformation
	// ------------------------------------------------------------------

	private void tellJoke(ServerLevel level, Player player) {
		int joke = pickJoke(player, lastJoke, random);
		lastJoke = joke;
		Component text = Component.translatable("jtechstorage.chel.joke." + joke);

		for (ServerPlayer listener : level.players()) {
			if (listener == player || listener.distanceToSqr(this) < 12 * 12) {
				listener.sendSystemMessage(Component.translatable("jtechstorage.chel.joke_format", getDisplayName(), text));
				recordJoke(listener, joke);
			}
		}

		playSound(ModSounds.CHEL_LAUGH, 1.0f, 1.0f);
		spinTicks = 10;
		jumpFromGround();
		level.sendParticles(ParticleTypes.NOTE, getX(), getY() + 1.6, getZ(), 5, 0.4, 0.2, 0.4, 1.0);
	}

	// ------------------------------------------------------------------
	// Joke book
	// ------------------------------------------------------------------

	/** Mostly a joke the player does not yet have in the joke book – otherwise any (never twice in a row). */
	public static int pickJoke(Player player, int last, RandomSource random) {
		List<Integer> heard = jokeBook(player).getOrDefault(ModContent.JOKES_HEARD, List.of());

		if (heard.size() < JOKES && random.nextFloat() < 0.7f) {
			List<Integer> fresh = new ArrayList<>();

			for (int i = 0; i < JOKES; i++) {
				if (!heard.contains(i) && i != last) {
					fresh.add(i);
				}
			}

			if (!fresh.isEmpty()) {
				return fresh.get(random.nextInt(fresh.size()));
			}
		}

		int joke;

		do {
			joke = random.nextInt(JOKES);
		} while (joke == last);

		return joke;
	}

	/** First joke book in the inventory (or empty). */
	public static ItemStack jokeBook(Player player) {
		Inventory inventory = player.getInventory();

		for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
			ItemStack stack = inventory.getItem(slot);

			if (stack.is(ModContent.JOKE_BOOK)) {
				return stack;
			}
		}

		return ItemStack.EMPTY;
	}

	/** Records a heard joke in the joke book. Whoever never had one gets one as a gift with the first joke. */
	public static void recordJoke(ServerPlayer player, int joke) {
		ItemStack book = jokeBook(player);

		if (book.isEmpty()) {
			if (player.entityTags().contains("jtechstorage.joke_book")) {
				return;
			}

			player.addTag("jtechstorage.joke_book");
			book = new ItemStack(ModContent.JOKE_BOOK);
			book.set(ModContent.JOKES_HEARD, List.of(joke));
			player.getInventory().placeItemBackInInventory(book, Prediction.SERVER_ONLY);
			player.sendSystemMessage(Component.translatable("jtechstorage.joke_book.gift").withStyle(ChatFormatting.LIGHT_PURPLE));
			return;
		}

		List<Integer> heard = book.getOrDefault(ModContent.JOKES_HEARD, List.of());

		if (heard.contains(joke)) {
			return;
		}

		List<Integer> updated = new ArrayList<>(heard);
		updated.add(joke);
		updated.sort(null);
		book.set(ModContent.JOKES_HEARD, List.copyOf(updated));
		player.sendOverlayMessage(Component.translatable("jtechstorage.joke_book.new", updated.size(), JOKES)
				.withStyle(ChatFormatting.LIGHT_PURPLE));

		if (updated.size() >= JOKES) {
			ModAdvancements.grant(player, "joke_master");
			player.sendSystemMessage(Component.translatable("jtechstorage.joke_book.complete").withStyle(ChatFormatting.GOLD));
		}
	}

	/** Nether star: Chel becomes Peter Chel Parker, protector of the base. */
	private void transform(ServerLevel level, Player player) {
		PeterChelParker hero = HelpersContent.PETER_CHEL_PARKER.create(level, EntitySpawnReason.CONVERSION);

		if (hero == null) {
			return;
		}

		LightningBolt bolt = EntityTypes.LIGHTNING_BOLT.create(level, EntitySpawnReason.TRIGGERED);

		if (bolt != null) {
			bolt.snapTo(getX(), getY(), getZ());
			bolt.setVisualOnly(true);
			level.addFreshEntity(bolt);
		}

		hero.snapTo(getX(), getY(), getZ(), getYRot(), 0.0f);
		hero.inheritFrom(this, player);
		ItemStack hat = getItemBySlot(EquipmentSlot.HEAD);

		if (!hat.isEmpty()) {
			hero.setItemSlot(EquipmentSlot.HEAD, hat.copy());
		}

		level.addFreshEntity(hero);
		level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, getX(), getY() + 1.0, getZ(), 80, 0.6, 1.0, 0.6, 0.6);
		level.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 1.0, getZ(), 40, 0.4, 1.2, 0.4, 0.15);
		level.sendParticles(ParticleTypes.WITCH, getX(), getY() + 1.0, getZ(), 40, 0.8, 1.0, 0.8, 0.05);
		hero.playSound(SoundEvents.TOTEM_USE, 1.0f, 0.9f);
		hero.playSound(ModSounds.PETER_TRANSFORM, 1.5f, 1.0f);

		for (ServerPlayer listener : level.players()) {
			if (listener == player || listener.distanceToSqr(this) < 32 * 32) {
				listener.sendSystemMessage(Component.translatable("jtechstorage.chel.transform").withStyle(ChatFormatting.GOLD));
			}
		}

		if (player instanceof ServerPlayer serverPlayer) {
			ModAdvancements.grant(serverPlayer, "peter_chel_parker");
		}

		discard();
	}

	// ------------------------------------------------------------------
	// Orders (used by the order menu)
	// ------------------------------------------------------------------

	public ItemStack orderPrototype(int slot) {
		Item item = orderItems[slot];
		return item == null ? ItemStack.EMPTY : new ItemStack(item);
	}

	/** Current network stock for each order field. */
	public long[] stockForOrders() {
		long[] stock = new long[MAX_ORDERS];
		StorageTerminalBlockEntity terminal = terminal();

		if (terminal == null) {
			return stock;
		}

		StorageNetwork network = terminal.buildNetwork();

		for (int i = 0; i < MAX_ORDERS; i++) {
			if (orderItems[i] != null) {
				stock[i] = network.count(new ItemStack(orderItems[i]));
			}
		}

		return stock;
	}

	public int orderTarget(int slot) {
		return orderItems[slot] == null ? 0 : orderCounts[slot];
	}

	private boolean hasOrders() {
		for (Item item : orderItems) {
			if (item != null) {
				return true;
			}
		}

		return false;
	}

	/** Target amount for an item (protection against infinite loops when choosing ingredients). */
	private int targetFor(Item item) {
		int target = 0;

		for (int i = 0; i < MAX_ORDERS; i++) {
			if (orderItems[i] == item) {
				target = Math.max(target, orderCounts[i]);
			}
		}

		return target;
	}

	/** Sets an order from an item. Returns an error text or {@code null} on success. */
	public @Nullable Component setOrder(int slot, ItemStack stack) {
		if (!(level() instanceof ServerLevel serverLevel) || stack.isEmpty()) {
			return null;
		}

		if (findRecipes(serverLevel, stack.getItem()).isEmpty()) {
			return Component.translatable("jtechstorage.chel.no_recipe", stack.getHoverName());
		}

		orderItems[slot] = stack.getItem();
		orderCounts[slot] = Math.clamp(stack.getCount(), 1, MAX_TARGET);
		playSound(SoundEvents.VILLAGER_YES, 0.8f, 1.6f);
		jumpFromGround();
		craftCooldown = Math.min(craftCooldown, 20);
		return null;
	}

	public void adjustOrder(int slot, int delta) {
		if (orderItems[slot] != null) {
			orderCounts[slot] = Math.clamp(orderCounts[slot] + delta, 1, MAX_TARGET);
		}
	}

	public void clearOrder(int slot) {
		orderItems[slot] = null;
		orderCounts[slot] = 0;
	}

	/** Menu provider for Chel's order menu. */
	private class ChelMenuProvider implements ExtendedMenuProvider<ChelOrderMenu.OpenData> {
		@Override
		public ChelOrderMenu.OpenData getScreenOpeningData(ServerPlayer player) {
			return new ChelOrderMenu.OpenData(getId());
		}

		@Override
		public Component getDisplayName() {
			return Component.translatable("jtechstorage.chel.menu.title", Chel.this.getDisplayName());
		}

		@Override
		public AbstractContainerMenu createMenu(int containerId,
				Inventory inventory, Player player) {
			return new ChelOrderMenu(containerId, inventory, Chel.this);
		}
	}

	/** Chel speaks: action bar for the player. */
	private void say(Player player, Component text) {
		player.sendOverlayMessage(Component.translatable("jtechstorage.chel.says", getDisplayName(), text));
	}

	private void sayToNearby(ServerLevel level, Component text) {
		for (ServerPlayer player : level.players()) {
			if (player.distanceToSqr(this) < 12 * 12) {
				say(player, text);
			}
		}
	}

	// ------------------------------------------------------------------
	// Behavior
	// ------------------------------------------------------------------

	@Override
	public void aiStep() {
		super.aiStep();

		if (!(level() instanceof ServerLevel serverLevel)) {
			return;
		}

		int crafting = getCraftingTicks();

		if (crafting > 0) {
			entityData.set(DATA_CRAFTING, crafting - 1);
		}

		if (isResting()) {
			return;   // closing time – Chel snores
		}

		chaos(serverLevel);

		if (terminalPos == null || !hasOrders()) {
			return;
		}

		if (--craftCooldown > 0) {
			return;
		}

		StorageTerminalBlockEntity terminal = terminal();
		craftCooldown = (int) Math.round(CRAFT_INTERVAL / speedFactor());

		if (terminal == null) {
			setStatus("jtechstorage.helper.status.no_terminal");
			return;
		}

		workOnOrders(serverLevel, terminal);
	}

	/** Chaos: hopping, spinning and unprompted quips. */
	private void chaos(ServerLevel level) {
		if (spinTicks > 0) {
			spinTicks--;
			setYRot(getYRot() + 36.0f);
			yBodyRot = getYRot();
			yHeadRot = getYRot();
		} else if (random.nextInt(400) == 0) {
			spinTicks = 10;
		}

		if (onGround() && random.nextInt(160) == 0) {
			jumpFromGround();
		}

		if (--sayCooldown <= 0) {
			sayCooldown = 1200 + random.nextInt(1800);
			sayToNearby(level, Component.translatable("jtechstorage.chel.say." + random.nextInt(SAY_LINES)));
		}
	}

	private void workOnOrders(ServerLevel level, StorageTerminalBlockEntity terminal) {
		StorageNetwork network = terminal.buildNetwork();

		for (int n = 0; n < MAX_ORDERS; n++) {
			int slot = Math.floorMod(orderIndex + n, MAX_ORDERS);
			Item item = orderItems[slot];

			if (item == null) {
				continue;
			}

			int target = orderCounts[slot];
			ItemStack prototype = new ItemStack(item);
			long have = network.count(prototype);

			if (have >= target) {
				continue;
			}

			orderIndex += n + 1;
			int crafted = 0;
			Component missing = null;

			for (int round = 0; round < MAX_CRAFTS_PER_ROUND && have < target; round++) {
				CraftResult result = craftOnce(level, network, item);

				if (result.crafted() <= 0) {
					missing = result.missing();
					break;
				}

				crafted += result.crafted();
				have += result.crafted();
			}

			if (crafted > 0) {
				entityData.set(DATA_CRAFTING, 30);
				playSound(SoundEvents.CRAFTER_CRAFT, 0.6f, 0.9f + random.nextFloat() * 0.4f);
				playSound(ModSounds.CHEL_CRAFT, 0.8f, 0.95f + random.nextFloat() * 0.1f);
				level.sendParticles(ParticleTypes.HAPPY_VILLAGER, getX(), getY() + 1.2, getZ(), 8, 0.4, 0.3, 0.4, 0.0);
				level.sendParticles(ParticleTypes.WAX_ON, getX(), getY() + 1.6, getZ(), 6, 0.3, 0.2, 0.3, 0.5);
				setStatus("jtechstorage.chel.status.crafted", crafted, prototype.getHoverName());

				if (random.nextInt(3) == 0) {
					sayToNearby(level, Component.translatable("jtechstorage.chel.crafted." + random.nextInt(4), crafted,
							prototype.getHoverName()));
				}

				if (!craftedOnce && owner != null && level.getPlayerByUUID(owner) instanceof ServerPlayer serverPlayer) {
					ModAdvancements.grant(serverPlayer, "chel_craft");
					craftedOnce = true;
				}
			} else if (missing != null) {
				setStatus("jtechstorage.chel.status.missing", missing, prototype.getHoverName());
			} else {
				setStatus("jtechstorage.chel.status.no_recipe", prototype.getHoverName());
			}

			return; // only one order per round
		}

		setStatus("jtechstorage.chel.status.all_good");
	}

	private record CraftResult(int crafted, @Nullable Component missing) {
	}

	/** Crafts once: takes ingredients from the network, result + leftovers back into the network. */
	private CraftResult craftOnce(ServerLevel level, StorageNetwork network, Item wanted) {
		Component missing = null;

		for (ResourceKey<Recipe<?>> key : findRecipes(level, wanted)) {
			Optional<RecipeHolder<?>> holder = level.recipeAccess().byKey(key);

			if (holder.isEmpty() || !(holder.get().value() instanceof CraftingRecipe recipe)) {
				continue;
			}

			Grid grid = gridOf(recipe);

			if (grid == null) {
				continue;
			}

			// 1. find matching ingredients in the network (without removing)
			List<StorageNetwork.Entry> stock = network.aggregate();
			Map<ItemStack, Integer> reserved = new HashMap<>();
			List<ItemStack> chosen = new ArrayList<>();
			boolean ok = true;

			for (Optional<Ingredient> slot : grid.slots()) {
				if (slot.isEmpty()) {
					chosen.add(ItemStack.EMPTY);
					continue;
				}

				ItemStack pick = pickIngredient(stock, reserved, slot.get(), wanted);

				if (pick == null) {
					missing = slot.get().items().findFirst().map(h -> new ItemStack(h).getHoverName()).orElse(Component.literal("?"));
					ok = false;
					break;
				}

				chosen.add(pick);
			}

			if (!ok) {
				continue;
			}

			// 2. remove them
			List<ItemStack> taken = new ArrayList<>();

			for (ItemStack prototype : chosen) {
				if (prototype.isEmpty()) {
					taken.add(ItemStack.EMPTY);
					continue;
				}

				ItemStack got = network.extract(prototype, 1);

				if (got.isEmpty()) {
					ok = false;
					break;
				}

				taken.add(got);
			}

			CraftingInput input = ok ? CraftingInput.of(grid.width(), grid.height(), taken) : null;

			if (!ok || !recipe.matches(input, level)) {
				giveBack(level, network, taken);
				continue;
			}

			ItemStack result = recipe.assemble(input);

			if (result.isEmpty() || !result.is(wanted)) {
				giveBack(level, network, taken);
				continue;
			}

			int count = result.getCount();
			NonNullList<ItemStack> remainders = recipe.getRemainingItems(input);
			store(level, network, result);

			for (ItemStack remainder : remainders) {
				store(level, network, remainder);
			}

			return new CraftResult(count, null);
		}

		return new CraftResult(0, missing);
	}

	private record Grid(int width, int height, List<Optional<Ingredient>> slots) {
	}

	private static @Nullable Grid gridOf(CraftingRecipe recipe) {
		if (recipe instanceof ShapedRecipe shaped) {
			return new Grid(shaped.getWidth(), shaped.getHeight(), shaped.getIngredients());
		}

		if (recipe instanceof ShapelessRecipe shapeless) {
			List<Ingredient> ingredients = shapeless.placementInfo().ingredients();

			if (ingredients.isEmpty() || ingredients.size() > 9) {
				return null;
			}

			List<Optional<Ingredient>> slots = new ArrayList<>();
			ingredients.forEach(ingredient -> slots.add(Optional.of(ingredient)));
			int size = ingredients.size() <= 4 ? 2 : 3;

			while (slots.size() < size * size) {
				slots.add(Optional.empty());
			}

			return new Grid(size, size, slots);
		}

		return null;
	}

	/**
	 * Picks an ingredient. Items that are themselves an order are only used above their target amount –
	 * otherwise Chel would endlessly convert e.g. iron blocks and ingots into each other.
	 */
	private @Nullable ItemStack pickIngredient(List<StorageNetwork.Entry> stock, Map<ItemStack, Integer> reserved,
			Ingredient ingredient, Item wanted) {
		for (StorageNetwork.Entry entry : stock) {
			ItemStack prototype = entry.prototype();

			if (!ingredient.test(prototype) || prototype.has(DataComponents.CUSTOM_NAME) || prototype.isEnchanted()) {
				continue;
			}

			ItemStack key = null;

			for (ItemStack existing : reserved.keySet()) {
				if (ItemStack.isSameItemSameComponents(existing, prototype)) {
					key = existing;
				}
			}

			int used = key != null ? reserved.get(key) : 0;

			long keep = prototype.is(wanted) ? 0 : targetFor(prototype.getItem());

			if (entry.total() - keep > used) {
				reserved.put(key != null ? key : prototype, used + 1);
				return prototype.copyWithCount(1);
			}
		}

		return null;
	}

	private void giveBack(ServerLevel level, StorageNetwork network, List<ItemStack> taken) {
		for (ItemStack stack : taken) {
			store(level, network, stack);
		}
	}

	private void store(ServerLevel level, StorageNetwork network, ItemStack stack) {
		if (stack.isEmpty()) {
			return;
		}

		ItemStack rest = network.insert(stack.copy());

		if (!rest.isEmpty()) {
			spawnAtLocation(level, rest);
		}
	}

	/** All crafting table recipes whose result is this item (trial craft with the first matching items). */
	private List<ResourceKey<Recipe<?>>> findRecipes(ServerLevel level, Item wanted) {
		return recipeCache.computeIfAbsent(wanted, item -> {
			List<ResourceKey<Recipe<?>>> found = new ArrayList<>();

			for (RecipeHolder<?> holder : level.recipeAccess().getRecipes()) {
				if (!(holder.value() instanceof CraftingRecipe recipe)) {
					continue;
				}

				Grid grid = gridOf(recipe);

				if (grid == null) {
					continue;
				}

				List<ItemStack> sample = new ArrayList<>();
				boolean ok = true;

				for (Optional<Ingredient> slot : grid.slots()) {
					if (slot.isEmpty()) {
						sample.add(ItemStack.EMPTY);
						continue;
					}

					Optional<Holder<Item>> first = slot.get().items().findFirst();

					if (first.isEmpty()) {
						ok = false;
						break;
					}

					sample.add(new ItemStack(first.get()));
				}

				if (!ok) {
					continue;
				}

				CraftingInput input = CraftingInput.of(grid.width(), grid.height(), sample);

				try {
					if (recipe.matches(input, level) && recipe.assemble(input).is(item)) {
						found.add(holder.id());
					}
				} catch (RuntimeException ignored) {
					// skip exotic recipes from other mods
				}
			}

			return found;
		});
	}

	// ------------------------------------------------------------------
	// Saving
	// ------------------------------------------------------------------

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);

		List<Integer> slots = new ArrayList<>();
		List<Item> items = new ArrayList<>();
		List<Integer> counts = new ArrayList<>();

		for (int i = 0; i < MAX_ORDERS; i++) {
			if (orderItems[i] != null) {
				slots.add(i);
				items.add(orderItems[i]);
				counts.add(orderCounts[i]);
			}
		}

		if (!items.isEmpty()) {
			output.store("OrderSlots", Codec.INT.listOf(), slots);
			output.store("OrderItems", BuiltInRegistries.ITEM.byNameCodec().listOf(), items);
			output.store("OrderCounts", Codec.INT.listOf(), counts);
		}

		output.putBoolean("CraftedOnce", craftedOnce);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		Arrays.fill(orderItems, null);
		Arrays.fill(orderCounts, 0);
		List<Item> items = input.read("OrderItems", BuiltInRegistries.ITEM.byNameCodec().listOf()).orElse(List.of());
		List<Integer> counts = input.read("OrderCounts", Codec.INT.listOf()).orElse(List.of());
		List<Integer> slots = input.read("OrderSlots", Codec.INT.listOf()).orElse(List.of());

		for (int i = 0; i < items.size() && i < counts.size(); i++) {
			int slot = i < slots.size() ? slots.get(i) : i;   // older versions: without field numbers

			if (slot >= 0 && slot < MAX_ORDERS) {
				orderItems[slot] = items.get(i);
				orderCounts[slot] = counts.get(i);
			}
		}

		craftedOnce = input.getBooleanOr("CraftedOnce", false);
		setStatus(terminalPos == null ? unlinkedStatus() : "jtechstorage.chel.status.linked");
	}

	/** Stays near his terminal (he is chaotic, but not lost). */
	private class StayNearTerminalGoal extends Goal {
		StayNearTerminalGoal() {
			setFlags(EnumSet.of(Goal.Flag.MOVE));
		}

		@Override
		public boolean canUse() {
			return terminalPos != null && distanceToSqr(Vec3.atCenterOf(terminalPos)) > STAY_DISTANCE * STAY_DISTANCE;
		}

		@Override
		public boolean canContinueToUse() {
			return terminalPos != null && distanceToSqr(Vec3.atCenterOf(terminalPos)) > 3 * 3 && !getNavigation().isDone();
		}

		@Override
		public void start() {
			BlockPos pos = terminalPos;

			if (pos != null) {
				getNavigation().moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 1.0);
			}
		}
	}

	@Override
	protected SoundEvent helperSleepSound() {
		return ModSounds.CHEL_SLEEP;
	}

	@Override
	protected SoundEvent helperAmbientSound() {
		return ModSounds.CHEL_AMBIENT;
	}
}
