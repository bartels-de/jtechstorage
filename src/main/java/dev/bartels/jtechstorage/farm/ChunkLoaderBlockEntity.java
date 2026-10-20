package dev.bartels.jtechstorage.farm;

import java.util.UUID;

import org.jspecify.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import dev.bartels.jtechstorage.ModConfig;

/**
 * Chunk Loader (v0.27.1): hält seinen eigenen Chunk dauerhaft geladen (wie /forceload), solange er Amethyst als Treibstoff
 * hat und sein Besitzer das Limit nicht überschreitet. 1 Scherbe = 60 Minuten (Config), Amethyst-Block = 4 Scherben.
 */
public class ChunkLoaderBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer {
	public static final int ACTIVE = 0;
	public static final int NO_FUEL = 1;
	public static final int LIMIT = 2;
	public static final int NO_OWNER = 3;

	private NonNullList<ItemStack> items = NonNullList.withSize(1, ItemStack.EMPTY);
	private @Nullable UUID owner;
	/** Verbleibende Ticks der gerade „brennenden“ Scherbe. */
	private int burn;
	private int status = NO_FUEL;
	private int tick;
	private @Nullable UUID boundsViewer;
	private int boundsTicks;

	public ChunkLoaderBlockEntity(BlockPos pos, BlockState state) {
		super(FarmContent.CHUNK_LOADER_BLOCK_ENTITY, pos, state);
	}

	public static int ticksPerShard() {
		return ModConfig.get().farm.chunkLoaderMinutesPerShard * 1200;
	}

	/** Scherben-Wert eines Stapels (Scherbe 1, Block 4). */
	public static int shardValue(ItemStack stack) {
		return stack.is(Items.AMETHYST_SHARD) ? 1 : stack.is(Items.AMETHYST_BLOCK) ? 4 : 0;
	}

	public void setOwner(UUID owner) {
		this.owner = owner;
		setChanged();
	}

	public @Nullable UUID getOwner() {
		return owner;
	}

	public int getStatus() {
		return status;
	}

	/** Restlaufzeit in Minuten (laufende Scherbe + Vorrat). */
	public int minutesLeft() {
		long ticks = burn + (long) items.get(0).getCount() * shardValue(items.get(0)) * ticksPerShard();
		return (int) Math.min(Integer.MAX_VALUE, ticks / 1200);
	}

	public int ownerCount() {
		return owner == null || !(level instanceof ServerLevel server) ? 0 : ChunkLoaderData.get(server.getServer()).count(owner);
	}

	public void showBounds(ServerPlayer player) {
		boundsViewer = player.getUUID();
		boundsTicks = 200;
	}

	private GlobalPos globalPos() {
		return GlobalPos.of(level.dimension(), worldPosition);
	}

	public static void serverTick(Level level, BlockPos pos, BlockState state, ChunkLoaderBlockEntity loader) {
		if (!(level instanceof ServerLevel server)) {
			return;
		}

		if (loader.boundsTicks > 0) {
			loader.boundsTicks--;

			if (loader.boundsTicks % 10 == 0 && loader.boundsViewer != null
					&& server.getServer().getPlayerList().getPlayer(loader.boundsViewer) instanceof ServerPlayer viewer && viewer.level() == level) {
				loader.drawBounds(server, viewer);
			}
		}

		if (++loader.tick % 20 != 0) {
			return;
		}

		ChunkLoaderData data = ChunkLoaderData.get(server.getServer());
		GlobalPos here = loader.globalPos();
		boolean active = data.isActive(here);
		int status;

		if (loader.owner == null) {
			status = NO_OWNER;
		} else if (!active && data.count(loader.owner) >= ModConfig.get().farm.chunkLoadersPerPlayer) {
			status = LIMIT;
		} else {
			if (loader.burn <= 0) {
				loader.refuel();
			}

			status = loader.burn > 0 ? ACTIVE : NO_FUEL;
		}

		if (status == ACTIVE) {
			loader.burn -= 20;

			if (!active) {
				data.setActive(here, loader.owner, true);
				server.getChunkSource().updateChunkForced(new net.minecraft.world.level.ChunkPos(pos.getX() >> 4, pos.getZ() >> 4), true);
			}
		} else if (active) {
			loader.deactivate(server);

			if (status == NO_FUEL && loader.owner != null && server.getServer().getPlayerList().getPlayer(loader.owner) instanceof ServerPlayer owner) {
				owner.sendSystemMessage(Component.translatable("jtechstorage.chunk_loader.out_of_fuel", pos.getX(), pos.getY(), pos.getZ())
						.withStyle(ChatFormatting.GOLD));
			}
		}

		if (status != loader.status) {
			loader.status = status;
			loader.setChanged();
		}

		if (state.getValue(ChunkLoaderBlock.ACTIVE) != (status == ACTIVE)) {
			level.setBlock(pos, state.setValue(ChunkLoaderBlock.ACTIVE, status == ACTIVE), 3);
		}

		if (loader.tick % 1200 == 0) {
			loader.setChanged();
		}
	}

