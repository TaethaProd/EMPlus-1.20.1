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
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class ConfigManager {
private static final Gson GSON = new GsonBuilder()
			.registerTypeAdapter(ModConfig.OriginLoot.LootEntry.class, new LootEntryAdapter())
			.setPrettyPrinting()
			.create();
	private static final Path CONFIG_DIR = net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir()
			.resolve("emplus")
			.resolve("mythickeys");
	private static final Path CONFIG_PATH = CONFIG_DIR.resolve("emplus.json");
	private static ModConfig CONFIG;

	private ConfigManager() {
	}

	public static ModConfig get() {
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
			CONFIG = GSON.fromJson(reader, ModConfig.class);
			if (CONFIG == null) {
				CONFIG = createDefault();
			}
		} catch (IOException e) {
			EMPlus.LOGGER.error("Failed to read config {}, using defaults", CONFIG_PATH, e);
			CONFIG = createDefault();
		} catch (Exception e) {
			EMPlus.LOGGER.error("Invalid config {}, using defaults", CONFIG_PATH, e);
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
			EMPlus.LOGGER.error("Failed to save config {}", CONFIG_PATH, e);
		}
	}

	private static ModConfig createDefault() {
		ModConfig config = new ModConfig();
		config.originsSpecificLoot = false;
		ModConfig.OriginLoot sample = new ModConfig.OriginLoot();
		sample.origin = "origins:human";
		sample.tierLoot.put(1, List.of(new ModConfig.OriginLoot.LootEntry("minecraft:diamond_sword", 1.0)));
		sample.tierLoot.put(2, List.of(
				new ModConfig.OriginLoot.LootEntry("minecraft:golden_apple", 0.25),
				new ModConfig.OriginLoot.LootEntry("minecraft:iron_sword", 0.75)
		));
		// Legacy flat list fallback example.
		sample.items.add(new ModConfig.OriginLoot.LootEntry("minecraft:diamond", 0.5));
		config.originLoot.add(sample);
		return config;
	}

	private static class LootEntryAdapter implements JsonSerializer<ModConfig.OriginLoot.LootEntry>, JsonDeserializer<ModConfig.OriginLoot.LootEntry> {
		@Override
		public JsonElement serialize(ModConfig.OriginLoot.LootEntry src, Type typeOfSrc, JsonSerializationContext context) {
			JsonObject object = new JsonObject();
			object.addProperty("item", src.item);
			object.addProperty("chance", src.chance);
			return object;
		}

		@Override
		public ModConfig.OriginLoot.LootEntry deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
			if (json.isJsonPrimitive() && json.getAsJsonPrimitive().isString()) {
				return new ModConfig.OriginLoot.LootEntry(json.getAsString(), 1.0);
			}
			if (!json.isJsonObject()) {
				throw new JsonParseException("Expected loot entry to be a string or object");
			}
			JsonObject object = json.getAsJsonObject();
			String item = object.has("item") ? object.get("item").getAsString() : "";
			double chance = object.has("chance") ? object.get("chance").getAsDouble() : 1.0;
			return new ModConfig.OriginLoot.LootEntry(item, chance);
		}
	}
}
