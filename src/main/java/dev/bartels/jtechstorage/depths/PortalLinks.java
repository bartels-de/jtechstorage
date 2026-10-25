package dev.bartels.jtechstorage.depths;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.GlobalPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import dev.bartels.jtechstorage.JTechStorage;

/**
 * Feste Verbindungen zwischen Amethyst-Portalen (v0.38). Schlüssel ist jeweils der untere linke Portalblock; jede
 * Verbindung wird in beide Richtungen gespeichert.
 */
public class PortalLinks extends SavedData {
	private record Pair(GlobalPos a, GlobalPos b) {
		static final Codec<Pair> CODEC = RecordCodecBuilder.create(i -> i.group(
				GlobalPos.CODEC.fieldOf("a").forGetter(Pair::a),
				GlobalPos.CODEC.fieldOf("b").forGetter(Pair::b)
		).apply(i, Pair::new));
	}

	public static final Codec<PortalLinks> CODEC = Pair.CODEC.listOf().fieldOf("links").xmap(PortalLinks::new, PortalLinks::pairs).codec();
	public static final SavedDataType<PortalLinks> TYPE = new SavedDataType<>(JTechStorage.id("portal_links"), PortalLinks::new, CODEC,
			DataFixTypes.SAVED_DATA_COMMAND_STORAGE);

	private final Map<GlobalPos, GlobalPos> links = new HashMap<>();

	public PortalLinks() {
	}

	private PortalLinks(List<Pair> pairs) {
		for (Pair pair : pairs) {
			links.put(pair.a(), pair.b());
			links.put(pair.b(), pair.a());
		}
	}

	private List<Pair> pairs() {
		return links.entrySet().stream()
				.filter(e -> e.getKey().toString().compareTo(e.getValue().toString()) < 0)
				.map(e -> new Pair(e.getKey(), e.getValue())).toList();
	}

	public static PortalLinks get(MinecraftServer server) {
		return server.getDataStorage().computeIfAbsent(TYPE);
	}

	public Optional<GlobalPos> partner(GlobalPos portal) {
		return Optional.ofNullable(links.get(portal));
	}

	/** Verbindet a und b; alte Verbindungen beider Portale werden gelöst. */
	public void link(GlobalPos a, GlobalPos b) {
		unlink(a);
		unlink(b);
		links.put(a, b);
		links.put(b, a);
		setDirty();
	}

	/** Löst die Verbindung von {@code portal}; gibt den bisherigen Partner zurück. */
	public Optional<GlobalPos> unlink(GlobalPos portal) {
		GlobalPos other = links.remove(portal);

		if (other != null) {
			links.remove(other);
			setDirty();
		}

		return Optional.ofNullable(other);
	}
}
