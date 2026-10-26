package dev.bartels.jtechstorage.api;

import java.util.function.BiPredicate;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import net.fabricmc.loader.api.FabricLoader;

import dev.bartels.jtechstorage.JTechStorage;

/**
 * Connection points between the sub-mods (v0.43). The core knows neither Helpers nor Adventure; they register their
 * behavior here at startup. If a sub-mod is missing, the harmless defaults remain.
 */
public final class JtsHooks {
	public static final String HELPERS = "jtechstorage_helpers";
	public static final String ADVENTURE = "jtechstorage_adventure";

	/** Resonance storm disturbs at this location (Adventure). */
	public static BiPredicate<@Nullable Level, BlockPos> stormDisturbed = (level, pos) -> false;
	/** Is a resonance storm currently raging at this location? (Adventure) */
	public static BiPredicate<Level, BlockPos> stormActive = (level, pos) -> false;

	/** Recall with the storage key (Adventure, "Recall" enchantment). */
	public interface Recall {
		/** @return true if executed or rejected with a message */
		boolean tryRecall(ServerPlayer player, ItemStack key, GlobalPos terminal);
	}

	public static Recall recall = (player, key, terminal) -> false;

	private JtsHooks() {
	}

	public static boolean helpers() {
		return FabricLoader.getInstance().isModLoaded(HELPERS);
	}

	public static boolean adventure() {
		return FabricLoader.getInstance().isModLoaded(ADVENTURE);
	}

	/** Item of a (possibly missing) sub-mod by ID; if missing, {@link Items#AIR}. */
	public static Item item(String path) {
		return BuiltInRegistries.ITEM.getOptional(JTechStorage.id(path)).orElse(Items.AIR);
	}

	public static @Nullable EntityType<?> entity(String path) {
		return BuiltInRegistries.ENTITY_TYPE.getOptional(JTechStorage.id(path)).orElse(null);
	}

	/** Level of a (possibly missing) enchantment on the item. */
	public static int enchantment(Level level, String path, ItemStack stack) {
		if (stack.isEmpty()) {
			return 0;
		}

		var key = ResourceKey.create(Registries.ENCHANTMENT, JTechStorage.id(path));
		return level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(key)
				.map(h -> EnchantmentHelper.getItemEnchantmentLevel(h, stack)).orElse(0);
	}
}
