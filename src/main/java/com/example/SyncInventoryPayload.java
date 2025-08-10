package com.example;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record SyncInventoryPayload(List<ItemStack> stacks) implements CustomPayload {
    public static final Id<SyncInventoryPayload> ID = new Id<>(
            Identifier.of(SharedInventoryManager.MOD_ID, "sync_inventory"));

    public static final PacketCodec<RegistryByteBuf, SyncInventoryPayload> CODEC = new PacketCodec<>() {
        @Override
        public void encode(RegistryByteBuf buf, SyncInventoryPayload payload) {
            List<ItemStack> list = payload.stacks();
            buf.writeVarInt(list.size());
            for (ItemStack stack : list) {
                ItemStack.PACKET_CODEC.encode(buf, stack);
            }
        }

        @Override
        public SyncInventoryPayload decode(RegistryByteBuf buf) {
            int size = buf.readVarInt();
            List<ItemStack> list = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                list.add(ItemStack.PACKET_CODEC.decode(buf));
            }
            return new SyncInventoryPayload(list);
        }
    };

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}