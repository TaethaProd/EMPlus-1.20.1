package taethaprod.emplus.shop;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;

import java.util.ArrayList;
import java.util.List;

public final class ShopClientNetworking {
	private static final int ITEM_ID_MAX = 256;
	private static final int SHORT_TEXT_MAX = 128;
	private static final int DESCRIPTION_MAX = 512;

	private ShopClientNetworking() {
	}

	public static void register() {
		ClientPlayNetworking.registerGlobalReceiver(ShopNetworking.SHOP_SYNC, (client, handler, buf, responseSender) -> {
			long balance = buf.readLong();
			List<ShopTab> tabs = readTabs(buf);
			List<ShopEntry> buy = readEntries(buf);
			List<ShopEntry> sell = readEntries(buf);
			List<ShopCaseEntry> cases = readCases(buf);
			client.execute(() -> ShopClientData.update(balance, tabs, buy, sell, cases));
		});
	}

	public static void requestSync() {
		ClientPlayNetworking.send(ShopNetworking.SHOP_REQUEST, PacketByteBufs.create());
	}

	public static void sendAction(byte action, String entryId) {
		var buf = PacketByteBufs.create();
		buf.writeByte(action);
		buf.writeString(entryId != null ? entryId : "", ITEM_ID_MAX);
		ClientPlayNetworking.send(ShopNetworking.SHOP_ACTION, buf);
	}

	private static List<ShopTab> readTabs(net.minecraft.network.PacketByteBuf buf) {
		int count = buf.readVarInt();
		List<ShopTab> tabs = new ArrayList<>(Math.max(0, count));
		for (int i = 0; i < count; i++) {
			String id = buf.readString(SHORT_TEXT_MAX);
			String label = buf.readString(ITEM_ID_MAX);
			tabs.add(new ShopTab(id, label));
		}
		return tabs;
	}

	private static List<ShopEntry> readEntries(net.minecraft.network.PacketByteBuf buf) {
		int count = buf.readVarInt();
		List<ShopEntry> entries = new ArrayList<>(Math.max(0, count));
		for (int i = 0; i < count; i++) {
			String id = buf.readString(ITEM_ID_MAX);
			String type = buf.readString(SHORT_TEXT_MAX);
			String tab = buf.readString(SHORT_TEXT_MAX);
			String item = buf.readString(ITEM_ID_MAX);
			String name = buf.readString(ITEM_ID_MAX);
			String icon = buf.readString(ITEM_ID_MAX);
			String description = buf.readString(DESCRIPTION_MAX);
			int cost = buf.readVarInt();
			ShopEntry entry = new ShopEntry();
			entry.id = id;
			entry.type = type;
			entry.tab = tab;
			entry.item = item;
			entry.name = name;
			entry.icon = icon;
			entry.description = description;
			entry.cost = cost;
			entries.add(entry);
		}
		return entries;
	}

	private static List<ShopCaseEntry> readCases(net.minecraft.network.PacketByteBuf buf) {
		int count = buf.readVarInt();
		List<ShopCaseEntry> entries = new ArrayList<>(Math.max(0, count));
		for (int i = 0; i < count; i++) {
			String id = buf.readString(SHORT_TEXT_MAX);
			int cost = buf.readVarInt();
			ShopCaseEntry entry = new ShopCaseEntry();
			entry.id = id;
			entry.cost = cost;
			entries.add(entry);
		}
		return entries;
	}
}
