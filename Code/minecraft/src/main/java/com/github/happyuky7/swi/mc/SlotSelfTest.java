package com.github.happyuky7.swi.mc;

import com.github.happyuky7.swi.mc.compat.ItemCodec;
import com.github.happyuky7.swi.mc.section.SlotContainerCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;
import java.util.function.Function;

/**
 * Self-test hook for accessory mods: puts an item in the first available slot and checks it follows the group.
 */
public final class SlotSelfTest implements VanillaSelfTest.Hook {

    private final String name;
    private final Function<ServerPlayer, SlotContainerCodec.Slots> firstSlots;
    private String expected;

    public SlotSelfTest(String name, Function<ServerPlayer, SlotContainerCodec.Slots> firstSlots) {
        this.name = name;
        this.firstSlots = firstSlots;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public void prepare(ServerPlayer player, List<String> failures) {
        SlotContainerCodec.Slots slots = firstSlots.apply(player);
        if (slots == null || slots.size() == 0) {
            failures.add(name + ": no slot available on the test player");
            return;
        }
        ItemStack stack = new ItemStack(Items.EMERALD, 2);
        slots.set(0, stack);
        expected = ItemCodec.encode(player, stack);
    }

    @Override
    public void verifyCleared(ServerPlayer player, List<String> failures) {
        SlotContainerCodec.Slots slots = firstSlots.apply(player);
        if (expected != null && slots != null && !slots.get(0).isEmpty()) {
            failures.add(name + ": slot not cleared in the other group");
        }
    }

    @Override
    public void verifyRestored(ServerPlayer player, List<String> failures) {
        SlotContainerCodec.Slots slots = firstSlots.apply(player);
        if (expected != null && (slots == null || !expected.equals(ItemCodec.encode(player, slots.get(0))))) {
            failures.add(name + ": slot not restored (expected " + expected + ")");
        }
    }
}
