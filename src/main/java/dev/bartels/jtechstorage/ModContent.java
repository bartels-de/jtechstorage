package dev.bartels.jtechstorage;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import net.minecraft.ChatFormatting;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.Items;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Unit;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuType;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;

import dev.bartels.jtechstorage.backpack.BackpackItem;
import dev.bartels.jtechstorage.backpack.PlacedBackpackBlock;
import dev.bartels.jtechstorage.backpack.PlacedBackpackBlockEntity;
import dev.bartels.jtechstorage.block.StorageTerminalBlock;
import dev.bartels.jtechstorage.block.StorageTerminalBlockEntity;
import dev.bartels.jtechstorage.block.PhoenixNestBlock;
import dev.bartels.jtechstorage.block.PhoenixNestBlockEntity;
import dev.bartels.jtechstorage.block.TeamTerminalBlock;
import dev.bartels.jtechstorage.block.TeamTerminalBlockEntity;
import dev.bartels.jtechstorage.block.TradeTerminalBlock;
import dev.bartels.jtechstorage.block.TradeTerminalBlockEntity;
import dev.bartels.jtechstorage.entity.AmethystPhoenix;
import dev.bartels.jtechstorage.entity.AmethystSprite;
import dev.bartels.jtechstorage.entity.Chel;
import dev.bartels.jtechstorage.entity.Ashley;
import dev.bartels.jtechstorage.entity.CollectorMoth;
import dev.bartels.jtechstorage.entity.CrystalPaladin;
import dev.bartels.jtechstorage.entity.CrystalShard;
import dev.bartels.jtechstorage.entity.AmethystRobot;
import dev.bartels.jtechstorage.entity.JbartelsHologram;
import dev.bartels.jtechstorage.entity.PeterChelParker;
import dev.bartels.jtechstorage.entity.Phoenix;
import dev.bartels.jtechstorage.entity.SparkBeetle;
import dev.bartels.jtechstorage.entity.StorageGriffin;
import dev.bartels.jtechstorage.item.HelperSpawnItem;
import dev.bartels.jtechstorage.item.AmethystDraughtItem;
import dev.bartels.jtechstorage.item.GuideBookItem;
import dev.bartels.jtechstorage.item.StorageKeyItem;
import dev.bartels.jtechstorage.item.UpgradeItem;
import dev.bartels.jtechstorage.item.WirelessTerminalItem;
import dev.bartels.jtechstorage.menu.ChelOrderMenu;
import dev.bartels.jtechstorage.menu.StorageTerminalMenu;
import dev.bartels.jtechstorage.menu.NestMenu;
import dev.bartels.jtechstorage.menu.TeamMenu;
import dev.bartels.jtechstorage.menu.TradeMenu;

/** Alle Registrierungen der Mod an einem Ort. */
public final class ModContent {
	/** Blöcke in diesem Tag können mit dem Schlüssel ins Netzwerk aufgenommen werden. */
	public static final TagKey<Block> NETWORK_STORAGE = TagKey.create(Registries.BLOCK, JTechStorage.id("network_storage"));

	/** Reichweite ohne Upgrade (Blöcke, kugelförmig). */
	public static final int BASE_RANGE = ModConfig.get().network.baseRange;
	/** Anzahl verknüpfbarer Kisten ohne Upgrade (Doppeltruhe = 1). */
	public static final int BASE_CAPACITY = ModConfig.get().network.baseCapacity;
	/** Drahtloses Terminal: Reichweite = Terminal-Reichweite × Faktor. */
	public static final int WIRELESS_RANGE_FACTOR = ModConfig.get().network.wirelessRangeFactor;

	private static final List<Item> CREATIVE_ORDER = new ArrayList<>();

	// --- Daten-Komponente: an welches Terminal ein Schlüssel/Funkterminal gekoppelt ist ---

	public static final DataComponentType<GlobalPos> LINKED_TERMINAL = Registry.register(
			BuiltInRegistries.DATA_COMPONENT_TYPE, JTechStorage.id("linked_terminal"),
			DataComponentType.<GlobalPos>builder().persistent(GlobalPos.CODEC).networkSynchronized(GlobalPos.STREAM_CODEC).build());

