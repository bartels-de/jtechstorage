package dev.bartels.jtechstorage.helpers;

import dev.bartels.jtechstorage.JTechStorage;
import dev.bartels.jtechstorage.block.PhoenixNestBlock;
import dev.bartels.jtechstorage.block.PhoenixNestBlockEntity;
import dev.bartels.jtechstorage.block.PhoenixStopBlock;
import dev.bartels.jtechstorage.block.PhoenixStopBlockEntity;
import dev.bartels.jtechstorage.block.ResonanceCrystalBlock;
import dev.bartels.jtechstorage.block.SignalLanternBlock;
import dev.bartels.jtechstorage.entity.AmethystPhoenix;
import dev.bartels.jtechstorage.entity.AmethystSprite;
import dev.bartels.jtechstorage.entity.Ashley;
import dev.bartels.jtechstorage.entity.Chel;
import dev.bartels.jtechstorage.entity.Cheldrogan;
import dev.bartels.jtechstorage.entity.CollectorMoth;
import dev.bartels.jtechstorage.entity.CrystalPaladin;
import dev.bartels.jtechstorage.entity.PeterChelParker;
import dev.bartels.jtechstorage.entity.Phoenix;
import dev.bartels.jtechstorage.entity.SparkBeetle;
import dev.bartels.jtechstorage.entity.StorageGriffin;
import dev.bartels.jtechstorage.item.FlightPlanItem;
import dev.bartels.jtechstorage.item.HelperSpawnItem;
import dev.bartels.jtechstorage.item.LegacyPaladinItem;
import dev.bartels.jtechstorage.item.RouteFeatherItem;
import dev.bartels.jtechstorage.menu.AmethystPhoenixMenu;
import dev.bartels.jtechstorage.menu.ChelOrderMenu;
import dev.bartels.jtechstorage.menu.NestMenu;
import dev.bartels.jtechstorage.menu.PhoenixStopMenu;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuType;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

/** Sub-mod “Helpers &amp; Phoenix” (v0.43): helpers, phoenix mail, Amethyst Phoenix – formerly in ModContent. */
public final class HelpersContent {
	private static final List<Item> CREATIVE = new ArrayList<>();

	/** Legacy (route feather until v0.24): stops A and B – only used to convert old phoenix items. */
	public static final DataComponentType<GlobalPos> ROUTE_A = Registry.register(
			BuiltInRegistries.DATA_COMPONENT_TYPE, JTechStorage.id("route_a"),
			DataComponentType.<GlobalPos>builder().persistent(GlobalPos.CODEC).networkSynchronized(GlobalPos.STREAM_CODEC).build());

	public static final DataComponentType<GlobalPos> ROUTE_B = Registry.register(
			BuiltInRegistries.DATA_COMPONENT_TYPE, JTechStorage.id("route_b"),
			DataComponentType.<GlobalPos>builder().persistent(GlobalPos.CODEC).networkSynchronized(GlobalPos.STREAM_CODEC).build());

	/** Flight plan: stops of the Amethyst Phoenix in flight order. */
	public static final DataComponentType<List<FlightPlanItem.Stop>> FLIGHT_PLAN_STOPS = Registry.register(
			BuiltInRegistries.DATA_COMPONENT_TYPE, JTechStorage.id("flight_plan"),
			DataComponentType.<List<FlightPlanItem.Stop>>builder()
					.persistent(FlightPlanItem.Stop.CODEC.listOf())
					.networkSynchronized(FlightPlanItem.Stop.STREAM_CODEC.apply(ByteBufCodecs.list())).build());

	public static final ResourceKey<Block> PHOENIX_NEST_KEY = blockKey("phoenix_nest");

