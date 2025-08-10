package com.example.mixin;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.example.SharedInventoryManager;

@Mixin(ServerPlayerInteractionManager.class)
public class ServerPlayerInteractionManagerMixin {

    @Shadow
    public ServerPlayerEntity player;

    // Sync inventory after block placement
    @Inject(at = @At("RETURN"), method = "interactBlock")
    private void onInteractBlock(ServerPlayerEntity player, World world, net.minecraft.item.ItemStack stack, Hand hand,
            BlockHitResult hitResult, CallbackInfoReturnable<ActionResult> cir) {
        if (world.isClient()) {
            return;
        }
        // Sync the entire inventory after block placement
        SharedInventoryManager.syncEntireInventory(player);
    }
}
