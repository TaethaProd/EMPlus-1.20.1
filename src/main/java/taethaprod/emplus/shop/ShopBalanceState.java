package taethaprod.emplus.shop;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentState;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class ShopBalanceState extends PersistentState {
	private static final String DATA_NAME = "emplus_shop_balances";
	private static final String KEY_BALANCES = "Balances";

	private final Map<UUID, Long> balances = new HashMap<>();

	public static ShopBalanceState get(MinecraftServer server) {
		return server.getOverworld().getPersistentStateManager().getOrCreate(
				ShopBalanceState::fromNbt,
				ShopBalanceState::new,
				DATA_NAME
		);
	}

	public long getBalance(UUID uuid) {
		return Math.max(0L, balances.getOrDefault(uuid, 0L));
	}

	public long addBalance(UUID uuid, long delta) {
		long next = Math.max(0L, getBalance(uuid) + delta);
		balances.put(uuid, next);
		markDirty();
		return next;
	}

	public boolean trySpend(UUID uuid, long amount) {
		if (amount <= 0) {
			return true;
		}
		long current = getBalance(uuid);
		if (current < amount) {
			return false;
		}
		balances.put(uuid, current - amount);
		markDirty();
		return true;
	}

	public static ShopBalanceState fromNbt(NbtCompound tag) {
		ShopBalanceState state = new ShopBalanceState();
		NbtCompound map = tag.getCompound(KEY_BALANCES);
		for (String key : map.getKeys()) {
			try {
				UUID uuid = UUID.fromString(key);
				long balance = map.getLong(key);
				if (balance > 0L) {
					state.balances.put(uuid, balance);
				}
			} catch (IllegalArgumentException ignored) {
			}
		}
		return state;
	}

	@Override
	public NbtCompound writeNbt(NbtCompound nbt) {
		NbtCompound map = new NbtCompound();
		for (Map.Entry<UUID, Long> entry : balances.entrySet()) {
			map.putLong(entry.getKey().toString(), Math.max(0L, entry.getValue()));
		}
		nbt.put(KEY_BALANCES, map);
		return nbt;
	}
}
