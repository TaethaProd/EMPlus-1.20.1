package taethaprod.emplus.common;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentState;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class HomeState extends PersistentState {
	private static final String DATA_NAME = "emplus_homes";
	private static final String KEY_HOMES = "Homes";

	private final Map<UUID, HomeLocation> homes = new HashMap<>();

	public static HomeState get(MinecraftServer server) {
		return server.getOverworld().getPersistentStateManager().getOrCreate(
				HomeState::fromNbt,
				HomeState::new,
				DATA_NAME
		);
	}

	public HomeLocation getHome(UUID uuid) {
		return homes.get(uuid);
	}

	public void setHome(UUID uuid, HomeLocation location) {
		if (uuid == null || location == null) {
			return;
		}
		homes.put(uuid, location);
		markDirty();
	}

	public static HomeState fromNbt(NbtCompound tag) {
		HomeState state = new HomeState();
		NbtCompound map = tag.getCompound(KEY_HOMES);
		for (String key : map.getKeys()) {
			try {
				UUID uuid = UUID.fromString(key);
				NbtCompound entry = map.getCompound(key);
				String dimension = entry.getString("dimension");
				if (dimension == null || dimension.isBlank()) {
					continue;
				}
				HomeLocation location = new HomeLocation(
						entry.getDouble("x"),
						entry.getDouble("y"),
						entry.getDouble("z"),
						entry.getFloat("yaw"),
						entry.getFloat("pitch"),
						dimension
				);
				state.homes.put(uuid, location);
			} catch (IllegalArgumentException ignored) {
			}
		}
		return state;
	}

	@Override
	public NbtCompound writeNbt(NbtCompound nbt) {
		NbtCompound map = new NbtCompound();
		for (Map.Entry<UUID, HomeLocation> entry : homes.entrySet()) {
			HomeLocation location = entry.getValue();
			if (location == null || location.dimension == null || location.dimension.isBlank()) {
				continue;
			}
			NbtCompound value = new NbtCompound();
			value.putDouble("x", location.x);
			value.putDouble("y", location.y);
			value.putDouble("z", location.z);
			value.putFloat("yaw", location.yaw);
			value.putFloat("pitch", location.pitch);
			value.putString("dimension", location.dimension);
			map.put(entry.getKey().toString(), value);
		}
		nbt.put(KEY_HOMES, map);
		return nbt;
	}

	public static final class HomeLocation {
		public final double x;
		public final double y;
		public final double z;
		public final float yaw;
		public final float pitch;
		public final String dimension;

		public HomeLocation(double x, double y, double z, float yaw, float pitch, String dimension) {
			this.x = x;
			this.y = y;
			this.z = z;
			this.yaw = yaw;
			this.pitch = pitch;
			this.dimension = dimension;
		}
	}
}
