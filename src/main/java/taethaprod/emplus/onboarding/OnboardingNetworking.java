package taethaprod.emplus.onboarding;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import taethaprod.emplus.EMPlus;
import taethaprod.emplus.startui.StartUiConfigManager;
import taethaprod.emplus.startui.FactionClassCommandsConfig;
import taethaprod.emplus.startui.FactionClassCommandsConfigManager;
import taethaprod.emplus.startui.UpdateAnnouncementManager;

public final class OnboardingNetworking {
	public static final Identifier OPEN_INTRO = new Identifier(EMPlus.MOD_ID, "open_intro");
	public static final Identifier ONBOARDING_DONE = new Identifier(EMPlus.MOD_ID, "onboarding_done");

	private OnboardingNetworking() {
	}

	public static void registerServer() {
		ServerPlayNetworking.registerGlobalReceiver(ONBOARDING_DONE, (server, player, handler, buf, responseSender) -> {
			String faction = buf.readString(64);
			String classId = buf.isReadable() ? buf.readString(128) : "";
			String className = resolveClassName(classId);
			server.execute(() -> {
				runClassCommands(player, faction, classId);
				OnboardingManager.markOnboarded(player, faction, className);
				UpdateAnnouncementManager.trySendUpdate(player);
			});
		});

		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			ServerPlayerEntity player = handler.player;
			if (!OnboardingManager.isOnboarded(player) && StartUiConfigManager.get().showFactionScreen) {
				sendOpenIntro(player);
			}
		});
	}

	public static void sendOpenIntro(ServerPlayerEntity player) {
		PacketByteBuf buf = PacketByteBufs.create();
		ServerPlayNetworking.send(player, OPEN_INTRO, buf);
	}

	private static void runClassCommands(ServerPlayerEntity player, String factionId, String classId) {
		FactionClassCommandsConfig config = FactionClassCommandsConfigManager.get();
		java.util.List<FactionClassCommandsConfig.ClassEntry> entries;
		if ("emplus:faction_a".equals(factionId)) {
			entries = config.factionA;
		} else if ("emplus:faction_b".equals(factionId)) {
			entries = config.factionB;
		} else {
			entries = java.util.List.of();
		}
		FactionClassCommandsConfig.ClassEntry entry = entries.stream()
				.filter(e -> e != null && classId != null && classId.equals(e.id))
				.findFirst()
				.orElse(null);
		if (entry == null || entry.commands == null) {
			return;
		}
		var source = player.getServer().getCommandSource().withSilent();
		for (String raw : entry.commands) {
			if (raw == null || raw.isBlank()) continue;
			String cmd = raw.replace("{player}", player.getName().getString());
			player.getServer().getCommandManager().executeWithPrefix(source, cmd);
		}
	}

	private static String resolveClassName(String classId) {
		if (classId == null || classId.isBlank()) {
			return "";
		}
		String normalized = classId.trim().toLowerCase(java.util.Locale.ROOT);
		if (normalized.startsWith("class_")) {
			normalized = normalized.substring("class_".length());
		}
		return switch (normalized) {
			case "a1", "b1" -> "warrior";
			case "a2", "b2" -> "rogue";
			case "a3", "b3" -> "archer";
			case "a4" -> "wizard";
			case "a5" -> "deathknight";
			case "a6" -> "witcher";
			case "b4" -> "gunner";
			case "b5" -> "paladin";
			case "b6" -> "priest";
			default -> classId;
		};
	}
}
