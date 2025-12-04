package taethaprod.emplus;

import com.google.common.collect.ImmutableMap;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import taethaprod.emplus.item.MythicalKeyItem;

import java.util.*;

public final class ModItems {
	public static final Map<Integer, MythicalKeyItem> MYTHICAL_KEYS;
	public static final ItemGroup MYTHICAL_KEY_GROUP;
	public static final List<EntityType<? extends MobEntity>> KEY_MOBS = List.of(
			EntityType.ZOMBIE,
			EntityType.SKELETON,
			EntityType.WITCH
	);

	static {
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
						.icon(() -> createKeyStack(KEY_MOBS.get(0), 1))
						.displayName(Text.translatable("itemGroup." + EMPlus.MOD_ID + ".mythical_keys"))
						.entries((displayContext, entries) -> {
							for (EntityType<? extends MobEntity> type : KEY_MOBS) {
								for (MythicalKeyItem key : keys.values()) {
									entries.add(createKeyStack(type, key.getLevel()));
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
		item.setMob(stack, type);
		return stack;
	}

	public static EntityType<? extends MobEntity> getRandomMobType(net.minecraft.util.math.random.Random random) {
		return KEY_MOBS.get(random.nextInt(KEY_MOBS.size()));
	}
}
