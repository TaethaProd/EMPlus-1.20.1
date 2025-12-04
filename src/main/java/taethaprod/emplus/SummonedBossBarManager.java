package taethaprod.emplus;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.registry.RegistryKey;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class SummonedBossBarManager {
	private static final List<Entry> ENTRIES = new ArrayList<>();
	private static final double RANGE_SQUARED = 48.0 * 48.0;

	private SummonedBossBarManager() {
	}

	public static void track(LivingEntity mob, int level) {
		ServerBossBar bar = new ServerBossBar(
				Text.translatable("bossbar.emplus.mythical_mob", level),
				BossBar.Color.PURPLE,
				BossBar.Style.NOTCHED_10
		);
		bar.setDarkenSky(false);
		bar.setDragonMusic(false);
		bar.setThickenFog(false);

		ENTRIES.add(new Entry(mob.getUuid(), mob.getWorld().getRegistryKey(), bar));
	}

	public static void tick(MinecraftServer server) {
		Iterator<Entry> iterator = ENTRIES.iterator();
		while (iterator.hasNext()) {
			Entry entry = iterator.next();
			ServerWorld world = server.getWorld(entry.worldKey);
			if (world == null) {
				entry.bar.clearPlayers();
				iterator.remove();
				continue;
			}

			Entity entity = world.getEntity(entry.entityId);
			if (!(entity instanceof LivingEntity living) || !entity.isAlive()) {
				entry.bar.clearPlayers();
				iterator.remove();
				continue;
			}

			float percent = Math.max(0.0F, Math.min(1.0F, living.getHealth() / living.getMaxHealth()));
			entry.bar.setPercent(percent);

			for (ServerPlayerEntity player : world.getPlayers()) {
				boolean inRange = player.squaredDistanceTo(entity) <= RANGE_SQUARED;
				boolean alreadyTracking = entry.bar.getPlayers().contains(player);
				if (inRange && !alreadyTracking) {
					entry.bar.addPlayer(player);
				} else if (!inRange && alreadyTracking) {
					entry.bar.removePlayer(player);
				}
			}
		}
	}

private record Entry(java.util.UUID entityId, RegistryKey<World> worldKey, ServerBossBar bar) {
}
}
