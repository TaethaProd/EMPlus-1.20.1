package taethaprod.emplus.onboarding;

import net.minecraft.server.network.ServerPlayerEntity;

public final class OnboardingManager {
	private static final String ONBOARDED_TAG = "emplus:onboarded";
	private static final String FACTION_PREFIX = "emplus:faction=";

	private OnboardingManager() {
	}

	public static boolean isOnboarded(ServerPlayerEntity player) {
		return player.getCommandTags().contains(ONBOARDED_TAG);
	}

	public static void markOnboarded(ServerPlayerEntity player, String factionId) {
		clearFactionTags(player);
		if (factionId != null && !factionId.isBlank()) {
			player.addCommandTag(FACTION_PREFIX + factionId);
		}
		player.addCommandTag(ONBOARDED_TAG);
	}

	private static void clearFactionTags(ServerPlayerEntity player) {
		player.getCommandTags().removeIf(tag -> tag.startsWith(FACTION_PREFIX));
	}
}
