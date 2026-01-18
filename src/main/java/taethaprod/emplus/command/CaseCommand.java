package taethaprod.emplus.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import taethaprod.emplus.cases.CaseConfigManager;
import taethaprod.emplus.cases.CaseServerNetworking;

public final class CaseCommand {
	private CaseCommand() {
	}

	public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
		dispatcher.register(CommandManager.literal("case")
				.requires(source -> source.hasPermissionLevel(2))
				.then(CommandManager.argument("case", StringArgumentType.word())
						.then(CommandManager.argument("player", EntityArgumentType.player())
								.executes(context -> openCase(
										context.getSource(),
										StringArgumentType.getString(context, "case"),
										EntityArgumentType.getPlayer(context, "player")
								)))));
	}

	private static int openCase(ServerCommandSource source, String caseId, ServerPlayerEntity player) {
		if (!CaseConfigManager.getCaseTypes().contains(caseId)) {
			source.sendError(Text.literal("Unknown case: " + caseId));
			return 0;
		}
		CaseServerNetworking.sendOpen(player, caseId);
		return 1;
	}
}
