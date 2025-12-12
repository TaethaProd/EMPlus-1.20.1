package taethaprod.emplus;

import net.fabricmc.api.ClientModInitializer;
import taethaprod.emplus.classes.ClassRestrictionClient;
import taethaprod.emplus.onboarding.OnboardingClientNetworking;
import taethaprod.emplus.startui.StartUiConfigManager;
import taethaprod.emplus.startui.StartUiScreen;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;

public class EMPlusClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ClassRestrictionClient.register();
		OnboardingClientNetworking.register();
		StartUiConfigManager.load();
		ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
			if (StartUiConfigManager.get().showStartScreen) {
				client.execute(() -> client.setScreen(new StartUiScreen()));
			}
		});
	}
}
