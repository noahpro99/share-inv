package com.example;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.CommandManager;
import net.minecraft.util.collection.DefaultedList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.example.PlayerSyncData.HungerData;
import com.example.PlayerSyncData.HealthData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class SharedInventoryManager implements ModInitializer {

	public static final String MOD_ID = "share-inv";
	private static final Map<String, DefaultedList<ItemStack>> sharedInventories = new HashMap<>();
	// holds player UUID to shared inventory name mapping
	private static final Map<String, String> playerUUIDtoSharedInventoryName = new HashMap<>();
	// Storage for shared hunger data: inventoryName -> HungerData
	private static final Map<String, HungerData> sharedHungerData = new HashMap<>();
	// Storage for shared health data: inventoryName -> HealthData
	private static final Map<String, HealthData> sharedHealthData = new HashMap<>();
	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	private static volatile boolean IS_SYNCING = false;

	public static void joinSharedInventory(PlayerEntity player, String inventoryName) {
		LOGGER.info("Player {} is joining shared inventory: {}", player.getName().getString(), inventoryName);
		IS_SYNCING = true;
		try {
			// if there are no players so far, copy their inventory to the shared inventory
			PlayerInventory playerInventory = player.getInventory();
			if (!sharedInventories.containsKey(inventoryName)) {
				sharedInventories.put(inventoryName, DefaultedList.ofSize(playerInventory.size(), ItemStack.EMPTY));
				DefaultedList<ItemStack> inventory = sharedInventories.get(inventoryName);
				for (int i = 0; i < playerInventory.size(); i++) {
					inventory.set(i, playerInventory.getStack(i).copy());
				}

				// Copy hunger and health data for new shared inventory
				sharedHungerData.put(inventoryName, HungerData.fromPlayer(player));
				sharedHealthData.put(inventoryName, HealthData.fromPlayer(player));
			} else {
				// if the shared inventory already exists, replace the player's inventory with
				// the shared one
				DefaultedList<ItemStack> inventory = sharedInventories.get(inventoryName);
				for (int i = 0; i < inventory.size(); i++) {
					playerInventory.setStack(i, inventory.get(i).copy());
				}

				// Apply shared hunger and health data to the player
				HungerData sharedHunger = sharedHungerData.get(inventoryName);
				if (sharedHunger != null) {
					sharedHunger.applyToPlayer(player);
				}

				HealthData sharedHealth = sharedHealthData.get(inventoryName);
				if (sharedHealth != null) {
					sharedHealth.applyToPlayer(player);
				}

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
			IS_SYNCING = false;
		}
	}

	public static void leaveSharedInventory(PlayerEntity player) {
		String playerUUID = player.getUuidAsString();
		String inventoryName = playerUUIDtoSharedInventoryName.get(playerUUID);

		if (inventoryName != null) {
			LOGGER.info("Player {} is leaving shared inventory: {}", player.getName().getString(), inventoryName);
			playerUUIDtoSharedInventoryName.remove(playerUUID);

			// Check if this was the last player in the shared inventory
			boolean hasOtherPlayers = playerUUIDtoSharedInventoryName.containsValue(inventoryName);
			if (!hasOtherPlayers) {
				LOGGER.info("Removing empty shared inventory: {}", inventoryName);
				sharedInventories.remove(inventoryName);
				sharedHungerData.remove(inventoryName);
				sharedHealthData.remove(inventoryName);
			}
		} else {
			LOGGER.warn("Player {} is not in any shared inventory", player.getName().getString());
		}
	}

	// Sync hunger data for a player to the shared hunger and all group members
	public static void syncHunger(PlayerEntity player) {
		if (player.getWorld().isClient() || IS_SYNCING) {
			return;
		}
		String inventoryId = playerUUIDtoSharedInventoryName.get(player.getUuidAsString());
		if (inventoryId != null && sharedHungerData.containsKey(inventoryId)) {
			HungerData currentHunger = HungerData.fromPlayer(player);
			HungerData sharedHunger = sharedHungerData.get(inventoryId);

			// Only sync if hunger values have actually changed
			if (currentHunger.isDifferentFrom(sharedHunger)) {
				IS_SYNCING = true;
				try {
					// Update shared hunger data with this player's current state
					sharedHungerData.put(inventoryId, currentHunger.copy());

					// Sync to all other players in the group
					syncHungerToAllPlayersInGroup(player, inventoryId);
				} finally {
					IS_SYNCING = false;
				}
			}
		}
	}

	// Sync health data for a player to the shared health and all group members
	public static void syncHealth(PlayerEntity player) {
		if (player.getWorld().isClient() || IS_SYNCING) {
			return;
		}
		String inventoryId = playerUUIDtoSharedInventoryName.get(player.getUuidAsString());
		if (inventoryId != null && sharedHealthData.containsKey(inventoryId)) {
			HealthData currentHealth = HealthData.fromPlayer(player);
			HealthData sharedHealth = sharedHealthData.get(inventoryId);

			// Only sync if health values have actually changed
			if (currentHealth.isDifferentFrom(sharedHealth)) {
				IS_SYNCING = true;
				try {
					// Update shared health data with this player's current state
					sharedHealthData.put(inventoryId, currentHealth.copy());

					// Sync to all other players in the group
					syncHealthToAllPlayersInGroup(player, inventoryId);
				} finally {
					IS_SYNCING = false;
				}
			}
		}
	}

	// Sync the entire inventory for a player to the shared inventory and all group
	// members
	public static void syncEntireInventory(PlayerEntity player) {
		if (player.getWorld().isClient() || IS_SYNCING) {
			return;
		}
		String inventoryId = playerUUIDtoSharedInventoryName.get(player.getUuidAsString());
		if (inventoryId != null && sharedInventories.containsKey(inventoryId)) {
			IS_SYNCING = true;
			try {
				PlayerInventory playerInventory = player.getInventory();
				DefaultedList<ItemStack> shared = sharedInventories.get(inventoryId);
				for (int i = 0; i < Math.min(shared.size(), playerInventory.size()); i++) {
					shared.set(i, playerInventory.getStack(i).copy());
				}
				syncToAllPlayersInGroup(player, inventoryId);
			} finally {
				IS_SYNCING = false;
			}
		}
	}

	public static void syncToAllPlayersInGroup(PlayerEntity player, String inventoryId) {
		LOGGER.debug("Syncing inventory {} to all players in the group for player {}", inventoryId,
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
				LOGGER.debug("Sending inventory update to player {}", otherPlayer.getName().getString());
				// update the other player's inventory server-side
				PlayerInventory otherPlayerInventory = otherPlayer.getInventory();
				for (int i = 0; i < Math.min(stacksToSend.size(), otherPlayerInventory.size()); i++) {
					otherPlayerInventory.setStack(i, stacksToSend.get(i).copy());
				}
				otherPlayer.sendAbilitiesUpdate();
				// Server-side inventory changes automatically sync to client so we don't need
				// to mark dirty
			}
		}
	}

	public static void syncHungerToAllPlayersInGroup(PlayerEntity player, String inventoryId) {
		LOGGER.debug("Syncing hunger {} to all players in the group for player {}", inventoryId,
				player.getName().getString());
		HungerData sharedHunger = sharedHungerData.get(inventoryId);

		if (sharedHunger == null) {
			LOGGER.warn("Shared hunger data {} not found for player {}", inventoryId, player.getName().getString());
			return;
		}

		for (PlayerEntity otherPlayer : player.getWorld().getPlayers()) {
			String otherPlayerInventoryId = playerUUIDtoSharedInventoryName.get(otherPlayer.getUuidAsString());
			if (otherPlayer != player
					&& otherPlayerInventoryId != null
					&& otherPlayerInventoryId.equals(inventoryId)) {
				LOGGER.debug("Sending hunger update to player {}", otherPlayer.getName().getString());
				// update the other player's hunger server-side
				sharedHunger.applyToPlayer(otherPlayer);
			}
		}
	}

	public static void syncHealthToAllPlayersInGroup(PlayerEntity player, String inventoryId) {
		LOGGER.debug("Syncing health {} to all players in the group for player {}", inventoryId,
				player.getName().getString());
		HealthData sharedHealth = sharedHealthData.get(inventoryId);

		if (sharedHealth == null) {
			LOGGER.warn("Shared health data {} not found for player {}", inventoryId, player.getName().getString());
			return;
		}

		for (PlayerEntity otherPlayer : player.getWorld().getPlayers()) {
			String otherPlayerInventoryId = playerUUIDtoSharedInventoryName.get(otherPlayer.getUuidAsString());
			if (otherPlayer != player
					&& otherPlayerInventoryId != null
					&& otherPlayerInventoryId.equals(inventoryId)) {
				LOGGER.debug("Sending health update to player {}", otherPlayer.getName().getString());
				// update the other player's health server-side
				sharedHealth.applyToPlayer(otherPlayer);
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

			dispatcher.register(CommandManager.literal("leaveSharedInventory")
					.executes(context -> {
						PlayerEntity player = context.getSource().getPlayer();
						leaveSharedInventory(player);
						return 1;
					}));
		});
	}
}