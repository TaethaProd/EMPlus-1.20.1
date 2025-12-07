package taethaprod.emplus.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import taethaprod.emplus.EMPlus;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

public final class SpawnConfigManager {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path CONFIG_DIR = net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir()
			.resolve("emplus")
			.resolve("mythickeys");
	private static final Path CONFIG_PATH = CONFIG_DIR.resolve("emplus_spawns.json");
	private static SpawnConfig CONFIG;

	private SpawnConfigManager() {
	}

	public static SpawnConfig get() {
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
			CONFIG = GSON.fromJson(reader, SpawnConfig.class);
			if (CONFIG == null) {
				CONFIG = createDefault();
			}
			if (CONFIG.allowedEntities == null || CONFIG.allowedEntities.isEmpty()) {
				CONFIG.allowedEntities = defaultEntities();
			}
		} catch (IOException e) {
			EMPlus.LOGGER.error("Failed to read spawn config {}, using defaults", CONFIG_PATH, e);
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
			EMPlus.LOGGER.error("Failed to save spawn config {}", CONFIG_PATH, e);
		}
	}

	private static SpawnConfig createDefault() {
		SpawnConfig config = new SpawnConfig();
		config.allowedEntities = defaultEntities();
		return config;
	}

	private static List<String> defaultEntities() {
		return Arrays.asList(
				"minecraft:zombie",
				"minecraft:skeleton",
				"minecraft:witch"
		);
	}
}
