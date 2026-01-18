package taethaprod.emplus.startui;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import java.util.ArrayList;
import java.util.List;

public final class UpdateAnnouncementClientNetworking {
	private static final int TEXT_MAX = 512;

	private UpdateAnnouncementClientNetworking() {
	}

	public static void register() {
		ClientPlayNetworking.registerGlobalReceiver(UpdateAnnouncementNetworking.OPEN_UPDATE, (client, handler, buf, responseSender) -> {
			buf.readString(TEXT_MAX); // updateId, unused client-side
			String title = buf.readString(TEXT_MAX);
			int count = buf.readVarInt();
			List<String> lines = new ArrayList<>(Math.max(0, count));
			for (int i = 0; i < count; i++) {
				lines.add(buf.readString(TEXT_MAX));
			}
			client.execute(() -> client.setScreen(new UpdateAnnouncementScreen(title, lines)));
		});
	}
}
