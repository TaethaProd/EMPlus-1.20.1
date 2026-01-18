package taethaprod.emplus.games.artifacts;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;
import taethaprod.emplus.ModItems;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ArtifactGameManager {
	private static final int TICKS_PER_SECOND = 20;
	private static final int MAX_ZONE_ATTEMPTS = 256;
	private static final int SEARCH_ATTEMPTS_PER_TICK = 64;
	private static final int TOWER_HEIGHT = 5;
	private static final int CLEAR_HEIGHT = 8;
	private static final String FACTION_PREFIX = "emplus:faction=";
	private static final String ARTIFACT_MOB_TAG = "emplus:artifact_mob";
	private static ArtifactSession ACTIVE;
	private static PendingSpawn PENDING;
	private static int tickCounter = 0;
	private static int autoCooldownRemaining = 0;
	private static int chestCleanupRemaining = -1;
	private static ServerWorld chestCleanupWorld;
	private static final List<BlockPos> chestCleanupPositions = new ArrayList<>();

	private ArtifactGameManager() {
	}

	public static void registerServer() {
		ServerTickEvents.END_SERVER_TICK.register(ArtifactGameManager::tick);
	}

	public static int start(ServerCommandSource source) {
		if (ACTIVE != null || PENDING != null) {
			source.sendFeedback(() -> greenText(Text.translatable("emplus.artifacts.error.already_running")), false);
			return 0;
		}
		ArtifactsConfig config = ArtifactsConfigManager.get();
		ServerWorld world = source.getServer().getOverworld();
		if (world == null) {
			source.sendFeedback(() -> greenText(Text.translatable("emplus.artifacts.error.no_overworld")), false);
			return 0;
		}
		PENDING = new PendingSpawn(world, config);
		return 1;
	}

	public static int stop(ServerCommandSource source) {
		if (ACTIVE == null) {
			if (PENDING == null) {
				source.sendFeedback(() -> greenText(Text.translatable("emplus.artifacts.error.not_running")), false);
				return 0;
			}
			PENDING = null;
			source.sendFeedback(() -> greenText(Text.translatable("emplus.artifacts.stopped")), true);
			return 1;
		}
		ACTIVE.stop(source.getServer(), true);
		ACTIVE = null;
		resetAllLocators(source.getServer());
		if (ArtifactsConfigManager.get().autoEnabled) {
			autoCooldownRemaining = Math.max(0, ArtifactsConfigManager.get().autoCooldownSeconds);
		}
		source.sendFeedback(() -> greenText(Text.translatable("emplus.artifacts.stopped")), true);
		return 1;
	}

	private static void tick(MinecraftServer server) {
		tickCounter++;
		if (tickCounter < TICKS_PER_SECOND) {
			return;
		}
		tickCounter = 0;
		tickChestCleanup();
		if (PENDING != null) {
			PendingSpawn.Result result = PENDING.tick();
			if (result == PendingSpawn.Result.COMPLETE) {
				List<ArtifactZone> zones = createZonesFromPositions(PENDING.world, PENDING.config, PENDING.positions);
				ACTIVE = new ArtifactSession(PENDING.world, PENDING.config, zones);
				PENDING = null;
				broadcastStart(server, ACTIVE.config, zones.size());
				return;
			}
			if (result == PendingSpawn.Result.FAILED) {
				PENDING = null;
				server.getCommandSource().sendError(Text.translatable("emplus.artifacts.error.no_zones"));
				if (ArtifactsConfigManager.get().autoEnabled) {
					autoCooldownRemaining = Math.max(0, ArtifactsConfigManager.get().autoCooldownSeconds);
				}
				return;
			}
		}
		if (ACTIVE != null) {
			if (!ACTIVE.tick(server)) {
				ACTIVE = null;
				if (ArtifactsConfigManager.get().autoEnabled) {
					autoCooldownRemaining = Math.max(0, ArtifactsConfigManager.get().autoCooldownSeconds);
				}
			}
			return;
		}
		tickAuto(server);
	}

	private static void broadcastStart(MinecraftServer server, ArtifactsConfig config, int zoneCount) {
		broadcast(server, greenText(Text.translatable("emplus.artifacts.start.broadcast",
				zoneCount,
				MathHelper.floor(config.centerX),
				MathHelper.floor(config.centerZ),
				config.areaRadius)));
	}

	private static void broadcast(MinecraftServer server, Text message) {
		server.getPlayerManager().broadcast(message, false);
	}

	private static MutableText greenText(Text message) {
		return message.copy().formatted(Formatting.GREEN);
	}

	private static MutableText factionNameText(String teamId) {
		String normalized = normalizeTeamId(teamId);
		if ("faction_a".equals(normalized)) {
			return Text.translatable("emplus.faction.umbralis").formatted(Formatting.BLUE);
		}
		if ("faction_b".equals(normalized)) {
			return Text.translatable("emplus.faction.sanctarian").formatted(Formatting.RED);
		}
		if ("neutral".equals(normalized)) {
			return Text.translatable("emplus.faction.neutral").formatted(Formatting.GRAY);
		}
		return Text.translatable("emplus.faction.unknown", teamId == null ? "neutral" : teamId)
				.formatted(Formatting.GRAY);
	}

	public static int locate(ServerCommandSource source) {
		if (ACTIVE == null) {
			source.sendFeedback(() -> greenText(Text.translatable("emplus.artifacts.locate.none")), false);
			return 0;
		}
		BlockPos pos = ACTIVE.findNearestUncaptured(source.getPosition());
		if (pos == null) {
			source.sendFeedback(() -> greenText(Text.translatable("emplus.artifacts.locate.none")), false);
			return 0;
		}
		source.sendFeedback(() -> greenText(Text.translatable("emplus.artifacts.locate.coords",
				pos.getX(), pos.getY(), pos.getZ())), false);
		return 1;
	}

	public static int setAuto(ServerCommandSource source, boolean enabled) {
		ArtifactsConfig config = ArtifactsConfigManager.get();
		config.autoEnabled = enabled;
		ArtifactsConfigManager.save();
		String key = enabled ? "emplus.artifacts.auto.enabled" : "emplus.artifacts.auto.disabled";
		source.sendFeedback(() -> greenText(Text.translatable(key)), true);
		if (!enabled) {
			autoCooldownRemaining = 0;
			return 1;
		}
		if (ACTIVE == null && autoCooldownRemaining <= 0) {
			tryAutoStart(source.getServer());
		}
		return 1;
	}

	private static List<ArtifactZone> createZonesFromPositions(ServerWorld world, ArtifactsConfig config, List<BlockPos> positions) {
		List<ArtifactZone> zones = new ArrayList<>();
		for (BlockPos pos : positions) {
			spawnTower(world, pos);
			int waveInterval = Math.max(5, config.waveIntervalSeconds);
			int rewardCount = computeArtifactsForPosition(config, pos);
			zones.add(new ArtifactZone(pos, waveInterval, rewardCount));
		}
		return zones;
	}

	private static BlockPos findSurface(ServerWorld world, BlockPos pos) {
		if (world == null) {
			return null;
		}
		int chunkX = pos.getX() >> 4;
		int chunkZ = pos.getZ() >> 4;
		if (world.getChunkManager().getWorldChunk(chunkX, chunkZ) == null) {
			return null;
		}
		BlockPos top = world.getTopPosition(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, pos);
		if (top == null) {
			return null;
		}
		BlockPos surface = top.down();
		if (surface.getY() <= world.getBottomY()) {
			return null;
		}
		return surface;
	}

	private static boolean isValidSurface(ServerWorld world, BlockPos pos) {
		if (world == null || pos == null) {
			return false;
		}
		if (!world.getFluidState(pos).isEmpty()) {
			return false;
		}
		if (!world.getBlockState(pos).isSolidBlock(world, pos)) {
			return false;
		}
		return world.getBlockState(pos.up()).isAir();
	}

	private static boolean isClearVolume(ServerWorld world, BlockPos base, int radius, int height) {
		int minX = base.getX() - radius;
		int maxX = base.getX() + radius;
		int minZ = base.getZ() - radius;
		int maxZ = base.getZ() + radius;
		int minY = base.getY();
		int maxY = base.getY() + Math.max(1, height) - 1;
		for (int y = minY; y <= maxY; y++) {
			for (int x = minX; x <= maxX; x++) {
				for (int z = minZ; z <= maxZ; z++) {
					BlockPos check = new BlockPos(x, y, z);
					if (!world.getBlockState(check).isAir()) {
						return false;
					}
				}
			}
		}
		return true;
	}

	private static void spawnTower(ServerWorld world, BlockPos base) {
		for (int i = 0; i < TOWER_HEIGHT; i++) {
			world.setBlockState(base.up(i), Blocks.BEDROCK.getDefaultState());
		}
		BlockPos crystalPos = base.up(TOWER_HEIGHT);
		EndCrystalEntity crystal = new EndCrystalEntity(world, crystalPos.getX() + 0.5, crystalPos.getY(), crystalPos.getZ() + 0.5);
		world.spawnEntity(crystal);
	}

	private static boolean isFarEnough(BlockPos pos, Set<BlockPos> used, int minDistance) {
		int minSq = minDistance * minDistance;
		for (BlockPos other : used) {
			if (other.getSquaredDistance(pos) < minSq) {
				return false;
			}
		}
		return true;
	}

	private static int computeArtifactsForPosition(ArtifactsConfig config, BlockPos pos) {
		if (config == null || pos == null) {
			return 1;
		}
		double dx = pos.getX() - config.centerX;
		double dz = pos.getZ() - config.centerZ;
		double distance = Math.sqrt(dx * dx + dz * dz);
		int step = Math.max(1, config.artifactsRewardStepRadius);
		int bonus = (int) Math.floor(distance / step);
		int base = Math.max(1, config.artifactsPerChest);
		return Math.max(1, base + bonus);
	}

	private static void tickAuto(MinecraftServer server) {
		ArtifactsConfig config = ArtifactsConfigManager.get();
		if (!config.autoEnabled) {
			return;
		}
		if (PENDING != null) {
			return;
		}
		if (autoCooldownRemaining > 0) {
			autoCooldownRemaining--;
			return;
		}
		tryAutoStart(server);
	}

	private static void tryAutoStart(MinecraftServer server) {
		if (ACTIVE != null || PENDING != null) {
			return;
		}
		ServerCommandSource source = server.getCommandSource();
		int result = start(source);
		if (result <= 0) {
			autoCooldownRemaining = 10;
		}
	}

	private static final class PendingSpawn {
		private final ServerWorld world;
		private final ArtifactsConfig config;
		private final List<BlockPos> positions = new ArrayList<>();
		private final Set<BlockPos> used = new HashSet<>();
		private final Random random;
		private final int radius;
		private final int target;
		private final int minDistance;
		private final int maxAttempts;
		private int attempts = 0;

		private PendingSpawn(ServerWorld world, ArtifactsConfig config) {
			this.world = world;
			this.config = config;
			this.random = world.getRandom();
			this.radius = Math.max(1, config.areaRadius);
			this.target = Math.max(1, config.zones);
			this.minDistance = Math.max(6, config.zoneRadius * 4);
			int attemptFactor = Math.max(10, radius / Math.max(1, minDistance));
			this.maxAttempts = Math.max(MAX_ZONE_ATTEMPTS * target * attemptFactor, MAX_ZONE_ATTEMPTS);
		}

		private Result tick() {
			int attemptsThisTick = 0;
			while (positions.size() < target && attempts < maxAttempts && attemptsThisTick < SEARCH_ATTEMPTS_PER_TICK) {
				attempts++;
				attemptsThisTick++;
				int dx = random.nextInt(radius * 2 + 1) - radius;
				int dz = random.nextInt(radius * 2 + 1) - radius;
				int x = MathHelper.floor(config.centerX) + dx;
				int z = MathHelper.floor(config.centerZ) + dz;
				BlockPos surface = findSurface(world, new BlockPos(x, world.getTopY(), z));
				if (surface == null) {
					continue;
				}
				if (!isValidSurface(world, surface)) {
					continue;
				}
				BlockPos bedrockPos = surface.up();
				if (!isFarEnough(bedrockPos, used, minDistance)) {
					continue;
				}
				if (!isClearVolume(world, bedrockPos, 1, CLEAR_HEIGHT)) {
					continue;
				}
				used.add(bedrockPos);
				positions.add(bedrockPos);
			}
			if (positions.size() >= target) {
				return Result.COMPLETE;
			}
			if (attempts >= maxAttempts) {
				return Result.FAILED;
			}
			return Result.IN_PROGRESS;
		}

		private enum Result {
			IN_PROGRESS,
			COMPLETE,
			FAILED
		}
	}

	private static void scheduleChestCleanup(ServerWorld world, List<ArtifactZone> zones, int delaySeconds) {
		chestCleanupPositions.clear();
		chestCleanupWorld = world;
		for (ArtifactZone zone : zones) {
			chestCleanupPositions.add(zone.center);
		}
		if (delaySeconds <= 0) {
			chestCleanupRemaining = 0;
		} else {
			chestCleanupRemaining = delaySeconds;
		}
	}

	private static void tickChestCleanup() {
		if (chestCleanupRemaining < 0) {
			return;
		}
		if (chestCleanupRemaining > 0) {
			chestCleanupRemaining--;
			return;
		}
		if (chestCleanupWorld != null) {
			for (BlockPos pos : chestCleanupPositions) {
				if (chestCleanupWorld.getBlockState(pos).isOf(Blocks.CHEST)) {
					chestCleanupWorld.setBlockState(pos, Blocks.AIR.getDefaultState());
				}
			}
			removeArtifactMobs(chestCleanupWorld);
		}
		chestCleanupPositions.clear();
		chestCleanupWorld = null;
		chestCleanupRemaining = -1;
	}

	private static void removeArtifactMobs(ServerWorld world) {
		if (world == null) {
			return;
		}
		var border = world.getWorldBorder();
		double halfSize = border.getSize() / 2.0;
		double minX = border.getCenterX() - halfSize;
		double maxX = border.getCenterX() + halfSize;
		double minZ = border.getCenterZ() - halfSize;
		double maxZ = border.getCenterZ() + halfSize;
		Box box = new Box(minX, world.getBottomY(), minZ, maxX, world.getTopY(), maxZ);
		for (MobEntity mob : world.getEntitiesByClass(MobEntity.class, box,
				entity -> entity.getCommandTags().contains(ARTIFACT_MOB_TAG))) {
			mob.discard();
		}
	}

	private static void resetAllLocators(MinecraftServer server) {
		for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
			resetLocatorItems(player);
		}
	}

	private static void resetLocatorItems(ServerPlayerEntity player) {
		var inventory = player.getInventory();
		for (int i = 0; i < inventory.size(); i++) {
			ItemStack stack = inventory.getStack(i);
			if (stack.isEmpty() || stack.getItem() != ModItems.ARTIFACT_LOCATOR) {
				continue;
			}
			var nbt = stack.getNbt();
			if (nbt == null) {
				continue;
			}
			nbt.remove("LodestonePos");
			nbt.remove("LodestoneDimension");
			nbt.remove("LodestoneTracked");
			if (nbt.isEmpty()) {
				stack.setNbt(null);
			}
		}
	}

	private static final class ArtifactSession {
		private final ServerWorld world;
		private final ArtifactsConfig config;
		private final List<ArtifactZone> zones;
		private final List<EntityType<? extends MobEntity>> mobPool;
		private final Map<String, Integer> shards = new HashMap<>();
		private int elapsedSeconds = 0;

		private ArtifactSession(ServerWorld world, ArtifactsConfig config, List<ArtifactZone> zones) {
			this.world = world;
			this.config = config;
			this.zones = zones;
			this.mobPool = resolveMobPool(config);
		}

		private boolean tick(MinecraftServer server) {
			elapsedSeconds++;
			if (elapsedSeconds >= config.eventDurationSeconds) {
				endByTime(server);
				return false;
			}
			String leader = resolveLeaderTeam();
			int remainingZones = 0;
			for (ArtifactZone zone : zones) {
				if (zone.captured) {
					continue;
				}
				remainingZones++;
				updateZone(zone, leader, server);
			}
			if (remainingZones == 0) {
				endByAllCaptured(server);
				return false;
			}
			updateLocators(server);
			return true;
		}

		private void updateZone(ArtifactZone zone, String leader, MinecraftServer server) {
			List<ServerPlayerEntity> players = world.getPlayers(player -> isInside(zone, player.getPos()));
			zone.bar.clearPlayers();
			for (ServerPlayerEntity player : players) {
				zone.bar.addPlayer(player);
			}
			Map<String, Integer> teamCounts = countTeams(players);
			String winnerTeam = resolveZoneTeam(teamCounts);
			boolean contested = teamCounts.size() > 1;

			double baseRate = 1.0 / Math.max(1, config.captureSeconds);
			double progressMultiplier = 1.0;
			double decayMultiplier = 1.0;
			if (leader != null && leader.equals(winnerTeam)) {
				progressMultiplier = config.leaderProgressMultiplier;
				decayMultiplier = config.leaderDecayMultiplier;
			}

			if (players.isEmpty() || winnerTeam == null || contested) {
				zone.progress = Math.max(0.0, zone.progress - baseRate * decayMultiplier);
				if (zone.progress <= 0.0) {
					zone.progress = 0.0;
					zone.controllingTeam = null;
				}
			} else {
				zone.controllingTeam = winnerTeam;
				zone.progress = Math.min(1.0, zone.progress + baseRate * progressMultiplier * players.size());
			}

			zone.bar.setPercent((float) zone.progress);
			zone.bar.setName(Text.translatable("emplus.artifacts.bossbar", Math.round(zone.progress * 100.0)));

			int waveInterval = Math.max(5, config.waveIntervalSeconds);
			if (players.isEmpty()) {
				if (zone.waveTimer < waveInterval) {
					zone.waveTimer++;
				}
			} else {
				if (zone.waveTimer >= waveInterval) {
					zone.waveTimer = 0;
					boolean leaderPenalty = leader != null && leader.equals(zone.controllingTeam);
					spawnWave(zone, leaderPenalty);
				} else {
					zone.waveTimer++;
				}
			}

			if (zone.progress >= 1.0) {
				captureZone(zone, server);
			}
		}

		private void captureZone(ArtifactZone zone, MinecraftServer server) {
			zone.captured = true;
			zone.bar.clearPlayers();
			zone.bar.setVisible(false);
			zone.progress = 1.0;

			String team = normalizeTeamId(zone.controllingTeam);
			int next = shards.getOrDefault(team, 0) + 1;
			shards.put(team, next);

			spawnChest(zone.center, zone.rewardArtifacts);

			MutableText message = greenText(Text.translatable("emplus.artifacts.capture",
					factionNameText(team)));
			broadcast(server, message);

		}

		private void endByTime(MinecraftServer server) {
			broadcast(server, buildEndSummary(false));
			clearAllZoneBlocks();
			scheduleChestCleanup(world, zones, config.chestCleanupSeconds);
			resetAllLocators(server);
			endEvent();
		}

		private void endByAllCaptured(MinecraftServer server) {
			scheduleChestCleanup(world, zones, config.chestCleanupSeconds);
			resetAllLocators(server);
			endEvent();
		}

		private MutableText buildEndSummary(boolean allFound) {
			int umbralis = shards.getOrDefault("faction_a", 0);
			int sanctarian = shards.getOrDefault("faction_b", 0);
			String key = allFound ? "emplus.artifacts.summary.all_found" : "emplus.artifacts.summary.ended";
			return greenText(Text.translatable(key,
					factionNameText("faction_a"), umbralis,
					factionNameText("faction_b"), sanctarian));
		}

		private void endEvent() {
			for (ArtifactZone zone : zones) {
				zone.bar.clearPlayers();
				zone.bar.setVisible(false);
			}
		}

		private void stop(MinecraftServer server, boolean announce) {
			clearAllZoneBlocks();
			removeArtifactMobs(world);
			resetAllLocators(server);
			if (announce) {
				broadcast(server, greenText(Text.translatable("emplus.artifacts.stopped")));
			}
		}

		private void clearAllZoneBlocks() {
			for (ArtifactZone zone : zones) {
				zone.bar.clearPlayers();
				zone.bar.setVisible(false);
				clearZoneBlock(zone.center);
			}
		}

		private void spawnChest(BlockPos center, int rewardCount) {
			clearZoneBlock(center);
			world.setBlockState(center, Blocks.CHEST.getDefaultState());
			BlockEntity entity = world.getBlockEntity(center);
			if (entity instanceof ChestBlockEntity chest) {
				int remaining = Math.max(1, rewardCount);
				for (int slot = 0; slot < chest.size() && remaining > 0; slot++) {
					int stackSize = Math.min(remaining, ModItems.ARTIFACT.getMaxCount());
					ItemStack stack = new ItemStack(ModItems.ARTIFACT, stackSize);
					chest.setStack(slot, stack);
					remaining -= stackSize;
				}
				chest.markDirty();
			}
		}

		private void updateLocators(MinecraftServer server) {
			List<ArtifactZone> active = new ArrayList<>();
			for (ArtifactZone zone : zones) {
				if (!zone.captured) {
					active.add(zone);
				}
			}
			if (active.isEmpty()) {
				return;
			}
			for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
				ArtifactZone nearest = findNearestZone(player.getPos(), active);
				if (nearest != null) {
					updateLocatorItems(player, nearest.center);
				}
			}
		}

		private ArtifactZone findNearestZone(Vec3d pos, List<ArtifactZone> active) {
			ArtifactZone nearest = null;
			double best = Double.MAX_VALUE;
			for (ArtifactZone zone : active) {
				double dist = zone.center.toCenterPos().squaredDistanceTo(pos);
				if (dist < best) {
					best = dist;
					nearest = zone;
				}
			}
			return nearest;
		}

		private BlockPos findNearestUncaptured(Vec3d pos) {
			List<ArtifactZone> active = new ArrayList<>();
			for (ArtifactZone zone : zones) {
				if (!zone.captured) {
					active.add(zone);
				}
			}
			if (active.isEmpty()) {
				return null;
			}
			ArtifactZone nearest = findNearestZone(pos, active);
			return nearest == null ? null : nearest.center;
		}

		private void updateLocatorItems(ServerPlayerEntity player, BlockPos target) {
			var inventory = player.getInventory();
			for (int i = 0; i < inventory.size(); i++) {
				ItemStack stack = inventory.getStack(i);
				if (stack.isEmpty() || stack.getItem() != ModItems.ARTIFACT_LOCATOR) {
					continue;
				}
				var nbt = stack.getOrCreateNbt();
				nbt.put("LodestonePos", NbtHelper.fromBlockPos(target));
				nbt.putString("LodestoneDimension", world.getRegistryKey().getValue().toString());
				nbt.putBoolean("LodestoneTracked", false);
			}
		}

		private void spawnWave(ArtifactZone zone, boolean leaderPenalty) {
			int count = config.baseWaveMobs + (int) Math.round(zone.progress * config.extraWaveMobs);
			if (leaderPenalty) {
				count += config.leaderExtraMobs;
			}
			count = Math.max(0, count);
			for (int i = 0; i < count; i++) {
				EntityType<? extends MobEntity> type = pickMobType();
				MobEntity mob = type.create(world);
				if (mob == null) {
					continue;
				}
				mob.addCommandTag(ARTIFACT_MOB_TAG);
				BlockPos surface = findSurface(world, zone.center.add(randomOffset(), 0, randomOffset()));
				if (surface == null) {
					continue;
				}
				BlockPos spawn = surface.up();
				mob.refreshPositionAndAngles(spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5, world.getRandom().nextFloat() * 360.0F, 0.0F);
				world.spawnEntity(mob);
			}
		}

		private int randomOffset() {
			int distance = Math.max(4, config.zoneRadius + 2);
			return world.getRandom().nextInt(distance * 2 + 1) - distance;
		}

		private EntityType<? extends MobEntity> pickMobType() {
			if (mobPool.isEmpty()) {
				return ModItems.getRandomMobType(world.getRandom());
			}
			return mobPool.get(world.getRandom().nextInt(mobPool.size()));
		}

		private Map<String, Integer> countTeams(List<ServerPlayerEntity> players) {
			Map<String, Integer> counts = new HashMap<>();
			for (ServerPlayerEntity player : players) {
				String team = getTeamId(player);
				counts.put(team, counts.getOrDefault(team, 0) + 1);
			}
			return counts;
		}

		private String resolveZoneTeam(Map<String, Integer> counts) {
			String winner = null;
			int best = 0;
			boolean tie = false;
			for (Map.Entry<String, Integer> entry : counts.entrySet()) {
				int value = entry.getValue();
				if (value > best) {
					best = value;
					winner = entry.getKey();
					tie = false;
				} else if (value == best && value > 0) {
					tie = true;
				}
			}
			if (tie || best <= 0) {
				return null;
			}
			return winner;
		}

		private String resolveLeaderTeam() {
			String leader = null;
			int best = 0;
			boolean tie = false;
			for (Map.Entry<String, Integer> entry : shards.entrySet()) {
				int value = entry.getValue();
				if (value > best) {
					best = value;
					leader = entry.getKey();
					tie = false;
				} else if (value == best && value > 0) {
					tie = true;
				}
			}
			if (tie || best <= 0) {
				return null;
			}
			return leader;
		}

		private boolean isInside(ArtifactZone zone, Vec3d pos) {
			double distance = zone.center.toCenterPos().squaredDistanceTo(pos);
			double radius = Math.max(1, config.captureRadius);
			return distance <= radius * radius;
		}

		private void clearZoneBlock(BlockPos center) {
			if (center == null) {
				return;
			}
			for (int i = 0; i < TOWER_HEIGHT; i++) {
				BlockPos pos = center.up(i);
				if (world.getBlockState(pos).isOf(Blocks.BEDROCK) || world.getBlockState(pos).isOf(Blocks.CHEST)) {
					world.setBlockState(pos, Blocks.AIR.getDefaultState());
				}
			}
			BlockPos crystalPos = center.up(TOWER_HEIGHT);
			Box box = new Box(crystalPos).expand(1.0);
			for (EndCrystalEntity crystal : world.getEntitiesByClass(EndCrystalEntity.class, box, entity -> true)) {
				crystal.kill();
			}
		}
	}

	private static List<EntityType<? extends MobEntity>> resolveMobPool(ArtifactsConfig config) {
		List<EntityType<? extends MobEntity>> result = new ArrayList<>();
		if (config != null && config.mobPool != null) {
			for (String idString : config.mobPool) {
				net.minecraft.util.Identifier id = net.minecraft.util.Identifier.tryParse(idString);
				if (id == null || !Registries.ENTITY_TYPE.containsId(id)) {
					continue;
				}
				EntityType<?> type = Registries.ENTITY_TYPE.get(id);
				if (type.getSpawnGroup().isPeaceful()) {
					continue;
				}
				@SuppressWarnings("unchecked")
				EntityType<? extends MobEntity> mobType = (EntityType<? extends MobEntity>) type;
				result.add(mobType);
			}
		}
		if (result.isEmpty()) {
			result.add(EntityType.ZOMBIE);
			result.add(EntityType.SKELETON);
			result.add(EntityType.SPIDER);
		}
		return result;
	}

	private static String getTeamId(ServerPlayerEntity player) {
		for (String tag : player.getCommandTags()) {
			String normalized = normalizeTeamId(tag);
			if (!"neutral".equals(normalized)) {
				return normalized;
			}
		}
		return "neutral";
	}

	private static String normalizeTeamId(String tag) {
		if (tag == null || tag.isBlank()) {
			return "neutral";
		}
		if (tag.startsWith(FACTION_PREFIX)) {
			return normalizeTeamId(tag.substring(FACTION_PREFIX.length()));
		}
		if (tag.startsWith("emplus:faction_")) {
			return tag.substring("emplus:".length());
		}
		if (tag.startsWith("faction_")) {
			return tag;
		}
		return "neutral";
	}

	private static final class ArtifactZone {
		private final BlockPos center;
		private final ServerBossBar bar;
		private double progress = 0.0;
		private boolean captured = false;
		private String controllingTeam = null;
		private int waveTimer = 0;
		private final int rewardArtifacts;

		private ArtifactZone(BlockPos center, int initialWaveTimer, int rewardArtifacts) {
			this.center = center.toImmutable();
			this.bar = new ServerBossBar(Text.translatable("emplus.artifacts.bossbar", 0),
					BossBar.Color.BLUE, BossBar.Style.NOTCHED_10);
			this.bar.setDarkenSky(false);
			this.bar.setDragonMusic(false);
			this.bar.setThickenFog(false);
			this.waveTimer = Math.max(0, initialWaveTimer);
			this.rewardArtifacts = Math.max(1, rewardArtifacts);
		}
	}
}
