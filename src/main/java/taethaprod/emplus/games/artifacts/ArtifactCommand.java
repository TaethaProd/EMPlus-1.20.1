package taethaprod.emplus.games.artifacts;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class ArtifactCommand {
	private ArtifactCommand() {
	}

	public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
		dispatcher.register(CommandManager.literal("artifact")
				.requires(source -> source.hasPermissionLevel(2))
				.then(CommandManager.literal("start")
						.executes(context -> ArtifactGameManager.start(context.getSource())))
				.then(CommandManager.literal("stop")
						.executes(context -> ArtifactGameManager.stop(context.getSource())))
				.then(CommandManager.literal("area")
						.then(CommandManager.argument("radius", IntegerArgumentType.integer(1))
								.executes(context -> setArea(
										context.getSource(),
										IntegerArgumentType.getInteger(context, "radius")
								))))
				.then(CommandManager.literal("center")
						.then(CommandManager.argument("x", DoubleArgumentType.doubleArg())
								.then(CommandManager.argument("z", DoubleArgumentType.doubleArg())
										.executes(context -> setCenter(
												context.getSource(),
												DoubleArgumentType.getDouble(context, "x"),
												DoubleArgumentType.getDouble(context, "z")
										)))))
				.then(CommandManager.literal("zones")
						.then(CommandManager.argument("count", IntegerArgumentType.integer(1))
								.executes(context -> setZones(
										context.getSource(),
										IntegerArgumentType.getInteger(context, "count")
								))))
				.then(CommandManager.literal("auto")
						.then(CommandManager.literal("start")
								.executes(context -> ArtifactGameManager.setAuto(context.getSource(), true)))
						.then(CommandManager.literal("stop")
								.executes(context -> ArtifactGameManager.setAuto(context.getSource(), false))))
				.then(CommandManager.literal("locate")
						.executes(context -> ArtifactGameManager.locate(context.getSource())))
		);
	}

	private static int setArea(ServerCommandSource source, int radius) {
		ArtifactsConfig config = ArtifactsConfigManager.get();
		config.areaRadius = radius;
		ArtifactsConfigManager.save();
		source.sendFeedback(() -> Text.translatable("emplus.artifacts.command.area_set", radius)
				.formatted(Formatting.GREEN), true);
		return 1;
	}

	private static int setCenter(ServerCommandSource source, double x, double z) {
		ArtifactsConfig config = ArtifactsConfigManager.get();
		config.centerX = x;
		config.centerZ = z;
		ArtifactsConfigManager.save();
		source.sendFeedback(() -> Text.translatable("emplus.artifacts.command.center_set", x, z)
				.formatted(Formatting.GREEN), true);
		return 1;
	}

	private static int setZones(ServerCommandSource source, int count) {
		ArtifactsConfig config = ArtifactsConfigManager.get();
		config.zones = count;
		ArtifactsConfigManager.save();
		source.sendFeedback(() -> Text.translatable("emplus.artifacts.command.zones_set", count)
				.formatted(Formatting.GREEN), true);
		return 1;
	}
}
