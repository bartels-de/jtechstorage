package dev.bartels.jtechstorage;

import com.mojang.serialization.Codec;
import dev.bartels.jtechstorage.chest.ChestContent;
import dev.bartels.jtechstorage.display.DisplayContent;
import dev.bartels.jtechstorage.farm.FarmContent;
import dev.bartels.jtechstorage.logistics.LogisticsContent;
import dev.bartels.jtechstorage.tools.ToolsContent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import java.util.function.IntSupplier;
import java.util.function.Supplier;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.minecraft.ChatFormatting;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SmithingTemplateItem;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.Items;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Unit;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

import net.fabricmc.fabric.api.menu.v1.ExtendedMenuType;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;

import dev.bartels.jtechstorage.backpack.BackpackItem;
import dev.bartels.jtechstorage.backpack.PlacedBackpackBlock;
import dev.bartels.jtechstorage.backpack.PlacedBackpackBlockEntity;
import dev.bartels.jtechstorage.block.StorageTerminalBlock;
import dev.bartels.jtechstorage.block.StorageTerminalBlockEntity;
import dev.bartels.jtechstorage.block.TeamTerminalBlock;
import dev.bartels.jtechstorage.block.TeamTerminalBlockEntity;
import dev.bartels.jtechstorage.block.TradeTerminalBlock;
import dev.bartels.jtechstorage.block.TradeTerminalBlockEntity;
import dev.bartels.jtechstorage.entity.CrystalShard;
import dev.bartels.jtechstorage.entity.JbartelsHologram;
import dev.bartels.jtechstorage.item.GuideBookItem;
import dev.bartels.jtechstorage.item.StorageKeyItem;
import dev.bartels.jtechstorage.item.UpgradeItem;
import dev.bartels.jtechstorage.item.WirelessTerminalItem;
import dev.bartels.jtechstorage.menu.StorageTerminalMenu;
import dev.bartels.jtechstorage.menu.TeamMenu;
import dev.bartels.jtechstorage.menu.TradeMenu;

/** All of the mod's registrations in one place. */
public final class ModContent {
	/** Blocks in this tag can be added to the network with the key. */
	public static final TagKey<Block> NETWORK_STORAGE = TagKey.create(Registries.BLOCK, JTechStorage.id("network_storage"));

	/** Range without upgrade (blocks, spherical). */
	public static final int BASE_RANGE = ModConfig.get().network.baseRange;
	/** Number of linkable chests without upgrade (double chest = 1). */
	public static final int BASE_CAPACITY = ModConfig.get().network.baseCapacity;
	/** Wireless terminal: range = terminal range x factor. */
	public static final int WIRELESS_RANGE_FACTOR = ModConfig.get().network.wirelessRangeFactor;

	private static final List<Item> CREATIVE_ORDER = new ArrayList<>();
	private static final List<Supplier<? extends java.util.Collection<? extends ItemLike>>> CREATIVE_EXTRA = new ArrayList<>();

	/** v0.43: sub-mods attach their items to the shared creative tab. */
	public static void addCreative(Supplier<? extends java.util.Collection<? extends ItemLike>> items) {
		CREATIVE_EXTRA.add(items);
	}

	// --- Data component: which terminal a key/wireless terminal is bound to ---

	public static final DataComponentType<GlobalPos> LINKED_TERMINAL = Registry.register(
			BuiltInRegistries.DATA_COMPONENT_TYPE, JTechStorage.id("linked_terminal"),
			DataComponentType.<GlobalPos>builder().persistent(GlobalPos.CODEC).networkSynchronized(GlobalPos.STREAM_CODEC).build());

	/** Wireless terminal with built-in wireless core (from the Amethyst Robot) - only then does it work. */
	public static final DataComponentType<Unit> WIRELESS_CORE_INSTALLED = Registry.register(
			BuiltInRegistries.DATA_COMPONENT_TYPE, JTechStorage.id("wireless_core"),
			DataComponentType.<Unit>builder().persistent(Unit.CODEC).networkSynchronized(Unit.STREAM_CODEC).build());



