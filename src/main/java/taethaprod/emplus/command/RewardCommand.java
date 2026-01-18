package taethaprod.emplus.command;

import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import taethaprod.emplus.common.OnlineRewardManager;

public final class RewardCommand {
	private RewardCommand() {
	}

	public static void register(com.mojang.brigadier.CommandDispatcher<ServerCommandSource> dispatcher) {
		dispatcher.register(CommandManager.literal("reward")
				.then(CommandManager.literal("show")
						.executes(context -> setVisible(context.getSource(), true)))
				.then(CommandManager.literal("hide")
						.executes(context -> setVisible(context.getSource(), false))));
	}

	private static int setVisible(ServerCommandSource source, boolean visible) {
		if (!(source.getEntity() instanceof ServerPlayerEntity player)) {
			source.sendError(Text.literal("Player required when executed from console."));
			return 0;
		}
		OnlineRewardManager.setVisible(player, visible);
		return 1;
	}
}
