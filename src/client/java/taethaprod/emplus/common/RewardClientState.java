package taethaprod.emplus.common;

public final class RewardClientState {
	private static int remainingSeconds = 0;
	private static int rewardIntervalSeconds = 0;
	private static boolean visible = false;
	private static boolean claimed = false;
	private static int tickCounter = 0;

	private RewardClientState() {
	}

	public static void update(int remaining, int intervalSeconds, boolean show, boolean isClaimed) {
		remainingSeconds = Math.max(0, remaining);
		rewardIntervalSeconds = Math.max(0, intervalSeconds);
		visible = show;
		claimed = isClaimed;
		tickCounter = 0;
	}

	public static int getRemainingSeconds() {
		return remainingSeconds;
	}

	public static boolean isVisible() {
		return visible;
	}

	public static boolean isClaimed() {
		return claimed;
	}

	public static void tick() {
		if (rewardIntervalSeconds <= 0 || claimed) {
			return;
		}
		tickCounter++;
		if (tickCounter < 20) {
			return;
		}
		tickCounter = 0;
		if (remainingSeconds > 0) {
			remainingSeconds--;
		}
	}
}
