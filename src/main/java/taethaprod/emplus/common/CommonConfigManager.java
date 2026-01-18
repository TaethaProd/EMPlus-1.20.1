package taethaprod.emplus.common;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import taethaprod.emplus.EMPlus;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public final class CommonConfigManager {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path CONFIG_DIR = net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir()
			.resolve("emplus");
	private static final Path CONFIG_PATH = CONFIG_DIR.resolve("emplus_common.json");
	private static CommonConfig CONFIG;

	private CommonConfigManager() {
	}

	public static CommonConfig get() {
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
			CONFIG = GSON.fromJson(reader, CommonConfig.class);
			if (CONFIG == null) {
				CONFIG = createDefault();
			}
			ensureDefaults(CONFIG);
		} catch (IOException e) {
			EMPlus.LOGGER.error("Failed to read common config {}, using defaults", CONFIG_PATH, e);
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
			EMPlus.LOGGER.error("Failed to save common config {}", CONFIG_PATH, e);
		}
	}

	public static boolean isCommandEnabled(String commandKey) {
		CommonConfig config = get();
		if (config.commands == null) {
			return true;
		}
		String key = normalizeCommandKey(commandKey);
		Boolean enabled = config.commands.get(key);
		return enabled == null || enabled;
	}

	private static CommonConfig createDefault() {
		CommonConfig config = new CommonConfig();
		config.commands = new LinkedHashMap<>();
		config.commands.put("/tpspawn", true);
		config.reward_enable = true;
		config.reward_time = 3600;
		config.reward_command = "give {player} minecraft:diamond 1";
		config.reward_message = "You received a reward!";
		config.balance_steal = 10;
		return config;
	}

	private static void ensureDefaults(CommonConfig config) {
		if (config.commands == null) {
			config.commands = new LinkedHashMap<>();
		}
		Map<String, Boolean> commands = config.commands;
		String tpspawn = normalizeCommandKey("/tpspawn");
		if (!commands.containsKey(tpspawn)) {
			commands.put(tpspawn, true);
		}
		if (config.tpspawn == null) {
			config.tpspawn = new CommonConfig.TpspawnConfig();
		}
		if (config.tpspawn.factionA == null) {
			config.tpspawn.factionA = new CommonConfig.SpawnPoint(0.0, 80.0, 0.0, 0.0f, 0.0f, "minecraft:overworld");
		}
		if (config.tpspawn.factionB == null) {
			config.tpspawn.factionB = new CommonConfig.SpawnPoint(100.0, 80.0, 100.0, 0.0f, 0.0f, "minecraft:overworld");
		}
		if (config.reward_time <= 0) {
			config.reward_time = 3600;
		}
		if (config.reward_command == null) {
			config.reward_command = "give {player} minecraft:diamond 1";
		}
		if (config.reward_message == null) {
			config.reward_message = "You received a reward!";
		}
		if (config.balance_steal < 0) {
			config.balance_steal = 10;
		}
	}

	private static String normalizeCommandKey(String commandKey) {
		if (commandKey == null) {
			return "";
		}
		String trimmed = commandKey.trim();
		if (trimmed.startsWith("/")) {
			return trimmed;
		}
		return "/" + trimmed;
	}
}
