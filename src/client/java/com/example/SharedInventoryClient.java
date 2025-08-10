package com.example;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;

public class SharedInventoryClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// This entrypoint is suitable for setting up client-specific logic, such as
		// rendering.

		PayloadTypeRegistry.playS2C().register(SyncInventoryPayload.ID, SyncInventoryPayload.CODEC);

		ClientPlayNetworking.registerGlobalReceiver(SyncInventoryPayload.ID, (payload, context) -> {
			var client = context.client();
			var stacks = payload.stacks();

			client.execute(() -> {
				PlayerInventory playerInventory = client.player.getInventory();
				for (int i = 0; i < Math.min(stacks.size(), playerInventory.size()); i++) {
					ItemStack stack = stacks.get(i);
					playerInventory.setStack(i, stack);
				}
				playerInventory.markDirty();
			});
		});
	}
}