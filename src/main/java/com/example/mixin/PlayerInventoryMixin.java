package com.example.mixin;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.example.SharedInventoryManager;

@Mixin(PlayerInventory.class)
public class PlayerInventoryMixin {
	@Shadow
	public PlayerEntity player;

	// Broad net for slot changes - markDirty is called for most inventory mutations
	@Inject(at = @At("TAIL"), method = "markDirty")
	private void onMarkDirty(CallbackInfo info) {
		if (player.getWorld().isClient()) {
			return;
		}
		// Sync the entire inventory after any change that marks it dirty
		SharedInventoryManager.syncEntireInventory(player);
	}

	// Sync when items are offered to inventory (another pickup method)
	@Inject(at = @At("TAIL"), method = "offer")
	private void onOffer(ItemStack stack, boolean notifiesClient, CallbackInfo info) {
		if (!this.player.getWorld().isClient()) {
			SharedInventoryManager.syncEntireInventory(this.player);
		}
	}

	// Hook into insertStack to catch item pickups
	@Inject(at = @At("RETURN"), method = "insertStack(Lnet/minecraft/item/ItemStack;)Z")
	private void onInsertStack(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
		// Only sync if the item was actually inserted (return value is true)
		if (cir.getReturnValue() && !this.player.getWorld().isClient()) {
			SharedInventoryManager.syncEntireInventory(this.player);
		}
	}

	// Sync when player drops items
	@Inject(at = @At("RETURN"), method = "dropSelectedItem")
	private void onDropSelectedItem(boolean entireStack, CallbackInfoReturnable<ItemStack> cir) {
		if (!this.player.getWorld().isClient() && !cir.getReturnValue().isEmpty()) {
			// Only sync if something was actually dropped
			SharedInventoryManager.syncEntireInventory(this.player);
		}
	}

	// Hook into removeStack to catch item consumption (like drinking potions, using
	// buckets, etc.)
	@Inject(at = @At("RETURN"), method = "removeStack(II)Lnet/minecraft/item/ItemStack;")
	private void onRemoveStack(int slot, int amount, CallbackInfoReturnable<ItemStack> cir) {
		if (!this.player.getWorld().isClient() && !cir.getReturnValue().isEmpty()) {
			// Sync when items are removed (consumed, used, etc.)
			SharedInventoryManager.syncEntireInventory(this.player);
		}
	}

	// Hook into setStack to catch direct slot changes (like bucket use, item
	// swapping, etc.)
	@Inject(at = @At("TAIL"), method = "setStack")
	private void onSetStack(int slot, ItemStack stack, CallbackInfo info) {
		if (!this.player.getWorld().isClient()) {
			// Sync when items are directly set in slots
			SharedInventoryManager.syncEntireInventory(this.player);
		}
	}
}