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
import taethaprod.emplus.common.CommonConfig;
import taethaprod.emplus.common.CommonConfigManager;

public final class TpspawnCommand {
	private static final String FACTION_TAG_PREFIX = "emplus:faction=";
	private static final String FACTION_A = "emplus:faction_a";
	private static final String FACTION_B = "emplus:faction_b";

	private TpspawnCommand() {
	}

	public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
		dispatcher.register(CommandManager.literal("tpspawn")
				.requires(source -> source.hasPermissionLevel(2)
						&& CommonConfigManager.isCommandEnabled("/tpspawn"))
				.executes(ctx -> teleportSelf(ctx.getSource()))
				.then(CommandManager.argument("player", EntityArgumentType.player())
						.requires(source -> source.hasPermissionLevel(2))
						.executes(ctx -> teleportTarget(
								ctx.getSource(),
								EntityArgumentType.getPlayer(ctx, "player")
						))));
	}

	private static int teleportSelf(ServerCommandSource source) {
		if (!(source.getEntity() instanceof ServerPlayerEntity player)) {
			source.sendError(Text.literal("Player required when executed from console."));
			return 0;
		}
		return teleportTarget(source, player);
	}

	private static int teleportTarget(ServerCommandSource source, ServerPlayerEntity player) {
		String factionId = getFactionId(player);
		if (factionId.isEmpty()) {
			source.sendError(Text.literal("Faction not set for " + player.getName().getString() + "."));
			return 0;
		}
		CommonConfig.TpspawnConfig config = CommonConfigManager.get().tpspawn;
		CommonConfig.SpawnPoint point = resolveSpawn(config, factionId);
		if (point == null) {
			source.sendError(Text.literal("Spawn not configured for faction: " + factionId));
			return 0;
		}
		ServerWorld world = resolveWorld(source, point.dimension);
		if (world == null) {
			source.sendError(Text.literal("Invalid dimension: " + point.dimension));
			return 0;
		}
		player.teleport(world, point.x, point.y, point.z, point.yaw, point.pitch);
		return 1;
	}

	private static String getFactionId(ServerPlayerEntity player) {
		for (String tag : player.getCommandTags()) {
			if (tag.startsWith(FACTION_TAG_PREFIX)) {
				return tag.substring(FACTION_TAG_PREFIX.length());
			}
		}
		return "";
	}

	private static CommonConfig.SpawnPoint resolveSpawn(CommonConfig.TpspawnConfig config, String factionId) {
		if (config == null || factionId == null) {
			return null;
		}
		if (FACTION_A.equals(factionId)) {
			return config.factionA;
		}
		if (FACTION_B.equals(factionId)) {
			return config.factionB;
		}
		return null;
	}

	private static ServerWorld resolveWorld(ServerCommandSource source, String dimensionId) {
		if (dimensionId == null || dimensionId.isBlank()) {
			return source.getWorld();
		}
		Identifier id = Identifier.tryParse(dimensionId);
		if (id == null) {
			return null;
		}
		RegistryKey<net.minecraft.world.World> key = RegistryKey.of(RegistryKeys.WORLD, id);
		return source.getServer().getWorld(key);
	}
}