	/** Chel's joke book: numbers of the jokes already heard. */
	public static final DataComponentType<List<Integer>> JOKES_HEARD = Registry.register(
			BuiltInRegistries.DATA_COMPONENT_TYPE, JTechStorage.id("jokes_heard"),
			DataComponentType.<List<Integer>>builder().persistent(Codec.INT.listOf())
					.networkSynchronized(ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list())).build());

	// --- Blocks ---------------------------------------------------------

	public static final ResourceKey<Block> STORAGE_TERMINAL_KEY = blockKey("storage_terminal");
	public static final Block STORAGE_TERMINAL = new StorageTerminalBlock(terminalProperties(STORAGE_TERMINAL_KEY), false);
	public static final Item STORAGE_TERMINAL_ITEM = blockItem(STORAGE_TERMINAL, STORAGE_TERMINAL_KEY);

	public static final ResourceKey<Block> CRAFTING_TERMINAL_KEY = blockKey("crafting_terminal");
	public static final Block CRAFTING_TERMINAL = new StorageTerminalBlock(terminalProperties(CRAFTING_TERMINAL_KEY), true);
	public static final Item CRAFTING_TERMINAL_ITEM = blockItem(CRAFTING_TERMINAL, CRAFTING_TERMINAL_KEY);

	public static final BlockEntityType<StorageTerminalBlockEntity> STORAGE_TERMINAL_BLOCK_ENTITY =
			FabricBlockEntityTypeBuilder.create(StorageTerminalBlockEntity::new, STORAGE_TERMINAL, CRAFTING_TERMINAL).build();

	public static final ResourceKey<Block> TEAM_TERMINAL_KEY = blockKey("team_terminal");
	public static final Block TEAM_TERMINAL = new TeamTerminalBlock(terminalProperties(TEAM_TERMINAL_KEY));
	public static final Item TEAM_TERMINAL_ITEM = blockItem(TEAM_TERMINAL, TEAM_TERMINAL_KEY);
	public static final BlockEntityType<TeamTerminalBlockEntity> TEAM_TERMINAL_BLOCK_ENTITY =
			FabricBlockEntityTypeBuilder.create(TeamTerminalBlockEntity::new, TEAM_TERMINAL).build();

	public static final ResourceKey<Block> TRADE_TERMINAL_KEY = blockKey("trade_terminal");
	public static final Block TRADE_TERMINAL = new TradeTerminalBlock(terminalProperties(TRADE_TERMINAL_KEY));
	public static final Item TRADE_TERMINAL_ITEM = blockItem(TRADE_TERMINAL, TRADE_TERMINAL_KEY);
	public static final BlockEntityType<TradeTerminalBlockEntity> TRADE_TERMINAL_BLOCK_ENTITY =
			FabricBlockEntityTypeBuilder.create(TradeTerminalBlockEntity::new, TRADE_TERMINAL).build();



	/** Placed backpack (no item of its own - created by Shift + right-click with a backpack). */
	public static final ResourceKey<Block> PLACED_BACKPACK_KEY = blockKey("placed_backpack");
	public static final Block PLACED_BACKPACK = new PlacedBackpackBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.HAY_BLOCK)
			.noOcclusion().strength(0.6f).setId(PLACED_BACKPACK_KEY));
	public static final BlockEntityType<PlacedBackpackBlockEntity> PLACED_BACKPACK_BLOCK_ENTITY =
			FabricBlockEntityTypeBuilder.create(PlacedBackpackBlockEntity::new, PLACED_BACKPACK).build();

	// --- Tools ----------------------------------------------------------

	public static final Item GUIDE_BOOK = book("guide_book", GuideBookItem.Kind.HANDBOOK, Rarity.COMMON);
	public static final Item RECIPE_BOOK = book("recipe_book", GuideBookItem.Kind.RECIPES, Rarity.COMMON);
	public static final Item BESTIARY = book("bestiary", GuideBookItem.Kind.BESTIARY, Rarity.UNCOMMON);
	public static final Item ITEM_LEXICON = book("item_lexicon", GuideBookItem.Kind.LEXICON, Rarity.COMMON);
	public static final Item CRYSTAL_CHRONICLE = book("crystal_chronicle", GuideBookItem.Kind.CHRONICLE, Rarity.RARE);
	public static final Item JOKE_BOOK = book("joke_book", GuideBookItem.Kind.JOKES, Rarity.UNCOMMON);
	public static final Item STORAGE_KEY = item("storage_key", StorageKeyItem::new, new Item.Properties().stacksTo(1));
	public static final Item WIRELESS_TERMINAL = item("wireless_terminal", WirelessTerminalItem::new,
			new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));


	/** Amethyst Core: heart of every helper mob - only as (rare) loot from the Amethyst Robot. */
	public static final Item AMETHYST_CORE = item("amethyst_core", Item::new, new Item.Properties().stacksTo(16).rarity(Rarity.EPIC));

	/** Wireless Core: only as loot from the Amethyst Robot. */
	public static final Item WIRELESS_CORE = item("wireless_core", Item::new, new Item.Properties().stacksTo(16).rarity(Rarity.RARE));
	/** Crystal Obsidian and Amethyst Obsidian upgrade (v0.43 moved from EndContent into the core: replacement recipes without Adventure). */
	public static final Item CRYSTAL_OBSIDIAN = item("crystal_obsidian", Item::new,
			new Item.Properties().rarity(Rarity.RARE).fireResistant());
	public static final Item AMETHYST_OBSIDIAN_UPGRADE = item("amethyst_obsidian_upgrade_smithing_template",
			p -> new SmithingTemplateItem(
					Component.translatable("item.jtechstorage.amethyst_obsidian_upgrade.applies_to").withStyle(ChatFormatting.BLUE),
					Component.translatable("item.jtechstorage.amethyst_obsidian_upgrade.ingredients").withStyle(ChatFormatting.BLUE),
					Component.translatable("item.jtechstorage.amethyst_obsidian_upgrade.base_slot"),
					Component.translatable("item.jtechstorage.amethyst_obsidian_upgrade.additions_slot"),
					List.of(slot("helmet"), slot("sword"), slot("chestplate"), slot("pickaxe"), slot("leggings"), slot("axe"),
							slot("boots"), slot("hoe"), slot("shovel")),
					List.of(slot("ingot")), p),
			new Item.Properties().rarity(Rarity.EPIC).fireResistant());

	private static Identifier slot(String name) {
		return Identifier.withDefaultNamespace("container/slot/" + name);
	}


	// --- Upgrades -------------------------------------------------------

	public static final Item IRON_RANGE_UPGRADE = upgrade("iron_range_upgrade", UpgradeItem.Kind.RANGE, () -> ModConfig.get().network.ironRange, Rarity.COMMON);
	public static final Item GOLD_RANGE_UPGRADE = upgrade("gold_range_upgrade", UpgradeItem.Kind.RANGE, () -> ModConfig.get().network.goldRange, Rarity.UNCOMMON);
	public static final Item DIAMOND_RANGE_UPGRADE = upgrade("diamond_range_upgrade", UpgradeItem.Kind.RANGE, () -> ModConfig.get().network.diamondRange, Rarity.RARE);
	public static final Item NETHERITE_RANGE_UPGRADE = upgrade("netherite_range_upgrade", UpgradeItem.Kind.RANGE, () -> ModConfig.get().network.netheriteRange, Rarity.EPIC);
	/** Endgame: range 128, wireless terminal unlimited and cross-dimensional. */
	public static final Item ENDER_RANGE_UPGRADE = item("ender_range_upgrade",
			p -> new UpgradeItem(UpgradeItem.Kind.RANGE, () -> ModConfig.get().network.enderRange, true, p),
			new Item.Properties().stacksTo(16).rarity(Rarity.EPIC));

	public static final Item IRON_CAPACITY_UPGRADE = upgrade("iron_capacity_upgrade", UpgradeItem.Kind.CAPACITY, () -> ModConfig.get().network.ironCapacity, Rarity.COMMON);
	public static final Item GOLD_CAPACITY_UPGRADE = upgrade("gold_capacity_upgrade", UpgradeItem.Kind.CAPACITY, () -> ModConfig.get().network.goldCapacity, Rarity.UNCOMMON);
	public static final Item DIAMOND_CAPACITY_UPGRADE = upgrade("diamond_capacity_upgrade", UpgradeItem.Kind.CAPACITY, () -> ModConfig.get().network.diamondCapacity, Rarity.RARE);
	public static final Item NETHERITE_CAPACITY_UPGRADE = upgrade("netherite_capacity_upgrade", UpgradeItem.Kind.CAPACITY, () -> ModConfig.get().network.netheriteCapacity, Rarity.EPIC);
	/** Crystal Depth (v0.32): made of pure amethyst - above Ender/Netherite; range keeps the Ender abilities. */
	public static final Item CRYSTAL_RANGE_UPGRADE = item("crystal_range_upgrade",
			p -> new UpgradeItem(UpgradeItem.Kind.RANGE, () -> ModConfig.get().network.crystalRange, true, p),
			new Item.Properties().stacksTo(16).rarity(Rarity.EPIC));
	public static final Item CRYSTAL_CAPACITY_UPGRADE = upgrade("crystal_capacity_upgrade", UpgradeItem.Kind.CAPACITY, () -> ModConfig.get().network.crystalCapacity, Rarity.EPIC);

	// --- Amethyst Imp ---------------------------------------------------


	// --- Collector Moth -------------------------------------------------


	// --- Storage Grabber & Lightning Beetle ------------------------------





	// --- Cheldrogan (Peter's dragon form) -------------------------------







	public static final ResourceKey<EntityType<?>> JBARTELS_HOLOGRAM_KEY = ResourceKey.create(Registries.ENTITY_TYPE, JTechStorage.id("jbartels_hologram"));
	public static final EntityType<JbartelsHologram> JBARTELS_HOLOGRAM = EntityType.Builder.of(JbartelsHologram::new, MobCategory.MISC)
			.sized(0.6f, 1.9f)
			.clientTrackingRange(10)
			.build(JBARTELS_HOLOGRAM_KEY);

	public static final ResourceKey<EntityType<?>> CRYSTAL_SHARD_KEY = ResourceKey.create(Registries.ENTITY_TYPE, JTechStorage.id("crystal_shard"));
	public static final EntityType<CrystalShard> CRYSTAL_SHARD = EntityType.Builder.<CrystalShard>of(CrystalShard::new, MobCategory.MISC)
			.sized(0.25f, 0.25f)
			.clientTrackingRange(4)
			.updateInterval(10)
			.build(CRYSTAL_SHARD_KEY);


	// --- Backpacks ------------------------------------------------------

	public static final Item LEATHER_BACKPACK = backpack("leather_backpack", BackpackItem.Tier.LEATHER, Rarity.COMMON);
	public static final Item IRON_BACKPACK = backpack("iron_backpack", BackpackItem.Tier.IRON, Rarity.COMMON);
	public static final Item GOLD_BACKPACK = backpack("gold_backpack", BackpackItem.Tier.GOLD, Rarity.UNCOMMON);
	public static final Item DIAMOND_BACKPACK = backpack("diamond_backpack", BackpackItem.Tier.DIAMOND, Rarity.RARE);
	public static final Item NETHERITE_BACKPACK = backpack("netherite_backpack", BackpackItem.Tier.NETHERITE, Rarity.EPIC);

	private static Item backpack(String name, BackpackItem.Tier tier, Rarity rarity) {
		Item.Properties properties = new Item.Properties().stacksTo(1).rarity(rarity)
				.component(DataComponents.EQUIPPABLE, Equippable.builder(EquipmentSlot.CHEST)
						.setEquipSound(SoundEvents.ARMOR_EQUIP_LEATHER).setSwappable(false).setDamageOnHurt(false).build());

		if (tier == BackpackItem.Tier.NETHERITE) {
			properties = properties.fireResistant();
		}

		return item(name, p -> new BackpackItem(tier, p), properties);
	}

	/** Items helpers can wear as a hat (extensible via data pack). */
	public static final TagKey<Item> HELPER_HATS = TagKey.create(Registries.ITEM, JTechStorage.id("helper_hats"));

	// --- Menu -----------------------------------------------------------

	public static final ExtendedMenuType<StorageTerminalMenu, StorageTerminalMenu.OpenData> STORAGE_TERMINAL_MENU =
			new ExtendedMenuType<>(StorageTerminalMenu::new, StorageTerminalMenu.OpenData.STREAM_CODEC);
	public static final ExtendedMenuType<TeamMenu, TeamMenu.OpenData> TEAM_MENU =
			new ExtendedMenuType<>(TeamMenu::new, TeamMenu.OpenData.STREAM_CODEC);
	public static final ExtendedMenuType<TradeMenu, TradeMenu.OpenData> TRADE_MENU =
			new ExtendedMenuType<>(TradeMenu::new, TradeMenu.OpenData.STREAM_CODEC);

	private ModContent() {
	}

	private static ResourceKey<Block> blockKey(String name) {
		return ResourceKey.create(Registries.BLOCK, JTechStorage.id(name));
	}

	private static BlockBehaviour.Properties terminalProperties(ResourceKey<Block> key) {
		return BlockBehaviour.Properties.ofFullCopy(Blocks.POLISHED_DEEPSLATE).lightLevel(state -> 6).setId(key);
	}

	private static Item blockItem(Block block, ResourceKey<Block> key) {
		Item item = new BlockItem(block, new Item.Properties().useBlockDescriptionPrefix()
				.setId(ResourceKey.create(Registries.ITEM, key.identifier())));
		CREATIVE_ORDER.add(item);
		return item;
	}

	private static Item item(String name, Function<Item.Properties, Item> factory, Item.Properties properties) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, JTechStorage.id(name));
		Item item = Registry.register(BuiltInRegistries.ITEM, key, factory.apply(properties.setId(key)));
		CREATIVE_ORDER.add(item);
		return item;
	}

	private static Item book(String name, GuideBookItem.Kind kind, Rarity rarity) {
		return item(name, p -> new GuideBookItem(kind, p), new Item.Properties().stacksTo(1).rarity(rarity));
	}

	/** Value comes from the config (tooltip shows the server's values, see {@link UpgradeItem}). */
	private static Item upgrade(String name, UpgradeItem.Kind kind, IntSupplier value, Rarity rarity) {
		return item(name, p -> new UpgradeItem(kind, value, p), new Item.Properties().stacksTo(16).rarity(rarity));
	}

	public static void init() {
		Registry.register(BuiltInRegistries.BLOCK, STORAGE_TERMINAL_KEY, STORAGE_TERMINAL);
		Registry.register(BuiltInRegistries.ITEM, STORAGE_TERMINAL_KEY.identifier(), STORAGE_TERMINAL_ITEM);
		Registry.register(BuiltInRegistries.BLOCK, CRAFTING_TERMINAL_KEY, CRAFTING_TERMINAL);
		Registry.register(BuiltInRegistries.ITEM, CRAFTING_TERMINAL_KEY.identifier(), CRAFTING_TERMINAL_ITEM);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, JTechStorage.id("storage_terminal"), STORAGE_TERMINAL_BLOCK_ENTITY);
		Registry.register(BuiltInRegistries.MENU, JTechStorage.id("storage_terminal"), STORAGE_TERMINAL_MENU);
		Registry.register(BuiltInRegistries.BLOCK, TEAM_TERMINAL_KEY, TEAM_TERMINAL);
		Registry.register(BuiltInRegistries.ITEM, TEAM_TERMINAL_KEY.identifier(), TEAM_TERMINAL_ITEM);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, JTechStorage.id("team_terminal"), TEAM_TERMINAL_BLOCK_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK, TRADE_TERMINAL_KEY, TRADE_TERMINAL);
		Registry.register(BuiltInRegistries.ITEM, TRADE_TERMINAL_KEY.identifier(), TRADE_TERMINAL_ITEM);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, JTechStorage.id("trade_terminal"), TRADE_TERMINAL_BLOCK_ENTITY);
		Registry.register(BuiltInRegistries.MENU, JTechStorage.id("team"), TEAM_MENU);
		Registry.register(BuiltInRegistries.MENU, JTechStorage.id("trade"), TRADE_MENU);
		Registry.register(BuiltInRegistries.BLOCK, PLACED_BACKPACK_KEY, PLACED_BACKPACK);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, JTechStorage.id("placed_backpack"), PLACED_BACKPACK_BLOCK_ENTITY);
		Registry.register(BuiltInRegistries.ENTITY_TYPE, CRYSTAL_SHARD_KEY, CRYSTAL_SHARD);
		Registry.register(BuiltInRegistries.ENTITY_TYPE, JBARTELS_HOLOGRAM_KEY, JBARTELS_HOLOGRAM);
		FabricDefaultAttributeRegistry.register(JBARTELS_HOLOGRAM, JbartelsHologram.createAttributes());

		// Own creative tab with all of the mod's items
		Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, JTechStorage.id("main"),
				FabricCreativeModeTab.builder()
						.title(Component.translatable("itemGroup.jtechstorage"))
						.icon(() -> new ItemStack(STORAGE_TERMINAL_ITEM))
						.displayItems((parameters, output) -> {
							CREATIVE_ORDER.forEach(output::accept);
							FarmContent.creativeItems().forEach(output::accept);
							ChestContent.creativeItems().forEach(output::accept);
							ToolsContent.creativeItems().forEach(output::accept);
							LogisticsContent.creativeItems().forEach(output::accept);
							DisplayContent.creativeItems().forEach(output::accept);
							// Sub-mods (Helpers, Adventure) attach their items via addCreative
							CREATIVE_EXTRA.forEach(group -> group.get().forEach(output::accept));
						})
						.build());
	}
}
