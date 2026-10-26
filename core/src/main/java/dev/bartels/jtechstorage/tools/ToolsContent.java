package dev.bartels.jtechstorage.tools;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import com.mojang.serialization.Codec;

import net.minecraft.core.GlobalPos;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import dev.bartels.jtechstorage.JTechStorage;

/** Building tools (v0.39): Building Wand, Exchange Wand and Measuring Tape. */
public final class ToolsContent {
	private static final List<Item> CREATIVE = new ArrayList<>();

	/** Size of the wand: 1 = 3×3, 2 = 5×5, 3 = 7×7. */
	public static final DataComponentType<Integer> WAND_SIZE = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, JTechStorage.id("wand_size"),
			DataComponentType.<Integer>builder().persistent(Codec.intRange(1, 3)).networkSynchronized(ByteBufCodecs.VAR_INT).build());
	/** Selected block of the Exchange Wand. */
	public static final DataComponentType<BlockState> WAND_BLOCK = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, JTechStorage.id("wand_block"),
			DataComponentType.<BlockState>builder().persistent(BlockState.CODEC).networkSynchronized(ByteBufCodecs.idMapper(Block.BLOCK_STATE_REGISTRY)).build());
	/** First measuring point of the Measuring Tape. */
	public static final DataComponentType<GlobalPos> MEASURE_FROM = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, JTechStorage.id("measure_from"),
			DataComponentType.<GlobalPos>builder().persistent(GlobalPos.CODEC).networkSynchronized(GlobalPos.STREAM_CODEC).build());

	public static final Item BUILDING_WAND = item("building_wand", BuildingWandItem::new, new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));
	public static final Item EXCHANGE_WAND = item("exchange_wand", ExchangeWandItem::new, new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));
	public static final Item MEASURING_TAPE = item("measuring_tape", MeasuringTapeItem::new, new Item.Properties().stacksTo(1));

	private ToolsContent() {
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
		// loads the static registrations
	}
}