	/** Funk-Terminal mit eingebautem Funk-Kern (vom Amethyst-Roboter) – erst dann funktioniert es. */
	public static final DataComponentType<Unit> WIRELESS_CORE_INSTALLED = Registry.register(
			BuiltInRegistries.DATA_COMPONENT_TYPE, JTechStorage.id("wireless_core"),
			DataComponentType.<Unit>builder().persistent(Unit.CODEC).networkSynchronized(Unit.STREAM_CODEC).build());

	/** Alt (Route-Feder bis v0.24): Haltestellen A und B – nur noch zum Übernehmen alter Phönix-Items. */
	public static final DataComponentType<GlobalPos> ROUTE_A = Registry.register(
			BuiltInRegistries.DATA_COMPONENT_TYPE, JTechStorage.id("route_a"),
			DataComponentType.<GlobalPos>builder().persistent(GlobalPos.CODEC).networkSynchronized(GlobalPos.STREAM_CODEC).build());
	public static final DataComponentType<GlobalPos> ROUTE_B = Registry.register(
			BuiltInRegistries.DATA_COMPONENT_TYPE, JTechStorage.id("route_b"),
			DataComponentType.<GlobalPos>builder().persistent(GlobalPos.CODEC).networkSynchronized(GlobalPos.STREAM_CODEC).build());

	/** Flugplan: Haltestellen des Amethyst-Phönix in Flugreihenfolge. */
	public static final DataComponentType<List<dev.bartels.jtechstorage.item.FlightPlanItem.Stop>> FLIGHT_PLAN_STOPS = Registry.register(
			BuiltInRegistries.DATA_COMPONENT_TYPE, JTechStorage.id("flight_plan"),
			DataComponentType.<List<dev.bartels.jtechstorage.item.FlightPlanItem.Stop>>builder()
					.persistent(dev.bartels.jtechstorage.item.FlightPlanItem.Stop.CODEC.listOf())
					.networkSynchronized(dev.bartels.jtechstorage.item.FlightPlanItem.Stop.STREAM_CODEC.apply(ByteBufCodecs.list())).build());

