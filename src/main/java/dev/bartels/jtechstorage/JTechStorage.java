package dev.bartels.jtechstorage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.resources.Identifier;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import dev.bartels.jtechstorage.menu.StorageTerminalMenu;
import dev.bartels.jtechstorage.net.CraftingTransferPayload;
import dev.bartels.jtechstorage.menu.ChelOrderMenu;
import dev.bartels.jtechstorage.net.ChelOrderActionPayload;
import dev.bartels.jtechstorage.net.ChelOrdersPayload;
import dev.bartels.jtechstorage.net.TerminalActionPayload;
import dev.bartels.jtechstorage.net.TerminalContentsPayload;
import dev.bartels.jtechstorage.menu.TeamMenu;
import dev.bartels.jtechstorage.mail.PhoenixPost;
import dev.bartels.jtechstorage.menu.NestMenu;
import dev.bartels.jtechstorage.net.NestActionPayload;
import dev.bartels.jtechstorage.net.NestStatePayload;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
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
		ModContent.init();
		dev.bartels.jtechstorage.end.EndContent.init();
		dev.bartels.jtechstorage.enchant.ModEnchantments.init();

		PayloadTypeRegistry.clientboundPlay().registerLarge(TerminalContentsPayload.TYPE,
				TerminalContentsPayload.STREAM_CODEC, TerminalContentsPayload.MAX_SIZE);
		PayloadTypeRegistry.serverboundPlay().register(TerminalActionPayload.TYPE, TerminalActionPayload.STREAM_CODEC);
		PayloadTypeRegistry.serverboundPlay().register(CraftingTransferPayload.TYPE, CraftingTransferPayload.STREAM_CODEC);
		PayloadTypeRegistry.clientboundPlay().register(ChelOrdersPayload.TYPE, ChelOrdersPayload.STREAM_CODEC);
		PayloadTypeRegistry.serverboundPlay().register(ChelOrderActionPayload.TYPE, ChelOrderActionPayload.STREAM_CODEC);
		PayloadTypeRegistry.clientboundPlay().register(TeamStatePayload.TYPE, TeamStatePayload.STREAM_CODEC);
		PayloadTypeRegistry.serverboundPlay().register(TeamActionPayload.TYPE, TeamActionPayload.STREAM_CODEC);
		PayloadTypeRegistry.clientboundPlay().register(NestStatePayload.TYPE, NestStatePayload.STREAM_CODEC);
		PayloadTypeRegistry.serverboundPlay().register(NestActionPayload.TYPE, NestActionPayload.STREAM_CODEC);
		PayloadTypeRegistry.serverboundPlay().register(dev.bartels.jtechstorage.net.DoubleJumpPayload.TYPE, dev.bartels.jtechstorage.net.DoubleJumpPayload.STREAM_CODEC);
		PayloadTypeRegistry.serverboundPlay().register(dev.bartels.jtechstorage.net.PhoenixDepartPayload.TYPE, dev.bartels.jtechstorage.net.PhoenixDepartPayload.STREAM_CODEC);

		ServerPlayNetworking.registerGlobalReceiver(dev.bartels.jtechstorage.net.PhoenixDepartPayload.TYPE, (payload, context) -> {
			net.minecraft.server.level.ServerPlayer player = context.player();

			if (player.getVehicle() instanceof dev.bartels.jtechstorage.entity.AmethystPhoenix phoenix && phoenix.getId() == payload.entityId()
					&& phoenix.getFirstPassenger() == player && phoenix.departNow()) {
				player.sendOverlayMessage(net.minecraft.network.chat.Component.translatable("jtechstorage.amethyst_phoenix.early"));
			}
		});
		ServerPlayNetworking.registerGlobalReceiver(dev.bartels.jtechstorage.net.DoubleJumpPayload.TYPE,
				(payload, context) -> dev.bartels.jtechstorage.enchant.ModEnchantments.handleDoubleJump(context.player()));
		ServerPlayNetworking.registerGlobalReceiver(TerminalActionPayload.TYPE, (payload, context) -> {
			if (context.player().containerMenu instanceof StorageTerminalMenu menu
					&& menu.containerId == payload.containerId()
					&& menu.stillValid(context.player())) {
				menu.handleAction(context.player(), payload.action(), payload.prototype());
			}
		});

		ServerPlayNetworking.registerGlobalReceiver(CraftingTransferPayload.TYPE, (payload, context) -> {
			if (context.player().containerMenu instanceof StorageTerminalMenu menu
					&& menu.containerId == payload.containerId()
					&& menu.stillValid(context.player())) {
				menu.handleCraftingTransfer(context.player(), payload.slots());
			}
		});

		ServerPlayNetworking.registerGlobalReceiver(ChelOrderActionPayload.TYPE, (payload, context) -> {
			if (context.player().containerMenu instanceof ChelOrderMenu menu
					&& menu.containerId == payload.containerId()
					&& menu.stillValid(context.player())) {
				menu.handleAction(context.player(), payload.slot(), payload.action(), payload.amount());
			}
		});

		ServerPlayNetworking.registerGlobalReceiver(TeamActionPayload.TYPE, (payload, context) -> {
			if (context.player().containerMenu instanceof TeamMenu menu
					&& menu.containerId == payload.containerId()
					&& menu.stillValid(context.player())) {
				menu.handleAction(context.player(), payload.action(), payload.text(), payload.value());
			}
		});

		ServerPlayNetworking.registerGlobalReceiver(NestActionPayload.TYPE, (payload, context) -> {
			if (context.player().containerMenu instanceof NestMenu menu
					&& menu.containerId == payload.containerId()
					&& menu.stillValid(context.player())) {
				menu.handleAction(context.player(), payload.action(), payload.text(), payload.value());
			}
		});

		// Phönix-Post: einmal pro Sekunde fällige Pakete zustellen
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % 20 == 0) {
				PhoenixPost.get(server).tick(server);
			}
		});

		ModGuide.init();

		LOGGER.info("JTechStorage geladen");
	}
}
