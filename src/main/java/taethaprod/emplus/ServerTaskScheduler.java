package taethaprod.emplus;

import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

public final class ServerTaskScheduler {
	private static final List<Task> TASKS = new ArrayList<>();

	private ServerTaskScheduler() {
	}

	public static void schedule(ServerWorld world, int delayTicks, Consumer<ServerWorld> action) {
		if (delayTicks < 0) {
			delayTicks = 0;
		}
		TASKS.add(new Task(world.getRegistryKey(), delayTicks, action));
	}

	public static void tick(MinecraftServer server) {
		Iterator<Task> iterator = TASKS.iterator();
		while (iterator.hasNext()) {
			Task task = iterator.next();
			task.ticks--;
			if (task.ticks > 0) {
				continue;
			}
			ServerWorld world = server.getWorld(task.worldKey);
			if (world != null) {
				task.action.accept(world);
			}
			iterator.remove();
		}
	}

	private static final class Task {
		private final RegistryKey<World> worldKey;
		private int ticks;
		private final Consumer<ServerWorld> action;

		private Task(RegistryKey<World> worldKey, int ticks, Consumer<ServerWorld> action) {
			this.worldKey = worldKey;
			this.ticks = ticks;
			this.action = action;
		}
	}
}