	public static final Block PHOENIX_NEST = new PhoenixNestBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.HAY_BLOCK)
			.noOcclusion().lightLevel(state -> 7).setId(PHOENIX_NEST_KEY));

	public static final Item PHOENIX_NEST_ITEM = blockItem(PHOENIX_NEST, PHOENIX_NEST_KEY);

	public static final BlockEntityType<PhoenixNestBlockEntity> PHOENIX_NEST_BLOCK_ENTITY =
			FabricBlockEntityTypeBuilder.create(PhoenixNestBlockEntity::new, PHOENIX_NEST).build();

	/** Phoenix stop: landing spot with name and rest duration. */
	public static final ResourceKey<Block> PHOENIX_STOP_KEY = blockKey("phoenix_stop");

	public static final Block PHOENIX_STOP = new PhoenixStopBlock(BlockBehaviour.Properties
			.ofFullCopy(Blocks.POLISHED_DEEPSLATE).lightLevel(state -> 9).setId(PHOENIX_STOP_KEY));

	public static final Item PHOENIX_STOP_ITEM = blockItem(PHOENIX_STOP, PHOENIX_STOP_KEY);

	public static final BlockEntityType<PhoenixStopBlockEntity> PHOENIX_STOP_BLOCK_ENTITY =
			FabricBlockEntityTypeBuilder.create(PhoenixStopBlockEntity::new, PHOENIX_STOP).build();

	public static final ResourceKey<EntityType<?>> AMETHYST_SPRITE_KEY = ResourceKey.create(Registries.ENTITY_TYPE, JTechStorage.id("amethyst_sprite"));

	public static final EntityType<AmethystSprite> AMETHYST_SPRITE = EntityType.Builder.of(AmethystSprite::new, MobCategory.MISC)
			.sized(0.4f, 0.6f)
			.clientTrackingRange(8)
			.build(AMETHYST_SPRITE_KEY);

	public static final Item AMETHYST_SPRITE_ITEM = item("amethyst_sprite", p -> new HelperSpawnItem(() -> AMETHYST_SPRITE, p),
			new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));

	public static final ResourceKey<EntityType<?>> COLLECTOR_MOTH_KEY = ResourceKey.create(Registries.ENTITY_TYPE, JTechStorage.id("collector_moth"));

	public static final EntityType<CollectorMoth> COLLECTOR_MOTH = EntityType.Builder.of(CollectorMoth::new, MobCategory.MISC)
			.sized(0.5f, 0.4f)
			.clientTrackingRange(8)
			.build(COLLECTOR_MOTH_KEY);

	public static final Item COLLECTOR_MOTH_ITEM = item("collector_moth", p -> new HelperSpawnItem(() -> COLLECTOR_MOTH, p),
			new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));

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

	public static final ResourceKey<EntityType<?>> CHELDROGAN_KEY = ResourceKey.create(Registries.ENTITY_TYPE, JTechStorage.id("cheldrogan"));

	public static final EntityType<Cheldrogan> CHELDROGAN = EntityType.Builder
			.of(Cheldrogan::new, MobCategory.MISC)
			.sized(2.2f, 2.6f)
			.fireImmune()
			.clientTrackingRange(12)
			.build(CHELDROGAN_KEY);

	public static final Item CHELDROGAN_ITEM = item("cheldrogan", p -> new HelperSpawnItem(() -> CHELDROGAN, p),
			new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant());

	/** Resonance crystal: grows out of the ground from the crystal breath and briefly traps monsters (no item, no drops). */
	public static final ResourceKey<Block> RESONANCE_CRYSTAL_KEY = ResourceKey.create(Registries.BLOCK, JTechStorage.id("resonance_crystal"));

	public static final Block RESONANCE_CRYSTAL = new ResonanceCrystalBlock(BlockBehaviour.Properties.of()
			.mapColor(MapColor.COLOR_PURPLE).strength(0.8f).noLootTable().noOcclusion()
			.sound(SoundType.AMETHYST_CLUSTER).lightLevel(state -> 7)
			.pushReaction(PushReaction.POPPED).setId(RESONANCE_CRYSTAL_KEY));

	/** Dragon heart: turns Peter Chel Parker into Cheldrogan. */
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

	/** Removed in v0.20 – only used to convert old items into Peter. */
	public static final Item CRYSTAL_PALADIN_ITEM = Registry.register(BuiltInRegistries.ITEM, JTechStorage.id("crystal_paladin"),
			new LegacyPaladinItem(new Item.Properties().stacksTo(1)
					.setId(ResourceKey.create(Registries.ITEM, JTechStorage.id("crystal_paladin")))));

	public static final ResourceKey<EntityType<?>> PHOENIX_KEY = ResourceKey.create(Registries.ENTITY_TYPE, JTechStorage.id("phoenix"));

	public static final EntityType<Phoenix> PHOENIX = EntityType.Builder.of(Phoenix::new, MobCategory.MISC)
			.sized(0.8f, 0.9f)
			.fireImmune()
			.clientTrackingRange(10)
			.build(PHOENIX_KEY);

	public static final Item PHOENIX_ITEM = item("phoenix", p -> new HelperSpawnItem(() -> PHOENIX, p),
			new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant());

	public static final ResourceKey<EntityType<?>> AMETHYST_PHOENIX_KEY = ResourceKey.create(Registries.ENTITY_TYPE, JTechStorage.id("amethyst_phoenix"));

	public static final EntityType<AmethystPhoenix> AMETHYST_PHOENIX = EntityType.Builder.of(AmethystPhoenix::new, MobCategory.MISC)
			.sized(2.2f, 1.8f)
			.fireImmune()
			.clientTrackingRange(12)
			.build(AMETHYST_PHOENIX_KEY);

	/** Not craftable – obtained by packing up an Amethyst Phoenix. */
	public static final Item AMETHYST_PHOENIX_ITEM = item("amethyst_phoenix", p -> new HelperSpawnItem(() -> AMETHYST_PHOENIX, p),
			new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant());

	public static final Item FLIGHT_PLAN = item("flight_plan", FlightPlanItem::new,
			new Item.Properties().stacksTo(1).rarity(Rarity.RARE));

	/** Removed in v0.25 – old feathers become the flight plan in the inventory. */
	public static final Item ROUTE_FEATHER = Registry.register(BuiltInRegistries.ITEM, JTechStorage.id("route_feather"),
			new RouteFeatherItem(new Item.Properties().stacksTo(1)
					.setId(ResourceKey.create(Registries.ITEM, JTechStorage.id("route_feather")))));

	public static final ResourceKey<EntityType<?>> ASHLEY_KEY = ResourceKey.create(Registries.ENTITY_TYPE, JTechStorage.id("ashley"));

	public static final EntityType<Ashley> ASHLEY = EntityType.Builder.of(Ashley::new, MobCategory.MISC)
			.sized(0.7f, 0.8f)
			.clientTrackingRange(8)
			.build(ASHLEY_KEY);

	public static final Item ASHLEY_ITEM = item("ashley", p -> new HelperSpawnItem(() -> ASHLEY, p),
			new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));

	public static final ExtendedMenuType<ChelOrderMenu, ChelOrderMenu.OpenData> CHEL_ORDER_MENU =
			new ExtendedMenuType<>(ChelOrderMenu::new, ChelOrderMenu.OpenData.STREAM_CODEC);

	public static final ExtendedMenuType<NestMenu, NestMenu.OpenData> NEST_MENU =
			new ExtendedMenuType<>(NestMenu::new, NestMenu.OpenData.STREAM_CODEC);

	public static final ExtendedMenuType<AmethystPhoenixMenu, AmethystPhoenixMenu.OpenData> AMETHYST_PHOENIX_MENU =
			new ExtendedMenuType<>(AmethystPhoenixMenu::new, AmethystPhoenixMenu.OpenData.STREAM_CODEC);

	public static final ExtendedMenuType<PhoenixStopMenu, PhoenixStopMenu.OpenData> PHOENIX_STOP_MENU =
			new ExtendedMenuType<>(PhoenixStopMenu::new, PhoenixStopMenu.OpenData.STREAM_CODEC);

	/** Signal lantern (v0.43 from StormContent): glows while an Amethyst Phoenix rests at the stop. */
	public static final ResourceKey<Block> SIGNAL_LANTERN_KEY = ResourceKey.create(Registries.BLOCK, JTechStorage.id("signal_lantern"));
	public static final Block SIGNAL_LANTERN = new SignalLanternBlock(
			BlockBehaviour.Properties.ofFullCopy(Blocks.LANTERN)
			.lightLevel(state -> state.getValue(SignalLanternBlock.LIT) ? 13 : 3).setId(SIGNAL_LANTERN_KEY));
	public static final Item SIGNAL_LANTERN_ITEM = blockItem(SIGNAL_LANTERN, SIGNAL_LANTERN_KEY);

	private HelpersContent() {
	}

	private static ResourceKey<Block> blockKey(String name) {
		return ResourceKey.create(Registries.BLOCK, JTechStorage.id(name));
	}

	private static Item blockItem(Block block, ResourceKey<Block> key) {
		Item item = new BlockItem(block, new Item.Properties().useBlockDescriptionPrefix()
				.setId(ResourceKey.create(Registries.ITEM, key.identifier())));
		CREATIVE.add(item);
		return item;
	}

	private static Item item(String name, Function<Item.Properties, Item> factory, Item.Properties properties) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, JTechStorage.id(name));
		Item item = Registry.register(BuiltInRegistries.ITEM, key, factory.apply(properties.setId(key)));
		CREATIVE.add(item);
		return item;
	}

	public static List<Item> creativeItems() {
		return CREATIVE;
	}

	public static void init() {
		Registry.register(BuiltInRegistries.BLOCK, SIGNAL_LANTERN_KEY, SIGNAL_LANTERN);
		Registry.register(BuiltInRegistries.ITEM, SIGNAL_LANTERN_KEY.identifier(), SIGNAL_LANTERN_ITEM);
		Registry.register(BuiltInRegistries.MENU, JTechStorage.id("chel_orders"), CHEL_ORDER_MENU);
		Registry.register(BuiltInRegistries.BLOCK, PHOENIX_NEST_KEY, PHOENIX_NEST);
		Registry.register(BuiltInRegistries.ITEM, PHOENIX_NEST_KEY.identifier(), PHOENIX_NEST_ITEM);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, JTechStorage.id("phoenix_nest"), PHOENIX_NEST_BLOCK_ENTITY);
		Registry.register(BuiltInRegistries.MENU, JTechStorage.id("nest"), NEST_MENU);
		Registry.register(BuiltInRegistries.BLOCK, PHOENIX_STOP_KEY, PHOENIX_STOP);
		Registry.register(BuiltInRegistries.ITEM, PHOENIX_STOP_KEY.identifier(), PHOENIX_STOP_ITEM);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, JTechStorage.id("phoenix_stop"), PHOENIX_STOP_BLOCK_ENTITY);
		Registry.register(BuiltInRegistries.MENU, JTechStorage.id("phoenix_stop"), PHOENIX_STOP_MENU);
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
		FabricDefaultAttributeRegistry.register(CHELDROGAN, Cheldrogan.createAttributes());
		Registry.register(BuiltInRegistries.ENTITY_TYPE, CRYSTAL_PALADIN_KEY, CRYSTAL_PALADIN);
		FabricDefaultAttributeRegistry.register(CRYSTAL_PALADIN, CrystalPaladin.createAttributes());
		Registry.register(BuiltInRegistries.ENTITY_TYPE, ASHLEY_KEY, ASHLEY);
		FabricDefaultAttributeRegistry.register(ASHLEY, Ashley.createAttributes());
	}
}
