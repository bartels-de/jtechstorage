package dev.bartels.jtechstorage.depths.memory;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;

import dev.bartels.jtechstorage.JTechStorage;

/**
 * Erinnerungskristalle (v0.36): zwölf Sammelobjekte der Kristalltiefe – einer je Biom (als leuchtender Kristall auf
 * einem Sockel), einer in Jbartels' Werkstatt, einer in der Lagermeister-Siedlung und einer in der Kristallmine. In der
 * Erinnerungsvitrine werden sie ausgestellt; die volle Vitrine schaltet „Alle Erinnerungen“ frei.
 */
public final class MemoryContent {
	/** Reihenfolge = Bit in der Vitrine. Die ersten {@link #WORLD_KINDS} stehen als Block in der Welt. */
	public static final String[] KINDS = {"geode_halls", "crystal_lakes", "vein_chasm", "resonance_grotto", "prism_forest", "rose_gardens",
			"azure_frost", "citrine_dunes", "hollow_expanse", "workshop", "settlement", "mine"};
	public static final int WORLD_KINDS = 9;
	public static final int ALL = (1 << KINDS.length) - 1;

	private static final List<Item> CREATIVE = new ArrayList<>();
	public static final Item[] CRYSTALS = new Item[KINDS.length];

	public static final Block MEMORY_CRYSTAL;
	public static final Block MEMORY_CASE;
	public static final BlockEntityType<MemoryCaseBlockEntity> MEMORY_CASE_BLOCK_ENTITY;

	static {
		for (int i = 0; i < KINDS.length; i++) {
			int kind = i;
			ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, JTechStorage.id("memory_crystal_" + KINDS[i]));
			CRYSTALS[i] = Registry.register(BuiltInRegistries.ITEM, key,
					new MemoryCrystalItem(kind, new Item.Properties().stacksTo(16).rarity(Rarity.RARE).setId(key)));
			CREATIVE.add(CRYSTALS[i]);
		}

		ResourceKey<Block> crystalKey = ResourceKey.create(Registries.BLOCK, JTechStorage.id("memory_crystal"));
		MEMORY_CRYSTAL = Registry.register(BuiltInRegistries.BLOCK, crystalKey, new MemoryCrystalBlock(BlockBehaviour.Properties.of()
				.mapColor(MapColor.COLOR_PURPLE).strength(1.5f).sound(SoundType.AMETHYST).noOcclusion().lightLevel(state -> 7)
				.pushReaction(PushReaction.POPPED).setId(crystalKey)));

		ResourceKey<Block> caseKey = ResourceKey.create(Registries.BLOCK, JTechStorage.id("memory_case"));
		MEMORY_CASE = Registry.register(BuiltInRegistries.BLOCK, caseKey, new MemoryCaseBlock(BlockBehaviour.Properties.of()
				.mapColor(MapColor.COLOR_BROWN).strength(2.0f, 6.0f).sound(SoundType.WOOD).noOcclusion()
				.lightLevel(state -> 3).setId(caseKey)));
		CREATIVE.add(Registry.register(BuiltInRegistries.ITEM, JTechStorage.id("memory_case"), new BlockItem(MEMORY_CASE, new Item.Properties()
				.useBlockDescriptionPrefix().rarity(Rarity.UNCOMMON).setId(ResourceKey.create(Registries.ITEM, caseKey.identifier())))));
		MEMORY_CASE_BLOCK_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, JTechStorage.id("memory_case"),
				FabricBlockEntityTypeBuilder.create(MemoryCaseBlockEntity::new, MEMORY_CASE).build());
	}

	private MemoryContent() {
	}

	public static List<Item> creativeItems() {
		return CREATIVE;
	}

	/** Art (Index in {@link #KINDS}) des Gegenstands oder -1. */
	public static int kindOf(Item item) {
		return item instanceof MemoryCrystalItem crystal ? crystal.kind : -1;
	}

	public static void init() {
		// lädt die statischen Registrierungen
	}
}
