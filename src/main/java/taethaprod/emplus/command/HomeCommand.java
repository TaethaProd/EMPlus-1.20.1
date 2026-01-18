package taethaprod.emplus.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import taethaprod.emplus.common.HomeState;

public final class HomeCommand {
	private HomeCommand() {
	}

	public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
		dispatcher.register(CommandManager.literal("setplayerhome")
				.requires(source -> source.hasPermissionLevel(2))
				.then(CommandManager.argument("player", EntityArgumentType.player())
						.executes(context -> setHome(
								context.getSource(),
								EntityArgumentType.getPlayer(context, "player")
						))));

		dispatcher.register(CommandManager.literal("backhome")
				.requires(source -> source.hasPermissionLevel(2))
				.then(CommandManager.argument("player", EntityArgumentType.player())
						.executes(context -> teleportHome(
								context.getSource(),
								EntityArgumentType.getPlayer(context, "player")
						))));
	}

	private static int setHome(ServerCommandSource source, ServerPlayerEntity target) {
		HomeState state = HomeState.get(source.getServer());
		String dimension = target.getWorld().getRegistryKey().getValue().toString();
		state.setHome(target.getUuid(), new HomeState.HomeLocation(
				target.getX(),
				target.getY(),
				target.getZ(),
				target.getYaw(),
				target.getPitch(),
				dimension
		));
		source.sendFeedback(() -> Text.literal("Home set for " + target.getName().getString() + "."), true);
		return 1;
	}

	private static int teleportHome(ServerCommandSource source, ServerPlayerEntity target) {
		HomeState state = HomeState.get(source.getServer());
		HomeState.HomeLocation home = state.getHome(target.getUuid());
		if (home == null) {
			source.sendError(Text.literal("Home is not set for " + target.getName().getString() + "."));
			return 0;
		}
		Identifier id = Identifier.tryParse(home.dimension);
		if (id == null) {
			source.sendError(Text.literal("Invalid home dimension for " + target.getName().getString() + "."));
			return 0;
		}
		ServerWorld world = source.getServer().getWorld(RegistryKey.of(RegistryKeys.WORLD, id));
		if (world == null) {
			source.sendError(Text.literal("Home dimension is unavailable for " + target.getName().getString() + "."));
			return 0;
		}
		target.teleport(world, home.x, home.y, home.z, home.yaw, home.pitch);
		source.sendFeedback(() -> Text.literal("Teleported " + target.getName().getString() + " to home."), true);
		return 1;
	}
}
