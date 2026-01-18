package taethaprod.emplus.cases;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import taethaprod.emplus.shop.ShopBalanceState;
import taethaprod.emplus.shop.ShopServerNetworking;

import java.util.List;

public final class CaseServerNetworking {
	private static final int CASE_ID_MAX = 32;
	private static final int ITEM_ID_MAX = 256;
	private static final int TEXT_MAX = 256;
	private static final int DESCRIPTION_MAX = 512;

	private CaseServerNetworking() {
	}

	public static void registerServer() {
		ServerPlayNetworking.registerGlobalReceiver(CaseNetworking.CASE_REQUEST_OPEN, (server, player, handler, buf, responseSender) -> {
			String caseId = buf.readString(CASE_ID_MAX);
			server.execute(() -> handleOpenRequest(player, caseId));
		});

		ServerPlayNetworking.registerGlobalReceiver(CaseNetworking.CASE_SPIN, (server, player, handler, buf, responseSender) -> {
			String caseId = buf.readString(CASE_ID_MAX);
			server.execute(() -> handleSpin(player, caseId));
		});
	}

	public static void sendOpen(ServerPlayerEntity player, String caseId) {
		int cost = CaseConfigManager.getCost(caseId);
		List<CaseEntry> entries = CaseConfigManager.getEntries(caseId);
		if (entries.isEmpty()) {
			player.sendMessage(Text.literal("Case is empty."), false);
			return;
		}
		PacketByteBuf buf = PacketByteBufs.create();
		buf.writeString(caseId, CASE_ID_MAX);
		buf.writeVarInt(Math.max(0, cost));
		buf.writeVarInt(entries.size());
		for (CaseEntry entry : entries) {
			buf.writeString(entry.id != null ? entry.id : "", ITEM_ID_MAX);
			buf.writeString(entry.type != null ? entry.type : "", TEXT_MAX);
			buf.writeString(entry.item != null ? entry.item : "", ITEM_ID_MAX);
			buf.writeString(entry.name != null ? entry.name : "", TEXT_MAX);
			buf.writeString(entry.icon != null ? entry.icon : "", ITEM_ID_MAX);
			buf.writeString(entry.description != null ? entry.description : "", DESCRIPTION_MAX);
			buf.writeString(entry.rarity != null ? entry.rarity : "", TEXT_MAX);
			buf.writeDouble(entry.weight);
			buf.writeVarInt(Math.max(1, entry.count));
		}
		ServerPlayNetworking.send(player, CaseNetworking.CASE_OPEN, buf);
	}

	private static void handleOpenRequest(ServerPlayerEntity player, String caseId) {
		if (!CaseConfigManager.getCaseTypes().contains(caseId)) {
			player.sendMessage(Text.literal("Unknown case: " + caseId), false);
			return;
		}
		sendOpen(player, caseId);
	}

	private static void handleSpin(ServerPlayerEntity player, String caseId) {
		if (!CaseConfigManager.getCaseTypes().contains(caseId)) {
			player.sendMessage(Text.literal("Unknown case: " + caseId), false);
			sendResult(player, caseId, -1);
			return;
		}
		List<CaseEntry> entries = CaseConfigManager.getEntries(caseId);
		if (entries.isEmpty()) {
			player.sendMessage(Text.literal("Case is empty."), false);
			sendResult(player, caseId, -1);
			return;
		}
		int cost = CaseConfigManager.getCost(caseId);
		if (cost > 0) {
			ShopBalanceState state = ShopBalanceState.get(player.getServer());
			if (!state.trySpend(player.getUuid(), cost)) {
				player.sendMessage(Text.translatable("message.emplus.shop.not_enough_money"), false);
				sendResult(player, caseId, -1);
				return;
			}
		}
		CaseEntry entry = CaseConfigManager.pickRandomEntry(entries, player.getRandom());
		if (entry == null) {
			player.sendMessage(Text.literal("Case is empty."), false);
			if (cost > 0) {
				ShopBalanceState.get(player.getServer()).addBalance(player.getUuid(), cost);
			}
			sendResult(player, caseId, -1);
			return;
		}
		if (entry.isCommand()) {
			runCommandReward(player, entry.command);
		} else {
			giveItemReward(player, entry.item, entry.count);
		}
		int index = entries.indexOf(entry);
		sendResult(player, caseId, index);
		if (cost > 0) {
			ShopServerNetworking.sendShopSync(player);
		}
	}

	private static void sendResult(ServerPlayerEntity player, String caseId, int entryIndex) {
		PacketByteBuf buf = PacketByteBufs.create();
		buf.writeString(caseId, CASE_ID_MAX);
		buf.writeVarInt(entryIndex);
		ServerPlayNetworking.send(player, CaseNetworking.CASE_RESULT, buf);
	}

	private static void giveItemReward(ServerPlayerEntity player, String itemId, int count) {
		Identifier id = Identifier.tryParse(itemId);
		if (id == null) {
			player.sendMessage(Text.literal("Invalid item id: " + itemId), false);
			return;
		}
		var item = Registries.ITEM.get(id);
		if (item == net.minecraft.item.Items.AIR) {
			player.sendMessage(Text.literal("Item not found: " + itemId), false);
			return;
		}
		int remaining = Math.max(1, count);
		int maxCount = Math.max(1, item.getMaxCount());
		while (remaining > 0) {
			int stackCount = Math.min(maxCount, remaining);
			ItemStack stack = new ItemStack(item, stackCount);
			player.getInventory().insertStack(stack);
			if (!stack.isEmpty()) {
				player.dropItem(stack, false);
			}
			remaining -= stackCount;
		}
	}

	private static void runCommandReward(ServerPlayerEntity player, String command) {
		if (command == null || command.isBlank()) {
			return;
		}
		String cmd = command.replace("{player}", player.getName().getString());
		var source = player.getServer().getCommandSource().withSilent();
		player.getServer().getCommandManager().executeWithPrefix(source, cmd);
	}
}
