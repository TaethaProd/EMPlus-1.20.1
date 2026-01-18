package taethaprod.emplus.common;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

public final class RewardHudOverlay {
	private static final int PADDING = 8;

	private RewardHudOverlay() {
	}

	public static void register() {
		HudRenderCallback.EVENT.register((context, tickDelta) -> {
			MinecraftClient client = MinecraftClient.getInstance();
			if (client == null || client.options.hudHidden) {
				return;
			}
			if (!RewardClientState.isVisible()) {
				return;
			}
			Text text;
			if (RewardClientState.isClaimed()) {
				text = Text.translatable("ui.emplus.reward.timer_claimed");
			} else {
				int remaining = RewardClientState.getRemainingSeconds();
				String formatted = formatTime(remaining);
				text = Text.translatable("ui.emplus.reward.timer_label", formatted);
			}
			int width = client.textRenderer.getWidth(text);
			int screenWidth = client.getWindow().getScaledWidth();
			int screenHeight = client.getWindow().getScaledHeight();
			int anchorX = (screenWidth + screenWidth / 2) / 2;
			int x = anchorX - width / 2;
			int y = screenHeight - 32;
			if (x < PADDING) {
				x = PADDING;
			} else if (x + width > screenWidth - PADDING) {
				x = screenWidth - width - PADDING;
			}
			if (y < PADDING) {
				y = PADDING;
			}
			context.drawTextWithShadow(client.textRenderer, text, x, y, 0xFFFFFF);
		});
	}

	private static String formatTime(int totalSeconds) {
		int seconds = Math.max(0, totalSeconds);
		int hours = seconds / 3600;
		int minutes = (seconds % 3600) / 60;
		int remaining = seconds % 60;
		if (hours > 0) {
			return String.format("%d:%02d:%02d", hours, minutes, remaining);
		}
		return String.format("%d:%02d", minutes, remaining);
	}
}
