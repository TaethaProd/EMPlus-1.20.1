package taethaprod.emplus.cases;

public class CaseEntry {
	public static final String TYPE_ITEM = "item";
	public static final String TYPE_COMMAND = "command";

	public String id = "";
	public String type = TYPE_ITEM;
	public String item = "";
	public String name = "";
	public String icon = "";
	public String description = "";
	public String rarity = "common";
	public String command = "";
	public double weight = 1.0;
	public int count = 1;

	public CaseEntry() {
	}

	public CaseEntry(String item, double weight) {
		this.item = item;
		this.weight = weight;
	}

	public boolean isCommand() {
		return TYPE_COMMAND.equalsIgnoreCase(type);
	}
}
