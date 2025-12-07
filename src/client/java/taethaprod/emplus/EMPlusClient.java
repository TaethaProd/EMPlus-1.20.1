package taethaprod.emplus;

import net.fabricmc.api.ClientModInitializer;
import taethaprod.emplus.classes.ClassRestrictionClient;

public class EMPlusClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ClassRestrictionClient.register();
	}
}
