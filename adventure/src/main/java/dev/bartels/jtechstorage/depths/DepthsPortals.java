package dev.bartels.jtechstorage.depths;

import java.util.Comparator;
import java.util.Optional;

import org.jspecify.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;

import dev.bartels.jtechstorage.ModContent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * Amethyst Portal (v0.30): frame of amethyst blocks (inside 2–21 wide, 3–21 high), lit with an Amethyst Core
 * (the core is not consumed). On the other side, an existing portal within
 * 64 blocks is used or a new one is built – scale 1:1.
 */
public final class DepthsPortals {
	private static final int SEARCH_RADIUS = 64;
	private static final int MAX_SIZE = 21;
	private static final int[] FRONT_OFFSETS = {-1, 1};

	private DepthsPortals() {
	}

	public static boolean isFrame(BlockState state) {
		return state.is(Blocks.AMETHYST_BLOCK);
	}

	private static boolean isEmpty(BlockState state) {
		return state.isAir() || state.is(Blocks.FIRE) || state.is(DepthsContent.DEPTHS_PORTAL);
	}

	static void init() {
		// v0.38: link portals permanently with the storage key
		UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
			if (!player.getItemInHand(hand).is(ModContent.STORAGE_KEY)) {
				return InteractionResult.PASS;
			}

			BlockPos portal = portalAt(level, hit.getBlockPos());

			if (portal == null) {
				return InteractionResult.PASS;
			}

			if (player instanceof ServerPlayer serverPlayer && level instanceof ServerLevel serverLevel) {
				linkWithKey(serverPlayer, serverLevel, portal, player.getItemInHand(hand));
			}

			return InteractionResult.SUCCESS;
		});

		UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
			if (!player.getItemInHand(hand).is(ModContent.AMETHYST_CORE) || !isFrame(level.getBlockState(hit.getBlockPos()))) {
				return InteractionResult.PASS;
			}

			BlockPos inside = hit.getBlockPos().relative(hit.getDirection());

			for (Direction.Axis axis : new Direction.Axis[] {Direction.Axis.X, Direction.Axis.Z}) {
				Shape shape = find(level, inside, axis);

				if (shape != null) {
					if (!level.isClientSide()) {
						shape.fill(level);
						level.playSound(null, inside, SoundEvents.END_PORTAL_SPAWN, SoundSource.BLOCKS, 0.6f, 1.6f);
						level.playSound(null, inside, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 1.5f, 0.8f);

						if (level instanceof ServerLevel server) {
							Vec3 c = shape.center();
							server.sendParticles(ParticleTypes.END_ROD, c.x, c.y, c.z, 80, shape.width() * 0.3, shape.height() * 0.3, 0.3, 0.05);
							server.sendParticles(ParticleTypes.REVERSE_PORTAL, c.x, c.y, c.z, 120, shape.width() * 0.3, shape.height() * 0.3, 0.3, 0.3);
						}
					}

					return InteractionResult.SUCCESS;
				}
			}

			return InteractionResult.PASS;
		});
	}

	/** Portal block at {@code pos} or directly next to it (if {@code pos} is a frame block), otherwise null. */
	private static @Nullable BlockPos portalAt(Level level, BlockPos pos) {
		if (level.getBlockState(pos).is(DepthsContent.DEPTHS_PORTAL)) {
			return pos;
		}

		if (!isFrame(level.getBlockState(pos))) {
			return null;
		}

		for (Direction d : Direction.values()) {
			if (level.getBlockState(pos.relative(d)).is(DepthsContent.DEPTHS_PORTAL)) {
				return pos.relative(d);
			}
		}

		return null;
	}

	/** Lower-left block of the portal that {@code pos} belongs to (identifier for links). */
	static BlockPos anchor(Level level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		Direction.Axis axis = state.getOptionalValue(DepthsPortalBlock.AXIS).orElse(Direction.Axis.X);
		Direction left = axis == Direction.Axis.X ? Direction.WEST : Direction.NORTH;
		BlockPos.MutableBlockPos p = pos.mutable();

		for (int i = 0; i < 32 && level.getBlockState(p.below()).is(DepthsContent.DEPTHS_PORTAL); i++) {
			p.move(Direction.DOWN);
		}

		for (int i = 0; i < 32 && level.getBlockState(p.relative(left)).is(DepthsContent.DEPTHS_PORTAL); i++) {
			p.move(left);
		}

		return p.immutable();
	}

	/**
	 * First click remembers the portal on the key; a click on a portal in the other dimension links the two.
	 * Sneak + click unlinks the clicked portal.
	 */
	private static void linkWithKey(ServerPlayer player, ServerLevel level, BlockPos portal, ItemStack key) {
		GlobalPos here = GlobalPos.of(level.dimension(), anchor(level, portal));
		PortalLinks links = PortalLinks.get(level.getServer());
		Vec3 c = Vec3.atCenterOf(portal);

		if (player.isSecondaryUseActive()) {
			key.remove(DepthsContent.PORTAL_MARK);

			if (links.unlink(here).isPresent()) {
				player.sendOverlayMessage(Component.translatable("jtechstorage.portal.unlinked").withStyle(ChatFormatting.YELLOW));
				level.playSound(null, portal, SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.BLOCKS, 1.0f, 0.7f);
			} else {
				player.sendOverlayMessage(Component.translatable("jtechstorage.portal.not_linked").withStyle(ChatFormatting.GRAY));
			}

			return;
		}

		GlobalPos mark = key.get(DepthsContent.PORTAL_MARK);

		if (mark == null || mark.dimension().equals(here.dimension())) {
			key.set(DepthsContent.PORTAL_MARK, here);
			player.sendOverlayMessage(Component.translatable("jtechstorage.portal.marked").withStyle(ChatFormatting.LIGHT_PURPLE));
			level.playSound(null, portal, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.0f, 1.2f);
			level.sendParticles(ParticleTypes.END_ROD, c.x, c.y, c.z, 12, 0.3, 0.6, 0.3, 0.02);
			return;
		}

		links.link(mark, here);
		key.remove(DepthsContent.PORTAL_MARK);
		player.sendOverlayMessage(Component.translatable("jtechstorage.portal.linked", mark.pos().getX(), mark.pos().getY(), mark.pos().getZ())
				.withStyle(ChatFormatting.GREEN));
		level.playSound(null, portal, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 1.5f, 1.0f);
		level.sendParticles(ParticleTypes.REVERSE_PORTAL, c.x, c.y, c.z, 60, 0.4, 0.8, 0.4, 0.2);
	}

	/** Found portal interior: lower left, orientation, width and height. */
	public record Shape(BlockPos bottomLeft, Direction.Axis axis, int width, int height) {
		Direction right() {
			return axis == Direction.Axis.X ? Direction.EAST : Direction.SOUTH;
		}

		void fill(LevelAccessor level) {
			BlockState state = DepthsContent.DEPTHS_PORTAL.defaultBlockState().setValue(DepthsPortalBlock.AXIS, axis);

			for (int w = 0; w < width; w++) {
				for (int h = 0; h < height; h++) {
					level.setBlock(bottomLeft.relative(right(), w).above(h), state, 18);
				}
			}
		}

		Vec3 center() {
			BlockPos far = bottomLeft.relative(right(), width - 1).above(height - 1);
			return new Vec3((bottomLeft.getX() + far.getX()) / 2.0 + 0.5, (bottomLeft.getY() + far.getY()) / 2.0 + 0.5,
					(bottomLeft.getZ() + far.getZ()) / 2.0 + 0.5);
		}
	}

	/** Searches from {@code inside} for a valid frame in the plane {@code axis}. */
	public static @Nullable Shape find(Level level, BlockPos inside, Direction.Axis axis) {
		if (!isEmpty(level.getBlockState(inside))) {
			return null;
		}

		Direction right = axis == Direction.Axis.X ? Direction.EAST : Direction.SOUTH;
		Direction left = right.getOpposite();
		BlockPos p = inside;

		for (int i = 0; i < MAX_SIZE && isEmpty(level.getBlockState(p.below())); i++) {
			p = p.below();
		}

		if (!isFrame(level.getBlockState(p.below()))) {
			return null;
		}

		for (int i = 0; i < MAX_SIZE && isEmpty(level.getBlockState(p.relative(left))); i++) {
			p = p.relative(left);
		}

		if (!isFrame(level.getBlockState(p.relative(left)))) {
			return null;
		}

		BlockPos bottomLeft = p;
		int width = 0;

		while (width <= MAX_SIZE && isEmpty(level.getBlockState(bottomLeft.relative(right, width)))
				&& isFrame(level.getBlockState(bottomLeft.relative(right, width).below()))) {
			width++;
		}

		if (width < 2 || width > MAX_SIZE || !isFrame(level.getBlockState(bottomLeft.relative(right, width)))) {
			return null;
		}

		int height = 0;

		rows:
		while (height <= MAX_SIZE) {
			BlockPos row = bottomLeft.above(height);

			if (!isFrame(level.getBlockState(row.relative(left))) || !isFrame(level.getBlockState(row.relative(right, width)))) {
				break;
			}

			for (int w = 0; w < width; w++) {
				if (!isEmpty(level.getBlockState(row.relative(right, w)))) {
					break rows;
				}
			}

			height++;
		}

		if (height < 3 || height > MAX_SIZE) {
			return null;
		}

		for (int w = 0; w < width; w++) {
			if (!isFrame(level.getBlockState(bottomLeft.relative(right, w).above(height)))) {
				return null;
			}
		}

		return new Shape(bottomLeft, axis, width, height);
	}

	// ------------------------------------------------------------------
	// Travel
	// ------------------------------------------------------------------

	static @Nullable TeleportTransition destination(ServerLevel from, Entity entity, BlockPos pos) {
		MinecraftServer server = from.getServer();
		ServerLevel target = from.dimension() == DepthsContent.DEPTHS ? server.overworld() : server.getLevel(DepthsContent.DEPTHS);

		if (target == null) {
			return null;
		}

		WorldBorder border = target.getWorldBorder();
		BlockPos wanted = border.clampToBounds(pos.getX(), Math.max(target.getMinY() + 8, Math.min(pos.getY(), target.getMaxY() - 8)), pos.getZ());
		Direction.Axis axis = from.getBlockState(pos).getOptionalValue(DepthsPortalBlock.AXIS).orElse(Direction.Axis.X);
		BlockPos portal = linked(from, target, pos);

		if (portal == null) {
			portal = findPortal(target, wanted).orElse(null);
		}

		if (portal == null) {
			portal = createPortal(target, wanted, axis);
		}

		while (target.getBlockState(portal.below()).is(DepthsContent.DEPTHS_PORTAL)) {
			portal = portal.below();
		}

		return new TeleportTransition(target, Vec3.atBottomCenterOf(portal), Vec3.ZERO, entity.getYRot(), entity.getXRot(),
				TeleportTransition.PLAY_PORTAL_SOUND.then(TeleportTransition.PLACE_PORTAL_TICKET));
	}

	/** Target of a permanent link (v0.38) if the partner portal still exists; otherwise null (and the link is dropped). */
	private static @Nullable BlockPos linked(ServerLevel from, ServerLevel target, BlockPos pos) {
		PortalLinks links = PortalLinks.get(from.getServer());
		GlobalPos here = GlobalPos.of(from.dimension(), anchor(from, pos));
		GlobalPos partner = links.partner(here).orElse(null);

		if (partner == null || !partner.dimension().equals(target.dimension())) {
			return null;
		}

		target.getChunk(partner.pos());   // load the target chunk

		if (!target.getBlockState(partner.pos()).is(DepthsContent.DEPTHS_PORTAL)) {
			links.unlink(here);
			return null;
		}

		return partner.pos();
	}

	private static Optional<BlockPos> findPortal(ServerLevel level, BlockPos pos) {
		PoiManager poi = level.getPoiManager();
		poi.ensureLoadedAndValid(level, pos, SEARCH_RADIUS);
		WorldBorder border = level.getWorldBorder();
		return poi.getInSquare(holder -> holder.is(DepthsContent.PORTAL_POI), pos, SEARCH_RADIUS, PoiManager.Occupancy.ANY)
				.map(PoiRecord::getPos)
				.filter(border::isWithinBounds)
				.filter(p -> level.getBlockState(p).is(DepthsContent.DEPTHS_PORTAL))
				.min(Comparator.comparingDouble(p -> p.distSqr(pos)));
	}

	/** Builds a 4×5 portal at a suitable spot nearby; returns a portal block. */
	private static BlockPos createPortal(ServerLevel level, BlockPos wanted, Direction.Axis axis) {
		Direction right = axis == Direction.Axis.X ? Direction.EAST : Direction.SOUTH;
		Direction front = axis == Direction.Axis.X ? Direction.SOUTH : Direction.EAST;
		boolean depths = level.dimension() == DepthsContent.DEPTHS;
		BlockPos spot = null;

		search:
		for (int r = 0; r <= 16; r++) {
			for (int dx = -r; dx <= r; dx++) {
				for (int dz = -r; dz <= r; dz++) {
					if (Math.max(Math.abs(dx), Math.abs(dz)) != r) {
						continue;
					}

					int x = wanted.getX() + dx;
					int z = wanted.getZ() + dz;
					int top = depths ? Math.min(level.getMaxY() - 12, 165) : level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) + 1;
					int bottom = depths ? level.getMinY() + 12 : Math.max(level.getMinY() + 6, top - 12);

					for (int y = top; y >= bottom; y--) {
						BlockPos base = new BlockPos(x, y, z);

						if (fits(level, base, right, front)) {
							spot = base;
							break search;
						}
					}
				}
			}
		}

		if (spot == null) {
			int y = depths ? 64 : level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, wanted.getX(), wanted.getZ()) + 1;
			spot = new BlockPos(wanted.getX(), y, wanted.getZ());
		}

		BlockState frame = Blocks.AMETHYST_BLOCK.defaultBlockState();
		BlockState floor = depths ? DepthsContent.POLISHED_CRYSTAL_STONE.defaultBlockState() : Blocks.CALCITE.defaultBlockState();

		// Platform at the height of the lower frame row (in front of, behind and beside the portal) + clearance above
		for (int i = -1; i <= 4; i++) {
			for (int f = -1; f <= 1; f++) {
				BlockPos column = spot.relative(right, i).relative(front, f);

				if (f == 0 && i >= 0 && i <= 3) {
					if (!level.getBlockState(column.below()).isFaceSturdy(level, column.below(), Direction.UP)) {
						level.setBlock(column.below(), floor, 3);
					}

					continue;
				}

				level.setBlock(column, floor, 3);

				for (int h = 1; h <= 3; h++) {
					level.setBlock(column.above(h), Blocks.AIR.defaultBlockState(), 3);
				}
			}
		}

		for (int i = 0; i < 4; i++) {
			for (int h = 0; h < 5; h++) {
				BlockPos p = spot.relative(right, i).above(h);
				boolean edge = i == 0 || i == 3 || h == 0 || h == 4;
				level.setBlock(p, edge ? frame : Blocks.AIR.defaultBlockState(), 3);
			}
		}

		Shape shape = new Shape(spot.relative(right, 1).above(1), axis, 2, 3);
		shape.fill(level);
		return shape.bottomLeft();
	}

	/** Does a 4×5 frame fit here (solid ground, air for frame and passage)? */
	private static boolean fits(ServerLevel level, BlockPos base, Direction right, Direction front) {
		for (int i = 0; i < 4; i++) {
			BlockPos column = base.relative(right, i);
			BlockPos below = column.below();
			BlockState ground = level.getBlockState(below);

			if (!ground.isFaceSturdy(level, below, Direction.UP) || ground.is(Blocks.BEDROCK)) {
				return false;
			}

			for (int h = 0; h < 5; h++) {
				if (!replaceable(level.getBlockState(column.above(h)))) {
					return false;
				}
			}

			for (int f : FRONT_OFFSETS) {
				for (int h = 1; h < 3; h++) {
					if (!replaceable(level.getBlockState(column.relative(front, f).above(h)))) {
						return false;
					}
				}
			}
		}

		return true;
	}

	private static boolean replaceable(BlockState state) {
		return (state.isAir() || state.canBeReplaced()) && state.getFluidState().isEmpty();
	}
}
