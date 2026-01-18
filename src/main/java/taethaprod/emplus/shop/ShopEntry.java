package taethaprod.emplus.shop;

public class ShopEntry {
	public static final String TYPE_ITEM = "item";
	public static final String TYPE_SERVICE = "service";

	public String id = "";
	public String type = TYPE_ITEM;
	public String tab = "";
	public String item = "";
	public String name = "";
	public String icon = "";
	public String description = "";
	public int cost = 0;
	public String command = "";

	public ShopEntry() {
	}

	public ShopEntry(String item, int cost) {
		this.item = item;
		this.cost = cost;
	}

	public boolean isService() {
		return TYPE_SERVICE.equalsIgnoreCase(type);
	}
}
