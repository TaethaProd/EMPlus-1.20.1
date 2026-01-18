package taethaprod.emplus.common;

import java.util.LinkedHashMap;
import java.util.Map;

public class CommonConfig {
	public Map<String, Boolean> commands = new LinkedHashMap<>();
	public TpspawnConfig tpspawn = new TpspawnConfig();
	public boolean reward_enable = true;
	public int reward_time = 3600;
	public String reward_command = "give {player} minecraft:diamond 1";
	public String reward_message = "You received a reward!";
	public int balance_steal = 10;

	public static class TpspawnConfig {
		public SpawnPoint factionA = new SpawnPoint(0.0, 80.0, 0.0, 0.0f, 0.0f, "minecraft:overworld");
		public SpawnPoint factionB = new SpawnPoint(100.0, 80.0, 100.0, 0.0f, 0.0f, "minecraft:overworld");
	}

	public static class SpawnPoint {
		public double x;
		public double y;
		public double z;
		public float yaw;
		public float pitch;
		public String dimension;

		public SpawnPoint() {
		}

		public SpawnPoint(double x, double y, double z, float yaw, float pitch, String dimension) {
			this.x = x;
			this.y = y;
			this.z = z;
			this.yaw = yaw;
			this.pitch = pitch;
			this.dimension = dimension;
		}
	}
}
