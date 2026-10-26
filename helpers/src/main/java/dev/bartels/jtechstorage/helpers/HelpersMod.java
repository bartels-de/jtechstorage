package dev.bartels.jtechstorage.helpers;

import dev.bartels.jtechstorage.block.PhoenixStopBlockEntity;
import dev.bartels.jtechstorage.companion.CompanionContent;
import dev.bartels.jtechstorage.entity.AmethystPhoenix;
import dev.bartels.jtechstorage.entity.Cheldrogan;
import dev.bartels.jtechstorage.item.FlightPlanItem;
import dev.bartels.jtechstorage.mail.PhoenixPost;
import dev.bartels.jtechstorage.menu.ChelOrderMenu;
import dev.bartels.jtechstorage.menu.NestMenu;
import dev.bartels.jtechstorage.net.ChelOrderActionPayload;
import dev.bartels.jtechstorage.net.ChelOrdersPayload;
import dev.bartels.jtechstorage.net.CompanionActionPayload;
import dev.bartels.jtechstorage.net.CompanionStatusPayload;
import dev.bartels.jtechstorage.net.DragonActionPayload;
import dev.bartels.jtechstorage.net.FlightPlanEditPayload;
import dev.bartels.jtechstorage.net.NestActionPayload;
import dev.bartels.jtechstorage.net.NestStatePayload;
import dev.bartels.jtechstorage.ModContent;
import dev.bartels.jtechstorage.net.PhoenixDepartPayload;
import dev.bartels.jtechstorage.net.PhoenixStopEditPayload;
import dev.bartels.jtechstorage.wolf.WolfContent;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;

/** Entry point of the sub-mod “Helpers &amp; Phoenix” (v0.43). */
public class HelpersMod implements ModInitializer {
	@Override
	public void onInitialize() {
		HelpersContent.init();
		ModContent.addCreative(HelpersContent::creativeItems);
		ModContent.addCreative(WolfContent::creativeItems);
		ModContent.addCreative(CompanionContent::creativeItems);
		WolfContent.init();
		CompanionContent.init();
		PayloadTypeRegistry.clientboundPlay().register(ChelOrdersPayload.TYPE, ChelOrdersPayload.STREAM_CODEC);
		PayloadTypeRegistry.serverboundPlay().register(ChelOrderActionPayload.TYPE, ChelOrderActionPayload.STREAM_CODEC);
		PayloadTypeRegistry.clientboundPlay().register(NestStatePayload.TYPE, NestStatePayload.STREAM_CODEC);
		PayloadTypeRegistry.serverboundPlay().register(NestActionPayload.TYPE, NestActionPayload.STREAM_CODEC);
		PayloadTypeRegistry.serverboundPlay().register(PhoenixDepartPayload.TYPE, PhoenixDepartPayload.STREAM_CODEC);
		PayloadTypeRegistry.serverboundPlay().register(DragonActionPayload.TYPE, DragonActionPayload.STREAM_CODEC);
		PayloadTypeRegistry.serverboundPlay().register(CompanionActionPayload.TYPE, CompanionActionPayload.STREAM_CODEC);
		PayloadTypeRegistry.clientboundPlay().register(CompanionStatusPayload.TYPE, CompanionStatusPayload.STREAM_CODEC);
		PayloadTypeRegistry.serverboundPlay().register(PhoenixStopEditPayload.TYPE, PhoenixStopEditPayload.STREAM_CODEC);
		PayloadTypeRegistry.serverboundPlay().register(FlightPlanEditPayload.TYPE, FlightPlanEditPayload.STREAM_CODEC);
		ServerPlayNetworking.registerGlobalReceiver(PhoenixStopEditPayload.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();

			if (player.level().isLoaded(payload.pos()) && payload.pos().distToCenterSqr(player.position()) <= 10.0 * 10.0
					&& player.level().getBlockEntity(payload.pos()) instanceof PhoenixStopBlockEntity stop) {
				stop.apply(payload.name(), PhoenixStopBlockEntity.Rest.byId(payload.rest()));
				player.sendOverlayMessage(Component.translatable("jtechstorage.phoenix_stop.saved", stop.getStopName()));
			}
		});
		ServerPlayNetworking.registerGlobalReceiver(FlightPlanEditPayload.TYPE, (payload, context) -> {
			ItemStack stack = context.player().getItemInHand(payload.offHand()
					? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);

			if (stack.is(HelpersContent.FLIGHT_PLAN)) {
				FlightPlanItem.reorder(stack, payload.order());
			}
		});
		ServerPlayNetworking.registerGlobalReceiver(CompanionActionPayload.TYPE,
				(payload, context) -> CompanionContent.hotkey(context.player(), payload.action(), payload.arg()));
		ServerPlayNetworking.registerGlobalReceiver(DragonActionPayload.TYPE, (payload, context) -> {
			if (context.player().getVehicle() instanceof Cheldrogan dragon) {
				dragon.riderAction(context.player(), payload.action());
			}
		});
		// On Cheldrogan: left click is the tail sweep – no mining and no hitting with the hand
		AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) ->
				player.getVehicle() instanceof Cheldrogan ? InteractionResult.FAIL : InteractionResult.PASS);
		AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) ->
				player.getVehicle() instanceof Cheldrogan ? InteractionResult.FAIL : InteractionResult.PASS);

		ServerPlayNetworking.registerGlobalReceiver(PhoenixDepartPayload.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();

			if (player.getVehicle() instanceof AmethystPhoenix phoenix && phoenix.getId() == payload.entityId()
					&& phoenix.getFirstPassenger() == player && phoenix.departNow()) {
				player.sendOverlayMessage(Component.translatable("jtechstorage.amethyst_phoenix.early"));
			}
		});

		ServerPlayNetworking.registerGlobalReceiver(ChelOrderActionPayload.TYPE, (payload, context) -> {
			if (context.player().containerMenu instanceof ChelOrderMenu menu
					&& menu.containerId == payload.containerId()
					&& menu.stillValid(context.player())) {
				menu.handleAction(context.player(), payload.slot(), payload.action(), payload.amount());
			}
		});

		ServerPlayNetworking.registerGlobalReceiver(NestActionPayload.TYPE, (payload, context) -> {
			if (context.player().containerMenu instanceof NestMenu menu
					&& menu.containerId == payload.containerId()
					&& menu.stillValid(context.player())) {
				menu.handleAction(context.player(), payload.action(), payload.text(), payload.value());
			}
		});

		// Phoenix mail: deliver due packages once per second
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % 20 == 0) {
				PhoenixPost.get(server).tick(server);
			}
		});
	}
}
