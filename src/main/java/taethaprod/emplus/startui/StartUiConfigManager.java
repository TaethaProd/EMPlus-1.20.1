package taethaprod.emplus.startui;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import taethaprod.emplus.EMPlus;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public final class StartUiConfigManager {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path CONFIG_DIR = net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir()
			.resolve("emplus")
			.resolve("startui");
	private static final Path CONFIG_PATH = CONFIG_DIR.resolve("common.json");
	private static StartUiConfig CONFIG;

	private StartUiConfigManager() {
	}

	public static StartUiConfig get() {
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
			CONFIG = GSON.fromJson(reader, StartUiConfig.class);
			if (CONFIG == null) {
				CONFIG = createDefault();
			}
		} catch (IOException e) {
			EMPlus.LOGGER.error("Failed to read start UI config {}, using defaults", CONFIG_PATH, e);
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
			EMPlus.LOGGER.error("Failed to save start UI config {}", CONFIG_PATH, e);
		}
	}

	private static StartUiConfig createDefault() {
		return new StartUiConfig();
	}
}
