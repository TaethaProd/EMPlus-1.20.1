package taethaprod.emplus.common;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class OnlineRewardManager {
	private static final int TICKS_PER_SECOND = 20;
	private static final int SYNC_INTERVAL_SECONDS = 600;
	private static final String TAG_SECONDS_PREFIX = "emplus:reward_seconds=";
	private static final String TAG_DAY_PREFIX = "emplus:reward_day=";
	private static final String TAG_CLAIMED_PREFIX = "emplus:reward_claimed=";
	private static final Map<UUID, Integer> ONLINE_SECONDS = new HashMap<>();
	private static final Map<UUID, Boolean> HIDDEN = new HashMap<>();
	private static final Map<UUID, String> DAY = new HashMap<>();
	private static final Map<UUID, Boolean> CLAIMED = new HashMap<>();
	private static int tickCounter = 0;
	private static int syncCounter = 0;

	private OnlineRewardManager() {
	}

	public static void registerServer() {
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			UUID id = handler.player.getUuid();
			loadState(handler.player);
			HIDDEN.put(id, false);
			sendSync(handler.player);
		});
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			UUID id = handler.player.getUuid();
			saveState(handler.player);
			ONLINE_SECONDS.remove(id);
			HIDDEN.remove(id);
			DAY.remove(id);
			CLAIMED.remove(id);
		});
		ServerTickEvents.END_SERVER_TICK.register(OnlineRewardManager::tick);
	}

	public static void setVisible(ServerPlayerEntity player, boolean visible) {
		if (player == null) {
			return;
		}
		HIDDEN.put(player.getUuid(), !visible);
		sendSync(player);
	}

	private static void tick(MinecraftServer server) {
		tickCounter++;
		if (tickCounter < TICKS_PER_SECOND) {
			return;
		}
		tickCounter = 0;
		CommonConfig config = CommonConfigManager.get();
		boolean active = isRewardActive(config);
		String currentDay = getCurrentDayKey();
		syncCounter++;
		boolean shouldSync = syncCounter >= SYNC_INTERVAL_SECONDS;
		if (shouldSync) {
			syncCounter = 0;
		}
		for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
			UUID id = player.getUuid();
			int elapsed = ONLINE_SECONDS.getOrDefault(id, 0);
			boolean claimed = CLAIMED.getOrDefault(id, false);
			String lastDay = DAY.get(id);
			boolean dayChanged = lastDay == null || !lastDay.equals(currentDay);
			if (dayChanged) {
				elapsed = 0;
				claimed = false;
				DAY.put(id, currentDay);
				ONLINE_SECONDS.put(id, 0);
				CLAIMED.put(id, false);
				saveState(player);
			}
			if (active) {
				if (!claimed) {
					elapsed++;
					if (elapsed >= config.reward_time) {
						elapsed = config.reward_time;
						claimed = true;
						ONLINE_SECONDS.put(id, elapsed);
						CLAIMED.put(id, true);
						runRewardCommand(player, config);
						saveState(player);
						sendSync(player);
					}
				}
			} else {
				elapsed = 0;
				claimed = false;
			}
			ONLINE_SECONDS.put(id, elapsed);
			CLAIMED.put(id, claimed);
			if (shouldSync) {
				if (!HIDDEN.getOrDefault(id, false)) {
					int remaining = active && !claimed ? Math.max(0, config.reward_time - elapsed) : 0;
					int interval = active ? config.reward_time : 0;
					sendSync(player, remaining, interval, active, claimed);
				}
			}
		}
	}

	private static void runRewardCommand(ServerPlayerEntity player, CommonConfig config) {
		if (config.reward_command == null || config.reward_command.isBlank()) {
			return;
		}
		String cmd = config.reward_command.replace("{player}", player.getName().getString());
		var source = player.getServer().getCommandSource().withSilent();
		player.getServer().getCommandManager().executeWithPrefix(source, cmd);
		sendRewardMessage(player, config.reward_message);
	}

	private static void sendRewardMessage(ServerPlayerEntity player, String message) {
		if (message == null || message.isBlank()) {
			return;
		}
		String text = message.replace("{player}", player.getName().getString());
		player.sendMessage(net.minecraft.text.Text.literal(text), false);
	}

	private static boolean isRewardActive(CommonConfig config) {
		return config != null
				&& config.reward_enable
				&& config.reward_time > 0
				&& config.reward_command != null
				&& !config.reward_command.isBlank();
	}

	private static void sendSync(ServerPlayerEntity player) {
		CommonConfig config = CommonConfigManager.get();
		boolean active = isRewardActive(config);
		int elapsed = ONLINE_SECONDS.getOrDefault(player.getUuid(), 0);
		boolean hidden = HIDDEN.getOrDefault(player.getUuid(), false);
		boolean claimed = CLAIMED.getOrDefault(player.getUuid(), false);
		if (!active || hidden) {
			sendSync(player, 0, 0, false, claimed);
			return;
		}
		int remaining = claimed ? 0 : Math.max(0, config.reward_time - elapsed);
		sendSync(player, remaining, config.reward_time, true, claimed);
	}

	private static void sendSync(ServerPlayerEntity player, int remainingSeconds, int rewardTimeSeconds, boolean visible, boolean claimed) {
		PacketByteBuf buf = PacketByteBufs.create();
		buf.writeVarInt(Math.max(0, remainingSeconds));
		buf.writeVarInt(Math.max(0, rewardTimeSeconds));
		buf.writeBoolean(visible);
		buf.writeBoolean(claimed);
		ServerPlayNetworking.send(player, RewardNetworking.REWARD_SYNC, buf);
	}

	private static void loadState(ServerPlayerEntity player) {
		String currentDay = getCurrentDayKey();
		int elapsed = 0;
		String storedDay = null;
		String claimedDay = null;
		for (String tag : player.getCommandTags()) {
			if (tag.startsWith(TAG_SECONDS_PREFIX)) {
				try {
					elapsed = Integer.parseInt(tag.substring(TAG_SECONDS_PREFIX.length()));
				} catch (NumberFormatException ignored) {
				}
			} else if (tag.startsWith(TAG_DAY_PREFIX)) {
				storedDay = tag.substring(TAG_DAY_PREFIX.length());
			} else if (tag.startsWith(TAG_CLAIMED_PREFIX)) {
				claimedDay = tag.substring(TAG_CLAIMED_PREFIX.length());
			}
		}
		boolean claimed = claimedDay != null && claimedDay.equals(currentDay);
		if (storedDay == null || !storedDay.equals(currentDay)) {
			elapsed = 0;
			claimed = false;
		}
		UUID id = player.getUuid();
		ONLINE_SECONDS.put(id, Math.max(0, elapsed));
		CLAIMED.put(id, claimed);
		DAY.put(id, currentDay);
	}

	private static void saveState(ServerPlayerEntity player) {
		if (player == null) {
			return;
		}
		String currentDay = DAY.getOrDefault(player.getUuid(), getCurrentDayKey());
		int elapsed = ONLINE_SECONDS.getOrDefault(player.getUuid(), 0);
		boolean claimed = CLAIMED.getOrDefault(player.getUuid(), false);
		player.getCommandTags().removeIf(tag ->
				tag.startsWith(TAG_SECONDS_PREFIX)
						|| tag.startsWith(TAG_DAY_PREFIX)
						|| tag.startsWith(TAG_CLAIMED_PREFIX));
		player.addCommandTag(TAG_DAY_PREFIX + currentDay);
		player.addCommandTag(TAG_SECONDS_PREFIX + Math.max(0, elapsed));
		if (claimed) {
			player.addCommandTag(TAG_CLAIMED_PREFIX + currentDay);
		}
	}

	private static String getCurrentDayKey() {
		return LocalDate.now().toString();
	}
}
