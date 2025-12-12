package taethaprod.emplus.startui;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import taethaprod.emplus.EMPlus;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public final class FactionCommandsConfigManager {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path CONFIG_DIR = net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir()
			.resolve("emplus")
			.resolve("startui");
	private static final Path CONFIG_PATH = CONFIG_DIR.resolve("factionchoose.json");
	private static FactionCommandsConfig CONFIG;

	private FactionCommandsConfigManager() {
	}

	public static FactionCommandsConfig get() {
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
			CONFIG = GSON.fromJson(reader, FactionCommandsConfig.class);
			if (CONFIG == null) {
				CONFIG = createDefault();
			}
		} catch (IOException e) {
			EMPlus.LOGGER.error("Failed to read faction choose config {}, using defaults", CONFIG_PATH, e);
			CONFIG = createDefault();
		}
	}

	public static void save() {
		try {
			Files.createDirectories(CONFIG_DIR);
			try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
				GSON.toJson(CONFIG, writer);
			}
		} catch (IOException e) {
			EMPlus.LOGGER.error("Failed to save faction choose config {}", CONFIG_PATH, e);
		}
	}

	private static FactionCommandsConfig createDefault() {
		FactionCommandsConfig cfg = new FactionCommandsConfig();
		cfg.factionA.add("/tp {player} 0 100 0");
		cfg.factionA.add("/give {player} minecraft:apple");
		cfg.factionB.add("/tp {player} 100 100 100");
		cfg.factionB.add("/give {player} minecraft:apple");
		return cfg;
	}
}
