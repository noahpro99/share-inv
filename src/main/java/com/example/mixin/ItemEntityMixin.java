package com.example.mixin;

import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.example.SharedInventoryManager;

@Mixin(ItemEntity.class)
public class ItemEntityMixin {

    // Sync when items are picked up by players
    @Inject(at = @At("TAIL"), method = "onPlayerCollision")
    private void onPlayerCollision(PlayerEntity player, CallbackInfo info) {
        if (player.getWorld().isClient()) {
            return;
        }
        // Sync the entire inventory after picking up items
        SharedInventoryManager.syncEntireInventory(player);
    }
}
