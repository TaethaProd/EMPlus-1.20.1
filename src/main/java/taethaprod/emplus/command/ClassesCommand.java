package taethaprod.emplus.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.registry.Registries;
import taethaprod.emplus.classes.ClassesApi;
import taethaprod.emplus.classes.ClassesConfigManager;
import taethaprod.emplus.classes.ClassesRestrictionsManager;

public final class ClassesCommand {
	private ClassesCommand() {
	}

	public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
		dispatcher.register(CommandManager.literal("everlong")
				.requires(source -> source.hasPermissionLevel(2))
				.then(CommandManager.literal("classes")
						.then(CommandManager.literal("add")
								.then(CommandManager.argument("name", StringArgumentType.word())
										.executes(context -> addClass(context.getSource(), StringArgumentType.getString(context, "name")))))
						.then(CommandManager.literal("set")
								.then(CommandManager.argument("player", EntityArgumentType.player())
										.then(CommandManager.argument("name", StringArgumentType.word())
												.executes(context -> setClass(
														context.getSource(),
														EntityArgumentType.getPlayer(context, "player"),
														StringArgumentType.getString(context, "name")
												)))))
						.then(CommandManager.literal("restrict")
								.then(CommandManager.argument("item", StringArgumentType.string())
										.then(CommandManager.argument("name", StringArgumentType.word())
												.executes(context -> restrict(
														context.getSource(),
														StringArgumentType.getString(context, "item"),
														StringArgumentType.getString(context, "name")
												)))))));
	}

	private static int addClass(ServerCommandSource source, String className) {
		if (ClassesConfigManager.addClass(className)) {
			source.sendFeedback(() -> Text.translatable("message.emplus.classes.added", className), true);
			return 1;
		}
		source.sendError(Text.translatable("message.emplus.classes.add_failed"));
		return 0;
	}

	private static int setClass(ServerCommandSource source, ServerPlayerEntity player, String className) {
		if (!ClassesConfigManager.hasClass(className)) {
			source.sendError(Text.translatable("message.emplus.classes.not_found", className));
			return 0;
		}
		applyClassTag(player, className);
		source.sendFeedback(() -> Text.translatable("message.emplus.classes.set",
				className, player.getName().getString()), true);
		return 1;
	}

	private static int restrict(ServerCommandSource source, String itemIdString, String className) {
		if (!ClassesConfigManager.hasClass(className)) {
			source.sendError(Text.translatable("message.emplus.classes.not_found", className));
			return 0;
		}
		Identifier id = Identifier.tryParse(itemIdString);
		if (id == null) {
			source.sendError(Text.translatable("message.emplus.classes.invalid_item", itemIdString));
			return 0;
		}
		var item = Registries.ITEM.get(id);
		if (item == net.minecraft.item.Items.AIR) {
			source.sendError(Text.translatable("message.emplus.classes.item_not_found", id.toString()));
			return 0;
		}
		if (ClassesRestrictionsManager.addRestriction(className, id)) {
			source.sendFeedback(() -> Text.translatable("message.emplus.classes.restricted",
					id.toString(), className), true);
			return 1;
		}
		source.sendError(Text.translatable("message.emplus.classes.restriction_exists"));
		return 0;
	}

	private static void applyClassTag(ServerPlayerEntity player, String className) {
		player.getCommandTags().removeIf(tag -> tag.startsWith(ClassesApi.CLASS_TAG_PREFIX));
		player.addCommandTag(ClassesApi.CLASS_TAG_PREFIX + className);
	}
}
