package taethaprod.emplus;

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;
import taethaprod.emplus.config.BossScalingConfigManager;
import taethaprod.emplus.config.SpawnConfig;
import taethaprod.emplus.config.SpawnConfigManager;
import taethaprod.emplus.item.ArtifactLocatorItem;
import taethaprod.emplus.item.MythicalKeyItem;

import java.util.ArrayList;
import java.util.List;

public final class ModItems {
	public static Item ARTIFACT_LOCATOR;
	public static Item ARTIFACT;
	public static MythicalKeyItem MYTHICAL_KEY;
	public static final ItemGroup EMPLUS_GROUP;
	public static final ItemGroup MYTHICAL_KEY_GROUP;
	public static List<EntityType<? extends MobEntity>> KEY_MOBS = new ArrayList<>();

	static {
		refreshMobList();
		ARTIFACT_LOCATOR = new ArtifactLocatorItem(new Item.Settings().maxCount(1));
		Registry.register(Registries.ITEM, new Identifier(EMPlus.MOD_ID, "artifact_locator"), ARTIFACT_LOCATOR);

		ARTIFACT = new Item(new Item.Settings().rarity(Rarity.UNCOMMON));
		Registry.register(Registries.ITEM, new Identifier(EMPlus.MOD_ID, "artifact"), ARTIFACT);

		MYTHICAL_KEY = new MythicalKeyItem(new Item.Settings().maxCount(16).rarity(Rarity.EPIC));
		Registry.register(Registries.ITEM, new Identifier(EMPlus.MOD_ID, "mythical_key"), MYTHICAL_KEY);

		EMPLUS_GROUP = Registry.register(
				Registries.ITEM_GROUP,
				new Identifier(EMPlus.MOD_ID, "emplus"),
				FabricItemGroup.builder()
						.icon(() -> new ItemStack(ARTIFACT_LOCATOR))
						.displayName(Text.translatable("itemGroup." + EMPlus.MOD_ID + ".emplus"))
						.entries((displayContext, entries) -> {
							entries.add(ARTIFACT_LOCATOR);
							entries.add(ARTIFACT);
							for (int tier : BossScalingConfigManager.getDefinedTiers()) {
								entries.add(createKeyStack(null, tier));
							}
							for (EntityType<? extends MobEntity> mob : KEY_MOBS) {
								for (int tier : BossScalingConfigManager.getDefinedTiers()) {
									entries.add(createKeyStack(mob, tier));
								}
							}
						})
						.build()
		);

		MYTHICAL_KEY_GROUP = Registry.register(
				Registries.ITEM_GROUP,
				new Identifier(EMPlus.MOD_ID, "mythical_keys"),
				FabricItemGroup.builder()
						.icon(() -> createKeyStack(null, 1))
						.displayName(Text.translatable("itemGroup." + EMPlus.MOD_ID + ".mythical_keys"))
						.entries((displayContext, entries) -> {
							for (int tier : BossScalingConfigManager.getDefinedTiers()) {
								entries.add(createKeyStack(null, tier));
							}
							for (EntityType<? extends MobEntity> mob : KEY_MOBS) {
								for (int tier : BossScalingConfigManager.getDefinedTiers()) {
									entries.add(createKeyStack(mob, tier));
								}
							}
						})
						.build()
		);
	}

	private ModItems() {
	}

	public static void init() {
		// Trigger static initializers.
	}

	public static ItemStack createKeyStack(EntityType<? extends MobEntity> type, int level) {
		ItemStack stack = new ItemStack(MYTHICAL_KEY);
		MYTHICAL_KEY.setLevel(stack, level);
		if (type != null) {
			MYTHICAL_KEY.setMob(stack, type);
		}
		return stack;
	}

	public static EntityType<? extends MobEntity> getRandomMobType(net.minecraft.util.math.random.Random random) {
		if (KEY_MOBS.isEmpty()) {
			refreshMobList();
		}
		return KEY_MOBS.get(random.nextInt(KEY_MOBS.size()));
	}

	private static void refreshMobList() {
		KEY_MOBS = buildMobList();
	}

	private static List<EntityType<? extends MobEntity>> buildMobList() {
		SpawnConfigManager.load();
		SpawnConfig config = SpawnConfigManager.get();
		List<EntityType<? extends MobEntity>> result = new ArrayList<>();
		for (String idString : config.allowedEntities) {
			Identifier id = Identifier.tryParse(idString);
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
		if (result.isEmpty()) {
			// Fallback to defaults if config is empty or invalid.
			result.add(EntityType.ZOMBIE);
			result.add(EntityType.SKELETON);
			result.add(EntityType.WITCH);
		}
		return List.copyOf(result);
	}
}
