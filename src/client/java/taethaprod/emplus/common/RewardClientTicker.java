package taethaprod.emplus.common;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

public final class RewardClientTicker {
	private RewardClientTicker() {
	}

	public static void register() {
		ClientTickEvents.END_CLIENT_TICK.register(client -> RewardClientState.tick());
	}
}
