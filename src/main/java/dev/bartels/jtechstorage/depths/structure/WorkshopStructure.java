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
 * Jbartels' Werkstatt (v0.35): ein verlassenes Fachwerkhaus auf dem Höhlenboden der Kristalltiefe. Die Struktur sucht
 * in der Grundsäule eine Stelle mit festem Boden und mindestens 9 Blöcken Luft darüber (kein Wasser, keine Lava),
 * gräbt sich ihren Platz frei und bekommt ein Fundament.
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
		// lädt die statischen Registrierungen
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

		// Stichproben: Mitte und vier Punkte innerhalb der Grundfläche
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

		// in zufälliger Reihenfolge prüfen, ob die Ecken auch Boden haben
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

	/** Fester Boden bei y, darüber {@code room} Blöcke Luft. */
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

	/** Ecke: Boden höchstens 5 Blöcke tiefer oder 4 höher (wird abgegraben/aufgefüllt), kein Wasser darüber. */
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

	/** Das eine Teil der Werkstatt; füllt unter dem Boden ein Fundament auf. */
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
						continue;   // hier hat die Werkstatt keinen Boden
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
