package dev.bartels.jtechstorage.depths.structure;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

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
 * General cave structure of the Crystal Depths (v0.36) from a single template.
 * <ul>
 * <li>{@code placement: "floor"} – on the cave floor with {@code headroom} blocks of air above, gets a foundation
 * (Storage Masters' settlement).</li>
 * <li>{@code placement: "buried"} – inside the rock; the template hollows out its own passages (Crystal Mine).</li>
 * </ul>
 * {@code floor_offset} is the height of the floor within the template.
 */
public class CaveStructure extends Structure {
	public static final Identifier ID = JTechStorage.id("cave_template");
	public static final MapCodec<CaveStructure> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			settingsCodec(i),
			Identifier.CODEC.fieldOf("template").forGetter(s -> s.template),
			Codec.STRING.optionalFieldOf("placement", "floor").forGetter(s -> s.placement),
			Codec.INT.optionalFieldOf("headroom", 9).forGetter(s -> s.headroom),
			Codec.INT.optionalFieldOf("floor_offset", 0).forGetter(s -> s.floorOffset)).apply(i, CaveStructure::new));
	public static final StructureType<CaveStructure> TYPE = Registry.register(BuiltInRegistries.STRUCTURE_TYPE, ID, () -> CODEC);
	public static final StructurePieceType PIECE = Registry.register(BuiltInRegistries.STRUCTURE_PIECE, ID,
			(StructurePieceType.StructureTemplateType) Piece::new);

	private final Identifier template;
	private final String placement;
	private final int headroom;
	private final int floorOffset;

	public CaveStructure(StructureSettings settings, Identifier template, String placement, int headroom, int floorOffset) {
		super(settings);
		this.template = template;
		this.placement = placement;
		this.headroom = headroom;
		this.floorOffset = floorOffset;
	}

	public static void init() {
		// loads the static registrations
	}

	@Override
	public StructureType<?> type() {
		return TYPE;
	}

	private boolean buried() {
		return "buried".equals(placement);
	}

	@Override
	protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
		RandomSource random = context.random();
		Rotation rotation = Rotation.getRandom(random);
		ChunkPos chunk = context.chunkPos();
		BlockPos corner = new BlockPos(chunk.getMinBlockX(), 0, chunk.getMinBlockZ());
		StructureTemplate tpl = context.structureTemplateManager().getOrCreate(template);
		BoundingBox box = tpl.getBoundingBox(settings(rotation), corner);
		int inset = Math.max(3, Math.min(box.getXSpan(), box.getZSpan()) / 6);

		int[][] samples = {
				{box.getCenter().getX(), box.getCenter().getZ()},
				{box.minX() + inset, box.minZ() + inset}, {box.maxX() - inset, box.minZ() + inset},
				{box.minX() + inset, box.maxZ() - inset}, {box.maxX() - inset, box.maxZ() - inset}};
		NoiseColumn[] columns = new NoiseColumn[samples.length];

		for (int i = 0; i < samples.length; i++) {
			columns[i] = context.chunkGenerator().getBaseColumn(samples[i][0], samples[i][1], context.heightAccessor(), context.randomState());
		}

		int minY = context.heightAccessor().getMinY() + 12;
		int maxY = context.heightAccessor().getMaxY() - Math.max(headroom, box.getYSpan()) - 8;
		List<Integer> candidates = new ArrayList<>();

		for (int y = minY; y < maxY; y++) {
			if (buried() ? rock(columns[0], y, -floorOffset, headroom) : floor(columns[0], y, headroom)) {
				candidates.add(y);
			}
		}

		while (!candidates.isEmpty()) {
			int y = candidates.remove(random.nextInt(candidates.size()));
			boolean ok = true;

			for (int i = 1; i < columns.length && ok; i++) {
				ok = buried() ? rock(columns[i], y, -floorOffset, 4) : cornerOk(columns[i], y);
			}

			if (ok) {
				BlockPos pos = new BlockPos(corner.getX(), y - floorOffset, corner.getZ());
				boolean fill = !buried();
				return Optional.of(new GenerationStub(pos,
						builder -> builder.addPiece(new Piece(context.structureTemplateManager(), template, pos, rotation, fill))));
			}
		}

		return Optional.empty();
	}

	private static boolean solid(BlockState state) {
		return !state.isAir() && state.getFluidState().isEmpty();
	}

	/** Solid ground at y, with {@code room} blocks of air above. */
	private static boolean floor(NoiseColumn column, int y, int room) {
		if (!solid(column.getBlock(y)) || !solid(column.getBlock(y - 1))) {
			return false;
		}

		for (int k = 1; k <= room; k++) {
			if (!column.getBlock(y + k).isAir()) {
				return false;
			}
		}

		return true;
	}

	/** Solid rock from y+from to y+room (room for the mine, including its lower level). */
	private static boolean rock(NoiseColumn column, int y, int from, int room) {
		for (int k = Math.min(-2, from); k <= room; k++) {
			if (!solid(column.getBlock(y + k))) {
				return false;
			}
		}

		return true;
	}

	/** Corner: ground at most 5 blocks lower or 4 higher, no water above. */
	private static boolean cornerOk(NoiseColumn column, int y) {
		boolean ground = false;

		for (int f = y - 5; f <= y + 4; f++) {
			if (solid(column.getBlock(f)) && column.getBlock(f + 1).isAir()) {
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

	/** The single piece; fills a foundation for floor structures. */
	public static class Piece extends TemplateStructurePiece {
		private final boolean fill;

		public Piece(StructureTemplateManager manager, Identifier template, BlockPos pos, Rotation rotation, boolean fill) {
			super(PIECE, 0, manager, template, template.toString(), settings(rotation), pos);
			this.fill = fill;
		}

		public Piece(StructureTemplateManager manager, CompoundTag tag) {
			super(PIECE, tag, manager, id -> settings(Rotation.valueOf(tag.getStringOr("Rot", "NONE"))));
			this.fill = tag.getBooleanOr("Fill", true);
		}

		@Override
		protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
			super.addAdditionalSaveData(context, tag);
			tag.putString("Rot", placeSettings.getRotation().name());
			tag.putBoolean("Fill", fill);
		}

		@Override
		protected void handleDataMarker(String marker, BlockPos pos, ServerLevelAccessor level, RandomSource random, BoundingBox box) {
		}

		@Override
		public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
				BoundingBox chunkBox, ChunkPos chunk, BlockPos origin) {
			super.postProcess(level, structures, generator, random, chunkBox, chunk, origin);

			if (!fill) {
				return;
			}

			BlockState stone = DepthsContent.CRYSTAL_STONE.defaultBlockState();
			int floorY = boundingBox.minY();
			BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();

			for (int x = Math.max(boundingBox.minX(), chunkBox.minX()); x <= Math.min(boundingBox.maxX(), chunkBox.maxX()); x++) {
				for (int z = Math.max(boundingBox.minZ(), chunkBox.minZ()); z <= Math.min(boundingBox.maxZ(), chunkBox.maxZ()); z++) {
					p.set(x, floorY, z);

					if (level.getBlockState(p).isAir()) {
						continue;
					}

					for (int y = floorY - 1; y >= floorY - 12 && y > level.getMinY(); y--) {
						p.setY(y);
						BlockState state = level.getBlockState(p);

						if (!state.isAir() && state.getFluidState().isEmpty() && !state.canBeReplaced()) {
							break;
						}

						level.setBlock(p, stone, 2);
					}
				}
			}
		}
	}
}
