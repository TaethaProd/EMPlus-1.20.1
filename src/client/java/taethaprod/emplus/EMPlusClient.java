package taethaprod.emplus;

import net.fabricmc.api.ClientModInitializer;
import taethaprod.emplus.classes.ClassRestrictionClient;
import taethaprod.emplus.common.RewardClientNetworking;
import taethaprod.emplus.common.RewardClientTicker;
import taethaprod.emplus.common.RewardHudOverlay;
import taethaprod.emplus.games.artifacts.ArtifactClient;
import taethaprod.emplus.onboarding.OnboardingClientNetworking;
import taethaprod.emplus.startui.StartUiConfigManager;
import taethaprod.emplus.startui.StartUiScreen;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import taethaprod.emplus.cases.CaseClientNetworking;
import taethaprod.emplus.shop.ShopClientNetworking;
import taethaprod.emplus.shop.ShopKeybinds;
import taethaprod.emplus.startui.UpdateAnnouncementClientNetworking;

public class EMPlusClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ClassRestrictionClient.register();
		OnboardingClientNetworking.register();
		UpdateAnnouncementClientNetworking.register();
		CaseClientNetworking.register();
		ShopClientNetworking.register();
		ShopKeybinds.register();
		RewardClientNetworking.register();
		RewardHudOverlay.register();
		RewardClientTicker.register();
		ArtifactClient.register();
		StartUiConfigManager.load();
		ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
			if (StartUiConfigManager.get().showStartScreen) {
				client.execute(() -> client.setScreen(new StartUiScreen()));
			}
		});
	}
}
