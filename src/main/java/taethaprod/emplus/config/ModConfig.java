package taethaprod.emplus.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ModConfig {
	public boolean originsSpecificLoot = false;
	public List<OriginLoot> originLoot = new ArrayList<>();

	public static class OriginLoot {
		public String origin = "";
		// Legacy flat loot list (fallback if tierLoot is empty).
		public List<LootEntry> items = new ArrayList<>();
		// Loot per key tier (1-10).
		public Map<Integer, List<LootEntry>> tierLoot = new HashMap<>();

		public static class LootEntry {
			public String item = "";
			public double chance = 1.0;

			public LootEntry() {
			}

			public LootEntry(String item, double chance) {
				this.item = item;
				this.chance = chance;
			}
		}
	}
}
