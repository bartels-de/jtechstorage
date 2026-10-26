package dev.bartels.jtechstorage;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.server.level.ServerPlayer;

/** Grants advancements tied to actions (criterion "granted", trigger minecraft:impossible). */
public final class ModAdvancements {
	private ModAdvancements() {
	}

	public static void grant(ServerPlayer player, String name) {
		AdvancementHolder advancement = player.level().getServer().getAdvancements().get(JTechStorage.id(name));

		if (advancement != null) {
			player.getAdvancements().award(advancement, "granted");
		}
	}
}