	/** Chels Witzbuch: Nummern der schon gehörten Witze. */
	public static final DataComponentType<List<Integer>> JOKES_HEARD = Registry.register(
			BuiltInRegistries.DATA_COMPONENT_TYPE, JTechStorage.id("jokes_heard"),
			DataComponentType.<List<Integer>>builder().persistent(com.mojang.serialization.Codec.INT.listOf())
					.networkSynchronized(ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list())).build());

	// --- Blöcke ---------------------------------------------------------

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

	public static final ResourceKey<Block> PHOENIX_NEST_KEY = blockKey("phoenix_nest");
	public static final Block PHOENIX_NEST = new PhoenixNestBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.HAY_BLOCK)
			.noOcclusion().lightLevel(state -> 7).setId(PHOENIX_NEST_KEY));
	public static final Item PHOENIX_NEST_ITEM = blockItem(PHOENIX_NEST, PHOENIX_NEST_KEY);
	public static final BlockEntityType<PhoenixNestBlockEntity> PHOENIX_NEST_BLOCK_ENTITY =
			FabricBlockEntityTypeBuilder.create(PhoenixNestBlockEntity::new, PHOENIX_NEST).build();

	/** Phönix-Haltestelle: Landeplatz mit Name und Rastdauer. */
	public static final ResourceKey<Block> PHOENIX_STOP_KEY = blockKey("phoenix_stop");
	public static final Block PHOENIX_STOP = new dev.bartels.jtechstorage.block.PhoenixStopBlock(BlockBehaviour.Properties
			.ofFullCopy(Blocks.POLISHED_DEEPSLATE).lightLevel(state -> 9).setId(PHOENIX_STOP_KEY));
	public static final Item PHOENIX_STOP_ITEM = blockItem(PHOENIX_STOP, PHOENIX_STOP_KEY);
	public static final BlockEntityType<dev.bartels.jtechstorage.block.PhoenixStopBlockEntity> PHOENIX_STOP_BLOCK_ENTITY =
			FabricBlockEntityTypeBuilder.create(dev.bartels.jtechstorage.block.PhoenixStopBlockEntity::new, PHOENIX_STOP).build();

	/** Abgestellter Rucksack (kein eigenes Item – entsteht per Shift + Rechtsklick mit einem Rucksack). */
	public static final ResourceKey<Block> PLACED_BACKPACK_KEY = blockKey("placed_backpack");
	public static final Block PLACED_BACKPACK = new PlacedBackpackBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.HAY_BLOCK)
			.noOcclusion().strength(0.6f).setId(PLACED_BACKPACK_KEY));
	public static final BlockEntityType<PlacedBackpackBlockEntity> PLACED_BACKPACK_BLOCK_ENTITY =
			FabricBlockEntityTypeBuilder.create(PlacedBackpackBlockEntity::new, PLACED_BACKPACK).build();

	// --- Werkzeuge ------------------------------------------------------

	public static final Item GUIDE_BOOK = book("guide_book", GuideBookItem.Kind.HANDBOOK, Rarity.COMMON);
	public static final Item RECIPE_BOOK = book("recipe_book", GuideBookItem.Kind.RECIPES, Rarity.COMMON);
	public static final Item BESTIARY = book("bestiary", GuideBookItem.Kind.BESTIARY, Rarity.UNCOMMON);
	public static final Item ITEM_LEXICON = book("item_lexicon", GuideBookItem.Kind.LEXICON, Rarity.COMMON);
	public static final Item CRYSTAL_CHRONICLE = book("crystal_chronicle", GuideBookItem.Kind.CHRONICLE, Rarity.RARE);
	public static final Item JOKE_BOOK = book("joke_book", GuideBookItem.Kind.JOKES, Rarity.UNCOMMON);
	public static final Item STORAGE_KEY = item("storage_key", StorageKeyItem::new, new Item.Properties().stacksTo(1));
	public static final Item WIRELESS_TERMINAL = item("wireless_terminal", WirelessTerminalItem::new,
			new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));

	/** Resonanz-Zünder (früher Amethyst-Trunk): weckt den Amethyst-Roboter. */
	public static final Item AMETHYST_DRAUGHT = item("amethyst_draught", AmethystDraughtItem::new,
			new Item.Properties().stacksTo(1).rarity(Rarity.RARE)
					.component(DataComponents.CONSUMABLE, Consumable.builder().consumeSeconds(2.0f)
							.animation(ItemUseAnimation.TOOT_HORN)
							.sound(net.minecraft.core.Holder.direct(SoundEvents.BEACON_POWER_SELECT)).hasConsumeParticles(false).build()));

	/** Amethyst-Kern: Herz jedes Helfer-Mobs – nur als (seltene) Beute des Amethyst-Roboters. */
	public static final Item AMETHYST_CORE = item("amethyst_core", Item::new, new Item.Properties().stacksTo(16).rarity(Rarity.EPIC));

	/** Funk-Kern: nur als Beute des Amethyst-Roboters. */
	public static final Item WIRELESS_CORE = item("wireless_core", Item::new, new Item.Properties().stacksTo(16).rarity(Rarity.RARE));

	// --- Upgrades -------------------------------------------------------

	public static final Item IRON_RANGE_UPGRADE = upgrade("iron_range_upgrade", UpgradeItem.Kind.RANGE, () -> ModConfig.get().network.ironRange, Rarity.COMMON);
	public static final Item GOLD_RANGE_UPGRADE = upgrade("gold_range_upgrade", UpgradeItem.Kind.RANGE, () -> ModConfig.get().network.goldRange, Rarity.UNCOMMON);
	public static final Item DIAMOND_RANGE_UPGRADE = upgrade("diamond_range_upgrade", UpgradeItem.Kind.RANGE, () -> ModConfig.get().network.diamondRange, Rarity.RARE);
	public static final Item NETHERITE_RANGE_UPGRADE = upgrade("netherite_range_upgrade", UpgradeItem.Kind.RANGE, () -> ModConfig.get().network.netheriteRange, Rarity.EPIC);
	/** Endgame: Reichweite 128, Funk-Terminal grenzenlos und dimensionsübergreifend. */
	public static final Item ENDER_RANGE_UPGRADE = item("ender_range_upgrade",
			p -> new UpgradeItem(UpgradeItem.Kind.RANGE, () -> ModConfig.get().network.enderRange, true, p),
			new Item.Properties().stacksTo(16).rarity(Rarity.EPIC));

	public static final Item IRON_CAPACITY_UPGRADE = upgrade("iron_capacity_upgrade", UpgradeItem.Kind.CAPACITY, () -> ModConfig.get().network.ironCapacity, Rarity.COMMON);
	public static final Item GOLD_CAPACITY_UPGRADE = upgrade("gold_capacity_upgrade", UpgradeItem.Kind.CAPACITY, () -> ModConfig.get().network.goldCapacity, Rarity.UNCOMMON);
	public static final Item DIAMOND_CAPACITY_UPGRADE = upgrade("diamond_capacity_upgrade", UpgradeItem.Kind.CAPACITY, () -> ModConfig.get().network.diamondCapacity, Rarity.RARE);
	public static final Item NETHERITE_CAPACITY_UPGRADE = upgrade("netherite_capacity_upgrade", UpgradeItem.Kind.CAPACITY, () -> ModConfig.get().network.netheriteCapacity, Rarity.EPIC);

	// --- Amethyst-Wichtel ----------------------------------------------

	public static final ResourceKey<EntityType<?>> AMETHYST_SPRITE_KEY = ResourceKey.create(Registries.ENTITY_TYPE, JTechStorage.id("amethyst_sprite"));
	public static final EntityType<AmethystSprite> AMETHYST_SPRITE = EntityType.Builder.of(AmethystSprite::new, MobCategory.MISC)
			.sized(0.4f, 0.6f)
			.clientTrackingRange(8)
			.build(AMETHYST_SPRITE_KEY);
	public static final Item AMETHYST_SPRITE_ITEM = item("amethyst_sprite", p -> new HelperSpawnItem(() -> AMETHYST_SPRITE, p),
			new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));

	// --- Sammel-Falter --------------------------------------------------

	public static final ResourceKey<EntityType<?>> COLLECTOR_MOTH_KEY = ResourceKey.create(Registries.ENTITY_TYPE, JTechStorage.id("collector_moth"));
	public static final EntityType<CollectorMoth> COLLECTOR_MOTH = EntityType.Builder.of(CollectorMoth::new, MobCategory.MISC)
			.sized(0.5f, 0.4f)
			.clientTrackingRange(8)
			.build(COLLECTOR_MOTH_KEY);
	public static final Item COLLECTOR_MOTH_ITEM = item("collector_moth", p -> new HelperSpawnItem(() -> COLLECTOR_MOTH, p),
			new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));

	// --- Lager-Greif & Blitz-Käfer ---------------------------------------

	public static final ResourceKey<EntityType<?>> STORAGE_GRIFFIN_KEY = ResourceKey.create(Registries.ENTITY_TYPE, JTechStorage.id("storage_griffin"));
	public static final EntityType<StorageGriffin> STORAGE_GRIFFIN = EntityType.Builder.of(StorageGriffin::new, MobCategory.MISC)
			.sized(1.3f, 1.3f)
			.passengerAttachments(1.1f)
			.clientTrackingRange(10)
			.build(STORAGE_GRIFFIN_KEY);
	public static final Item STORAGE_GRIFFIN_ITEM = item("storage_griffin", p -> new HelperSpawnItem(() -> STORAGE_GRIFFIN, p),
			new Item.Properties().stacksTo(1).rarity(Rarity.RARE));

	public static final ResourceKey<EntityType<?>> SPARK_BEETLE_KEY = ResourceKey.create(Registries.ENTITY_TYPE, JTechStorage.id("spark_beetle"));
	public static final EntityType<SparkBeetle> SPARK_BEETLE = EntityType.Builder.of(SparkBeetle::new, MobCategory.MISC)
			.sized(0.55f, 0.5f)
			.clientTrackingRange(8)
			.build(SPARK_BEETLE_KEY);
	public static final Item SPARK_BEETLE_ITEM = item("spark_beetle", p -> new HelperSpawnItem(() -> SPARK_BEETLE, p),
			new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));

	public static final ResourceKey<EntityType<?>> CHEL_KEY = ResourceKey.create(Registries.ENTITY_TYPE, JTechStorage.id("chel"));
	public static final EntityType<Chel> CHEL = EntityType.Builder.of(Chel::new, MobCategory.MISC)
			.sized(0.6f, 1.05f)
			.clientTrackingRange(8)
			.build(CHEL_KEY);
	public static final Item CHEL_ITEM = item("chel", p -> new HelperSpawnItem(() -> CHEL, p),
			new Item.Properties().stacksTo(1).rarity(Rarity.EPIC));

	public static final ResourceKey<EntityType<?>> PETER_CHEL_PARKER_KEY = ResourceKey.create(Registries.ENTITY_TYPE,
			JTechStorage.id("peter_chel_parker"));
	public static final EntityType<PeterChelParker> PETER_CHEL_PARKER = EntityType.Builder.of(PeterChelParker::new, MobCategory.MISC)
			.sized(0.9f, 2.35f)
			.clientTrackingRange(10)
			.build(PETER_CHEL_PARKER_KEY);
	public static final Item PETER_CHEL_PARKER_ITEM = item("peter_chel_parker", p -> new HelperSpawnItem(() -> PETER_CHEL_PARKER, p),
			new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant());

	// --- Cheldrogan (Peters Drachenform) --------------------------------

	public static final ResourceKey<EntityType<?>> CHELDROGAN_KEY = ResourceKey.create(Registries.ENTITY_TYPE, JTechStorage.id("cheldrogan"));
	public static final EntityType<dev.bartels.jtechstorage.entity.Cheldrogan> CHELDROGAN = EntityType.Builder
			.of(dev.bartels.jtechstorage.entity.Cheldrogan::new, MobCategory.MISC)
			.sized(2.2f, 2.6f)
			.fireImmune()
			.clientTrackingRange(12)
			.build(CHELDROGAN_KEY);
	public static final Item CHELDROGAN_ITEM = item("cheldrogan", p -> new HelperSpawnItem(() -> CHELDROGAN, p),
			new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant());
	/** Resonanzkristall: wächst beim Kristall-Odem aus dem Boden und schließt Monster kurz ein (kein Item, keine Drops). */
	public static final ResourceKey<Block> RESONANCE_CRYSTAL_KEY = ResourceKey.create(Registries.BLOCK, JTechStorage.id("resonance_crystal"));
	public static final Block RESONANCE_CRYSTAL = new dev.bartels.jtechstorage.block.ResonanceCrystalBlock(BlockBehaviour.Properties.of()
			.mapColor(net.minecraft.world.level.material.MapColor.COLOR_PURPLE).strength(0.8f).noLootTable().noOcclusion()
			.sound(net.minecraft.world.level.block.SoundType.AMETHYST_CLUSTER).lightLevel(state -> 7)
			.pushReaction(net.minecraft.world.level.material.PushReaction.POPPED).setId(RESONANCE_CRYSTAL_KEY));
	/** Drachenherz: verwandelt Peter Chel Parker in Cheldrogan. */
	public static final Item DRAGON_HEART = item("dragon_heart", Item::new,
			new Item.Properties().stacksTo(16).rarity(Rarity.EPIC).fireResistant()
					.component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true)
					.component(DataComponents.LORE, new ItemLore(List.of(Component.translatable("item.jtechstorage.dragon_heart.lore")
							.withStyle(ChatFormatting.LIGHT_PURPLE).withStyle(style -> style.withItalic(false))))));

	public static final ResourceKey<EntityType<?>> CRYSTAL_PALADIN_KEY = ResourceKey.create(Registries.ENTITY_TYPE,
			JTechStorage.id("crystal_paladin"));
	public static final EntityType<CrystalPaladin> CRYSTAL_PALADIN = EntityType.Builder.of(CrystalPaladin::new, MobCategory.MISC)
			.sized(0.9f, 2.45f)
			.clientTrackingRange(10)
			.build(CRYSTAL_PALADIN_KEY);
	/** Entfernt in v0.20 – nur noch zur Umwandlung alter Items in Peter. */
	public static final Item CRYSTAL_PALADIN_ITEM = Registry.register(BuiltInRegistries.ITEM, JTechStorage.id("crystal_paladin"),
			new dev.bartels.jtechstorage.item.LegacyPaladinItem(new Item.Properties().stacksTo(1)
					.setId(ResourceKey.create(Registries.ITEM, JTechStorage.id("crystal_paladin")))));

	public static final ResourceKey<EntityType<?>> PHOENIX_KEY = ResourceKey.create(Registries.ENTITY_TYPE, JTechStorage.id("phoenix"));
	public static final EntityType<Phoenix> PHOENIX = EntityType.Builder.of(Phoenix::new, MobCategory.MISC)
			.sized(0.8f, 0.9f)
			.fireImmune()
			.clientTrackingRange(10)
			.build(PHOENIX_KEY);
	public static final Item PHOENIX_ITEM = item("phoenix", p -> new HelperSpawnItem(() -> PHOENIX, p),
			new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant());

	public static final ResourceKey<EntityType<?>> AMETHYST_FOX_KEY = ResourceKey.create(Registries.ENTITY_TYPE, JTechStorage.id("amethyst_fox"));
	public static final EntityType<dev.bartels.jtechstorage.entity.AmethystFox> AMETHYST_FOX = EntityType.Builder
			.of(dev.bartels.jtechstorage.entity.AmethystFox::new, MobCategory.MISC)
			.sized(0.7f, 0.75f)
			.clientTrackingRange(8)
			.build(AMETHYST_FOX_KEY);
	public static final Item AMETHYST_FOX_ITEM = item("amethyst_fox", p -> new HelperSpawnItem(() -> AMETHYST_FOX, p),
			new Item.Properties().stacksTo(1).rarity(Rarity.EPIC));

	public static final ResourceKey<EntityType<?>> AMETHYST_PHOENIX_KEY = ResourceKey.create(Registries.ENTITY_TYPE, JTechStorage.id("amethyst_phoenix"));
	public static final EntityType<AmethystPhoenix> AMETHYST_PHOENIX = EntityType.Builder.of(AmethystPhoenix::new, MobCategory.MISC)
			.sized(2.2f, 1.8f)
			.fireImmune()
			.clientTrackingRange(12)
			.build(AMETHYST_PHOENIX_KEY);
	/** Nicht craftbar – entsteht durch Einpacken eines Amethyst-Phönix. */
	public static final Item AMETHYST_PHOENIX_ITEM = item("amethyst_phoenix", p -> new HelperSpawnItem(() -> AMETHYST_PHOENIX, p),
			new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant());
	public static final Item FLIGHT_PLAN = item("flight_plan", dev.bartels.jtechstorage.item.FlightPlanItem::new,
			new Item.Properties().stacksTo(1).rarity(Rarity.RARE));
	/** Entfernt in v0.25 – alte Federn werden im Inventar zum Flugplan. */
	public static final Item ROUTE_FEATHER = Registry.register(BuiltInRegistries.ITEM, JTechStorage.id("route_feather"),
			new dev.bartels.jtechstorage.item.RouteFeatherItem(new Item.Properties().stacksTo(1)
					.setId(ResourceKey.create(Registries.ITEM, JTechStorage.id("route_feather")))));

	public static final ResourceKey<EntityType<?>> AMETHYST_ROBOT_KEY = ResourceKey.create(Registries.ENTITY_TYPE, JTechStorage.id("amethyst_robot"));
	public static final EntityType<AmethystRobot> AMETHYST_ROBOT = EntityType.Builder.of(AmethystRobot::new, MobCategory.MONSTER)
			.sized(2.8f, 5.0f)
			.clientTrackingRange(12)
			.build(AMETHYST_ROBOT_KEY);

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

	public static final ResourceKey<EntityType<?>> ASHLEY_KEY = ResourceKey.create(Registries.ENTITY_TYPE, JTechStorage.id("ashley"));
	public static final EntityType<Ashley> ASHLEY = EntityType.Builder.of(Ashley::new, MobCategory.MISC)
			.sized(0.7f, 0.8f)
			.clientTrackingRange(8)
			.build(ASHLEY_KEY);
	public static final Item ASHLEY_ITEM = item("ashley", p -> new HelperSpawnItem(() -> ASHLEY, p),
			new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));

	// --- Rucksäcke ------------------------------------------------------

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

	/** Items, die Helfer als Hut tragen können (Datenpaket-erweiterbar). */
	public static final TagKey<Item> HELPER_HATS = TagKey.create(Registries.ITEM, JTechStorage.id("helper_hats"));

	// --- Menü -----------------------------------------------------------

	public static final ExtendedMenuType<StorageTerminalMenu, StorageTerminalMenu.OpenData> STORAGE_TERMINAL_MENU =
			new ExtendedMenuType<>(StorageTerminalMenu::new, StorageTerminalMenu.OpenData.STREAM_CODEC);
	public static final ExtendedMenuType<ChelOrderMenu, ChelOrderMenu.OpenData> CHEL_ORDER_MENU =
			new ExtendedMenuType<>(ChelOrderMenu::new, ChelOrderMenu.OpenData.STREAM_CODEC);
	public static final ExtendedMenuType<TeamMenu, TeamMenu.OpenData> TEAM_MENU =
			new ExtendedMenuType<>(TeamMenu::new, TeamMenu.OpenData.STREAM_CODEC);
	public static final ExtendedMenuType<NestMenu, NestMenu.OpenData> NEST_MENU =
			new ExtendedMenuType<>(NestMenu::new, NestMenu.OpenData.STREAM_CODEC);
	public static final ExtendedMenuType<dev.bartels.jtechstorage.menu.AmethystPhoenixMenu, dev.bartels.jtechstorage.menu.AmethystPhoenixMenu.OpenData> AMETHYST_PHOENIX_MENU =
			new ExtendedMenuType<>(dev.bartels.jtechstorage.menu.AmethystPhoenixMenu::new, dev.bartels.jtechstorage.menu.AmethystPhoenixMenu.OpenData.STREAM_CODEC);
	public static final ExtendedMenuType<dev.bartels.jtechstorage.menu.PhoenixStopMenu, dev.bartels.jtechstorage.menu.PhoenixStopMenu.OpenData> PHOENIX_STOP_MENU =
			new ExtendedMenuType<>(dev.bartels.jtechstorage.menu.PhoenixStopMenu::new, dev.bartels.jtechstorage.menu.PhoenixStopMenu.OpenData.STREAM_CODEC);
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

	/** Wert kommt aus der Konfiguration (Tooltip zeigt die Werte des Servers, siehe {@link UpgradeItem}). */
	private static Item upgrade(String name, UpgradeItem.Kind kind, java.util.function.IntSupplier value, Rarity rarity) {
		return item(name, p -> new UpgradeItem(kind, value, p), new Item.Properties().stacksTo(16).rarity(rarity));
	}

	public static void init() {
		Registry.register(BuiltInRegistries.BLOCK, STORAGE_TERMINAL_KEY, STORAGE_TERMINAL);
		Registry.register(BuiltInRegistries.ITEM, STORAGE_TERMINAL_KEY.identifier(), STORAGE_TERMINAL_ITEM);
		Registry.register(BuiltInRegistries.BLOCK, CRAFTING_TERMINAL_KEY, CRAFTING_TERMINAL);
		Registry.register(BuiltInRegistries.ITEM, CRAFTING_TERMINAL_KEY.identifier(), CRAFTING_TERMINAL_ITEM);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, JTechStorage.id("storage_terminal"), STORAGE_TERMINAL_BLOCK_ENTITY);
		Registry.register(BuiltInRegistries.MENU, JTechStorage.id("storage_terminal"), STORAGE_TERMINAL_MENU);
		Registry.register(BuiltInRegistries.MENU, JTechStorage.id("chel_orders"), CHEL_ORDER_MENU);
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
		Registry.register(BuiltInRegistries.BLOCK, PHOENIX_NEST_KEY, PHOENIX_NEST);
		Registry.register(BuiltInRegistries.ITEM, PHOENIX_NEST_KEY.identifier(), PHOENIX_NEST_ITEM);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, JTechStorage.id("phoenix_nest"), PHOENIX_NEST_BLOCK_ENTITY);
		Registry.register(BuiltInRegistries.MENU, JTechStorage.id("nest"), NEST_MENU);
		Registry.register(BuiltInRegistries.BLOCK, PHOENIX_STOP_KEY, PHOENIX_STOP);
		Registry.register(BuiltInRegistries.ITEM, PHOENIX_STOP_KEY.identifier(), PHOENIX_STOP_ITEM);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, JTechStorage.id("phoenix_stop"), PHOENIX_STOP_BLOCK_ENTITY);
		Registry.register(BuiltInRegistries.MENU, JTechStorage.id("phoenix_stop"), PHOENIX_STOP_MENU);
		Registry.register(BuiltInRegistries.ENTITY_TYPE, AMETHYST_FOX_KEY, AMETHYST_FOX);
		FabricDefaultAttributeRegistry.register(AMETHYST_FOX, dev.bartels.jtechstorage.entity.AmethystFox.createAttributes());
		Registry.register(BuiltInRegistries.ENTITY_TYPE, AMETHYST_PHOENIX_KEY, AMETHYST_PHOENIX);
		FabricDefaultAttributeRegistry.register(AMETHYST_PHOENIX, AmethystPhoenix.createAttributes());
		Registry.register(BuiltInRegistries.MENU, JTechStorage.id("amethyst_phoenix"), AMETHYST_PHOENIX_MENU);
		Registry.register(BuiltInRegistries.ENTITY_TYPE, PHOENIX_KEY, PHOENIX);
		FabricDefaultAttributeRegistry.register(PHOENIX, Phoenix.createAttributes());
		Registry.register(BuiltInRegistries.ENTITY_TYPE, AMETHYST_SPRITE_KEY, AMETHYST_SPRITE);
		FabricDefaultAttributeRegistry.register(AMETHYST_SPRITE, AmethystSprite.createAttributes());
		Registry.register(BuiltInRegistries.ENTITY_TYPE, COLLECTOR_MOTH_KEY, COLLECTOR_MOTH);
		FabricDefaultAttributeRegistry.register(COLLECTOR_MOTH, CollectorMoth.createAttributes());
		Registry.register(BuiltInRegistries.ENTITY_TYPE, STORAGE_GRIFFIN_KEY, STORAGE_GRIFFIN);
		FabricDefaultAttributeRegistry.register(STORAGE_GRIFFIN, StorageGriffin.createAttributes());
		Registry.register(BuiltInRegistries.ENTITY_TYPE, SPARK_BEETLE_KEY, SPARK_BEETLE);
		FabricDefaultAttributeRegistry.register(SPARK_BEETLE, SparkBeetle.createAttributes());
		Registry.register(BuiltInRegistries.ENTITY_TYPE, CHEL_KEY, CHEL);
		FabricDefaultAttributeRegistry.register(CHEL, Chel.createAttributes());
		Registry.register(BuiltInRegistries.ENTITY_TYPE, PETER_CHEL_PARKER_KEY, PETER_CHEL_PARKER);
		FabricDefaultAttributeRegistry.register(PETER_CHEL_PARKER, PeterChelParker.createAttributes());
		Registry.register(BuiltInRegistries.ENTITY_TYPE, CHELDROGAN_KEY, CHELDROGAN);
		Registry.register(BuiltInRegistries.BLOCK, RESONANCE_CRYSTAL_KEY, RESONANCE_CRYSTAL);
		FabricDefaultAttributeRegistry.register(CHELDROGAN, dev.bartels.jtechstorage.entity.Cheldrogan.createAttributes());
		Registry.register(BuiltInRegistries.ENTITY_TYPE, CRYSTAL_PALADIN_KEY, CRYSTAL_PALADIN);
		FabricDefaultAttributeRegistry.register(CRYSTAL_PALADIN, CrystalPaladin.createAttributes());
		Registry.register(BuiltInRegistries.ENTITY_TYPE, CRYSTAL_SHARD_KEY, CRYSTAL_SHARD);
		Registry.register(BuiltInRegistries.ENTITY_TYPE, AMETHYST_ROBOT_KEY, AMETHYST_ROBOT);
		Registry.register(BuiltInRegistries.ENTITY_TYPE, JBARTELS_HOLOGRAM_KEY, JBARTELS_HOLOGRAM);
		FabricDefaultAttributeRegistry.register(JBARTELS_HOLOGRAM, JbartelsHologram.createAttributes());
		FabricDefaultAttributeRegistry.register(AMETHYST_ROBOT, AmethystRobot.createAttributes());
		Registry.register(BuiltInRegistries.ENTITY_TYPE, ASHLEY_KEY, ASHLEY);
		FabricDefaultAttributeRegistry.register(ASHLEY, Ashley.createAttributes());

		// Eigener Kreativ-Tab mit allen Items der Mod (End-Inhalte hängt EndContent an)
		Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, JTechStorage.id("main"),
				net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab.builder()
						.title(Component.translatable("itemGroup.jtechstorage"))
						.icon(() -> new net.minecraft.world.item.ItemStack(STORAGE_TERMINAL_ITEM))
						.displayItems((parameters, output) -> {
							CREATIVE_ORDER.forEach(output::accept);
							dev.bartels.jtechstorage.end.EndContent.creativeItems().forEach(output::accept);
							dev.bartels.jtechstorage.storm.StormContent.creativeItems().forEach(output::accept);
							dev.bartels.jtechstorage.wolf.WolfContent.creativeItems().forEach(output::accept);
							dev.bartels.jtechstorage.companion.CompanionContent.creativeItems().forEach(output::accept);
							dev.bartels.jtechstorage.farm.FarmContent.creativeItems().forEach(output::accept);
							dev.bartels.jtechstorage.chest.ChestContent.creativeItems().forEach(output::accept);
						})
						.build());
	}
}