	private void refuel() {
		ItemStack fuel = items.get(0);
		int value = shardValue(fuel);

		if (value > 0) {
			fuel.shrink(1);
			burn += value * ticksPerShard();
			setChanged();
		}
	}

	private void deactivate(ServerLevel server) {
		ChunkLoaderData data = ChunkLoaderData.get(server.getServer());
		GlobalPos here = globalPos();

		if (data.isActive(here)) {
			data.setActive(here, owner != null ? owner : new UUID(0, 0), false);

			if (!data.otherInChunk(here)) {
				server.getChunkSource().updateChunkForced(new net.minecraft.world.level.ChunkPos(worldPosition.getX() >> 4, worldPosition.getZ() >> 4), false);
			}
		}
	}

	private void drawBounds(ServerLevel level, ServerPlayer viewer) {
		int x0 = worldPosition.getX() >> 4 << 4;
		int z0 = worldPosition.getZ() >> 4 << 4;
		double y = viewer.getY() + 0.2;

		for (int k = 0; k <= 16; k += 2) {
			for (double h = 0; h <= 3; h += 1.5) {
				level.sendParticles(viewer, ParticleTypes.END_ROD, true, false, x0 + k, y + h, z0, 1, 0, 0, 0, 0);
				level.sendParticles(viewer, ParticleTypes.END_ROD, true, false, x0 + k, y + h, z0 + 16, 1, 0, 0, 0, 0);
				level.sendParticles(viewer, ParticleTypes.END_ROD, true, false, x0, y + h, z0 + k, 1, 0, 0, 0, 0);
				level.sendParticles(viewer, ParticleTypes.END_ROD, true, false, x0 + 16, y + h, z0 + k, 1, 0, 0, 0, 0);
			}
		}
	}

	@Override
	public void preRemoveSideEffects(BlockPos pos, BlockState state) {
		super.preRemoveSideEffects(pos, state);

		if (level instanceof ServerLevel server) {
			deactivate(server);
		}
	}

	// ------------------------------------------------------------------
	// Treibstoff-Platz (auch für Trichter)
	// ------------------------------------------------------------------

	@Override
	public boolean canPlaceItem(int slot, ItemStack stack) {
		return shardValue(stack) > 0 && (items.get(0).isEmpty() || ItemStack.isSameItemSameComponents(items.get(0), stack));
	}

	@Override
	public int[] getSlotsForFace(Direction side) {
		return new int[] {0};
	}

	@Override
	public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
		return canPlaceItem(slot, stack);
	}

	@Override
	public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
		return false;
	}

	@Override
	protected Component getDefaultName() {
		return Component.translatable("block.jtechstorage.chunk_loader");
	}

	@Override
	protected NonNullList<ItemStack> getItems() {
		return items;
	}

	@Override
	protected void setItems(NonNullList<ItemStack> items) {
		this.items = items;
	}

	@Override
	protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
		return new ChunkLoaderMenu(containerId, inventory, this);
	}

	@Override
	public int getContainerSize() {
		return 1;
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		ContainerHelper.saveAllItems(output, items);

		if (owner != null) {
			output.store("Owner", UUIDUtil.CODEC, owner);
		}

		output.putInt("Burn", burn);
		output.putInt("Status", status);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		items = NonNullList.withSize(1, ItemStack.EMPTY);
		ContainerHelper.loadAllItems(input, items);
		owner = input.read("Owner", UUIDUtil.CODEC).orElse(null);
		burn = input.getIntOr("Burn", 0);
		status = input.getIntOr("Status", NO_FUEL);
	}
}
