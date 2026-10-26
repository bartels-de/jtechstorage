package dev.bartels.jtechstorage.depths.structure;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

import dev.bartels.jtechstorage.JTechStorage;
import dev.bartels.jtechstorage.depths.DepthsContent;

/**
 * Jbartels' Workshop (v0.35): an abandoned half-timbered house on the cave floor of the Crystal Depths. The structure looks
 * in the base column for a spot with solid ground and at least 9 blocks of air above it (no water, no lava),
 * digs out its space and gets a foundation.
 */
public class WorkshopStructure extends Structure {
	public static final Identifier ID = JTechStorage.id("jbartels_workshop");
	public static final MapCodec<WorkshopStructure> CODEC = simpleCodec(WorkshopStructure::new);
	public static final StructureType<WorkshopStructure> TYPE = Registry.register(BuiltInRegistries.STRUCTURE_TYPE, ID, () -> CODEC);
	public static final StructurePieceType PIECE = Registry.register(BuiltInRegistries.STRUCTURE_PIECE, ID,
			(StructurePieceType.StructureTemplateType) Piece::new);
	private static final int HEADROOM = 9;

	public WorkshopStructure(StructureSettings settings) {
		super(settings);
	}

	public static void init() {
		// loads the static registrations
	}

	@Override
	public StructureType<?> type() {
		return TYPE;
	}

	@Override
	protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
		RandomSource random = context.random();
		Rotation rotation = Rotation.getRandom(random);
		ChunkPos chunk = context.chunkPos();
		BlockPos corner = new BlockPos(chunk.getMinBlockX(), 0, chunk.getMinBlockZ());
		StructureTemplate template = context.structureTemplateManager().getOrCreate(ID);
		BoundingBox box = template.getBoundingBox(settings(rotation), corner);

		// samples: center and four points within the footprint
		int[][] samples = {
				{box.getCenter().getX(), box.getCenter().getZ()},
				{box.minX() + 4, box.minZ() + 4}, {box.maxX() - 4, box.minZ() + 4},
				{box.minX() + 4, box.maxZ() - 4}, {box.maxX() - 4, box.maxZ() - 4}};
		NoiseColumn[] columns = new NoiseColumn[samples.length];

		for (int i = 0; i < samples.length; i++) {
			columns[i] = context.chunkGenerator().getBaseColumn(samples[i][0], samples[i][1], context.heightAccessor(), context.randomState());
		}

		int minY = context.heightAccessor().getMinY() + 10;
		int maxY = context.heightAccessor().getMaxY() - HEADROOM - 6;
		List<Integer> candidates = new ArrayList<>();

		for (int y = minY; y < maxY; y++) {
			if (floor(columns[0], y, HEADROOM)) {
				candidates.add(y);
			}
		}

		// check in random order whether the corners also have ground
		while (!candidates.isEmpty()) {
			int y = candidates.remove(random.nextInt(candidates.size()));
			boolean ok = true;

			for (int i = 1; i < columns.length && ok; i++) {
				ok = cornerOk(columns[i], y);
			}

			if (ok) {
				BlockPos pos = new BlockPos(corner.getX(), y, corner.getZ());
				return Optional.of(new GenerationStub(pos, builder -> builder.addPiece(new Piece(context.structureTemplateManager(), pos, rotation))));
			}
		}

		return Optional.empty();
	}

	private static boolean solid(BlockState state) {
		return !state.isAir() && state.getFluidState().isEmpty();
	}

	private static boolean clear(BlockState state) {
		return state.isAir();
	}

	/** Solid ground at y, with {@code room} blocks of air above. */
	private static boolean floor(NoiseColumn column, int y, int room) {
		if (!solid(column.getBlock(y)) || !solid(column.getBlock(y - 1))) {
			return false;
		}

		for (int k = 1; k <= room; k++) {
			if (!clear(column.getBlock(y + k))) {
				return false;
			}
		}

		return true;
	}

	/** Corner: ground at most 5 blocks lower or 4 higher (dug away/filled in), no water above. */
	private static boolean cornerOk(NoiseColumn column, int y) {
		boolean ground = false;

		for (int f = y - 5; f <= y + 4; f++) {
			if (solid(column.getBlock(f)) && clear(column.getBlock(f + 1))) {
				ground = true;
			}
		}

		for (int k = 0; k <= 5; k++) {
			if (!column.getBlock(y + k).getFluidState().isEmpty()) {
				return false;
			}
		}

		return ground;
	}

	static StructurePlaceSettings settings(Rotation rotation) {
		return new StructurePlaceSettings().setRotation(rotation).setMirror(Mirror.NONE).addProcessor(BlockIgnoreProcessor.STRUCTURE_BLOCK);
	}

	/** The single piece of the workshop; fills a foundation below the ground. */
	public static class Piece extends TemplateStructurePiece {
		public Piece(StructureTemplateManager manager, BlockPos pos, Rotation rotation) {
			super(PIECE, 0, manager, ID, ID.toString(), settings(rotation), pos);
		}

		public Piece(StructureTemplateManager manager, CompoundTag tag) {
			super(PIECE, tag, manager, id -> settings(Rotation.valueOf(tag.getStringOr("Rot", "NONE"))));
		}

		@Override
		protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
			super.addAdditionalSaveData(context, tag);
			tag.putString("Rot", placeSettings.getRotation().name());
		}

		@Override
		protected void handleDataMarker(String marker, BlockPos pos, ServerLevelAccessor level, RandomSource random, BoundingBox box) {
		}

		@Override
		public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
				BoundingBox chunkBox, ChunkPos chunk, BlockPos origin) {
			super.postProcess(level, structures, generator, random, chunkBox, chunk, origin);
			BlockState fill = DepthsContent.CRYSTAL_STONE.defaultBlockState();
			int floorY = boundingBox.minY();
			BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();

			for (int x = Math.max(boundingBox.minX(), chunkBox.minX()); x <= Math.min(boundingBox.maxX(), chunkBox.maxX()); x++) {
				for (int z = Math.max(boundingBox.minZ(), chunkBox.minZ()); z <= Math.min(boundingBox.maxZ(), chunkBox.maxZ()); z++) {
					p.set(x, floorY, z);

					if (level.getBlockState(p).isAir()) {
						continue;   // the workshop has no ground here
					}

					for (int y = floorY - 1; y >= floorY - 12 && y > level.getMinY(); y--) {
						p.setY(y);
						BlockState state = level.getBlockState(p);

						if (!state.isAir() && state.getFluidState().isEmpty() && !state.canBeReplaced()) {
							break;
						}

						level.setBlock(p, fill, 2);
					}
				}
			}
		}
	}
}
