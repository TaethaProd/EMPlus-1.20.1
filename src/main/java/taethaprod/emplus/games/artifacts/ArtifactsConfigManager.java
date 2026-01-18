package taethaprod.emplus.games.artifacts;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import taethaprod.emplus.EMPlus;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ArtifactsConfigManager {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path CONFIG_DIR = net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir()
			.resolve("emplus")
			.resolve("Games");
	private static final Path CONFIG_PATH = CONFIG_DIR.resolve("artifacts.json");
	private static ArtifactsConfig CONFIG;

	private ArtifactsConfigManager() {
	}

	public static ArtifactsConfig get() {
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
			CONFIG = GSON.fromJson(reader, ArtifactsConfig.class);
			if (CONFIG == null) {
				CONFIG = createDefault();
			}
		} catch (IOException e) {
			EMPlus.LOGGER.error("Failed to read artifacts config {}, using defaults", CONFIG_PATH, e);
			CONFIG = createDefault();
		} catch (Exception e) {
			EMPlus.LOGGER.error("Invalid artifacts config {}, using defaults", CONFIG_PATH, e);
			CONFIG = createDefault();
		}
		ensureDefaults(CONFIG);
	}

	public static void save() {
		try {
			Files.createDirectories(CONFIG_DIR);
			try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
				GSON.toJson(CONFIG, writer);
			}
		} catch (IOException e) {
			EMPlus.LOGGER.error("Failed to save artifacts config {}", CONFIG_PATH, e);
		}
	}

	private static ArtifactsConfig createDefault() {
		return new ArtifactsConfig();
	}

	private static void ensureDefaults(ArtifactsConfig config) {
		if (config.areaRadius <= 0) {
			config.areaRadius = 500;
		}
		if (config.zones <= 0) {
			config.zones = 5;
		}
		if (config.zoneRadius <= 0) {
			config.zoneRadius = 6;
		}
		if (config.captureRadius <= 0) {
			config.captureRadius = config.zoneRadius;
		}
		if (config.artifactsPerChest <= 0) {
			config.artifactsPerChest = 1;
		}
		if (config.artifactsRewardStepRadius <= 0) {
			config.artifactsRewardStepRadius = 500;
		}
		if (config.chestCleanupSeconds < 0) {
			config.chestCleanupSeconds = 60;
		}
		if (config.autoCooldownSeconds < 0) {
			config.autoCooldownSeconds = 3600;
		}
		if (config.captureSeconds <= 0) {
			config.captureSeconds = 25;
		}
		if (config.eventDurationSeconds <= 0) {
			config.eventDurationSeconds = 720;
		}
		if (config.waveIntervalSeconds <= 0) {
			config.waveIntervalSeconds = 30;
		}
		if (config.baseWaveMobs < 0) {
			config.baseWaveMobs = 2;
		}
		if (config.extraWaveMobs < 0) {
			config.extraWaveMobs = 4;
		}
		if (config.leaderExtraMobs < 0) {
			config.leaderExtraMobs = 2;
		}
		if (config.leaderProgressMultiplier <= 0.0) {
			config.leaderProgressMultiplier = 0.8;
		}
		if (config.leaderDecayMultiplier <= 0.0) {
			config.leaderDecayMultiplier = 1.25;
		}
		if (config.mobPool == null || config.mobPool.isEmpty()) {
			config.mobPool = new java.util.ArrayList<>(java.util.List.of(
					"minecraft:zombie",
					"minecraft:skeleton",
					"minecraft:spider"
			));
		}
	}
}
