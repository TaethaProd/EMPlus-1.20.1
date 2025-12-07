package taethaprod.emplus;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import taethaprod.emplus.config.ConfigManager;
import taethaprod.emplus.config.ModConfig;
import taethaprod.emplus.config.SpawnConfigManager;
import taethaprod.emplus.config.BossScalingConfigManager;
import taethaprod.emplus.classes.ClassesConfigManager;
import taethaprod.emplus.classes.ClassesRestrictionsManager;
import taethaprod.emplus.classes.ClassRestrictionHandler;
import taethaprod.emplus.command.ClassesCommand;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class EMPlus implements ModInitializer {
	public static final String MOD_ID = "emplus";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ConfigManager.load();
		SpawnConfigManager.load();
		BossScalingConfigManager.load();
		ClassesConfigManager.load();
		ClassesRestrictionsManager.load();
		ModItems.init();

		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			ClassesCommand.register(dispatcher);
		});

		ClassRestrictionHandler.registerServer();

		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> dropNextKey(entity));
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			ServerTaskScheduler.tick(server);
			SummonedBossBarManager.tick(server);
		});

		LOGGER.info("Hello Fabric world!");
	}

	private void dropNextKey(LivingEntity entity) {
		if (entity.getWorld().isClient()) {
			return;
		}

		Integer nextKeyLevel = null;
		Integer summonLevel = null;
		String originId = null;
		for (String tag : entity.getCommandTags()) {
			if (tag.startsWith("emplus:mythical_key_next_level=")) {
				try {
					nextKeyLevel = Integer.parseInt(tag.substring(tag.lastIndexOf('=') + 1));
				} catch (NumberFormatException ignored) {
				}
			} else if (tag.startsWith("emplus:mythical_key_level=")) {
				try {
					summonLevel = Integer.parseInt(tag.substring(tag.lastIndexOf('=') + 1));
				} catch (NumberFormatException ignored) {
				}
			} else if (tag.startsWith("emplus:origin=")) {
				originId = tag.substring(tag.indexOf('=') + 1);
			}
		}

		int maxTier = taethaprod.emplus.config.BossScalingConfigManager.getMaxTier();
		if (nextKeyLevel != null && nextKeyLevel >= 1 && nextKeyLevel <= maxTier) {
			var mob = ModItems.getRandomMobType(entity.getWorld().getRandom());
			ItemStack stack = ModItems.createKeyStack(mob, nextKeyLevel);
			if (!stack.isEmpty()) {
				entity.dropStack(stack);
			}
		}

		int lootTier = resolveTierForLoot(summonLevel, nextKeyLevel, maxTier);
		dropOriginLoot(entity, originId, lootTier);
	}

	private int resolveTierForLoot(Integer summonLevel, Integer nextKeyLevel, int maxTier) {
		if (summonLevel != null && summonLevel >= 1 && summonLevel <= maxTier) {
			return summonLevel;
		}
		if (nextKeyLevel != null && nextKeyLevel >= 1 && nextKeyLevel <= maxTier) {
			// next key level is always current + 1 during summon time.
			return Math.max(1, Math.min(maxTier, nextKeyLevel - 1));
		}
		return 1;
	}

	private void dropOriginLoot(LivingEntity entity, String originId, int tier) {
		ModConfig config = ConfigManager.get();
		if (!config.originsSpecificLoot) {
			return;
		}
		if (originId == null || originId.isEmpty()) {
			return;
		}
		for (ModConfig.OriginLoot loot : config.originLoot) {
			if (loot.origin == null || loot.items == null) {
				continue;
			}
			if (!originId.equals(loot.origin)) {
				continue;
			}
			ItemStack lootStack = pickWeightedLoot(loot, tier, entity);
			if (!lootStack.isEmpty()) {
				entity.dropStack(lootStack);
			}
			break;
		}
	}

	private ItemStack pickWeightedLoot(ModConfig.OriginLoot loot, int tier, LivingEntity entity) {
		List<ModConfig.OriginLoot.LootEntry> entries = null;
		if (loot.tierLoot != null) {
			entries = loot.tierLoot.get(tier);
		}
		if (entries == null || entries.isEmpty()) {
			entries = loot.items;
		}
		if (entries == null || entries.isEmpty()) {
			return ItemStack.EMPTY;
		}

		List<LootCandidate> candidates = new ArrayList<>();
		double totalWeight = 0.0;
		for (ModConfig.OriginLoot.LootEntry entry : entries) {
			if (entry == null || entry.item == null) {
				continue;
			}
			Identifier id = Identifier.tryParse(entry.item);
			if (id == null) {
				continue;
			}
			var item = net.minecraft.registry.Registries.ITEM.get(id);
			if (item == net.minecraft.item.Items.AIR) {
				continue;
			}
			double weight = Math.max(0.0, entry.chance);
			if (weight <= 0.0) {
				continue;
			}
			totalWeight += weight;
			candidates.add(new LootCandidate(item, weight));
		}

		if (candidates.isEmpty() || totalWeight <= 0.0) {
			return ItemStack.EMPTY;
		}

		double roll = entity.getWorld().getRandom().nextDouble() * totalWeight;
		for (LootCandidate candidate : candidates) {
			roll -= candidate.weight;
			if (roll <= 0.0) {
				return new ItemStack(candidate.item());
			}
		}
		// Fallback to last entry in case of floating point drift.
		return new ItemStack(candidates.get(candidates.size() - 1).item());
	}

	private record LootCandidate(net.minecraft.item.Item item, double weight) {
	}
}
