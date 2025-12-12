package taethaprod.emplus.startui;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import taethaprod.emplus.EMPlus;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class FactionClassCommandsConfigManager {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path CONFIG_DIR = net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir()
			.resolve("emplus")
			.resolve("startui");
	private static final Path CONFIG_PATH = CONFIG_DIR.resolve("factionclasses.json");
	private static FactionClassCommandsConfig CONFIG;

	private FactionClassCommandsConfigManager() {
	}

	public static FactionClassCommandsConfig get() {
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
			CONFIG = GSON.fromJson(reader, FactionClassCommandsConfig.class);
			if (CONFIG == null) {
				CONFIG = createDefault();
			}
		} catch (IOException e) {
			EMPlus.LOGGER.error("Failed to read faction class config {}, using defaults", CONFIG_PATH, e);
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
			EMPlus.LOGGER.error("Failed to save faction class config {}", CONFIG_PATH, e);
		}
	}

	private static FactionClassCommandsConfig createDefault() {
		FactionClassCommandsConfig cfg = new FactionClassCommandsConfig();
		cfg.factionA = defaultClasses("A");
		cfg.factionB = defaultClasses("B");
		return cfg;
	}

	private static List<FactionClassCommandsConfig.ClassEntry> defaultClasses(String letter) {
		List<FactionClassCommandsConfig.ClassEntry> list = new ArrayList<>();
		for (int i = 1; i <= 6; i++) {
			FactionClassCommandsConfig.ClassEntry entry = new FactionClassCommandsConfig.ClassEntry();
			entry.id = "class_" + letter.toLowerCase() + i;
			entry.label = "Класс " + letter + i;
			entry.commands = new ArrayList<>();
			entry.commands.add("/tp {player} 0 0 0");
			entry.commands.add("/give {player} minecraft:apple");
			list.add(entry);
		}
		return list;
	}
}
