package dev.bartels.jtechstorage.end;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SmithingTemplateItem;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorMaterials;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.structure.Structure;

import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;

import dev.bartels.jtechstorage.JTechStorage;
import dev.bartels.jtechstorage.entity.AmethystSpider;
import dev.bartels.jtechstorage.entity.WebShot;

/**
 * End-Update (v0.19): Kristallhort (Struktur im äußeren End), Kristall-Altar, Große Amethyst-Spinne (Endboss),
 * Hort-Kompass, Kristall-Kokon, Amethyst-Obsidian-Upgrade + Amethyst-Obsidian-Ausrüstung (besser als Netherit).
 */
public final class EndContent {
	private static final List<Item> GEAR = new ArrayList<>();
	private static final List<Item> MISC = new ArrayList<>();

	/** Struktur-Tag für den Hort-Kompass und Fortschritte. */
	public static final TagKey<Structure> CRYSTAL_LAIR = TagKey.create(Registries.STRUCTURE, JTechStorage.id("crystal_lair"));
	public static final TagKey<Item> REPAIRS_AMETHYST_OBSIDIAN = TagKey.create(Registries.ITEM, JTechStorage.id("repairs_amethyst_obsidian"));
	public static final ResourceKey<EquipmentAsset> AMETHYST_OBSIDIAN_ASSET = ResourceKey.create(EquipmentAssets.ROOT_ID,
			JTechStorage.id("amethyst_obsidian"));

	/** Netherit: 37 / 3-6-8-3 / Härte 3 / Rückstoß 0,1 / Verzauberbarkeit 15. */
	public static final ArmorMaterial AMETHYST_OBSIDIAN_ARMOR = new ArmorMaterial(45, ArmorMaterials.makeDefense(4, 7, 9, 4, 13), 18,
			SoundEvents.ARMOR_EQUIP_NETHERITE, 4.0f, 0.15f, REPAIRS_AMETHYST_OBSIDIAN, AMETHYST_OBSIDIAN_ASSET);

	/** Netherit: 2031 Haltbarkeit / Tempo 9 / +4 Schaden / Verzauberbarkeit 15. */
	public static final ToolMaterial AMETHYST_OBSIDIAN_TOOL = new ToolMaterial(BlockTags.INCORRECT_FOR_NETHERITE_TOOL, 2700, 11.0f, 5.0f, 18,
			REPAIRS_AMETHYST_OBSIDIAN);

	// --- Blöcke ---------------------------------------------------------

