package taethaprod.emplus.onboarding;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public final class OnboardingCommand {
	private OnboardingCommand() {
	}

	public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
		dispatcher.register(
				CommandManager.literal("everlong")
						.then(CommandManager.literal("ui")
								.then(CommandManager.literal("FactionSelectScreen")
										.then(CommandManager.literal("open")
												.requires(src -> src.hasPermissionLevel(2))
												.executes(ctx -> {
													ServerCommandSource src = ctx.getSource();
													if (!(src.getEntity() instanceof ServerPlayerEntity player)) {
														src.sendError(Text.literal("Эта команда доступна только игроку."));
														return 0;
													}
													OnboardingNetworking.sendOpenIntro(player);
													src.sendFeedback(() -> Text.literal("Экран выбора фракции открыт."), false);
													return 1;
												}))))
		);
	}
}
