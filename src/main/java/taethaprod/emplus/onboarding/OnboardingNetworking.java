package taethaprod.emplus.onboarding;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import taethaprod.emplus.EMPlus;
import taethaprod.emplus.startui.FactionCommandsConfig;
import taethaprod.emplus.startui.FactionCommandsConfigManager;
import taethaprod.emplus.startui.FactionClassCommandsConfig;
import taethaprod.emplus.startui.FactionClassCommandsConfigManager;

public final class OnboardingNetworking {
	public static final Identifier OPEN_INTRO = new Identifier(EMPlus.MOD_ID, "open_intro");
	public static final Identifier ONBOARDING_DONE = new Identifier(EMPlus.MOD_ID, "onboarding_done");

	private OnboardingNetworking() {
	}

	public static void registerServer() {
		ServerPlayNetworking.registerGlobalReceiver(ONBOARDING_DONE, (server, player, handler, buf, responseSender) -> {
			String faction = buf.readString(64);
			String classId = buf.isReadable() ? buf.readString(128) : "";
			server.execute(() -> {
				runFactionCommands(player, faction);
				runClassCommands(player, faction, classId);
				OnboardingManager.markOnboarded(player, faction, classId);
			});
		});

		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			ServerPlayerEntity player = handler.player;
			if (!OnboardingManager.isOnboarded(player)) {
				sendOpenIntro(player);
			}
		});
	}

	public static void sendOpenIntro(ServerPlayerEntity player) {
		PacketByteBuf buf = PacketByteBufs.create();
		ServerPlayNetworking.send(player, OPEN_INTRO, buf);
	}

	private static void runFactionCommands(ServerPlayerEntity player, String factionId) {
		FactionCommandsConfig config = FactionCommandsConfigManager.get();
		boolean isA = "emplus:faction_a".equals(factionId);
		boolean isB = "emplus:faction_b".equals(factionId);
		java.util.List<String> commands = isA ? config.factionA : isB ? config.factionB : java.util.List.of();
		var source = player.getServer().getCommandSource().withSilent();
		for (String raw : commands) {
			if (raw == null || raw.isBlank()) continue;
			String cmd = raw.replace("{player}", player.getName().getString());
			player.getServer().getCommandManager().executeWithPrefix(source, cmd);
		}
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
}
