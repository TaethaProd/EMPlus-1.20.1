package taethaprod.emplus.shop;

import net.minecraft.util.Identifier;
import taethaprod.emplus.EMPlus;

public final class ShopNetworking {
	public static final Identifier SHOP_REQUEST = new Identifier(EMPlus.MOD_ID, "shop_request");
	public static final Identifier SHOP_ACTION = new Identifier(EMPlus.MOD_ID, "shop_action");
	public static final Identifier SHOP_SYNC = new Identifier(EMPlus.MOD_ID, "shop_sync");

	public static final byte ACTION_BUY = 1;
	public static final byte ACTION_SELL = 2;

	private ShopNetworking() {
	}
}
