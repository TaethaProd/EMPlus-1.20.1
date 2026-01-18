package taethaprod.emplus.games.artifacts;

import java.util.ArrayList;
import java.util.List;

public class ArtifactsConfig {
	public double centerX = 0.0;
	public double centerZ = 0.0;
	public int areaRadius = 500;
	public int zones = 5;
	public int zoneRadius = 6;
	public int captureRadius = 6;
	public int artifactsPerChest = 1;
	public int artifactsRewardStepRadius = 500;
	public int captureSeconds = 25;
	public int eventDurationSeconds = 720;
	public int chestCleanupSeconds = 60;
	public boolean autoEnabled = false;
	public int autoCooldownSeconds = 3600;
	public int waveIntervalSeconds = 30;
	public int baseWaveMobs = 2;
	public int extraWaveMobs = 4;
	public int leaderExtraMobs = 2;
	public double leaderProgressMultiplier = 0.8;
	public double leaderDecayMultiplier = 1.25;
	public List<String> mobPool = new ArrayList<>(List.of(
			"minecraft:zombie",
			"minecraft:skeleton",
			"minecraft:spider"
	));
}
