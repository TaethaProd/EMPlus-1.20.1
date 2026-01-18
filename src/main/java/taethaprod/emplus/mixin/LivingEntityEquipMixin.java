package taethaprod.emplus.mixin;

import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import taethaprod.emplus.classes.ClassesConfigManager;
import taethaprod.emplus.classes.ClassesRestrictionsManager;

@Mixin(PlayerEntity.class)
public abstract class LivingEntityEquipMixin {
	@Inject(method = "equipStack(Lnet/minecraft/entity/EquipmentSlot;Lnet/minecraft/item/ItemStack;)V", at = @At("HEAD"), cancellable = true)
	private void emplus$blockRestrictedEquip(EquipmentSlot slot, ItemStack stack, CallbackInfo ci) {
		if (!(PlayerEntity.class.cast(this) instanceof ServerPlayerEntity player)) {
			return;
		}
		if (stack.isEmpty()) {
			return;
		}
		if (slot == EquipmentSlot.OFFHAND || slot.getType() == EquipmentSlot.Type.ARMOR) {
			if (!ClassesRestrictionsManager.isAllowedForPlayer(player, stack)) {
				String classes = ClassesRestrictionsManager.getAllRequiredClasses(stack).stream()
						.map(ClassesConfigManager::getDisplayName)
						.collect(java.util.stream.Collectors.joining(", "));
				player.sendMessage(Text.translatable("message.emplus.classes.required", classes)
						.formatted(Formatting.RED), true);
				ci.cancel();
				// Ensure client sees item still in hand/offhand after rejection.
				player.getInventory().markDirty();
				player.playerScreenHandler.syncState();
			}
		}
	}
}
