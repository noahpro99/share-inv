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

	// Hook into death to handle shared inventory drops properly
	@Inject(at = @At("HEAD"), method = "onDeath")
	private void onDeath(net.minecraft.entity.damage.DamageSource damageSource, CallbackInfo info) {
		ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;
		String inventoryId = SharedInventoryManager.getPlayerInventoryId(player);

		if (inventoryId != null) {
			// Check if this is the first player in the group to die
			if (SharedInventoryManager.shouldDropItemsOnDeath(player, inventoryId)) {
				// This is the first death - this player will drop items normally
				// Clear all other players' inventories to prevent duplication
				SharedInventoryManager.clearInventoriesForAllPlayersInGroup(player, inventoryId);
				// Clear the shared inventory since items are now on the ground
				SharedInventoryManager.clearSharedInventory(inventoryId);
			} else {
				// Someone already died and dropped items, clear this player's inventory
				player.getInventory().clear();
			}
		}
	}

	// Hook into respawn to restore shared inventory
	@Inject(at = @At("TAIL"), method = "copyFrom")
	private void onRespawn(ServerPlayerEntity oldPlayer, boolean alive, CallbackInfo info) {
		ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;
		// After respawning, get the current shared inventory state and apply it
		// This restores the shared inventory to the respawned player
		String inventoryId = SharedInventoryManager.getPlayerInventoryId(player);
		if (inventoryId != null) {
			SharedInventoryManager.restoreSharedInventoryToPlayer(player, inventoryId);
			// Clear death tracking since someone has respawned
			SharedInventoryManager.clearDeathTracking(inventoryId);
		}
	}
}