	public static final ResourceKey<Block> CRYSTAL_ALTAR_KEY = ResourceKey.create(Registries.BLOCK, JTechStorage.id("crystal_altar"));
	public static final Block CRYSTAL_ALTAR = new CrystalAltarBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OBSIDIAN)
			.strength(-1.0f, 3600000.0f).noLootTable().lightLevel(state -> 11).noOcclusion().setId(CRYSTAL_ALTAR_KEY));
	public static final Item CRYSTAL_ALTAR_ITEM = new BlockItem(CRYSTAL_ALTAR, new Item.Properties().useBlockDescriptionPrefix()
			.rarity(Rarity.EPIC).setId(ResourceKey.create(Registries.ITEM, CRYSTAL_ALTAR_KEY.identifier())));
	public static final BlockEntityType<CrystalAltarBlockEntity> CRYSTAL_ALTAR_BLOCK_ENTITY =
			FabricBlockEntityTypeBuilder.create(CrystalAltarBlockEntity::new, CRYSTAL_ALTAR).build();

	// --- Gegenstände ----------------------------------------------------

	public static final Item CRYSTAL_OBSIDIAN = item("crystal_obsidian", Item::new,
			new Item.Properties().rarity(Rarity.RARE).fireResistant(), MISC);

	public static final Item AMETHYST_OBSIDIAN_UPGRADE = item("amethyst_obsidian_upgrade_smithing_template",
			p -> new SmithingTemplateItem(
					Component.translatable("item.jtechstorage.amethyst_obsidian_upgrade.applies_to").withStyle(ChatFormatting.BLUE),
					Component.translatable("item.jtechstorage.amethyst_obsidian_upgrade.ingredients").withStyle(ChatFormatting.BLUE),
					Component.translatable("item.jtechstorage.amethyst_obsidian_upgrade.base_slot"),
					Component.translatable("item.jtechstorage.amethyst_obsidian_upgrade.additions_slot"),
					List.of(slot("helmet"), slot("sword"), slot("chestplate"), slot("pickaxe"), slot("leggings"), slot("axe"),
							slot("boots"), slot("hoe"), slot("shovel")),
					List.of(slot("ingot")), p),
			new Item.Properties().rarity(Rarity.EPIC).fireResistant(), MISC);

	public static final Item LAIR_COMPASS = item("lair_compass", LairCompassItem::new,
			new Item.Properties().stacksTo(1).rarity(Rarity.RARE), MISC);

	public static final Item CRYSTAL_COCOON = item("crystal_cocoon", Item::new,
			new Item.Properties().stacksTo(16).rarity(Rarity.EPIC), MISC);

	// --- Ausrüstung -----------------------------------------------------

	public static final Item AMETHYST_OBSIDIAN_HELMET = armor("amethyst_obsidian_helmet", ArmorType.HELMET);
	public static final Item AMETHYST_OBSIDIAN_CHESTPLATE = armor("amethyst_obsidian_chestplate", ArmorType.CHESTPLATE);
	public static final Item AMETHYST_OBSIDIAN_LEGGINGS = armor("amethyst_obsidian_leggings", ArmorType.LEGGINGS);
	public static final Item AMETHYST_OBSIDIAN_BOOTS = armor("amethyst_obsidian_boots", ArmorType.BOOTS);
	public static final Item AMETHYST_OBSIDIAN_SWORD = item("amethyst_obsidian_sword", Item::new,
			gear().sword(AMETHYST_OBSIDIAN_TOOL, 3.0f, -2.4f), GEAR);
	public static final Item AMETHYST_OBSIDIAN_PICKAXE = item("amethyst_obsidian_pickaxe", Item::new,
			gear().pickaxe(AMETHYST_OBSIDIAN_TOOL, 1.0f, -2.8f), GEAR);
	public static final Item AMETHYST_OBSIDIAN_AXE = item("amethyst_obsidian_axe", Item::new,
			gear().axe(AMETHYST_OBSIDIAN_TOOL, 5.0f, -3.0f), GEAR);
	public static final Item AMETHYST_OBSIDIAN_SHOVEL = item("amethyst_obsidian_shovel", Item::new,
			gear().shovel(AMETHYST_OBSIDIAN_TOOL, 1.5f, -3.0f), GEAR);
	public static final Item AMETHYST_OBSIDIAN_HOE = item("amethyst_obsidian_hoe", Item::new,
			gear().hoe(AMETHYST_OBSIDIAN_TOOL, -5.0f, 0.0f), GEAR);

	// --- Wesen ----------------------------------------------------------

	public static final ResourceKey<EntityType<?>> AMETHYST_SPIDER_KEY = ResourceKey.create(Registries.ENTITY_TYPE,
			JTechStorage.id("amethyst_spider"));
	public static final EntityType<AmethystSpider> AMETHYST_SPIDER = EntityType.Builder.of(AmethystSpider::new, MobCategory.MONSTER)
			.sized(4.2f, 2.4f)
			.fireImmune()
			.clientTrackingRange(12)
			.build(AMETHYST_SPIDER_KEY);

	public static final ResourceKey<EntityType<?>> WEB_SHOT_KEY = ResourceKey.create(Registries.ENTITY_TYPE, JTechStorage.id("web_shot"));
	public static final EntityType<WebShot> WEB_SHOT = EntityType.Builder.<WebShot>of(WebShot::new, MobCategory.MISC)
			.sized(0.4f, 0.4f)
			.clientTrackingRange(6)
			.updateInterval(5)
			.build(WEB_SHOT_KEY);

	private EndContent() {
	}

	private static Identifier slot(String name) {
		return Identifier.withDefaultNamespace("container/slot/" + name);
	}

	private static Item.Properties gear() {
		return new Item.Properties().rarity(Rarity.EPIC).fireResistant();
	}

	private static Item armor(String name, ArmorType type) {
		return item(name, Item::new, gear().humanoidArmor(AMETHYST_OBSIDIAN_ARMOR, type)
				.component(DataComponents.LORE, new ItemLore(List.of(
						lore("item.jtechstorage.amethyst_obsidian.set", ChatFormatting.GOLD),
						lore("item.jtechstorage.amethyst_obsidian.set.stats", ChatFormatting.GRAY),
						lore("item.jtechstorage.amethyst_obsidian.set.thorns", ChatFormatting.LIGHT_PURPLE)))), GEAR);
	}

	private static Component lore(String key, ChatFormatting color) {
		return Component.translatable(key).withStyle(color).withStyle(style -> style.withItalic(false));
	}

	private static Item item(String name, Function<Item.Properties, Item> factory, Item.Properties properties, List<Item> group) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, JTechStorage.id(name));
		Item item = Registry.register(BuiltInRegistries.ITEM, key, factory.apply(properties.setId(key)));
		group.add(item);
		return item;
	}

	public static void init() {
		Registry.register(BuiltInRegistries.BLOCK, CRYSTAL_ALTAR_KEY, CRYSTAL_ALTAR);
		Registry.register(BuiltInRegistries.ITEM, CRYSTAL_ALTAR_KEY.identifier(), CRYSTAL_ALTAR_ITEM);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, JTechStorage.id("crystal_altar"), CRYSTAL_ALTAR_BLOCK_ENTITY);
		Registry.register(BuiltInRegistries.ENTITY_TYPE, AMETHYST_SPIDER_KEY, AMETHYST_SPIDER);
		FabricDefaultAttributeRegistry.register(AMETHYST_SPIDER, AmethystSpider.createAttributes());
		Registry.register(BuiltInRegistries.ENTITY_TYPE, WEB_SHOT_KEY, WEB_SHOT);
		ArmorSetBonus.init();

		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.COMBAT).register(output -> GEAR.forEach(output::accept));
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS).register(output -> {
			MISC.forEach(output::accept);
			output.accept(CRYSTAL_ALTAR_ITEM);
		});
	}
}
