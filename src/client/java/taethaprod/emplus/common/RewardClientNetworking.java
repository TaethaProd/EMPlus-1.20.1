package taethaprod.emplus.common;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public final class RewardClientNetworking {
	private RewardClientNetworking() {
	}

	public static void register() {
		ClientPlayNetworking.registerGlobalReceiver(RewardNetworking.REWARD_SYNC, (client, handler, buf, responseSender) -> {
			int remaining = buf.readVarInt();
			int intervalSeconds = buf.readVarInt();
			boolean visible = buf.readBoolean();
			boolean claimed = buf.readBoolean();
			client.execute(() -> RewardClientState.update(remaining, intervalSeconds, visible, claimed));
		});
	}
}
