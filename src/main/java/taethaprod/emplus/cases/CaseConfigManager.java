package taethaprod.emplus.cases;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import taethaprod.emplus.EMPlus;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class CaseConfigManager {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path CONFIG_DIR = net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir()
			.resolve("emplus")
			.resolve("cases");
	private static final List<String> CASE_TYPES = List.of("common", "rare", "epic", "legendary");
	private static final Map<String, CaseConfig> CONFIGS = new LinkedHashMap<>();

	private CaseConfigManager() {
	}

	public static void load() {
		CONFIGS.clear();
		for (String type : CASE_TYPES) {
			Path path = CONFIG_DIR.resolve(type + ".json");
			CaseConfig config = loadConfig(path, () -> createDefault(type));
			CONFIGS.put(type, config);
		}
	}

	public static List<String> getCaseTypes() {
		return CASE_TYPES;
	}

	public static CaseConfig getCase(String caseId) {
		if (CONFIGS.isEmpty()) {
			load();
		}
		return CONFIGS.get(caseId);
	}

	public static int getCost(String caseId) {
		CaseConfig config = getCase(caseId);
		if (config == null) {
			return 0;
		}
		return Math.max(0, config.cost);
	}

	public static List<CaseEntry> getEntries(String caseId) {
		CaseConfig config = getCase(caseId);
		if (config == null || config.entries == null) {
			return List.of();
		}
		List<CaseEntry> cleaned = new ArrayList<>();
		for (CaseEntry entry : config.entries) {
			CaseEntry normalized = normalizeEntry(entry);
			if (normalized != null) {
				cleaned.add(normalized);
			}
		}
		return List.copyOf(cleaned);
	}

	public static CaseEntry pickRandomEntry(String caseId, net.minecraft.util.math.random.Random random) {
		return pickRandomEntry(getEntries(caseId), random);
	}

	public static CaseEntry pickRandomEntry(List<CaseEntry> entries, net.minecraft.util.math.random.Random random) {
		if (entries == null || entries.isEmpty()) {
			return null;
		}
		double totalWeight = 0.0;
		for (CaseEntry entry : entries) {
			totalWeight += Math.max(0.0, entry.weight);
		}
		if (totalWeight <= 0.0) {
			return entries.get(random.nextInt(entries.size()));
		}
		double roll = random.nextDouble() * totalWeight;
		for (CaseEntry entry : entries) {
			roll -= Math.max(0.0, entry.weight);
			if (roll <= 0.0) {
				return entry;
			}
		}
		return entries.get(entries.size() - 1);
	}

	private static CaseConfig loadConfig(Path path, java.util.function.Supplier<CaseConfig> defaults) {
		if (!Files.exists(path)) {
			CaseConfig config = defaults.get();
			saveConfig(path, config);
			return config;
		}
		try (Reader reader = Files.newBufferedReader(path)) {
			CaseConfig config = GSON.fromJson(reader, CaseConfig.class);
			if (config == null) {
				config = defaults.get();
			}
			if (config.entries == null) {
				config.entries = new ArrayList<>();
			}
			if (config.cost < 0) {
				config.cost = 0;
			}
			return config;
		} catch (IOException e) {
			EMPlus.LOGGER.error("Failed to read case config {}, using defaults", path, e);
			return defaults.get();
		}
	}

	private static void saveConfig(Path path, CaseConfig config) {
		try {
			Files.createDirectories(CONFIG_DIR);
			try (Writer writer = Files.newBufferedWriter(path)) {
				GSON.toJson(config, writer);
			}
		} catch (IOException e) {
			EMPlus.LOGGER.error("Failed to save case config {}", path, e);
		}
	}

	private static CaseConfig createDefault(String type) {
		CaseConfig config = new CaseConfig();
		config.cost = 0;
		CaseEntry sample = new CaseEntry("minecraft:iron_ingot", 1.0);
		sample.id = type + "_iron";
		sample.type = CaseEntry.TYPE_ITEM;
		sample.name = "Iron Ingot";
		sample.icon = "minecraft:iron_ingot";
		sample.rarity = "common";
		sample.count = 1;
		config.entries.add(sample);

		CaseEntry command = new CaseEntry();
		command.id = type + "_service";
		command.type = CaseEntry.TYPE_COMMAND;
		command.name = "Bonus Command";
		command.icon = "minecraft:command_block";
		command.description = "Runs a server command for the player.";
		command.rarity = "common";
		command.command = "give {player} minecraft:gold_ingot 1";
		command.weight = 1.0;
		command.count = 1;
		config.entries.add(command);

		return config;
	}

	private static CaseEntry normalizeEntry(CaseEntry entry) {
		if (entry == null) {
			return null;
		}
		String type = normalizeType(entry.type, entry.command);
		String id = normalizeId(entry.id, entry.item, entry.name);
		if (id == null) {
			return null;
		}
		if (CaseEntry.TYPE_ITEM.equalsIgnoreCase(type) && (entry.item == null || entry.item.isBlank())) {
			return null;
		}
		if (CaseEntry.TYPE_COMMAND.equalsIgnoreCase(type) && (entry.command == null || entry.command.isBlank())) {
			return null;
		}
		CaseEntry normalized = new CaseEntry();
		normalized.id = id;
		normalized.type = type;
		normalized.item = entry.item != null ? entry.item : "";
		normalized.name = entry.name != null ? entry.name : "";
		normalized.icon = entry.icon != null ? entry.icon : "";
		normalized.description = entry.description != null ? entry.description : "";
		normalized.rarity = normalizeRarity(entry.rarity);
		normalized.command = entry.command != null ? entry.command : "";
		normalized.weight = entry.weight <= 0.0 ? 1.0 : entry.weight;
		normalized.count = entry.count <= 0 ? 1 : entry.count;
		return normalized;
	}

	private static String normalizeType(String type, String command) {
		if (type == null || type.isBlank()) {
			return (command != null && !command.isBlank()) ? CaseEntry.TYPE_COMMAND : CaseEntry.TYPE_ITEM;
		}
		return CaseEntry.TYPE_COMMAND.equalsIgnoreCase(type) ? CaseEntry.TYPE_COMMAND : CaseEntry.TYPE_ITEM;
	}

	private static String normalizeId(String id, String item, String name) {
		if (id != null && !id.isBlank()) {
			return id;
		}
		if (item != null && !item.isBlank()) {
			return item;
		}
		if (name != null && !name.isBlank()) {
			return name;
		}
		return null;
	}

	private static String normalizeRarity(String rarity) {
		if (rarity == null || rarity.isBlank()) {
			return "common";
		}
		String value = rarity.trim().toLowerCase();
		return switch (value) {
			case "rare", "epic", "legendary" -> value;
			default -> "common";
		};
	}
}
