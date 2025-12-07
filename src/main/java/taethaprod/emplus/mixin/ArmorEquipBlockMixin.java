package taethaprod.emplus.mixin;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import taethaprod.emplus.classes.ClassesRestrictionsManager;

import java.util.Set;
import java.util.stream.Collectors;

@Mixin(ScreenHandler.class)
public abstract class ArmorEquipBlockMixin {
	@Shadow @Final
	public DefaultedList<Slot> slots;

	@Inject(method = "onSlotClick", at = @At("HEAD"), cancellable = true)
	private void emplus$blockRestrictedArmor(int slotIndex, int button, SlotActionType actionType, PlayerEntity player, CallbackInfo ci) {
		if (!(player instanceof ServerPlayerEntity serverPlayer)) {
			return;
		}
		if (slotIndex < 0 || slotIndex >= this.slots.size()) {
			return;
		}
		Slot slot = this.slots.get(slotIndex);

		// Block shift-clicking restricted armor items anywhere (prevents auto-equip).
		if (actionType == SlotActionType.QUICK_MOVE) {
			ItemStack moving = slot.getStack();
			if (!moving.isEmpty() && !ClassesRestrictionsManager.isAllowedForPlayer(serverPlayer, moving)) {
				sendRequirementMessage(serverPlayer, moving);
				ci.cancel();
				return;
			}
		}

		if (!isArmorSlot(slot, player)) {
			return;
		}

		ItemStack incoming = ItemStack.EMPTY;
		switch (actionType) {
			case PICKUP, QUICK_CRAFT, THROW, CLONE, PICKUP_ALL -> incoming = player.currentScreenHandler.getCursorStack();
			case SWAP -> {
				if (button >= 0 && button < 9) {
					incoming = player.getInventory().getStack(button);
				}
			}
			default -> {
			}
		}
		if (incoming.isEmpty()) {
			incoming = slot.getStack();
		}
		if (incoming.isEmpty()) {
			return;
		}
		if (!ClassesRestrictionsManager.isAllowedForPlayer(serverPlayer, incoming)) {
			sendRequirementMessage(serverPlayer, incoming);
			ci.cancel();
		}
	}

	private boolean isArmorSlot(Slot slot, PlayerEntity player) {
		if (!(player.getInventory() == slot.inventory)) {
			return false;
		}
		int index = slot.getIndex();
		return index >= 36 && index < 40; // PlayerInventory armor slots
	}

	private void sendRequirementMessage(ServerPlayerEntity player, ItemStack stack) {
		Set<String> required = ClassesRestrictionsManager.getAllRequiredClasses(stack);
		if (required.isEmpty()) {
			return;
		}
		player.sendMessage(Text.literal("Requires class: " + required.stream().collect(Collectors.joining(", "))), true);
	}
}
