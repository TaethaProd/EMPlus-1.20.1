package taethaprod.emplus.config;

import java.util.HashMap;
import java.util.Map;

public class BossScalingConfig {
	// Map key tier -> per-attribute multipliers.
	public Map<Integer, TierScaling> tierMultipliers = new HashMap<>();

	public static class TierScaling {
		public double maxHealth = 1.0;
		public double armor = 1.0;
		public double attackDamage = 1.0;
		public double movementSpeed = 1.0;
		public double knockbackResistance = 1.0;
		public double followRange = 1.0;
	}
}
