package taethaprod.emplus.startui;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import taethaprod.emplus.onboarding.OnboardingManager;

import java.util.ArrayList;
import java.util.List;

public final class UpdateAnnouncementManager {
	private static final String SEEN_TAG_PREFIX = "update_seen=";
	private static final int TEXT_MAX = 512;

	private UpdateAnnouncementManager() {
	}

	public static void registerServer() {
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			trySendUpdate(handler.player);
		});
	}

	public static void trySendUpdate(ServerPlayerEntity player) {
		UpdateAnnouncementConfig config = UpdateAnnouncementConfigManager.get();
		if (config == null || !config.enabled) {
			return;
		}
		if (!OnboardingManager.isOnboarded(player)) {
			return;
		}
		String updateId = config.updateId != null ? config.updateId.trim() : "";
		if (updateId.isEmpty()) {
			return;
		}
		if (hasSeenUpdate(player, updateId)) {
			return;
		}
		List<String> lines = splitMessage(config.message);
		String title = config.title != null ? config.title : "";
		sendUpdate(player, updateId, title, lines);
		markSeen(player, updateId);
	}

	private static List<String> splitMessage(String message) {
		if (message == null || message.isBlank()) {
			return List.of();
		}
		String normalized = message.replace("\r\n", "\n");
		String[] parts = normalized.split("\n");
		List<String> lines = new ArrayList<>();
		for (String part : parts) {
			if (part != null) {
				lines.add(part);
			}
		}
		return lines;
	}

	private static void sendUpdate(ServerPlayerEntity player, String updateId, String title, List<String> lines) {
		PacketByteBuf buf = PacketByteBufs.create();
		buf.writeString(updateId, TEXT_MAX);
		buf.writeString(title != null ? title : "", TEXT_MAX);
		buf.writeVarInt(lines.size());
		for (String line : lines) {
			buf.writeString(line != null ? line : "", TEXT_MAX);
		}
		ServerPlayNetworking.send(player, UpdateAnnouncementNetworking.OPEN_UPDATE, buf);
	}

	private static boolean hasSeenUpdate(ServerPlayerEntity player, String updateId) {
		for (String tag : player.getCommandTags()) {
			if (tag.equals(SEEN_TAG_PREFIX + updateId)) {
				return true;
			}
		}
		return false;
	}

	private static void markSeen(ServerPlayerEntity player, String updateId) {
		player.getCommandTags().removeIf(tag -> tag.startsWith(SEEN_TAG_PREFIX));
		player.addCommandTag(SEEN_TAG_PREFIX + updateId);
	}
}
