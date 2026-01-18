package taethaprod.emplus.shop;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public final class ShopKeybinds {
	private static final KeyBinding OPEN_SHOP = KeyBindingHelper.registerKeyBinding(new KeyBinding(
			"key.emplus.open_shop",
			InputUtil.Type.KEYSYM,
			GLFW.GLFW_KEY_O,
			"key.categories.emplus"
	));

	private ShopKeybinds() {
	}

	public static void register() {
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (OPEN_SHOP.wasPressed()) {
				if (client.currentScreen == null) {
					client.setScreen(new ShopScreen());
				}
			}
		});
	}
}
