package taethaprod.emplus.shop;

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

public final class ShopConfigManager {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path CONFIG_DIR = net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir()
			.resolve("emplus")
			.resolve("shop");
	private static final Path BUY_PATH = CONFIG_DIR.resolve("shop_buy.json");
	private static final Path SELL_PATH = CONFIG_DIR.resolve("shop_sell.json");
	private static ShopConfig BUY_CONFIG;
	private static ShopConfig SELL_CONFIG;
	private static final String DEFAULT_BUY_TAB_ID = "items";

	private ShopConfigManager() {
	}

	public static void load() {
		BUY_CONFIG = loadConfig(BUY_PATH, ShopConfigManager::createDefaultBuy, true);
		SELL_CONFIG = loadConfig(SELL_PATH, ShopConfigManager::createDefaultSell, false);
	}

	public static ShopConfig getBuy() {
		if (BUY_CONFIG == null) {
			load();
		}
		return BUY_CONFIG;
	}

	public static ShopConfig getSell() {
		if (SELL_CONFIG == null) {
			load();
		}
		return SELL_CONFIG;
	}

	public static List<ShopTab> getBuyTabs() {
		List<ShopTab> tabs = sanitizeTabs(getBuy().tabs);
		if (tabs.isEmpty()) {
			return defaultBuyTabs();
		}
		return tabs;
	}

	public static List<ShopEntry> getBuyEntries() {
		return normalizeBuyEntries(getBuy().items);
	}

	public static List<ShopEntry> getSellEntries() {
		return normalizeSellEntries(getSell().items);
	}

	public static ShopEntry findBuyEntry(String entryId) {
		return findEntry(getBuyEntries(), entryId);
	}

	public static ShopEntry findSellEntry(String entryId) {
		return findEntry(getSellEntries(), entryId);
	}

	private static ShopConfig loadConfig(Path path, java.util.function.Supplier<ShopConfig> defaults, boolean ensureTabs) {
		if (!Files.exists(path)) {
			ShopConfig config = defaults.get();
			saveConfig(path, config);
			return config;
		}
		try (Reader reader = Files.newBufferedReader(path)) {
			ShopConfig config = GSON.fromJson(reader, ShopConfig.class);
			if (config == null) {
				config = defaults.get();
			}
			if (config.tabs == null) {
				config.tabs = new ArrayList<>();
			}
			if (config.items == null) {
				config.items = new ArrayList<>();
			}
			if (ensureTabs && config.tabs.isEmpty()) {
				config.tabs = new ArrayList<>(defaultBuyTabs());
			}
			return config;
		} catch (IOException e) {
			EMPlus.LOGGER.error("Failed to read shop config {}, using defaults", path, e);
			return defaults.get();
		} catch (Exception e) {
			EMPlus.LOGGER.error("Invalid shop config {}, using defaults", path, e);
			return defaults.get();
		}
	}

	private static void saveConfig(Path path, ShopConfig config) {
		try {
			Files.createDirectories(CONFIG_DIR);
			try (Writer writer = Files.newBufferedWriter(path)) {
				GSON.toJson(config, writer);
			}
		} catch (IOException e) {
			EMPlus.LOGGER.error("Failed to save shop config {}", path, e);
		}
	}

	private static ShopConfig createDefaultBuy() {
		ShopConfig config = new ShopConfig();
		config.tabs = new ArrayList<>(defaultBuyTabs());

		ShopEntry diamond = new ShopEntry("minecraft:diamond", 100);
		diamond.id = "minecraft:diamond";
		diamond.type = ShopEntry.TYPE_ITEM;
		diamond.tab = DEFAULT_BUY_TAB_ID;
		config.items.add(diamond);

		ShopEntry goldenApple = new ShopEntry("minecraft:golden_apple", 35);
		goldenApple.id = "minecraft:golden_apple";
		goldenApple.type = ShopEntry.TYPE_ITEM;
		goldenApple.tab = DEFAULT_BUY_TAB_ID;
		config.items.add(goldenApple);

		ShopEntry heal = new ShopEntry();
		heal.id = "service_heal";
		heal.type = ShopEntry.TYPE_SERVICE;
		heal.tab = "services";
		heal.name = "Полное лечение";
		heal.icon = "minecraft:golden_apple";
		heal.description = "Полностью восстанавливает здоровье.";
		heal.cost = 50;
		heal.command = "effect give {player} minecraft:regeneration 5 2";
		config.items.add(heal);
		return config;
	}

	private static ShopConfig createDefaultSell() {
		ShopConfig config = new ShopConfig();
		ShopEntry cobble = new ShopEntry("minecraft:cobblestone", 1);
		cobble.id = "minecraft:cobblestone";
		cobble.type = ShopEntry.TYPE_ITEM;
		config.items.add(cobble);

		ShopEntry iron = new ShopEntry("minecraft:iron_ingot", 12);
		iron.id = "minecraft:iron_ingot";
		iron.type = ShopEntry.TYPE_ITEM;
		config.items.add(iron);
		return config;
	}

