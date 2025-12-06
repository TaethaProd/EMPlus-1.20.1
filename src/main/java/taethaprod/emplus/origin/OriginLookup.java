package taethaprod.emplus.origin;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;

import java.util.Optional;
import java.util.Set;

public final class OriginLookup {
	private static final String ORIGINS_MOD_ID = "origins";

	private OriginLookup() {
	}

	public static Optional<String> getOriginId(PlayerEntity player) {
		if (!FabricLoader.getInstance().isModLoaded(ORIGINS_MOD_ID)) {
			return Optional.empty();
		}
		NbtCompound nbt = player.writeNbt(new NbtCompound());

		// Common keys to try first.
		if (nbt.contains("Origin", NbtElement.STRING_TYPE)) {
			String origin = nbt.getString("Origin");
			if (isOriginId(origin)) {
				return Optional.of(origin);
			}
		}
		if (nbt.contains("origins:origin", NbtElement.STRING_TYPE)) {
			String origin = nbt.getString("origins:origin");
			if (isOriginId(origin)) {
				return Optional.of(origin);
			}
		}
		if (nbt.contains("origins:origin", NbtElement.COMPOUND_TYPE)) {
			Optional<String> fromLayers = readFromOriginLayers(nbt.getCompound("origins:origin"));
			if (fromLayers.isPresent()) {
				return fromLayers;
			}
		}

		// Search nested compounds (e.g., under cardinal_components).
		Optional<String> nestedOrigin = findOriginCompound(nbt);
		if (nestedOrigin.isPresent()) {
			return nestedOrigin;
		}

		// Fallback: scan string entries for an origins namespace.
		return scanForOriginString(nbt);
	}

	private static Optional<String> readFromOriginLayers(NbtCompound originRoot) {
		// Direct field, if present.
		if (originRoot.contains("Origin", NbtElement.STRING_TYPE)) {
			String direct = originRoot.getString("Origin");
			if (isOriginId(direct)) {
				return Optional.of(direct);
			}
		}

		if (!originRoot.contains("OriginLayers", NbtElement.LIST_TYPE)) {
			return Optional.empty();
		}
		NbtList list = originRoot.getList("OriginLayers", NbtElement.COMPOUND_TYPE);
		for (int i = 0; i < list.size(); i++) {
			NbtElement el = list.get(i);
			if (!(el instanceof NbtCompound layer)) {
				continue;
			}
			if (layer.contains("Origin", NbtElement.STRING_TYPE)) {
				String originId = layer.getString("Origin");
				if (isOriginId(originId)) {
					return Optional.of(originId);
				}
			}
		}
		return Optional.empty();
	}

	private static Optional<String> findOriginCompound(NbtCompound nbt) {
		for (String key : nbt.getKeys()) {
			NbtElement element = nbt.get(key);
			if (element instanceof NbtCompound child) {
				if ("origins:origin".equals(key)) {
					Optional<String> fromLayers = readFromOriginLayers(child);
					if (fromLayers.isPresent()) {
						return fromLayers;
					}
				}
				Optional<String> nested = findOriginCompound(child);
				if (nested.isPresent()) {
					return nested;
				}
			} else if (element instanceof NbtList list) {
				for (NbtElement entry : list) {
					if (entry instanceof NbtCompound entryCompound) {
						Optional<String> nested = findOriginCompound(entryCompound);
						if (nested.isPresent()) {
							return nested;
						}
					}
				}
			}
		}
		return Optional.empty();
	}

	private static Optional<String> scanForOriginString(NbtCompound nbt) {
		Set<String> keys = nbt.getKeys();
		for (String key : keys) {
			NbtElement element = nbt.get(key);
			if (element instanceof NbtString string) {
				String value = string.asString();
				if (isOriginId(value)) {
					return Optional.of(value);
				}
			} else if (element instanceof NbtCompound child) {
				Optional<String> nested = scanForOriginString(child);
				if (nested.isPresent()) {
					return nested;
				}
			}
		}
		return Optional.empty();
	}

	private static boolean isOriginId(String value) {
		return value != null && value.contains(":") && value.startsWith("origins:");
	}
}
