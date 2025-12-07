package taethaprod.emplus.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import taethaprod.emplus.EMPlus;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public final class BossScalingConfigManager {
private static final Gson GSON = new GsonBuilder()
			.registerTypeAdapter(BossScalingConfig.TierScaling.class, new TierScalingAdapter())
			.setPrettyPrinting()
			.create();
	private static final Path CONFIG_DIR = net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir()
			.resolve("emplus")
			.resolve("mythickeys");
	private static final Path CONFIG_PATH = CONFIG_DIR.resolve("emplus_scaling.json");
	private static BossScalingConfig CONFIG;

	private BossScalingConfigManager() {
	}

	public static BossScalingConfig get() {
		if (CONFIG == null) {
			load();
		}
		return CONFIG;
	}

	public static void load() {
		if (!Files.exists(CONFIG_PATH)) {
			CONFIG = createDefault();
			save();
			return;
		}
		try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
			CONFIG = GSON.fromJson(reader, BossScalingConfig.class);
			if (CONFIG == null) {
				CONFIG = createDefault();
			}
			if (CONFIG.tierMultipliers == null || CONFIG.tierMultipliers.isEmpty()) {
				CONFIG.tierMultipliers = defaultMultipliers();
			}
		} catch (IOException e) {
			EMPlus.LOGGER.error("Failed to read scaling config {}, using defaults", CONFIG_PATH, e);
			CONFIG = createDefault();
		}
	}

	public static void save() {
		try {
			Files.createDirectories(CONFIG_PATH.getParent());
			try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
				GSON.toJson(CONFIG, writer);
			}
		} catch (IOException e) {
			EMPlus.LOGGER.error("Failed to save scaling config {}", CONFIG_PATH, e);
		}
	}

	public static double getMultiplierForTier(int tier) {
		return getTierScaling(tier).maxHealth; // kept for backward compatibility usage (not used directly).
	}

	public static BossScalingConfig.TierScaling getTierScaling(int tier) {
		Map<Integer, BossScalingConfig.TierScaling> map = get().tierMultipliers;
		if (map == null) {
			return defaultScale(tier);
		}
		return map.getOrDefault(tier, defaultScale(tier));
	}

	public static int getMaxTier() {
		Map<Integer, BossScalingConfig.TierScaling> map = get().tierMultipliers;
		if (map == null || map.isEmpty()) {
			return 1;
		}
		return map.keySet().stream().mapToInt(Integer::intValue).max().orElse(1);
	}

	public static java.util.List<Integer> getDefinedTiers() {
		Map<Integer, BossScalingConfig.TierScaling> map = get().tierMultipliers;
		if (map == null || map.isEmpty()) {
			return java.util.List.of(1);
		}
		return map.keySet().stream().sorted().toList();
	}

	private static BossScalingConfig createDefault() {
		BossScalingConfig config = new BossScalingConfig();
		config.tierMultipliers = defaultMultipliers();
		return config;
	}

	private static Map<Integer, BossScalingConfig.TierScaling> defaultMultipliers() {
		Map<Integer, BossScalingConfig.TierScaling> map = new HashMap<>();
		for (int i = 1; i <= 10; i++) {
			map.put(i, defaultScale(i));
		}
		return map;
	}

	private static BossScalingConfig.TierScaling defaultScale(int tier) {
		int clamped = Math.min(Math.max(tier, 1), 10);
		double base = switch (clamped) {
			case 1 -> 1.0;
			case 2 -> 2.28;
			case 3 -> 3.0;
			case 4 -> 4.0;
			case 5 -> 5.0;
			case 6 -> 6.66;
			case 7 -> 7.0;
			case 8 -> 8.0;
			case 9 -> 9.11;
			default -> 11.11;
		};
		double move = 1.0 + (clamped - 1) * 0.03; // 1.0 -> ~1.27
		double follow = 1.0 + (clamped - 1) * 0.05; // 1.0 -> 1.45

		BossScalingConfig.TierScaling s = new BossScalingConfig.TierScaling();
		s.maxHealth = base;
		s.armor = base;
		s.attackDamage = base;
		s.movementSpeed = move;
		s.knockbackResistance = 1.0;
		s.followRange = follow;
		return s;
	}

	private static class TierScalingAdapter implements JsonSerializer<BossScalingConfig.TierScaling>, JsonDeserializer<BossScalingConfig.TierScaling> {
		@Override
		public JsonElement serialize(BossScalingConfig.TierScaling src, java.lang.reflect.Type typeOfSrc, JsonSerializationContext context) {
			JsonObject obj = new JsonObject();
			obj.addProperty("maxHealth", src.maxHealth);
			obj.addProperty("armor", src.armor);
			obj.addProperty("attackDamage", src.attackDamage);
			obj.addProperty("movementSpeed", src.movementSpeed);
			obj.addProperty("knockbackResistance", src.knockbackResistance);
			obj.addProperty("followRange", src.followRange);
			return obj;
		}

		@Override
		public BossScalingConfig.TierScaling deserialize(JsonElement json, java.lang.reflect.Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
			if (json == null || json.isJsonNull()) {
				return defaultScale(1);
			}
			if (json.isJsonPrimitive() && json.getAsJsonPrimitive().isNumber()) {
				double all = json.getAsDouble();
				BossScalingConfig.TierScaling s = new BossScalingConfig.TierScaling();
				s.maxHealth = all;
				s.armor = all;
				s.attackDamage = all;
				s.movementSpeed = all;
				s.knockbackResistance = all;
				s.followRange = all;
				return s;
			}
			if (!json.isJsonObject()) {
				throw new JsonParseException("Expected object or number for TierScaling");
			}
			JsonObject obj = json.getAsJsonObject();
			BossScalingConfig.TierScaling s = new BossScalingConfig.TierScaling();
			s.maxHealth = getOrDefault(obj, "maxHealth", 1.0);
			s.armor = getOrDefault(obj, "armor", 1.0);
			s.attackDamage = getOrDefault(obj, "attackDamage", 1.0);
			s.movementSpeed = getOrDefault(obj, "movementSpeed", 1.0);
			s.knockbackResistance = getOrDefault(obj, "knockbackResistance", 1.0);
			s.followRange = getOrDefault(obj, "followRange", 1.0);
			return s;
		}

		private double getOrDefault(JsonObject obj, String key, double def) {
			if (!obj.has(key) || obj.get(key).isJsonNull()) {
				return def;
			}
			try {
				return obj.get(key).getAsDouble();
			} catch (Exception e) {
				return def;
			}
		}
	}
}
