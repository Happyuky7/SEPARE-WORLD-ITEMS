package com.github.happyuky7.swi.mc.section;

import com.github.happyuky7.swi.api.data.DataNode;
import com.github.happyuky7.swi.mc.compat.ItemCodec;
import com.github.happyuky7.swi.mc.compat.Mc;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * Saves/restores one slot container of an accessory mod (Curios, Trinkets, Accessories) as
 * {@code <index>: <SNBT>} entries. Items whose slot no longer exists in the destination group are given
 * back to the player (or dropped) instead of being deleted.
 */
public final class SlotContainerCodec {

    /** Minimal view over a mod's slot container. */
    public interface Slots {
        int size();

        ItemStack get(int slot);

        void set(int slot, ItemStack stack);
    }

    /** Zero-slot container: every stored item is given back to the player. */
    public static final Slots NONE = new Slots() {
        public int size() { return 0; }
        public ItemStack get(int slot) { return ItemStack.EMPTY; }
        public void set(int slot, ItemStack stack) { }
    };

    private SlotContainerCodec() {
    }

    /** View over a vanilla {@link net.minecraft.world.Container} (Trinkets, Accessories...). */
    public static Slots of(net.minecraft.world.Container container) {
        return new Slots() {
            public int size() { return container.getContainerSize(); }
            public ItemStack get(int slot) { return container.getItem(slot); }
            public void set(int slot, ItemStack stack) { container.setItem(slot, stack); }
        };
    }

    public static void save(ServerPlayer player, Slots slots, DataNode out) {
        for (int i = 0; i < slots.size(); i++) {
            String snbt = ItemCodec.encode(player, slots.get(i));
            if (snbt != null) {
                out.set(Integer.toString(i), snbt);
            }
        }
    }

    public static void load(ServerPlayer player, Slots slots, DataNode in) {
        for (int i = 0; i < slots.size(); i++) {
            slots.set(i, ItemStack.EMPTY);
        }
        if (in == null) {
            return;
        }
        for (String key : in.keys()) {
            ItemStack stack = ItemCodec.decode(player, in.get(key));
            if (stack.isEmpty()) {
                continue;
            }
            int slot;
            try {
                slot = Integer.parseInt(key);
            } catch (NumberFormatException e) {
                slot = -1;
            }
            if (slot >= 0 && slot < slots.size()) {
                slots.set(slot, stack);
            } else {
                Mc.giveOrDrop(player, stack);
            }
        }
    }

    public static void clear(Slots slots) {
        for (int i = 0; i < slots.size(); i++) {
            slots.set(i, ItemStack.EMPTY);
        }
    }
}
