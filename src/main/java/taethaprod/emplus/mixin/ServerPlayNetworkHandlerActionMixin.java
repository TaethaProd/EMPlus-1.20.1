package taethaprod.emplus.mixin;

import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import taethaprod.emplus.classes.ClassesConfigManager;
import taethaprod.emplus.classes.ClassesRestrictionsManager;

import java.util.Set;
import java.util.stream.Collectors;

@Mixin(ServerPlayNetworkHandler.class)
public abstract class ServerPlayNetworkHandlerActionMixin {
	@Shadow public ServerPlayerEntity player;

	@Inject(method = "onPlayerAction", at = @At("HEAD"), cancellable = true)
	private void emplus$blockSwapToOffhand(PlayerActionC2SPacket packet, CallbackInfo ci) {
		if (packet.getAction() != PlayerActionC2SPacket.Action.SWAP_ITEM_WITH_OFFHAND) {
			return;
		}
		ItemStack main = player.getMainHandStack();
		ItemStack off = player.getOffHandStack();
		if (isRestricted(main) || isRestricted(off)) {
			ItemStack offending = isRestricted(main) ? main : off;
			sendRequirementMessage(offending);
			player.playerScreenHandler.syncState();
			ci.cancel();
		}
	}

	private boolean isRestricted(ItemStack stack) {
		return stack != null && !stack.isEmpty() && !ClassesRestrictionsManager.isAllowedForPlayer(player, stack);
	}

	private void sendRequirementMessage(ItemStack stack) {
		Set<String> required = ClassesRestrictionsManager.getAllRequiredClasses(stack);
		if (required.isEmpty()) {
			return;
		}
		String classes = required.stream()
				.map(ClassesConfigManager::getDisplayName)
				.collect(Collectors.joining(", "));
		player.sendMessage(Text.translatable("message.emplus.classes.required", classes)
				.formatted(Formatting.RED), true);
	}
}
