package taethaprod.emplus.onboarding;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public final class OnboardingCommand {
	private OnboardingCommand() {
	}

	public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
		LiteralArgumentBuilder<ServerCommandSource> open = CommandManager.literal("open")
				.requires(src -> src.hasPermissionLevel(2))
				.executes(ctx -> openForSource(ctx.getSource()))
				.then(CommandManager.argument("player", EntityArgumentType.player())
						.executes(ctx -> openForPlayer(
								ctx.getSource(),
								EntityArgumentType.getPlayer(ctx, "player")
						)));

		LiteralArgumentBuilder<ServerCommandSource> root = CommandManager.literal("everlong")
				.then(CommandManager.literal("ui")
						.then(CommandManager.literal("FactionSelectScreen")
								.then(open)));

		dispatcher.register(root);
	}

	private static int openForSource(ServerCommandSource source) {
		if (!(source.getEntity() instanceof ServerPlayerEntity player)) {
			source.sendError(Text.literal("Player required when executed from console."));
			return 0;
		}
		return openForPlayer(source, player);
	}

	private static int openForPlayer(ServerCommandSource source, ServerPlayerEntity player) {
		OnboardingNetworking.sendOpenIntro(player);
		source.sendFeedback(() -> Text.literal("Opened faction select for " + player.getName().getString() + "."), false);
		return 1;
	}
}
