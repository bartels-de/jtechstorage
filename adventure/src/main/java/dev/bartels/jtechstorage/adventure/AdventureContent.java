package dev.bartels.jtechstorage.adventure;

import dev.bartels.jtechstorage.JTechStorage;
import dev.bartels.jtechstorage.entity.AmethystFox;
import dev.bartels.jtechstorage.entity.AmethystRobot;
import dev.bartels.jtechstorage.item.AmethystDraughtItem;
import dev.bartels.jtechstorage.item.HelperSpawnItem;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.level.block.Block;

/** Adventure sub-mod (v0.43): Amethyst Robot, Amethyst Draught, Amethyst Fox – previously in ModContent. */
public final class AdventureContent {
	private static final List<Item> CREATIVE = new ArrayList<>();

	/** Resonance Igniter (formerly Amethyst Draught): wakes the Amethyst Robot. */
	public static final Item AMETHYST_DRAUGHT = item("amethyst_draught", AmethystDraughtItem::new,
			new Item.Properties().stacksTo(1).rarity(Rarity.RARE)
					.component(DataComponents.CONSUMABLE, Consumable.builder().consumeSeconds(2.0f)
							.animation(ItemUseAnimation.TOOT_HORN)
							.sound(Holder.direct(SoundEvents.BEACON_POWER_SELECT)).hasConsumeParticles(false).build()));

	public static final ResourceKey<EntityType<?>> AMETHYST_FOX_KEY = ResourceKey.create(Registries.ENTITY_TYPE, JTechStorage.id("amethyst_fox"));

	public static final EntityType<AmethystFox> AMETHYST_FOX = EntityType.Builder
			.of(AmethystFox::new, MobCategory.MISC)
			.sized(0.7f, 0.75f)
			.clientTrackingRange(8)
			.build(AMETHYST_FOX_KEY);

	public static final Item AMETHYST_FOX_ITEM = item("amethyst_fox", p -> new HelperSpawnItem(() -> AMETHYST_FOX, p),
			new Item.Properties().stacksTo(1).rarity(Rarity.EPIC));

	public static final ResourceKey<EntityType<?>> AMETHYST_ROBOT_KEY = ResourceKey.create(Registries.ENTITY_TYPE, JTechStorage.id("amethyst_robot"));

	public static final EntityType<AmethystRobot> AMETHYST_ROBOT = EntityType.Builder.of(AmethystRobot::new, MobCategory.MONSTER)
			.sized(2.8f, 5.0f)
			.clientTrackingRange(12)
			.build(AMETHYST_ROBOT_KEY);

	private AdventureContent() {
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
		Registry.register(BuiltInRegistries.ENTITY_TYPE, AMETHYST_FOX_KEY, AMETHYST_FOX);
		FabricDefaultAttributeRegistry.register(AMETHYST_FOX, AmethystFox.createAttributes());
		Registry.register(BuiltInRegistries.ENTITY_TYPE, AMETHYST_ROBOT_KEY, AMETHYST_ROBOT);
		FabricDefaultAttributeRegistry.register(AMETHYST_ROBOT, AmethystRobot.createAttributes());
	}
}
