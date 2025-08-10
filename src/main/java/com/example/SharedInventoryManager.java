package com.example;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.CommandManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mojang.brigadier.arguments.StringArgumentType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class SharedInventoryManager implements ModInitializer {

	public static final String MOD_ID = "share-inv";
	private static final Map<String, DefaultedList<ItemStack>> sharedInventories = new HashMap<>();
	// holds player UUID to shared inventory name mapping
	private static final Map<String, String> playerUUIDtoSharedInventoryName = new HashMap<>();
	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	public static final Identifier SYNC_INVENTORY_PACKET = Identifier.of(MOD_ID, "sync_inventory");
	private static final ThreadLocal<Boolean> IS_SYNCING = ThreadLocal.withInitial(() -> false);

	public static void joinSharedInventory(PlayerEntity player, String inventoryName) {
		LOGGER.info("Player {} is joining shared inventory: {}", player.getName().getString(), inventoryName);
		IS_SYNCING.set(true);
		try {
			// if there are no players so far, copy their inventory to the shared inventory
			PlayerInventory playerInventory = player.getInventory();
			if (!sharedInventories.containsKey(inventoryName)) {
				sharedInventories.put(inventoryName, DefaultedList.ofSize(playerInventory.size(), ItemStack.EMPTY));
				DefaultedList<ItemStack> inventory = sharedInventories.get(inventoryName);
				for (int i = 0; i < playerInventory.size(); i++) {
					inventory.set(i, playerInventory.getStack(i).copy());
				}
			} else {
				// if the shared inventory already exists, replace the player's inventory with
				// the shared one
				DefaultedList<ItemStack> inventory = sharedInventories.get(inventoryName);
				for (int i = 0; i < inventory.size(); i++) {
					playerInventory.setStack(i, inventory.get(i).copy());
				}
				playerInventory.markDirty();
				player.sendAbilitiesUpdate();
			}

			// add the player to the shared inventory
			String playerUUID = player.getUuidAsString();
			if (!playerUUIDtoSharedInventoryName.containsKey(playerUUID)) {
				playerUUIDtoSharedInventoryName.put(playerUUID, inventoryName);
			} else {
				playerUUIDtoSharedInventoryName.put(playerUUID, inventoryName);
			}
		} finally {
			IS_SYNCING.set(false);
		}
	}

	// Sync the entire inventory for a player to the shared inventory and all group
	// members
	public static void syncEntireInventory(PlayerEntity player) {
		if (player.getWorld().isClient() || IS_SYNCING.get()) {
			return;
		}
		String inventoryId = playerUUIDtoSharedInventoryName.get(player.getUuidAsString());
		if (inventoryId != null && sharedInventories.containsKey(inventoryId)) {
			IS_SYNCING.set(true);
			try {
				PlayerInventory playerInventory = player.getInventory();
				DefaultedList<ItemStack> shared = sharedInventories.get(inventoryId);
				for (int i = 0; i < Math.min(shared.size(), playerInventory.size()); i++) {
					shared.set(i, playerInventory.getStack(i).copy());
				}
				syncToAllPlayersInGroup(player, inventoryId);
			} finally {
				IS_SYNCING.set(false);
			}
		}
	}

	public static void syncToAllPlayersInGroup(PlayerEntity player, String inventoryId) {
		LOGGER.info("Syncing inventory {} to all players in the group for player {}", inventoryId,
				player.getName().getString());
		DefaultedList<ItemStack> sharedInventory = sharedInventories.get(inventoryId);

		if (sharedInventory == null) {
			LOGGER.warn("Shared inventory {} not found for player {}", inventoryId, player.getName().getString());
			return;
		}

		var stacksToSend = new ArrayList<ItemStack>(sharedInventory);
		for (PlayerEntity otherPlayer : player.getWorld().getPlayers()) {
			String otherPlayerInventoryId = playerUUIDtoSharedInventoryName.get(otherPlayer.getUuidAsString());
			if (otherPlayer != player
					&& otherPlayerInventoryId != null
					&& otherPlayerInventoryId.equals(inventoryId)) {
				LOGGER.info("Sending inventory update to player {}", otherPlayer.getName().getString());
				// update the other player's inventory server-side
				PlayerInventory otherPlayerInventory = otherPlayer.getInventory();
				for (int i = 0; i < Math.min(stacksToSend.size(), otherPlayerInventory.size()); i++) {
					otherPlayerInventory.setStack(i, stacksToSend.get(i).copy());
				}
				otherPlayerInventory.markDirty();
				// Server-side inventory changes automatically sync to client
			}
		}
	}

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

		LOGGER.info("Hello Fabric world!");

		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			dispatcher.register(CommandManager.literal("joinSharedInventory")
					.then(CommandManager.argument("inventoryName", StringArgumentType.string())
							.executes(context -> {
								PlayerEntity player = context.getSource().getPlayer();
								String inventoryName = StringArgumentType.getString(context, "inventoryName");
								joinSharedInventory(player, inventoryName);
								return 1;
							})));
		});
	}
}