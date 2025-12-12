package taethaprod.emplus.onboarding;

import net.minecraft.server.network.ServerPlayerEntity;

public final class OnboardingManager {
	private static final String ONBOARDED_TAG = "emplus:onboarded";
	private static final String FACTION_PREFIX = "emplus:faction=";
	private static final String CLASS_PREFIX = "emplus:class=";

	private OnboardingManager() {
	}

	public static boolean isOnboarded(ServerPlayerEntity player) {
		return player.getCommandTags().contains(ONBOARDED_TAG);
	}

	public static void markOnboarded(ServerPlayerEntity player, String factionId, String classId) {
		clearFactionTags(player);
		clearClassTags(player);
		if (factionId != null && !factionId.isBlank()) {
			player.addCommandTag(FACTION_PREFIX + factionId);
		}
		if (classId != null && !classId.isBlank()) {
			player.addCommandTag(CLASS_PREFIX + classId);
		}
		player.addCommandTag(ONBOARDED_TAG);
	}

	private static void clearFactionTags(ServerPlayerEntity player) {
		player.getCommandTags().removeIf(tag -> tag.startsWith(FACTION_PREFIX));
	}

	private static void clearClassTags(ServerPlayerEntity player) {
		player.getCommandTags().removeIf(tag -> tag.startsWith(CLASS_PREFIX));
	}
}
