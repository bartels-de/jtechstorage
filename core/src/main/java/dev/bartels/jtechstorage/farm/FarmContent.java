package dev.bartels.jtechstorage.farm;

import java.util.ArrayList;
import java.util.List;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.entity.BlockEntityType;

import net.fabricmc.fabric.api.menu.v1.ExtendedMenuType;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;

import dev.bartels.jtechstorage.JTechStorage;
import dev.bartels.jtechstorage.menu.StorageTerminalMenu;

/**
 * Farm extras (v0.27.0): Amethyst Hopper, Crystal Pusher and Farm Monitor (with a protection zone for companions and the
 * "Farms" list in the terminal).
 */
public final class FarmContent {
	private static final List<Item> CREATIVE = new ArrayList<>();

	public static final ResourceKey<Block> AMETHYST_HOPPER_KEY = key("amethyst_hopper");
	public static final Block AMETHYST_HOPPER = new AmethystHopperBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.HOPPER).setId(AMETHYST_HOPPER_KEY));
	public static final Item AMETHYST_HOPPER_ITEM = item(AMETHYST_HOPPER, AMETHYST_HOPPER_KEY);
	public static final BlockEntityType<AmethystHopperBlockEntity> AMETHYST_HOPPER_BLOCK_ENTITY =
			FabricBlockEntityTypeBuilder.create(AmethystHopperBlockEntity::new, AMETHYST_HOPPER).build();

	public static final ResourceKey<Block> CRYSTAL_PUSHER_KEY = key("crystal_pusher");
	public static final Block CRYSTAL_PUSHER = new CrystalPusherBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.POLISHED_DEEPSLATE)
			.lightLevel(state -> 3).setId(CRYSTAL_PUSHER_KEY));
	public static final Item CRYSTAL_PUSHER_ITEM = item(CRYSTAL_PUSHER, CRYSTAL_PUSHER_KEY);
	public static final BlockEntityType<CrystalPusherBlockEntity> CRYSTAL_PUSHER_BLOCK_ENTITY =
			FabricBlockEntityTypeBuilder.create(CrystalPusherBlockEntity::new, CRYSTAL_PUSHER).build();
	public static final MenuType<CrystalPusherMenu> CRYSTAL_PUSHER_MENU = new MenuType<>(CrystalPusherMenu::new, FeatureFlags.VANILLA_SET);

	public static final ResourceKey<Block> FARM_MONITOR_KEY = key("farm_monitor");
	public static final Block FARM_MONITOR = new FarmMonitorBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.POLISHED_DEEPSLATE)
			.lightLevel(state -> 5).setId(FARM_MONITOR_KEY));
	public static final Item FARM_MONITOR_ITEM = item(FARM_MONITOR, FARM_MONITOR_KEY);
	public static final BlockEntityType<FarmMonitorBlockEntity> FARM_MONITOR_BLOCK_ENTITY =
			FabricBlockEntityTypeBuilder.create(FarmMonitorBlockEntity::new, FARM_MONITOR).build();
	public static final ExtendedMenuType<FarmMonitorMenu, FarmMonitorMenu.OpenData> FARM_MONITOR_MENU =
			new ExtendedMenuType<>(FarmMonitorMenu::new, FarmMonitorMenu.OpenData.STREAM_CODEC);

	public static final ResourceKey<Block> CHUNK_LOADER_KEY = key("chunk_loader");
	public static final Block CHUNK_LOADER = new ChunkLoaderBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.POLISHED_DEEPSLATE)
			.lightLevel(state -> state.getValue(ChunkLoaderBlock.ACTIVE) ? 10 : 2).setId(CHUNK_LOADER_KEY));
	public static final Item CHUNK_LOADER_ITEM = item(CHUNK_LOADER, CHUNK_LOADER_KEY);
	public static final BlockEntityType<ChunkLoaderBlockEntity> CHUNK_LOADER_BLOCK_ENTITY =
			FabricBlockEntityTypeBuilder.create(ChunkLoaderBlockEntity::new, CHUNK_LOADER).build();
	public static final MenuType<ChunkLoaderMenu> CHUNK_LOADER_MENU = new MenuType<>(ChunkLoaderMenu::new, FeatureFlags.VANILLA_SET);

	public static final ResourceKey<Block> BLOCK_PLACER_KEY = key("block_placer");
	public static final Block BLOCK_PLACER = new BlockPlacerBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.DISPENSER).setId(BLOCK_PLACER_KEY));
	public static final Item BLOCK_PLACER_ITEM = item(BLOCK_PLACER, BLOCK_PLACER_KEY);
	public static final BlockEntityType<BlockPlacerBlockEntity> BLOCK_PLACER_BLOCK_ENTITY =
			FabricBlockEntityTypeBuilder.create(BlockPlacerBlockEntity::new, BLOCK_PLACER).build();

	private FarmContent() {
	}

	private static ResourceKey<Block> key(String name) {
		return ResourceKey.create(Registries.BLOCK, JTechStorage.id(name));
	}

	private static Item item(Block block, ResourceKey<Block> key) {
		Item item = new BlockItem(block, new Item.Properties().useBlockDescriptionPrefix().setId(ResourceKey.create(Registries.ITEM, key.identifier())));
		CREATIVE.add(item);
		return item;
	}

	public static List<Item> creativeItems() {
		return CREATIVE;
	}

	public static void init() {
		register(AMETHYST_HOPPER_KEY, AMETHYST_HOPPER, AMETHYST_HOPPER_ITEM);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, JTechStorage.id("amethyst_hopper"), AMETHYST_HOPPER_BLOCK_ENTITY);
		register(CRYSTAL_PUSHER_KEY, CRYSTAL_PUSHER, CRYSTAL_PUSHER_ITEM);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, JTechStorage.id("crystal_pusher"), CRYSTAL_PUSHER_BLOCK_ENTITY);
		Registry.register(BuiltInRegistries.MENU, JTechStorage.id("crystal_pusher"), CRYSTAL_PUSHER_MENU);
		register(FARM_MONITOR_KEY, FARM_MONITOR, FARM_MONITOR_ITEM);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, JTechStorage.id("farm_monitor"), FARM_MONITOR_BLOCK_ENTITY);
		Registry.register(BuiltInRegistries.MENU, JTechStorage.id("farm_monitor"), FARM_MONITOR_MENU);
		register(BLOCK_PLACER_KEY, BLOCK_PLACER, BLOCK_PLACER_ITEM);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, JTechStorage.id("block_placer"), BLOCK_PLACER_BLOCK_ENTITY);
		register(CHUNK_LOADER_KEY, CHUNK_LOADER, CHUNK_LOADER_ITEM);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, JTechStorage.id("chunk_loader"), CHUNK_LOADER_BLOCK_ENTITY);
		Registry.register(BuiltInRegistries.MENU, JTechStorage.id("chunk_loader"), CHUNK_LOADER_MENU);

		PayloadTypeRegistry.serverboundPlay().register(FarmPayloads.Rename.TYPE, FarmPayloads.Rename.STREAM_CODEC);
		PayloadTypeRegistry.serverboundPlay().register(FarmPayloads.Request.TYPE, FarmPayloads.Request.STREAM_CODEC);
		PayloadTypeRegistry.clientboundPlay().register(FarmPayloads.Farms.TYPE, FarmPayloads.Farms.STREAM_CODEC);

		ServerPlayNetworking.registerGlobalReceiver(FarmPayloads.Rename.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();

			if (player.level().getBlockEntity(payload.pos()) instanceof FarmMonitorBlockEntity monitor
					&& payload.pos().distToCenterSqr(player.position()) <= 8.0 * 8.0 && monitor.mayEdit(player)) {
				monitor.setFarmName(payload.name());
			}
		});

		ServerPlayNetworking.registerGlobalReceiver(FarmPayloads.Request.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();

			if (player.containerMenu instanceof StorageTerminalMenu menu && menu.containerId == payload.containerId() && menu.stillValid(player)) {
				GlobalPos terminal = menu.terminalPos();
				List<FarmPayloads.Farm> list = new ArrayList<>();

				if (terminal != null) {
					long now = player.level().getGameTime();

					for (FarmData.Entry entry : FarmData.get(player.level().getServer()).forTerminal(terminal)) {
						list.add(new FarmPayloads.Farm(entry.name(), entry.monitor().dimension().identifier().toString(), entry.monitor().pos(),
								entry.perHour(), entry.status(), entry.topItem(), (int) Math.clamp((now - entry.updated()) / 1200, 0, 1000000)));
					}
				}

				ServerPlayNetworking.send(player, new FarmPayloads.Farms(payload.containerId(), list));
			}
		});

		ServerLifecycleEvents.SERVER_STOPPED.register(server -> FarmZones.clear());
	}

	private static void register(ResourceKey<Block> key, Block block, Item item) {
		Registry.register(BuiltInRegistries.BLOCK, key, block);
		Registry.register(BuiltInRegistries.ITEM, key.identifier(), item);
	}
}
