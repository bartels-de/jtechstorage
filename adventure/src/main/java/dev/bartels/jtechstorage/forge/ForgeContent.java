package dev.bartels.jtechstorage.forge;

import java.util.Map;

import com.mojang.serialization.Codec;

import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import dev.bartels.jtechstorage.JTechStorage;
import java.util.HashMap;

/** Crystal Forge (v0.40): block, menu, upgrades component and self-repair. */
public final class ForgeContent {
	public static final TagKey<Item> FORGEABLE = TagKey.create(Registries.ITEM, JTechStorage.id("forgeable"));
	public static final DataComponentType<Map<String, Integer>> UPGRADES = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,
			JTechStorage.id("forge_upgrades"), DataComponentType.<Map<String, Integer>>builder()
					.persistent(Codec.unboundedMap(Codec.STRING, Codec.INT))
					.networkSynchronized(ByteBufCodecs.map(HashMap::new, ByteBufCodecs.STRING_UTF8, ByteBufCodecs.VAR_INT)
							.map(m -> (Map<String, Integer>) m, m -> new HashMap<>(m)))
					.build());

	public static final Block FORGE;
	public static final Item FORGE_ITEM;
	public static final MenuType<ForgeMenu> MENU = Registry.register(BuiltInRegistries.MENU, JTechStorage.id("crystal_forge"),
			new MenuType<>(ForgeMenu::new, FeatureFlags.VANILLA_SET));

	static {
		ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, JTechStorage.id("crystal_forge"));
		FORGE = Registry.register(BuiltInRegistries.BLOCK, key, new ForgeBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.SMITHING_TABLE)
				.strength(3.5f).lightLevel(state -> 6).requiresCorrectToolForDrops().setId(key)));
		FORGE_ITEM = Registry.register(BuiltInRegistries.ITEM, JTechStorage.id("crystal_forge"), new BlockItem(FORGE, new Item.Properties()
				.useBlockDescriptionPrefix().rarity(Rarity.UNCOMMON).setId(ResourceKey.create(Registries.ITEM, key.identifier()))));
	}

	private ForgeContent() {
	}

	public static void init() {
		// Self-repair: every 100 ticks 1 durability per upgraded item in the inventory (= 1 per 5 seconds)
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % 100 != 0) {
				return;
			}

			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				Inventory inv = player.getInventory();

				for (int i = 0; i < inv.getContainerSize(); i++) {
					ItemStack stack = inv.getItem(i);

					if (stack.isDamaged() && ForgeUpgrade.level(stack, ForgeUpgrade.MENDING) > 0) {
						stack.setDamageValue(stack.getDamageValue() - 1);
					}
				}
			}
		});
	}
}
