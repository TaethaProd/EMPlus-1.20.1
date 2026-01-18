package taethaprod.emplus.shop;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import taethaprod.emplus.cases.CaseConfigManager;

public final class ShopServerNetworking {
	private static final int ITEM_ID_MAX = 256;
	private static final int SHORT_TEXT_MAX = 128;
	private static final int DESCRIPTION_MAX = 512;

	private ShopServerNetworking() {
	}

	public static void registerServer() {
		ServerPlayNetworking.registerGlobalReceiver(ShopNetworking.SHOP_REQUEST, (server, player, handler, buf, responseSender) ->
				server.execute(() -> sendShopSync(player)));

		ServerPlayNetworking.registerGlobalReceiver(ShopNetworking.SHOP_ACTION, (server, player, handler, buf, responseSender) -> {
			byte action = buf.readByte();
			String entryId = buf.readString(ITEM_ID_MAX);
			server.execute(() -> handleAction(player, action, entryId));
		});

		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			sendShopSync(handler.player);
		});
	}

	public static void sendShopSync(ServerPlayerEntity player) {
		PacketByteBuf buf = PacketByteBufs.create();
		long balance = ShopBalanceState.get(player.getServer()).getBalance(player.getUuid());
		buf.writeLong(balance);
		writeTabs(buf, ShopConfigManager.getBuyTabs());
		writeEntries(buf, ShopConfigManager.getBuyEntries());
		writeEntries(buf, ShopConfigManager.getSellEntries());
		writeCases(buf);
		ServerPlayNetworking.send(player, ShopNetworking.SHOP_SYNC, buf);
	}

	private static void writeTabs(PacketByteBuf buf, java.util.List<ShopTab> tabs) {
		if (tabs == null) {
			buf.writeVarInt(0);
			return;
		}
		buf.writeVarInt(tabs.size());
		for (ShopTab tab : tabs) {
			String id = tab != null && tab.id != null ? tab.id : "";
			String label = tab != null && tab.label != null ? tab.label : "";
			buf.writeString(id, SHORT_TEXT_MAX);
			buf.writeString(label, ITEM_ID_MAX);
		}
	}

	private static void writeEntries(PacketByteBuf buf, java.util.List<ShopEntry> entries) {
		if (entries == null) {
			buf.writeVarInt(0);
			return;
		}
		buf.writeVarInt(entries.size());
		for (ShopEntry entry : entries) {
			String entryId = entry != null && entry.id != null ? entry.id : "";
			String type = entry != null && entry.type != null ? entry.type : "";
			String tab = entry != null && entry.tab != null ? entry.tab : "";
			String item = entry != null && entry.item != null ? entry.item : "";
			String name = entry != null && entry.name != null ? entry.name : "";
			String icon = entry != null && entry.icon != null ? entry.icon : "";
			String description = entry != null && entry.description != null ? entry.description : "";
			buf.writeString(entryId, ITEM_ID_MAX);
			buf.writeString(type, SHORT_TEXT_MAX);
			buf.writeString(tab, SHORT_TEXT_MAX);
			buf.writeString(item, ITEM_ID_MAX);
			buf.writeString(name, ITEM_ID_MAX);
			buf.writeString(icon, ITEM_ID_MAX);
			buf.writeString(description, DESCRIPTION_MAX);
			buf.writeVarInt(Math.max(0, entry != null ? entry.cost : 0));
		}
	}

	private static void writeCases(PacketByteBuf buf) {
		var types = CaseConfigManager.getCaseTypes();
		buf.writeVarInt(types.size());
		for (String type : types) {
			String id = type != null ? type : "";
			int cost = CaseConfigManager.getCost(id);
			buf.writeString(id, SHORT_TEXT_MAX);
			buf.writeVarInt(Math.max(0, cost));
		}
	}

	private static void handleAction(ServerPlayerEntity player, byte action, String entryId) {
		if (action == ShopNetworking.ACTION_BUY) {
			handleBuy(player, entryId);
		} else if (action == ShopNetworking.ACTION_SELL) {
			handleSell(player, entryId);
		}
	}

	private static void handleBuy(ServerPlayerEntity player, String entryId) {
		ShopEntry entry = ShopConfigManager.findBuyEntry(entryId);
		if (entry == null) {
			player.sendMessage(Text.translatable("message.emplus.shop.not_available_buy"), false);
			return;
		}
		if (entry.isService()) {
			handleServicePurchase(player, entry);
			return;
		}
		net.minecraft.item.Item item = resolveItem(entry.item, player);
		if (item == null) {
			return;
		}
		int cost = Math.max(0, entry.cost);
		ShopBalanceState state = ShopBalanceState.get(player.getServer());
		if (!state.trySpend(player.getUuid(), cost)) {
			player.sendMessage(Text.translatable("message.emplus.shop.not_enough_money"), false);
			return;
		}

		ItemStack stack = new ItemStack(item);
		player.getInventory().insertStack(stack);
		if (!stack.isEmpty()) {
			state.addBalance(player.getUuid(), cost);
			player.sendMessage(Text.translatable("message.emplus.shop.inventory_full"), false);
			sendShopSync(player);
			return;
		}

		player.sendMessage(Text.translatable("message.emplus.shop.purchased", item.getName(), cost), false);
		sendShopSync(player);
	}

	private static void handleSell(ServerPlayerEntity player, String entryId) {
		ShopEntry entry = ShopConfigManager.findSellEntry(entryId);
		if (entry == null) {
			player.sendMessage(Text.translatable("message.emplus.shop.not_available_sell"), false);
			return;
		}
		net.minecraft.item.Item item = resolveItem(entry.item, player);
		if (item == null) {
			return;
		}
		if (!removeOneItem(player, item)) {
			player.sendMessage(Text.translatable("message.emplus.shop.missing_item"), false);
			return;
		}
		int reward = Math.max(0, entry.cost);
		ShopBalanceState state = ShopBalanceState.get(player.getServer());
		state.addBalance(player.getUuid(), reward);
		player.sendMessage(Text.translatable("message.emplus.shop.sold", item.getName(), reward), false);
		sendShopSync(player);
	}

	private static void handleServicePurchase(ServerPlayerEntity player, ShopEntry entry) {
		String command = entry.command != null ? entry.command.trim() : "";
		if (command.isEmpty()) {
			player.sendMessage(Text.translatable("message.emplus.shop.service_missing_command"), false);
			return;
		}
		int cost = Math.max(0, entry.cost);
		ShopBalanceState state = ShopBalanceState.get(player.getServer());
		if (!state.trySpend(player.getUuid(), cost)) {
			player.sendMessage(Text.translatable("message.emplus.shop.not_enough_money"), false);
			return;
		}
		String cmd = command.replace("{player}", player.getName().getString());
		var source = player.getServer().getCommandSource().withSilent();
		int result = player.getServer().getCommandManager().executeWithPrefix(source, cmd);
		if (result <= 0) {
			state.addBalance(player.getUuid(), cost);
			player.sendMessage(Text.translatable("message.emplus.shop.service_failed"), false);
			sendShopSync(player);
			return;
		}
		Text name = resolveEntryName(entry, null);
		player.sendMessage(Text.translatable("message.emplus.shop.purchased", name, cost), false);
		sendShopSync(player);
	}

	private static net.minecraft.item.Item resolveItem(String itemId, ServerPlayerEntity player) {
		Identifier id = Identifier.tryParse(itemId);
		if (id == null) {
			player.sendMessage(Text.translatable("message.emplus.shop.invalid_item_id", itemId), false);
			return null;
		}
		var item = Registries.ITEM.get(id);
		if (item == net.minecraft.item.Items.AIR) {
			player.sendMessage(Text.translatable("message.emplus.shop.item_not_found"), false);
			return null;
		}
		return item;
	}

	private static Text resolveEntryName(ShopEntry entry, net.minecraft.item.Item item) {
		if (entry != null && entry.name != null && !entry.name.isBlank()) {
			return Text.literal(entry.name);
		}
		if (item != null) {
			return item.getName();
		}
		if (entry != null && entry.item != null && !entry.item.isBlank()) {
			return Text.literal(entry.item);
		}
		return Text.literal(entry != null && entry.id != null ? entry.id : "service");
	}

	private static boolean removeOneItem(ServerPlayerEntity player, net.minecraft.item.Item item) {
		var inventory = player.getInventory();
		for (int i = 0; i < inventory.size(); i++) {
			ItemStack stack = inventory.getStack(i);
			if (!stack.isEmpty() && stack.isOf(item)) {
				stack.decrement(1);
				if (stack.isEmpty()) {
					inventory.setStack(i, ItemStack.EMPTY);
				}
				return true;
			}
		}
		return false;
	}
}
