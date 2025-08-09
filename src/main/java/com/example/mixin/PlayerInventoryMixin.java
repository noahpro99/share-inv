package com.example.mixin;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftServer.class)
public class PlayerInventoryMixin {
	@Inject(at = @At("TAIL"), method = "setStack")
	private void onSetStack(int slot, ItemStack stack, CallbackInfo info) {
		PlayerEntity player = (PlayerEntity) (Object) this;
		SharedInventoryManager.syncInventoryChange(player, slot, stack);
	}
}