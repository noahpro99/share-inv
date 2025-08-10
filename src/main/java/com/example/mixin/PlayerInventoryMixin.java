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

	@Inject(at = @At("TAIL"), method = "setStack")
	private void onSetStack(int slot, ItemStack stack, CallbackInfo info) {
		if (player.getWorld().isClient()) {
			return;
		}
		SharedInventoryManager.syncInventoryChange(player, slot, stack);
	}
}