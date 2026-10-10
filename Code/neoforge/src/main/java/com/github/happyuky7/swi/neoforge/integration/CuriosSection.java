package com.github.happyuky7.swi.neoforge.integration;

import com.github.happyuky7.swi.api.data.DataNode;
import com.github.happyuky7.swi.api.section.DataSection;
import com.github.happyuky7.swi.mc.section.SlotContainerCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

import java.util.Map;

/**
 * Curios slots (rings, necklaces, backpacks...) saved per group, including cosmetic slots.
 * Record layout: {@code custom.curios.<slotType>.items.<i>} and {@code custom.curios.<slotType>.cosmetic.<i>}.
 * Toggle: {@code settings.options.saves.curios}.
 */
public final class CuriosSection implements DataSection<ServerPlayer> {

    @Override
    public @NotNull String id() {
        return "curios";
    }

    @Override
    public @NotNull Class<ServerPlayer> playerType() {
        return ServerPlayer.class;
    }

    @Override
    public void save(@NotNull ServerPlayer player, @NotNull DataNode out) {
        CuriosApi.getCuriosInventory(player).ifPresent(inv -> {
            for (Map.Entry<String, ICurioStacksHandler> e : inv.getCurios().entrySet()) {
                SlotContainerCodec.save(player, slots(e.getValue().getStacks()), out.getOrCreateNode(e.getKey() + ".items"));
                SlotContainerCodec.save(player, slots(e.getValue().getCosmeticStacks()), out.getOrCreateNode(e.getKey() + ".cosmetic"));
            }
        });
    }

    @Override
    public void load(@NotNull ServerPlayer player, @NotNull DataNode in) {
        CuriosApi.getCuriosInventory(player).ifPresent(inv -> {
            for (Map.Entry<String, ICurioStacksHandler> e : inv.getCurios().entrySet()) {
                SlotContainerCodec.load(player, slots(e.getValue().getStacks()), in.getNode(e.getKey() + ".items"));
                SlotContainerCodec.load(player, slots(e.getValue().getCosmeticStacks()), in.getNode(e.getKey() + ".cosmetic"));
            }
            // Slot types that exist in the record but not on this server: give the items back.
            for (String type : in.keys()) {
                if (!inv.getCurios().containsKey(type)) {
                    SlotContainerCodec.load(player, SlotContainerCodec.NONE, in.getNode(type + ".items"));
                    SlotContainerCodec.load(player, SlotContainerCodec.NONE, in.getNode(type + ".cosmetic"));
                }
            }
        });
    }

    @Override
    public void clear(@NotNull ServerPlayer player) {
        CuriosApi.getCuriosInventory(player).ifPresent(inv -> {
            for (ICurioStacksHandler handler : inv.getCurios().values()) {
                SlotContainerCodec.clear(slots(handler.getStacks()));
                SlotContainerCodec.clear(slots(handler.getCosmeticStacks()));
            }
        });
    }

    private static SlotContainerCodec.Slots slots(IDynamicStackHandler handler) {
        return new SlotContainerCodec.Slots() {
            public int size() { return handler.getSlots(); }
            public ItemStack get(int slot) { return handler.getStackInSlot(slot); }
            public void set(int slot, ItemStack stack) { handler.setStackInSlot(slot, stack); }
        };
    }
}
