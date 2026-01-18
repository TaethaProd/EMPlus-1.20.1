package taethaprod.emplus.cases;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import java.util.ArrayList;
import java.util.List;

public final class CaseClientNetworking {
	private static final int CASE_ID_MAX = 32;
	private static final int ITEM_ID_MAX = 256;
	private static final int TEXT_MAX = 256;
	private static final int DESCRIPTION_MAX = 512;

	private CaseClientNetworking() {
	}

	public static void register() {
		ClientPlayNetworking.registerGlobalReceiver(CaseNetworking.CASE_OPEN, (client, handler, buf, responseSender) -> {
			String caseId = buf.readString(CASE_ID_MAX);
			int cost = Math.max(0, buf.readVarInt());
			int count = buf.readVarInt();
			List<CaseEntry> entries = new ArrayList<>(Math.max(0, count));
			for (int i = 0; i < count; i++) {
				CaseEntry entry = new CaseEntry();
				entry.id = buf.readString(ITEM_ID_MAX);
				entry.type = buf.readString(TEXT_MAX);
				entry.item = buf.readString(ITEM_ID_MAX);
				entry.name = buf.readString(TEXT_MAX);
				entry.icon = buf.readString(ITEM_ID_MAX);
				entry.description = buf.readString(DESCRIPTION_MAX);
				entry.rarity = buf.readString(TEXT_MAX);
				entry.weight = buf.readDouble();
				entry.count = buf.readVarInt();
				entries.add(entry);
			}
			client.execute(() -> client.setScreen(new CaseScreen(caseId, entries, cost)));
		});

		ClientPlayNetworking.registerGlobalReceiver(CaseNetworking.CASE_RESULT, (client, handler, buf, responseSender) -> {
			String caseId = buf.readString(CASE_ID_MAX);
			int entryIndex = buf.readVarInt();
			client.execute(() -> {
				if (client.currentScreen instanceof CaseScreen screen && screen.matchesCase(caseId)) {
					screen.onResult(entryIndex);
				} else {
					CaseClientState.setPendingResult(caseId, entryIndex);
				}
			});
		});
	}

	public static void sendSpinRequest(String caseId) {
		var buf = net.fabricmc.fabric.api.networking.v1.PacketByteBufs.create();
		buf.writeString(caseId != null ? caseId : "", CASE_ID_MAX);
		ClientPlayNetworking.send(CaseNetworking.CASE_SPIN, buf);
	}

	public static void requestOpen(String caseId) {
		var buf = net.fabricmc.fabric.api.networking.v1.PacketByteBufs.create();
		buf.writeString(caseId != null ? caseId : "", CASE_ID_MAX);
		ClientPlayNetworking.send(CaseNetworking.CASE_REQUEST_OPEN, buf);
	}
}
