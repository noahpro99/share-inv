package com.example.mixin;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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

	// Sync when items are dropped with Q (offerOrDrop handles Q drops) -
	// offerOrDrop is void in 1.21
	@Inject(at = @At("TAIL"), method = "offerOrDrop")
	private void onOfferOrDrop(ItemStack stack, CallbackInfo info) {
		if (player.getWorld().isClient()) {
			return;
		}
		// Sync the entire inventory after dropping items
		SharedInventoryManager.syncEntireInventory(player);
	}
}