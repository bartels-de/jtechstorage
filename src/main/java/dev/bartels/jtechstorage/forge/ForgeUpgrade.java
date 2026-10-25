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
 * Verbesserungen der Kristall-Schmiede (v0.40) für Netherit- und Amethyst-Obsidian-Ausrüstung. Die Stufen stehen in der
 * Komponente {@link ForgeContent#UPGRADES}; Haltbarkeit und Attribute werden daraus jedes Mal neu berechnet.
 */
public enum ForgeUpgrade {
	/** +25 % Haltbarkeit je Stufe. */
	HARDENED(3, null),
	/** Werkzeuge/Waffen: +1 Angriffsschaden und +3 Abbautempo je Stufe. */
	SHARPENED(3, false),
	/** Rüstung: +1 Rüstung und +0,5 Härte je Stufe. */
	REINFORCED(3, true),
	/** Repariert sich langsam selbst (1 Haltbarkeit alle 5 Sekunden). */
	MENDING(1, null);

	public final int maxLevel;
	/** null = alles, true = nur Rüstung, false = nur Werkzeuge/Waffen. */
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

	/** Material für die nächste Stufe (Stufe {@code next}). */
	public ItemStack cost(int next) {
		return switch (this) {
			case HARDENED -> new ItemStack(DepthsContent.PURE_AMETHYST, 2 * next);
			case SHARPENED -> new ItemStack(DepthsContent.GLOW_CRYSTAL, 4 * next);
			case REINFORCED -> new ItemStack(Items.NETHERITE_SCRAP, next);
			case MENDING -> new ItemStack(Items.NETHERITE_INGOT, 1);
		};
	}

	/** Setzt die Stufe und berechnet Haltbarkeit und Attribute neu. */
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
