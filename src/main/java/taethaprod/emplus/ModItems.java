package taethaprod.emplus;

import com.google.common.collect.ImmutableMap;
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
import taethaprod.emplus.config.SpawnConfig;
import taethaprod.emplus.config.SpawnConfigManager;
import taethaprod.emplus.item.MythicalKeyItem;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ModItems {
	public static final Map<Integer, MythicalKeyItem> MYTHICAL_KEYS;
	public static final ItemGroup MYTHICAL_KEY_GROUP;
	public static List<EntityType<? extends MobEntity>> KEY_MOBS = new ArrayList<>();

	static {
		refreshMobList();
		Map<Integer, MythicalKeyItem> keys = new LinkedHashMap<>();
		for (int level = 1; level <= 10; level++) {
			MythicalKeyItem key = new MythicalKeyItem(level, new Item.Settings().maxCount(16));
			Identifier id = new Identifier(EMPlus.MOD_ID, "mythical_key_" + level);
			Registry.register(Registries.ITEM, id, key);
			keys.put(level, key);
		}
		MYTHICAL_KEYS = ImmutableMap.copyOf(keys);

		MYTHICAL_KEY_GROUP = Registry.register(
				Registries.ITEM_GROUP,
				new Identifier(EMPlus.MOD_ID, "mythical_keys"),
				FabricItemGroup.builder()
						.icon(() -> new ItemStack(keys.get(1)))
						.displayName(Text.translatable("itemGroup." + EMPlus.MOD_ID + ".mythical_keys"))
						.entries((displayContext, entries) -> {
							keys.values().forEach(key -> entries.add(new ItemStack(key)));
							for (EntityType<? extends MobEntity> mob : KEY_MOBS) {
								for (MythicalKeyItem key : keys.values()) {
									entries.add(createKeyStack(mob, key.getLevel()));
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

	public static Item getKey(int level) {
		return MYTHICAL_KEYS.get(level);
	}

	public static ItemStack createKeyStack(EntityType<? extends MobEntity> type, int level) {
		MythicalKeyItem item = MYTHICAL_KEYS.get(level);
		if (item == null) {
			return ItemStack.EMPTY;
		}
		ItemStack stack = new ItemStack(item);
		if (type != null) {
			item.setMob(stack, type);
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
