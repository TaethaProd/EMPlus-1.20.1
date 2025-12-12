package taethaprod.emplus.onboarding;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public final class OnboardingClientNetworking {
	private OnboardingClientNetworking() {
	}

	public static void register() {
		ClientPlayNetworking.registerGlobalReceiver(OnboardingNetworking.OPEN_INTRO, (client, handler, buf, responseSender) -> {
			client.execute(() -> client.setScreen(new FactionSelectScreen()));
		});
	}
}
