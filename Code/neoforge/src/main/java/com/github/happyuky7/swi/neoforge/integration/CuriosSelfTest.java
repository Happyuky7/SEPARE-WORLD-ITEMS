package com.github.happyuky7.swi.neoforge.integration;

import com.github.happyuky7.swi.mc.VanillaSelfTest;
import com.github.happyuky7.swi.mc.compat.ItemCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;

import java.util.List;
import java.util.Map;

/**
 * Self-test hook: puts an item in the first Curios slot found and checks it follows the group.
 * The dev run creates a "ring" slot through a test datapack.
 */
public final class CuriosSelfTest implements VanillaSelfTest.Hook {

    private String slotType;
    private String expected;

    @Override
    public String name() {
        return "Curios";
    }

    @Override
    public void prepare(ServerPlayer player, List<String> failures) {
        CuriosApi.getCuriosInventory(player).ifPresent(inv -> {
            // Real players get their slots when they join the level; the fake test player never does.
            if (inv.getCurios().isEmpty()) {
                inv.reset();
            }
            for (Map.Entry<String, ICurioStacksHandler> e : inv.getCurios().entrySet()) {
                if (e.getValue().getStacks().getSlots() > 0) {
                    slotType = e.getKey();
                    ItemStack stack = new ItemStack(Items.GOLD_INGOT, 3);
                    e.getValue().getStacks().setStackInSlot(0, stack);
                    expected = ItemCodec.encode(player, stack);
                    return;
                }
            }
        });
        if (slotType == null) {
            failures.add("Curios: no curio slot available on the test player");
        }
    }

    @Override
    public void verifyCleared(ServerPlayer player, List<String> failures) {
        if (slotType != null && !current(player).isEmpty()) {
            failures.add("Curios: slot " + slotType + " not cleared in the other group");
        }
    }

    @Override
    public void verifyRestored(ServerPlayer player, List<String> failures) {
        if (slotType != null && !String.valueOf(expected).equals(ItemCodec.encode(player, current(player)))) {
            failures.add("Curios: slot " + slotType + " not restored (expected " + expected + ")");
        }
    }

    private ItemStack current(ServerPlayer player) {
        return CuriosApi.getCuriosInventory(player)
                .flatMap(inv -> inv.getStacksHandler(slotType))
                .map(h -> h.getStacks().getStackInSlot(0))
                .orElse(ItemStack.EMPTY);
    }
}
