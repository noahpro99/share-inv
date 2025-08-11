package com.example.mixin;

import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.example.SharedInventoryManager;

@Mixin(ServerPlayerEntity.class)
public class ServerPlayerEntityMixin {

	private static final int SYNC_INTERVAL = 10; // Sync every 10 ticks (0.5 seconds)
	private int syncTicker = 0;

	// Hook into the tick method but only sync hunger and health every 10 ticks
	// (half second)
	@Inject(at = @At("TAIL"), method = "tick")
	private void onTick(CallbackInfo info) {
		// Only sync every SYNC_INTERVAL ticks to reduce overhead
		if (++syncTicker >= SYNC_INTERVAL) {
			syncTicker = 0;
			ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;
			SharedInventoryManager.syncHunger(player);
			SharedInventoryManager.syncHealth(player);
		}
	}
}
