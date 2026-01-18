package taethaprod.emplus.shop;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public final class MoneyCommand {
	private MoneyCommand() {
	}

	public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
		dispatcher.register(CommandManager.literal("money")
				.then(CommandManager.literal("add")
						.requires(source -> source.hasPermissionLevel(2))
						.then(CommandManager.argument("player", EntityArgumentType.player())
								.then(CommandManager.argument("amount", IntegerArgumentType.integer(1))
										.executes(context -> addMoney(
												context.getSource(),
												EntityArgumentType.getPlayer(context, "player"),
												IntegerArgumentType.getInteger(context, "amount")
										)))))
				.then(CommandManager.literal("give")
						.requires(source -> source.getEntity() instanceof ServerPlayerEntity)
						.then(CommandManager.argument("player", EntityArgumentType.player())
								.then(CommandManager.argument("amount", IntegerArgumentType.integer(1))
										.executes(context -> giveMoney(
												context.getSource(),
												EntityArgumentType.getPlayer(context, "player"),
												IntegerArgumentType.getInteger(context, "amount")
										)))))
				.then(CommandManager.literal("set")
						.requires(source -> source.hasPermissionLevel(2))
						.then(CommandManager.argument("player", EntityArgumentType.player())
								.then(CommandManager.argument("amount", IntegerArgumentType.integer(0))
										.executes(context -> setMoney(
												context.getSource(),
												EntityArgumentType.getPlayer(context, "player"),
												IntegerArgumentType.getInteger(context, "amount")
										)))))
				.then(CommandManager.literal("check")
						.requires(source -> source.hasPermissionLevel(2))
						.then(CommandManager.argument("player", EntityArgumentType.player())
								.executes(context -> checkMoney(
										context.getSource(),
										EntityArgumentType.getPlayer(context, "player")
								)))));
	}

	private static int addMoney(ServerCommandSource source, ServerPlayerEntity target, int amount) {
		ShopBalanceState state = ShopBalanceState.get(source.getServer());
		long next = state.addBalance(target.getUuid(), amount);
		source.sendFeedback(() -> Text.literal("Added " + amount + " to " + target.getName().getString() + ". Balance: " + next), true);
		target.sendMessage(Text.literal("You received " + amount + " money."), false);
		ShopServerNetworking.sendShopSync(target);
		return 1;
	}

	private static int giveMoney(ServerCommandSource source, ServerPlayerEntity target, int amount) {
		if (!(source.getEntity() instanceof ServerPlayerEntity giver)) {
			source.sendError(Text.literal("Only players can give money."));
			return 0;
		}
		ShopBalanceState state = ShopBalanceState.get(source.getServer());
		if (!state.trySpend(giver.getUuid(), amount)) {
			source.sendError(Text.literal("Not enough money."));
			return 0;
		}
		state.addBalance(target.getUuid(), amount);
		source.sendFeedback(() -> Text.literal("Sent " + amount + " to " + target.getName().getString() + "."), false);
		if (!giver.getUuid().equals(target.getUuid())) {
			target.sendMessage(Text.literal(giver.getName().getString() + " sent you " + amount + " money."), false);
		}
		ShopServerNetworking.sendShopSync(giver);
		ShopServerNetworking.sendShopSync(target);
		return 1;
	}

	private static int setMoney(ServerCommandSource source, ServerPlayerEntity target, int amount) {
		ShopBalanceState state = ShopBalanceState.get(source.getServer());
		long current = state.getBalance(target.getUuid());
		long next = state.addBalance(target.getUuid(), amount - current);
		source.sendFeedback(() -> Text.literal("Set balance for " + target.getName().getString()
				+ " to " + next + "."), true);
		ShopServerNetworking.sendShopSync(target);
		return 1;
	}

	private static int checkMoney(ServerCommandSource source, ServerPlayerEntity target) {
		ShopBalanceState state = ShopBalanceState.get(source.getServer());
		long balance = state.getBalance(target.getUuid());
		source.sendFeedback(() -> Text.literal(target.getName().getString() + " balance: " + balance), false);
		return 1;
	}
}
