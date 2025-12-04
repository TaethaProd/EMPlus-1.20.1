package taethaprod.emplus;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EMPlus implements ModInitializer {
	public static final String MOD_ID = "emplus";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ModItems.init();

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

		for (String tag : entity.getCommandTags()) {
			if (!tag.startsWith("emplus:mythical_key_next_level=")) {
				continue;
			}
			try {
				int level = Integer.parseInt(tag.substring(tag.lastIndexOf('=') + 1));
				if (level >= 1 && level <= 10) {
					var mob = ModItems.getRandomMobType(entity.getWorld().getRandom());
					ItemStack stack = ModItems.createKeyStack(mob, level);
					if (!stack.isEmpty()) {
						entity.dropStack(stack);
					}
				}
			} catch (NumberFormatException ignored) {
			}
			break;
		}
	}
}