	private static List<ShopTab> sanitizeTabs(List<ShopTab> list) {
		if (list == null) {
			return List.of();
		}
		List<ShopTab> cleaned = new ArrayList<>();
		for (ShopTab tab : list) {
			if (tab == null) {
				continue;
			}
			String id = tab.id != null ? tab.id.trim() : "";
			if (id.isEmpty()) {
				continue;
			}
			String label = tab.label != null ? tab.label : "";
			ShopTab normalized = new ShopTab(id, label.isBlank() ? id : label);
			cleaned.add(normalized);
		}
		return List.copyOf(cleaned);
	}

	private static List<ShopTab> defaultBuyTabs() {
		return List.of(
				new ShopTab("items", "Предметы"),
				new ShopTab("services", "Услуги")
		);
	}

	private static List<ShopEntry> normalizeBuyEntries(List<ShopEntry> list) {
		if (list == null) {
			return List.of();
		}
		List<ShopTab> tabs = getBuyTabs();
		String defaultTab = getDefaultBuyTabId();
		java.util.Set<String> tabIds = new java.util.HashSet<>();
		for (ShopTab tab : tabs) {
			if (tab != null && tab.id != null && !tab.id.isBlank()) {
				tabIds.add(tab.id);
			}
		}
		List<ShopEntry> cleaned = new ArrayList<>();
		for (ShopEntry entry : list) {
			ShopEntry normalized = normalizeBuyEntry(entry, defaultTab, tabIds);
			if (normalized != null) {
				cleaned.add(normalized);
			}
		}
		return List.copyOf(cleaned);
	}

	private static ShopEntry normalizeBuyEntry(ShopEntry entry, String defaultTab, java.util.Set<String> tabIds) {
		if (entry == null) {
			return null;
		}
		String type = normalizeType(entry.type, entry.command);
		String id = normalizeId(entry.id, entry.item, entry.name);
		if (id == null) {
			return null;
		}
		if (ShopEntry.TYPE_ITEM.equalsIgnoreCase(type) && (entry.item == null || entry.item.isBlank())) {
			return null;
		}
		ShopEntry normalized = new ShopEntry();
		normalized.id = id;
		normalized.type = type;
		String tab = entry.tab != null && !entry.tab.isBlank() ? entry.tab : defaultTab;
		normalized.tab = tabIds.isEmpty() || tabIds.contains(tab) ? tab : defaultTab;
		normalized.item = entry.item != null ? entry.item : "";
		normalized.name = entry.name != null ? entry.name : "";
		normalized.icon = entry.icon != null ? entry.icon : "";
		normalized.description = entry.description != null ? entry.description : "";
		normalized.cost = entry.cost;
		normalized.command = entry.command != null ? entry.command : "";
		return normalized;
	}

	private static List<ShopEntry> normalizeSellEntries(List<ShopEntry> list) {
		if (list == null) {
			return List.of();
		}
		List<ShopEntry> cleaned = new ArrayList<>();
		for (ShopEntry entry : list) {
			ShopEntry normalized = normalizeSellEntry(entry);
			if (normalized != null) {
				cleaned.add(normalized);
			}
		}
		return List.copyOf(cleaned);
	}

	private static ShopEntry normalizeSellEntry(ShopEntry entry) {
		if (entry == null) {
			return null;
		}
		String id = normalizeId(entry.id, entry.item, entry.name);
		if (id == null) {
			return null;
		}
		if (entry.item == null || entry.item.isBlank()) {
			return null;
		}
		ShopEntry normalized = new ShopEntry();
		normalized.id = id;
		normalized.type = ShopEntry.TYPE_ITEM;
		normalized.item = entry.item != null ? entry.item : "";
		normalized.name = entry.name != null ? entry.name : "";
		normalized.icon = entry.icon != null ? entry.icon : "";
		normalized.description = entry.description != null ? entry.description : "";
		normalized.cost = entry.cost;
		return normalized;
	}

	private static String normalizeType(String type, String command) {
		if (type == null || type.isBlank()) {
			return (command != null && !command.isBlank()) ? ShopEntry.TYPE_SERVICE : ShopEntry.TYPE_ITEM;
		}
		return ShopEntry.TYPE_SERVICE.equalsIgnoreCase(type) ? ShopEntry.TYPE_SERVICE : ShopEntry.TYPE_ITEM;
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

	private static String getDefaultBuyTabId() {
		List<ShopTab> tabs = getBuyTabs();
		if (!tabs.isEmpty()) {
			return tabs.get(0).id;
		}
		return DEFAULT_BUY_TAB_ID;
	}

	private static ShopEntry findEntry(List<ShopEntry> entries, String entryId) {
		if (entries == null || entryId == null || entryId.isBlank()) {
			return null;
		}
		for (ShopEntry entry : entries) {
			if (entry != null && entryId.equals(entry.id)) {
				return entry;
			}
		}
		return null;
	}
}
