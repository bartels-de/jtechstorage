package dev.bartels.jtechstorage.forge;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.equipment.Equippable;

import dev.bartels.jtechstorage.JTechStorage;
import dev.bartels.jtechstorage.depths.DepthsContent;

/**
 * Upgrades of the Crystal Forge (v0.40) for Netherite and Amethyst Obsidian gear. The levels are stored in the
 * component {@link ForgeContent#UPGRADES}; durability and attributes are recomputed from it every time.
 */
public enum ForgeUpgrade {
	/** +25 % durability per level. */
	HARDENED(3, null),
	/** Tools/weapons: +1 attack damage and +3 mining speed per level. */
	SHARPENED(3, false),
	/** Armor: +1 armor and +0.5 toughness per level. */
	REINFORCED(3, true),
	/** Slowly repairs itself (1 durability every 5 seconds). */
	MENDING(1, null);

	public final int maxLevel;
	/** null = everything, true = armor only, false = tools/weapons only. */
	private final Boolean armorOnly;

	ForgeUpgrade(int maxLevel, Boolean armorOnly) {
		this.maxLevel = maxLevel;
		this.armorOnly = armorOnly;
	}

	public String id() {
		return name().toLowerCase(Locale.ROOT);
	}

	public static boolean isArmor(ItemStack stack) {
		Equippable eq = stack.get(DataComponents.EQUIPPABLE);
		return eq != null && eq.slot().isArmor();
	}

	public boolean appliesTo(ItemStack stack) {
		return armorOnly == null || armorOnly == isArmor(stack);
	}

	public static int level(ItemStack stack, ForgeUpgrade upgrade) {
		Map<String, Integer> map = stack.get(ForgeContent.UPGRADES);
		return map == null ? 0 : map.getOrDefault(upgrade.id(), 0);
	}

	/** Material for the next level (level {@code next}). */
	public ItemStack cost(int next) {
		return switch (this) {
			case HARDENED -> new ItemStack(DepthsContent.PURE_AMETHYST, 2 * next);
			case SHARPENED -> new ItemStack(DepthsContent.GLOW_CRYSTAL, 4 * next);
			case REINFORCED -> new ItemStack(Items.NETHERITE_SCRAP, next);
			case MENDING -> new ItemStack(Items.NETHERITE_INGOT, 1);
		};
	}

	/** Sets the level and recomputes durability and attributes. */
	public static void set(ItemStack stack, ForgeUpgrade upgrade, int level) {
		Map<String, Integer> map = new HashMap<>(stack.getOrDefault(ForgeContent.UPGRADES, Map.of()));
		map.put(upgrade.id(), level);
		stack.set(ForgeContent.UPGRADES, Map.copyOf(map));
		refresh(stack);
	}

	public static void refresh(ItemStack stack) {
		Item item = stack.getItem();
		Integer baseMax = item.components().get(DataComponents.MAX_DAMAGE);

		if (baseMax != null) {
			stack.set(DataComponents.MAX_DAMAGE, Math.round(baseMax * (1.0f + 0.25f * level(stack, HARDENED))));
		}

		ItemAttributeModifiers mods = item.components().getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
		int sharp = level(stack, SHARPENED);
		int armor = level(stack, REINFORCED);

		if (isArmor(stack)) {
			EquipmentSlotGroup group = EquipmentSlotGroup.bySlot(stack.get(DataComponents.EQUIPPABLE).slot());

			if (armor > 0) {
				mods = mods.withModifierAdded(Attributes.ARMOR, new AttributeModifier(id("reinforced_armor"), armor, AttributeModifier.Operation.ADD_VALUE), group)
						.withModifierAdded(Attributes.ARMOR_TOUGHNESS, new AttributeModifier(id("reinforced_toughness"), armor * 0.5,
								AttributeModifier.Operation.ADD_VALUE), group);
			}
		} else if (sharp > 0) {
			mods = mods.withModifierAdded(Attributes.ATTACK_DAMAGE, new AttributeModifier(id("sharpened_damage"), sharp, AttributeModifier.Operation.ADD_VALUE),
					EquipmentSlotGroup.MAINHAND);

			if (stack.has(DataComponents.TOOL)) {
				mods = mods.withModifierAdded(Attributes.MINING_EFFICIENCY, new AttributeModifier(id("sharpened_speed"), sharp * 3.0,
						AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND);
			}
		}

		stack.set(DataComponents.ATTRIBUTE_MODIFIERS, mods);
	}

	private static Identifier id(String path) {
		return JTechStorage.id("forge_" + path);
	}
}
