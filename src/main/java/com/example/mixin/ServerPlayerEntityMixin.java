package com.example.mixin;

import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.example.SharedInventoryManager;

@Mixin(ServerPlayerEntity.class)
public class ServerPlayerEntityMixin {

	private static final int HUNGER_SYNC_INTERVAL = 10; // Sync every 10 ticks (0.5 seconds)
	private int hungerSyncTicker = 0;

	// Hook into the tick method but only sync hunger every 10 ticks (half second)
	@Inject(at = @At("TAIL"), method = "tick")
	private void onTick(CallbackInfo info) {
		// Only sync hunger every HUNGER_SYNC_INTERVAL ticks to reduce overhead
		if (++hungerSyncTicker >= HUNGER_SYNC_INTERVAL) {
			hungerSyncTicker = 0;
			ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;
			SharedInventoryManager.syncHunger(player);
		}
	}
}
