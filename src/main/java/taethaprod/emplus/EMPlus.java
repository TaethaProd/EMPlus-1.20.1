package taethaprod.emplus;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import taethaprod.emplus.config.ConfigManager;
import taethaprod.emplus.config.ModConfig;
import taethaprod.emplus.config.SpawnConfigManager;
import taethaprod.emplus.config.BossScalingConfigManager;
import taethaprod.emplus.classes.ClassesConfigManager;
import taethaprod.emplus.classes.ClassesRestrictionsManager;
import taethaprod.emplus.classes.ClassRestrictionHandler;
import taethaprod.emplus.command.CaseCommand;
import taethaprod.emplus.command.ClassesCommand;
import taethaprod.emplus.command.HomeCommand;
import taethaprod.emplus.command.RewardCommand;
import taethaprod.emplus.command.TpspawnCommand;
import taethaprod.emplus.common.CommonConfig;
import taethaprod.emplus.common.CommonConfigManager;
import taethaprod.emplus.common.OnlineRewardManager;
import taethaprod.emplus.cases.CaseConfigManager;
import taethaprod.emplus.cases.CaseServerNetworking;
import taethaprod.emplus.games.artifacts.ArtifactCommand;
import taethaprod.emplus.games.artifacts.ArtifactGameManager;
import taethaprod.emplus.games.artifacts.ArtifactsConfigManager;
import taethaprod.emplus.onboarding.OnboardingNetworking;
import taethaprod.emplus.onboarding.OnboardingCommand;
import taethaprod.emplus.startui.StartUiConfigManager;
import taethaprod.emplus.startui.FactionClassCommandsConfigManager;
import taethaprod.emplus.startui.UpdateAnnouncementConfigManager;
import taethaprod.emplus.startui.UpdateAnnouncementManager;
import taethaprod.emplus.item.MythicalKeyItem;
import taethaprod.emplus.shop.MoneyCommand;
import taethaprod.emplus.shop.ShopBalanceState;
import taethaprod.emplus.shop.ShopConfigManager;
import taethaprod.emplus.shop.ShopServerNetworking;

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
		CommonConfigManager.load();
		CaseConfigManager.load();
		ArtifactsConfigManager.load();
		StartUiConfigManager.load();
		FactionClassCommandsConfigManager.load();
		UpdateAnnouncementConfigManager.load();
		ShopConfigManager.load();
		ModItems.init();

		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			ClassesCommand.register(dispatcher);
			OnboardingCommand.register(dispatcher);
			MoneyCommand.register(dispatcher);
			TpspawnCommand.register(dispatcher);
			CaseCommand.register(dispatcher);
			RewardCommand.register(dispatcher);
			HomeCommand.register(dispatcher);
			ArtifactCommand.register(dispatcher);
		});

		ClassRestrictionHandler.registerServer();
		OnboardingNetworking.registerServer();
		UpdateAnnouncementManager.registerServer();
		ShopServerNetworking.registerServer();
		CaseServerNetworking.registerServer();
		OnlineRewardManager.registerServer();
		ArtifactGameManager.registerServer();

		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> dropNextKey(entity, source));
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			ServerTaskScheduler.tick(server);
			SummonedBossBarManager.tick(server);
		});

		LOGGER.info("Hello Fabric world!");
	}

	private void dropNextKey(LivingEntity entity, DamageSource source) {
		if (entity.getWorld().isClient()) {
			return;
		}
		despawnBossIfOwnerKilled(entity, source);
		handlePvpBalanceSteal(entity, source);

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

	private void despawnBossIfOwnerKilled(LivingEntity victim, DamageSource source) {
		if (!(victim instanceof net.minecraft.server.network.ServerPlayerEntity player)) {
			return;
		}
		if (source == null) {
			return;
		}
		net.minecraft.entity.Entity attacker = source.getAttacker();
		if (!(attacker instanceof LivingEntity mob)) {
			return;
		}
		java.util.UUID ownerId = player.getUuid();
		boolean owned = false;
		for (String tag : mob.getCommandTags()) {
			if (tag.startsWith(MythicalKeyItem.OWNER_TAG_PREFIX)) {
				String raw = tag.substring(MythicalKeyItem.OWNER_TAG_PREFIX.length());
				try {
					java.util.UUID parsed = java.util.UUID.fromString(raw);
					if (ownerId.equals(parsed)) {
						owned = true;
						break;
					}
				} catch (IllegalArgumentException ignored) {
				}
			}
		}
		if (owned) {
			mob.discard();
		}
	}

	private void handlePvpBalanceSteal(LivingEntity victim, DamageSource source) {
		if (!(victim instanceof net.minecraft.server.network.ServerPlayerEntity target)) {
			return;
		}
		if (source == null) {
			return;
		}
		net.minecraft.entity.Entity attacker = source.getAttacker();
		if (!(attacker instanceof net.minecraft.server.network.ServerPlayerEntity killer)) {
			return;
		}
		if (killer.getUuid().equals(target.getUuid())) {
			return;
		}
		String killerFaction = getNormalizedFaction(killer);
		String targetFaction = getNormalizedFaction(target);
		if (killerFaction.isEmpty() || targetFaction.isEmpty()) {
			return;
		}
		if (killerFaction.equals(targetFaction)) {
			return;
		}
		CommonConfig config = CommonConfigManager.get();
		int percent = config.balance_steal;
		if (percent <= 0) {
			return;
		}
		ShopBalanceState state = ShopBalanceState.get(target.getServer());
		long targetBalance = state.getBalance(target.getUuid());
		if (targetBalance <= 0L) {
			return;
		}
		long transfer = (targetBalance * percent) / 100L;
		if (transfer <= 0L) {
			return;
		}
		if (transfer > targetBalance) {
			transfer = targetBalance;
		}
		if (!state.trySpend(target.getUuid(), transfer)) {
			return;
		}
		state.addBalance(killer.getUuid(), transfer);
		long killerBalance = state.getBalance(killer.getUuid());
		long targetBalanceAfter = state.getBalance(target.getUuid());
		killer.sendMessage(Text.translatable("message.emplus.money.steal.gain",
				target.getName().getString(), transfer, killerBalance), false);
		target.sendMessage(Text.translatable("message.emplus.money.steal.loss",
				transfer, targetBalanceAfter), false);
		ShopServerNetworking.sendShopSync(killer);
		ShopServerNetworking.sendShopSync(target);
	}

	private String getNormalizedFaction(net.minecraft.server.network.ServerPlayerEntity player) {
		for (String tag : player.getCommandTags()) {
			String normalized = normalizeFactionTag(tag);
			if (!normalized.isEmpty()) {
				return normalized;
			}
		}
		return "";
	}

	private String normalizeFactionTag(String tag) {
		if (tag == null || tag.isBlank()) {
			return "";
		}
		String value = tag;
		if (value.startsWith("emplus:faction=")) {
			value = value.substring("emplus:faction=".length());
		}
		if (value.startsWith("emplus:")) {
			value = value.substring("emplus:".length());
		}
		if (value.startsWith("faction_")) {
			return value;
		}
		return "";
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
