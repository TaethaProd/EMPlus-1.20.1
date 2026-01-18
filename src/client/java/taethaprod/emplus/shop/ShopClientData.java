package taethaprod.emplus.shop;

import java.util.ArrayList;
import java.util.List;

public final class ShopClientData {
	private static long balance = 0L;
	private static List<ShopTab> buyTabs = List.of();
	private static List<ShopEntry> buyEntries = List.of();
	private static List<ShopEntry> sellEntries = List.of();
	private static List<ShopCaseEntry> caseEntries = List.of();
	private static int revision = 0;

	private ShopClientData() {
	}

	public static void update(long balance, List<ShopTab> tabs, List<ShopEntry> buy, List<ShopEntry> sell, List<ShopCaseEntry> cases) {
		ShopClientData.balance = Math.max(0L, balance);
		ShopClientData.buyTabs = copyTabs(tabs);
		ShopClientData.buyEntries = copyEntries(buy);
		ShopClientData.sellEntries = copyEntries(sell);
		ShopClientData.caseEntries = copyCaseEntries(cases);
		revision++;
	}

	public static long getBalance() {
		return balance;
	}

	public static List<ShopEntry> getBuyEntries() {
		return buyEntries;
	}

	public static List<ShopTab> getBuyTabs() {
		return buyTabs;
	}

	public static List<ShopEntry> getSellEntries() {
		return sellEntries;
	}

	public static List<ShopCaseEntry> getCaseEntries() {
		return caseEntries;
	}

	public static int getRevision() {
		return revision;
	}

	private static List<ShopEntry> copyEntries(List<ShopEntry> input) {
		if (input == null || input.isEmpty()) {
			return List.of();
		}
		return new ArrayList<>(input);
	}

	private static List<ShopTab> copyTabs(List<ShopTab> input) {
		if (input == null || input.isEmpty()) {
			return List.of();
		}
		return new ArrayList<>(input);
	}

	private static List<ShopCaseEntry> copyCaseEntries(List<ShopCaseEntry> input) {
		if (input == null || input.isEmpty()) {
			return List.of();
		}
		return new ArrayList<>(input);
	}
}
