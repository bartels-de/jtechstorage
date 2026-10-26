package dev.bartels.jtechstorage.adventure;

import dev.bartels.jtechstorage.ModContent;
import dev.bartels.jtechstorage.depths.DepthsAnimals;
import dev.bartels.jtechstorage.depths.DepthsContent;
import dev.bartels.jtechstorage.depths.memory.MemoryContent;
import dev.bartels.jtechstorage.depths.structure.CaveStructure;
import dev.bartels.jtechstorage.depths.structure.WorkshopStructure;
import dev.bartels.jtechstorage.depths.temple.TempleContent;
import dev.bartels.jtechstorage.enchant.ModEnchantments;
import dev.bartels.jtechstorage.end.EndContent;
import dev.bartels.jtechstorage.forge.ForgeContent;
import dev.bartels.jtechstorage.net.DoubleJumpPayload;
import dev.bartels.jtechstorage.storm.StormContent;
import java.util.List;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

/** Entry point of the Adventure sub-mod (v0.43). */
public class AdventureMod implements ModInitializer {
	@Override
	public void onInitialize() {
		AdventureContent.init();
		ModContent.addCreative(AdventureContent::creativeItems);
		ModContent.addCreative(EndContent::creativeItems);
		ModContent.addCreative(StormContent::creativeItems);
		ModContent.addCreative(DepthsContent::creativeItems);
		ModContent.addCreative(DepthsAnimals::creativeItems);
		ModContent.addCreative(MemoryContent::creativeItems);
		ModContent.addCreative(TempleContent::creativeItems);
		ModContent.addCreative(() -> List.of(ForgeContent.FORGE_ITEM));
		EndContent.init();
		StormContent.init();
		DepthsContent.init();
		DepthsAnimals.init();
		MemoryContent.init();
		TempleContent.init();
		ForgeContent.init();
		ModEnchantments.init();
		WorkshopStructure.init();
		CaveStructure.init();
		PayloadTypeRegistry.serverboundPlay().register(DoubleJumpPayload.TYPE, DoubleJumpPayload.STREAM_CODEC);
		ServerPlayNetworking.registerGlobalReceiver(DoubleJumpPayload.TYPE,
				(payload, context) -> ModEnchantments.handleDoubleJump(context.player()));
	}
}
