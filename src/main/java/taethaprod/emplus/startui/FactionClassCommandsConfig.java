package taethaprod.emplus.startui;

import java.util.ArrayList;
import java.util.List;

public class FactionClassCommandsConfig {
	public List<ClassEntry> factionA = new ArrayList<>();
	public List<ClassEntry> factionB = new ArrayList<>();

	public static class ClassEntry {
		public String id;
		public String label;
		public List<String> commands = new ArrayList<>();
	}
}
