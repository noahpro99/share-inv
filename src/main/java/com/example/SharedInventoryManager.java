package com.example;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mojang.brigadier.arguments.StringArgumentType;

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

	public static void joinSharedInventory(PlayerEntity player, String inventoryName) {
		LOGGER.info("Player {} is joining shared inventory: {}", player.getName().getString(), inventoryName);
		// if there are no players so far, copy their inventory to the shared inventory
		PlayerInventory playerInventory = player.getInventory();
		if (!sharedInventories.containsKey(inventoryName)) {
			sharedInventories.put(inventoryName, DefaultedList.ofSize(playerInventory.size(), ItemStack.EMPTY));
			DefaultedList<ItemStack> inventory = sharedInventories.get(inventoryName);
			for (int i = 0; i < playerInventory.size(); i++) {
				inventory.set(i, playerInventory.getStack(i));
			}
		} else {
			// if the shared inventory already exists, replace the player's inventory with
			// the shared one
			DefaultedList<ItemStack> inventory = sharedInventories.get(inventoryName);
			for (int i = 0; i < inventory.size(); i++) {
				playerInventory.setStack(i, inventory.get(i));
			}
			playerInventory.markDirty();
			player.sendAbilitiesUpdate();
		}

		// add the player to the shared inventory
		String playerUUID = player.getUuidAsString();
		if (!playerUUIDtoSharedInventoryName.containsKey(playerUUID)) {
			playerUUIDtoSharedInventoryName.put(playerUUID, inventoryName);
		} else {
			// if the player is already in a shared inventory, remove them from that one
			String oldInventoryName = playerUUIDtoSharedInventoryName.get(playerUUID);
			if (!oldInventoryName.equals(inventoryName)) {
				sharedInventories.get(oldInventoryName).set(playerInventory.size(), ItemStack.EMPTY);
			}
			playerUUIDtoSharedInventoryName.put(playerUUID, inventoryName);
		}
	}

	public static void syncInventoryChange(PlayerEntity player, int slot, ItemStack stack) {
		LOGGER.info("Syncing inventory change for player {}: slot {}, stack {}", player.getName().getString(), slot,
				stack);
		String inventoryId = playerUUIDtoSharedInventoryName.get(player.getUuidAsString());
		if (inventoryId != null && sharedInventories.containsKey(inventoryId)) {
			sharedInventories.get(inventoryId).set(slot, stack);
			// Notify the player that their inventory has been updated
			syncToAllPlayersInGroup(player, inventoryId);
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

		PacketByteBuf buf = PacketByteBufs.create();
		buf.writeInt(sharedInventory.size());
		for (ItemStack stack : sharedInventory) {
			buf.writeNbt(stack.encode(player.getRegistryManager()));
		}

		for (PlayerEntity otherPlayer : player.getWorld().getPlayers()) {
			String otherPlayerInventoryId = playerUUIDtoSharedInventoryName.get(otherPlayer.getUuidAsString());
			if (otherPlayer != player
					&& otherPlayerInventoryId != null
					&& otherPlayerInventoryId.equals(inventoryId)) {
				LOGGER.info("Sending inventory update to player {}", otherPlayer.getName().getString());
				// Send the packet to the other player
				ServerPlayNetworking.send((ServerPlayerEntity) otherPlayer, new SyncInventoryPayload(sharedInventory));
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