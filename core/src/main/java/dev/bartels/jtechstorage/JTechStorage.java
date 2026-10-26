package dev.bartels.jtechstorage;

import dev.bartels.jtechstorage.chest.ChestContent;
import dev.bartels.jtechstorage.cosmetic.Cosmetics;
import dev.bartels.jtechstorage.display.DisplayContent;
import dev.bartels.jtechstorage.farm.FarmContent;
import dev.bartels.jtechstorage.logistics.LogisticsContent;
import dev.bartels.jtechstorage.net.ConfigSyncPayload;
import dev.bartels.jtechstorage.net.CosmeticEffectPayload;
import dev.bartels.jtechstorage.net.CosmeticEquipPayload;
import dev.bartels.jtechstorage.net.CosmeticRequestPayload;
import dev.bartels.jtechstorage.net.CosmeticUnlocksPayload;
import dev.bartels.jtechstorage.net.TerminalStatsPayload;
import dev.bartels.jtechstorage.net.TerminalStatsRequestPayload;
import dev.bartels.jtechstorage.net.TerminalViewPayload;
import dev.bartels.jtechstorage.tools.ToolsContent;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.resources.Identifier;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import dev.bartels.jtechstorage.menu.StorageTerminalMenu;
import dev.bartels.jtechstorage.net.CraftingTransferPayload;
import dev.bartels.jtechstorage.net.TerminalActionPayload;
import dev.bartels.jtechstorage.net.TerminalContentsPayload;
import dev.bartels.jtechstorage.menu.TeamMenu;
import dev.bartels.jtechstorage.net.TeamActionPayload;
import dev.bartels.jtechstorage.net.TeamStatePayload;

public class JTechStorage implements ModInitializer {
	public static final String MOD_ID = "jtechstorage";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	@Override
	public void onInitialize() {
		ModConfig.load();
		ModSounds.init();
		ModContent.init();
		FarmContent.init();
		ChestContent.init();
		ToolsContent.init();
		LogisticsContent.init();
		Cosmetics.init();
		JbartelsMilestones.init();
		DisplayContent.init();

		PayloadTypeRegistry.clientboundPlay().registerLarge(TerminalContentsPayload.TYPE,
				TerminalContentsPayload.STREAM_CODEC, TerminalContentsPayload.MAX_SIZE);
		PayloadTypeRegistry.serverboundPlay().register(TerminalActionPayload.TYPE, TerminalActionPayload.STREAM_CODEC);
		PayloadTypeRegistry.serverboundPlay().register(CraftingTransferPayload.TYPE, CraftingTransferPayload.STREAM_CODEC);
		PayloadTypeRegistry.clientboundPlay().register(TeamStatePayload.TYPE, TeamStatePayload.STREAM_CODEC);
		PayloadTypeRegistry.serverboundPlay().register(TeamActionPayload.TYPE, TeamActionPayload.STREAM_CODEC);
		PayloadTypeRegistry.clientboundPlay().register(ConfigSyncPayload.TYPE, ConfigSyncPayload.STREAM_CODEC);
		// Player cosmetics (v0.34)
		PayloadTypeRegistry.serverboundPlay().register(CosmeticEquipPayload.TYPE, CosmeticEquipPayload.STREAM_CODEC);
		PayloadTypeRegistry.serverboundPlay().register(CosmeticRequestPayload.TYPE, CosmeticRequestPayload.STREAM_CODEC);
		PayloadTypeRegistry.clientboundPlay().register(CosmeticUnlocksPayload.TYPE, CosmeticUnlocksPayload.STREAM_CODEC);
		PayloadTypeRegistry.clientboundPlay().register(CosmeticEffectPayload.TYPE, CosmeticEffectPayload.STREAM_CODEC);
		ServerPlayNetworking.registerGlobalReceiver(CosmeticEquipPayload.TYPE, (payload, context) -> {
			Cosmetics.equip(context.player(), payload.slot(), payload.cosmetic());
			Cosmetics.sendUnlocks(context.player());
		});
		ServerPlayNetworking.registerGlobalReceiver(CosmeticRequestPayload.TYPE, (payload, context) -> {
			if (payload.action() == CosmeticRequestPayload.EMOTE) {
				Cosmetics.emote(context.player());
			} else {
				Cosmetics.sendUnlocks(context.player());
			}
		});
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
				sender.sendPacket(new ConfigSyncPayload(ModConfig.toNetworkJson())));
		PayloadTypeRegistry.serverboundPlay().register(TerminalViewPayload.TYPE, TerminalViewPayload.STREAM_CODEC);
		ServerPlayNetworking.registerGlobalReceiver(TerminalViewPayload.TYPE, (payload, context) -> {
			if (context.player().containerMenu instanceof StorageTerminalMenu menu && menu.containerId == payload.containerId()) {
				menu.setView(payload.prototypes());
			}
		});
		ServerPlayNetworking.registerGlobalReceiver(TerminalActionPayload.TYPE, (payload, context) -> {
			if (context.player().containerMenu instanceof StorageTerminalMenu menu
					&& menu.containerId == payload.containerId()
					&& menu.stillValid(context.player())) {
				menu.handleAction(context.player(), payload.action(), payload.prototype());
			}
		});

		PayloadTypeRegistry.serverboundPlay().register(TerminalStatsRequestPayload.TYPE, TerminalStatsRequestPayload.STREAM_CODEC);
		PayloadTypeRegistry.clientboundPlay().register(TerminalStatsPayload.TYPE, TerminalStatsPayload.STREAM_CODEC);
		ServerPlayNetworking.registerGlobalReceiver(TerminalStatsRequestPayload.TYPE, (payload, context) -> {
			if (context.player().containerMenu instanceof StorageTerminalMenu menu
					&& menu.containerId == payload.containerId()
					&& menu.stillValid(context.player())) {
				menu.sendStats(context.player());
			}
		});

		ServerPlayNetworking.registerGlobalReceiver(CraftingTransferPayload.TYPE, (payload, context) -> {
			if (context.player().containerMenu instanceof StorageTerminalMenu menu
					&& menu.containerId == payload.containerId()
					&& menu.stillValid(context.player())) {
				menu.handleCraftingTransfer(context.player(), payload.slots());
			}
		});

		ServerPlayNetworking.registerGlobalReceiver(TeamActionPayload.TYPE, (payload, context) -> {
			if (context.player().containerMenu instanceof TeamMenu menu
					&& menu.containerId == payload.containerId()
					&& menu.stillValid(context.player())) {
				menu.handleAction(context.player(), payload.action(), payload.text(), payload.value());
			}
		});

		ModGuide.init();

		LOGGER.info("JTechStorage geladen");
	}
}
