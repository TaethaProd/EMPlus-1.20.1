package taethaprod.emplus.config;

import java.util.ArrayList;
import java.util.List;

public class ModConfig {
	public boolean originsSpecificLoot = false;
	public List<OriginLoot> originLoot = new ArrayList<>();

	public static class OriginLoot {
		public String origin = "";
		public List<String> items = new ArrayList<>();
	}
}
